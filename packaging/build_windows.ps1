#Requires -Version 5.1
<#
.SYNOPSIS
    Build a self-contained GABI app-image (and optionally an MSI installer) using jpackage.

.DESCRIPTION
    Prerequisite: JDK 17+ (jpackage is bundled with the JDK since JDK 16 / JEP 392).
    MSI/EXE installer types additionally require WiX Toolset 3.x on PATH.
    app-image type (the default) needs NO WiX.

    Step 1: builds the Spring Boot fat-jar via Maven (skipping tests).
    Step 2: runs jpackage to produce a self-contained app-image under packaging\bin\GABI\.
    The bundled runtime is produced automatically by jlink (jpackage --runtime-image omitted
    -> jpackage auto-runs jlink to build a trimmed JRE from the JDK on PATH).

.PARAMETER Type
    jpackage --type value. Default: app-image (no WiX needed).
    Alternatives: msi, exe (both require WiX Toolset installed).

.PARAMETER SkipBuild
    Skip the Maven build step (use if gabi-1.0.0-exec.jar already exists).

.EXAMPLE
    # Build app-image (default, no WiX required)
    .\packaging\build_windows.ps1

.EXAMPLE
    # Build MSI installer (requires WiX 3.x on PATH)
    .\packaging\build_windows.ps1 -Type msi

.EXAMPLE
    # Skip Maven build if jar already exists
    .\packaging\build_windows.ps1 -SkipBuild
#>

param(
    [ValidateSet("app-image", "msi", "exe")]
    [string]$Type = "app-image",
    [switch]$SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# --- Resolve repo root (script lives in <repo>/packaging/) ---
$RepoRoot = Split-Path -Parent $PSScriptRoot
$JarDir   = Join-Path $RepoRoot "target"
$JarName  = "gabi-1.0.0-exec.jar"
$JarPath  = Join-Path $JarDir $JarName
$DestDir  = Join-Path $PSScriptRoot "bin"

Write-Host "=== GABI jpackage build ===" -ForegroundColor Cyan
Write-Host "Repo root : $RepoRoot"
Write-Host "Jar       : $JarPath"
Write-Host "Type      : $Type"
Write-Host "Dest      : $DestDir"
Write-Host ""

# --- Step 1: Maven build ---
if (-not $SkipBuild) {
    Write-Host "[1/2] Building Spring Boot fat-jar (mvn -q -DskipTests package)..." -ForegroundColor Yellow
    Push-Location $RepoRoot
    try {
        & mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Maven build failed (exit $LASTEXITCODE). Aborting."
            exit 1
        }
    } finally {
        Pop-Location
    }
    Write-Host "      Maven build OK." -ForegroundColor Green
} else {
    Write-Host "[1/2] Skipping Maven build (-SkipBuild specified)." -ForegroundColor DarkGray
}

# --- Verify jar exists ---
if (-not (Test-Path $JarPath)) {
    Write-Error "Jar not found: $JarPath`nRun Maven first (step 1) or omit -SkipBuild."
    exit 2
}

# --- Step 2: jpackage ---
Write-Host ""
Write-Host "[2/2] Running jpackage --type $Type ..." -ForegroundColor Yellow

# Clean previous build output for this type to avoid stale artifacts
$AppImageDir = Join-Path $DestDir "GABI"
if ($Type -eq "app-image" -and (Test-Path $AppImageDir)) {
    Write-Host "      Removing stale $AppImageDir ..."
    Remove-Item -Recurse -Force $AppImageDir
}

# The Main-Class in the MANIFEST.MF of the Spring Boot 4.x exec fat-jar is:
#   org.springframework.boot.loader.launch.JarLauncher
# This is the Spring Boot 4.x (Boot Loader 3.x) repackaged-jar launcher.
# It reads BOOT-INF/lib/ and BOOT-INF/classes/ from the fat-jar and delegates
# to GabiApplication (listed as Start-Class in the manifest).
# Using JarLauncher as --main-class is the correct approach for a non-modular
# Spring Boot fat-jar packaged with the spring-boot-maven-plugin repackage goal.

$JpackageArgs = @(
    "--type",        $Type,
    "--name",        "GABI",
    "--app-version", "1.0.0",
    "--input",       $JarDir,
    "--main-jar",    $JarName,
    "--main-class",  "org.springframework.boot.loader.launch.JarLauncher",
    "--dest",        $DestDir,
    "--win-console"          # keep the console window so CLI output is visible
    # --win-menu, --win-shortcut, --win-dir-chooser can be added for installer types
)

if ($Type -eq "msi" -or $Type -eq "exe") {
    $JpackageArgs += @("--win-menu", "--win-shortcut", "--win-dir-chooser")
}

# No --icon: no icon asset found in the repo (skip rather than error)
# Add --icon .\packaging\gabi.ico if you place a 256x256 ICO file there later.

Write-Host "      jpackage $($JpackageArgs -join ' ')"
Write-Host ""

& jpackage @JpackageArgs
$JpackageExit = $LASTEXITCODE

if ($JpackageExit -ne 0) {
    Write-Error "jpackage failed (exit $JpackageExit)."
    exit 1
}

Write-Host ""
Write-Host "=== Build successful ===" -ForegroundColor Green

if ($Type -eq "app-image") {
    Write-Host "App-image: $AppImageDir"
    Write-Host ""
    Write-Host "To launch GABI (CLI mode, default):"
    Write-Host "  $AppImageDir\GABI.exe"
    Write-Host ""
    Write-Host "To launch in HTTP server mode (MCP Streamable-HTTP + REST on :8080):"
    Write-Host "  $AppImageDir\GABI.exe --spring.profiles.active=server"
    Write-Host ""
    Write-Host "To launch in STDIO mode (for MCP clients that drive the process):"
    Write-Host "  $AppImageDir\GABI.exe --spring.ai.mcp.server.stdio=true --spring.main.web-application-type=none"
    Write-Host ""
    Write-Host "Remember: set DB_USER and DB_PASSWORD environment variables before launching."
} else {
    Write-Host "Installer produced in: $DestDir"
}

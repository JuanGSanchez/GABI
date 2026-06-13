# GABI — Packaging Guide (jpackage)

GABI is distributed as a **self-contained native app-image** built with `jpackage` (bundled in
JDK 17+). The app-image includes a trimmed JRE produced by `jlink`, so end-users do not need a
system-wide Java installation to run GABI.

---

## Prerequisites

| Requirement | Detail |
|-------------|--------|
| **JDK 17+** | `jpackage` ships with the JDK since JDK 16 (JEP 392). JDK 17 is the project baseline. Verify with `jpackage --version`. |
| **Maven 3.6.3+** | Needed to build the fat-jar before packaging. Verify with `mvn --version`. |
| **WiX Toolset 3.x** | **Only** required for `--type msi` or `--type exe` (Windows installer). NOT needed for `app-image`. Install from wixtoolset.org if you want an MSI. |
| **Apache Derby network server** | GABI connects to Derby over the network (`localhost:1527`). The packaged app bundles the Derby network client driver (`derbyclient`) but NOT a Derby server. Start the server separately before launching GABI. |

---

## Build commands

### Windows (PowerShell)

```powershell
# From the repository root — builds the fat-jar then the app-image
.\packaging\build_windows.ps1

# Skip the Maven build if target\gabi-1.0.0-exec.jar already exists
.\packaging\build_windows.ps1 -SkipBuild

# Build an MSI installer instead (requires WiX Toolset 3.x)
.\packaging\build_windows.ps1 -Type msi
```

### Linux / macOS (Bash)

```bash
# From the repository root
./packaging/build_posix.sh            # app-image (default)
./packaging/build_posix.sh deb        # Debian package (requires dpkg / fakeroot)
./packaging/build_posix.sh rpm        # RPM package (requires rpm-build)
./packaging/build_posix.sh dmg        # macOS DMG (requires Xcode CLI tools)
```

### Manual (single jpackage command, Windows)

If you prefer to run jpackage directly after building the jar:

```bat
rem Build the jar first
mvn -q -DskipTests package

rem Build the app-image (no WiX needed)
jpackage --type app-image ^
    --name GABI ^
    --app-version 1.0.0 ^
    --input target ^
    --main-jar gabi-1.0.0-exec.jar ^
    --main-class org.springframework.boot.loader.launch.JarLauncher ^
    --dest packaging\bin ^
    --win-console
```

---

## The --main-class value explained

The Spring Boot Maven plugin (`spring-boot-maven-plugin`) repackages the jar so that its
`MANIFEST.MF` contains:

```
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: GabiApplication
```

`JarLauncher` is the Spring Boot 4.x fat-jar launcher (package `org.springframework.boot.loader.launch`
in Boot 4.x / Boot Loader 3.x — note: Spring Boot 3.x used the older `org.springframework.boot.loader.JarLauncher`
without the `.launch` sub-package). It reads all dependencies from `BOOT-INF/lib/` inside the jar
and then delegates to `GabiApplication` (the `Start-Class`). Passing `JarLauncher` as `--main-class`
to jpackage is the correct approach for a non-modular Spring Boot fat-jar; pointing directly at
`GabiApplication` would fail because the `BOOT-INF/` classpath layout would not be set up.

---

## Output layout

After a successful `app-image` build the `packaging/bin/` directory contains:

```
packaging/
  bin/
    GABI/                          <- self-contained app-image root
      GABI.exe                     <- Windows launcher (console-capable, --win-console)
      runtime/                     <- bundled JRE (produced by jlink, trimmed from JDK 17)
        bin/
        lib/
        conf/
      app/
        gabi-1.0.0-exec.jar        <- the Spring Boot fat-jar
      GABI.ico                     <- app icon (absent if no icon was provided)
```

On Linux/macOS the launcher is `bin/GABI/bin/GABI` (no `.exe`).

Typical sizes:
- `runtime/`: ~60–80 MB (trimmed JRE; jpackage auto-runs jlink with `--strip-debug
  --no-man-pages --no-header-files`).
- `app/gabi-1.0.0-exec.jar`: ~120 MB (Spring Boot fat-jar including all Spring AI and Derby deps).
- Total app-image: ~180–200 MB.

---

## Running the packaged app

GABI runs in three modes. Set `DB_USER` and `DB_PASSWORD` via environment variables before
launching (never hardcode credentials).

### CLI mode (default)

```bat
rem Windows
set DB_USER=<your-derby-user>
set DB_PASSWORD=<your-derby-password>
packaging\bin\GABI\GABI.exe
```

```bash
# Linux / macOS
export DB_USER=<your-derby-user>
export DB_PASSWORD=<your-derby-password>
packaging/bin/GABI/bin/GABI
```

The interactive CLI library manager starts. Connect as an admin with the credentials you supplied
when you initialized the Derby database (default demo credentials from the original project were
`admin / 1234`; change them — see security notes).

### HTTP server mode (MCP Streamable-HTTP + REST on port 8080)

```bat
rem Windows
set DB_USER=<your-derby-user>
set DB_PASSWORD=<your-derby-password>
packaging\bin\GABI\GABI.exe --spring.profiles.active=server
```

The `server` Spring profile activates the embedded Tomcat web stack, exposing:
- MCP Streamable-HTTP endpoint: `http://localhost:8080/mcp`
- REST API: `http://localhost:8080/api/…`

Override the port with `--server.port=<port>`.

### STDIO mode (for MCP clients that drive the process)

```bat
rem Windows
set DB_USER=<your-derby-user>
set DB_PASSWORD=<your-derby-password>
packaging\bin\GABI\GABI.exe ^
    --spring.ai.mcp.server.stdio=true ^
    --spring.main.web-application-type=none
```

The MCP client (e.g. Claude Desktop) sends JSON-RPC requests over stdin and reads responses from
stdout. No HTTP port is opened. See `docs/README-access.md` for the MCP client configuration.

### Passing the AI provider profile

GABI defaults to **Ollama** (local, no API key). To switch providers add a profile flag:

```bat
rem OpenAI (requires OPENAI_API_KEY env var)
set OPENAI_API_KEY=sk-...
packaging\bin\GABI\GABI.exe --spring.profiles.active=server,openai

rem Anthropic chat (requires ANTHROPIC_API_KEY; embeddings stay on Ollama)
set ANTHROPIC_API_KEY=sk-ant-...
packaging\bin\GABI\GABI.exe --spring.profiles.active=server,anthropic
```

---

## Bundled JRE notes

`jpackage` automatically runs `jlink` to produce a trimmed JRE from the JDK on PATH. Because GABI
is a non-modular (classpath) application, the auto-generated runtime includes the full set of JDK
modules needed to run a classpath app — it cannot be trimmed further without explicit
`--add-modules` / `--jlink-options`. The result is still significantly smaller than a full JDK.

To use a pre-built runtime image instead (e.g. a Liberica or Temurin compact JRE), pass
`--runtime-image <path-to-runtime>` in place of the auto-jlink step.

---

## Optional: MSI / EXE installer (Windows, requires WiX)

```bat
rem Requires WiX Toolset 3.x on PATH (https://wixtoolset.org/)
.\packaging\build_windows.ps1 -Type msi
```

The MSI installer registers GABI in Add/Remove Programs, creates Start Menu shortcuts, and lets
the user choose the install directory. `--win-console` is also passed so the console window remains
accessible.

---

## External configuration

The packaged app reads writable configuration from `%USERPROFILE%\.gabi\` (Windows) or
`~/.gabi/` (Linux/macOS) — NOT from its own install directory (which may be under Program Files
and not writable). Do not place `configuration.properties` in `packaging/bin/GABI/app/`.

---

## Security notes

- Never commit `DB_USER` / `DB_PASSWORD` or API keys. Supply them via environment variables only.
- The Derby network server (`:1527`) must be secured independently; GABI's client credentials are
  the only auth layer on the DB connection.
- The packaged app bundles the Derby **network client** driver (`derbyclient-10.16.1.1.jar`).
  It does NOT bundle a Derby network server. Start `NetworkServerControl start` (or equivalent)
  on the DB host before launching GABI.

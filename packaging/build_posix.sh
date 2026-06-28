#!/usr/bin/env bash
# build_posix.sh — build a self-contained GABI app-image on Linux or macOS using jpackage.
#
# Prerequisite: JDK 17+ (jpackage ships with the JDK since JDK 16 / JEP 392).
# Linux installer types (deb, rpm) additionally require fakeroot / dpkg / rpm-build.
# macOS installer types (dmg, pkg) require Xcode command-line tools.
# app-image type (the default) needs none of the above extras.
#
# Usage:
#   cd <repo-root>
#   ./packaging/build_posix.sh [app-image|deb|rpm|dmg|pkg]
#
# Default type: app-image (portable self-contained folder, no system packages needed).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"
JAR_DIR="$REPO_ROOT/target"
JAR_NAME="gabi-1.0.0-exec.jar"
JAR_PATH="$JAR_DIR/$JAR_NAME"
DEST_DIR="$SCRIPT_DIR/bin"
TYPE="${1:-app-image}"

echo "=== GABI jpackage build (POSIX) ==="
echo "Repo root : $REPO_ROOT"
echo "Jar       : $JAR_PATH"
echo "Type      : $TYPE"
echo "Dest      : $DEST_DIR"
echo ""

# Step 1: Maven build
echo "[1/2] Building Spring Boot fat-jar (mvn -q -DskipTests package)..."
(cd "$REPO_ROOT" && mvn -q -DskipTests package)
echo "      Maven build OK."

# Verify jar
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: Jar not found: $JAR_PATH" >&2
    exit 2
fi

# Step 2: jpackage
echo ""
echo "[2/2] Running jpackage --type $TYPE ..."

# The Main-Class in the Spring Boot 4.x exec fat-jar MANIFEST.MF is:
#   org.springframework.boot.loader.launch.JarLauncher
# JarLauncher reads BOOT-INF/lib/ and BOOT-INF/classes/ and delegates to GabiApplication.
# This is the correct --main-class for a Spring Boot repackaged jar.

JPACKAGE_ARGS=(
    --type        "$TYPE"
    --name        "GABI"
    --app-version "1.0.0"
    --input       "$JAR_DIR"
    --main-jar    "$JAR_NAME"
    --main-class  "org.springframework.boot.loader.launch.JarLauncher"
    --dest        "$DEST_DIR"
)

# No --icon: no icon asset found in the repo.
# Add: --icon ./packaging/gabi.png  (512x512 PNG on macOS; 48x48 PNG on Linux)

jpackage "${JPACKAGE_ARGS[@]}"

echo ""
echo "=== Build successful ==="
echo "App-image: $DEST_DIR/GABI"
echo ""
echo "To launch GABI (CLI mode, default):"
echo "  $DEST_DIR/GABI/bin/GABI"
echo ""
echo "To launch in HTTP server mode:"
echo "  $DEST_DIR/GABI/bin/GABI --spring.profiles.active=server"
echo ""
echo "To launch in STDIO mode:"
echo "  $DEST_DIR/GABI/bin/GABI --spring.ai.mcp.server.stdio=true --spring.main.web-application-type=none"
echo ""
echo "Remember: set DB_USER and DB_PASSWORD environment variables before launching."

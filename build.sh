#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BIN_DIR="${SCRIPT_DIR}/bin"
DIST_DIR="${SCRIPT_DIR}/dist"

echo "=== 1. Cleaning build directories ==="
rm -rf "${BIN_DIR}" "${DIST_DIR}"
mkdir -p "${BIN_DIR}" "${DIST_DIR}"

echo "=== 2. Compiling Java sources ==="
JAVAC_BIN="javac"
if [ -x "/opt/homebrew/opt/openjdk@17/bin/javac" ]; then
    JAVAC_BIN="/opt/homebrew/opt/openjdk@17/bin/javac"
fi

"${JAVAC_BIN}" -d "${BIN_DIR}" $(find "${SCRIPT_DIR}/src" -name "*.java")

echo "=== 3. Packaging smpopenmod.jar ==="
JAR_BIN="jar"
if [ -x "/opt/homebrew/opt/openjdk@17/bin/jar" ]; then
    JAR_BIN="/opt/homebrew/opt/openjdk@17/bin/jar"
fi

"${JAR_BIN}" -cfm "${DIST_DIR}/smpopenmod.jar" "${SCRIPT_DIR}/resources/MANIFEST.MF" -C "${BIN_DIR}" .

echo "=== 4. Packaging smpopenmod.deb ==="
"${SCRIPT_DIR}/package_deb.sh"

echo "=== Build Complete! Artifacts: ==="
ls -lh "${DIST_DIR}"

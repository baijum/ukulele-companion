#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=== Installing JDK 21 ==="
export HOMEBREW_NO_AUTO_UPDATE=1
export HOMEBREW_NO_ENV_HINTS=1
brew install openjdk@21
JAVA_HOME="$(brew --prefix openjdk@21)"
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: Failed to resolve JAVA_HOME for openjdk@21."
    exit 1
fi
export JAVA_HOME

echo "=== Building KMP shared framework ==="
cd "$REPO_ROOT"
./gradlew :shared:linkReleaseFrameworkIosArm64 -Dorg.gradle.java.installations.auto-download=false

echo "=== Downloading ONNX Runtime ==="
bash "$REPO_ROOT/iosApp/setup_onnxruntime.sh"

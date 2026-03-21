#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=== Installing JDK 17 ==="
export HOMEBREW_NO_AUTO_UPDATE=1
export HOMEBREW_NO_ENV_HINTS=1
brew install openjdk@17
JAVA_HOME="$(brew --prefix openjdk@17)"
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: Failed to resolve JAVA_HOME for openjdk@17."
    exit 1
fi
export JAVA_HOME

# Create symlink so Xcode build phases can discover Java automatically
sudo ln -sfn "$JAVA_HOME/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk

echo "=== Building KMP shared framework ==="
cd "$REPO_ROOT"
./gradlew :shared:linkReleaseFrameworkIosArm64

echo "=== Downloading ONNX Runtime ==="
bash "$REPO_ROOT/iosApp/setup_onnxruntime.sh"

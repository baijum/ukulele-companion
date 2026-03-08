#!/bin/bash
# Downloads and extracts the ONNX Runtime iOS xcframework.
# Run this once before building the iOS app.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRAMEWORKS_DIR="$SCRIPT_DIR/Frameworks"
ORT_VERSION="1.24.2"
ORT_URL="https://download.onnxruntime.ai/pod-archive-onnxruntime-c-${ORT_VERSION}.zip"
ORT_SHA256="f7100a992d2a8135168c8afd831e6a58b465349101982aa58b3e11d36e600b54"

if [ -d "$FRAMEWORKS_DIR/onnxruntime.xcframework" ]; then
    echo "onnxruntime.xcframework already exists, skipping download."
    exit 0
fi

echo "Downloading ONNX Runtime ${ORT_VERSION}..."
TMPFILE=$(mktemp /tmp/onnxruntime-XXXXXX.zip)
trap "rm -f $TMPFILE" EXIT

curl -sL -o "$TMPFILE" "$ORT_URL"

echo "Verifying checksum..."
ACTUAL_SHA=$(shasum -a 256 "$TMPFILE" | awk '{print $1}')
if [ "$ACTUAL_SHA" != "$ORT_SHA256" ]; then
    echo "ERROR: SHA256 mismatch. Expected: $ORT_SHA256, Got: $ACTUAL_SHA"
    exit 1
fi

echo "Extracting xcframework..."
mkdir -p "$FRAMEWORKS_DIR"
unzip -q -o "$TMPFILE" "onnxruntime.xcframework/*" -d "$FRAMEWORKS_DIR/"

# Add module maps for Swift import
for PLATFORM_DIR in "$FRAMEWORKS_DIR"/onnxruntime.xcframework/*/onnxruntime.framework; do
    MODULES_DIR="$PLATFORM_DIR/Modules"
    if [ ! -f "$MODULES_DIR/module.modulemap" ]; then
        mkdir -p "$MODULES_DIR"
        cat > "$MODULES_DIR/module.modulemap" <<'MODULEMAP'
framework module onnxruntime {
    header "onnxruntime_c_api.h"
    export *
}
MODULEMAP
    fi
done

echo "Done. onnxruntime.xcframework is ready at $FRAMEWORKS_DIR/"

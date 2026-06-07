#!/bin/bash
# Downloads and extracts the ONNX Runtime iOS xcframework.
# Run this once before building the iOS app.
#
# To update ONNX Runtime to a new version:
#   1. Change ORT_VERSION below to the new version number
#   2. Download the new zip: curl -LO "https://download.onnxruntime.ai/pod-archive-onnxruntime-c-<version>.zip"
#   3. Compute the SHA256: shasum -a 256 pod-archive-onnxruntime-c-<version>.zip
#   4. Update ORT_SHA256 below with the new hash
#   5. Also update onnxruntime version in gradle/libs.versions.toml (Android)
#   6. Re-run this script (it auto-detects the version mismatch and re-downloads)
#
# Note: Dependabot cannot detect updates to this script-based dependency.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRAMEWORKS_DIR="$SCRIPT_DIR/Frameworks"
ORT_VERSION="1.26.0"
ORT_FILENAME="pod-archive-onnxruntime-c-${ORT_VERSION}.zip"
ORT_PRIMARY_URL="https://github.com/baijum/ukulele-companion/releases/download/vendor-onnxruntime-c-${ORT_VERSION}/${ORT_FILENAME}"
ORT_FALLBACK_URL="https://download.onnxruntime.ai/${ORT_FILENAME}"
ORT_FALLBACK_URL2="https://onnxruntimepackages.z14.web.core.windows.net/${ORT_FILENAME}"
ORT_SHA256="08502ec252f6f03ebee6e729cbe0525c777b47daa76d3bc8c04bba82201d8a36"

VERSION_MARKER="$FRAMEWORKS_DIR/.onnxruntime_version"
if [ -d "$FRAMEWORKS_DIR/onnxruntime.xcframework" ]; then
    if [ -f "$VERSION_MARKER" ] && [ "$(cat "$VERSION_MARKER")" = "$ORT_VERSION" ]; then
        echo "onnxruntime.xcframework $ORT_VERSION already exists, skipping download."
        exit 0
    fi
    echo "Version mismatch or missing marker — removing old xcframework..."
    rm -rf "$FRAMEWORKS_DIR/onnxruntime.xcframework"
    rm -f "$VERSION_MARKER"
fi

TMPFILE=$(mktemp /tmp/onnxruntime-XXXXXX.zip)
trap 'rm -f "$TMPFILE"' EXIT

DOWNLOADED=false
for ORT_URL in "$ORT_PRIMARY_URL" "$ORT_FALLBACK_URL" "$ORT_FALLBACK_URL2"; do
    echo "Downloading ONNX Runtime ${ORT_VERSION} from ${ORT_URL}..."
    if curl --fail --show-error --location \
            --connect-timeout 15 --max-time 300 \
            --retry 3 --retry-delay 5 --retry-all-errors \
            -o "$TMPFILE" "$ORT_URL"; then
        DOWNLOADED=true
        break
    fi
    echo "Warning: Download failed from ${ORT_URL}, trying next mirror..."
done

if [ "$DOWNLOADED" != "true" ]; then
    echo ""
    echo "ERROR: Failed to download ONNX Runtime from all mirrors."
    echo "Tried: ${ORT_PRIMARY_URL}"
    echo "       ${ORT_FALLBACK_URL}"
    echo "       ${ORT_FALLBACK_URL2}"
    echo "You can also download manually and place onnxruntime.xcframework in iosApp/Frameworks/"
    exit 1
fi

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

echo "$ORT_VERSION" > "$VERSION_MARKER"
echo "Done. onnxruntime.xcframework $ORT_VERSION is ready at $FRAMEWORKS_DIR/"

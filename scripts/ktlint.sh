#!/usr/bin/env bash
#
# Standalone ktlint runner — no Gradle plugin, no dependency-lock churn.
#
# Downloads a pinned ktlint release (once) into a user cache and runs it.
# With no path arguments it lints the Kotlin source sets; pass -F (or --format)
# to auto-fix. Used by scripts/preflight.sh and CI.
#
#   scripts/ktlint.sh            # check shared/ + app/ Kotlin sources
#   scripts/ktlint.sh -F         # format them in place
#   scripts/ktlint.sh <globs>    # check specific files/globs
#
set -euo pipefail

KTLINT_VERSION="1.8.0"
CACHE_DIR="${KTLINT_CACHE_DIR:-${XDG_CACHE_HOME:-$HOME/.cache}/ktlint}"
KTLINT_BIN="$CACHE_DIR/ktlint-$KTLINT_VERSION"
DOWNLOAD_URL="https://github.com/ktlint/ktlint/releases/download/${KTLINT_VERSION}/ktlint"

if [[ ! -x "$KTLINT_BIN" ]]; then
  echo "Downloading ktlint ${KTLINT_VERSION} -> ${KTLINT_BIN}" >&2
  mkdir -p "$CACHE_DIR"
  curl --fail --silent --show-error --location -o "$KTLINT_BIN.tmp" "$DOWNLOAD_URL"
  chmod +x "$KTLINT_BIN.tmp"
  mv "$KTLINT_BIN.tmp" "$KTLINT_BIN"
fi

# If the caller passed no path/glob (only flags or nothing), lint the source sets.
has_path=false
for arg in "$@"; do
  [[ "$arg" != -* ]] && has_path=true
done
if ! $has_path; then
  set -- "$@" "shared/src/**/*.kt" "app/src/**/*.kt"
fi

exec "$KTLINT_BIN" "$@"

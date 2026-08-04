#!/usr/bin/env bash
#
# Pre-submission checks — one "definition of done" for Android + shared changes.
# Runs the deterministic gates (matches CI), reports every failure rather than
# stopping at the first, then prints the manual checklist that can't be automated.
#
# Usage:
#   scripts/preflight.sh                    # run all gates (Android + shared)
#   PREFLIGHT_SKIP_LINT=1 scripts/preflight.sh   # skip the slower Android lint
#   PREFLIGHT_IOS=1 scripts/preflight.sh         # also run the iOS build gate
#
# Note: instrumented (device) tests are intentionally NOT run here — they need
# a running emulator. See the manual checklist below.
set -uo pipefail
cd "$(dirname "$0")/.."

bold() { printf '\033[1m%s\033[0m\n' "$1"; }
declare -a RESULTS=()
FAILED=0

run() { # run "<label>" <command...>
  local label="$1"; shift
  bold "▶ $label"
  if "$@"; then
    RESULTS+=("PASS  $label")
  else
    RESULTS+=("FAIL  $label")
    FAILED=1
  fi
  echo
}

run "ktlint (new violations only)" scripts/ktlint.sh --baseline=ktlint-baseline.xml
run "shared KMP tests"             ./gradlew :shared:jvmTest
run "Android unit tests"           ./gradlew testDebugUnitTest
if [[ "${PREFLIGHT_SKIP_LINT:-0}" != "1" ]]; then
  run "Android lint (debug)"       ./gradlew lintDebug
else
  RESULTS+=("SKIP  Android lint (PREFLIGHT_SKIP_LINT=1)")
fi

if [[ "${PREFLIGHT_IOS:-0}" == "1" ]]; then
  if command -v xcodebuild &>/dev/null; then
    run "iOS build" xcodebuild \
      -project iosApp/UkuleleCompanion.xcodeproj \
      -scheme UkuleleCompanion \
      -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
      -quiet \
      build
  else
    RESULTS+=("SKIP  iOS build (xcodebuild not found)")
  fi
else
  RESULTS+=("SKIP  iOS build (set PREFLIGHT_IOS=1 to include)")
fi

bold "════════ Automated gate summary ════════"
printf '  %s\n' "${RESULTS[@]}"
echo

bold "════════ Manual checklist (verify by hand) ════════"
cat <<'EOF'
  Both platforms
    [ ] Both High-G and Low-G tuning work (if chord/note logic changed)
    [ ] Left-handed mode not broken (if fretboard UI changed)
    [ ] New animations respect reduce motion
    [ ] No per-frame allocations added in the audio hot path

  Android
    [ ] Verified on device or emulator
    [ ] TalkBack navigation works for changed screens
    [ ] Correct in light, dark, and high-contrast themes

  iOS (run with PREFLIGHT_IOS=1 to automate the build gate)
    [ ] xcodebuild build succeeds (automated if PREFLIGHT_IOS=1)
    [ ] VoiceOver navigation works for changed screens
EOF

if [[ "$FAILED" -ne 0 ]]; then
  echo
  bold "✗ One or more automated gates failed."
  exit 1
fi
echo
bold "✓ Automated gates passed. Complete the manual checklist before submitting."

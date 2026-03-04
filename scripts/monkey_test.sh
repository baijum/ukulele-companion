#!/usr/bin/env bash
#
# Reproducible Android Monkey UI stress test for Ukulele Companion.
#
# Usage:
#   ./scripts/monkey_test.sh              # 10,000 events, auto-generated seed
#   ./scripts/monkey_test.sh 42           # 10,000 events, seed=42 (reproducible)
#   ./scripts/monkey_test.sh 42 50000     # 50,000 events, seed=42
#
# The seed is printed at the start and saved in the log file name so you can
# reproduce any crash by re-running with the same seed.

set -euo pipefail

PACKAGE="com.baijum.ukufretboard"
SEED="${1:-$(date +%s)}"
EVENTS="${2:-10000}"
LOG_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_FILE="${LOG_DIR}/monkey_log_${SEED}.txt"

echo "============================================"
echo " Monkey Test: Ukulele Companion"
echo " Package:  $PACKAGE"
echo " Seed:     $SEED"
echo " Events:   $EVENTS"
echo " Log file: $LOG_FILE"
echo "============================================"
echo ""

if ! adb devices 2>/dev/null | grep -q 'device$'; then
    echo "ERROR: No connected device or emulator found."
    echo "       Start an emulator or connect a device first."
    exit 1
fi

adb shell monkey -p "$PACKAGE" \
    -s "$SEED" \
    --throttle 100 \
    --pct-touch 45 \
    --pct-motion 20 \
    --pct-nav 15 \
    --pct-majornav 5 \
    --pct-syskeys 5 \
    --pct-appswitch 5 \
    --pct-anyevent 5 \
    --ignore-crashes \
    --ignore-timeouts \
    -v -v \
    "$EVENTS" 2>&1 | tee "$LOG_FILE"

echo ""
echo "============================================"
echo " Monkey test complete."
echo " Log saved to: $LOG_FILE"
echo " Seed was: $SEED (use this to reproduce)"
echo "============================================"

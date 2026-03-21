---
name: android-bug-reproduce
description: Reproduce and debug Android bugs on an emulator using ADB. Covers emulator setup, test data seeding, UI navigation via uiautomator, log capture, and screenshots. Use when the user asks to reproduce a bug, debug an issue on an emulator, or investigate an Android crash/behavior.
---

# Android Bug Reproduction on Emulator

Systematically reproduce and investigate Android bugs using ADB and the emulator.

## Prerequisites

- Android SDK installed with `emulator` and `adb` on PATH
- At least one AVD configured (check with `emulator -list-avds`)
- A debug APK (build with `./gradlew assembleDebug` if needed)

## Workflow

### Step 1: Start the emulator

Check for a running emulator first:

```bash
adb devices
```

If none is running, start one:

```bash
emulator -list-avds
emulator -avd <avd_name> -no-snapshot-load &
```

Wait for the device to be ready:

```bash
adb wait-for-device
adb shell getprop sys.boot_completed  # should return "1"
```

### Step 2: Install the debug APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Seed test data (if needed)

For features that require existing data (e.g., chord sheets, favorites), seed SharedPreferences directly:

```bash
# Stop the app first
adb shell am force-stop com.baijum.ukufretboard

# Write test data to SharedPreferences
adb shell run-as com.baijum.ukufretboard sh -c 'cat > shared_prefs/<pref_file>.xml << '\''XMLEOF'\''
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<map>
    <!-- Your test data here -->
</map>
XMLEOF'
```

Common preference files:

| File | Contents |
|------|----------|
| `chord_sheets.xml` | Saved songs/chord sheets (JSON list) |
| `favorites.xml` | Favorited chords |
| `app_settings.xml` | App settings |

Example — seed a chord sheet:

```bash
adb shell run-as com.baijum.ukufretboard sh -c 'cat > shared_prefs/chord_sheets.xml << '\''XMLEOF'\''
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<map>
    <string name="chord_sheets_json">[{"id":"test-1","title":"Test Song","artist":"Test","content":"[C]Hello [Am]world\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6\nLine 7\nLine 8\nLine 9\nLine 10","strumPatternName":"","labels":[],"createdAt":1700000000000,"updatedAt":1700000000000}]</string>
</map>
XMLEOF'
```

### Step 4: Launch the app and navigate

```bash
adb shell am start -n com.baijum.ukufretboard/.MainActivity
```

#### Find UI element coordinates with uiautomator

Dump the current UI tree to find element bounds:

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb shell cat /sdcard/ui.xml | head -c 5000  # quick preview
```

Search for a specific element by content-description or text:

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml /tmp/ui.xml
grep -o 'content-desc="[^"]*"[^/]*bounds="[^"]*"' /tmp/ui.xml
```

#### Common navigation actions

```bash
# Open navigation drawer
adb shell uiautomator dump /sdcard/ui.xml
# Find "Open navigation menu" button bounds, then:
adb shell input tap <x> <y>

# Tap by coordinates (center of element bounds)
# bounds="[left,top][right,bottom]" -> tap at ((left+right)/2, (top+bottom)/2)
adb shell input tap 540 960

# Go back
adb shell input keyevent 4

# Enter text in a focused field
adb shell input text "hello"

# Scroll down
adb shell input swipe 540 1500 540 500 300
```

### Step 5: Add instrumentation logging

When the root cause isn't obvious, add temporary `Log.d` statements to the relevant code:

```kotlin
import android.util.Log

Log.d("DEBUG_<issue>", "state: autoScrolling=$autoScrolling, scrollValue=${scrollState.value}")
```

Rebuild and reinstall:

```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 6: Capture and filter logs

```bash
# Filter for your debug tag
adb logcat -c  # clear old logs
adb logcat -s "DEBUG_<issue>:D" &

# Or capture to file for analysis
adb logcat -s "DEBUG_<issue>:D" > /tmp/debug_log.txt &
```

Reproduce the bug, then stop logcat and analyze the output.

### Step 7: Take screenshots

```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png /tmp/screenshot_before.png
```

For before/after comparison:

```bash
# Before fix
adb shell screencap -p /sdcard/before.png
adb pull /sdcard/before.png /tmp/before.png

# After fix (rebuild, reinstall, reproduce)
adb shell screencap -p /sdcard/after.png
adb pull /sdcard/after.png /tmp/after.png
```

### Step 8: Clean up

Remove debug logging before committing:

```bash
# Find all debug log lines added during this session
grep -rn "DEBUG_<issue>" app/src/main/java/
```

Remove the `Log.d` lines and the `import android.util.Log` if no longer needed.

## Tips

- **Prefer seeding data over UI automation** — writing SharedPreferences XML is far more reliable than tapping through UI to create test data.
- **Use `uiautomator dump` before every tap** — screen layouts change between devices and orientations; never hardcode coordinates from memory.
- **Keep debug tags unique** — include the issue number (e.g., `DEBUG_issue41`) so logs don't mix with other debugging sessions.
- **Force-stop before seeding** — SharedPreferences are cached in memory; the app must be stopped before writing XML files.

---
name: ios-screenshot-capture
description: Capture screenshots and screen recordings from an iOS Simulator using xcrun simctl. Use when the user asks to take iOS screenshots, capture iOS screens, record iOS video, create iOS app visuals, generate iOS documentation images, or mentions simctl screenshots.
---

# iOS Screenshot Capture via Simulator

Capture screenshots and screen recordings from a running iOS Simulator using `xcrun simctl`.

## Prerequisites

- Xcode installed at `/Applications/Xcode.app`
- A booted iOS Simulator (verify with `xcrun simctl list devices booted`)
- App installed on the simulator
- If no simulator is running, use the [ios-simulator-run](~/.cursor/skills/ios-simulator-run/SKILL.md) skill to build and launch first.

## Workflow

### 1. Get the booted simulator UUID

```bash
SIM_UUID=$(xcrun simctl list devices booted -j | python3 -c "import sys,json; devs=json.load(sys.stdin)['devices']; print([d['udid'] for ds in devs.values() for d in ds if d['state']=='Booted'][0])")
echo $SIM_UUID
```

If no simulator is booted:

```bash
xcrun simctl boot <DEVICE_UUID>
open -a Simulator
```

### 2. Capture a screenshot

```bash
xcrun simctl io $SIM_UUID screenshot output.png
```

The file is written directly to the local filesystem. PNG format by default.

### 3. Navigate between screens

iOS Simulator navigation is more limited than ADB. The recommended approach is a combination of techniques:

**Accessibility inspector (find element coordinates):**

```bash
# Use Accessibility Inspector.app (ships with Xcode) for element discovery
open -a "Accessibility Inspector"
```

Point the inspector at the Simulator window to find element positions and accessibility identifiers.

**Keyboard shortcuts in Simulator:**

- `Cmd+Shift+H` — Home button
- `Cmd+Left/Right` — Rotate device

**simctl status bar (clean screenshots):**

```bash
xcrun simctl status_bar $SIM_UUID override \
  --time "9:41" \
  --batteryState charged \
  --batteryLevel 100 \
  --cellularMode active \
  --cellularBars 4
```

Reset after capturing:

```bash
xcrun simctl status_bar $SIM_UUID clear
```

### 4. App navigation reference

The iOS app uses a **tab bar** with four tabs (Play, Create, Learn, Reference). Each tab opens a menu-style NavigationStack with rows for each screen. Settings is accessible via a gear icon in each tab's toolbar.

| Tab | Screens |
|-----|---------|
| Play | Explorer, Tuner, Pitch Monitor, Metronome, Chord Library, Favorites |
| Create | Songbook, Melody Notepad, Strumming Patterns, Chord Progressions |
| Learn | Progress, Daily Challenge, Practice Routine, Achievements, Play Along, Chord Transitions, Theory Lessons, Theory Quiz, Interval Trainer, Note Quiz, Chord Ear Training, Scale Practice |
| Reference | Capo Guide, Circle of Fifths, Chord Substitutions, Scale Chords, Fretboard Note Map, Glossary |

To navigate: tap a tab at the bottom, then tap a row in the menu list.

### 5. Record screen video

```bash
# Start recording (sends SIGINT to stop)
xcrun simctl io $SIM_UUID recordVideo --codec=h264 output.mp4 &
PID=$!

# ... perform interactions ...
sleep 10

# Stop recording
kill -INT $PID
wait $PID 2>/dev/null
```

## Batch Capture Pattern

For capturing multiple screens (e.g., for documentation or a manual):

1. Build and install the app on the simulator.
2. Launch the app and dismiss any onboarding.
3. Set a clean status bar.
4. Navigate to each screen and capture.

```bash
SIM_UUID=$(xcrun simctl list devices booted -j | python3 -c "import sys,json; devs=json.load(sys.stdin)['devices']; print([d['udid'] for ds in devs.values() for d in ds if d['state']=='Booted'][0])")
BUNDLE_ID=com.baijum.ukufretboard.ios
OUT_DIR=docs/manual/ios/screenshots

# Ensure app is installed and running
xcrun simctl launch $SIM_UUID $BUNDLE_ID
sleep 3

# Set clean status bar
xcrun simctl status_bar $SIM_UUID override \
  --time "9:41" --batteryState charged --batteryLevel 100 \
  --cellularMode active --cellularBars 4

# Navigate to each screen manually (tab bar + row taps) and capture:
# After navigating to Explorer:
xcrun simctl io $SIM_UUID screenshot $OUT_DIR/explorer-fretboard.png
# After navigating to Tuner:
xcrun simctl io $SIM_UUID screenshot $OUT_DIR/tuner.png
# ... continue for each screen ...

# Reset status bar
xcrun simctl status_bar $SIM_UUID clear
```

### Naming convention

Use `<section>-<description>.png`:
- `explorer-fretboard.png`
- `settings-panel.png`
- `chords-library.png`

## Quick Reference

| Task | Command |
|------|---------|
| Screenshot | `xcrun simctl io <UUID> screenshot file.png` |
| Record video | `xcrun simctl io <UUID> recordVideo file.mp4` |
| List booted devices | `xcrun simctl list devices booted` |
| Boot simulator | `xcrun simctl boot <UUID>` |
| Shutdown simulator | `xcrun simctl shutdown <UUID>` |
| Install app | `xcrun simctl install <UUID> <path.app>` |
| Launch app | `xcrun simctl launch <UUID> <bundle-id>` |
| Terminate app | `xcrun simctl terminate <UUID> <bundle-id>` |
| Override status bar | `xcrun simctl status_bar <UUID> override --time "9:41"` |
| Clear status bar | `xcrun simctl status_bar <UUID> clear` |
| Open Simulator GUI | `open -a Simulator` |
| Open Accessibility Inspector | `open -a "Accessibility Inspector"` |

## App Store Screenshots

Simulator screenshots are typically larger than App Store required dimensions and need resizing via `sips`.

### Required dimensions

| Display | Portrait | Landscape |
|---------|----------|-----------|
| iPhone 6.7" (Pro Max) | 1290 × 2796 | 2796 × 1290 |
| iPhone 6.5" | 1284 × 2778 or 1242 × 2688 | 2778 × 1284 or 2688 × 1242 |
| iPad 13" | 2048 × 2732 or 2064 × 2752 | 2732 × 2048 or 2752 × 2064 |

### Recommended simulators

| Display | Simulator |
|---------|-----------|
| iPhone 6.7" / 6.5" | iPhone 16 Pro Max |
| iPad 13" | iPad Pro 13-inch (M4 or M5) |

### Resize workflow

```bash
# Check captured dimensions
sips -g pixelWidth -g pixelHeight screenshot.png

# Resize to iPhone 6.5" (1284x2778)
sips -z 2778 1284 screenshot.png

# Resize to iPhone 6.7" (1290x2796)
sips -z 2796 1290 screenshot.png

# Resize to iPad 13" (2048x2732)
sips -z 2732 2048 screenshot.png
```

Note: `sips -z` takes height first, then width.

### Batch resize

```bash
mkdir -p appstore/6.5 appstore/6.7 appstore/ipad-13
for f in screenshots/*.png; do
  name=$(basename "$f")
  cp "$f" "appstore/6.5/$name"  && sips -z 2778 1284 "appstore/6.5/$name" >/dev/null
  cp "$f" "appstore/6.7/$name"  && sips -z 2796 1290 "appstore/6.7/$name" >/dev/null
done
# iPad screenshots (captured from iPad simulator) go to ipad-13
for f in ipad-screenshots/*.png; do
  name=$(basename "$f")
  cp "$f" "appstore/ipad-13/$name" && sips -z 2732 2048 "appstore/ipad-13/$name" >/dev/null
done
```

iPad screenshots captured from iPad Pro 13-inch simulators are typically 2064x2752, which is already an accepted App Store dimension and may not need resizing.

## Troubleshooting

- **"No devices are booted"**: No simulator running. Boot one first: `xcrun simctl boot <UUID> && open -a Simulator`.
- **Empty/white screenshot**: The app may not have rendered yet. Add `sleep 2` before capturing.
- **Wrong simulator**: Multiple simulators may be booted. Use `xcrun simctl list devices booted` to find the correct UUID.
- **App not installed**: Build and install first. See [ios-simulator-run](~/.cursor/skills/ios-simulator-run/SKILL.md).
- **Status bar shows random time/battery**: Use `xcrun simctl status_bar ... override` before capturing for clean screenshots.
- **Onboarding blocks UI**: Fresh installs show onboarding. Dismiss it before capturing, or erase the simulator to get a fresh state: `xcrun simctl erase <UUID>`.
- **Navigation is manual**: Unlike ADB, simctl has no UI element discovery or tap commands. Navigate screens manually in the Simulator GUI, then capture programmatically via simctl.

---
name: record-clips
description: Record scene video clips for a TOML video project. Reads the android.toml or ios.toml to get the scene list and min_clip_duration, navigates to each screen via ADB (Android) or simctl (iOS), and records individual clips. Use when the user says /record-clips and provides a TOML path.
---

# Record Clips for Video Project

Record per-scene video clips for a TOML video project. Each clip is a short screen recording of one scene's interactions on the Android emulator or iOS Simulator.

## Prerequisites

### Android
- ADB at `~/Library/Android/sdk/platform-tools/adb`
- A running emulator (verify with `adb devices`)
- App installed on emulator (run `./gradlew installDebug` if needed)
- Onboarding already dismissed

### iOS
- Xcode with `xcrun simctl` available
- A booted iOS Simulator (verify with `xcrun simctl list devices booted`)
- App installed on simulator
- Onboarding already dismissed

## Recommended Workflow (Audio-First)

Before recording clips, generate the voiceover audio first so you know exactly how long each clip needs to be:

```bash
source ~/.secrets
python3 scripts/assemble_video.py <android.toml or ios.toml> --audio-only
```

This prints a table showing the required `min_clip_duration` for each scene. Record clips at least that long.

## Recording Process (Android)

### Step 1: Read the project

Parse the `android.toml` to get the scene list. For each scene, note:
- `name` -- what the scene shows
- `video` -- where to save the clip (under `clips/android/`)
- `min_clip_duration` -- minimum recording length (or use 30s default)
- `recording_notes` -- **step-by-step instructions** for what to do on screen during recording. Follow these precisely to avoid navigating away from the target screen or triggering unwanted intents. If `recording_notes` is absent, infer interactions from the `narration` text.

### Step 2: Prepare the app

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB shell am force-stop com.baijum.ukufretboard
sleep 1
$ADB shell am start -n com.baijum.ukufretboard/.MainActivity
sleep 3
```

### Step 3: Record each scene

For each scene:

1. Read the scene's `recording_notes` field for explicit interaction instructions
2. Navigate to the correct screen state (use drawer + uiautomator)
3. Set up the initial state described in `recording_notes`
4. Start recording with the scene's `min_clip_duration` as the time limit
5. Perform the interactions listed in `recording_notes` (respect any "Do NOT" warnings)
6. Stop recording and pull the file

```bash
# Navigate to screen (use uiautomator to find coordinates)
$ADB shell input tap 80 200    # hamburger menu
sleep 1
$ADB shell uiautomator dump /sdcard/ui.xml 2>/dev/null
$ADB shell cat /sdcard/ui.xml | tr '>' '\n' | grep 'text="<ScreenName>"'
$ADB shell input tap <x> <y>
sleep 2

# Record clip
$ADB shell screenrecord --time-limit <min_clip_duration> --size 1080x2424 /sdcard/scene.mp4 &
PID=$!
sleep 2

# Perform interactions for this scene
# ...
sleep <remaining_time>

# Stop and pull
kill $PID 2>/dev/null
sleep 3
$ADB pull /sdcard/scene.mp4 <project_dir>/clips/android/<scene_file>.mp4
$ADB shell rm /sdcard/scene.mp4
```

### Step 4: Validate all clips

After recording all clips, check that every one has a valid duration:

```bash
for clip in <project_dir>/clips/android/*.mp4; do
  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$clip")
  echo "$(basename $clip): ${dur}s"
done
```

Any clip showing `N/A` is corrupt and must be re-recorded. Common causes:
- Recording was too short (under ~3s) -- the moov atom wasn't written
- Static screens with no pixel changes -- add a touch interaction to generate frames

## Recording Process (iOS)

### Step 1: Read the project

Parse the `ios.toml` to get the scene list. For each scene, note:
- `name` -- what the scene shows
- `video` -- where to save the clip (under `clips/ios/`)
- `min_clip_duration` -- minimum recording length (or use 30s default)
- `recording_notes` -- **step-by-step instructions** for iOS-specific interactions

### Step 2: Prepare the app

```bash
SIM_UUID=$(xcrun simctl list devices booted -j | python3 -c "import sys,json; devs=json.load(sys.stdin)['devices']; print([d['udid'] for ds in devs.values() for d in ds if d['state']=='Booted'][0])")
xcrun simctl terminate $SIM_UUID com.baijum.ukufretboard.ios
sleep 1
xcrun simctl launch $SIM_UUID com.baijum.ukufretboard.ios
sleep 3
```

### Step 3: Record each scene

```bash
# Start recording
xcrun simctl io $SIM_UUID recordVideo --codec=h264 /tmp/scene.mp4 &
PID=$!
sleep 2

# Perform interactions (navigate manually or via accessibility automation)
# ...
sleep <remaining_time>

# Stop and move
kill -INT $PID 2>/dev/null
sleep 3
mv /tmp/scene.mp4 <project_dir>/clips/ios/<scene_file>.mp4
```

### Step 4: Validate all clips

```bash
for clip in <project_dir>/clips/ios/*.mp4; do
  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$clip")
  echo "$(basename $clip): ${dur}s"
done
```

## Navigation Reference (Android)

Use `uiautomator dump` for precise coordinates. See [android-screenshot-capture](~/.cursor/skills/android-screenshot-capture/SKILL.md) for details.

**Drawer items**: Open drawer with `input tap 80 200`, then find items by text.

**Collapsible sections**: If a drawer section is collapsed, expand it first:
```bash
$ADB shell cat /sdcard/ui.xml | tr '>' '\n' | grep 'content-desc="Expand'
```

**Below the fold**: Scroll down in the drawer for Reference section items:
```bash
$ADB shell input swipe 400 1800 400 400 500
```

**Settings**: Tap the gear icon (top-right, not in drawer):
```bash
$ADB shell input tap 1007 200
```

## Tips

- **Follow `recording_notes` precisely** -- These instructions prevent common errors like accidentally navigating away from the screen, tapping share buttons that launch intent pickers, or ending up on the home screen.
- **Always record longer than needed** -- the assembler pads clips with `tpad` if they're short, but clips that are too short (< 3s) may be corrupt.
- **Add interaction to static screens** -- Screens like the Tuner or Pitch Monitor at rest produce very few frames. Touch the screen or scroll slightly to ensure the recording is valid.
- **Use `--time-limit` generously (Android)** -- Set it to `min_clip_duration + 5` for safety. You can always `kill` the recording early if the interactions finish sooner.
- **Re-record individual scenes** -- One of the key benefits of the TOML project system. Just re-record the one clip and re-run `python3 scripts/assemble_video.py <toml-file>`.
- **Respect "Do NOT" warnings** -- Some scenes warn against tapping specific buttons (e.g., share buttons that launch Android intent pickers). Ignoring these will navigate away from the app.

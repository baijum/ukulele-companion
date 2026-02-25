---
name: record-clips
description: Record scene video clips for a TOML video project. Reads the project.toml to get the scene list and min_clip_duration, navigates to each screen via ADB, and records individual clips. Use when the user says /record-clips and provides a project.toml path.
---

# Record Clips for Video Project

Record per-scene video clips for a TOML video project. Each clip is a short screen recording of one scene's interactions on the Android emulator.

## Prerequisites

- ADB at `~/Library/Android/sdk/platform-tools/adb`
- A running emulator (verify with `adb devices`)
- App installed on emulator (run `./gradlew installDebug` if needed)
- Onboarding already dismissed

## Recommended Workflow (Audio-First)

Before recording clips, generate the voiceover audio first so you know exactly how long each clip needs to be:

```bash
source ~/.secrets
python3 scripts/assemble_video.py <project.toml> --audio-only
```

This prints a table showing the required `min_clip_duration` for each scene. Record clips at least that long.

## Recording Process

### Step 1: Read the project

Parse the `project.toml` to get the scene list. For each scene, note:
- `name` -- what the scene shows
- `video` -- where to save the clip
- `min_clip_duration` -- minimum recording length (or use 30s default)

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

1. Navigate to the correct screen state (use drawer + uiautomator)
2. Set up the initial state for the scene
3. Start recording with the scene's `min_clip_duration` as the time limit
4. Perform the scene's demo interactions
5. Stop recording and pull the file

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
$ADB pull /sdcard/scene.mp4 <project_dir>/clips/<scene_file>.mp4
$ADB shell rm /sdcard/scene.mp4
```

### Step 4: Validate all clips

After recording all clips, check that every one has a valid duration:

```bash
for clip in <project_dir>/clips/*.mp4; do
  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$clip")
  echo "$(basename $clip): ${dur}s"
done
```

Any clip showing `N/A` is corrupt and must be re-recorded. Common causes:
- Recording was too short (under ~3s) -- the moov atom wasn't written
- Static screens with no pixel changes -- add a touch interaction to generate frames

## Navigation Reference

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

- **Always record longer than needed** -- the assembler pads clips with `tpad` if they're short, but clips that are too short (< 3s) may be corrupt.
- **Add interaction to static screens** -- Screens like the Tuner or Pitch Monitor at rest produce very few frames. Touch the screen or scroll slightly to ensure the recording is valid.
- **Use `--time-limit` generously** -- Set it to `min_clip_duration + 5` for safety. You can always `kill` the recording early if the interactions finish sooner.
- **Re-record individual scenes** -- One of the key benefits of the TOML project system. Just re-record the one clip and re-run `python3 scripts/assemble_video.py project.toml`.

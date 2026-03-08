---
name: assemble-video
description: Assemble a narrated video from a TOML project file using the committed assembler script. Use when the user says /assemble-video and provides an android.toml or ios.toml path, or wants to generate audio for the audio-first workflow.
---

# Assemble Video from TOML Project

Runs `scripts/assemble_video.py` to assemble per-scene video clips with AI-generated voiceover narration and branding jingles into a final MP4.

## Prerequisites

- `ffmpeg` on PATH
- Python 3.11+ (for `tomllib`)
- `OPENAI_API_KEY` in `~/.secrets`
- `pip install openai`
- Pre-recorded video clips (use `/record-clips` to create them)

## Workflow

### Full assembly (clips already recorded)

```bash
source ~/.secrets
python3 scripts/assemble_video.py docs/videos/<feature>/android.toml
```

Replace `android.toml` with `ios.toml` for iOS videos.

The script:
1. Generates missing TTS audio from narration text
2. Pads each clip with `tpad` to match narration duration
3. Concatenates scenes using the ffmpeg `concat` filter
4. Adds branding jingles (intro/outro)
5. Reports output path, duration, and size

### Audio-first workflow (recommended)

Generate audio first to know exact clip durations before recording:

```bash
# Step 1: Generate audio, print required clip durations
source ~/.secrets
python3 scripts/assemble_video.py docs/videos/<feature>/android.toml --audio-only

# Step 2: Record clips using the printed durations
# Use /record-clips skill

# Step 3: Assemble final video
python3 scripts/assemble_video.py docs/videos/<feature>/android.toml
```

## TOML Project Format

Each feature has per-platform TOML files in `docs/videos/<feature>/`:
- `android.toml` -- Android-specific scenes, recording notes, and resolution
- `ios.toml` -- iOS-specific scenes, recording notes, and resolution

Audio (`audio/` directory) is shared across platforms since narration is platform-agnostic.

```toml
[project]
title = "Feature Name - Ukulele Companion"
output = "../../feature-videos/android/feature-name.mp4"
resolution = "1080x2424"

[branding]
intro = "../../jingle-intro.wav"
outro = "../../jingle-outro.wav"

[voiceover]
model = "tts-1-hd"
voice = "nova"
volume_boost = 3.0

[[scene]]
name = "scene-name"
video = "clips/android/01-scene.mp4"
audio = "audio/01-scene.mp3"
narration = """Narration text."""
delay = 1.0
min_clip_duration = 25
```

See existing projects at `docs/videos/explorer/android.toml` and `docs/videos/tuner/android.toml` for full examples.

## Creating a New Video Project

1. Create the directory: `mkdir -p docs/videos/<feature>/clips/android docs/videos/<feature>/clips/ios docs/videos/<feature>/audio`
2. Write `android.toml` (and/or `ios.toml`) with scenes and narration
3. Run `--audio-only` to generate audio and see required clip durations
4. Use `/record-clips` to record scene clips
5. Run the assembler to produce the final video

## Related Skills

- `/record-clips` -- Record scene clips for a TOML project
- [android-screenshot-capture](~/.cursor/skills/android-screenshot-capture/SKILL.md) -- ADB navigation and uiautomator reference

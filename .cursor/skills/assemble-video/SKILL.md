---
name: assemble-video
description: Assemble a narrated video from a TOML project file using the committed assembler script. Use when the user says /assemble-video and provides a project.toml path, or wants to generate audio for the audio-first workflow.
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
python3 scripts/assemble_video.py <project.toml>
```

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
python3 scripts/assemble_video.py <project.toml> --audio-only

# Step 2: Record clips using the printed durations
# Use /record-clips skill

# Step 3: Assemble final video
python3 scripts/assemble_video.py <project.toml>
```

## TOML Project Format

Projects live in `docs/videos/<feature>/project.toml`:

```toml
[project]
title = "Feature Name - Ukulele Companion"
output = "../../feature-videos/feature-name.mp4"

[branding]
intro = "../../jingle-intro.wav"
outro = "../../jingle-outro.wav"

[voiceover]
model = "tts-1-hd"
voice = "nova"
volume_boost = 3.0

[[scene]]
name = "scene-name"
video = "clips/01-scene.mp4"
audio = "audio/01-scene.mp3"
narration = """Narration text."""
delay = 1.0
min_clip_duration = 25
```

See existing projects at `docs/videos/explorer/project.toml` and `docs/videos/tuner/project.toml` for full examples.

## Creating a New Video Project

1. Create the directory: `mkdir -p docs/videos/<feature>/clips docs/videos/<feature>/audio`
2. Write `project.toml` with scenes and narration
3. Run `--audio-only` to generate audio and see required clip durations
4. Use `/record-clips` to record scene clips
5. Run the assembler to produce the final video

## Related Skills

- `/record-clips` -- Record scene clips for a TOML project
- [android-screenshot-capture](~/.cursor/skills/android-screenshot-capture/SKILL.md) -- ADB navigation and uiautomator reference

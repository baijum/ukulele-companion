# Video Projects

This directory contains TOML project files that define narrated feature videos for Ukulele Companion. Each subdirectory represents one feature and contains a `project.toml` with scene definitions, narration scripts, and assembly settings.

## Projects

| Feature | Directory | Scenes | Status |
|---------|-----------|--------|--------|
| Explorer | [explorer/](explorer/) | 6 | Ready |
| Tuner | [tuner/](tuner/) | 5 | Ready |
| Pitch Monitor | [pitch-monitor/](pitch-monitor/) | 4 | Ready |
| Metronome | [metronome/](metronome/) | 5 | Ready |
| Chord Library | [chords/](chords/) | 5 | Ready |
| Favorites | [favorites/](favorites/) | 4 | Ready |
| Songbook | [songs/](songs/) | 5 | Ready |
| Melody Notepad | [melody-notepad/](melody-notepad/) | 5 | TOML ready |
| Strumming Patterns | [patterns/](patterns/) | 5 | TOML ready |
| Chord Progressions | [progressions/](progressions/) | 5 | TOML ready |
| Play Store Promo | [play-store-promo/](play-store-promo/) | 9 | TOML ready |

**Ready** = clips recorded, video assembled. **TOML ready** = narration written and audio generated, clips not yet recorded.

## Directory Structure

```
docs/videos/
├── README.md
├── <feature>/
│   ├── project.toml      # Scene definitions, narration, settings (committed)
│   ├── clips/             # Recorded screen captures (gitignored)
│   │   ├── 01-scene.mp4
│   │   └── ...
│   └── audio/             # Generated TTS voiceover (gitignored)
│       ├── 01-scene.mp3
│       └── ...
docs/
├── feature-videos/        # Final assembled videos (gitignored)
│   ├── explorer.mp4
│   └── ...
├── jingle-intro.wav       # Branding jingles (committed)
└── jingle-outro.wav
```

Only `project.toml` files are committed to git. Clips, audio, and final videos are gitignored since they can be regenerated from the TOML definitions.

## How to Produce a Video

### Prerequisites

- Android emulator running with the app installed
- `ffmpeg` on PATH
- Python 3.11+ with `openai` package (`pip install openai`)
- `OPENAI_API_KEY` set (via `source ~/.secrets`)

### Audio-First Workflow (Recommended)

```bash
# 1. Generate voiceover audio and see required clip durations
source ~/.secrets
python3 scripts/assemble_video.py docs/videos/<feature>/project.toml --audio-only

# 2. Record clips on the emulator (use /record-clips skill in Cursor)

# 3. Assemble the final video
python3 scripts/assemble_video.py docs/videos/<feature>/project.toml
```

### Cursor Skills

| Skill | Purpose |
|-------|---------|
| `/assemble-video` | Run the assembler script on a project |
| `/record-clips` | Record scene clips on the emulator |

## TOML Format Reference

```toml
[project]
title = "Feature Name - Ukulele Companion"
description = "Short description"
output = "../../feature-videos/feature-name.mp4"
resolution = "1080x2424"

[branding]
intro = "../../jingle-intro.wav"
outro = "../../jingle-outro.wav"

[voiceover]
provider = "openai"
model = "tts-1-hd"
voice = "nova"
volume_boost = 3.0

[[scene]]
name = "scene-name"
video = "clips/01-scene.mp4"
audio = "audio/01-scene.mp3"
delay = 1.0
min_clip_duration = 25
recording_notes = """
Step-by-step instructions for the recording agent.
1. Stay on this screen, do not navigate away
2. Tap element X at (~x,y) -- wait 3s
3. Do NOT tap the share button (launches intent picker)
"""
narration = """
Narration text for this scene. The assembler generates TTS
audio from this text if the audio file doesn't exist yet.
"""
```

### Fields

| Field | Description |
|-------|-------------|
| `project.output` | Path to the final assembled MP4 (relative to the TOML file) |
| `branding.intro/outro` | Paths to jingle WAV files prepended/appended to the video |
| `voiceover.volume_boost` | Multiplier applied to voiceover volume during assembly |
| `scene.delay` | Seconds of silence before narration starts in this scene |
| `scene.min_clip_duration` | Minimum recording length in seconds (calculated from audio duration + delay + 2s buffer) |
| `scene.recording_notes` | Step-by-step ADB interaction instructions for the recording agent. Describes what to tap, swipe, and what NOT to do. Ignored by the assembler script. |
| `scene.narration` | Text sent to OpenAI TTS to generate the voiceover audio |

## Creating a New Video Project

1. Create the directory: `mkdir -p docs/videos/<feature>`
2. Write a `project.toml` following the format above with 4-6 scenes
3. Run `--audio-only` to generate audio and calibrate `min_clip_duration` values
4. Update the TOML with the printed durations
5. Record clips using the `/record-clips` skill
6. Assemble with `python3 scripts/assemble_video.py <project.toml>`
7. Add the new project to the table in this README

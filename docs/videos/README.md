# Video Projects

This directory contains TOML project files that define narrated feature videos for Ukulele Companion. Each subdirectory represents one feature and contains per-platform TOML files (`android.toml`, `ios.toml`) with scene definitions, narration scripts, and assembly settings.

## Projects

| Feature | Directory | Scenes | Android | iOS |
|---------|-----------|--------|---------|-----|
| Explorer | [explorer/](explorer/) | 6 | Ready | -- |
| Tuner | [tuner/](tuner/) | 5 | Ready | -- |
| Pitch Monitor | [pitch-monitor/](pitch-monitor/) | 4 | Ready | -- |
| Metronome | [metronome/](metronome/) | 5 | Ready | -- |
| Chord Library | [chords/](chords/) | 5 | Ready | -- |
| Favorites | [favorites/](favorites/) | 4 | Ready | -- |
| Songbook | [songs/](songs/) | 5 | Ready | -- |
| Melody Notepad | [melody-notepad/](melody-notepad/) | 5 | TOML ready | -- |
| Strumming Patterns | [patterns/](patterns/) | 5 | TOML ready | -- |
| Chord Progressions | [progressions/](progressions/) | 5 | TOML ready | -- |
| Play Store Promo | [play-store-promo/](play-store-promo/) | 9 | TOML ready | -- |

**Ready** = clips recorded, video assembled. **TOML ready** = narration written and audio generated, clips not yet recorded. **--** = TOML not yet created.

## Shorts (YouTube Shorts / Instagram Reels)

Short-form promotional videos targeting 30–50 seconds each. Use true 9:16 resolution (`1080x1920`) — set the emulator to this resolution before recording. Assembled videos land in `docs/feature-videos/android/shorts/`.

| Short | Directory | Scenes | Hook | Android |
|-------|-----------|--------|------|---------|
| Chord Detector | [shorts/chord-detector/](shorts/chord-detector/) | 2 | "Tap the fretboard. Know your chord instantly." | TOML ready |
| Tuner Freeze | [shorts/tuner-freeze/](shorts/tuner-freeze/) | 2 | "The needle freezes so you never lose your note." | TOML ready |
| Progressions Practice | [shorts/progressions-practice/](shorts/progressions-practice/) | 2 | "Stop fumbling chord changes." | TOML ready |
| Scale Explorer | [shorts/scale-explorer/](shorts/scale-explorer/) | 2 | "37 scales, color-mapped across the neck." | TOML ready |
| Free and Offline | [shorts/free-and-offline/](shorts/free-and-offline/) | 1 | Value-prop closer — free / offline / accessible | TOML ready |

### Producing a Short

The workflow is identical to feature videos, but set the emulator resolution to `1080x1920` first:

```bash
# In Android Studio: AVD Manager → Edit AVD → Screen resolution → 1080 × 1920

# 1. Generate audio and check min_clip_duration values
source ~/.secrets
python3 scripts/assemble_video.py docs/videos/shorts/<name>/android.toml --audio-only

# 2. Record clips (use /record-clips skill in Cursor)

# 3. Assemble
python3 scripts/assemble_video.py docs/videos/shorts/<name>/android.toml
```

### Suggested production order

1. `chord-detector` — most visually impressive, strongest hook potential
2. `progressions-practice` — strong emotional arc, straightforward to record
3. `tuner-freeze` — the freeze moment is a satisfying scroll-stopper
4. `scale-explorer` — colorful and fast-paced
5. `free-and-offline` — calm single-scene; can re-use b-roll from other shorts

## Directory Structure

```
docs/videos/
├── README.md
├── <feature>/
│   ├── android.toml          # Android scenes, recording_notes, resolution (committed)
│   ├── ios.toml              # iOS scenes, recording_notes, resolution (committed)
│   ├── audio/                # Shared TTS voiceover (gitignored)
│   │   ├── 01-scene.mp3
│   │   └── ...
│   └── clips/
│       ├── android/          # Android screen recordings (gitignored)
│       │   ├── 01-scene.mp4
│       │   └── ...
│       └── ios/              # iOS screen recordings (gitignored)
│           ├── 01-scene.mp4
│           └── ...
docs/
├── feature-videos/
│   ├── android/              # Assembled Android videos (gitignored)
│   │   ├── explorer.mp4
│   │   └── ...
│   └── ios/                  # Assembled iOS videos (gitignored)
│       ├── explorer.mp4
│       └── ...
├── jingle-intro.wav          # Branding jingles (committed)
└── jingle-outro.wav
```

Only TOML files are committed to git. Clips, audio, and final videos are gitignored since they can be regenerated from the TOML definitions. Audio is shared across platforms because narration is platform-agnostic.

## How to Produce a Video

### Prerequisites

- **Android**: Android emulator running with the app installed, ADB on PATH
- **iOS**: iOS Simulator running with the app installed, `xcrun simctl` available
- `ffmpeg` on PATH
- Python 3.11+ with `openai` package (`pip install openai`)
- `OPENAI_API_KEY` set (via `source ~/.secrets`)

### Audio-First Workflow (Recommended)

```bash
# 1. Generate voiceover audio and see required clip durations
source ~/.secrets
python3 scripts/assemble_video.py docs/videos/<feature>/android.toml --audio-only

# 2. Record clips on the emulator/simulator (use /record-clips skill in Cursor)

# 3. Assemble the final video
python3 scripts/assemble_video.py docs/videos/<feature>/android.toml
```

Replace `android.toml` with `ios.toml` for iOS videos.

### Cursor Skills

| Skill | Purpose |
|-------|---------|
| `/assemble-video` | Run the assembler script on a project |
| `/record-clips` | Record scene clips on the Android emulator |

## TOML Format Reference

Each platform gets its own TOML file (`android.toml` or `ios.toml`) sharing the same scene structure. Platform-specific fields include `resolution`, `recording_notes`, and `video` paths. Audio paths and narration text are typically identical across platforms.

```toml
[project]
title = "Feature Name - Ukulele Companion"
description = "Short description"
output = "../../feature-videos/android/feature-name.mp4"
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
video = "clips/android/01-scene.mp4"
audio = "audio/01-scene.mp3"
delay = 1.0
min_clip_duration = 25
recording_notes = """
Platform-specific interaction instructions for the recording agent.
Android: ADB taps, uiautomator. iOS: xcrun simctl, accessibility IDs.
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
| `project.resolution` | Recording resolution (platform-specific) |
| `branding.intro/outro` | Paths to jingle WAV files prepended/appended to the video |
| `voiceover.volume_boost` | Multiplier applied to voiceover volume during assembly |
| `scene.video` | Path to the video clip under `clips/android/` or `clips/ios/` |
| `scene.audio` | Path to the TTS audio under `audio/` (shared across platforms) |
| `scene.delay` | Seconds of silence before narration starts in this scene |
| `scene.min_clip_duration` | Minimum recording length in seconds (calculated from audio duration + delay + 2s buffer) |
| `scene.recording_notes` | Platform-specific interaction instructions for the recording agent. Ignored by the assembler script. |
| `scene.narration` | Text sent to OpenAI TTS to generate the voiceover audio |

## Creating a New Video Project

1. Create the directory: `mkdir -p docs/videos/<feature>`
2. Write `android.toml` (and/or `ios.toml`) following the format above with 4-6 scenes
3. Run `--audio-only` to generate audio and calibrate `min_clip_duration` values
4. Update the TOML with the printed durations
5. Record clips using the `/record-clips` skill
6. Assemble with `python3 scripts/assemble_video.py <toml-file>`
7. Add the new project to the table in this README

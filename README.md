<p align="center">
  <img src="docs/app-icon-512.png" alt="Ukulele Companion" width="128" height="128">
</p>

<h1 align="center">🎵 Ukulele Companion</h1>

<p align="center">
  <b>A free, offline, ad-free Android app for learning ukulele</b><br>
  Chords, scales, music theory, composition tools, and more.<br>
  Built with <b>Kotlin</b> and <b>Jetpack Compose</b>.
</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0-blue.svg?logo=kotlin" alt="Kotlin"></a>
  <a href="https://developer.android.com/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-green.svg?logo=jetpackcompose" alt="Jetpack Compose"></a>
  <a href="https://developer.android.com/about/versions/oreo"><img src="https://img.shields.io/badge/Min%20SDK-26-orange.svg" alt="Min SDK"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"></a>
  <a href="https://github.com/baijum/ukulele-companion/actions/workflows/android.yml"><img src="https://github.com/baijum/ukulele-companion/actions/workflows/android.yml/badge.svg" alt="CI"></a>
</p>

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🎸 Interactive Fretboard Explorer
Tap fret positions on a visual ukulele fretboard (standard GCEA tuning, frets 0–12) and the app instantly detects and displays the chord. Supports 9 chord types: Major, Minor, Dom7, Min7, Maj7, Diminished, Augmented, Sus2, and Sus4.

### 📚 Chord Library
Browse playable voicings for any chord. Select a root note, category (Triad, Seventh, Suspended, Extended), and chord type to see algorithmically generated voicings displayed as mini fretboard diagrams.

### 🔄 Transpose
Shift chords up or down by semitones with +/- buttons. Shows the capo equivalent for easy reference.

### 🧠 Neural-Powered Tuner
The tuner now uses a hybrid pipeline: fast YIN pitch tracking on every frame, supervised by SwiftF0 neural inference at intervals. This improves robustness against octave mistakes and unstable frames while preserving responsive needle movement.

### 🥁 Strumming Patterns
A reference guide with 8 common ukulele strumming patterns — from beginner (All Downs, Island Strum) to intermediate (Calypso, Ska). Each pattern includes visual beat arrows, notation, description, and suggested tempo.

### 🎶 Chord Progressions
Common chord progressions for any key, in both major and minor scales. Includes Pop, Classic Rock, 50s, Folk, Jazz ii-V-I, Reggae, and more. Tap any chord chip to jump to its voicings.

</td>
<td width="50%">

### 🎼 Scale Overlay
Highlight notes from any of 7 scales (Major, Minor, Pentatonic Major/Minor, Blues, Dorian, Mixolydian) directly on the fretboard. Root notes shown with a distinct color.

### ⭐ Favorites
Long-press any voicing in the Chord Library to save it. Access your saved voicings from the dedicated Favorites tab.

### 📝 Song Chord Sheets
Create a personal songbook with lyrics and inline chord markers (e.g., `Some[C]where over the [Em]rainbow`). Tap any chord name to view its voicings.


### 🔊 Sound Playback
Hear chords played back using sine wave synthesis. Notes are strummed with a configurable delay between strings.

### 🎓 Music Theory & Learning
Theory lessons, ear training, interval trainer, circle of fifths, glossary, scale practice, achievements, and more.

</td>
</tr>
</table>

## 📱 Screenshots

<p align="center">
  <img src="docs/screenshots/chord-detected.png" width="30%" alt="Chord Detection">
  <img src="docs/screenshots/chord-library.png" width="30%" alt="Chord Library">
  <img src="docs/screenshots/scale-overlay.png" width="30%" alt="Scale Overlay">
</p>
<p align="center">
  <img src="docs/screenshots/strumming-patterns.png" width="30%" alt="Strumming Patterns">
  <img src="docs/screenshots/chord-progressions.png" width="30%" alt="Chord Progressions">
  <img src="docs/screenshots/circle-of-fifths.png" width="30%" alt="Circle of Fifths">
</p>
<p align="center">
  <a href="https://youtube.com/shorts/Vu0_naGO2wA">
    <img src="https://img.youtube.com/vi/Vu0_naGO2wA/0.jpg" width="30%" alt="Watch demo video">
  </a>
  <br>
  <sub>Tap to watch the demo video</sub>
</p>

### ⚙️ Settings

- **Display**: Sharp/Flat note names, Light/Dark/System/High Contrast theme
- **Tuning**: High-G (standard), Low-G, Baritone, D-Tuning
- **Fretboard**: Left-handed mode (mirrors the fretboard)
- **Sound**: Enable/disable, strum delay, note duration

### ♿ Accessibility

Ukulele Companion is designed to be usable by everyone, including blind and visually impaired musicians:

- **TalkBack support**: All interactive elements have descriptive content descriptions for Android's screen reader
- **Heading semantics**: Screen titles and section headers are marked as headings for efficient screen reader navigation
- **Live regions**: Dynamic content like tuner readings, chord detection, and pitch monitoring are announced by screen readers as they change
- **Canvas alternatives**: Visual-only components (tuner meter, chord diagrams, fretboard, pitch monitor, Circle of Fifths) have text descriptions for screen readers
- **High contrast theme**: A high-contrast color scheme is available in Display settings
- **Logical focus order**: Navigation follows a logical order for keyboard and switch access users

---

## 💡 Philosophy

| Principle | What it means |
|-----------|---------------|
| **Free forever** | No ads, no in-app purchases — ever |
| **Fully offline** | No internet required, no analytics, no tracking |
| **No login** | Just open and play |
| **Educational** | Designed for beginners and learners |

---

## 🏗️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | ViewModel + StateFlow |
| Audio | Android AudioTrack (sine wave synthesis) |

| Persistence | SharedPreferences |
| Serialization | Kotlinx Serialization |
| Neural Inference | ONNX Runtime (SwiftF0 supervisor) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

---

## 📁 Project Structure

```
com.baijum.ukufretboard
├── audio/              # Sine wave tone generation, metronome, audio capture
│   ├── AudioCaptureEngine.kt
│   ├── MetronomeEngine.kt
│   └── ToneGenerator.kt
├── data/               # Notes, chords, scales, progressions, patterns, persistence
│   ├── ChordFormulas, Notes, Scales, Progressions, StrumPatterns
│   ├── Repositories (Favorites, LearningProgress, Achievements)
│   ├── ChordPro parser/exporter, VoicingGenerator
│   └── sync/           # Backup & restore
├── domain/             # Core business logic
│   ├── ChordDetector, AudioChordDetector, PitchDetector
│   ├── Transpose, CapoCalculator, KeyDetector
│   ├── ScalePracticeGenerator
│   └── VoiceLeading, AchievementChecker
├── ui/                 # Compose screens and components (30+ screens)
│   ├── FretboardScreen (main navigation)
│   ├── Tabs: ChordLibrary, Favorites, Progressions, Songbook, Tuner
│   ├── Views: CircleOfFifths, TheoryQuiz, EarTraining, PlayAlong
│   └── theme/          # Material 3 theming
├── viewmodel/          # UI state management (11 ViewModels)
│   ├── FretboardViewModel, ChordLibraryViewModel
│   ├── SettingsViewModel, SongbookViewModel
│   └── TunerViewModel, PitchMonitorViewModel
```

> **132 Kotlin source files** across 6 packages — a well-organized, single-module Android app.

---

## 🚀 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable — Ladybug or newer)
- JDK 11+

### Clone & Build

```bash
git clone https://github.com/baijum/ukulele-companion.git
cd ukulele-companion
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Run on Emulator or Device

Open the project in Android Studio, select a device/emulator, and click **Run** (or press `Shift+F10`).

### Release Build

1. Create a `keystore.properties` file in the project root:

```properties
storeFile=path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

2. Build the release bundle:

```bash
./gradlew bundleRelease
```

The AAB will be at `app/build/outputs/bundle/release/app-release.aab`.

---

## 🤝 Contributing

**Contributions are welcome!** Whether you're fixing a bug, adding a feature, improving documentation, or refactoring code — we'd love your help.

Please read the [Contributing Guide](CONTRIBUTING.md) for detailed instructions on development setup, code guidelines, and the PR process. All participants are expected to follow our [Code of Conduct](CODE_OF_CONDUCT.md).

### How to Contribute

1. **Fork** the repository
2. **Create a branch** for your feature or fix (`git checkout -b feature/my-feature`)
3. **Make your changes** and test them
4. **Commit** with a clear message (`git commit -m "Add: description of change"`)
5. **Push** to your fork (`git push origin feature/my-feature`)
6. **Open a Pull Request** describing what you changed and why

### 🤖 AI-Assisted Contributing

We actively encourage contributors to use **AI coding tools** to accelerate their work on this project. The codebase is well-structured and AI-friendly:

- **Use [Cursor](https://cursor.com), [GitHub Copilot](https://github.com/features/copilot), or similar AI tools** to explore the codebase, understand patterns, and generate code that fits the existing architecture.
- **Leverage AI for code reviews** — before submitting a PR, ask an AI assistant to review your changes for consistency with the project's patterns.
- **Use AI to write tests** — the project currently has no automated tests (see [Good First Issues](#-good-first-issues) below), making it a perfect opportunity for AI-assisted test generation.
- **AI-powered documentation** — use AI tools to help write clear commit messages, PR descriptions, and inline documentation.

> **Tip:** This project uses standard Kotlin + Jetpack Compose patterns. AI tools work exceptionally well with the codebase because it follows consistent conventions throughout.

### 🟢 Good First Issues

Looking for a place to start? Here are some areas where contributions would be especially valuable:

| Area | Description | Difficulty |
|------|-------------|------------|
| **Unit Tests** | Add tests for `ChordDetector`, `Transpose`, `CapoCalculator`, and other domain logic | Beginner |
| **UI Tests** | Add Compose UI tests for screens and components | Beginner |
| **Accessibility** | Maintain and improve TalkBack support, content descriptions, live regions | Beginner |
| **New Scales** | Add more scale types to the Scale Overlay feature | Beginner |
| **New Strumming Patterns** | Expand the strumming pattern library | Beginner |
| **New Chord Progressions** | Add genre-specific chord progressions | Beginner |
| **Localization** | Translate the app into other languages | Intermediate |
| **CI/CD** | Set up GitHub Actions for build verification | Intermediate |
| **Alternate Tunings** | Add support for more ukulele tuning variants | Intermediate |
| **Instrument Samples** | Replace sine wave synthesis with real ukulele samples | Advanced |

### Code Style

- **Kotlin** with official code style
- **Jetpack Compose** for all UI — no XML layouts
- **ViewModel + StateFlow** for state management
- **SharedPreferences** for persistence (via repository pattern)
- Follow existing patterns in the codebase — consistency is valued

### Architecture at a Glance

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│     UI      │ ──▶ │  ViewModel   │ ──▶ │   Domain    │
│  (Compose)  │ ◀── │ (StateFlow)  │ ◀── │   (Logic)   │
└─────────────┘     └──────────────┘     └─────────────┘
                           │                     │
                           ▼                     ▼
                    ┌──────────────┐     ┌─────────────┐
                    │     Data     │     │    Audio     │
                    │ (Repository) │     │  (Playback)  │
                    └──────────────┘     └─────────────┘
```

- **UI layer**: 44 Compose files, single-activity architecture via `MainActivity`
- **ViewModel layer**: 11 ViewModels managing state with `StateFlow`
- **Domain layer**: Pure Kotlin logic for chord detection, transposition, scales
- **Data layer**: Repositories wrapping SharedPreferences, chord formulas, scale data
- **Audio layer**: Tone generation, metronome, microphone-based pitch detection

---

## 📖 Documentation

- **Website**: https://baijum.github.io/ukulele-companion/
- **Privacy policy**: https://baijum.github.io/ukulele-companion/privacy-policy/

Detailed feature documentation and a user manual are available in the [`docs/`](docs/) directory:

- **Feature specs**: 22 design documents in [`docs/spec/`](docs/spec/)
- **User manual**: Step-by-step guide in [`docs/manual/`](docs/manual/)

---

## 🧪 Beta Testing

Want to try new features before they go live? Join the
[Ukulele Companion Testers](https://groups.google.com/g/ukulele-companion)
Google Group to get access to pre-release builds through the Google Play Store.

As a tester you can:
- Install pre-release builds from the Play Store
- Try new features before they are publicly available
- Report bugs or share feedback to help improve the app

---

## 🙏 Attribution

Audio samples are from the "Ukelele single notes, close-mic" pack by
[stomachache](https://freesound.org/people/stomachache/packs/8545/) on
Freesound.org, licensed under
[CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).
See [ATTRIBUTION.md](ATTRIBUTION.md) for full details.

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">
  <b>Built with ❤️ for ukulele players everywhere</b><br>
  <sub>Star the repo if you find it useful — it helps others discover the project!</sub>
</p>

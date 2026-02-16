# AGENTS.md -- AI Agent Instructions for Ukulele Companion

Mandatory instructions for AI coding agents (Cursor, Copilot, Codex, etc.) working on this codebase.

## Project Overview

Ukulele Companion is a **free, fully offline** Android app for learning and playing ukulele. A core user base includes **blind and visually impaired musicians** who rely on TalkBack. Every code change must preserve accessibility. Breaking accessibility is treated as seriously as breaking functionality.

**Hard constraints -- never violate these:**
- No network dependencies -- the app must remain fully offline
- No analytics, tracking, or telemetry
- No ads or monetization code
- No third-party SDKs without prior discussion
- Never commit `keystore.properties`, API keys, or secrets

## Tech Stack

| Component | Details |
|-----------|---------|
| Language | Kotlin 2.3.10 (JVM target 11) |
| UI | Jetpack Compose (BOM 2026.02.00), Material 3 |
| Architecture | Single Activity, MVVM, StateFlow |
| Min / Target / Compile SDK | 26 / 35 / 36 |
| Persistence | SharedPreferences + DataStore (no Room) |
| Serialization | kotlinx-serialization-json |
| Audio ML | ONNX Runtime (neural pitch supervision) |
| Widget | Jetpack Glance (Chord of the Day) |
| Background | WorkManager (daily notification) |
| Build | Gradle 9.3.1, AGP 8.9.1, Kotlin DSL, version catalog (`libs.versions.toml`) |

## Package Structure

All source code lives under `app/src/main/java/com/baijum/ukufretboard/`:

| Package | Contents |
|---------|----------|
| `audio/` | `ToneGenerator` (SoundPool playback), `MetronomeEngine`, `AudioCaptureEngine` (44.1kHz PCM) |
| `data/` | Data models, repositories (SharedPreferences-backed), backup/restore, sync |
| `domain/` | Pure Kotlin business logic -- chord detection, transposition, pitch detection, scales, music theory. **No Android imports allowed.** |
| `ui/` | 44+ Compose screens/components. Navigation via `ModalNavigationDrawer` in `FretboardScreen.kt` (no NavHost) |
| `viewmodel/` | 11 ViewModels exposing `StateFlow` (never `LiveData`) |
| `widget/` | Glance-based Chord of the Day home screen widget |
| `MainActivity.kt` | Single-activity entry point |

### ViewModels

`FretboardViewModel`, `TunerViewModel`, `PitchMonitorViewModel`, `ChordLibraryViewModel`, `FavoritesViewModel`, `SongbookViewModel`, `SettingsViewModel`, `LearningProgressViewModel`, `ScalePracticeViewModel`, `BackupRestoreViewModel`, `CustomProgressionViewModel`

### Navigation

The app uses a `ModalNavigationDrawer` with ~30 sections grouped into Play, Create, Learn, and Reference. Screen selection is managed via `mutableIntStateOf` with a `when` block -- there is no Compose NavHost or NavController.

## Audio Processing

- **Pitch detection**: YIN algorithm (pure Kotlin, FFT-based cross-correlation). Frequency range 65--1100 Hz. Confidence scoring and continuity constraints.
- **Playback**: `SoundPool` with OGG ukulele samples (one per pitch class, octave via playback rate). Supports polyphonic chord playback (up to 8 streams) with strum delay simulation.
- **Metronome**: Coroutine-based beat scheduler with configurable BPM and beats-per-chord.
- **Audio capture**: `AudioRecord` at 44.1 kHz, PCM 16-bit mono, 4096-sample frames with 75% overlap (~43 updates/sec).
- **Neural pitch**: Optional ONNX Runtime supervisor for enhanced pitch detection.

## Architecture Rules

- ViewModels expose state via `StateFlow` -- do not use `LiveData`
- Repositories abstract SharedPreferences -- ViewModels must not access SharedPreferences directly
- Domain logic (`domain/` package) must not import Android framework classes
- All async work uses Kotlin coroutines (no RxJava, no callbacks)
- Maintain the existing package structure -- discuss changes in an issue first

## Accessibility Requirements

A core user base relies on TalkBack. **Every code change must preserve and improve accessibility.**

### Rule 1: Icons need contentDescription

Interactive/informative icons MUST have a descriptive `contentDescription`. Decorative-only icons (inside a labeled button where text suffices) may use `null`. Use conditional descriptions for toggle states.

```kotlin
Icon(
    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
    contentDescription = if (isPlaying) "Stop" else "Play"
)
```

### Rule 2: Headings and navigation semantics

Screen titles and section headers MUST have `Modifier.semantics { heading() }`. Navigation containers should use `Modifier.semantics { role = Role.Navigation }`.

```kotlin
Text("Tuner", style = MaterialTheme.typography.titleLarge,
    modifier = Modifier.semantics { heading() })
```

### Rule 3: Canvas needs text alternatives

Any `Canvas` conveying information MUST use `clearAndSetSemantics` with a data-driven description.

```kotlin
Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)
    .clearAndSetSemantics { contentDescription = "Tuning meter, 5 cents sharp" }
) { /* drawing */ }
```

### Rule 4: Dynamic content needs live regions

Use `LiveRegionMode.Polite` for frequent updates (pitch, cents). Use `LiveRegionMode.Assertive` for important state changes (in tune, correct answer, error).

```kotlin
Text(detectedNote, modifier = Modifier.semantics {
    liveRegion = LiveRegionMode.Polite
})
```

### Rule 5: Interactive elements must be focusable and described

Clickable composables need a content description or visible label. Custom interactive components must include `role = Role.Button`. Reflect state changes in `stateDescription`.

```kotlin
Box(modifier = Modifier.clickable { onFretTap(string, fret) }.semantics {
    contentDescription = "$stringName string, fret $fretNumber, $noteName"
    role = Role.Button
    if (isSelected) stateDescription = "selected"
})
```

### Rule 6: Modals must manage focus

Modal titles must have heading semantics. Focus moves to modal content on open and returns to the trigger on dismiss.

### Rule 7: Never remove existing accessibility attributes

Do not remove `contentDescription`, `Modifier.semantics {}` blocks, or `liveRegion` annotations during refactoring. If restructuring a composable, preserve all accessibility attributes in the new structure.

### Key files with accessibility patterns

| File | Notes |
|------|-------|
| `ui/FretboardScreen.kt` | Heading semantics, drawer structure |
| `ui/TunerTab.kt` | Live regions, canvas alternative |
| `ui/VerticalChordDiagram.kt` | `clearAndSetSemantics` on Canvas |
| `ui/FretboardView.kt` | Cell semantics, selection announcements |
| `ui/PitchMonitorTab.kt` | Live regions, canvas alternative |
| `ui/CircleOfFifthsView.kt` | Canvas alternative, key selection |
| `ui/SettingsSheet.kt` | Section heading semantics |
| `ui/theme/Theme.kt` | High contrast theme support |

### contentDescription style

- Sentence case: `"Play all inversions"`, not `"Play All Inversions"`
- Action-oriented: `"Open navigation menu"`, `"Delete note"`
- Conditional for toggles: `if (isPlaying) "Stop" else "Play"`

## Code Style

- **Kotlin** with [official code style](https://kotlinlang.org/docs/coding-conventions.html)
- **Compose-only UI** -- no XML layouts
- **Material 3** components and theming
- Prefer immutable data (`val`, `data class`, `List` over `MutableList` in public APIs)
- Use Kotlin idioms (`let`, `apply`, `also`, `when`) where they improve readability
- Minimize recompositions -- use `remember`, `derivedStateOf`, and `key` appropriately
- Files: PascalCase (`ChordDetector.kt`). Functions: camelCase. Classes/Objects/Enums: PascalCase.

## Build and CI

```bash
./gradlew assembleDebug    # Build debug APK
./gradlew lintDebug         # Run Android Lint
```

**CI** (GitHub Actions on push/PR to `main`): JDK 17 setup, lint, build debug APK, upload artifact.

**Commit format:**
```
Type: short description

Optional body explaining what and why
```
Types: `Add`, `Fix`, `Update`, `Refactor`, `Test`, `Docs`, `Chore`

## Testing

- **Current coverage**: Minimal. One accessibility test file (`AccessibilityTest.kt` in `androidTest/`).
- **Framework**: Compose UI Testing (`ui-test-junit4`), JUnit 4.
- **Domain logic** in `domain/` is pure Kotlin with no Android dependencies -- ideal for unit tests.
- Test both High-G and Low-G tuning when changing chord/note logic.
- Test left-handed mode when touching fretboard UI.
- Verify light, dark, and high-contrast themes for UI changes.

## Pre-Submission Checklist

- [ ] Code builds without errors (`./gradlew assembleDebug`)
- [ ] Changes verified on device or emulator
- [ ] All new icons have appropriate `contentDescription`
- [ ] New screens/sections have heading semantics
- [ ] Canvas components have text alternatives via `clearAndSetSemantics`
- [ ] Dynamic content has `liveRegion` where needed
- [ ] Interactive elements are focusable and described
- [ ] No existing accessibility attributes removed
- [ ] TalkBack navigation works for changed screens
- [ ] UI looks correct in light, dark, and high-contrast themes
- [ ] Left-handed mode not broken (if fretboard UI changed)
- [ ] Both High-G and Low-G tuning work (if chord/note logic changed)

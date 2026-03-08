# AGENTS.md -- AI Agent Instructions for Ukulele Companion

Mandatory instructions for AI coding agents (Cursor, Copilot, Codex, etc.) working on this codebase.

## Project Overview

Ukulele Companion is a **free, fully offline** multiplatform app (Android + iOS) for learning and playing ukulele. A core user base includes **blind and visually impaired musicians** who rely on TalkBack (Android) and VoiceOver (iOS). Every code change must preserve accessibility. Breaking accessibility is treated as seriously as breaking functionality.

**Hard constraints -- never violate these:**
- No network dependencies -- the app must remain fully offline
- No analytics, tracking, or telemetry
- No ads or monetization code
- No third-party SDKs without prior discussion
- Never commit `keystore.properties`, API keys, or secrets

## Tech Stack

### Shared (KMP)

| Component | Details |
|-----------|---------|
| Module | `:shared` -- Kotlin Multiplatform library (Android + iOS) |
| Targets | Android, iosX64, iosArm64, iosSimulatorArm64 |
| Source | `shared/src/commonMain/kotlin/com/baijum/ukufretboard/` |
| Contents | 55 files: 31 domain + 23 data + 1 platform expect/actual |
| Platform | `expect/actual` in `platform/PlatformUtils.kt` (`generateUuid`, `currentTimeMillis`, `currentYear`, `currentDayOfYear`) |

### Android

| Component | Details |
|-----------|---------|
| Language | Kotlin 2.3.10 (JVM target 11) |
| UI | Jetpack Compose (BOM 2026.02.00), Material 3 |
| Architecture | Single Activity, MVVM, StateFlow |
| Min / Target / Compile SDK | 26 / 35 / 36 |
| Persistence | SharedPreferences + DataStore (no Room) |
| Serialization | kotlinx-serialization-json |
| Audio ML | ONNX Runtime Android 1.24.2 (neural pitch supervision) |
| Build | Gradle 9.3.1, AGP 9.0.1, Kotlin DSL, version catalog (`libs.versions.toml`) |

### iOS

| Component | Details |
|-----------|---------|
| Language | Swift 6, SwiftUI |
| Architecture | MVVM, `@StateObject` / `@Published` |
| Min iOS | 17.0 |
| Bundle ID | `com.baijum.ukufretboard.ios` |
| Xcode project | `iosApp/UkuleleCompanion.xcodeproj` |
| KMP framework | Static `shared.framework` built via Gradle |
| Audio ML | ONNX Runtime 1.24.2 xcframework (C API, `iosApp/Frameworks/`) |
| Audio | AVAudioEngine + AVAudioPlayerNode for tone playback |
| Localization | 1010 strings, 16 locales via `Localizable.xcstrings` |

## Package Structure

### Shared module (`shared/src/commonMain/kotlin/com/baijum/ukufretboard/`)

| Package | Contents |
|---------|----------|
| `domain/` | Pure Kotlin business logic -- chord detection, transposition, pitch detection, scales, music theory, tuner note mapping. **No platform imports allowed.** |
| `data/` | Data models, enums (`UkuleleTuning`, `Notes`), configuration types |
| `platform/` | `expect/actual` declarations for platform-specific functions |

Platform actuals: `shared/src/androidMain/` (java.util), `shared/src/iosMain/` (Foundation)

### Android app (`app/src/main/java/com/baijum/ukufretboard/`)

| Package | Contents |
|---------|----------|
| `audio/` | `ToneGenerator` (SoundPool playback), `MetronomeEngine`, `AudioCaptureEngine` (44.1kHz PCM) |
| `data/` | Repositories (SharedPreferences-backed), backup/restore manager |
| `domain/` | Android-specific domain logic -- `NeuralPitchSupervisor`, `ChordImageSharer`, `AchievementChecker` |
| `ui/` | 54 Compose screens/components. Navigation via `ModalNavigationDrawer` in `FretboardScreen.kt` (no NavHost) |
| `viewmodel/` | 13 ViewModels exposing `StateFlow` (never `LiveData`) |

| `MainActivity.kt` | Single-activity entry point |

### iOS app (`iosApp/UkuleleCompanion/`)

| Directory | Contents |
|-----------|----------|
| `Audio/` | `AudioCaptureEngine` (AVAudioEngine), `TonePlayer` (WAV sample playback), `NeuralPitchSupervisor` (ONNX C API) |
| `Views/` | 47 SwiftUI views: `PlayView`, `ExplorerView`, `TunerView`, `OnboardingView`, `CreateView`, `LearnView`, `ReferenceView`, `MetronomeView`, `FavoritesView`, `SettingsView`, `HelpView`, `ChordLibraryView`, `PitchMonitorView`, `StrumPatternsView`, `ProgressionsView`, `SongbookView`, `MelodyNotepadView`, `AchievementsView`, `ChordTransitionsView`, `PlayAlongView`, `DailyChallengeView`, `PracticeRoutineView`, `IntervalTrainerView`, `TheoryQuizView`, `NoteQuizView`, `ScalePracticeView`, `LearningProgressView`, `FullScreenFretboardView`, + more |
| `ViewModels/` | 15 ViewModels: `FretboardViewModel`, `TunerViewModel`, `MetronomeViewModel`, `FavoritesViewModel`, `SettingsViewModel`, `ChordLibraryViewModel`, `SongbookViewModel`, `MelodyViewModel`, `ProgressionsViewModel`, `LearnViewModel`, `PitchMonitorViewModel`, `ScalePracticeViewModel`, `PracticeTimerViewModel`, `CustomPatternsViewModel`, `PlayAlongViewModel` |
| `Helpers/` | `AccessibilityHelper` (VoiceOver announcer), `BackupRestoreManager` (JSON backup/restore) |
| `Resources/` | 14 WAV samples + `swift_f0_model.onnx` |

### ViewModels

`FretboardViewModel`, `TunerViewModel`, `PitchMonitorViewModel`, `ChordLibraryViewModel`, `FavoritesViewModel`, `SongbookViewModel`, `SettingsViewModel`, `ScalePracticeViewModel`, `MetronomeViewModel`, `MelodyViewModel`, `ChordTransitionsViewModel`, `PracticeTimerViewModel`, `CustomPatternsViewModel`, `LearnViewModel`, `PlayAlongViewModel`, `ProgressionsViewModel`

### Navigation

**Android**: `ModalNavigationDrawer` with ~30 sections grouped into Play, Create, Learn, and Reference. Screen selection is managed via `mutableIntStateOf` with a `when` block -- there is no Compose NavHost or NavController.

**iOS**: `TabView` with 4 tabs (Play, Create, Learn, Reference). Each tab is a menu-style `NavigationStack` with `NavigationLink` rows. Play contains Explorer, Tuner, Pitch Monitor, Metronome, Chord Library, and Favorites. Settings gear icon in each tab's toolbar presents `SettingsView` as a `.sheet`. Help is accessible from within Settings.

## Audio Processing

### Shared (KMP)
- **Pitch detection**: YIN algorithm (pure Kotlin, FFT-based cross-correlation). Frequency range 65--1100 Hz. Confidence scoring and continuity constraints.
- **FFT**: Custom implementation with cached twiddle factors and pre-allocated work buffers.

### Android
- **Playback**: `SoundPool` with OGG ukulele samples (one per pitch class, octave via playback rate). Supports polyphonic chord playback (up to 8 streams) with strum delay simulation.
- **Metronome**: Coroutine-based beat scheduler with configurable BPM and beats-per-chord.
- **Audio capture**: `AudioRecord` at 44.1 kHz, PCM 16-bit mono, 4096-sample frames with 75% overlap (~43 updates/sec).
- **Neural pitch**: ONNX Runtime Android supervisor for enhanced pitch detection.

### iOS
- **Playback**: `AVAudioEngine` + `AVAudioPlayerNode` with bundled WAV samples (mono, 44.1 kHz). Player node connected using explicit mono format to avoid channel mismatch.
- **Metronome**: Timer-based beat scheduler with click samples.
- **Audio capture**: `AVAudioEngine` input node tap at 44.1 kHz mono.
- **Neural pitch**: ONNX Runtime xcframework via C API (`onnxruntime.xcframework` in `iosApp/Frameworks/`, gitignored). Setup via `iosApp/setup_onnxruntime.sh`.
- **Simulator note**: Audio session activation is skipped on simulator (`#if targetEnvironment(simulator)`) to prevent CoreAudio deadlocks.

## Architecture Rules

### General
- Domain logic in `shared/src/commonMain/.../domain/` must not import platform-specific classes -- use `expect/actual` for platform needs
- New business logic should go in the shared module when possible
- Maintain the existing package structure -- discuss changes in an issue first

### Android
- ViewModels expose state via `StateFlow` -- do not use `LiveData`
- Repositories abstract SharedPreferences -- ViewModels must not access SharedPreferences directly
- All async work uses Kotlin coroutines (no RxJava, no callbacks)

### iOS
- ViewModels use `@Published` properties with `ObservableObject`
- Use `@StateObject` for view-owned ViewModels, `@ObservedObject` for passed-in ones
- KMP Swift naming: classes drop `Shared` prefix (e.g., `PitchDetector.shared`, `UkuleleTuning.highG`)
- KMP numeric types: `KotlinFloatArray`, `KotlinDouble` (no prefix)

## Accessibility Requirements

A core user base relies on TalkBack (Android) and VoiceOver (iOS). **Every code change must preserve and improve accessibility.**

### iOS (SwiftUI)

- Use `.accessibilityLabel()` on interactive elements and images
- Use `.accessibilityHint()` for non-obvious interactions
- Mark section titles with `.accessibilityAddTraits(.isHeader)`
- Use `.accessibilityValue()` for dynamic state (sliders, pickers)
- Custom drawn views need `.accessibilityRepresentation` or `.accessibilityElement(children:)`

### Android (Compose)

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
# Android
./gradlew assembleDebug                        # Build debug APK
./gradlew lintDebug                            # Run Android Lint

# iOS (requires Xcode, runs Gradle to build shared framework)
xcodebuild -project iosApp/UkuleleCompanion.xcodeproj \
  -scheme UkuleleCompanion \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  build
```

**CI** (GitHub Actions on push/PR to `main`):

- **Android CI** (`android.yml`): JDK 17 setup, lint, unit tests, build debug APK, APK size report, dependency review.
- **iOS CI** (`ios.yml`): JDK 17 + Xcode 16.2 on macOS, build shared KMP framework, download ONNX Runtime xcframework, build iOS app, run unit tests.

**Commit format:**
```
Type: short description

Optional body explaining what and why
```
Types: `Add`, `Fix`, `Update`, `Refactor`, `Test`, `Docs`, `Chore`

## Testing

- **Framework**: JUnit 4, Kotest Property Testing (`io.kotest:kotest-property`), Compose UI Testing (`ui-test-junit4`).
- **Run all unit tests**: `./gradlew testDebugUnitTest`
- **Run instrumented tests**: `./gradlew connectedAndroidTest` (requires emulator/device)

### Unit tests (`app/src/test/`)

| File | What it tests |
|------|---------------|
| `PitchDetectorTest.kt` | YIN pitch detection with synthetic sine waves |
| `AudioResamplerTest.kt` | Downsampling ratio, empty input, frequency preservation |
| `FFTProcessorTest.kt` | DC signal, pure sine peak, FFT/IFFT round-trip |
| `TunerNoteMapperTest.kt` | Frequency-to-note mapping, string matching, hysteresis |
| `TtsAnnouncementThrottlerTest.kt` | Tuner spoken feedback throttling |

### Property-based tests (`app/src/test/`, `*PropertyTest.kt`)

Kotest property tests generate thousands of random inputs per test and verify invariants. Use `runBlocking { checkAll(...) { ... } }` with JUnit 4.

| File | What it covers |
|------|---------------|
| `TransposePropertyTest.kt` | Identity, inverse round-trip, associativity, chord name preservation, capo fret range |
| `ChordNameParserPropertyTest.kt` | Arbitrary string robustness, known chord parsing, case insensitivity, unique suggestions |
| `ChordDetectorPropertyTest.kt` | Empty/single/pair handling, known formula detection, exhaustive result types |
| `FFTProcessorPropertyTest.kt` | FFT/IFFT round-trip with random signals, sine peak detection, Hanning window properties |
| `PitchDetectorPropertyTest.kt` | Silent/quiet buffer rejection, pure sine accuracy, frequency range, previousFrequency robustness |
| `ChordSheetTransposePropertyTest.kt` | Zero/twelve identity, double transpose round-trip, quality suffix and non-chord text preservation |
| `CapoCalculatorPropertyTest.kt` | Capo position range, score ordering, voicing fret range, sounding name correctness |

### Instrumented tests (`app/src/androidTest/`)

| File | What it tests |
|------|---------------|
| `AccessibilityTest.kt` | Content descriptions, headings, clickable node descriptions |
| `TunerSpokenFeedbackTest.kt` | Live-region semantics suppressed when spoken feedback is enabled |

### iOS unit tests (`iosApp/UkuleleCompanionTests/`)

| File | What it tests |
|------|---------------|
| `SettingsViewModelTests.swift` | Default values, save/load, export/import, onboarding flag |

### UI stress testing

Run the Monkey script to send random UI events to the app on a connected device/emulator:

```bash
./scripts/monkey_test.sh          # 10,000 events, auto-generated seed
./scripts/monkey_test.sh 42       # 10,000 events, reproducible seed
./scripts/monkey_test.sh 42 50000 # 50,000 events, reproducible seed
```

### Testing guidelines

- Test both High-G and Low-G tuning when changing chord/note logic.
- Test left-handed mode when touching fretboard UI.
- Verify light, dark, and high-contrast themes for UI changes.
- Domain logic (`domain/` package) is pure Kotlin -- add property tests for new invariants.

## Pre-Submission Checklist

### Both platforms
- [ ] Shared module code builds (`./gradlew :shared:build`)
- [ ] Both High-G and Low-G tuning work (if chord/note logic changed)
- [ ] Left-handed mode not broken (if fretboard UI changed)

### Android
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

### iOS
- [ ] iOS builds without errors (`xcodebuild ... build`)
- [ ] Changes verified on simulator or device
- [ ] SwiftUI views have appropriate `.accessibilityLabel()` / `.accessibilityHint()`
- [ ] No existing accessibility modifiers removed
- [ ] VoiceOver navigation works for changed screens

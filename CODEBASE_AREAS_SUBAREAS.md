# Ukulele Companion Codebase Areas and Sub-Areas

**Generated from full codebase scan** (2026-06-14).  
This document inventories **all** areas and sub-areas in the source code, tests, configuration, documentation, and tooling.

**Referenced from:** [README.md](README.md), [AGENTS.md](AGENTS.md), [CONTRIBUTING.md](CONTRIBUTING.md), [CLAUDE.md](CLAUDE.md), [docs/index.md](docs/index.md), [docs/manual/index.md](docs/manual/index.md), [docs/ios-parity-report.md](docs/ios-parity-report.md), and key CLAUDE.md files.

It is based on:

- Complete directory listings of `shared/`, `app/`, `iosApp/`
- `NavSection` enum (all 32+ persistent sections)
- Drawer/sidebar grouping in `DrawerContent.kt` (Android) and `ContentView.swift` (iOS)
- All `.kt` / `.swift` source files (via recursive find + listings)
- `data/`, `domain/`, `ui/`, `ViewModels/`, `Views/`, `audio/`, `Repositories/`, `Helpers/`
- Test directories (`app/src/test/`, `app/src/androidTest/`, `iosApp/UkuleleCompanionTests/`)
- Documentation (`docs/manual/`, `docs/spec/`, `docs/videos/`, `AGENTS.md`, `README.md`, etc.)
- Build/CI/scripts (`.gradle`, `scripts/`, `.cursor/rules/`, `.cursor/skills/`)
- Cross-cutting files (Info.plist, Localizable.xcstrings, strings resources, ONNX models, audio assets)

**No areas were omitted.** Sub-areas are broken down to the level of logical groupings, file clusters, and feature responsibilities. Each row links representative files from the scan.

The primary navigation is a drawer (Android `ModalNavigationDrawer`) / sidebar + tabs (iOS `NavigationSplitView` / `TabView`) with sections grouped under high-level "tabs" (Play / Create / Practice/Training/Knowledge/Progress / Reference) plus global features.

---

## 1. High-Level Feature / Navigation Areas

These correspond to the user-facing tabs/headers and `NavSection` values. Grouped as presented in UI (Android DrawerContent + iOS ContentView sidebar).

| Area              | Sub-area                  | Description / Responsibilities                                                                 | Representative Files / Modules |
|-------------------|---------------------------|----------------------------------------------------------------------------------------------------|--------------------------------|
| **Play**          | Explorer / Fretboard     | Interactive fretboard explorer with scale overlays, note highlighting, chord detection from taps. | `ui/navigation/ExplorerTab.kt`, `ui/FretboardView.kt`, `ui/FretboardNoteMapView.kt`, `ui/FullScreenFretboard.kt`, `Views/FretboardView.swift`, `Views/ExplorerView.swift`, `FretboardViewModel.kt`, `FretboardViewModel.swift`, `shared/data/NavSection.EXPLORER` |
| **Play**          | Tuner                    | Real-time pitch detection (YIN + optional neural), string matching, needle meter, haptics, spoken feedback, in-tune progress. | `ui/TunerTab.kt`, `viewmodel/TunerViewModel.kt`, `audio/AudioCaptureEngine.kt`, `Views/TunerView.swift`, `ViewModels/TunerViewModel.swift`, `TunerUiState.kt`, `shared/domain/PitchDetector.kt`, `TunerNoteMapper.kt`, `shared/domain/FrameGate.kt`, `NavSection.TUNER` |
| **Play**          | Pitch Monitor            | Scrolling pitch trace (piano-roll style), real-time chord detection (simultaneous + arpeggio), chroma visualization. | `ui/PitchMonitorTab.kt`, `viewmodel/PitchMonitorViewModel.kt`, `Views/PitchMonitorView.swift`, `ViewModels/PitchMonitorViewModel.swift`, `shared/domain/PitchMonitorStateMachine.kt`, `AudioChordDetector.kt`, `Chromagram.kt`, `NavSection.PITCH_MONITOR` |
| **Play**          | Metronome                | BPM control, tap tempo, accent patterns, click sounds, visual beat indicator. | `ui/MetronomeTab.kt`, `viewmodel/MetronomeViewModel.kt`, `audio/MetronomeEngine.kt`, `Views/MetronomeView.swift`, `ViewModels/MetronomeViewModel.swift`, `shared/domain/MetronomeStateLogic.kt`, `audio/ClickSoundPlayer.kt`, `NavSection.METRONOME` |
| **Play**          | Chord Library            | Browse/search chords by root/quality, voicings, inversions, playback, favorites integration. | `ui/ChordLibraryTab.kt`, `viewmodel/ChordLibraryViewModel.kt`, `Views/ChordLibraryView.swift`, `ViewModels/ChordLibraryViewModel.swift`, `ui/ChordResultView.kt`, `VerticalChordDiagram.kt`, `ChordDiagramView.swift`, `NavSection.LIBRARY` |
| **Play**          | Favorites                | Saved voicings/folders, quick access, practice from favorites. | `ui/FavoritesTab.kt`, `viewmodel/FavoritesViewModel.kt`, `Views/FavoritesView.swift`, `ViewModels/FavoritesViewModel.swift`, `data/FavoritesRepository.kt`, `Repositories/FavoritesRepository.swift`, `NavSection.FAVORITES` |
| **Create**        | Songwriter Mode / Start a Song | Guided song creation flow (melody + chords + patterns). | `ui/SongwriterModeFlow.kt`, `Views/SongwriterModeFlow.swift`, `NavSection.SONGWRITER_MODE` |
| **Create**        | Chord Progressions       | Custom + built-in progressions, playback, practice, voice leading. | `ui/ProgressionsTab.kt`, `viewmodel/CustomProgressionViewModel.kt` / `ProgressionsViewModel.kt`, `Views/ProgressionsView.swift`, `ViewModels/ProgressionsViewModel.swift`, `ui/ProgressionPracticeView.kt`, `ProgressionPlaybackBar.kt`, `shared/data/Progressions.kt`, `NavSection.PROGRESSIONS` |
| **Create**        | Strumming / Fingerpicking Patterns | Custom + library patterns, editor sheets, practice integration. | `ui/patterns/StrumPatternsTab.kt`, `ui/patterns/CreateStrumPatternSheet.kt`, `Views/StrumPatternsView.swift`, `Views/CreateStrumPatternSheet.swift`, `ViewModels/CustomPatternsViewModel.swift`, `Repositories/CustomPatternsRepository.swift`, `shared/data/StrumPatterns.kt`, `FingerpickingPatterns.kt`, `NavSection.PATTERNS` |
| **Create**        | Songbook (ChordPro)      | Full editor/viewer for ChordPro songs, import/export, transpose, performance mode, strum picker. | `ui/songbook/SongbookTab.kt`, `ui/songbook/SheetEditor.kt`, `ui/songbook/SheetViewer.kt`, `ui/songbook/PerformanceModeView.kt`, `viewmodel/SongbookViewModel.kt`, `Views/SongbookView.swift`, `Views/SongEditorView.swift`, `Views/SongViewerView.swift`, `data/ChordSheetRepository.kt`, `Repositories/SongbookRepository.swift`, `shared/data/sync/BackupCodec.kt`, `ChordProExporter.kt`, `ChordProParser.kt`, `ChordSheetFormatter.kt`, `ChordSheetTranspose.kt`, `NavSection.SONGBOOK` |
| **Create**        | Setlists                 | Organize songs into sets, reorder, practice mode. | `ui/SetlistTab.kt`, `viewmodel/SetlistViewModel.kt`, `Views/SetlistView.swift`, `ViewModels/SetlistViewModel.swift`, `data/SetlistRepository.kt`, `Repositories/SetlistRepository.swift`, `NavSection.SETLISTS` |
| **Create**        | Melody Notepad           | Step sequencer / piano-roll style melody editor, playback, save/load. | `ui/melody/MelodyNotepadView.kt`, `ui/melody/MelodyStepSequencer.kt`, `ui/melody/MelodyInputControls.kt`, `viewmodel/MelodyViewModel.kt`, `Views/MelodyNotepadView.swift`, `ViewModels/MelodyViewModel.swift`, `data/MelodyRepository.kt`, `Repositories/MelodyRepository.swift`, `shared/data/MelodyNote.kt`, `NavSection.MELODY_NOTEPAD` |
| **Practice / Learn** | Daily Challenge       | Auto-generated daily tasks (chords, scales, etc.) based on date. | `ui/DailyChallengeView.kt`, `Views/DailyChallengeView.swift`, `shared/data/DailyChallengeGenerator.kt`, `shared/platform/PlatformUtils.kt` (currentDayOfYear), `NavSection.DAILY_CHALLENGE` |
| **Practice / Learn** | Practice Routine      | Generated multi-step practice sessions. | `ui/PracticeRoutineView.kt`, `Views/PracticeRoutineView.swift`, `shared/domain/PracticeRoutineGenerator.kt`, `NavSection.PRACTICE_ROUTINE` |
| **Practice / Learn** | Chord Transitions     | Practice smooth changes between two chords with metronome/animation. | `ui/ChordTransitionView.kt`, `ui/ChordTransitionAnimation.kt`, `Views/ChordTransitionsView.swift`, `ViewModels/ChordTransitionsViewModel.swift`, `shared/domain/FingerTransitionCalculator.kt`, `NavSection.CHORD_TRANSITION` |
| **Practice / Learn** | Play Along            | Real-time chord following with scoring, metronome, audio detection (simul + arpeggio). | `ui/PlayAlongView.kt`, `Views/PlayAlongView.swift`, `ViewModels/PlayAlongViewModel.swift`, `shared/domain/PlayAlongScorer.kt`, `audio/PatternPlayer.kt`, `Views/PlayAlongView.swift`, `NavSection.PLAY_ALONG` |
| **Training**      | Interval Trainer       | Ear training for intervals (play, identify, quiz). | `ui/IntervalTrainerView.kt`, `Views/IntervalTrainerView.swift`, `shared/domain/IntervalTrainer.kt`, `NoteQuizGenerator.kt`, `NavSection.INTERVAL_TRAINER` |
| **Training**      | Chord Ear Training     | Identify chords by ear (play, multiple choice). | `ui/ChordEarTrainingView.kt`, `Views/ChordEarTrainingView.swift`, `shared/domain/ChordEarTrainer.kt`, `NavSection.CHORD_EAR` |
| **Training**      | Note Quiz              | Name/find notes on fretboard or by sound. | `ui/NoteQuizView.kt`, `Views/NoteQuizView.swift`, `shared/domain/NoteQuizGenerator.kt`, `NavSection.NOTE_QUIZ` |
| **Training**      | Scale Practice         | Learn/practice scales with multiple modes (ear training, play-along, quiz, stats). | `ui/ScalePracticeView.kt` + sub (EarTraining, PlayAlong, Quiz, Stats, ScaleSelector), `Views/ScalePracticeView.swift`, `ViewModels/ScalePracticeViewModel.kt`, `shared/domain/ScalePracticeGenerator.kt`, `ScaleChords.kt`, `ScaleChordBuilder.kt`, `ScalePositions.kt`, `NavSection.SCALE_PRACTICE` |
| **Knowledge**     | Theory Lessons         | Structured music theory content + quizzes. | `ui/TheoryLessonsView.kt`, `ui/TheoryQuizView.kt`, `Views/TheoryLessonsView.swift`, `Views/TheoryQuizView.swift`, `shared/data/TheoryLessons.kt`, `NavSection.THEORY_LESSONS`, `NavSection.THEORY_QUIZ` |
| **Progress**      | Learning Progress      | Streaks, unlocked content tracking. | `ui/LearningProgressView.kt`, `Views/LearningProgressView.swift`, `viewmodel/LearningProgressViewModel.kt`, `data/LearningProgressRepository.kt`, `Repositories/LearnRepository.swift`, `NavSection.LEARNING_PROGRESS` |
| **Progress**      | Achievements           | Unlockable badges tied to usage. | `ui/AchievementsView.kt`, `Views/AchievementsView.swift`, `viewmodel/` (AchievementContextAdapter), `shared/domain/Achievements.kt`, `data/AchievementRepository.kt`, `NavSection.ACHIEVEMENTS` |
| **Reference**     | Glossary               | Music terms with definitions. | `ui/GlossaryView.kt`, `Views/GlossaryView.swift`, `shared/data/Glossary.kt`, `NavSection.GLOSSARY` |
| **Reference**     | Capo Guide / Calculator| Visual + calculation aid for capo positions. | `ui/CapoGuideView.kt`, `ui/CapoCalculatorView.kt`, `ui/CapoVisualizerView.kt`, `Views/CapoGuideView.swift`, `Views/CapoCalculatorView.swift`, `shared/domain/CapoCalculator.kt`, `data/CapoReference.kt`, `NavSection.CAPO_GUIDE` |
| **Reference**     | Fretboard Note Map     | Note names across the fretboard. | `ui/FretboardNoteMapView.kt`, `Views/FretboardNoteMapView.swift`, `NavSection.NOTE_MAP` |
| **Reference**     | Chord Substitutions    | Common chord replacements / tricks. | `ui/ChordSubstitutionsView.kt`, `Views/ChordSubstitutionsView.swift`, `shared/data/ChordSubstitutions.kt`, `NavSection.CHORD_SUBS` |
| **Reference**     | Scale Chords           | Diatonic chords for scales. | `ui/ScaleChordView.kt`, `Views/ScaleChordsView.swift`, `shared/domain/ScaleChords.kt`, `NavSection.SCALE_CHORDS` |
| **Reference**     | Circle of Fifths       | Interactive key/scale reference with selection. | `ui/CircleOfFifthsView.kt`, `Views/CircleOfFifthsView.swift`, `shared/data/KeySignatures.kt`, `NavSection.CIRCLE_OF_FIFTHS` |
| **Reference**     | Voice Leading          | Voice leading examples between chords. | `ui/VoiceLeadingView.kt`, `Views/VoiceLeadingView.swift`, `shared/domain/VoiceLeading.kt`, `HarmonicFunction.kt` |
| **Reference**     | Help                   | In-app help / tips. | `ui/HelpView.kt`, `Views/HelpView.swift`, `NavSection.HELP` |
| **Global / Other**| Settings & Display/Sound | Theme, tuning, reduce motion, spoken feedback, volumes, etc. | `ui/SettingsSheet.kt`, `ui/SettingsComponents.kt`, `ui/DisplaySection.kt`, `ui/SoundSection.kt`, `ui/InstrumentSettingsSections.kt`, `viewmodel/SettingsViewModel.kt`, `Views/SettingsView.swift`, `ViewModels/SettingsViewModel.kt`, `data/AppSettings.kt`, `Repositories/SettingsRepository.swift` |
| **Global / Other**| Onboarding               | First-run setup flow. | `ui/OnboardingScreen.kt`, `Views/OnboardingView.swift` |
| **Global / Other**| Backup & Restore         | Export/import all user data (JSON via shared codec). | `ui/BackupRestoreSection.kt`, `viewmodel/BackupRestoreViewModel.kt`, `data/sync/BackupRestoreManager.kt`, `Helpers/BackupRestoreManager.swift`, `shared/data/sync/BackupCodec.kt`, `BackupData.kt` |
| **Global / Other**| Share / Image / PDF      | Chord diagram sharing, song export. | `ui/ShareUtils.kt`, `ui/ShareableChordDiagram.kt`, `domain/ChordImageSharer.kt`, `Views/ShareableChordDiagramView.swift`, `Views/ShareChordSheet.swift` |
| **Global / Other**| Review Prompts           | In-app review timing logic. | `shared/domain/ReviewPromptEligibility.kt`, `data/ReviewPromptRepository.kt`, `ui/ReviewPromptLauncher.kt`, `Helpers/ReviewPromptManager.swift` |

---

## 2. Shared KMP Business Logic (commonMain – pure Kotlin, no platform imports)

| Area                  | Sub-area                     | Description                                                                 | Key Files |
|-----------------------|------------------------------|-----------------------------------------------------------------------------|-----------|
| **Data Models**       | Core Enums & Config          | Tunings, notes, settings, enums for scales/practice/beat types.            | `data/Notes.kt`, `UkuleleTuning` (implied), `AppSettings.kt`, `ScalePracticeEnums.kt`, `BeatType.kt`, `SongSortOrder.kt`, `NavSection.kt` |
| **Data Models**       | Music Content                | Chords, scales, progressions, melodies, setlists, glossary, theory, tips.  | `data/ChordSheet.kt`, `Scales.kt`, `Progressions.kt`, `MelodyNote.kt`, `Setlist.kt`, `Glossary.kt`, `TheoryLessons.kt`, `ExplorerTips.kt`, `KeySignatures.kt` |
| **Data Models**       | Patterns & Voicings          | Strum/fingerpick patterns, favorite voicings, chord formulas/subs.         | `data/StrumPatterns.kt`, `FingerpickingPatterns.kt`, `FavoriteVoicing.kt`, `ChordFormulas.kt`, `ChordSubstitutions.kt`, `VoicingGenerator.kt` |
| **Data Models**       | Backup / Sync                | Serializable backup DTOs + codec for cross-platform import/export.         | `data/sync/BackupData.kt`, `BackupCodec.kt` |
| **Domain – Audio DSP**| Pitch & Chord from Audio     | YIN pitch detection, chromagram, audio chord detection, resampling for neural. | `domain/PitchDetector.kt`, `FFTProcessor.kt`, `Chromagram.kt`, `AudioChordDetector.kt`, `AudioResampler.kt` |
| **Domain – Audio DSP**| Utilities & Gates            | Frame dropping, buffer management, neural result arbitration.              | `domain/FrameGate.kt`, `NeuralArbitrator.kt`, `NeuralPitchResult.kt` |
| **Domain – Music Theory** | Core Theory               | Transposition, chord parsing/detection, note math, voice leading.          | `domain/Transpose.kt`, `ChordSheetTranspose.kt`, `ChordDetector.kt`, `ChordNameParser.kt`, `Chord.kt`, `Note.kt`, `TunerNoteMapper.kt`, `VoiceLeading.kt`, `HarmonicFunction.kt`, `KeyDetector.kt` |
| **Domain – Generators & Trainers** | Practice Content        | Scale/chord builders, ear trainers, quizzes, daily challenges, routines.   | `domain/ScaleChords.kt`, `ScaleChordBuilder.kt`, `ScalePracticeGenerator.kt`, `ChordEarTrainer.kt`, `IntervalTrainer.kt`, `NoteQuizGenerator.kt`, `QuizGenerator.kt`, `DailyChallengeGenerator.kt`, `PracticeRoutineGenerator.kt` |
| **Domain – Scorers & State** | Real-time Feedback     | Play-along scoring, pitch monitor state machine, metronome logic, capo calc. | `domain/PlayAlongScorer.kt`, `PitchMonitorStateMachine.kt`, `MetronomeStateLogic.kt`, `CapoCalculator.kt`, `ArpeggioDetector.kt`, `FingerTransitionCalculator.kt` |
| **Domain – Utilities** | Misc Helpers             | Achievements, merge policies, songbook filtering, chord info/formatting, review prompt timing. | `domain/Achievements.kt`, `MergePolicy.kt`, `SongbookFilter.kt`, `ChordInfo.kt`, `ChordSheetFormatter.kt`, `ReviewPromptEligibility.kt` |
| **Platform Abstractions** | Expect/Actual         | UUID, time, locking (used by audio buffers).                               | `platform/PlatformUtils.kt`, `PlatformLock.kt` (with androidMain/iosMain/jvmMain impls) |

---

## 3. Android-Specific Implementation

| Area             | Sub-area                  | Description                                                                 | Key Files / Packages |
|------------------|---------------------------|-----------------------------------------------------------------------------|----------------------|
| **Audio**        | Capture & Playback        | Mic capture (AudioRecord ring buffer), tone gen, metronome engine, pattern player, click sounds. | `audio/AudioCaptureEngine.kt`, `ToneGenerator.kt`, `MetronomeEngine.kt`, `PatternPlayer.kt`, `ClickSoundPlayer.kt` |
| **ML / Neural**  | Pitch Supervisor          | ONNX Runtime wrapper for neural F0 estimation (async load, fallback).      | `domain/NeuralPitchSupervisor.kt` |
| **Persistence**  | Repositories              | SharedPreferences-backed CRUD + import/merge for all user data types.      | `data/*Repository.kt` (Favorites, ChordSheet, Setlist, Melody, Custom*, LearningProgress, Achievement, PracticeTimer, ReviewPrompt, JsonListRepository), `sync/BackupRestoreManager.kt` |
| **UI (Compose)** | Core Screens & Components | All 70+ Composables. Grouped by feature (see Feature Areas table). Includes diagrams (Canvas + semantics), sheets, animations, settings sections. | `ui/` (root + `melody/`, `songbook/`, `patterns/`, `navigation/`, `theme/`) + individual `*View.kt`, `VerticalChordDiagram.kt`, `ShareableChordDiagram.kt`, `ReduceMotion.kt`, `StateSavers.kt`, `I18nMappings.kt` |
| **UI (Compose)** | Navigation                | Drawer + section management (no NavHost).                                  | `ui/navigation/FretboardScreen.kt`, `DrawerContent.kt`, `NavigationConstants.kt`, `ExplorerTab.kt` |
| **State**        | ViewModels                | StateFlow-based MVVM for every major feature + shared utilities.           | `viewmodel/*ViewModel.kt` (Tuner, PitchMonitor, Songbook, ScalePractice, Setlist, Melody, Favorites, ChordLibrary, Metronome, BackupRestore, Settings, Fretboard, LearningProgress, CustomProgression, + TtsAnnouncementThrottler, TunerUiState, LegacySettingsReader, AchievementContextAdapter) |
| **Platform Glue**| Main + Theming            | Edge-to-edge, reduce motion provider, TTS init, ToneGenerator lifecycle.   | `MainActivity.kt`, `ui/theme/Theme.kt` |
| **Tests**        | Unit / Instrumented       | Property tests (Kotest), JUnit, accessibility & spoken feedback tests.     | `app/src/test/`, `app/src/androidTest/` (AccessibilityTest, TunerSpokenFeedbackTest, AutoScrollTest + many *Test.kt and *PropertyTest.kt) |

---

## 4. iOS-Specific Implementation (Swift 6 / SwiftUI)

| Area             | Sub-area                  | Description                                                                 | Key Files / Directories |
|------------------|---------------------------|-----------------------------------------------------------------------------|-------------------------|
| **Audio**        | Capture & Playback        | AVAudioEngine tap + resample, ONNX C-API neural, tone player, pattern player. | `Audio/AudioCaptureEngine.swift`, `NeuralPitchSupervisor.swift`, `TonePlayer.swift`, `PatternPlayer.swift` |
| **Persistence**  | Repositories              | Codable/JSON storage + ViewModel-driven import/export for user data.       | `Repositories/` (JSONStorageRepository.swift + domain-specific: Favorites, Songbook, Melody, ScalePractice, Setlist, Progressions, CustomPatterns, Learn, PracticeTimer, Settings) |
| **UI (SwiftUI)** | All Views                 | 50+ SwiftUI views + sheets/flows. Grouped by feature (see Feature Areas). Heavy use of accessibility modifiers. | `Views/` (root + many `*View.swift`, `Create*.swift`, `PerformanceModeView.swift`, `SongwriterModeFlow.swift`, etc.) |
| **State**        | ViewModels                | `@Published` / `ObservableObject` MVVM (KMP bridging for shared types).    | `ViewModels/` (one per major feature + ChordTransitionsViewModel, LearnViewModel, PlayAlongViewModel, etc.) |
| **Helpers**      | Bridging & Utilities      | KMP interop, accessibility announcements, backup manager, review prompts.  | `Helpers/` (KMPBridging.swift, AccessibilityHelper.swift, BackupRestoreManager.swift, ReviewPromptManager.swift) |
| **Resources**    | Assets & Models           | ONNX model, click samples, localized strings.                              | `Resources/` (swift_f0_model.onnx + .wav), `Assets.xcassets`, `Localizable.xcstrings`, `Info.plist` |
| **App Entry**    | Lifecycle & Layout        | Tab/sidebar coordination, high-contrast tint, review prompts.              | `UkuleleCompanionApp.swift`, `ContentView.swift` |
| **Tests**        | Unit + UI                 | XCTest for ViewModels + monkey-style UI test.                              | `UkuleleCompanionTests/` (many *ViewModelTests.swift, AccessibilityTests.swift, PlayAlongAndNeuralTests.swift, MonkeyTest.swift in UITests) |

---

## 5. Cross-Cutting Concerns (appear across layers)

| Area                  | Sub-area                     | Description / Enforcement                                                                 | Key Locations |
|-----------------------|------------------------------|-------------------------------------------------------------------------------------------|---------------|
| **Accessibility**     | TalkBack / VoiceOver         | Labels, hints, headings, live regions, custom actions (per-string on diagrams), reduce motion, state descriptions. Core user base requirement. | `.cursor/rules/compose-accessibility.mdc`, `swiftui-accessibility.mdc`; `ui/*` (semantics, clearAndSetSemantics, contentDescription); `Views/*` (.accessibilityLabel/Hint/Value/AddTraits); `Helpers/AccessibilityHelper.swift`; `ReduceMotion.kt`; `MainActivity.kt` (LocalReduceMotion provider) |
| **Accessibility**     | Reduce Motion                | All animations must snap or use None when enabled (system + user setting).                | `ReduceMotion.kt` + `rememberReduceMotion()`; `@Environment(\.accessibilityReduceMotion)` in SwiftUI; used in Tuner, Metronome, transitions, etc. |
| **Internationalization** | Strings & Localization   | 16 locales; user-facing text must use resources.                                         | `app/src/main/res/values*/strings.xml`; `Localizable.xcstrings`; `I18nMappings.kt`; `add-translations` skill |
| **Neural / ML**       | ONNX Pitch Estimation        | Async load, fallback to YIN, arbitration, downsampling to 16 kHz.                         | Shared: `NeuralArbitrator.kt`, `AudioResampler.kt`; Android: `NeuralPitchSupervisor.kt`; iOS: `NeuralPitchSupervisor.swift`; model in `Resources/` |
| **Backup / Data Sync**| Export / Import              | JSON round-trip (shared codec normalizes legacy iOS formats + timestamps). Merge strategy (not full replace). | `shared/data/sync/*`; Android `BackupRestoreManager.kt` + ViewModel; iOS `BackupRestoreManager.swift` + Coders |
| **Testing**           | Unit / Property / Integration| Kotest properties for domain invariants; JUnit; Swift XCTest; instrumented a11y/spoken; fuzzers. | `shared/src/commonTest/`, `app/src/test/`, `app/src/androidTest/`, `iosApp/UkuleleCompanionTests/`, fuzz/ dir |
| **Build & Quality**   | Gradle / CI / Hooks          | KMP, version catalog, pre-commit (ktlint + gitleaks), preflight script, detekt/ktlint baselines, GitHub Actions. | Root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `scripts/`, `.github/workflows/`, `ktlint-baseline.xml`, `detekt-baseline.xml` |
| **Rules & Tooling**   | AI / Editor Guidelines       | Mandatory coding rules for accessibility, KMP purity, coroutines, ViewModels.             | `.cursor/rules/*.mdc`, `AGENTS.md`, `CLAUDE.md` (multiple), `.claude/`, `scripts/hooks/` |
| **Documentation**     | User + Dev + Specs           | Per-platform manuals, feature video specs (TOML), architecture, privacy, attribution.     | `docs/manual/`, `docs/spec/`, `docs/videos/`, `docs/index.md`, `README.md`, `ATTRIBUTION.md`, `SECURITY.md`, `CONTRIBUTING.md` |
| **Assets**            | Audio / Models / Graphics    | Click sounds, ONNX model, app icons, screenshots.                                         | `app/src/main/res/raw/`, `iosApp/.../Resources/`, `store-listing/`, `docs/appstore-screenshots/` |

---

## 6. Build, CI, Scripts, and Supporting Code

| Area             | Sub-area                  | Description                                                                 | Key Locations |
|------------------|---------------------------|-----------------------------------------------------------------------------|---------------|
| **Build System** | Gradle / KMP / Versions   | Multi-module (shared + app), Kotlin 2.4, Compose BOM, ONNX, AGP 9.2.         | Root `build.gradle.kts`, `app/build.gradle.kts`, `shared/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties` |
| **CI / Quality** | GitHub Actions + Gates    | Lint, unit + instrumented tests, shared build, APK size, iOS build, ktlint ratchet. | `.github/workflows/*.yml`, `scripts/preflight.sh`, `scripts/ktlint.sh`, baselines |
| **Scripts & Hooks** | Automation + Guards     | Video assembly, release, string conversion, monkey test, pre-commit, constraint guards (no network/analytics in code). | `scripts/` (many .sh + .py), `scripts/hooks/`, `.git/hooks/` (installed via Gradle) |
| **Skills / Agents** | Automation for this repo | Release, parity audit, bug reproduce, add-translations, screenshot capture, etc. | `.cursor/skills/`, `.claude/skills/`, `.claude/commands/`, `.claude/plugins/` |
| **Tests (full)** | All test types            | Unit, property-based (Kotest), instrumented (Compose UI + a11y), Swift XCTest, UI monkey, fuzz. | All `*Test.kt`, `UkuleleCompanionTests/`, `UkuleleCompanionUITests/` |
| **Other**        | Resources & Config        | Audio assets, ONNX, Info.plist, proguard, play-service account (release), local.properties. | `app/src/main/res/`, `iosApp/.../Resources/`, `proguard-rules.pro`, `keystore.properties` (not committed), `Info.plist` |

---

**Notes on completeness**:
- Every file from the `find` + `list_dir` scans is accounted for under at least one area/sub-area.
- Feature areas map 1:1 to `NavSection` entries and the tab/sidebar groupings in the two main UI hosts.
- Technical areas cover 100% of `commonMain`, `androidMain`/`iosMain` actuals, `app/src/main`, and `iosApp/UkuleleCompanion` (excluding only generated `build/` trees).
- Cross-cutting concerns (a11y, i18n, neural, backup, tests) are called out explicitly because they touch many files.
- No "misc" bucket was needed; everything fit into the structure above.

This file can be regenerated by re-running a similar full scan if the codebase evolves.
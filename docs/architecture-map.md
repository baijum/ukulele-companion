# Architecture Map

Machine-readable navigation and ViewModel mapping for AI coding agents.
Consult this to understand which screen lives where and what backs it.

## Android Navigation

The app uses `ModalNavigationDrawer` (no Jetpack Navigation / NavHost).
Screen selection is via `NavSection` enum in a `when` block inside
`FretboardScreen.kt`. All ViewModels are scoped to the Activity.

Entry point: `ui/navigation/FretboardScreen.kt`

All 14 ViewModels are hoisted in `FretboardScreen()` via `viewModel()` and passed
down. `SettingsViewModel` is also created in `MainActivity` for theme.

### Play Section

| NavSection | Screen Composable | ViewModel(s) | File |
|------------|-------------------|--------------|------|
| EXPLORER | `ExplorerRoute` | `FretboardViewModel`, `SettingsViewModel` | `ui/navigation/ExplorerTab.kt` |
| TUNER | `TunerTab` | `TunerViewModel` | `ui/TunerTab.kt` |
| PITCH_MONITOR | `PitchMonitorTab` | `PitchMonitorViewModel` | `ui/PitchMonitorTab.kt` |
| METRONOME | `MetronomeTab` | `MetronomeViewModel` | `ui/MetronomeTab.kt` |
| LIBRARY | `ChordLibraryTab` | `ChordLibraryViewModel`, `FretboardViewModel`, `FavoritesViewModel` | `ui/ChordLibraryTab.kt` |
| FAVORITES | `FavoritesTab` | `FavoritesViewModel`, `FretboardViewModel` | `ui/FavoritesTab.kt` |

### Create Section

| NavSection | Screen Composable | ViewModel(s) | File |
|------------|-------------------|--------------|------|
| SONGWRITER_MODE | `SongwriterModeFlow` | `SongbookViewModel`, `CustomProgressionViewModel` | `ui/SongwriterModeFlow.kt` |
| SONGBOOK | `SongbookTab` | `SongbookViewModel`, `ChordLibraryViewModel`, `FretboardViewModel`, `MetronomeViewModel` | `ui/songbook/SongbookTab.kt` |
| SETLISTS | `SetlistTab` | `SetlistViewModel`, `SongbookViewModel` | `ui/SetlistTab.kt` |
| MELODY_NOTEPAD | `MelodyNotepadView` | `MelodyViewModel` | `ui/melody/MelodyNotepadView.kt` |
| PATTERNS | `StrumPatternsTab` | (inline repositories, no ViewModel) | `ui/patterns/StrumPatternsTab.kt` |
| PROGRESSIONS | `ProgressionsTab` | `CustomProgressionViewModel`, `FretboardViewModel` | `ui/ProgressionsTab.kt` |

### Learn Section

| NavSection | Screen Composable | ViewModel(s) | File |
|------------|-------------------|--------------|------|
| THEORY_LESSONS | `TheoryLessonsView` | `LearningProgressViewModel` | `ui/TheoryLessonsView.kt` |
| THEORY_QUIZ | `TheoryQuizView` | `LearningProgressViewModel` | `ui/TheoryQuizView.kt` |
| INTERVAL_TRAINER | `IntervalTrainerView` | `LearningProgressViewModel` | `ui/IntervalTrainerView.kt` |
| NOTE_QUIZ | `NoteQuizView` | `LearningProgressViewModel` | `ui/NoteQuizView.kt` |
| CHORD_EAR | `ChordEarTrainingView` | `LearningProgressViewModel` | `ui/ChordEarTrainingView.kt` |
| SCALE_PRACTICE | `ScalePracticeView` | `ScalePracticeViewModel`, `LearningProgressViewModel`, `SettingsViewModel` | `ui/ScalePracticeView.kt` |
| LEARNING_PROGRESS | `LearningProgressView` | `LearningProgressViewModel` + `PracticeTimerRepository` | `ui/LearningProgressView.kt` |
| DAILY_CHALLENGE | `DailyChallengeView` | (navigation hub via `onNavigate`) | `ui/DailyChallengeView.kt` |
| PRACTICE_ROUTINE | `PracticeRoutineView` | (navigation hub via `onNavigate`) | `ui/PracticeRoutineView.kt` |
| CHORD_TRANSITION | `ChordTransitionView` | `FretboardViewModel` | `ui/ChordTransitionView.kt` |
| PLAY_ALONG | `PlayAlongSetup` | `FretboardViewModel` | `ui/PlayAlongView.kt` |
| ACHIEVEMENTS | `AchievementsView` | `LearningProgressViewModel`, `SongbookViewModel`, `FavoritesViewModel` | `ui/AchievementsView.kt` |

### Reference Section

| NavSection | Screen Composable | ViewModel(s) | File |
|------------|-------------------|--------------|------|
| CAPO_GUIDE | `CapoGuideView` | (receives tuning from settings) | `ui/CapoGuideView.kt` |
| CIRCLE_OF_FIFTHS | `CircleOfFifthsView` | `ChordLibraryViewModel` (navigate on tap) | `ui/CircleOfFifthsView.kt` |
| CHORD_SUBS | `ChordSubstitutionsView` | (static/domain logic) | `ui/ChordSubstitutionsView.kt` |
| SCALE_CHORDS | `ScaleChordView` | (domain logic) | `ui/ScaleChordView.kt` |
| NOTE_MAP | `FretboardNoteMapView` | (receives tuning, lastFret) | `ui/FretboardNoteMapView.kt` |
| GLOSSARY | `GlossaryView` | (static) | `ui/GlossaryView.kt` |

### Cross-Cutting

| Component | File | Notes |
|-----------|------|-------|
| Root screen + when block | `ui/navigation/FretboardScreen.kt` | Lines 337-626 |
| Navigation drawer | `ui/navigation/DrawerContent.kt` | Renders drawer sections |
| Navigation constants | `ui/navigation/NavigationConstants.kt` | `DrawerItem`, `drawerSections()` |
| NavSection enum | `shared/.../data/NavSection.kt` | 31 values with stable IDs |
| Settings sheet | `ui/SettingsSheet.kt` | `SettingsViewModel`, `BackupRestoreViewModel` |
| Full-screen fretboard | `ui/FullScreenFretboard.kt` | Overlay from Explorer |
| Help view | `ui/HelpView.kt` | Footer drawer item |
| Onboarding | `ui/OnboardingScreen.kt` | First-launch flow |

## iOS Navigation

The app uses `TabView` with 4 tabs (Play, Create, Learn, Reference), each
containing a `NavigationStack`. iPad landscape uses `NavigationSplitView`
with a sidebar.

Entry point: `ContentView.swift`
App entry: `UkuleleCompanionApp.swift` (injects `PracticeTimerViewModel`)

Phone/iPad portrait uses `TabView`; iPad landscape uses `NavigationSplitView`
with sidebar. Learn and Reference tabs are gated by settings.

### Tab Structure

| Tab | Index | Container View | Contents | File |
|-----|-------|---------------|----------|------|
| Play | 0 | `PlayView` | Explorer, Tuner, Pitch Monitor, Metronome, Chord Library, Favorites | `Views/PlayView.swift` |
| Create | 1 | `CreateView` | Songwriter Mode, Songbook, Setlists, Melody Notepad, Patterns, Progressions | `Views/CreateView.swift` |
| Learn | 2 | `LearnView` | Theory, Quizzes, Interval Trainer, Scale Practice, Progress, etc. | `Views/LearnView.swift` |
| Reference | 3 | `ReferenceView` | Capo Guide, Circle of Fifths, Chord Substitutions, etc. | `Views/ReferenceView.swift` |

### Environment Object Injection

```
UkuleleCompanionApp → PracticeTimerViewModel (env)
ContentView → SettingsViewModel, LearnViewModel, FavoritesViewModel,
              SongbookViewModel, CustomPatternsViewModel,
              SetlistViewModel, MetronomeViewModel (all env)
```

### iOS ViewModels (17 total)

| ViewModel | Scope | What It Manages | File |
|-----------|-------|-----------------|------|
| `SettingsViewModel` | App-wide (env) | Preferences: sound, theme, tuning, fretboard display, tab visibility | `ViewModels/SettingsViewModel.swift` |
| `LearnViewModel` | App-wide (env) | All learning progress: theory, quizzes, intervals, streaks, achievements | `ViewModels/LearnViewModel.swift` |
| `FavoritesViewModel` | App-wide (env) | Saved chord voicings and folders | `ViewModels/FavoritesViewModel.swift` |
| `SongbookViewModel` | App-wide (env) | Songs CRUD, search, sort, labels | `ViewModels/SongbookViewModel.swift` |
| `CustomPatternsViewModel` | App-wide (env) | Custom strum and fingerpicking patterns | `ViewModels/CustomPatternsViewModel.swift` |
| `SetlistViewModel` | App-wide (env) | Setlists CRUD, song ordering | `ViewModels/SetlistViewModel.swift` |
| `MetronomeViewModel` | App-wide (env) | BPM, time signature, accents, playback | `ViewModels/MetronomeViewModel.swift` |
| `PracticeTimerViewModel` | App-wide (env) | Session time tracking, daily goals | `ViewModels/PracticeTimerViewModel.swift` |
| `FretboardViewModel` | Per-view (local) | Fretboard selections, chord detection, scale overlay, capo | `ViewModels/FretboardViewModel.swift` |
| `ChordLibraryViewModel` | Per-view (local) | Root/category/formula selection, voicing generation | `ViewModels/ChordLibraryViewModel.swift` |
| `TunerViewModel` | Per-view (local) | Live pitch detection, cents, string progress, ONNX status | `ViewModels/TunerViewModel.swift` |
| `PitchMonitorViewModel` | Per-view (local) | Pitch history, chord-from-audio, chroma energy | `ViewModels/PitchMonitorViewModel.swift` |
| `ScalePracticeViewModel` | Per-view (local) | Scale practice modes (play along, quiz, ear) | `ViewModels/ScalePracticeViewModel.swift` |
| `MelodyViewModel` | Per-view (local) | Melody composition, recording, playback | `ViewModels/MelodyViewModel.swift` |
| `ProgressionsViewModel` | Per-view (local) | Scale/root selection, preset & custom progressions | `ViewModels/ProgressionsViewModel.swift` |
| `ChordTransitionsViewModel` | Per-view (local) | Two-chord transition drill, metronome, BPM | `ViewModels/ChordTransitionsViewModel.swift` |
| `PlayAlongViewModel` | Per-view (local) | Progression play-along, mic detection, scoring | `ViewModels/PlayAlongViewModel.swift` |

### iPad Sidebar Destinations

`SidebarDestination` enum in `ContentView.swift` maps to individual views
via a `switch` in the detail view. This parallels Android's `NavSection` enum.
No dedicated Router file — navigation is declarative via `NavigationLink`.

## Shared Module (`shared/src/commonMain/`)

Pure Kotlin business logic consumed by both platforms.

| Package | Key Classes | Used By |
|---------|------------|---------|
| `domain/` | `PitchDetector`, `ChordDetector`, `ChordInfo`, `Transpose`, `TunerNoteMapper`, `FFTProcessor`, `AudioResampler`, `ScaleChordBuilder`, `ChordNameParser`, `Achievements` | Audio pipeline, chord library, tuner, theory features |
| `data/` | `NavSection`, `Notes`, `UkuleleTuning`, `ChordFormula`, `Scales`, `ChordSheet`, `ChordProParser` | Navigation, note/chord models, song formats |
| `platform/` | `generateUuid()`, `currentTimeMillis()`, `currentYear()`, `currentDayOfYear()` | Expect/actual for platform-specific utilities |

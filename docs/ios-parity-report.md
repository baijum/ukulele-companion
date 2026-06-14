# iOS Parity Report

Generated: 2026-03-08 (v3, post P0-P5 fixes) | Updated: 2026-06-08 (v4, post accessibility + performance) | Branch: `main`

Systematic comparison of the iOS port against the Android reference implementation using code-level structural analysis (Layer 1) and TOML-based feature verification (Layer 2).

## Summary

| Metric | Count |
|--------|-------|
| Top-level screens | 28 Android / 29 iOS (matched + FullScreenFretboardView) |
| ViewModels | 14 Android / 15 iOS (matched + PlayAlongViewModel) |
| Views | 54 Android / 47 iOS |
| TOML scenes verified | 49 |
| P0-P5 gaps fixed | 54 / 54 |

All 54 prioritized gaps from the initial audit (P0 through P5) have been resolved. Both platforms are at full feature parity across all screens, sub-features, and settings.

**Code-level reference:** The complete mapping of feature areas, navigation sections (NavSection), shared packages, platform-specific UI/ViewModels, and cross-cutting code (including the exact areas compared here) lives in [../CODEBASE_AREAS_SUBAREAS.md](../CODEBASE_AREAS_SUBAREAS.md).

### Post-parity updates (June 2026)

**Accessibility parity (#140):** Haptic in-tune feedback, reduce motion support, per-string chord diagram exploration, needle meter zone descriptions, color-only state differentiation fixes, and accessibility hints have been implemented on both platforms simultaneously.

**Performance parity (#141):** Frame-dropping backpressure, async ONNX model loading, and the "SwiftF0 Loading..." badge state are implemented identically on both platforms. Shared KMP buffer optimizations (PitchDetector, AudioResampler) benefit both platforms equally.

**Test coverage (#136, #137):** Both platforms now have ViewModel-level unit tests covering tuner arbitration, pitch monitor, metronome, fretboard, chord transitions, scale practice, melody, play-along, and neural pitch supervision.

---

## Completed Phases

| Phase | Items Fixed |
|-------|------------|
| **P0 (6)** | Polyphonic chord playback, scale overlay, capo simulation, tuner string buttons, chord library inversions, Play Along mic detection |
| **P1 (9)** | Chord ear training full playback, chord library tap-to-play, songbook chord chips, strum pattern play, progressions play + harmonic labels, chord transition selectors, left-handed mode |
| **P2 (10)** | High contrast theme, language picker, spoken feedback, needle meter, full-screen fretboard, noise gate, about links, explorer tips, bass/common highlight, favorites reorder |
| **P3 (9)** | Blitz mode, interval visual mode, note quiz tuning, scale practice loop/filter, melody rename, daily tip, practice routine, note map tap-to-play, arpeggio label |
| **P4 (8)** | Songbook auto-scroll/viewer, chord transitions diagrams/stats, favorites card enhancements, explorer chord interaction, melody recording polish, chord library transpose, strum patterns management, progressions management |
| **P5 (11)** | Tuner auto-start/neural badge/A4, interval trainer auto-level-up/harmonic fix, theory quiz "All" category/stats, note quiz stats, scale practice fretboard/categories, learning progress reset, chord diagram capo bar/inversion/favorite, settings auto-start/credits, capo guide tuning/"was" note, help Full Screen entry, fretboard note map tips |

---

## Layer 1: Feature Matrix (Code-Level Comparison)

### Play Section

| Feature | Android File | iOS File | Status |
|---------|-------------|----------|--------|
| Explorer (fretboard + detection) | FretboardScreen.kt, FretboardView.kt | ExplorerView.swift, FretboardView.swift | matched |
| Chord result display | ChordResultView.kt | ChordResultView.swift | matched |
| Fretboard grid | FretboardView.kt | FretboardView.swift | matched |
| Tuner | TunerTab.kt | TunerView.swift | matched |
| Pitch Monitor | PitchMonitorTab.kt | PitchMonitorView.swift | matched |
| Metronome | MetronomeTab.kt | MetronomeView.swift | matched |
| Chord Library | ChordLibraryTab.kt | ChordLibraryView.swift | matched |
| Favorites | FavoritesTab.kt | FavoritesView.swift | matched |

### Create Section

| Feature | Android File | iOS File | Status |
|---------|-------------|----------|--------|
| Songbook | SongbookTab.kt | SongbookView.swift | matched |
| Melody Notepad | MelodyNotepadView.kt | MelodyNotepadView.swift | matched |
| Strum Patterns | StrumPatternsTab.kt | StrumPatternsView.swift | matched |
| Progressions | ProgressionsTab.kt | ProgressionsView.swift | matched |

### Learn Section

| Feature | Android File | iOS File | Status |
|---------|-------------|----------|--------|
| Theory Lessons | TheoryLessonsView.kt | TheoryLessonsView.swift | matched |
| Theory Quiz | TheoryQuizView.kt | TheoryQuizView.swift | matched |
| Interval Trainer | IntervalTrainerView.kt | IntervalTrainerView.swift | matched |
| Note Quiz | NoteQuizView.kt | NoteQuizView.swift | matched |
| Chord Ear Training | ChordEarTrainingView.kt | ChordEarTrainingView.swift | matched |
| Scale Practice | ScalePracticeView.kt | ScalePracticeView.swift | matched |
| Learning Progress | LearningProgressView.kt | LearningProgressView.swift | matched |
| Daily Challenge | DailyChallengeView.kt | DailyChallengeView.swift | matched |
| Practice Routine | PracticeRoutineView.kt | PracticeRoutineView.swift | matched |
| Chord Transitions | ChordTransitionView.kt | ChordTransitionsView.swift | matched |
| Play Along | PlayAlongView.kt | PlayAlongView.swift | matched |
| Achievements | AchievementsView.kt | AchievementsView.swift | matched |

### Reference Section

| Feature | Android File | iOS File | Status |
|---------|-------------|----------|--------|
| Capo Guide | CapoGuideView.kt | CapoGuideView.swift | matched |
| Circle of Fifths | CircleOfFifthsView.kt | CircleOfFifthsView.swift | matched |
| Chord Substitutions | ChordSubstitutionsView.kt | ChordSubstitutionsView.swift | matched |
| Scale Chords | ScaleChordView.kt | ScaleChordsView.swift | matched |
| Fretboard Note Map | FretboardNoteMapView.kt | FretboardNoteMapView.swift | matched |
| Glossary | GlossaryView.kt | GlossaryView.swift | matched |

### Other

| Feature | Android File | iOS File | Status |
|---------|-------------|----------|--------|
| Settings | SettingsSheet.kt | SettingsView.swift | matched |
| Help | HelpView.kt | HelpView.swift | matched |
| Onboarding | OnboardingScreen.kt | OnboardingView.swift | matched |
| Full-Screen Fretboard | FullScreenFretboard.kt | FullScreenFretboardView.swift | matched |
| Chord Diagram | VerticalChordDiagram.kt | ChordDiagramView.swift | matched |

---

## Settings Comparison

| Section | Option | Android | iOS | Status |
|---------|--------|:-------:|:---:|--------|
| Sound | Master toggle, volume, note duration | yes | yes | matched |
| Sound | Strum delay, strum direction | yes | yes | matched |
| Sound | Play on tap | yes | yes | matched |
| Sound | Noise gate filtering | yes | yes | matched |
| Display | Theme (Light/Dark/System) | yes | yes | matched |
| Display | High Contrast theme | yes | yes | matched |
| Display | Show explorer tips | yes | yes | matched |
| Display | Show Learn/Reference sections | yes | yes | matched |
| Language | In-app language picker (16 locales) | yes | yes | matched |
| Tuning | 8 tuning presets | yes | yes | matched |
| Tuner | Precision mode | yes | yes | matched |
| Tuner | Auto advance | yes | yes | matched |
| Tuner | A4 reference frequency | yes | yes | matched |
| Tuner | Spoken feedback (TTS) | yes | yes | matched |
| Tuner | Auto start on open | yes | yes | matched |
| Fretboard | Left-handed mode | yes | yes | matched |
| Fretboard | Show note names | yes | yes | matched |
| Fretboard | Allow muted strings | yes | yes | matched |
| Fretboard | Last fret | yes | yes | matched |
| Backup | Export / Restore | yes | yes | matched |
| About | App name, version | yes | yes | matched |
| About | Website, free book, video guide | yes | yes | matched |
| About | Credits, tagline | yes | yes | matched |

---

## Matched Features (Full Parity)

All screens have full or near-full feature parity:

- Fretboard grid (scale overlay, capo, left-handed)
- Explorer (suggested chords, tap-to-library, tappable alternates)
- Tuner (auto-start, neural badge, A4 display, spoken feedback)
- Pitch Monitor (arpeggio label, noise gate)
- Metronome
- Chord Library (inversions, transpose, tap-to-play)
- Favorites (chord diagrams, play, share, reorder)
- Chord Ear Training (full chord playback)
- Songbook (auto-scroll, key detection, tappable chords, edit/preview)
- Melody Notepad (stabilization, feedback, chip-based editing)
- Strum Patterns (play, duplicate, edit, per-card BPM)
- Progressions (play, duplicate, progress bar, harmonic labels)
- Theory Lessons
- Theory Quiz (Blitz, "All" category, session/all-time stats)
- Interval Trainer (visual mode, auto-level-up, harmonic playback)
- Note Quiz (tuning-aware, session/all-time stats)
- Scale Practice (fretboard toggle, category filter, loop)
- Learning Progress (reset button)
- Daily Challenge (Tip of Day)
- Practice Routine (guided step flow)
- Chord Transitions (diagrams, playback, tap tempo, stats)
- Play Along (mic detection, scoring)
- Achievements
- Circle of Fifths
- Chord Substitutions
- Scale Chords
- Glossary
- Full-Screen Fretboard
- Capo Guide (tuning-aware, "was" note)
- Fretboard Note Map (tips card, tap-to-play)
- Chord Diagram (capo bar, inversion label, favorite icon)
- Settings (all options matched)
- Help (Full Screen Mode entry)
- Backup & Restore
- Onboarding

---

## iOS-Only Features

Features that exist on iOS but not Android:

| Feature | iOS Location | Notes |
|---------|-------------|-------|
| Favorites: exportData/importData | FavoritesViewModel.swift | Per-feature export (Android uses global backup) |
| Songbook: transpose, exportChordPro | SongbookViewModel.swift | iOS has inline transpose |
| Melody: exportData/importData | MelodyViewModel.swift | Per-feature export |
| Progressions: presets, diatonic degrees, shareText | ProgressionsViewModel.swift | Richer progression browsing |
| MetronomeViewModel: tapTempo function | MetronomeViewModel.swift | Android uses inline logic |

---

## How to Re-Run This Audit

Use the [platform-parity-audit](.cursor/skills/platform-parity-audit/SKILL.md) skill to regenerate this report after making changes. The skill provides a three-layer workflow:

1. **Layer 1 (Code)**: Compare ViewModels and Views between platforms
2. **Layer 2 (TOML)**: Verify docs/videos/ scene features in iOS code
3. **Layer 3 (Visual)**: Capture side-by-side screenshots (requires running devices)

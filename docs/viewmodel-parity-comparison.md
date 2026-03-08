# Android vs iOS ViewModel/View Parity Comparison

**Learn, Reference, and Settings features — Ukulele Companion**

This document compares the public API surface and functional behavior of Android and iOS implementations for each ViewModel/View pair. Focus is on **functional differences** (features, options, behaviors), not cosmetic naming.

---

## 1. LearningProgressViewModel (Android) vs LearnViewModel (iOS)

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **State model** | `LearningProgressState` (StateFlow) with aggregated stats | `stateVersion` (Int) for refresh; no aggregated state object | **partial** |
| **Theory lessons** | `markLessonCompleted`, `isLessonCompleted`, `markLessonQuizPassed`, `isLessonQuizPassed` | Same | **matched** |
| **Theory quiz** | `recordQuizAnswer`, `quizStats(category?)` | Same | **matched** |
| **Interval trainer** | `recordIntervalAnswer`, `intervalStats(level?)` | Same | **matched** |
| **Note quiz** | `recordNoteQuizAnswer`, `noteQuizStats()` | Same | **matched** |
| **Chord ear** | `recordChordEarAnswer`, `chordEarStats(level?)` | Same | **matched** |
| **Scale practice** | `recordScalePracticeAnswer`, `scalePracticeStats(mode?)` | Same | **matched** |
| **Daily streak** | `currentDayStreak()`, `bestDayStreak()` via state | Same via `recordActivity()`, `currentDayStreak()`, `bestDayStreak()` | **matched** |
| **Achievements** | Not in LearningProgressViewModel; handled by AchievementChecker + separate state | `unlockedAchievementIds`, `unlockAchievement()` | **partial** — iOS has achievements in LearnViewModel |
| **Reset** | `clearAllProgress()` | Missing | **missing-ios** |
| **Backup export/import** | Via LearningProgressRepository (used by BackupRestoreManager) | `exportProgress()`, `importProgress()` in LearnViewModel | **matched** |
| **completedLessonCount / passedQuizCount** | Computed in state | `completedLessonCount()`, `passedQuizCount()` | **matched** |

**Summary:** Android has `clearAllProgress()`; iOS does not. Android exposes a rich `LearningProgressState`; iOS uses `stateVersion` for invalidation. Otherwise functionally matched.

---

## 2. ScalePracticeViewModel (Android) vs ScalePracticeViewModel (iOS)

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Modes** | Play Along, Quiz, Ear Training | Same | **matched** |
| **Play Along** | BPM, direction (asc/desc/both), loop, fret position filter (ALL/OPEN/MID/HIGH), show fretboard | BPM, direction; no loop, no fret position, no fretboard toggle | **partial** |
| **Settings restore** | `restoreSettings(ScalePracticeSettings)`, `currentSettings()` | None — no persistence | **missing-ios** |
| **Quiz** | `generateQuizQuestion`, `submitQuizAnswer` (returns correct), in-session stats | Same; delegates to LearnViewModel for persistence | **matched** |
| **Ear training** | `generateEarQuestion`, `playEarScale`, `submitEarAnswer` | Same | **matched** |
| **Play Along playback** | MetronomeEngine + ToneGenerator, chord playback on beat | Timer + TonePlayer, note sequence | **matched** (different impl) |
| **Fret position filter** | ALL, OPEN, MID, HIGH for fretboard overlay | None | **missing-ios** |
| **Loop playback** | `toggleLoop()` | None | **missing-ios** |
| **Show fretboard** | `toggleFretboard()` | None | **missing-ios** |

**Summary:** iOS missing: settings persistence, loop playback, fret position filter, show fretboard toggle.

---

## 3. BackupRestoreViewModel (Android) vs BackupRestoreManager (iOS)

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Architecture** | Dedicated ViewModel with `BackupRestoreState` (Idle/Exporting/Importing/Success/Error) | `BackupRestoreManager` (ObservableObject) embedded in SettingsView | **partial** |
| **Export** | `exportBackup(uri)` via SAF; `lastBackupDate` in state | `buildBackupData()` → FileDocument; `recordBackupDate()` | **matched** |
| **Import** | `importBackup(uri)` via SAF | `restoreFromURL(url)` via document picker | **matched** |
| **Data scope** | Favorites, folders, chord sheets, progressions, strum/fingerpick patterns, melodies, learning progress, settings | Same + practice timer, custom patterns | **matched** (iOS may include more) |
| **Operation state** | `resetState()` to clear Success/Error | No explicit reset | **partial** |
| **Error handling** | Success/Error messages in state | Silent on failure (guard returns) | **partial** |

**Summary:** Both support full backup/restore. Android has explicit operation state and error UI; iOS uses document picker and may fail silently.

---

## 4. TheoryLessonsView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Structure** | Modules → lessons; progress bar; lesson detail with key points + mini quiz | Same (List sections) | **matched** |
| **Progress display** | Overall progress bar (completed/total) | Quiz passed / completed icons only | **partial** |
| **Lesson completion** | `markLessonCompleted` on back | `markLessonCompleted` on appear | **matched** |
| **Quiz** | Mini quiz with options, correct/incorrect feedback, explanation | Same | **matched** |
| **Module names** | `theoryModuleName()` for localized names | Raw module strings | **partial** |

**Summary:** Functionally matched; Android has richer progress display and localized module names.

---

## 5. TheoryQuizView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Modes** | Standard + **Blitz** (60s timed, +3s per correct) | Standard only | **missing-ios** |
| **Category** | All + Intervals, Chords, Keys, Scales, Progressions | Same categories | **matched** |
| **Session stats** | Score, accuracy, streak, best | Score, total, overall accuracy | **partial** |
| **All-time stats** | Yes | Yes (overall %) | **matched** |
| **Persistence** | `recordQuizAnswer` | Same | **matched** |
| **"All" category** | `selectedCategory == null` | Not shown; defaults to first | **partial** |

**Summary:** **Blitz mode** (timed quiz with bonus seconds) is Android-only. iOS missing Blitz.

---

## 6. IntervalTrainerView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Modes** | **Visual** (notes shown) + **Audio** (play interval, hide until answered) | Audio-only (play interval, note1 -> ?) | **missing-ios** |
| **Direction** | Ascending, Descending, Harmonic (audio mode) | Up, Down, Both (harmonic) | **matched** |
| **Level** | 1–4 (Easy/Medium/Hard/Expert) | Same | **matched** |
| **Session stats** | Correct, accuracy, streak, best | Score, total | **partial** |
| **All-time stats** | Yes | No | **missing-ios** |
| **Auto-level up** | After 5 correct in a row | No | **missing-ios** |
| **Semitone hint** | Visual mode shows "X semitones apart" | No | **partial** |
| **Replay** | Replay button in audio mode | Play Interval button | **matched** |

**Summary:** Android has Visual mode, all-time stats, auto-level up. iOS is audio-only with simpler UI.

---

## 7. NoteQuizView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Modes** | Name It, Find It | Same | **matched** |
| **Difficulty** | Easy, Medium, Hard (1–3) | Same | **matched** |
| **Tuning** | Passed as parameter (supports High-G/Low-G) | Hardcoded `stringOpenNotes` (High-G) | **missing-ios** |
| **Session stats** | Correct, accuracy, streak, best | Score, total | **partial** |
| **All-time stats** | Yes | No | **missing-ios** |

**Summary:** iOS uses fixed High-G tuning; Android respects app tuning. iOS missing all-time stats.

---

## 8. ChordEarTrainingView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Level** | 1–4 | Same | **matched** |
| **Chord playback** | Full chord via ToneGenerator.playChord (strum) | Single root note only | **missing-ios** |
| **Session stats** | Correct, accuracy, streak, best | Score, total | **partial** |
| **All-time stats** | Yes | No | **missing-ios** |
| **Auto-level up** | After 5 correct in a row | No | **missing-ios** |
| **Full chord name** | Shown after answer | Root + options only | **partial** |

**Summary:** iOS plays only root note, not full chord — significant functional gap. Missing all-time stats and auto-level.

---

## 9. DailyChallengeView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Challenges** | 3 from `DailyChallengeGenerator.today()` | Same | **matched** |
| **Completion** | Navigate via "Go" button (onNavigate) | "Mark Complete" + UserDefaults persistence | **partial** |
| **Tip of the day** | Rotating tips (7 tips by day-of-year) | None | **missing-ios** |
| **Persistence** | No completion tracking | `saveDayProgress`, `loadDayProgress` per day | **partial** — iOS tracks completion, Android does not |
| **recordActivity** | Not used | `learnVM.recordActivity()` on complete | **partial** |

**Summary:** Android has Tip of the day; iOS has completion tracking. Different design choices.

---

## 10. PracticeRoutineView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Setup** | Duration slider (5–60 min), skill level, focus areas | Duration picker (10/15/20/30), level, focus toggles | **partial** |
| **Duration** | 5–60 min (slider) | 10, 15, 20, 30 only | **missing-ios** |
| **Active routine** | Step-by-step with "Go" + "Done", progress bar, reset | List of steps only; no step completion flow | **missing-ios** |
| **Navigation** | `onNavigate` to app sections per step | None | **missing-ios** |
| **Completion** | Completion card when all steps done | None | **missing-ios** |

**Summary:** Android has full guided routine with step completion and navigation; iOS only shows a generated list.

---

## 11. ChordTransitionView (Android) vs ChordTransitionsView (iOS)

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Chord input** | Root + quality selector (C, G, Am, etc.) with visual chord diagrams | Text fields "Chord 1", "Chord 2" | **partial** |
| **Chord diagrams** | VerticalChordDiagram for both chords | None | **missing-ios** |
| **Metronome** | MetronomeEngine, BPM slider, tap tempo, beats per chord (1/2/4/8) | Timer, BPM slider, beats (2/4/8) | **matched** |
| **Play chord on beat** | `onPlayVoicing` callback | No chord playback | **missing-ios** |
| **Session stats** | Switches, elapsed time, switches/min | Transition count only | **partial** |
| **Swap chords** | Swap button | None | **missing-ios** |
| **Tuning/left-handed** | Passed from settings | Not configurable | **partial** |

**Summary:** Android has chord diagrams, chord playback, swap, richer stats. iOS uses text input and no diagrams.

---

## 12. PlayAlongView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Setup** | Key, scale type, progression selection | Same | **matched** |
| **Real-time feedback** | **Microphone + chord detection** (AudioChordDetector), correct/incorrect feedback | None — metronome only | **missing-ios** |
| **Score** | Accuracy %, grade, best streak (PlayAlongScorer) | None | **missing-ios** |
| **Mic permission** | Request + UI when denied | N/A | **partial** |
| **Chord playback** | Plays voicing on each chord change | None | **missing-ios** |
| **Tap tempo** | Yes | No | **missing-ios** |

**Summary:** Android has full play-along with mic detection and scoring; iOS is metronome-only, no feedback.

---

## 13. AchievementsView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Source** | `AchievementChecker` (shared/KMP) + `Achievement` with `icon` (ImageVector) | `AchievementDef` (Swift) + SF Symbol names | **partial** |
| **Categories** | AchievementCategory (KMP) | AchievementCategorySwift | **matched** |
| **Context** | `AchievementContext` from LearningProgressState + songs/favorites counts | `AchievementContextSwift` from LearnViewModel + UserDefaults | **matched** |
| **Unlock** | AchievementChecker checks; unlockedIds from progress ViewModel | `checkNewAchievements` in view; `learnVM.unlockAchievement` | **matched** |
| **Filter** | By category | By category | **matched** |
| **AchievementSummaryCard** | Compact card for Progress screen | None | **partial** |

**Summary:** Functionally matched; different implementations (KMP vs Swift). Android has AchievementSummaryCard for embedding.

---

## 14. CapoGuideView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Sections** | What is a capo, How it changes pitch, Common positions, When to use, Try it yourself | What is a capo, How it changes pitch, When to use | **partial** |
| **Interactive fret** | Fret selector 0–12, effective pitches | 0–8 | **partial** |
| **Common positions table** | Yes | Folded into "When to use" | **partial** |
| **Tuning** | Passed (UkuleleTuning) | Hardcoded standard | **partial** |

**Summary:** Android has more sections and higher fret range; otherwise matched.

---

## 15. CircleOfFifthsView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Major/Minor** | Toggle | Picker | **matched** |
| **Interactive circle** | Tap key, detail panel | Same | **matched** |
| **Detail** | Key signature, relative minor, diatonic chords | Same | **matched** |
| **onChordTapped** | Callback for chord selection | Not exposed | **partial** |

**Summary:** Matched; Android exposes chord tap callback.

---

## 16. ChordSubstitutionsView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Key selector** | 12 keys | Same | **matched** |
| **Categories** | SubstitutionCategory from ChordSubstitutions | Same | **matched** |
| **Examples** | Original → substitution with explanation | Same | **matched** |

**Summary:** **matched**

---

## 17. ScaleChordView (Android) vs ScaleChordsView (iOS)

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Root + scale** | Selector | Same | **matched** |
| **Diatonic triads** | Roman numeral, chord name, quality, notes | Same | **matched** |

**Summary:** **matched**

---

## 18. FretboardNoteMapView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Tuning** | Passed (UkuleleTuning) | Hardcoded High-G | **missing-ios** |
| **High-G / Low-G toggle** | When applicable | None | **missing-ios** |
| **Highlight note** | Filter by pitch class | Same | **matched** |
| **Tap to play** | ToneGenerator.playNote | None | **missing-ios** |
| **lastFret** | Configurable | Fixed 12 | **partial** |

**Summary:** Android has tuning param, High/Low-G toggle, tap-to-play. iOS is fixed High-G, no sound.

---

## 19. GlossaryView

| Aspect | Android | iOS | Status |
|--------|---------|-----|--------|
| **Search** | OutlinedTextField | `.searchable` | **matched** |
| **Expandable** | Expand/collapse term | Same | **matched** |
| **Example** | Shown when expanded | Same | **matched** |
| **Grouping** | Alphabetical | Same | **matched** |

**Summary:** **matched**

---

## 20. SettingsViewModel (Android) vs SettingsViewModel (iOS)

| Setting | Android | iOS | Status |
|---------|---------|-----|--------|
| **Sound** | | | |
| soundEnabled | ✓ | ✓ | matched |
| volume | ✓ | ✓ | matched |
| noteDurationMs | ✓ | ✓ | matched |
| strumDelayMs | ✓ | ✓ | matched |
| strumDown | ✓ | ✓ | matched |
| playOnTap | ✓ | ✓ | matched |
| noiseGateFiltering | ✓ | ✓ | matched |
| **Display** | | | |
| themeMode | ✓ | ✓ | matched |
| showExplorerTips | ✓ (show_tips) | showTips | matched |
| showLearnSection | ✓ | showLearnTab | matched |
| showReferenceSection | ✓ | showReferenceTab | matched |
| **Tuning** | ✓ | ✓ | matched |
| **Fretboard** | | | |
| leftHanded | ✓ | ✓ | matched |
| lastFret | ✓ | ✓ | matched |
| showNoteNames | ✓ | ✓ | matched |
| allowMutedStrings | ✓ | allowMuted | matched |
| **Scale Practice** | | | |
| lastRoot, lastScale, lastCategory | ✓ | — | **missing-ios** |
| lastBpm, lastMode | ✓ | — | **missing-ios** |
| showFretboard | ✓ | — | **missing-ios** |
| **Tuner** | | | |
| spokenFeedback | ✓ | — | **missing-ios** |
| precisionMode | ✓ | ✓ | matched |
| a4Reference | ✓ | ✓ | matched |
| autoAdvance | ✓ | ✓ | matched |
| autoStart | ✓ | — | **missing-ios** |
| **Onboarding** | ✓ | ✓ | matched |
| **explorerTipsDismissed** | ✓ | — | **partial** (showTips may cover) |

**Summary:** iOS missing: Scale Practice persistence, tuner spoken feedback, tuner auto-start. Android has more granular display toggles (explorer tips vs learn/reference sections).

---

## Summary Table

| # | Component | Status | Key Gaps |
|---|-----------|--------|----------|
| 1 | LearningProgress vs LearnViewModel | partial | iOS: no `clearAllProgress` |
| 2 | ScalePracticeViewModel | partial | iOS: no settings persistence, loop, fret position, fretboard |
| 3 | BackupRestore | partial | iOS: no explicit error/state UI |
| 4 | TheoryLessonsView | partial | iOS: simpler progress display |
| 5 | TheoryQuizView | partial | **iOS: no Blitz mode** |
| 6 | IntervalTrainerView | partial | **iOS: no Visual mode**, no all-time stats, no auto-level |
| 7 | NoteQuizView | partial | iOS: fixed tuning, no all-time stats |
| 8 | ChordEarTrainingView | partial | **iOS: plays root only, not full chord**; no all-time stats |
| 9 | DailyChallengeView | partial | Android: Tip of day; iOS: completion tracking |
| 10 | PracticeRoutineView | partial | **iOS: no guided step flow**, limited duration options |
| 11 | ChordTransitionView | partial | **iOS: no chord diagrams**, no playback, no swap |
| 12 | PlayAlongView | partial | **iOS: no mic detection**, no scoring, no chord playback |
| 13 | AchievementsView | matched | — |
| 14 | CapoGuideView | partial | Android: more sections, tuning param |
| 15 | CircleOfFifthsView | matched | — |
| 16 | ChordSubstitutionsView | matched | — |
| 17 | ScaleChordView | matched | — |
| 18 | FretboardNoteMapView | partial | **iOS: fixed tuning**, no tap-to-play, no High/Low-G |
| 19 | GlossaryView | matched | — |
| 20 | SettingsViewModel | partial | iOS: no Scale Practice, tuner spoken/auto-start |

---

## Priority Gaps (iOS missing significant features)

1. **PlayAlongView** — No mic detection, no real-time feedback, no scoring
2. **ChordEarTrainingView** — Plays root note only instead of full chord
3. **TheoryQuizView** — No Blitz mode
4. **IntervalTrainerView** — No Visual mode, no all-time stats
5. **PracticeRoutineView** — No guided step completion flow
6. **ChordTransitionView** — No chord diagrams, no playback
7. **ScalePracticeViewModel** — No settings persistence, loop, fret position
8. **FretboardNoteMapView** — Fixed tuning, no tap-to-play
9. **SettingsViewModel** — No Scale Practice persistence, tuner spoken/auto-start

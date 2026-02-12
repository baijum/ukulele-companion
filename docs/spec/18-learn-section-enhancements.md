# Feature: Learn Section Enhancements — Gamification, Quiz Improvements & Cross-Linking

**Status: PROPOSED**

## Summary

A collection of enhancements to the Learn section that improve engagement, add variety, and connect the three learning features (Theory Lessons, Theory Quiz, Interval Trainer) into a cohesive learning experience. Includes gamification elements (achievements, daily streaks, timed challenges), expanded quiz question types, interactive lesson demos, cross-feature navigation, and two new training modes (Fretboard Note Quiz, Chord Ear Training).

## Motivation

- The three Learn features are currently siloed — no links between lessons, quizzes, and the interval trainer
- The Theory Quiz generates only one question template per category; adding variety prevents repetitiveness
- Leading apps (Solfy, Tomus, Note Quest, NoteQuiz) use gamification (streaks, badges, timed modes) to drive daily engagement
- The Theory Lessons are text-only despite the app having rich interactive components (fretboard, chord diagrams, scale overlays) that could be embedded
- A Fretboard Note Quiz and Chord Ear Training mode would round out the Learn section as a comprehensive music education tool

---

## Feature A: Cross-Feature Linking

### Problem

The Learn section features don't reference each other. A user reading the Intervals lesson has no way to jump to the Interval Trainer. A user finishing a quiz has no suggestion to explore related lessons.

### Proposed Solution

Add contextual navigation links between features:

#### From Theory Lessons
- Intervals lesson → "Practice this: Interval Trainer" button
- Chords lesson → "Explore chords: Chord Library" link
- Keys & Signatures lesson → "See it: Circle of Fifths" link
- Scales lesson → "Try it: Scale Overlay" link
- After completing a lesson's mini quiz → "Test yourself more: Theory Quiz" with the relevant category pre-selected

#### From Theory Quiz
- After answering a question → "Learn more: [Relevant Lesson]" link in the explanation area
- Category selection screen → "Study first" link to the matching lesson module

#### From Interval Trainer
- Header area → "What are intervals? Learn" link to the Intervals lesson
- After a wrong answer → Brief tooltip: "An interval is the distance between two notes"

### Technical Approach

```kotlin
// Navigation callbacks already exist in FretboardScreen.kt
// Add optional navigation parameters to Learn views:

@Composable
fun TheoryLessonsView(
    onNavigateToIntervalTrainer: () -> Unit,
    onNavigateToQuiz: (QuizCategory?) -> Unit,
    onNavigateToChordLibrary: () -> Unit,
    onNavigateToScaleOverlay: () -> Unit,
    onNavigateToCircleOfFifths: () -> Unit,
)

// In lesson detail, add contextual action buttons:
@Composable
fun LessonActionButton(lesson: TheoryLesson) {
    when (lesson.module) {
        "Intervals" -> Button("Practice: Interval Trainer") { onNavigateToIntervalTrainer() }
        "Chords" -> Button("Explore: Chord Library") { onNavigateToChordLibrary() }
        // ...
    }
}
```

### Files

- **Modify:** `ui/TheoryLessonsView.kt` — Add navigation action buttons per lesson
- **Modify:** `ui/TheoryQuizView.kt` — Add "Learn more" links in answer explanations
- **Modify:** `ui/IntervalTrainerView.kt` — Add "What are intervals?" link
- **Modify:** `ui/FretboardScreen.kt` — Wire up cross-navigation callbacks

### Effort Estimate

- **Complexity**: Low
- **Estimated time**: 1 day

---

## Feature B: Expanded Quiz Question Templates

### Problem

Each quiz category currently generates only one question format. After a few rounds, the questions feel repetitive even though the specific notes/chords change.

### Proposed Solution

Add 2–3 question templates per category for variety:

#### Intervals (currently: "What interval is X to Y?")
- **New:** "How many semitones in a [interval name]?" → Answer: number
- **New:** "Name the interval: [N] semitones" → Answer: interval name
- **New:** "What note is a [interval] above [note]?" → Answer: note name

#### Chords (currently: "What chord type has formula X?")
- **New:** "What notes are in [Root] [Quality]?" → Answer: note list (e.g., "D, F, A")
- **New:** "How many notes in a [chord type]?" → Answer: 3 or 4
- **New:** "What is the formula of a [quality] chord?" → Answer: formula string

#### Keys (currently: "What is the key signature of X major?")
- **New:** "What is the relative minor of [key] major?" → Answer: key name
- **New:** "Which key has [N] sharps?" → Answer: key name
- **New:** "What is the relative major of [key] minor?" → Answer: key name

#### Scales (currently: "How many notes in the X scale?")
- **New:** "Which scale has the pattern [W-H-W-...]?" → Answer: scale name
- **New:** "What mode starts on the [Nth] degree of major?" → Answer: mode name
- **New:** "What is the [Nth] note of the [key] major scale?" → Answer: note name

#### Progressions (currently: "What is the [numeral] chord in X major?")
- **New:** "In the key of [key], what Roman numeral is [chord]?" → Answer: numeral
- **New:** "Is the [numeral] chord in a major key major or minor?" → Answer: quality
- **New:** "Name the progression: I-V-vi-IV" → Answer: progression name

### Technical Approach

```kotlin
// In QuizGenerator.kt, each category randomly picks a template:
private fun generateIntervalQuestion(): QuizQuestion {
    return when ((0..2).random()) {
        0 -> generateIntervalNameQuestion()      // existing
        1 -> generateSemitoneCountQuestion()     // new
        2 -> generateNoteAboveQuestion()          // new
        else -> generateIntervalNameQuestion()
    }
}
```

### Files

- **Modify:** `domain/QuizGenerator.kt` — Add new question generator methods per category

### Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 2 days

---

## Feature C: Timed Challenge / Blitz Mode

### Problem

The Theory Quiz is untimed and open-ended. There's no urgency or competitive element to drive engagement.

### Proposed Solution

Add a "Blitz Mode" to the Theory Quiz: answer as many questions correctly as possible in 60 seconds.

#### Rules
- 60-second countdown timer displayed prominently
- Questions from all categories (or a selected category)
- Each correct answer scores 1 point; incorrect answers score 0 (no penalty)
- A 3-second bonus for each correct answer (extends the timer, rewarding streaks)
- Final score displayed with high score tracking
- Difficulty increases as score rises (narrower answer options, harder categories)

#### UI

```
┌─────────────────────────────┐
│  [*Standard*]   [ Blitz ]   │  ← Mode toggle
│                             │
│        ⏱️ 0:47              │  ← Countdown timer
│        Score: 12            │  ← Current score
│                             │
│  What interval is C to G?   │
│                             │
│  ┌──────┐  ┌──────┐        │
│  │  M3  │  │  P5  │        │  ← Tap to answer
│  └──────┘  └──────┘        │
│  ┌──────┐  ┌──────┐        │
│  │  m3  │  │  P4  │        │
│  └──────┘  └──────┘        │
│                             │
│     Best: 18 (All-time)     │
└─────────────────────────────┘
```

After time runs out:
- "Time's Up!" overlay with final score, accuracy, and whether it's a new high score
- "Play Again" and "Back to Standard" buttons

### Technical Approach

```kotlin
// Add to TheoryQuizView.kt:
var isBlitzMode by remember { mutableStateOf(false) }
var timeRemainingMs by remember { mutableLongStateOf(60_000L) }
var blitzScore by remember { mutableIntStateOf(0) }

// Countdown using LaunchedEffect:
LaunchedEffect(isBlitzMode) {
    if (isBlitzMode) {
        while (timeRemainingMs > 0) {
            delay(100)
            timeRemainingMs -= 100
        }
        // Time's up — show results
    }
}
```

### Files

- **Modify:** `ui/TheoryQuizView.kt` — Add Blitz mode toggle, timer, auto-advance on answer

### Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 1–2 days

---

## Feature D: Interactive Demos in Theory Lessons

### Problem

Theory Lessons are text-only. The PRD proposed embedding interactive components (fretboard, chord diagrams, scale overlays) but this was never implemented. Text-only lessons miss the opportunity to show concepts visually on the instrument.

### Proposed Solution

Add an optional interactive demo section to each lesson, using existing app composables:

| Lesson | Demo Type | What It Shows |
|--------|-----------|---------------|
| The 12 Notes | Fretboard | All 12 notes highlighted on the fretboard |
| Sharps and Flats | Fretboard | Highlight enharmonic pairs (C#/Db, etc.) |
| What Is an Interval? | Interval Visual | Two notes with distance shown (reuse Interval Trainer visual) |
| The 12 Interval Names | Interval Visual | Cycle through intervals with tap |
| The Major Scale | Scale Overlay | C major scale on fretboard |
| Minor Scales | Scale Overlay | A minor scale on fretboard |
| Modes | Scale Overlay | Dorian scale on fretboard |
| Triads | Chord Diagram | C major chord diagram |
| Seventh Chords | Chord Diagram | Cmaj7 chord diagram |
| What Is a Key? | Circle of Fifths | Highlight C major on circle |
| The Circle of Fifths | Circle of Fifths | Interactive circle (link to full view) |
| Diatonic Harmony | Progression | I-IV-V-I in C major |
| Chord Functions | Progression | Color-coded T/S/D labels |
| Time Signatures | Strum Pattern | 4/4 strumming pattern |
| Note Values | Strum Pattern | Show note duration differences |

### Technical Approach

Add a `demoType` field to `TheoryLesson`:

```kotlin
enum class LessonDemoType {
    NONE,
    FRETBOARD_NOTES,
    SCALE_OVERLAY,
    CHORD_DIAGRAM,
    INTERVAL_VISUAL,
    CIRCLE_OF_FIFTHS,
    PROGRESSION,
    STRUM_PATTERN,
}

data class TheoryLesson(
    // ... existing fields ...
    val demoType: LessonDemoType = LessonDemoType.NONE,
    val demoConfig: Map<String, String> = emptyMap(),
    // e.g., demoConfig = mapOf("root" to "0", "scale" to "Major")
)
```

In `TheoryLessonsView`, render the appropriate composable based on `demoType`:

```kotlin
when (lesson.demoType) {
    SCALE_OVERLAY -> MiniScaleOverlay(
        root = lesson.demoConfig["root"]?.toInt() ?: 0,
        scaleName = lesson.demoConfig["scale"] ?: "Major",
    )
    CHORD_DIAGRAM -> MiniChordDiagram(
        chordName = lesson.demoConfig["chord"] ?: "C",
    )
    // ...
}
```

### Files

- **Modify:** `data/TheoryLessons.kt` — Add `demoType` and `demoConfig` fields to each lesson
- **Modify:** `ui/TheoryLessonsView.kt` — Render interactive demo based on type
- **Possibly new:** `ui/components/MiniScaleOverlay.kt`, `MiniChordDiagram.kt` — Simplified versions of existing composables sized for embedding in lessons

### Effort Estimate

- **Complexity**: Medium–High
- **Estimated time**: 3–4 days

---

## Feature E: Fretboard Note Quiz

### Problem

Learning note positions on the fretboard is a fundamental skill for any string instrument player. The app has a full interactive fretboard but never challenges users to identify notes by position.

### Proposed Solution

A "Note Quiz" mode accessible from the Learn section:

#### Mode 1: Name the Note
- A position is highlighted on the fretboard (specific string + fret)
- User selects the note name from 4 options
- Progressive difficulty: open strings → frets 1–5 → frets 1–12

#### Mode 2: Find the Note
- A note name is displayed (e.g., "Find D on the A string")
- User taps the correct fret position on the fretboard
- Multiple correct positions possible (e.g., D appears on fret 7 of the G string and fret 2 of the C string)

#### UI Layout

```
┌─────────────────────────────┐
│  [*Name It*]   [ Find It ]  │  ← Mode toggle
│                             │
│  Easy  Medium  Hard         │  ← Difficulty
│                             │
│  ┌───┬───┬───┬───┬───┐     │
│  │ G │   │   │   │ ● │     │  ← Fretboard with
│  │ C │   │   │   │   │     │     highlighted position
│  │ E │   │   │   │   │     │
│  │ A │   │   │   │   │     │
│  └───┴───┴───┴───┴───┘     │
│                             │
│  What note is this?         │
│                             │
│  [ C ]  [ D ]  [*E*]  [ F ]│  ← Answer options
│                             │
│  Correct: 8  Accuracy: 80% │
└─────────────────────────────┘
```

### Technical Approach

```kotlin
data class NoteQuizQuestion(
    val string: Int,          // 0–3 (G, C, E, A for standard tuning)
    val fret: Int,            // 0–12
    val correctNote: String,
    val options: List<String>,
    val correctIndex: Int,
)

object NoteQuizGenerator {
    // String open notes for standard tuning (high-G)
    private val STRING_NOTES = listOf(7, 0, 4, 9) // G, C, E, A

    fun generate(maxFret: Int = 12): NoteQuizQuestion {
        val string = (0..3).random()
        val fret = (0..maxFret).random()
        val pitchClass = (STRING_NOTES[string] + fret) % 12
        val correctNote = Notes.pitchClassToName(pitchClass)
        // Generate 3 wrong answers from nearby notes...
    }
}
```

### Files

- **New:** `domain/NoteQuizGenerator.kt` — Question generation for note identification
- **New:** `ui/NoteQuizView.kt` — Fretboard-based quiz UI
- **Modify:** `ui/FretboardScreen.kt` — Add "Note Quiz" to Learn section in drawer

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 3 days

---

## Feature F: Chord Ear Training

### Problem

Beyond intervals, identifying chord types by ear is the next essential ear training skill. The app has `ChordFormulas` and `ToneGenerator` but no chord ear training mode.

### Proposed Solution

A "Chord Ear Training" mode:

1. Play a chord (3–4 notes strummed) using `ToneGenerator`
2. User identifies the chord type: Major, Minor, Diminished, Augmented, or 7th variants
3. Progressive difficulty:
   - Level 1: Major vs Minor (the fundamental distinction)
   - Level 2: + Diminished, Augmented
   - Level 3: + Dominant 7th, Major 7th, Minor 7th
   - Level 4: All chord types

#### UI

Similar to Interval Trainer Audio mode but with chord-specific elements:

```
┌─────────────────────────────┐
│   Chord Ear Training        │
│                             │
│  Easy  Medium  Hard  Expert │
│                             │
│      ┌──────────────┐       │
│      │   🔊 Replay  │       │
│      │    C chord   │       │  ← Root shown, type hidden
│      └──────────────┘       │
│                             │
│  ┌───────┐  ┌───────┐      │
│  │ Major │  │ Minor │      │
│  └───────┘  └───────┘      │
│  ┌───────┐  ┌───────┐      │
│  │  Dim  │  │  Aug  │      │
│  └───────┘  └───────┘      │
│                             │
│  Score: 6/8  Streak: 3     │
└─────────────────────────────┘
```

### Technical Approach

```kotlin
data class ChordEarQuestion(
    val rootPitchClass: Int,
    val rootName: String,
    val chordFormula: ChordFormula,
    val correctAnswer: String,     // "Major", "Minor", etc.
    val options: List<String>,
    val correctIndex: Int,
    val frequencies: List<Float>,  // Frequencies of all notes in the chord
)

object ChordEarTrainer {
    private val LEVEL_TYPES = mapOf(
        1 to listOf("Major", "Minor"),
        2 to listOf("Major", "Minor", "Diminished", "Augmented"),
        3 to listOf("Major", "Minor", "Diminished", "Dominant 7th", "Major 7th", "Minor 7th"),
        4 to ChordFormulas.ALL.map { it.quality },
    )

    fun generateQuestion(level: Int): ChordEarQuestion { /* ... */ }
}
```

### Files

- **New:** `domain/ChordEarTrainer.kt` — Chord ear training question generation
- **New:** `ui/ChordEarTrainingView.kt` — Chord ear training UI
- **Modify:** `ui/FretboardScreen.kt` — Add "Chord Ear Training" to Learn section in drawer

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 3 days

---

## Feature G: Achievements & Daily Streaks

### Problem

There's no reward system for consistent practice. Users complete exercises but receive no recognition for milestones. Leading apps (Solfy, Duolingo, Tomus) use achievements and streaks as primary engagement drivers.

### Proposed Solution

#### Daily Practice Streak

- Track consecutive days with at least one learning activity (quiz, interval training, lesson read)
- Display streak count on the Learn section header and progress dashboard
- Streak badge: fire/flame icon with day count
- Optional: notification reminder if streak is about to break (end of day without activity)

#### Achievements

Unlockable badges for reaching milestones:

| Achievement | Condition | Icon Idea |
|-------------|-----------|-----------|
| First Steps | Complete your first theory lesson | 🎵 |
| Bookworm | Complete all 15 theory lessons | 📚 |
| Quick Study | Pass all 15 lesson mini quizzes | ✅ |
| Sharp Ear | Answer 10 interval questions correctly | 👂 |
| Perfect Pitch | Get 20 intervals right in a row | 🎯 |
| Quiz Whiz | Answer 50 quiz questions correctly | 🧠 |
| Category King | Achieve 90%+ accuracy in all 5 quiz categories | 👑 |
| Speed Demon | Score 15+ in Blitz mode | ⚡ |
| Streak Master | Maintain a 7-day practice streak | 🔥 |
| Theory Expert | Achieve 85%+ accuracy across all learning features | 🏆 |
| Note Navigator | Answer 30 fretboard note questions correctly | 🎸 |
| Chord Detective | Identify 20 chord types by ear correctly | 🔍 |

#### UI

- Achievements displayed in the Progress Dashboard (Feature 16)
- Locked achievements shown as grayed-out with progress indicator
- Toast/snackbar notification when an achievement is unlocked
- Achievement count shown in Learn section header

### Technical Approach

```kotlin
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Float,  // 0.0–1.0
)

object Achievements {
    val ALL = listOf(
        Achievement("first_steps", "First Steps", "Complete your first theory lesson", "🎵", ...),
        // ...
    )

    fun checkAndUnlock(repo: LearningProgressRepository): List<Achievement> {
        // Check each achievement condition against persisted stats
        // Return newly unlocked achievements for display
    }
}
```

### Files

- **New:** `data/Achievements.kt` — Achievement definitions and unlock conditions
- **Modify:** `data/LearningProgressRepository.kt` — Store unlocked achievements
- **Modify:** `ui/LearningProgressView.kt` — Display achievements grid
- **Modify:** `ui/TheoryLessonsView.kt`, `ui/TheoryQuizView.kt`, `ui/IntervalTrainerView.kt` — Check achievements after each activity

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 2–3 days

---

## Implementation Priority

| Priority | Feature | Effort | Impact | Dependencies |
|----------|---------|--------|--------|--------------|
| 1 | A: Cross-Feature Linking | 1 day | Medium | None |
| 2 | B: Expanded Quiz Templates | 2 days | Medium | None |
| 3 | C: Timed Challenge / Blitz Mode | 1–2 days | Medium | None |
| 4 | E: Fretboard Note Quiz | 3 days | High | None |
| 5 | G: Achievements & Daily Streaks | 2–3 days | High | Feature 16 (Persistence) |
| 6 | D: Interactive Lesson Demos | 3–4 days | Medium | None |
| 7 | F: Chord Ear Training | 3 days | High | Feature 17 (Audio Trainer) |

Features A, B, and C are low-hanging fruit that can be implemented independently. Feature E (Fretboard Note Quiz) adds a new training dimension that leverages the app's core fretboard UI. Feature G (Achievements) depends on persistence being in place first (Feature 16). Feature F (Chord Ear Training) is a natural extension of Feature 17 (Audio Interval Trainer).

---

## Architecture Notes

- All new features follow the established pattern: data in `data/`, logic in `domain/`, UI in `ui/`
- No existing functionality is modified or broken — all changes are additive
- Cross-feature linking (Feature A) requires passing navigation callbacks through the Compose hierarchy, following the existing pattern in `FretboardScreen.kt`
- Fretboard Note Quiz (Feature E) must be tuning-aware — note positions change in Low-G tuning
- Achievements (Feature G) should be checked lazily (only after a relevant activity completes, not on every recomposition)

## Cross-References

- Feature 16 (`docs/spec/16-learn-section-persistence.md`) — Required for persistence of achievements, streaks, and all-time stats
- Feature 17 (`docs/spec/17-interval-trainer-audio.md`) — Audio infrastructure needed for Chord Ear Training (Feature F)
- Feature 15 (`docs/spec/15-music-theory-learning.md`) — Original PRD proposing the Learn section; this document extends and refines those proposals

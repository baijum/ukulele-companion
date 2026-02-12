# Feature: Music Theory Learning Companion

**Status: PROPOSED**

## Summary

A comprehensive set of features to transform the app from a chord reference tool into a full music theory learning companion for ukulele players. Builds on the app's existing strong foundation — pitch classes, intervals, scales, chords, progressions, inversions, voice leading, transposition, and capo tools — by filling key educational gaps and adding interactive learning modes.

## Motivation

- The app already encodes substantial music theory (20 chord formulas, 7 scales, 12 interval names, inversion detection, voice leading) but presents most of it as passive reference rather than active teaching
- Leading competitors (Yousician, Troubadour) emphasize interactive learning, quizzes, and ear training — areas this app doesn't yet cover
- The Circle of Fifths, the most important visual tool in music theory, is entirely absent despite all the underlying data being in place
- Users who learned chords and progressions naturally ask "why do these chords go together?" — the app shows *what* but not *why*
- Adding more scales (modes) requires only new data entries in `Scales.kt`, with zero UI changes
- Music theory knowledge is instrument-agnostic but best learned *on* an instrument — a ukulele-specific theory app fills a market gap

---

## Current State: What the App Already Covers

### 1. Notes & Intervals (Strong)

- All 12 chromatic pitch classes with sharp/flat naming (`Notes.kt`)
- Full interval naming system: Root, m2, M2, m3, M3, P4, d5, P5, m6, M6, m7, M7 (`ChordInfo.kt`)
- Formula degree labels: 1, b2, 2, b3, 3, 4, b5, 5, #5, 6, b7, 7 (`ChordInfo.kt`)
- Real-time interval breakdown when chords are detected on the fretboard (`ChordResultView.kt`)

### 2. Scales (Good, limited scope)

- 7 scale types: Major, Natural Minor, Pentatonic Major/Minor, Blues, Dorian, Mixolydian (`Scales.kt`)
- Visual fretboard overlay with root note distinction (`ScaleSelector.kt`, `FretboardView.kt`)
- Works for all 12 keys via `scaleNotes()` function
- **Gap:** Harmonic Minor, Melodic Minor, Phrygian, Lydian, Locrian, Whole Tone, Chromatic are missing

### 3. Chords (Very Strong)

- 20 chord formulas across 4 categories: Triads (4), Suspended (3), Seventh (6), Extended (5+) (`ChordFormulas.kt`)
- Real-time chord detection from fretboard taps (`ChordDetector.kt`)
- Inversion detection with slash notation — Root, 1st, 2nd, 3rd inversion (`ChordInfo.kt`)
- Multi-voicing generation with playability scoring (`VoicingGenerator.kt`)
- Inversion comparison view, difficulty ratings, fingering suggestions
- **Gap:** 11th/13th chords, altered dominants (b9, #11), chord function labels (Tonic/Subdominant/Dominant)

### 4. Progressions & Harmony (Good)

- 12 preset progressions: 8 major (Pop, Classic Rock, 50s, Folk, Sad, Jazz ii-V-I, Reggae, Pachelbel) + 4 minor (`Progressions.kt`)
- Roman numeral (Nashville number) notation with diatonic chord degrees for Major and Minor
- Custom progression builder (`CreateProgressionSheet.kt`)
- Voice leading with DP-based optimal path finding (`VoiceLeading.kt`, `VoiceLeadingView.kt`)
- **Gap:** No chord function explanations, secondary dominants, borrowed chords, or modal interchange

### 5. Transposition & Capo (Excellent)

- Full transposition system for all keys (`Transpose.kt`)
- Capo calculator with playability scoring (`CapoCalculator.kt`)
- Interactive capo visualizer with side-by-side diagrams (`CapoVisualizerView.kt`)
- 5-section educational capo guide with interactive demo (`CapoGuideView.kt`)

### 6. Rhythm (Basic)

- 8 strumming patterns with arrow notation and difficulty levels (`StrumPatterns.kt`)
- BPM suggestions per pattern
- **Gap:** No time signature explanations, note value theory, subdivisions, or rhythmic exercises

---

## Gap Analysis

Compared to standard music theory curricula and leading theory-teaching apps, the following gaps are identified, organized by impact:

### Tier 1 — Foundational Gaps (High Impact)

| Gap | Why It Matters |
|-----|----------------|
| **Circle of Fifths** | The single most important visual tool in music theory. Connects keys, key signatures, relative majors/minors, and chord relationships. The app has all underlying data but no visual representation. |
| **Interval Ear Training** | The app names intervals visually but never trains the ear. Interval recognition is the #1 skill for developing musicianship. |
| **Scale Degree Names & Functions** | The app uses Roman numerals but never explains *why* certain chords feel resolved (Tonic), create tension (Dominant), or move forward (Subdominant). This bridges "what chords go together" and "why." |
| **Key Signature Reference** | The app works in pitch classes but never shows sharps/flats in a key. Essential for players who also read sheet music. |

### Tier 2 — Intermediate Gaps (Medium Impact)

| Gap | Why It Matters |
|-----|----------------|
| **More Scale Types (All Modes)** | Missing Phrygian, Lydian, Locrian, Harmonic Minor, Melodic Minor, Whole Tone. Architecture already supports them — just needs new entries in `Scales.kt`. |
| **Scale-Chord Relationship View** | Scale overlay and progressions tab are disconnected. A "Chords in this Scale" panel would show diatonic triads/sevenths derived from the selected scale. (Also proposed in `docs/spec/14-composition-tools.md` Idea 5.) |
| **Chord Function Labels** | Tag each chord in a progression with T (Tonic), S (Subdominant), D (Dominant) so users understand harmonic role, not just the numeral. |
| **Interactive Theory Quizzes** | Gamified exercises reinforce passive knowledge through active recall: "Name this interval," "What's the V chord in D major?" |

### Tier 3 — Advanced Gaps (Niche, valuable for serious learners)

| Gap | Why It Matters |
|-----|----------------|
| **Chord Substitution Guide** | Common substitutions (relative minor for major, tritone sub, diminished passing chords) are powerful composition tools that the app could demonstrate interactively. |
| **Secondary Dominants & Borrowed Chords** | V/V, V/vi, and borrowing from parallel minor create unexpected harmonic color. Educational content with fretboard examples would be unique. |
| **Rhythm Theory** | Time signatures (4/4, 3/4, 6/8), note values, subdivisions, syncopation. The strumming patterns exist but lack theoretical grounding. |

---

## Proposed Features

### Feature A: Interactive Circle of Fifths

#### Problem

The Circle of Fifths is the most important visual tool in music theory — it shows how all 12 keys relate to each other, reveals key signatures, identifies relative major/minor pairs, and illustrates closely related keys. The app has all the underlying data (`Notes.kt`, `Scales.kt`, `Progressions.kt`) but no visual representation.

#### Proposed Solution

A dedicated "Circle of Fifths" screen accessible from the navigation drawer:

- **Visual circular diagram** showing all 12 major keys around the outer ring and their relative minors on an inner ring
- **Tap a key** to see: key signature (number of sharps/flats), relative minor/major, diatonic chords (I through vii), common progressions in that key
- **Highlight relationships**: adjacent keys on the circle are closely related (share 6 of 7 notes); opposite keys are distant
- **Rotate to transpose**: drag the circle to shift everything by N semitones
- **Connect to existing features**: "View chords" links to the Chord Library filtered to that key; "View progressions" links to the Progressions tab set to that key

#### Technical Approach

```kotlin
// Circle of Fifths key order (clockwise from top)
val CIRCLE_OF_FIFTHS = listOf(0, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10, 5)
// C, G, D, A, E, B, F#/Gb, Db, Ab, Eb, Bb, F

// Key signature data
data class KeySignature(
    val pitchClass: Int,
    val sharps: Int,       // 0–7
    val flats: Int,        // 0–7
    val sharpNotes: List<String>,  // e.g., ["F#", "C#"]
    val flatNotes: List<String>,   // e.g., ["Bb", "Eb"]
    val relativeMinor: Int, // pitch class of relative minor (pitchClass + 9) % 12
)

// UI: Custom Canvas composable drawing the circular diagram
@Composable
fun CircleOfFifthsView(
    selectedKey: Int?,
    onKeySelected: (Int) -> Unit,
    onViewChords: (Int) -> Unit,
    onViewProgressions: (Int, ScaleType) -> Unit,
)
```

- Draw using Compose `Canvas` with `drawArc`, `drawText`, and `drawCircle`
- Outer ring: 12 major key segments; inner ring: 12 relative minor keys
- Selected key highlights its segment and shows detail panel below
- Detail panel: key signature, diatonic chords as tappable chips, "closely related keys" (adjacent on circle)

#### Files

- **New:** `data/KeySignatures.kt` — Key signature data for all 12 major and minor keys
- **New:** `ui/CircleOfFifthsView.kt` — Canvas-based circular diagram composable
- **Modify:** `ui/FretboardScreen.kt` — Add "Circle of Fifths" to navigation drawer

#### Effort Estimate

- **Complexity**: Medium–High (custom Canvas drawing is the main challenge)
- **Estimated time**: 3–4 days
- **APK size impact**: Negligible

---

### Feature B: Interval Trainer

#### Problem

The app displays interval names (Root, m3, P5, etc.) in chord breakdowns, but never challenges the user to identify intervals themselves. Interval recognition — both visual and aural — is the foundational skill for all music theory, yet the app has no training mode for it.

#### Proposed Solution

An "Interval Trainer" accessible from the navigation drawer with two modes:

- **Visual mode (Fretboard Quiz)**: Two notes are highlighted on the fretboard. The user must identify the interval (multiple choice from 4 options). Correct answers advance difficulty.
- **Audio mode (Ear Training)**: Two notes are played sequentially using the existing `ToneGenerator`. The user identifies the interval by ear. Can replay the notes.
- **Progressive difficulty**:
  - Level 1: P5, P4, Octave (easy to distinguish)
  - Level 2: Add M3, m3 (the "happy vs sad" intervals)
  - Level 3: Add M2, m2, m7, M7
  - Level 4: All 12 intervals
- **Score tracking**: Correct/incorrect streak, accuracy percentage, best streak (persisted via SharedPreferences)

#### Technical Approach

```kotlin
data class IntervalQuestion(
    val note1PitchClass: Int,
    val note2PitchClass: Int,
    val correctInterval: Int,         // semitones (0–11)
    val correctIntervalName: String,  // from ChordInfo.INTERVAL_NAMES
    val options: List<String>,        // 4 multiple-choice options
)

// Generate a random question at the given difficulty level
fun generateQuestion(level: Int, useFlats: Boolean): IntervalQuestion {
    val allowedIntervals = when (level) {
        1 -> listOf(5, 7, 12)          // P4, P5, Octave
        2 -> listOf(3, 4, 5, 7, 12)    // + m3, M3
        3 -> listOf(1, 2, 3, 4, 5, 7, 10, 11) // + m2, M2, m7, M7
        else -> (0..11).toList()        // All intervals
    }
    // Pick random root and interval, generate wrong options
    // ...
}
```

- Reuse `ChordInfo.INTERVAL_NAMES` for answer options
- Reuse `ToneGenerator` for playing notes in ear training mode
- Highlight notes on a read-only mini fretboard in visual mode
- Store best scores in SharedPreferences (same pattern as favorites)

#### Files

- **New:** `domain/IntervalTrainer.kt` — Question generation, scoring logic
- **New:** `data/TrainerScoreRepository.kt` — SharedPreferences persistence for scores
- **New:** `ui/IntervalTrainerView.kt` — Quiz UI with fretboard display and multiple choice
- **Modify:** `ui/FretboardScreen.kt` — Add "Interval Trainer" to navigation drawer

#### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 3–4 days
- **APK size impact**: Negligible

---

### Feature C: Theory Lessons Hub

#### Problem

The app teaches concepts incidentally (you learn intervals by seeing chord breakdowns, you learn scale degrees by viewing progressions) but has no structured curriculum. A beginner who opens the app can explore chords but has no guided path through music theory fundamentals.

#### Proposed Solution

A "Learn Theory" section in the navigation drawer with structured, topic-based lessons:

- **7 topic modules**: Notes & Pitch, Intervals, Scales, Chords, Keys & Key Signatures, Progressions & Harmony, Rhythm
- **Each lesson contains**:
  - Concise explanation text (2–3 paragraphs)
  - Interactive demo using existing app components (fretboard, chord diagrams, scale overlay)
  - Mini quiz (2–3 questions to check understanding)
- **Free browse**: All lessons accessible from the start (no forced progression)
- **Completion tracking**: Mark lessons as completed (persisted via SharedPreferences)

#### Lesson Outline

| Module | Lessons |
|--------|---------|
| **Notes & Pitch** | The 12 notes, sharps & flats, enharmonic equivalents, pitch classes |
| **Intervals** | What is an interval?, counting semitones, interval names, consonance vs. dissonance |
| **Scales** | Major scale construction, minor scales, pentatonic scales, modes (overview), scale degrees |
| **Chords** | Triads (major, minor, dim, aug), seventh chords, suspended chords, extended chords, chord formulas |
| **Keys & Signatures** | What is a key?, key signatures, Circle of Fifths, relative major/minor |
| **Progressions** | Diatonic harmony, Roman numerals, chord functions (T/S/D), common progressions, voice leading |
| **Rhythm** | Beat and tempo, time signatures, note values, strumming patterns, syncopation |

#### Technical Approach

```kotlin
data class TheoryLesson(
    val id: String,
    val module: String,
    val title: String,
    val content: String,           // Explanation text (can include simple markdown)
    val demoType: DemoType,        // Which interactive component to embed
    val demoConfig: Map<String, String>, // Parameters for the demo
    val quizQuestions: List<QuizQuestion>,
)

enum class DemoType {
    FRETBOARD,         // Interactive fretboard with preset notes
    CHORD_DIAGRAM,     // Show specific chord diagrams
    SCALE_OVERLAY,     // Fretboard with scale highlighted
    PROGRESSION,       // Show a progression with Roman numerals
    INTERVAL_PLAYER,   // Play two notes for interval demo
    NONE,              // Text-only lesson
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)
```

- Lessons defined as static data in `data/TheoryLessons.kt`
- Each demo type renders the appropriate existing composable with specific parameters
- Quiz results tracked per lesson in SharedPreferences
- UI: Scrollable lesson list grouped by module, each expandable into lesson content

#### Files

- **New:** `data/TheoryLessons.kt` — All lesson content, demos, and quiz data
- **New:** `data/LessonProgressRepository.kt` — SharedPreferences for completion tracking
- **New:** `ui/TheoryLessonsView.kt` — Lesson list, content display, quiz interaction
- **Modify:** `ui/FretboardScreen.kt` — Add "Learn Theory" to navigation drawer

#### Effort Estimate

- **Complexity**: Medium (mostly content authoring; UI reuses existing components)
- **Estimated time**: 4–5 days
- **APK size impact**: Negligible (text content only)

---

### Feature D: Chord Function Labels

#### Problem

The Progressions tab shows Roman numerals (I, IV, V, vi) but never explains the *harmonic function* of each chord — why the V chord creates tension that "wants" to resolve to I, or why IV feels like forward motion. Understanding chord functions is the key insight that connects knowing progressions to understanding harmony.

#### Proposed Solution

Add function labels and color-coding to the existing Progressions tab:

- **Label each chord** with its harmonic function: **T** (Tonic), **S** (Subdominant), **D** (Dominant)
- **Color-code** chords by function (e.g., Tonic = blue, Subdominant = green, Dominant = red/orange)
- **Tap for explanation**: Tapping a function label shows a brief tooltip explaining the role
- **Function mapping** for major scale:
  - Tonic (T): I, iii, vi — "home," "rest," "resolution"
  - Subdominant (S): ii, IV — "departure," "motion," "going somewhere"
  - Dominant (D): V, vii° — "tension," "needs to resolve," "pulls back to Tonic"

#### Technical Approach

```kotlin
enum class HarmonicFunction(val label: String, val description: String) {
    TONIC("T", "Feels like home — stable and resolved"),
    SUBDOMINANT("S", "Creates forward motion — moves away from home"),
    DOMINANT("D", "Builds tension — wants to resolve back to Tonic"),
}

// Add function field to ChordDegree (or compute from numeral)
fun harmonicFunction(numeral: String, scaleType: ScaleType): HarmonicFunction = when {
    // Major scale
    scaleType == ScaleType.MAJOR && numeral in listOf("I", "iii", "vi") -> HarmonicFunction.TONIC
    scaleType == ScaleType.MAJOR && numeral in listOf("ii", "IV") -> HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.MAJOR && numeral in listOf("V", "vii°") -> HarmonicFunction.DOMINANT
    // Minor scale
    scaleType == ScaleType.MINOR && numeral in listOf("i", "III", "VI") -> HarmonicFunction.TONIC
    scaleType == ScaleType.MINOR && numeral in listOf("ii°", "iv") -> HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.MINOR && numeral in listOf("V", "v", "VII") -> HarmonicFunction.DOMINANT
    else -> HarmonicFunction.TONIC
}
```

- Minimal change: add function badge next to each chord chip in `ProgressionsTab.kt`
- Color the chord chip background or border based on function
- On long-press or tap of the function badge, show a small popup with the description

#### Files

- **New:** `domain/HarmonicFunction.kt` — Function enum, mapping logic
- **Modify:** `ui/ProgressionsTab.kt` — Add function badge and color-coding to chord chips
- **Modify:** `data/Progressions.kt` — Optionally add `function` field to `ChordDegree`

#### Effort Estimate

- **Complexity**: Low
- **Estimated time**: 1 day
- **APK size impact**: Negligible

---

### Feature E: Extended Scales (All Modes)

#### Problem

The app supports 7 scales but is missing several important ones: the remaining church modes (Phrygian, Lydian, Locrian), Harmonic Minor, Melodic Minor, Whole Tone, and Chromatic. These are commonly referenced in music theory education and useful for advanced players exploring modal music.

#### Proposed Solution

Add the missing scale types to `Scales.kt`. The existing scale overlay UI, scale selector chips, and `scaleNotes()` function work without any changes — they iterate over `Scales.ALL` dynamically.

#### New Scale Entries

| Scale | Intervals | Notes | Common Use |
|-------|-----------|-------|------------|
| Harmonic Minor | 0, 2, 3, 5, 7, 8, 11 | 7 | Classical, metal, Middle Eastern |
| Melodic Minor (ascending) | 0, 2, 3, 5, 7, 9, 11 | 7 | Jazz, classical |
| Phrygian | 0, 1, 3, 5, 7, 8, 10 | 7 | Flamenco, metal, Spanish |
| Lydian | 0, 2, 4, 6, 7, 9, 11 | 7 | Film scores, dream-like feel |
| Locrian | 0, 1, 3, 5, 6, 8, 10 | 7 | Rarely used; theoretical completeness |
| Whole Tone | 0, 2, 4, 6, 8, 10 | 6 | Impressionism, dream sequences |
| Chromatic | 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 | 12 | Reference; shows all notes |

#### Technical Approach

```kotlin
// Add to Scales.ALL in Scales.kt:
Scale("Harmonic Minor",         listOf(0, 2, 3, 5, 7, 8, 11)),
Scale("Melodic Minor",          listOf(0, 2, 3, 5, 7, 9, 11)),
Scale("Phrygian",               listOf(0, 1, 3, 5, 7, 8, 10)),
Scale("Lydian",                 listOf(0, 2, 4, 6, 7, 9, 11)),
Scale("Locrian",                listOf(0, 1, 3, 5, 6, 8, 10)),
Scale("Whole Tone",             listOf(0, 2, 4, 6, 8, 10)),
Scale("Chromatic",              listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)),
```

No other file changes needed. The `ScaleSelector` UI and `FretboardView` overlay already iterate over `Scales.ALL`.

#### Files

- **Modify:** `data/Scales.kt` — Add 7 new scale entries to `ALL`

#### Effort Estimate

- **Complexity**: Trivial
- **Estimated time**: 15 minutes
- **APK size impact**: None

---

### Feature F: Key Signature Reference

#### Problem

The app works entirely in pitch classes (0–11), which is computationally elegant but disconnects from the music-notation concept of key signatures. Players who read sheet music, participate in ensembles, or study theory textbooks need to know that "D major has 2 sharps (F# and C#)." This information is implicit in the app's data but never surfaced.

#### Proposed Solution

A "Key Signatures" reference panel, integrated into the Circle of Fifths view (Feature A) or as a standalone section:

- **For any selected key**: Show the number of sharps or flats and which notes they are
- **Order of sharps**: F, C, G, D, A, E, B (mnemonic: "Father Charles Goes Down And Ends Battle")
- **Order of flats**: B, E, A, D, G, C, F (reverse of sharps)
- **Visual display**: Could optionally show a mini staff with sharps/flats (or just text listing)

#### Technical Approach

```kotlin
// Key signature data (major keys only; minor keys share with relative major)
val KEY_SIGNATURES: Map<Int, KeySignature> = mapOf(
    0  to KeySignature(sharps = 0, flats = 0, accidentals = emptyList()),           // C
    7  to KeySignature(sharps = 1, flats = 0, accidentals = listOf("F#")),          // G
    2  to KeySignature(sharps = 2, flats = 0, accidentals = listOf("F#", "C#")),    // D
    9  to KeySignature(sharps = 3, flats = 0, accidentals = listOf("F#", "C#", "G#")), // A
    4  to KeySignature(sharps = 4, flats = 0, accidentals = listOf("F#", "C#", "G#", "D#")), // E
    11 to KeySignature(sharps = 5, flats = 0, accidentals = listOf("F#", "C#", "G#", "D#", "A#")), // B
    6  to KeySignature(sharps = 6, flats = 0, accidentals = listOf("F#", "C#", "G#", "D#", "A#", "E#")), // F#
    5  to KeySignature(sharps = 0, flats = 1, accidentals = listOf("Bb")),          // F
    10 to KeySignature(sharps = 0, flats = 2, accidentals = listOf("Bb", "Eb")),    // Bb
    3  to KeySignature(sharps = 0, flats = 3, accidentals = listOf("Bb", "Eb", "Ab")), // Eb
    8  to KeySignature(sharps = 0, flats = 4, accidentals = listOf("Bb", "Eb", "Ab", "Db")), // Ab
    1  to KeySignature(sharps = 0, flats = 5, accidentals = listOf("Bb", "Eb", "Ab", "Db", "Gb")), // Db
)
```

#### Files

- **New:** `data/KeySignatures.kt` — Key signature data (can be shared with Circle of Fifths)
- **Modify:** `ui/CircleOfFifthsView.kt` — Show key signature in detail panel (if built with Feature A)
- Or **New:** `ui/KeySignatureView.kt` — Standalone reference view

#### Effort Estimate

- **Complexity**: Low
- **Estimated time**: 1 day
- **APK size impact**: Negligible

---

### Feature G: Theory Quizzes

#### Problem

The app provides information but never tests understanding. Educational research shows that active recall (quizzes, flashcards) is far more effective than passive reading for retention. Gamified learning elements (streaks, scores) also increase engagement and return visits.

#### Proposed Solution

A "Theory Quiz" mode accessible from the navigation drawer with multiple categories:

#### Quiz Categories & Example Questions

**Intervals:**
- "What interval is C to G?" → P5
- "How many semitones in a minor 3rd?" → 3
- "Name the interval: 7 semitones" → Perfect 5th

**Chords:**
- "What notes are in a D minor chord?" → D, F, A
- "Name the chord with formula 1 b3 5 b7" → Minor 7th
- "What type of chord is C-E-G?" → Major

**Keys:**
- "How many sharps in A major?" → 3
- "What is the relative minor of G major?" → E minor
- "Which key has 2 flats?" → Bb major

**Scales:**
- "What notes are in the C major scale?" → C D E F G A B
- "What mode starts on the 2nd degree of major?" → Dorian
- "Which scale has intervals 0-2-3-5-7-9-10?" → Dorian

**Progressions:**
- "What is the V chord in D major?" → A
- "In the key of G, what Roman numeral is Em?" → vi
- "Name the progression: I-V-vi-IV" → Pop / Four Chords

#### Technical Approach

```kotlin
data class QuizQuestion(
    val category: QuizCategory,
    val question: String,
    val options: List<String>,  // 4 options for multiple choice
    val correctIndex: Int,
    val explanation: String,    // Shown after answering
)

enum class QuizCategory(val label: String) {
    INTERVALS("Intervals"),
    CHORDS("Chords"),
    KEYS("Keys"),
    SCALES("Scales"),
    PROGRESSIONS("Progressions"),
}

// Questions can be generated dynamically from existing data:
fun generateIntervalQuestion(): QuizQuestion {
    val root = (0..11).random()
    val interval = (1..11).random()
    val target = (root + interval) % 12
    val rootName = Notes.pitchClassToName(root)
    val targetName = Notes.pitchClassToName(target)
    val correctAnswer = INTERVAL_NAMES[interval]!!
    // Generate 3 wrong answers from other interval names...
}
```

- Most questions generated dynamically from existing data in `Notes.kt`, `ChordFormulas.kt`, `Scales.kt`, `Progressions.kt` — no need for a large static question bank
- Score persistence: total correct, accuracy %, best streak per category (SharedPreferences)
- UI: Card-based quiz with 4 tappable answer options, immediate feedback (green/red), explanation

#### Files

- **New:** `domain/QuizGenerator.kt` — Dynamic question generation from app data
- **New:** `data/QuizScoreRepository.kt` — SharedPreferences for scores and streaks
- **New:** `ui/TheoryQuizView.kt` — Quiz UI with categories, questions, scoring
- **Modify:** `ui/FretboardScreen.kt` — Add "Theory Quiz" to navigation drawer

#### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 3–4 days
- **APK size impact**: Negligible

---

### Feature H: Chord Substitutions & Borrowed Chords Guide

#### Problem

Intermediate-to-advanced players want to go beyond standard progressions. Chord substitution is a key technique for adding harmonic interest — replacing one chord with another that shares common tones or creates a similar harmonic effect. The app currently has no educational content about this.

#### Proposed Solution

A "Chord Substitutions" educational guide (similar in structure to the Capo Guide) with interactive examples:

#### Content Sections

**1. Diatonic Substitutions**
- Replace a chord with another chord from the same key that shares 2 of 3 notes
- Example: In C major, replace C (C-E-G) with Am (A-C-E) — they share C and E
- Substitution pairs: I/vi, ii/IV, iii/I, V/vii°

**2. Relative Major/Minor Swaps**
- Replace a major chord with its relative minor (or vice versa)
- Example: Replace F with Dm in a C major progression
- Always works because they share 2 notes

**3. Tritone Substitution**
- Replace a dominant 7th chord with the dominant 7th a tritone away
- Example: Replace G7 with Db7 (both contain B and F)
- Creates chromatic bass movement: Db → C instead of G → C

**4. Modal Interchange (Borrowed Chords)**
- Borrow chords from the parallel minor (or other mode) into a major key
- Example: In C major, use Fm (from C minor) instead of F
- Common borrowings: iv, bVI, bVII from natural minor

**5. Secondary Dominants**
- Use the V chord of any diatonic chord (not just the I) as a temporary tonicization
- Example: In C major, play D7 (V of G) before G to emphasize the arrival on V
- Common: V/V, V/vi, V/ii

#### Technical Approach

```kotlin
data class Substitution(
    val name: String,
    val description: String,
    val originalNumeral: String,
    val substituteNumeral: String,
    val sharedNotes: String,        // "C, E" for I→vi
    val exampleInC: String,         // "Replace C with Am"
)

data class SubstitutionCategory(
    val title: String,
    val explanation: String,
    val substitutions: List<Substitution>,
)
```

- Static educational content in `data/ChordSubstitutions.kt`
- Interactive "Try it" buttons that show the original and substituted progressions side by side
- Link to Chord Library to view voicings for the substitute chord

#### Files

- **New:** `data/ChordSubstitutions.kt` — Substitution data and educational content
- **New:** `ui/ChordSubstitutionsView.kt` — Guide UI with sections and interactive examples
- **Modify:** `ui/FretboardScreen.kt` — Add "Chord Substitutions" to navigation drawer

#### Effort Estimate

- **Complexity**: Medium (mostly content; interactive examples add moderate UI work)
- **Estimated time**: 2–3 days
- **APK size impact**: Negligible

---

## Implementation Priority

Suggested order based on educational impact, technical complexity, and user value:

| Priority | Feature | Impact | Complexity | Time |
|----------|---------|--------|------------|------|
| 1 | E: Extended Scales (All Modes) | Medium | Trivial | 15 min |
| 2 | D: Chord Function Labels | High | Low | 1 day |
| 3 | F: Key Signature Reference | Medium | Low | 1 day |
| 4 | A: Interactive Circle of Fifths | High | Medium–High | 3–4 days |
| 5 | G: Theory Quizzes | High | Medium | 3–4 days |
| 6 | B: Interval Trainer | High | Medium | 3–4 days |
| 7 | H: Chord Substitutions Guide | Medium | Medium | 2–3 days |
| 8 | C: Theory Lessons Hub | High | Medium | 4–5 days |

Features E and D provide the highest return for minimal effort. Feature A (Circle of Fifths) is the signature piece that would distinguish the app as a theory companion. Features B and G add the active-learning dimension that competitors have.

## Architecture Notes

- Most features build on existing data in `Notes.kt`, `Scales.kt`, `ChordFormulas.kt`, `Progressions.kt`, `ChordInfo.kt`
- New features should follow the established pattern: data in `data/`, logic in `domain/`, UI in `ui/`
- Quiz and trainer score persistence can use `SharedPreferences` (consistent with existing `FavoritesRepository`, `ChordSheetRepository`)
- Circle of Fifths is the most complex UI piece — requires custom `Canvas` composable
- Extended Scales (Feature E) is trivially easy — just add entries to `Scales.ALL`
- Theory Lessons Hub (Feature C) is content-heavy but architecturally simple — most effort is writing lesson text
- All features are additive — no existing functionality needs to be modified or broken

## Dependencies Between Proposed Features

- Feature F (Key Signatures) naturally integrates into Feature A (Circle of Fifths) — build them together
- Feature G (Quizzes) becomes richer after Feature E (more scales to quiz on) and Feature D (can quiz on chord functions)
- Feature C (Lessons Hub) can reference all other features as interactive demos
- Features B, D, E, G, H are independent and can be built in any order

## Cross-References

- Composition tools proposed in `docs/spec/14-composition-tools.md` Idea 5 (Scale-Aware Chord Suggestions) overlaps with the Scale-Chord Relationship gap identified here — implementing either covers that gap
- The Capo Guide (`ui/CapoGuideView.kt`) serves as a structural template for Features C and H (educational content with interactive demos)
- The existing `ChordInfo.INTERVAL_NAMES` map is directly reusable for Features B (Interval Trainer) and G (Quizzes)

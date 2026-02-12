# Feature: Interval Trainer Audio / Ear Training Mode

**Status: PROPOSED**

## Summary

Add an audio-based ear training mode to the existing Interval Trainer. Users hear two notes played sequentially (or simultaneously) and identify the interval by ear. This complements the existing visual mode and addresses the most significant gap in the Learn section — interval recognition is fundamentally an aural skill, yet the current trainer is visual-only.

## Motivation

- The PRD (`docs/spec/15-music-theory-learning.md`, Feature B) explicitly proposed audio mode but it was never implemented
- Interval ear training is the #1 skill-building exercise across all music theory apps (Auricula, RelativePitch, EarMaster, Solfy)
- The app already has `ToneGenerator` for playing notes — the audio infrastructure exists
- Visual interval identification (current mode) tests music theory knowledge; audio identification tests musical ear — these are complementary but distinct skills
- Leading ear training apps offer ascending, descending, and harmonic (simultaneous) intervals — the current app offers none of these
- A ukulele-specific ear training tool fills a gap in the market; most ear training apps use piano tones

---

## Scope

### In Scope

- Audio playback of two notes for interval identification (ear training)
- Three playback modes: ascending (low to high), descending (high to low), harmonic (simultaneous)
- Replay button to hear the interval again
- Reuse existing progressive difficulty levels (Easy/Medium/Hard/Expert)
- Toggle between Visual mode and Audio mode within the same screen
- Score tracking for audio mode (separate from visual mode stats)
- Integration with persistence (Feature 16) when available

### Out of Scope

- Realistic instrument samples (ukulele, piano, guitar) — use existing sine wave `ToneGenerator`
- Microphone-based pitch detection (user sings/plays the interval)
- Melodic dictation (sequences of more than 2 notes)
- Custom interval selection (user picks which intervals to practice)
- Audio for Theory Quiz or Theory Lessons

---

## Design

### 1. Mode Toggle

The Interval Trainer screen adds a mode selector at the top:

```
[ Visual ]  [ Audio ]
```

- **Visual** (existing): Shows two colored note circles with an arrow, user identifies the interval
- **Audio** (new): Plays two notes, screen shows a play button and answer options — no note names visible until answered

### 2. Audio Playback

When a new question is generated in Audio mode:

1. Two notes are played automatically with a ~500ms gap between them
2. A "Replay" button allows the user to hear the interval again (unlimited replays)
3. A direction indicator shows whether the interval is ascending or descending

#### Playback Modes

Controlled by filter chips below the mode toggle:

```
[ Ascending ]  [ Descending ]  [ Harmonic ]  [ Random ]
```

- **Ascending**: First note is lower, second is higher
- **Descending**: First note is higher, second is lower
- **Harmonic**: Both notes played simultaneously
- **Random**: Randomly picks ascending or descending per question

### 3. Audio Question UI

```
┌─────────────────────────────┐
│   [ Visual ]  [*Audio*]     │  ← Mode toggle
│                             │
│  Easy  Medium  Hard  Expert │  ← Difficulty chips
│  Asc   Desc   Harm  Random │  ← Direction chips
│                             │
│      ┌──────────────┐       │
│      │   🔊 Replay  │       │  ← Large replay button
│      └──────────────┘       │
│                             │
│   ┌──────┐  ┌──────┐       │
│   │  m3  │  │  M3  │       │  ← Answer option buttons
│   └──────┘  └──────┘       │
│   ┌──────┐  ┌──────┐       │
│   │  P4  │  │  P5  │       │
│   └──────┘  └──────┘       │
│                             │
│  Correct: 7  Total: 10     │  ← Score stats
│  Accuracy: 70%  Streak: 3  │
└─────────────────────────────┘
```

After answering:
- Correct: Green highlight, show note names (e.g., "C to E = Major 3rd")
- Incorrect: Red highlight, show correct answer, auto-replay the interval once
- "Next" button to proceed

### 4. Question Generation

Extend `IntervalTrainer.generateQuestion()` to support direction:

```kotlin
enum class IntervalDirection {
    ASCENDING,   // note2 > note1
    DESCENDING,  // note2 < note1
    HARMONIC,    // played simultaneously
}

data class IntervalQuestion(
    // ... existing fields ...
    val direction: IntervalDirection,
    val note1Frequency: Float,  // Hz for ToneGenerator
    val note2Frequency: Float,  // Hz for ToneGenerator
)

fun generateQuestion(
    level: Int,
    direction: IntervalDirection = IntervalDirection.ASCENDING,
): IntervalQuestion {
    // ... existing logic ...
    // Add frequency calculation:
    // A4 = 440 Hz, each semitone = multiply by 2^(1/12)
    val baseOctave = 4  // Middle octave
    val note1Midi = 60 + note1PitchClass  // C4 = MIDI 60
    val note2Midi = when (direction) {
        ASCENDING -> note1Midi + interval
        DESCENDING -> note1Midi - interval
        HARMONIC -> note1Midi + interval
    }
    val note1Freq = 440.0 * 2.0.pow((note1Midi - 69) / 12.0)
    val note2Freq = 440.0 * 2.0.pow((note2Midi - 69) / 12.0)
}
```

### 5. Audio Playback Integration

```kotlin
// In IntervalTrainerView.kt or a new AudioIntervalPlayer.kt:

fun playInterval(
    question: IntervalQuestion,
    toneGenerator: ToneGenerator,
    direction: IntervalDirection,
) {
    when (direction) {
        ASCENDING, DESCENDING -> {
            toneGenerator.playTone(question.note1Frequency, durationMs = 800)
            delay(500)
            toneGenerator.playTone(question.note2Frequency, durationMs = 800)
        }
        HARMONIC -> {
            // Play both notes simultaneously
            toneGenerator.playTone(question.note1Frequency, durationMs = 1200)
            toneGenerator.playTone(question.note2Frequency, durationMs = 1200)
        }
    }
}
```

### 6. Difficulty Levels (Same as Visual Mode)

| Level | Intervals | Rationale |
|-------|-----------|-----------|
| Easy | P4, P5, Octave | Very distinct sounds, easy to tell apart |
| Medium | + m3, M3 | "Happy vs sad" — iconic ear training exercise |
| Hard | + m2, M2, m7, M7 | Seconds and sevenths are harder to distinguish |
| Expert | All 12 intervals | Full chromatic interval recognition |

Auto-level-up after 5 correct in a row (same as visual mode).

---

## Technical Approach

### Files

- **Modify:** `domain/IntervalTrainer.kt` — Add `IntervalDirection` enum, frequency calculation, direction-aware question generation
- **Modify:** `ui/IntervalTrainerView.kt` — Add mode toggle (Visual/Audio), direction chips, replay button, audio playback logic
- **Possibly new:** `domain/AudioIntervalPlayer.kt` — Encapsulate audio playback logic if it becomes complex
- **Modify:** `ui/FretboardScreen.kt` — No changes needed (Interval Trainer is already in the drawer)

### Dependencies on Existing Code

- `ToneGenerator` — Already exists for chord playback; reuse for playing individual notes
- `IntervalTrainer.INTERVAL_NAMES` — Reuse for answer options
- `Notes.pitchClassToName()` — Reuse for displaying note names after answering

### Frequency Calculation

Standard equal temperament tuning:

```
frequency = 440 * 2^((midiNote - 69) / 12)
```

Where MIDI note 60 = C4 (middle C), 69 = A4 (440 Hz).

For ukulele-relevant range, notes should be generated in octaves 3–5 (approximately 130 Hz to 1047 Hz) to sound natural.

### State Management

Audio mode adds the following state to `IntervalTrainerView`:

```kotlin
var isAudioMode by remember { mutableStateOf(false) }
var direction by remember { mutableStateOf(IntervalDirection.ASCENDING) }
var isPlaying by remember { mutableStateOf(false) }
```

The existing score tracking (correct, total, streak, bestStreak) is shared across both modes in the current session, but persisted separately per mode when Feature 16 is implemented.

---

## Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 3–4 days
- **APK size impact**: Negligible (no audio sample files — synthesized tones only)

## Dependencies

- `ToneGenerator` must support playing individual notes at specified frequencies (verify existing API)
- Optional: Feature 16 (Persistence) for saving audio mode scores separately

## Future Enhancements

- **Custom interval selection**: Let users pick exactly which intervals to practice (e.g., only m3 and M3)
- **Instrument timbre options**: Sine wave, square wave, or sampled piano/ukulele tones
- **Reference song associations**: "Here Comes the Bride" = P4, "Twinkle Twinkle" = P5, "Somewhere Over the Rainbow" = Octave — show these as memory aids
- **Chord ear training**: Extend beyond intervals to identify chord types (major, minor, dim, 7th) by ear — proposed separately in Feature 18

## Testing Strategy

- Verify audio plays correctly for ascending, descending, and harmonic modes
- Verify replay button works and can be pressed multiple times
- Verify correct/incorrect feedback displays properly and auto-replays on wrong answer
- Verify difficulty levels filter intervals correctly (same as visual mode)
- Verify auto-level-up works after 5 consecutive correct answers
- Verify mode toggle preserves difficulty level selection
- Verify no audio artifacts (clicks, pops) between notes
- Test on different devices for volume levels and audio latency

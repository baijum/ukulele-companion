# Test Plan: Quiz & Training Generators

## Scope
- `shared/.../domain/QuizGenerator.kt`
- `shared/.../domain/NoteQuizGenerator.kt`
- `shared/.../domain/ScalePracticeGenerator.kt`
- `shared/.../domain/IntervalTrainer.kt`
- `shared/.../domain/ChordEarTrainer.kt`

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/QuizGeneratorTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/NoteQuizGeneratorTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ScalePracticeGeneratorTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/IntervalTrainerTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ChordEarTrainerTest.kt`

## QuizGenerator Test Cases

### `generate(category?)`
- No category → returns a valid question of any type
- Each category filter returns matching question type
- Question has non-empty text
- Question has at least 2 answer options
- Correct answer index is within options bounds
- All 10+ question types can be generated (run N times, collect types)
- No crash on repeated generation (100 calls)

## NoteQuizGenerator Test Cases

### `generateNameIt()`
- Returns a question with a fret position
- Answer is a valid note name (A-G with optional sharp/flat)
- String index is 0-3 (4 ukulele strings)
- Fret is in valid range (0-12)

### `generateFindIt()`
- Returns a target note name and expected position(s)
- Target note is a valid pitch class name
- At least one valid answer position exists

## ScalePracticeGenerator Test Cases

### `generateQuizQuestion()` / `generateEarQuestion()`
- Returns valid question with non-empty prompt
- Answer options are present
- Correct answer index is valid
- All 4 question types can be generated
- Scale names reference actual scales from Scales.ALL

## IntervalTrainer Test Cases

### `generateQuestion(level, direction)`
- Level 1 → only simple intervals (unison, 3rd, 5th, octave)
- Level 4 → includes all intervals
- Each level produces questions within its interval set
- Direction parameter affects question framing
- Generated intervals are musically valid (0-12 semitones)

## ChordEarTrainer Test Cases

### `generateQuestion(level)`
- Level 1 → major vs minor only
- Level 4 → includes 7ths, diminished, augmented
- Answer options match the level's chord set
- Correct answer is always in the options list
- Chord quality names are valid

## General Properties (all generators)
- No null returns
- Repeated calls produce varied output (not always identical)
- No exceptions thrown for any valid input

## Priority
Medium — Generators are pure functions with randomized output; tests should verify structural validity rather than exact values.

## Estimated Test Count
~35-40 test cases

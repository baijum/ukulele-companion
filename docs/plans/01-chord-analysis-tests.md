# Test Plan: Chord Analysis

## Scope
- `shared/.../domain/ChordInfo.kt`
- `shared/.../domain/ChordSheetFormatter.kt`

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ChordInfoTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ChordSheetFormatterTest.kt`

## ChordInfo Test Cases

### `buildIntervalBreakdown()`
- Major triad returns R, 3, 5
- Minor triad returns R, b3, 5
- Seventh chord includes 7th interval
- Empty frets returns empty breakdown

### `buildFormulaString()`
- Standard chord formulas (major, minor, dim, aug)
- Extended chords (7th, 9th)

### `suggestFingering()` / `formatFingering()`
- Common open chords (C, Am, F, G)
- Barre chord suggestions
- Format output matches expected string representation

### `rateDifficulty()`
- Open chord rated easy
- Barre chord rated harder
- Wide fret span increases difficulty

### `determineInversion()`
- Root position (bass = root)
- First inversion (bass = 3rd)
- Second inversion (bass = 5th)

### `slashNotation()` / `findBassStringIndex()` / `bassPitchClass()`
- Slash chord with non-root bass
- Root position returns no slash notation
- Bass string index for standard tuning

## ChordSheetFormatter Test Cases

### `formatChordsAboveLyrics()`
- Single chord above single word
- Multiple chords spaced correctly above lyrics
- Empty input returns empty output
- Long lyrics with few chords

### `formatPlainText()`
- Strips chord markers, returns plain lyrics
- Preserves line breaks

### `formatProgression()`
- Standard progression (I-IV-V-I) formats correctly
- Single chord progression

## Priority
Medium — ChordInfo has high API surface area (9 public functions) and is used across chord detail UIs.

## Estimated Test Count
~25-30 test cases

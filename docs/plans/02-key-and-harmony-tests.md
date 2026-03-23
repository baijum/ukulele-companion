# Test Plan: Key & Harmony Detection

## Scope
- `shared/.../domain/KeyDetector.kt`
- `shared/.../data/KeySignatures.kt`
- `shared/.../domain/HarmonicFunction.kt`

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/KeyDetectorTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/KeySignaturesTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/HarmonicFunctionTest.kt`

## KeyDetector Test Cases

### `detectKey(chordNames)`
- I-IV-V progression in C → detects C major
- i-iv-v progression in Am → detects A minor
- Jazz ii-V-I in G → detects G major
- Single chord → returns result with low confidence
- Empty list → returns null
- Ambiguous chords (relative major/minor) → picks higher confidence
- All 12 keys: verify correct detection for transposed progressions
- Mixed sharp/flat chord names still detect correctly

## KeySignatures Test Cases

### `forKey(pitchClass)`
- All 12 pitch classes return valid KeySignature
- C major → 0 sharps, 0 flats
- G major → 1 sharp (F#)
- F major → 1 flat (Bb)

### `formatSignature()`
- Key with sharps displays correctly
- Key with flats displays correctly
- C major displays "no sharps or flats"

### `diatonicChordsForMajor(pitchClass)`
- C major → C, Dm, Em, F, G, Am, Bdim
- Verify Roman numerals match (I, ii, iii, IV, V, vi, vii°)

### `diatonicChordsForMinor(pitchClass)`
- A minor → Am, Bdim, C, Dm, Em, F, G
- Verify Roman numerals for natural minor

### `closelyRelatedKeys(pitchClass)`
- C → returns F and G (IV and V)
- Circle of fifths adjacency for all keys

## HarmonicFunction Test Cases

### `harmonicFunction(numeral, scaleType)`
- I/iii/vi → TONIC
- ii/IV → SUBDOMINANT
- V/vii° → DOMINANT
- Major vs minor scale assignments
- Edge cases: modal numerals

## Priority
High — KeyDetector and KeySignatures contain significant logic (scoring, circle of fifths) used in chord sheet analysis and theory sections.

## Estimated Test Count
~35-40 test cases

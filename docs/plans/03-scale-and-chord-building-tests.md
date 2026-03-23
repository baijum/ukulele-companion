# Test Plan: Scale & Chord Building

## Scope
- `shared/.../domain/ScaleChords.kt`
- `shared/.../domain/ScaleChordBuilder.kt`
- `shared/.../data/ScalePositions.kt`
- `shared/.../data/Scales.kt` (validation tests)

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ScaleChordsTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ScaleChordBuilderTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/ScalePositionsTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/ScalesTest.kt`

## ScaleChords Test Cases

### `diatonicTriads(root, scale)`
- C major scale → C, Dm, Em, F, G, Am, Bdim
- A natural minor → Am, Bdim, C, Dm, Em, F, G
- D dorian → Dm, Em, F, G, Am, Bdim, C
- Verify chord qualities match scale degrees
- All 7 modes produce 7 triads each

## ScaleChordBuilder Test Cases

### `buildTriads(rootPitchClass, scale)`
- C major → correct pitch classes in each triad
- Minor scale → correct quality assignments
- Verify root pitch class of each triad matches scale degree
- All triads contain exactly 3 notes

## ScalePositions Test Cases

### `generate(root, intervals, tuningPitchClasses)`
- C major on standard tuning → produces multiple positions
- Each position spans at most 4-5 frets
- Adjacent positions overlap by 1 fret
- All scale notes are reachable across all positions combined
- Pentatonic scale produces fewer notes per position
- Different tunings (Low G vs High G) produce different positions
- Root = 0 (C) through root = 11 (B): positions shift correctly

## Scales Validation Test Cases

### Data integrity
- All 40+ scales have non-empty intervals
- All intervals are in range 0-11
- All scales have at least one category
- No duplicate scale names
- `forCategory()` returns non-empty lists for each category
- `scaleNotes()` produces correct pitch class sets for known scales (C major → {0,2,4,5,7,9,11})

## Priority
Medium — ScalePositions has algorithmic complexity; ScaleChords/Builder are pure functions ideal for unit testing.

## Estimated Test Count
~30-35 test cases

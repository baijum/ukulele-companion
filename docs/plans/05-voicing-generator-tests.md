# Test Plan: Voicing Generator

## Scope
- `shared/.../data/VoicingGenerator.kt` (HIGH complexity)

## Current Coverage
None. This is the most complex untested data-layer file (262 lines, Cartesian product + filtering + sorting).

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/VoicingGeneratorTest.kt`

## Test Cases

### Basic Generation
- C major on standard High-G tuning → returns non-empty list
- Am chord → returns known voicing (0,0,0,0) as first or top result
- Every generated voicing contains all required pitch classes
- No voicing exceeds fret span of 4

### Chord Quality Coverage
- Major, minor, diminished, augmented triads
- 7th chords (maj7, min7, dom7, dim7)
- Sus2, sus4 chords
- 6th chords

### Sorting & Ranking
- Open-string voicings ranked higher than barre voicings
- Lower-position voicings preferred over higher positions
- Smaller fret spans preferred
- Known easy chords (C, Am, F) appear near top of results

### Tuning Variations
- High-G tuning produces different voicings than Low-G
- Baritone tuning (DGBE) produces guitar-like voicings
- D-tuning shifts all voicings appropriately

### Muted Strings
- `allowMutedStrings = false` → all 4 strings fretted/open
- `allowMutedStrings = true` → returns additional voicings with muted strings
- Muted voicings appear after non-muted in results

### Result Limits
- Returns at most 10 standard voicings
- Returns at most 5 additional muted voicings (when allowed)
- No duplicate voicings in results

### Extended Chords
- Extended chords (9th, 11th) drop omittable intervals
- Required intervals (root, 3rd/quality-defining) always present
- Subset generation produces valid subsets

### Edge Cases
- Chord with no valid voicings in range → returns empty list
- Root pitch class 0-11 all produce results for C major formula
- Very high fret positions (beyond 12) excluded or handled

## Priority
Critical — VoicingGenerator is the core algorithm behind the chord library UI. Bugs here affect every chord lookup. High algorithmic complexity (Cartesian product, filtering, dedup, comparator sorting).

## Estimated Test Count
~30-35 test cases

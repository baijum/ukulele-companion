# Test Plan: Voice Leading & Finger Transitions

## Scope
- `shared/.../domain/VoiceLeading.kt` (HIGH complexity — dynamic programming)
- `shared/.../domain/FingerTransitionCalculator.kt`

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/VoiceLeadingTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/FingerTransitionCalculatorTest.kt`

## VoiceLeading Test Cases

### `distance()`
- Same voicing → distance 0
- Adjacent fret change on one string → small distance
- Large fret jump → larger distance
- Open string to barre → measured correctly

### `computeTransition()`
- Identical voicings → minimal transition
- C to Am (shared notes) → smooth transition
- C to F# (no shared notes) → larger transition

### `computeOptimalPath(progression, keyRoot, tuning)`
- I-IV-V-I in C → returns valid path with all voicings
- Two-chord progression → picks closest voicings
- Single chord → returns trivial path
- Empty progression → returns null or empty path
- Long progression (8+ chords) → completes without excessive time
- Path minimizes total voice-leading distance
- All voicings in path are playable (fret span ≤ 4)
- Different tunings produce different optimal paths

### Edge Cases
- Chord with no known voicings → graceful handling
- Repeated chord in progression → may reuse same voicing

## FingerTransitionCalculator Test Cases

### `calculateTransition(fromFrets, toFrets)`
- Same frets → all STAY movements
- One finger slides → SLIDE movement detected
- Finger lifts off → LIFT movement
- New finger placed → PLACE movement
- Complete chord change → mix of MOVE/LIFT/PLACE
- Open string to fretted → PLACE on that string
- Fretted to open → LIFT on that string

### `describeMovement()`
- STAY → appropriate description
- SLIDE → includes "slide" language
- Each movement type produces human-readable description

### Edge Cases
- Empty fret arrays
- Muted strings (-1 values)
- Same chord shape at different positions

## Priority
High — VoiceLeading uses O(n × k²) dynamic programming and is the most algorithmically complex untested code in the shared module.

## Estimated Test Count
~25-30 test cases

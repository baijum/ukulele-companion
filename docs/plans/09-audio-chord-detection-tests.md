# Test Plan: Audio Chord Detection

## Scope
- `shared/.../domain/Chromagram.kt`
- `shared/.../domain/AudioChordDetector.kt`
- `shared/.../domain/ArpeggioDetector.kt`

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ChromagramTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/AudioChordDetectorTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/ArpeggioDetectorTest.kt`

## Chromagram Test Cases

### `compute(magnitudes, sampleRate, fftSize)`
- Pure C4 sine (261.63 Hz) → highest energy in pitch class 0
- Pure A4 sine (440 Hz) → highest energy in pitch class 9
- C major chord (C+E+G) → energy peaks at pitch classes 0, 4, 7
- Silent input (all zeros) → all bins near zero
- Returns exactly 12 bins (one per pitch class)
- All bins are non-negative
- Different sample rates produce consistent pitch class mapping
- DC component (bin 0) does not leak into chromagram

### Synthetic Test Signals
- Generate magnitude spectrum from known sine frequencies
- Verify dominant pitch class matches expected note

## AudioChordDetector Test Cases

### `detect(samples, sampleRate, threshold)`
- Synthetic C major chord audio → detects C or related chord
- Single note → returns appropriate result (single note or no chord)
- Silent audio → no detection
- Below threshold → no detection
- Result contains valid chord name when detected
- Different thresholds affect sensitivity

### Integration with Chromagram
- Verify detector uses chromagram internally
- Consistent results for same input

## ArpeggioDetector Test Cases

### `addNote()` / `detect()` / `clear()`
- Add C, E, G within window → detects C major arpeggio
- Add notes outside time window → no arpeggio detected
- `clear()` resets state → subsequent detect returns nothing
- Add single note → no arpeggio (need 3+ notes)
- Add two notes → no arpeggio
- Rapid notes within window → detected
- Notes spread beyond windowMs → not detected

### Stateful Behavior
- Multiple arpeggios in sequence (clear between)
- Overlapping arpeggios
- Window boundary edge cases

## Priority
Medium — Audio chord detection builds on the well-tested FFTProcessor and PitchDetector. Tests here use synthetic signals rather than real audio.

## Estimated Test Count
~30 test cases

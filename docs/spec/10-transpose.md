# Feature: Transpose Tool

**Status: IMPLEMENTED (v1.6)**

## Summary

Allow users to shift a set of chords up or down by semitones, simulating a capo or key change. Useful for finding easier chord shapes or matching a singer's vocal range.

## Motivation

- Transposing is one of the most common tasks for musicians
- Beginners often need to change keys to find chords they can play
- Capo simulation helps ukulele players adapt guitar-oriented chord sheets
- Quick mental math (what's F# transposed up 3?) is hard — the app can do it instantly

## Scope

### In Scope

- Transpose a single chord: show the resulting chord name
- Transpose a chord progression: shift all chords by N semitones
- Slider or +/- buttons to select semitone offset (-11 to +11)
- Show original and transposed chords side by side
- Capo label (e.g., "Capo 3" = transpose down 3 semitones)
- Integration with Chord Progressions feature (if implemented)

### Out of Scope

- Transposing chord sheets / song lyrics (could integrate with Song Chord Sheets later)
- Audio pitch shifting
- Changing chord quality during transpose (e.g., simplifying Bm to something easier)

## Technical Approach

### Core Logic

Transposition is simply pitch class arithmetic:

```kotlin
fun transpose(rootPitchClass: Int, semitones: Int): Int =
    (rootPitchClass + semitones + 12) % 12
```

The chord quality (major, minor, 7th, etc.) stays the same — only the root changes.

### Progression Transposition

```kotlin
fun transposeProgression(chords: List<Pair<Int, String>>, semitones: Int): List<Pair<Int, String>> =
    chords.map { (root, quality) -> transpose(root, semitones) to quality }
```

### UI Integration Options

**Option A: Standalone transpose tool**
- Dedicated section/screen where user inputs chords and transposes them

**Option B: Integrated into existing features (Recommended)**
- Add transpose controls to the Chord Library and Progressions tabs
- "+/- semitone" buttons that shift the selected root note
- Show "Original: C → Transposed: Eb (Capo 3)" labels

### Capo Mapping

Capo on fret N means the open strings are N semitones higher. To play a song written in key X with a capo on fret N, transpose all chords down by N semitones:

```kotlin
fun capoTranspose(originalRoot: Int, capoFret: Int): Int =
    (originalRoot - capoFret + 12) % 12
```

## Architecture

```
domain/
  └── Transpose.kt             -- transpose functions (new)

ui/
  └── TransposeControls.kt     -- reusable +/- UI component (new)
  └── ChordLibraryTab.kt       -- integrate transpose controls
  └── ProgressionsTab.kt       -- integrate transpose controls (if exists)
```

## UX Design

- **Controls**: Compact row with "−" and "+" buttons flanking a semitone counter
- **Display**:
  ```
  Original: C Major     [−] +3 [+]     Transposed: Eb Major
  ```
- **Capo mode**: Toggle to show capo fret number instead of semitone offset
- **Progression view**: All chords shift simultaneously, shown in a comparison row
- **Integration**: Appears as a collapsible section within the Chord Library or a floating control

## Edge Cases

- Transpose by 0 semitones: show same chord (no-op)
- Transpose by 12: same as 0 (wrap around)
- Negative semitones: transpose down (handled by modulo arithmetic)
- Enharmonic naming: use sharp/flat preference setting
- Transposing complex chords (e.g., Bbm7sus4): only root changes, suffix preserved

## Effort Estimate

- **Complexity**: Low
- **Estimated time**: 0.5–1 day
- **New files**: 2
- **Modified files**: 1–2
- **APK size impact**: Negligible

## Dependencies

- None

## Testing

- Unit test: transpose all 12 pitch classes by all 12 offsets, verify correctness
- Unit test: capo mapping produces correct results
- Unit test: chord quality is preserved after transposition
- Manual test: transpose UI updates chord names correctly
- Manual test: +12 and -12 return to original

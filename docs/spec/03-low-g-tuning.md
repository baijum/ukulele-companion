# Feature: Low-G Tuning Support

**Status: IMPLEMENTED (v1.5)**

## Summary

Add support for Low-G tuning (G3 C4 E4 A4) as an alternative to the default High-G tuning (G4 C4 E4 A4). Users can switch between tunings, and chord detection and voicing generation adapt accordingly.

## Motivation

- Low-G tuning is extremely popular, especially for fingerpicking and solo ukulele
- It extends the instrument's range by a full octave on the G string
- Many ukulele tutorial resources use Low-G tuning
- The codebase already uses tuning constants, making this a natural extension

## Scope

### In Scope

- A tuning selector (High-G / Low-G) accessible from the app bar or settings
- Chord detection works correctly with Low-G tuning
- Chord Library voicings regenerate for the selected tuning
- Fretboard note labels update to reflect the lower octave on the G string
- Persist tuning preference across sessions

### Out of Scope

- Arbitrary custom tunings (e.g., D-tuning, Baritone DGBE)
- Multiple instruments (guitar, banjo, etc.)
- Tuner functionality

## Technical Approach

### Current State

In `FretboardViewModel.kt`, tuning is defined as:

```kotlin
val OPEN_STRINGS = listOf(7, 0, 4, 9)  // G, C, E, A pitch classes
```

Pitch classes alone don't encode octave, so chord detection works identically for both tunings. The difference is purely in:
1. Note display (G4 vs G3 label)
2. Voicing sorting/ranking (some voicings sound different with a low G)

### Implementation Details

1. **Tuning enum** in `data/Tuning.kt`:
   ```kotlin
   enum class UkuleleTuning(val label: String, val octaves: List<Int>) {
       HIGH_G("High-G (Standard)", listOf(4, 4, 4, 4)),
       LOW_G("Low-G", listOf(3, 4, 4, 4)),
   }
   ```

2. **ViewModel changes**:
   - Add `selectedTuning: UkuleleTuning` to UI state
   - Add `setTuning()` method
   - Voicing generator receives tuning context for smarter sorting

3. **VoicingGenerator changes**:
   - With Low-G, certain voicings that use the G string for bass notes become more idiomatic
   - Adjust voicing scoring/sorting to prefer bass-on-G voicings in Low-G mode

4. **UI changes**:
   - Tuning selector in the app bar (dropdown or toggle)
   - Fretboard string labels update (show "G3" vs "G4")

5. **Preference**: Persist with DataStore (can share the same preferences file as flat/sharp toggle)

## Architecture

```
data/
  └── Tuning.kt              -- UkuleleTuning enum (new)
  └── VoicingGenerator.kt    -- accept tuning for sorting
  └── UserPreferences.kt     -- persist tuning choice

viewmodel/
  └── FretboardViewModel.kt  -- tuning state & setter
  └── ChordLibraryViewModel.kt -- regenerate voicings on tuning change

ui/
  └── FretboardScreen.kt     -- tuning selector
  └── FretboardView.kt       -- string labels
```

## UX Design

- Selector: dropdown menu from the app bar (tap "High-G" label to switch)
- Clear label showing current tuning at all times
- When switching tuning, clear current fretboard selections (or keep them and re-detect)
- Chord Library voicings regenerate automatically

## Edge Cases

- Switching tuning while notes are selected: re-run chord detection (pitch classes don't change, so results are the same, but display may differ)
- Voicing generator: some voicings are only practical in one tuning — consider flagging these
- Ensure the "finger positions" display still works correctly

## Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 1 day
- **New files**: 1 (Tuning.kt)
- **Modified files**: 4–5
- **APK size impact**: Negligible

## Dependencies

- None (may share DataStore dependency with flat note names feature)

## Testing

- Unit test: verify pitch classes remain correct for both tunings
- Unit test: voicing generator produces valid voicings for Low-G
- Manual test: switching tuning updates labels and voicings
- Manual test: preference persists after restart

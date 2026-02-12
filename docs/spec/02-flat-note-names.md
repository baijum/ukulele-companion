# Feature: Flat Note Names Toggle

**Status: IMPLEMENTED (v1.5)**

## Summary

Add the ability to display enharmonic note names using flats (Db, Eb, Gb, Ab, Bb) instead of only sharps (C#, D#, F#, G#, A#). Users can toggle between the two conventions.

## Motivation

- Musical keys like F major, Bb major, Eb major conventionally use flat names
- Players coming from band/orchestral backgrounds are more familiar with flats
- Showing both conventions is more musically accurate and educational
- Simple change with meaningful usability improvement

## Scope

### In Scope

- A "Sharp/Flat" toggle accessible from the main screen or a settings area
- All note displays update accordingly: fretboard labels, chord names, chord result, chord library
- Persist the user's preference across app sessions
- Chord names adapt (e.g., "Db Major" instead of "C# Major")

### Out of Scope

- Context-aware enharmonic spelling (e.g., automatically choosing sharp vs flat based on key)
- Double sharps/flats (e.g., F## or Bbb)
- Key signature display

## Technical Approach

### Data Changes

Add a parallel note name list in `Notes.kt`:

```kotlin
val NOTE_NAMES_SHARP = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
val NOTE_NAMES_FLAT  = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
```

### Preference Storage

Use Jetpack `DataStore` (Preferences) to persist the choice:
- Key: `use_flats` (Boolean, default `false`)
- Lightweight, no database needed

### Display Logic

- `pitchClassToName(pitchClass, useFlats)` — select from the appropriate list
- All composables that display note names read the preference from ViewModel state

### ViewModel Changes

- Add `useFlats: Boolean` to `FretboardUiState`
- Add `toggleNoteNameStyle()` method
- Both `FretboardViewModel` and `ChordLibraryViewModel` observe the preference

### UI Changes

- Add a small toggle button (e.g., "♯ / ♭") in the top app bar or as a settings icon
- All note name displays re-render when toggled

## Architecture

```
data/
  └── Notes.kt              -- add NOTE_NAMES_FLAT list
  └── UserPreferences.kt    -- DataStore wrapper (new)

viewmodel/
  └── FretboardViewModel.kt -- useFlats state, toggle method

ui/
  └── FretboardScreen.kt    -- toggle button in app bar
  └── FretboardView.kt      -- pass useFlats to note labels
  └── ChordResultView.kt    -- use correct note names
  └── ChordLibraryTab.kt    -- use correct root note names
```

## UX Design

- Toggle location: small icon button in the top app bar (♯/♭ symbol)
- Immediate visual update across all screens when toggled
- Default: sharps (current behavior, no surprise for existing users)
- Tooltip or label on first use: "Switch between sharp and flat note names"

## Edge Cases

- Notes without enharmonic equivalents (C, D, E, F, G, A, B) display the same in both modes
- Chord symbols adapt: "C#" → "Db", "G#m" → "Abm", etc.
- Root note selector in Chord Library updates chip labels accordingly

## Effort Estimate

- **Complexity**: Low
- **Estimated time**: 0.5–1 day
- **New files**: 1 (UserPreferences)
- **Modified files**: 5–6
- **APK size impact**: Negligible

## Dependencies

- `androidx.datastore:datastore-preferences` (add to dependencies)

## Testing

- Unit test: verify both note name lists produce correct names for all 12 pitch classes
- Unit test: chord detection output uses the selected naming convention
- Manual test: toggle switch updates all displayed note names immediately
- Manual test: preference persists after app restart

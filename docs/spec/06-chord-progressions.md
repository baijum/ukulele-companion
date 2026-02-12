# Feature: Chord Progressions

**Status: IMPLEMENTED (v1.6)**

## Summary

Show common chord progressions for a selected key, helping beginners understand which chords naturally go together and enabling them to start playing songs quickly.

## Motivation

- "What chords go with C?" is one of the most common beginner questions
- Chord progressions are the backbone of nearly every song
- Displaying progressions bridges the gap between knowing individual chords and playing music
- Highly educational — teaches music theory concepts (I-IV-V, relative minor) through practice

## Scope

### In Scope

- Select a key (root note + major/minor)
- Display common progressions using Nashville number notation (I, IV, V, vi, etc.)
- Show the actual chord names for the selected key (e.g., in C major: C, F, G, Am)
- Tap any chord in the progression to view it on the fretboard
- Include 5–8 standard progressions per key

### Out of Scope

- Custom/user-created progressions
- Playback of progressions with audio (could combine with Sound Playback feature later)
- Tempo, rhythm, or strumming patterns within progressions
- Song database or song matching

## Technical Approach

### Progression Data

Define progressions as interval patterns from the root:

```kotlin
data class Progression(
    val name: String,
    val description: String,
    val degrees: List<ChordDegree>,
)

data class ChordDegree(
    val interval: Int,      // semitones from root (0, 2, 4, 5, 7, 9, 11)
    val quality: String,    // "Major", "Minor", "Dom7", etc.
    val numeral: String,    // "I", "ii", "iii", "IV", "V", "vi", "vii°"
)
```

### Standard Progressions to Include

| Name | Pattern | Example in C |
|------|---------|-------------|
| Pop / "Four Chords" | I – V – vi – IV | C – G – Am – F |
| Classic Rock | I – IV – V | C – F – G |
| 50s / Doo-Wop | I – vi – IV – V | C – Am – F – G |
| Blues | I – I – IV – IV – I – I – V – IV – I | 12-bar blues |
| Folk / Country | I – IV – I – V | C – F – C – G |
| Sad / Emotional | vi – IV – I – V | Am – F – C – G |
| Jazz ii-V-I | ii – V – I | Dm – G – C |
| Reggae | I – IV – V – IV | C – F – G – F |

### Scale-Based Chord Generation

For a given key, derive diatonic chords from the major (or minor) scale:

**Major scale degrees:**
- I = Major, ii = Minor, iii = Minor, IV = Major, V = Major, vi = Minor, vii° = Diminished

**Minor scale degrees:**
- i = Minor, ii° = Dim, III = Major, iv = Minor, v = Minor, VI = Major, VII = Major

### ViewModel

New `ProgressionViewModel`:
- `selectedKey: StateFlow<Int>` (pitch class)
- `selectedScale: StateFlow<ScaleType>` (Major/Minor)
- `progressions: StateFlow<List<ResolvedProgression>>`
- Each `ResolvedProgression` contains the actual chord names for the selected key

### UI Flow

1. User selects a key (root note chips) and scale (Major/Minor toggle)
2. List of common progressions appears with names and descriptions
3. Each progression shows the chord sequence as tappable chips
4. Tapping a chord chip navigates to the Explorer with that chord's voicing applied

## Architecture

```
data/
  └── Progressions.kt          -- progression definitions (new)
  └── ScaleUtils.kt            -- scale degree to chord mapping (new)

viewmodel/
  └── ProgressionViewModel.kt  -- key/scale selection, resolution (new)

ui/
  └── FretboardScreen.kt       -- add Progressions tab
  └── ProgressionsTab.kt       -- key selector + progression list (new)
```

## UX Design

- **New tab**: "Progressions" alongside Explorer and Chord Library
- **Key selector**: Same root note chips as Chord Library (reuse component)
- **Scale toggle**: "Major / Minor" toggle buttons
- **Progression cards**: Each card shows:
  - Name (e.g., "Pop / Four Chords")
  - Numeral notation (I – V – vi – IV)
  - Resolved chords as tappable chips (C – G – Am – F)
  - Brief description
- **Interaction**: Tap a chord chip → switch to Explorer with that voicing
- **Color coding**: Major chords in one color, minor in another, diminished in a third

## Edge Cases

- Keys with enharmonic ambiguity: use the sharp/flat preference setting
- Diminished chords may not have great ukulele voicings — show best available
- Some progressions are the same pattern starting on different degrees (e.g., vi–IV–I–V is I–V–vi–IV starting on vi) — include both as they're used differently

## Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 2–3 days
- **New files**: 3–4
- **Modified files**: 2–3
- **APK size impact**: Negligible

## Dependencies

- None (pure data and logic)

## Testing

- Unit test: verify diatonic chord generation for all 12 major keys
- Unit test: verify diatonic chord generation for all 12 minor keys
- Unit test: progression resolution produces correct chord names
- Manual test: select key, verify progression chords are correct
- Manual test: tap chord chip, verify correct voicing appears on fretboard

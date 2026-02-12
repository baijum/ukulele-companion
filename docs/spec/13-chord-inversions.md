# Feature: Chord Inversions

**Status: IMPLEMENTED (v2.5) — Ideas 1–5; Idea 6 added in v2.6**

## Summary

Add chord inversion awareness throughout the app to help users understand, identify, and practice inversions on ukulele. Inversions are a fundamental music theory concept where the same chord is played with a different note in the bass, producing a subtly different character.

## Motivation

- Inversions are essential for smooth chord transitions (voice leading)
- Understanding inversions deepens a player's grasp of chord construction
- Many ukulele voicings are naturally inversions, but players don't realize it
- Knowing inversions unlocks more musical expression with the same chord knowledge
- The app already generates multiple voicings per chord — labeling inversions adds educational value with minimal effort

## Background

A chord inversion is determined by which chord tone is the **lowest-pitched note** (bass note):

- **Root position**: root in the bass (e.g., C-E-G)
- **1st inversion**: 3rd in the bass (e.g., E-G-C)
- **2nd inversion**: 5th in the bass (e.g., G-C-E)
- **3rd inversion** (7th chords only): 7th in the bass (e.g., Bb-C-E-G)

On ukulele with re-entrant tuning (G4-C4-E4-A4), the bass note is typically on the **C string** (string index 1, the lowest-pitched open string), not the top G string. This makes inversions particularly interesting to explore since the bass note isn't always on the "first" string visually.

## Scope

### In Scope

The feature is organized into six progressive ideas, from simplest to most ambitious.

#### Idea 1: Label Inversions on Existing Voicings

- Compute the lowest-pitched note in each `ChordVoicing` by comparing actual pitches (accounting for re-entrant G string tuning)
- Determine inversion by checking the interval of the bass note relative to the chord root
- Display a small badge on each chord diagram preview in the Chord Library: "Root", "1st Inv", "2nd Inv", "3rd Inv"
- Add a utility function in `ChordInfo`:
  - `determineInversion(voicing, rootPitchClass, tuning)` -> inversion label string
  - `findBassNote(voicing, tuning)` -> Note (the lowest-pitched note)

#### Idea 2: Show Inversion in Explorer Detection

- When a chord is detected on the fretboard, display the inversion alongside the existing chord details
- Use slash notation: "C/E (1st Inversion)" instead of just "C"
- The bass note is determined from the fretboard selections + current tuning
- Integrates naturally with the existing interval breakdown display in `ChordResultView`

#### Idea 3: Inversion Filter in Chord Library

- Add `FilterChip` buttons in `ChordLibraryTab` to filter voicings by inversion type
- Filter options: All / Root / 1st Inv / 2nd Inv (/ 3rd Inv for 7th chords)
- Uses the same inversion detection utility from Idea 1
- Helps users deliberately browse and practice specific inversions

#### Idea 4: Bass Note Highlighting on Fretboard and Diagrams

- Visually distinguish the bass note from other fretted notes
- On the interactive fretboard: draw the bass note dot in a different color or with a distinct outline
- On chord diagram previews: same visual distinction
- Provides an immediate visual cue about which note is "grounding" the chord

#### Idea 5: Inversion Comparison View

- A dedicated sub-view or mode that groups voicings by inversion type
- User picks a chord (e.g., C major) and sees representative voicings organized under "Root Position", "1st Inversion", "2nd Inversion" headings
- Each group has a "Play" button so users can hear how the same chord sounds in different inversions
- Could be a sub-view within the Chord Library or a new section

#### Idea 6: Voice Leading Exercises *(Implemented)*

- Builds on the existing Chord Progressions feature
- A "Voice Leading" button on each progression card computes the optimal voicing path
- Uses dynamic programming (shortest path through voicing graph) to minimize total finger movement
- Dedicated `VoiceLeadingView` with:
  - Chord timeline showing the full progression with highlighted current transition pair
  - Side-by-side chord diagrams with common tone highlighting (secondary color)
  - Per-string movement breakdown (which fingers stay, which move, and direction)
  - Educational tips ("Keep your finger on the C string — same note in both chords")
  - Audio playback: individual chord play buttons + "Play All" for the full path
  - Prev/Next navigation to step through each transition
  - Total path summary (total frets movement across all transitions)
- Key files: `domain/VoiceLeading.kt` (algorithm), `ui/VoiceLeadingView.kt` (UI),
  modified `ui/ProgressionsTab.kt`, `ui/ChordDiagramPreview.kt` (common tone colors),
  `ui/FretboardScreen.kt` (parameter wiring)

### Out of Scope

- Audio analysis / microphone-based inversion detection
- Inversion-specific exercises with scoring or gamification
- Guitar or other instrument support

## Implementation Notes

### Inversion Detection Algorithm

```
1. For each string in the voicing, compute the actual MIDI pitch:
   midiPitch = (openPitchClass + fret) + (octave * 12)
   (account for re-entrant G string being octave 4, not 3)

2. Find the string with the lowest MIDI pitch (the bass note)

3. Compute the interval of the bass note relative to the chord root:
   interval = (bassPitchClass - rootPitchClass + 12) % 12

4. Match the interval against the chord formula:
   - If interval == 0: Root position
   - If interval matches the 3rd (3 or 4 semitones): 1st inversion
   - If interval matches the 5th (7 semitones): 2nd inversion
   - If interval matches the 7th (10 or 11 semitones): 3rd inversion
```

### Key Files

- `domain/ChordInfo.kt` — `determineInversion()`, `findBassNote()`, `slashNotation()` utilities
- `domain/VoiceLeading.kt` — DP algorithm for optimal voice leading paths
- `ui/ChordLibraryTab.kt` — inversion badges, filter chips, compare mode
- `ui/ChordResultView.kt` — slash notation and inversion label in explorer
- `ui/ChordDiagramPreview.kt` — bass note highlighting + common tone highlighting
- `ui/VoiceLeadingView.kt` — dedicated voice leading view with transition diagrams
- `ui/ProgressionsTab.kt` — voice leading button and view integration
- `ui/FretboardScreen.kt` — parameter wiring for voice leading

### Implementation Order (Completed)

1. Ideas 1 + 2 — shared detection logic, immediate educational value
2. Idea 3 — filter chips in Chord Library
3. Idea 4 — bass note visual highlighting on diagrams
4. Idea 5 — Inversion Comparison View
5. Idea 6 — Voice Leading Exercises (DP algorithm + dedicated view)

## Testing

- Verify inversion detection for standard tuning (High-G) and Low-G tuning
- Confirm that re-entrant tuning correctly identifies C string as bass for open chords
- Test with all chord categories: triads (Root/1st/2nd), 7th chords (Root/1st/2nd/3rd), suspended chords
- Verify filter in Chord Library correctly groups voicings
- Verify slash notation displays correctly for all roots and inversions
- Test bass note highlighting renders correctly in both light and dark themes

## References

- [Music theory: Chord inversions](https://en.wikipedia.org/wiki/Inversion_(music))
- Existing voicing generation: `data/VoicingGenerator.kt`
- Existing interval display: `domain/ChordInfo.kt`
- Existing chord detection: `domain/ChordDetector.kt`

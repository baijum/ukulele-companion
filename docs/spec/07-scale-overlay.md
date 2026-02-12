# Feature: Scale Overlay on Fretboard

**Status: IMPLEMENTED (v1.6)**

## Summary

Highlight notes belonging to a selected musical scale directly on the fretboard, helping users visualize which notes "belong together" in a given key and understand the relationship between scales and chords.

## Motivation

- Scales are fundamental to music theory and improvisation
- Visual overlay on the fretboard makes abstract theory tangible
- Helps users understand why certain chords work in certain keys
- Enables users to experiment with melodies and riffs
- Natural complement to the existing chord features

## Scope

### In Scope

- Select a key (root note) and scale type
- Overlay scale notes on the fretboard with distinct visual markers
- Scale notes coexist with chord selections (different visual styles)
- Highlight root notes of the scale distinctly
- Supported scales: Major, Natural Minor, Pentatonic Major, Pentatonic Minor, Blues, Dorian, Mixolydian

### Out of Scope

- Scale playback / audio (handled by Sound Playback feature)
- Scale practice exercises or games
- Modes beyond the listed set (can add more later)
- Scale patterns / positions (guitar-style CAGED system)

## Technical Approach

### Scale Data

```kotlin
data class Scale(
    val name: String,
    val intervals: List<Int>,  // semitones from root
)

val SCALES = listOf(
    Scale("Major",             listOf(0, 2, 4, 5, 7, 9, 11)),
    Scale("Natural Minor",     listOf(0, 2, 3, 5, 7, 8, 10)),
    Scale("Pentatonic Major",  listOf(0, 2, 4, 7, 9)),
    Scale("Pentatonic Minor",  listOf(0, 3, 5, 7, 10)),
    Scale("Blues",             listOf(0, 3, 5, 6, 7, 10)),
    Scale("Dorian",            listOf(0, 2, 3, 5, 7, 9, 10)),
    Scale("Mixolydian",        listOf(0, 2, 4, 5, 7, 9, 10)),
)
```

### Note Highlighting

For a given root + scale, compute the set of pitch classes in the scale:

```kotlin
fun scaleNotes(root: Int, scale: Scale): Set<Int> =
    scale.intervals.map { (root + it) % 12 }.toSet()
```

On the fretboard, for each string/fret position:
- If the note is in the scale → show a colored dot (e.g., light blue)
- If the note is the root → show a stronger dot (e.g., darker blue or outlined)
- If the note is also a chord selection → show both indicators

### ViewModel Changes

Add to `FretboardViewModel` or create `ScaleOverlayState`:
- `scaleRoot: Int?` (null = no overlay)
- `selectedScale: Scale?`
- `scaleNotes: Set<Int>` (derived)
- `toggleScaleOverlay()`, `setScaleRoot()`, `setScale()`

### UI Changes

- Scale selector panel (collapsible or in a bottom sheet)
- Fretboard draws scale dots behind/alongside chord selection dots
- Toggle to show/hide the overlay
- Legend showing what the colors mean

## Architecture

```
data/
  └── Scales.kt               -- Scale data class and definitions (new)

viewmodel/
  └── FretboardViewModel.kt   -- scale overlay state

ui/
  └── FretboardView.kt        -- draw scale note indicators
  └── FretboardScreen.kt      -- scale selector panel
  └── ScaleSelector.kt        -- root + scale type picker (new)
```

## UX Design

- **Activation**: Button or toggle in the Explorer tab (e.g., "Scales" chip)
- **Selector**: Bottom sheet or dropdown with root note + scale type
- **Visual style**:
  - Scale notes: semi-transparent colored circles on fret positions
  - Root notes: solid colored circles or circles with border
  - Chord selections: existing style (filled dots), drawn on top of scale dots
- **Color**: Use a distinct color from chord selections (e.g., blue for scale, orange for chord)
- **Legend**: Small inline legend: "● Root  ○ Scale note"
- **Dismissal**: Tap toggle again or "Clear" button to remove overlay

## Edge Cases

- Scale overlay + chord selections simultaneously: both should be visible with distinct styling
- All 12 frets × 4 strings = 52 positions; some scales cover many — ensure visual clarity
- Chromatic scale (all 12 notes) would highlight everything — exclude or warn
- Switching root/scale while chord is selected: overlay updates, chord remains

## Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 1.5–2 days
- **New files**: 2
- **Modified files**: 3
- **APK size impact**: Negligible

## Dependencies

- None

## Testing

- Unit test: verify scale note computation for all roots and scale types
- Unit test: verify root note is always in the scale note set
- Manual test: select C Major scale, verify correct notes highlighted on fretboard
- Manual test: scale overlay + chord selection display correctly together
- Manual test: switching scales updates overlay immediately

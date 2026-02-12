# Feature: Left-Handed Mode

**Status: IMPLEMENTED (v1.5)**

## Summary

Add a left-handed mode that mirrors the fretboard horizontally, so the nut appears on the right and fret numbers increase to the left. This matches how a left-handed player holds the instrument.

## Motivation

- Roughly 10% of people are left-handed
- Left-handed players hold the ukulele mirrored, so a standard fretboard diagram feels backwards
- Simple UI transformation with high impact for affected users
- Many competing apps lack this feature

## Scope

### In Scope

- Mirror the fretboard layout (strings stay top-to-bottom, frets reverse left-to-right)
- Toggle in settings or app bar
- Chord diagram previews in the Library also mirror
- Persist preference across sessions

### Out of Scope

- Restringing simulation (reversing string order, as some left-handed players do)
- Different chord voicings for left-handed play (voicings are the same, just visually mirrored)

## Technical Approach

### Implementation

The fretboard is drawn using Compose Canvas and Row/Column layouts. Mirroring requires:

1. **FretboardView.kt**: Reverse the order of fret columns (0–12 becomes 12–0)
2. **ChordDiagramPreview.kt**: Reverse the column drawing order
3. **State**: Add `isLeftHanded: Boolean` to UI state, persisted via DataStore

The underlying data model (pitch classes, selections, chord detection) is completely unaffected — only the visual rendering changes.

### Key Insight

Since selections are stored as `Map<Int, Int?>` (stringIndex → fret), and chord detection operates on pitch classes, no logic changes are needed. Only the rendering order of fret columns in the UI is reversed.

```kotlin
val fretRange = if (isLeftHanded) (maxFret downTo 0) else (0..maxFret)
```

## Architecture

```
data/
  └── UserPreferences.kt     -- persist left-handed preference

viewmodel/
  └── FretboardViewModel.kt  -- isLeftHanded state

ui/
  └── FretboardView.kt       -- reverse fret column order
  └── ChordDiagramPreview.kt -- reverse diagram columns
  └── FretboardScreen.kt     -- toggle button
```

## UX Design

- Toggle: icon button in app bar (mirror/flip icon)
- Fret numbers (0, 1, 2...) still display correctly, just right-to-left
- Nut (fret 0) appears on the right side
- String labels (G, C, E, A) remain on the left (closest to nut, now on right) — or follow the nut side
- Smooth transition: no animation needed, instant flip

## Edge Cases

- Chord diagram previews must also respect the setting
- Fret number labels must remain readable
- Touch targets remain the same size (no layout compression)

## Effort Estimate

- **Complexity**: Low
- **Estimated time**: 0.5 day
- **New files**: 0
- **Modified files**: 3–4
- **APK size impact**: None

## Dependencies

- DataStore (shared with other preference features)

## Testing

- Manual test: tap frets in left-handed mode, verify correct notes are selected
- Manual test: chord detection produces same results regardless of mode
- Manual test: chord diagrams in Library mirror correctly
- Manual test: preference persists after restart

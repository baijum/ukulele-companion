# Feature: Song Chord Sheets

**Status: IMPLEMENTED (v1.6)**

## Summary

Allow users to create simple chord sheets — lyrics with chord names placed above the words — and tap any chord name to instantly see it on the fretboard. A personal, offline songbook.

## Motivation

- Players learn songs by following chord sheets (lyrics + chords)
- Having chords linked to the fretboard explorer creates a seamless learning flow
- Users can build a personal songbook without needing internet or third-party apps
- Bridges the gap between knowing chords and actually playing songs

## Scope

### In Scope

- Create, edit, and delete chord sheets
- Simple text editor with chord markers above lyrics
- Tap any chord name in a sheet to view it on the fretboard
- List of saved chord sheets (personal songbook)
- Basic formatting: title, artist (optional), chord + lyric lines
- Persist chord sheets locally

### Out of Scope

- Importing chord sheets from external sources / websites
- Auto-scroll or tempo-synced scrolling
- Audio playback alongside chord sheets
- Chord transposition within sheets (see separate Transpose feature)
- Sharing or exporting chord sheets
- Chord auto-detection from lyrics

## Technical Approach

### Chord Sheet Format

Use a simple text format where chord lines are distinguished from lyric lines:

```
[Title: Somewhere Over the Rainbow]
[Artist: IZ]

[C]    [Em]       [F]      [C]
Somewhere over the rainbow, way up high
[F]      [C]           [G]        [Am]   [F]
And the dreams that you dream of, once in a lullaby
```

Alternatively, use inline markers:
```
Some[C]where over the [Em]rainbow, [F]way up [C]high
```

### Recommended: Inline Markers

Inline chord markers `[ChordName]` are easier to parse and align with lyrics.

### Data Storage

Use **DataStore with JSON** or **Room** to store chord sheets:

```kotlin
data class ChordSheet(
    val id: String,          // UUID
    val title: String,
    val artist: String?,
    val content: String,     // raw text with [chord] markers
    val createdAt: Long,
    val updatedAt: Long,
)
```

### Chord Parsing

Regex to find chord markers: `\[([A-G][#b]?(?:m|maj|dim|aug|sus|7|m7|maj7|6|9)*)\]`

Extract chord names, match against known `ChordFormula` symbols, and make them tappable.

### UI Flow

1. **Songbook tab/screen**: List of saved chord sheets
2. **Sheet viewer**: Displays lyrics with highlighted chord names
3. **Tap a chord**: Bottom sheet or navigation to Explorer with that chord's voicing
4. **Edit mode**: Simple text editor for creating/editing sheets

## Architecture

```
data/
  └── ChordSheet.kt           -- data class (new)
  └── ChordSheetRepository.kt -- persistence (new)
  └── ChordParser.kt          -- extract chord names from text (new)

viewmodel/
  └── SongbookViewModel.kt    -- sheet list and CRUD (new)
  └── ChordSheetViewModel.kt  -- viewing/editing a single sheet (new)

ui/
  └── SongbookScreen.kt       -- list of sheets (new)
  └── ChordSheetView.kt       -- sheet viewer with tappable chords (new)
  └── ChordSheetEditor.kt     -- text editor for creating sheets (new)
```

## UX Design

- **Navigation**: Accessible from the main app bar or as an additional tab
- **Songbook list**: Cards with title, artist, and last edited date
- **Viewer**:
  - Monospace font for alignment
  - Chord names in a distinct color (e.g., primary theme color)
  - Chord names are tappable — triggers a bottom sheet with the chord diagram
  - Scroll vertically for long songs
- **Editor**:
  - Plain text input
  - Helper toolbar with common chord names for quick insertion
  - Preview toggle to see formatted output
- **Empty state**: "No songs yet. Tap + to create your first chord sheet."

## Edge Cases

- Unknown chord names in brackets: display as plain text (not tappable), or show a "?" indicator
- Very long songs: lazy scrolling, ensure smooth performance
- Special characters in lyrics: ensure parser doesn't break on apostrophes, etc.
- Multiple voicings for a chord: show the default/first voicing, user can browse alternatives

## Effort Estimate

- **Complexity**: High
- **Estimated time**: 3–5 days
- **New files**: 5–7
- **Modified files**: 2–3
- **APK size impact**: Minimal

## Dependencies

- Room or DataStore (for persistence)
- `kotlinx.serialization` (if using DataStore)

## Testing

- Unit test: chord parser extracts correct chord names from sample text
- Unit test: parser handles edge cases (empty brackets, unknown chords, special characters)
- Unit test: CRUD operations on chord sheets
- Manual test: create a chord sheet, view it, tap chords to see on fretboard
- Manual test: edit and delete sheets
- Manual test: persistence across app restarts

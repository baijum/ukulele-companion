# Feature: Chord Favorites / Bookmarks

**Status: IMPLEMENTED (v1.6)**

## Summary

Allow users to save frequently used chord voicings to a personal favorites list for quick access, avoiding the need to search through the Chord Library each time.

## Motivation

- Players develop a repertoire of "go-to" chord shapes they use regularly
- Navigating the full Chord Library every time is tedious for known chords
- Favorites provide a personalized, quick-access chord reference
- Encourages users to build their chord vocabulary progressively

## Scope

### In Scope

- Save/unsave a voicing from the Chord Library (bookmark icon on each voicing card)
- Dedicated "Favorites" tab or section for quick access to saved voicings
- Display saved voicings with chord name, diagram preview, and finger positions
- Tap a favorite to apply it to the fretboard (same as Chord Library behavior)
- Remove voicings from favorites
- Persist favorites across app sessions

### Out of Scope

- Organizing favorites into folders or categories (future enhancement)
- Sharing favorites with other users
- Cloud sync
- Reordering favorites (use natural sort: by root note, then chord type)

## Technical Approach

### Data Storage

Use **Room database** (lightweight, offline, no setup):

```kotlin
@Entity(tableName = "favorites")
data class FavoriteVoicing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rootNote: Int,          // pitch class 0-11
    val chordSymbol: String,    // e.g., "m7", "sus2"
    val frets: String,          // serialized: "0,0,0,3"
    val addedAt: Long,          // timestamp
)
```

Alternatively, use **DataStore with JSON serialization** to avoid adding Room as a dependency. For a small list of favorites (likely <100 items), DataStore is sufficient.

### Recommended: DataStore (Simpler)

- Serialize favorites as JSON in DataStore
- Use `kotlinx.serialization` for encoding/decoding
- Avoids Room dependency and schema migrations
- Sufficient for the expected data volume

### ViewModel

New `FavoritesViewModel`:
- `favorites: StateFlow<List<FavoriteVoicing>>`
- `addFavorite(rootNote, chordSymbol, frets)`
- `removeFavorite(id)`
- `isFavorite(rootNote, chordSymbol, frets): Boolean`

### UI Changes

1. **Chord Library**: Add a bookmark/heart icon on each voicing card
2. **New tab or section**: "Favorites" tab alongside Explorer and Chord Library
3. **Favorites display**: Grid of chord diagram previews (reuse `ChordDiagramPreview`)
4. **Empty state**: Friendly message when no favorites saved yet

## Architecture

```
data/
  └── FavoriteVoicing.kt       -- data class (new)
  └── FavoritesRepository.kt   -- DataStore read/write (new)

viewmodel/
  └── FavoritesViewModel.kt    -- favorites state management (new)

ui/
  └── FretboardScreen.kt       -- add Favorites tab
  └── FavoritesTab.kt          -- favorites grid (new)
  └── ChordDiagramPreview.kt   -- add bookmark icon overlay
  └── ChordLibraryTab.kt       -- bookmark toggle on cards
```

## UX Design

- **Add to favorites**: Heart/bookmark icon on each voicing card in the Library; filled when saved
- **Favorites tab**: Third tab "Favorites" (or accessible via icon in app bar)
- **Card display**: Same as Library cards — mini diagram + chord name + finger positions
- **Remove**: Long-press or tap filled heart icon to remove
- **Empty state**: Illustration or text: "No favorites yet. Browse the Chord Library to save voicings."
- **Order**: Group by root note (C, C#, D...), then alphabetical by chord type

## Edge Cases

- Duplicate prevention: same voicing can't be saved twice
- Voicing equality: compare by root note + chord symbol + fret list
- Large favorites list: LazyVerticalGrid handles scrolling efficiently
- Data migration: if format changes in future, handle gracefully

## Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 1.5–2 days
- **New files**: 3–4
- **Modified files**: 3–4
- **APK size impact**: Negligible

## Dependencies

- `kotlinx.serialization` (for DataStore JSON approach)
- Or `androidx.room` (if database approach chosen)

## Testing

- Unit test: add, remove, check duplicate prevention
- Unit test: serialization/deserialization roundtrip
- Manual test: save voicing from Library, verify it appears in Favorites
- Manual test: tap favorite to apply to fretboard
- Manual test: favorites persist after app restart
- Manual test: empty state displays correctly

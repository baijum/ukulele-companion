# Songbook Feature Gap Analysis: Ukulele Companion vs Popular Apps

## Context
Research into songbook features available in popular ukulele/guitar apps (Ultimate Guitar, Yousician, Songsterr, SongBook ChordPro, Kala, GuitarTapp) compared against what Ukulele Companion currently offers. The goal is to identify missing features that would bring the app closer to parity with market leaders.

---

## Current App Songbook Features (Already Implemented)

| Feature | Android | iOS |
|---------|---------|-----|
| ChordPro import/export | Yes | Yes |
| Plain text import | Yes | Yes |
| Song create/edit/delete | Yes | Yes |
| Transpose (+/- semitones) | Yes | Yes |
| Capo support (0-12) | Yes | Yes |
| Key detection from chords | Yes | Yes |
| Chords above lyrics display | Yes | Yes |
| Tap chord for diagram + playback | Yes | Yes |
| Labels/tags for organization | Yes | Yes |
| Search by title/artist | Yes | Yes |
| Sort (modified, added, title, artist) | Yes | Yes |
| Label filtering | Yes | Yes |
| Strum pattern assignment | Yes | Yes |
| Share (ChordPro, plain text, clipboard) | Yes | Yes |
| Backup/restore (JSON, cross-platform) | Yes | Yes |
| Chord chip quick-insert in editor | Yes | Yes |
| Edit/preview toggle in editor | Yes | Yes |
| Left-handed chord diagrams | Yes | Yes |
| Auto-scroll with speed control | Yes (0.5x-3x) | Yes (0.5x-3x) |
| Paste import (ChordPro) | Yes | Yes |
| Songwriter mode (guided flow) | Yes | Yes |

---

## Gap Analysis: Missing Features

### Tier 1 - High Impact, Commonly Found in Competitors

| # | Feature | Description | Found In |
|---|---------|-------------|----------|
| ~~1~~ | ~~**Auto-scroll (Android)**~~ | ~~Already implemented on both platforms (0.5x-3x speed, play/pause/stop).~~ | ~~N/A~~ |
| 2 | **Setlists / Playlists** | Group songs into ordered setlists for gigs or practice sessions. Navigate between songs sequentially. | SongBook ChordPro, GuitarTapp, Ultimate Guitar |
| 3 | **Font size / display customization** | Pinch-to-zoom or font size slider for song display. Critical for readability at distance (e.g., music stand). | SongBook, Ultimate Guitar, ChordPro apps |
| 4 | ~~**Dark mode for song display**~~ | ~~App already supports system light/dark/high-contrast themes. Song-specific toggle not needed.~~ | ~~N/A~~ |
| 5 | **PDF export / printing** | Export songs as formatted PDF for printing or sharing with bandmates who don't use the app. | SongBook ChordPro, GuitarTapp |

### Tier 2 - Medium Impact, Differentiating Features

| # | Feature | Description | Found In |
|---|---------|-------------|----------|
| 6 | **Metronome integration in songbook** | Embed metronome with song's tempo (from `{tempo}` directive). Start/stop while viewing song. | ChordPro apps, Kala |
| 7 | **Performance / stage mode** | Full-screen, distraction-free display with large text, auto-scroll, and minimal UI. External display support (AirPlay/Chromecast). | SongBook ChordPro, GuitarTapp |
| 8 | **Setlist auto-advance** | Automatically move to the next song in a setlist after scrolling completes or on timer. | SongBook ChordPro |
| 9 | **Song sections navigation** | Jump-to-section buttons (Verse 1, Chorus, Bridge) parsed from ChordPro section markers. | ChordPro apps, Ultimate Guitar |
| 10 | **Duplicate song** | Clone an existing song to create variations (different key, arrangement). | SongBook ChordPro |
| 11 | **Song statistics** | Track practice count, last played date, total time spent on each song. | Yousician, Kala |
| 12 | **Batch operations** | Multi-select songs for bulk delete, label assignment, or export. | Ultimate Guitar |

### Tier 3 - Nice to Have, Advanced Features

| # | Feature | Description | Found In |
|---|---------|-------------|----------|
| 13 | **External device control** | Bluetooth foot pedal / page turner support for hands-free page turning and auto-scroll control. | SongBook ChordPro, GuitarTapp |
| 14 | **Cloud sync** | Sync songbook across devices (iCloud / Google Drive). | Ultimate Guitar, SongBook |
| 15 | **Song annotations / notes** | Add personal notes, highlights, or markings to specific sections of a song. | Ultimate Guitar |
| 16 | **Multiple chord voicings per song** | Allow user to pick preferred voicing for each chord in a song (not just library default). | Guitar Pro, Ultimate Guitar |
| 17 | **Tablature display** | Render `{start_of_tab}` content with monospaced tab formatting. | Ultimate Guitar, Songsterr, Guitar Pro |
| 18 | **Audio playback / backing tracks** | Play backing track alongside song display, synced with scroll. | Ultimate Guitar, Yousician, Kala |
| 19 | **Loop sections** | Mark and loop specific sections for practice (e.g., repeat chorus 5 times). | Ultimate Guitar, Yousician |
| 20 | **Smart scroll** | Auto-scroll that adapts speed based on section length or playing pace. | Yousician |
| 21 | **Import from URL** | Paste a URL to import songs from popular tab sites. | Ultimate Guitar |
| 22 | **Folders / hierarchical organization** | Organize songs into nested folders (beyond flat labels). | GuitarTapp |

---

## Recommended Priority

**Quick wins (small effort, high value):**
1. ~~Auto-scroll on Android~~ (already implemented)
2. Duplicate song
3. Font size control (pinch-to-zoom or slider)
4. Song sections navigation

**Medium effort, high value:**
5. Setlists / playlists
6. PDF export
7. Performance / stage mode
8. Batch operations

**Larger initiatives:**
9. Metronome integration in songbook
10. ~~Cloud sync~~ (conflicts with offline-first architecture)
11. External device control (foot pedals)
12. Song statistics / practice tracking

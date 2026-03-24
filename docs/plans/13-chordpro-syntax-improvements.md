# Plan: ChordPro Practical Syntax Improvements

## Scope
- `shared/.../data/ChordParser.kt`
- `shared/.../data/ChordProParser.kt`
- `shared/.../data/ChordProExporter.kt`
- `shared/.../data/ChordSheet.kt`
- `shared/.../data/sync/BackupData.kt`
- `shared/.../domain/ChordSheetTranspose.kt`
- `shared/.../domain/ChordSheetFormatter.kt`
- `app/.../data/ChordSheetRepository.kt` (Android persistence)
- `app/.../data/sync/BackupRestoreManager.kt` (Android backup)
- `iosApp/.../ViewModels/SongbookViewModel.swift` (iOS StoredSong + persistence)

## Pre-Existing Bug: Section Labels Parsed as Chords

`ChordParser.CHORD_PATTERN` matches `[Chorus]` and `[Bridge]` as chords (C and B
are in A-G range). Fix with a negative lookahead excluding known section names.

## Fixes

1. **`#` comment lines** — skip lines starting with `#` in parser
2. **Section labels with custom names** — `{start_of_verse: Verse 2}` → `[Verse 2]`;
   add intro, outro, interlude section types
3. **Tab blocks** — handle `{start_of_tab}`/`{end_of_tab}` with `[Tab]` label,
   preserve tab content as monospaced text
4. **`{chorus}` recall** — insert `[Chorus]` label
5. **`{subtitle}` separation** — new `subtitle` field on `ChordSheet`, update
   persistence on both platforms and backup format
6. **`{tempo}` and `{time}`** — insert as formatted content comment
7. **`{define}` directive** — explicitly skip in `when` block
8. **Exporter `{end_of_...}`** — emit closing directives
9. **Exporter regex** — match new section types and custom names

## Priority
Medium — correctness ensures interoperability with other music apps.

## Estimated Effort
~4-6 hours total across all fixes and tests.

# Test Plan: ChordPro Parser & Exporter

## Scope
- `shared/.../data/ChordProParser.kt`
- `shared/.../data/ChordProExporter.kt`

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/ChordProParserTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/ChordProExporterTest.kt`

## ChordProParser Test Cases

### `parse(input, defaultTitle)`
- Basic song with {title} and {artist} directives
- Chord lines with `[Am]lyrics [G]here` notation
- Section directives: {start_of_chorus}, {end_of_chorus}
- Verse, bridge, chorus sections parsed correctly
- {key} directive sets key field
- {capo} directive sets capo field
- {comment} directive preserved
- Missing title uses defaultTitle parameter
- Empty input returns empty ChordSheet
- Multiple sections in sequence

### `isChordProFile(filename)`
- `.cho` → true
- `.chordpro` → true
- `.chopro` → true
- `.crd` → true
- `.pro` → true
- `.txt` → false
- `.pdf` → false
- Case sensitivity check

### Round-Trip
- Parse then export produces semantically equivalent output
- Export then parse recovers original data

## ChordProExporter Test Cases

### `export(sheet)`
- Sheet with title → includes {title: ...}
- Sheet with artist → includes {artist: ...}
- Sheet with key → includes {key: ...}
- Sheet with capo → includes {capo: ...}
- Chord markers in content converted to `[Chord]` inline notation
- Section labels converted to {start_of_section} directives
- Empty sheet → minimal valid ChordPro output

### `suggestedFilename(sheet)`
- Title "My Song" → "My_Song.cho" or similar sanitized name
- Title with special characters → sanitized
- Empty title → fallback filename

## Priority
Medium — ChordPro is a standard format; correctness ensures interoperability with other music apps.

## Estimated Test Count
~25 test cases

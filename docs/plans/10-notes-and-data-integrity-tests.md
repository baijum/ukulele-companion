# Test Plan: Notes & Data Integrity

## Scope
- `shared/.../data/Notes.kt`
- `shared/.../data/Progressions.kt` (validation)
- `shared/.../data/ChordFormulas.kt` (validation)
- `shared/.../data/TheoryLessons.kt` (validation)
- `shared/.../data/StrumPatterns.kt` (validation)
- `shared/.../data/FingerpickingPatterns.kt` (validation)
- `shared/.../data/Glossary.kt` (validation)
- `shared/.../data/sync/BackupData.kt` (serialization)

## Current Coverage
Notes is used indirectly by other tests but has no dedicated tests. Static data files have no validation tests.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/NotesTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/DataIntegrityTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/BackupDataTest.kt`

## Notes Test Cases

### `pitchClassToName(pitchClass)`
- 0 → "C", 1 → "C#" or "Db", ..., 11 → "B"
- All 12 pitch classes return non-empty names
- Values outside 0-11 handled (modulo or error)

### `enharmonicForKey(pitchClass, keyRoot, isMinor)`
- Pitch class 1 in key of D (2 sharps) → "C#"
- Pitch class 1 in key of Db (5 flats) → "Db"
- Null keyRoot → falls back to standard names
- Minor keys use correct accidentals
- All 12 pitch classes × flat keys → use flats
- All 12 pitch classes × sharp keys → use sharps

### Array Constants
- NOTE_NAMES_SHARP has 12 entries
- NOTE_NAMES_FLAT has 12 entries
- NOTE_NAMES_STANDARD has 12 entries
- PITCH_CLASS_COUNT == 12

## Data Integrity Test Cases (single test file, multiple sections)

### Progressions
- All progressions have non-empty name and description
- All degrees have valid intervals (0-11)
- `forScale()` returns non-empty for each ScaleType
- `diatonicDegrees()` returns 7 degrees for each scale type
- No duplicate progression names within same scale type

### ChordFormulas
- ALL list is non-empty (19 formulas)
- Each formula has non-empty symbol
- Each formula has non-empty intervals set
- Root (0) is in every formula's intervals
- BY_CATEGORY covers all ChordCategory values
- No duplicate symbols

### TheoryLessons
- ALL has 37 lessons
- Each lesson has non-empty title, content, keyPoints
- Quiz: quizCorrectIndex is within quizOptions bounds
- All MODULES strings are non-empty
- `byModule()` groups cover all lessons
- No duplicate lesson IDs

### StrumPatterns
- ALL is non-empty (17 patterns)
- Each pattern has non-empty name and beats
- BPM ranges are valid (min < max, both > 0)
- Time signatures are standard (3/4, 4/4, 6/8)

### FingerpickingPatterns
- ALL is non-empty (11 patterns)
- Each step references valid string index (0-3)
- Each step uses valid Finger enum

### Glossary
- ALL is non-empty (80+ entries)
- Each entry has non-empty term and definition
- Entries are alphabetically sorted

## BackupData Test Cases

### Serialization Round-Trip
- Create BackupData with sample data → serialize to JSON → deserialize → equals original
- Empty BackupData → serializes/deserializes correctly
- Version field is 3

## Priority
Low-Medium — Data integrity tests are quick to write and catch regressions when static data is edited. Notes tests verify a core utility used everywhere.

## Estimated Test Count
~40-50 test cases

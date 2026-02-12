# Feature: New Reference Tools

**Status: PROPOSED**

## Summary

New reference tools to expand the Reference section beyond its current three features (Circle of Fifths, Capo Guide, Chord Substitutions). These tools fill gaps identified through comparison with leading apps (Fender Tune, GuitarToolkit, Chord.Rocks, Tonality) and address common learning needs that the app's existing data can power with minimal new infrastructure.

## Motivation

- Leading apps (Fender Tune, GuitarToolkit) include scale finders, chord-scale relationship views, and notation references as standard reference tools
- The app has rich data (`Scales.kt`, `ChordFormulas.kt`, `Notes.kt`, `Progressions.kt`) that could power several reference views with little new logic
- A music glossary would help beginners understand terms used throughout the app (intervals, inversions, diatonic, enharmonic, etc.)
- Scale-chord relationships are a commonly requested feature that bridges the Scale Overlay and Progressions tab — showing which chords naturally fit a scale
- Fretboard note mapping is a fundamental reference that every string instrument app should offer

---

## Feature A: Scale-Chord Relationship Viewer

### Problem

The Scale Overlay and Progressions tab are disconnected. A user viewing the C major scale can't see which chords are built from that scale. Conversely, a user viewing a C major progression can't see the underlying scale. This relationship — "these chords belong to this scale because they're built from its notes" — is the central insight of harmony, and the app doesn't surface it.

This was identified as a Tier 2 gap in `docs/spec/15-music-theory-learning.md` and also proposed in `docs/spec/14-composition-tools.md` (Idea 5: Scale-Aware Chord Suggestions).

### Proposed Solution

A "Chords in Scale" reference view that, given a root note and scale type:

1. Shows all notes in the scale
2. Builds a triad (and optionally a 7th chord) on each scale degree
3. Displays each chord with its Roman numeral, quality, notes, and a mini chord diagram
4. Highlights the chord's harmonic function (Tonic/Subdominant/Dominant)
5. Allows tapping any chord to navigate to the Chord Library

#### UI Layout

```
┌─────────────────────────────────┐
│  Chords in Scale                │
│                                 │
│  Root: [ C ] [ C# ] [ D ] ...  │
│  Scale: [ Major ] [ Minor ] ...│
│                                 │
│  Scale notes: C D E F G A B    │
│                                 │
│  ┌──────────────────────────┐   │
│  │ I — C Major    [T]      │   │  ← Tonic
│  │ Notes: C E G            │   │
│  │ [chord diagram]  [▶]    │   │
│  └──────────────────────────┘   │
│  ┌──────────────────────────┐   │
│  │ ii — D Minor   [S]      │   │  ← Subdominant
│  │ Notes: D F A            │   │
│  │ [chord diagram]  [▶]    │   │
│  └──────────────────────────┘   │
│  ... (all 7 degrees) ...        │
│                                 │
│  Toggle: [ Triads ] [ 7ths ]   │
└─────────────────────────────────┘
```

### Technical Approach

The logic to build chords from scale degrees already exists implicitly in `KeySignatures.diatonicChordsForMajor()`. This feature generalizes it to any scale type:

```kotlin
data class ScaleChord(
    val degree: Int,              // 1-based scale degree
    val numeral: String,          // "I", "ii", "iii", etc.
    val rootPitchClass: Int,
    val rootName: String,
    val quality: String,          // "Major", "Minor", "Diminished"
    val notes: List<String>,      // ["C", "E", "G"]
    val function: HarmonicFunction?, // T, S, D (for major/minor scales)
)

object ScaleChordBuilder {
    /**
     * Builds diatonic chords (triads or 7ths) from a scale.
     *
     * For each scale degree, stacks 3rds using only notes from the scale.
     */
    fun buildChords(
        rootPitchClass: Int,
        scale: Scale,
        includeSevenths: Boolean = false,
    ): List<ScaleChord> {
        val scaleNotes = scale.intervals.map { (rootPitchClass + it) % 12 }
        return scaleNotes.mapIndexed { index, notePitchClass ->
            val third = scaleNotes[(index + 2) % scaleNotes.size]
            val fifth = scaleNotes[(index + 4) % scaleNotes.size]
            // Determine quality from intervals between root, 3rd, 5th
            val thirdInterval = (third - notePitchClass + 12) % 12
            val fifthInterval = (fifth - notePitchClass + 12) % 12
            val quality = determineTriadQuality(thirdInterval, fifthInterval)
            // ...
        }
    }

    private fun determineTriadQuality(third: Int, fifth: Int): String = when {
        third == 4 && fifth == 7 -> "Major"
        third == 3 && fifth == 7 -> "Minor"
        third == 3 && fifth == 6 -> "Diminished"
        third == 4 && fifth == 8 -> "Augmented"
        else -> "Unknown"
    }
}
```

### Files

- **New:** `domain/ScaleChordBuilder.kt` — Logic to build chords from any scale
- **New:** `ui/ScaleChordView.kt` — Scale-chord relationship viewer UI
- **Modify:** `ui/FretboardScreen.kt` — Add "Chords in Scale" to Reference section in drawer

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 3 days
- **APK size impact**: Negligible

---

## Feature B: Music Glossary

### Problem

The app uses many music theory terms (interval, inversion, diatonic, enharmonic, voicing, tritone, subdominant, etc.) without defining them in-context. A beginner encountering "1st inversion" or "tritone substitution" has no quick reference. The Theory Lessons cover some terms but are structured as full lessons, not quick-lookup definitions.

### Proposed Solution

A searchable "Music Glossary" reference with concise definitions for all music terms used in the app:

#### Entry Structure

```kotlin
data class GlossaryEntry(
    val term: String,
    val definition: String,
    val example: String?,           // Optional concrete example
    val relatedTerms: List<String>, // Cross-references to other entries
    val seeAlso: String?,           // Link to relevant app feature
)
```

#### Content Coverage

| Category | Example Terms |
|----------|---------------|
| **Notes & Pitch** | Chromatic, Enharmonic, Semitone, Whole step, Octave, Pitch class, Sharp, Flat, Natural |
| **Intervals** | Interval, Minor 2nd through Major 7th, Perfect, Tritone, Consonance, Dissonance, Compound interval |
| **Scales** | Scale, Mode, Major, Minor, Pentatonic, Blues, Dorian, Mixolydian, Phrygian, Lydian, Locrian, Degree, Tonic (note) |
| **Chords** | Chord, Triad, Seventh chord, Suspended, Augmented, Diminished, Power chord, Inversion, Voicing, Root position, Slash chord, Formula |
| **Harmony** | Key, Key signature, Diatonic, Roman numeral, Tonic (function), Subdominant, Dominant, Resolution, Cadence, Modulation |
| **Progressions** | Progression, Voice leading, Chord substitution, Secondary dominant, Borrowed chord, Modal interchange, Tritone substitution |
| **Rhythm** | Beat, Tempo, BPM, Time signature, Measure/Bar, Whole/Half/Quarter/Eighth note, Syncopation, Downbeat, Upbeat, Strum |
| **Ukulele-Specific** | Fret, String, Nut, Capo, Barre chord, Open chord, Fingering, Tablature, Standard tuning, Low-G tuning, Re-entrant tuning |

Estimated 80–100 entries total.

#### UI Layout

```
┌─────────────────────────────┐
│  Music Glossary             │
│                             │
│  🔍 [ Search terms... ]    │
│                             │
│  ── C ──                    │
│  Cadence                    │
│    A chord progression      │
│    that ends a phrase...    │
│    Example: V → I (perfect  │
│    cadence)                 │
│    See also: Resolution,    │
│    Dominant                 │
│    Related: Circle of 5ths  │
│                             │
│  Capo                       │
│    A clamp placed across    │
│    all strings to raise...  │
│    See also: Transposition  │
│    Related: Capo Guide →    │
│                             │
│  ... (alphabetical list)    │
└─────────────────────────────┘
```

### Technical Approach

- Entries defined as static data in `data/Glossary.kt`
- UI uses a `LazyColumn` with sticky headers for alphabetical sections
- Search filter uses simple string matching on term and definition
- "Related" links navigate to the relevant app feature (Chord Library, Circle of Fifths, etc.)
- "See also" links scroll to another glossary entry

```kotlin
@Composable
fun GlossaryView(
    onNavigateToFeature: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val entries = remember(searchQuery) {
        Glossary.ALL
            .filter { it.term.contains(searchQuery, ignoreCase = true) ||
                      it.definition.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.term.first().uppercaseChar() }
    }

    LazyColumn {
        entries.forEach { (letter, group) ->
            stickyHeader { Text(letter.toString()) }
            items(group) { entry -> GlossaryEntryCard(entry) }
        }
    }
}
```

### Files

- **New:** `data/Glossary.kt` — All glossary entries (80–100 definitions)
- **New:** `ui/GlossaryView.kt` — Searchable glossary UI
- **Modify:** `ui/FretboardScreen.kt` — Add "Music Glossary" to Reference section in drawer

### Effort Estimate

- **Complexity**: Low–Medium (mostly content authoring)
- **Estimated time**: 3 days (2 days content, 1 day UI)
- **APK size impact**: Negligible (text only)

---

## Feature C: Fretboard Note Map

### Problem

Knowing which note is at every fret on every string is fundamental to playing ukulele. The app has an interactive fretboard but it only shows notes when chord fingers are placed or a scale overlay is active. There's no dedicated "show me all the notes" reference view.

Leading apps (Fender Tune with 2,000+ scales, Chord.Rocks, GuitarToolkit) all include fretboard note displays as a core reference tool.

### Proposed Solution

A "Fretboard Notes" reference view showing all note names on the fretboard:

#### Modes

1. **All Notes** — Every note labeled at every fret (12 or 15 frets)
2. **Natural Notes Only** — Show only C, D, E, F, G, A, B (hide sharps/flats for clarity)
3. **Single Note Finder** — Select a note name, see all positions highlighted
4. **Octave View** — Select a note, see how the same pitch appears across strings and octaves

#### UI Layout

```
┌────────────────────────────────┐
│  Fretboard Notes               │
│                                │
│  [All] [Naturals] [Find] [Oct]│
│                                │
│  Tuning: Standard (GCEA)      │
│  Frets: [ 12 ] [ 15 ]         │
│                                │
│     0   1   2   3   4   5 ... │
│  G: G  G#  A  A#  B   C  ... │
│  C: C  C#  D  D#  E   F  ... │
│  E: E  F  F#  G  G#   A  ... │
│  A: A  A#  B   C  C#   D ... │
│                                │
│  ← Interactive fretboard →     │
│  (tap any position to hear)   │
└────────────────────────────────┘
```

### Technical Approach

```kotlin
@Composable
fun FretboardNoteMapView(
    isLowG: Boolean,
    isLeftHanded: Boolean,
    useFlats: Boolean,
) {
    val openStrings = if (isLowG) {
        listOf(7, 0, 4, 9)  // G3, C4, E4, A4
    } else {
        listOf(7, 0, 4, 9)  // G4, C4, E4, A4
    }

    // Generate note grid: 4 strings × 13+ frets
    val noteGrid = openStrings.map { openPitch ->
        (0..maxFret).map { fret ->
            val pitchClass = (openPitch + fret) % 12
            Notes.pitchClassToName(pitchClass, useFlats)
        }
    }
    // Render as interactive fretboard with labels
}
```

- Reuse the existing `FretboardView` composable with a "show all labels" mode, or create a simplified read-only fretboard that always shows note names
- Respect Left-Handed mode (mirror the fretboard)
- Respect Flat Note Names preference
- Tap any fret to hear the note (using `ToneGenerator`)
- In "Find" mode, highlight all positions of the selected note in a distinct color

### Files

- **New:** `ui/FretboardNoteMapView.kt` — Fretboard note reference UI
- **Modify:** `ui/FretboardScreen.kt` — Add "Fretboard Notes" to Reference section in drawer
- **Possibly reuse:** `ui/FretboardView.kt` — If the existing fretboard composable can accept a "show all labels" parameter

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 2–3 days
- **APK size impact**: Negligible

---

## Feature D: Common Chord Shapes Quick Reference

### Problem

The Chord Library is comprehensive (20 formulas × 12 roots × multiple voicings) but can be overwhelming for beginners. There's no "quick reference card" showing just the essential chords every ukulele player should know — the 15–20 most common open chords with their standard fingerings.

### Proposed Solution

A "Common Chords" quick reference card showing the most-used ukulele chords in a grid:

#### Content

| Row | Chords |
|-----|--------|
| Major | C, D, E, F, G, A, Bb |
| Minor | Am, Dm, Em, Gm, Bm |
| Seventh | A7, C7, D7, E7, G7 |
| Other | Cmaj7, Am7, Dm7 |

~20 chords total — the "essentials" that cover 90% of songs.

#### UI Layout

```
┌──────────────────────────────┐
│  Common Chords               │
│                              │
│  ── Major ──                 │
│  [C][D][E][F][G][A][Bb]     │  ← Tap to expand
│                              │
│  ── Minor ──                 │
│  [Am][Dm][Em][Gm][Bm]       │
│                              │
│  ── Seventh ──               │
│  [A7][C7][D7][E7][G7]       │
│                              │
│  Tap any chord for diagram   │
│  and audio playback          │
└──────────────────────────────┘
```

Tapping a chord shows:
- Chord diagram with finger positions
- Fret numbers (e.g., "2010")
- Play button to hear the chord
- "More voicings" link to Chord Library

### Technical Approach

```kotlin
data class CommonChord(
    val name: String,
    val rootPitchClass: Int,
    val quality: String,
    val fingering: String,    // Standard fingering, e.g., "0003"
    val category: String,     // "Major", "Minor", "Seventh"
)

object CommonChords {
    val ALL = listOf(
        CommonChord("C", 0, "Major", "0003", "Major"),
        CommonChord("D", 2, "Major", "2220", "Major"),
        CommonChord("Am", 9, "Minor", "2000", "Minor"),
        // ...
    )
}
```

- Chord diagrams rendered using existing chord rendering components
- Audio via existing `ToneGenerator`
- "More voicings" navigates to Chord Library with root and quality pre-selected

### Files

- **New:** `data/CommonChords.kt` — Curated list of ~20 essential chords
- **New:** `ui/CommonChordsView.kt` — Quick reference grid UI
- **Modify:** `ui/FretboardScreen.kt` — Add "Common Chords" to Reference section in drawer

### Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 2 days
- **APK size impact**: Negligible

---

## Feature E: Ukulele Tuning Reference

### Problem

The app supports standard (GCEA) and Low-G tuning but doesn't explain the differences, common alternate tunings, or how tuning affects chord voicings. Beginners often don't understand what "re-entrant tuning" means or why Low-G changes the instrument's character.

### Proposed Solution

A "Tuning Reference" guide covering:

#### Sections

1. **Standard Tuning (G-C-E-A)** — Re-entrant tuning explained, why the G string is high, how it creates the ukulele's characteristic sound
2. **Low-G Tuning** — What it is, how it extends range, which genres benefit, what changes in chord voicings
3. **Other Common Tunings** — D-tuning (A-D-F#-B), Baritone (D-G-B-E), Slack-key (G-C-E-G)
4. **Re-entrant vs Linear** — The difference and how it affects fingerpicking and chord voicings
5. **Tuning Tips** — How to tune, tuning stability, string types

#### Interactive Elements

- Audio playback of open strings in each tuning
- Side-by-side comparison: hear standard vs Low-G for the same chord
- Link to the app's tuning toggle to switch between standard and Low-G

### Technical Approach

```kotlin
data class TuningInfo(
    val name: String,
    val strings: List<String>,      // ["G4", "C4", "E4", "A4"]
    val pitchClasses: List<Int>,    // [7, 0, 4, 9]
    val description: String,
    val characteristics: List<String>,
    val bestFor: List<String>,      // ["Traditional Hawaiian", "Strumming"]
)

object TuningReference {
    val TUNINGS = listOf(
        TuningInfo(
            name = "Standard (High-G)",
            strings = listOf("G4", "C4", "E4", "A4"),
            pitchClasses = listOf(7, 0, 4, 9),
            description = "The traditional re-entrant tuning...",
            characteristics = listOf("Bright, jangly sound", "Re-entrant (G higher than C)"),
            bestFor = listOf("Traditional Hawaiian", "Strumming", "Fingerpicking patterns"),
        ),
        // ... Low-G, D-tuning, Baritone, Slack-key
    )
}
```

### Files

- **New:** `data/TuningReference.kt` — Tuning data and educational content
- **New:** `ui/TuningReferenceView.kt` — Tuning guide UI
- **Modify:** `ui/FretboardScreen.kt` — Add "Tuning Guide" to Reference section in drawer

### Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 2 days
- **APK size impact**: Negligible

---

## Implementation Priority

| Priority | Feature | Effort | Impact | Rationale |
|----------|---------|--------|--------|-----------|
| 1 | A: Scale-Chord Relationship Viewer | 3 days | High | Fills the biggest conceptual gap — bridges scales and chords. Also proposed in docs 14 and 15. |
| 2 | C: Fretboard Note Map | 2–3 days | High | Fundamental reference tool, leverages existing fretboard UI. |
| 3 | B: Music Glossary | 3 days | Medium | Helps beginners throughout the app; content-heavy but simple UI. |
| 4 | D: Common Chord Shapes | 2 days | Medium | High beginner value; quick reference card for the most-used chords. |
| 5 | E: Ukulele Tuning Reference | 2 days | Low–Medium | Niche but useful; educational content about the instrument itself. |

Feature A (Scale-Chord Viewer) has the highest impact because it connects two existing features (Scale Overlay and Progressions) that are currently disconnected. Feature C (Fretboard Note Map) is a standard reference tool that every competitor offers.

---

## Architecture Notes

- All features follow the established pattern: data in `data/`, UI in `ui/`, logic in `domain/` (if needed)
- Scale-Chord Builder (Feature A) creates a new domain concept that could be reused by composition tools (see `docs/spec/14-composition-tools.md`)
- Fretboard Note Map (Feature C) should respect all user preferences: Left-Handed mode, Flat Note Names, Low-G tuning
- Glossary (Feature B) entries should cross-reference each other and link to relevant app features
- Common Chords (Feature D) fingerings should come from `VoicingGenerator` where possible, with curated overrides for the standard "textbook" fingerings
- All new Reference features should be accessible from the navigation drawer under the "Reference" section header

## Dependencies

- Feature A depends on `Scales.kt` and `ChordFormulas.kt` for data
- Feature C depends on `FretboardView` or equivalent composable, plus `ToneGenerator` for tap-to-play
- Feature D depends on chord diagram rendering components
- Features B and E are self-contained (text content only)

## Testing Strategy

- **Scale-Chord Viewer**: Verify correct chords are built for all scale types (major, minor, pentatonic, modes); verify Roman numerals and qualities match standard harmony
- **Fretboard Note Map**: Verify correct notes at all fret positions for both standard and Low-G tuning; verify left-handed mode mirrors correctly; verify flat names display when preference is set
- **Music Glossary**: Verify search filters correctly; verify cross-reference links scroll to correct entries; verify feature links navigate correctly
- **Common Chords**: Verify all fingerings produce correct chord types; verify audio playback; verify Chord Library navigation
- **Tuning Reference**: Verify audio plays correct pitches for each tuning

## Cross-References

- `docs/spec/14-composition-tools.md` — Idea 5 (Scale-Aware Chord Suggestions) overlaps with Feature A; implementing either covers the gap
- `docs/spec/15-music-theory-learning.md` — Tier 2 gap: Scale-Chord Relationship View aligns with Feature A
- `docs/spec/19-reference-section-enhancements.md` — Enhancements to existing Reference features; shares mini chord diagram component with Features A and D

# Feature: Composition Tools

**Status: PROPOSED**

## Summary

A collection of features aimed at helping ukulele players compose original music directly within the app. Builds on existing capabilities (chord progressions, scale overlay, chord sheets, audio playback) to provide a more complete songwriting workflow.

## Motivation

- Musicians using the app for learning eventually want to create their own music
- The app already has chord progressions, scales, transposition, and chord sheets — but they operate in silos
- Bridging these features and adding composition-specific tools turns the app from a reference tool into a creative companion
- Ukulele is popular among singer-songwriters who compose on the go

---

## Idea 1: Progression Playback with Tempo (Metronome)

### Problem

Custom and preset chord progressions can only be played as a quick sequential strum (via Voice Leading's "Play All"). There's no way to hear a progression at a steady tempo with configurable beats per chord — essential for evaluating feel and flow during composition.

### Proposed Solution

Add a playback toolbar to the Progressions tab that plays the selected progression at a configurable BPM:

- **BPM control**: Slider or stepper (60–200 BPM, default 100)
- **Beats per chord**: How many beats before advancing to the next chord (1, 2, 4, or 8)
- **Visual metronome**: Highlight the current chord in the progression with a pulsing indicator
- **Repeat toggle**: Loop the progression continuously until stopped
- **Tap tempo**: Tap a button rhythmically to set the BPM

### Technical Approach

```kotlin
// Metronome engine using coroutines
class MetronomeEngine {
    fun start(bpm: Int, beatsPerChord: Int, chordCount: Int, onBeat: (chordIndex: Int, beat: Int) -> Unit)
    fun stop()
}
```

- Use `kotlinx.coroutines.delay` for timing (sufficient accuracy for a practice metronome)
- On each chord change, call `FretboardViewModel.playVoicing()` with the best voicing from `VoicingGenerator`
- UI: A collapsible playback bar below the progression cards with play/stop, BPM, and beats-per-chord controls

### Files

- **New:** `audio/MetronomeEngine.kt` — Coroutine-based beat scheduler
- **New:** `ui/ProgressionPlaybackBar.kt` — Composable playback toolbar
- **Modify:** `ui/ProgressionsTab.kt` — Add playback bar and wire controls
- **Modify:** `viewmodel/FretboardViewModel.kt` — Expose method to play a voicing for a given root + quality

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 2–3 days
- **APK size impact**: Negligible

---

## Idea 2: Chord Sheet Transpose

### Problem

The Transpose tool works on individual chords in the Chord Library, but chord sheets (in the Songbook) have no transposition support. A composer who wrote a song in G but wants to try it in A must manually re-enter every chord marker.

### Proposed Solution

Add a transpose control to the chord sheet view/edit screen:

- **+/- buttons** to shift all inline chord markers up or down by N semitones
- **Preview mode**: Show the transposed chord names in a different color before committing
- **Apply**: Permanently rewrite the chord markers in the sheet content
- **Capo label**: Show the equivalent capo position (e.g., "Capo 2 from original key")

### Technical Approach

```kotlin
// Parse all [Chord] markers from sheet content, transpose, and replace
fun transposeChordSheet(content: String, semitones: Int, useFlats: Boolean): String {
    return content.replace(Regex("\\[([A-G][#b]?[^\\]]*)]")) { match ->
        val chord = match.groupValues[1]
        val transposed = transposeChordName(chord, semitones, useFlats)
        "[$transposed]"
    }
}
```

- Reuse existing `Transpose.transposePitchClass()` and `Notes.pitchClassToName()` logic
- Parse root note from chord marker, transpose it, preserve quality suffix

### Files

- **New:** `domain/ChordSheetTranspose.kt` — Regex-based chord marker transposition
- **Modify:** `ui/SongbookTab.kt` — Add transpose controls to the sheet detail/edit view
- **Modify:** `data/ChordSheetRepository.kt` — Optionally save transposed version

### Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 1–2 days
- **APK size impact**: Negligible

---

## Idea 3: Export & Share

### Problem

Chord sheets and custom progressions are locked inside the app. The only way to get data out is via Google Drive sync (backup/restore), which isn't suitable for sharing a single song with a bandmate or posting lyrics online.

### Proposed Solution

Add share/export functionality to chord sheets and progressions:

- **Chord Sheets**: Export as plain text with inline chord notation (the existing `[Chord]text` format), or a formatted version with chords above lyrics
- **Progressions**: Export as a one-line summary (e.g., "Pop / Four Chords in C: C – G – Am – F")
- **Share via Android share sheet**: Use `Intent.ACTION_SEND` so the user can send to any app (Messages, Email, Notes, etc.)
- **Copy to clipboard**: One-tap copy as an alternative

### Technical Approach

```kotlin
// Format chord sheet for sharing (chords above lyrics)
fun formatChordSheet(sheet: ChordSheet): String {
    // Convert "[C]Somewhere over the [Em]rainbow"
    // to:
    // C              Em
    // Somewhere over rainbow
}

// Share via Android Intent
fun shareText(context: Context, title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
```

### Files

- **New:** `domain/ChordSheetFormatter.kt` — Plain text and chords-above-lyrics formatters
- **Modify:** `ui/SongbookTab.kt` — Add share/copy buttons to chord sheet detail view
- **Modify:** `ui/ProgressionsTab.kt` — Add share button to progression cards

### Effort Estimate

- **Complexity**: Low
- **Estimated time**: 1 day
- **APK size impact**: Negligible

---

## Idea 4: Melody Notepad

### Problem

The app is entirely chord-focused. Composers also need to capture melody ideas — a sequence of individual notes with timing. Currently, tapping frets on the Explorer only selects one note per string for chord detection; there's no way to record a sequence of notes.

### Proposed Solution

A new "Melody" mode in the Explorer or a dedicated section where:

- **Tap notes on the fretboard** to add them to a sequence (shown as a scrollable timeline below)
- **Play back** the sequence at a configurable tempo
- **Edit**: Delete notes, rearrange order, adjust duration (quarter, eighth, half note)
- **Save**: Persist melodies locally (SharedPreferences, same pattern as favorites)
- **Combine with chords**: Optionally attach a chord progression so the melody plays over the chords

### Technical Approach

```kotlin
data class MelodyNote(
    val pitchClass: Int,      // 0–11
    val octave: Int,          // 3–5 (ukulele range)
    val duration: NoteDuration, // QUARTER, EIGHTH, HALF, WHOLE
    val stringIndex: Int,     // Which string it was played on
    val fret: Int,            // Which fret
)

enum class NoteDuration(val beats: Float) {
    WHOLE(4f), HALF(2f), QUARTER(1f), EIGHTH(0.5f)
}
```

- Timeline UI: Horizontal scrollable row of note blocks, colored by pitch
- Playback: Use existing `ToneGenerator` to play individual notes at intervals determined by BPM and note duration
- Storage: Serialize as pipe-delimited strings (same pattern as favorites/chord sheets)

### Files

- **New:** `data/MelodyNote.kt` — Data model
- **New:** `data/MelodyRepository.kt` — SharedPreferences persistence
- **New:** `ui/MelodyNotepad.kt` — Timeline UI + fretboard integration
- **New:** `viewmodel/MelodyViewModel.kt` — Sequence management and playback
- **Modify:** `ui/FretboardScreen.kt` — Add "Melody" to navigation drawer

### Effort Estimate

- **Complexity**: High
- **Estimated time**: 4–5 days
- **APK size impact**: Negligible

---

## Idea 5: Scale-Aware Chord Suggestions

### Problem

The scale overlay shows which notes belong to a scale, and the custom progression builder shows diatonic chords for Major/Minor. But these aren't connected — a composer working in D Dorian can't easily see which chords fit that scale. Also, the custom progression builder only supports Major and Minor, not the 5 other scale types the app supports (Pentatonic Major/Minor, Blues, Dorian, Mixolydian).

### Proposed Solution

A "Chords in Scale" panel that:

- **Shows all diatonic chords** for the currently selected scale and root
- **Works with all 7 scale types**: Derives triads by stacking thirds from each scale degree
- **One-tap navigation**: Tap a chord to see its voicings in the Chord Library
- **Integration**: Accessible from the Scale Overlay section on the Explorer tab, or as a standalone view

### Technical Approach

```kotlin
// Given a scale's intervals, derive triads at each degree
fun diatonicTriads(scaleIntervals: List<Int>, root: Int): List<DiatonicChord> {
    return scaleIntervals.mapIndexed { degree, interval ->
        val chordRoot = (root + interval) % 12
        val third = scaleIntervals[(degree + 2) % scaleIntervals.size]
        val fifth = scaleIntervals[(degree + 4) % scaleIntervals.size]
        // Determine quality from interval sizes
        val quality = classifyTriad(third - interval, fifth - interval)
        DiatonicChord(chordRoot, quality, degree + 1)
    }
}
```

- For pentatonic scales (5 notes), only 5 triads are possible; some may be unusual
- For blues scale (6 notes), triads may include augmented or ambiguous qualities
- Display as a row of tappable chord chips with the scale degree number and quality

### Files

- **New:** `domain/ScaleChords.kt` — Scale-to-chord derivation logic
- **Modify:** `ui/ScaleSelector.kt` — Add "Chords in this scale" expandable section
- **Optionally modify:** `ui/ProgressionsTab.kt` — Use all scale types in custom progression builder

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 2–3 days
- **APK size impact**: Negligible

---

## Implementation Priority

Suggested order based on impact and complexity:

| Priority | Idea | Impact | Complexity |
|----------|------|--------|------------|
| 1 | Progression Playback with Tempo | High | Medium |
| 2 | Export & Share | High | Low |
| 3 | Chord Sheet Transpose | Medium | Low–Medium |
| 4 | Scale-Aware Chord Suggestions | Medium | Medium |
| 5 | Melody Notepad | High | High |

Ideas 1–3 provide the most immediate value for composers with the least implementation effort. Idea 4 enhances the theory/education side. Idea 5 (Melody Notepad) is the most ambitious and could be a standalone feature release.

## Dependencies

- Idea 1 (Tempo Playback) depends on: existing `ToneGenerator`, `VoicingGenerator`
- Idea 2 (Sheet Transpose) depends on: existing `Transpose` utilities, `ChordSheetRepository`
- Idea 3 (Export & Share) depends on: existing `ChordSheet` data model
- Idea 4 (Melody Notepad) depends on: existing `ToneGenerator`, new data model
- Idea 5 (Scale Chord Suggestions) depends on: existing `Scales.kt`, `ChordFormulas`

# Feature: Composition Tools

**Status: DONE** *(all five ideas implemented on both platforms)*

## Summary

A collection of features aimed at helping ukulele players compose original music directly within
the app. Builds on existing capabilities (chord progressions, scale overlay, chord sheets, audio
playback) to provide a more complete songwriting workflow.

## Motivation

- Musicians using the app for learning eventually want to create their own music
- The app already has chord progressions, scales, transposition, and chord sheets — but they operate in silos
- Bridging these features and adding composition-specific tools turns the app from a reference tool into a creative companion
- Ukulele is popular among singer-songwriters who compose on the go

## Implementation Status Key

- **Done** — fully implemented on that platform
- **Partially Built** — core logic or one platform exists, gaps remain
- **Needs Polish** — core logic exists, UX or wiring incomplete
- **Missing** — genuinely not yet implemented

---

## Idea 1: Chord Sheet Transpose

**Status: Done** *(both platforms have preview transpose and "Save in this key" commit flow)*

Both platforms call `ChordSheetTranspose.transpose()` for real-time preview.
`ChordProParser` already parses `{key}` and `{capo}` directives.

- **Android:** `SheetViewer` in `SongbookTab.kt` has `transposeSemitones` state, +/- buttons,
  semitone label, reset, capo equivalent, and share-transposed options. Missing: "Save in this
  key" button and `SongbookViewModel` method to persist the transposed content.
- **iOS:** `SongViewerView` in `SongbookView.swift` has the same preview transpose UI.
  `SongbookViewModel.swift` has `transpose(song:semitones:)`. Missing: "Save in this key"
  commit flow.

### Revised UX Model

The original spec proposed a transpose control on the *edit* screen. This is the wrong
placement — transposing is a **viewing** action, not an editing one. A composer wants to
audition "what does this song sound like in A?" before committing.

**Better flow (non-destructive preview first):**

1. In `SheetViewer` (read mode), show a key indicator at the top: `Key: G  ▲ ▼`
2. Tapping ▲ / ▼ shifts all inline chord markers in real-time — no permanent change yet
3. A "Save in this key" button commits the transposed version to the repository
4. Show capo equivalent alongside: `Key: A  (= Capo 2 on G)`
5. Use existing `enharmonicForKey()` for context-aware flat/sharp spelling

### Technical Approach

```kotlin
// Existing shared-domain utility — no new file needed
val transposedContent = ChordSheetTranspose.transpose(content, semitones)
```

```kotlin
// SongbookViewModel additions
private val _transposeOffset = MutableStateFlow(0)
val transposeOffset: StateFlow<Int> = _transposeOffset.asStateFlow()

fun shiftTranspose(delta: Int) { _transposeOffset.value += delta }
fun applyTranspose() { /* rewrite sheet content via ChordSheetTranspose, save */ }
fun resetTranspose() { _transposeOffset.value = 0 }
```

### Files — Android

- **Existing:** `shared/src/commonMain/.../domain/ChordSheetTranspose.kt` — regex-based chord marker transposition (reuse as-is)
- **Modify:** `viewmodel/SongbookViewModel.kt` — add `transposeOffset` state, `shiftTranspose()`, `applyTranspose()`
- **Modify:** `ui/SongbookTab.kt` — add "Save in this key" button to `SheetViewer` (preview transpose already exists)

### Files — iOS

- **Existing:** `iosApp/.../ViewModels/SongbookViewModel.swift` — has `transpose(song:semitones:)` (reuse as-is)
- **Modify:** `iosApp/.../Views/SongbookView.swift` (`SongViewerView`) — add "Save in this key" button

### Effort Estimate

- **Complexity**: Low ("Save in this key" button + ViewModel persist method on both platforms)
- **Estimated time**: 0.5 days Android + 0.5 days iOS

---

## Idea 2: Export & Share

**Status: Done** *(format-picker sheet and clipboard copy implemented on both platforms)*

### What Already Exists

**Shared domain:**
- `ChordSheetFormatter.formatChordsAboveLyrics(sheet)` — chords-above-lyrics plain-text formatter
- `ChordSheetFormatter.formatProgression(progression, keyRoot)` — one-line progression summary
- `ChordProExporter.export(sheet)` — exports to ChordPro `.cho` format

**Android:**
- `ShareUtils.shareText(context, title, text)` — share sheet via `Intent.ACTION_SEND`
- `ChordImageSharer` — shares chord diagrams as PNGs
- No format-picker bottom sheet; no clipboard copy; no progression share button

**iOS:**
- `SongViewerView` shares plain text and ChordPro via `UIActivityViewController` (inline `Menu`)
- `ProgressionsView` shares progressions via `ShareLink` using `ChordSheetFormatter`
- No format-picker sheet; no clipboard copy

### Remaining Gaps (both platforms)

1. **No format-picker bottom sheet / action sheet.** Users must navigate a menu to find the
   right format. A dedicated picker makes the options clearer.

2. **No copy-to-clipboard.** Neither platform offers a one-tap clipboard copy for chord sheets
   or progressions.

### Revised UX Model

A "Share" tap on a chord sheet opens a format-picker bottom sheet:

```text
Share chord sheet as:
  [ ChordPro (.cho) ]        ← for apps that parse it
  [ Plain text ]              ← chords-above-lyrics, readable by anyone
  [ Copy to clipboard ]       ← one tap, fastest for messaging
```

### Technical Approach

```kotlin
// Existing shared-domain file — already implemented
object ChordSheetFormatter {
    // Convert "[C]Somewhere over the [Em]rainbow"
    // to:
    // C              Em
    // Somewhere over the rainbow
    fun toChordAboveLyrics(content: String): String { ... }

    // One-line summary for progressions
    fun progressionSummary(progression: Progression, key: String): String {
        // "Pop / C: C – G – Am – F"
    }
}
```

### Files — Android

- **Existing:** `shared/src/commonMain/.../domain/ChordSheetFormatter.kt` — chords-above-lyrics and progression summary formatters
- **Modify:** `ui/SongbookTab.kt` — replace direct share call with format-picker bottom sheet, add clipboard copy
- **Modify:** `ui/ProgressionsTab.kt` — add share icon to progression cards

### Files — iOS

- **Modify:** `iosApp/.../Views/SongbookView.swift` (`SongViewerView`) — replace `Menu`-based share with format-picker sheet, add clipboard copy
- **Modify:** `iosApp/.../Views/ProgressionsView.swift` — add clipboard copy option to progression share

### Effort Estimate

- **Complexity**: Low
- **Estimated time**: 1 day Android + 0.5 days iOS

---

## Idea 3: Scale-Aware Chord Suggestions

**Status: Done** *(diatonic chord chips in progression builder on both platforms)*

### What Already Exists

**Shared domain:**
- `ScaleChords.diatonicTriads(scaleIntervals, root)` — derives triads for any scale

**Android:**
- `ScaleSelector` composable has `onChordTapped: (DiatonicChord) -> Unit` callback and renders
  a diatonic chords row, but this lives on the Explorer tab — not in the progression builder.
- The Explorer tab and Progressions tab are separate screens. A composer working in D Dorian
  must memorize the diatonic chords, navigate to Progressions, and re-enter them manually.

**iOS:**
- `CreateProgressionSheet` in `ProgressionsView.swift` already has diatonic chord chips
  ("Tap chords to add") wired through `ProgressionsViewModel.diatonicDegreesForScale()`.
  Tapping a chip appends that chord degree to the progression. This matches the proposed UX.

### Remaining Gap (Android only)

Inside `CreateProgressionSheet` (the modal for building a custom progression), add a
**"From Scale" helper panel** matching the iOS implementation:

- User selects a root and scale type (chips already exist in `ProgressionsTab`)
- App shows the diatonic chords for that scale as tappable chips: `I (C) · ii (Dm) · iii (Em)…`
- Tapping a chip appends that chord degree to the progression being built
- No new navigation required — everything stays in the same sheet

### Technical Approach

```kotlin
// In CreateProgressionSheet composable:
val diatonicChords = remember(selectedRoot, selectedScale) {
    ScaleChords.diatonicTriads(selectedScale.intervals, selectedRoot)
}

// Render as tappable filter chips
LazyRow {
    items(diatonicChords) { chord ->
        FilterChip(
            label = { Text("${chord.numeral} (${chord.name})") },
            onClick = { onAddDegree(chord.toChordDegree()) }
        )
    }
}
```

### Files — Android

- **Modify:** `ui/ProgressionsTab.kt` — add "From Scale" panel in `CreateProgressionSheet`
- No new files needed; `ScaleChords.diatonicTriads()` already handles the derivation

### Files — iOS

- **Existing:** `iosApp/.../Views/ProgressionsView.swift` (`CreateProgressionSheet`) — diatonic chord chips already implemented; no changes needed

### Effort Estimate

- **Complexity**: Low (Android only)
- **Estimated time**: 1 day Android; iOS done

---

## Idea 4: Melody Notepad

**Status: Done** *(step sequencer mode with 8/16-step grid implemented on both platforms)*

### What Already Exists

**Shared domain:**
- `MelodyNote` data model: `pitchClass`, `octave`, `duration`, `stringIndex`, `fret`

**Android:**
- `MelodyRepository` — SharedPreferences persistence (referenced in `BackupRestoreManager`)
- Navigation drawer entry "Melody Notepad" under Create section
- `ToneGenerator.playNote()` for individual note playback

**iOS:**
- `MelodyViewModel.swift` — tap + record input modes, BPM playback, persistence via UserDefaults
- `MelodyNotepadView.swift` — pitch grid, octave stepper, duration row, chip timeline, save/load
- Uses its own `MelodyNoteData` model (no `stringIndex`/`fret`; has `isRest` flag instead)

### Revised UX Model

The original spec proposed tapping frets on the fretboard to build a melody timeline. This
creates two problems:

1. **Dual cognitive load:** The user must think about fret positions *and* melody at the same
   time. Fretboard positions are physical; melody is musical. Conflating them adds friction.

2. **Mobile timeline editing is hard:** Horizontal scrolling, reordering, duration selectors
   per note — all create fat-finger issues on small screens.

**Simplified approach — Step Sequencer:**

- Show a fixed grid of 8 or 16 steps (expandable)
- Each step: large note name button (C, D, E…) + duration preset (♩ ♪ 𝅗𝅥 𝅝)
- Tap note to cycle through pitch; long-press to clear
- Playback at configurable BPM, loops continuously
- **Killer feature:** attach an existing chord progression to play underneath the melody

**Positioning:** Market this as a *melody capture* tool — quick idea → playback → save.
Not a full notation editor. That framing sets correct expectations and keeps scope small.

### Technical Approach

```kotlin
data class MelodyNote(
    val pitchClass: Int?,        // 0–11, null for a rest
    val octave: Int = 4,         // 4 = middle C octave
    val duration: NoteDuration = NoteDuration.QUARTER,
    val stringIndex: Int? = null,  // which ukulele string (0–3), null if unassigned
    val fret: Int? = null,
)

enum class NoteDuration(val label: String, val beats: Float) {
    WHOLE("Whole", 4f), HALF("Half", 2f), QUARTER("Quarter", 1f),
    EIGHTH("Eighth", 0.5f), SIXTEENTH("Sixteenth", 0.25f)
}
```

```kotlin
// MelodyViewModel
class MelodyViewModel : ViewModel() {
    val steps: StateFlow<List<MelodyNote?>>  // null = empty step
    val bpm: StateFlow<Int>
    val linkedProgressionId: StateFlow<String?>

    fun setStep(index: Int, note: MelodyNote?)
    fun play()
    fun stop()
    fun linkProgression(id: String)
    fun save(name: String)
}
```

### Files — Android

- **Existing:** `shared/src/commonMain/.../data/MelodyNote.kt` — data model *(implemented)*
- **Existing:** `app/src/main/java/.../data/MelodyRepository.kt` — SharedPreferences persistence *(implemented)*
- **New/Rework:** `ui/MelodyNotepad.kt` — step sequencer grid UI
- **Modify:** `viewmodel/MelodyViewModel.kt` — add step sequencer mode (`setStep()`, `linkedProgressionId`) to existing ViewModel

### Files — iOS

- **Rework:** `iosApp/.../ViewModels/MelodyViewModel.swift` — add step sequencer mode and linked-progression support
- **Rework:** `iosApp/.../Views/MelodyNotepadView.swift` — replace tap/record UI with step sequencer grid

### Effort Estimate

- **Complexity**: Medium (simplified from original High with step sequencer approach)
- **Estimated time**: 3–4 days Android + 2–3 days iOS

---

## Idea 5: Songwriter Mode — A Guided Composition Flow

**Status: Done** *("Start a Song" guided flow implemented on both platforms)*

### Problem

With 30+ screens in the navigation drawer, the composition workflow is fragmented. A user who
wants to write a song from scratch must manually orchestrate across Scales (Explorer), Progressions,
Songbook, and Export — there is no guided path.

### Proposed Solution

A **"Start a Song"** entry in the Create section of the drawer that acts as a lightweight
coordinator, walking the user through existing screens in sequence:

```text
Step 1: Choose a key and scale
        → Pre-fills key/scale selectors in Progressions

Step 2: Build a chord progression
        → Opens CreateProgressionSheet with scale chords pre-loaded (Idea 3)

Step 3: Hear it at tempo
        → Auto-opens ProgressionPlaybackBar on the built progression

Step 4: Add lyrics
        → Opens a new ChordSheet pre-filled with the progression's chords as markers

Step 5: Transpose if needed
        → Inline key +/- on the sheet viewer (Idea 1)

Step 6: Share
        → Format picker bottom sheet (Idea 2)
```

No new screens are required. The "Songwriter Mode" is purely a **navigation coordinator** that
pre-wires existing screens together and maintains state across them.

### Files — Android

- **New:** `ui/SongwriterModeFlow.kt` — step state machine, navigation coordinator
- **Modify:** `ui/FretboardScreen.kt` — add "Start a Song" entry in drawer Create section

### Files — iOS

- **New:** `iosApp/.../Views/SongwriterModeFlow.swift` — step state machine, navigation coordinator
- **Modify:** `iosApp/.../Views/CreateView.swift` — add "Start a Song" entry to Create list

### Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 2 days Android + 2 days iOS

---

## Revised Implementation Priority

Ordered by UX impact relative to implementation effort:

| Priority | Idea | UX Impact | Effort | Android | iOS |
|----------|------|-----------|--------|---------|-----|
| 1 | Chord Sheet Transpose (Idea 1) | High — core composer need | 0.5d + 0.5d | Partially Built | Partially Built |
| 2 | Export & Share formatter (Idea 2) | High — bandmate sharing | 1d + 0.5d | Needs Polish | Needs Polish |
| 3 | Scale → Progression wiring (Idea 3) | High — eliminates mental context-switch | 1d | Needs Polish | Done |
| 4 | Songwriter Mode (Idea 5) | High — unified workflow | 2d + 2d | Proposed | Proposed |
| 5 | Melody Notepad (Idea 4) | High when done right | 3–4d + 2–3d | Partially Built | Partially Built |

Ideas 1–3 can ship together as a polish release. Idea 5 (Songwriter Mode) ties them together
into a coherent UX story. Idea 4 (Melody Notepad) is best treated as a standalone release.

## Dependencies

- Idea 1 (Sheet Transpose) depends on: existing `Transpose.kt`, `Notes.pitchClassToName()`, existing `ChordSheetTranspose.kt`
- Idea 2 (Export & Share) depends on: existing `ShareUtils.kt`, `ChordProExporter`, existing `ChordSheetFormatter.kt`
- Idea 3 (Scale → Progression) depends on: existing `ScaleChords.diatonicTriads()`, `ProgressionsTab`
- Idea 4 (Melody Notepad) depends on: existing `ToneGenerator`, existing `MelodyNote`/`MelodyRepository`
- Idea 5 (Songwriter Mode) depends on: Idea 1, Idea 2, Idea 3 being complete

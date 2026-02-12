# Feature: Reference Section Enhancements

**Status: PROPOSED**

## Summary

Enhancements to the three existing Reference section features — Circle of Fifths, Capo Guide, and Chord Substitutions — to add interactivity, cross-linking, and missing content. These features are well-built but largely static; this plan addresses gaps identified through codebase review and comparison with leading apps (Fender Tune, Tonality, Circle o Fifths, Chord.Rocks).

## Motivation

- The Circle of Fifths has strong Canvas-based visuals but lacks minor key diatonic chords, rotation, and closely-related-key highlighting
- The Capo Guide mentions Chord Library features ("Capo" and "Viz") in text but has no programmatic navigation links to them
- Chord Substitutions is entirely read-only — no audio examples, no interactive fretboard demos, no links to try substitutions in the Progressions tab
- Leading apps (Tonality, Circle o Fifths for Android) offer audio playback on the Circle of Fifths, key locking, and visual relationship indicators
- Cross-linking between Reference and Learn sections is absent — a user viewing the Circle of Fifths can't jump to the "Keys & Signatures" lesson or the Theory Quiz

---

## Feature A: Circle of Fifths Enhancements

### Current State

The Circle of Fifths (`CircleOfFifthsView.kt`, 494 lines) is a Canvas-based interactive diagram with:
- Outer ring (12 major keys) and inner ring (12 relative minor keys)
- Tap-to-select with detail panel showing key signature, relative minor/major, diatonic chords
- Diatonic chord chips that navigate to the Chord Library
- Enharmonic-aware note naming via `Notes.enharmonicForKey()`

### Gaps

1. **Minor key diatonic chords** — Selecting a minor key shows basic info but not the diatonic chords (the function `diatonicChordsForMajor()` exists but there is no `diatonicChordsForMinor()`)
2. **Closely related key highlighting** — The detail panel mentions "closely related keys" as text but doesn't highlight adjacent keys on the circle visually
3. **No audio** — Competing apps (Circle o Fifths, Tonality) play the scale or diatonic chords when a key is selected
4. **No rotation/transposition** — The PRD proposed drag-to-rotate but this was not implemented
5. **No connection to Learn section** — No link to the "Circle of Fifths" theory lesson or the "Keys" quiz category

### Proposed Enhancements

#### A1. Diatonic Chords for Minor Keys

Add `diatonicChordsForMinor()` to `KeySignatures.kt`:

```kotlin
fun diatonicChordsForMinor(pitchClass: Int): List<Pair<String, String>> {
    val degrees = listOf(
        Triple(0, "m", "i"),
        Triple(2, "dim", "ii°"),
        Triple(3, "", "III"),
        Triple(5, "m", "iv"),
        Triple(7, "m", "v"),     // natural minor; harmonic minor uses V (major)
        Triple(8, "", "VI"),
        Triple(10, "", "VII"),
    )
    return degrees.map { (interval, quality, numeral) ->
        val root = (pitchClass + interval) % Notes.PITCH_CLASS_COUNT
        numeral to (Notes.enharmonicForKey(root, (pitchClass + 3) % 12) + quality)
    }
}
```

Display these in the `MinorKeyDetail()` panel as tappable chord chips, matching the major key detail panel.

#### A2. Closely Related Key Highlighting

When a key is selected, visually highlight the two adjacent keys on the circle (one clockwise, one counter-clockwise) with a subtle accent color or thicker border. These keys share 6 of 7 notes and are the most natural modulation targets.

```kotlin
// In drawCircle(), after drawing the selected segment:
val selectedIndex = CIRCLE_ORDER.indexOf(selectedKey)
val relatedIndices = listOf(
    (selectedIndex - 1 + 12) % 12,
    (selectedIndex + 1) % 12,
)
relatedIndices.forEach { index ->
    drawSegmentHighlight(
        index = index,
        color = relatedKeyColor.copy(alpha = 0.3f),
    )
}
```

Add a label in the detail panel: "Closely related: [Key1] and [Key2] (share 6 of 7 notes)"

#### A3. Play Scale / Diatonic Chords Audio

Add a "Play Scale" button and a "Play Chords" button in the detail panel:

- **Play Scale**: Plays the 7 notes of the selected key's major (or minor) scale sequentially using `ToneGenerator`, ascending then descending
- **Play Chords**: Plays the diatonic triads (I, ii, iii, IV, V, vi, vii°) one at a time, each as a brief strummed chord

```kotlin
// In the detail panel:
Row {
    OutlinedButton(onClick = { playScale(selectedKey) }) {
        Icon(Icons.Filled.MusicNote, null)
        Text("Play Scale")
    }
    OutlinedButton(onClick = { playDiatonicChords(selectedKey) }) {
        Icon(Icons.Filled.QueueMusic, null)
        Text("Play Chords")
    }
}
```

#### A4. Cross-Feature Links

Add navigation links in the detail panel:
- "Learn about keys" → Navigate to Theory Lesson "What Is a Key?" or "The Circle of Fifths"
- "Quiz yourself" → Navigate to Theory Quiz with Keys category pre-selected
- "View progressions" → Navigate to Progressions tab set to the selected key

#### A5. Parallel/Relative Toggle

Add a toggle to switch the inner ring between:
- **Relative minor** (default) — shows the relative minor of each major key
- **Parallel minor** — shows the parallel minor (same root, different mode)

This helps illustrate the difference between relative and parallel key relationships — a common source of confusion for learners.

### Files

- **Modify:** `data/KeySignatures.kt` — Add `diatonicChordsForMinor()`
- **Modify:** `ui/CircleOfFifthsView.kt` — Add related-key highlighting, audio buttons, inner ring toggle, cross-links
- **Modify:** `ui/FretboardScreen.kt` — Wire up cross-navigation callbacks

### Effort Estimate

- A1 (Minor diatonic chords): 0.5 days
- A2 (Related key highlighting): 0.5 days
- A3 (Audio playback): 1 day
- A4 (Cross-feature links): 0.5 days
- A5 (Parallel/relative toggle): 0.5 days
- **Total: 3 days**

---

## Feature B: Capo Guide Enhancements

### Current State

The Capo Guide (`CapoGuideView.kt`, 480 lines) is a 5-section educational guide with:
- Clear text explanations about what a capo does and when to use it
- Interactive pitch demo (select capo fret 0–7, see how open string pitches change)
- Common capo positions reference table
- Practical scenario cards (problem/solution format)
- Text reference to Chord Library's "Capo" and "Viz" features

### Gaps

1. **No programmatic navigation** — The guide mentions Chord Library features in text ("Head to the Chord Library...") but has no tappable links
2. **No audio demo** — The interactive pitch demo shows note names but doesn't play the sounds
3. **No reverse lookup** — Users can't ask "I want to play in Eb — where should I put the capo?"
4. **No Low-G tuning support** — Pitch calculations use standard high-G tuning only
5. **Static scenarios** — The problem/solution cards are fixed; could be dynamic based on commonly used chords

### Proposed Enhancements

#### B1. Navigation Links to Chord Library

Replace the text-only reference with tappable buttons:

```kotlin
// In the "Try It Yourself" section:
FilledTonalButton(onClick = { onNavigateToChordLibrary() }) {
    Icon(Icons.Filled.Piano, null)
    Text("Open Chord Library")
}
OutlinedButton(onClick = { onNavigateToCapoVisualizer() }) {
    Icon(Icons.Filled.Visibility, null)
    Text("Open Capo Visualizer")
}
```

#### B2. Audio in Pitch Demo

When the user selects a capo fret in the interactive demo, add a "Play Open Strings" button that plays the four open string pitches (as transposed by the capo) sequentially:

```kotlin
Button(onClick = {
    val pitches = CapoReference.STANDARD_OPEN_PITCHES.map { (it + capoFret) % 12 }
    pitches.forEach { playNote(it) }
}) {
    Icon(Icons.Filled.VolumeUp, null)
    Text("Play Open Strings")
}
```

This gives users an auditory sense of how the capo changes the instrument's character.

#### B3. Reverse Capo Lookup ("I Want to Play in...")

Add a section where the user selects a target key, and the guide suggests the best capo position and chord shapes:

```kotlin
// UI: Key selector + results
var targetKey by remember { mutableStateOf<Int?>(null) }

// "I want to play in: [C] [C#] [D] [Eb] ... "
// Result: "Capo fret 3, play C shapes → sounds in Eb"
//         "Capo fret 1, play D shapes → sounds in Eb"
```

This reverses the common capo positions table — instead of "capo here, get this key," it's "I need this key, where's the capo?"

The logic selects capo positions that result in the simplest chord shapes (from `FRIENDLY_SHAPES`):

```kotlin
fun suggestCapoPositions(targetKeyPitchClass: Int): List<CapoSuggestion> {
    // For each friendly shape root, calculate: capoFret = (target - shapeRoot + 12) % 12
    // Filter for practical frets (1–7)
    // Sort by ease of shapes and fret position
}
```

#### B4. Low-G Tuning Awareness

Check whether Low-G tuning is active and adjust the pitch calculations accordingly:

```kotlin
val openPitches = if (isLowGTuning) {
    listOf(7, 0, 4, 9) // Same pitch classes, different octave for G string
} else {
    CapoReference.STANDARD_OPEN_PITCHES
}
```

The note names don't change (G is still G), but the audio demo should play the G string one octave lower.

#### B5. Dynamic Scenario Cards

Add 2–3 more scenario cards based on common use cases:

| Problem | Solution |
|---------|----------|
| Song uses F# and B chords (hard barre shapes) | Capo on fret 2, play easy E and A shapes |
| Want to play "Somewhere Over the Rainbow" in the original key (C) but prefer G shapes | Capo on fret 5, play G shapes |
| Playing with a singer — song is too low in C | Move capo up (try fret 2 or 3) to raise the pitch while keeping shapes |

### Files

- **Modify:** `ui/CapoGuideView.kt` — Add navigation buttons, audio in demo, reverse lookup section, Low-G awareness
- **Modify:** `data/CapoReference.kt` — Add reverse lookup logic, new scenarios
- **Modify:** `ui/FretboardScreen.kt` — Wire up navigation callbacks for Chord Library and Capo Visualizer

### Effort Estimate

- B1 (Navigation links): 0.5 days
- B2 (Audio demo): 0.5 days
- B3 (Reverse capo lookup): 1 day
- B4 (Low-G awareness): 0.5 days
- B5 (Dynamic scenarios): 0.5 days
- **Total: 3 days**

---

## Feature C: Chord Substitutions Enhancements

### Current State

Chord Substitutions (`ChordSubstitutionsView.kt`, 170 lines) is a text-based educational guide with:
- 5 categories: Diatonic, Relative Swaps, Tritone, Modal Interchange, Secondary Dominants
- Each category has explanation text and specific examples (original → substitute)
- Examples are all in the key of C
- A practice tip card at the bottom
- No interactivity beyond scrolling

### Gaps

1. **Key-locked to C** — All examples are hardcoded in C major; users can't see substitutions in their preferred key
2. **No audio comparison** — Users can't hear the difference between original and substituted chords
3. **No fretboard visualization** — No chord diagrams showing what the substitution looks like on ukulele
4. **No connection to Progressions tab** — Can't try a substitution in an actual progression
5. **No interactive "Try It" mode** — Users read about substitutions but can't experiment
6. **No links to Learn section** — No connection to Theory Lessons on harmony/progressions

### Proposed Enhancements

#### C1. Transposable Key Selector

Add a key selector at the top of the screen. All examples dynamically transpose to the selected key:

```kotlin
var selectedKey by remember { mutableIntStateOf(0) } // C = default

// Key selector chips:
// [ C ] [ C# ] [ D ] [ Eb ] ... [ B ]

// Transpose substitution examples:
fun transposeExample(sub: Substitution, fromKey: Int, toKey: Int): Substitution {
    // Recalculate example chord names for the target key
    val offset = (toKey - fromKey + 12) % 12
    // ...
}
```

This makes the guide practical for any key the user is working in.

#### C2. Audio A/B Comparison

Add "Play Original" and "Play Substitute" buttons next to each substitution example:

```kotlin
Row {
    OutlinedButton(onClick = { playChord(originalChord) }) {
        Text("▶ Original")
    }
    Text("→")
    OutlinedButton(onClick = { playChord(substituteChord) }) {
        Text("▶ Substitute")
    }
}
```

Hearing the difference between C → Am or G7 → Db7 is far more instructive than reading about it. Use the existing `ToneGenerator` to play 3–4 note chords.

#### C3. Mini Chord Diagrams

Show small chord diagrams for both the original and substitute chords, using the existing chord rendering components:

```kotlin
Row(horizontalArrangement = Arrangement.SpaceEvenly) {
    MiniChordDiagram(chord = "C", voicing = bestVoicing("C"))
    Icon(Icons.Filled.ArrowForward, null)
    MiniChordDiagram(chord = "Am", voicing = bestVoicing("Am"))
}
```

This shows users exactly how to play the substitution on their ukulele.

#### C4. "Try in Progression" Button

Add a button that takes a standard progression, applies the substitution, and navigates to the Progressions tab:

```kotlin
// Example: Diatonic substitution I → vi
// Original: C - F - G - C  (I - IV - V - I)
// Substituted: Am - F - G - C  (vi - IV - V - I)

TextButton(onClick = {
    onNavigateToProgressions(
        key = selectedKey,
        progression = listOf("vi", "IV", "V", "I"),
    )
}) {
    Text("Try in a progression →")
}
```

#### C5. Cross-Feature Links

- Link to Theory Lesson "Chord Functions" from the Diatonic Substitutions section
- Link to Theory Lesson "Diatonic Harmony" from the introduction
- Link to Circle of Fifths from the Tritone Substitution section (tritone is the opposite point on the circle)
- Link to Theory Quiz (Progressions category) from the practice tip

#### C6. Substitution Quiz Mode

A mini quiz at the bottom of the guide testing substitution knowledge:

- "What is a common substitute for the IV chord?" → ii
- "In a tritone substitution, G7 becomes...?" → Db7
- "Which borrowed chord from C minor adds a bittersweet feel?" → Fm (iv)

This could reuse the same quiz UI pattern from Theory Lessons (the per-lesson mini quiz).

### Files

- **Modify:** `data/ChordSubstitutions.kt` — Add transposition logic, quiz questions
- **Modify:** `ui/ChordSubstitutionsView.kt` — Add key selector, audio buttons, chord diagrams, progression links, quiz
- **Possibly new:** `ui/components/MiniChordDiagram.kt` — Small chord diagram composable (reusable by Theory Lessons Feature D in doc 18)
- **Modify:** `ui/FretboardScreen.kt` — Wire up navigation to Progressions tab with pre-set data

### Effort Estimate

- C1 (Key selector): 1 day
- C2 (Audio A/B): 1 day
- C3 (Mini chord diagrams): 1–2 days
- C4 (Try in progression): 0.5 days
- C5 (Cross-links): 0.5 days
- C6 (Substitution quiz): 1 day
- **Total: 5–6 days**

---

## Feature D: Reference Section Cross-Linking

### Problem

The three Reference features are isolated from each other and from the Learn and Tools sections. The Circle of Fifths links to the Chord Library (the only cross-link in the entire Reference section), but there are natural connections everywhere.

### Proposed Links

| From | To | Trigger |
|------|----|---------|
| Circle of Fifths → Capo Guide | "Use a capo to play in this key" button | Key has complex chords |
| Circle of Fifths → Chord Substitutions | "Substitutions in this key" link | Any key selected |
| Circle of Fifths → Theory Lessons | "Learn about keys" link | Detail panel |
| Circle of Fifths → Theory Quiz | "Test your knowledge" link | Detail panel |
| Circle of Fifths → Scale Overlay | "View scale" link | Key selected |
| Capo Guide → Circle of Fifths | "See key relationships" link | Pitch demo section |
| Capo Guide → Chord Library | Direct navigation button | "Try It" section |
| Capo Guide → Capo Visualizer | Direct navigation button | "Try It" section |
| Chord Substitutions → Progressions | "Try in a progression" button | Per substitution |
| Chord Substitutions → Circle of Fifths | "See on Circle of Fifths" link | Tritone section |
| Chord Substitutions → Theory Lessons | "Learn chord functions" link | Diatonic section |

### Technical Approach

All Reference views need navigation callbacks added to their composable signatures:

```kotlin
@Composable
fun CircleOfFifthsView(
    onChordTapped: (Int, String) -> Unit,        // existing
    onNavigateToCapoGuide: () -> Unit,            // new
    onNavigateToSubstitutions: () -> Unit,        // new
    onNavigateToTheoryLesson: (String) -> Unit,   // new
    onNavigateToQuiz: (QuizCategory) -> Unit,     // new
    onNavigateToScaleOverlay: (Int) -> Unit,      // new
)
```

### Files

- **Modify:** `ui/CircleOfFifthsView.kt`, `ui/CapoGuideView.kt`, `ui/ChordSubstitutionsView.kt` — Add navigation link buttons
- **Modify:** `ui/FretboardScreen.kt` — Wire all cross-navigation callbacks

### Effort Estimate

- **Complexity**: Low
- **Estimated time**: 1 day

---

## Implementation Priority

| Priority | Enhancement | Effort | Impact |
|----------|-------------|--------|--------|
| 1 | D: Cross-Feature Linking | 1 day | Medium |
| 2 | A1: Minor key diatonic chords | 0.5 days | Medium |
| 3 | A2: Related key highlighting | 0.5 days | Medium |
| 4 | B1: Capo Guide navigation links | 0.5 days | Medium |
| 5 | C1: Transposable key selector | 1 day | High |
| 6 | B3: Reverse capo lookup | 1 day | High |
| 7 | A3: Circle of Fifths audio | 1 day | Medium |
| 8 | C2: Audio A/B comparison | 1 day | High |
| 9 | C3: Mini chord diagrams | 1–2 days | High |
| 10 | B2: Capo Guide audio demo | 0.5 days | Low |
| 11 | A4: Cross-feature links | 0.5 days | Medium |
| 12 | A5: Parallel/relative toggle | 0.5 days | Low |
| 13 | C4: Try in progression | 0.5 days | Medium |
| 14 | C5: Cross-links | 0.5 days | Medium |
| 15 | C6: Substitution quiz | 1 day | Medium |
| 16 | B4: Low-G awareness | 0.5 days | Low |
| 17 | B5: Dynamic scenarios | 0.5 days | Low |

Items 1–6 deliver the most value with minimal effort. The Chord Substitutions enhancements (C1–C3) have the highest individual impact since they transform a static guide into an interactive tool.

---

## Architecture Notes

- All enhancements are additive — no existing functionality is removed or broken
- Audio features reuse the existing `ToneGenerator` — no new audio infrastructure needed
- Cross-linking follows the established pattern of passing lambda callbacks through `FretboardScreen.kt`
- Mini chord diagrams (`MiniChordDiagram`) should be a reusable composable shared with Theory Lessons interactive demos (doc 18, Feature D)
- Key transposition logic for Chord Substitutions can reuse `Notes.pitchClassToName()` and `Notes.enharmonicForKey()`

## Dependencies

- Audio enhancements (A3, B2, C2) depend on `ToneGenerator` supporting single-note playback at specified frequencies
- Mini chord diagrams (C3) depend on `VoicingGenerator` being available to compute voicings for arbitrary chords
- Cross-linking (D) requires all target screens to accept optional pre-selection parameters

## Testing Strategy

- Verify Circle of Fifths minor key detail shows correct diatonic chords for all 12 minor keys
- Verify related-key highlighting shows correct adjacent keys on the circle
- Verify audio playback (scale and chords) plays correct notes for selected keys
- Verify Capo Guide navigation buttons open correct Chord Library and Visualizer screens
- Verify reverse capo lookup suggests valid capo positions for all 12 target keys
- Verify Chord Substitutions key selector transposes all examples correctly
- Verify audio A/B comparison plays correct original and substitute chords
- Verify all cross-feature links navigate to the correct screens with correct pre-selections

## Cross-References

- `docs/spec/15-music-theory-learning.md` — Original PRD for music theory features; Circle of Fifths (Feature A) and Key Signatures (Feature F) are implemented
- `docs/spec/16-learn-section-persistence.md` — Persistence patterns applicable if Reference features add saved preferences
- `docs/spec/17-interval-trainer-audio.md` — Audio infrastructure patterns reusable for Circle of Fifths and Chord Substitutions audio
- `docs/spec/18-learn-section-enhancements.md` — Cross-feature linking (Feature A) and interactive demos (Feature D) share components with this plan

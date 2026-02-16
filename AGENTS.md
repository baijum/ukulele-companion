# AGENTS.md — AI Agent Instructions for Ukulele Companion

This file provides mandatory instructions for AI coding agents (Cursor, Copilot, Codex, etc.) working on this codebase. These rules exist to protect accessibility for screen reader users and must never be bypassed.

## Accessibility Requirements

A core user base of this app includes blind and visually impaired musicians who rely on TalkBack (Android) and other screen readers. Every code change must preserve and improve accessibility. Breaking accessibility is treated as seriously as breaking functionality.

### Rule 1: Every Informative Icon Must Have a contentDescription

When adding or modifying an `Icon` composable:

- **Informative/interactive icons** MUST have a descriptive `contentDescription` string
- **Purely decorative icons** (no information conveyed) may use `contentDescription = null`
- When in doubt, add a description — it is always safer

```kotlin
// CORRECT: Interactive icon with description
Icon(Icons.Filled.PlayArrow, contentDescription = "Play melody")

// CORRECT: State-dependent description
Icon(
    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
    contentDescription = if (isPlaying) "Stop" else "Play"
)

// CORRECT: Purely decorative icon (inside a labeled button where the text is sufficient)
Icon(Icons.Filled.Star, contentDescription = null)

// WRONG: Interactive icon without description
Icon(Icons.Filled.Delete, contentDescription = null) // Screen reader says nothing
```

### Rule 2: Use Compose Semantics for Screen Structure

- **Screen titles** must have `Modifier.semantics { heading() }`
- **Section headers** must have `Modifier.semantics { heading() }`
- **Navigation containers** should use `Modifier.semantics { role = Role.Navigation }`

```kotlin
// CORRECT: Screen title announced as heading
Text(
    text = "Tuner",
    style = MaterialTheme.typography.titleLarge,
    modifier = Modifier.semantics { heading() }
)
```

### Rule 3: Canvas and Custom Drawings Need Text Alternatives

Any `Canvas` composable or custom drawing that conveys information MUST be wrapped with semantics providing a text alternative:

```kotlin
// CORRECT: Canvas with accessibility description
Canvas(
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .clearAndSetSemantics {
            contentDescription = "Tuning meter, 5 cents sharp"
        }
) {
    // drawing code
}
```

### Rule 4: Dynamic Content Needs Live Regions

When UI content updates dynamically (tuner readings, chord detection, scores, quiz feedback), use live regions so screen readers announce the changes:

```kotlin
// CORRECT: Tuner note announced when it changes
Text(
    text = detectedNote,
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite  // frequent updates
    }
)

// CORRECT: Important state change announced immediately
Text(
    text = "In Tune!",
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Assertive  // important feedback
    }
)
```

Use `Polite` for frequent updates (pitch, cents). Use `Assertive` for important state changes (in tune, correct answer, error).

### Rule 5: Interactive Elements Must Be Focusable and Described

- Clickable composables must have a content description or visible label
- Custom interactive components (e.g., fretboard cells, chord diagram taps) must include `role = Role.Button` in semantics
- State changes on interactive elements must be reflected in `stateDescription`

```kotlin
// CORRECT: Interactive fret cell with full description
Box(
    modifier = Modifier
        .clickable { onFretTap(string, fret) }
        .semantics {
            contentDescription = "$stringName string, fret $fretNumber, $noteName"
            role = Role.Button
            if (isSelected) stateDescription = "selected"
        }
)
```

### Rule 6: Modals and Bottom Sheets Must Manage Focus

When creating or modifying `ModalBottomSheet`, `AlertDialog`, or overlay composables:

- The title must have `Modifier.semantics { heading() }`
- Focus should move to the modal content when it opens
- Dismissing the modal should return focus to the trigger element

### Rule 7: Never Remove Existing Accessibility Attributes

- Do not remove `contentDescription` from icons during refactoring
- Do not remove `Modifier.semantics {}` blocks
- Do not remove `liveRegion` annotations
- If restructuring a composable, ensure all accessibility attributes are preserved in the new structure

## Pre-Submission Accessibility Checklist

Before submitting any code change, verify:

- [ ] All new icons have appropriate `contentDescription` (or explicit `null` for decorative only)
- [ ] All new screens/sections have heading semantics
- [ ] All new Canvas/custom drawings have text alternatives via `clearAndSetSemantics`
- [ ] All new dynamic content has `liveRegion` where needed
- [ ] All new interactive elements are focusable and described
- [ ] No existing accessibility attributes were removed or broken
- [ ] The feature works correctly with TalkBack enabled (test on device or emulator)

## How to Test with TalkBack

1. Enable TalkBack: Settings > Accessibility > TalkBack > On
2. Navigate your changed screens using swipe gestures (swipe right = next element, swipe left = previous)
3. Verify every interactive element is announced with a meaningful description
4. Verify state changes are announced (e.g., toggling play/stop, selecting a chord)
5. Verify dynamic content updates are spoken (e.g., tuner readings, quiz feedback)
6. Verify you can complete the full user flow without seeing the screen

## Patterns Reference

### Existing contentDescription Style

- Use clear, action-oriented descriptions: `"Open navigation menu"`, `"Start tuning"`, `"Delete note"`
- Use conditional descriptions for toggle states: `if (isPlaying) "Stop" else "Play"`
- Capitalize the first word only (sentence case): `"Play all inversions"`, not `"Play All Inversions"`

### Accessibility Utility Functions

When describing chord voicings for screen readers, generate descriptions from data:

```kotlin
fun ChordVoicing.toAccessibilityDescription(): String {
    val stringDescriptions = positions.mapIndexed { index, fret ->
        val stringName = listOf("G", "C", "E", "A")[index]
        when (fret) {
            -1 -> "$stringName string muted"
            0 -> "$stringName string open"
            else -> "$stringName string fret $fret"
        }
    }
    return "${name}: ${stringDescriptions.joinToString(", ")}"
}
```

### Key Files

| Component | File | Accessibility Notes |
|-----------|------|-------------------|
| Main navigation | `ui/FretboardScreen.kt` | Heading semantics on titles, drawer structure |
| Tuner | `ui/TunerTab.kt` | Live regions for note/cents, canvas alternative |
| Chord diagrams | `ui/VerticalChordDiagram.kt` | clearAndSetSemantics on Canvas |
| Fretboard | `ui/FretboardView.kt` | Cell semantics, selection announcements |
| Pitch monitor | `ui/PitchMonitorTab.kt` | Live regions, canvas alternative |
| Circle of Fifths | `ui/CircleOfFifthsView.kt` | Canvas alternative, key selection announcements |
| Settings | `ui/SettingsSheet.kt` | Section heading semantics |
| Theme | `ui/theme/Theme.kt` | High contrast theme support |

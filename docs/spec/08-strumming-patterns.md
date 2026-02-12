# Feature: Strumming Pattern Guide

**Status: IMPLEMENTED (v1.6)**

## Summary

Display basic ukulele strumming patterns visually, helping beginners learn rhythm alongside chords. Show common patterns with directional arrows and optional timing indicators.

## Motivation

- Beginners often learn chord shapes but struggle with strumming rhythm
- "How do I strum this?" is a top beginner question alongside "What chord is this?"
- Visual strumming guides complement the chord explorer naturally
- Keeps the app as a one-stop beginner reference

## Scope

### In Scope

- Library of 8–12 common strumming patterns
- Visual representation with down (↓) and up (↑) arrows
- Timing indicators (quarter notes, eighth notes)
- Pattern name and description (e.g., "Island Strum", "Basic Down")
- Suggested tempo range for each pattern
- Pattern categorization by difficulty (Beginner, Intermediate)

### Out of Scope

- Audio playback of strumming patterns (could integrate with Sound Playback later)
- Animated strumming demonstration
- Custom pattern creation
- Metronome functionality
- Fingerpicking patterns (different from strumming)

## Technical Approach

### Pattern Data

```kotlin
enum class StrumDirection { DOWN, UP, MISS, PAUSE }

data class StrumBeat(
    val direction: StrumDirection,
    val emphasis: Boolean = false,  // accented beat
)

data class StrumPattern(
    val name: String,
    val description: String,
    val difficulty: Difficulty,
    val beatsPerMeasure: Int,       // 4 for 4/4 time
    val beats: List<StrumBeat>,
    val suggestedBpm: IntRange,
    val notation: String,           // e.g., "D - D U - U D U"
)

enum class Difficulty { BEGINNER, INTERMEDIATE }
```

### Standard Patterns

| Name | Notation | Difficulty |
|------|----------|------------|
| All Downs | D D D D | Beginner |
| Down-Up | D U D U D U D U | Beginner |
| Island Strum | D DU UDU | Beginner |
| Calypso | D - DU -U | Intermediate |
| Ska | - U - U - U - U | Intermediate |
| Swing | D - U D - U | Intermediate |
| Folk | D DU D DU | Beginner |
| Reggae | - D - D | Beginner |

### Visual Rendering

Use Compose Canvas to draw:
- Horizontal timeline with beat divisions
- Arrows pointing down (↓) or up (↑) at each beat position
- Emphasized beats drawn larger or bolder
- Missed strums shown as dotted or grayed arrows
- Beat count numbers below (1 & 2 & 3 & 4 &)

## Architecture

```
data/
  └── StrumPatterns.kt         -- pattern definitions (new)

ui/
  └── FretboardScreen.kt      -- navigation to patterns
  └── StrumPatternsTab.kt     -- pattern list (new)
  └── StrumPatternView.kt     -- visual pattern renderer (new)
```

## UX Design

- **Access**: New tab "Patterns" or accessible from a menu/icon
- **Layout**: Scrollable list of pattern cards
- **Each card shows**:
  - Pattern name and difficulty badge
  - Visual arrow representation
  - Beat counting (1 & 2 & 3 & 4 &)
  - Notation string (D DU UDU)
  - Brief description and suggested tempo
- **Interaction**: Tap to expand for fuller view; patterns are read-only reference material
- **Design**: Clean, minimalist, consistent with the app's educational tone

## Edge Cases

- Patterns with different time signatures (3/4 waltz) — support `beatsPerMeasure` flexibility
- Very fast patterns may need wider display — use horizontal scrolling if needed
- Accessibility: use content descriptions for arrows for screen readers

## Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 1–1.5 days
- **New files**: 3
- **Modified files**: 1
- **APK size impact**: Negligible

## Dependencies

- None

## Testing

- Unit test: verify all pattern definitions have valid beat counts
- Manual test: verify visual rendering matches notation string for each pattern
- Manual test: scrolling and card layout work on different screen sizes

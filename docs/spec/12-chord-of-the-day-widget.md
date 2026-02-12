# Feature: Chord of the Day Widget

**Status: IMPLEMENTED (v1.6)**

## Summary

A home screen widget that displays a new chord each day with its name, diagram, and finger positions. Tapping the widget opens the app with that chord loaded on the fretboard. Encourages daily learning.

## Motivation

- Home screen presence keeps the app top-of-mind
- "Learn one chord a day" is an effective beginner strategy
- Passive exposure to new chords broadens the user's vocabulary
- Widgets are an underused feature in music education apps — differentiator

## Scope

### In Scope

- Android home screen widget (Glance / AppWidgetProvider)
- Displays: chord name, mini diagram, finger positions
- Rotates daily through a curated list of chords
- Tap widget → opens app with that chord on the fretboard
- Resizable widget (small: name only, medium: name + positions, large: name + diagram)
- Works fully offline

### Out of Scope

- User-configurable chord rotation (e.g., "only show minor chords")
- Multiple widgets with different chords
- Notification-based chord of the day (could be separate feature)
- Progress tracking (e.g., "chords learned" counter)

## Technical Approach

### Chord Selection Algorithm

Use a deterministic, date-based selection to ensure:
- Same chord appears for the entire day
- Different chord each day
- Cycles through all available chords before repeating

```kotlin
fun chordOfTheDay(date: LocalDate): ChordOfDay {
    val daysSinceEpoch = date.toEpochDay()
    val chordIndex = (daysSinceEpoch % CURATED_CHORDS.size).toInt()
    return CURATED_CHORDS[chordIndex]
}
```

### Curated Chord List

A list of ~60–90 chords ordered by difficulty:
- Week 1–2: Open major chords (C, F, G, A, D)
- Week 3–4: Open minor chords (Am, Dm, Em)
- Week 5–8: Seventh chords, barre chords, etc.

### Widget Implementation

**Option A: Jetpack Glance (Recommended)**
- Modern, Compose-like API for widgets
- Easier to maintain alongside the Compose UI codebase
- Requires `androidx.glance:glance-appwidget`

**Option B: Traditional AppWidgetProvider + RemoteViews**
- More mature, broader device compatibility
- XML layouts, less flexible

### Recommended: Jetpack Glance

```kotlin
class ChordOfDayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val chord = chordOfTheDay(LocalDate.now())
        provideContent {
            ChordWidgetContent(chord)
        }
    }
}
```

### Widget Update Schedule

- Use `AlarmManager` or `WorkManager` to trigger a daily update at midnight
- `onUpdate()` in the widget receiver refreshes the displayed chord
- Fallback: update when the widget is first added and on each system boot

### Deep Link

Tapping the widget launches the app with an intent extra:
```kotlin
intent.putExtra("chord_root", chord.rootPitchClass)
intent.putExtra("chord_symbol", chord.symbol)
```

The `MainActivity` reads these extras and navigates to the Explorer with the chord applied.

## Architecture

```
data/
  └── ChordOfDay.kt            -- curated chord list + selection logic (new)

widget/
  └── ChordOfDayWidget.kt      -- Glance widget (new)
  └── ChordWidgetReceiver.kt   -- widget provider/receiver (new)
  └── ChordWidgetContent.kt    -- widget UI composable (new)

ui/
  └── MainActivity.kt          -- handle deep link intent
```

### Manifest Additions

```xml
<receiver android:name=".widget.ChordWidgetReceiver" ...>
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/chord_widget_info" />
</receiver>
```

## UX Design

- **Small widget** (2×1): Chord name + finger positions text
- **Medium widget** (3×2): Chord name + mini diagram + positions
- **Large widget** (4×2): Full diagram + chord name + quality + notes
- **Styling**: Match app theme, clean white/dark background with clear contrast
- **Tap action**: Opens app directly to the chord
- **Update**: Smooth transition at midnight, no flicker

## Edge Cases

- Widget added but app data cleared: chord selection is date-based, no dependency on app state
- Very small widget size: gracefully degrade to text-only display
- Device timezone change: chord updates based on local date
- Multiple widgets: all show the same chord (consistent experience)

## Effort Estimate

- **Complexity**: Medium–High
- **Estimated time**: 2–3 days
- **New files**: 4–5
- **Modified files**: 2 (MainActivity, AndroidManifest)
- **APK size impact**: Minimal

## Dependencies

- `androidx.glance:glance-appwidget` (for Glance approach)
- `androidx.glance:glance-material3` (for Material3 theming in widget)

## Testing

- Unit test: chord selection algorithm produces different chords on different dates
- Unit test: chord selection wraps around after exhausting the list
- Unit test: same date always produces the same chord
- Manual test: add widget to home screen, verify chord displays
- Manual test: verify chord updates the next day
- Manual test: tap widget opens app with correct chord
- Manual test: widget renders correctly at different sizes

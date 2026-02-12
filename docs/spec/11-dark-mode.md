# Feature: Dark Mode / Theme Toggle

**Status: IMPLEMENTED (v1.5)**

## Summary

Add a dark mode option alongside the existing light theme, allowing users to switch between light and dark themes or follow the system setting. Reduces eye strain during evening practice sessions.

## Motivation

- Dark mode is expected in modern Android apps
- Musicians often practice in dimly lit environments
- Reduces eye strain and battery usage on OLED screens
- Android's Material3 / Material You has excellent dark theme support built in

## Scope

### In Scope

- Dark color scheme for all screens (Explorer, Chord Library, any new tabs)
- Theme toggle: Light / Dark / System (follow device setting)
- Fretboard and chord diagram colors adapt to the theme
- Persist theme preference across sessions

### Out of Scope

- Custom color themes / accent color picker
- Per-screen theming
- High contrast / accessibility themes (could be a future addition)

## Technical Approach

### Current State

The app uses Material3 theming via `Theme.kt`. Jetpack Compose and Material3 provide built-in dark theme support through `darkColorScheme()`.

### Implementation

1. **Define dark color scheme** in `Theme.kt`:
   ```kotlin
   private val DarkColorScheme = darkColorScheme(
       primary = ...,
       onPrimary = ...,
       surface = ...,
       // etc.
   )
   ```

2. **Theme selection logic**:
   ```kotlin
   @Composable
   fun UkuleleCompanionTheme(
       themeMode: ThemeMode = ThemeMode.SYSTEM,
       content: @Composable () -> Unit,
   ) {
       val darkTheme = when (themeMode) {
           ThemeMode.LIGHT -> false
           ThemeMode.DARK -> true
           ThemeMode.SYSTEM -> isSystemInDarkTheme()
       }
       MaterialTheme(
           colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
           content = content,
       )
   }
   ```

3. **Fretboard colors**: The fretboard uses custom Canvas drawing with hardcoded colors. These need to be pulled from the theme:
   - Fret lines: `MaterialTheme.colorScheme.outline`
   - String lines: `MaterialTheme.colorScheme.onSurface`
   - Selected dot: `MaterialTheme.colorScheme.primary`
   - Background: `MaterialTheme.colorScheme.surface`

4. **Chord diagram colors**: Same approach — derive from theme instead of hardcoded values.

5. **Preference storage**: DataStore (shared with other settings).

### ThemeMode Enum

```kotlin
enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}
```

## Architecture

```
data/
  └── UserPreferences.kt      -- theme preference (shared)

ui/
  └── theme/Theme.kt          -- add dark color scheme, ThemeMode
  └── FretboardView.kt        -- use theme colors instead of hardcoded
  └── ChordDiagramPreview.kt  -- use theme colors
  └── FretboardScreen.kt      -- theme toggle in settings/app bar

viewmodel/
  └── SettingsViewModel.kt    -- theme preference management (new or shared)
```

## UX Design

- **Toggle location**: Settings icon in app bar → dialog or bottom sheet with theme options
- **Options**: Three-way toggle: Light ☀️ / Dark 🌙 / System 📱
- **Default**: System (respects device-wide setting)
- **Transition**: Instant, no animation needed (Compose handles recomposition)
- **Fretboard in dark mode**:
  - Dark wood-toned background or dark gray
  - Light-colored fret lines and strings
  - Bright accent color for selected dots
  - High contrast for readability

## Edge Cases

- Custom Canvas drawings (fretboard, diagrams) must explicitly use theme-aware colors
- Text contrast: ensure all text meets WCAG AA contrast ratios in both themes
- Theme change while on Chord Library: all diagram previews re-render
- System theme change while app is open: should update automatically if using `SYSTEM` mode

## Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 1–1.5 days
- **New files**: 0–1
- **Modified files**: 4–6
- **APK size impact**: None

## Dependencies

- DataStore (shared with other settings)
- Material3 (already included)

## Testing

- Manual test: verify all screens look correct in light mode
- Manual test: verify all screens look correct in dark mode
- Manual test: system mode follows device setting
- Manual test: fretboard and diagrams are readable in both themes
- Manual test: preference persists after restart
- Accessibility test: verify text contrast ratios

# Plan: iOS Songbook Parity Gaps

## Scope
Close five feature and accessibility gaps in the iOS Songbook where Android has
equivalent functionality or the implementation has architectural issues.

- `iosApp/UkuleleCompanion/ContentView.swift`
- `iosApp/UkuleleCompanion/Views/SongbookView.swift`
- `iosApp/UkuleleCompanion/Views/SongEditorView.swift`
- `iosApp/UkuleleCompanion/Views/SongwriterModeFlow.swift`
- `iosApp/UkuleleCompanion/Views/SettingsView.swift`
- `iosApp/UkuleleCompanion/Views/CreateView.swift`
- `iosApp/UkuleleCompanion/ViewModels/SongbookViewModel.swift`

## Gap A: Share a Single SongbookViewModel Across Screens

### Problem
`SongbookView`, `SongwriterModeFlow`, and `SettingsView` each create their own
`@StateObject SongbookViewModel()`. They all read/write the same UserDefaults key
`"chord_sheets"`, but in-memory state does not sync. Saving a song in Songwriter
Mode will not appear in an already-open Songbook list until the view is recreated.

Android uses a single `SongbookViewModel` instance shared across Songbook,
Songwriter, and Achievements, so all screens see the same live data.

### Approach
1. Create a single `@StateObject private var songbookVM = SongbookViewModel()` in
   `ContentView.swift`.
2. Inject it via `.environmentObject(songbookVM)` on the root view hierarchy.
3. Update each consumer:
   - `SongbookView`: change `@StateObject private var viewModel = SongbookViewModel()`
     to `@EnvironmentObject var viewModel: SongbookViewModel`.
   - `SongwriterModeFlow`: change `@StateObject private var songbookViewModel = SongbookViewModel()`
     to `@EnvironmentObject var songbookViewModel: SongbookViewModel`.
   - `SettingsView`: change `@StateObject private var songbookVM = SongbookViewModel()`
     to `@EnvironmentObject var songbookVM: SongbookViewModel`.
4. Verify that `NavigationLink` destinations in `CreateView` propagate the
   environment (SwiftUI does this by default for `NavigationLink` within a
   `NavigationStack`).
5. Update `SongbookViewModelTests.swift` if tests create views that require the
   environment object.

### Risk
This changes ownership semantics. If any view currently relies on getting a fresh
VM instance (e.g. to reset search/sort state on appear), that behavior will change.
Mitigation: add an `onAppear { viewModel.clearFilters() }` if needed.

### Files to Modify
- `iosApp/.../ContentView.swift` — create and inject shared VM
- `iosApp/.../Views/SongbookView.swift` — `@StateObject` → `@EnvironmentObject`
- `iosApp/.../Views/SongwriterModeFlow.swift` — `@StateObject` → `@EnvironmentObject`
- `iosApp/.../Views/SettingsView.swift` — `@StateObject` → `@EnvironmentObject`
- `iosApp/.../Views/CreateView.swift` — verify environment propagation

---

## Gap B: Include Custom Strum Patterns in Songbook Pickers

### Problem
The songbook strum pattern picker in `SongbookView.swift` (~line 590) and
`SongEditorView.swift` (~line 91) only show `StrumPatterns.shared.ALL` (built-in
KMP patterns). Custom patterns exist via `CustomPatternsViewModel` /
`CustomStrumPatternData` (UserDefaults key `"custom_strum_patterns"`), but the
songbook pickers do not include them.

Android includes custom strum patterns from `CustomStrumPatternRepository`
alongside built-in patterns in its songbook strum picker.

### Approach
1. Pass `CustomPatternsViewModel` into `SongViewerView` and `SongEditorView` —
   either as a parameter or via `@EnvironmentObject`.
2. In the strum picker list/picker UI, add a "Custom" section after the built-in
   patterns listing each custom pattern's name and notation.
3. When a custom pattern is selected, store its name in `strumPatternName` the
   same way built-in patterns are stored.

### Files to Modify
- `iosApp/.../Views/SongbookView.swift` — `strumPatternPicker` in `SongViewerView`
- `iosApp/.../Views/SongEditorView.swift` — strum pattern `Picker` section

---

## Gap C: Add Accessibility Labels to Auto-Scroll Speed Buttons

### Problem
Speed chips (0.5x, 1.0x, 2.0x, 3.0x) in `SongViewerView` (~line 705 in
`SongbookView.swift`) have no `.accessibilityLabel`. The play/pause/stop buttons
do have labels, but the speed buttons are bare `Text` capsules. VoiceOver users
cannot distinguish them meaningfully.

### Approach
Add modifiers to each speed button in the `ForEach` block:
```swift
.accessibilityLabel("Speed \(speed, specifier: "%.1f")x")
.accessibilityAddTraits(scrollSpeed == speed ? .isSelected : [])
```

### Files to Modify
- `iosApp/.../Views/SongbookView.swift` — speed button `ForEach` block (~line 710)

---

## Gap D: Add Live Region Announcement for Songwriter Mode Step Changes

### Problem
Android uses `LiveRegionMode.Polite` on Songwriter step labels so TalkBack
announces step transitions automatically. iOS `SongwriterModeFlow` has no
equivalent — step changes are silent for VoiceOver users.

### Approach
Use `AccessibilityAnnouncer.shared.announce(...)` when the step index changes.
Add an `.onChange(of: currentStep)` modifier that announces the new step title,
for example:
```swift
.onChange(of: currentStep) { _, newStep in
    let stepTitles = ["Choose Key & Scale", "Build Progression", ...]
    AccessibilityAnnouncer.shared.announce("Step \(newStep + 1): \(stepTitles[newStep])")
}
```

### Files to Modify
- `iosApp/.../Views/SongwriterModeFlow.swift` — add `.onChange(of: currentStep)`

---

## Gap E: Render Chords Above Lyrics (ChordPro Style) — ALREADY IMPLEMENTED

> **Status: Complete.** iOS already implements chords-above-lyrics rendering via
> `buildChordsAboveLyrics` in `parsedLineView` within `SongbookView.swift`.
> The chord row renders as an `HStack` above a separate `Text` lyric line in a
> `VStack(alignment: .leading, spacing: 0)`. No further work needed.

---

## Priority
Medium-High — Gap A (shared ViewModel) is an architectural fix that prevents stale
data bugs. Gap C, D are accessibility fixes for VoiceOver users, which the project
treats as seriously as functional bugs per AGENTS.md. Gap E is already implemented.

## Estimated Effort
- Gap A: ~1-2 hours (refactor ownership, verify navigation propagation, test)
- Gap B: ~1 hour (wire custom patterns into two pickers)
- Gap C: ~15 minutes (add two accessibility modifiers)
- Gap D: ~30 minutes (add onChange handler with announcements)
- Gap E: ~0 hours (already implemented)

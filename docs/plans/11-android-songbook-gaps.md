# Plan: Android Songbook Parity Gaps

## Scope
Close three feature gaps in the Android Songbook where iOS has equivalent functionality.

- `app/src/main/java/com/baijum/ukufretboard/ui/SongbookTab.kt`
- `app/src/main/java/com/baijum/ukufretboard/ui/FretboardScreen.kt`
- `app/src/main/java/com/baijum/ukufretboard/audio/ToneGenerator.kt`

## Gap A: Add Chord Playback in Songbook Viewer

### Problem
Tapping a chord in the songbook viewer only navigates to the Chord Library via
`onChordTapped` → `navigateToChord` → `NAV_LIBRARY` (SongbookTab.kt ~line 741,
FretboardScreen.kt ~line 614). There is no in-context audio playback. iOS shows a
popover with a chord diagram and a Play button using `TonePlayer`.

### Context
Android already has `ToneGenerator.playChord()` (~line 168 in
`audio/ToneGenerator.kt`) and the shared module provides `VoicingGenerator` for
generating chord voicings.

### Approach
1. Add a `tappedChord` state to the `SheetViewer` section of `SongbookTab.kt`.
2. On chord tap, show a bottom sheet (or popup) with:
   - `ChordDiagramView` for the tapped chord.
   - A "Play" button that calls `ToneGenerator.playChord` with notes from
     `VoicingGenerator`.
   - A "View in Library" link that calls the existing `onChordTapped` navigation.
3. In `FretboardScreen.kt`, pass a `ToneGenerator` instance (or a play callback)
   into `SongbookTab` alongside the existing `onChordTapped`.

### Accessibility
- Bottom sheet heading with `semantics { heading() }`.
- Chord name as content description on the diagram.
- Play button: `contentDescription = "Play [chord name] chord"`.
- "View in Library" link with clear action description.

### Files to Modify
- `app/.../ui/SongbookTab.kt` — bottom sheet UI, `tappedChord` state
- `app/.../ui/FretboardScreen.kt` — pass `ToneGenerator` or play callback

---

## Gap B: Add Paste-Based ChordPro Import

### Problem
Import is file-only via `ActivityResultContracts.OpenDocument()` (~line 214 in
SongbookTab.kt). iOS has a dedicated "Import ChordPro" sheet with a `TextEditor`
for pasting clipboard content directly.

### Approach
1. Add a "Paste ChordPro" option next to the existing file import FAB (or in a
   dropdown menu from it).
2. Show a dialog or bottom sheet containing:
   - A multi-line `TextField` for pasting ChordPro text.
   - Import and Cancel buttons.
3. On Import, call `viewModel.importChordPro(content, null)`.

### Accessibility
- Dialog title with `semantics { heading() }`.
- `TextField` label: "Paste ChordPro content".
- Import button content description.

### Files to Modify
- `app/.../ui/SongbookTab.kt` — paste import dialog state and composable

---

## Gap C: Add TalkBack Announcement on Clipboard Copy

### Problem
Copying formatted text to clipboard (~line 895 in SongbookTab.kt) shows a Toast
but has no explicit TalkBack announcement. iOS uses
`AccessibilityAnnouncer.shared.announce("Copied to clipboard")` for VoiceOver.

### Context
Toast messages are not reliably read by TalkBack. An explicit accessibility
announcement ensures blind users know the copy succeeded.

### Approach
After `ShareUtils.copyToClipboard(...)`, add an accessibility announcement using
`LocalView.current.announceForAccessibility(copiedMsg)` from Compose.

### Files to Modify
- `app/.../ui/SongbookTab.kt` — add `announceForAccessibility` call after copy

---

## Priority
Medium — these are UX parity gaps, not broken functionality. Gap A (chord playback)
is the largest change; Gap C (TalkBack announcement) is a one-line fix.

## Estimated Effort
- Gap A: ~2-3 hours (new bottom sheet composable, wiring ToneGenerator)
- Gap B: ~1 hour (dialog with TextField)
- Gap C: ~15 minutes (single accessibility call)

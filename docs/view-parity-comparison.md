# Android vs iOS View Parity Comparison

Comparison of UI layer (views only) for Play and Create features. Focus: buttons, controls, dialogs, interactions, and accessibility attributes.

---

## 1. FretboardView (interactive fretboard grid)

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Interactive fret cells (tap to select) | Yes, clickable cells with full semantics | Yes, Button with FretCellView | matched |
| String labels (G, C, E, A) | Yes, fixed column | Yes, stringLabelsColumn | matched |
| Fret numbers row | Yes, with capo badge | Yes, fret numbers (no capo badge) | partial |
| Position markers (dots at 5, 7, 10, 12) | Yes, SINGLE_MARKER_FRETS + DOUBLE_MARKER_FRETS | Yes, markerFrets + doubleMarkerFret | matched |
| Horizontal scroll | Yes, scrollable | Yes, ScrollView | matched |
| Left-handed mode | Yes, fretRange reversed, isNutOnLeft | No, fixed layout | missing-ios |
| Scale overlay (scaleNotes, scaleRoot, scalePositionFretRange) | Yes, inScale/isScaleRoot dots, SCALE_DOT_SIZE | No | missing-ios |
| Capo visualization (capoFret, blocked cells) | Yes, capo bar, blocked overlay, CapoBadgeColor | No | missing-ios |
| Configurable lastFret | Yes, parameter | No, uses FretboardViewModel.fretCount | partial |
| Cell semantics (contentDescription, role, selected, stateDescription) | Yes, full buildString with fretboard_string, fretboard_selected, etc. | Yes, accessibilityLabel, accessibilityHint, accessibilityAddTraits(.isSelected) | matched |
| Blocked-by-capo stateDescription | Yes, fretboardBlockedText | N/A (no capo) | missing-ios |
| Nut drawing | Yes, thick line on open-string side | Yes, nutWidth in drawFretboard | matched |
| Note names in cells | Yes, showNoteNames | Yes, showNoteName | matched |

---

## 2. ChordResultView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| No selection state | Yes, "Tap the fretboard" + SuggestedChordsSection + ExplorerDidYouKnowCard | Yes, "Tap the fretboard to explore" | partial |
| Single note state | Yes, ChordHeadlineWithPlay + "Single note" | Yes, name + "Single note" | matched |
| Interval state | Yes, note names + "Incomplete chord" | Yes, notes + "Interval" | partial |
| Chord found state | Yes, displayName with slash notation, quality, inversion | Yes, name, quality | partial |
| Alternate chord names (tappable) | Yes, onAlternateChordTapped, slash notation | Yes, alternates as non-tappable capsules | partial |
| Play button | Yes, IconButton with PlayArrow | Yes, Button with play.fill | matched |
| Share button | Yes, onShareChord callback | Yes, onShareChord | matched |
| Show in library (tap chord name) | Yes, onShowInLibrary, underlined | No | missing-ios |
| Finger positions display | Yes, monospace | Yes, frets joined | matched |
| Chord detail section (intervals, formula, fingering, difficulty, inversion) | Yes, ChordInfoRow for each | Yes, chordDetailSection | matched |
| Capo-adjusted inversion | Yes, invFrets = frets + capoFret | No (no capo in ChordResultView) | partial |
| Suggested chords (C, Am, F, G7 chips) | Yes, SuggestedChordsSection when no selection | No | missing-ios |
| Did You Know / Explorer tips card | Yes, ExplorerDidYouKnowCard with next tip | No | missing-ios |
| Alternate notation (aliases) | Yes, "Also written as" | Yes, ChordDetail aliasText | matched |
| Accessibility: liveRegion on chord | No | No | matched |
| Accessibility: combined labels | Android uses semantics; iOS uses accessibilityCombined | Both have accessibility | matched |

---

## 3. TunerTab / TunerView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Tuning label | Yes, tuning.localizedLabel() + string names + A4 ref | No | missing-ios |
| SwiftF0 / Neural status badge | Yes, SwiftF0StatusBadge | No | missing-ios |
| Precision mode badge | Yes, PrecisionModeBadge | No | missing-ios |
| Detected note display | Yes, large display, color by status | Yes, noteName, noteColor | matched |
| Last settled note (SILENT state) | Yes, lastSettledNote, "Last note" label | No | missing-ios |
| Needle meter (semicircular) | Yes, NeedleMeter Canvas, -50..+50 cents | No, linear CentsGaugeView | partial |
| Numeric cents display | Yes, "0 ¢", "+5 ¢", etc. | No (gauge only) | missing-ios |
| Guidance text | Yes, tuner_play_a_string, tuner_in_tune, etc. | Yes, tuningStatus | matched |
| All strings tuned celebration | Yes, AnimatedVisibility "All strings tuned!" | No | missing-ios |
| String reference buttons (G, C, E, A) | Yes, StringButton with isActive, isTuned, isAutoAdvanceTarget | No | missing-ios |
| Start/Stop button | Yes, Mic/MicOff icons | Yes, toggleCapture | matched |
| Auto-start (tunerSettings.autoStart) | Yes | No | missing-ios |
| Auto-advance target highlight | Yes, isAutoAdvanceTarget on StringButton | N/A | missing-ios |
| Haptic on string tuned | Yes, LongPress haptic | No | missing-ios |
| Spoken feedback toggle | Yes, tunerSettings.spokenFeedback suppresses liveRegion | iOS has AccessibilityAnnouncer | partial |
| liveRegion on note/guidance | Yes, Polite/Assertive | Yes, updatesFrequently | partial |
| Canvas semantics (clearAndSetSemantics) | Yes, meterDescription | Yes, centsAccessibilityValue | matched |
| Octave display | No | Yes, "Octave N" | missing-android |
| Frequency display | No | Yes, "%.1f Hz" | missing-android |
| String match display | No | Yes, "String: X", tuningStatus | partial (Android has string buttons instead) |

---

## 4. PitchMonitorTab / PitchMonitorView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Recent notes (horizontal scroll) | Yes, badges, latest highlighted | Yes, same | matched |
| Detected chord display | Yes, primaryContainer background | Yes, accentColor.opacity(0.15) | matched |
| Arpeggio label | Yes, pitch_monitor_arpeggio_label | No | missing-ios |
| Scrolling pitch canvas | Yes, PitchCanvas with note grid, chroma glow, trace | Yes, drawPitchCanvas | matched |
| Chroma glow on active lanes | Yes, drawChromaGlow | Yes | matched |
| Pitch trace + current dot | Yes | Yes | matched |
| Start/Stop button | Yes, Mic/MicOff | Yes | matched |
| liveRegion on recent notes | Yes, LiveRegionMode.Polite | No | partial |
| liveRegion on chord (Assertive) | Yes | No | partial |
| clearAndSetSemantics on canvas | Yes, pitchVizDesc | Yes, accessibilityCombined | matched |
| RequireMicPermission wrapper | Yes | No (handled in ViewModel/onAppear) | partial |
| DisplayLink / frame drive | Yes, LaunchedEffect + awaitFrame | Yes, DisplayLinkTimer | matched |

---

## 5. MetronomeTab / MetronomeView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| BPM display | Yes, displayLarge | Yes, 72pt | matched |
| BPM +/- buttons | Yes, IconButton Remove/Add | Yes, minus.circle / plus.circle | matched |
| BPM slider | Yes, 30..300 | Yes, 30...300 | matched |
| Tap Tempo | Yes, TapTempoButton | Yes, "Tap Tempo" button | matched |
| Time signature chips | Yes, 2/4, 3/4, 4/4, 5/4, 6/4, 7/4, 6/8, 12/8 | Yes, same list | matched |
| Accent pattern (beat indicators) | Yes, BeatIndicator with ACCENT/NORMAL/MUTE | Yes, BeatIndicatorView | matched |
| Tap to change beat type | Yes, toggleBeatType | Yes, onTapGesture | matched |
| Subdivision (Quarter, Eighth, Triplet, Sixteenth) | Yes, FilterChips | Yes, subdivisionLabels | matched |
| Compound subdivision locked message | Yes, metronome_compound_subdivision_locked | Yes, "Subdivision is fixed for compound meters" | matched |
| Play/Stop FAB | Yes, 72dp FloatingActionButton | Yes, 72x72 Circle button | matched |
| Measure counter | Yes, when playing | Yes, when playing | matched |
| Beat indicator semantics | Yes, contentDescription, role, stateDescription | Yes, accessibilityLabel, accessibilityHint, accessibilityAddTraits | matched |
| liveRegion on BPM | Yes, LiveRegionMode.Polite | Yes, accessibilityValue | matched |
| Heading semantics | Yes, semantics { heading() } on section labels | Yes, accessibilityAddTraits(.isHeader) | matched |

---

## 6. ChordLibraryTab / ChordLibraryView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Chord search bar | Yes, OutlinedTextField + suggestions dropdown | Yes, TextField + results | matched |
| Search results (select to apply) | Yes, ChordSuggestionRow, selectSearchResult | Yes, ChordSuggestionRow | matched |
| Root note selector | Yes, 12 FilterChips | Yes, 12 buttons | matched |
| Category selector | Yes, Triad, Seventh, Suspended, Extended | Yes, allCategories | matched |
| Formula selector | Yes, FilterChips per formula | Yes, formulaSelector | matched |
| Transpose controls | Yes, TransposeControls | No | missing-ios |
| Collapsible filters (FilterToggleRow) | Yes, expanded/collapsed | No, always visible | partial |
| Inversion filter chips | Yes, All, Root, 1st Inv, 2nd Inv, 3rd Inv (with counts) | No | missing-ios |
| Capo button | Yes, "Capo" OutlinedButton | Yes, context menu "Capo Positions" | partial |
| Capo Visualizer button | Yes, "Viz" OutlinedButton | Yes, context menu "Capo Visualizer" | partial |
| Compare inversions mode | Yes, InversionCompareView, Play All Inversions | No | missing-ios |
| Voicing grid | Yes, LazyVerticalGrid, VerticalChordDiagram | Yes, LazyVGrid, ChordDiagramView | matched |
| Long-press / context menu | Yes, onVoicingLongPressed | Yes, .contextMenu | matched |
| Favorite heart on diagram | Yes, isFavorite, onFavoriteClick | No (no favorites in library grid) | partial |
| Play voicing | Yes, onPlayVoicing | No (onApplyVoicing only) | partial |
| Play all inversions | Yes, onPlayVoicingsSequentially | No | missing-ios |
| Share as image | Yes, onVoicingLongPressed → share | Yes, ShareChordSheet | matched |
| Left-handed mode | Yes, leftHanded param | No | missing-ios |
| Chord diagram: inversion label, bass highlight | Yes, inversionLabel, bassStringIndex | No | missing-ios |
| Search bar semantics | Yes, contentDescription | Yes, accessibilityLabel | matched |

---

## 7. FavoritesTab / FavoritesView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Empty state | Yes, title + message | Yes, heart icon + message | matched |
| Folder chips (All + folders) | Yes, FilterChip per folder | Yes, Button/Menu per folder | matched |
| Create folder | Yes, "+" FilterChip, AlertDialog | Yes, "+" button, alert | matched |
| Rename folder | Yes, IconButton Edit on chip, AlertDialog | Yes, Menu "Rename" | matched |
| Delete folder | Yes, IconButton Delete on chip, confirmation | Yes, Menu "Delete", confirmationDialog | matched |
| Folder management sheet | Yes, FavoriteFolderSheet | Yes, FolderManagementSheet | matched |
| Drag-and-drop reorder | Yes, ReorderableFavoritesGrid, drag handle | No | missing-ios |
| Voicing cards | Yes, VerticalChordDiagram | Yes, FavoriteCard with fret numbers | partial |
| Chord diagram in card | Yes, full VerticalChordDiagram | No, mini fret display (numbers only) | partial |
| Manage folders (folder icon) | Yes, IconButton FolderOpen | Yes, folder button | matched |
| Remove from favorites | Yes, IconButton Favorite (red) | Yes, heart.fill button | matched |
| Share voicing | Yes, onLongClick → onShareVoicing | No | missing-ios |
| Left-handed mode | Yes, leftHanded on diagram | N/A (no diagram) | partial |

---

## 8. SongbookTab / SongbookView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Search bar | Yes, OutlinedTextField | Yes, TextField | matched |
| Sort order | Yes, DropdownMenu (Last modified, Date added, Title, Artist) | Yes, Picker menu | matched |
| Label filter chips | Yes, FilterChip per label, clear chip | Yes, Button per label | matched |
| Sheet list | Yes, LazyColumn, SheetCard | Yes, List, song rows | matched |
| Sheet card: title, artist, labels | Yes, AssistChip per label | Yes, labels as capsules | matched |
| Import | Yes, SmallFAB, OpenDocument launcher | Yes, "Import ChordPro" in toolbar menu | matched |
| New sheet FAB | Yes, FloatingActionButton | Yes, "New Song" in toolbar | matched |
| Sheet viewer | Yes, SheetViewer | Yes, SongViewerView | partial |
| Transpose controls | Yes, +/- buttons, semitone label, reset | Yes, +/- in toolbar | matched |
| Capo hint when transposed | Yes, songbook_capo_hint | Yes, "Capo: fret N" if song.capo > 0 | partial |
| Strum pattern row | Yes, StrumPatternRow, change/remove | No in viewer | missing-ios |
| Labels display/edit in viewer | Yes, LabelDisplayRow, add label | No | missing-ios |
| Key detection display | Yes, KeyDetector.detectKey | No | missing-ios |
| Tappable chord names | Yes, withLink, onChordTapped | No (plain text) | missing-ios |
| Share menu (text, transposed, ChordPro export) | Yes, DropdownMenu | Yes, ShareLink | partial |
| Edit / Delete in viewer | Yes, IconButtons | Yes, pencil, trash | matched |
| Auto-scroll | Yes, Play/Pause FAB, speed chips (0.5x–3x) | No | missing-ios |
| Sheet editor | Yes, SheetEditor | Yes, SongEditorView | partial |
| Chord insertion helper chips | Yes, common chords (C, G, Am, etc.) | No | missing-ios |
| Edit/Preview toggle | Yes, FilterChips | No | missing-ios |
| Discard changes dialog | Yes | No (implied by sheet) | partial |
| Import: ChordPro vs plain text | Yes, ChordProParser.isChordProFile | Yes, importChordPro | matched |
| List swipe-to-delete | No | Yes, .onDelete | missing-android |

---

## 9. MelodyNotepadView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Input mode toggle (Tap / Record) | Yes, FilterChips | Yes, Picker segmented | matched |
| Tap: 12-note palette | Yes, horizontal scroll, CircleShape | Yes, LazyVGrid 6 columns | matched |
| Tap: Octave control | Yes, +/- IconButtons, 3–6 range | Yes, Stepper 3...6 | matched |
| Tap: Add rest button | Yes, "Rest" in palette | Yes, "Rest" button | matched |
| Duration selector | Yes, FilterChips (Whole, Half, Quarter, etc.) | Yes, duration buttons | matched |
| Record: Start/Stop | Yes, OutlinedButton | Yes, Record/Stop button | matched |
| Record: Detected note display | Yes, with stabilization progress | Yes, "Detected: N" | partial |
| Record: Stabilization progress bar | Yes, LinearProgressIndicator | No | missing-ios |
| Record: Last added feedback | Yes, AnimatedVisibility "Added X" | No | missing-ios |
| Note sequence display | Yes, NoteBlock horizontal scroll | Yes, List with noteRow | partial |
| Note selection + delete | Yes, selectNote, deleteSelectedNote | Yes, onDelete | matched |
| BPM slider | Yes, 40..220 | Yes, 40...200 | partial |
| Play / Stop playback | Yes | Yes | matched |
| Clear all | Yes | Yes (New button) | matched |
| Save dialog | Yes, SaveMelodyDialog | Yes, alert | matched |
| Load dialog/sheet | Yes, LoadMelodyDialog | Yes, loadSheet | matched |
| Rename dialog | Yes | No | missing-ios |
| Delete melody (from load) | Yes, IconButton in MelodyListItem | Yes, .onDelete | matched |
| Discard changes dialog | Yes | No | missing-ios |
| Menu (Save As, Load, Rename, New, Delete) | Yes, DropdownMenu | No, separate buttons | partial |
| Title bar with loaded name | Yes | Yes, persistenceControls | matched |
| Accessibility: note block semantics | Yes, contentDescription, role, stateDescription | Yes, accessibilityCombined | matched |
| Accessibility: duration chips | Yes, semantics | Yes, accessibilityAddTraits | matched |

---

## 10. StrumPatternsTab / StrumPatternsView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Strumming / Fingerpicking toggle | Yes, FilterChips | Yes, Picker segmented | matched |
| My Patterns section | Yes, custom patterns first | Yes | matched |
| Presets section | Yes | Yes | matched |
| Difficulty filter | No | Yes, All/Beginner/Intermediate/Advanced | missing-android |
| Pattern card: name, time sig, difficulty | Yes | Yes | matched |
| Beat/step display | Yes, BeatDisplay, BeatArrow | Yes, HStack of symbols | matched |
| Notation string | Yes | Yes | matched |
| Counting | Yes | Yes | matched |
| Description | Yes | Yes (expandable on iOS) | partial |
| Genres | Yes, FlowRow chips | Yes, ScrollView | matched |
| BPM slider per card | Yes, 40..220 | No, BPM range text only | missing-ios |
| Play/Stop button | Yes, IconButton | No (no play on card) | missing-ios |
| Duplicate | Yes, ContentCopy IconButton | No | missing-ios |
| Edit (custom patterns) | Yes, Edit IconButton | No (delete only) | missing-ios |
| Delete (custom patterns) | Yes, Delete IconButton | Yes, swipe/context menu | matched |
| Create custom FAB | Yes | Yes, toolbar + | matched |
| Create strum sheet | Yes, CreateStrumPatternSheet (ModalBottomSheet) | Yes, CreateStrumPatternSheet | partial |
| Create strum: time signatures | Yes, 11 options (2/2–12/8) | Yes, 3 options (4/4, 3/4, 6/8) | partial |
| Create strum: tap beat to cycle direction | Yes, DOWN/UP/CHUCK/MISS/PAUSE | Yes, Picker per beat | partial |
| Create strum: accent toggles | Yes, tap to toggle emphasis | Yes, Toggle per beat | matched |
| Create strum: add/remove beats | Yes, +/- buttons | Yes, Add Beat, onDelete | matched |
| Create fingerpick sheet | Yes, CreateFingerpickingPatternSheet | Yes, CreateFingerpickPatternSheet | partial |
| Create fingerpick: finger + string selectors | Yes, FilterChips | Yes, Picker per step | matched |
| Expandable card (tap to show details) | No | Yes, expandedIndex | missing-android |
| Heading semantics | Yes | Yes | matched |

---

## 11. ProgressionsTab / ProgressionsView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Key selector | Yes, 12 FilterChips | Yes, rootSelector | matched |
| Scale type (Major/Minor) | Yes | Yes | matched |
| New progression button | Yes, OutlinedButton | Yes, toolbar + | matched |
| Custom progressions section | Yes | Yes | matched |
| Preset progressions | Yes | Yes | matched |
| Progression card: name, degrees, description | Yes | Yes | matched |
| Chord chips (resolved names) | Yes, SuggestionChip with harmonic function label | Yes, numeral + resolved | partial |
| Voice Leading | Yes, TextButton | Yes, NavigationLink | matched |
| Capo | Yes, TextButton | Yes, NavigationLink | matched |
| Share | Yes, IconButton | Yes, ShareLink on custom; preset has ShareLink | matched |
| Play | Yes, IconButton | No | missing-ios |
| Practice | Yes, TextButton | Yes, NavigationLink | matched |
| Duplicate (custom) | Yes, IconButton | No | missing-ios |
| Edit (custom) | Yes, IconButton | No | missing-ios |
| Delete (custom) | Yes, IconButton | Yes, trash button | matched |
| Playback bar (when playing) | Yes, ProgressionPlaybackBar | N/A | missing-ios |
| Capo calculator view | Yes, CapoCalculatorProgressionView | Yes, NavigationLink to CapoCalculatorView | matched |
| Voice leading view | Yes, VoiceLeadingView | Yes, NavigationLink | matched |
| Practice view | Yes, ProgressionPracticeView | Yes, NavigationLink | matched |
| Create progression sheet | Yes, CreateProgressionSheet | Yes, CreateProgressionSheet | partial |
| Harmonic function labels (Tonic, Subdominant, Dominant) | Yes, colored labels on chips | No | missing-ios |

---

## 12. VerticalChordDiagram / ChordDiagramView

| Feature/Element | Android | iOS | Status |
|----------------|---------|-----|--------|
| Vertical layout (strings left-to-right) | Yes | Yes | matched |
| Nut, fret lines, string lines | Yes | Yes | matched |
| Open string circles | Yes | Yes | matched |
| Muted "x" | Yes | Yes | matched |
| Fretted dots | Yes | Yes | matched |
| Position label (e.g. "3fr") | Yes, when not at nut | Yes | matched |
| Chord name | Yes, optional top-right | Yes, optional above | matched |
| Left-handed mode | Yes, mirrored strings | No | missing-ios |
| Bass string highlight | Yes, bassStringIndex, bassDotColor | No | missing-ios |
| Common tone highlight | Yes, commonToneIndices | No | missing-ios |
| Capo bar | Yes, capoFret | No | missing-ios |
| Inversion label | Yes, below diagram | No | missing-ios |
| Favorite heart icon | Yes, onFavoriteClick | No | missing-ios |
| Share (long-press) | Yes, onLongClick | No (handled by parent context menu) | partial |
| Clickable (onClick) | Yes, combinedClickable | Yes (parent Button) | matched |
| clearAndSetSemantics on canvas | Yes, then overridden by card semantics | accessibilityHidden on canvas | partial |
| Full chord description | Yes, diagram_chord_cd + per-string | Yes, fretDescription | matched |

---

## Summary: High-Impact Gaps (iOS missing)

1. **FretboardView**: Left-handed mode, scale overlay, capo visualization
2. **ChordResultView**: Suggested chords, Did You Know card, Show in library (tap chord name)
3. **TunerTab**: String reference buttons, needle meter (vs linear), all-strings-tuned celebration, precision mode, A4 reference
4. **ChordLibraryTab**: Inversion filter, Compare inversions, Transpose controls, collapsible filters, Play voicing
5. **FavoritesTab**: Drag-and-drop reorder, full chord diagram (iOS uses mini fret numbers), Share
6. **SongbookTab**: Tappable chords, auto-scroll, strum pattern in viewer, labels in viewer, key detection, chord insertion chips
7. **MelodyNotepadView**: Stabilization progress, last-added feedback, Rename dialog
8. **StrumPatternsTab**: BPM slider per card, Play/Stop on card, Duplicate, Edit
9. **ProgressionsTab**: Play button, Duplicate, Edit, Playback bar, harmonic function labels
10. **VerticalChordDiagram**: Left-handed, bass highlight, common tones, capo bar, inversion label, favorite icon

---

## Summary: High-Impact Gaps (Android missing)

1. **TunerView**: Octave display, frequency display (Android has string buttons instead)
2. **SongbookView**: Swipe-to-delete on list

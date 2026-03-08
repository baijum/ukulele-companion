---
name: platform-parity-audit
description: Audit iOS port parity against the Android app. Compares ViewModels, Views, Settings, and Navigation at the code level, verifies features from docs/videos/ TOML project files, and optionally captures side-by-side screenshots. Use when the user asks to compare iOS vs Android, check iOS parity, find missing iOS features, audit the port, or mentions feature gaps.
---

# Platform Parity Audit

Systematically compare the iOS port against the Android reference implementation to identify missing features, partial implementations, and behavioral differences. The audit has three layers that can be run independently.

## Layer 1: Code-Level Structural Diff (no devices needed)

Compare source files between platforms to find functional gaps. This is the fastest layer and should always be run first.

### Step 1: Inventory Android screens and ViewModels

List all Android UI files and ViewModels:

```bash
ls app/src/main/java/com/baijum/ukufretboard/ui/*.kt
ls app/src/main/java/com/baijum/ukufretboard/viewmodel/*.kt
```

### Step 2: Inventory iOS screens and ViewModels

List all iOS View and ViewModel files:

```bash
ls iosApp/UkuleleCompanion/Views/*.swift
ls iosApp/UkuleleCompanion/ViewModels/*.swift
```

### Step 3: Map Android screens to iOS counterparts

Build a feature matrix by matching each Android screen to its iOS equivalent. Use the mapping table below as a starting reference:

| Category | Android Screen (ui/) | iOS Screen (Views/) |
|----------|---------------------|---------------------|
| Play | FretboardScreen.kt (Explorer) | ExplorerView.swift |
| Play | TunerTab.kt | TunerView.swift |
| Play | PitchMonitorTab.kt | PitchMonitorView.swift |
| Play | MetronomeTab.kt | MetronomeView.swift |
| Play | ChordLibraryTab.kt | ChordLibraryView.swift |
| Play | FavoritesTab.kt | FavoritesView.swift |
| Create | SongbookTab.kt | SongbookView.swift |
| Create | MelodyNotepadView.kt | MelodyNotepadView.swift |
| Create | StrumPatternsTab.kt | StrumPatternsView.swift |
| Create | ProgressionsTab.kt | ProgressionsView.swift |
| Learn | TheoryLessonsView.kt | TheoryLessonsView.swift |
| Learn | TheoryQuizView.kt | TheoryQuizView.swift |
| Learn | IntervalTrainerView.kt | IntervalTrainerView.swift |
| Learn | NoteQuizView.kt | NoteQuizView.swift |
| Learn | ChordEarTrainingView.kt | ChordEarTrainingView.swift |
| Learn | ScalePracticeView.kt | ScalePracticeView.swift |
| Learn | LearningProgressView.kt | LearningProgressView.swift |
| Learn | DailyChallengeView.kt | DailyChallengeView.swift |
| Learn | PracticeRoutineView.kt | PracticeRoutineView.swift |
| Learn | ChordTransitionView.kt | ChordTransitionsView.swift |
| Learn | PlayAlongView.kt | PlayAlongView.swift |
| Learn | AchievementsView.kt | AchievementsView.swift |
| Reference | CapoGuideView.kt | CapoGuideView.swift |
| Reference | CircleOfFifthsView.kt | CircleOfFifthsView.swift |
| Reference | ChordSubstitutionsView.kt | ChordSubstitutionsView.swift |
| Reference | ScaleChordView.kt | ScaleChordsView.swift |
| Reference | FretboardNoteMapView.kt | FretboardNoteMapView.swift |
| Reference | GlossaryView.kt | GlossaryView.swift |
| Other | SettingsSheet.kt | SettingsView.swift |
| Other | HelpView.kt | HelpView.swift |
| Other | OnboardingScreen.kt | OnboardingView.swift |

### Step 4: Compare ViewModel properties

For each ViewModel pair, compare the Android `StateFlow`/`MutableState` properties against iOS `@Published` properties. Look for:

- Properties present in Android but missing in iOS (missing features)
- Functions in Android ViewModel with no iOS equivalent
- Different default values or enum options

Example comparison approach:

```bash
# Extract Android ViewModel public API
rg "val |var |fun " app/src/main/java/com/baijum/ukufretboard/viewmodel/FretboardViewModel.kt | head -40

# Extract iOS ViewModel public API
rg "@Published|func " iosApp/UkuleleCompanion/ViewModels/FretboardViewModel.swift | head -40
```

### Step 5: Compare Settings

Compare all settings keys between platforms:

```bash
# Android settings
rg "putBoolean\|putString\|putInt\|putFloat\|getBoolean\|getString\|getInt\|getFloat" app/src/main/java/com/baijum/ukufretboard/data/

# iOS settings
rg "UserDefaults\|@AppStorage\|defaults\." iosApp/UkuleleCompanion/ViewModels/SettingsViewModel.swift
```

### Step 6: Compare sub-view features

For each matched screen pair, read both files and compare:

- Buttons and interactive elements
- Dialogs, sheets, and modals
- Toggle/switch options
- Picker/selector options
- Accessibility labels and hints

### Step 7: Generate the feature matrix

Create a markdown table in `docs/ios-parity-report.md` with columns:

| Feature | Android File | iOS File | Status | Notes |
|---------|-------------|----------|--------|-------|

Status values:
- **matched** — Feature exists and is functionally equivalent
- **partial** — Feature exists but is missing sub-features or has different behavior
- **missing** — Feature exists in Android but has no iOS counterpart
- **ios-only** — Feature exists in iOS but not Android

## Layer 2: TOML-Based Feature Verification

The `docs/videos/` directory contains TOML project files that document detailed feature scenarios. Use these as checklists.

### Step 1: List all TOML projects

```bash
ls docs/videos/*/android.toml
```

### Step 2: Parse each project file

For each `android.toml`, read the `[[scene]]` entries. Each scene has:
- `name` — what the scene demonstrates
- `narration` — description of the feature behavior
- `recording_notes` — step-by-step interaction instructions

### Step 3: Verify each scene in iOS code

For each scene, search the iOS codebase for the described feature:

```bash
# Example: verify chord detection exists in iOS
rg "detect\|chord.*result\|ChordResult" iosApp/UkuleleCompanion/Views/ExplorerView.swift
```

Mark each scene as:
- **verified** — the feature described in the scene exists in iOS
- **partial** — some aspects of the scene's feature are missing
- **missing** — the feature is not implemented in iOS

### Step 4: Supplement with Learn and Reference

The TOML files cover Play and Create features only. For Learn and Reference screens, manually verify by reading the iOS view files and comparing against the Android equivalents.

## Layer 3: Visual Screenshot Comparison (requires running devices)

Capture side-by-side screenshots to catch visual/layout differences.

### Prerequisites

- Android emulator running with app installed (see [android-screenshot-capture](~/.cursor/skills/android-screenshot-capture/SKILL.md))
- iOS simulator running with app installed (see [ios-simulator-run](~/.cursor/skills/ios-simulator-run/SKILL.md))

### Step 1: Create output directory

```bash
mkdir -p docs/parity-screenshots/{android,ios}
```

### Step 2: Capture Android screenshots

```bash
ADB=~/Library/Android/sdk/platform-tools/adb

# Launch app
$ADB shell am start -n com.baijum.ukufretboard/.MainActivity
sleep 3

# For each screen: navigate, wait, capture
# Example: Explorer
$ADB exec-out screencap -p > docs/parity-screenshots/android/explorer.png

# Example: Chord Library (via drawer)
$ADB shell input tap 80 200  # hamburger menu
sleep 1
$ADB shell uiautomator dump /sdcard/ui.xml 2>/dev/null
$ADB shell cat /sdcard/ui.xml | tr '>' '\n' | grep 'text="Chords"'
# Tap found coordinates
$ADB shell input tap <x> <y>
sleep 2
$ADB exec-out screencap -p > docs/parity-screenshots/android/chord-library.png
```

### Step 3: Capture iOS screenshots

```bash
# Get booted simulator UUID
SIM_UUID=$(xcrun simctl list devices booted -j | python3 -c "import sys,json; devs=json.load(sys.stdin)['devices']; print([d['udid'] for ds in devs.values() for d in ds if d['state']=='Booted'][0])")

# Launch app
xcrun simctl launch $SIM_UUID com.baijum.ukufretboard.ios
sleep 3

# Capture each screen (navigate via UI or deep links if available)
xcrun simctl io $SIM_UUID screenshot docs/parity-screenshots/ios/explorer.png
```

iOS navigation automation is limited compared to ADB. For screens requiring navigation:
- Use `xcrun simctl` status bar overrides for consistent appearance
- Navigate screens manually in the simulator and capture programmatically
- Consider adding SwiftUI test hooks for automated navigation in the future

### Step 4: Compare screenshots

Review the paired screenshots (`android/<name>.png` vs `ios/<name>.png`) visually or present them side-by-side. Look for:

- Missing buttons, sections, or controls
- Different layout or ordering of elements
- Missing icons or visual indicators
- Accessibility overlay differences (test with VoiceOver/TalkBack enabled)

### Naming convention

Use consistent names matching the feature:
- `explorer.png`, `tuner.png`, `metronome.png`, `chord-library.png`
- `settings.png`, `onboarding.png`
- `{feature}-{subscreen}.png` for sub-screens (e.g., `progressions-practice.png`)

## Output: Parity Report

Generate `docs/ios-parity-report.md` containing:

1. **Summary** — overall parity percentage and key gaps
2. **Feature matrix** — full table from Layer 1
3. **TOML verification results** — scene-by-scene status from Layer 2
4. **Screenshot pairs** — references to captured images from Layer 3 (if run)
5. **Priority list** — missing/partial features ranked by importance

## Quick Reference

| Task | Command |
|------|---------|
| List Android UI files | `ls app/src/main/java/com/baijum/ukufretboard/ui/*.kt` |
| List iOS View files | `ls iosApp/UkuleleCompanion/Views/*.swift` |
| List Android ViewModels | `ls app/src/main/java/com/baijum/ukufretboard/viewmodel/*.kt` |
| List iOS ViewModels | `ls iosApp/UkuleleCompanion/ViewModels/*.swift` |
| Compare ViewModel API | `rg "val \|var \|fun " <android.kt>` vs `rg "@Published\|func " <ios.swift>` |
| List TOML projects | `ls docs/videos/*/android.toml` |
| Android screenshot | `adb exec-out screencap -p > file.png` |
| iOS screenshot | `xcrun simctl io <UUID> screenshot file.png` |

## Troubleshooting

- **Files don't match names in mapping table**: File naming conventions differ between platforms. Use the mapping table in Step 3 as the authoritative reference and update it if files are renamed.
- **ViewModel lives in unexpected location**: iOS `TunerViewModel` is defined inline in `Views/TunerView.swift`, not in `ViewModels/`. Check both directories.
- **Shared KMP code not considered**: Features implemented in `shared/src/commonMain/` are available to both platforms. When a feature appears "missing" from iOS, check if the shared module provides it.
- **iOS navigation harder to automate**: Unlike ADB's `uiautomator` and `input tap`, iOS simulator automation is limited to `xcrun simctl` which has no UI element discovery. For full automation, use XCUITest or navigate manually.

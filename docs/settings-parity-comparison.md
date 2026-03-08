# Settings, Help, Onboarding & Full Screen Parity Comparison

**Android vs iOS — Ukulele Companion**

---

## 1. Settings Options — Full Parity Table

| Section | Option | Android | iOS | Parity | Notes |
|---------|--------|---------|-----|--------|-------|
| **Sound** | Sound enabled (master toggle) | ✓ | ✓ | ✅ | Both platforms |
| | Volume (slider) | ✓ | ✓ | ✅ | Both platforms |
| | Note duration | ✓ | ✓ | ✅ | Android: slider 300–1200ms; iOS: stepper 300–1200ms step 50 |
| | Strum delay | ✓ | ✓ | ✅ | Android: slider 0–150ms; iOS: stepper 0–150ms step 10 |
| | Strum direction (down/up) | ✓ | ✓ | ✅ | Android: FilterChips; iOS: Toggle "Strum Down" |
| | Play on tap | ✓ | ✓ | ✅ | Both platforms |
| | **Noise gate filtering** | ✓ | ❌ | ⚠️ **iOS missing** | Android: slider 0–100% for Pitch Monitor/Melody recording; iOS has no equivalent |
| **Display** | Theme | ✓ (4) | ✓ (3) | ⚠️ **Partial** | Android: Light, Dark, System, **High Contrast**; iOS: Light, Dark, System only |
| | Show explorer tips | ✓ | ✓ | ✅ | Android: `showExplorerTips`; iOS: `showTips` |
| | Show Learn section/tab | ✓ | ✓ | ✅ | Both platforms |
| | Show Reference section/tab | ✓ | ✓ | ✅ | Both platforms |
| **Language** | App language picker | ✓ | ❌ | ⚠️ **iOS missing** | Android: 16 locales via `AppCompatDelegate.setApplicationLocales`; iOS uses system locale only |
| **Tuning** | Tuning preset | ✓ | ✓ | ✅ | Both: 8 tunings; Android uses shared `UkuleleTuning` enum; iOS uses hardcoded strings |
| **Tuner** | Spoken feedback | ✓ | ❌ | ⚠️ **iOS missing** | Android: TTS for blind/VI users; iOS has localized strings but no UI/setting |
| | Precision mode | ✓ | ✓ | ✅ | Both platforms |
| | Auto advance | ✓ | ✓ | ✅ | Both platforms |
| | **Auto start** | ✓ | ❌ | ⚠️ **iOS missing** | Android: tuner starts listening on tab open; iOS has localized strings but no UI/setting |
| | A4 reference (Hz) | ✓ | ✓ | ✅ | Both: slider 415–465 Hz |
| **Fretboard** | Left-handed | ✓ | ✓ | ✅ | Both platforms |
| | Show note names | ✓ | ✓ | ✅ | Both platforms |
| | Allow muted strings | ✓ | ✓ | ✅ | Both platforms |
| | Last fret (12–22) | ✓ | ✓ | ✅ | Android: slider; iOS: stepper |
| **Backup & Restore** | Export backup | ✓ | ✓ | ✅ | Both platforms |
| | Restore from backup | ✓ | ✓ | ✅ | Both platforms |
| **About** | App name & version | ✓ | ✓ | ✅ | Both platforms |
| | Help link | — | ✓ | — | iOS: NavigationLink to HelpView; Android: Help in nav drawer, not in Settings |
| | Website link | ✓ | ❌ | ⚠️ **iOS missing** | Android: baijum.github.io/ukulele-companion |
| | Free book link | ✓ | ❌ | ⚠️ **iOS missing** | Android: archive.org/details/ukulele-book |
| | Video guide link | ✓ | ❌ | ⚠️ **iOS missing** | Android: YouTube playlist |
| | Credits (audio samples, license) | ✓ | ❌ | ⚠️ **iOS missing** | Android: credits section |
| | Tagline | ✓ | ❌ | ⚠️ **iOS missing** | Android: app tagline |

---

## 2. Settings ViewModel Comparison

| Aspect | Android | iOS |
|--------|---------|-----|
| **Architecture** | `AndroidViewModel` with `StateFlow<AppSettings>` | `ObservableObject` with `@Published` properties |
| **Data source** | Shared `AppSettings` data class (KMP) | Local Swift structs, UserDefaults keys |
| **Persistence** | SharedPreferences | UserDefaults (suite: `app_settings`) |
| **Key mismatch** | Uses `KEY_*` constants | Different keys: `volume` vs `sound_volume`, `selected_tuning` vs `tuning`, etc. |
| **Scale practice** | Persisted in SettingsViewModel | Not in iOS SettingsViewModel |
| **Pitch monitor** | `PitchMonitorSettings` (placeholder) | Not present |
| **explorerTipsDismissed** | Separate from showExplorerTips | iOS uses single `showTips` |
| **replaceAll / export** | `replaceAll()`, `exportSettings()` for sync | `importSettings()`, `exportSettings()` for backup |

---

## 3. HelpView Comparison

| Aspect | Android | iOS |
|--------|---------|-----|
| **Structure** | Same 5 sections: Play, Create, Learn, Reference, Other | Same 5 sections |
| **Content source** | `stringResource(R.string.*)` — localized | Hardcoded English strings in Swift |
| **Expandable entries** | `AnimatedVisibility` + `clickable` | `Button` + `expandedEntry` state |
| **Play section** | Explorer, Tuner, Pitch Monitor, Chords, Favorites | Same 5 entries |
| **Create section** | Songs, Melody Notepad, Patterns, Progressions | Songbook, Melody Notepad, Strum Patterns, Progressions |
| **Learn section** | 12 entries (Theory, Quiz, Interval, Note Quiz, Chord Ear, Scale Practice, Progress, Daily Challenge, Practice Routine, Chord Transitions, Play Along, Achievements) | Same 12 entries (order differs slightly) |
| **Reference section** | Capo Guide, Circle of Fifths, Chord Substitutions, **Chords in Scale**, Fretboard Note Map, Glossary | Capo Guide, Circle of Fifths, Chord Substitutions, **Scale Chords**, Fretboard Note Map, Glossary |
| **Other section** | Settings, Sharing, **Full Screen Mode** | Settings, Sharing, **Backup & Restore** |
| **Full Screen entry** | ✓ (Android has full-screen fretboard) | ❌ (iOS has no full-screen mode) |
| **Backup & Restore entry** | ❌ | ✓ |

---

## 4. OnboardingScreen / OnboardingView Comparison

| Aspect | Android | iOS |
|--------|---------|-----|
| **Page count** | 6 pages | 6 pages |
| **Page 1: Welcome** | ✓ Icon, title, subtitle | ✓ Same structure |
| **Page 2: Features** | Tuner, Chords, Fretboard, Songbook, Learn | Tuner, Chord library, Fretboard, Songbook, Learn |
| **Page 3: Accessibility** | TalkBack, Spoken feedback, High contrast | VoiceOver, Spoken tuner, High contrast |
| **Page 4: Privacy** | Offline, No ads, Your data | Same |
| **Page 5: Navigation** | Play, Create, Learn, Reference | Same (tab bar) |
| **Page 6: Quick Setup** | Tuning chips, Left-handed toggle, Theme chips | Tuning grid, Left-handed toggle, Theme picker |
| **Tuning source** | Shared `UkuleleTuning.entries` | Hardcoded `tuningOptions` array |
| **Theme options** | Light, Dark, System (from ThemeMode) | Light, Dark, System |
| **Skip / Back / Next** | ✓ | ✓ |
| **Page indicator** | Dots with semantics | Dots with accessibility label |
| **Pager** | `HorizontalPager` | `TabView` with `.page` style |
| **Settings integration** | Uses `SettingsViewModel` (updateTuning, updateFretboard, updateDisplay) | Uses `SettingsViewModel` (selectedTuning, leftHanded, themeMode) |
| **Localization** | `stringResource(R.string.*)` | Hardcoded English |

---

## 5. FullScreenFretboard — iOS Equivalent

| Aspect | Android | iOS |
|--------|---------|-----|
| **Full-screen fretboard** | ✓ `FullScreenFretboard.kt` | ❌ **No equivalent** |
| **Behavior** | Forces landscape, immersive mode, auto-hide overlay, chord detection, Play/Reset/Exit | — |
| **Entry point** | Expand icon in Explorer tab | — |
| **Help reference** | Android Help has "Full Screen Mode" entry | iOS Help has "Backup & Restore" instead; no "Full Screen" (iOS doesn't have this feature) |

**Conclusion:** iOS does **not** have a full-screen fretboard view. The Android `FullScreenFretboard.kt` provides a landscape-only, immersive mode fretboard with overlay controls. iOS has no equivalent.

---

## 6. Summary of Gaps

### iOS missing (relative to Android)

1. **Settings**
   - Noise gate filtering (slider)
   - High Contrast theme
   - Language picker
   - Tuner: Spoken feedback
   - Tuner: Auto start
   - About: Website, free book, video guide, credits, tagline

2. **Settings persistence**
   - Key naming differs from Android (e.g. `volume` vs `sound_volume`); may affect backup/restore compatibility

3. **Help**
   - Full Screen Mode entry (not applicable since iOS has no full-screen)
   - All content hardcoded in English (not localized)

4. **Full-screen fretboard**
   - No equivalent feature

### Android missing (relative to iOS)

1. **Help**
   - Backup & Restore as a dedicated help entry (Android has Backup in Settings but not in Help Other section)

---

## 7. Recommendations

1. **iOS Settings:** Add Tuner spoken feedback, auto start, noise gate filtering, High Contrast theme, and Language section if feasible.
2. **iOS About:** Add website, free book, video guide, credits, and tagline for consistency.
3. **Backup/Restore:** Align UserDefaults keys with Android SharedPreferences keys for cross-platform backup compatibility.
4. **iOS Full Screen:** Consider implementing a landscape full-screen fretboard for parity with Android.
5. **Help:** Consider adding Backup & Restore to Android Help Other section, and localize iOS Help content.

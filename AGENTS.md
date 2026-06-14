# AGENTS.md -- AI Agent Instructions for Ukulele Companion

Mandatory instructions for AI coding agents (Cursor, Copilot, Codex, etc.) working on this codebase.

**See also:** [CODEBASE_AREAS_SUBAREAS.md](CODEBASE_AREAS_SUBAREAS.md) — exhaustive table of feature areas (Play/Create/Practice/Reference + all NavSections), packages, UI/Views, domain logic, and cross-cutting code.

Detailed coding rules live in `.cursor/rules/*.mdc` — **the canonical source**. They auto-attach in Cursor; Claude Code picks them up via directory-scoped `CLAUDE.md` pointer files (e.g. `shared/src/commonMain/`, `app/src/main/.../ui/`, `iosApp/UkuleleCompanion/`); other agents should open the relevant rule from the Coding Rules Reference table below. This document provides project-level context and constraints.

## Project Overview

Ukulele Companion is a **free, fully offline** multiplatform app (Android + iOS) for learning and playing ukulele. A core user base includes **blind and visually impaired musicians** who rely on TalkBack (Android) and VoiceOver (iOS). Every code change must preserve accessibility. Breaking accessibility is treated as seriously as breaking functionality.

**Hard constraints -- never violate these:**
- No network dependencies -- the app must remain fully offline
- No analytics, tracking, or telemetry
- No ads or monetization code without prior discussion
- No third-party SDKs without prior discussion
- Never commit `keystore.properties`, API keys, or secrets

## Tech Stack

| Platform | Key Details |
|----------|-------------|
| **Shared (KMP)** | `:shared` module — pure Kotlin business logic in `domain/`, data models in `data/`, `expect/actual` in `platform/` |
| **Android** | Kotlin 2.4.0, Jetpack Compose (BOM 2026.05.01), Material 3, Single Activity, MVVM, StateFlow |
| **iOS** | Swift 6, SwiftUI, MVVM, `@StateObject`/`@Published`, iOS 16.0+, static `shared.framework` via Gradle |
| **Audio ML** | ONNX Runtime 1.26.0 on both platforms (Android AAR, iOS xcframework via C API) |
| **Build** | Gradle 9.5.1, AGP 9.2.1, Kotlin DSL, version catalog (`libs.versions.toml`) |

## Package Structure

### Shared module (`shared/src/commonMain/kotlin/com/baijum/ukufretboard/`)

| Package | Contents |
|---------|----------|
| `domain/` | Pure Kotlin business logic — chord detection, transposition, pitch detection, scales, music theory. **No platform imports.** |
| `data/` | Data models, enums (`UkuleleTuning`, `Notes`), configuration types |
| `platform/` | `expect/actual` declarations (`generateUuid`, `currentTimeMillis`, `currentYear`, `currentDayOfYear`) |

### Android app (`app/src/main/java/com/baijum/ukufretboard/`)

| Package | Contents |
|---------|----------|
| `audio/` | `ToneGenerator`, `MetronomeEngine`, `AudioCaptureEngine` (44.1kHz PCM) |
| `data/` | Repositories (SharedPreferences-backed), backup/restore manager |
| `domain/` | `NeuralPitchSupervisor`, `ChordImageSharer`, `AchievementChecker` |
| `ui/` | 76 Compose screens/components via `ModalNavigationDrawer` (no NavHost) |
| `viewmodel/` | 14 ViewModels exposing `StateFlow` |

### iOS app (`iosApp/UkuleleCompanion/`)

| Directory | Contents |
|-----------|----------|
| `Audio/` | `AudioCaptureEngine`, `TonePlayer`, `NeuralPitchSupervisor` (ONNX C API) |
| `Views/` | 50 SwiftUI views across Play, Create, Learn, Reference tabs |
| `ViewModels/` | 17 ViewModels using `@Published` |
| `Helpers/` | `AccessibilityHelper`, `BackupRestoreManager` |

**Full codebase inventory:** For an exhaustive table of all feature areas (Play/Create/Practice/Reference + every NavSection), shared domain logic, platform implementations, UI components, cross-cutting concerns (accessibility, neural, backup), tests, and build tooling, see [CODEBASE_AREAS_SUBAREAS.md](CODEBASE_AREAS_SUBAREAS.md).

## Key Patterns

### Accessibility patterns

- **Reduce motion:** Android uses `LocalReduceMotion` (from `ReduceMotion.kt`, provided in `MainActivity`). iOS uses `@Environment(\.accessibilityReduceMotion)`. All animations must check this and use `snap()` / `nil` animation when enabled.
- **Haptic feedback:** The tuner provides haptic feedback when a string enters the in-tune zone. Android: `HapticFeedbackType.LongPress` via `LocalHapticFeedback`. iOS: `UINotificationFeedbackGenerator.success`.
- **Chord diagram exploration:** Both platforms expose per-string details via screen reader custom actions (Android: `CustomAccessibilityAction`; iOS: `accessibilityCustomContent`).
- **NeedleMeter zones:** The meter announces zone names (In tune / Close / Flat / Sharp / No signal) via `stateDescription` (Android) and `accessibilityValue` (iOS).
- **Hints:** Long-press actions (e.g. share chord diagram) must declare `onLongClickLabel` (Android) or `accessibilityHint` (iOS).

### Audio pipeline patterns

- **Frame-dropping:** Both `TunerViewModel` and `PitchMonitorViewModel` guard `processBuffer` with an `isProcessing` flag (Android: `AtomicBoolean`; iOS: `Bool` on `@MainActor`) to drop frames when processing falls behind under thermal throttling.
- **Async ONNX loading:** The neural pitch supervisor loads on a background thread (`Dispatchers.IO` on Android, `nonisolated static` + `Task` on iOS). `NeuralRuntimeStatus` has three states: `LOADING`, `ACTIVE`, `FALLBACK`.
- **Buffer reuse:** `PitchDetector` and `AudioResampler` in the shared KMP module cache work buffers across frames to avoid per-frame GC pressure. Follow the `ensureFftBuffers` / `ensureDiffBuffers` pattern when adding new array allocations in the audio path.

## Coding Rules Reference

The canonical rule bodies live in these `.cursor/rules/*.mdc` files (auto-attach in Cursor; opened on demand by other agents):

| Rule | Applies to | What it covers |
|------|-----------|----------------|
| `compose-accessibility.mdc` | `ui/**/*.kt` | TalkBack: icons, headings, Canvas, live regions, focus, modals |
| `swiftui-accessibility.mdc` | `iosApp/**/*.swift` | VoiceOver: labels, hints, traits, values |
| `shared-module.mdc` | `shared/src/commonMain/**/*.kt` | No platform imports, expect/actual, KMP conventions |
| `android-viewmodel.mdc` | `viewmodel/**/*.kt` | StateFlow, repository abstraction, coroutines |
| `ios-viewmodel.mdc` | `ViewModels/**/*.swift` | @Published, @StateObject vs @ObservedObject, KMP naming |
| `compose-ui.mdc` | `ui/**/*.kt` | Material 3, recomposition, Compose-only UI |
| `compose-coroutines.mdc` | `ui/**/*.kt` | Compose scroll/coroutine patterns, programmatic vs user scroll |
| `testing-conventions.mdc` | `test/**/*.kt`, `androidTest/**/*.kt` | Kotest property tests, JUnit 4, what to test when |
| `edge_to_edge.mdc` | `MainActivity.kt`, `Theme.kt`, `libs.versions.toml` | Play Store edge-to-edge warnings (deferred) |

## Skills Reference

Skills in `.cursor/skills/` provide step-by-step workflows for common tasks:

| Skill | When to use |
|-------|-------------|
| `release` | **Primary release workflow** — bump both Android + iOS versions in one commit, tag, build both platforms, upload Android to Play Store, open iOS archive for App Store |
| `add-translations` | Adding new user-facing strings to all 16 supported locales (Android `strings.xml` + iOS `Localizable.xcstrings`) |
| `android-release` | Android-only hotfix release (for normal releases, use `release` instead) |
| `ios-release` | iOS-only hotfix release (for normal releases, use `release` instead) |
| `github-release` | Create a tag without building (for normal releases, use `release` instead) |
| `android-bug-reproduce` | Reproduce and debug Android bugs on an emulator using ADB |
| `platform-parity-audit` | Audit iOS port parity against the Android app |
| `record-clips` | Record scene video clips for a TOML video project |
| `assemble-video` | Assemble a narrated video from a TOML project file |

## Build and CI

```bash
# Android
./gradlew assembleDebug                        # Build debug APK
./gradlew lintDebug                            # Run Android Lint
./gradlew detekt                               # Run Detekt static analysis (baseline ratchet)
./gradlew testDebugUnitTest                    # Run unit tests
./gradlew connectedAndroidTest                 # Run instrumented tests

# Quality gates (run before submitting)
scripts/preflight.sh                           # ktlint ratchet + shared tests + unit tests + lint
scripts/ktlint.sh                              # ktlint check only (-F to auto-format)

# Git hooks (install once)
./gradlew installGitHooks                      # copies pre-commit hook to .git/hooks/
# Or manually: cp scripts/pre-commit .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit
# Pre-commit runs: ktlint on staged .kt files + gitleaks (if installed)

# iOS (requires Xcode)
xcodebuild -project iosApp/UkuleleCompanion.xcodeproj \
  -scheme UkuleleCompanion \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  build
```

**CI** (GitHub Actions on push/PR to `main`):
- **Android** (`android.yml`): JDK 17, lint (debug + release), unit tests, shared module tests, debug APK, release APK + R8 mapping, APK size report, instrumented tests (API 33/35)
- **iOS** (`ios.yml`): JDK 17 + Xcode 16.4, shared KMP framework (debug + release), iOS build (debug + release), unit tests
- **ktlint** (`ktlint.yml`): baseline-aware ratchet on Kotlin changes — fails only on violations beyond `ktlint-baseline.xml`

**Local guardrails (Claude Code):** `.claude/settings.json` wires PreToolUse hooks (`scripts/hooks/`) that block network/analytics/secrets and platform imports in `commonMain`, plus a PostToolUse ktlint-style nudge. Slash commands: `/extract-to-shared`, `/preflight`, `/add-string`. Subagent: `accessibility-reviewer`. These activate at session start; if a hook blocks an edit, it explains why.

**Commit format:**
```
Type: short description

Optional body explaining what and why
```
Types: `Add`, `Fix`, `Update`, `Refactor`, `Test`, `Docs`, `Chore`

## Pre-Submission Checklist

### Both platforms
- [ ] `scripts/preflight.sh` passes (or run the individual gates below)
- [ ] Shared module builds (`./gradlew :shared:build`)
- [ ] Both High-G and Low-G tuning work (if chord/note logic changed)
- [ ] Left-handed mode not broken (if fretboard UI changed)
- [ ] New animations respect reduce motion (see Key Patterns above)
- [ ] No per-frame allocations added in audio hot path

### Android
- [ ] Builds without errors (`./gradlew assembleDebug`)
- [ ] Changes verified on device or emulator
- [ ] Accessibility rules followed (see `compose-accessibility.mdc`)
- [ ] TalkBack navigation works for changed screens
- [ ] UI looks correct in light, dark, and high-contrast themes

### iOS
- [ ] Builds without errors (`xcodebuild ... build`)
- [ ] Changes verified on simulator or device
- [ ] Accessibility rules followed (see `swiftui-accessibility.mdc`)
- [ ] VoiceOver navigation works for changed screens

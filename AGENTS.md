# AGENTS.md -- AI Agent Instructions for Ukulele Companion

Mandatory instructions for AI coding agents (Cursor, Copilot, Codex, etc.) working on this codebase.

Detailed coding rules are in `.cursor/rules/*.mdc` files that auto-attach when editing relevant files. This document provides project-level context and constraints.

## Project Overview

Ukulele Companion is a **free, fully offline** multiplatform app (Android + iOS) for learning and playing ukulele. A core user base includes **blind and visually impaired musicians** who rely on TalkBack (Android) and VoiceOver (iOS). Every code change must preserve accessibility. Breaking accessibility is treated as seriously as breaking functionality.

**Hard constraints -- never violate these:**
- No network dependencies -- the app must remain fully offline
- No analytics, tracking, or telemetry
- No ads or monetization code
- No third-party SDKs without prior discussion
- Never commit `keystore.properties`, API keys, or secrets

## Tech Stack

| Platform | Key Details |
|----------|-------------|
| **Shared (KMP)** | `:shared` module — pure Kotlin business logic in `domain/`, data models in `data/`, `expect/actual` in `platform/` |
| **Android** | Kotlin 2.3.10, Jetpack Compose (BOM 2026.02.00), Material 3, Single Activity, MVVM, StateFlow |
| **iOS** | Swift 6, SwiftUI, MVVM, `@StateObject`/`@Published`, iOS 17.0+, static `shared.framework` via Gradle |
| **Audio ML** | ONNX Runtime 1.24.2 on both platforms (Android AAR, iOS xcframework via C API) |
| **Build** | Gradle 9.3.1, AGP 9.0.1, Kotlin DSL, version catalog (`libs.versions.toml`) |

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
| `ui/` | 54 Compose screens/components via `ModalNavigationDrawer` (no NavHost) |
| `viewmodel/` | 13 ViewModels exposing `StateFlow` |

### iOS app (`iosApp/UkuleleCompanion/`)

| Directory | Contents |
|-----------|----------|
| `Audio/` | `AudioCaptureEngine`, `TonePlayer`, `NeuralPitchSupervisor` (ONNX C API) |
| `Views/` | 47 SwiftUI views across Play, Create, Learn, Reference tabs |
| `ViewModels/` | 15 ViewModels using `@Published` |
| `Helpers/` | `AccessibilityHelper`, `BackupRestoreManager` |

## Cursor Rules Reference

Detailed rules auto-attach when editing matching files:

| Rule | Applies to | What it covers |
|------|-----------|----------------|
| `compose-accessibility.mdc` | `ui/**/*.kt` | TalkBack: icons, headings, Canvas, live regions, focus, modals |
| `swiftui-accessibility.mdc` | `iosApp/**/*.swift` | VoiceOver: labels, hints, traits, values |
| `shared-module.mdc` | `shared/src/commonMain/**/*.kt` | No platform imports, expect/actual, KMP conventions |
| `android-viewmodel.mdc` | `viewmodel/**/*.kt` | StateFlow, repository abstraction, coroutines |
| `ios-viewmodel.mdc` | `ViewModels/**/*.swift` | @Published, @StateObject vs @ObservedObject, KMP naming |
| `compose-ui.mdc` | `ui/**/*.kt` | Material 3, recomposition, Compose-only UI |
| `testing-conventions.mdc` | `test/**/*.kt`, `androidTest/**/*.kt` | Kotest property tests, JUnit 4, what to test when |
| `edge_to_edge.mdc` | `MainActivity.kt`, `Theme.kt`, `libs.versions.toml` | Play Store edge-to-edge warnings (deferred) |

## Build and CI

```bash
# Android
./gradlew assembleDebug                        # Build debug APK
./gradlew lintDebug                            # Run Android Lint
./gradlew testDebugUnitTest                    # Run unit tests
./gradlew connectedAndroidTest                 # Run instrumented tests

# iOS (requires Xcode)
xcodebuild -project iosApp/UkuleleCompanion.xcodeproj \
  -scheme UkuleleCompanion \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  build
```

**CI** (GitHub Actions on push/PR to `main`):
- **Android** (`android.yml`): JDK 17, lint, unit tests, debug APK, APK size report
- **iOS** (`ios.yml`): JDK 17 + Xcode 16.2, shared KMP framework, iOS build, unit tests

**Commit format:**
```
Type: short description

Optional body explaining what and why
```
Types: `Add`, `Fix`, `Update`, `Refactor`, `Test`, `Docs`, `Chore`

## Pre-Submission Checklist

### Both platforms
- [ ] Shared module builds (`./gradlew :shared:build`)
- [ ] Both High-G and Low-G tuning work (if chord/note logic changed)
- [ ] Left-handed mode not broken (if fretboard UI changed)

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

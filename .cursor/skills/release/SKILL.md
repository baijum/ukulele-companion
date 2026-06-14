---
name: release
description: Unified release for both Android and iOS from a single version tag. Runs quality gates, bumps both platform versions in one commit, tags, builds both platforms, uploads Android to Play Store, and opens iOS archive for App Store upload. Use when the user asks to release, ship, cut a release, or make a new version.
---

# Release (Android + iOS)

Build and release both platforms from a single version tag. Bumps Android and iOS versions atomically in one commit, then builds and distributes both.

## Prerequisites

- `gh` CLI authenticated (`gh auth status`)
- Working tree is clean, on `main`, up to date with origin
- Signing keystore via `keystore.properties` at project root (Android)
- Google Play service account key at `app/play-service-account.json` (Android upload)
- Xcode installed with code signing configured (iOS)
- JDK 17 for shared KMP framework build

## Workflow

### Step 1: Verify clean state

```bash
git status
git branch --show-current
gh auth status
```

Confirm the branch is `main` and there are no uncommitted changes. If the working tree is dirty, stop and ask the user to commit or stash first.

### Step 2: Determine the next version

Read the latest existing tag:

```bash
git describe --tags --abbrev=0
```

The project uses **semantic versioning** (`major.minor.patch`) with a `v` prefix (e.g., `v9.11.0`).

If the user did not specify a version or release type, ask which type of release to create:

| Release type | What changes | Example |
|--------------|-------------|---------|
| **Major** | Bump major, reset minor and patch to 0 | `9.11.0` -> `10.0.0` |
| **Feature** | Bump minor, reset patch to 0 | `9.11.0` -> `9.12.0` |
| **Bugfix** | Bump patch | `9.11.0` -> `9.11.1` |

Ask the user to confirm the new version before proceeding.

### Step 3: Run quality gates

All gates must pass before any version bumping or tagging.

#### 3a. Run lint

```bash
./gradlew lintDebug
```

**If lint fails, stop and fix the errors first.**

#### 3b. Run unit tests

```bash
./gradlew testDebugUnitTest
```

#### 3c. Run instrumented tests

Check for a running emulator (`adb devices`) and run:

```bash
./gradlew connectedAndroidTest
```

**If any test fails, stop the release and fix the failures first.** Do not proceed to version bumping until all gates pass.

### Step 4: Bump both platform versions in one commit

#### 4a. Android version bump

Read the current `versionCode` and `versionName` in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = <current>
    versionName = "<major>.<minor>.<patch>"
}
```

- Set `versionName` to the new version (e.g., `"9.12.0"`)
- Increment `versionCode` by 1

#### 4b. iOS version bump

Read the current `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` in `iosApp/UkuleleCompanion.xcodeproj/project.pbxproj`.

- Set `MARKETING_VERSION` to the new version (e.g., `9.12.0`)
- Increment `CURRENT_PROJECT_VERSION` by 1

Both values appear in **four** build-settings blocks (Debug and Release for the app target, Debug and Release for the test target). Update all four occurrences of each.

#### 4c. Generate changelog

Run the changelog script to generate a categorized release notes section from commits since the previous tag:

```bash
scripts/changelog.sh <previous-tag> HEAD v<version>
```

Review the output. If it looks correct, prepend it to `CHANGELOG.md` (after the file header). The header is the first 4 lines (title + description); insert the new section below the header with a blank line separator.

#### 4d. Commit

Stage all three files and commit:

```bash
git add app/build.gradle.kts iosApp/UkuleleCompanion.xcodeproj/project.pbxproj CHANGELOG.md
git commit -m "Release: bump version to <version> (Android versionCode <N>, iOS build <M>)"
```

Do **not** push yet — the tag is created next.

### Step 5: Tag and push

Create an annotated tag on the version-bump commit and push everything:

```bash
git tag -a v<version> -m "v<version>"
git push origin main v<version>
```

This ensures the tag points to the commit that has the correct version numbers and changelog in both platforms.

### Step 6: Create GitHub release with customer-facing release notes

The full developer changelog is already in `CHANGELOG.md` (step 4c). The GitHub release notes should be a **customer-facing summary** — what users will see and care about.

Write the release notes by reviewing the full changelog and applying these rules:

1. **Drop internal changes entirely** — refactoring, CI/CD, dependency bumps, code cleanup, documentation, and test additions are invisible to users. Do not list them.
2. **Describe features in terms of user benefit**, not implementation. Write "Songbook now shows chord diagrams alongside lyrics" instead of "Add persistent chord diagram rail in Songbook viewer".
3. **Group related fixes into one line**. Four separate tuner signal-processing fixes become "Tuner is now smoother and more accurate on iOS".
4. **Use plain language** — no code identifiers, class names, or technical jargon. Write for someone who plays ukulele, not someone who reads the source code.
5. **Keep it short** — aim for 5–10 bullet points. Users skim release notes.

#### Format

```markdown
## What's New

### New Features
- <user-visible feature described as benefit>

### Improvements
- <noticeable quality/UX improvement>

### Bug Fixes
- <user-facing bug that was fixed, described by symptom>
```

Omit any section that has no entries. Omit `### Bug Fixes` entirely for feature releases where all fixes are minor.

#### Example

Full changelog might contain:
```
### Added
- Add persistent chord diagram rail in Songbook viewer
- Add configurable chord display style and color settings
### Fixed
- Add median frequency smoothing and display-cents EMA to iOS tuner
- Add onset blanking to iOS tuner to suppress pluck transients
- Add string-switch hysteresis to iOS tuner
- Use target-string cents for tuner needle on iOS
### Changed
- Extract route composables from FretboardScreen when block
- Replace SettingsViewModel manual SharedPreferences with JSON serialization
### Maintenance
- Create subdirectory CLAUDE.md pointer files
- Drop API 26 from instrumented test matrix
```

Customer-facing release notes:
```
## What's New

### New Features
- Chord diagrams now appear alongside lyrics in the Songbook viewer
- Choose how chords are displayed and colored in your chord sheets

### Improvements
- Tuner on iOS is now smoother and more accurate, with less needle jitter
```

#### Create the release

```bash
gh release create v<version> \
  --title "v<version>" \
  --notes "$(cat <<'EOF'
## What's New

<customer-facing summary written above>

EOF
)"
```

**Do not attach any binary files** — binaries are distributed through the Play Store and App Store.

### Step 7: Generate store "What's New" blurb

Write a short "What's New" blurb for the app store listings based on the customer-facing release notes from step 6. This blurb must:

- Be **500 characters or fewer** (Play Store limit)
- Read as a concise product update (1–4 bullet points)
- Use the same plain-language, user-benefit style as the release notes

Write the blurb to the Gradle Play Publisher's conventional location so it's automatically included in the Play Store upload:

```bash
mkdir -p app/src/main/play/release-notes/en-US
cat > app/src/main/play/release-notes/en-US/default.txt << 'EOF'
<What's New blurb here>
EOF
```

Also show the blurb to the user in a copyable block for App Store Connect's "What's New in This Version" field (same text works for both stores).

### Step 8: Build Android

```bash
./gradlew assembleRelease bundleRelease
```

Verify outputs:

```bash
ls -lh app/build/outputs/apk/release/app-release*.apk \
       app/build/outputs/bundle/release/app-release.aab
```

### Step 9: Smoke test on emulator

Verify the release APK launches and key screens are reachable. This step is **advisory** — if no emulator is running, warn the user and skip to the next step.

#### 9a. Check for a running emulator

```bash
adb devices | grep -w device
```

If no device is listed, print a warning and skip to Step 10.

#### 9b. Install the release APK

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

#### 9c. Launch and verify

```bash
adb shell am start -n com.baijum.ukufretboard/.MainActivity
sleep 5
```

Check for crashes:

```bash
adb logcat -d | grep -i 'FATAL\|AndroidRuntime' | tail -5
```

If any FATAL exception is found, **stop the release** and report the crash to the user.

#### 9d. Take screenshots

```bash
adb exec-out screencap -p > build/release-smoke-explorer.png
```

Read and show `build/release-smoke-explorer.png` to the user for visual confirmation that the main screen rendered correctly.

#### 9e. Navigate to Tuner

Open the navigation drawer and tap "Tuner":

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml build/ui-dump.xml
```

Find the drawer/menu button bounds in the UI dump XML, tap it, then find and tap "Tuner". Take a second screenshot:

```bash
adb exec-out screencap -p > build/release-smoke-tuner.png
```

Show the screenshot to the user.

#### 9f. Result

If both screenshots look correct and no crashes were found, proceed. Otherwise stop and investigate.

### Step 10: Upload Android to Play Store

Check for the service account key:

```bash
ls app/play-service-account.json
```

If the file **exists**, upload the AAB to internal testing (the "What's New" text from step 7 is picked up automatically):

```bash
./gradlew publishReleaseBundle
```

Use `block_until_ms: 120000` (upload can take 30–60s).

If the file is **missing**, warn the user and skip this step. The AAB can be uploaded manually via the [Google Play Console](https://play.google.com/console/).

### Step 11: Build iOS

#### 11a. Build the shared KMP framework for device

```bash
./gradlew :shared:linkReleaseFrameworkIosArm64
```

#### 11b. Download ONNX Runtime (if needed)

```bash
bash iosApp/setup_onnxruntime.sh
```

#### 11c. Build the iOS archive

```bash
xcodebuild archive \
  -project iosApp/UkuleleCompanion.xcodeproj \
  -scheme UkuleleCompanion \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath build/UkuleleCompanion.xcarchive
```

Use `block_until_ms: 300000` (archive builds can take several minutes).

**Important notes:**
- Do NOT pass `CODE_SIGNING_ALLOWED=NO` — the archive must be signed for App Store upload.
- The first build may trigger a macOS Keychain dialog. The user must click "Always Allow" and enter their Mac login password.

#### 11d. Verify static frameworks are not embedded

```bash
ls build/UkuleleCompanion.xcarchive/Products/Applications/UkuleleCompanion.app/Frameworks/
```

This directory should be **empty**. If it contains `shared.framework` or `onnxruntime.framework`, remove them from "Embed Frameworks" in `project.pbxproj`.

### Step 12: Open iOS archive

```bash
open build/UkuleleCompanion.xcarchive
```

This opens Xcode Organizer where the user can click **"Distribute App"** > **"App Store Connect"** > **"Upload"**.

### Step 13: Report to user

Provide:
- The new tag name (e.g., `v9.12.0`)
- The GitHub release URL
- Android: `versionName` / `versionCode`, AAB path and size, Play Store upload status
- iOS: `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`, archive path
- The "What's New" blurb (for copy-pasting into App Store Connect)
- Smoke test result (pass/skip/fail)
- Reminder: iOS upload is manual via Xcode Organizer (Step 12)
- Next steps: test in internal/TestFlight, then promote via `/play-store-promote` and App Store Connect

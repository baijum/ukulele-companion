---
name: android-release
description: Create a release for the Ukulele Companion Android app. Runs lint and all tests first (unit and instrumented), then bumps version, builds release AAB and APK (without debug info), commits, tags, pushes, creates a GitHub release with the APK attached, and uploads the AAB to Google Play Store internal testing. Use when the user asks to release, create a build, prepare a release, tag a version, or publish to GitHub.
---

# Android Release

## Prerequisites

- Signing keystore configured via `keystore.properties` at the project root
- `gh` CLI authenticated for GitHub release creation
- Google Play service account key at `app/play-service-account.json` (see [play-store-upload](.cursor/skills/play-store-upload/SKILL.md) for setup)
- All tests and lint must pass before releasing (see Step 0)

## Workflow

### Step 0: Run lint, unit tests, and instrumented tests

#### 0a. Run lint

Follow the [android-lint](~/.cursor/skills/android-lint/SKILL.md) skill workflow:

```bash
./gradlew lintDebug
```

**If lint fails, stop and fix the errors first.** Do not proceed until lint passes.

#### 0b. Run unit tests

```bash
./gradlew testDebugUnitTest
```

#### 0c. Run instrumented tests

Follow the [android-test-run](~/.cursor/skills/android-test-run/SKILL.md) skill workflow. Check for a running emulator (`adb devices`) and run:

```bash
./gradlew connectedAndroidTest
```

**If any tests fail, stop the release and fix the failures first.** Do not proceed to version bumping until lint and all tests pass.

### Step 1: Determine the next version

Read the current `versionCode` and `versionName` in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = <current>
    versionName = "<major>.<minor>.<patch>"
}
```

The project uses **semantic versioning** (`major.minor.patch`).

If the user did not specify a release type, ask them which type of release to create:

| Release type | What changes | Example |
|--------------|-------------|---------|
| **Major** | Bump major, reset minor and patch to 0 | `5.16.0` -> `6.0.0` |
| **Feature** | Bump minor, reset patch to 0 | `5.16.0` -> `5.17.0` |
| **Bugfix** | Bump patch | `5.16.0` -> `5.16.1` |

Always increment `versionCode` by 1 regardless of release type.

Ask the user to confirm the new version before proceeding.

### Step 2: Bump the version

Edit `app/build.gradle.kts` and update `versionCode` and `versionName` in `defaultConfig`.

### Step 3: Build release artifacts

Run both Gradle tasks in a single invocation:

```bash
./gradlew assembleRelease bundleRelease
```

This produces **release** builds that are minified, resource-shrunk, and signed (no debug info):

| Artifact | Path |
|----------|------|
| APK | `app/build/outputs/apk/release/app-release.apk` |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |

### Step 4: Verify outputs

Confirm both files exist and print their sizes:

```bash
ls -lh app/build/outputs/apk/release/app-release.apk \
       app/build/outputs/bundle/release/app-release.aab
```

### Step 5: Commit and tag

Stage all changes, commit, create an annotated tag, and push:

```bash
git add .
git commit -m "Release v<versionName>"
git tag v<versionName>
git push && git push origin v<versionName>
```

Commit message format: `Release v<major>.<minor>.<patch>`
Tag format: `v<major>.<minor>.<patch>`

### Step 6: Create GitHub release

Use the `gh` CLI to create a release with the APK attached:

```bash
gh release create v<versionName> \
  app/build/outputs/apk/release/app-release.apk \
  --title "v<versionName>" \
  --notes "$(cat <<'EOF'
## What's New

- <summarize changes since last release>

## Downloads

- **APK**: `app-release.apk` attached below (sideload on Android 8.0+)
- **AAB**: Built for Play Store distribution

EOF
)"
```

Populate the "What's New" section by reviewing commits since the previous tag:

```bash
git log <previous-tag>..HEAD --oneline
```

### Step 7: Upload to Google Play Store

Follow the [play-store-upload](.cursor/skills/play-store-upload/SKILL.md) skill workflow to upload the AAB to the Play Store internal testing track:

```bash
./gradlew publishReleaseBundle
```

- Use `block_until_ms: 120000` (upload can take 30-60s).
- This uploads the AAB to the **internal testing** track.

### Step 8: Report to user

Provide the user with:
- The new version (`versionName` / `versionCode`)
- Paths to the AAB and APK with file sizes
- The GitHub release URL
- Confirmation that the AAB was uploaded to Play Store internal testing

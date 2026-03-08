---
name: android-release
description: Build and upload an Android release based on an existing git tag. Runs lint and all tests first (unit and instrumented), bumps version to match the tag, builds release AAB and APK (without debug info), commits the version bump, and uploads the AAB to Google Play Store internal testing. Use when the user asks to make an Android release, build for Play Store, or upload to Google Play.
---

# Android Release

Build and upload an Android release based on an existing version tag created by the [github-release](.cursor/skills/github-release/SKILL.md) skill.

## Prerequisites

- An existing version tag (e.g., `v9.11.0`) created via the `github-release` skill
- Signing keystore configured via `keystore.properties` at the project root
- Google Play service account key at `app/play-service-account.json` (see [play-store-upload](.cursor/skills/play-store-upload/SKILL.md) for setup)
- All tests and lint must pass before releasing (see Step 1)

## Workflow

### Step 0: Determine the target tag

If the user specified a tag, validate it exists:

```bash
git tag -l v<version>
```

If no tag was specified, default to the latest tag:

```bash
git describe --tags --abbrev=0
```

Confirm the tag with the user before proceeding.

### Step 1: Run lint, unit tests, and instrumented tests

#### 1a. Run lint

Follow the [android-lint](~/.cursor/skills/android-lint/SKILL.md) skill workflow:

```bash
./gradlew lintDebug
```

**If lint fails, stop and fix the errors first.** Do not proceed until lint passes.

#### 1b. Run unit tests

```bash
./gradlew testDebugUnitTest
```

#### 1c. Run instrumented tests

Follow the [android-test-run](~/.cursor/skills/android-test-run/SKILL.md) skill workflow. Check for a running emulator (`adb devices`) and run:

```bash
./gradlew connectedAndroidTest
```

**If any tests fail, stop the release and fix the failures first.** Do not proceed to version bumping until lint and all tests pass.

### Step 2: Bump the version

Read the current `versionCode` and `versionName` in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = <current>
    versionName = "<major>.<minor>.<patch>"
}
```

Set `versionName` to match the tag (e.g., tag `v9.11.0` -> `versionName = "9.11.0"`).
Increment `versionCode` by 1 regardless of version type.

Ask the user to confirm the new version before editing.

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

### Step 5: Commit version bump

Stage the version change, commit, and push to main:

```bash
git add app/build.gradle.kts
git commit -m "Android: bump version to <versionName> (versionCode <versionCode>)"
git push
```

### Step 6: Upload to Google Play Store

Follow the [play-store-upload](.cursor/skills/play-store-upload/SKILL.md) skill workflow to upload the AAB to the Play Store internal testing track:

```bash
./gradlew publishReleaseBundle
```

- Use `block_until_ms: 120000` (upload can take 30-60s).
- This uploads the AAB to the **internal testing** track.

### Step 7: Report to user

Provide the user with:
- The tag used (e.g., `v9.11.0`)
- The new version (`versionName` / `versionCode`)
- Paths to the AAB and APK with file sizes
- Confirmation that the AAB was uploaded to Play Store internal testing

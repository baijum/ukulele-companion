---
name: ios-release
description: Build an iOS release based on an existing git tag. Bumps version to match the tag, builds the shared KMP framework and the iOS app archive. Use when the user asks to make an iOS release, build for App Store, or create an iOS archive.
---

# iOS Release

Build an iOS release archive based on an existing version tag created by the [github-release](.cursor/skills/github-release/SKILL.md) skill.

## Prerequisites

- An existing version tag (e.g., `v9.11.0`) created via the `github-release` skill
- Xcode installed with the `UkuleleCompanion` scheme
- Code signing configured (Development Team set in project.pbxproj)
- ONNX Runtime xcframework set up via `iosApp/setup_onnxruntime.sh`
- JDK 17 for building the shared KMP framework

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

### Step 1: Bump the version

Read the current `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` in `iosApp/UkuleleCompanion.xcodeproj/project.pbxproj`.

Set `MARKETING_VERSION` to match the tag (e.g., tag `v9.11.0` -> `MARKETING_VERSION = 9.11.0`).
Increment `CURRENT_PROJECT_VERSION` by 1.

Both values appear in **four** build-settings blocks in `project.pbxproj` (Debug and Release for the app target, Debug and Release for the test target). Update all four occurrences of each.

### Step 2: Build the shared KMP framework

Build the release framework for iOS device:

```bash
./gradlew :shared:linkReleaseFrameworkIosArm64
```

### Step 3: Download ONNX Runtime (if needed)

Ensure the ONNX Runtime xcframework is present:

```bash
bash iosApp/setup_onnxruntime.sh
```

### Step 4: Build the iOS archive

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
- Do NOT pass `CODE_SIGNING_ALLOWED=NO` -- the archive must be signed for App Store upload.
- The first build may trigger a macOS Keychain dialog asking for permission to use the signing key. The user must click "Always Allow" and enter their Mac login password.
- `FRAMEWORK_SEARCH_PATHS` for Release configurations must only include the `iosArm64/releaseFramework` path, not simulator paths. Verify in `project.pbxproj` if you encounter linker errors about simulator architectures.

### Step 5: Verify static frameworks are not embedded

`shared.framework` (Kotlin/Native) and `onnxruntime.xcframework` are **static libraries**. They must only be *linked*, never *embedded*. If they appear in the "Embed Frameworks" build phase in `project.pbxproj`, App Store validation will fail with MinimumOSVersion errors.

Check with:

```bash
ls build/UkuleleCompanion.xcarchive/Products/Applications/UkuleleCompanion.app/Frameworks/
```

This directory should be **empty**. If it contains `shared.framework` or `onnxruntime.framework`, remove them from the "Embed Frameworks" section in `project.pbxproj`.

### Step 6: Commit version bump

Stage the version change, commit, and push to main:

```bash
git add iosApp/UkuleleCompanion.xcodeproj/project.pbxproj
git commit -m "iOS: bump version to <version> (build <buildNumber>)"
git push
```

### Step 7: Open in Xcode Organizer

```bash
open build/UkuleleCompanion.xcarchive
```

This opens Xcode Organizer where the user can click **"Distribute App"** > **"App Store Connect"** > **"Upload"** to upload the build.

### Step 8: Report to user

Provide the user with:
- The tag used (e.g., `v9.11.0`)
- The new version (`MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`)
- Archive path (`build/UkuleleCompanion.xcarchive`)
- Instruction to upload via Xcode Organizer

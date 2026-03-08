---
name: ios-release
description: Build an iOS release based on an existing git tag. Bumps version to match the tag, builds the shared KMP framework and the iOS app archive. Use when the user asks to make an iOS release, build for App Store, or create an iOS archive.
---

# iOS Release

Build an iOS release archive based on an existing version tag created by the [github-release](.cursor/skills/github-release/SKILL.md) skill.

## Prerequisites

- An existing version tag (e.g., `v9.11.0`) created via the `github-release` skill
- Xcode installed with the `UkuleleCompanion` scheme
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
  -archivePath build/UkuleleCompanion.xcarchive \
  CODE_SIGNING_ALLOWED=NO
```

Use `block_until_ms: 300000` (archive builds can take several minutes).

### Step 5: Commit version bump

Stage the version change, commit, and push to main:

```bash
git add iosApp/UkuleleCompanion.xcodeproj/project.pbxproj
git commit -m "iOS: bump version to <version> (build <buildNumber>)"
git push
```

### Step 6: Report to user

Provide the user with:
- The tag used (e.g., `v9.11.0`)
- The new version (`MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`)
- Archive path (`build/UkuleleCompanion.xcarchive`)

**Note:** App Store upload is not automated yet. The archive can be exported and uploaded manually via Xcode Organizer or Transporter.

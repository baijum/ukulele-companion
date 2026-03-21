---
name: ios-appstore-publish
description: Publish the iOS app to Apple's App Store. Covers the full workflow from code signing setup through archive, upload, metadata, and submission. Use when the user asks to publish to App Store, submit for review, upload to App Store Connect, or distribute the iOS app.
---

# iOS App Store Publishing

End-to-end workflow for publishing the iOS app to Apple's App Store.

## Prerequisites

- Apple Developer Program membership ($99/year)
- App created in [App Store Connect](https://appstoreconnect.apple.com) with Bundle ID `com.baijum.ukufretboard.ios`
- Xcode with code signing configured (Development Team in project.pbxproj)
- Privacy policy URL (currently: `https://baijum.github.io/ukulele-companion/privacy-policy/`)

## Workflow

### 1. Create a version tag

Use the [github-release](.cursor/skills/github-release/SKILL.md) skill. iOS uses `ios/vX.Y.Z` prefix tags to avoid collisions with Android tags.

### 2. Build the release archive

Use the [ios-release](.cursor/skills/ios-release/SKILL.md) skill. This bumps version, builds the KMP framework, and creates a signed archive.

### 3. Capture App Store screenshots

Use the [ios-screenshot-capture](.cursor/skills/ios-screenshot-capture/SKILL.md) skill. Required displays:

| Display | Simulator | Output dir |
|---------|-----------|------------|
| iPhone 6.5" (1284x2778) | iPhone 16 Pro Max | `docs/appstore-screenshots/iphone/` |
| iPad 13" (2064x2752) | iPad Pro 13-inch (M4/M5) | `docs/appstore-screenshots/ipad/` |

Ask the user to navigate to each screen in the Simulator, then capture with `xcrun simctl io`. Resize to required dimensions (see ios-screenshot-capture skill).

Recommended screens (5-6): Explorer/Fretboard, Chord Library, Tuner, Songbook, a Learn screen, a Reference screen.

### 4. Upload to App Store Connect

```bash
open build/UkuleleCompanion.xcarchive
```

In Xcode Organizer: **Distribute App** > **App Store Connect** > **Upload**.

The first upload may prompt for Keychain access. Apple processes the build in 15-30 minutes.

After processing, go to **App Store Connect** > **TestFlight** or **iOS Builds** to manage the build. Complete **Export Compliance** when prompted (select "None of the algorithms mentioned above" for this app since it is fully offline with no encryption).

### 5. Fill in App Store metadata

In App Store Connect, fill in:

**App Information:**
- Primary Category: **Music**
- Secondary Category: **Education**
- Content Rights: "This app does not contain, show, or access third-party content"

**Pricing:** $0.00 (Free)

**Tax Category:** App Store software

**Version Information:**
- Screenshots: Upload for iPhone 6.5" and iPad 13" displays (Apple auto-scales for smaller devices)
- Promotional Text (≤170 chars, updatable without review)
- Description
- Keywords (comma-separated, 100 chars max)
- Support URL
- Privacy Policy URL
- Age Rating: Answer all questions as "None"

### 6. Submit for review

Click **Add for Review** > **Submit to App Review**.

Apple review typically takes 24-48 hours. You'll receive an email when complete.

## Common issues

| Issue | Cause | Fix |
|-------|-------|-----|
| MinimumOSVersion validation error | Static frameworks embedded instead of linked | Remove `shared.framework` and `onnxruntime.xcframework` from "Embed Frameworks" build phase in project.pbxproj |
| Linker error: built for iOS-simulator | Release FRAMEWORK_SEARCH_PATHS includes simulator paths | Remove `iosSimulatorArm64` and `iosX64` paths from Release config in project.pbxproj |
| Screenshot dimensions wrong | Simulator renders at non-standard resolution | Resize with `sips -z <height> <width> file.png` |
| Code signing hangs | Keychain access dialog pending | User must enter Mac password and click "Always Allow" |
| Build not appearing in App Store Connect | Processing takes time | Wait 15-30 minutes after upload |

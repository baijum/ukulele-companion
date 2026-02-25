---
name: play-store-upload
description: Upload a release AAB to Google Play Store using the Gradle Play Publisher plugin. Use when the user asks to upload to Play Store, publish to Google Play, deploy to internal testing, promote a release, or mentions Play Console.
---

# Play Store Upload

Upload the release AAB to Google Play Store's internal testing track using the Gradle Play Publisher (GPP) plugin.

## Prerequisites

These must be completed once before first use:

### 1. Google Cloud Service Account

1. Create or select a GCP project at https://console.cloud.google.com
2. Enable the [AndroidPublisher API](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com)
3. Create a [service account and JSON key](https://console.cloud.google.com/apis/credentials/serviceaccountkey):
   - Select "New service account"
   - Name it (e.g., `play-publisher`)
   - Give it the "Project Owner" role temporarily
   - Create a JSON key and download it
4. Place the JSON key file at `app/play-service-account.json` (already in `.gitignore`)
5. Remove the "Project Owner" role from the service account in [IAM settings](https://console.cloud.google.com/iam-admin/iam)

### 2. Play Console Permissions

1. Go to [Users and permissions](https://play.google.com/console/developers/users-and-permissions)
2. Click "Invite new user"
3. Paste the service account email (found in the JSON key file)
4. Grant permissions: "Release to testing tracks" and "Manage store presence"
5. Restrict to the app `com.baijum.ukufretboard`

### 3. Verify Setup

```bash
./gradlew bootstrapListing
```

If this succeeds, the service account is correctly connected.

## Workflow

### Step 1: Verify the release AAB exists

The AAB should already be built by the [android-release](.cursor/skills/android-release/SKILL.md) skill. Verify it exists:

```bash
ls -lh app/build/outputs/bundle/release/app-release.aab
```

If it doesn't exist, build it:

```bash
./gradlew bundleRelease
```

### Step 2: Upload to internal testing

```bash
./gradlew publishReleaseBundle
```

- Use `block_until_ms: 120000` (upload can take 30-60s depending on file size and network).
- This uploads the AAB to the **internal testing** track.
- The release is created with status `COMPLETED` (immediately available to internal testers).

### Step 3: Report result

After a successful upload, provide:
- Confirmation that the AAB was uploaded to internal testing
- Link: https://play.google.com/console/developers/apps/com.baijum.ukufretboard/tracks/internal-testing

## Promoting to Other Tracks

### Promote to production

```bash
./gradlew promoteReleaseArtifact --track production --release-status completed
```

### Promote to beta/alpha

```bash
./gradlew promoteReleaseArtifact --track beta
```

### Staged rollout to production

```bash
./gradlew promoteReleaseArtifact --track production --release-status inProgress --user-fraction 0.1
```

This rolls out to 10% of users. Increase the fraction in subsequent runs.

## Quick Reference

| Task | Command |
|------|---------|
| Upload to internal | `./gradlew publishReleaseBundle` |
| Promote to production | `./gradlew promoteReleaseArtifact --track production --release-status completed` |
| Promote to beta | `./gradlew promoteReleaseArtifact --track beta` |
| Staged rollout (10%) | `./gradlew promoteReleaseArtifact --track production --release-status inProgress --user-fraction 0.1` |
| Validate setup | `./gradlew bootstrapListing` |
| List GPP tasks | `./gradlew tasks --group publishing` |

## Troubleshooting

- **"No service account credentials"**: Ensure `app/play-service-account.json` exists. Alternatively, set the `ANDROID_PUBLISHER_CREDENTIALS` environment variable to the JSON file contents.
- **"Unauthorized" or 403**: The service account email needs permissions in Play Console. Check Users and permissions.
- **"App not found"**: The first version must be uploaded manually via Play Console. The API cannot register new apps.
- **"Version code already exists"**: Bump `versionCode` in `app/build.gradle.kts` before uploading.
- **"APK specifies a version code that has already been used"**: Same as above -- each upload needs a unique, incrementing version code.
- **Build fails with GPP errors**: Run `./gradlew clean bundleRelease` then retry the publish.

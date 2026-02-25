---
name: play-store-promote
description: Promote a release from Google Play internal testing to a closed testing track (Alpha or App Hive Testing). Use when the user asks to promote a release, move to closed testing, push to alpha, push to App Hive Testing, or mentions track promotion.
---

# Play Store Promote

Promote a release from the internal testing track to one of the closed testing tracks using the Gradle Play Publisher (GPP) plugin.

## Prerequisites

- Release already uploaded to internal testing (via [play-store-upload](.cursor/skills/play-store-upload/SKILL.md) or [android-release](.cursor/skills/android-release/SKILL.md))
- Service account key at `app/play-service-account.json`
- Service account has "Release to testing tracks" permission in Play Console

## Workflow

### Step 1: Choose the target track

Ask the user which closed testing track to promote to:

| Track | Name in Play Console |
|-------|---------------------|
| **Alpha** | Alpha |
| **App Hive Testing** | App Hive Testing |

### Step 2: Promote the release

**Promote to Alpha:**

```bash
./gradlew promoteReleaseArtifact --from-track internal --promote-track alpha --release-status completed
```

**Promote to App Hive Testing:**

```bash
./gradlew promoteReleaseArtifact --from-track internal --promote-track "App Hive Testing" --release-status completed
```

- Use `block_until_ms: 60000` (promotion typically takes 10-30s).
- The `--from-track internal` flag tells GPP to take the latest release from internal testing.
- The `--release-status completed` flag makes the release immediately available to testers.

### Step 3: Report result

After a successful promotion, provide:
- Confirmation of the source track (internal) and destination track
- Link to Play Console: https://play.google.com/console/developers/apps/com.baijum.ukufretboard/tracks

## Quick Reference

| Task | Command |
|------|---------|
| Promote to Alpha | `./gradlew promoteReleaseArtifact --from-track internal --promote-track alpha --release-status completed` |
| Promote to App Hive Testing | `./gradlew promoteReleaseArtifact --from-track internal --promote-track "App Hive Testing" --release-status completed` |
| Promote to production | `./gradlew promoteReleaseArtifact --from-track internal --promote-track production --release-status completed` |
| Staged rollout to production | `./gradlew promoteReleaseArtifact --from-track internal --promote-track production --release-status inProgress --user-fraction 0.1` |

## Troubleshooting

- **"No release found in track 'internal'"**: Upload a release to internal testing first using `./gradlew publishReleaseBundle`.
- **"Track not found"**: Verify the track name matches exactly what's in Play Console (case-sensitive). Use quotes around names with spaces.
- **"Unauthorized" or 403**: The service account needs "Release to testing tracks" permission in Play Console.
- **Release not visible to testers**: Ensure testers are added to the target track's tester list in Play Console under Testing > Closed testing.

---
name: play-store-promote-public
description: Promote a release to Google Play open testing or production tracks. Supports full rollout and staged rollout to production. Use when the user asks to promote to open testing, beta, go to production, release to all users, staged rollout, or increase rollout percentage.
---

# Play Store Promote Public

Promote a release to open testing or production using the Gradle Play Publisher (GPP) plugin.

## Prerequisites

- Release already uploaded to internal testing (via [play-store-upload](../play-store-upload/SKILL.md) or [android-release](../android-release/SKILL.md))
- Service account key at `app/play-service-account.json`
- Service account has "Release to testing tracks" and "Release apps to production" permissions in Play Console

## Workflow

### Step 1: Choose the target track and rollout strategy

Ask the user which track to promote to:

| Track | Description | Risk level |
|-------|-------------|------------|
| **Open testing** | Available to anyone who opts in via Play Store | Medium |
| **Production (staged)** | Gradual rollout to a percentage of users | Low |
| **Production (full)** | Immediate rollout to all users | High |

For production, recommend staged rollout starting at 20% unless the user requests full rollout.

### Step 2: Promote the release

**Promote to open testing:**

```bash
./gradlew promoteReleaseArtifact --from-track internal --promote-track beta --release-status completed
```

**Staged rollout to production (recommended):**

```bash
./gradlew promoteReleaseArtifact --from-track internal --promote-track production --release-status inProgress --user-fraction 0.2
```

**Full rollout to production:**

```bash
./gradlew promoteReleaseArtifact --from-track internal --promote-track production --release-status completed
```

- Use `block_until_ms: 60000` (promotion typically takes 10-30s).
- `--release-status inProgress` with `--user-fraction` enables staged rollout.
- `--release-status completed` makes the release available to all users immediately.

### Step 3: Increase staged rollout (if applicable)

If using staged rollout, the user may later ask to increase the percentage or complete the rollout:

**Increase rollout percentage:**

```bash
./gradlew promoteReleaseArtifact --from-track production --promote-track production --release-status inProgress --user-fraction <fraction>
```

Common rollout stages: 0.2 → 0.5 → 1.0

**Complete the rollout (100%):**

```bash
./gradlew promoteReleaseArtifact --from-track production --promote-track production --release-status completed
```

### Step 4: Report result

After a successful promotion, provide:
- Confirmation of the source track and destination track
- Rollout percentage (if staged)
- Link to Play Console: https://play.google.com/console/developers/apps/com.baijum.ukufretboard/tracks

## Quick Reference

| Task | Command |
|------|---------|
| Open testing | `./gradlew promoteReleaseArtifact --from-track internal --promote-track beta --release-status completed` |
| Production staged 20% | `./gradlew promoteReleaseArtifact --from-track internal --promote-track production --release-status inProgress --user-fraction 0.2` |
| Production staged 50% | `./gradlew promoteReleaseArtifact --from-track production --promote-track production --release-status inProgress --user-fraction 0.5` |
| Production full rollout | `./gradlew promoteReleaseArtifact --from-track internal --promote-track production --release-status completed` |
| Complete staged rollout | `./gradlew promoteReleaseArtifact --from-track production --promote-track production --release-status completed` |

## Troubleshooting

- **"No release found in track"**: Ensure a release exists in the source track. Upload to internal testing first with `./gradlew publishReleaseBundle`.
- **"Track not found"**: Open testing is `beta` in GPP. Production is `production`.
- **"Unauthorized" or 403**: The service account needs "Release apps to production" permission in Play Console for production promotions.
- **"Cannot reduce user fraction"**: Staged rollouts can only increase the fraction or complete. To roll back, halt the rollout in Play Console.
- **Staged rollout not progressing**: Monitor crash rate and ANR rate in Play Console before increasing the rollout percentage.

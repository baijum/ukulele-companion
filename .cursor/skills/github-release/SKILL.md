---
name: github-release
description: Create a GitHub release with a version tag and auto-generated release notes. Does not build artifacts or upload to any store. Use when the user asks to tag a version, create a GitHub release, cut a release, or prepare release notes.
---

# GitHub Release

> **For normal releases, use [`/release`](../.cursor/skills/release/SKILL.md) instead** — it creates the tag, bumps both platform versions, builds, and uploads in one workflow. This skill is for creating a tag without building.

Create a version tag on `main` and a GitHub release with release notes summarizing changes since the previous tag.

## Prerequisites

- `gh` CLI authenticated (`gh auth status`)
- Working tree is clean (`git status` shows no uncommitted changes)
- You are on the `main` branch and up to date with origin

## Workflow

### Step 1: Verify clean state

```bash
git status
git branch --show-current
```

Confirm the branch is `main` and there are no uncommitted changes. If the working tree is dirty, stop and ask the user to commit or stash first.

### Step 2: Determine the next version

Read the latest existing tag:

```bash
git describe --tags --abbrev=0
```

The project uses **semantic versioning** (`major.minor.patch`) with a `v` prefix (e.g., `v9.10.1`).

If the user did not specify a version or release type, ask which type of release to create:

| Release type | What changes | Example |
|--------------|-------------|---------|
| **Major** | Bump major, reset minor and patch to 0 | `9.10.1` -> `10.0.0` |
| **Feature** | Bump minor, reset patch to 0 | `9.10.1` -> `9.11.0` |
| **Bugfix** | Bump patch | `9.10.1` -> `9.10.2` |

Ask the user to confirm the new version before proceeding.

### Step 3: Create and push the tag

Create an annotated tag on HEAD and push it:

```bash
git tag -a v<version> -m "v<version>"
git push origin v<version>
```

### Step 4: Generate release notes

Review commits since the previous tag to build the release notes:

```bash
git log <previous-tag>..v<version> --oneline
```

Summarize the changes into categories (Add, Fix, Update, Refactor, etc.) matching the project's commit format.

### Step 5: Create GitHub release

Use the `gh` CLI to create the release. **Do not attach any binary files** -- binaries are distributed through the Play Store and App Store.

```bash
gh release create v<version> \
  --title "v<version>" \
  --notes "$(cat <<'EOF'
## What's New

- <categorized summary of changes since last release>

EOF
)"
```

### Step 6: Report to user

Provide:
- The new tag name (e.g., `v9.11.0`)
- The GitHub release URL
- Reminder that platform releases (Android, iOS) can now be run using this tag

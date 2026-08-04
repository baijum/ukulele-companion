# Deterministic Check Registry

Every automated check available in this project, its command, scope, and
expected runtime. Agents should use the fastest applicable check during
development and the full preflight before committing.

## In-Loop Checks (PostToolUse hooks, ~instant)

These run automatically after every file edit in Claude Code sessions.

| Check | Command | Scope | Runtime |
|-------|---------|-------|---------|
| Style nudge | `scripts/hooks/style-check.py` | `.kt`, `.kts`, `.swift` — line length, trailing whitespace, final newline | <1s |
| ktlint single-file | `scripts/hooks/ktlint-check.py` | `.kt`, `.kts` — full ktlint formatting check on the edited file | 2-5s |
| Accessibility structural | `scripts/hooks/accessibility-check.py` | UI `.kt` and `.swift` files — Icon contentDescription, Canvas semantics, Image labels | <1s |

## Pre-Edit Guards (PreToolUse hooks, ~instant)

These block edits before they happen.

| Check | Command | Scope | Runtime |
|-------|---------|-------|---------|
| Hard constraints | `scripts/hooks/guard-constraints.py` | All files — blocks network imports, analytics, tracking, secrets | <1s |
| KMP purity | `scripts/hooks/guard-kmp-purity.py` | `shared/src/commonMain/**/*.kt` — blocks platform imports | <1s |

## Pre-Commit Checks (git hook)

Run automatically on `git commit` via `.git/hooks/pre-commit`.

| Check | Command | Scope | Runtime |
|-------|---------|-------|---------|
| ktlint (staged files) | `scripts/ktlint.sh --baseline=ktlint-baseline.xml <staged .kt files>` | Staged `.kt` files only | 3-10s |
| gitleaks | `gitleaks git --pre-commit --staged` | Staged diff — detects secrets | 1-3s |

Install with: `./gradlew installGitHooks` or `cp scripts/pre-commit .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit`

## Preflight (pre-submission gate)

The "definition of done" for Android + shared changes. Run before every commit.

| Check | Command | Scope | Runtime |
|-------|---------|-------|---------|
| **Full preflight** | `scripts/preflight.sh` | All four gates below + manual checklist | 3-8 min |
| ktlint ratchet | `scripts/ktlint.sh --baseline=ktlint-baseline.xml` | All `.kt` in shared/ + app/ | 5-15s |
| Shared KMP tests | `./gradlew :shared:jvmTest` | `shared/src/commonTest/` | 15-30s |
| Android unit tests | `./gradlew testDebugUnitTest` | `app/src/test/` | 30-90s |
| Android lint (debug) | `./gradlew lintDebug` | Full Android lint ruleset | 60-180s |
| Skip lint variant | `PREFLIGHT_SKIP_LINT=1 scripts/preflight.sh` | Skips the slower lint gate | 1-3 min |

## Individual Build Checks

| Check | Command | Scope | Runtime |
|-------|---------|-------|---------|
| Android debug build | `./gradlew assembleDebug` | Full Android debug APK | 1-3 min |
| Shared module build | `./gradlew :shared:build` | KMP shared module (all targets) | 30-60s |
| Detekt | `./gradlew detekt` | Static analysis (baseline ratchet) | 30-60s |
| ktlint format | `scripts/ktlint.sh -F` | Auto-fix formatting in-place | 5-15s |
| iOS build | `xcodebuild -project iosApp/UkuleleCompanion.xcodeproj -scheme UkuleleCompanion -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build` | Full iOS build | 2-5 min |

## CI Checks (GitHub Actions on push/PR to main)

| Workflow | File | What It Runs |
|----------|------|-------------|
| Android | `.github/workflows/android.yml` | lint (debug + release), unit tests, shared tests, debug APK, release APK + R8 mapping, APK size report, instrumented tests (API 33/35) |
| iOS | `.github/workflows/ios.yml` | shared KMP framework (debug + release), iOS build (debug + release), unit tests |
| ktlint | `.github/workflows/ktlint.yml` | baseline-aware ratchet on Kotlin changes |

## Recommended Check Sequences

**Quick iteration (during development):**
In-loop hooks handle this automatically. For manual checks:
```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest
```

**Before committing:**
```bash
scripts/preflight.sh
```

**When iOS code changed (additionally):**
```bash
xcodebuild -project iosApp/UkuleleCompanion.xcodeproj \
  -scheme UkuleleCompanion \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  build
```

**Full validation (before release):**
```bash
scripts/preflight.sh
./gradlew detekt
./gradlew connectedAndroidTest  # requires emulator
# iOS build + test separately via Xcode
```

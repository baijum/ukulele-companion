# Contributing to Ukulele Companion

Thank you for your interest in contributing! This project is a free, offline ukulele learning app for Android, and every contribution — big or small — helps make it better for musicians everywhere.

Please read this guide before submitting your first pull request. If you have questions, feel free to open a [discussion](https://github.com/baijum/ukulele-companion/issues) or ask in your PR.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Using AI Tools](#using-ai-tools)
- [Code Guidelines](#code-guidelines)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [What Makes a Good Contribution](#what-makes-a-good-contribution)
- [Reporting Bugs](#reporting-bugs)
- [Requesting Features](#requesting-features)
- [Security Issues](#security-issues)

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to baijum@gmail.com.

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/<your-username>/ukulele-companion.git
   cd ukulele-companion
   ```
3. **Open** the project in Android Studio
4. **Build** and run:
   ```bash
   ./gradlew assembleDebug
   ```
5. **Create a branch** for your work:
   ```bash
   git checkout -b feature/my-contribution
   ```

## Development Setup

### Requirements

- **Android Studio** (latest stable — Ladybug or newer)
- **JDK 11+**
- **Android SDK** with API level 35 installed
- An **emulator** or physical device running Android 8.0+ (API 26+)

### Project Overview

| Directory | Contents |
|-----------|----------|
| `app/src/main/java/com/baijum/ukufretboard/` | All Kotlin source code |
| `audio/` | Tone generation, metronome, audio capture |
| `data/` | Notes, chords, scales, repositories, persistence |
| `domain/` | Pure business logic (chord detection, transposition) |
| `ui/` | Jetpack Compose screens and components |
| `viewmodel/` | ViewModels with StateFlow |

| `docs/` | Feature specs and user manual |

### Key Architectural Patterns

- **Single Activity**: Everything runs through `MainActivity` with Compose navigation
- **ViewModel + StateFlow**: All UI state is managed via ViewModels exposing `StateFlow`
- **Repository Pattern**: Data access is abstracted through repository classes
- **Pure Domain Logic**: The `domain/` package has no Android dependencies, making it easy to test
- **No Network**: The app is fully offline — do not add network dependencies

## How to Contribute

### Areas Where Help Is Needed

| Area | What's Involved | Difficulty |
|------|----------------|------------|
| **Unit Tests** | Test domain logic (`ChordDetector`, `Transpose`, `CapoCalculator`, etc.) | Beginner |
| **UI Tests** | Compose UI tests for screens and components | Beginner |
| **Accessibility** | TalkBack support, content descriptions, live regions, heading semantics | Beginner |
| **Documentation** | Improve code comments, feature docs, user manual | Beginner |
| **New Musical Content** | Scales, strumming patterns, chord progressions | Beginner |
| **Localization** | Translate strings to other languages | Intermediate |
| **CI/CD** | GitHub Actions for automated builds and tests | Intermediate |
| **Bug Fixes** | Check open issues for reported bugs | Varies |
| **Performance** | Optimize Compose recompositions, audio latency | Advanced |

### Types of Contributions

- **Bug fixes** — Find and fix issues in the app
- **Features** — Add new functionality (please open an issue first to discuss)
- **Tests** — Unit tests, integration tests, UI tests (especially welcome!)
- **Documentation** — Code comments, README improvements, feature docs
- **Refactoring** — Improve code quality without changing behavior
- **Musical content** — New chord progressions, strumming patterns, scales, theory lessons
- **Accessibility** — Maintain and improve screen reader support (see [Accessibility Guidelines](#accessibility-guidelines))
- **Translations** — Help make the app available in more languages

## Using AI Tools

We **actively encourage** the use of AI coding assistants for contributing to this project. The codebase follows consistent Kotlin + Jetpack Compose patterns that AI tools understand well.

### Recommended AI Workflows

**Exploring the codebase:**
- Use [Cursor](https://cursor.com), [GitHub Copilot](https://github.com/features/copilot), or similar tools to quickly understand how existing features are built
- Ask your AI assistant to explain the architecture of a specific screen or feature
- Have AI map out dependencies between files before making changes

**Writing code:**
- Use AI to generate boilerplate (ViewModels, Compose screens, repository classes) that matches existing patterns
- Let AI suggest implementations based on similar code already in the project
- Use AI autocomplete to maintain consistency with the codebase's style

**Writing tests:**
- The project currently has no automated tests — this is a great area for AI-assisted contributions
- Ask AI to generate unit tests for the pure-logic classes in `domain/` (e.g., `ChordDetector`, `Transpose`, `CapoCalculator`)
- Use AI to create Compose UI test scaffolding

**Code review:**
- Before submitting a PR, ask an AI assistant to review your changes for:
  - Consistency with the project's patterns
  - Potential bugs or edge cases
  - Missing null checks or error handling
  - Compose best practices (unnecessary recompositions, state handling)

**Documentation:**
- Use AI to help write clear commit messages and PR descriptions
- Generate KDoc comments for public functions and classes
- Improve inline comments where logic is complex

### AI Contribution Guidelines

When using AI-generated code:

1. **Understand what the code does** — Don't submit code you can't explain
2. **Test it** — Run the app and verify the feature works on a device/emulator
3. **Review it for correctness** — AI can make mistakes with music theory, chord formulas, or Android-specific APIs
4. **Adapt it to the codebase** — Ensure it follows the existing patterns (see [Code Guidelines](#code-guidelines))
5. **Mention AI usage** — You're welcome to note in your PR if AI tools helped, but it's not required

## Accessibility Guidelines

This app is actively used by blind and visually impaired musicians with screen readers. **Every code change must preserve accessibility.** See also [AGENTS.md](AGENTS.md) for AI-specific rules.

### Content Descriptions

- Every **informative/interactive icon** must have a `contentDescription` string
- **Decorative-only icons** (inside labeled buttons where the text is sufficient) may use `contentDescription = null`
- Use **conditional descriptions** for toggle states: `if (isPlaying) "Stop" else "Play"`

### Heading Semantics

- **Screen titles** and **section headers** must have `Modifier.semantics { heading() }`
- This enables screen reader users to navigate by headings (swipe up/down with TalkBack)

### Live Regions

- Use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` for frequently updating text (pitch, current note)
- Use `LiveRegionMode.Assertive` for important state changes ("In Tune!", quiz results)

### Canvas Components

- Any `Canvas` that conveys information must be wrapped with `Modifier.clearAndSetSemantics { contentDescription = "..." }`
- Generate the description from the underlying data model, not from visual appearance

### Testing with TalkBack

1. Enable TalkBack: Settings > Accessibility > TalkBack > On
2. Swipe right/left to navigate elements — verify everything is announced
3. Swipe up/down to navigate by headings — verify screen structure makes sense
4. Complete the full user flow without looking at the screen

## Code Guidelines

### Language & Style

- **Kotlin** with the [official Kotlin code style](https://kotlinlang.org/docs/coding-conventions.html)
- Use **meaningful names** for variables, functions, and classes
- Prefer **immutable** data (`val`, `data class`, `List` over `MutableList` in public APIs)
- Use **Kotlin idioms** (`let`, `apply`, `also`, `when`, etc.) where they improve readability

### Jetpack Compose

- All UI must be written in **Jetpack Compose** — no XML layouts
- Use **Material 3** components and theming
- Follow the [Compose API guidelines](https://developer.android.com/develop/ui/compose/api-guidelines)
- Avoid unnecessary recompositions — use `remember`, `derivedStateOf`, and `key` where appropriate
- Extract reusable components into their own `@Composable` functions

### Architecture

- **ViewModels** expose state via `StateFlow` — do not use `LiveData`
- **Repositories** abstract data sources — do not access SharedPreferences directly from ViewModels
- **Domain logic** in the `domain/` package must not import Android framework classes
- Maintain the existing package structure — discuss changes in an issue first

### Things to Avoid

- **Do not add network dependencies** — the app must remain fully offline
- **Do not add analytics or tracking** — this is a core principle of the project
- **Do not add ads or monetization** — the app is free forever
- **Do not add third-party SDKs** without prior discussion — keep the dependency footprint minimal
- **Do not commit** `keystore.properties`, API keys, or other secrets

## Commit Messages

Write clear, descriptive commit messages:

```
<type>: <short description>

<optional body explaining what and why>
```

**Types:**
- `Add` — a new feature or capability
- `Fix` — a bug fix
- `Update` — an enhancement to an existing feature
- `Refactor` — code restructuring without behavior change
- `Test` — adding or updating tests
- `Docs` — documentation changes
- `Chore` — build config, dependency updates, tooling

**Examples:**
```
Add: Dorian mode to scale overlay

Fix: Crash when tapping Stop in Pitch Monitor

Test: Unit tests for ChordDetector major/minor detection

Docs: Add contributing guidelines
```

## Pull Request Process

1. **Ensure your branch is up to date** with `main`:
   ```bash
   git fetch origin
   git rebase origin/main
   ```

2. **Test your changes** — build the app and verify on a device or emulator

3. **Open a Pull Request** against `main` with:
   - A clear title describing the change
   - A description of what you changed and why
   - Screenshots or screen recordings for UI changes
   - Steps to test the change

4. **Address review feedback** — maintainers may request changes. This is normal and collaborative.

5. **Be patient** — this is a hobby project. Reviews may take a few days.

### PR Checklist

Before submitting, verify:

- [ ] Code builds without errors (`./gradlew assembleDebug`)
- [ ] Changes work on a device or emulator
- [ ] No new warnings introduced
- [ ] Code follows the project's existing patterns
- [ ] UI changes look correct in both light and dark themes
- [ ] All new icons have appropriate `contentDescription` (see [Accessibility Guidelines](#accessibility-guidelines))
- [ ] New screens have heading semantics on titles
- [ ] Dynamic content has `liveRegion` where needed
- [ ] TalkBack navigation works for changed screens (if UI was modified)
- [ ] Left-handed mode is not broken (if touching fretboard UI)
- [ ] Both High-G and Low-G tuning modes work (if touching chord/note logic)

## What Makes a Good Contribution

**Good contributions:**
- Solve a real problem or add clear value
- Follow the existing code patterns
- Include context (why, not just what)
- Are tested and verified on a device
- Are focused — one logical change per PR

**Great contributions also:**
- Add tests for the new or changed code
- Update documentation if behavior changes
- Consider edge cases (different tunings, left-handed mode, theme variants)
- Are small enough to review easily

## Reporting Bugs

Use the [Bug Report](https://github.com/baijum/ukulele-companion/issues/new?template=bug_report.md) issue template and include:

- **Device and Android version**
- **App version** (shown in Settings)
- **Steps to reproduce** the issue
- **Expected behavior** vs. **actual behavior**
- **Screenshots or screen recordings** if applicable

## Requesting Features

Use the [Feature Request](https://github.com/baijum/ukulele-companion/issues/new?template=feature_request.md) issue template and include:

- **What** you'd like to see
- **Why** it would be useful
- **How** it might work (if you have ideas)

For large features, please open an issue to discuss before starting work.

## Security Issues

**Do not open a public issue for security vulnerabilities.** See [SECURITY.md](SECURITY.md) for responsible disclosure instructions.

---

Thank you for contributing to Ukulele Companion! Every improvement helps musicians learn and enjoy the ukulele.

# MEMORY.md — Persistent Agent Memory

Notes that persist across coding sessions. Agents should read this at session
start and append to it when they discover non-obvious codebase facts.

## How to Use

- **Read at session start** to load context from previous sessions.
- **Append (never overwrite)** when you discover something non-obvious about the
  codebase that would save time in future sessions.
- Keep entries concise — one or two lines each.
- Prefix each entry with a date stamp.
- Periodically prune entries that have been codified into rules or AGENTS.md.

## Long-Term Facts

These are stable truths about the codebase unlikely to change frequently.

- The app has 76 Compose screens managed by a `ModalNavigationDrawer` — there
  is no Jetpack Navigation (NavHost). Screen switching is done via a `when`
  on `currentScreen` state.
- iOS uses a `TabView` with four tabs (Play, Create, Learn, Reference), each
  with its own `NavigationStack`. This differs from Android's drawer pattern.
- The ONNX model file (`tiny_pitch_v3.onnx`) is bundled as an Android asset
  and an iOS resource. It is loaded asynchronously via `NeuralPitchSupervisor`.
- `SharedPreferences` is the only persistence on Android (no Room/SQLite).
  iOS uses `UserDefaults`.
- The ktlint baseline (`ktlint-baseline.xml`) is a ratchet — new violations
  fail CI but existing ones are grandfathered. Run `scripts/ktlint.sh -F` to
  auto-format.
- String translations flow one-way: Android `strings.xml` (source of truth) →
  `scripts/convert_strings.py` → iOS `Localizable.xcstrings`. Never edit the
  iOS file directly.
- The shared KMP module exposes Kotlin classes to iOS via a static framework.
  In Swift, the `Shared` prefix is dropped (e.g., `PitchDetector` not
  `SharedPitchDetector`).

## Session Log

<!-- Append new entries below this line. Format: YYYY-MM-DD: <note> -->

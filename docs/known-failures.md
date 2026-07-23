# Known Agent Failures

Recurring mistakes AI coding agents make on this codebase, with fixes. Consult
this at session start to avoid repeating them.

<!--
Format:
## <Short title>
**Symptom:** What the agent does wrong.
**Fix:** The correct approach.
**Rule:** Link to the rule that should prevent this (if any).
-->

## Platform imports in commonMain

**Symptom:** Agent adds `import java.util.*` or `import android.*` in
`shared/src/commonMain/` files, breaking the iOS build.
**Fix:** Use `expect/actual` in the `platform/` package. Only pure Kotlin
imports are allowed in commonMain.
**Rule:** `guard-kmp-purity.py`, `.cursor/rules/shared-module.mdc`

## Missing contentDescription on Icon()

**Symptom:** Agent adds `Icon(Icons.Filled.X, contentDescription = null)` on
interactive/informative icons, making them invisible to TalkBack.
**Fix:** Always provide a descriptive `contentDescription` for icons that convey
meaning. Only use `null` for purely decorative icons inside an already-labeled
container.
**Rule:** `.cursor/rules/compose-accessibility.mdc` Rule 1

## Canvas without clearAndSetSemantics

**Symptom:** Agent adds a `Canvas` composable for data visualization but omits
`clearAndSetSemantics`, making the drawing invisible to screen readers.
**Fix:** Wrap the Canvas modifier chain with `clearAndSetSemantics { contentDescription = "..." }`
using a data-driven description.
**Rule:** `.cursor/rules/compose-accessibility.mdc` Rule 3

## Forgetting reduce-motion checks on animations

**Symptom:** Agent adds `animateFloatAsState` or similar without checking
`LocalReduceMotion.current` (Android) or `accessibilityReduceMotion` (iOS).
**Fix:** Wrap animation specs: use `snap()` when reduce motion is enabled.
**Rule:** `.cursor/rules/compose-accessibility.mdc`, `.cursor/rules/swiftui-accessibility.mdc`

## Per-frame allocations in audio path

**Symptom:** Agent creates new arrays or objects inside `processBuffer` or
audio callback hot paths, causing GC pressure and audio glitches.
**Fix:** Follow the `ensureFftBuffers` / `ensureDiffBuffers` pattern — allocate
once and cache across frames.
**Rule:** AGENTS.md "Audio pipeline patterns"

## Hardcoded strings instead of string resources

**Symptom:** Agent uses hardcoded English strings in Compose UI or SwiftUI
views instead of `stringResource(R.string.xxx)` / `NSLocalizedString`.
**Fix:** Add strings via the add-translations skill, which handles all 16
locales and the iOS catalog generation.
**Rule:** `.claude/commands/add-string.md`

## Network/analytics imports (false positive note)

**Symptom:** Agent mentions networking concepts in comments or documentation
and the guard-constraints hook blocks the edit.
**Fix:** The hook uses regex on import/dependency patterns, not prose. If the
block is a false positive on a comment, rewording the comment usually resolves
it. If a genuine exception is needed, ask the user.
**Rule:** `guard-constraints.py`

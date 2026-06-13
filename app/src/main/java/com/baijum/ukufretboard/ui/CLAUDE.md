# Android Compose UI

**Canonical rules** (read the relevant one before editing here):
- [`.cursor/rules/compose-accessibility.mdc`](../../../../../../../../.cursor/rules/compose-accessibility.mdc) — TalkBack: icon `contentDescription`, `semantics { heading() }`, `clearAndSetSemantics` for Canvas, `liveRegion`, focus, modals
- [`.cursor/rules/compose-ui.mdc`](../../../../../../../../.cursor/rules/compose-ui.mdc) — Material 3, recomposition, Compose-only UI, theming
- [`.cursor/rules/compose-coroutines.mdc`](../../../../../../../../.cursor/rules/compose-coroutines.mdc) — programmatic vs user scroll, cancellation-safe flag resets

Essence: **accessibility is treated as seriously as functionality** — a core user base is
blind/visually impaired. Every UI change must keep TalkBack working, respect
`LocalReduceMotion` for animations, and preserve existing semantics.

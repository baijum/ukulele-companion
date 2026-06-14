# UI Layer (Compose)

See canonical rules: `.cursor/rules/compose-accessibility.mdc`, `.cursor/rules/compose-ui.mdc`, `.cursor/rules/compose-coroutines.mdc`

Critical constraints:
- Every interactive element must have a contentDescription or be semantically labeled for TalkBack
- All animations must respect `LocalReduceMotion` — use `snap()` when motion is reduced
- No NavHost — navigation is via `ModalNavigationDrawer` with state hoisting

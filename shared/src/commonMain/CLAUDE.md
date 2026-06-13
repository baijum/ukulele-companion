# KMP shared — commonMain

**Canonical rule:** [`.cursor/rules/shared-module.mdc`](../../../.cursor/rules/shared-module.mdc) — read it before editing here.

Essence: pure Kotlin only. No `java.*`, `javax.*`, `android.*`, `androidx.*`, or
`platform.*` imports in this source set. Platform needs go through `expect`/`actual`
in the `platform/` package (Android in `androidMain`, iOS in `iosMain`). New business
logic — chord detection, transposition, music theory, pitch detection, scales — belongs
here so both platforms share it.

_For Claude Code this is enforced by `scripts/hooks/guard-kmp-purity.py`; the `.mdc` file remains the source of truth for the full rule._

---
description: Extract Android/iOS logic into the shared KMP module, test-first
argument-hint: [logic to extract, e.g. "songbook sort comparator"]
---

Extract the following into the `:shared` KMP module: **$ARGUMENTS**

This is the project's most common refactor. Follow this order exactly — tests first,
so behavior is pinned before the move:

1. **Locate** the current logic and every caller (Android `app/`, and the iOS
   equivalent in `iosApp/` if it was duplicated there).
2. **Pin behavior first.** Add or port unit tests into
   `shared/src/commonTest/` that cover the current behavior. Run them green
   against the *existing* implementation if it's already reachable, otherwise
   write them to describe the intended contract.
3. **Move the pure logic** into `shared/src/commonMain/kotlin/com/baijum/ukufretboard/`
   (`domain/` for business logic, `data/` for models/enums). It must be pure Kotlin:
   no `java.*`, `android.*`, `androidx.*`, or `platform.*` imports
   (enforced by `scripts/hooks/guard-kmp-purity.py`; see
   `shared/src/commonMain/CLAUDE.md`).
4. **Platform needs → expect/actual** in the `platform/` package, with Android impl
   in `androidMain` and iOS impl in `iosMain`.
5. **Keep callers working.** Replace the original Android definition with a thin
   shim / `typealias` if it's still referenced, then migrate callers.
6. **Wire up iOS** to consume the shared type (KMP Swift naming drops the `Shared`
   prefix, e.g. `PitchDetector.shared`).
7. **Verify:** run `scripts/preflight.sh`. Build iOS separately (`xcodebuild ... build`)
   since preflight doesn't cover it.

Keep the change small and reviewable — one logical extraction per PR, matching the
existing `Refactor: extract … into shared KMP module` commit style.

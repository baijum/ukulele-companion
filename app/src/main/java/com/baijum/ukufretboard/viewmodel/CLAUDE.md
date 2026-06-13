# Android ViewModels

**Canonical rule:** [`.cursor/rules/android-viewmodel.mdc`](../../../../../../../../.cursor/rules/android-viewmodel.mdc) — read it before editing here.

Essence: expose state as `StateFlow` (no `LiveData`), go through repository abstractions,
use structured coroutines. Audio-path ViewModels (`TunerViewModel`, `PitchMonitorViewModel`)
guard `processBuffer` with an `isProcessing` flag to drop frames under thermal throttling —
do not add per-frame allocations.

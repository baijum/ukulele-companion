# iOS app (SwiftUI)

**Canonical rules** (read the relevant one before editing here):
- [`.cursor/rules/swiftui-accessibility.mdc`](../../.cursor/rules/swiftui-accessibility.mdc) — VoiceOver: labels, hints, traits, values, attribute preservation (applies to all Swift here)
- [`.cursor/rules/ios-viewmodel.mdc`](../../.cursor/rules/ios-viewmodel.mdc) — `@Published`, `@StateObject` vs `@ObservedObject`, KMP Swift naming, KMP collection bridging (applies to `ViewModels/`)

Essence: **accessibility is treated as seriously as functionality** — keep VoiceOver working
and respect `@Environment(\.accessibilityReduceMotion)` for animations. KMP types drop the
`Shared` prefix in Swift (e.g. `PitchDetector.shared`, `UkuleleTuning.highG`); avoid
force-unwraps — prefer optional binding even for constant values.

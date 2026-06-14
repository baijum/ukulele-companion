# Views (SwiftUI)

See canonical rule: `.cursor/rules/swiftui-accessibility.mdc`

Critical constraints:
- Every interactive element must have an `.accessibilityLabel` for VoiceOver
- All animations must respect `@Environment(\.accessibilityReduceMotion)` — use `nil` animation when reduced
- Use `.accessibilityHint` for long-press and custom actions

# ViewModels (iOS)

See canonical rule: `.cursor/rules/ios-viewmodel.mdc`

Critical constraints:
- Use `@Published` properties for observable state
- Views own ViewModels via `@StateObject`; child views use `@ObservedObject`
- Call shared KMP functions using their Swift-mapped names (e.g. `ChordDetector.shared.detect(...)`)

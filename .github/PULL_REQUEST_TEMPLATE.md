## Summary of changes

<!-- What changed and why? -->

## Accessibility impact

<!-- Critical for our blind/visually impaired user base -->
- [ ] No accessibility impact
- [ ] TalkBack/VoiceOver tested on changed screens
- [ ] New elements have contentDescription/accessibilityLabel

## Commands run

<!-- Which quality gates were verified? -->
- [ ] `scripts/preflight.sh` (or individual gates below)
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew detekt`
- [ ] `xcodebuild ... build` (if iOS changed)

## Tuning modes tested

<!-- If chord/note logic changed -->
- [ ] N/A — no chord/note logic changes
- [ ] High-G tuning verified
- [ ] Low-G tuning verified

## Risk areas touched

- [ ] Audio pipeline (performance-critical)
- [ ] Shared KMP module (affects both platforms)
- [ ] Build configuration / dependencies
- [ ] CI workflows
- [ ] None of the above

## Screenshots/recordings

<!-- If UI changed, attach before/after screenshots or screen recordings -->

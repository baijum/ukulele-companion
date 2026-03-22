# iOS Documentation Screenshots

Screenshots of the iOS app for use in documentation, videos, and the project manual.
Each file is a symlink to the corresponding image in
[`docs/appstore-screenshots/iphone/`](../../appstore-screenshots/README.md).

## Screens captured

| # | File | Screen |
|---|------|--------|
| 01 | `explorer-fretboard.png` | Explorer / Fretboard |
| 02 | `chord-library.png` | Chord Library |
| 03 | `tuner.png` | Chromatic Tuner |
| 04 | `songbook.png` | Songbook |
| 05 | `learn.png` | Learn |
| 06 | `reference.png` | Reference |

## Device info

| Simulator | Resolution |
|-----------|------------|
| iPhone 16 Pro Max | 1284 x 2778 px |

## Recapturing screenshots

Use the [ios-screenshot-capture](../../../.cursor/skills/ios-screenshot-capture/SKILL.md) skill.
Navigate to each screen in the Simulator, then capture with `xcrun simctl io`.
After recapturing the App Store screenshots, these symlinks will automatically
reflect the updated images.

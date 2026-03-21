# App Store Screenshots

Screenshots for the Apple App Store listing. Apple requires only the largest display
size per device family and auto-scales for smaller devices.

## Screens captured

| # | Screen | Description |
|---|--------|-------------|
| 01 | Explorer / Fretboard | Interactive fretboard showing notes and finger positions |
| 02 | Chord Library | Browsable collection of ukulele chord diagrams |
| 03 | Tuner | Chromatic tuner for tuning the ukulele |
| 04 | Songbook | Personal songbook with saved songs |
| 05 | Learn | Practice lessons and learning resources |
| 06 | Reference | Scales, intervals, and music theory reference |

## Directory layout

| Directory | Device | Resolution | Display |
|-----------|--------|------------|---------|
| `iphone/` | iPhone 16 Pro Max | 1320 x 2868 px | 6.9" |
| `ipad/` | iPad Pro 13-inch (M4/M5) | 2064 x 2752 px | 13" |

## Recapturing screenshots

Use the [ios-screenshot-capture](../../.cursor/skills/ios-screenshot-capture/SKILL.md) skill.
Navigate to each screen in the Simulator, then capture with `xcrun simctl io`.

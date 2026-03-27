# Security Policy

## About This App

Ukulele Companion is a **fully offline** Android and iOS app with no backend servers, no user accounts, no analytics, and no network-dependent features. All data is stored locally on the device (SharedPreferences on Android, UserDefaults on iOS).

### Android Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `RECORD_AUDIO` | Chromatic tuner and audio chord detection | Optional |
| `POST_NOTIFICATIONS` | Chord of the Day daily notification | Optional |
| `INTERNET` | Declared in the app manifest (no dependency requires it); not actively used for data transmission | — |

### iOS Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| Microphone | Chromatic tuner and audio chord detection | Optional |

## Reporting a Vulnerability

If you discover a security issue in this project, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, please email the maintainer directly:

- **Email**: baijum@gmail.com
- **Subject line**: `[SECURITY] ukulele-companion: <brief description>`

### What to Include

- A description of the vulnerability
- Steps to reproduce the issue
- The potential impact
- Suggested fix (if you have one)

### Response Timeline

- **Acknowledgment**: Within 72 hours of your report
- **Assessment**: Within 1 week, we will assess severity and confirm the issue
- **Fix**: Security fixes will be prioritized and released as soon as practical

## Scope

The following are in scope for security reports:

- Vulnerabilities in the Android or iOS application code
- Vulnerabilities in the KMP shared module code
- Unsafe handling of microphone audio data
- Data leakage from local storage (SharedPreferences on Android, UserDefaults on iOS)
- ONNX Runtime native integration (C API on iOS, Android library)
- Issues in third-party dependencies
- Unsafe file handling (backup/restore, ChordPro import/export)

The following are **out of scope**:

- Issues requiring physical access to an unlocked device
- Denial of service on the local app
- Social engineering attacks

## Security Practices

This project follows these security practices:

- **No data collection** — the app does not transmit any user data on either platform
- **No third-party SDKs** that collect analytics or telemetry
- **ProGuard/R8** enabled on Android release builds for code shrinking and obfuscation
- **Minimal permissions** — only requests what is necessary, and microphone access is optional
- **Local-only storage** — all user data (favorites, songbook, settings) stays on device
- **ONNX Runtime** — bundled for neural pitch detection; kept up to date with security patches

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest release | Yes |
| Older versions | No — please update to the latest release from the Play Store or App Store |

## Acknowledgments

We appreciate the security research community. Contributors who responsibly disclose valid vulnerabilities will be acknowledged here (with permission).

Thank you for helping keep Ukulele Companion safe.

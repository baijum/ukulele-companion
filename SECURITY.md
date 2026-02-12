# Security Policy

## About This App

Ukulele Companion is a **fully offline** Android app with no backend servers, no user accounts, no analytics, and no network-dependent features. All data is stored locally on the device using SharedPreferences.

The app requests the following permissions:

| Permission | Purpose | Required |
|------------|---------|----------|
| `RECORD_AUDIO` | Chromatic tuner and audio chord detection | Optional |
| `POST_NOTIFICATIONS` | Chord of the Day daily notification | Optional |
| `INTERNET` | Declared but not actively used for data transmission | — |

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

- Vulnerabilities in the Android application code
- Unsafe handling of microphone audio data
- Data leakage from local storage (SharedPreferences)
- Issues in third-party dependencies
- Unsafe file handling (backup/restore, ChordPro import/export)

The following are **out of scope**:

- Issues requiring physical access to an unlocked device
- Denial of service on the local app
- Social engineering attacks

## Security Practices

This project follows these security practices:

- **No data collection** — the app does not transmit any user data
- **No third-party SDKs** that collect analytics or telemetry
- **ProGuard/R8** enabled on release builds for code shrinking and obfuscation
- **Minimal permissions** — only requests what is necessary, and microphone access is optional
- **Local-only storage** — all user data (favorites, songbook, settings) stays on device

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest (5.x) | Yes |
| Older versions | No — please update to the latest release |

## Acknowledgments

We appreciate the security research community. Contributors who responsibly disclose valid vulnerabilities will be acknowledged here (with permission).

Thank you for helping keep Ukulele Companion safe.

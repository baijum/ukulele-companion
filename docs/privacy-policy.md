# Uke (Ukulele Companion) - Privacy Policy

Last updated: August 20, 2026

## Overview

Uke (Ukulele Companion) is a free, open-source ukulele learning app.
Your privacy is important to us. This policy explains what data the app
accesses and how it is handled.

## Data We Do Not Collect

Uke does **not**:

- Collect, store, or transmit any personal information
- Use analytics, tracking, or advertising SDKs
- Require an account or login to use core features
- Send any of your data to servers we control — there are none
- Require an internet connection for any of its music features

## Offline by Default

Every feature that teaches, tunes, or records — the fretboard, chord library,
tuner, metronome, songbook, practice tools — runs entirely on your device with
no network access. The app has no backend and no account system, so there is
nothing for it to sync.

Two things reach outside the app, and both are started by you or by your
device's own operating system:

- **Links you tap.** The website, free-book, video-guide, and open-source
  licence links under Settings hand a web address to your browser. The app
  itself fetches nothing; from that point on you are on the linked site, under
  its own privacy policy.
- **The store review prompt.** See below.

## Store Review Prompt

Once you have opened the app on at least five separate days, and a week has
passed since you installed it, the app may ask your device's app store to show
its own "rate this app" card:

- On Android this uses the Google Play In-App Review API, part of the Google
  Play Store app already on your device.
- On iOS this uses Apple's StoreKit review request.

The rating and any review text you write go to Google or Apple under **their**
privacy policies, not to us — the same as rating the app from the store listing
directly. The app sends no data of its own along with the request, receives no
information about you back, and never learns what rating you gave, or even
whether the card appeared at all. The request is made at most three times in the
app's lifetime, at least 90 days apart, and ignoring it costs you nothing.

The counters that decide when to ask (how many separate days you have opened
the app, when it was first launched, how many times it has asked) are stored
only on your device.

## Microphone Access

The app requests microphone access **only** for its optional pitch-detection
features — the **Tuner**, **Pitch Monitor**, **Play Along**, and the recording
mode of the **Melody Notepad**. When you use one of these features, the app
listens to audio from your device's microphone to detect the pitch of your
ukulele strings in real time.

- Audio is processed **entirely on your device** and is never recorded, saved, or transmitted.
- Audio data is discarded immediately after pitch detection; no audio buffers are retained.
- The microphone is only active while one of these features is running. It stops as soon as you leave the screen or tap "Stop."
- If you deny microphone permission, every other feature of the app continues to work normally.

## Backup (Optional)

You may optionally export your data to a local file and import it back later.
On both Android and iOS, backups are written to and read from local files you
choose; the app does not upload your data to any cloud service or external
server.

## Local Storage

Preferences (tuning, theme, favorites, chord sheets) are stored locally on your
device using standard local storage (SharedPreferences on Android, UserDefaults
on iOS). This data stays on your device unless you explicitly use the backup
feature.

## Children's Privacy

The app does not knowingly collect any information from children. It contains no
ads, in-app purchases, or social features.

## Changes to This Policy

If this policy is updated, the new version will be posted on this website with
an updated date. Continued use of the app after changes constitutes acceptance
of the revised policy.

## Contact

If you have questions about this privacy policy, please open an issue on the
project's GitHub repository.

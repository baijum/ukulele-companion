# Speech: From Chords To Code — Building A Ukulele Companion App

**FOSSMeet 2026 | NIT Calicut | Saturday, April 11, 6:00–6:45 PM | Bhaskara Hall**

> Total time: ~42 minutes speaking + 3 minutes Q&A.
> Pace: conversational, ~130 words/minute.
> Tip: the audience has been in talks since 3 PM — keep energy up, use the screenshots and diagrams to anchor attention.

---

## SECTION A: OPENING (~4 minutes)

### Slide 1 — Title

Good evening, everyone. Thank you for sticking around on a Saturday evening — I know it has been a long day of talks.

My name is Baiju Muthukadan, and this talk is called "From Chords To Code: Building A Ukulele Companion App."

Over the next 45 minutes, I want to take you through the journey of building a real, shipping mobile app as a free and open source project — from the first commit to two app stores, from basic chord math to running a neural network on your phone. And yes, there will be actual code.

### Slide 2 — About Me

A little about me. I have been working with open source for a long time. Some of you might know me from my work in the Python and Go communities — I wrote "A Comprehensive Guide to Go Programming."

But this talk is about something different. About two years ago, I picked up the ukulele. I wanted a practice app that worked offline, had no ads, and did not track me. I could not find one that met all three criteria. So I built one.

The entire project is MIT-licensed and on GitHub at the URL you see on screen. Everything I show today — the algorithms, the neural model integration, the architecture — you can read, fork, and contribute to.

### Slide 3 — Why This Talk?

Why am I telling you this story at a FOSS conference?

Three reasons.

First — the personal itch. I needed a tool that did not exist the way I wanted it to exist. That is the oldest motivation in open source.

Second — scope. What started as a single screen — tap frets, see a chord name — grew into 17 feature areas, two platforms, and a neural network. That growth taught me a lot about architecture and about knowing when to say no.

Third — and most relevant to this room — this is a story about building a real product as a FOSS project. Not a library, not a framework. A product. With users, app store reviews, localization in 16 languages, and accessibility for blind musicians. I think those stories are worth sharing, because they are different from the typical open source narrative.

This talk covers architecture, signal processing, accessibility, and the open source journey. Let us get into it.

---

## SECTION B: WHAT THE APP DOES (~7 minutes)

### Slide 4 — The App at a Glance

Let me give you the elevator pitch.

Ukulele Companion is a free, fully offline ukulele learning app. It runs on Android and iOS. It has 17 distinct feature areas — tuner, fretboard explorer, chord library, songbook, metronome, and more. It is localized in 16 languages, including Hindi, Arabic, Japanese, and Chinese. And it is MIT-licensed.

The key constraint — and I will come back to this several times — is that the app is completely offline. No network calls. No telemetry. No analytics. No ads. Your phone's microphone is the only permission it asks for, and only when you open the tuner. This is not a compromise; it is a feature.

### Slide 5 — Fretboard Explorer

Let me show you the core feature that started it all.

This is the Fretboard Explorer. You see a visual ukulele fretboard on screen. You tap on the fret positions where you would place your fingers. And the app instantly tells you what chord you are playing.

It recognizes 20 chord types — major, minor, diminished, augmented, sevenths, suspended, extended chords. And it shows you alternate notation. If you are playing an A minor seventh, it shows you that Am7 is also written as A-7 in jazz notation.

The detection uses pure interval math. There is no machine learning here, no lookup table. It is modular arithmetic over pitch classes. I will walk you through the algorithm in detail later — it is surprisingly elegant.

### Slide 6 — Chord Library & Scale Overlay

Next to the explorer, there is a chord library and a scale overlay.

The chord library lets you browse voicings by root note, category, and type. But here is what makes it interesting: the voicings are not stored in a database. They are generated algorithmically at runtime. I will explain how that works later.

The scale overlay highlights notes from any of 38 scales directly on the fretboard, and shows you the diatonic chords for that scale. Both of these features share the same data models from the shared Kotlin module — which is a good example of the code reuse that Kotlin Multiplatform enables.

### Slide 7 — Tuner & Pitch Monitor

This is the tuner — and it is the most technically interesting part of the app.

It uses a hybrid pipeline: a classical signal processing algorithm called YIN for real-time pitch detection, supervised by a tiny neural network called SwiftF0 for octave error correction. The audio capture runs at 44.1 kHz with 4096-sample frames, giving us about 23 milliseconds of latency.

The app supports 8 tuning presets — High-G, Low-G, Baritone, D-Tuning, and others. The pitch monitor on the right shows you the raw frequency readings over time, which is useful for debugging and for advanced players who want to see their intonation.

I will do a deep dive into this entire pipeline later. It is the section I am most excited about.

### Slide 8 — Songbook & Practice Tools

The app is not just a reference tool. It is also a practice tool.

The songbook uses ChordPro format — you write lyrics with inline chord markers, and the app renders them with the chords highlighted above the text. You can transpose songs, organize them into setlists, and use auto-scroll for hands-free playing during practice or performance.

The metronome supports 30 to 300 BPM, tap tempo, time signatures from 2/4 to 7/4, accent patterns, and subdivisions. There are 14 strumming patterns and 10 fingerpicking patterns, each with audio playback so you can hear what they sound like before you try them on the instrument.

These are the features that make people come back to the app daily. The reference tools get you started; the practice tools keep you playing.

### Slide 9 — Learning & Composition

Finally, there is a learning and composition layer.

You can see the circle of fifths on screen — this is an interactive reference that shows key relationships, relative minors, and chord families.

There is ear training — the app plays an interval and you identify it. There is an interval trainer, theory lessons, and a glossary of music terms.

For composition, there is a melody notepad with an 8 or 16-step sequencer, and you can record melodies directly from the microphone. And there is a songwriter mode — a guided flow that takes you from picking a key, to choosing a chord progression, to writing lyrics with inline chords, and saves the result directly to your songbook.

The app also tracks your practice time, gives you daily challenges, and has an achievement system. All of this works offline, of course.

---

## SECTION C: ARCHITECTURE (~7 minutes)

### Slide 10 — Architecture Overview

Now let us look at how all of this is built. Three layers, two platforms, one shared core.

This is the architecture diagram. At the centre is a shared Kotlin Multiplatform module. It contains all the domain logic — chord detection, pitch detection, music theory, FFT, transposition, scale calculations, data models. This module has zero platform imports. It is pure Kotlin.

Below the shared module, there are two platform-specific apps. On the left, Android with Jetpack Compose and Material 3. On the right, iOS with SwiftUI. Each platform app handles its own UI, audio capture, audio playback, persistence, and accessibility — the things that are inherently platform-specific.

The shared module compiles to a regular library on Android and to a static framework on iOS. Swift can call the Kotlin functions directly through the framework.

### Slide 11 — The Shared KMP Module

Let me zoom into the shared module because this is the intellectual heart of the project.

It has 31 domain files and 24 data files. The domain layer contains the algorithms — ChordDetector, PitchDetector, FFTProcessor, Chromagram, Transpose, ScaleChords, and more. The data layer contains the music theory knowledge — chord formulas for 20 chord types, definitions for 38 scales, chord progression templates, strumming and fingerpicking patterns.

The platform layer is minimal. It uses Kotlin's expect/actual mechanism for exactly two things: UUID generation and getting the current timestamp. That is it. Everything else is pure Kotlin that compiles and runs identically on both platforms.

This was the key architectural decision: push as much as possible into the shared module. Platform-specific code should only exist for things that genuinely require native APIs. This gives you two benefits — code correctness is verified once, and features land on both platforms simultaneously.

### Slide 12 — Android Layer

The Android app is a fairly standard Jetpack Compose setup.

58 Compose screens, 15 ViewModels, a single Activity. I use a ModalNavigationDrawer instead of Jetpack Navigation's NavHost — that was a pragmatic decision early on, and it has held up well. Each feature area has its own ViewModel exposing StateFlow for reactive state management.

Audio playback uses SoundPool with OGG-encoded ukulele samples — real instrument recordings, not synthesized tones. Audio capture for the tuner uses AudioRecord at 44.1 kHz, 16-bit PCM, mono. And neural inference runs through ONNX Runtime's Android AAR.

### Slide 13 — iOS Layer

The iOS app mirrors the Android feature set using SwiftUI.

50 SwiftUI views, 18 ViewModels, targeting iOS 16 and above. State management uses ObservableObject with @Published properties — it is the SwiftUI equivalent of Android's ViewModel with StateFlow.

Audio playback uses AVFoundation with WAV samples. The key difference from Android is in neural inference — on iOS, ONNX Runtime is integrated through the C API via an xcframework, rather than a first-party Swift package. That required writing some C interop code with custom module maps, but it works reliably.

The shared Kotlin module compiles to a static framework. A shell script build phase in Xcode runs the Gradle task to build the framework for the correct architecture — arm64 for device, simulator arm64 for the simulator.

### Slide 14 — Platform Boundaries

This table shows what stays platform-specific and why.

Audio capture: AudioRecord on Android, AVAudioEngine on iOS. Playback: SoundPool on Android, AVAudioPlayer on iOS. Neural inference: ONNX AAR on Android, ONNX C API on iOS. Persistence: SharedPreferences on Android, UserDefaults on iOS. Accessibility: TalkBack semantics on Android, VoiceOver traits on iOS. And of course, the UI frameworks are completely different — Compose and SwiftUI.

I want to emphasize that this is an intentional boundary, not a limitation of KMP. Trying to abstract audio capture across platforms would produce a worse abstraction than two focused, platform-native implementations. The shared module provides the algorithms; each platform wraps them in its native APIs. This is the right separation of concerns.

---

## SECTION D: TECHNICAL DEEP-DIVES (~12 minutes)

### Slide 15 — Deep Dive: Chord Detection

Let us go deeper. How does the chord detection actually work?

When you tap fret positions on the fretboard, the app converts each position to a pitch class — an integer from 0 to 11. C is 0, C sharp is 1, D is 2, and so on up to B at 11. If you tap the same note on different strings, you get duplicates, so the first step is to remove those.

Then, for each unique pitch class, the algorithm treats it as a candidate root note. For each candidate root, it computes the intervals of all other notes relative to that root, using modular arithmetic: interval equals pitch class minus root plus 12, mod 12. The "plus 12 mod 12" handles the wraparound — so if the root is A and you have a G, you get the correct interval of 10 semitones, not a negative number.

Then it compares the resulting interval set against all 20 known chord formulas. The first match wins — and because triads come before seventh chords in the formula list, simpler interpretations are preferred. But all matches are collected as alternates, so if a set of notes could be interpreted as both C6 and Am7, the app shows both.

### Slide 16 — Chord Formulas as Data

The beauty of this approach is that the chord formulas are pure data.

Look at the data class on screen. A ChordFormula has a symbol, a quality description, an interval set, and a list of aliases. A major chord is the set {0, 4, 7} — root, major third, perfect fifth. Minor is {0, 3, 7}. Diminished is {0, 3, 6}. Dominant seventh is {0, 4, 7, 10}.

And the detection — the actual matching — is a single line of code. If the computed interval set equals the formula's interval set, it is a match. That is it. Set equality.

This design makes the system trivially testable and trivially extensible. To add a new chord type, you add one data entry to the formulas list. The detection algorithm, the chord library, the voicing generator — they all pick it up automatically.

### Slide 17 — YIN Pitch Detection Pipeline

Now let us talk about the tuner, which is where the real signal processing lives.

The tuner uses the YIN algorithm, published by de Cheveigné and Kawahara in 2002. It is the gold standard for monophonic pitch detection — perfect for a single ukulele string.

I implemented it in pure Kotlin. No native DSP libraries, no JNI, no platform dependencies. Just Kotlin running in the shared KMP module.

The pipeline has seven steps. Audio comes in from the microphone at 44.1 kHz in 4096-sample frames. First, a silence gate checks the RMS amplitude — if the signal is too quiet, we skip the frame. Then the difference function is computed, which is the core of YIN. I use FFT to compute this in O(N log N) instead of the naive O(N squared) approach.

Next, the cumulative mean normalized difference smooths the result. A threshold search finds the first dip below 0.15, which indicates periodicity. A confidence gate rejects frames where the dip is not deep enough — this prevents false detections during attack transients when you first pluck a string.

Finally, the best local estimate refines the lag location, and parabolic interpolation gives us sub-sample precision. The lag is converted to a frequency by dividing the sample rate by the refined lag.

### Slide 18 — YIN Key Insight

The fundamental intuition behind YIN is simple.

You take the audio signal and slide it against itself at different offsets, called lags. At each lag, you compute the sum of squared differences between the original signal and the shifted version. At the lag that matches the signal's period, the shifted version aligns perfectly, so the difference is minimal.

That lag gives you the period. And frequency equals sample rate divided by the period.

The clever optimization is in how we compute the difference function. Instead of the naive double loop — which is O(N squared) — YIN decomposes it into energy terms and a cross-correlation term. The cross-correlation can be computed using FFT, which brings it down to O(N log N). For a 4096-sample frame, that is the difference between about 16 million operations and about 50 thousand.

Our frame size is 4096 samples, which is about 93 milliseconds of audio at 44.1 kHz. The hop size is 1024, which means we get about 43 pitch updates per second — fast enough that the tuner needle moves smoothly. And the detection range covers 65 to 1100 Hz, which spans all standard ukulele tunings with room to spare.

### Slide 19 — Neural Pitch Supervisor

YIN works very well for clean, sustained notes. But it has two weaknesses.

First, octave errors. YIN sometimes locks onto a harmonic instead of the fundamental, especially on the lower strings where the harmonics are strong. Second, noise sensitivity. In a room with background noise — like, say, a college auditorium — the difference function gets noisy, and the threshold search can produce false positives.

To address this, I added a neural pitch supervisor. The system is a hybrid — YIN runs on every frame for low-latency response, and a neural model called SwiftF0 runs every fifth frame, about every 100 milliseconds, as a periodic second opinion.

The arbitration logic is straightforward. When YIN and SwiftF0 agree, trust YIN — it has better frequency resolution. When they disagree by exactly one octave, trust the neural model — it is much better at resolving octave ambiguity. When they disagree by more than an octave, use the one with higher confidence. This simple rule set eliminated virtually all octave jump issues in real-world testing.

### Slide 20 — SwiftF0 Model

SwiftF0 is a tiny 2D convolutional neural network by Nieradzik, published in 2025.

The key numbers: 95 thousand parameters, 398 kilobytes as an ONNX file. Compare that to CREPE, a popular pitch detection model, which has 22 million parameters. SwiftF0 is about 230 times smaller, and it is designed specifically for on-device, real-time use.

It takes raw audio at 16 kHz as input and outputs a pitch estimate in Hertz plus a confidence score. I downsample from 44.1 kHz to 16 kHz with linear interpolation — that is acceptable because ukulele fundamentals are well below 8 kHz, so we do not lose relevant information.

The model runs via ONNX Runtime on both platforms — the Android AAR on Android, and the C API xcframework on iOS. Inference takes less than 10 milliseconds on a mid-range phone, so it fits comfortably within the 100-millisecond interval. The model is MIT-licensed, same as the rest of the project.

### Slide 21 — Chromagram: Audio Chord Detection

The app can also detect chords from audio, not just from fretboard taps. This uses a technique from music information retrieval called a chromagram.

The idea is to take the FFT magnitude spectrum — those 4096 frequency bins — and map each bin to one of 12 pitch classes. You do this using the formula on screen: round 12 times log base 2 of the frequency divided by C0, mod 12. This maps every frequency to its pitch class, regardless of which octave it is in.

Then you accumulate the energy from all bins that map to the same pitch class, giving you 12 chroma bins — one for each note. Normalize so they sum to 1.0, and you have a 12-element vector that describes which notes are present in the audio.

And here is the elegant part: you feed that into the same ChordDetector that handles fretboard taps. The same algorithm, the same formula matching, the same code. Just different input. That is the benefit of designing the chord detection around pitch classes rather than fret positions from the start.

### Slide 22 — Pure-Kotlin FFT

The FFT implementation deserves a brief mention because it underpins both the pitch detector and the chromagram.

It is a pure-Kotlin radix-2 Cooley-Tukey implementation. It operates on power-of-2 buffer sizes, uses bit-reversal permutation for in-place computation, and has the standard butterfly stages.

The key performance optimization is twiddle factor caching. The twiddle factors — those cosine and sine values used in the butterfly operations — are pre-computed for each FFT size and cached in a HashMap. Since the tuner always uses 4096-sample frames, the twiddle factors are computed once on the first frame and reused for all subsequent frames — about 43 times per second.

This FFT runs in the shared KMP module, which means the same implementation runs on Android and iOS. No JNI, no native code, no platform-specific optimizations. Just portable Kotlin. For a 4096-point FFT on a modern phone, the performance is more than adequate.

### Slide 23 — Algorithmic Chord Voicings

One more deep dive — the voicing generator.

When you open the chord library and select, say, C major seventh, the app shows you several playable voicings on the fretboard. Those voicings are not stored in a database. They are generated at runtime.

The algorithm takes a root note and a chord formula — which gives you the required pitch classes. Then it enumerates combinations of fret positions across the four ukulele strings, where each string must produce one of the required pitch classes. It filters out combinations that do not cover all required notes, and ranks the remaining ones by playability — favoring smaller fret spreads, open strings, and positions that are comfortable to hold.

The beauty of this approach is that adding a new chord formula to the data list automatically generates voicings for all 12 root notes. No manual curation required. And the same algorithm works for any tuning — High-G, Low-G, Baritone — because it works from pitch classes, not from hardcoded fret positions.

---

## SECTION E: QUALITY & ACCESSIBILITY (~6 minutes)

### Slide 24 — Testing Strategy

Testing audio and music algorithms is tricky, because the edge cases are not obvious. What happens if the sample rate is 192 kHz? What if the buffer contains NaN? What if you try to detect a chord from zero notes?

I use three layers of testing.

Unit tests with kotlin.test for basic domain logic and data models — these are the straightforward cases. Property-based tests with Kotest that generate thousands of random inputs per test and verify invariants. And fuzz tests with Jazzer that throw adversarial inputs at the pitch detector and FFT to ensure they never crash.

All of this runs on GitHub Actions on every push and every pull request. The shared module has 39 test files, and we are at 86.8% instruction coverage measured by Kover.

### Slide 25 — Property-Based Testing Example

Let me show you what these tests actually look like.

The fuzz test on the top generates random sample rates between 8 kHz and 192 kHz, creates audio buffers of random sizes filled with random floats — including NaN, Infinity, and denormalized values — and calls PitchDetector.detect. The only assertion is that it does not throw. We do not care what pitch it returns for garbage input; we just want crash-freedom.

This has found real bugs. Denormalized floats that caused the FFT to produce NaN, which propagated through the pipeline. Division by zero in an edge case of the CMND normalization. Buffer index out of bounds when the frame size was very small.

The property test on the bottom verifies an algebraic invariant: transposing a pitch class up by N semitones and then down by N semitones should always return the original pitch class. Kotest generates thousands of random combinations and checks this property. It is an incredibly powerful technique for mathematical code like music theory algorithms.

### Slide 26 — Accessibility

This slide is important to me.

A core user base of this app includes blind and visually impaired musicians. This is not a nice-to-have feature — it is a design constraint baked into the project's AGENTS.md file. The rule is simple: breaking accessibility is treated as seriously as breaking functionality.

On Android, every element has TalkBack content descriptions. On iOS, every element has VoiceOver traits and labels. The fretboard, the tuner needle, the chord diagrams — these are all custom Canvas drawings, which means the accessibility framework cannot infer their content. So each one has explicit text alternatives that describe what is on screen.

The tuner uses live regions — when the detected note changes, the screen reader announces it in real time. Heading semantics are applied throughout so that screen reader users can navigate between sections efficiently. And there is a high-contrast theme for users with low vision.

This took real engineering effort. But it is the right thing to do, and at a FOSS conference, I think it is worth saying: if your software is not accessible, it is not truly free.

### Slide 27 — Localization

The app ships in 16 languages.

On Android, each locale has its own strings.xml resource file. On iOS, Apple uses a single Localizable.xcstrings JSON file that contains all translations for all locales.

We support right-to-left layout for Arabic, which required careful testing of every screen. We have tooling that lets us add a new translatable string to all 15 non-English locales at once, which prevents the common problem of adding a string in English and forgetting to translate it.

One lesson I would share: do localization early. Adding it from the start is a small incremental cost per feature. Retrofitting it later is a massive, error-prone project. If you are building an app that you want to reach a global audience, wire up the localization infrastructure before you have more than a few screens.

---

## SECTION F: OPEN SOURCE & LESSONS (~6 minutes)

### Slide 28 — Open Source Infrastructure

Let me talk about what it takes to run this as a proper open source project.

The repository has a CONTRIBUTING.md with setup instructions and coding conventions. A CODE_OF_CONDUCT.md. Six GitHub Actions workflows that run on every push and PR — Android builds and tests, iOS builds and tests, fuzz tests, documentation site deployment, iOS stress tests, and an AI-assisted review workflow.

The documentation site is built with MkDocs and includes a full user manual for both Android and iOS, with screenshots for every feature.

And then there is AGENTS.md — this is a file that encodes the project's non-negotiable constraints for any AI coding assistant that works on the codebase. It says: this app must stay offline, must not add telemetry, must preserve accessibility, and must maintain feature parity between platforms. These are not guidelines; they are rules.

### Slide 29 — AI-Assisted Development

I want to be transparent about AI's role in this project, because I think it is relevant to how software is being built in 2026.

AI coding tools were used extensively — for code generation, for writing tests, for accessibility audits, for cross-platform parity checks. When you have 58 Compose screens and 50 SwiftUI views that need to stay in sync, having an assistant that can compare them and flag discrepancies is genuinely useful.

But the key insight is that you need guardrails. The Cursor rules in this project auto-attach contextual constraints when you edit specific types of files. Edit a Compose file, and the TalkBack accessibility patterns are enforced. Edit a SwiftUI file, and VoiceOver patterns are enforced. Edit a test file, and the testing conventions are injected.

AGENTS.md acts as the constitution. It encodes what is non-negotiable. AI accelerates development; rules prevent drift. The constraints are enforced by configuration, not by trust.

### Slide 30 — Lessons Learned

Five lessons from building this project.

One: push logic into the shared module. Platform-specific code should only handle platform APIs. We have about 4,700 lines of domain logic that runs identically on both platforms without a single change. That is 4,700 lines of code where a bug fix lands on both platforms at once.

Two: accessibility from day one. I tried adding TalkBack support to a feature after the fact once, and it took three times as long as building it in from the start. Accessible design is not something you bolt on; it shapes how you structure your composables and your view hierarchy.

Three: offline-first is a feature, not a limitation. I shipped Google Drive sync and then removed it. Users trust software that they control. When your app works without a network, it works everywhere — on a plane, in a park, in a rural area with no connectivity.

Four: property-based testing catches bugs that humans would never write test cases for. Denormalized floats in the FFT. Edge cases in transposition. Buffer boundary issues at unusual sample rates. If you write code that does math, use property-based testing.

Five: parity is a product feature. The iOS port is not done when the screens look the same. It is done when every feature behaves identically — the same chord voicings, the same tuner behavior, the same songbook format. Parity is ongoing work, and it requires discipline.

### Slide 31 — Product Decisions That Mattered

Some product decisions defined this project more than any line of code.

I shipped Google Drive sync in an early version. It worked. Users could back up their songbook to the cloud. But it created a dependency on Google services, and it undermined the core promise of an offline app. In version 5.0, I removed it entirely and replaced it with local backup and restore. Harder to use? Yes. More honest? Also yes.

No ads, no analytics, no telemetry — ever. The privacy policy is one sentence: "This app does not collect any data." I genuinely do not know how many active users I have. I cannot do A/B testing. I cannot track which features are popular. And for this project, that is the right trade-off.

MIT license. Not GPL, not AGPL, not Apache. MIT. The simplest, most permissive option. It removes all friction for contributors and for anyone who wants to learn from the code. At a FOSS conference, license choice matters, and I wanted to make it as easy as possible for people to use this work.

---

## SECTION G: CLOSING (~3 minutes)

### Slide 32 — Get Involved

The project is on GitHub at github.com/baijum/ukulele-companion.

If you play ukulele — or want to start — download the app from Google Play or the App Store and try it out.

If you write Kotlin, SwiftUI, or want to learn Kotlin Multiplatform, contributions are welcome. We particularly need help with translations — if you speak a language we do not support yet, adding a new locale is a well-documented process. We also welcome accessibility testing on different devices, new strumming and fingerpicking patterns, and documentation improvements.

There is also a free companion book called "The Complete Ukulele Learning Book" available as a PDF on archive.org. It covers the same material as the app's learning section but in a more traditional format.

### Slide 33 — Thank You

Thank you for your attention. This has been "From Chords To Code: Building A Ukulele Companion App."

I have a few minutes for questions. And if anyone wants to see the tuner or the chord detection in action, I have the app running on my phone and I would be happy to do a quick live demo.

---

## APPENDIX: Anticipated Q&A

**Q: Why Kotlin Multiplatform instead of Flutter or React Native?**
A: KMP lets you share logic while keeping native UI. The Compose and SwiftUI UIs feel native because they are native. Audio APIs, accessibility, and persistence are platform-specific by design. KMP does not try to abstract those away, which I think is the right call.

**Q: How accurate is the tuner?**
A: Within about 1-2 cents for sustained notes in a quiet environment. The YIN algorithm with parabolic interpolation gives sub-sample precision. The neural supervisor mostly helps with avoiding octave errors, not with precision.

**Q: How do you handle different ukulele tunings?**
A: Each tuning is defined as a data class with the four string frequencies. The fretboard explorer, chord library, voicing generator, and tuner all read from the same tuning configuration. Changing tunings is a single setting change that propagates everywhere.

**Q: Can I use the chord detection / pitch detection in my own project?**
A: Absolutely. The shared module is MIT-licensed and has zero platform dependencies. You could use PitchDetector, ChordDetector, or FFTProcessor in any Kotlin project — server-side, desktop, or mobile.

**Q: How do you keep Android and iOS in sync?**
A: Shared module changes land on both platforms automatically. For UI features, we have a feature parity tracking process and AI-assisted comparison between the two codebases. But honestly, it requires discipline and regular audits.

**Q: What about web / desktop support?**
A: The shared module already compiles to JVM, so a desktop app is feasible. Web would require Kotlin/JS or Kotlin/Wasm compilation. It is not on the roadmap, but the architecture would support it.

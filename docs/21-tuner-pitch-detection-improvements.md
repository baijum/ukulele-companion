# Tuner Pitch Detection Improvements

Detailed implementation plan for improving the tuner's pitch detection pipeline,
based on a comparative analysis of the current YIN implementation against
published research on real-time pitch estimation algorithms (de Cheveigné &
Kawahara 2002, McLeod 2005, Kim et al. 2018, Riou et al. 2025).

## Current State

The tuner uses a pure-Kotlin YIN algorithm (`PitchDetector.kt`) with all seven
canonical steps plus a confidence gate: Fast YIN (FFT-based Squared Difference
Function), Cumulative Mean Normalized Difference, Absolute Thresholding with
pitch continuity constraint, Confidence Rejection, Best Local Estimate (raw SDF
refinement), and Parabolic Interpolation on the raw SDF. Audio is captured via
`AudioCaptureEngine` at 44.1 kHz with 75% frame overlap (HOP_SIZE=1024, ~23 ms
between updates, ~43 fps). A 5-frame median filter in `TunerViewModel` smooths
the output before mapping to notes via `TunerNoteMapper`. Onset detection via
RMS energy derivative suppresses transient attack frames (~46 ms blanking).
Pitch continuity tracking constrains the lag search to ±2 semitones of the
previous frame, with automatic fallback to the full range on string changes.
Confidence-based opacity modulation in `TunerTab` fades the note label, needle,
and guidance text proportionally to detection quality. Chord detection in
`PitchMonitorViewModel` is throttled to every 4th frame to avoid unnecessary FFT
work at the higher frame rate.

**Strengths:** Complete 7-step Fast YIN pipeline (O(N log N) via FFT), 75%
overlapped circular buffer for smooth needle response (~43 fps), confidence
gating (threshold 0.30) rejects aperiodic attack frames, onset blanking
suppresses pluck transients (~46 ms), Best Local Estimate refines lag in raw
SDF for improved accuracy, pitch continuity tracking (±2 semitones) prevents
wild jumps during sustained notes, confidence-driven UI opacity gives visual
feedback on signal quality, appropriate CMND threshold (0.15), robust median
smoothing, well-suited for nylon/fluorocarbon ukulele strings (low
inharmonicity).

**Weaknesses:** (All identified weaknesses have been addressed in Phases 1-4.)

---

## Improvement 1: Transient Gating and Confidence Check [DONE — Phase 1]

**Priority:** High
**Impact:** Prevents "flash of wrong notes" when a string is plucked
**Files:** `PitchDetector.kt`, `TunerViewModel.kt`

### Problem

The initial pluck attack (20-50 ms) is dominated by broadband noise from the
pick/finger striking the string. During this phase the signal is non-periodic,
and YIN can find spurious dips in the CMNDF at random lags. The current
implementation only gates on RMS silence (threshold 0.01) — a loud pluck passes
the silence gate but produces unreliable pitch estimates.

### Design

Two complementary mechanisms:

#### A. Confidence Gating in PitchDetector

The CMNDF minimum value (`cmnd[bestTau]`) is already returned as `confidence` in
`PitchResult`. A low value (close to 0) indicates strong periodicity; a high
value indicates noise or aperiodicity.

**Change:** Add a confidence rejection threshold. If the CMNDF dip is not deep
enough, return `null` (no pitch) instead of a low-confidence result.

```kotlin
// In PitchDetector.detect(), after finding bestTau:
private const val CONFIDENCE_REJECT_THRESHOLD = 0.30

// After parabolic interpolation, before returning:
if (cmnd[bestTau] > CONFIDENCE_REJECT_THRESHOLD) return null
```

**Rationale:** During a clean sustain, `cmnd[bestTau]` is typically 0.01-0.10.
During attack transients it jumps to 0.3-0.8. A threshold of 0.30 rejects the
noisy attack frames while accepting all clean tonal frames.

**Tuning:** This value may need adjustment. Start at 0.30 and test with
aggressive strumming. If too many valid frames are rejected (sluggish onset),
raise to 0.35. If transient flashes persist, lower to 0.25.

#### B. Onset Detection via Energy Derivative in TunerViewModel

Track RMS energy across frames. When a sudden spike is detected (pluck onset),
suppress pitch updates for a configurable blanking period.

```kotlin
// New state in TunerViewModel:
private var previousRms: Float = 0f
private var blankingFramesRemaining: Int = 0

private const val ONSET_RATIO_THRESHOLD = 3.0  // RMS must triple to trigger
private const val BLANKING_FRAMES = 2          // suppress 2 frames (~186 ms at current rate)

// In processBuffer(), before calling PitchDetector:
val currentRms = rms(samples)
if (previousRms > 0 && currentRms / previousRms > ONSET_RATIO_THRESHOLD) {
    blankingFramesRemaining = BLANKING_FRAMES
}
previousRms = currentRms

if (blankingFramesRemaining > 0) {
    blankingFramesRemaining--
    return  // skip this frame
}
```

**Note:** The `rms()` function currently lives in `PitchDetector`. It should be
extracted to a shared utility or computed in `TunerViewModel` before calling
`detect()`, so we avoid computing it twice.

**Note:** Once frame overlap (Improvement 2) is implemented, `BLANKING_FRAMES`
should be recalculated based on the new hop size. At 75% overlap with a 1024-
sample hop, each frame is ~23 ms, so blanking 2 frames = ~46 ms (still in the
right range for attack suppression).

### Test Strategy

- Pluck each string aggressively with a pick; verify no transient flash of
  wrong notes on the display.
- Verify that the tuner locks onto the sustain phase within ~100 ms of pluck.
- Test with palm-muted plucks (very short attack, low sustain) — the tuner
  should show silence rather than a random note.
- Compare behavior with and without confidence gating on all four strings
  across all tunings.

---

## Improvement 2: Circular Buffer with Frame Overlap [DONE — Phase 2]

**Priority:** High
**Impact:** 4x more responsive needle movement (93 ms -> ~23 ms update rate)
**Files:** `AudioCaptureEngine.kt`, `TunerViewModel.kt`, `PitchMonitorViewModel.kt`

### Problem

The current implementation reads non-overlapping 4096-sample frames. Each frame
takes ~93 ms to fill, meaning the UI can only update ~10.7 times per second.
The research recommends 75% overlap for smooth visual feedback, which would
yield ~43 updates per second.

### Design

Replace the sequential read-and-emit model with a circular (ring) buffer that
continuously accumulates audio and emits overlapping analysis windows.

#### Ring Buffer Implementation

```kotlin
// In AudioCaptureEngine:
private const val HOP_SIZE = 1024          // 75% overlap with 4096 window
private const val READ_CHUNK = 1024        // read in hop-sized chunks

// Ring buffer state:
private val ringBuffer = FloatArray(FRAME_SIZE)  // 4096 samples
private var writePos = 0
private var samplesAccumulated = 0

// In the capture loop:
while (isActive) {
    val read = recorder.read(shortBuf, 0, READ_CHUNK)
    if (read > 0) {
        for (i in 0 until read) {
            ringBuffer[writePos] = shortBuf[i] * SHORT_TO_FLOAT
            writePos = (writePos + 1) % FRAME_SIZE
            samplesAccumulated++
        }

        // Once we have a full window, emit every HOP_SIZE samples
        if (samplesAccumulated >= FRAME_SIZE) {
            // Copy ring buffer to linear analysis buffer
            val analysisBuf = FloatArray(FRAME_SIZE)
            for (i in 0 until FRAME_SIZE) {
                analysisBuf[i] = ringBuffer[(writePos + i) % FRAME_SIZE]
            }
            onBuffer(analysisBuf)
            samplesAccumulated = FRAME_SIZE - HOP_SIZE  // retain overlap
        }
    }
}
```

#### Considerations

- **Memory:** One extra 4096-float array (~16 KB) — negligible.
- **CPU:** YIN will now run ~4x more often. This makes Improvement 4 (Fast YIN)
  more important. However, even without Fast YIN, the current O(N^2)
  implementation takes ~1-3 ms per frame on modern Android devices, so 4x is
  still well under budget at ~4-12 ms per 23 ms hop.
- **Thread safety:** The ring buffer write and analysis read happen on the same
  coroutine (Dispatchers.Default), so no synchronization is needed. If later
  moved to separate threads, a lock-free SPSC queue would be appropriate.
- **Backward compatibility:** The `onBuffer` callback signature stays the same
  (still receives `FloatArray` of size `FRAME_SIZE`). Downstream code
  (`PitchDetector`, `TunerViewModel`) needs no changes.

#### Frame Interval Update

Update `TunerViewModel.FRAME_INTERVAL_MS` to reflect the new hop size:

```kotlin
private const val FRAME_INTERVAL_MS = (
    HOP_SIZE * 1000L / AudioCaptureEngine.SAMPLE_RATE  // ~23 ms
)
```

This affects the in-tune hold timer calculation (`IN_TUNE_HOLD_MS / FRAME_INTERVAL_MS`).

### Test Strategy

- Measure actual update rate with a counter — should be ~43 fps.
- Visually compare needle smoothness before and after.
- Verify that pitch accuracy is unchanged (same window size, same algorithm).
- Profile CPU usage on a low-end device (e.g., Pixel 3a) to confirm headroom.
- Ensure the in-tune hold timer (2 seconds) still works correctly with the new
  frame interval.

---

## Improvement 3: Fast YIN via FFT [DONE — Phase 3]

**Priority:** Medium (becomes High if Improvement 2 is implemented)
**Impact:** ~5-10x reduction in pitch detection CPU cost
**Files:** `PitchDetector.kt`, `FFTProcessor.kt`

### Problem

The difference function is computed with nested loops — O(N * maxLag):

```kotlin
for (tau in 1..maxLag) {          // maxLag ≈ 678 (44100/65)
    var sum = 0.0f
    for (j in 0 until halfLen) {  // halfLen = 2048
        val delta = samples[j] - samples[j + tau]
        sum += delta * delta
    }
    diff[tau] = sum
}
```

This is ~1.39M multiply-accumulate operations per frame. With 75% overlap from
Improvement 2, that's ~60M operations per second.

### Design

The SDF can be decomposed as:

    d(tau) = E_x(0) + E_x(tau) - 2 * r(tau)

where:
- `E_x(0) = sum(x[j]^2)` for j in the reference window (constant per frame)
- `E_x(tau) = sum(x[j+tau]^2)` — sliding window energy, computable via prefix sum
- `r(tau) = sum(x[j] * x[j+tau])` — cross-correlation, computable via FFT

#### Step-by-step:

1. **Compute E_x(0):** Single pass over the reference window. O(N).

2. **Compute E_x(tau) via prefix sum:**
   ```kotlin
   val prefixSq = DoubleArray(samples.size + 1)
   for (i in samples.indices) {
       prefixSq[i + 1] = prefixSq[i] + samples[i].toDouble() * samples[i]
   }
   // E_x(tau) = prefixSq[tau + halfLen] - prefixSq[tau]
   ```
   O(N) to build, O(1) per query.

3. **Compute r(tau) via FFT:**
   Reuse the existing `FFTProcessor` (already has radix-2 Cooley-Tukey):
   ```kotlin
   // Zero-pad samples to next power of 2 >= 2*N to avoid circular artifacts
   val padded = zeroPad(samples, nextPow2(2 * samples.size))
   val spectrum = FFTProcessor.fft(padded)
   // Autocorrelation = IFFT(|FFT(x)|^2)
   val powerSpectrum = spectrum.map { re*re + im*im }
   val autocorr = FFTProcessor.ifft(powerSpectrum)
   ```
   O(N log N).

4. **Assemble SDF:**
   ```kotlin
   for (tau in 1..maxLag) {
       diff[tau] = (energy0 + energySlidingWindow(tau) - 2 * autocorr[tau]).toFloat()
   }
   ```
   O(maxLag).

#### FFTProcessor Changes

The existing `FFTProcessor` only computes forward FFT and magnitude spectrum. It
will need:

- An inverse FFT function (`ifft`) — straightforward: conjugate input, forward
  FFT, conjugate output, divide by N.
- Support for `DoubleArray` input (current implementation uses `FloatArray`). Or
  convert at boundaries.
- Zero-padding utility.

#### Considerations

- The FFT approach is faster for large windows but has overhead for small ones.
  With N=4096 and maxLag=678, FFT wins by a large margin.
- Numerical precision: FFT introduces floating-point rounding. For pitch
  detection this is negligible — the CMNDF normalization and parabolic
  interpolation are far more sensitive to signal quality than to 1e-7 rounding
  errors.
- The existing `FFTProcessor` is already used for chord detection, so this
  change unifies the DSP infrastructure.

### Test Strategy

- Unit test: Compare Fast YIN output against brute-force YIN for a set of
  synthetic test signals (pure sine, harmonics, noisy). Results should match
  within floating-point tolerance (<0.01 Hz).
- Benchmark: Measure wall-clock time per `detect()` call before and after on a
  target device. Expect 5-10x speedup.
- Profile memory: The FFT approach uses more temporary buffers. Verify no GC
  pressure on low-end devices.

---

## Improvement 4: YIN Step 5 — Best Local Estimate [DONE — Phase 1]

**Priority:** Medium
**Impact:** Small accuracy improvement (~0.1-0.5 cents)
**Files:** `PitchDetector.kt`

### Problem

The CMNDF normalization (cumulative mean division) can slightly shift the exact
location of the minimum relative to the raw difference function. The original
YIN paper's Step 5 corrects for this.

### Design

After finding `bestTau` via the CMNDF threshold search, refine the estimate
using the raw SDF:

```kotlin
// After Step 3 (absolute threshold), before Step 4 (interpolation):

// Step 5: Best Local Estimate — search vicinity in raw SDF
val searchRadius = 2  // +/- 2 samples around CMNDF minimum
var refinedBestTau = bestTau
var bestDiff = diff[bestTau]

for (candidate in (bestTau - searchRadius)..(bestTau + searchRadius)) {
    if (candidate in minLag..maxLag && diff[candidate] < bestDiff) {
        bestDiff = diff[candidate]
        refinedBestTau = candidate
    }
}

// Step 4: Parabolic interpolation on the RAW SDF (not CMNDF)
val refinedTau = parabolicInterpolation(diff, refinedBestTau, maxLag)
```

**Key change:** Parabolic interpolation now operates on the `diff` array (raw
SDF) rather than `cmnd` (CMNDF). The raw SDF minimum is the true least-squares
optimal lag.

### Considerations

- This is a subtle improvement. For most ukulele signals the CMNDF and SDF
  minima are within 0.1-0.5 samples of each other.
- The confidence value should still come from `cmnd[bestTau]` (the CMNDF), as
  it provides a normalized 0-1 confidence metric. Only the lag refinement uses
  the raw SDF.
- No additional computation — just a 5-iteration loop and reusing the existing
  `diff` array.

### Test Strategy

- Synthetic test: Generate a known-frequency sine wave, detect with and without
  Step 5, compare error in cents. Step 5 should reduce error.
- A/B test with real ukulele audio: Record samples, run both versions, compare
  standard deviation of detected frequency over sustained notes.

---

## Improvement 5: Pitch Continuity Tracking [DONE — Phase 4]

**Priority:** Low-Medium
**Impact:** Reduces occasional pitch jumps during sustained notes
**Files:** `PitchDetector.kt` or `TunerViewModel.kt`

### Problem

Each frame is analyzed independently with the full lag search range (minLag to
maxLag, corresponding to 65-1100 Hz). During a sustained note, the pitch changes
very slowly (if at all), but a noise spike or amplitude modulation could cause
YIN to find a spurious dip far from the true pitch.

### Design

Track the previous frame's detected lag and constrain the search range in
subsequent frames:

```kotlin
// In TunerViewModel (stateful tracking):
private var previousLag: Double? = null

private const val MAX_SEMITONE_JUMP = 2.0  // allow up to 2 semitones between frames
private const val CONTINUITY_FACTOR = 2.0.pow(MAX_SEMITONE_JUMP / 12.0)
// CONTINUITY_FACTOR ≈ 1.122 (12.2% deviation)

// When calling PitchDetector, pass constrained range:
val constrainedMinFreq = if (previousLag != null) {
    (sampleRate / previousLag!!) / CONTINUITY_FACTOR
} else MIN_FREQUENCY

val constrainedMaxFreq = if (previousLag != null) {
    (sampleRate / previousLag!!) * CONTINUITY_FACTOR
} else MAX_FREQUENCY
```

#### Alternative: Implement in PitchDetector Directly

Add optional `previousFrequency` parameter to `detect()`:

```kotlin
fun detect(
    samples: FloatArray,
    sampleRate: Int,
    threshold: Double = DEFAULT_THRESHOLD,
    previousFrequency: Double? = null,  // NEW
): PitchResult?
```

When `previousFrequency` is provided, narrow `minLag` and `maxLag` accordingly.
If the constrained search finds no valid dip, fall back to the full range
(handles string changes and new notes).

### Considerations

- Must handle string changes gracefully. When the user moves to a different
  string, the pitch jumps by a large interval. The fallback to full-range
  search handles this.
- Must handle silence gaps. When `previousFrequency` is stale (after silence),
  clear it and use the full range.
- This optimization also reduces computation: fewer lags to evaluate.
- Should be combined with the median smoother — continuity tracking prevents
  outliers from entering the median buffer in the first place.

### Test Strategy

- Play a sustained note and introduce a brief noise burst (tap the body).
  Without continuity tracking, the needle may jump. With it, it should remain
  stable.
- Rapidly switch between two strings. Verify the tuner still locks onto the new
  string within 2-3 frames (the fallback should fire).
- Test edge case: bend a string while tuning. The pitch change should be smooth
  and tracked correctly within the continuity window.

---

## Improvement 6: Confidence-Based UI Feedback [DONE — Phase 4]

**Priority:** Low
**Impact:** Polish — gives users visual feedback on detection reliability
**Files:** `ui/TunerTab.kt`, `TunerViewModel.kt`

### Problem

The `confidence` value (CMNDF minimum, where lower = better) is computed and
stored in `TunerUiState` but never used in the UI. When the signal is weak or
noisy, the tuner shows the same fully-opaque needle and note label as when the
signal is clean, giving the user no indication of reliability.

### Design

#### A. Needle Opacity

Map confidence to needle alpha:

```kotlin
// In TunerTab.kt, where the needle is drawn:
// confidence ranges from 0.0 (perfect) to ~0.15 (threshold)
// Map to alpha: 0.0 -> 1.0 (fully opaque), 0.15 -> 0.7 (slightly faded)
val needleAlpha = (1.0 - (confidence / 0.15) * 0.3).coerceIn(0.4, 1.0)
```

#### B. Note Label Opacity

Apply the same alpha to the detected note text:

```kotlin
Text(
    text = detectedNote,
    color = noteColor.copy(alpha = needleAlpha.toFloat()),
    // ...
)
```

#### C. Optional: Confidence Indicator

Add a small text or icon below the note label showing signal quality:

```kotlin
val signalQuality = when {
    confidence < 0.05 -> "Strong"
    confidence < 0.10 -> "Good"
    confidence < 0.15 -> "Weak"
    else -> ""  // should not reach here due to gating
}
```

### Considerations

- Keep it subtle. Musicians want to focus on the needle position, not a
  confidence meter. Opacity modulation is less distracting than an additional
  UI element.
- This pairs well with Improvement 1 (confidence gating). Frames with
  confidence > 0.30 are rejected entirely; frames between 0.15-0.30 are shown
  but dimmed; frames below 0.15 are shown at full opacity.
- Accessibility: Ensure the minimum alpha (0.4) still meets contrast
  requirements for visually impaired users.

### Test Strategy

- Pluck a string and observe the needle opacity during attack (should fade)
  and sustain (should be fully opaque).
- Gradually mute a ringing string. The display should fade as the note decays
  into noise, rather than showing a confident-looking wrong note.
- Verify the experience doesn't feel "flickery" — the 120 ms animation tween
  on the needle should naturally smooth any rapid alpha changes.

---

## Implementation Order

The improvements have the following dependency relationships:

```
  1. Transient Gating        (standalone, do first)
  |
  2. Frame Overlap            (standalone, do second)
  |
  3. Fast YIN via FFT         (benefits from 2, but independent)
  |
  4. Best Local Estimate      (standalone, easy addition)
  |
  5. Pitch Continuity         (benefits from 1 and 2 being done)
  |
  6. Confidence UI Feedback   (benefits from 1, pairs with gating)
```

**Suggested phases:**

- **Phase 1 (Quick wins) [DONE]:** Improvements 1 and 4 — confidence gating
  (`CONFIDENCE_REJECT_THRESHOLD = 0.30`), onset detection via RMS energy
  derivative (`ONSET_RATIO_THRESHOLD = 3.0`, `BLANKING_FRAMES = 2`), Best
  Local Estimate (±2 sample search in raw SDF), parabolic interpolation on
  raw SDF, and `rms()` exposed as public API on `PitchDetector`.

- **Phase 2 (Responsiveness) [DONE]:** Improvement 2 — circular ring buffer
  with `HOP_SIZE = 1024` (75% overlap, ~23 ms / ~43 fps). `FRAME_INTERVAL_MS`
  updated in `TunerViewModel`; `BLANKING_FRAMES` now blanks ~46 ms.
  `PitchMonitorViewModel` adjusted: `CHORD_HOLD_FRAMES` 3→12 to maintain
  ~280 ms temporal smoothing, chord detection throttled to every 4th frame
  via `CHORD_DETECTION_INTERVAL`.

- **Phase 3 (Performance) [DONE]:** Improvement 3 — Fast YIN via FFT.
  Added `ifft()` to `FFTProcessor`. `PitchDetector` now computes the SDF as
  `d(tau) = E0 + E(tau) - 2*r(tau)` where `r(tau)` uses
  `IFFT(conj(FFT(ref)) * FFT(sig))` (O(N log N)) and energy terms use a
  prefix-sum of squares (O(N)). Brute-force O(N * maxLag) loop removed.

- **Phase 4 (Polish) [DONE]:** Improvements 5 and 6.
  `PitchDetector.detect()` now accepts an optional `previousFrequency`
  parameter. When provided, the CMND threshold search is first constrained
  to lags within ±2 semitones (`MAX_SEMITONE_JUMP = 2.0`,
  `CONTINUITY_FACTOR ≈ 1.1225`). If the constrained window yields no valid
  dip (e.g. string change), the search falls back to the full range. The
  threshold search logic was extracted into a reusable `findFirstDipBelow()`
  helper. `TunerViewModel` tracks `previousFrequency` across frames (reset
  on silence, start, and stop).
  For UI feedback, `TunerTab` computes an animated `confidenceAlpha` from
  the YIN confidence: `(1.0 - confidence * 2).coerceIn(0.4, 1.0)`. This
  alpha is applied to the note label, guidance text, and the needle/pivot
  dot in `NeedleMeter`, so borderline detections appear dimmer and confident
  readings appear fully opaque.

---

## References

- de Cheveigné, A. & Kawahara, H. (2002). "YIN, a fundamental frequency
  estimator for speech and music." JASA 111(4), 1917-1930.
- McLeod, P. & Wyvill, G. (2005). "A Smarter Way to Find Pitch." ICMC.
- Kim, J.W. et al. (2018). "CREPE: A Convolutional Representation for Pitch
  Estimation." ICASSP.
- Riou, D. et al. (2025). "SwiftF0: A Lightweight Pitch Estimator."
- Mauch, M. & Dixon, S. (2014). "pYIN: A Fundamental Frequency Estimator
  Using Probabilistic Threshold Distributions." ICASSP.

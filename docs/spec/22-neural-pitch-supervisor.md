# Neural Pitch Supervisor — SwiftF0 Integration

Future enhancement plan for adding a lightweight neural pitch estimator
(SwiftF0) as a supervisory layer alongside the existing Fast YIN tuner pipeline.
This document describes the hybrid architecture, integration design, alternative
approaches, and trigger criteria for when to pursue this work.

**Status:** Potential future enhancement — not yet scheduled.

## Motivation

The current tuner pipeline (documented in `docs/spec/21-tuner-pitch-detection-improvements.md`)
implements a complete 7-step Fast YIN algorithm with confidence gating, onset
blanking, pitch continuity tracking, and confidence-based UI feedback. This
pipeline performs well for typical practice environments.

However, purely heuristic pitch detection has inherent limitations:

- **Noise robustness:** YIN accuracy degrades below ~10 dB SNR. In noisy
  environments (stage performance, fans, background conversation) the
  confidence gate will reject most frames, producing a sluggish or unresponsive
  tuner rather than a wrong one — but neither outcome is ideal.

- **Octave errors on low strings:** Baritone ukulele D3 (~146.8 Hz) and low-G
  tunings produce signals where sub-harmonics can occasionally create a deeper
  CMNDF dip than the true fundamental, especially during amplitude modulation
  in the decay phase. The continuity tracker mitigates this but cannot
  eliminate it.

- **Attack phase detection:** The onset blanking suppresses the chaotic attack
  transient but also introduces a ~46 ms dead zone. A neural estimator trained
  on plucked-string audio could potentially identify the correct pitch even
  during the attack, reducing perceived latency.

The research literature (Nieradzik 2025, Kim et al. 2018) recommends a hybrid
architecture: a classical estimator for low-latency, high-precision tracking
backed by a neural network for noise robustness and octave error correction.

---

## SwiftF0 Overview

SwiftF0 (Nieradzik 2025) is a lightweight monophonic pitch detector designed
to bridge the gap between YIN's speed and CREPE's robustness.

| Property            | Value                                  |
|---------------------|----------------------------------------|
| Architecture        | 2D Convolutional Neural Network        |
| Input               | Raw audio at 16 kHz (model computes STFT internally) |
| STFT window         | 1024 samples                           |
| STFT hop            | 256 samples (16 ms at 16 kHz)          |
| Output              | Per-frame pitch (Hz) + confidence (0-1)|
| Frequency range     | 46.875 Hz - 2093.75 Hz (G1 to C7)     |
| Parameters          | ~95K                                   |
| Model format        | ONNX                                   |
| Model file size     | ~398 KB                                |
| License             | MIT                                    |
| Inference speed     | 132 ms for 5 seconds of audio on CPU   |
| Repository          | [lars76/swift-f0](https://github.com/lars76/swift-f0) |
| Paper               | arXiv:2508.18440                       |

**Key advantage over CREPE:** SwiftF0 has ~95K parameters vs CREPE's 22M,
making it viable for mobile devices without a dedicated GPU. In the
[Pitch Detection Benchmark](https://github.com/lars76/pitch-benchmark/),
SwiftF0 outperforms CREPE in both speed and accuracy.

**Frequency coverage for ukulele:** The model's range (46.875-2093.75 Hz)
fully covers all ukulele tunings including baritone. Our current YIN range
(65-1100 Hz) is a subset.

---

## Architecture — Hybrid Supervisor Pattern

The integration follows a supervisor pattern where Fast YIN remains the
primary estimator and SwiftF0 acts as a periodic "second opinion."

```
Audio Frame (~23 ms)
       |
       v
  +-----------+         +-----------------+
  | Fast YIN  |         | SwiftF0 (ONNX)  |
  | (every    |         | (every ~100 ms) |
  | frame)    |         |                 |
  +-----------+         +-----------------+
       |                       |
       v                       v
  YIN result              Neural result
  (freq, conf)            (freq, conf)
       |                       |
       +----------+------------+
                  |
                  v
         +----------------+
         | Arbitration    |
         | Logic          |
         +----------------+
                  |
                  v
           Final pitch
           → UI update
```

### Design Principles

1. **YIN stays primary.** It runs on every frame (~43 fps) and drives the
   needle directly. The user sees YIN's output most of the time. This ensures
   the low-latency, fluid needle movement musicians expect.

2. **SwiftF0 runs intermittently.** Neural inference runs every N frames
   (approximately every 100 ms, i.e., every 4-5 frames at the current
   ~23 ms hop rate). This limits CPU overhead to one inference per ~100 ms
   while still catching errors quickly.

3. **Arbitration, not replacement.** The arbitration logic combines both
   signals rather than choosing one. This avoids introducing neural inference
   latency into the display update loop.

### Arbitration Rules

The arbitration logic in `TunerViewModel` decides the final pitch output:

```kotlin
// Pseudocode — arbitration logic
fun arbitrate(
    yinResult: PitchResult?,
    neuralResult: NeuralPitchResult?,  // may be null on non-supervisor frames
): PitchResult? {
    if (yinResult == null) return null  // silence — trust YIN's silence gate

    if (neuralResult == null) return yinResult  // no neural result this frame

    // Case 1: Both agree (within 1 semitone) — use YIN (higher precision)
    if (withinSemitones(yinResult.frequencyHz, neuralResult.frequencyHz, 1.0)) {
        return yinResult
    }

    // Case 2: Octave relationship — trust SwiftF0 (trained to avoid octave errors)
    if (isOctaveRelation(yinResult.frequencyHz, neuralResult.frequencyHz)) {
        return if (neuralResult.confidence > 0.8) {
            yinResult.copy(frequencyHz = neuralResult.frequencyHz)
        } else {
            yinResult  // not confident enough to override
        }
    }

    // Case 3: Large disagreement — trust the more confident one,
    // but only override YIN if neural confidence is high
    return if (neuralResult.confidence > 0.9 && yinResult.confidence > 0.20) {
        yinResult.copy(frequencyHz = neuralResult.frequencyHz)
    } else {
        yinResult
    }
}
```

---

## Integration Design

### A. Model Bundling

The ONNX model file (`model.onnx`, ~398 KB) is extracted from the SwiftF0 pip
package and placed in the Android assets directory:

```
app/src/main/assets/swift_f0_model.onnx
```

This adds ~398 KB to the APK. With compression in the final APK/AAB, the
actual impact is smaller.

### B. Dependency — ONNX Runtime for Android

Add the ONNX Runtime Android library:

```kotlin
// In app/build.gradle.kts dependencies:
implementation("com.microsoft.onnxruntime:onnxruntime-android:latest.release")
```

**Size impact:** The ONNX Runtime AAR adds ~8-15 MB to the APK due to native
`.so` files for each supported ABI (arm64-v8a, armeabi-v7a, x86_64). This is
the single largest cost of this enhancement.

**ABI filtering** can reduce this if only ARM is targeted:

```kotlin
// In android.defaultConfig:
ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a")
}
```

This drops x86/x86_64 support (emulators only) and saves ~3-5 MB.

### C. Preprocessing Pipeline — Resampling

SwiftF0 expects mono audio at 16 kHz. Our `AudioCaptureEngine` captures at
44.1 kHz. A resampling step is required.

```kotlin
// Downsample 44100 → 16000 (ratio ≈ 2.75625)
// Use a simple sinc-interpolation or polyphase filter.

object AudioResampler {
    private const val SOURCE_RATE = 44100
    private const val TARGET_RATE = 16000

    /**
     * Downsamples a 44.1 kHz float buffer to 16 kHz.
     *
     * Uses linear interpolation — acceptable for pitch estimation where
     * spectral fidelity above 8 kHz is irrelevant (ukulele fundamentals
     * are below 1.1 kHz). A low-pass anti-aliasing filter at 7.5 kHz
     * should precede this in production.
     */
    fun downsample(input: FloatArray): FloatArray {
        val ratio = SOURCE_RATE.toDouble() / TARGET_RATE
        val outLen = (input.size / ratio).toInt()
        val output = FloatArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i * ratio
            val idx = srcPos.toInt()
            val frac = (srcPos - idx).toFloat()
            output[i] = if (idx + 1 < input.size) {
                input[idx] * (1f - frac) + input[idx + 1] * frac
            } else {
                input[idx]
            }
        }
        return output
    }
}
```

**Note:** The ONNX model computes its own STFT internally. Unlike the
initial analysis suggested, we do NOT need to compute the STFT on the Android
side — the model takes raw waveform as input shaped as `[1, numSamples]`.

### D. NeuralPitchSupervisor Class

A new domain class that wraps ONNX inference:

**File:** `app/src/main/java/com/baijum/ukufretboard/domain/NeuralPitchSupervisor.kt`

```kotlin
data class NeuralPitchResult(
    val frequencyHz: Float,
    val confidence: Float,
)

/**
 * Lightweight wrapper around the SwiftF0 ONNX model for neural pitch
 * estimation. Designed to run intermittently (~100 ms) alongside the
 * primary Fast YIN pipeline as an octave-error supervisor.
 *
 * Thread safety: This class is NOT thread-safe. Callers must ensure
 * sequential access (e.g. from a single coroutine on Dispatchers.Default).
 */
class NeuralPitchSupervisor(context: Context) {

    private val session: OrtSession
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    init {
        val modelBytes = context.assets.open("swift_f0_model.onnx").readBytes()
        val opts = OrtSession.SessionOptions().apply {
            setInterOpNumThreads(1)
            setIntraOpNumThreads(1)
        }
        session = env.createSession(modelBytes, opts)
    }

    /**
     * Runs neural pitch estimation on the given 44.1 kHz audio buffer.
     *
     * Internally downsamples to 16 kHz, runs the ONNX model, and returns
     * the pitch and confidence for the middle frame of the buffer.
     *
     * @return A [NeuralPitchResult] or null if the model outputs no
     *   confident pitch (e.g. silence or noise).
     */
    fun estimate(samples: FloatArray): NeuralPitchResult? {
        val resampled = AudioResampler.downsample(samples)
        if (resampled.isEmpty()) return null

        // Model input: [1, numSamples]
        val tensor = OnnxTensor.createTensor(
            env,
            arrayOf(resampled),  // shape: [1, N]
        )

        val outputs = session.run(mapOf("input" to tensor))

        // Model outputs: [0] = pitch_hz [1, frames], [1] = confidence [1, frames]
        val pitchArray = (outputs[0].value as Array<FloatArray>)[0]
        val confArray = (outputs[1].value as Array<FloatArray>)[0]

        if (pitchArray.isEmpty()) return null

        // Take the middle frame as the representative estimate
        val midIdx = pitchArray.size / 2
        val freq = pitchArray[midIdx]
        val conf = confArray[midIdx]

        tensor.close()
        outputs.forEach { it.value.close() }

        return if (conf > 0.5f && freq in 46.875f..2093.75f) {
            NeuralPitchResult(freq, conf)
        } else null
    }

    fun close() {
        session.close()
    }
}
```

### E. Wiring into TunerViewModel

The supervisor runs alongside the existing pipeline, gated by a frame counter:

```kotlin
// New state in TunerViewModel:
private var neuralSupervisor: NeuralPitchSupervisor? = null
private var neuralFrameCounter = 0
private var lastNeuralResult: NeuralPitchResult? = null

private companion object {
    /** Run neural inference every 5th frame (~115 ms at 23 ms/frame). */
    const val NEURAL_SUPERVISOR_INTERVAL = 5
}

// In processBuffer(), after PitchDetector.detect():
if (neuralSupervisor != null && neuralFrameCounter++ % NEURAL_SUPERVISOR_INTERVAL == 0) {
    lastNeuralResult = neuralSupervisor!!.estimate(samples)
}

val finalResult = arbitrate(result, lastNeuralResult)
// ... continue with finalResult instead of result ...
```

### F. Feature Toggle

The neural supervisor should be opt-in via settings, since it increases APK
size and CPU usage:

```kotlin
// In AppSettings:
val neuralPitchSupervisor: Boolean  // default = false
```

When disabled, `neuralSupervisor` stays null and no ONNX inference runs. The
model is still bundled in the APK (assets cannot be conditionally excluded at
runtime), but no memory is allocated for the ONNX session.

---

## Alternative: TensorFlow Lite Conversion

Instead of ONNX Runtime, the model can be converted to TensorFlow Lite format.

**Conversion pipeline:**

```bash
pip install onnx2tf tf2onnx tensorflow
onnx2tf -i model.onnx -o tflite_model/
```

**Advantages:**
- TFLite runtime on Android is smaller (~3-5 MB vs ~8-15 MB for ONNX Runtime).
- Better ecosystem support on Android (Google's own library).
- Potential for GPU delegate or NNAPI delegate acceleration.
- More mature ProGuard/R8 integration.

**Disadvantages:**
- Conversion may introduce numerical differences. Model accuracy must be
  re-validated after conversion.
- ONNX-to-TFLite conversion is not always lossless for all operator types.
  The SwiftF0 model uses standard Conv2D and Dense layers, so conversion
  should be straightforward, but requires testing.
- Adds a dependency on `org.tensorflow:tensorflow-lite` instead.

**Recommendation:** Start with ONNX Runtime for compatibility with the
upstream model, then evaluate TFLite conversion if APK size becomes a concern.

---

## Alternative: Pure-Kotlin CNN Inference

At only ~95K parameters, the SwiftF0 model is small enough that hand-written
inference in pure Kotlin is feasible, eliminating the native dependency
entirely.

**What this entails:**

1. Parse the ONNX model to extract layer weights, biases, and topology.
2. Implement 2D convolution, batch normalization, ReLU, and fully-connected
   layers in Kotlin.
3. Implement the STFT preprocessing (1024-sample window, 256-sample hop,
   Hanning window, magnitude spectrum, frequency bin pruning).
4. Load weights from a custom binary format bundled in assets.

**Advantages:**
- Zero native dependencies. APK size increase is only the weight file (~398 KB)
  plus a few hundred lines of Kotlin.
- Full control over numerical precision and memory allocation.
- No ProGuard/R8 complications.
- Consistent with the project's current pure-Kotlin philosophy.

**Disadvantages:**
- Significant development effort (~2-4 weeks for a correct, tested
  implementation).
- Must implement the STFT from scratch (our existing `FFTProcessor` does
  forward FFT but not windowed STFT with the exact parameters SwiftF0 expects).
- Performance may be slower than ONNX Runtime's optimized C++ kernels,
  especially for the convolution layers. However, at ~95K parameters and
  running only every ~100 ms, this is likely still under budget.
- Maintenance burden: if SwiftF0 releases a new model, the inference engine
  must be updated manually.

**Recommendation:** Consider this approach only if the zero-dependency
constraint is firm and APK size is a hard requirement. The ONNX Runtime
approach is significantly less effort and more maintainable.

---

## APK Size and Performance Budget

### Size Impact

| Component                    | Approximate Size |
|------------------------------|------------------|
| ONNX model file (assets)    | ~398 KB          |
| ONNX Runtime AAR (arm64)    | ~8-10 MB         |
| ONNX Runtime AAR (armeabi)  | ~5-7 MB          |
| ONNX Runtime AAR (x86_64)   | ~5-7 MB          |
| Resampler + Supervisor code | ~2 KB            |
| **Total (ARM only)**         | **~13-17 MB**    |
| **Total (all ABIs)**         | **~18-25 MB**    |

For context, the current APK is pure Kotlin with no native libraries. Adding
ONNX Runtime roughly doubles the download size. Using Android App Bundles
(AAB) mitigates this: Google Play delivers only the ABI matching the user's
device, so actual download increase is ~8-10 MB.

### Performance Budget

| Metric                     | Target              | Notes                          |
|----------------------------|---------------------|--------------------------------|
| Neural inference latency   | < 10 ms             | Per invocation on mid-range device |
| Invocation cadence         | Every ~100 ms       | 1 in 5 frames at ~23 ms/frame |
| CPU overhead               | < 5% additional     | Over baseline YIN pipeline     |
| Memory (ONNX session)      | < 5 MB              | Model + runtime buffers        |
| Resampling latency         | < 1 ms              | 4096 samples → ~1486 samples   |

SwiftF0's published benchmark: 132 ms for 5 seconds of audio on CPU. That is
~26.4 ms per second of audio, or roughly 0.42 ms per 16 ms frame. Even on a
mid-range Android device with 2-3x overhead, individual inference should
complete well within the 10 ms budget.

---

## Considerations

### Resampling Quality

Linear interpolation (shown in the design above) is acceptable for a first
implementation because ukulele fundamentals are well below the 8 kHz Nyquist
limit of 16 kHz. However, aliasing from harmonics above 8 kHz could
introduce artifacts. A production implementation should add a low-pass
anti-aliasing filter (e.g., a simple FIR at 7.5 kHz cutoff) before
downsampling.

### Thread Scheduling

Neural inference should NOT run on the audio capture thread. The existing
architecture already processes audio on `Dispatchers.Default`, so ONNX
inference runs on the same coroutine as YIN — this is acceptable because
inference is fast (~1-5 ms) and runs only every ~100 ms. If profiling reveals
contention, inference can be moved to a dedicated single-thread dispatcher.

### Graceful Degradation

If the ONNX model fails to load (missing asset, unsupported device, OOM),
the supervisor should be silently disabled and the tuner should fall back to
pure YIN. No user-visible error should appear.

```kotlin
neuralSupervisor = try {
    NeuralPitchSupervisor(context)
} catch (e: Exception) {
    Log.w("TunerViewModel", "Neural supervisor unavailable: ${e.message}")
    null
}
```

### ProGuard / R8 Rules

ONNX Runtime uses JNI and reflection internally. Add keep rules:

```proguard
# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
```

### Model Updates

The SwiftF0 repository (lars76/swift-f0) is a research project with limited
commit history. If the upstream model is updated, the ONNX file in assets must
be replaced manually. Pin to a specific version (v0.1.2 as of this writing)
and document the provenance.

---

## Phased Rollout

### Phase A: Model Extraction and Standalone Benchmark

**Goal:** Validate that the ONNX model runs correctly on Android and measure
inference performance.

**Tasks:**
1. Extract `model.onnx` from the SwiftF0 pip package (v0.1.2).
2. Place in `app/src/main/assets/swift_f0_model.onnx`.
3. Add ONNX Runtime dependency to `build.gradle.kts`.
4. Create a minimal `NeuralPitchSupervisor` class that loads the model and
   runs inference on a test buffer.
5. Write an instrumented test that feeds known-frequency sine waves and
   verifies the output pitch matches within 1 Hz.
6. Benchmark inference time on a physical device (target: < 10 ms).

**Files:** `build.gradle.kts`, `NeuralPitchSupervisor.kt`, new test file.

### Phase B: Preprocessing Pipeline

**Goal:** Build the resampling infrastructure and verify end-to-end accuracy.

**Tasks:**
1. Implement `AudioResampler.downsample()` (44.1 kHz to 16 kHz).
2. Optionally add a simple FIR anti-aliasing filter.
3. Wire resampling into `NeuralPitchSupervisor.estimate()`.
4. Test with real ukulele recordings: compare SwiftF0's output against YIN
   on the same audio and verify agreement on clean signals.

**Files:** `AudioResampler.kt`, `NeuralPitchSupervisor.kt`.

### Phase C: Arbitration Logic

**Goal:** Integrate the supervisor into the tuner pipeline with arbitration.

**Tasks:**
1. Add `neuralSupervisor`, `neuralFrameCounter`, and `lastNeuralResult` state
   to `TunerViewModel`.
2. Implement the `arbitrate()` function (see Arbitration Rules above).
3. Add frame-counter gating (`NEURAL_SUPERVISOR_INTERVAL = 5`).
4. Test octave-error correction: artificially inject octave errors in YIN
   output and verify the supervisor corrects them.
5. Profile CPU and memory on a low-end device (e.g., Pixel 3a).

**Files:** `TunerViewModel.kt`.

### Phase D: Feature Toggle and Polish

**Goal:** Make the feature user-configurable and production-ready.

**Tasks:**
1. Add `neuralPitchSupervisor` boolean to `AppSettings`.
2. Add a toggle in the settings UI with explanatory text ("Uses more battery
   but improves accuracy in noisy environments").
3. Ensure graceful degradation if model loading fails.
4. Add ProGuard rules for ONNX Runtime.
5. Update `docs/spec/21-tuner-pitch-detection-improvements.md` to reference this
   document as the next step.
6. A/B test with beta users: compare pitch stability with and without the
   neural supervisor in noisy environments.

**Files:** `AppSettings.kt`, settings UI, `proguard-rules.pro`.

---

## When to Pursue This

This enhancement is **not currently scheduled**. Trigger criteria for
prioritising it:

- **User feedback:** Reports of poor tuner performance in noisy environments
  (stage, rehearsal rooms, outdoor busking).
- **Competitive pressure:** Other ukulele/guitar tuner apps ship neural-backed
  pitch detection with demonstrably better noise robustness.
- **Use case expansion:** If the app targets professional or live-performance
  users who need reliable tuning in challenging acoustic conditions.
- **APK size tolerance:** If the app already adds other native dependencies
  (e.g., audio effects, Bluetooth MIDI) that normalize the ~10 MB ONNX
  Runtime overhead.

Until these triggers are met, the current Fast YIN pipeline with all four
phases of improvements provides excellent accuracy and responsiveness for
typical ukulele practice environments.

---

## References

- Nieradzik, L. (2025). "SwiftF0: Fast and Accurate Monophonic Pitch
  Detection." arXiv:2508.18440.
  Repository: https://github.com/lars76/swift-f0
- Kim, J.W. et al. (2018). "CREPE: A Convolutional Representation for Pitch
  Estimation." ICASSP.
- de Cheveigné, A. & Kawahara, H. (2002). "YIN, a fundamental frequency
  estimator for speech and music." JASA 111(4), 1917-1930.
- ONNX Runtime for Android:
  https://onnxruntime.ai/docs/get-started/with-java.html
- TensorFlow Lite for Android:
  https://www.tensorflow.org/lite/guide/android

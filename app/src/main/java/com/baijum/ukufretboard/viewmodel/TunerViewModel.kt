package com.baijum.ukufretboard.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.audio.AudioCaptureEngine
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.domain.NeuralPitchResult
import com.baijum.ukufretboard.domain.NeuralPitchSupervisor
import com.baijum.ukufretboard.domain.NoteInfo
import com.baijum.ukufretboard.domain.PitchDetector
import com.baijum.ukufretboard.domain.PitchResult
import com.baijum.ukufretboard.domain.StringMatch
import com.baijum.ukufretboard.domain.TunerNoteMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log2

/**
 * Tuning accuracy status shown to the user.
 */
enum class TuningStatus {
    /** No sound detected. */
    SILENT,
    /** Detected note is more than [TunerViewModel.CLOSE_CENTS] flat. */
    FLAT,
    /** Detected note is more than [TunerViewModel.CLOSE_CENTS] sharp. */
    SHARP,
    /** Detected note is within [TunerViewModel.IN_TUNE_CENTS] of the target. */
    IN_TUNE,
    /** Detected note is close but not yet in tune. */
    CLOSE,
}

enum class NeuralRuntimeStatus {
    LOADING,
    ACTIVE,
    FALLBACK,
}

/**
 * UI state for the Tuner screen.
 */
data class TunerUiState(
    /** Whether the microphone is actively capturing audio. */
    val isListening: Boolean = false,
    /** Display name of the detected note (e.g. "A4"), or null if silent. */
    val detectedNote: String? = null,
    /** Cents deviation from the nearest note (−50 .. +50). */
    val centsDeviation: Double = 0.0,
    /** Display-only smoothed cents for calmer needle movement. */
    val displayCentsDeviation: Double = 0.0,
    /** Detection confidence (lower = more confident in YIN terms). */
    val confidence: Double = 1.0,
    /** Current tuning accuracy status. */
    val tuningStatus: TuningStatus = TuningStatus.SILENT,
    /** The nearest string in the current tuning, if any. */
    val targetString: StringMatch? = null,
    /** Per-string progress — true when a string has been successfully tuned. */
    val stringProgress: List<Boolean> = listOf(false, false, false, false),
    /** The underlying [NoteInfo] for downstream consumers (e.g. reference tone). */
    val noteInfo: NoteInfo? = null,
    /** Whether SwiftF0 session/model is available on this device/build. */
    val isNeuralAvailable: Boolean = false,
    /** Whether SwiftF0 is actively producing usable runtime estimates. */
    val isNeuralActive: Boolean = false,
    /** High-level status shown in the permanent tuner badge. */
    val neuralRuntimeStatus: NeuralRuntimeStatus = NeuralRuntimeStatus.LOADING,
    /** Index of the string that auto-advance is suggesting the user tune next, or -1 if none. */
    val autoAdvanceTarget: Int = -1,
    /** Note name that was sustained long enough before silence (e.g. "A4"), or null. */
    val lastSettledNote: String? = null,
    /** Cents deviation when the settled note was last updated; drives the frozen needle. */
    val lastSettledCents: Double = 0.0,
)

/**
 * ViewModel for the Tuner feature.
 *
 * Orchestrates the pipeline:
 * [AudioCaptureEngine] → [PitchDetector] → [TunerNoteMapper] → [TunerUiState].
 *
 * Applies a rolling-median smoothing window to reduce jitter and requires an
 * in-tune reading to be sustained for [IN_TUNE_HOLD_MS] before marking a
 * string as completed.
 */
class TunerViewModel : ViewModel() {

    companion object {
        /** Default cents threshold for "in tune" (standard mode). */
        const val IN_TUNE_CENTS = 6.0

        /** Cents threshold for "in tune" in precision mode. */
        const val PRECISION_IN_TUNE_CENTS = 2.0

        /** Cents threshold for "close" (between close and flat/sharp). */
        const val CLOSE_CENTS = 15.0

        /** Smoothing factor for display-only cents damping (0..1). */
        private const val DISPLAY_CENTS_ALPHA = 0.30

        /** Suppress tiny centre jitter for the needle UI. */
        private const val DISPLAY_DEADBAND_CENTS = 0.8

        /** How many recent readings to keep for smoothing. */
        private const val SMOOTHING_WINDOW = 5

        /** Milliseconds a string must stay in-tune before it's marked done. */
        private const val IN_TUNE_HOLD_MS = 1400L

        /**
         * Approximate interval between pitch readings in ms.
         *
         * With 75 % overlap ([AudioCaptureEngine.HOP_SIZE] = 1024 at
         * 44.1 kHz) this is ~23 ms — roughly 43 updates per second.
         */
        private const val FRAME_INTERVAL_MS = (
            AudioCaptureEngine.HOP_SIZE * 1000L / AudioCaptureEngine.SAMPLE_RATE
        )

        /**
         * Minimum RMS ratio threshold for onset (pluck) detection.
         *
         * The adaptive onset detector uses an exponential moving average of
         * recent RMS values. When the current RMS exceeds the EMA by the
         * computed adaptive ratio, a transient attack is assumed. This
         * minimum ensures onsets are detected even during sustained loud
         * passages where the EMA is already high.
         */
        private const val ONSET_MIN_RATIO = 2.5f

        /**
         * Maximum adaptive onset ratio to prevent the threshold from
         * becoming too high during very quiet passages.
         */
        private const val ONSET_MAX_RATIO = 5.0f

        /**
         * Smoothing factor for the RMS exponential moving average used in
         * adaptive onset detection. Lower values make the EMA slower to
         * track, providing a more stable baseline.
         */
        private const val ONSET_EMA_ALPHA = 0.15f

        /**
         * Number of frames to suppress after detecting an onset.
         *
         * At ~23 ms per frame (75 % overlap), 2 frames ≈ 46 ms — aligned
         * with the typical 20–50 ms attack phase of a plucked ukulele string.
         */
        private const val BLANKING_FRAMES = 2

        /**
         * How long to keep the last valid reading before showing SILENT.
         *
         * At ~23 ms per frame, 400 ms corresponds to ~17 frames. This prevents
         * brief dropouts from instantly replacing guidance text with
         * "Play a string…".
         */
        private const val LOST_SIGNAL_HOLD_MS = 400L

        /**
         * Neural supervisor cadence. At ~23 ms per frame, 5 frames ≈ 115 ms.
         */
        private const val NEURAL_SUPERVISOR_INTERVAL = 5

        /** If stale, neural estimates are ignored until refreshed. */
        private const val NEURAL_RESULT_TTL_FRAMES = 10

        /** Mark runtime fallback if this many inferences fail in a row. */
        private const val NEURAL_FAILURE_THRESHOLD = 15

        /** Ignore tiny YIN-vs-neural disagreements. */
        private const val ARBITRATION_IGNORE_SEMITONES = 1.5

        /** Strong disagreement threshold for non-octave correction. */
        private const val ARBITRATION_STRONG_SEMITONES = 2.5

        /** Required consecutive similar neural readings before override. */
        private const val NEURAL_CONSISTENCY_FRAMES = 2

        /** Hysteresis for keeping previous target string assignment. */
        private const val STRING_SWITCH_HYSTERESIS_CENTS = 4.0

        /**
         * Minimum consecutive frames with the same note before it becomes
         * the "settled" note. At ~23 ms/frame, 15 frames ≈ 350 ms.
         */
        private const val SETTLED_NOTE_MIN_FRAMES = 15

        /** Periodic telemetry cadence to keep logs readable. */
        private const val TELEMETRY_LOG_INTERVAL_FRAMES = 25L

        private const val TAG = "TunerViewModel"

        @androidx.annotation.VisibleForTesting
        internal fun semitoneDistance(aHz: Double, bHz: Double): Double {
            if (aHz <= 0.0 || bHz <= 0.0) return Double.MAX_VALUE
            return abs(12.0 * log2(aHz / bHz))
        }

        @androidx.annotation.VisibleForTesting
        internal fun isOctaveRelation(aHz: Double, bHz: Double): Boolean {
            if (aHz <= 0.0 || bHz <= 0.0) return false
            val semitones = semitoneDistance(aHz, bHz)
            return abs(semitones - 12.0) <= 1.0 || abs(semitones - 24.0) <= 1.0
        }
    }

    // --- State ---------------------------------------------------------------

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    /** Current tuning — set externally by the host screen from SettingsViewModel. */
    private var currentTuning: UkuleleTuning = UkuleleTuning.HIGH_G

    /** Current tuner settings — set externally by the host screen. */
    private var tunerSettings: TunerSettings = TunerSettings()

    /** Application context used to initialize optional neural supervisor. */
    private var appContext: Context? = null

    // --- Text-to-Speech state ------------------------------------------------

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val ttsThrottler = TtsAnnouncementThrottler()

    // --- Noise gate ----------------------------------------------------------

    @Volatile
    private var noiseGateRms = 0.04f

    fun setNoiseGateRms(rms: Float) {
        noiseGateRms = rms
    }

    // --- Smoothing state -----------------------------------------------------

    /** Ring buffer of recent frequency readings for median smoothing. */
    private val recentFrequencies = ArrayDeque<Double>(SMOOTHING_WINDOW)

    /** Consecutive in-tune frame count for the current target string. */
    private var inTuneFrames = 0

    /** Index of the string that was in-tune in the previous frame. */
    private var inTuneStringIndex = -1

    // --- Onset detection state -----------------------------------------------

    /** RMS energy of the previous frame, for onset (pluck) detection. */
    private var previousRms = 0f

    /** Exponential moving average of RMS, for adaptive onset threshold. */
    private var rmsEma = 0f

    /** Remaining frames to suppress after an onset is detected. */
    private var blankingFramesRemaining = 0

    /** Consecutive frames with no reliable pitch result. */
    private var lostSignalFrames = 0

    // --- Pitch continuity state ----------------------------------------------

    /**
     * Frequency detected in the previous frame, or `null` after silence /
     * start / stop.  Passed to [PitchDetector.detect] so the lag search
     * is constrained to a narrow pitch window, preventing wild jumps.
     */
    private var previousFrequency: Double? = null
    private var displayCentsFiltered = 0.0

    /** Frames to hold the last non-silent reading before switching to SILENT. */
    private val lostSignalHoldFrames = (
        LOST_SIGNAL_HOLD_MS / FRAME_INTERVAL_MS
    ).toInt().coerceAtLeast(1)

    // --- Frame-dropping (backpressure) ----------------------------------------
    private val isProcessing = AtomicBoolean(false)

    // --- Neural supervisor state --------------------------------------------

    private var neuralSupervisor: NeuralPitchSupervisor? = null
    private var neuralFrameCounter = 0
    private var lastNeuralResult: NeuralPitchResult? = null
    private var neuralResultAgeFrames = Int.MAX_VALUE
    private var consecutiveNeuralFailures = 0
    private var lastNeuralFrequencyForConsistency: Double? = null
    private var neuralConsistencyFrames = 0

    // --- Settled note tracking ------------------------------------------------

    /** Note name currently being sustained (for settled-note detection). */
    private var settledNoteName: String? = null

    /** Consecutive frames the same note has been detected. */
    private var settledNoteFrames = 0

    /** Cents deviation tracked alongside the settled note. */
    private var settledNoteCents = 0.0

    // --- Calibration telemetry ------------------------------------------------
    private var telemetryFrameCounter = 0L
    private var telemetryOverrideCount = 0L
    private var lastLoggedStatus: TuningStatus = TuningStatus.SILENT

    // --- Public API ----------------------------------------------------------

    /**
     * Updates the tuning used for string matching.
     */
    fun setTuning(tuning: UkuleleTuning) {
        currentTuning = tuning
        resetProgress()
    }

    /**
     * Updates tuner-specific settings (spoken feedback, precision, A4, auto-advance).
     */
    fun setTunerSettings(settings: TunerSettings) {
        val oldSpoken = tunerSettings.spokenFeedback
        tunerSettings = settings
        if (settings.spokenFeedback && !oldSpoken) {
            initializeTts()
        } else if (!settings.spokenFeedback && oldSpoken) {
            shutdownTts()
        }
    }

    /**
     * Provides an application context so optional neural supervisor can load.
     */
    fun setApplicationContext(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        initializeNeuralSupervisor()
        if (tunerSettings.spokenFeedback) {
            initializeTts()
        }
    }

    /**
     * Starts listening to the microphone and processing pitch.
     */
    fun startTuning() {
        if (_uiState.value.isListening) return

        _uiState.update {
            it.copy(
                isListening = true,
                tuningStatus = TuningStatus.SILENT,
                lastSettledNote = null,
                lastSettledCents = 0.0,
            )
        }
        recentFrequencies.clear()
        inTuneFrames = 0
        inTuneStringIndex = -1
        previousRms = 0f
        rmsEma = 0f
        blankingFramesRemaining = 0
        lostSignalFrames = 0
        previousFrequency = null
        displayCentsFiltered = 0.0
        settledNoteName = null
        settledNoteFrames = 0
        settledNoteCents = 0.0
        neuralFrameCounter = 0
        lastNeuralResult = null
        neuralResultAgeFrames = Int.MAX_VALUE
        consecutiveNeuralFailures = 0
        lastNeuralFrequencyForConsistency = null
        neuralConsistencyFrames = 0
        telemetryFrameCounter = 0
        telemetryOverrideCount = 0
        lastLoggedStatus = TuningStatus.SILENT

        AudioCaptureEngine.start(viewModelScope) { buffer ->
            processBuffer(buffer)
        }
    }

    /**
     * Stops listening and releases the microphone.
     *
     * Internal processing state ([recentFrequencies], [previousFrequency], etc.)
     * is deliberately **not** cleared here because [processBuffer] may still
     * be executing on a background thread ([kotlinx.coroutines.Dispatchers.Default]).
     * Clearing a non-thread-safe [ArrayDeque] from the main thread while the
     * background thread iterates it causes a [ConcurrentModificationException]
     * (or [IndexOutOfBoundsException]) that crashes the Activity.
     *
     * The internal state is reset in [startTuning] instead, before any
     * new background work begins.
     */
    fun stopTuning() {
        AudioCaptureEngine.stop()
        _uiState.update {
            it.copy(
                isListening = false,
                tuningStatus = TuningStatus.SILENT,
                detectedNote = null,
                centsDeviation = 0.0,
                displayCentsDeviation = 0.0,
                targetString = null,
                noteInfo = null,
                lastSettledNote = null,
                lastSettledCents = 0.0,
            )
        }
    }

    /**
     * Resets string-tuned progress (e.g. when switching tuning).
     */
    fun resetProgress() {
        _uiState.update {
            it.copy(stringProgress = listOf(false, false, false, false))
        }
        inTuneFrames = 0
        inTuneStringIndex = -1
        lostSignalFrames = 0
    }

    override fun onCleared() {
        super.onCleared()
        AudioCaptureEngine.stop()
        neuralSupervisor?.close()
        neuralSupervisor = null
        shutdownTts()
    }

    // --- Internal pipeline ---------------------------------------------------

    /**
     * Processes a single audio buffer through pitch detection and note mapping.
     *
     * Called on [kotlinx.coroutines.Dispatchers.Default] from the capture
     * coroutine. A final buffer may arrive after [stopTuning] has already
     * set [TunerUiState.isListening] to `false`; the early-return guard
     * below prevents stale data from briefly flashing in the UI and avoids
     * a [ConcurrentModificationException] on [recentFrequencies].
     */
    private fun processBuffer(samples: FloatArray) {
        if (!_uiState.value.isListening) return
        if (!isProcessing.compareAndSet(false, true)) return
        try {
            processBufferInner(samples)
        } finally {
            isProcessing.set(false)
        }
    }

    private fun processBufferInner(samples: FloatArray) {
        // --- Onset detection (adaptive) -------------------------------------
        // Detect sudden energy spikes (pluck attacks) using an adaptive
        // threshold based on a running EMA of recent RMS values. This makes
        // detection robust for both soft playing and aggressive strumming.
        val currentRms = PitchDetector.rms(samples)
        if (rmsEma > 0f) {
            // Adaptive ratio: inversely proportional to the EMA level.
            // Quiet passages → higher ratio (more selective), loud passages
            // → lower ratio (still catches onsets above the sustained level).
            val adaptiveRatio = (ONSET_MIN_RATIO + (0.01f / rmsEma))
                .coerceIn(ONSET_MIN_RATIO, ONSET_MAX_RATIO)
            if (currentRms / rmsEma > adaptiveRatio) {
                blankingFramesRemaining = BLANKING_FRAMES
            }
        }
        // Update EMA (use direct assignment for the first frame).
        rmsEma = if (rmsEma == 0f) currentRms
        else rmsEma + ONSET_EMA_ALPHA * (currentRms - rmsEma)
        previousRms = currentRms

        if (blankingFramesRemaining > 0) {
            blankingFramesRemaining--
            return // skip — likely attack transient
        }

        if (currentRms < noiseGateRms) {
            lostSignalFrames = 0
            previousFrequency = null
            recentFrequencies.clear()
            inTuneFrames = 0
            inTuneStringIndex = -1
            displayCentsFiltered = 0.0
            settledNoteName = null
            settledNoteFrames = 0
            _uiState.update {
                it.copy(
                    tuningStatus = TuningStatus.SILENT,
                    detectedNote = null,
                    centsDeviation = 0.0,
                    displayCentsDeviation = 0.0,
                    confidence = 1.0,
                    targetString = null,
                    noteInfo = null,
                )
            }
            return
        }

        val result = PitchDetector.detect(
            samples,
            AudioCaptureEngine.SAMPLE_RATE,
            previousFrequency = previousFrequency,
        )

        if (result == null) {
            // Brief gaps are common while strings decay. Keep the last reading
            // for a short grace window so guidance text remains readable.
            lostSignalFrames++
            if (
                lostSignalFrames < lostSignalHoldFrames &&
                _uiState.value.tuningStatus != TuningStatus.SILENT
            ) {
                return
            }

            // Sustained loss — now transition to SILENT and clear continuity.
            lostSignalFrames = 0
            previousFrequency = null
            recentFrequencies.clear()
            inTuneFrames = 0
            inTuneStringIndex = -1
            displayCentsFiltered = 0.0
            settledNoteName = null
            settledNoteFrames = 0
            _uiState.update {
                it.copy(
                    tuningStatus = TuningStatus.SILENT,
                    detectedNote = null,
                    centsDeviation = 0.0,
                    displayCentsDeviation = 0.0,
                    confidence = 1.0,
                    targetString = null,
                    noteInfo = null,
                )
            }
            return
        }

        lostSignalFrames = 0

        val neuralResultForFrame = runNeuralSupervisor(samples)
        val arbitration = arbitrate(result, neuralResultForFrame)
        val finalPitch = arbitration.result
        if (arbitration.overrideApplied) {
            telemetryOverrideCount++
        }

        // Track for next frame's continuity constraint.
        previousFrequency = finalPitch.frequencyHz

        // --- Smoothing -------------------------------------------------------
        recentFrequencies.addLast(finalPitch.frequencyHz)
        if (recentFrequencies.size > SMOOTHING_WINDOW) {
            recentFrequencies.removeFirst()
        }
        val smoothedHz = medianFrequency()

        // --- Note mapping ----------------------------------------------------
        val a4Ref = tunerSettings.a4Reference.toDouble()
        val noteInfo = TunerNoteMapper.mapFrequency(smoothedHz, a4Ref) ?: return
        val previousTargetStringIndex = _uiState.value.targetString?.stringIndex
        val stringMatch = TunerNoteMapper.findNearestStringWithHysteresis(
            noteInfo = noteInfo,
            tuning = currentTuning,
            previousStringIndex = previousTargetStringIndex,
            switchHysteresisCents = STRING_SWITCH_HYSTERESIS_CENTS,
            a4Reference = a4Ref,
        )

        // Use cents-from-target-string for the meter (more useful than
        // cents-from-nearest-chromatic-note when tuning a specific string).
        val cents = stringMatch.centsFromTarget
        val absCents = abs(cents)
        val clampedCents = cents.coerceIn(-50.0, 50.0)
        val displayCents = smoothDisplayCents(clampedCents)

        val effectiveInTuneCents = if (tunerSettings.precisionMode) {
            PRECISION_IN_TUNE_CENTS
        } else {
            IN_TUNE_CENTS
        }

        val status = when {
            absCents <= effectiveInTuneCents -> TuningStatus.IN_TUNE
            absCents <= CLOSE_CENTS -> TuningStatus.CLOSE
            cents < 0 -> TuningStatus.FLAT
            else -> TuningStatus.SHARP
        }

        // --- String completion tracking --------------------------------------
        val progress = _uiState.value.stringProgress.toMutableList()
        var justTuned = false

        if (status == TuningStatus.IN_TUNE && stringMatch.stringIndex == inTuneStringIndex) {
            inTuneFrames++
            val holdFrames = (IN_TUNE_HOLD_MS / FRAME_INTERVAL_MS).toInt()
            if (inTuneFrames >= holdFrames && !progress[stringMatch.stringIndex]) {
                progress[stringMatch.stringIndex] = true
                justTuned = true
            }
        } else if (status == TuningStatus.IN_TUNE) {
            inTuneStringIndex = stringMatch.stringIndex
            inTuneFrames = 1
        } else {
            inTuneFrames = 0
            inTuneStringIndex = -1
        }

        // --- Auto-advance target ---------------------------------------------
        val autoAdvanceIdx = if (tunerSettings.autoAdvance && justTuned) {
            findNextUntunedString(progress)
        } else if (tunerSettings.autoAdvance) {
            _uiState.value.autoAdvanceTarget
        } else {
            -1
        }

        // --- Settled note tracking -------------------------------------------
        val displayNote = "${noteInfo.noteName}${noteInfo.octave}"

        if (displayNote == settledNoteName) {
            settledNoteFrames++
            settledNoteCents = clampedCents
        } else {
            settledNoteName = displayNote
            settledNoteFrames = 1
            settledNoteCents = clampedCents
        }

        val settledUpdate = if (settledNoteFrames >= SETTLED_NOTE_MIN_FRAMES) {
            settledNoteName to settledNoteCents
        } else {
            null
        }

        // --- Emit state ------------------------------------------------------
        _uiState.update {
            it.copy(
                detectedNote = displayNote,
                centsDeviation = clampedCents,
                displayCentsDeviation = displayCents,
                confidence = finalPitch.confidence,
                tuningStatus = status,
                targetString = stringMatch,
                stringProgress = progress,
                noteInfo = noteInfo,
                autoAdvanceTarget = autoAdvanceIdx,
                lastSettledNote = settledUpdate?.first ?: it.lastSettledNote,
                lastSettledCents = settledUpdate?.second ?: it.lastSettledCents,
            )
        }

        // --- Spoken feedback -------------------------------------------------
        if (tunerSettings.spokenFeedback) {
            speakTuningFeedback(
                noteName = noteInfo.noteName,
                status = status,
                cents = clampedCents,
                stringName = stringMatch.stringName,
                justTuned = justTuned,
            )
        }

        logCalibrationTelemetry(
            yinResult = result,
            neuralResult = neuralResultForFrame,
            finalPitch = finalPitch,
            cents = clampedCents,
            status = status,
            stringMatch = stringMatch,
            arbitrationReason = arbitration.reason,
        )
    }

    /**
     * Returns the median of the recent frequency readings.
     *
     * Median is more robust than mean against occasional outlier frames
     * (e.g. harmonic jumps).
     */
    private fun medianFrequency(): Double {
        val sorted = recentFrequencies.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0 && sorted.size >= 2) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }

    private fun initializeNeuralSupervisor() {
        if (neuralSupervisor != null) return
        val ctx = appContext ?: return
        updateNeuralStatus(
            available = false,
            active = false,
            status = NeuralRuntimeStatus.LOADING,
        )
        viewModelScope.launch {
            val supervisor = withContext(Dispatchers.IO) {
                try {
                    NeuralPitchSupervisor(ctx)
                } catch (e: Exception) {
                    Log.w(TAG, "Neural supervisor unavailable: ${e.message}")
                    null
                }
            }
            neuralSupervisor = supervisor
            if (supervisor != null) {
                updateNeuralStatus(
                    available = true,
                    active = true,
                    status = NeuralRuntimeStatus.ACTIVE,
                )
            } else {
                updateNeuralStatus(
                    available = false,
                    active = false,
                    status = NeuralRuntimeStatus.FALLBACK,
                )
            }
        }
    }

    private fun runNeuralSupervisor(samples: FloatArray): NeuralPitchResult? {
        val supervisor = neuralSupervisor ?: return null
        neuralFrameCounter++

        if (neuralFrameCounter % NEURAL_SUPERVISOR_INTERVAL == 0) {
            val estimate = supervisor.estimate(samples)
            lastNeuralResult = estimate
            neuralResultAgeFrames = 0
            if (estimate != null) {
                consecutiveNeuralFailures = 0
                updateNeuralConsistency(estimate.frequencyHz)
                updateNeuralStatus(
                    available = true,
                    active = true,
                    status = NeuralRuntimeStatus.ACTIVE,
                )
            } else {
                consecutiveNeuralFailures++
                neuralConsistencyFrames = 0
                lastNeuralFrequencyForConsistency = null
                if (consecutiveNeuralFailures >= NEURAL_FAILURE_THRESHOLD) {
                    updateNeuralStatus(
                        available = true,
                        active = false,
                        status = NeuralRuntimeStatus.FALLBACK,
                    )
                }
            }
        } else if (neuralResultAgeFrames < Int.MAX_VALUE) {
            neuralResultAgeFrames++
        }

        return if (neuralResultAgeFrames <= NEURAL_RESULT_TTL_FRAMES) {
            lastNeuralResult
        } else {
            null
        }
    }

    private data class ArbitrationDecision(
        val result: PitchResult,
        val overrideApplied: Boolean,
        val reason: String,
    )

    private fun arbitrate(
        yinResult: PitchResult,
        neuralResult: NeuralPitchResult?,
    ): ArbitrationDecision {
        if (neuralResult == null) {
            return ArbitrationDecision(
                result = yinResult,
                overrideApplied = false,
                reason = "no_neural",
            )
        }

        val semitoneGap = semitoneDistance(yinResult.frequencyHz, neuralResult.frequencyHz)
        if (semitoneGap <= ARBITRATION_IGNORE_SEMITONES) {
            return ArbitrationDecision(
                result = yinResult,
                overrideApplied = false,
                reason = "small_gap",
            )
        }

        if (neuralConsistencyFrames < NEURAL_CONSISTENCY_FRAMES) {
            return ArbitrationDecision(
                result = yinResult,
                overrideApplied = false,
                reason = "neural_inconsistent",
            )
        }

        if (isOctaveRelation(yinResult.frequencyHz, neuralResult.frequencyHz) &&
            neuralResult.confidence >= 0.85 &&
            yinResult.confidence >= 0.12
        ) {
            return ArbitrationDecision(
                result = yinResult.copy(frequencyHz = neuralResult.frequencyHz),
                overrideApplied = true,
                reason = "octave_correction",
            )
        }

        return if (semitoneGap >= ARBITRATION_STRONG_SEMITONES &&
            neuralResult.confidence >= 0.93 &&
            yinResult.confidence >= 0.16
        ) {
            ArbitrationDecision(
                result = yinResult.copy(frequencyHz = neuralResult.frequencyHz),
                overrideApplied = true,
                reason = "strong_disagreement",
            )
        } else {
            ArbitrationDecision(
                result = yinResult,
                overrideApplied = false,
                reason = "keep_yin",
            )
        }
    }

    private fun semitoneDistance(aHz: Double, bHz: Double): Double =
        Companion.semitoneDistance(aHz, bHz)

    private fun isOctaveRelation(aHz: Double, bHz: Double): Boolean =
        Companion.isOctaveRelation(aHz, bHz)

    private fun updateNeuralConsistency(neuralFrequencyHz: Double) {
        if (neuralFrequencyHz <= 0.0) {
            neuralConsistencyFrames = 0
            lastNeuralFrequencyForConsistency = null
            return
        }

        val previous = lastNeuralFrequencyForConsistency
        neuralConsistencyFrames = if (previous != null &&
            semitoneDistance(previous, neuralFrequencyHz) <= 0.5
        ) {
            neuralConsistencyFrames + 1
        } else {
            1
        }
        lastNeuralFrequencyForConsistency = neuralFrequencyHz
    }

    private fun smoothDisplayCents(rawCents: Double): Double {
        val target = if (abs(rawCents) < DISPLAY_DEADBAND_CENTS) 0.0 else rawCents
        displayCentsFiltered += DISPLAY_CENTS_ALPHA * (target - displayCentsFiltered)
        if (abs(displayCentsFiltered) < DISPLAY_DEADBAND_CENTS / 2.0) {
            displayCentsFiltered = 0.0
        }
        return displayCentsFiltered
    }

    private fun updateNeuralStatus(
        available: Boolean,
        active: Boolean,
        status: NeuralRuntimeStatus,
    ) {
        _uiState.update {
            it.copy(
                isNeuralAvailable = available,
                isNeuralActive = active,
                neuralRuntimeStatus = status,
            )
        }
    }

    // --- TTS helpers ---------------------------------------------------------

    private fun initializeTts() {
        if (tts != null) return
        val ctx = appContext ?: return
        tts = TextToSpeech(ctx) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.language = Locale.US
        }
    }

    private fun shutdownTts() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    /**
     * Speaks tuning feedback using TTS. Throttled to avoid overwhelming the user.
     *
     * Modelled after Talking Tuner (iOS): speaks the note name and how many
     * cents sharp or flat. Announces "In tune!" when the string is on pitch
     * and "String tuned!" when a string is marked as completed.
     */
    private fun speakTuningFeedback(
        noteName: String,
        status: TuningStatus,
        cents: Double,
        stringName: String,
        justTuned: Boolean,
    ) {
        if (!ttsReady || tts == null) return

        if (!ttsThrottler.shouldAnnounce(noteName, status, cents, justTuned)) return

        if (justTuned) {
            tts?.speak(
                "$stringName string tuned!",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "tuned_$stringName",
            )
            return
        }

        val message = when (status) {
            TuningStatus.SILENT -> return
            TuningStatus.IN_TUNE -> "$noteName, in tune"
            TuningStatus.CLOSE -> {
                val absCents = abs(cents).toInt()
                val direction = if (cents < 0) "flat" else "sharp"
                "$noteName, $absCents cents $direction"
            }
            TuningStatus.FLAT -> {
                val absCents = abs(cents).toInt()
                "$noteName, $absCents cents flat"
            }
            TuningStatus.SHARP -> {
                val absCents = abs(cents).toInt()
                "$noteName, $absCents cents sharp"
            }
        }

        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "feedback_$noteName")
    }

    // --- Auto-advance helpers ------------------------------------------------

    /**
     * Finds the next untuned string index, wrapping around. Returns -1 if all are tuned.
     */
    private fun findNextUntunedString(progress: List<Boolean>): Int {
        val current = inTuneStringIndex
        for (offset in 1..progress.size) {
            val idx = (current + offset) % progress.size
            if (!progress[idx]) return idx
        }
        return -1
    }

    private fun logCalibrationTelemetry(
        yinResult: PitchResult,
        neuralResult: NeuralPitchResult?,
        finalPitch: PitchResult,
        cents: Double,
        status: TuningStatus,
        stringMatch: StringMatch,
        arbitrationReason: String,
    ) {
        if (!Log.isLoggable(TAG, Log.DEBUG)) return

        telemetryFrameCounter++

        if (status != lastLoggedStatus) {
            Log.d(
                TAG,
                "Status transition: $lastLoggedStatus -> $status " +
                    "(string=${stringMatch.stringName}, cents=${"%.2f".format(cents)})",
            )
            lastLoggedStatus = status
        }

        if (telemetryFrameCounter % TELEMETRY_LOG_INTERVAL_FRAMES != 0L) return

        val neuralFreq = neuralResult?.frequencyHz?.let { "%.2f".format(it) } ?: "null"
        val neuralConf = neuralResult?.confidence?.let { "%.2f".format(it) } ?: "null"
        val inferenceMs = neuralSupervisor?.lastInferenceMs()?.let { "%.2f".format(it) } ?: "null"

        Log.d(
            TAG,
            "Telemetry frame=$telemetryFrameCounter " +
                "yinHz=${"%.2f".format(yinResult.frequencyHz)} yinConf=${"%.2f".format(yinResult.confidence)} " +
                "neuralHz=$neuralFreq neuralConf=$neuralConf neuralMs=$inferenceMs " +
                "finalHz=${"%.2f".format(finalPitch.frequencyHz)} finalConf=${"%.2f".format(finalPitch.confidence)} " +
                "string=${stringMatch.stringName} cents=${"%.2f".format(cents)} status=$status " +
                "reason=$arbitrationReason overrides=$telemetryOverrideCount",
        )
    }
}

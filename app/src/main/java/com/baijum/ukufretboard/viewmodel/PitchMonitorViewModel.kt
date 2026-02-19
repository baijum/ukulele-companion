package com.baijum.ukufretboard.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.audio.AudioCaptureEngine
import com.baijum.ukufretboard.domain.ArpeggioDetector
import com.baijum.ukufretboard.domain.AudioChordDetector
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.domain.NeuralPitchResult
import com.baijum.ukufretboard.domain.NeuralPitchSupervisor
import com.baijum.ukufretboard.domain.PitchDetector
import com.baijum.ukufretboard.domain.PitchResult
import com.baijum.ukufretboard.domain.TunerNoteMapper
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * A single pitch data point in the scrolling visualization.
 *
 * @property timestampMs Wall-clock timestamp (from [System.currentTimeMillis]).
 * @property midiNote    Continuous MIDI note number (e.g. 69.0 = A4 exactly,
 *   69.5 = A4 + 50 cents). `null` when silence is detected. Using a continuous
 *   value rather than a rounded integer allows the UI to show pitch bends
 *   and vibrato as smooth vertical motion.
 */
data class PitchPoint(
    val timestampMs: Long,
    val midiNote: Float?,
)

/**
 * UI state for the Pitch Monitor screen.
 */
data class PitchMonitorUiState(
    /** Whether the microphone is actively capturing audio. */
    val isListening: Boolean = false,

    /** Rolling history of pitch data points for the scrolling canvas. */
    val pitchHistory: List<PitchPoint> = emptyList(),

    /** Display name of the current note (e.g. "A4"), or null if silent. */
    val currentNote: String? = null,

    /** Name of the detected chord (e.g. "C", "Am7"), or null if none. */
    val detectedChord: String? = null,

    /** Notes of the detected chord (e.g. ["C", "E", "G"]). */
    val detectedChordNotes: List<String> = emptyList(),

    /** Confidence of the chord detection (0.0 .. 1.0). */
    val chordConfidence: Float = 0f,

    /** Rolling history of recently detected note names, deduped on consecutive repeats. */
    val recentNotes: List<String> = emptyList(),

    /** True when the displayed chord was detected via arpeggio (sequential notes). */
    val isArpeggioChord: Boolean = false,

    /** 12-bin chromagram energy (C=0 .. B=11), normalized 0..1. Used for canvas glow. */
    val chromaEnergy: FloatArray = FloatArray(12),
)

/**
 * ViewModel for the Pitch Monitor feature.
 *
 * Orchestrates a dual pipeline from a single audio buffer:
 *
 * 1. **Pitch path**: [AudioCaptureEngine] → [PitchDetector] (YIN) →
 *    [TunerNoteMapper] → [PitchPoint] history for the scrolling canvas.
 *
 * 2. **Chord path**: [AudioCaptureEngine] → [AudioChordDetector]
 *    (FFT → Chromagram → [ChordDetector]) → chord name display.
 *
 * Both paths share the same audio buffer to avoid redundant mic capture.
 *
 * Note: [AudioCaptureEngine] is a singleton, so the Tuner and Pitch Monitor
 * cannot run simultaneously. Navigating away stops capture via
 * `DisposableEffect` in the UI layer.
 */
class PitchMonitorViewModel : ViewModel() {

    companion object {
        /** How many recent frequency readings to keep for median smoothing. */
        private const val SMOOTHING_WINDOW = 5

        /** Maximum pitch history duration in milliseconds (~10 seconds). */
        private const val HISTORY_DURATION_MS = 10_000L

        /**
         * Number of consecutive chord detections required before showing
         * the chord in the UI. Lower than before to make live feedback
         * responsive for short ukulele strums.
         */
        private const val CHORD_HOLD_FRAMES = 4

        /**
         * Run chord detection (FFT + Chromagram) every Nth frame.
         *
         * The pitch path runs on every frame for smooth scrolling, but
         * chord detection is heavier (FFT + Chromagram + matching) and
         * doesn't benefit from 43 fps. Running every 4th frame keeps it
         * at ~21 Hz, balancing responsiveness with CPU use.
         */
        private const val CHORD_DETECTION_INTERVAL = 2

        /**
         * How many detection cycles can miss before clearing a shown chord.
         * This avoids UI dropouts between strums and brief noisy frames.
         */
        private const val CHORD_MISS_TOLERANCE = 3

        /**
         * Consecutive arpeggio detections required before showing the chord.
         * Lower than [CHORD_HOLD_FRAMES] because arpeggios inherently
         * accumulate over time and don't need as much confirmation.
         */
        private const val ARPEGGIO_HOLD_FRAMES = 2

        /**
         * Minimum RMS energy to consider a frame as intentional audio.
         * Frames below this are treated as silence, suppressing spurious
         * pitch detections, arpeggio accumulation, and chromagram glow
         * from background noise. Typical room noise is 0.001--0.003;
         * a soft ukulele pluck is ~0.01+.
         */
        private const val NOISE_GATE_RMS = 0.005f

        /**
         * RMS ratio threshold for onset (pluck) detection.
         *
         * When the current frame's RMS exceeds the previous frame's RMS by
         * this factor, a transient attack is assumed and pitch updates are
         * suppressed for [BLANKING_FRAMES]. Same value as [TunerViewModel].
         */
        private const val ONSET_RATIO_THRESHOLD = 3.0f

        /**
         * Number of frames to suppress after detecting an onset.
         *
         * At ~23 ms per frame (75 % overlap), 2 frames ≈ 46 ms — aligned
         * with the typical 20–50 ms attack phase of a plucked string.
         */
        private const val BLANKING_FRAMES = 2

        /** Run neural supervisor every Nth frame (~115 ms at 23 ms/frame). */
        private const val NEURAL_SUPERVISOR_INTERVAL = 5

        /** Neural results older than this many frames are ignored. */
        private const val NEURAL_RESULT_TTL_FRAMES = 10

        /** Ignore tiny YIN-vs-neural disagreements. */
        private const val ARBITRATION_IGNORE_SEMITONES = 1.5

        /** Strong disagreement threshold for non-octave correction. */
        private const val ARBITRATION_STRONG_SEMITONES = 2.5

        /** Require short temporal consistency before neural override. */
        private const val NEURAL_CONSISTENCY_FRAMES = 2

        private const val TELEMETRY_LOG_INTERVAL_FRAMES = 25L
        private const val TAG = "PitchMonitorVM"
    }

    // --- State ---------------------------------------------------------------

    private val _uiState = MutableStateFlow(PitchMonitorUiState())
    val uiState: StateFlow<PitchMonitorUiState> = _uiState.asStateFlow()

    // --- Smoothing state -----------------------------------------------------

    /** Ring buffer of recent frequency readings for median pitch smoothing. */
    private val recentFrequencies = ArrayDeque<Double>(SMOOTHING_WINDOW)

    /** Last detected chord name (for temporal smoothing). */
    private var lastChordName: String? = null

    /** Consecutive frames the same chord has been detected. */
    private var chordHoldCount = 0

    /** The chord name currently displayed (after hold threshold). */
    private var displayedChord: String? = null

    /** Notes for the currently displayed chord. */
    private var displayedChordNotes: List<String> = emptyList()

    /** Confidence of the most recent chord detection (for the UI). */
    private var lastChordConfidence = 0f

    /** Consecutive detection cycles where no chord was found. */
    private var chordMissCount = 0

    /** Frame counter for throttling chord detection to every Nth frame. */
    private var chordFrameCounter = 0

    /** Most recent chromagram from chord detection, kept across throttled frames. */
    private var lastChromaEnergy = FloatArray(12)

    // --- Arpeggio detection state --------------------------------------------

    private val arpeggioDetector = ArpeggioDetector(windowMs = 3_000L)

    private var lastArpeggioChordName: String? = null
    private var arpeggioHoldCount = 0
    private var displayedArpeggioChord: String? = null
    private var displayedArpeggioChordNotes: List<String> = emptyList()

    /** Timestamp of the most recent simultaneous chord confirmation. */
    private var lastSimultaneousChordMs = 0L

    /** Timestamp of the most recent arpeggio chord confirmation. */
    private var lastArpeggioChordMs = 0L

    /** Application context used to initialize optional neural supervisor. */
    private var appContext: Context? = null

    // --- Onset detection state -----------------------------------------------

    /** RMS energy of the previous frame, for onset (pluck) detection. */
    private var previousRms = 0f

    /** Remaining frames to suppress after an onset is detected. */
    private var blankingFramesRemaining = 0

    // --- Pitch continuity state ----------------------------------------------

    /**
     * Frequency detected in the previous frame, or `null` after silence /
     * start.  Passed to [PitchDetector.detect] so the lag search is
     * constrained to a narrow pitch window, preventing wild jumps.
     */
    private var previousFrequency: Double? = null

    // --- Neural supervisor state ----------------------------------------------

    private var neuralSupervisor: NeuralPitchSupervisor? = null
    private var neuralFrameCounter = 0
    private var lastNeuralResult: NeuralPitchResult? = null
    private var neuralResultAgeFrames = Int.MAX_VALUE
    private var lastNeuralFrequencyForConsistency: Double? = null
    private var neuralConsistencyFrames = 0
    private var telemetryFrameCounter = 0L

    // --- Public API ----------------------------------------------------------

    /**
     * Starts listening to the microphone and processing audio.
     */
    fun startListening() {
        if (_uiState.value.isListening) return

        initializeNeuralSupervisor()

        _uiState.update { it.copy(isListening = true) }
        recentFrequencies.clear()
        lastChordName = null
        chordHoldCount = 0
        displayedChord = null
        displayedChordNotes = emptyList()
        lastChordConfidence = 0f
        chordMissCount = 0
        chordFrameCounter = 0
        lastChromaEnergy = FloatArray(12)
        arpeggioDetector.clear()
        lastArpeggioChordName = null
        arpeggioHoldCount = 0
        displayedArpeggioChord = null
        displayedArpeggioChordNotes = emptyList()
        lastSimultaneousChordMs = 0L
        lastArpeggioChordMs = 0L
        previousRms = 0f
        blankingFramesRemaining = 0
        previousFrequency = null
        neuralFrameCounter = 0
        lastNeuralResult = null
        neuralResultAgeFrames = Int.MAX_VALUE
        lastNeuralFrequencyForConsistency = null
        neuralConsistencyFrames = 0
        telemetryFrameCounter = 0

        AudioCaptureEngine.start(viewModelScope) { buffer ->
            processBuffer(buffer)
        }
    }

    /**
     * Stops listening and releases the microphone.
     *
     * Internal processing state ([recentFrequencies], [lastChordName], etc.)
     * is deliberately **not** cleared here because [processBuffer] may still
     * be executing on a background thread ([kotlinx.coroutines.Dispatchers.Default]).
     * Clearing a non-thread-safe [ArrayDeque] from the main thread while the
     * background thread iterates it causes a [ConcurrentModificationException]
     * (or [IndexOutOfBoundsException]) that crashes the Activity.
     *
     * The internal state is reset in [startListening] instead, before any
     * new background work begins.
     */
    fun stopListening() {
        AudioCaptureEngine.stop()
        _uiState.update {
            it.copy(
                isListening = false,
                currentNote = null,
                detectedChord = null,
                detectedChordNotes = emptyList(),
                chordConfidence = 0f,
                recentNotes = emptyList(),
                isArpeggioChord = false,
                chromaEnergy = FloatArray(12),
            )
        }
    }

    /**
     * Provides an application context so optional neural supervisor can load.
     */
    fun setApplicationContext(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
    }

    override fun onCleared() {
        super.onCleared()
        AudioCaptureEngine.stop()
        neuralSupervisor?.close()
        neuralSupervisor = null
    }

    // --- Internal pipeline ---------------------------------------------------

    /**
     * Processes a single audio buffer through both the pitch and chord pipelines.
     *
     * Called on [kotlinx.coroutines.Dispatchers.Default] from the capture
     * coroutine. A final buffer may arrive after [stopListening] has already
     * set [PitchMonitorUiState.isListening] to `false`; the early-return
     * guard below prevents stale data from briefly flashing in the UI.
     */
    private fun processBuffer(samples: FloatArray) {
        if (!_uiState.value.isListening) return

        // Guard: the FFT requires a power-of-2 buffer.  When the recorder is
        // stopped, a partial (non-power-of-2) buffer may slip through; drop it.
        val n = samples.size
        if (n == 0 || n and (n - 1) != 0) return

        // --- Onset detection ----------------------------------------------------
        // Detect sudden energy spikes (pluck attacks) and suppress pitch
        // updates for a few frames so the non-periodic transient doesn't
        // produce spike artifacts in the scrolling graph.
        val currentRms = PitchDetector.rms(samples)
        if (previousRms > 0f && currentRms / previousRms > ONSET_RATIO_THRESHOLD) {
            blankingFramesRemaining = BLANKING_FRAMES
        }
        previousRms = currentRms

        if (blankingFramesRemaining > 0) {
            blankingFramesRemaining--
            // Still emit a null PitchPoint so the scrolling graph shows a
            // gap rather than freezing (unlike TunerViewModel which just
            // returns early — the pitch canvas needs continuous timestamps).
            val now = System.currentTimeMillis()
            val newPoint = PitchPoint(timestampMs = now, midiNote = null)
            _uiState.update { current ->
                val cutoff = now - HISTORY_DURATION_MS
                val trimmed = current.pitchHistory.dropWhile { it.timestampMs < cutoff }
                current.copy(pitchHistory = trimmed + newPoint)
            }
            return
        }

        val now = System.currentTimeMillis()

        // --- Noise gate ---------------------------------------------------------
        // Suppress all detection when the frame is too quiet (background noise).
        if (currentRms < NOISE_GATE_RMS) {
            previousFrequency = null
            recentFrequencies.clear()
            lastChromaEnergy = FloatArray(12)
            val newPoint = PitchPoint(timestampMs = now, midiNote = null)
            _uiState.update { current ->
                val cutoff = now - HISTORY_DURATION_MS
                val trimmed = current.pitchHistory.dropWhile { it.timestampMs < cutoff }
                current.copy(
                    pitchHistory = trimmed + newPoint,
                    currentNote = null,
                    chromaEnergy = FloatArray(12),
                )
            }
            return
        }

        // =====================================================================
        // PATH 1: Pitch detection (YIN) → scrolling visualization
        // =====================================================================
        val yinResult = PitchDetector.detect(
            samples,
            AudioCaptureEngine.SAMPLE_RATE,
            previousFrequency = previousFrequency,
        )
        val neuralResult = runNeuralSupervisor(samples)
        val pitchResult = arbitrate(yinResult, neuralResult)

        val midiNote: Float?
        val currentNote: String?

        if (pitchResult != null) {
            // Track for next frame's continuity constraint.
            previousFrequency = pitchResult.frequencyHz

            // Smooth the frequency with a median filter
            recentFrequencies.addLast(pitchResult.frequencyHz)
            if (recentFrequencies.size > SMOOTHING_WINDOW) {
                recentFrequencies.removeFirst()
            }
            val smoothedHz = medianFrequency()

            // Convert to continuous MIDI note number for smooth Y positioning
            midiNote = (69.0 + 12.0 * log2(smoothedHz / 440.0)).toFloat()

            // Get note name for display
            val noteInfo = TunerNoteMapper.mapFrequency(smoothedHz)
            currentNote = noteInfo?.let { "${it.noteName}${it.octave}" }
        } else {
            previousFrequency = null
            recentFrequencies.clear()
            midiNote = null
            currentNote = null
        }

        val newPoint = PitchPoint(timestampMs = now, midiNote = midiNote)

        // Feed detected pitch to the arpeggio detector
        if (midiNote != null) {
            val pitchClass = midiNote.roundToInt() % 12
            arpeggioDetector.addNote(now, pitchClass)
        }

        // =====================================================================
        // PATH 2: Chord detection (FFT → Chromagram → ChordDetector)
        // Throttled to every CHORD_DETECTION_INTERVAL frames to avoid
        // running the heavier FFT pipeline at the full 43 fps overlap rate.
        // =====================================================================
        if (chordFrameCounter++ % CHORD_DETECTION_INTERVAL == 0) {
            val preferredRootPitchClass = preferredRootPitchClass(neuralResult, pitchResult)
            val chordResult = AudioChordDetector.detect(
                samples = samples,
                preferredRootPitchClass = preferredRootPitchClass,
            )
            lastChordConfidence = chordResult.confidence
            lastChromaEnergy = chordResult.chromagram

            val rawChordName = when (val det = chordResult.detection) {
                is ChordDetector.DetectionResult.ChordFound -> det.result.name
                else -> null
            }
            val rawChordNotes = when (val det = chordResult.detection) {
                is ChordDetector.DetectionResult.ChordFound -> det.result.notes.map { it.name }
                else -> emptyList()
            }

            // Temporal smoothing: require the same chord for CHORD_HOLD_FRAMES
            if (rawChordName == lastChordName && rawChordName != null) {
                chordHoldCount++
                chordMissCount = 0
            } else {
                lastChordName = rawChordName
                chordHoldCount = if (rawChordName != null) 1 else 0
                chordMissCount = if (rawChordName == null) chordMissCount + 1 else 0
            }

            if (chordHoldCount >= CHORD_HOLD_FRAMES) {
                displayedChord = rawChordName
                displayedChordNotes = rawChordNotes
                if (rawChordName != null) lastSimultaneousChordMs = now
            } else if (rawChordName == null && chordMissCount > CHORD_MISS_TOLERANCE) {
                displayedChord = null
                displayedChordNotes = emptyList()
                lastChordName = null
                chordHoldCount = 0
            }
        }

        // =====================================================================
        // PATH 3: Arpeggio detection (accumulated pitch classes → ChordDetector)
        // =====================================================================
        val arpeggioResult = arpeggioDetector.detect(now)
        val rawArpChordName = arpeggioResult?.result?.name
        val rawArpChordNotes = arpeggioResult?.result?.notes?.map { it.name } ?: emptyList()

        if (rawArpChordName == lastArpeggioChordName && rawArpChordName != null) {
            arpeggioHoldCount++
        } else {
            lastArpeggioChordName = rawArpChordName
            arpeggioHoldCount = if (rawArpChordName != null) 1 else 0
        }

        if (arpeggioHoldCount >= ARPEGGIO_HOLD_FRAMES) {
            displayedArpeggioChord = rawArpChordName
            displayedArpeggioChordNotes = rawArpChordNotes
            if (rawArpChordName != null) lastArpeggioChordMs = now
        } else if (rawArpChordName == null) {
            displayedArpeggioChord = null
            displayedArpeggioChordNotes = emptyList()
        }

        // =====================================================================
        // Merge: most-recent-wins between simultaneous and arpeggio detection
        // =====================================================================
        val finalChord: String?
        val finalChordNotes: List<String>
        val finalIsArpeggio: Boolean

        if (displayedChord != null && displayedArpeggioChord != null) {
            if (lastSimultaneousChordMs >= lastArpeggioChordMs) {
                finalChord = displayedChord
                finalChordNotes = displayedChordNotes
                finalIsArpeggio = false
            } else {
                finalChord = displayedArpeggioChord
                finalChordNotes = displayedArpeggioChordNotes
                finalIsArpeggio = true
            }
        } else if (displayedChord != null) {
            finalChord = displayedChord
            finalChordNotes = displayedChordNotes
            finalIsArpeggio = false
        } else if (displayedArpeggioChord != null) {
            finalChord = displayedArpeggioChord
            finalChordNotes = displayedArpeggioChordNotes
            finalIsArpeggio = true
        } else {
            finalChord = null
            finalChordNotes = emptyList()
            finalIsArpeggio = false
        }

        // =====================================================================
        // Update UI state
        // =====================================================================
        _uiState.update { current ->
            val cutoff = now - HISTORY_DURATION_MS
            val trimmed = current.pitchHistory.dropWhile { it.timestampMs < cutoff }

            val updatedNotes = if (currentNote != null && currentNote != current.recentNotes.lastOrNull()) {
                (current.recentNotes + currentNote).takeLast(20)
            } else {
                current.recentNotes
            }

            current.copy(
                pitchHistory = trimmed + newPoint,
                currentNote = currentNote,
                detectedChord = finalChord,
                detectedChordNotes = finalChordNotes,
                chordConfidence = if (finalChord != null) lastChordConfidence else 0f,
                recentNotes = updatedNotes,
                isArpeggioChord = finalIsArpeggio,
                chromaEnergy = lastChromaEnergy.copyOf(),
            )
        }

        logTelemetry(
            yinResult = yinResult,
            neuralResult = neuralResult,
            finalResult = pitchResult,
            chordConfidence = lastChordConfidence,
            displayedChord = displayedChord,
        )
    }

    /**
     * Returns the median of recent frequency readings.
     * Median is more robust than mean against occasional harmonic-jump outliers.
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
        neuralSupervisor = try {
            NeuralPitchSupervisor(ctx)
        } catch (e: Exception) {
            Log.w(TAG, "Neural supervisor unavailable: ${e.message}")
            null
        }
    }

    private fun runNeuralSupervisor(samples: FloatArray): NeuralPitchResult? {
        val supervisor = neuralSupervisor ?: return null
        neuralFrameCounter++

        if (neuralFrameCounter % NEURAL_SUPERVISOR_INTERVAL == 0) {
            val estimate = supervisor.estimate(samples)
            lastNeuralResult = estimate
            neuralResultAgeFrames = 0
            updateNeuralConsistency(estimate?.frequencyHz)
        } else if (neuralResultAgeFrames < Int.MAX_VALUE) {
            neuralResultAgeFrames++
        }

        return if (neuralResultAgeFrames <= NEURAL_RESULT_TTL_FRAMES) {
            lastNeuralResult
        } else {
            null
        }
    }

    private fun arbitrate(
        yinResult: PitchResult?,
        neuralResult: NeuralPitchResult?,
    ): PitchResult? {
        if (yinResult == null) return null
        if (neuralResult == null) return yinResult

        if (neuralConsistencyFrames < NEURAL_CONSISTENCY_FRAMES) return yinResult

        val semitoneGap = semitoneDistance(yinResult.frequencyHz, neuralResult.frequencyHz)
        if (semitoneGap <= ARBITRATION_IGNORE_SEMITONES) return yinResult

        if (isOctaveRelation(yinResult.frequencyHz, neuralResult.frequencyHz) &&
            neuralResult.confidence >= 0.85 &&
            yinResult.confidence >= 0.12
        ) {
            return yinResult.copy(frequencyHz = neuralResult.frequencyHz)
        }

        return if (semitoneGap >= ARBITRATION_STRONG_SEMITONES &&
            neuralResult.confidence >= 0.93 &&
            yinResult.confidence >= 0.16
        ) {
            yinResult.copy(frequencyHz = neuralResult.frequencyHz)
        } else {
            yinResult
        }
    }

    private fun updateNeuralConsistency(neuralFrequencyHz: Double?) {
        if (neuralFrequencyHz == null || neuralFrequencyHz <= 0.0) {
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

    private fun preferredRootPitchClass(
        neuralResult: NeuralPitchResult?,
        finalResult: PitchResult?,
    ): Int? {
        val sourceHz = when {
            neuralResult != null && neuralResult.confidence >= 0.70 -> neuralResult.frequencyHz
            finalResult != null -> finalResult.frequencyHz
            else -> return null
        }
        if (sourceHz <= 0.0) return null
        val midi = 69.0 + 12.0 * log2(sourceHz / 440.0)
        val pitchClass = ((midi.roundToInt() % 12) + 12) % 12
        return pitchClass
    }

    private fun semitoneDistance(aHz: Double, bHz: Double): Double {
        if (aHz <= 0.0 || bHz <= 0.0) return Double.MAX_VALUE
        return abs(12.0 * log2(aHz / bHz))
    }

    private fun isOctaveRelation(aHz: Double, bHz: Double): Boolean {
        if (aHz <= 0.0 || bHz <= 0.0) return false
        val semitones = semitoneDistance(aHz, bHz)
        return abs(semitones - 12.0) <= 1.0 || abs(semitones - 24.0) <= 1.0
    }

    private fun logTelemetry(
        yinResult: PitchResult?,
        neuralResult: NeuralPitchResult?,
        finalResult: PitchResult?,
        chordConfidence: Float,
        displayedChord: String?,
    ) {
        if (!Log.isLoggable(TAG, Log.DEBUG)) return
        telemetryFrameCounter++
        if (telemetryFrameCounter % TELEMETRY_LOG_INTERVAL_FRAMES != 0L) return

        val yinHz = yinResult?.frequencyHz?.let { "%.2f".format(it) } ?: "null"
        val yinConf = yinResult?.confidence?.let { "%.2f".format(it) } ?: "null"
        val neuralHz = neuralResult?.frequencyHz?.let { "%.2f".format(it) } ?: "null"
        val neuralConf = neuralResult?.confidence?.let { "%.2f".format(it) } ?: "null"
        val finalHz = finalResult?.frequencyHz?.let { "%.2f".format(it) } ?: "null"
        val finalConf = finalResult?.confidence?.let { "%.2f".format(it) } ?: "null"
        val neuralMs = neuralSupervisor?.lastInferenceMs()?.let { "%.2f".format(it) } ?: "null"

        Log.d(
            TAG,
            "Telemetry frame=$telemetryFrameCounter " +
                "yinHz=$yinHz yinConf=$yinConf neuralHz=$neuralHz neuralConf=$neuralConf " +
                "finalHz=$finalHz finalConf=$finalConf neuralMs=$neuralMs " +
                "chord=${displayedChord ?: "null"} chordConf=${"%.2f".format(chordConfidence)}",
        )
    }
}

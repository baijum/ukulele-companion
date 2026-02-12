package com.baijum.ukufretboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.audio.AudioCaptureEngine
import com.baijum.ukufretboard.domain.AudioChordDetector
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.domain.PitchDetector
import com.baijum.ukufretboard.domain.TunerNoteMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.log2

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

    /** Confidence of the chord detection (0.0 .. 1.0). */
    val chordConfidence: Float = 0f,
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
         * Number of consecutive frames a chord must be stable before
         * it is shown in the UI. At ~23 ms per frame (75 % overlap),
         * 12 frames ≈ 280 ms. This prevents rapid flickering during strums.
         */
        private const val CHORD_HOLD_FRAMES = 12

        /**
         * Run chord detection (FFT + Chromagram) every Nth frame.
         *
         * The pitch path runs on every frame for smooth scrolling, but
         * chord detection is heavier (FFT + Chromagram + matching) and
         * doesn't benefit from 43 fps. Running every 4th frame keeps it
         * at ~10 Hz — the same effective rate as before the overlap change.
         */
        private const val CHORD_DETECTION_INTERVAL = 4
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

    /** Confidence of the most recent chord detection (for the UI). */
    private var lastChordConfidence = 0f

    /** Frame counter for throttling chord detection to every Nth frame. */
    private var chordFrameCounter = 0

    // --- Public API ----------------------------------------------------------

    /**
     * Starts listening to the microphone and processing audio.
     */
    fun startListening() {
        if (_uiState.value.isListening) return

        _uiState.update { it.copy(isListening = true) }
        recentFrequencies.clear()
        lastChordName = null
        chordHoldCount = 0
        displayedChord = null
        lastChordConfidence = 0f
        chordFrameCounter = 0

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
                chordConfidence = 0f,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        AudioCaptureEngine.stop()
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

        val now = System.currentTimeMillis()

        // =====================================================================
        // PATH 1: Pitch detection (YIN) → scrolling visualization
        // =====================================================================
        val pitchResult = PitchDetector.detect(samples, AudioCaptureEngine.SAMPLE_RATE)

        val midiNote: Float?
        val currentNote: String?

        if (pitchResult != null) {
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
            recentFrequencies.clear()
            midiNote = null
            currentNote = null
        }

        val newPoint = PitchPoint(timestampMs = now, midiNote = midiNote)

        // =====================================================================
        // PATH 2: Chord detection (FFT → Chromagram → ChordDetector)
        // Throttled to every CHORD_DETECTION_INTERVAL frames to avoid
        // running the heavier FFT pipeline at the full 43 fps overlap rate.
        // =====================================================================
        if (chordFrameCounter++ % CHORD_DETECTION_INTERVAL == 0) {
            val chordResult = AudioChordDetector.detect(samples)
            lastChordConfidence = chordResult.confidence

            val rawChordName = when (val det = chordResult.detection) {
                is ChordDetector.DetectionResult.ChordFound -> det.result.name
                else -> null
            }

            // Temporal smoothing: require the same chord for CHORD_HOLD_FRAMES
            if (rawChordName == lastChordName && rawChordName != null) {
                chordHoldCount++
            } else {
                lastChordName = rawChordName
                chordHoldCount = if (rawChordName != null) 1 else 0
            }

            if (chordHoldCount >= CHORD_HOLD_FRAMES) {
                displayedChord = rawChordName
            } else if (rawChordName == null) {
                // Decay: clear display after a few silent frames
                displayedChord = null
            }
        }

        // =====================================================================
        // Update UI state
        // =====================================================================
        _uiState.update { current ->
            // Trim old points beyond the history window
            val cutoff = now - HISTORY_DURATION_MS
            val trimmed = current.pitchHistory.dropWhile { it.timestampMs < cutoff }

            current.copy(
                pitchHistory = trimmed + newPoint,
                currentNote = currentNote,
                detectedChord = displayedChord,
                chordConfidence = if (displayedChord != null) lastChordConfidence else 0f,
            )
        }
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
}

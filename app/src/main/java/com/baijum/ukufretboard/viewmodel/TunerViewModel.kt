package com.baijum.ukufretboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.audio.AudioCaptureEngine
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.domain.NoteInfo
import com.baijum.ukufretboard.domain.PitchDetector
import com.baijum.ukufretboard.domain.StringMatch
import com.baijum.ukufretboard.domain.TunerNoteMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

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
        /** Cents threshold for "in tune". */
        const val IN_TUNE_CENTS = 5.0

        /** Cents threshold for "close" (between close and flat/sharp). */
        const val CLOSE_CENTS = 15.0

        /** How many recent readings to keep for smoothing. */
        private const val SMOOTHING_WINDOW = 5

        /** Milliseconds a string must stay in-tune before it's marked done. */
        private const val IN_TUNE_HOLD_MS = 2000L

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
         * RMS ratio threshold for onset (pluck) detection.
         *
         * When the current frame's RMS exceeds the previous frame's RMS by
         * this factor, a transient attack is assumed and pitch updates are
         * suppressed for [BLANKING_FRAMES] to avoid displaying spurious notes.
         */
        private const val ONSET_RATIO_THRESHOLD = 3.0f

        /**
         * Number of frames to suppress after detecting an onset.
         *
         * At ~23 ms per frame (75 % overlap), 2 frames ≈ 46 ms — aligned
         * with the typical 20–50 ms attack phase of a plucked ukulele string.
         */
        private const val BLANKING_FRAMES = 2
    }

    // --- State ---------------------------------------------------------------

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    /** Current tuning — set externally by the host screen from SettingsViewModel. */
    private var currentTuning: UkuleleTuning = UkuleleTuning.HIGH_G

    

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

    /** Remaining frames to suppress after an onset is detected. */
    private var blankingFramesRemaining = 0

    // --- Pitch continuity state ----------------------------------------------

    /**
     * Frequency detected in the previous frame, or `null` after silence /
     * start / stop.  Passed to [PitchDetector.detect] so the lag search
     * is constrained to a narrow pitch window, preventing wild jumps.
     */
    private var previousFrequency: Double? = null

    // --- Public API ----------------------------------------------------------

    /**
     * Updates the tuning used for string matching.
     */
    fun setTuning(tuning: UkuleleTuning) {
        currentTuning = tuning
        resetProgress()
    }

    /**
     * Starts listening to the microphone and processing pitch.
     */
    fun startTuning() {
        if (_uiState.value.isListening) return

        _uiState.update { it.copy(isListening = true, tuningStatus = TuningStatus.SILENT) }
        recentFrequencies.clear()
        inTuneFrames = 0
        inTuneStringIndex = -1
        previousRms = 0f
        blankingFramesRemaining = 0
        previousFrequency = null

        AudioCaptureEngine.start(viewModelScope) { buffer ->
            processBuffer(buffer)
        }
    }

    /**
     * Stops listening and releases the microphone.
     */
    fun stopTuning() {
        AudioCaptureEngine.stop()
        _uiState.update {
            it.copy(
                isListening = false,
                tuningStatus = TuningStatus.SILENT,
                detectedNote = null,
                centsDeviation = 0.0,
                targetString = null,
                noteInfo = null,
            )
        }
        recentFrequencies.clear()
        inTuneFrames = 0
        previousRms = 0f
        blankingFramesRemaining = 0
        previousFrequency = null
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
    }

    override fun onCleared() {
        super.onCleared()
        AudioCaptureEngine.stop()
    }

    // --- Internal pipeline ---------------------------------------------------

    /**
     * Processes a single audio buffer through pitch detection and note mapping.
     */
    private fun processBuffer(samples: FloatArray) {
        // --- Onset detection ------------------------------------------------
        // Detect sudden energy spikes (pluck attacks) and suppress pitch
        // updates for a few frames so the non-periodic transient doesn't
        // cause the display to flash a wrong note.
        val currentRms = PitchDetector.rms(samples)
        if (previousRms > 0f && currentRms / previousRms > ONSET_RATIO_THRESHOLD) {
            blankingFramesRemaining = BLANKING_FRAMES
        }
        previousRms = currentRms

        if (blankingFramesRemaining > 0) {
            blankingFramesRemaining--
            return // skip — likely attack transient
        }

        val result = PitchDetector.detect(
            samples,
            AudioCaptureEngine.SAMPLE_RATE,
            previousFrequency = previousFrequency,
        )

        if (result == null) {
            // Silence or aperiodic — clear continuity and decay towards neutral.
            previousFrequency = null
            recentFrequencies.clear()
            inTuneFrames = 0
            _uiState.update {
                it.copy(
                    tuningStatus = TuningStatus.SILENT,
                    detectedNote = null,
                    centsDeviation = 0.0,
                    confidence = 1.0,
                    targetString = null,
                    noteInfo = null,
                )
            }
            return
        }

        // Track for next frame's continuity constraint.
        previousFrequency = result.frequencyHz

        // --- Smoothing -------------------------------------------------------
        recentFrequencies.addLast(result.frequencyHz)
        if (recentFrequencies.size > SMOOTHING_WINDOW) {
            recentFrequencies.removeFirst()
        }
        val smoothedHz = medianFrequency()

        // --- Note mapping ----------------------------------------------------
        val noteInfo = TunerNoteMapper.mapFrequency(smoothedHz) ?: return
        val stringMatch = TunerNoteMapper.findNearestString(noteInfo, currentTuning)

        // Use cents-from-target-string for the meter (more useful than
        // cents-from-nearest-chromatic-note when tuning a specific string).
        val cents = stringMatch.centsFromTarget
        val absCents = abs(cents)

        val status = when {
            absCents <= IN_TUNE_CENTS -> TuningStatus.IN_TUNE
            absCents <= CLOSE_CENTS -> TuningStatus.CLOSE
            cents < 0 -> TuningStatus.FLAT
            else -> TuningStatus.SHARP
        }

        // --- String completion tracking --------------------------------------
        val progress = _uiState.value.stringProgress.toMutableList()

        if (status == TuningStatus.IN_TUNE && stringMatch.stringIndex == inTuneStringIndex) {
            inTuneFrames++
            val holdFrames = (IN_TUNE_HOLD_MS / FRAME_INTERVAL_MS).toInt()
            if (inTuneFrames >= holdFrames) {
                progress[stringMatch.stringIndex] = true
            }
        } else if (status == TuningStatus.IN_TUNE) {
            inTuneStringIndex = stringMatch.stringIndex
            inTuneFrames = 1
        } else {
            inTuneFrames = 0
            inTuneStringIndex = -1
        }

        // --- Emit state ------------------------------------------------------
        val displayNote = "${noteInfo.noteName}${noteInfo.octave}"

        _uiState.update {
            it.copy(
                detectedNote = displayNote,
                centsDeviation = cents.coerceIn(-50.0, 50.0),
                confidence = result.confidence,
                tuningStatus = status,
                targetString = stringMatch,
                stringProgress = progress,
                noteInfo = noteInfo,
            )
        }
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
}

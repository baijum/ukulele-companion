package com.baijum.ukufretboard.domain

import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Result of a chord detection frame, passed to [PitchMonitorStateMachine]
 * when chord detection was performed (not on throttled frames).
 *
 * @property name Detected chord name (e.g. "C", "Am7"), or null if no chord.
 * @property notes Note names of the detected chord.
 * @property confidence Confidence of the detection (0.0 .. 1.0).
 */
data class ChordFrameInput(
    val name: String?,
    val notes: List<String>,
    val confidence: Float,
)

/**
 * Output of [PitchMonitorStateMachine.processDetections] containing
 * the smoothed and merged results for a single audio frame.
 *
 * @property smoothedFrequency Median-smoothed frequency in Hz, or null when
 *   no pitch was detected.
 * @property displayedChord Final chord name after temporal smoothing and
 *   simultaneous/arpeggio merge, or null.
 * @property displayedChordNotes Note names of the displayed chord.
 * @property chordConfidence Confidence of the displayed chord (0 when null).
 * @property isArpeggioChord True when the displayed chord came from arpeggio
 *   detection rather than simultaneous chord detection.
 */
data class PitchMonitorFrame(
    val smoothedFrequency: Double?,
    val displayedChord: String?,
    val displayedChordNotes: List<String>,
    val chordConfidence: Float,
    val isArpeggioChord: Boolean,
)

/**
 * Result of [PitchMonitorStateMachine.gateFrame].
 */
sealed class FrameGateResult {
    /** Frame is blanked due to an onset transient (pluck attack). */
    data object Blanking : FrameGateResult()

    /** Frame is below the noise gate threshold. */
    data object Silent : FrameGateResult()

    /** Frame should proceed to detection and [PitchMonitorStateMachine.processDetections]. */
    data object Process : FrameGateResult()
}

/**
 * Pure-Kotlin state machine for pitch monitor frame processing.
 *
 * Owns the temporal smoothing logic shared between Android and iOS
 * pitch monitor ViewModels:
 *
 * - **Onset detection**: RMS spike detection with blanking suppression
 * - **Noise gate**: silence detection below a configurable threshold
 * - **Median frequency smoothing**: rolling median over [SMOOTHING_WINDOW] frames
 * - **Arpeggio detection**: accumulates smoothed pitch classes via [ArpeggioDetector]
 * - **Chord temporal smoothing**: hold/miss with configurable thresholds
 * - **Arpeggio temporal smoothing**: hold confirmation
 * - **Chord merge**: most-recent-wins between simultaneous and arpeggio
 *
 * Contains **no platform dependencies** — audio capture, pitch detection,
 * chord detection, and UI updates remain in the platform ViewModels.
 *
 * Typical per-frame flow:
 * ```
 * val rms = PitchDetector.rms(samples)
 * when (stateMachine.gateFrame(rms)) {
 *     FrameGateResult.Blanking -> { emitNullPitch(); return }
 *     FrameGateResult.Silent   -> { resetContinuity(); return }
 *     FrameGateResult.Process  -> { /* proceed */ }
 * }
 * val pitch = detectPitch(samples)
 * val chord = detectChord(samples) // throttled by caller
 * val frame = stateMachine.processDetections(pitch, chord, timestamp)
 * updateUi(frame)
 * ```
 *
 * @see NeuralArbitrator for the precedent shared audio state machine pattern
 */
class PitchMonitorStateMachine(arpeggioWindowMs: Long = 3_000L) {

    companion object {
        /** How many recent frequency readings to keep for median smoothing. */
        const val SMOOTHING_WINDOW = 5

        /**
         * RMS ratio threshold for onset (pluck) detection.
         *
         * When the current frame's RMS exceeds the previous frame's RMS by
         * this factor, a transient attack is assumed and pitch updates are
         * suppressed for [BLANKING_FRAMES].
         */
        const val ONSET_RATIO_THRESHOLD = 3.0f

        /**
         * Number of frames to suppress after detecting an onset.
         *
         * At ~23 ms per frame (75 % overlap), 2 frames ≈ 46 ms — aligned
         * with the typical 20–50 ms attack phase of a plucked string.
         */
        const val BLANKING_FRAMES = 2

        /**
         * Number of consecutive chord detections required before showing
         * the chord in the UI.
         */
        const val CHORD_HOLD_FRAMES = 2

        /**
         * How many detection cycles can miss before clearing a shown chord.
         * Avoids UI dropouts between strums and brief noisy frames.
         */
        const val CHORD_MISS_TOLERANCE = 3

        /**
         * Consecutive arpeggio detections required before showing the chord.
         * Lower than [CHORD_HOLD_FRAMES] because arpeggios inherently
         * accumulate over time and don't need as much confirmation.
         */
        const val ARPEGGIO_HOLD_FRAMES = 2
    }

    /**
     * RMS energy floor for the noise gate, configurable from settings.
     *
     * Written from the main thread (settings changes) and read from the
     * audio processing thread. A one-frame stale value is harmless, so
     * no synchronisation annotation is required.
     */
    var noiseGateRms: Float = 0.005f

    // --- Onset detection state -----------------------------------------------

    private var previousRms = 0f
    private var blankingFramesRemaining = 0

    // --- Frequency smoothing state -------------------------------------------

    private val smoothingBuffer = ArrayDeque<Double>(SMOOTHING_WINDOW)

    // --- Arpeggio detection --------------------------------------------------

    private val arpeggioDetector = ArpeggioDetector(windowMs = arpeggioWindowMs)

    // --- Chord temporal smoothing state --------------------------------------

    private var lastChordName: String? = null
    private var chordHoldCount = 0
    private var displayedChord: String? = null
    private var displayedChordNotes: List<String> = emptyList()
    private var lastChordConfidence = 0f
    private var chordMissCount = 0

    // --- Arpeggio temporal smoothing state -----------------------------------

    private var lastArpeggioChordName: String? = null
    private var arpeggioHoldCount = 0
    private var displayedArpeggioChord: String? = null
    private var displayedArpeggioChordNotes: List<String> = emptyList()

    // --- Chord merge state ---------------------------------------------------

    private var lastSimultaneousChordMs = 0L
    private var lastArpeggioChordMs = 0L

    // --- Public API ----------------------------------------------------------

    /**
     * Processes the RMS energy for onset detection and noise gate.
     *
     * Call this every frame before running detection. The returned
     * [FrameGateResult] determines whether detection should proceed:
     *
     * - [FrameGateResult.Blanking]: onset transient detected, skip detection
     * - [FrameGateResult.Silent]: below noise gate, skip detection
     * - [FrameGateResult.Process]: proceed with detection and call [processDetections]
     *
     * When [FrameGateResult.Silent] is returned, the frequency smoothing
     * buffer is cleared so the next active frame starts fresh.
     */
    fun gateFrame(rms: Float): FrameGateResult {
        if (previousRms > 0f && rms / previousRms > ONSET_RATIO_THRESHOLD) {
            blankingFramesRemaining = BLANKING_FRAMES
        }
        previousRms = rms

        if (blankingFramesRemaining > 0) {
            blankingFramesRemaining--
            return FrameGateResult.Blanking
        }

        if (rms < noiseGateRms) {
            smoothingBuffer.clear()
            return FrameGateResult.Silent
        }

        return FrameGateResult.Process
    }

    /**
     * Processes detection results with temporal smoothing and merge.
     *
     * Call this only when [gateFrame] returned [FrameGateResult.Process].
     *
     * @param pitchHz Detected pitch frequency in Hz after arbitration,
     *   or null if no pitch was found by the detector.
     * @param chordInput Chord detection result, or null if chord detection
     *   was not run this frame (e.g. throttled to every Nth frame).
     * @param timestampMs Current wall-clock timestamp in milliseconds.
     */
    fun processDetections(
        pitchHz: Double?,
        chordInput: ChordFrameInput?,
        timestampMs: Long,
    ): PitchMonitorFrame {
        // --- Frequency smoothing ------------------------------------------------
        val smoothedHz = if (pitchHz != null) {
            smoothingBuffer.addLast(pitchHz)
            if (smoothingBuffer.size > SMOOTHING_WINDOW) {
                smoothingBuffer.removeFirst()
            }
            medianFrequency()
        } else {
            smoothingBuffer.clear()
            null
        }

        // --- Arpeggio detection -------------------------------------------------
        if (smoothedHz != null) {
            val midiNote = 69.0 + 12.0 * log2(smoothedHz / 440.0)
            val pitchClass = ((midiNote.roundToInt() % 12) + 12) % 12
            arpeggioDetector.addNote(timestampMs, pitchClass)
        }
        val arpeggioResult = arpeggioDetector.detect(timestampMs)
        val rawArpChordName = arpeggioResult?.result?.name
        val rawArpChordNotes =
            arpeggioResult?.result?.notes?.map { it.name } ?: emptyList()

        // --- Chord temporal smoothing -------------------------------------------
        if (chordInput != null) {
            val rawChordName = chordInput.name
            lastChordConfidence = chordInput.confidence

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
                displayedChordNotes = chordInput.notes
                if (rawChordName != null) lastSimultaneousChordMs = timestampMs
            } else if (rawChordName == null && chordMissCount > CHORD_MISS_TOLERANCE) {
                displayedChord = null
                displayedChordNotes = emptyList()
                lastChordName = null
                chordHoldCount = 0
            }
        }

        // --- Arpeggio temporal smoothing ----------------------------------------
        if (rawArpChordName == lastArpeggioChordName && rawArpChordName != null) {
            arpeggioHoldCount++
        } else {
            lastArpeggioChordName = rawArpChordName
            arpeggioHoldCount = if (rawArpChordName != null) 1 else 0
        }

        if (arpeggioHoldCount >= ARPEGGIO_HOLD_FRAMES) {
            displayedArpeggioChord = rawArpChordName
            displayedArpeggioChordNotes = rawArpChordNotes
            if (rawArpChordName != null) lastArpeggioChordMs = timestampMs
        } else if (rawArpChordName == null) {
            displayedArpeggioChord = null
            displayedArpeggioChordNotes = emptyList()
        }

        // --- Merge: most-recent-wins between simultaneous and arpeggio ----------
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

        return PitchMonitorFrame(
            smoothedFrequency = smoothedHz,
            displayedChord = finalChord,
            displayedChordNotes = finalChordNotes,
            chordConfidence = if (finalChord != null) lastChordConfidence else 0f,
            isArpeggioChord = finalIsArpeggio,
        )
    }

    /**
     * Resets all internal state. Call when the pitch monitor starts or restarts.
     */
    fun reset() {
        previousRms = 0f
        blankingFramesRemaining = 0
        smoothingBuffer.clear()
        arpeggioDetector.clear()
        lastChordName = null
        chordHoldCount = 0
        displayedChord = null
        displayedChordNotes = emptyList()
        lastChordConfidence = 0f
        chordMissCount = 0
        lastArpeggioChordName = null
        arpeggioHoldCount = 0
        displayedArpeggioChord = null
        displayedArpeggioChordNotes = emptyList()
        lastSimultaneousChordMs = 0L
        lastArpeggioChordMs = 0L
    }

    // --- Internal ------------------------------------------------------------

    /**
     * Returns the median of recent frequency readings.
     * Median is more robust than mean against occasional harmonic-jump outliers.
     */
    private fun medianFrequency(): Double {
        val sorted = smoothingBuffer.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0 && sorted.size >= 2) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
}

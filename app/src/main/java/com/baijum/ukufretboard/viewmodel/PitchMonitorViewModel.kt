package com.baijum.ukufretboard.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.audio.AudioCaptureEngine
import com.baijum.ukufretboard.domain.AudioChordDetector
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.domain.ChordFrameInput
import com.baijum.ukufretboard.domain.FrameGate
import com.baijum.ukufretboard.domain.FrameGateResult
import com.baijum.ukufretboard.domain.NeuralPitchResult
import com.baijum.ukufretboard.domain.NeuralPitchSupervisor
import com.baijum.ukufretboard.domain.PitchDetector
import com.baijum.ukufretboard.domain.PitchMonitorStateMachine
import com.baijum.ukufretboard.domain.PitchResult
import com.baijum.ukufretboard.domain.TunerNoteMapper
import com.baijum.ukufretboard.domain.awaitEnterSuspending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
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
        /** Maximum pitch history duration in milliseconds (~10 seconds). */
        private const val HISTORY_DURATION_MS = 10_000L

        /**
         * Run chord detection (FFT + Chromagram) every Nth frame.
         *
         * The pitch path runs on every frame for smooth scrolling, but
         * chord detection is heavier (FFT + Chromagram + matching) and
         * doesn't benefit from 43 fps. Running every 4th frame keeps it
         * at ~21 Hz, balancing responsiveness with CPU use.
         */
        private const val CHORD_DETECTION_INTERVAL = 2

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

    // --- Shared state machine ------------------------------------------------

    private val stateMachine = PitchMonitorStateMachine()

    fun setNoiseGateRms(rms: Float) {
        stateMachine.noiseGateRms = rms
    }

    /** Frame counter for throttling chord detection to every Nth frame. */
    private var chordFrameCounter = 0

    /** Most recent chromagram from chord detection, kept across throttled frames. */
    private var lastChromaEnergy = FloatArray(12)

    /** Application context used to initialize optional neural supervisor. */
    private var appContext: Context? = null

    // --- Pitch continuity state ----------------------------------------------

    /**
     * Frequency detected in the previous frame, or `null` after silence /
     * start.  Passed to [PitchDetector.detect] so the lag search is
     * constrained to a narrow pitch window, preventing wild jumps.
     */
    private var previousFrequency: Double? = null

    // --- Frame-dropping (backpressure) ----------------------------------------
    private val frameGate = FrameGate()

    // --- Neural supervisor state ----------------------------------------------

    private val supervisorLock = Any()
    private var isCleared = false
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
        val ctx = appContext ?: return

        initializeNeuralSupervisor()

        _uiState.update { it.copy(isListening = true) }

        viewModelScope.launch {
            if (!frameGate.awaitEnterSuspending()) {
                _uiState.update { it.copy(isListening = false) }
                return@launch
            }
            try {
                stateMachine.reset()
                chordFrameCounter = 0
                lastChromaEnergy = FloatArray(12)
                previousFrequency = null
                neuralFrameCounter = 0
                lastNeuralResult = null
                neuralResultAgeFrames = Int.MAX_VALUE
                lastNeuralFrequencyForConsistency = null
                neuralConsistencyFrames = 0
                telemetryFrameCounter = 0
            } finally {
                frameGate.exit()
            }

            AudioCaptureEngine.start(
                viewModelScope,
                ctx,
                onInterrupted = { stopListening() },
            ) { buffer ->
                processBuffer(buffer)
            }
        }
    }

    /**
     * Stops listening and releases the microphone.
     *
     * Internal processing state (the [stateMachine], neural supervisor, etc.)
     * is deliberately **not** cleared here because [processBuffer] may still
     * be executing on a background thread ([kotlinx.coroutines.Dispatchers.Default]).
     * Clearing non-thread-safe state from the main thread while the background
     * thread uses it causes a crash.
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
        val supervisor =
            synchronized(supervisorLock) {
                isCleared = true
                val s = neuralSupervisor
                neuralSupervisor = null
                s
            }
        AudioCaptureEngine.stop()
        if (frameGate.tryEnter()) {
            try {
                supervisor?.close()
            } finally {
                frameGate.exit()
            }
        } else {
            supervisor?.close()
        }
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
        if (!frameGate.tryEnter()) return
        try {
            processBufferInner(samples)
        } finally {
            frameGate.exit()
        }
    }

    private fun processBufferInner(samples: FloatArray) {
        val n = samples.size
        if (n == 0 || n and (n - 1) != 0) return

        val currentRms = PitchDetector.rms(samples)
        val now = System.currentTimeMillis()

        when (stateMachine.gateFrame(currentRms)) {
            FrameGateResult.Blanking -> {
                val newPoint = PitchPoint(timestampMs = now, midiNote = null)
                _uiState.update { current ->
                    val cutoff = now - HISTORY_DURATION_MS
                    val trimmed = current.pitchHistory.dropWhile { it.timestampMs < cutoff }
                    current.copy(pitchHistory = trimmed + newPoint)
                }
                return
            }

            FrameGateResult.Silent -> {
                previousFrequency = null
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

            FrameGateResult.Process -> { /* proceed with detection */ }
        }

        // --- Pitch detection (YIN + neural arbitration) -------------------------
        val yinResult =
            PitchDetector.detect(
                samples,
                AudioCaptureEngine.SAMPLE_RATE,
                previousFrequency = previousFrequency,
            )
        val neuralResult = runNeuralSupervisor(samples)
        val pitchResult = arbitrate(yinResult, neuralResult)

        previousFrequency = pitchResult?.frequencyHz

        // --- Chord detection (throttled) ----------------------------------------
        val chordInput: ChordFrameInput? =
            if (chordFrameCounter++ % CHORD_DETECTION_INTERVAL == 0) {
                val preferredRootPitchClass = preferredRootPitchClass(neuralResult, pitchResult)
                val chordResult =
                    AudioChordDetector.detect(
                        samples = samples,
                        preferredRootPitchClass = preferredRootPitchClass,
                    )
                lastChromaEnergy = chordResult.chromagram

                val rawChordName =
                    when (val det = chordResult.detection) {
                        is ChordDetector.DetectionResult.ChordFound -> det.result.name
                        else -> null
                    }
                val rawChordNotes =
                    when (val det = chordResult.detection) {
                        is ChordDetector.DetectionResult.ChordFound -> det.result.notes.map { it.name }
                        else -> emptyList()
                    }

                ChordFrameInput(
                    name = rawChordName,
                    notes = rawChordNotes,
                    confidence = chordResult.confidence,
                )
            } else {
                null
            }

        // --- Delegate to shared state machine -----------------------------------
        val frame =
            stateMachine.processDetections(
                pitchHz = pitchResult?.frequencyHz,
                chordInput = chordInput,
                timestampMs = now,
            )

        // --- Build UI state from frame output -----------------------------------
        val midiNote: Float? =
            frame.smoothedFrequency?.let {
                (69.0 + 12.0 * log2(it / 440.0)).toFloat()
            }

        val currentNote: String? =
            frame.smoothedFrequency?.let { hz ->
                TunerNoteMapper.mapFrequency(hz)?.let { "${it.noteName}${it.octave}" }
            }

        val newPoint = PitchPoint(timestampMs = now, midiNote = midiNote)

        _uiState.update { current ->
            val cutoff = now - HISTORY_DURATION_MS
            val trimmed = current.pitchHistory.dropWhile { it.timestampMs < cutoff }

            val updatedNotes =
                if (currentNote != null && currentNote != current.recentNotes.lastOrNull()) {
                    (current.recentNotes + currentNote).takeLast(20)
                } else {
                    current.recentNotes
                }

            current.copy(
                pitchHistory = trimmed + newPoint,
                currentNote = currentNote,
                detectedChord = frame.displayedChord,
                detectedChordNotes = frame.displayedChordNotes,
                chordConfidence = frame.chordConfidence,
                recentNotes = updatedNotes,
                isArpeggioChord = frame.isArpeggioChord,
                chromaEnergy = lastChromaEnergy.copyOf(),
            )
        }

        logTelemetry(
            yinResult = yinResult,
            neuralResult = neuralResult,
            finalResult = pitchResult,
            chordConfidence = frame.chordConfidence,
            displayedChord = frame.displayedChord,
        )
    }

    private fun initializeNeuralSupervisor() {
        synchronized(supervisorLock) {
            if (neuralSupervisor != null) return
        }
        val ctx = appContext ?: return
        viewModelScope.launch {
            var supervisor: NeuralPitchSupervisor? = null
            var registered = false
            try {
                supervisor =
                    withContext(Dispatchers.IO) {
                        try {
                            NeuralPitchSupervisor(ctx)
                        } catch (e: Exception) {
                            Log.w(TAG, "Neural supervisor unavailable: ${e.message}")
                            null
                        }
                    }
                synchronized(supervisorLock) {
                    if (!isCleared) {
                        neuralSupervisor = supervisor
                        registered = true
                    }
                }
            } finally {
                if (!registered) supervisor?.close()
            }
        }
    }

    private fun runNeuralSupervisor(samples: FloatArray): NeuralPitchResult? {
        val supervisor = synchronized(supervisorLock) { neuralSupervisor } ?: return null
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
        neuralConsistencyFrames =
            if (previous != null &&
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
        val sourceHz =
            when {
                neuralResult != null && neuralResult.confidence >= 0.70 -> neuralResult.frequencyHz
                finalResult != null -> finalResult.frequencyHz
                else -> return null
            }
        if (sourceHz <= 0.0) return null
        val midi = 69.0 + 12.0 * log2(sourceHz / 440.0)
        val pitchClass = ((midi.roundToInt() % 12) + 12) % 12
        return pitchClass
    }

    private fun semitoneDistance(
        aHz: Double,
        bHz: Double,
    ): Double {
        if (aHz <= 0.0 || bHz <= 0.0) return Double.MAX_VALUE
        return abs(12.0 * log2(aHz / bHz))
    }

    private fun isOctaveRelation(
        aHz: Double,
        bHz: Double,
    ): Boolean {
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
        val neuralMs =
            synchronized(supervisorLock) { neuralSupervisor }
                ?.lastInferenceMs()
                ?.let { "%.2f".format(it) } ?: "null"

        Log.d(
            TAG,
            "Telemetry frame=$telemetryFrameCounter " +
                "yinHz=$yinHz yinConf=$yinConf neuralHz=$neuralHz neuralConf=$neuralConf " +
                "finalHz=$finalHz finalConf=$finalConf neuralMs=$neuralMs " +
                "chord=${displayedChord ?: "null"} chordConf=${"%.2f".format(chordConfidence)}",
        )
    }
}

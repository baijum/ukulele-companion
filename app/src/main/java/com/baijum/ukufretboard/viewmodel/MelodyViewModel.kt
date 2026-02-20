package com.baijum.ukufretboard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.audio.AudioCaptureEngine
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.Melody
import com.baijum.ukufretboard.data.MelodyNote
import com.baijum.ukufretboard.data.MelodyRepository
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.domain.NoteInfo
import com.baijum.ukufretboard.domain.PitchDetector
import com.baijum.ukufretboard.domain.TunerNoteMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Input mode for adding notes to the melody.
 */
enum class MelodyInputMode {
    TAP,
    RECORD,
}

/**
 * UI state for the Melody Notepad screen.
 */
data class MelodyUiState(
    val notes: List<MelodyNote> = emptyList(),
    val selectedDuration: NoteDuration = NoteDuration.QUARTER,
    val bpm: Int = 120,
    val isPlaying: Boolean = false,
    val playingIndex: Int = -1,
    val selectedNoteIndex: Int = -1,
    val inputMode: MelodyInputMode = MelodyInputMode.TAP,
    val currentOctave: Int = 4,
    /** Currently loaded melody ID, or null if unsaved. */
    val loadedMelodyId: String? = null,
    /** Name of the currently loaded melody. */
    val loadedMelodyName: String? = null,
    /** Whether notes have changed since last save. */
    val hasUnsavedChanges: Boolean = false,
    /** All saved melodies for the load dialog. */
    val savedMelodies: List<Melody> = emptyList(),
    /** Whether the microphone is actively listening. */
    val isRecording: Boolean = false,
    /** The note currently being detected by the microphone. */
    val detectedNote: NoteInfo? = null,
    /** Consecutive frames the same note has been detected. */
    val stabilizationProgress: Float = 0f,
    /** Last note that was auto-added via recording (for feedback). */
    val lastAddedFeedback: String? = null,
)

/**
 * ViewModel for the Melody Notepad feature.
 *
 * Manages melody composition state, persistence via [MelodyRepository],
 * and microphone-based note recording using the existing pitch detection pipeline.
 */
class MelodyViewModel : ViewModel() {

    companion object {
        private const val MIN_OCTAVE = 3
        private const val MAX_OCTAVE = 6
        /**
         * Frames of consistent detection required before accepting a note (~250ms).
         * At ~43 frames/sec (23ms per frame), 11 frames ≈ 253ms.
         */
        private const val STABILIZATION_FRAMES = 11
        private const val SMOOTHING_WINDOW = 5
        private const val ONSET_RATIO_THRESHOLD = 3.0f
        private const val BLANKING_FRAMES = 2
        private const val FEEDBACK_DURATION_MS = 1500L
    }

    private val _uiState = MutableStateFlow(MelodyUiState())
    val uiState: StateFlow<MelodyUiState> = _uiState.asStateFlow()

    private var repository: MelodyRepository? = null
    private var soundSettings: SoundSettings = SoundSettings()

    // --- Recording state ---
    private var previousRms = 0f
    private var blankingFramesRemaining = 0
    private var previousFrequency: Double? = null
    private val recentFrequencies = ArrayDeque<Double>(SMOOTHING_WINDOW)
    private var stableNoteInfo: NoteInfo? = null
    private var stabilizationFrames = 0

    fun setApplicationContext(context: Context) {
        if (repository == null) {
            repository = MelodyRepository(context.applicationContext)
            refreshSavedMelodies()
        }
    }

    fun setSoundSettings(settings: SoundSettings) {
        soundSettings = settings
    }

    // --- Note composition ---

    fun addNote(pitchClass: Int, octave: Int? = null) {
        val state = _uiState.value
        val note = MelodyNote(
            pitchClass = pitchClass,
            octave = octave ?: state.currentOctave,
            duration = state.selectedDuration,
        )
        _uiState.update {
            it.copy(
                notes = it.notes + note,
                hasUnsavedChanges = true,
            )
        }
        playNote(pitchClass, octave ?: state.currentOctave)
    }

    fun addRest() {
        val state = _uiState.value
        val rest = MelodyNote(pitchClass = null, duration = state.selectedDuration)
        _uiState.update {
            it.copy(
                notes = it.notes + rest,
                hasUnsavedChanges = true,
            )
        }
    }

    fun deleteSelectedNote() {
        val state = _uiState.value
        val idx = state.selectedNoteIndex
        if (idx !in state.notes.indices) return
        _uiState.update {
            it.copy(
                notes = it.notes.toMutableList().apply { removeAt(idx) },
                selectedNoteIndex = -1,
                hasUnsavedChanges = true,
            )
        }
    }

    fun selectNote(index: Int) {
        val current = _uiState.value.selectedNoteIndex
        _uiState.update { it.copy(selectedNoteIndex = if (current == index) -1 else index) }
    }

    fun clearAll() {
        _uiState.update {
            it.copy(
                notes = emptyList(),
                selectedNoteIndex = -1,
                playingIndex = -1,
                hasUnsavedChanges = it.loadedMelodyId != null || it.notes.isNotEmpty(),
            )
        }
    }

    fun setDuration(duration: NoteDuration) {
        _uiState.update { it.copy(selectedDuration = duration) }
    }

    fun setBpm(bpm: Int) {
        _uiState.update {
            it.copy(
                bpm = bpm,
                hasUnsavedChanges = true,
            )
        }
    }

    // --- Octave control ---

    fun setOctave(octave: Int) {
        _uiState.update { it.copy(currentOctave = octave.coerceIn(MIN_OCTAVE, MAX_OCTAVE)) }
    }

    fun incrementOctave() {
        _uiState.update {
            it.copy(currentOctave = (it.currentOctave + 1).coerceAtMost(MAX_OCTAVE))
        }
    }

    fun decrementOctave() {
        _uiState.update {
            it.copy(currentOctave = (it.currentOctave - 1).coerceAtLeast(MIN_OCTAVE))
        }
    }

    // --- Input mode ---

    fun setInputMode(mode: MelodyInputMode) {
        if (_uiState.value.isRecording) stopRecording()
        _uiState.update { it.copy(inputMode = mode) }
    }

    // --- Playback ---

    fun playMelody() {
        val state = _uiState.value
        if (state.isPlaying || state.notes.isEmpty()) return
        if (state.isRecording) stopRecording()

        _uiState.update { it.copy(isPlaying = true) }
        viewModelScope.launch {
            state.notes.forEachIndexed { index, note ->
                if (!_uiState.value.isPlaying) return@launch
                _uiState.update { it.copy(playingIndex = index) }
                if (note.pitchClass != null) {
                    playNote(note.pitchClass, note.octave)
                }
                val durationMs = (note.duration.beats * 60_000f / state.bpm).toLong()
                delay(durationMs)
            }
            _uiState.update { it.copy(playingIndex = -1, isPlaying = false) }
        }
    }

    fun stopPlayback() {
        _uiState.update { it.copy(isPlaying = false, playingIndex = -1) }
    }

    private fun playNote(pitchClass: Int, octave: Int) {
        if (!soundSettings.enabled) return
        viewModelScope.launch {
            ToneGenerator.playNote(
                pitchClass = pitchClass,
                octave = octave,
                durationMs = soundSettings.noteDurationMs,
                volume = soundSettings.volume,
            )
        }
    }

    // --- Persistence ---

    fun saveMelody(name: String) {
        val repo = repository ?: return
        val state = _uiState.value
        val melody = Melody(
            id = state.loadedMelodyId ?: java.util.UUID.randomUUID().toString(),
            name = name,
            notes = state.notes,
            bpm = state.bpm,
            createdAt = if (state.loadedMelodyId != null) {
                repo.get(state.loadedMelodyId)?.createdAt ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            },
        )
        repo.save(melody)
        _uiState.update {
            it.copy(
                loadedMelodyId = melody.id,
                loadedMelodyName = melody.name,
                hasUnsavedChanges = false,
            )
        }
        refreshSavedMelodies()
    }

    fun loadMelody(melody: Melody) {
        _uiState.update {
            it.copy(
                notes = melody.notes,
                bpm = melody.bpm,
                loadedMelodyId = melody.id,
                loadedMelodyName = melody.name,
                hasUnsavedChanges = false,
                selectedNoteIndex = -1,
                playingIndex = -1,
                isPlaying = false,
            )
        }
    }

    fun deleteMelody(id: String) {
        val repo = repository ?: return
        repo.delete(id)
        val state = _uiState.value
        if (state.loadedMelodyId == id) {
            _uiState.update {
                it.copy(
                    loadedMelodyId = null,
                    loadedMelodyName = null,
                )
            }
        }
        refreshSavedMelodies()
    }

    fun renameMelody(id: String, newName: String) {
        val repo = repository ?: return
        val melody = repo.get(id) ?: return
        val updated = melody.copy(name = newName)
        repo.save(updated)
        val state = _uiState.value
        if (state.loadedMelodyId == id) {
            _uiState.update { it.copy(loadedMelodyName = newName) }
        }
        refreshSavedMelodies()
    }

    fun newMelody() {
        if (_uiState.value.isRecording) stopRecording()
        _uiState.update {
            it.copy(
                notes = emptyList(),
                bpm = 120,
                loadedMelodyId = null,
                loadedMelodyName = null,
                hasUnsavedChanges = false,
                selectedNoteIndex = -1,
                playingIndex = -1,
                isPlaying = false,
            )
        }
    }

    private fun refreshSavedMelodies() {
        val repo = repository ?: return
        _uiState.update { it.copy(savedMelodies = repo.getAll()) }
    }

    // --- Microphone recording ---

    fun startRecording() {
        if (_uiState.value.isRecording) return
        if (_uiState.value.isPlaying) stopPlayback()

        resetRecordingState()
        _uiState.update {
            it.copy(
                isRecording = true,
                detectedNote = null,
                stabilizationProgress = 0f,
            )
        }

        AudioCaptureEngine.start(viewModelScope) { buffer ->
            processRecordingBuffer(buffer)
        }
    }

    fun stopRecording() {
        AudioCaptureEngine.stop()
        _uiState.update {
            it.copy(
                isRecording = false,
                detectedNote = null,
                stabilizationProgress = 0f,
                lastAddedFeedback = null,
            )
        }
        resetRecordingState()
    }

    private fun resetRecordingState() {
        previousRms = 0f
        blankingFramesRemaining = 0
        previousFrequency = null
        recentFrequencies.clear()
        stableNoteInfo = null
        stabilizationFrames = 0
    }

    private fun processRecordingBuffer(samples: FloatArray) {
        val currentRms = PitchDetector.rms(samples)
        if (previousRms > 0f && currentRms / previousRms > ONSET_RATIO_THRESHOLD) {
            blankingFramesRemaining = BLANKING_FRAMES
        }
        previousRms = currentRms

        if (blankingFramesRemaining > 0) {
            blankingFramesRemaining--
            return
        }

        val result = PitchDetector.detect(
            samples,
            AudioCaptureEngine.SAMPLE_RATE,
            previousFrequency = previousFrequency,
        )

        if (result == null) {
            previousFrequency = null
            recentFrequencies.clear()
            stableNoteInfo = null
            stabilizationFrames = 0
            _uiState.update {
                it.copy(detectedNote = null, stabilizationProgress = 0f)
            }
            return
        }

        previousFrequency = result.frequencyHz

        recentFrequencies.addLast(result.frequencyHz)
        if (recentFrequencies.size > SMOOTHING_WINDOW) {
            recentFrequencies.removeFirst()
        }
        val smoothedHz = recentFrequencies.sorted().let { sorted ->
            val mid = sorted.size / 2
            if (sorted.size % 2 == 0 && sorted.size >= 2) {
                (sorted[mid - 1] + sorted[mid]) / 2.0
            } else {
                sorted[mid]
            }
        }

        val noteInfo = TunerNoteMapper.mapFrequency(
            smoothedHz,
            TunerNoteMapper.DEFAULT_A4_HZ,
        ) ?: return

        val previousStable = stableNoteInfo
        if (previousStable != null &&
            previousStable.pitchClass == noteInfo.pitchClass &&
            previousStable.octave == noteInfo.octave
        ) {
            stabilizationFrames++
        } else {
            stableNoteInfo = noteInfo
            stabilizationFrames = 1
        }

        val progress = (stabilizationFrames.toFloat() / STABILIZATION_FRAMES).coerceAtMost(1f)
        _uiState.update {
            it.copy(detectedNote = noteInfo, stabilizationProgress = progress)
        }

        if (stabilizationFrames >= STABILIZATION_FRAMES) {
            acceptRecordedNote(noteInfo)
            stableNoteInfo = null
            stabilizationFrames = 0
            recentFrequencies.clear()
            previousFrequency = null
        }
    }

    private fun acceptRecordedNote(noteInfo: NoteInfo) {
        val state = _uiState.value
        val note = MelodyNote(
            pitchClass = noteInfo.pitchClass,
            octave = noteInfo.octave,
            duration = state.selectedDuration,
        )
        val noteName = Notes.pitchClassToName(noteInfo.pitchClass)
        val durationLabel = state.selectedDuration.label.lowercase()
        val feedback = "$noteName${noteInfo.octave} $durationLabel"

        _uiState.update {
            it.copy(
                notes = it.notes + note,
                hasUnsavedChanges = true,
                stabilizationProgress = 0f,
                detectedNote = null,
                lastAddedFeedback = feedback,
            )
        }

        playNote(noteInfo.pitchClass, noteInfo.octave)

        viewModelScope.launch {
            delay(FEEDBACK_DURATION_MS)
            _uiState.update {
                if (it.lastAddedFeedback == feedback) {
                    it.copy(lastAddedFeedback = null)
                } else {
                    it
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.isRecording) {
            AudioCaptureEngine.stop()
        }
    }
}

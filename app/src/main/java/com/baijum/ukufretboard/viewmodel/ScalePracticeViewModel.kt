package com.baijum.ukufretboard.viewmodel

import androidx.lifecycle.ViewModel
import com.baijum.ukufretboard.audio.MetronomeEngine
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.PlayDirection
import com.baijum.ukufretboard.data.FretPosition
import com.baijum.ukufretboard.data.PracticeMode
import com.baijum.ukufretboard.data.Scale
import com.baijum.ukufretboard.data.ScaleCategory
import com.baijum.ukufretboard.data.ScalePracticeSettings
import com.baijum.ukufretboard.data.Scales
import com.baijum.ukufretboard.domain.ScalePracticeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Play Along playback state.
 */
enum class PlaybackState {
    STOPPED, PLAYING, PAUSED,
}

/**
 * UI state for the Scale Practice screen.
 */
data class ScalePracticeUiState(
    // ── Shared state ──
    val mode: PracticeMode = PracticeMode.PLAY_ALONG,
    val selectedCategory: ScaleCategory? = null,
    val selectedRoot: Int = 0,
    val selectedScale: Scale = Scales.ALL.first(),
    val availableScales: List<Scale> = Scales.ALL,

    // ── Play Along ──
    val bpm: Int = ScalePracticeSettings.DEFAULT_BPM,
    val direction: PlayDirection = PlayDirection.ASCENDING,
    val playbackState: PlaybackState = PlaybackState.STOPPED,
    val currentNoteIndex: Int = 0,
    val playAlongNotes: List<Int> = emptyList(), // pitch classes in play order
    val showFretboard: Boolean = false,
    val loopPlayback: Boolean = false,
    val fretPosition: FretPosition = FretPosition.ALL,

    // ── Quiz ──
    val quizQuestion: ScalePracticeGenerator.ScaleQuizQuestion? = null,
    val quizSelectedAnswer: Int? = null,
    val quizCorrect: Int = 0,
    val quizTotal: Int = 0,
    val quizStreak: Int = 0,
    val quizBestStreak: Int = 0,

    // ── Ear Training ──
    val earQuestion: ScalePracticeGenerator.EarTrainingQuestion? = null,
    val earSelectedAnswer: Int? = null,
    val earCorrect: Int = 0,
    val earTotal: Int = 0,
    val earStreak: Int = 0,
    val earBestStreak: Int = 0,
)

/**
 * ViewModel for the Scale Practice screen.
 *
 * Manages state for three practice modes: Play Along, Scale Quiz,
 * and Scale Ear Training. Uses [MetronomeEngine] for tempo-based
 * playback and [ToneGenerator] for audio.
 */
class ScalePracticeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScalePracticeUiState())
    val uiState: StateFlow<ScalePracticeUiState> = _uiState.asStateFlow()

    private val metronome = MetronomeEngine()

    // ── Initialization from saved settings ──────────────────────────

    /**
     * Restores practice settings from persisted [ScalePracticeSettings].
     * Call once after the ViewModel is created.
     */
    fun restoreSettings(settings: ScalePracticeSettings) {
        val category = ScaleCategory.entries.firstOrNull { it.name == settings.lastCategory }
        val scales = Scales.forCategory(category).ifEmpty { Scales.ALL }
        val scale = scales.firstOrNull { it.name == settings.lastScaleName } ?: scales.first()
        val mode = PracticeMode.entries.getOrElse(settings.lastMode) { PracticeMode.PLAY_ALONG }

        val direction = PlayDirection.entries.getOrElse(settings.lastDirection) { PlayDirection.ASCENDING }
        val fretPosition = FretPosition.entries.getOrElse(settings.lastFretPosition) { FretPosition.ALL }

        _uiState.update {
            it.copy(
                mode = mode,
                selectedCategory = category,
                selectedRoot = settings.lastRoot.coerceIn(0, 11),
                selectedScale = scale,
                availableScales = scales,
                bpm = settings.lastBpm.coerceIn(
                    ScalePracticeSettings.MIN_BPM,
                    ScalePracticeSettings.MAX_BPM,
                ),
                showFretboard = settings.showFretboard,
                direction = direction,
                loopPlayback = settings.loopPlayback,
                fretPosition = fretPosition,
            )
        }
    }

    /**
     * Returns current settings for persistence.
     */
    fun currentSettings(): ScalePracticeSettings {
        val s = _uiState.value
        return ScalePracticeSettings(
            lastRoot = s.selectedRoot,
            lastScaleName = s.selectedScale.name,
            lastCategory = s.selectedCategory?.name ?: "",
            lastBpm = s.bpm,
            lastMode = s.mode.ordinal,
            showFretboard = s.showFretboard,
            lastDirection = s.direction.ordinal,
            loopPlayback = s.loopPlayback,
            lastFretPosition = s.fretPosition.ordinal,
        )
    }

    // ── Shared controls ─────────────────────────────────────────────

    fun setMode(mode: PracticeMode) {
        stopPlayback()
        _uiState.update { it.copy(mode = mode) }
    }

    fun setCategory(category: ScaleCategory?) {
        stopPlayback()
        val scales = Scales.forCategory(category).ifEmpty { Scales.ALL }
        val currentScale = _uiState.value.selectedScale
        val newScale = if (currentScale in scales) currentScale else scales.first()
        _uiState.update {
            it.copy(
                selectedCategory = category,
                availableScales = scales,
                selectedScale = newScale,
            )
        }
    }

    fun setRoot(root: Int) {
        stopPlayback()
        _uiState.update { it.copy(selectedRoot = root.coerceIn(0, 11)) }
    }

    fun setScale(scale: Scale) {
        stopPlayback()
        _uiState.update { it.copy(selectedScale = scale) }
    }

    fun toggleFretboard() {
        _uiState.update { it.copy(showFretboard = !it.showFretboard) }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(loopPlayback = !it.loopPlayback) }
    }

    fun setFretPosition(position: FretPosition) {
        _uiState.update { it.copy(fretPosition = position) }
    }

    // ── Play Along ──────────────────────────────────────────────────

    fun setBpm(bpm: Int) {
        _uiState.update {
            it.copy(bpm = bpm.coerceIn(ScalePracticeSettings.MIN_BPM, ScalePracticeSettings.MAX_BPM))
        }
    }

    fun setDirection(direction: PlayDirection) {
        stopPlayback()
        _uiState.update { it.copy(direction = direction) }
    }

    /**
     * Builds the sequence of pitch classes to play based on root, scale, and direction.
     */
    private fun buildPlaySequence(): List<Int> {
        val state = _uiState.value
        val root = state.selectedRoot
        val intervals = state.selectedScale.intervals

        val ascending = intervals.map { (root + it) % Notes.PITCH_CLASS_COUNT }
        return when (state.direction) {
            PlayDirection.ASCENDING -> ascending
            PlayDirection.DESCENDING -> ascending.reversed()
            PlayDirection.BOTH -> ascending + ascending.reversed().drop(1)
        }
    }

    /**
     * Starts or resumes Play Along playback.
     *
     * @param scope Coroutine scope for the metronome and audio.
     */
    fun startPlayback(scope: CoroutineScope) {
        val notes = buildPlaySequence()
        if (notes.isEmpty()) return

        _uiState.update {
            it.copy(
                playbackState = PlaybackState.PLAYING,
                currentNoteIndex = 0,
                playAlongNotes = notes,
            )
        }

        metronome.start(
            scope = scope,
            bpm = _uiState.value.bpm,
            beatsPerChord = 1,
            chordCount = notes.size,
            loop = _uiState.value.loopPlayback,
            onBeat = { noteIndex, _ ->
                _uiState.update { it.copy(currentNoteIndex = noteIndex) }
                // Play the note audio
                scope.launch {
                    ToneGenerator.playNote(
                        pitchClass = notes[noteIndex],
                        octave = 4,
                        durationMs = (60_000 / _uiState.value.bpm).coerceIn(200, 800),
                    )
                }
            },
            onComplete = {
                _uiState.update { it.copy(playbackState = PlaybackState.STOPPED) }
            },
        )
    }

    fun stopPlayback() {
        metronome.stop()
        _uiState.update {
            it.copy(
                playbackState = PlaybackState.STOPPED,
                currentNoteIndex = 0,
            )
        }
    }

    // ── Quiz ────────────────────────────────────────────────────────

    fun generateQuizQuestion() {
        val question = ScalePracticeGenerator.generateQuizQuestion(_uiState.value.selectedCategory)
        _uiState.update { it.copy(quizQuestion = question, quizSelectedAnswer = null) }
    }

    /**
     * Submits a quiz answer. Returns true if correct.
     */
    fun submitQuizAnswer(answerIndex: Int): Boolean {
        val question = _uiState.value.quizQuestion ?: return false
        val correct = answerIndex == question.correctIndex
        _uiState.update { state ->
            val newTotal = state.quizTotal + 1
            val newCorrect = state.quizCorrect + if (correct) 1 else 0
            val newStreak = if (correct) state.quizStreak + 1 else 0
            val newBest = maxOf(state.quizBestStreak, newStreak)
            state.copy(
                quizSelectedAnswer = answerIndex,
                quizTotal = newTotal,
                quizCorrect = newCorrect,
                quizStreak = newStreak,
                quizBestStreak = newBest,
            )
        }
        return correct
    }

    // ── Ear Training ────────────────────────────────────────────────

    fun generateEarQuestion() {
        val question = ScalePracticeGenerator.generateEarQuestion(_uiState.value.selectedCategory)
        _uiState.update { it.copy(earQuestion = question, earSelectedAnswer = null) }
    }

    /**
     * Plays the ear training scale audio.
     */
    fun playEarScale(scope: CoroutineScope) {
        val question = _uiState.value.earQuestion ?: return
        val notes = question.scale.intervals.map {
            (question.root + it) % Notes.PITCH_CLASS_COUNT
        }
        scope.launch {
            for (pc in notes) {
                ToneGenerator.playNote(pitchClass = pc, octave = 4, durationMs = 400)
                kotlinx.coroutines.delay(250)
            }
        }
    }

    /**
     * Submits an ear training answer. Returns true if correct.
     */
    fun submitEarAnswer(answerIndex: Int): Boolean {
        val question = _uiState.value.earQuestion ?: return false
        val correct = answerIndex == question.correctIndex
        _uiState.update { state ->
            val newTotal = state.earTotal + 1
            val newCorrect = state.earCorrect + if (correct) 1 else 0
            val newStreak = if (correct) state.earStreak + 1 else 0
            val newBest = maxOf(state.earBestStreak, newStreak)
            state.copy(
                earSelectedAnswer = answerIndex,
                earTotal = newTotal,
                earCorrect = newCorrect,
                earStreak = newStreak,
                earBestStreak = newBest,
            )
        }
        return correct
    }

    override fun onCleared() {
        super.onCleared()
        metronome.stop()
    }
}

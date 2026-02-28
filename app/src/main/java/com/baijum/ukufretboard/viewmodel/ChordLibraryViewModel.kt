package com.baijum.ukufretboard.viewmodel

import androidx.lifecycle.ViewModel
import com.baijum.ukufretboard.data.ChordCategory
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.Transpose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Immutable UI state for the chord library tab.
 *
 * @property selectedRoot The pitch class (0–11) of the currently selected root note.
 * @property selectedCategory The currently selected chord category filter.
 * @property selectedFormula The currently selected chord formula, or null if none is chosen.
 * @property voicings The generated voicings for the current root + formula selection.
 */
data class ChordLibraryUiState(
    val selectedRoot: Int = 0,
    val selectedCategory: ChordCategory = ChordCategory.TRIAD,
    val selectedFormula: ChordFormula? = null,
    val voicings: List<ChordVoicing> = emptyList(),
)

/**
 * ViewModel for the chord library tab.
 *
 * Manages the selection of root note, chord category, and chord formula,
 * and generates playable voicings via [VoicingGenerator] whenever the
 * selection changes.
 */
class ChordLibraryViewModel : ViewModel() {

    /** The tuning used for voicing generation. */
    private var tuning = FretboardViewModel.STANDARD_TUNING

    /** Whether the voicing generator may include voicings with muted strings. */
    private var allowMutedStrings: Boolean = false

    private val _uiState = MutableStateFlow(
        ChordLibraryUiState().let { initial ->
            // Auto-select the first formula in the default category
            val firstFormula = ChordFormulas.BY_CATEGORY[initial.selectedCategory]?.firstOrNull()
            if (firstFormula != null) {
                initial.copy(
                    selectedFormula = firstFormula,
                    voicings = VoicingGenerator.generate(
                        rootPitchClass = initial.selectedRoot,
                        formula = firstFormula,
                        tuning = tuning,
                    ),
                )
            } else {
                initial
            }
        }
    )

    /** Observable UI state for the chord library tab. */
    val uiState: StateFlow<ChordLibraryUiState> = _uiState.asStateFlow()

    /**
     * Updates the tuning used by voicing generation and refreshes current results.
     */
    fun setTuning(tuning: List<UkuleleString>) {
        if (this.tuning == tuning) return
        this.tuning = tuning
        _uiState.update { current ->
            current.copy(
                voicings = generateVoicings(current.selectedRoot, current.selectedFormula),
            )
        }
    }

    /**
     * Selects a root note and regenerates voicings.
     *
     * @param pitchClass The pitch class (0–11) of the root note.
     */
    fun selectRoot(pitchClass: Int) {
        _uiState.update { current ->
            current.copy(
                selectedRoot = pitchClass,
                voicings = generateVoicings(pitchClass, current.selectedFormula),
            )
        }
    }

    /**
     * Selects a chord category and auto-selects its first formula.
     *
     * @param category The [ChordCategory] to switch to.
     */
    fun selectCategory(category: ChordCategory) {
        val firstFormula = ChordFormulas.BY_CATEGORY[category]?.firstOrNull()
        _uiState.update { current ->
            current.copy(
                selectedCategory = category,
                selectedFormula = firstFormula,
                voicings = generateVoicings(current.selectedRoot, firstFormula),
            )
        }
    }

    /**
     * Selects a specific chord formula and regenerates voicings.
     *
     * @param formula The [ChordFormula] to select.
     */
    fun selectFormula(formula: ChordFormula) {
        _uiState.update { current ->
            current.copy(
                selectedFormula = formula,
                voicings = generateVoicings(current.selectedRoot, formula),
            )
        }
    }

    /**
     * Transposes the current root by [semitones] (+1 up, -1 down) and
     * regenerates voicings.
     */
    fun transpose(semitones: Int) {
        _uiState.update { current ->
            val newRoot = Transpose.transposePitchClass(current.selectedRoot, semitones)
            current.copy(
                selectedRoot = newRoot,
                voicings = generateVoicings(newRoot, current.selectedFormula),
            )
        }
    }

    /**
     * Returns the display name for the currently selected chord
     * (e.g., "Cm7", "G", "F#dim").
     */
    fun currentChordName(): String {
        val state = _uiState.value
        val rootName = Notes.pitchClassToName(state.selectedRoot)
        val symbol = state.selectedFormula?.symbol ?: ""
        return "$rootName$symbol"
    }

    /**
     * Updates the muted-string setting and regenerates voicings if the value changed.
     *
     * Called from the UI layer when the "Allow Muted Strings" setting changes.
     */
    fun setAllowMutedStrings(allowed: Boolean) {
        if (allowMutedStrings == allowed) return
        allowMutedStrings = allowed
        _uiState.update { current ->
            current.copy(
                voicings = generateVoicings(current.selectedRoot, current.selectedFormula),
            )
        }
    }

    private fun generateVoicings(rootPitchClass: Int, formula: ChordFormula?): List<ChordVoicing> {
        if (formula == null) return emptyList()
        return VoicingGenerator.generate(
            rootPitchClass = rootPitchClass,
            formula = formula,
            tuning = tuning,
            allowMutedStrings = allowMutedStrings,
        )
    }
}

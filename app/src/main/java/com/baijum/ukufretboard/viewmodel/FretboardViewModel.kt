package com.baijum.ukufretboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Scale
import com.baijum.ukufretboard.data.Scales
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.domain.AlternateChord
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.domain.ChordResult
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.domain.calculateNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the scale note overlay on the fretboard.
 *
 * @property enabled Whether the scale overlay is currently visible.
 * @property root The pitch class (0–11) of the scale root.
 * @property scale The selected scale definition, or null if none.
 * @property scaleNotes The computed set of pitch classes in the scale (derived).
 */
data class ScaleOverlayState(
    val enabled: Boolean = false,
    val root: Int = 0,
    val scale: Scale? = null,
    val scaleNotes: Set<Int> = emptySet(),
    val positionFretRange: IntRange? = null,
)

/**
 * Immutable UI state for the fretboard screen.
 *
 * @property selections A map from string index (0–3) to the selected fret number (0–12),
 *   or null if no fret is selected on that string. Each string can have at most one
 *   selected fret, matching real ukulele behavior where each string sounds one note.
 * @property showNoteNames Whether to display note names on the fretboard cells.
 * @property detectionResult The current chord detection result based on selected frets.
 */
data class FretboardUiState(
    val selections: Map<Int, Int?> = mapOf(0 to null, 1 to null, 2 to null, 3 to null),
    val showNoteNames: Boolean = true,
    val detectionResult: ChordDetector.DetectionResult = ChordDetector.DetectionResult.NoSelection,
    val scaleOverlay: ScaleOverlayState = ScaleOverlayState(),
    val tuning: List<UkuleleString> = emptyList(),
    val capoFret: Int = 0,
    val lastFret: Int = LAST_FRET,
) {
    companion object {
        /** Default last fret (matches FretboardViewModel.LAST_FRET). */
        const val LAST_FRET = 12
    }

    /**
     * Human-readable finger position string derived from the current selections.
     * Format: "0 - 0 - 0 - 3" (one entry per string, "x" for unselected strings).
     */
    val fingerPositions: String = selections.entries
        .sortedBy { it.key }
        .joinToString(" - ") { (_, fret) -> fret?.toString() ?: "x" }
}

/**
 * ViewModel that manages the fretboard UI state and chord detection logic.
 *
 * Handles user interactions (fret taps, reset, toggle note names) and automatically
 * recomputes chord detection whenever the fret selection changes. Uses [StateFlow]
 * for observable, immutable UI state that Compose can collect.
 */
class FretboardViewModel : ViewModel() {

    companion object {
        /** Number of fret positions on the ukulele (0 = open string through 12). */
        const val FRET_COUNT = 13

        /** Index of the first fret (open string). */
        const val OPEN_STRING_FRET = 0

        /** Default last fret (minimum for the configurable range). */
        const val LAST_FRET = 12

        /** Maximum configurable last fret. */
        const val MAX_LAST_FRET = 22

        /**
         * Standard ukulele tuning (high-G / re-entrant).
         *
         * In standard tuning, the strings are tuned to:
         * - String 0: G4 (pitch class 7)
         * - String 1: C4 (pitch class 0)
         * - String 2: E4 (pitch class 4)
         * - String 3: A4 (pitch class 9)
         *
         * Note: The G string is higher than the C string (re-entrant tuning),
         * which is the most common ukulele tuning.
         */
        val STANDARD_TUNING: List<UkuleleString> = listOf(
            UkuleleString(name = "G", openPitchClass = 7),
            UkuleleString(name = "C", openPitchClass = 0),
            UkuleleString(name = "E", openPitchClass = 4),
            UkuleleString(name = "A", openPitchClass = 9),
        )

        /**
         * Builds a tuning list for the given tuning variant.
         */
        fun buildTuning(variant: UkuleleTuning): List<UkuleleString> =
            variant.stringNames.indices.map { i ->
                UkuleleString(
                    name = variant.stringNames[i],
                    openPitchClass = variant.pitchClasses[i],
                    octave = variant.octaves[i],
                )
            }
    }

    /**
     * The current tuning configuration, derived from the selected tuning variant.
     *
     * High-G: G4 (standard re-entrant). Low-G: G3.
     */
    var tuning: List<UkuleleString> = STANDARD_TUNING
        private set

    private val _uiState = MutableStateFlow(FretboardUiState(tuning = tuning))

    /** Observable UI state for the fretboard screen. */
    val uiState: StateFlow<FretboardUiState> = _uiState.asStateFlow()

    /** Current sound settings, updated from [SettingsViewModel] via the UI layer. */
    private var soundSettings: SoundSettings = SoundSettings()

    

    /**
     * Updates the sound settings used by playback methods.
     * Called by the UI layer when [SettingsViewModel.settings] changes.
     */
    fun setSoundSettings(settings: SoundSettings) {
        soundSettings = settings
    }

    /**
     * Updates the tuning based on settings. Only the G string octave changes
     * between High-G and Low-G; pitch classes remain the same.
     */
    fun setTuningSettings(settings: TuningSettings) {
        val newTuning = buildTuning(settings.tuning)
        if (tuning != newTuning) {
            tuning = newTuning
            _uiState.update { current ->
                current.copy(
                    tuning = newTuning,
                    detectionResult = detectChord(current.selections),
                )
            }
        }
    }

    /**
     * Toggles a fret selection on the specified string.
     *
     * - If the fret is already selected on that string, it is deselected (set to null).
     * - If a different fret (or no fret) is selected, the new fret becomes selected.
     * - Only one fret per string can be selected at a time, matching real ukulele behavior.
     *
     * After toggling, chord detection is automatically re-run on the new selection.
     * If play-on-tap is enabled and a fret was selected (not deselected), the note
     * is played immediately.
     *
     * @param stringIndex The index of the string (0 = G, 1 = C, 2 = E, 3 = A).
     * @param fret The fret number (0 = open string, 1–12 = fretted positions).
     */
    fun toggleFret(stringIndex: Int, fret: Int) {
        val capo = _uiState.value.capoFret
        if (capo > 0 && fret in 1..capo) return // blocked by capo

        val wasSelected = _uiState.value.selections[stringIndex] == fret
        _uiState.update { current ->
            val currentFret = current.selections[stringIndex]
            val newFret = if (currentFret == fret) null else fret
            val newSelections = current.selections.toMutableMap().apply {
                this[stringIndex] = newFret
            }
            current.copy(
                selections = newSelections,
                detectionResult = detectChord(newSelections),
            )
        }

        // Play the tapped note if play-on-tap is enabled and we just selected (not deselected)
        if (!wasSelected && soundSettings.enabled && soundSettings.playOnTap) {
            playNoteAt(stringIndex, fret)
        }
    }

    /**
     * Clears all fret selections and resets the detection result.
     * Preserves the current note name visibility setting.
     */
    fun clearAll() {
        _uiState.update {
            FretboardUiState(showNoteNames = it.showNoteNames, tuning = it.tuning)
        }
    }

    /**
     * Sets the capo fret position.
     *
     * When capo is > 0, all pitch calculations (chord detection, note names,
     * sound playback) are shifted up by the capo amount, matching the behavior
     * of a real capo clamped at that fret.
     *
     * @param fret The fret position for the capo (0 = no capo, 1–12 = capo at fret).
     */
    fun setCapoFret(fret: Int) {
        _uiState.update { current ->
            val newCapo = fret.coerceIn(0, current.lastFret)
            // Clear any selections that fall behind the new capo position
            val cleaned = current.selections.mapValues { (_, sel) ->
                if (sel != null && newCapo > 0 && sel in 1..newCapo) null else sel
            }
            current.copy(
                capoFret = newCapo,
                selections = cleaned,
                detectionResult = detectChord(cleaned, newCapo),
            )
        }
    }

    /**
     * Sets the highest fret shown on the fretboard (12–22).
     *
     * If the current capo position exceeds the new last fret, the capo is
     * coerced down. Selections beyond the new range are cleared.
     */
    fun setLastFret(fret: Int) {
        _uiState.update { current ->
            val newLast = fret.coerceIn(LAST_FRET, MAX_LAST_FRET)
            val newCapo = current.capoFret.coerceAtMost(newLast)
            val cleaned = current.selections.mapValues { (_, sel) ->
                if (sel != null && sel > newLast) null else sel
            }
            current.copy(
                lastFret = newLast,
                capoFret = newCapo,
                selections = cleaned,
                detectionResult = detectChord(cleaned, newCapo),
            )
        }
    }

    /**
     * Applies a chord voicing from the chord library onto the fretboard.
     *
     * Sets each string's fret selection to the voicing's fret values and
     * determines the chord detection result.
     *
     * When [rootPitchClass] and [formula] are provided (i.e., the voicing came
     * from the chord library with a known identity), the detection result uses
     * that identity as the primary chord name and discovers alternates via
     * detection. This avoids the ambiguity where, for example, an Am7 voicing
     * would otherwise be detected as C6 due to candidate-root ordering.
     *
     * When called without chord context (e.g., programmatic use), falls back to
     * normal detection.
     *
     * @param voicing The [ChordVoicing] to apply (one fret per string).
     * @param rootPitchClass The pitch class of the intended root note (0–11),
     *   or null to use detection.
     * @param formula The [ChordFormula] of the intended chord, or null to use detection.
     */
    fun applyVoicing(
        voicing: com.baijum.ukufretboard.domain.ChordVoicing,
        rootPitchClass: Int? = null,
        formula: ChordFormula? = null,
    ) {
        _uiState.update { current ->
            val newSelections = voicing.frets.mapIndexed { i, fret ->
                i to (if (fret < 0) null else fret)
            }.toMap()

            val detection = if (rootPitchClass != null && formula != null) {
                // Run detection to discover alternate chord names. This goes
                // through the capo-aware path, so its notes already reflect the
                // sounding strings.
                val detected = detectChord(newSelections)

                // Transpose the intended root by the capo so the named chord
                // matches the strings that actually sound. Every other pitch
                // site in this file adds the capo; without it, applying a
                // library "C" at capo 2 would label the board "C" while the
                // strings sound D.
                val capo = current.capoFret
                val soundingRoot = (rootPitchClass + capo).mod(Notes.PITCH_CLASS_COUNT)
                val alternates = buildAlternates(detected, soundingRoot, formula)

                val rootNote = Note(
                    pitchClass = soundingRoot,
                    name = Notes.pitchClassToName(soundingRoot),
                )
                // Use the capo-aware note list from detection so the notes
                // shown match the sounding strings (identical to tapping the
                // same frets by hand).
                val chordNotes = detectedNotes(detected)

                ChordDetector.DetectionResult.ChordFound(
                    ChordResult(
                        name = rootNote.name + formula.symbol,
                        quality = formula.quality,
                        root = rootNote,
                        notes = chordNotes,
                        matchedFormula = formula,
                        alternates = alternates,
                    )
                )
            } else {
                detectChord(newSelections)
            }

            current.copy(
                selections = newSelections,
                detectionResult = detection,
            )
        }
    }

    /**
     * Extracts the note list carried by a [ChordDetector.DetectionResult].
     *
     * Used by [applyVoicing] so the notes shown for a library voicing come from
     * the capo-aware detection pass and match what tapping the same frets by
     * hand produces.
     */
    private fun detectedNotes(detected: ChordDetector.DetectionResult): List<Note> =
        when (detected) {
            is ChordDetector.DetectionResult.ChordFound -> detected.result.notes
            is ChordDetector.DetectionResult.NoMatch -> detected.notes
            is ChordDetector.DetectionResult.Interval -> detected.notes
            is ChordDetector.DetectionResult.SingleNote -> listOf(detected.note)
            ChordDetector.DetectionResult.NoSelection -> emptyList()
        }

    /**
     * Builds the list of alternate chord interpretations, excluding the
     * intended chord identified by [rootPitchClass] and [formula].
     */
    private fun buildAlternates(
        detected: ChordDetector.DetectionResult,
        rootPitchClass: Int,
        formula: ChordFormula,
    ): List<AlternateChord> {
        if (detected !is ChordDetector.DetectionResult.ChordFound) return emptyList()
        val result = detected.result
        val all = mutableListOf<AlternateChord>()

        // Include the detector's primary result if it differs from the intended chord
        val mf = result.matchedFormula
        if (mf != null &&
            (result.root.pitchClass != rootPitchClass || mf != formula)
        ) {
            all.add(
                AlternateChord(
                    name = result.name,
                    rootPitchClass = result.root.pitchClass,
                    formula = mf,
                )
            )
        }

        // Include the detector's own alternates, excluding the intended chord
        all.addAll(
            result.alternates.filter {
                it.rootPitchClass != rootPitchClass || it.formula != formula
            }
        )
        return all
    }

    /**
     * Sets whether note names are displayed on the fretboard cells.
     */
    fun setShowNoteNames(show: Boolean) {
        _uiState.update { it.copy(showNoteNames = show) }
    }

    // ── Scale overlay controls ──

    /**
     * Toggles the scale overlay on/off.
     */
    fun toggleScaleOverlay() {
        _uiState.update { current ->
            val overlay = current.scaleOverlay
            current.copy(scaleOverlay = overlay.copy(enabled = !overlay.enabled))
        }
    }

    /**
     * Sets the scale overlay root note and recomputes scale notes.
     */
    fun setScaleRoot(root: Int) {
        _uiState.update { current ->
            val overlay = current.scaleOverlay
            val notes = overlay.scale?.let { Scales.scaleNotes(root, it) } ?: emptySet()
            current.copy(scaleOverlay = overlay.copy(root = root, scaleNotes = notes))
        }
    }

    /**
     * Sets the scale type and recomputes scale notes.
     */
    fun setScale(scale: Scale) {
        _uiState.update { current ->
            val overlay = current.scaleOverlay
            val notes = Scales.scaleNotes(overlay.root, scale)
            current.copy(
                scaleOverlay = overlay.copy(
                    scale = scale,
                    scaleNotes = notes,
                    enabled = true,
                    positionFretRange = null,
                )
            )
        }
    }

    /**
     * Sets the fret range filter for scale positions.
     * Pass null to show all scale notes.
     */
    fun setScalePositionRange(fretRange: IntRange?) {
        _uiState.update { current ->
            val overlay = current.scaleOverlay
            current.copy(scaleOverlay = overlay.copy(positionFretRange = fretRange))
        }
    }

    /**
     * Computes the note at a given string and fret position using the current tuning.
     *
     * @param stringIndex The index of the string (0–3).
     * @param fret The fret number (0–12).
     * @return The [Note] at that position.
     */
    fun getNoteAt(stringIndex: Int, fret: Int): Note {
        val openPitchClass = tuning[stringIndex].openPitchClass
        val capo = _uiState.value.capoFret
        val pitchClass = ((openPitchClass + fret + capo) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
        val overlay = _uiState.value.scaleOverlay
        val name = if (overlay.enabled && overlay.scale != null) {
            // Use key-aware enharmonic spelling when a scale is active
            val isMinor = overlay.scale.intervals.size > 2 && overlay.scale.intervals[2] == 3
            Notes.enharmonicForKey(pitchClass, overlay.root, isMinor)
        } else {
            Notes.pitchClassToName(pitchClass)
        }
        return Note(pitchClass = pitchClass, name = name)
    }

    /**
     * Plays the currently selected notes as a strummed chord.
     *
     * Collects the pitch class and octave for each string that has a fret selected,
     * then delegates to [ToneGenerator.playChord] on a background coroutine.
     * Strum direction, note duration, and strum delay are taken from [soundSettings].
     *
     * If sound is disabled or no notes are selected, this method does nothing.
     */
    fun playChord() {
        if (!soundSettings.enabled) return

        val state = _uiState.value
        val capo = state.capoFret
        val notes = state.selections.entries
            .let { entries ->
                if (soundSettings.strumDown) entries.sortedBy { it.key }
                else entries.sortedByDescending { it.key }
            }
            .filter { it.value != null }
            .map { (stringIndex, fret) ->
                val string = tuning[stringIndex]
                val pitchClass = ((string.openPitchClass + fret!! + capo) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
                val octave = computeOctave(string.openPitchClass, string.octave, fret + capo)
                pitchClass to octave
            }

        if (notes.isNotEmpty()) {
            viewModelScope.launch {
                ToneGenerator.playChord(
                    notes = notes,
                    noteDurationMs = soundSettings.noteDurationMs,
                    strumDelayMs = soundSettings.strumDelayMs,
                    volume = soundSettings.volume,
                )
            }
        }
    }

    /**
     * Plays a specific [ChordVoicing] as a strummed chord.
     *
     * Used by the Chord Library's compare/preview features to audition
     * a voicing without applying it to the fretboard.
     *
     * @param voicing The voicing to play.
     */
    fun playVoicing(voicing: com.baijum.ukufretboard.domain.ChordVoicing) {
        if (!soundSettings.enabled) return
        val capo = _uiState.value.capoFret

        val notes = voicing.frets.mapIndexedNotNull { index, fret ->
            if (fret == com.baijum.ukufretboard.domain.ChordVoicing.MUTED) return@mapIndexedNotNull null
            val string = tuning[index]
            val pitchClass = ((string.openPitchClass + fret + capo) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
            val octave = computeOctave(string.openPitchClass, string.octave, fret + capo)
            pitchClass to octave
        }

        viewModelScope.launch {
            ToneGenerator.playChord(
                notes = notes,
                noteDurationMs = soundSettings.noteDurationMs,
                strumDelayMs = soundSettings.strumDelayMs,
                volume = soundSettings.volume,
            )
        }
    }

    /**
     * Plays a chord by its display name (e.g. "Am", "C#m7").
     *
     * Parses the name into a root pitch class and formula, generates voicings
     * for the current tuning, and plays the first available voicing.
     *
     * @param chordName The chord name to play.
     */
    fun playChordByName(chordName: String) {
        if (!soundSettings.enabled) return
        val parsed = ChordNameParser.parse(chordName) ?: return
        val voicings = VoicingGenerator.generate(parsed.rootPitchClass, parsed.formula, tuning)
        val voicing = voicings.firstOrNull() ?: return
        playVoicing(voicing)
    }

    /**
     * Plays a list of voicings sequentially with a pause between each.
     *
     * Used by the "Play All Inversions" feature to let users hear
     * how the same chord sounds in different inversions.
     *
     * @param voicings The voicings to play in order.
     * @param pauseMs Pause between voicings in milliseconds.
     */
    fun playVoicingsSequentially(
        voicings: List<com.baijum.ukufretboard.domain.ChordVoicing>,
        pauseMs: Long = 1000L,
    ) {
        if (!soundSettings.enabled || voicings.isEmpty()) return
        val capo = _uiState.value.capoFret

        viewModelScope.launch {
            voicings.forEach { voicing ->
                val notes = voicing.frets.mapIndexedNotNull { index, fret ->
                    if (fret == com.baijum.ukufretboard.domain.ChordVoicing.MUTED) return@mapIndexedNotNull null
                    val string = tuning[index]
                    val pitchClass = ((string.openPitchClass + fret + capo) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
                    val octave = computeOctave(string.openPitchClass, string.octave, fret + capo)
                    pitchClass to octave
                }
                ToneGenerator.playChord(
                    notes = notes,
                    noteDurationMs = soundSettings.noteDurationMs,
                    strumDelayMs = soundSettings.strumDelayMs,
                    volume = soundSettings.volume,
                )
                kotlinx.coroutines.delay(pauseMs)
            }
        }
    }

    /**
     * Plays a single note at the given string and fret position.
     *
     * Used by the play-on-tap feature. Note duration is taken from [soundSettings].
     *
     * @param stringIndex The index of the string (0–3).
     * @param fret The fret number (0–12).
     */
    private fun playNoteAt(stringIndex: Int, fret: Int) {
        val string = tuning[stringIndex]
        val capo = _uiState.value.capoFret
        val pitchClass = ((string.openPitchClass + fret + capo) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
        val octave = computeOctave(string.openPitchClass, string.octave, fret + capo)
        viewModelScope.launch {
            ToneGenerator.playNote(
                pitchClass = pitchClass,
                octave = octave,
                durationMs = soundSettings.noteDurationMs,
                volume = soundSettings.volume,
            )
        }
    }

    /**
     * Plays a single note by pitch class (for melody notepad and interval trainer).
     *
     * Uses octave 4 (middle C octave) by default.
     *
     * @param pitchClass The pitch class (0–11).
     * @param octave The octave (default 4).
     */
    fun playMelodyNote(pitchClass: Int, octave: Int = 4) {
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

    /**
     * Computes the actual octave of a note produced at a given fret on a string.
     *
     * When fretting raises the pitch class past B (11) back to C (0), the octave
     * increments by one. For example, A4 (pitch class 9) at fret 4 produces
     * C#5 (pitch class 1, octave 5).
     *
     * @param openPitchClass The pitch class of the open string.
     * @param openOctave The octave of the open string.
     * @param fret The fret number (0–12).
     * @return The octave of the fretted note.
     */
    private fun computeOctave(openPitchClass: Int, openOctave: Int, fret: Int): Int {
        return openOctave + (openPitchClass + fret) / Notes.PITCH_CLASS_COUNT
    }

    /**
     * Runs chord detection on the current set of selected fret positions.
     *
     * Collects the pitch class for each string that has a fret selected,
     * then delegates to [ChordDetector.detect] for interval-based matching.
     */
    private fun detectChord(
        selections: Map<Int, Int?>,
        capo: Int = _uiState.value.capoFret,
    ): ChordDetector.DetectionResult {
        val pitchClasses = selections.entries
            .filter { it.value != null }
            .map { (stringIndex, fret) ->
                val openPitchClass = tuning[stringIndex].openPitchClass
                ((openPitchClass + fret!! + capo) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
            }
        return ChordDetector.detect(pitchClasses)
    }
}

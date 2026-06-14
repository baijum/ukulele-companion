package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordVoicing

/**
 * Utility functions for computing detailed chord information —
 * interval breakdowns, formula strings, fingering suggestions,
 * and difficulty ratings.
 *
 * All functions are pure and stateless.
 */
object ChordInfo {

    // ── Interval names (abbreviated) ────────────────────────────────────
    /** Maps a semitone interval (0–11) to its abbreviated interval name. */
    private val INTERVAL_NAMES = mapOf(
        0 to "Root",
        1 to "m2",
        2 to "M2",
        3 to "m3",
        4 to "M3",
        5 to "P4",
        6 to "d5",
        7 to "P5",
        8 to "m6",
        9 to "M6",
        10 to "m7",
        11 to "M7",
    )

    // ── Formula degree labels ───────────────────────────────────────────
    /** Maps a semitone interval (0–11) to its scale-degree formula symbol. */
    private val FORMULA_DEGREES = mapOf(
        0 to "1",
        1 to "b2",
        2 to "2",
        3 to "b3",
        4 to "3",
        5 to "4",
        6 to "b5",
        7 to "5",
        8 to "#5",
        9 to "6",
        10 to "b7",
        11 to "7",
    )

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Builds a human-readable interval breakdown string.
     *
     * Example output: `"C (Root)  Eb (m3)  G (P5)  Bb (m7)"`
     *
     * Notes are ordered by their interval from the root (ascending).
     *
     * @param root The root [Note] of the chord.
     * @param notes All unique notes in the chord.
     * @return A formatted string showing each note with its interval role.
     */
    fun buildIntervalBreakdown(root: Note, notes: List<Note>): String {
        return notes
            .map { note ->
                val interval = (note.pitchClass - root.pitchClass + Notes.PITCH_CLASS_COUNT) %
                    Notes.PITCH_CLASS_COUNT
                val intervalName = INTERVAL_NAMES[interval] ?: "?"
                "${note.name} ($intervalName)"
            }
            .sortedBy { entry ->
                // Sort by interval value for consistent ordering
                val noteName = entry.substringBefore(" (")
                val pc = Notes.NOTE_NAMES_STANDARD.indexOf(noteName).takeIf { it >= 0 }
                    ?: Notes.NOTE_NAMES_SHARP.indexOf(noteName).takeIf { it >= 0 }
                    ?: Notes.NOTE_NAMES_FLAT.indexOf(noteName)
                (pc - root.pitchClass + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
            }
            .joinToString("  ")
    }

    /**
     * Builds a chord formula string from a [ChordFormula].
     *
     * Example output: `"1 b3 5 b7"`
     *
     * Intervals are sorted ascending and mapped to their scale-degree symbols.
     *
     * @param formula The chord formula containing the interval set.
     * @return A space-separated formula string.
     */
    fun buildFormulaString(formula: ChordFormula): String {
        return formula.intervals
            .sorted()
            .mapNotNull { FORMULA_DEGREES[it] }
            .joinToString("  ")
    }

    /**
     * Suggests which finger to use for each string based on fret positions.
     *
     * Returns a list of 4 integers (one per string, in G-C-E-A order):
     * - `0` = open string (no finger needed)
     * - `1` = index finger
     * - `2` = middle finger
     * - `3` = ring finger
     * - `4` = pinky finger
     *
     * The heuristic works as follows:
     * 1. Open strings get finger 0.
     * 2. If all fretted strings are on the same fret (barre), they all get finger 1.
     * 3. Otherwise, fretted positions are ranked by fret number and assigned
     *    ascending fingers starting from 1.
     *
     * @param frets A list of 4 fret numbers (one per string). All values 0–12.
     * @return A list of 4 finger assignments.
     */
    fun suggestFingering(frets: List<Int>): List<Int> {
        if (frets.size != 4) return frets.map { if (it == 0) 0 else 1 }

        // Collect the distinct fretted (non-zero) fret values, sorted ascending
        val frettedValues = frets.filter { it > 0 }.distinct().sorted()

        if (frettedValues.isEmpty()) {
            // All open strings
            return listOf(0, 0, 0, 0)
        }

        // Map each distinct fret value to a finger number (1-based)
        val fretToFinger = mutableMapOf<Int, Int>()
        frettedValues.forEachIndexed { index, fretValue ->
            fretToFinger[fretValue] = (index + 1).coerceAtMost(4)
        }

        return frets.map { fret ->
            when {
                fret <= 0 -> 0 // open (0) or muted (-1) — no finger
                else -> fretToFinger[fret] ?: 1
            }
        }
    }

    /**
     * Formats a fingering list as a display string.
     *
     * Example output: `"0  0  1  1"`
     *
     * @param fingering A list of 4 finger assignments from [suggestFingering].
     * @return A formatted string.
     */
    fun formatFingering(fingering: List<Int>): String {
        return fingering.joinToString("  ")
    }

    /**
     * Rates the difficulty of a chord voicing based on its fret positions.
     *
     * @param frets A list of 4 fret numbers (one per string). All values 0–12.
     * @return A human-readable difficulty label.
     */
    fun rateDifficulty(frets: List<Int>): String {
        val frettedPositions = frets.filter { it > 0 }

        // All open strings
        if (frettedPositions.isEmpty()) return "Very easy"

        val frettedCount = frettedPositions.size
        val minFret = frettedPositions.min()
        val maxFret = frettedPositions.max()
        val span = maxFret - minFret

        // Single fretted string at a low position
        if (frettedCount == 1 && maxFret <= 3) return "Very easy"

        // All fretted strings on the same fret (barre) at low position
        if (span == 0 && maxFret <= 3) return "Easy"

        // Small span, low frets
        if (span <= 2 && maxFret <= 5) return "Easy"

        // Moderate span or barre shapes
        if (span <= 3 && maxFret <= 7) return "Medium"

        // Wide span or high fret positions
        return "Hard"
    }

    // ── Inversion detection ─────────────────────────────────────────────

    /**
     * Possible chord inversions.
     *
     * @property label Short label for display in badges and info rows.
     */
    enum class Inversion(val label: String) {
        ROOT("Root"),
        FIRST("1st Inv"),
        SECOND("2nd Inv"),
        THIRD("3rd Inv"),
    }

    /**
     * Finds the index of the string that produces the lowest-pitched note
     * in a voicing, accounting for re-entrant tuning.
     *
     * The actual MIDI-like pitch is computed as:
     *     pitch = octave * 12 + pitchClass + fret
     *
     * This correctly handles the re-entrant G4 string being higher in pitch
     * than the C4 string.
     *
     * @param frets A list of 4 fret numbers (one per string, G-C-E-A order).
     * @param tuning The current tuning (provides open pitch class and octave per string).
     * @return The string index (0–3) of the bass note.
     */
    fun findBassStringIndex(frets: List<Int>, tuning: List<UkuleleString>): Int {
        // Exclude muted strings (MUTED = -1) from bass note consideration
        val playedIndices = frets.indices.filter { frets[it] != ChordVoicing.MUTED }
        return playedIndices.minByOrNull { i ->
            val string = tuning[i]
            string.octave * Notes.PITCH_CLASS_COUNT + string.openPitchClass + frets[i]
        } ?: 0
    }

    /**
     * Computes the pitch class of the bass (lowest-pitched) note in a voicing.
     *
     * @param frets A list of 4 fret numbers (one per string, G-C-E-A order).
     * @param tuning The current tuning.
     * @return The pitch class (0–11) of the lowest note.
     */
    fun bassPitchClass(frets: List<Int>, tuning: List<UkuleleString>): Int {
        val bassIndex = findBassStringIndex(frets, tuning)
        val string = tuning[bassIndex]
        val raw = string.openPitchClass + frets[bassIndex]
        return ((raw % Notes.PITCH_CLASS_COUNT) + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
    }

    /**
     * Determines the inversion of a chord voicing.
     *
     * Computes the interval between the bass note and the chord root, then
     * checks which chord tone (root, 3rd, 5th, or 7th) occupies the bass.
     *
     * @param frets A list of 4 fret numbers (one per string, G-C-E-A order).
     * @param rootPitchClass The pitch class of the chord root.
     * @param formula The chord formula (provides the interval set).
     * @param tuning The current tuning.
     * @return The [Inversion] type, or [Inversion.ROOT] if the bass note is the root.
     */
    fun determineInversion(
        frets: List<Int>,
        rootPitchClass: Int,
        formula: ChordFormula,
        tuning: List<UkuleleString>,
    ): Inversion {
        val bassPc = bassPitchClass(frets, tuning)
        val interval = (bassPc - rootPitchClass + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
        return inversionForInterval(interval, formula)
    }

    /**
     * Maps a bass interval (semitones above root) to the corresponding [Inversion].
     *
     * @param interval Semitone interval from root to bass (0–11).
     * @param formula The chord formula (used to validate 3rd inversion for 7th chords).
     */
    fun inversionForInterval(
        interval: Int,
        formula: ChordFormula,
    ): Inversion {
        if (interval == 0) return Inversion.ROOT
        if (interval == 3 || interval == 4) return Inversion.FIRST
        if (interval == 6 || interval == 7 || interval == 8) return Inversion.SECOND
        if ((interval == 10 || interval == 11) && formula.intervals.any { it >= 10 }) {
            return Inversion.THIRD
        }
        return Inversion.ROOT
    }

    /**
     * Returns slash notation for a chord with its bass note.
     *
     * Example: for C major with E in the bass, returns "C/E".
     * Returns just the chord name if it's root position.
     *
     * @param chordName The chord name (e.g., "C", "Am7").
     * @param inversion The detected inversion.
     * @param bassPc The pitch class of the bass note.
     * @return The chord name with optional slash bass notation.
     */
    fun slashNotation(
        chordName: String,
        inversion: Inversion,
        bassPc: Int,
    ): String {
        if (inversion == Inversion.ROOT) return chordName
        val bassName = Notes.pitchClassToName(bassPc)
        return "$chordName/$bassName"
    }
}

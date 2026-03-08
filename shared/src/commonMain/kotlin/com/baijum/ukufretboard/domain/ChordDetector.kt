package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes

/**
 * Detects chords from a set of pitch classes using interval matching.
 *
 * The detection algorithm works as follows:
 * 1. Removes duplicate pitch classes from the input.
 * 2. Handles edge cases: 0 notes (no selection), 1 note (single note), 2 notes (interval).
 * 3. For 3+ unique pitch classes, tries each as a potential root note.
 * 4. For each candidate root, computes the interval set relative to that root.
 * 5. Matches the interval set against all known [ChordFormulas].
 * 6. Returns the first match found (triads are preferred over extended chords
 *    because of the formula list ordering).
 *
 * This approach is purely mathematical — no audio analysis or machine learning is used.
 */
object ChordDetector {

    /**
     * The result of chord detection from a set of selected notes.
     */
    sealed class DetectionResult {
        /** No notes are selected on the fretboard. */
        data object NoSelection : DetectionResult()

        /** Exactly one note is selected — not enough to form a chord. */
        data class SingleNote(val note: Note) : DetectionResult()

        /** Two notes are selected — an interval, but not a complete chord. */
        data class Interval(val notes: List<Note>) : DetectionResult()

        /** A known chord was successfully detected. */
        data class ChordFound(val result: ChordResult) : DetectionResult()

        /** Three or more notes selected but no known chord formula matches. */
        data class NoMatch(val notes: List<Note>) : DetectionResult()
    }

    /**
     * Detects a chord from the given list of pitch classes.
     *
     * @param pitchClasses A list of pitch class integers (0–11) derived from the
     *   selected fret positions on the fretboard. May contain duplicates (e.g., the
     *   same note on different strings).
     * @return A [DetectionResult] describing what was detected.
     */
    fun detect(pitchClasses: List<Int>): DetectionResult {
        val uniquePitchClasses = pitchClasses.distinct()

        return when (uniquePitchClasses.size) {
            0 -> DetectionResult.NoSelection
            1 -> {
                val pc = uniquePitchClasses.first()
                DetectionResult.SingleNote(
                    Note(pitchClass = pc, name = Notes.pitchClassToName(pc))
                )
            }
            2 -> {
                val notes = uniquePitchClasses.map {
                    Note(pitchClass = it, name = Notes.pitchClassToName(it))
                }
                DetectionResult.Interval(notes)
            }
            else -> findChord(uniquePitchClasses)
        }
    }

    /**
     * Attempts to match a set of 3+ unique pitch classes against known chord formulas.
     *
     * For each pitch class in the input, treats it as a candidate root and computes
     * the intervals of all other pitch classes relative to that root using modular
     * arithmetic:
     *
     *     interval = (pitchClass - candidateRoot + 12) % 12
     *
     * Collects **all** matching (root, formula) pairs so that enharmonically
     * equivalent chords are surfaced as alternates. For example, pitch classes
     * {A, C, E, G} will match both C6 and Am7. The first match becomes the
     * primary result; the rest are returned as [AlternateChord] entries.
     *
     * The outer loop iterates over candidate roots in the order they appear in the
     * input, and the inner loop iterates over formulas in priority order (triads
     * before seventh chords).
     *
     * @param uniquePitchClasses A list of 3 or more distinct pitch class integers.
     * @return [DetectionResult.ChordFound] if at least one match is found,
     *   or [DetectionResult.NoMatch].
     */
    private fun findChord(uniquePitchClasses: List<Int>): DetectionResult {
        val pitchClassSet = uniquePitchClasses.toSet()
        val matches = mutableListOf<Pair<Int, ChordFormula>>()

        for (candidateRoot in uniquePitchClasses) {
            // Compute intervals relative to the candidate root
            val intervals = pitchClassSet.map { pc ->
                (pc - candidateRoot + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
            }.toSet()

            // Collect all matching formulas for this candidate root
            for (formula in ChordFormulas.ALL) {
                if (intervals == formula.intervals) {
                    matches.add(candidateRoot to formula)
                }
            }
        }

        if (matches.isEmpty()) {
            val notes = uniquePitchClasses.map {
                Note(pitchClass = it, name = Notes.pitchClassToName(it))
            }
            return DetectionResult.NoMatch(notes)
        }

        val (primaryRoot, primaryFormula) = matches.first()
        val rootNote = Note(
            pitchClass = primaryRoot,
            name = Notes.pitchClassToName(primaryRoot),
        )
        val chordNotes = uniquePitchClasses.map {
            Note(pitchClass = it, name = Notes.pitchClassToName(it))
        }
        val alternates = matches.drop(1).map { (root, formula) ->
            AlternateChord(
                name = Notes.pitchClassToName(root) + formula.symbol,
                rootPitchClass = root,
                formula = formula,
            )
        }

        return DetectionResult.ChordFound(
            ChordResult(
                name = rootNote.name + primaryFormula.symbol,
                quality = primaryFormula.quality,
                root = rootNote,
                notes = chordNotes,
                matchedFormula = primaryFormula,
                alternates = alternates,
            )
        )
    }
}

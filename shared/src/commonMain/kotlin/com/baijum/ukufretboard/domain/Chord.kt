package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormula

/**
 * An alternative chord interpretation of the same set of pitch classes.
 *
 * For example, the pitch classes {A, C, E, G} can be interpreted as Am7 (root A,
 * formula m7) or C6 (root C, formula 6). When one interpretation is chosen as the
 * primary [ChordResult], the others appear as [AlternateChord] entries.
 *
 * @property name The full display name (e.g., "Am7", "C6").
 * @property rootPitchClass The pitch class of the alternate root (0–11).
 * @property formula The [ChordFormula] for this interpretation.
 */
data class AlternateChord(
    val name: String,
    val rootPitchClass: Int,
    val formula: ChordFormula,
)

/**
 * Represents a successfully detected chord.
 *
 * @property name The full display name of the chord (e.g., "Am", "G7", "Cmaj7").
 *   Composed of the root note name plus the chord formula symbol.
 * @property quality A human-readable description of the chord quality
 *   (e.g., "Minor", "Dominant 7th", "Major").
 * @property root The root [Note] of the chord — the note that gives the chord its letter name.
 * @property notes All unique [Note]s that make up the chord.
 * @property matchedFormula The [ChordFormula] that was matched during detection,
 *   providing access to interval data for display purposes. Null when the chord
 *   was not detected via formula matching.
 * @property alternates Other valid chord interpretations of the same pitch classes.
 *   For example, if the primary result is C6, alternates may include Am7.
 */
data class ChordResult(
    val name: String,
    val quality: String,
    val root: Note,
    val notes: List<Note>,
    val matchedFormula: ChordFormula? = null,
    val alternates: List<AlternateChord> = emptyList(),
)

/**
 * Represents a specific way to play a chord on the ukulele fretboard.
 *
 * A single chord (e.g., C major) can be played in multiple positions on the
 * fretboard. Each voicing specifies the exact fret for each of the 4 strings.
 *
 * @property frets A list of 4 fret numbers, one per string (G, C, E, A in order).
 *   Values are [MUTED] (-1) for a muted/unplayed string, 0 for open, or 1–12 for fretted.
 *   Muted-string voicings are only generated when the user enables the
 *   "Allow Muted Strings" setting.
 * @property notes The [Note] produced at each string position, in string order.
 *   Null for muted strings (fret == [MUTED]).
 * @property minFret The lowest fretted (non-open) position, or 0 if all strings are open.
 *   Muted strings are excluded from this calculation.
 *   Used to determine the diagram display range.
 * @property maxFret The highest fret used in this voicing.
 *   Muted strings are excluded from this calculation.
 */
data class ChordVoicing(
    val frets: List<Int>,
    val notes: List<Note?>,
    val minFret: Int,
    val maxFret: Int,
) {
    companion object {
        /** Sentinel fret value indicating a muted (unplayed) string. */
        const val MUTED = -1
    }
}

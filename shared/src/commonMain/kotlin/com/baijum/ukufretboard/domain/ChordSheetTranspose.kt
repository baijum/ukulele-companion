package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.Notes

/**
 * Transposition utilities for chord sheets.
 *
 * Parses inline `[ChordName]` markers from chord sheet content,
 * transposes each chord root by the given number of semitones,
 * and preserves the chord quality suffix.
 */
object ChordSheetTranspose {

    /** Regex matching `[ChordName]` markers, capturing root and quality separately. */
    private val CHORD_MARKER = Regex("""\[(?!(?:${ChordParser.SECTION_NAMES})\b)([A-G][#b]?)([^]]*)]""")

    /**
     * Transposes all `[Chord]` markers in the given content by [semitones].
     *
     * @param content The raw chord sheet content with `[ChordName]` markers.
     * @param semitones Number of semitones to transpose (positive = up, negative = down).
     * @return New content string with all chord markers transposed.
     */
    fun transpose(content: String, semitones: Int): String {
        if (semitones == 0) return content

        return CHORD_MARKER.replace(content) { match ->
            val rootStr = match.groupValues[1]
            val qualitySuffix = match.groupValues[2]

            // Parse the root note to a pitch class
            val rootPc = Notes.NOTE_NAMES_SHARP.indexOf(rootStr).takeIf { it >= 0 }
                ?: Notes.NOTE_NAMES_FLAT.indexOf(rootStr).takeIf { it >= 0 }
                ?: return@replace match.value // unknown root, leave as-is

            // Transpose
            val newPc = Transpose.transposePitchClass(rootPc, semitones)
            val newRoot = Notes.pitchClassToName(newPc)

            "[$newRoot$qualitySuffix]"
        }
    }

    /**
     * Returns the semitone description for display.
     *
     * @param semitones The transposition amount.
     * @return Display string like "+2", "-3", or "0".
     */
    fun semitoneLabel(semitones: Int): String = when {
        semitones > 0 -> "+$semitones"
        else -> "$semitones"
    }
}

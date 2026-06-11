package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.Notes

/**
 * Transposition utilities for chord sheets.
 *
 * Parses inline `[ChordName]` markers from chord sheet content,
 * transposes each chord root and slash-chord bass note by the given
 * number of semitones, and preserves the chord quality suffix.
 */
object ChordSheetTranspose {

    /** Regex matching `[ChordName]` markers, capturing root and quality separately. */
    private val CHORD_MARKER = Regex("""\[(?!(?:${ChordParser.SECTION_NAMES})\b)([A-G][#b]?)([^]]*)]""")

    /** Bass note at the start of the post-slash segment, e.g. "/G" or "m7/G". */
    private val BASS_NOTE = Regex("""^([A-G][#b]?)""")

    /**
     * Transposes all `[Chord]` markers in the given content by [semitones].
     *
     * Root and slash-chord bass notes are transposed; quality suffixes are preserved.
     *
     * @param content The raw chord sheet content with `[ChordName]` markers.
     * @param semitones Number of semitones to transpose (positive = up, negative = down).
     * @return New content string with all chord markers transposed.
     */
    fun transpose(content: String, semitones: Int): String {
        if (semitones == 0) return content

        return CHORD_MARKER.replace(content) { match ->
            val rootStr = match.groupValues[1]
            var qualitySuffix = match.groupValues[2]

            val rootPc = parseNoteToPitchClass(rootStr)
                ?: return@replace match.value

            val newRoot = Notes.pitchClassToName(
                Transpose.transposePitchClass(rootPc, semitones),
            )

            val slashIdx = qualitySuffix.indexOf('/')
            if (slashIdx >= 0) {
                val afterSlash = qualitySuffix.substring(slashIdx + 1)
                val bassMatch = BASS_NOTE.matchAt(afterSlash, 0)
                val bassPc = bassMatch?.let { parseNoteToPitchClass(it.groupValues[1]) }
                if (bassPc != null) {
                    val newBass = Notes.pitchClassToName(
                        Transpose.transposePitchClass(bassPc, semitones),
                    )
                    val tail = afterSlash.substring(bassMatch.range.last + 1)
                    qualitySuffix = qualitySuffix.substring(0, slashIdx + 1) + newBass + tail
                }
            }

            "[$newRoot$qualitySuffix]"
        }
    }

    private fun parseNoteToPitchClass(note: String): Int? =
        Notes.NOTE_NAMES_SHARP.indexOf(note).takeIf { it >= 0 }
            ?: Notes.NOTE_NAMES_FLAT.indexOf(note).takeIf { it >= 0 }

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

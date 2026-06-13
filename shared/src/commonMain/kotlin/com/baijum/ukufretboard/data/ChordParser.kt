package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.domain.ChordNameParser

/**
 * Parses chord names from text containing `[ChordName]` markers.
 *
 * Uses [ChordNameParser.parse] to validate that bracket contents are
 * real chords, so section labels like `[Coda]`, `[Fine]`, `[Ending]`
 * are never mistaken for chord markers.
 *
 * Accepts any content after the root note (A-G with optional # or b),
 * supporting all chord qualities including slash chords (e.g., `[C/G]`).
 */
object ChordParser {

    /** Regex matching potential `[ChordName]` markers — root note + any suffix. */
    private val BRACKET_PATTERN = Regex("""\[([A-G][#b]?[^]]*)]""")

    /**
     * Returns true if the bracket content parses as a valid chord.
     * For slash chords, only the part before the slash is validated
     * (preserving unknown bass notes like `[C/X]`).
     */
    private fun isValidChord(bracketContent: String): Boolean {
        val slashIdx = bracketContent.indexOf('/')
        val toValidate = if (slashIdx >= 0) {
            bracketContent.substring(0, slashIdx)
        } else {
            bracketContent
        }
        return ChordNameParser.parse(toValidate) != null
    }

    /**
     * Extracts all unique chord names from the given text.
     *
     * @param text The text containing `[ChordName]` markers.
     * @return A list of unique chord name strings found in order.
     */
    fun extractChords(text: String): List<String> =
        BRACKET_PATTERN.findAll(text)
            .map { it.groupValues[1] }
            .filter { isValidChord(it) }
            .distinct()
            .toList()

    /**
     * Splits text into segments: plain text and chord markers.
     *
     * @param line A single line of text.
     * @return A list of [TextSegment] alternating between text and chord markers.
     */
    fun parseLine(line: String): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var lastEnd = 0

        BRACKET_PATTERN.findAll(line).forEach { match ->
            val content = match.groupValues[1]
            if (isValidChord(content)) {
                if (match.range.first > lastEnd) {
                    segments.add(TextSegment.PlainText(line.substring(lastEnd, match.range.first)))
                }
                segments.add(TextSegment.Chord(content))
                lastEnd = match.range.last + 1
            }
        }

        if (lastEnd < line.length) {
            segments.add(TextSegment.PlainText(line.substring(lastEnd)))
        }

        return segments
    }

    /**
     * A segment of parsed text — either plain text or a chord name.
     */
    sealed class TextSegment {
        data class PlainText(val text: String) : TextSegment()
        data class Chord(val name: String) : TextSegment()
    }
}

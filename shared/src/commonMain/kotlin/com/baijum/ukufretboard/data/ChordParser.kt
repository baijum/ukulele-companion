package com.baijum.ukufretboard.data

/**
 * Parses chord names from text containing `[ChordName]` markers.
 *
 * Uses the same permissive regex as [com.baijum.ukufretboard.domain.ChordSheetTranspose]
 * to ensure consistent chord recognition across parsing and transposition.
 * Accepts any content after the root note (A-G with optional # or b),
 * supporting all chord qualities including slash chords (e.g., `[C/G]`).
 *
 * Section labels like `[Chorus]`, `[Bridge]`, `[Intro]` are excluded via a
 * negative lookahead so they are not mistakenly treated as chord markers.
 */
object ChordParser {

    /** Known section label names that must not be matched as chords. */
    val SECTION_NAMES = "Chorus|Verse|Bridge|Intro|Outro|Tab|Interlude"

    /** Regex matching `[ChordName]` markers — root note + any quality suffix. */
    val CHORD_PATTERN = Regex("""\[(?!(?:$SECTION_NAMES)\b)([A-G][#b]?[^]]*)]""")

    /**
     * Extracts all unique chord names from the given text.
     *
     * @param text The text containing `[ChordName]` markers.
     * @return A list of unique chord name strings found in order.
     */
    fun extractChords(text: String): List<String> =
        CHORD_PATTERN.findAll(text)
            .map { it.groupValues[1] }
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

        CHORD_PATTERN.findAll(line).forEach { match ->
            // Add text before the chord
            if (match.range.first > lastEnd) {
                segments.add(TextSegment.PlainText(line.substring(lastEnd, match.range.first)))
            }
            // Add the chord
            segments.add(TextSegment.Chord(match.groupValues[1]))
            lastEnd = match.range.last + 1
        }

        // Add remaining text
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

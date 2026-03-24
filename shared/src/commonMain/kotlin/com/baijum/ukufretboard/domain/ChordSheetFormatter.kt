package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progression

/**
 * Formatting and sharing utilities for chord sheets and progressions.
 */
object ChordSheetFormatter {

    /**
     * Formats a chord sheet with chords placed above the lyrics.
     *
     * Converts inline `[Chord]` notation to a "chords above lyrics" layout:
     * ```
     *    C              Em
     * Somewhere over the rainbow
     * ```
     *
     * @param sheet The chord sheet to format.
     * @return Formatted text with chords above lyrics.
     */
    fun formatChordsAboveLyrics(sheet: ChordSheet): String {
        val sb = StringBuilder()

        sb.appendLine(sheet.title)
        if (sheet.subtitle.isNotEmpty()) {
            sb.appendLine(sheet.subtitle)
        }
        if (sheet.artist.isNotEmpty()) {
            sb.appendLine("by ${sheet.artist}")
        }
        sb.appendLine()

        sheet.content.lines().forEach { line ->
            val segments = ChordParser.parseLine(line)
            if (segments.isEmpty() || segments.all { it is ChordParser.TextSegment.PlainText }) {
                // No chords — just output the line
                sb.appendLine(line.replace(Regex("\\[[^]]*]"), ""))
            } else {
                // Build chord line and lyric line
                val chordLine = StringBuilder()
                val lyricLine = StringBuilder()

                segments.forEach { segment ->
                    when (segment) {
                        is ChordParser.TextSegment.PlainText -> {
                            // Pad chord line to align
                            while (chordLine.length < lyricLine.length) {
                                chordLine.append(' ')
                            }
                            lyricLine.append(segment.text)
                        }
                        is ChordParser.TextSegment.Chord -> {
                            // Pad chord line to match current position
                            while (chordLine.length < lyricLine.length) {
                                chordLine.append(' ')
                            }
                            chordLine.append(segment.name)
                        }
                    }
                }

                val chordStr = chordLine.toString().trimEnd()
                if (chordStr.isNotEmpty()) {
                    sb.appendLine(chordStr)
                }
                sb.appendLine(lyricLine.toString())
            }
        }

        return sb.toString().trimEnd()
    }

    /**
     * Formats a chord sheet as plain text with inline chord brackets.
     *
     * @param sheet The chord sheet to format.
     * @return Plain text representation.
     */
    fun formatPlainText(sheet: ChordSheet): String {
        val sb = StringBuilder()
        sb.appendLine(sheet.title)
        if (sheet.artist.isNotEmpty()) {
            sb.appendLine("by ${sheet.artist}")
        }
        sb.appendLine()
        sb.append(sheet.content)
        return sb.toString().trimEnd()
    }

    /**
     * Formats a chord progression as a shareable one-line summary.
     *
     * Example: "Pop / Four Chords in C: C – G – Am – F"
     *
     * @param progression The progression to format.
     * @param keyRoot The root pitch class (0–11).
     * @return Formatted progression string.
     */
    fun formatProgression(
        progression: Progression,
        keyRoot: Int,
    ): String {
        val keyName = Notes.enharmonicForKey(keyRoot, keyRoot)
        val chords = progression.degrees.joinToString(" \u2013 ") { degree ->
            val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            Notes.enharmonicForKey(chordRoot, keyRoot) + degree.quality
        }
        val numerals = progression.degrees.joinToString(" \u2013 ") { it.numeral }
        return "${progression.name} in $keyName:\n$numerals\n$chords"
    }

}

package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.Progression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordSheetFormatterTest {

    private fun sheet(
        title: String = "Test Song",
        artist: String = "",
        content: String = "",
    ) = ChordSheet(
        id = "test-id",
        title = title,
        artist = artist,
        content = content,
        createdAt = 0L,
        updatedAt = 0L,
    )

    // --- formatChordsAboveLyrics() ---

    @Test
    fun chordsAboveLyricsIncludesTitle() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(title = "My Song", content = "Hello")
        )
        assertTrue(result.startsWith("My Song"), "Should start with title: $result")
    }

    @Test
    fun chordsAboveLyricsIncludesArtist() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(title = "My Song", artist = "The Band", content = "Hello")
        )
        assertTrue(result.contains("by The Band"), "Should include artist: $result")
    }

    @Test
    fun chordsAboveLyricsOmitsEmptyArtist() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(title = "My Song", content = "Hello")
        )
        assertFalse(result.contains("by "), "Should not include 'by' when artist is empty")
    }

    @Test
    fun chordsAboveLyricsPlacesChordsAbove() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(content = "[C]Hello [G]world")
        )
        val lines = result.lines()
        // Find the chord line and lyric line
        val chordLineIdx = lines.indexOfFirst { it.trimStart().startsWith("C") && it.contains("G") }
        val lyricLineIdx = lines.indexOfFirst { it.contains("Hello") && it.contains("world") }
        assertTrue(chordLineIdx >= 0, "Should have a chord line with C and G")
        assertTrue(lyricLineIdx >= 0, "Should have a lyric line")
        assertTrue(chordLineIdx < lyricLineIdx, "Chords should be above lyrics")
    }

    @Test
    fun chordsAboveLyricsStripsChordBrackets() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(content = "[Am]Lyrics here")
        )
        assertFalse(result.contains("[Am]"), "Brackets should be stripped")
        assertTrue(result.contains("Lyrics here"), "Lyrics should remain")
        assertTrue(result.contains("Am"), "Chord name should appear (above)")
    }

    @Test
    fun plainLineWithoutChordsPassesThrough() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(content = "Just plain text")
        )
        assertTrue(result.contains("Just plain text"), "Plain text should pass through")
    }

    @Test
    fun emptyContentProducesHeaderOnly() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(title = "Empty", content = "")
        )
        assertTrue(result.contains("Empty"), "Should contain title")
    }

    @Test
    fun multipleLinesChordsAboveLyrics() {
        val result = ChordSheetFormatter.formatChordsAboveLyrics(
            sheet(content = "[C]Line one\n[Am]Line two")
        )
        assertTrue(result.contains("Line one"), "First line lyrics present")
        assertTrue(result.contains("Line two"), "Second line lyrics present")
        assertTrue(result.contains("C"), "C chord present")
        assertTrue(result.contains("Am"), "Am chord present")
    }

    // --- formatPlainText() ---

    @Test
    fun plainTextIncludesTitle() {
        val result = ChordSheetFormatter.formatPlainText(
            sheet(title = "My Song", content = "[C]Hello")
        )
        assertTrue(result.startsWith("My Song"), "Should start with title")
    }

    @Test
    fun plainTextIncludesArtist() {
        val result = ChordSheetFormatter.formatPlainText(
            sheet(title = "Song", artist = "Artist", content = "text")
        )
        assertTrue(result.contains("by Artist"))
    }

    @Test
    fun plainTextOmitsEmptyArtist() {
        val result = ChordSheetFormatter.formatPlainText(
            sheet(title = "Song", content = "text")
        )
        assertFalse(result.contains("by "))
    }

    @Test
    fun plainTextPreservesChordBrackets() {
        val result = ChordSheetFormatter.formatPlainText(
            sheet(content = "[Am]Hello [G]world")
        )
        assertTrue(result.contains("[Am]"), "Chord brackets should be preserved")
        assertTrue(result.contains("[G]"), "Chord brackets should be preserved")
    }

    @Test
    fun plainTextPreservesLineBreaks() {
        val result = ChordSheetFormatter.formatPlainText(
            sheet(content = "Line 1\nLine 2\nLine 3")
        )
        assertTrue(result.contains("Line 1"))
        assertTrue(result.contains("Line 2"))
        assertTrue(result.contains("Line 3"))
    }

    // --- formatProgression() ---

    @Test
    fun progressionIncludesNameAndKey() {
        val prog = Progression(
            "Pop", "desc",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(7, "", "V"),
            ),
        )
        val result = ChordSheetFormatter.formatProgression(prog, 0)
        assertTrue(result.contains("Pop"), "Should contain progression name")
        assertTrue(result.contains("C"), "Should contain key name C")
    }

    @Test
    fun progressionInCMajorShowsCorrectChords() {
        val prog = Progression(
            "I-IV-V-I", "test",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
                ChordDegree(0, "", "I"),
            ),
        )
        val result = ChordSheetFormatter.formatProgression(prog, 0)
        // Check chord names
        assertTrue(result.contains("C"), "Should contain C")
        assertTrue(result.contains("F"), "Should contain F")
        assertTrue(result.contains("G"), "Should contain G")
    }

    @Test
    fun progressionShowsRomanNumerals() {
        val prog = Progression(
            "test", "desc",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(9, "m", "vi"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
            ),
        )
        val result = ChordSheetFormatter.formatProgression(prog, 0)
        assertTrue(result.contains("I"), "Should contain I")
        assertTrue(result.contains("vi"), "Should contain vi")
        assertTrue(result.contains("IV"), "Should contain IV")
        assertTrue(result.contains("V"), "Should contain V")
    }

    @Test
    fun progressionInGMajor() {
        val prog = Progression(
            "test", "desc",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
            ),
        )
        val result = ChordSheetFormatter.formatProgression(prog, 7) // G major
        assertTrue(result.contains("G"), "Should contain G as key/root")
        assertTrue(result.contains("D"), "V of G is D")
    }

    @Test
    fun progressionWithMinorChords() {
        val prog = Progression(
            "test", "desc",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(9, "m", "vi"),
            ),
        )
        val result = ChordSheetFormatter.formatProgression(prog, 0)
        assertTrue(result.contains("Am"), "vi of C should be Am")
    }

    @Test
    fun progressionUsesEnDashSeparator() {
        val prog = Progression(
            "test", "desc",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
            ),
        )
        val result = ChordSheetFormatter.formatProgression(prog, 0)
        assertTrue(result.contains("\u2013"), "Should use en dash separator")
    }

    @Test
    fun singleChordProgression() {
        val prog = Progression(
            "One Chord", "desc",
            listOf(ChordDegree(0, "", "I")),
        )
        val result = ChordSheetFormatter.formatProgression(prog, 0)
        assertTrue(result.contains("C"), "Should contain chord C")
        assertFalse(result.contains("\u2013"), "No separator for single chord")
    }
}

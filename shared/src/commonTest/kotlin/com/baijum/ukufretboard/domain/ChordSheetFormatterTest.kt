package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.Progression
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChordSheetFormatterTest {
    private fun sheet(
        title: String = "Test Song",
        subtitle: String = "",
        artist: String = "",
        content: String = "",
    ) = ChordSheet(
        id = "test-id",
        title = title,
        subtitle = subtitle,
        artist = artist,
        content = content,
        createdAt = 0L,
        updatedAt = 0L,
    )

    // --- formatChordsAboveLyrics() ---

    @Test
    fun chordsAboveLyricsIncludesTitle() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(title = "My Song", content = "Hello"),
            )
        assertTrue(result.startsWith("My Song"), "Should start with title: $result")
    }

    @Test
    fun chordsAboveLyricsIncludesArtist() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(title = "My Song", artist = "The Band", content = "Hello"),
            )
        assertTrue(result.contains("by The Band"), "Should include artist: $result")
    }

    @Test
    fun chordsAboveLyricsOmitsEmptyArtist() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(title = "My Song", content = "Hello"),
            )
        assertFalse(result.contains("by "), "Should not include 'by' when artist is empty")
    }

    @Test
    fun chordsAboveLyricsIncludesSubtitle() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(title = "My Song", subtitle = "Words by Newton", content = "Hello"),
            )
        assertTrue(result.contains("Words by Newton"), "Should include subtitle: $result")
    }

    @Test
    fun chordsAboveLyricsOmitsEmptySubtitle() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(title = "My Song", content = "Hello"),
            )
        assertFalse(result.contains("Words by"), "Should not include subtitle when empty")
    }

    @Test
    fun chordsAboveLyricsPlacesChordsAbove() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(content = "[C]Hello [G]world"),
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
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(content = "[Am]Lyrics here"),
            )
        assertFalse(result.contains("[Am]"), "Brackets should be stripped")
        assertTrue(result.contains("Lyrics here"), "Lyrics should remain")
        assertTrue(result.contains("Am"), "Chord name should appear (above)")
    }

    @Test
    fun plainLineWithoutChordsPassesThrough() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(content = "Just plain text"),
            )
        assertTrue(result.contains("Just plain text"), "Plain text should pass through")
    }

    @Test
    fun emptyContentProducesHeaderOnly() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(title = "Empty", content = ""),
            )
        assertTrue(result.contains("Empty"), "Should contain title")
    }

    @Test
    fun multipleLinesChordsAboveLyrics() {
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(content = "[C]Line one\n[Am]Line two"),
            )
        assertTrue(result.contains("Line one"), "First line lyrics present")
        assertTrue(result.contains("Line two"), "Second line lyrics present")
        assertTrue(result.contains("C"), "C chord present")
        assertTrue(result.contains("Am"), "Am chord present")
    }

    @Test
    fun chordsAboveLyricsKeepsSectionHeaders() {
        // Regression for #590: section labels are chord-free lines, so the
        // "no chords" branch must emit them verbatim rather than stripping the
        // bracket token. Both label shapes reach this branch:
        // [Chorus] (root C + "horus", invalid chord) and [Verse] (V not A-G).
        val result =
            ChordSheetFormatter.formatChordsAboveLyrics(
                sheet(content = "[Verse]\nLine A\n[Chorus]\nLine B"),
            )
        assertTrue(result.contains("[Verse]"), "Verse header should survive: $result")
        assertTrue(result.contains("[Chorus]"), "Chorus header should survive: $result")
    }

    // --- formatPlainText() ---

    @Test
    fun plainTextIncludesTitle() {
        val result =
            ChordSheetFormatter.formatPlainText(
                sheet(title = "My Song", content = "[C]Hello"),
            )
        assertTrue(result.startsWith("My Song"), "Should start with title")
    }

    @Test
    fun plainTextIncludesArtist() {
        val result =
            ChordSheetFormatter.formatPlainText(
                sheet(title = "Song", artist = "Artist", content = "text"),
            )
        assertTrue(result.contains("by Artist"))
    }

    @Test
    fun plainTextOmitsEmptyArtist() {
        val result =
            ChordSheetFormatter.formatPlainText(
                sheet(title = "Song", content = "text"),
            )
        assertFalse(result.contains("by "))
    }

    @Test
    fun plainTextIncludesSubtitle() {
        val result =
            ChordSheetFormatter.formatPlainText(
                sheet(title = "Song", subtitle = "A Subtitle", content = "text"),
            )
        assertTrue(result.contains("A Subtitle"), "Should include subtitle: $result")
    }

    @Test
    fun plainTextOmitsEmptySubtitle() {
        val result =
            ChordSheetFormatter.formatPlainText(
                sheet(title = "Song", content = "text"),
            )
        val lines = result.lines()
        assertEquals("Song", lines[0], "First line should be title")
        assertTrue(lines[1].isEmpty(), "Second line should be blank when no subtitle/artist")
    }

    @Test
    fun plainTextPreservesChordBrackets() {
        val result =
            ChordSheetFormatter.formatPlainText(
                sheet(content = "[Am]Hello [G]world"),
            )
        assertTrue(result.contains("[Am]"), "Chord brackets should be preserved")
        assertTrue(result.contains("[G]"), "Chord brackets should be preserved")
    }

    @Test
    fun plainTextPreservesLineBreaks() {
        val result =
            ChordSheetFormatter.formatPlainText(
                sheet(content = "Line 1\nLine 2\nLine 3"),
            )
        assertTrue(result.contains("Line 1"))
        assertTrue(result.contains("Line 2"))
        assertTrue(result.contains("Line 3"))
    }

    // --- formatProgression() ---

    @Test
    fun progressionIncludesNameAndKey() {
        val prog =
            Progression(
                "Pop",
                "desc",
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
        val prog =
            Progression(
                "I-IV-V-I",
                "test",
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
        val prog =
            Progression(
                "test",
                "desc",
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
        val prog =
            Progression(
                "test",
                "desc",
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
        val prog =
            Progression(
                "test",
                "desc",
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
        val prog =
            Progression(
                "test",
                "desc",
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
        val prog =
            Progression(
                "One Chord",
                "desc",
                listOf(ChordDegree(0, "", "I")),
            )
        val result = ChordSheetFormatter.formatProgression(prog, 0)
        assertTrue(result.contains("C"), "Should contain chord C")
        assertFalse(result.contains("\u2013"), "No separator for single chord")
    }

    // --- extractTempo() ---

    @Test
    fun extractTempoReturnsNullForNoTempoLine() {
        assertNull(ChordSheetFormatter.extractTempo("[C]Hello [G]World"))
    }

    @Test
    fun extractTempoReturnsNullForEmptyContent() {
        assertNull(ChordSheetFormatter.extractTempo(""))
    }

    @Test
    fun extractTempoExtractsBpm() {
        assertEquals(120, ChordSheetFormatter.extractTempo("Tempo: 120 BPM"))
    }

    @Test
    fun extractTempoIsCaseInsensitive() {
        assertEquals(80, ChordSheetFormatter.extractTempo("tempo: 80 bpm"))
    }

    @Test
    fun extractTempoIgnoresPartialMatches() {
        assertNull(ChordSheetFormatter.extractTempo("My Tempo: fast"))
    }

    @Test
    fun extractTempoWorksAmongOtherLines() {
        val content = "[Verse]\n[C]Hello world\nTempo: 140 BPM\n[G]Goodbye"
        assertEquals(140, ChordSheetFormatter.extractTempo(content))
    }

    @Test
    fun extractTempoIgnoresTempoInMiddleOfLine() {
        assertNull(ChordSheetFormatter.extractTempo("The Tempo: 100 BPM is fast"))
    }

    // --- extractSections() ---

    @Test
    fun extractSectionsReturnsEmptyForNoSections() {
        val sections = ChordSheetFormatter.extractSections("Just plain text\nMore text")
        assertTrue(sections.isEmpty())
    }

    @Test
    fun extractSectionsFindsVerse() {
        val sections = ChordSheetFormatter.extractSections("[Verse]\n[C]Hello world")
        assertEquals(1, sections.size)
        assertEquals("Verse", sections[0].label)
        assertEquals(0, sections[0].lineIndex)
    }

    @Test
    fun extractSectionsFindsMultipleSections() {
        val content = "[Verse]\nLine 1\n[Chorus]\nLine 2\n[Bridge]\nLine 3"
        val sections = ChordSheetFormatter.extractSections(content)
        assertEquals(3, sections.size)
        assertEquals("Verse", sections[0].label)
        assertEquals(0, sections[0].lineIndex)
        assertEquals("Chorus", sections[1].label)
        assertEquals(2, sections[1].lineIndex)
        assertEquals("Bridge", sections[2].label)
        assertEquals(4, sections[2].lineIndex)
    }

    @Test
    fun extractSectionsHandlesNumberedSections() {
        val content = "[Verse 1]\nLine 1\n[Verse 2]\nLine 2"
        val sections = ChordSheetFormatter.extractSections(content)
        assertEquals(2, sections.size)
        assertEquals("Verse 1", sections[0].label)
        assertEquals("Verse 2", sections[1].label)
    }

    @Test
    fun extractSectionsDoesNotMatchChords() {
        val content = "[Am]Hello [C#m7]World"
        val sections = ChordSheetFormatter.extractSections(content)
        assertTrue(sections.isEmpty(), "Chords should not be matched as sections")
    }

    @Test
    fun extractSectionsMixedContent() {
        val content = "[Intro]\n[Am]Hello [G]World\n[Chorus]\n[C]Sing along"
        val sections = ChordSheetFormatter.extractSections(content)
        assertEquals(2, sections.size)
        assertEquals("Intro", sections[0].label)
        assertEquals(0, sections[0].lineIndex)
        assertEquals("Chorus", sections[1].label)
        assertEquals(2, sections[1].lineIndex)
    }

    @Test
    fun extractSectionsCorrectLineIndices() {
        val content = "Line 0\nLine 1\n[Verse]\nLine 3\nLine 4\n[Chorus]\nLine 6"
        val sections = ChordSheetFormatter.extractSections(content)
        assertEquals(2, sections.size)
        assertEquals(2, sections[0].lineIndex)
        assertEquals(5, sections[1].lineIndex)
    }

    @Test
    fun extractSectionsReturnsEmptyForEmptyContent() {
        assertTrue(ChordSheetFormatter.extractSections("").isEmpty())
    }

    // --- Property tests: extractTempo / extractSections ---

    @Test
    fun extractTempoNeverCrashesOnArbitraryStrings() {
        runBlocking {
            checkAll(Arb.string(0..200)) { content ->
                ChordSheetFormatter.extractTempo(content)
            }
        }
    }

    @Test
    fun extractSectionsNeverCrashesOnArbitraryStrings() {
        runBlocking {
            checkAll(Arb.string(0..200)) { content ->
                ChordSheetFormatter.extractSections(content)
            }
        }
    }

    @Test
    fun extractTempoRoundTripsValidBpm() {
        runBlocking {
            checkAll(Arb.int(30..300)) { bpm ->
                val content = "Tempo: $bpm BPM"
                assertEquals(
                    bpm,
                    ChordSheetFormatter.extractTempo(content),
                    "Should extract BPM $bpm from generated tempo line",
                )
            }
        }
    }

    @Test
    fun extractTempoRoundTripsWithSurroundingContent() {
        runBlocking {
            checkAll(Arb.int(30..300)) { bpm ->
                val content = "[Verse]\n[C]Hello\nTempo: $bpm BPM\n[G]World"
                assertEquals(
                    bpm,
                    ChordSheetFormatter.extractTempo(content),
                    "Should extract BPM $bpm from content with surrounding lines",
                )
            }
        }
    }
}

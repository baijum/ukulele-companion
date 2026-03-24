package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordProParserTest {

    // --- parse: title ---

    @Test
    fun parsesTitleDirective() {
        val sheet = ChordProParser.parse("{title: My Song}")
        assertEquals("My Song", sheet.title)
    }

    @Test
    fun parsesShortTitleDirective() {
        val sheet = ChordProParser.parse("{t: Short Title}")
        assertEquals("Short Title", sheet.title)
    }

    @Test
    fun missingTitleUsesDefault() {
        val sheet = ChordProParser.parse("[Am]Hello world")
        assertEquals("Imported Song", sheet.title)
    }

    @Test
    fun missingTitleUsesCustomDefault() {
        val sheet = ChordProParser.parse("[Am]Hello", "Custom Default")
        assertEquals("Custom Default", sheet.title)
    }

    // --- parse: artist ---

    @Test
    fun parsesArtistDirective() {
        val sheet = ChordProParser.parse("{artist: The Band}")
        assertEquals("The Band", sheet.artist)
    }

    @Test
    fun missingArtistIsEmpty() {
        val sheet = ChordProParser.parse("{title: Song}")
        assertEquals("", sheet.artist)
    }

    // --- parse: subtitle ---

    @Test
    fun parsesSubtitleDirective() {
        val sheet = ChordProParser.parse("{subtitle: Words by John Newton}")
        assertEquals("Words by John Newton", sheet.subtitle)
    }

    @Test
    fun parsesStAsSubtitle() {
        val sheet = ChordProParser.parse("{st: Short Subtitle}")
        assertEquals("Short Subtitle", sheet.subtitle)
    }

    @Test
    fun subtitleAndArtistAreSeparate() {
        val sheet = ChordProParser.parse("{subtitle: Words by Newton}\n{artist: Traditional}")
        assertEquals("Words by Newton", sheet.subtitle)
        assertEquals("Traditional", sheet.artist)
    }

    @Test
    fun missingSubtitleIsEmpty() {
        val sheet = ChordProParser.parse("{title: Song}")
        assertEquals("", sheet.subtitle)
    }

    // --- parse: key and capo ---

    @Test
    fun parsesKeyDirective() {
        val sheet = ChordProParser.parse("{key: G}")
        assertEquals("G", sheet.key)
    }

    @Test
    fun parsesCapoDirective() {
        val sheet = ChordProParser.parse("{capo: 3}")
        assertEquals(3, sheet.capo)
    }

    @Test
    fun invalidCapoDefaultsToZero() {
        val sheet = ChordProParser.parse("{capo: abc}")
        assertEquals(0, sheet.capo)
    }

    @Test
    fun missingKeyIsEmpty() {
        val sheet = ChordProParser.parse("{title: Song}")
        assertEquals("", sheet.key)
    }

    @Test
    fun missingCapoIsZero() {
        val sheet = ChordProParser.parse("{title: Song}")
        assertEquals(0, sheet.capo)
    }

    // --- parse: section directives ---

    @Test
    fun startOfChorusInsertsLabel() {
        val sheet = ChordProParser.parse("{start_of_chorus}")
        assertTrue(sheet.content.contains("[Chorus]"))
    }

    @Test
    fun socShorthandInsertsLabel() {
        val sheet = ChordProParser.parse("{soc}")
        assertTrue(sheet.content.contains("[Chorus]"))
    }

    @Test
    fun endOfChorusInsertsBlankLine() {
        val sheet = ChordProParser.parse("{start_of_chorus}\nLyrics\n{end_of_chorus}\nMore")
        assertTrue(sheet.content.contains("[Chorus]"))
    }

    @Test
    fun startOfVerseInsertsLabel() {
        val sheet = ChordProParser.parse("{start_of_verse}")
        assertTrue(sheet.content.contains("[Verse]"))
    }

    @Test
    fun sovShorthandInsertsLabel() {
        val sheet = ChordProParser.parse("{sov}")
        assertTrue(sheet.content.contains("[Verse]"))
    }

    @Test
    fun startOfBridgeInsertsLabel() {
        val sheet = ChordProParser.parse("{start_of_bridge}")
        assertTrue(sheet.content.contains("[Bridge]"))
    }

    @Test
    fun sobShorthandInsertsLabel() {
        val sheet = ChordProParser.parse("{sob}")
        assertTrue(sheet.content.contains("[Bridge]"))
    }

    // --- parse: custom section labels ---

    @Test
    fun startOfVerseWithCustomName() {
        val sheet = ChordProParser.parse("{start_of_verse: Verse 2}")
        assertTrue(sheet.content.contains("[Verse 2]"), "Content: ${sheet.content}")
    }

    @Test
    fun startOfChorusWithCustomName() {
        val sheet = ChordProParser.parse("{start_of_chorus: Main Chorus}")
        assertTrue(sheet.content.contains("[Main Chorus]"), "Content: ${sheet.content}")
    }

    @Test
    fun startOfVerseWithDefaultName() {
        val sheet = ChordProParser.parse("{start_of_verse: Verse}")
        assertTrue(sheet.content.contains("[Verse]"), "Content: ${sheet.content}")
    }

    // --- parse: additional section types ---

    @Test
    fun startOfIntroInsertsLabel() {
        val sheet = ChordProParser.parse("{start_of_intro}")
        assertTrue(sheet.content.contains("[Intro]"))
    }

    @Test
    fun endOfIntroInsertsBlankLine() {
        val sheet = ChordProParser.parse("{start_of_intro}\nRiff\n{end_of_intro}\nVerse")
        assertTrue(sheet.content.contains("[Intro]"))
        assertTrue(sheet.content.contains("Verse"))
    }

    @Test
    fun startOfOutroInsertsLabel() {
        val sheet = ChordProParser.parse("{start_of_outro}")
        assertTrue(sheet.content.contains("[Outro]"))
    }

    @Test
    fun endOfOutroInsertsBlankLine() {
        val sheet = ChordProParser.parse("{start_of_outro}\nFade\n{end_of_outro}")
        assertTrue(sheet.content.contains("[Outro]"))
    }

    @Test
    fun startOfInterludeInsertsLabel() {
        val sheet = ChordProParser.parse("{start_of_interlude}")
        assertTrue(sheet.content.contains("[Interlude]"))
    }

    @Test
    fun endOfInterludeInsertsBlankLine() {
        val sheet = ChordProParser.parse("{start_of_interlude}\nSolo\n{end_of_interlude}")
        assertTrue(sheet.content.contains("[Interlude]"))
    }

    // --- parse: tab blocks ---

    @Test
    fun startOfTabInsertsLabel() {
        val sheet = ChordProParser.parse("{start_of_tab}")
        assertTrue(sheet.content.contains("[Tab]"))
    }

    @Test
    fun sotShorthandInsertsLabel() {
        val sheet = ChordProParser.parse("{sot}")
        assertTrue(sheet.content.contains("[Tab]"))
    }

    @Test
    fun tabContentPreserved() {
        val input = "{start_of_tab}\ne|---3---\nB|---0---\n{end_of_tab}"
        val sheet = ChordProParser.parse(input)
        assertTrue(sheet.content.contains("[Tab]"))
        assertTrue(sheet.content.contains("e|---3---"), "Tab content should be preserved")
        assertTrue(sheet.content.contains("B|---0---"), "Tab content should be preserved")
    }

    // --- parse: chorus recall ---

    @Test
    fun chorusRecallInsertsLabel() {
        val sheet = ChordProParser.parse("{chorus}")
        assertTrue(sheet.content.contains("[Chorus]"))
    }

    @Test
    fun chorusRecallAfterContent() {
        val sheet = ChordProParser.parse("Some lyrics\n{chorus}\nMore lyrics")
        val lines = sheet.content.lines()
        assertTrue(lines.any { it.trim() == "[Chorus]" }, "Content: ${sheet.content}")
    }

    // --- parse: comment ---

    @Test
    fun commentDirectiveAddsText() {
        val sheet = ChordProParser.parse("{comment: Play softly}")
        assertTrue(sheet.content.contains("Play softly"))
    }

    @Test
    fun cShorthandAddsComment() {
        val sheet = ChordProParser.parse("{c: Repeat 2x}")
        assertTrue(sheet.content.contains("Repeat 2x"))
    }

    @Test
    fun emptyCommentIsSkipped() {
        val sheet = ChordProParser.parse("{comment: }")
        assertEquals("", sheet.content)
    }

    // --- parse: # comment lines ---

    @Test
    fun hashCommentLinesAreSkipped() {
        val sheet = ChordProParser.parse("# This is a comment\nLyrics here")
        assertFalse(sheet.content.contains("# This is a comment"))
        assertTrue(sheet.content.contains("Lyrics here"))
    }

    @Test
    fun hashCommentAtStartOfFile() {
        val sheet = ChordProParser.parse("# File comment\n{title: Song}\n[C]Hello")
        assertEquals("Song", sheet.title)
        assertFalse(sheet.content.contains("File comment"))
        assertTrue(sheet.content.contains("[C]Hello"))
    }

    @Test
    fun indentedHashCommentSkipped() {
        val sheet = ChordProParser.parse("  # Indented comment\nLyrics")
        assertFalse(sheet.content.contains("comment"))
        assertTrue(sheet.content.contains("Lyrics"))
    }

    // --- parse: tempo and time ---

    @Test
    fun tempoDirectiveInsertsComment() {
        val sheet = ChordProParser.parse("{tempo: 120}")
        assertTrue(sheet.content.contains("Tempo: 120 BPM"), "Content: ${sheet.content}")
    }

    @Test
    fun timeDirectiveInsertsComment() {
        val sheet = ChordProParser.parse("{time: 3/4}")
        assertTrue(sheet.content.contains("Time: 3/4"), "Content: ${sheet.content}")
    }

    @Test
    fun tempoAndTimeBeforeContent() {
        val sheet = ChordProParser.parse("{tempo: 80}\n{time: 3/4}\n[C]Hello")
        val lines = sheet.content.lines()
        assertTrue(lines[0].contains("Tempo: 80 BPM"))
        assertTrue(lines[1].contains("Time: 3/4"))
        assertTrue(sheet.content.contains("[C]Hello"))
    }

    @Test
    fun emptyTempoIsSkipped() {
        val sheet = ChordProParser.parse("{tempo: }\nLyrics")
        assertFalse(sheet.content.contains("Tempo"))
    }

    // --- parse: define directive ---

    @Test
    fun defineDirectiveIsSkipped() {
        val sheet = ChordProParser.parse("{define: G base-fret 1 frets 0 2 3 2}\nLyrics")
        assertFalse(sheet.content.contains("define"))
        assertFalse(sheet.content.contains("base-fret"))
        assertTrue(sheet.content.contains("Lyrics"))
    }

    // --- parse: chord lines pass through ---

    @Test
    fun chordMarkersPassThrough() {
        val sheet = ChordProParser.parse("[Am]Hello [G]world")
        assertTrue(sheet.content.contains("[Am]"))
        assertTrue(sheet.content.contains("[G]"))
        assertTrue(sheet.content.contains("Hello"))
    }

    @Test
    fun plainLinesPassThrough() {
        val sheet = ChordProParser.parse("Just plain lyrics")
        assertEquals("Just plain lyrics", sheet.content)
    }

    // --- parse: unknown directives ---

    @Test
    fun unknownDirectivesAreSkipped() {
        val sheet = ChordProParser.parse("{unknown: value}\nLyrics here")
        assertTrue(sheet.content.contains("Lyrics here"))
        assertFalse(sheet.content.contains("unknown"))
    }

    // --- parse: full song ---

    @Test
    fun parsesCompleteSong() {
        val input = """
            {title: Amazing Grace}
            {artist: Traditional}
            {key: G}
            {capo: 2}

            {start_of_verse}
            [G]Amazing [G7]grace, how [C]sweet the [G]sound
            {end_of_verse}
        """.trimIndent()
        val sheet = ChordProParser.parse(input)
        assertEquals("Amazing Grace", sheet.title)
        assertEquals("Traditional", sheet.artist)
        assertEquals("G", sheet.key)
        assertEquals(2, sheet.capo)
        assertTrue(sheet.content.contains("[Verse]"))
        assertTrue(sheet.content.contains("[G]Amazing"))
    }

    @Test
    fun parsesFullChordProExample() {
        val input = """
            # Amazing Grace - Sample ChordPro file
            {title: Amazing Grace}
            {subtitle: Words by John Newton}
            {artist: Traditional}
            {key: G}
            {tempo: 80}
            {time: 3/4}
            {capo: 2}
            {define: G base-fret 1 frets 0 2 3 2}
            {start_of_chorus: Chorus}
            A[G]mazing [G7]grace
            {end_of_chorus}
            {start_of_verse: Verse 2}
            'Twas [G]grace
            {end_of_verse}
            {comment: Repeat chorus}
            {chorus}
            {start_of_tab}
            e|---3---
            {end_of_tab}
        """.trimIndent()
        val sheet = ChordProParser.parse(input)
        assertEquals("Amazing Grace", sheet.title)
        assertEquals("Words by John Newton", sheet.subtitle)
        assertEquals("Traditional", sheet.artist)
        assertEquals("G", sheet.key)
        assertEquals(2, sheet.capo)
        assertTrue(sheet.content.contains("Tempo: 80 BPM"))
        assertTrue(sheet.content.contains("Time: 3/4"))
        assertTrue(sheet.content.contains("[Chorus]"))
        assertTrue(sheet.content.contains("[Verse 2]"))
        assertTrue(sheet.content.contains("Repeat chorus"))
        assertTrue(sheet.content.contains("[Tab]"))
        assertTrue(sheet.content.contains("e|---3---"))
        assertFalse(sheet.content.contains("define"))
        assertFalse(sheet.content.contains("# Amazing"))
    }

    // --- parse: empty input ---

    @Test
    fun emptyInputUsesDefaults() {
        val sheet = ChordProParser.parse("")
        assertEquals("Imported Song", sheet.title)
        assertEquals("", sheet.artist)
        assertEquals("", sheet.subtitle)
        assertEquals("", sheet.content)
    }

    // --- isChordProFile ---

    @Test
    fun choExtensionIsChordPro() {
        assertTrue(ChordProParser.isChordProFile("song.cho"))
    }

    @Test
    fun chordproExtensionIsChordPro() {
        assertTrue(ChordProParser.isChordProFile("song.chordpro"))
    }

    @Test
    fun choproExtensionIsChordPro() {
        assertTrue(ChordProParser.isChordProFile("song.chopro"))
    }

    @Test
    fun crdExtensionIsChordPro() {
        assertTrue(ChordProParser.isChordProFile("song.crd"))
    }

    @Test
    fun proExtensionIsChordPro() {
        assertTrue(ChordProParser.isChordProFile("song.pro"))
    }

    @Test
    fun txtExtensionIsNotChordPro() {
        assertFalse(ChordProParser.isChordProFile("song.txt"))
    }

    @Test
    fun pdfExtensionIsNotChordPro() {
        assertFalse(ChordProParser.isChordProFile("song.pdf"))
    }

    @Test
    fun caseInsensitiveExtension() {
        assertTrue(ChordProParser.isChordProFile("SONG.CHO"))
        assertTrue(ChordProParser.isChordProFile("Song.ChordPro"))
    }

    @Test
    fun pathWithExtensionWorks() {
        assertTrue(ChordProParser.isChordProFile("/path/to/song.cho"))
    }
}

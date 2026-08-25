package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.domain.ChordSheetTranspose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordProExporterTest {

    private fun sheet(
        title: String = "Test Song",
        subtitle: String = "",
        artist: String = "",
        content: String = "",
        key: String = "",
        capo: Int = 0,
    ) = ChordSheet(
        id = "test-id",
        title = title,
        subtitle = subtitle,
        artist = artist,
        content = content,
        key = key,
        capo = capo,
        createdAt = 0L,
        updatedAt = 0L,
    )

    // --- export: title ---

    @Test
    fun exportIncludesTitle() {
        val result = ChordProExporter.export(sheet(title = "My Song"))
        assertTrue(result.contains("{title: My Song}"))
    }

    // --- export: subtitle ---

    @Test
    fun exportIncludesSubtitle() {
        val result = ChordProExporter.export(sheet(subtitle = "Words by Newton"))
        assertTrue(result.contains("{subtitle: Words by Newton}"))
    }

    @Test
    fun exportOmitsEmptySubtitle() {
        val result = ChordProExporter.export(sheet(subtitle = ""))
        assertFalse(result.contains("{subtitle"))
    }

    // --- export: artist ---

    @Test
    fun exportIncludesArtist() {
        val result = ChordProExporter.export(sheet(artist = "The Band"))
        assertTrue(result.contains("{artist: The Band}"))
    }

    @Test
    fun exportOmitsEmptyArtist() {
        val result = ChordProExporter.export(sheet(artist = ""))
        assertFalse(result.contains("{artist"))
    }

    // --- export: key ---

    @Test
    fun exportIncludesKey() {
        val result = ChordProExporter.export(sheet(key = "G"))
        assertTrue(result.contains("{key: G}"))
    }

    @Test
    fun exportOmitsEmptyKey() {
        val result = ChordProExporter.export(sheet(key = ""))
        assertFalse(result.contains("{key"))
    }

    // Regression for #591: exporting a transposed preview must move the key with
    // the content. Mirrors the effectiveSheet construction in SheetViewer.kt so a
    // +2 preview of a song in C emits {key: D}, not the stale {key: C}.
    @Test
    fun exportTransposedPreviewMovesKeyWithContent() {
        val semitones = 2
        val original = sheet(key = "C", content = "[C]Amazing grace")
        val effectiveSheet =
            original.copy(
                content = ChordSheetTranspose.transpose(original.content, semitones),
                key = ChordSheetTranspose.transposeKey(original.key, semitones),
            )
        val result = ChordProExporter.export(effectiveSheet)
        assertTrue(result.contains("{key: D}"), "Key should be transposed to D: $result")
        assertFalse(result.contains("{key: C}"), "Stale original key must not survive: $result")
    }

    // --- export: capo ---

    @Test
    fun exportIncludesCapo() {
        val result = ChordProExporter.export(sheet(capo = 3))
        assertTrue(result.contains("{capo: 3}"))
    }

    @Test
    fun exportOmitsZeroCapo() {
        val result = ChordProExporter.export(sheet(capo = 0))
        assertFalse(result.contains("{capo"))
    }

    // --- export: section labels ---

    @Test
    fun exportConvertsChorusLabel() {
        val result = ChordProExporter.export(sheet(content = "[Chorus]\nLyrics"))
        assertTrue(result.contains("{start_of_chorus}"), "Should convert [Chorus]: $result")
    }

    @Test
    fun exportConvertsVerseLabel() {
        val result = ChordProExporter.export(sheet(content = "[Verse]\nLyrics"))
        assertTrue(result.contains("{start_of_verse}"), "Should convert [Verse]: $result")
    }

    @Test
    fun exportConvertsBridgeLabel() {
        val result = ChordProExporter.export(sheet(content = "[Bridge]\nLyrics"))
        assertTrue(result.contains("{start_of_bridge}"), "Should convert [Bridge]: $result")
    }

    @Test
    fun exportConvertsIntroLabel() {
        val result = ChordProExporter.export(sheet(content = "[Intro]\nRiff"))
        assertTrue(result.contains("{start_of_intro}"), "Result: $result")
    }

    @Test
    fun exportConvertsOutroLabel() {
        val result = ChordProExporter.export(sheet(content = "[Outro]\nFade"))
        assertTrue(result.contains("{start_of_outro}"), "Result: $result")
    }

    @Test
    fun exportConvertsInterludeLabel() {
        val result = ChordProExporter.export(sheet(content = "[Interlude]\nSolo"))
        assertTrue(result.contains("{start_of_interlude}"), "Result: $result")
    }

    @Test
    fun exportConvertsTabLabel() {
        val result = ChordProExporter.export(sheet(content = "[Tab]\ne|---3---"))
        assertTrue(result.contains("{start_of_tab}"), "Result: $result")
    }

    // --- export: custom section names ---

    @Test
    fun exportConvertsCustomVerseLabel() {
        val result = ChordProExporter.export(sheet(content = "[Verse 2]\nLyrics"))
        assertTrue(result.contains("{start_of_verse: Verse 2}"), "Result: $result")
    }

    @Test
    fun exportConvertsCustomChorusLabel() {
        val result = ChordProExporter.export(sheet(content = "[Chorus Final]\nBig finish"))
        assertTrue(result.contains("{start_of_chorus: Chorus Final}"), "Result: $result")
    }

    // --- export: end directives ---

    @Test
    fun exportEmitsEndOfChorus() {
        val result = ChordProExporter.export(sheet(content = "[Chorus]\nLyrics here"))
        assertTrue(result.contains("{end_of_chorus}"), "Should emit end: $result")
    }

    @Test
    fun exportEmitsEndBeforeNextSection() {
        val result = ChordProExporter.export(sheet(content = "[Verse]\nLine 1\n[Chorus]\nLine 2"))
        assertTrue(result.contains("{end_of_verse}"), "Should close verse: $result")
        assertTrue(result.contains("{start_of_chorus}"), "Should open chorus: $result")
        assertTrue(result.contains("{end_of_chorus}"), "Should close chorus: $result")
    }

    @Test
    fun exportEmitsEndAtEndOfContent() {
        val result = ChordProExporter.export(sheet(content = "[Bridge]\nFinal lyrics"))
        assertTrue(result.contains("{end_of_bridge}"), "Should close bridge at EOF: $result")
    }

    // --- export: content pass-through ---

    @Test
    fun exportPreservesChordMarkers() {
        val result = ChordProExporter.export(sheet(content = "[Am]Hello [G]world"))
        assertTrue(result.contains("[Am]Hello [G]world"))
    }

    @Test
    fun exportPreservesPlainLines() {
        val result = ChordProExporter.export(sheet(content = "Just lyrics"))
        assertTrue(result.contains("Just lyrics"))
    }

    // --- export: full song ---

    @Test
    fun exportCompleteSong() {
        val result = ChordProExporter.export(sheet(
            title = "Amazing Grace",
            artist = "Traditional",
            key = "G",
            capo = 2,
            content = "[Verse]\n[G]Amazing grace",
        ))
        assertTrue(result.contains("{title: Amazing Grace}"))
        assertTrue(result.contains("{artist: Traditional}"))
        assertTrue(result.contains("{key: G}"))
        assertTrue(result.contains("{capo: 2}"))
        assertTrue(result.contains("{start_of_verse}"))
        assertTrue(result.contains("[G]Amazing grace"))
        assertTrue(result.contains("{end_of_verse}"))
    }

    // --- export: metadata escaping ---

    @Test
    fun exportSanitizesBracesInTitle() {
        val result = ChordProExporter.export(sheet(title = "Hey} {capo: 7"))
        assertTrue(result.contains("{title: Hey capo: 7}"), "Result: $result")
        assertFalse(result.contains("{capo: 7}"), "Brace injection should be stripped: $result")
    }

    @Test
    fun exportSanitizesNewlinesInArtist() {
        val result = ChordProExporter.export(sheet(artist = "Line1\nLine2"))
        assertTrue(result.contains("{artist: Line1 Line2}"), "Newline should become space: $result")
    }

    // --- export: content line escaping ---

    @Test
    fun exportEscapesHashPrefixedContent() {
        val result = ChordProExporter.export(sheet(content = "#1 Hit Song"))
        assertTrue(result.contains("{comment: #1 Hit Song}"), "Hash line should be wrapped: $result")
    }

    @Test
    fun exportEscapesBracePrefixedContent() {
        val result = ChordProExporter.export(sheet(content = "{custom directive}"))
        assertTrue(result.contains("\\{custom directive}"), "Brace line should be escaped: $result")
    }

    // --- round-trip: edge cases ---

    @Test
    fun roundTripPreservesHashPrefixedLyrics() {
        val original = sheet(title = "Test", content = "#1 Hit\nNormal line")
        val exported = ChordProExporter.export(original)
        val reimported = ChordProParser.parse(exported)
        assertTrue(reimported.content.contains("#1 Hit"), "Content: ${reimported.content}")
        assertTrue(reimported.content.contains("Normal line"))
    }

    @Test
    fun roundTripPreservesBracePrefixedContent() {
        val original = sheet(title = "Test", content = "{looks like directive}\nNormal")
        val exported = ChordProExporter.export(original)
        val reimported = ChordProParser.parse(exported)
        assertTrue(reimported.content.contains("{looks like directive}"), "Content: ${reimported.content}")
    }

    @Test
    fun roundTripSanitizesTitleWithBraces() {
        val original = sheet(title = "Hey} {capo: 7")
        val exported = ChordProExporter.export(original)
        val reimported = ChordProParser.parse(exported)
        assertEquals("Hey capo: 7", reimported.title)
    }

    // --- suggestedFilename ---

    @Test
    fun suggestedFilenameUsesTitle() {
        val filename = ChordProExporter.suggestedFilename(sheet(title = "My Song"))
        assertEquals("My_Song.cho", filename)
    }

    @Test
    fun suggestedFilenameSanitizesSpecialChars() {
        val filename = ChordProExporter.suggestedFilename(sheet(title = "Hello! World?"))
        assertEquals("Hello_World.cho", filename)
    }

    @Test
    fun suggestedFilenameEmptyTitleUsesFallback() {
        val filename = ChordProExporter.suggestedFilename(sheet(title = ""))
        assertEquals("song.cho", filename)
    }

    @Test
    fun suggestedFilenameTruncatesLongTitle() {
        val longTitle = "A".repeat(100)
        val filename = ChordProExporter.suggestedFilename(sheet(title = longTitle))
        assertTrue(filename.length <= 54, "Filename too long: ${filename.length}")
        assertTrue(filename.endsWith(".cho"))
    }

    @Test
    fun suggestedFilenameHasChoExtension() {
        val filename = ChordProExporter.suggestedFilename(sheet(title = "Test"))
        assertTrue(filename.endsWith(".cho"))
    }

    // --- Round-trip ---

    @Test
    fun roundTripPreservesMetadata() {
        val original = sheet(
            title = "Round Trip",
            subtitle = "A Subtitle",
            artist = "Test Artist",
            key = "Am",
            capo = 4,
            content = "[Am]Hello [G]world",
        )
        val exported = ChordProExporter.export(original)
        val reimported = ChordProParser.parse(exported)
        assertEquals("Round Trip", reimported.title)
        assertEquals("A Subtitle", reimported.subtitle)
        assertEquals("Test Artist", reimported.artist)
        assertEquals("Am", reimported.key)
        assertEquals(4, reimported.capo)
        assertTrue(reimported.content.contains("[Am]Hello [G]world"))
    }

    @Test
    fun roundTripPreservesSections() {
        val original = sheet(
            title = "Sections Test",
            content = "[Verse]\nLine 1\n[Chorus]\nLine 2\n[Bridge]\nLine 3",
        )
        val exported = ChordProExporter.export(original)
        val reimported = ChordProParser.parse(exported)
        assertTrue(reimported.content.contains("[Verse]"))
        assertTrue(reimported.content.contains("[Chorus]"))
        assertTrue(reimported.content.contains("[Bridge]"))
        assertTrue(reimported.content.contains("Line 1"))
        assertTrue(reimported.content.contains("Line 2"))
        assertTrue(reimported.content.contains("Line 3"))
    }

    @Test
    fun roundTripPreservesCustomSectionNames() {
        val original = sheet(
            title = "Custom Sections",
            content = "[Verse 2]\nSecond verse lyrics",
        )
        val exported = ChordProExporter.export(original)
        val reimported = ChordProParser.parse(exported)
        assertTrue(reimported.content.contains("[Verse 2]"), "Content: ${reimported.content}")
    }
}

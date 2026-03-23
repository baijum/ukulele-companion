package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordProExporterTest {

    private fun sheet(
        title: String = "Test Song",
        artist: String = "",
        content: String = "",
        key: String = "",
        capo: Int = 0,
    ) = ChordSheet(
        id = "test-id",
        title = title,
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
        val result = ChordProExporter.export(sheet(content = "[Chorus]"))
        assertTrue(result.contains("{start_of_chorus}"), "Should convert [Chorus]: $result")
    }

    @Test
    fun exportConvertsVerseLabel() {
        val result = ChordProExporter.export(sheet(content = "[Verse]"))
        assertTrue(result.contains("{start_of_verse}"), "Should convert [Verse]: $result")
    }

    @Test
    fun exportConvertsBridgeLabel() {
        val result = ChordProExporter.export(sheet(content = "[Bridge]"))
        assertTrue(result.contains("{start_of_bridge}"), "Should convert [Bridge]: $result")
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
        // Base is truncated to 50 chars + .cho = 54 chars max
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
            artist = "Test Artist",
            key = "Am",
            capo = 4,
            content = "[Am]Hello [G]world",
        )
        val exported = ChordProExporter.export(original)
        val reimported = ChordProParser.parse(exported)
        assertEquals("Round Trip", reimported.title)
        assertEquals("Test Artist", reimported.artist)
        assertEquals("Am", reimported.key)
        assertEquals(4, reimported.capo)
        assertTrue(reimported.content.contains("[Am]Hello [G]world"))
    }
}

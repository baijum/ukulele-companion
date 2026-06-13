package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordParserTest {

    // --- CHORD_PATTERN: section labels excluded ---

    @Test
    fun chorusNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Chorus]")
        assertTrue(chords.isEmpty(), "Chorus should not be a chord: $chords")
    }

    @Test
    fun bridgeNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Bridge]")
        assertTrue(chords.isEmpty(), "Bridge should not be a chord: $chords")
    }

    @Test
    fun verseNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Verse]")
        assertTrue(chords.isEmpty(), "Verse should not be a chord: $chords")
    }

    @Test
    fun introNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Intro]")
        assertTrue(chords.isEmpty(), "Intro should not be a chord: $chords")
    }

    @Test
    fun outroNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Outro]")
        assertTrue(chords.isEmpty(), "Outro should not be a chord: $chords")
    }

    @Test
    fun tabNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Tab]")
        assertTrue(chords.isEmpty(), "Tab should not be a chord: $chords")
    }

    @Test
    fun interludeNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Interlude]")
        assertTrue(chords.isEmpty(), "Interlude should not be a chord: $chords")
    }

    @Test
    fun customSectionNameNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Verse 2]")
        assertTrue(chords.isEmpty(), "Verse 2 should not be a chord: $chords")
    }

    @Test
    fun codaNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Coda]")
        assertTrue(chords.isEmpty(), "Coda should not be a chord: $chords")
    }

    @Test
    fun endingNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Ending]")
        assertTrue(chords.isEmpty(), "Ending should not be a chord: $chords")
    }

    @Test
    fun breakNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Break]")
        assertTrue(chords.isEmpty(), "Break should not be a chord: $chords")
    }

    @Test
    fun fineNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Fine]")
        assertTrue(chords.isEmpty(), "Fine should not be a chord: $chords")
    }

    @Test
    fun bassLabelNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Bass]")
        assertTrue(chords.isEmpty(), "Bass should not be a chord: $chords")
    }

    @Test
    fun guitarSoloNotMatchedAsChord() {
        val chords = ChordParser.extractChords("[Guitar Solo]")
        assertTrue(chords.isEmpty(), "Guitar Solo should not be a chord: $chords")
    }

    @Test
    fun chorusMixedWithChordsOnlyExtractsChords() {
        val chords = ChordParser.extractChords("[Chorus]\n[C]Hello [G]World")
        assertEquals(listOf("C", "G"), chords)
    }

    // --- CHORD_PATTERN: real chords still matched ---

    @Test
    fun singleChordMatches() {
        val chords = ChordParser.extractChords("[C]")
        assertEquals(listOf("C"), chords)
    }

    @Test
    fun flatChordMatches() {
        val chords = ChordParser.extractChords("[Bb]")
        assertEquals(listOf("Bb"), chords)
    }

    @Test
    fun sharpChordMatches() {
        val chords = ChordParser.extractChords("[F#m]")
        assertEquals(listOf("F#m"), chords)
    }

    @Test
    fun seventhChordMatches() {
        val chords = ChordParser.extractChords("[G7]")
        assertEquals(listOf("G7"), chords)
    }

    @Test
    fun slashChordMatches() {
        val chords = ChordParser.extractChords("[C/G]")
        assertEquals(listOf("C/G"), chords)
    }

    @Test
    fun multipleDistinctChords() {
        val chords = ChordParser.extractChords("[Am]Hello [G]world [C]!")
        assertEquals(listOf("Am", "G", "C"), chords)
    }

    // --- parseLine: section labels are plain text ---

    @Test
    fun parseLineChorusIsPlainText() {
        val segments = ChordParser.parseLine("[Chorus]")
        assertEquals(1, segments.size)
        assertTrue(segments[0] is ChordParser.TextSegment.PlainText)
        assertEquals("[Chorus]", (segments[0] as ChordParser.TextSegment.PlainText).text)
    }

    @Test
    fun parseLineBridgeIsPlainText() {
        val segments = ChordParser.parseLine("[Bridge]")
        assertEquals(1, segments.size)
        assertTrue(segments[0] is ChordParser.TextSegment.PlainText)
    }

    @Test
    fun parseLineMixedSectionAndChords() {
        val segments = ChordParser.parseLine("[Chorus] [C]Hello")
        assertEquals(3, segments.size)
        assertTrue(segments[0] is ChordParser.TextSegment.PlainText)
        assertEquals("[Chorus] ", (segments[0] as ChordParser.TextSegment.PlainText).text)
        assertTrue(segments[1] is ChordParser.TextSegment.Chord)
        assertEquals("C", (segments[1] as ChordParser.TextSegment.Chord).name)
        assertTrue(segments[2] is ChordParser.TextSegment.PlainText)
        assertEquals("Hello", (segments[2] as ChordParser.TextSegment.PlainText).text)
    }
}

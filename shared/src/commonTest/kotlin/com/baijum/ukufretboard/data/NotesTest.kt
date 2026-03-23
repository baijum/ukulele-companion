package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotesTest {

    // --- Array constants ---

    @Test
    fun noteNamesSharpHas12Entries() {
        assertEquals(12, Notes.NOTE_NAMES_SHARP.size)
    }

    @Test
    fun noteNamesFlatHas12Entries() {
        assertEquals(12, Notes.NOTE_NAMES_FLAT.size)
    }

    @Test
    fun noteNamesStandardHas12Entries() {
        assertEquals(12, Notes.NOTE_NAMES_STANDARD.size)
    }

    @Test
    fun pitchClassCountIs12() {
        assertEquals(12, Notes.PITCH_CLASS_COUNT)
    }

    @Test
    fun noteNamesIsAliasForSharp() {
        assertEquals(Notes.NOTE_NAMES_SHARP, Notes.NOTE_NAMES)
    }

    // --- pitchClassToName ---

    @Test
    fun pitchClass0IsC() {
        assertEquals("C", Notes.pitchClassToName(0))
    }

    @Test
    fun pitchClass9IsA() {
        assertEquals("A", Notes.pitchClassToName(9))
    }

    @Test
    fun allPitchClassesReturnNonEmpty() {
        for (pc in 0..11) {
            assertTrue(Notes.pitchClassToName(pc).isNotEmpty(), "Pitch class $pc should have a name")
        }
    }

    @Test
    fun pitchClassToNameUsesStandardSpelling() {
        // Standard uses sharps for C# and F#, flats for Eb, Ab, Bb
        assertEquals("C#", Notes.pitchClassToName(1))
        assertEquals("Eb", Notes.pitchClassToName(3))
        assertEquals("F#", Notes.pitchClassToName(6))
        assertEquals("Ab", Notes.pitchClassToName(8))
        assertEquals("Bb", Notes.pitchClassToName(10))
    }

    // --- enharmonicForKey ---

    @Test
    fun nullKeyRootUsesStandard() {
        assertEquals("C#", Notes.enharmonicForKey(1, null))
        assertEquals("Eb", Notes.enharmonicForKey(3, null))
    }

    @Test
    fun sharpKeyUsesSharps() {
        // G major (key root 7) uses sharps
        assertEquals("C#", Notes.enharmonicForKey(1, 7))
        assertEquals("D#", Notes.enharmonicForKey(3, 7))
        assertEquals("F#", Notes.enharmonicForKey(6, 7))
    }

    @Test
    fun flatKeyUsesFlats() {
        // F major (key root 5) uses flats
        assertEquals("Db", Notes.enharmonicForKey(1, 5))
        assertEquals("Eb", Notes.enharmonicForKey(3, 5))
        assertEquals("Bb", Notes.enharmonicForKey(10, 5))
    }

    @Test
    fun cMajorUsesNeitherFlatNorSharp() {
        // C major (key root 0) — not a flat key, uses sharps
        assertEquals("C", Notes.enharmonicForKey(0, 0))
        assertEquals("C#", Notes.enharmonicForKey(1, 0))
    }

    @Test
    fun minorKeyUsesCorrectAccidentals() {
        // D minor (key root 2, isMinor=true) uses flats
        assertEquals("Bb", Notes.enharmonicForKey(10, 2, isMinor = true))
        // E minor (key root 4, isMinor=true) uses sharps
        assertEquals("F#", Notes.enharmonicForKey(6, 4, isMinor = true))
    }

    @Test
    fun naturalNotesUnchangedByKey() {
        // Natural notes (C, D, E, F, G, A, B) are the same in both spellings
        for (keyRoot in 0..11) {
            assertEquals("C", Notes.enharmonicForKey(0, keyRoot))
            assertEquals("D", Notes.enharmonicForKey(2, keyRoot))
            assertEquals("E", Notes.enharmonicForKey(4, keyRoot))
            assertEquals("F", Notes.enharmonicForKey(5, keyRoot))
            assertEquals("G", Notes.enharmonicForKey(7, keyRoot))
            assertEquals("A", Notes.enharmonicForKey(9, keyRoot))
            assertEquals("B", Notes.enharmonicForKey(11, keyRoot))
        }
    }
}

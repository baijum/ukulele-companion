package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Scale
import com.baijum.ukufretboard.data.Scales
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScaleChordBuilderTest {

    private val majorScale = Scales.ALL.first { it.name == "Major" }
    private val naturalMinor = Scales.ALL.first { it.name == "Natural Minor" }
    private val pentatonicMajor = Scales.ALL.first { it.name == "Pentatonic Major" }

    // --- Basic count ---

    @Test
    fun majorScaleProducesSevenChords() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertEquals(7, chords.size)
    }

    @Test
    fun pentatonicProducesFiveChords() {
        val chords = ScaleChordBuilder.buildTriads(0, pentatonicMajor)
        assertEquals(5, chords.size)
    }

    @Test
    fun scaleWithFewerThanFiveNotesReturnsEmpty() {
        val tinyScale = Scale("Tiny", listOf(0, 5, 7))
        val chords = ScaleChordBuilder.buildTriads(0, tinyScale)
        assertTrue(chords.isEmpty())
    }

    // --- C major qualities ---

    @Test
    fun cMajorQualities() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertEquals("Major", chords[0].quality)      // I
        assertEquals("Minor", chords[1].quality)       // ii
        assertEquals("Minor", chords[2].quality)       // iii
        assertEquals("Major", chords[3].quality)       // IV
        assertEquals("Major", chords[4].quality)       // V
        assertEquals("Minor", chords[5].quality)       // vi
        assertEquals("Diminished", chords[6].quality)  // vii°
    }

    @Test
    fun cMajorSymbols() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertEquals("", chords[0].symbol)
        assertEquals("m", chords[1].symbol)
        assertEquals("dim", chords[6].symbol)
    }

    // --- Roman numerals ---

    @Test
    fun majorNumeralsUseUpperCase() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertEquals("I", chords[0].numeral)
        assertEquals("IV", chords[3].numeral)
        assertEquals("V", chords[4].numeral)
    }

    @Test
    fun minorNumeralsUseLowerCase() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertEquals("ii", chords[1].numeral)
        assertEquals("iii", chords[2].numeral)
        assertEquals("vi", chords[5].numeral)
    }

    @Test
    fun diminishedNumeralHasDegreeSymbol() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertTrue(chords[6].numeral.contains("\u00B0"), "vii should have ° symbol")
    }

    // --- Notes contain 3 pitch class names ---

    @Test
    fun eachChordHasThreeNotes() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        for (chord in chords) {
            assertEquals(3, chord.notes.size, "Chord ${chord.numeral} should have 3 notes")
        }
    }

    @Test
    fun cMajorChordNotesAreCorrect() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        val cChord = chords[0]
        assertTrue(cChord.notes.contains("C"), "C major should contain C")
        assertTrue(cChord.notes.contains("E"), "C major should contain E")
        assertTrue(cChord.notes.contains("G"), "C major should contain G")
    }

    // --- Degree numbering ---

    @Test
    fun degreesAreOneBased() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        chords.forEachIndexed { i, chord ->
            assertEquals(i + 1, chord.degree)
        }
    }

    // --- Root pitch class ---

    @Test
    fun rootPitchClassesMatchScaleDegrees() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertEquals(0, chords[0].rootPitchClass)  // C
        assertEquals(2, chords[1].rootPitchClass)  // D
        assertEquals(4, chords[2].rootPitchClass)  // E
        assertEquals(5, chords[3].rootPitchClass)  // F
        assertEquals(7, chords[4].rootPitchClass)  // G
        assertEquals(9, chords[5].rootPitchClass)  // A
        assertEquals(11, chords[6].rootPitchClass) // B
    }

    // --- Transposition ---

    @Test
    fun gMajorTransposesCorrectly() {
        val chords = ScaleChordBuilder.buildTriads(7, majorScale)
        assertEquals(7, chords[0].rootPitchClass)  // G
        assertEquals("Major", chords[0].quality)
        assertEquals(9, chords[1].rootPitchClass)  // A
        assertEquals("Minor", chords[1].quality)
    }

    // --- Minor scale ---

    @Test
    fun aMinorQualities() {
        val chords = ScaleChordBuilder.buildTriads(9, naturalMinor)
        assertEquals("Minor", chords[0].quality)       // i
        assertEquals("Diminished", chords[1].quality)  // ii°
        assertEquals("Major", chords[2].quality)       // III
        assertEquals("Minor", chords[3].quality)       // iv
        assertEquals("Minor", chords[4].quality)       // v
        assertEquals("Major", chords[5].quality)       // VI
        assertEquals("Major", chords[6].quality)       // VII
    }

    // --- All roots ---

    @Test
    fun allRootsProduceValidChords() {
        for (root in 0..11) {
            val chords = ScaleChordBuilder.buildTriads(root, majorScale)
            assertEquals(7, chords.size)
            for (chord in chords) {
                assertTrue(chord.rootPitchClass in 0..11)
                assertTrue(chord.quality in listOf("Major", "Minor", "Diminished", "Augmented"))
            }
        }
    }

    // --- rootName matches pitchClass ---

    @Test
    fun rootNameMatchesPitchClass() {
        val chords = ScaleChordBuilder.buildTriads(0, majorScale)
        assertEquals("C", chords[0].rootName)
        assertEquals("G", chords[4].rootName)
    }
}

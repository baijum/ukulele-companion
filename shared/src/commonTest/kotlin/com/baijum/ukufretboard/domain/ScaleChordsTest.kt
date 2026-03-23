package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Scales
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScaleChordsTest {

    private val majorScale = Scales.ALL.first { it.name == "Major" }
    private val naturalMinor = Scales.ALL.first { it.name == "Natural Minor" }
    private val dorian = Scales.ALL.first { it.name == "Dorian" }
    private val pentatonicMajor = Scales.ALL.first { it.name == "Pentatonic Major" }
    private val blues = Scales.ALL.first { it.name == "Blues" }

    // --- diatonicTriads count ---

    @Test
    fun majorScaleProducesSevenTriads() {
        val triads = ScaleChords.diatonicTriads(0, majorScale)
        assertEquals(7, triads.size)
    }

    @Test
    fun naturalMinorProducesSevenTriads() {
        val triads = ScaleChords.diatonicTriads(0, naturalMinor)
        assertEquals(7, triads.size)
    }

    @Test
    fun pentatonicProducesFiveTriads() {
        val triads = ScaleChords.diatonicTriads(0, pentatonicMajor)
        assertEquals(5, triads.size)
    }

    @Test
    fun bluesProducesSixTriads() {
        val triads = ScaleChords.diatonicTriads(0, blues)
        assertEquals(6, triads.size)
    }

    // --- C major scale qualities ---

    @Test
    fun cMajorTriadQualities() {
        val triads = ScaleChords.diatonicTriads(0, majorScale)
        // I=Major, ii=Minor, iii=Minor, IV=Major, V=Major, vi=Minor, vii=Dim
        assertEquals("", triads[0].quality)     // I = C Major
        assertEquals("m", triads[1].quality)    // ii = Dm
        assertEquals("m", triads[2].quality)    // iii = Em
        assertEquals("", triads[3].quality)     // IV = F Major
        assertEquals("", triads[4].quality)     // V = G Major
        assertEquals("m", triads[5].quality)    // vi = Am
        assertEquals("dim", triads[6].quality)  // vii° = Bdim
    }

    @Test
    fun cMajorTriadRootPitchClasses() {
        val triads = ScaleChords.diatonicTriads(0, majorScale)
        assertEquals(0, triads[0].rootPitchClass)  // C
        assertEquals(2, triads[1].rootPitchClass)  // D
        assertEquals(4, triads[2].rootPitchClass)  // E
        assertEquals(5, triads[3].rootPitchClass)  // F
        assertEquals(7, triads[4].rootPitchClass)  // G
        assertEquals(9, triads[5].rootPitchClass)  // A
        assertEquals(11, triads[6].rootPitchClass) // B
    }

    // --- A natural minor qualities ---

    @Test
    fun aMinorTriadQualities() {
        val triads = ScaleChords.diatonicTriads(9, naturalMinor)
        // i=Minor, ii°=Dim, III=Major, iv=Minor, v=Minor, VI=Major, VII=Major
        assertEquals("m", triads[0].quality)    // i = Am
        assertEquals("dim", triads[1].quality)  // ii° = Bdim
        assertEquals("", triads[2].quality)     // III = C
        assertEquals("m", triads[3].quality)    // iv = Dm
        assertEquals("m", triads[4].quality)    // v = Em
        assertEquals("", triads[5].quality)     // VI = F
        assertEquals("", triads[6].quality)     // VII = G
    }

    // --- Dorian ---

    @Test
    fun dDorianTriadQualities() {
        val triads = ScaleChords.diatonicTriads(2, dorian)
        // i=Minor, ii=Minor, III=Major, IV=Major, v=Minor, vi°=Dim, VII=Major
        assertEquals("m", triads[0].quality)    // i = Dm
        assertEquals("m", triads[1].quality)    // ii = Em
        assertEquals("", triads[2].quality)     // III = F
        assertEquals("", triads[3].quality)     // IV = G
        assertEquals("m", triads[4].quality)    // v = Am
        assertEquals("dim", triads[5].quality)  // vi° = Bdim
        assertEquals("", triads[6].quality)     // VII = C
    }

    // --- Degree numbering ---

    @Test
    fun degreesAreOneBased() {
        val triads = ScaleChords.diatonicTriads(0, majorScale)
        triads.forEachIndexed { i, chord ->
            assertEquals(i + 1, chord.degree, "Degree should be ${i + 1}")
        }
    }

    // --- Quality names ---

    @Test
    fun qualityNamesAreHumanReadable() {
        val triads = ScaleChords.diatonicTriads(0, majorScale)
        assertEquals("Major", triads[0].qualityName)
        assertEquals("Minor", triads[1].qualityName)
        assertEquals("Diminished", triads[6].qualityName)
    }

    // --- Transposition ---

    @Test
    fun gMajorTriadsAreTransposedCorrectly() {
        val triads = ScaleChords.diatonicTriads(7, majorScale)
        assertEquals(7, triads[0].rootPitchClass)  // G
        assertEquals(9, triads[1].rootPitchClass)  // A
        assertEquals(11, triads[2].rootPitchClass) // B
        assertEquals(0, triads[3].rootPitchClass)  // C
        assertEquals(2, triads[4].rootPitchClass)  // D
    }

    // --- formatChord ---

    @Test
    fun formatChordMajor() {
        val chord = ScaleChords.DiatonicChord(0, "", "Major", 1)
        assertEquals("C", ScaleChords.formatChord(chord, 0))
    }

    @Test
    fun formatChordMinor() {
        val chord = ScaleChords.DiatonicChord(2, "m", "Minor", 2)
        assertEquals("Dm", ScaleChords.formatChord(chord, 0))
    }

    @Test
    fun formatChordDiminished() {
        val chord = ScaleChords.DiatonicChord(11, "dim", "Diminished", 7)
        assertEquals("Bdim", ScaleChords.formatChord(chord, 0))
    }

    // --- All 12 roots produce valid triads ---

    @Test
    fun allRootsProduceValidTriads() {
        for (root in 0..11) {
            val triads = ScaleChords.diatonicTriads(root, majorScale)
            assertEquals(7, triads.size, "Root $root should produce 7 triads")
            for (chord in triads) {
                assertTrue(chord.rootPitchClass in 0..11)
                assertTrue(chord.quality in listOf("", "m", "dim", "aug"))
            }
        }
    }
}

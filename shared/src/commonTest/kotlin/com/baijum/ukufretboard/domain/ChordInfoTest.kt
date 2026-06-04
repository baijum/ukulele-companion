package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChordInfoTest {

    private val highG = listOf(
        UkuleleString("G", 7, 4),
        UkuleleString("C", 0, 4),
        UkuleleString("E", 4, 4),
        UkuleleString("A", 9, 4),
    )

    private val lowG = listOf(
        UkuleleString("g", 7, 3),
        UkuleleString("C", 0, 4),
        UkuleleString("E", 4, 4),
        UkuleleString("A", 9, 4),
    )

    private val major = ChordFormulas.ALL.first { it.symbol == "" }
    private val minor = ChordFormulas.ALL.first { it.symbol == "m" }
    private val dom7 = ChordFormulas.ALL.first { it.symbol == "7" }
    private val dim = ChordFormulas.ALL.first { it.symbol == "dim" }
    private val aug = ChordFormulas.ALL.first { it.symbol == "aug" }

    // --- buildIntervalBreakdown() ---

    @Test
    fun majorTriadBreakdownContainsRootM3P5() {
        val root = Note(0, "C")
        val notes = listOf(Note(0, "C"), Note(4, "E"), Note(7, "G"))
        val breakdown = ChordInfo.buildIntervalBreakdown(root, notes)
        assertTrue(breakdown.contains("Root"), "Should contain Root: $breakdown")
        assertTrue(breakdown.contains("M3"), "Should contain M3: $breakdown")
        assertTrue(breakdown.contains("P5"), "Should contain P5: $breakdown")
    }

    @Test
    fun minorTriadBreakdownContainsm3() {
        val root = Note(9, "A")
        val notes = listOf(Note(9, "A"), Note(0, "C"), Note(4, "E"))
        val breakdown = ChordInfo.buildIntervalBreakdown(root, notes)
        assertTrue(breakdown.contains("Root"), "Should contain Root: $breakdown")
        assertTrue(breakdown.contains("m3"), "Should contain m3: $breakdown")
        assertTrue(breakdown.contains("P5"), "Should contain P5: $breakdown")
    }

    @Test
    fun dom7BreakdownContainsm7() {
        val root = Note(7, "G")
        val notes = listOf(Note(7, "G"), Note(11, "B"), Note(2, "D"), Note(5, "F"))
        val breakdown = ChordInfo.buildIntervalBreakdown(root, notes)
        assertTrue(breakdown.contains("m7"), "Should contain m7: $breakdown")
    }

    @Test
    fun breakdownContainsNoteNames() {
        val root = Note(0, "C")
        val notes = listOf(Note(0, "C"), Note(4, "E"), Note(7, "G"))
        val breakdown = ChordInfo.buildIntervalBreakdown(root, notes)
        assertTrue(breakdown.contains("C"), "Should contain note name C")
        assertTrue(breakdown.contains("E"), "Should contain note name E")
        assertTrue(breakdown.contains("G"), "Should contain note name G")
    }

    // --- buildFormulaString() ---

    @Test
    fun majorFormulaIs135() {
        val formula = ChordInfo.buildFormulaString(major)
        assertTrue(formula.contains("1"), "Major should contain 1: $formula")
        assertTrue(formula.contains("3"), "Major should contain 3: $formula")
        assertTrue(formula.contains("5"), "Major should contain 5: $formula")
    }

    @Test
    fun minorFormulaContainsb3() {
        val formula = ChordInfo.buildFormulaString(minor)
        assertTrue(formula.contains("b3"), "Minor should contain b3: $formula")
    }

    @Test
    fun dom7FormulaContainsb7() {
        val formula = ChordInfo.buildFormulaString(dom7)
        assertTrue(formula.contains("b7"), "Dom7 should contain b7: $formula")
    }

    @Test
    fun dimFormulaContainsb5() {
        val formula = ChordInfo.buildFormulaString(dim)
        assertTrue(formula.contains("b5"), "Dim should contain b5: $formula")
    }

    @Test
    fun augFormulaContainsSharp5() {
        val formula = ChordInfo.buildFormulaString(aug)
        assertTrue(formula.contains("#5"), "Aug should contain #5: $formula")
    }

    // --- suggestFingering() ---

    @Test
    fun allOpenStringsReturnAllZeros() {
        assertEquals(listOf(0, 0, 0, 0), ChordInfo.suggestFingering(listOf(0, 0, 0, 0)))
    }

    @Test
    fun singleFrettedStringGetsFinger1() {
        val fingering = ChordInfo.suggestFingering(listOf(0, 0, 0, 3))
        assertEquals(0, fingering[0])
        assertEquals(0, fingering[1])
        assertEquals(0, fingering[2])
        assertEquals(1, fingering[3]) // single fretted → finger 1
    }

    @Test
    fun twoDistinctFretsGetFingers1And2() {
        // F major: 2,0,1,0
        val fingering = ChordInfo.suggestFingering(listOf(2, 0, 1, 0))
        assertEquals(0, fingering[1]) // open
        assertEquals(0, fingering[3]) // open
        // Fret 1 → finger 1, fret 2 → finger 2
        assertEquals(1, fingering[2]) // fret 1
        assertEquals(2, fingering[0]) // fret 2
    }

    @Test
    fun barreChordAllSameFretGetFinger1() {
        val fingering = ChordInfo.suggestFingering(listOf(2, 2, 2, 2))
        // All on same fret → all get finger 1 (barre)
        assertTrue(fingering.all { it == 1 }, "Barre should all be finger 1: $fingering")
    }

    @Test
    fun threeDistinctFretsGetThreeFingers() {
        // G major: 0,2,3,2
        val fingering = ChordInfo.suggestFingering(listOf(0, 2, 3, 2))
        assertEquals(0, fingering[0]) // open
        // fret 2 → finger 1, fret 3 → finger 2
        assertEquals(1, fingering[1])
        assertEquals(2, fingering[2])
        assertEquals(1, fingering[3])
    }

    @Test
    fun mutedStringGetsFinger0() {
        val fingering = ChordInfo.suggestFingering(listOf(-1, 0, 1, 2))
        assertEquals(0, fingering[0]) // muted → 0
    }

    // --- formatFingering() ---

    @Test
    fun formatFingeringProducesSpacedString() {
        val result = ChordInfo.formatFingering(listOf(0, 0, 1, 1))
        assertEquals("0  0  1  1", result)
    }

    @Test
    fun formatFingeringAllZeros() {
        assertEquals("0  0  0  0", ChordInfo.formatFingering(listOf(0, 0, 0, 0)))
    }

    // --- rateDifficulty() ---

    @Test
    fun allOpenIsVeryEasy() {
        assertEquals("Very easy", ChordInfo.rateDifficulty(listOf(0, 0, 0, 0)))
    }

    @Test
    fun singleLowFretIsVeryEasy() {
        assertEquals("Very easy", ChordInfo.rateDifficulty(listOf(0, 0, 0, 3)))
    }

    @Test
    fun smallSpanLowFretsIsEasy() {
        // F major: 2,0,1,0 → span 1, max fret 2
        assertEquals("Easy", ChordInfo.rateDifficulty(listOf(2, 0, 1, 0)))
    }

    @Test
    fun barreAtLowPositionIsEasy() {
        assertEquals("Easy", ChordInfo.rateDifficulty(listOf(2, 2, 2, 2)))
    }

    @Test
    fun moderateSpanIsMedium() {
        // span 3, max fret 7
        assertEquals("Medium", ChordInfo.rateDifficulty(listOf(4, 5, 6, 7)))
    }

    @Test
    fun wideSpanOrHighFretsIsHard() {
        assertEquals("Hard", ChordInfo.rateDifficulty(listOf(5, 8, 9, 10)))
    }

    // --- determineInversion() ---

    @Test
    fun rootPositionWhenBassIsRoot() {
        // C major 0,0,0,3 — bass is C string open (pitch class 0) = root
        // In High-G, C string (index 1) is lowest: octave 4, pc 0 = pitch 48
        // G string (index 0): octave 4, pc 7 = pitch 55 (higher due to re-entrant)
        val inversion = ChordInfo.determineInversion(listOf(0, 0, 0, 3), 0, major, highG)
        assertEquals(ChordInfo.Inversion.ROOT, inversion)
    }

    @Test
    fun firstInversionWhenBassIs3rd() {
        // C/E: bass note E (pc 4) is the major 3rd of C
        // Need a voicing where E is the lowest pitch
        // In High-G: C string is lowest (pc 0 + fret 4 = pc 4 = E)
        val inversion = ChordInfo.determineInversion(listOf(0, 4, 0, 3), 0, major, highG)
        assertEquals(ChordInfo.Inversion.FIRST, inversion)
    }

    @Test
    fun secondInversionWhenBassIs5th() {
        // C/G: bass note G (pc 7) is the perfect 5th of C
        // In High-G: C string open is pc 0, but at fret 7 it's pc 7
        // Actually, let's use Low-G where G string is lowest (octave 3, pc 7)
        val inversion = ChordInfo.determineInversion(listOf(0, 0, 0, 3), 0, major, lowG)
        assertEquals(ChordInfo.Inversion.SECOND, inversion)
    }

    @Test
    fun thirdInversionFor7thChord() {
        // C7/Bb: bass = Bb (pc 10) = minor 7th of C
        // In Low-G: G string (octave 3) at fret 3 = pc (7+3)%12 = 10 = Bb
        // G3 + 3 frets = pitch 39 (lowest), C4 = 48, E4 = 52, A4 = 57
        val inversion = ChordInfo.determineInversion(listOf(3, 0, 0, 3), 0, dom7, lowG)
        assertEquals(ChordInfo.Inversion.THIRD, inversion)
    }

    // --- findBassStringIndex() ---

    @Test
    fun highGBassIsCString() {
        // In High-G (re-entrant), C4 string is lowest pitch
        // G4=55, C4=48, E4=52, A4=57 → C string (index 1) is bass
        val bassIdx = ChordInfo.findBassStringIndex(listOf(0, 0, 0, 0), highG)
        assertEquals(1, bassIdx)
    }

    @Test
    fun lowGBassIsGString() {
        // In Low-G, g3 string is lowest pitch
        // g3=43, C4=48, E4=52, A4=57 → G string (index 0) is bass
        val bassIdx = ChordInfo.findBassStringIndex(listOf(0, 0, 0, 0), lowG)
        assertEquals(0, bassIdx)
    }

    @Test
    fun highGFrettingCanChangeBass() {
        // If C string is fretted high, another string might become bass
        // G4=55, C4+7=55, E4=52, A4=57 → E string (index 2) is now bass
        val bassIdx = ChordInfo.findBassStringIndex(listOf(0, 7, 0, 0), highG)
        assertEquals(2, bassIdx)
    }

    @Test
    fun mutedStringExcludedFromBass() {
        // Mute the C string (normally bass in High-G)
        val bassIdx = ChordInfo.findBassStringIndex(listOf(0, -1, 0, 0), highG)
        // E4=52 is next lowest after C is muted (G4=55, E4=52, A4=57)
        assertEquals(2, bassIdx)
    }

    // --- bassPitchClass() ---

    @Test
    fun bassPitchClassHighGAllOpen() {
        // C string is bass in High-G, open = pc 0 (C)
        assertEquals(0, ChordInfo.bassPitchClass(listOf(0, 0, 0, 0), highG))
    }

    @Test
    fun bassPitchClassLowGAllOpen() {
        // G string is bass in Low-G, open = pc 7 (G)
        assertEquals(7, ChordInfo.bassPitchClass(listOf(0, 0, 0, 0), lowG))
    }

    @Test
    fun bassPitchClassWithFretting() {
        // C string at fret 2 = pc (0+2)%12 = 2 (D)
        assertEquals(2, ChordInfo.bassPitchClass(listOf(0, 2, 0, 0), highG))
    }

    @Test
    fun bassPitchClassNeverNegativeWithMutedStrings() {
        // findBassStringIndex filters MUTED (-1) strings, but if a non-muted
        // fret produces a negative sum the result must still be 0..11
        val frets = listOf(-1, 0, 0, 0) // G muted, C/E/A open
        val result = ChordInfo.bassPitchClass(frets, highG)
        assertTrue(result in 0..11, "bassPitchClass=$result should be in 0..11")
    }

    // --- slashNotation() ---

    @Test
    fun rootPositionReturnsPlainName() {
        assertEquals("C", ChordInfo.slashNotation("C", ChordInfo.Inversion.ROOT, 0))
    }

    @Test
    fun firstInversionReturnsSlash() {
        assertEquals("C/E", ChordInfo.slashNotation("C", ChordInfo.Inversion.FIRST, 4))
    }

    @Test
    fun secondInversionReturnsSlash() {
        assertEquals("C/G", ChordInfo.slashNotation("C", ChordInfo.Inversion.SECOND, 7))
    }

    @Test
    fun thirdInversionReturnsSlash() {
        val result = ChordInfo.slashNotation("C7", ChordInfo.Inversion.THIRD, 10)
        assertTrue(result.startsWith("C7/"), "Should have slash: $result")
    }
}

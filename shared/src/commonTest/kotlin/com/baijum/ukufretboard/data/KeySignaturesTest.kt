package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeySignaturesTest {

    // --- forKey() ---

    @Test
    fun allTwelvePitchClassesHaveKeySignatures() {
        for (pc in 0..11) {
            assertNotNull(KeySignatures.forKey(pc), "Pitch class $pc should have a key signature")
        }
    }

    @Test
    fun invalidPitchClassReturnsNull() {
        assertNull(KeySignatures.forKey(12))
        assertNull(KeySignatures.forKey(-1))
    }

    @Test
    fun cMajorHasNoAccidentals() {
        val sig = KeySignatures.forKey(0)!!
        assertEquals(0, sig.sharps)
        assertEquals(0, sig.flats)
        assertTrue(sig.accidentals.isEmpty())
    }

    @Test
    fun gMajorHasOneSharp() {
        val sig = KeySignatures.forKey(7)!!
        assertEquals(1, sig.sharps)
        assertEquals(0, sig.flats)
        assertEquals(listOf("F#"), sig.accidentals)
    }

    @Test
    fun dMajorHasTwoSharps() {
        val sig = KeySignatures.forKey(2)!!
        assertEquals(2, sig.sharps)
        assertEquals(listOf("F#", "C#"), sig.accidentals)
    }

    @Test
    fun fMajorHasOneFlat() {
        val sig = KeySignatures.forKey(5)!!
        assertEquals(0, sig.sharps)
        assertEquals(1, sig.flats)
        assertEquals(listOf("Bb"), sig.accidentals)
    }

    @Test
    fun bbMajorHasTwoFlats() {
        val sig = KeySignatures.forKey(10)!!
        assertEquals(2, sig.flats)
        assertEquals(listOf("Bb", "Eb"), sig.accidentals)
    }

    @Test
    fun ebMajorHasThreeFlats() {
        val sig = KeySignatures.forKey(3)!!
        assertEquals(3, sig.flats)
        assertEquals(listOf("Bb", "Eb", "Ab"), sig.accidentals)
    }

    // --- Relative minor ---

    @Test
    fun cMajorRelativeMinorIsA() {
        assertEquals(9, KeySignatures.forKey(0)!!.relativeMinorPitchClass)
    }

    @Test
    fun gMajorRelativeMinorIsE() {
        assertEquals(4, KeySignatures.forKey(7)!!.relativeMinorPitchClass)
    }

    @Test
    fun relativeMinorIs3SemitonesBelow() {
        for ((pc, sig) in KeySignatures.ALL) {
            val expected = (pc - 3 + 12) % 12
            assertEquals(expected, sig.relativeMinorPitchClass,
                "Relative minor of pitch class $pc should be ${expected}")
        }
    }

    // --- formatSignature() ---

    @Test
    fun formatCMajor() {
        val sig = KeySignatures.forKey(0)!!
        assertEquals("No sharps or flats", KeySignatures.formatSignature(sig))
    }

    @Test
    fun formatGMajorSingularSharp() {
        val sig = KeySignatures.forKey(7)!!
        assertEquals("1 sharp (F#)", KeySignatures.formatSignature(sig))
    }

    @Test
    fun formatDMajorPluralSharps() {
        val sig = KeySignatures.forKey(2)!!
        assertEquals("2 sharps (F#, C#)", KeySignatures.formatSignature(sig))
    }

    @Test
    fun formatFMajorSingularFlat() {
        val sig = KeySignatures.forKey(5)!!
        assertEquals("1 flat (Bb)", KeySignatures.formatSignature(sig))
    }

    @Test
    fun formatEbMajorPluralFlats() {
        val sig = KeySignatures.forKey(3)!!
        assertEquals("3 flats (Bb, Eb, Ab)", KeySignatures.formatSignature(sig))
    }

    // --- diatonicChordsForMajor() ---

    @Test
    fun cMajorDiatonicChordsAreCorrect() {
        val chords = KeySignatures.diatonicChordsForMajor(0)
        assertEquals(7, chords.size)
        assertEquals("I" to "C", chords[0])
        assertEquals("ii" to "Dm", chords[1])
        assertEquals("iii" to "Em", chords[2])
        assertEquals("IV" to "F", chords[3])
        assertEquals("V" to "G", chords[4])
        assertEquals("vi" to "Am", chords[5])
        assertEquals("vii\u00B0" to "Bdim", chords[6])
    }

    @Test
    fun gMajorDiatonicChordsAreCorrect() {
        val chords = KeySignatures.diatonicChordsForMajor(7)
        assertEquals("I" to "G", chords[0])
        assertEquals("ii" to "Am", chords[1])
        assertEquals("IV" to "C", chords[3])
        assertEquals("V" to "D", chords[4])
    }

    @Test
    fun diatonicChordsAlwaysReturnSeven() {
        for (pc in 0..11) {
            assertEquals(7, KeySignatures.diatonicChordsForMajor(pc).size,
                "Pitch class $pc should have 7 diatonic chords")
        }
    }

    // --- diatonicChordsForMinor() ---

    @Test
    fun aMinorDiatonicChordsAreCorrect() {
        val chords = KeySignatures.diatonicChordsForMinor(9)
        assertEquals(7, chords.size)
        assertEquals("i" to "Am", chords[0])
        assertEquals("ii\u00B0" to "Bdim", chords[1])
        assertEquals("III" to "C", chords[2])
        assertEquals("iv" to "Dm", chords[3])
        assertEquals("v" to "Em", chords[4])
        assertEquals("VI" to "F", chords[5])
        assertEquals("VII" to "G", chords[6])
    }

    @Test
    fun diatonicMinorChordsAlwaysReturnSeven() {
        for (pc in 0..11) {
            assertEquals(7, KeySignatures.diatonicChordsForMinor(pc).size,
                "Pitch class $pc minor should have 7 diatonic chords")
        }
    }

    // --- closelyRelatedKeys() ---

    @Test
    fun cCloselyRelatedKeysAreFAndG() {
        val (ccw, cw) = KeySignatures.closelyRelatedKeys(0)
        assertEquals(5, ccw)  // F (one flat)
        assertEquals(7, cw)   // G (one sharp)
    }

    @Test
    fun gCloselyRelatedKeysAreCAndD() {
        val (ccw, cw) = KeySignatures.closelyRelatedKeys(7)
        assertEquals(0, ccw)  // C
        assertEquals(2, cw)   // D
    }

    @Test
    fun closelyRelatedKeysWrapsAround() {
        // F (index 11 in circle) wraps to Bb (index 10) and C (index 0)
        val (ccw, cw) = KeySignatures.closelyRelatedKeys(5)
        assertEquals(10, ccw) // Bb
        assertEquals(0, cw)   // C
    }

    // --- CIRCLE_ORDER ---

    @Test
    fun circleOrderHasTwelveEntries() {
        assertEquals(12, KeySignatures.CIRCLE_ORDER.size)
    }

    @Test
    fun circleOrderContainsAllPitchClasses() {
        assertEquals((0..11).toSet(), KeySignatures.CIRCLE_ORDER.toSet())
    }

    @Test
    fun circleOrderStartsWithC() {
        assertEquals(0, KeySignatures.CIRCLE_ORDER[0])
    }

    // --- ORDER_OF_SHARPS / FLATS ---

    @Test
    fun orderOfSharpsHasSevenEntries() {
        assertEquals(7, KeySignatures.ORDER_OF_SHARPS.size)
    }

    @Test
    fun orderOfFlatsHasSevenEntries() {
        assertEquals(7, KeySignatures.ORDER_OF_FLATS.size)
    }

    @Test
    fun sharpsFollowCorrectOrder() {
        assertEquals("F#", KeySignatures.ORDER_OF_SHARPS[0])
        assertEquals("C#", KeySignatures.ORDER_OF_SHARPS[1])
    }

    @Test
    fun flatsFollowCorrectOrder() {
        assertEquals("Bb", KeySignatures.ORDER_OF_FLATS[0])
        assertEquals("Eb", KeySignatures.ORDER_OF_FLATS[1])
    }

    // --- Key signature accidentals are prefixes of ORDER_OF_SHARPS/FLATS ---

    @Test
    fun sharpKeyAccidentalsMatchOrderOfSharps() {
        for ((_, sig) in KeySignatures.ALL) {
            if (sig.sharps > 0) {
                assertEquals(
                    KeySignatures.ORDER_OF_SHARPS.take(sig.sharps),
                    sig.accidentals,
                    "Key with ${sig.sharps} sharps should use first ${sig.sharps} from ORDER_OF_SHARPS"
                )
            }
        }
    }

    @Test
    fun flatKeyAccidentalsMatchOrderOfFlats() {
        for ((_, sig) in KeySignatures.ALL) {
            if (sig.flats > 0) {
                assertEquals(
                    KeySignatures.ORDER_OF_FLATS.take(sig.flats),
                    sig.accidentals,
                    "Key with ${sig.flats} flats should use first ${sig.flats} from ORDER_OF_FLATS"
                )
            }
        }
    }
}

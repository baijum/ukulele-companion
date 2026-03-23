package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyDetectorTest {

    // --- Empty / null inputs ---

    @Test
    fun emptyListReturnsNull() {
        assertNull(KeyDetector.detectKey(emptyList()))
    }

    @Test
    fun unparsableChordNamesReturnsNull() {
        assertNull(KeyDetector.detectKey(listOf("xyz", "123", "!!!")))
    }

    // --- Major key detection ---

    @Test
    fun detectsCMajorFromIIVVI() {
        val result = KeyDetector.detectKey(listOf("C", "F", "G", "C"))
        assertNotNull(result)
        assertEquals(0, result.rootPitchClass)
        assertEquals(false, result.isMinor)
        assertTrue(result.displayName.contains("C"))
        assertTrue(result.displayName.contains("major"))
    }

    @Test
    fun detectsGMajorFromIIVVI() {
        val result = KeyDetector.detectKey(listOf("G", "C", "D", "G"))
        assertNotNull(result)
        assertEquals(7, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }

    @Test
    fun detectsDMajorFromIVviIV() {
        val result = KeyDetector.detectKey(listOf("D", "A", "Bm", "G"))
        assertNotNull(result)
        assertEquals(2, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }

    @Test
    fun detectsFMajorFromIiiIVV() {
        val result = KeyDetector.detectKey(listOf("F", "Gm", "Bb", "C"))
        assertNotNull(result)
        assertEquals(5, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }

    // --- Minor key detection ---

    @Test
    fun detectsAMinorFromIivVi() {
        val result = KeyDetector.detectKey(listOf("Am", "Dm", "Em", "Am"))
        assertNotNull(result)
        assertEquals(9, result.rootPitchClass)
        assertEquals(true, result.isMinor)
        assertTrue(result.displayName.contains("minor"))
    }

    @Test
    fun detectsEMinorFromIiiIVV() {
        val result = KeyDetector.detectKey(listOf("Em", "Am", "Bm", "Em"))
        assertNotNull(result)
        assertEquals(4, result.rootPitchClass)
        assertEquals(true, result.isMinor)
    }

    // --- Pop progression (I-V-vi-IV) ---

    @Test
    fun detectsCMajorFromPopProgression() {
        val result = KeyDetector.detectKey(listOf("C", "G", "Am", "F"))
        assertNotNull(result)
        assertEquals(0, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }

    // --- Jazz ii-V-I ---

    @Test
    fun detectsGMajorFromJazzIiVI() {
        val result = KeyDetector.detectKey(listOf("Am7", "D7", "Gmaj7"))
        assertNotNull(result)
        assertEquals(7, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }

    // --- Extended chord qualities ---

    @Test
    fun handlesSeventhChords() {
        val result = KeyDetector.detectKey(listOf("Cmaj7", "Dm7", "G7", "Cmaj7"))
        assertNotNull(result)
        assertEquals(0, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }

    @Test
    fun handlesSusChords() {
        // sus chords normalized to base quality
        val result = KeyDetector.detectKey(listOf("C", "Fsus4", "G", "C"))
        assertNotNull(result)
        assertEquals(0, result.rootPitchClass)
    }

    // --- Flat key chord names ---

    @Test
    fun handlesFlatsInChordNames() {
        val result = KeyDetector.detectKey(listOf("Bb", "Eb", "F", "Bb"))
        assertNotNull(result)
        assertEquals(10, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }

    @Test
    fun handlesSharpChordNames() {
        val result = KeyDetector.detectKey(listOf("F#m", "Bm", "C#m", "F#m"))
        assertNotNull(result)
        assertEquals(6, result.rootPitchClass)
        assertEquals(true, result.isMinor)
    }

    // --- Single chord ---

    @Test
    fun singleChordReturnsResult() {
        val result = KeyDetector.detectKey(listOf("C"))
        assertNotNull(result)
        // Single major chord → likely major key of that root
        assertEquals(0, result.rootPitchClass)
    }

    @Test
    fun singleMinorChordDetectsMinor() {
        val result = KeyDetector.detectKey(listOf("Am"))
        assertNotNull(result)
        assertEquals(9, result.rootPitchClass)
        assertEquals(true, result.isMinor)
    }

    // --- Confidence ---

    @Test
    fun confidenceIsPositiveForValidChords() {
        val result = KeyDetector.detectKey(listOf("C", "F", "G"))
        assertNotNull(result)
        assertTrue(result.confidence > 0f)
    }

    @Test
    fun strongProgressionHasHigherConfidence() {
        // 6 diatonic chords with tonic first+last → high score per chord
        // vs 2 chords where F# is non-diatonic in C → low score per chord
        val strongResult = KeyDetector.detectKey(listOf("C", "F", "G", "Am", "Dm", "C"))
        val weakResult = KeyDetector.detectKey(listOf("C", "F#"))
        assertNotNull(strongResult)
        assertNotNull(weakResult)
        assertTrue(
            strongResult.confidence >= weakResult.confidence,
            "Diatonic progression (${strongResult.confidence}) should score >= non-diatonic (${weakResult.confidence})"
        )
    }

    // --- All 12 keys produce results ---

    @Test
    fun allTwelveKeysDetectable() {
        val roots = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
        for (root in roots) {
            val result = KeyDetector.detectKey(listOf(root))
            assertNotNull(result, "Should detect key for chord $root")
        }
    }

    // --- Mixed sharp/flat ---

    @Test
    fun mixedSharpAndFlatChordNames() {
        // Mix sharps and flats — Db and C# are the same pitch class (1)
        val result = KeyDetector.detectKey(listOf("Db", "Gb", "Ab", "Db"))
        assertNotNull(result)
        assertEquals(1, result.rootPitchClass) // Db major

        val result2 = KeyDetector.detectKey(listOf("C#", "F#", "G#", "C#"))
        assertNotNull(result2)
        assertEquals(1, result2.rootPitchClass) // Same key via sharps
    }

    // --- Display name format ---

    @Test
    fun displayNameIncludesMajorOrMinor() {
        val major = KeyDetector.detectKey(listOf("C", "F", "G", "C"))
        assertNotNull(major)
        assertTrue(major.displayName.contains("major"))

        val minor = KeyDetector.detectKey(listOf("Am", "Dm", "Em", "Am"))
        assertNotNull(minor)
        assertTrue(minor.displayName.contains("minor"))
    }

    // --- Diminished / augmented chords ---

    @Test
    fun handlesDiminishedChords() {
        val result = KeyDetector.detectKey(listOf("C", "Dm", "Em", "F", "G", "Am", "Bdim"))
        assertNotNull(result)
        assertEquals(0, result.rootPitchClass)
        assertEquals(false, result.isMinor)
    }
}

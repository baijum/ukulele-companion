package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.Progressions
import com.baijum.ukufretboard.data.ScaleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoiceLeadingTest {

    private val highG = listOf(
        UkuleleString("G", 7, 4),
        UkuleleString("C", 0, 4),
        UkuleleString("E", 4, 4),
        UkuleleString("A", 9, 4),
    )

    // --- distance() ---

    @Test
    fun distanceIdenticalVoicingsIsZero() {
        val v = voicing(0, 0, 0, 3) // C major
        assertEquals(0, VoiceLeading.distance(v, v))
    }

    @Test
    fun distanceSymmetric() {
        val v1 = voicing(0, 0, 0, 3)
        val v2 = voicing(2, 0, 1, 0)
        assertEquals(VoiceLeading.distance(v1, v2), VoiceLeading.distance(v2, v1))
    }

    @Test
    fun distanceCalculatesCorrectly() {
        val v1 = voicing(0, 0, 0, 3) // C: frets 0,0,0,3
        val v2 = voicing(2, 0, 1, 0) // F: frets 2,0,1,0
        // |0-2| + |0-0| + |0-1| + |3-0| = 2 + 0 + 1 + 3 = 6
        assertEquals(6, VoiceLeading.distance(v1, v2))
    }

    @Test
    fun distanceAllOpenToAllFretted() {
        val v1 = voicing(0, 0, 0, 0)
        val v2 = voicing(2, 2, 2, 2)
        assertEquals(8, VoiceLeading.distance(v1, v2))
    }

    @Test
    fun distanceSingleStringChange() {
        val v1 = voicing(0, 0, 0, 0)
        val v2 = voicing(0, 0, 0, 3)
        assertEquals(3, VoiceLeading.distance(v1, v2))
    }

    // --- computeTransition() ---

    @Test
    fun transitionIdenticalVoicingsAllCommonTones() {
        val v = voicing(0, 0, 0, 3)
        val transition = VoiceLeading.computeTransition(v, v)
        assertEquals(setOf(0, 1, 2, 3), transition.commonToneIndices)
        assertEquals(0, transition.totalDistance)
        assertEquals(listOf(0, 0, 0, 0), transition.movements)
    }

    @Test
    fun transitionDetectsCommonTones() {
        val v1 = voicing(0, 0, 0, 3) // C
        val v2 = voicing(0, 0, 0, 0) // Am-ish open
        val transition = VoiceLeading.computeTransition(v1, v2)
        // Strings 0, 1, 2 stay the same
        assertTrue(transition.commonToneIndices.containsAll(setOf(0, 1, 2)))
        assertEquals(3, transition.totalDistance)
    }

    @Test
    fun transitionMovementsAreSignedCorrectly() {
        val v1 = voicing(0, 0, 0, 3)
        val v2 = voicing(2, 0, 1, 0)
        val transition = VoiceLeading.computeTransition(v1, v2)
        // movements: 2-0=2, 0-0=0, 1-0=1, 0-3=-3
        assertEquals(listOf(2, 0, 1, -3), transition.movements)
    }

    @Test
    fun transitionTotalDistanceMatchesDistance() {
        val v1 = voicing(0, 0, 0, 3)
        val v2 = voicing(2, 0, 1, 0)
        val transition = VoiceLeading.computeTransition(v1, v2)
        assertEquals(VoiceLeading.distance(v1, v2), transition.totalDistance)
    }

    // --- computeOptimalPath() ---

    @Test
    fun emptyProgressionReturnsNull() {
        val prog = Progression("empty", "empty", emptyList())
        assertNull(VoiceLeading.computeOptimalPath(prog, 0, highG))
    }

    @Test
    fun singleChordProgressionReturnsPath() {
        val prog = Progression(
            "single", "one chord",
            listOf(ChordDegree(0, "", "I")),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals(1, path.steps.size)
        assertEquals(0, path.transitions.size)
        assertEquals(0, path.totalDistance)
    }

    @Test
    fun twoChordProgressionReturnsPathWithOneTransition() {
        val prog = Progression(
            "I-IV", "two chords",
            listOf(
                ChordDegree(0, "", "I"),  // C major
                ChordDegree(5, "", "IV"), // F major
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals(2, path.steps.size)
        assertEquals(1, path.transitions.size)
        assertTrue(path.totalDistance >= 0)
    }

    @Test
    fun pathStepsMatchProgressionSize() {
        val prog = Progression(
            "I-IV-V-I", "four chords",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
                ChordDegree(0, "", "I"),
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals(4, path.steps.size)
        assertEquals(3, path.transitions.size)
    }

    @Test
    fun pathTransitionCountIsStepsMinusOne() {
        val prog = Progression(
            "I-V-vi-IV", "pop",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(7, "", "V"),
                ChordDegree(9, "m", "vi"),
                ChordDegree(5, "", "IV"),
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals(path.steps.size - 1, path.transitions.size)
    }

    @Test
    fun pathTotalDistanceEqualsTransitionSum() {
        val prog = Progression(
            "I-IV-V", "three chords",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals(path.transitions.sumOf { it.totalDistance }, path.totalDistance)
    }

    @Test
    fun pathVoicingsAreAllPlayable() {
        val prog = Progression(
            "I-IV-V-I", "test",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
                ChordDegree(0, "", "I"),
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        for (step in path.steps) {
            val fretted = step.voicing.frets.filter { it > 0 }
            if (fretted.isNotEmpty()) {
                assertTrue(fretted.max() - fretted.min() <= 4, "Fret span too wide: ${step.voicing.frets}")
            }
            for (fret in step.voicing.frets) {
                assertTrue(fret in 0..12, "Fret out of range: $fret")
            }
        }
    }

    @Test
    fun invalidChordQualityReturnsNull() {
        val prog = Progression(
            "bad", "bad quality",
            listOf(ChordDegree(0, "nonexistent_quality", "I")),
        )
        assertNull(VoiceLeading.computeOptimalPath(prog, 0, highG))
    }

    @Test
    fun allTwelveKeysProducePaths() {
        val prog = Progression(
            "I-IV-V", "test",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
            ),
        )
        for (key in 0..11) {
            val path = VoiceLeading.computeOptimalPath(prog, key, highG)
            assertNotNull(path, "Key $key should produce a path for I-IV-V")
        }
    }

    @Test
    fun pathPreservesProgressionName() {
        val prog = Progression(
            "My Progression", "description",
            listOf(ChordDegree(0, "", "I"), ChordDegree(5, "", "IV")),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals("My Progression", path.progressionName)
    }

    @Test
    fun pathPreservesKeyRoot() {
        val prog = Progression(
            "test", "test",
            listOf(ChordDegree(0, "", "I"), ChordDegree(7, "", "V")),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 7, highG)
        assertNotNull(path)
        assertEquals(7, path.keyRoot)
    }

    @Test
    fun stepNumeralsMatchDegrees() {
        val prog = Progression(
            "test", "test",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(9, "m", "vi"),
                ChordDegree(5, "", "IV"),
                ChordDegree(7, "", "V"),
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals("I", path.steps[0].numeral)
        assertEquals("vi", path.steps[1].numeral)
        assertEquals("IV", path.steps[2].numeral)
        assertEquals("V", path.steps[3].numeral)
    }

    @Test
    fun minorProgressionWorks() {
        val prog = Progression(
            "i-iv-v", "minor",
            listOf(
                ChordDegree(0, "m", "i"),
                ChordDegree(5, "m", "iv"),
                ChordDegree(7, "m", "v"),
            ),
            scaleType = ScaleType.MINOR,
        )
        val path = VoiceLeading.computeOptimalPath(prog, 9, highG) // A minor
        assertNotNull(path)
        assertEquals(3, path.steps.size)
    }

    @Test
    fun seventhChordProgressionWorks() {
        val prog = Progression(
            "ii-V-I", "jazz",
            listOf(
                ChordDegree(2, "m7", "ii"),
                ChordDegree(7, "7", "V"),
                ChordDegree(0, "maj7", "I"),
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals(3, path.steps.size)
    }

    @Test
    fun longProgressionCompletes() {
        // 8-chord progression should still complete quickly
        val prog = Progression(
            "long", "long progression",
            listOf(
                ChordDegree(0, "", "I"),
                ChordDegree(5, "", "IV"),
                ChordDegree(0, "", "I"),
                ChordDegree(7, "", "V"),
                ChordDegree(9, "m", "vi"),
                ChordDegree(5, "", "IV"),
                ChordDegree(0, "", "I"),
                ChordDegree(7, "", "V"),
            ),
        )
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path)
        assertEquals(8, path.steps.size)
        assertEquals(7, path.transitions.size)
    }

    @Test
    fun realProgressionFromLibraryWorks() {
        // Use the first major progression from the library
        val prog = Progressions.MAJOR_PROGRESSIONS.first()
        val path = VoiceLeading.computeOptimalPath(prog, 0, highG)
        assertNotNull(path, "First major progression should produce a path in key of C")
    }

    // Helper to create a simple voicing from 4 frets
    private fun voicing(g: Int, c: Int, e: Int, a: Int): ChordVoicing {
        val frets = listOf(g, c, e, a)
        val fretted = frets.filter { it > 0 }
        return ChordVoicing(
            frets = frets,
            notes = frets.mapIndexed { i, fret ->
                val pc = (highG[i].openPitchClass + fret) % 12
                Note(pitchClass = pc, name = "")
            },
            minFret = fretted.minOrNull() ?: 0,
            maxFret = fretted.maxOrNull() ?: 0,
        )
    }
}

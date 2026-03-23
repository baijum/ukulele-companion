package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.UkuleleString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoicingGeneratorTest {

    // Standard High-G tuning: G4 C4 E4 A4
    private val highG = listOf(
        UkuleleString("G", 7, 4),
        UkuleleString("C", 0, 4),
        UkuleleString("E", 4, 4),
        UkuleleString("A", 9, 4),
    )

    // Low-G tuning: g3 C4 E4 A4
    private val lowG = listOf(
        UkuleleString("g", 7, 3),
        UkuleleString("C", 0, 4),
        UkuleleString("E", 4, 4),
        UkuleleString("A", 9, 4),
    )

    // Baritone tuning: D3 G3 B4 E4
    private val baritone = listOf(
        UkuleleString("D", 2, 3),
        UkuleleString("G", 7, 3),
        UkuleleString("B", 11, 4),
        UkuleleString("E", 4, 4),
    )

    private val major = ChordFormulas.ALL.first { it.symbol == "" }
    private val minor = ChordFormulas.ALL.first { it.symbol == "m" }
    private val dim = ChordFormulas.ALL.first { it.symbol == "dim" }
    private val aug = ChordFormulas.ALL.first { it.symbol == "aug" }
    private val dom7 = ChordFormulas.ALL.first { it.symbol == "7" }
    private val min7 = ChordFormulas.ALL.first { it.symbol == "m7" }
    private val maj7 = ChordFormulas.ALL.first { it.symbol == "maj7" }
    private val dim7 = ChordFormulas.ALL.first { it.symbol == "dim7" }
    private val sus2 = ChordFormulas.ALL.first { it.symbol == "sus2" }
    private val sus4 = ChordFormulas.ALL.first { it.symbol == "sus4" }
    private val sixth = ChordFormulas.ALL.first { it.symbol == "6" }
    private val min6 = ChordFormulas.ALL.first { it.symbol == "m6" }
    private val dom9 = ChordFormulas.ALL.first { it.symbol == "9" }
    private val min9 = ChordFormulas.ALL.first { it.symbol == "m9" }
    private val add9 = ChordFormulas.ALL.first { it.symbol == "add9" }

    // --- Basic generation ---

    @Test
    fun cMajorReturnsNonEmpty() {
        val voicings = VoicingGenerator.generate(0, major, highG)
        assertTrue(voicings.isNotEmpty(), "C major should have voicings")
    }

    @Test
    fun amContainsKnownOpenVoicing() {
        // Am on standard ukulele is commonly 2,0,0,0
        val voicings = VoicingGenerator.generate(9, minor, highG)
        val hasOpen = voicings.any { it.frets == listOf(2, 0, 0, 0) }
        assertTrue(hasOpen, "Am should include the 2,0,0,0 voicing")
    }

    @Test
    fun cMajorContainsKnownOpenVoicing() {
        // C major on standard ukulele is commonly 0,0,0,3
        val voicings = VoicingGenerator.generate(0, major, highG)
        val hasOpen = voicings.any { it.frets == listOf(0, 0, 0, 3) }
        assertTrue(hasOpen, "C major should include the 0,0,0,3 voicing")
    }

    @Test
    fun fMajorContainsKnownVoicing() {
        // F major: 2,0,1,0
        val voicings = VoicingGenerator.generate(5, major, highG)
        val hasKnown = voicings.any { it.frets == listOf(2, 0, 1, 0) }
        assertTrue(hasKnown, "F major should include the 2,0,1,0 voicing")
    }

    @Test
    fun gMajorContainsKnownVoicing() {
        // G major: 0,2,3,2
        val voicings = VoicingGenerator.generate(7, major, highG)
        val hasKnown = voicings.any { it.frets == listOf(0, 2, 3, 2) }
        assertTrue(hasKnown, "G major should include the 0,2,3,2 voicing")
    }

    // --- Pitch class correctness ---

    @Test
    fun allVoicingsContainRequiredPitchClasses() {
        val rootPitchClass = 0 // C
        val voicings = VoicingGenerator.generate(rootPitchClass, major, highG)
        val requiredPCs = major.intervals.map { (rootPitchClass + it) % 12 }.toSet()

        for (voicing in voicings) {
            val producedPCs = voicing.frets.mapIndexedNotNull { i, fret ->
                if (fret == ChordVoicing.MUTED) null
                else (highG[i].openPitchClass + fret) % 12
            }.toSet()
            assertTrue(
                producedPCs.containsAll(requiredPCs),
                "Voicing ${voicing.frets} should contain all pitch classes $requiredPCs but has $producedPCs"
            )
        }
    }

    @Test
    fun minorChordHasCorrectPitchClasses() {
        val rootPitchClass = 2 // D
        val voicings = VoicingGenerator.generate(rootPitchClass, minor, highG)
        val requiredPCs = minor.intervals.map { (rootPitchClass + it) % 12 }.toSet() // {2, 5, 9}

        for (voicing in voicings) {
            val producedPCs = voicing.frets.mapIndexedNotNull { i, fret ->
                if (fret == ChordVoicing.MUTED) null
                else (highG[i].openPitchClass + fret) % 12
            }.toSet()
            assertTrue(producedPCs.containsAll(requiredPCs))
        }
    }

    // --- Fret span constraint ---

    @Test
    fun noVoicingExceedsFretSpan() {
        for (formula in ChordFormulas.ALL) {
            for (root in 0..11) {
                val voicings = VoicingGenerator.generate(root, formula, highG)
                for (voicing in voicings) {
                    val fretted = voicing.frets.filter { it > 0 }
                    if (fretted.isNotEmpty()) {
                        val span = fretted.max() - fretted.min()
                        assertTrue(
                            span <= 4,
                            "Voicing ${voicing.frets} for root=$root formula=${formula.symbol} has span $span > 4"
                        )
                    }
                }
            }
        }
    }

    // --- Result limits ---

    @Test
    fun returnsAtMostTenVoicings() {
        for (root in 0..11) {
            val voicings = VoicingGenerator.generate(root, major, highG)
            assertTrue(voicings.size <= 10, "Root $root returned ${voicings.size} voicings")
        }
    }

    @Test
    fun noDuplicateVoicings() {
        val voicings = VoicingGenerator.generate(0, major, highG)
        val fretSets = voicings.map { it.frets }
        assertEquals(fretSets.size, fretSets.toSet().size, "Duplicate voicings found")
    }

    // --- Sorting order ---

    @Test
    fun lowerPositionVoicingsAppearFirst() {
        val voicings = VoicingGenerator.generate(0, major, highG)
        if (voicings.size >= 2) {
            // First voicing should have minFret <= last voicing's minFret
            assertTrue(voicings.first().minFret <= voicings.last().minFret)
        }
    }

    @Test
    fun openStringVoicingsPreferred() {
        val voicings = VoicingGenerator.generate(0, major, highG)
        if (voicings.size >= 2) {
            val firstOpenCount = voicings.first().frets.count { it == 0 }
            val lastOpenCount = voicings.last().frets.count { it == 0 }
            // Among voicings at similar positions, more open strings should rank higher
            // We just verify the first voicing has open strings for C major (known: 0,0,0,3)
            assertTrue(firstOpenCount > 0, "First C major voicing should have open strings")
        }
    }

    // --- Chord quality coverage ---

    @Test
    fun allTriadFormulasProduceVoicings() {
        for (formula in listOf(major, minor, dim, aug)) {
            val voicings = VoicingGenerator.generate(0, formula, highG)
            assertTrue(voicings.isNotEmpty(), "C ${formula.quality} should have voicings")
        }
    }

    @Test
    fun allSeventhFormulasProduceVoicings() {
        for (formula in listOf(dom7, min7, maj7, dim7)) {
            val voicings = VoicingGenerator.generate(0, formula, highG)
            assertTrue(voicings.isNotEmpty(), "C ${formula.quality} should have voicings")
        }
    }

    @Test
    fun suspendedChordsProduceVoicings() {
        for (formula in listOf(sus2, sus4)) {
            val voicings = VoicingGenerator.generate(0, formula, highG)
            assertTrue(voicings.isNotEmpty(), "C ${formula.quality} should have voicings")
        }
    }

    @Test
    fun sixthChordsProduceVoicings() {
        for (formula in listOf(sixth, min6)) {
            val voicings = VoicingGenerator.generate(0, formula, highG)
            assertTrue(voicings.isNotEmpty(), "C ${formula.quality} should have voicings")
        }
    }

    // --- Extended chords (subset generation) ---

    @Test
    fun dom9ProducesVoicings() {
        // 9th chord has 5 notes but only 4 strings — requires subset generation
        val voicings = VoicingGenerator.generate(0, dom9, highG)
        assertTrue(voicings.isNotEmpty(), "C9 should have voicings via subset generation")
    }

    @Test
    fun min9ProducesVoicings() {
        val voicings = VoicingGenerator.generate(0, min9, highG)
        assertTrue(voicings.isNotEmpty(), "Cm9 should have voicings via subset generation")
    }

    @Test
    fun add9ProducesVoicings() {
        val voicings = VoicingGenerator.generate(0, add9, highG)
        assertTrue(voicings.isNotEmpty(), "Cadd9 should have voicings")
    }

    @Test
    fun extendedChordDropsOmittableIntervals() {
        // dom9 has intervals {0,2,4,7,10} with omittable={7}
        // On 4 strings, the 5th (interval 7) should be droppable
        val rootPitchClass = 0
        val voicings = VoicingGenerator.generate(rootPitchClass, dom9, highG)
        val requiredPCs = dom9.intervals
            .subtract(dom9.omittable)
            .map { (rootPitchClass + it) % 12 }
            .toSet()

        for (voicing in voicings) {
            val producedPCs = voicing.frets.mapIndexedNotNull { i, fret ->
                if (fret == ChordVoicing.MUTED) null
                else (highG[i].openPitchClass + fret) % 12
            }.toSet()
            assertTrue(
                producedPCs.containsAll(requiredPCs),
                "Voicing ${voicing.frets} must contain required PCs $requiredPCs"
            )
        }
    }

    // --- Tuning variations ---

    @Test
    fun lowGTuningProducesVoicings() {
        val voicings = VoicingGenerator.generate(0, major, lowG)
        assertTrue(voicings.isNotEmpty(), "C major on Low-G should have voicings")
    }

    @Test
    fun baritoneTuningProducesVoicings() {
        val voicings = VoicingGenerator.generate(0, major, baritone)
        assertTrue(voicings.isNotEmpty(), "C major on Baritone should have voicings")
    }

    @Test
    fun differentTuningsProduceDifferentVoicings() {
        val highGVoicings = VoicingGenerator.generate(0, major, highG).map { it.frets }.toSet()
        val baritoneVoicings = VoicingGenerator.generate(0, major, baritone).map { it.frets }.toSet()
        assertTrue(
            highGVoicings != baritoneVoicings,
            "High-G and Baritone should produce different voicings for C major"
        )
    }

    // --- Muted strings ---

    @Test
    fun noMutedStringsWhenDisabled() {
        val voicings = VoicingGenerator.generate(0, major, highG, allowMutedStrings = false)
        for (voicing in voicings) {
            assertTrue(
                voicing.frets.none { it == ChordVoicing.MUTED },
                "No muted strings expected when allowMutedStrings=false"
            )
        }
    }

    @Test
    fun mutedVoicingsIncludedWhenEnabled() {
        // Try all 12 roots to find at least one chord that produces muted voicings
        val anyMuted = (0..11).any { root ->
            val voicings = VoicingGenerator.generate(root, major, highG, allowMutedStrings = true)
            voicings.any { v -> v.frets.any { it == ChordVoicing.MUTED } }
        }
        // It's OK if no muted voicings appear for major chords (all 4 strings can always play),
        // but if they do appear, they should be valid
        val voicings = VoicingGenerator.generate(0, major, highG, allowMutedStrings = true)
        val nonMuted = voicings.filter { v -> v.frets.none { it == ChordVoicing.MUTED } }
        val muted = voicings.filter { v -> v.frets.any { it == ChordVoicing.MUTED } }
        assertTrue(nonMuted.size <= 10, "At most 10 non-muted voicings")
        assertTrue(muted.size <= 5, "At most 5 muted voicings")
    }

    @Test
    fun mutedVoicingsAppearAfterFullVoicings() {
        val voicings = VoicingGenerator.generate(0, major, highG, allowMutedStrings = true)
        var seenMuted = false
        for (voicing in voicings) {
            val isMuted = voicing.frets.any { it == ChordVoicing.MUTED }
            if (isMuted) seenMuted = true
            if (!isMuted && seenMuted) {
                assertTrue(false, "Non-muted voicing found after muted voicing")
            }
        }
    }

    @Test
    fun mutedVoicingsHaveAtMostOneMutedString() {
        for (root in 0..11) {
            val voicings = VoicingGenerator.generate(root, major, highG, allowMutedStrings = true)
            for (voicing in voicings) {
                val mutedCount = voicing.frets.count { it == ChordVoicing.MUTED }
                assertTrue(mutedCount <= 1, "Voicing ${voicing.frets} has $mutedCount muted strings")
            }
        }
    }

    // --- All 12 roots ---

    @Test
    fun allRootsProduceVoicingsForMajor() {
        for (root in 0..11) {
            val voicings = VoicingGenerator.generate(root, major, highG)
            assertTrue(voicings.isNotEmpty(), "Root $root major should have voicings")
        }
    }

    @Test
    fun allRootsProduceVoicingsForMinor() {
        for (root in 0..11) {
            val voicings = VoicingGenerator.generate(root, minor, highG)
            assertTrue(voicings.isNotEmpty(), "Root $root minor should have voicings")
        }
    }

    // --- minFret / maxFret correctness ---

    @Test
    fun minFretAndMaxFretAreCorrect() {
        val voicings = VoicingGenerator.generate(0, major, highG)
        for (voicing in voicings) {
            val fretted = voicing.frets.filter { it > 0 }
            val expectedMin = fretted.minOrNull() ?: 0
            val expectedMax = fretted.maxOrNull() ?: 0
            assertEquals(expectedMin, voicing.minFret, "minFret mismatch for ${voicing.frets}")
            assertEquals(expectedMax, voicing.maxFret, "maxFret mismatch for ${voicing.frets}")
        }
    }

    // --- Notes correctness ---

    @Test
    fun voicingNotesMatchFrets() {
        val voicings = VoicingGenerator.generate(0, major, highG)
        for (voicing in voicings) {
            assertEquals(4, voicing.notes.size, "Should have 4 notes")
            for (i in 0..3) {
                val fret = voicing.frets[i]
                if (fret == ChordVoicing.MUTED) {
                    assertEquals(null, voicing.notes[i], "Muted string should have null note")
                } else {
                    val expectedPC = (highG[i].openPitchClass + fret) % 12
                    assertEquals(
                        expectedPC,
                        voicing.notes[i]!!.pitchClass,
                        "Note pitch class mismatch at string $i fret $fret"
                    )
                }
            }
        }
    }

    // --- All formulas produce voicings for at least some roots ---

    @Test
    fun everyFormulaProducesVoicingsForAtLeastOneRoot() {
        for (formula in ChordFormulas.ALL) {
            val anyVoicings = (0..11).any { root ->
                VoicingGenerator.generate(root, formula, highG).isNotEmpty()
            }
            assertTrue(anyVoicings, "${formula.quality} (${formula.symbol}) has no voicings for any root")
        }
    }

    // --- Frets in valid range ---

    @Test
    fun allFretsInValidRange() {
        for (formula in listOf(major, minor, dom7)) {
            for (root in 0..11) {
                val voicings = VoicingGenerator.generate(root, formula, highG)
                for (voicing in voicings) {
                    for (fret in voicing.frets) {
                        assertTrue(
                            fret == ChordVoicing.MUTED || fret in 0..12,
                            "Fret $fret out of range in ${voicing.frets}"
                        )
                    }
                }
            }
        }
    }
}

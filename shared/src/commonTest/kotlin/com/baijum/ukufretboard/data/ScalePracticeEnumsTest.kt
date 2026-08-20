package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [PracticeMode], [PlayDirection] and [FretPosition].
 *
 * `ScalePracticeSettings` persists `lastMode`, `lastDirection` and
 * `lastFretPosition` as **ordinals**, read back through `entries.getOrElse`.
 * Reordering any of these enums therefore silently changes what a user's saved
 * preference means, which is why the ordinal assertions below exist.
 */
class ScalePracticeEnumsTest {
    // ── FretPosition.range ───────────────────────────────────────────

    @Test
    fun allReturnsNoRangeMeaningNoFilter() {
        assertNull(FretPosition.ALL.range(), "ALL must not restrict the fretboard")
        assertNull(FretPosition.ALL.range(22), "ALL ignores lastFret")
    }

    @Test
    fun openCoversTheNutThroughFretFour() {
        assertEquals(0..4, FretPosition.OPEN.range())
    }

    @Test
    fun midCoversFourThroughEight() {
        assertEquals(4..8, FretPosition.MID.range())
    }

    @Test
    fun highStartsAtSevenAndRunsToTheLastFret() {
        assertEquals(7..12, FretPosition.HIGH.range(12))
        assertEquals(7..22, FretPosition.HIGH.range(22))
    }

    @Test
    fun onlyHighRespondsToTheLastFretArgument() {
        assertEquals(FretPosition.OPEN.range(12), FretPosition.OPEN.range(22))
        assertEquals(FretPosition.MID.range(12), FretPosition.MID.range(22))
        assertTrue(
            FretPosition.HIGH.range(12) != FretPosition.HIGH.range(22),
            "HIGH must extend with the fretboard",
        )
    }

    @Test
    fun positionsDeliberatelyOverlapAtFretsFourSevenAndEight() {
        // The overlap is intentional: a scale shape near a boundary should be
        // reachable from either position rather than being cut in half.
        assertTrue(4 in FretPosition.OPEN.range()!! && 4 in FretPosition.MID.range()!!)
        assertTrue(7 in FretPosition.MID.range()!! && 7 in FretPosition.HIGH.range(12)!!)
        assertTrue(8 in FretPosition.MID.range()!! && 8 in FretPosition.HIGH.range(12)!!)
    }

    @Test
    fun everyFretUpToTheLastIsCoveredByAtLeastOnePosition() {
        for (lastFret in FretboardSettings.MIN_LAST_FRET..FretboardSettings.MAX_LAST_FRET) {
            val covered =
                buildSet {
                    addAll(FretPosition.OPEN.range(lastFret)!!)
                    addAll(FretPosition.MID.range(lastFret)!!)
                    addAll(FretPosition.HIGH.range(lastFret)!!)
                }
            assertEquals(
                (0..lastFret).toSet(),
                covered,
                "positions leave a gap when lastFret is $lastFret",
            )
        }
    }

    // ── Ordinals: the on-disk contract ───────────────────────────────

    @Test
    fun practiceModeOrdinalsAreTheOnDiskContract() {
        assertEquals(
            listOf("PLAY_ALONG", "QUIZ", "EAR_TRAINING"),
            PracticeMode.entries.map { it.name },
            "ScalePracticeSettings.lastMode stores this ordinal",
        )
    }

    @Test
    fun playDirectionOrdinalsAreTheOnDiskContract() {
        assertEquals(
            listOf("ASCENDING", "DESCENDING", "BOTH"),
            PlayDirection.entries.map { it.name },
            "ScalePracticeSettings.lastDirection stores this ordinal",
        )
    }

    @Test
    fun fretPositionOrdinalsAreTheOnDiskContract() {
        assertEquals(
            listOf("ALL", "OPEN", "MID", "HIGH"),
            FretPosition.entries.map { it.name },
            "ScalePracticeSettings.lastFretPosition stores this ordinal",
        )
    }

    @Test
    fun theDefaultSettingsOrdinalsResolveToTheFirstEntryOfEachEnum() {
        val defaults = ScalePracticeSettings()
        assertEquals(PracticeMode.PLAY_ALONG, PracticeMode.entries[defaults.lastMode])
        assertEquals(PlayDirection.ASCENDING, PlayDirection.entries[defaults.lastDirection])
        assertEquals(FretPosition.ALL, FretPosition.entries[defaults.lastFretPosition])
    }

    // ── Labels ───────────────────────────────────────────────────────

    @Test
    fun labelsAreUniqueAndNonBlankWithinEachEnum() {
        for (labels in listOf(
            PracticeMode.entries.map { it.label },
            PlayDirection.entries.map { it.label },
            FretPosition.entries.map { it.label },
        )) {
            assertEquals(labels.size, labels.toSet().size, "duplicate labels: $labels")
            for (label in labels) {
                assertTrue(label.isNotBlank(), "blank label in $labels")
            }
        }
    }
}

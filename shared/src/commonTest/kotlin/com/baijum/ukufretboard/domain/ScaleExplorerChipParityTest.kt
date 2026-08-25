package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Scales
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression guard for issue #605.
 *
 * Both Scale Explorer screens now build their chord chips from the single
 * shared [ScaleChordBuilder]:
 * - Android `ScaleSelector` renders each chip as `rootName + symbol`.
 * - iOS `ScaleSelectorView` renders each chip as `rootName + symbol`.
 *
 * Previously Android routed through a separate `ScaleChords.diatonicTriads`
 * implementation whose fallback guessed minor/augmented qualities, so it
 * showed "Fm"/"Caug" where iOS showed "F"/"C" for the same non-tertian scale
 * degree. This test pins the chip row produced by the shared path so both
 * platforms stay in agreement.
 */
class ScaleExplorerChipParityTest {
    /** Mirrors exactly how both platforms render a chip label. */
    private fun chipRow(
        root: Int,
        scaleName: String,
    ): List<String> {
        val scale = Scales.ALL.first { it.name == scaleName }
        return ScaleChordBuilder.buildTriads(root, scale).map { it.rootName + it.symbol }
    }

    @Test
    fun cBluesChipRowIsIdenticalAcrossPlatforms() {
        // C Blues = [0, 3, 5, 6, 7, 10]; degree 3 (F) is a non-tertian (2,7) pair.
        assertEquals(
            listOf("C", "Ebm", "F", "F#", "G", "Bb"),
            chipRow(0, "Blues"),
        )
    }

    @Test
    fun cPentatonicMajorChipRowIsIdenticalAcrossPlatforms() {
        // C Pentatonic Major = [0, 2, 4, 7, 9]; every degree is non-tertian.
        assertEquals(
            listOf("C", "D", "E", "G", "A"),
            chipRow(0, "Pentatonic Major"),
        )
    }
}

package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Scale

/**
 * Derives diatonic chords from any scale type by stacking thirds
 * from each scale degree.
 *
 * For 7-note scales, this produces 7 triads (major, minor, diminished, or augmented).
 * For pentatonic scales (5 notes), 5 triads are produced.
 * For the blues scale (6 notes), 6 triads are produced.
 */
object ScaleChords {

    /**
     * A diatonic chord derived from a scale degree.
     *
     * @property rootPitchClass The pitch class of the chord root (0–11).
     * @property quality The chord quality symbol ("", "m", "dim", "aug").
     * @property qualityName Human-readable quality ("Major", "Minor", etc.).
     * @property degree The 1-based scale degree number.
     */
    data class DiatonicChord(
        val rootPitchClass: Int,
        val quality: String,
        val qualityName: String,
        val degree: Int,
    )

    /**
     * Derives diatonic triads for the given scale and root.
     *
     * For each scale degree, builds a triad by stacking two thirds
     * (every other note in the scale). The quality is determined by
     * the interval sizes:
     * - Major third (3–4 semitones) + minor third = Major
     * - Minor third + major third = Minor
     * - Minor third + minor third = Diminished
     * - Major third + major third = Augmented
     *
     * @param root The root pitch class (0–11).
     * @param scale The scale definition.
     * @return List of [DiatonicChord] for each scale degree.
     */
    fun diatonicTriads(root: Int, scale: Scale): List<DiatonicChord> {
        val intervals = scale.intervals
        val n = intervals.size

        return intervals.mapIndexed { degreeIndex, rootInterval ->
            val chordRoot = (root + rootInterval) % Notes.PITCH_CLASS_COUNT

            // Stack thirds: get the 3rd and 5th by jumping every other scale degree
            val thirdIndex = (degreeIndex + 2) % n
            val fifthIndex = (degreeIndex + 4) % n

            val thirdInterval = intervals[thirdIndex]
            val fifthInterval = intervals[fifthIndex]

            // Calculate semitone distances from chord root
            val thirdSemitones = ((thirdInterval - rootInterval) + 12) % 12
            val fifthSemitones = ((fifthInterval - rootInterval) + 12) % 12

            val (quality, qualityName) = classifyTriad(thirdSemitones, fifthSemitones)

            DiatonicChord(
                rootPitchClass = chordRoot,
                quality = quality,
                qualityName = qualityName,
                degree = degreeIndex + 1,
            )
        }
    }

    /**
     * Classifies a triad based on the intervals from root to third and fifth.
     *
     * @param thirdSemitones Semitones from root to third.
     * @param fifthSemitones Semitones from root to fifth.
     * @return Pair of (quality symbol, quality name).
     */
    private fun classifyTriad(thirdSemitones: Int, fifthSemitones: Int): Pair<String, String> {
        return when {
            thirdSemitones == 4 && fifthSemitones == 7 -> "" to "Major"
            thirdSemitones == 3 && fifthSemitones == 7 -> "m" to "Minor"
            thirdSemitones == 3 && fifthSemitones == 6 -> "dim" to "Diminished"
            thirdSemitones == 4 && fifthSemitones == 8 -> "aug" to "Augmented"
            // Fallback for unusual interval combinations
            thirdSemitones <= 3 && fifthSemitones <= 6 -> "dim" to "Diminished"
            thirdSemitones >= 4 && fifthSemitones >= 8 -> "aug" to "Augmented"
            thirdSemitones <= 3 -> "m" to "Minor"
            else -> "" to "Major"
        }
    }

    /**
     * Formats a diatonic chord for display using standard note names.
     *
     * @param chord The diatonic chord.
     * @param scaleRoot The root pitch class of the scale, for key-aware spelling.
     * @return Display string like "Cm", "Ddim", "G".
     */
    fun formatChord(chord: DiatonicChord, scaleRoot: Int): String {
        return Notes.enharmonicForKey(chord.rootPitchClass, scaleRoot) + chord.quality
    }
}

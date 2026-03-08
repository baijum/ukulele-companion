package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Scale

/**
 * A chord built from a scale degree.
 *
 * @property degree 1-based scale degree (1–7).
 * @property numeral Roman numeral notation (e.g., "I", "ii", "iii").
 * @property rootPitchClass Pitch class of the chord root.
 * @property rootName Display name of the chord root.
 * @property quality Chord quality ("Major", "Minor", "Diminished", "Augmented").
 * @property symbol Chord symbol suffix ("", "m", "dim", "aug").
 * @property notes Display names of all notes in the chord.
 */
data class ScaleChord(
    val degree: Int,
    val numeral: String,
    val rootPitchClass: Int,
    val rootName: String,
    val quality: String,
    val symbol: String,
    val notes: List<String>,
)

/**
 * Builds diatonic triads from any scale.
 *
 * For each scale degree, stacks 3rds using only notes from the scale
 * to determine the triad quality (Major, Minor, Diminished, Augmented).
 */
object ScaleChordBuilder {

    /** Roman numerals for major quality. */
    private val UPPER_NUMERALS = listOf("I", "II", "III", "IV", "V", "VI", "VII")

    /** Roman numerals for minor quality. */
    private val LOWER_NUMERALS = listOf("i", "ii", "iii", "iv", "v", "vi", "vii")

    /**
     * Builds diatonic triads for a scale.
     *
     * @param rootPitchClass Root pitch class (0–11).
     * @param scale The scale definition.
     * @return List of [ScaleChord] for each degree, or empty if scale has fewer than 5 notes.
     */
    fun buildTriads(rootPitchClass: Int, scale: Scale): List<ScaleChord> {
        val intervals = scale.intervals
        if (intervals.size < 5) return emptyList() // Can't build triads reliably

        val scaleNotes = intervals.map { (rootPitchClass + it) % 12 }

        return scaleNotes.mapIndexed { index, notePc ->
            // Stack 3rds: root, 3rd (2 scale degrees up), 5th (4 scale degrees up)
            val thirdPc = scaleNotes[(index + 2) % scaleNotes.size]
            val fifthPc = scaleNotes[(index + 4) % scaleNotes.size]

            val thirdInterval = (thirdPc - notePc + 12) % 12
            val fifthInterval = (fifthPc - notePc + 12) % 12

            val (quality, symbol) = determineTriadQuality(thirdInterval, fifthInterval)
            val numeral = when (quality) {
                "Major" -> UPPER_NUMERALS.getOrElse(index) { "${index + 1}" }
                "Minor" -> LOWER_NUMERALS.getOrElse(index) { "${index + 1}" }
                "Diminished" -> "${LOWER_NUMERALS.getOrElse(index) { "${index + 1}" }}\u00B0"
                "Augmented" -> "${UPPER_NUMERALS.getOrElse(index) { "${index + 1}" }}+"
                else -> "${index + 1}"
            }

            val noteNames = listOf(
                Notes.pitchClassToName(notePc),
                Notes.pitchClassToName(thirdPc),
                Notes.pitchClassToName(fifthPc),
            )

            ScaleChord(
                degree = index + 1,
                numeral = numeral,
                rootPitchClass = notePc,
                rootName = Notes.pitchClassToName(notePc),
                quality = quality,
                symbol = symbol,
                notes = noteNames,
            )
        }
    }

    private fun determineTriadQuality(thirdInterval: Int, fifthInterval: Int): Pair<String, String> =
        when {
            thirdInterval == 4 && fifthInterval == 7 -> "Major" to ""
            thirdInterval == 3 && fifthInterval == 7 -> "Minor" to "m"
            thirdInterval == 3 && fifthInterval == 6 -> "Diminished" to "dim"
            thirdInterval == 4 && fifthInterval == 8 -> "Augmented" to "aug"
            else -> "Major" to "" // Default fallback
        }
}

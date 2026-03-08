package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes

/**
 * Generates interval identification exercises for ear and visual training.
 *
 * Difficulty levels progressively introduce more intervals:
 * - Level 1: P4, P5, Octave (easily distinguishable)
 * - Level 2: + Major 3rd, Minor 3rd
 * - Level 3: + Major 2nd, Minor 2nd, Minor 7th, Major 7th
 * - Level 4: All 12 intervals
 */
object IntervalTrainer {

    /** Direction for audio playback of an interval. */
    enum class IntervalDirection {
        /** Second note is higher than the first. */
        ASCENDING,
        /** Second note is lower than the first. */
        DESCENDING,
        /** Both notes are played simultaneously. */
        HARMONIC,
    }

    /** Full interval names for all 12 semitones. */
    val INTERVAL_NAMES = listOf(
        "Unison", "Minor 2nd", "Major 2nd", "Minor 3rd", "Major 3rd",
        "Perfect 4th", "Tritone", "Perfect 5th", "Minor 6th", "Major 6th",
        "Minor 7th", "Major 7th",
    )

    /** Short interval labels. */
    val INTERVAL_SHORT = listOf(
        "P1", "m2", "M2", "m3", "M3", "P4", "TT", "P5", "m6", "M6", "m7", "M7",
    )

    /** Intervals available at each difficulty level. */
    private val LEVEL_INTERVALS = mapOf(
        1 to listOf(5, 7, 12),               // P4, P5, Octave
        2 to listOf(3, 4, 5, 7, 12),         // + m3, M3
        3 to listOf(1, 2, 3, 4, 5, 7, 10, 11), // + m2, M2, m7, M7
        4 to (1..11).toList(),                // All intervals
    )

    /**
     * An interval training question.
     *
     * @property note1PitchClass First note pitch class.
     * @property note2PitchClass Second note pitch class.
     * @property note1Name First note display name.
     * @property note2Name Second note display name.
     * @property intervalSemitones Correct interval in semitones.
     * @property correctAnswer The correct interval name.
     * @property options Four multiple-choice answer options.
     * @property correctIndex Index of correct answer in options.
     * @property note1Fret Fret position on a string (for visual mode).
     * @property note2Fret Fret position on a string (for visual mode).
     */
    data class IntervalQuestion(
        val note1PitchClass: Int,
        val note2PitchClass: Int,
        val note1Name: String,
        val note2Name: String,
        val intervalSemitones: Int,
        val correctAnswer: String,
        val options: List<String>,
        val correctIndex: Int,
        val note1Fret: Int,
        val note2Fret: Int,
        /** Octave of the first note for audio playback. */
        val note1Octave: Int = 4,
        /** Octave of the second note for audio playback. */
        val note2Octave: Int = 4,
        /** Direction for audio playback. */
        val direction: IntervalDirection = IntervalDirection.ASCENDING,
    )

    /**
     * Generates a random interval question at the given difficulty level.
     *
     * @param level Difficulty level (1–4).
     * @param direction The playback direction for audio mode (unused in visual mode).
     * @return An [IntervalQuestion] with 4 options.
     */
    fun generateQuestion(
        level: Int,
        direction: IntervalDirection = IntervalDirection.ASCENDING,
    ): IntervalQuestion {
        val allowedIntervals = LEVEL_INTERVALS[level.coerceIn(1, 4)]!!
        val interval = allowedIntervals.random()
        val effectiveInterval = if (interval == 12) 0 else interval // Octave maps to 0 semitones

        val note1 = (0..11).random()
        val note2 = (note1 + interval) % 12

        val note1Name = Notes.pitchClassToName(note1)
        val note2Name = Notes.pitchClassToName(note2)

        val correctAnswer = if (interval == 12) "Octave" else INTERVAL_NAMES[interval]

        // Generate wrong options from other allowed intervals
        val wrongIntervals = allowedIntervals.filter { it != interval }.shuffled().take(3)
        val wrongNames = wrongIntervals.map {
            if (it == 12) "Octave" else INTERVAL_NAMES[it]
        }
        val options = (wrongNames + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        // Calculate fret positions for visual mode (using C string = pitch class 0)
        val note1Fret = note1 // fret on the C string
        val note2Fret = (note1 + interval).coerceAtMost(12)

        // Calculate octaves for audio playback
        val baseOctave = 4
        val note1Octave: Int
        val note2Octave: Int
        when (direction) {
            IntervalDirection.ASCENDING, IntervalDirection.HARMONIC -> {
                note1Octave = baseOctave
                // If the interval wraps past pitch class 11 to 0+, it's in the next octave
                note2Octave = if (note1 + interval >= 12) baseOctave + 1 else baseOctave
            }
            IntervalDirection.DESCENDING -> {
                note1Octave = baseOctave + 1
                // Descending: second note is lower
                note2Octave = if (note1 + interval >= 12) baseOctave + 1 else baseOctave
                // Actually for descending we want note1 high, note2 low
                // note2 = note1 - interval
            }
        }

        return IntervalQuestion(
            note1PitchClass = note1,
            note2PitchClass = note2,
            note1Name = note1Name,
            note2Name = note2Name,
            intervalSemitones = interval,
            correctAnswer = correctAnswer,
            options = options,
            correctIndex = correctIndex,
            note1Fret = note1Fret,
            note2Fret = note2Fret,
            note1Octave = note1Octave,
            note2Octave = note2Octave,
            direction = direction,
        )
    }
}

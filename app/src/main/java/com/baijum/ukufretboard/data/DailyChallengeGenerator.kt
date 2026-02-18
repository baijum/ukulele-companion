package com.baijum.ukufretboard.data

import java.util.Calendar
import kotlin.random.Random

/**
 * Generates date-deterministic daily challenges.
 *
 * Each day produces 3 challenges of different types that adapt to the
 * user's progress. The same challenges are generated for the same day
 * using a date-seeded random number generator.
 */
object DailyChallengeGenerator {

    /**
     * Types of daily challenges.
     */
    enum class ChallengeType(val label: String) {
        LEARN_CHORD("Learn a New Chord"),
        THEORY_QUIZ("Theory Quiz"),
        PRACTICE_SONG("Practice a Song"),
        SCALE_PRACTICE("Scale Practice"),
        CHORD_TRANSITION("Chord Transition"),
        EAR_TRAINING("Ear Training"),
    }

    /**
     * A daily challenge definition.
     *
     * @property type The type of challenge.
     * @property title Display title.
     * @property description What the user should do.
     * @property targetCount How many repetitions/correct answers to achieve.
     * @property navTarget The navigation target index (if applicable).
     */
    data class DailyChallenge(
        val type: ChallengeType,
        val title: String,
        val description: String,
        val targetCount: Int = 1,
        val navTarget: Int? = null,
    )

    /**
     * Generates the daily challenges for the given date.
     *
     * Always returns 3 challenges of different types, seeded by the date.
     *
     * @param year The year.
     * @param dayOfYear The day of year (1–366).
     * @return A list of 3 [DailyChallenge] instances.
     */
    fun generateForDate(
        year: Int = Calendar.getInstance().get(Calendar.YEAR),
        dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR),
    ): List<DailyChallenge> {
        val seed = (year * 1000L + dayOfYear)
        val random = Random(seed)

        val types = ChallengeType.entries.toMutableList()

        return (0 until 3).map {
            val typeIndex = random.nextInt(types.size)
            val type = types.removeAt(typeIndex)
            generateChallenge(type, random)
        }
    }

    /**
     * Returns the same challenges as [generateForDate] for today.
     */
    fun today(): List<DailyChallenge> = generateForDate()

    private fun generateChallenge(
        type: ChallengeType,
        random: Random,
    ): DailyChallenge = when (type) {
        ChallengeType.LEARN_CHORD -> {
            val roots = Notes.NOTE_NAMES_STANDARD
            val qualities = listOf("", "m", "7", "m7", "maj7")
            val root = roots[random.nextInt(roots.size)]
            val quality = qualities[random.nextInt(qualities.size)]
            val chordName = "$root${if (quality.isEmpty()) "" else quality}"
            DailyChallenge(
                type = type,
                title = "Learn $chordName",
                description = "Find and practice the $chordName chord. Try all voicings!",
                navTarget = 1, // NAV_LIBRARY
            )
        }
        ChallengeType.THEORY_QUIZ -> {
            val count = listOf(5, 10, 15)[random.nextInt(3)]
            DailyChallenge(
                type = type,
                title = "Theory Quiz: $count Questions",
                description = "Answer $count theory quiz questions correctly.",
                targetCount = count,
                navTarget = 8, // NAV_THEORY_QUIZ
            )
        }
        ChallengeType.PRACTICE_SONG -> {
            DailyChallenge(
                type = type,
                title = "Practice a Song",
                description = "Open the songbook and practice any song for 5 minutes.",
                navTarget = 5, // NAV_SONGBOOK
            )
        }
        ChallengeType.SCALE_PRACTICE -> {
            val scales = listOf("Major", "Minor", "Pentatonic Major", "Pentatonic Minor", "Blues")
            val scale = scales[random.nextInt(scales.size)]
            DailyChallenge(
                type = type,
                title = "Scale: $scale",
                description = "Practice the $scale scale in at least 2 different keys.",
                navTarget = 22, // NAV_SCALE_PRACTICE
            )
        }
        ChallengeType.CHORD_TRANSITION -> {
            val roots = Notes.NOTE_NAMES_STANDARD
            val root1 = roots[random.nextInt(roots.size)]
            val root2 = roots[random.nextInt(roots.size)]
            val quality = if (random.nextBoolean()) "" else "m"
            DailyChallenge(
                type = type,
                title = "Switch: $root1$quality to $root2",
                description = "Practice switching between $root1$quality and $root2 at 60 BPM. Aim for 20 switches per minute.",
                targetCount = 20,
                navTarget = 25, // NAV_CHORD_TRANSITION
            )
        }
        ChallengeType.EAR_TRAINING -> {
            val count = listOf(5, 10)[random.nextInt(2)]
            DailyChallenge(
                type = type,
                title = "Ear Training: $count Rounds",
                description = "Complete $count rounds of ear training with at least 60% accuracy.",
                targetCount = count,
                navTarget = 19, // NAV_CHORD_EAR
            )
        }
    }
}

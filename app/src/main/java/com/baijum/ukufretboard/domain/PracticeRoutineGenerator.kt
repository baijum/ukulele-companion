package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import kotlin.random.Random

/**
 * Generates personalised practice routines based on the user's skill level
 * and learning progress.
 *
 * A routine consists of sequential [PracticeStep]s of different types
 * (warm-up, chord drill, scale practice, ear training, etc.) that together
 * form a structured practice session.
 */
object PracticeRoutineGenerator {

    /**
     * Generates a practice routine.
     *
     * @param durationMinutes Target duration in minutes.
     * @param skillLevel The user's estimated skill level.
     * @param focusAreas Optional areas to emphasise.
     * @return A [PracticeRoutine] with sequenced steps.
     */
    fun generate(
        durationMinutes: Int = 15,
        skillLevel: SkillLevel = SkillLevel.BEGINNER,
        focusAreas: Set<FocusArea> = FocusArea.entries.toSet(),
    ): PracticeRoutine {
        val steps = mutableListOf<PracticeStep>()
        val random = Random(System.currentTimeMillis())

        // Always start with warm-up
        steps.add(generateWarmUp(skillLevel, random))

        // Allocate remaining time to focused exercises
        val warmUpTime = steps.first().durationMinutes
        val remainingTime = (durationMinutes - warmUpTime).coerceAtLeast(5)
        val exerciseCount = (remainingTime / 3).coerceIn(2, 5)
        val timePerExercise = remainingTime / exerciseCount

        val availableTypes = focusAreas.flatMap { it.stepTypes }.distinct().toMutableList()

        repeat(exerciseCount) {
            if (availableTypes.isEmpty()) return@repeat
            val typeIndex = random.nextInt(availableTypes.size)
            val type = availableTypes.removeAt(typeIndex)
            steps.add(generateStep(type, timePerExercise, skillLevel, random))
        }

        // End with cool-down / free play
        steps.add(
            PracticeStep(
                type = StepType.FREE_PLAY,
                title = "Cool Down",
                description = "Play something you enjoy! Practice a favourite song or strum freely.",
                durationMinutes = 2,
                navTarget = null,
            ),
        )

        return PracticeRoutine(
            title = "${durationMinutes}-Minute ${skillLevel.label} Practice",
            steps = steps,
            totalMinutes = steps.sumOf { it.durationMinutes },
        )
    }

    private fun generateWarmUp(level: SkillLevel, random: Random): PracticeStep {
        val warmups = when (level) {
            SkillLevel.BEGINNER -> listOf(
                "Slowly strum each open string (G-C-E-A) and listen for clear tones.",
                "Practice placing and lifting your fingers on the C chord shape.",
                "Strum C and Am chords slowly, 4 beats each, for 2 minutes.",
            )
            SkillLevel.INTERMEDIATE -> listOf(
                "Play a chromatic scale on each string from fret 1 to 5 and back.",
                "Cycle through C-Am-F-G at 80 BPM, 4 beats per chord.",
                "Practice barre chord shapes slowly up the neck.",
            )
            SkillLevel.ADVANCED -> listOf(
                "Warm up with the major scale in 3 different positions.",
                "Play chord inversions up the neck for any chord.",
                "Fingerpick through an arpeggio pattern at a comfortable tempo.",
            )
        }
        return PracticeStep(
            type = StepType.WARM_UP,
            title = "Warm Up",
            description = warmups[random.nextInt(warmups.size)],
            durationMinutes = 2,
            navTarget = null,
        )
    }

    private fun generateStep(
        type: StepType,
        minutes: Int,
        level: SkillLevel,
        random: Random,
    ): PracticeStep = when (type) {
        StepType.CHORD_DRILL -> {
            val chords = when (level) {
                SkillLevel.BEGINNER -> listOf("C", "Am", "F", "G", "Em", "Dm")
                SkillLevel.INTERMEDIATE -> listOf("Bm", "F#m", "Bb", "Eb", "A7", "Dm7")
                SkillLevel.ADVANCED -> listOf("Cmaj7", "Am7", "Dm9", "G13", "Bbmaj7")
            }
            val chord1 = chords[random.nextInt(chords.size)]
            val chord2 = chords[random.nextInt(chords.size)]
            PracticeStep(
                type = type,
                title = "Chord Transition: $chord1 ↔ $chord2",
                description = "Switch between $chord1 and $chord2 with a metronome. Start at 60 BPM and increase by 10 each minute.",
                durationMinutes = minutes,
                navTarget = 25, // NAV_CHORD_TRANSITION
            )
        }
        StepType.SCALE_PRACTICE -> {
            val scales = when (level) {
                SkillLevel.BEGINNER -> listOf("C Major", "A Minor", "G Major")
                SkillLevel.INTERMEDIATE -> listOf("D Major", "E Minor Pentatonic", "Blues Scale")
                SkillLevel.ADVANCED -> listOf("Mixolydian", "Dorian", "Melodic Minor")
            }
            val scale = scales[random.nextInt(scales.size)]
            PracticeStep(
                type = type,
                title = "Scale: $scale",
                description = "Play the $scale scale ascending and descending. Try in different positions on the fretboard.",
                durationMinutes = minutes,
                navTarget = 22, // NAV_SCALE_PRACTICE
            )
        }
        StepType.EAR_TRAINING -> {
            PracticeStep(
                type = type,
                title = "Ear Training",
                description = "Identify intervals and chord types by ear. Focus on getting at least 70% correct.",
                durationMinutes = minutes,
                navTarget = 19, // NAV_CHORD_EAR
            )
        }
        StepType.THEORY_QUIZ -> {
            PracticeStep(
                type = type,
                title = "Theory Check",
                description = "Answer theory questions to reinforce your knowledge. Aim for a 5-question streak!",
                durationMinutes = minutes,
                navTarget = 8, // NAV_THEORY_QUIZ
            )
        }
        StepType.SONG_PRACTICE -> {
            PracticeStep(
                type = type,
                title = "Song Practice",
                description = "Pick a song from your songbook and play through it. Focus on smooth chord changes.",
                durationMinutes = minutes,
                navTarget = 5, // NAV_SONGBOOK
            )
        }

        StepType.WARM_UP, StepType.FREE_PLAY -> {
            // Already handled separately
            PracticeStep(
                type = type,
                title = "Free Play",
                description = "Play whatever you like!",
                durationMinutes = minutes,
                navTarget = null,
            )
        }
    }

    /**
     * Practice step types.
     */
    enum class StepType(val label: String) {
        WARM_UP("Warm Up"),
        CHORD_DRILL("Chord Drill"),
        SCALE_PRACTICE("Scale Practice"),
        EAR_TRAINING("Ear Training"),
        THEORY_QUIZ("Theory Quiz"),
        SONG_PRACTICE("Song Practice"),

        FREE_PLAY("Free Play"),
    }

    /**
     * Focus areas that map to step types.
     */
    enum class FocusArea(val label: String, val stepTypes: List<StepType>) {
        CHORDS("Chords", listOf(StepType.CHORD_DRILL)),
        SCALES("Scales", listOf(StepType.SCALE_PRACTICE)),
        EAR("Ear Training", listOf(StepType.EAR_TRAINING)),
        THEORY("Theory", listOf(StepType.THEORY_QUIZ)),
        SONGS("Songs", listOf(StepType.SONG_PRACTICE)),
    }

    /**
     * User skill levels.
     */
    enum class SkillLevel(val label: String) {
        BEGINNER("Beginner"),
        INTERMEDIATE("Intermediate"),
        ADVANCED("Advanced"),
    }
}

/**
 * A complete practice routine with sequenced steps.
 */
data class PracticeRoutine(
    val title: String,
    val steps: List<PracticeStep>,
    val totalMinutes: Int,
)

/**
 * A single step within a practice routine.
 */
data class PracticeStep(
    val type: PracticeRoutineGenerator.StepType,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val navTarget: Int?,
)

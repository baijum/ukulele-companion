package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes

/**
 * Generates chord ear training exercises.
 *
 * A chord is played and the user must identify its quality (Major, Minor, etc.).
 *
 * Difficulty levels control which chord qualities are available:
 * - Level 1 (Easy): Major, Minor
 * - Level 2 (Medium): + Diminished, Augmented
 * - Level 3 (Hard): + Dominant 7th, Major 7th, Minor 7th
 * - Level 4 (Expert): All qualities from ChordFormulas.ALL
 */
object ChordEarTrainer {

    /**
     * A chord ear training question.
     *
     * @property rootPitchClass Pitch class of the root note (0–11).
     * @property rootName Display name of the root note.
     * @property quality The chord quality name (e.g., "Major", "Minor").
     * @property symbol The chord symbol suffix (e.g., "", "m", "7").
     * @property notes Notes for audio playback as (pitchClass, octave) pairs.
     * @property correctAnswer The correct quality name.
     * @property options Answer options.
     * @property correctIndex Index of the correct answer in [options].
     */
    data class ChordEarQuestion(
        val rootPitchClass: Int,
        val rootName: String,
        val quality: String,
        val symbol: String,
        val notes: List<Pair<Int, Int>>,
        val correctAnswer: String,
        val options: List<String>,
        val correctIndex: Int,
    )

    /** Quality names available per level. */
    private val LEVEL_QUALITIES: Map<Int, List<String>> = mapOf(
        1 to listOf("Major", "Minor"),
        2 to listOf("Major", "Minor", "Diminished", "Augmented"),
        3 to listOf("Major", "Minor", "Diminished", "Augmented",
            "Dominant 7th", "Major 7th", "Minor 7th"),
        4 to ChordFormulas.ALL.map { it.quality }.distinct(),
    )

    /**
     * Generates a chord ear training question at the given difficulty.
     *
     * @param level Difficulty level 1–4.
     */
    fun generateQuestion(level: Int = 1): ChordEarQuestion {
        val lvl = level.coerceIn(1, 4)
        val qualities = LEVEL_QUALITIES[lvl]!!

        // Pick a random quality
        val targetQuality = qualities.random()

        // Find the matching ChordFormula
        val formula = ChordFormulas.ALL.first { it.quality == targetQuality }

        // Pick a random root note (0–11)
        val rootPc = (0..11).random()
        val rootName = Notes.pitchClassToName(rootPc)

        // Build notes for playback: apply each interval to the root at octave 4
        val baseOctave = 4
        val notes = formula.intervals
            .filter { it !in formula.omittable || formula.intervals.size <= 4 }
            .sorted()
            .map { interval ->
                val pc = (rootPc + interval) % 12
                val octave = if (rootPc + interval >= 12) baseOctave + 1 else baseOctave
                pc to octave
            }

        // Generate distractors: pick other qualities from the same level
        val wrong = qualities.filter { it != targetQuality }.shuffled().take(3)

        // If fewer than 3 wrong options available, pad from other levels
        val allQualities = ChordFormulas.ALL.map { it.quality }.distinct()
        val paddedWrong = if (wrong.size < 3) {
            val extra = allQualities
                .filter { it != targetQuality && it !in wrong }
                .shuffled()
                .take(3 - wrong.size)
            wrong + extra
        } else {
            wrong
        }

        val options = (paddedWrong + targetQuality).shuffled()
        val correctIndex = options.indexOf(targetQuality)

        return ChordEarQuestion(
            rootPitchClass = rootPc,
            rootName = rootName,
            quality = targetQuality,
            symbol = formula.symbol,
            notes = notes,
            correctAnswer = targetQuality,
            options = options,
            correctIndex = correctIndex,
        )
    }
}

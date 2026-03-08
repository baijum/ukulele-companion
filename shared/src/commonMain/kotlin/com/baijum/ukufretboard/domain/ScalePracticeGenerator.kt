package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Scale
import com.baijum.ukufretboard.data.ScaleCategory
import com.baijum.ukufretboard.data.Scales

/**
 * Generates quiz and ear-training questions for scale practice.
 *
 * Questions are built algorithmically from [Scales.ALL] and can be
 * filtered by [ScaleCategory] so the practice session only uses
 * scales the player has selected.
 */
object ScalePracticeGenerator {

    // ── Data classes ────────────────────────────────────────────────

    /**
     * A multiple-choice scale quiz question.
     *
     * @property question The question text.
     * @property options Four answer options.
     * @property correctIndex Index of the correct answer (0–3).
     * @property explanation Shown after answering.
     */
    data class ScaleQuizQuestion(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String,
    )

    /**
     * An ear-training question where the user hears a scale and identifies it.
     *
     * @property root The root pitch class (0–11) of the played scale.
     * @property scale The [Scale] being played.
     * @property options Scale name options for the user to pick from.
     * @property correctIndex Index of the correct answer.
     */
    data class EarTrainingQuestion(
        val root: Int,
        val scale: Scale,
        val options: List<String>,
        val correctIndex: Int,
    )

    // ── Ordinal helpers ─────────────────────────────────────────────

    private fun ordinal(n: Int): String = when (n) {
        1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th"
    }

    /**
     * Returns the note names of a scale in order, using correct enharmonic
     * spelling for the key.
     */
    private fun scaleNoteNames(root: Int, scale: Scale): List<String> {
        val isMinor = scale.intervals.size > 2 && scale.intervals[2] == 3
        return scale.intervals.map { interval ->
            val pc = (root + interval) % Notes.PITCH_CLASS_COUNT
            Notes.enharmonicForKey(pc, root, isMinor)
        }
    }

    // ── Quiz question generation ────────────────────────────────────

    /** Question type selector — four distinct question formats. */
    private enum class QuizType { DEGREE_NOTE, IDENTIFY_SCALE, NOTE_DEGREE, SCALE_NOTES }

    /**
     * Generates a random scale quiz question from scales in the given category.
     *
     * @param category Filter scales to this category, or null for all.
     */
    fun generateQuizQuestion(category: ScaleCategory? = null): ScaleQuizQuestion {
        val pool = Scales.forCategory(category).ifEmpty { Scales.ALL }
        return when (QuizType.entries.random()) {
            QuizType.DEGREE_NOTE -> generateDegreeNoteQuestion(pool)
            QuizType.IDENTIFY_SCALE -> generateIdentifyScaleQuestion(pool)
            QuizType.NOTE_DEGREE -> generateNoteDegreeQuestion(pool)
            QuizType.SCALE_NOTES -> generateScaleNotesQuestion(pool)
        }
    }

    /**
     * "What is the Nth degree of [Root] [Scale]?"
     * Answer: a note name.
     */
    private fun generateDegreeNoteQuestion(pool: List<Scale>): ScaleQuizQuestion {
        val scale = pool.random()
        val root = (0..11).random()
        val rootName = Notes.enharmonicForKey(root, root, false)
        val names = scaleNoteNames(root, scale)
        val degreeIndex = (1 until names.size).random() // skip 0 (root)
        val correctAnswer = names[degreeIndex]

        // Wrong answers: other note names not in the correct answer
        val allNotes = Notes.NOTE_NAMES_SHARP + Notes.NOTE_NAMES_FLAT
        val wrong = allNotes
            .filter { it != correctAnswer }
            .distinct()
            .shuffled()
            .take(3)

        val options = (wrong + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return ScaleQuizQuestion(
            question = "What is the ${ordinal(degreeIndex + 1)} degree of $rootName ${scale.name}?",
            options = options,
            correctIndex = correctIndex,
            explanation = "$rootName ${scale.name}: ${names.joinToString(" – ")}",
        )
    }

    /**
     * "Which scale contains these notes: [list]?"
     * Answer: a scale name.
     */
    private fun generateIdentifyScaleQuestion(pool: List<Scale>): ScaleQuizQuestion {
        val scale = pool.random()
        val root = (0..11).random()
        val rootName = Notes.enharmonicForKey(root, root, false)
        val names = scaleNoteNames(root, scale)
        val correctAnswer = scale.name

        val wrong = pool
            .filter { it.name != scale.name }
            .map { it.name }
            .distinct()
            .shuffled()
            .take(3)

        // If not enough distractors in pool, fall back to full list
        val distractors = if (wrong.size < 3) {
            Scales.ALL.map { it.name }.filter { it != scale.name }.distinct().shuffled().take(3)
        } else {
            wrong
        }

        val options = (distractors + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return ScaleQuizQuestion(
            question = "Which scale contains: ${names.joinToString(", ")}?",
            options = options,
            correctIndex = correctIndex,
            explanation = "These are the notes of $rootName ${scale.name}.",
        )
    }

    /**
     * "What degree is [Note] in [Root] [Scale]?"
     * Answer: a degree number string like "3rd".
     */
    private fun generateNoteDegreeQuestion(pool: List<Scale>): ScaleQuizQuestion {
        val scale = pool.random()
        val root = (0..11).random()
        val rootName = Notes.enharmonicForKey(root, root, false)
        val names = scaleNoteNames(root, scale)
        val degreeIndex = (1 until names.size).random()
        val targetNote = names[degreeIndex]
        val correctAnswer = ordinal(degreeIndex + 1)

        // Wrong answers: other plausible degree ordinals
        val wrong = (1..scale.intervals.size)
            .filter { it != degreeIndex + 1 }
            .map { ordinal(it) }
            .shuffled()
            .take(3)

        val options = (wrong + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return ScaleQuizQuestion(
            question = "What degree is $targetNote in $rootName ${scale.name}?",
            options = options,
            correctIndex = correctIndex,
            explanation = "$targetNote is the $correctAnswer degree of $rootName ${scale.name}.",
        )
    }

    /**
     * "Name the notes in [Root] [Scale]"
     * Answer: ordered comma-separated note list.
     */
    private fun generateScaleNotesQuestion(pool: List<Scale>): ScaleQuizQuestion {
        val scale = pool.random()
        val root = (0..11).random()
        val rootName = Notes.enharmonicForKey(root, root, false)
        val names = scaleNoteNames(root, scale)
        val correctAnswer = names.joinToString(", ")

        // Generate wrong answers by using different roots or scales
        val wrong = mutableListOf<String>()
        val candidates = pool.shuffled()
        for (candidate in candidates) {
            if (wrong.size >= 3) break
            val altRoot = if (candidate == scale) (root + listOf(2, 5, 7).random()) % 12 else root
            val altNames = scaleNoteNames(altRoot, candidate).joinToString(", ")
            if (altNames != correctAnswer && altNames !in wrong) {
                wrong.add(altNames)
            }
        }
        // Fall back if still not enough
        while (wrong.size < 3) {
            val altRoot = (0..11).random()
            val altScale = Scales.ALL.random()
            val altNames = scaleNoteNames(altRoot, altScale).joinToString(", ")
            if (altNames != correctAnswer && altNames !in wrong) {
                wrong.add(altNames)
            }
        }

        val options = (wrong + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return ScaleQuizQuestion(
            question = "Which are the notes in $rootName ${scale.name}?",
            options = options,
            correctIndex = correctIndex,
            explanation = "$rootName ${scale.name}: ${names.joinToString(" – ")}",
        )
    }

    // ── Ear training question generation ────────────────────────────

    /**
     * Generates an ear-training question from scales in the given category.
     *
     * @param category Filter scales to this category, or null for all.
     */
    fun generateEarQuestion(category: ScaleCategory? = null): EarTrainingQuestion {
        val pool = Scales.forCategory(category).ifEmpty { Scales.ALL }
        val scale = pool.random()
        val root = (0..11).random()

        val distractorPool = pool.filter { it.name != scale.name }.ifEmpty {
            Scales.ALL.filter { it.name != scale.name }
        }
        val wrong = distractorPool
            .map { it.name }
            .distinct()
            .shuffled()
            .take(3)

        val options = (wrong + scale.name).shuffled()
        val correctIndex = options.indexOf(scale.name)

        return EarTrainingQuestion(
            root = root,
            scale = scale,
            options = options,
            correctIndex = correctIndex,
        )
    }
}

package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.KeySignatures
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progressions
import com.baijum.ukufretboard.data.Scales

/**
 * Dynamically generates music theory quiz questions from existing app data.
 *
 * Questions cover five categories: Intervals, Chords, Keys, Scales, and Progressions.
 * All questions are generated algorithmically — no static question bank needed.
 */
object QuizGenerator {

    /** Quiz question categories. */
    enum class QuizCategory(val label: String) {
        INTERVALS("Intervals"),
        CHORDS("Chords"),
        KEYS("Keys"),
        SCALES("Scales"),
        PROGRESSIONS("Progressions"),
    }

    /**
     * A single quiz question with multiple choice answers.
     *
     * @property category The question category.
     * @property question The question text.
     * @property options Four answer options.
     * @property correctIndex Index of the correct answer (0–3).
     * @property explanation Explanation shown after answering.
     */
    data class QuizQuestion(
        val category: QuizCategory,
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String,
    )

    /** Interval names used for quiz generation. */
    private val INTERVAL_NAMES = listOf(
        "Unison", "Minor 2nd", "Major 2nd", "Minor 3rd", "Major 3rd",
        "Perfect 4th", "Tritone", "Perfect 5th", "Minor 6th", "Major 6th",
        "Minor 7th", "Major 7th",
    )

    /**
     * Generates a random quiz question from the specified category.
     *
     * @param category The category to generate from, or null for random.
     * @return A [QuizQuestion] with 4 options.
     */
    fun generate(category: QuizCategory? = null): QuizQuestion {
        val cat = category ?: QuizCategory.entries.random()
        return when (cat) {
            QuizCategory.INTERVALS -> if ((0..1).random() == 0) generateIntervalQuestion() else generateSemitoneCountQuestion()
            QuizCategory.CHORDS -> if ((0..1).random() == 0) generateChordQuestion() else generateChordNotesQuestion()
            QuizCategory.KEYS -> if ((0..1).random() == 0) generateKeyQuestion() else generateRelativeMinorQuestion()
            QuizCategory.SCALES -> if ((0..1).random() == 0) generateScaleQuestion() else generateScalePatternQuestion()
            QuizCategory.PROGRESSIONS -> if ((0..1).random() == 0) generateProgressionQuestion() else generateChordQualityQuestion()
        }
    }

    // ── Interval Questions ──────────────────────────────────────────

    /** "What interval is X to Y?" */
    private fun generateIntervalQuestion(): QuizQuestion {
        val root = (0..11).random()
        val interval = (1..11).random()
        val rootName = Notes.pitchClassToName(root)
        val targetName = Notes.pitchClassToName((root + interval) % 12)
        val correctAnswer = INTERVAL_NAMES[interval]

        val wrongAnswers = INTERVAL_NAMES
            .filterIndexed { i, _ -> i != interval && i != 0 }
            .shuffled()
            .take(3)

        val options = (wrongAnswers + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.INTERVALS,
            question = "What interval is $rootName to $targetName?",
            options = options,
            correctIndex = correctIndex,
            explanation = "$rootName to $targetName is $interval semitone${if (interval != 1) "s" else ""}, which is a $correctAnswer.",
        )
    }

    /** "How many semitones in a [interval name]?" */
    private fun generateSemitoneCountQuestion(): QuizQuestion {
        val interval = (1..11).random()
        val intervalName = INTERVAL_NAMES[interval]
        val correctAnswer = "$interval"

        // Generate 3 nearby wrong answers
        val wrong = ((1..11).toList() - interval).shuffled().take(3).map { "$it" }
        val options = (wrong + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.INTERVALS,
            question = "How many semitones in a $intervalName?",
            options = options,
            correctIndex = correctIndex,
            explanation = "A $intervalName spans $interval semitone${if (interval != 1) "s" else ""}.",
        )
    }

    // ── Chord Questions ─────────────────────────────────────────────

    /** "What chord type has the formula X?" */
    private fun generateChordQuestion(): QuizQuestion {
        val formulas = ChordFormulas.ALL.filter { it.intervals.size <= 4 }
        val formula = formulas.random()
        val root = (0..11).random()
        val rootName = Notes.pitchClassToName(root)

        // Question: "What type of chord has the formula 1 b3 5?"
        val formulaStr = formula.intervals.sorted().mapNotNull { interval ->
            when (interval) {
                0 -> "1"; 1 -> "b2"; 2 -> "2"; 3 -> "b3"; 4 -> "3"
                5 -> "4"; 6 -> "b5"; 7 -> "5"; 8 -> "#5"; 9 -> "6"
                10 -> "b7"; 11 -> "7"; else -> null
            }
        }.joinToString(" ")

        val correctAnswer = formula.quality

        val wrongAnswers = formulas
            .filter { it.quality != formula.quality }
            .map { it.quality }
            .distinct()
            .shuffled()
            .take(3)

        val options = (wrongAnswers + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.CHORDS,
            question = "What chord type has the formula: $formulaStr?",
            options = options,
            correctIndex = correctIndex,
            explanation = "The formula $formulaStr defines a ${formula.quality} chord (${rootName}${formula.symbol}).",
        )
    }

    private fun generateKeyQuestion(): QuizQuestion {
        val keySigs = KeySignatures.ALL.values.toList()
        val keySig = keySigs.random()
        val keyName = Notes.pitchClassToName(keySig.pitchClass)

        val sigStr = KeySignatures.formatSignature(keySig)
        val correctAnswer = sigStr

        // Generate wrong answers from other keys
        val wrongAnswers = keySigs
            .filter { it.pitchClass != keySig.pitchClass }
            .map { KeySignatures.formatSignature(it) }
            .distinct()
            .shuffled()
            .take(3)

        val options = (wrongAnswers + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.KEYS,
            question = "What is the key signature of $keyName major?",
            options = options,
            correctIndex = correctIndex,
            explanation = "$keyName major has $sigStr.",
        )
    }

    private fun generateScaleQuestion(): QuizQuestion {
        val scales = Scales.ALL.filter { it.intervals.size in 5..7 }
        val scale = scales.random()
        val root = (0..11).random()
        val rootName = Notes.pitchClassToName(root)

        // "How many notes in the ___ scale?"
        val correctAnswer = "${scale.intervals.size} notes"
        val allCounts = scales.map { it.intervals.size }.distinct().sorted()
        val wrongAnswers = allCounts
            .filter { it != scale.intervals.size }
            .map { "$it notes" }
            .shuffled()
            .take(3)

        val options = (wrongAnswers + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.SCALES,
            question = "How many notes are in the ${scale.name} scale?",
            options = options,
            correctIndex = correctIndex,
            explanation = "The ${scale.name} scale has ${scale.intervals.size} notes per octave.",
        )
    }

    private fun generateProgressionQuestion(): QuizQuestion {
        val root = (0..11).random()
        val rootName = Notes.pitchClassToName(root)

        // "What is the V chord in __ major?"
        val degreeIndex = listOf(1, 2, 3, 4).random() // ii, iii, IV, V
        val degrees = Progressions.diatonicDegrees(com.baijum.ukufretboard.data.ScaleType.MAJOR)
        val degree = degrees[degreeIndex]
        val chordRoot = (root + degree.interval) % 12
        val correctAnswer = Notes.pitchClassToName(chordRoot) + degree.quality

        // Wrong answers: other diatonic chords
        val wrongAnswers = degrees
            .filterIndexed { i, _ -> i != degreeIndex }
            .map {
                val cr = (root + it.interval) % 12
                Notes.pitchClassToName(cr) + it.quality
            }
            .shuffled()
            .take(3)

        val options = (wrongAnswers + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.PROGRESSIONS,
            question = "What is the ${degree.numeral} chord in $rootName major?",
            options = options,
            correctIndex = correctIndex,
            explanation = "In $rootName major, the ${degree.numeral} chord is $correctAnswer.",
        )
    }

    /** "Is the [numeral] chord in a major key major or minor?" */
    private fun generateChordQualityQuestion(): QuizQuestion {
        val degrees = Progressions.diatonicDegrees(com.baijum.ukufretboard.data.ScaleType.MAJOR)
        val degreeIndex = (0..6).random()
        val degree = degrees[degreeIndex]
        val quality = when {
            degree.quality.isEmpty() -> "Major"
            degree.quality == "m" -> "Minor"
            degree.quality == "dim" -> "Diminished"
            else -> degree.quality
        }
        val correctAnswer = quality

        val allQualities = listOf("Major", "Minor", "Diminished").filter { it != quality }
        val wrong = allQualities + listOf("Augmented").take(3 - allQualities.size)
        val options = (wrong.take(3) + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.PROGRESSIONS,
            question = "In a major key, what quality is the ${degree.numeral} chord?",
            options = options,
            correctIndex = correctIndex,
            explanation = "In a major key, the ${degree.numeral} chord is $quality.",
        )
    }

    // ── New Chord Question ──────────────────────────────────────────

    /** "How many notes in a [chord type]?" */
    private fun generateChordNotesQuestion(): QuizQuestion {
        val formulas = ChordFormulas.ALL.filter { it.intervals.size in 3..5 }
        val formula = formulas.random()
        val noteCount = formula.intervals.size
        val correctAnswer = "$noteCount notes"

        val wrong = listOf(3, 4, 5, 6)
            .filter { it != noteCount }
            .shuffled()
            .take(3)
            .map { "$it notes" }
        val options = (wrong + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.CHORDS,
            question = "How many notes in a ${formula.quality} chord?",
            options = options,
            correctIndex = correctIndex,
            explanation = "A ${formula.quality} chord (${formula.symbol}) has $noteCount notes.",
        )
    }

    // ── New Key Question ────────────────────────────────────────────

    /** "What is the relative minor of [key] major?" */
    private fun generateRelativeMinorQuestion(): QuizQuestion {
        val keySigs = KeySignatures.ALL.values.toList()
        val keySig = keySigs.random()
        val keyName = Notes.pitchClassToName(keySig.pitchClass)
        val relMinorName = Notes.pitchClassToName(keySig.relativeMinorPitchClass)
        val correctAnswer = "$relMinorName minor"

        val wrong = keySigs
            .filter { it.pitchClass != keySig.pitchClass }
            .map { "${Notes.pitchClassToName(it.relativeMinorPitchClass)} minor" }
            .distinct()
            .shuffled()
            .take(3)

        val options = (wrong + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        return QuizQuestion(
            category = QuizCategory.KEYS,
            question = "What is the relative minor of $keyName major?",
            options = options,
            correctIndex = correctIndex,
            explanation = "The relative minor of $keyName major is $relMinorName minor (3 semitones below the root).",
        )
    }

    // ── New Scale Question ──────────────────────────────────────────

    /** "Which mode starts on the Nth degree of the major scale?" */
    private fun generateScalePatternQuestion(): QuizQuestion {
        val modes = listOf(
            1 to "Ionian (Major)",
            2 to "Dorian",
            3 to "Phrygian",
            4 to "Lydian",
            5 to "Mixolydian",
            6 to "Aeolian (Natural Minor)",
            7 to "Locrian",
        )
        val (degree, modeName) = modes.random()
        val correctAnswer = modeName

        val wrong = modes
            .filter { it.second != modeName }
            .map { it.second }
            .shuffled()
            .take(3)

        val options = (wrong + correctAnswer).shuffled()
        val correctIndex = options.indexOf(correctAnswer)

        val ordinal = when (degree) {
            1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${degree}th"
        }

        return QuizQuestion(
            category = QuizCategory.SCALES,
            question = "Which mode starts on the $ordinal degree of the major scale?",
            options = options,
            correctIndex = correctIndex,
            explanation = "The $ordinal mode of the major scale is $modeName.",
        )
    }
}

package com.baijum.ukufretboard.data

/**
 * Which finger plucks the string in a fingerpicking step.
 */
enum class Finger(val label: String) {
    THUMB("T"),
    INDEX("I"),
    MIDDLE("M"),
    RING("R"),
}

/**
 * A single step in a fingerpicking pattern.
 *
 * @property finger The picking finger.
 * @property stringIndex The string to pluck (0=G, 1=C, 2=E, 3=A).
 * @property emphasis Whether this step is accented.
 */
data class FingerpickStep(
    val finger: Finger,
    val stringIndex: Int,
    val emphasis: Boolean = false,
)

/**
 * A complete fingerpicking pattern with metadata.
 *
 * @property name Human-readable name of the pattern.
 * @property description Brief description of the pattern and its use.
 * @property difficulty Skill level required.
 * @property timeSignature Time signature label (e.g., "4/4", "3/4", "6/8").
 * @property steps The sequence of picking steps.
 * @property notation Compact text notation showing finger labels.
 * @property suggestedBpm Recommended tempo range in BPM.
 */
data class FingerpickingPattern(
    val name: String,
    val description: String,
    val difficulty: Difficulty,
    val timeSignature: String = "4/4",
    val steps: List<FingerpickStep>,
    val notation: String,
    val suggestedBpm: IntRange,
)

/**
 * Library of common ukulele fingerpicking patterns.
 *
 * String assignments follow standard right-hand technique:
 * - Thumb (T): G string (0) and C string (1)
 * - Index (I): E string (2)
 * - Middle (M): A string (3)
 * - Ring (R): occasionally A string (3) for four-finger patterns
 */
object FingerpickingPatterns {

    /** String name labels indexed by string index. */
    val STRING_NAMES = listOf("G", "C", "E", "A")

    val ALL: List<FingerpickingPattern> = listOf(
        FingerpickingPattern(
            name = "Arpeggio Up",
            description = "Play strings from lowest to highest, one at a time. The foundation of all fingerpicking.",
            difficulty = Difficulty.BEGINNER,
            steps = listOf(
                FingerpickStep(Finger.THUMB, 0, emphasis = true),
                FingerpickStep(Finger.THUMB, 1),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.MIDDLE, 3),
            ),
            notation = "T(G) T(C) I(E) M(A)",
            suggestedBpm = 60..100,
        ),
        FingerpickingPattern(
            name = "Arpeggio Down",
            description = "Play strings from highest to lowest. A descending variation of the basic arpeggio.",
            difficulty = Difficulty.BEGINNER,
            steps = listOf(
                FingerpickStep(Finger.MIDDLE, 3, emphasis = true),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.THUMB, 1),
                FingerpickStep(Finger.THUMB, 0),
            ),
            notation = "M(A) I(E) T(C) T(G)",
            suggestedBpm = 60..100,
        ),
        FingerpickingPattern(
            name = "Pinch",
            description = "Thumb and middle finger pluck simultaneously, creating a full sound. Common in folk and Hawaiian music.",
            difficulty = Difficulty.BEGINNER,
            steps = listOf(
                FingerpickStep(Finger.THUMB, 0, emphasis = true),
                // Middle plucks simultaneously (shown sequentially for notation)
                FingerpickStep(Finger.MIDDLE, 3, emphasis = true),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.THUMB, 1),
            ),
            notation = "T+M  I(E)  T(C)",
            suggestedBpm = 60..90,
        ),
        FingerpickingPattern(
            name = "Travis Pick",
            description = "An alternating bass pattern popular in folk and country. Thumb alternates between bass strings while fingers pick melody.",
            difficulty = Difficulty.INTERMEDIATE,
            steps = listOf(
                FingerpickStep(Finger.THUMB, 0, emphasis = true),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.THUMB, 1, emphasis = true),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.THUMB, 0, emphasis = true),
                FingerpickStep(Finger.MIDDLE, 3),
                FingerpickStep(Finger.THUMB, 1, emphasis = true),
                FingerpickStep(Finger.INDEX, 2),
            ),
            notation = "T(G) I  T(C) I  T(G) M  T(C) I",
            suggestedBpm = 60..100,
        ),
        FingerpickingPattern(
            name = "Roll",
            description = "A rapid ascending roll across all strings. Creates a flowing, harp-like effect.",
            difficulty = Difficulty.INTERMEDIATE,
            steps = listOf(
                FingerpickStep(Finger.THUMB, 0, emphasis = true),
                FingerpickStep(Finger.THUMB, 1),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.MIDDLE, 3),
                FingerpickStep(Finger.MIDDLE, 3),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.THUMB, 1),
                FingerpickStep(Finger.THUMB, 0),
            ),
            notation = "T(G) T(C) I  M  M  I  T(C) T(G)",
            suggestedBpm = 70..110,
        ),
        FingerpickingPattern(
            name = "Inside-Out",
            description = "Start from the inner strings and move outward. Creates a distinctive, open sound.",
            difficulty = Difficulty.INTERMEDIATE,
            steps = listOf(
                FingerpickStep(Finger.THUMB, 1, emphasis = true),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.THUMB, 0),
                FingerpickStep(Finger.MIDDLE, 3),
            ),
            notation = "T(C) I(E) T(G) M(A)",
            suggestedBpm = 60..100,
        ),
        FingerpickingPattern(
            name = "Outside-In",
            description = "Start from the outer strings and move inward. A mirror of the inside-out pattern.",
            difficulty = Difficulty.INTERMEDIATE,
            steps = listOf(
                FingerpickStep(Finger.THUMB, 0, emphasis = true),
                FingerpickStep(Finger.MIDDLE, 3),
                FingerpickStep(Finger.THUMB, 1),
                FingerpickStep(Finger.INDEX, 2),
            ),
            notation = "T(G) M(A) T(C) I(E)",
            suggestedBpm = 60..100,
        ),
        FingerpickingPattern(
            name = "Clawhammer",
            description = "A banjo-inspired technique adapted for ukulele. Thumb plays melody on inner strings, index strums down.",
            difficulty = Difficulty.INTERMEDIATE,
            timeSignature = "3/4",
            steps = listOf(
                FingerpickStep(Finger.INDEX, 3, emphasis = true),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.THUMB, 0),
                FingerpickStep(Finger.INDEX, 3),
                FingerpickStep(Finger.INDEX, 2),
                FingerpickStep(Finger.INDEX, 3),
            ),
            notation = "I(A) I(E) T(G) I(A) I(E) I(A)",
            suggestedBpm = 70..110,
        ),
    )
}

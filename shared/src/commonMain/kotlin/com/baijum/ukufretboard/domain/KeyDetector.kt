package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes

/**
 * Detects the musical key of a song from its chord names.
 *
 * Uses a simplified Krumhansl–Schmuckler approach: for each candidate key,
 * score how well the song's chords fit as diatonic chords of that key.
 * The key with the highest score is selected.
 */
object KeyDetector {

    /**
     * Diatonic chord qualities for each scale degree of a major key.
     *
     * Index 0 = I (major), 1 = ii (minor), 2 = iii (minor), 3 = IV (major),
     * 4 = V (major), 5 = vi (minor), 6 = vii (diminished).
     */
    private val MAJOR_SCALE_INTERVALS = listOf(0, 2, 4, 5, 7, 9, 11)
    private val MAJOR_DIATONIC_QUALITIES = listOf("", "m", "m", "", "", "m", "dim")

    /**
     * Diatonic chord qualities for natural minor.
     *
     * i (minor), ii (dim), III (major), iv (minor), v (minor), VI (major), VII (major).
     */
    private val MINOR_SCALE_INTERVALS = listOf(0, 2, 3, 5, 7, 8, 10)
    private val MINOR_DIATONIC_QUALITIES = listOf("m", "dim", "", "m", "m", "", "")

    /** Regex to parse a chord name into root + quality. */
    private val CHORD_NAME_REGEX = Regex("^([A-G][#b]?)(.*)")

    /**
     * Detects the most likely key from a list of chord names.
     *
     * @param chordNames Chord names found in the song (e.g., "Am", "C", "G", "F").
     * @return A [KeyResult] with the detected key, or null if no chords are provided.
     */
    fun detectKey(chordNames: List<String>): KeyResult? {
        if (chordNames.isEmpty()) return null

        // Parse chord names to (pitchClass, quality) pairs
        val parsed = chordNames.mapNotNull { name ->
            val match = CHORD_NAME_REGEX.find(name) ?: return@mapNotNull null
            val rootStr = match.groupValues[1]
            val quality = normalizeQuality(match.groupValues[2])
            val pc = Notes.NOTE_NAMES_SHARP.indexOf(rootStr).takeIf { it >= 0 }
                ?: Notes.NOTE_NAMES_FLAT.indexOf(rootStr).takeIf { it >= 0 }
                ?: return@mapNotNull null
            pc to quality
        }

        if (parsed.isEmpty()) return null

        var bestScore = -1
        var bestKey = 0
        var bestIsMinor = false

        // Try all 12 major keys
        for (root in 0 until Notes.PITCH_CLASS_COUNT) {
            val majorScore = scoreKey(parsed, root, MAJOR_SCALE_INTERVALS, MAJOR_DIATONIC_QUALITIES)
            if (majorScore > bestScore) {
                bestScore = majorScore
                bestKey = root
                bestIsMinor = false
            }

            val minorScore = scoreKey(parsed, root, MINOR_SCALE_INTERVALS, MINOR_DIATONIC_QUALITIES)
            if (minorScore > bestScore) {
                bestScore = minorScore
                bestKey = root
                bestIsMinor = true
            }
        }

        val keyName = Notes.enharmonicForKey(bestKey, bestKey, bestIsMinor)
        val label = if (bestIsMinor) "$keyName minor" else "$keyName major"

        return KeyResult(
            rootPitchClass = bestKey,
            isMinor = bestIsMinor,
            displayName = label,
            confidence = if (parsed.isNotEmpty()) bestScore.toFloat() / parsed.size else 0f,
        )
    }

    /**
     * Scores how well the given chords fit within a specific key.
     *
     * Each chord gets points for:
     * - 3 points if the root is a diatonic scale tone AND the quality matches the diatonic quality
     * - 1 point if the root is a diatonic scale tone but quality doesn't match
     * - 0 points otherwise
     *
     * The first chord (tonic) gets a bonus of 2 if it matches the I/i chord.
     */
    private fun scoreKey(
        chords: List<Pair<Int, String>>,
        keyRoot: Int,
        scaleIntervals: List<Int>,
        diatonicQualities: List<String>,
    ): Int {
        val diatonicRoots = scaleIntervals.map { (keyRoot + it) % Notes.PITCH_CLASS_COUNT }
        var score = 0

        chords.forEachIndexed { index, (chordRoot, chordQuality) ->
            val degreeIndex = diatonicRoots.indexOf(chordRoot)
            if (degreeIndex >= 0) {
                val expectedQuality = diatonicQualities[degreeIndex]
                if (qualityMatches(chordQuality, expectedQuality)) {
                    score += 3
                } else {
                    score += 1
                }
                // Bonus for first/last chord being the tonic
                if ((index == 0 || index == chords.lastIndex) && degreeIndex == 0 &&
                    qualityMatches(chordQuality, diatonicQualities[0])
                ) {
                    score += 2
                }
            }
        }

        return score
    }

    /**
     * Normalizes chord quality to a simplified form for comparison.
     *
     * Maps extended qualities to their base:
     * - "m7", "m9" -> "m"
     * - "7", "9", "maj7" -> ""
     * - "sus2", "sus4" -> "sus"
     */
    private fun normalizeQuality(quality: String): String = when {
        quality.startsWith("dim") -> "dim"
        quality.startsWith("aug") -> "aug"
        quality.startsWith("sus") -> "sus"
        quality == "m" || quality.startsWith("m7") || quality.startsWith("m9") ||
            quality.startsWith("min") -> "m"
        quality == "" || quality == "maj" || quality.startsWith("maj7") ||
            quality == "7" || quality == "9" || quality == "6" ||
            quality.startsWith("add") -> ""
        else -> quality
    }

    /**
     * Checks if a chord quality matches the expected diatonic quality.
     */
    private fun qualityMatches(actual: String, expected: String): Boolean {
        if (actual == expected) return true
        // "m" covers "m7", etc.
        if (expected == "m" && actual.startsWith("m")) return true
        if (expected == "" && (actual == "7" || actual == "maj7" || actual == "6")) return true
        return false
    }

    /**
     * Result of key detection.
     *
     * @property rootPitchClass The pitch class of the detected key root (0–11).
     * @property isMinor Whether the detected key is minor.
     * @property displayName Human-readable key name (e.g., "C major", "A minor").
     * @property confidence Score from 0.0 to ~5.0 indicating detection reliability.
     */
    data class KeyResult(
        val rootPitchClass: Int,
        val isMinor: Boolean,
        val displayName: String,
        val confidence: Float,
    )
}

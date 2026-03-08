package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes

/**
 * Scores real-time play-along performance by comparing detected chords
 * from the audio pipeline with the expected chord at each beat.
 *
 * Tracks per-beat accuracy and generates an overall session score.
 */
class PlayAlongScorer {

    private val beatResults = mutableListOf<BeatResult>()
    private var totalBeats = 0
    private var correctBeats = 0
    private var currentStreak = 0
    private var bestStreak = 0

    /**
     * Records the result for a single beat.
     *
     * @param expectedChord The chord name expected at this beat (e.g., "Am").
     * @param detectedChord The chord name detected from audio (e.g., "Am"), or null if nothing detected.
     * @param confidence The detection confidence (0.0–1.0).
     */
    fun recordBeat(
        expectedChord: String,
        detectedChord: String?,
        confidence: Float,
    ) {
        val isCorrect = detectedChord != null && normalizeChord(expectedChord) == normalizeChord(detectedChord)

        beatResults.add(
            BeatResult(
                expected = expectedChord,
                detected = detectedChord,
                isCorrect = isCorrect,
                confidence = confidence,
            ),
        )

        totalBeats++
        if (isCorrect) {
            correctBeats++
            currentStreak++
            if (currentStreak > bestStreak) bestStreak = currentStreak
        } else {
            currentStreak = 0
        }
    }

    /**
     * Returns the current session score.
     */
    fun getScore(): PlayAlongScore = PlayAlongScore(
        totalBeats = totalBeats,
        correctBeats = correctBeats,
        accuracy = if (totalBeats > 0) correctBeats.toFloat() / totalBeats else 0f,
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        beatResults = beatResults.toList(),
    )

    /**
     * Resets the scorer for a new session.
     */
    fun reset() {
        beatResults.clear()
        totalBeats = 0
        correctBeats = 0
        currentStreak = 0
        bestStreak = 0
    }

    /**
     * Normalizes a chord name for comparison (strips extensions).
     */
    private fun normalizeChord(name: String): String {
        // Remove 7ths, 9ths etc. for basic matching
        return name.replace(Regex("[79]$"), "")
            .replace("maj", "")
            .trim()
    }

    /**
     * Result for a single beat.
     */
    data class BeatResult(
        val expected: String,
        val detected: String?,
        val isCorrect: Boolean,
        val confidence: Float,
    )

    /**
     * Overall session score.
     */
    data class PlayAlongScore(
        val totalBeats: Int = 0,
        val correctBeats: Int = 0,
        val accuracy: Float = 0f,
        val currentStreak: Int = 0,
        val bestStreak: Int = 0,
        val beatResults: List<BeatResult> = emptyList(),
    ) {
        val accuracyPercent: Int get() = (accuracy * 100).toInt()

        val grade: String
            get() = when {
                accuracyPercent >= 90 -> "S"
                accuracyPercent >= 80 -> "A"
                accuracyPercent >= 70 -> "B"
                accuracyPercent >= 60 -> "C"
                accuracyPercent >= 50 -> "D"
                else -> "F"
            }
    }
}

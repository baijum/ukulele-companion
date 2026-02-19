package com.baijum.ukufretboard.domain

/**
 * Detects chords from sequentially played notes (arpeggios).
 *
 * Maintains a sliding time-window buffer of pitch classes. When enough unique
 * pitch classes accumulate within the window, delegates to [ChordDetector] for
 * interval-based chord matching.
 *
 * @param windowMs Duration of the sliding window in milliseconds.
 */
class ArpeggioDetector(private val windowMs: Long = 3_000L) {

    private data class TimedPitchClass(val timestampMs: Long, val pitchClass: Int)

    private val buffer = ArrayDeque<TimedPitchClass>()

    /**
     * Adds a detected pitch class to the buffer.
     *
     * Consecutive duplicates are ignored to prevent sustained-note flooding.
     * Entries older than [windowMs] are pruned on each call.
     */
    fun addNote(timestampMs: Long, pitchClass: Int) {
        if (buffer.lastOrNull()?.pitchClass == pitchClass) return
        prune(timestampMs)
        buffer.addLast(TimedPitchClass(timestampMs, pitchClass))
    }

    /**
     * Attempts chord detection on the accumulated pitch classes.
     *
     * @return A [ChordDetector.DetectionResult.ChordFound] if 3+ unique pitch
     *   classes form a known chord, or `null` otherwise.
     */
    fun detect(currentTimeMs: Long): ChordDetector.DetectionResult.ChordFound? {
        prune(currentTimeMs)
        val uniquePitchClasses = buffer.map { it.pitchClass }.distinct()
        if (uniquePitchClasses.size < 3) return null

        val result = ChordDetector.detect(uniquePitchClasses)
        return result as? ChordDetector.DetectionResult.ChordFound
    }

    fun clear() {
        buffer.clear()
    }

    private fun prune(currentTimeMs: Long) {
        val cutoff = currentTimeMs - windowMs
        while (buffer.isNotEmpty() && buffer.first().timestampMs < cutoff) {
            buffer.removeFirst()
        }
    }
}

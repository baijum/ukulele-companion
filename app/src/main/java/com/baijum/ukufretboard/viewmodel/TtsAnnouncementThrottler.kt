package com.baijum.ukufretboard.viewmodel

/**
 * Determines whether a TTS tuning announcement should be spoken based on
 * time-based throttling and duplicate suppression.
 *
 * This is a pure Kotlin class with no Android dependencies, making it
 * straightforward to unit test.
 *
 * @param minIntervalMs minimum milliseconds between non-in-tune announcements.
 * @param inTuneIntervalMs minimum milliseconds between "in tune" announcements.
 * @param centsBucketSize cents deviation is divided into buckets of this size;
 *   the same note+status is only re-announced when the bucket changes.
 * @param timeProvider injectable clock for testing; defaults to system time.
 */
class TtsAnnouncementThrottler(
    private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS,
    private val inTuneIntervalMs: Long = DEFAULT_IN_TUNE_INTERVAL_MS,
    private val centsBucketSize: Int = DEFAULT_CENTS_BUCKET_SIZE,
    private val timeProvider: () -> Long = System::currentTimeMillis,
) {

    private var lastSpokenTimeMs = 0L
    private var lastStatus: TuningStatus? = null
    private var lastNote: String? = null
    private var lastCentsBucket: Int = Int.MIN_VALUE

    /**
     * Returns `true` if the announcement should be spoken, `false` if it
     * should be suppressed. When returning `true`, the internal state is
     * updated automatically.
     */
    fun shouldAnnounce(
        noteName: String,
        status: TuningStatus,
        cents: Double,
        justTuned: Boolean,
    ): Boolean {
        val now = timeProvider()

        if (justTuned) {
            recordState(now, status, noteName, centsBucket = 0)
            return true
        }

        if (status == TuningStatus.SILENT) return false

        val centsBucket = (cents / centsBucketSize).toInt()

        val minInterval = if (status == TuningStatus.IN_TUNE) {
            inTuneIntervalMs
        } else {
            minIntervalMs
        }

        if (now - lastSpokenTimeMs < minInterval) return false

        if (status == lastStatus &&
            noteName == lastNote &&
            centsBucket == lastCentsBucket
        ) return false

        recordState(now, status, noteName, centsBucket)
        return true
    }

    /** Resets internal state (e.g., when the setting is toggled off and back on). */
    fun reset() {
        lastSpokenTimeMs = 0L
        lastStatus = null
        lastNote = null
        lastCentsBucket = Int.MIN_VALUE
    }

    private fun recordState(
        timeMs: Long,
        status: TuningStatus,
        noteName: String,
        centsBucket: Int,
    ) {
        lastSpokenTimeMs = timeMs
        lastStatus = status
        lastNote = noteName
        lastCentsBucket = centsBucket
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MS = 2000L
        const val DEFAULT_IN_TUNE_INTERVAL_MS = 3000L
        const val DEFAULT_CENTS_BUCKET_SIZE = 5
    }
}

package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.BeatType

/**
 * Pure functions for metronome business logic shared between Android and iOS.
 *
 * These extract duplicated tempo, time-signature, subdivision, and accent-pattern
 * calculations from the platform ViewModels into testable, platform-free code.
 */
object MetronomeStateLogic {
    private const val BPM_MIN = 30
    private const val BPM_MAX = 300
    private const val BEATS_MIN = 2
    private const val BEATS_MAX = 7
    private const val SUBDIVISION_MIN = 1
    private const val SUBDIVISION_MAX = 4

    /**
     * Result of parsing a time signature label like "4/4" or "6/8".
     *
     * @property isCompound Whether the time signature is compound (e.g. 6/8, 12/8).
     * @property beatsPerMeasure Logical beat count for accent pattern purposes.
     * @property subdivision Number of sub-beats per beat (3 for compound, 1 for simple).
     */
    data class TimeSignatureResult(
        val isCompound: Boolean,
        val beatsPerMeasure: Int,
        val subdivision: Int,
    )

    /** Coerce a BPM value to the valid range [30, 300]. */
    fun clampBpm(value: Int): Int = value.coerceIn(BPM_MIN, BPM_MAX)

    /**
     * Parse a time signature label into its compound/simple components.
     *
     * - "6/8" → compound, 2 beats, subdivision 3
     * - "12/8" → compound, 4 beats, subdivision 3
     * - "n/4" patterns → simple, n beats (clamped 2–7), subdivision 1
     */
    fun parseTimeSignature(label: String): TimeSignatureResult =
        when (label) {
            "6/8" -> {
                TimeSignatureResult(isCompound = true, beatsPerMeasure = 2, subdivision = 3)
            }

            "12/8" -> {
                TimeSignatureResult(isCompound = true, beatsPerMeasure = 4, subdivision = 3)
            }

            else -> {
                val beats =
                    label.substringBefore("/").toIntOrNull()?.coerceIn(BEATS_MIN, BEATS_MAX)
                        ?: 4
                TimeSignatureResult(isCompound = false, beatsPerMeasure = beats, subdivision = 1)
            }
        }

    /** Cycle a beat type: ACCENT → NORMAL → MUTE → ACCENT. */
    fun cycleBeatType(current: BeatType): BeatType =
        when (current) {
            BeatType.ACCENT -> BeatType.NORMAL
            BeatType.NORMAL -> BeatType.MUTE
            BeatType.MUTE -> BeatType.ACCENT
        }

    /** Build the default accent pattern: first beat ACCENT, rest NORMAL. */
    fun defaultAccentPattern(beatsPerMeasure: Int): List<BeatType> =
        List(beatsPerMeasure) { if (it == 0) BeatType.ACCENT else BeatType.NORMAL }

    /** Coerce a subdivision value to the valid range [1, 4]. */
    fun clampSubdivision(value: Int): Int = value.coerceIn(SUBDIVISION_MIN, SUBDIVISION_MAX)
}

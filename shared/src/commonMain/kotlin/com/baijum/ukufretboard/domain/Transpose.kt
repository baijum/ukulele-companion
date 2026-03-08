package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes

/**
 * Transposition utilities for shifting chords up or down by semitones.
 *
 * Transposing a chord changes only its root note — the chord quality
 * (major, minor, 7th, etc.) remains the same. This is equivalent to
 * using a capo or changing the key of a song.
 */
object Transpose {

    /**
     * Transposes a pitch class by the given number of semitones.
     *
     * @param pitchClass The original pitch class (0–11).
     * @param semitones The number of semitones to shift (positive = up, negative = down).
     * @return The transposed pitch class (0–11).
     */
    fun transposePitchClass(pitchClass: Int, semitones: Int): Int =
        ((pitchClass + semitones) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT

    /**
     * Returns the transposed chord name.
     *
     * @param rootPitchClass The original root pitch class (0–11).
     * @param symbol The chord quality symbol (e.g., "m", "7", "maj7").
     * @param semitones The number of semitones to shift.
     * @return The transposed chord name (e.g., "Ebm7").
     */
    fun transposeChordName(
        rootPitchClass: Int,
        symbol: String,
        semitones: Int,
    ): String {
        val newRoot = transposePitchClass(rootPitchClass, semitones)
        return Notes.pitchClassToName(newRoot) + symbol
    }

    /**
     * Computes the capo fret needed to play chords in [targetKey] using
     * shapes from [originalKey].
     *
     * @param originalKey The pitch class of the original key.
     * @param targetKey The pitch class of the desired sounding key.
     * @return The capo fret (0–11).
     */
    fun capoFret(originalKey: Int, targetKey: Int): Int =
        ((targetKey - originalKey) % Notes.PITCH_CLASS_COUNT + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
}

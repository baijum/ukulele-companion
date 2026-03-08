package com.baijum.ukufretboard.data

/**
 * Mapping of pitch class integers (0–11) to their human-readable note names.
 *
 * Pitch classes follow the standard chromatic scale where C = 0.
 * This is the universal numbering used in music theory for the twelve
 * tones of the Western chromatic scale.
 */
object Notes {

    /**
     * Note names indexed by pitch class, using sharps for accidentals.
     *
     * Index 0 = C, 1 = C#, 2 = D, ... 11 = B.
     */
    val NOTE_NAMES_SHARP: List<String> = listOf(
        "C", "C#", "D", "D#", "E", "F",
        "F#", "G", "G#", "A", "A#", "B"
    )

    /**
     * Note names indexed by pitch class, using flats for accidentals.
     *
     * Index 0 = C, 1 = Db, 2 = D, ... 11 = B.
     */
    val NOTE_NAMES_FLAT: List<String> = listOf(
        "C", "Db", "D", "Eb", "E", "F",
        "Gb", "G", "Ab", "A", "Bb", "B"
    )

    /**
     * Standard note names using the most common enharmonic spelling for
     * each accidental in popular music and education.
     *
     * Sharps for C# and F#; flats for Eb, Ab, and Bb.
     * Used as the default when no musical key context is available.
     */
    val NOTE_NAMES_STANDARD: List<String> = listOf(
        "C", "C#", "D", "Eb", "E", "F",
        "F#", "G", "Ab", "A", "Bb", "B"
    )

    /**
     * Alias for [NOTE_NAMES_SHARP] to maintain backward compatibility.
     */
    val NOTE_NAMES: List<String> = NOTE_NAMES_SHARP

    /** Total number of pitch classes in the chromatic scale. */
    const val PITCH_CLASS_COUNT = 12

    /**
     * Converts a pitch class integer to its human-readable note name
     * using the standard enharmonic spellings ([NOTE_NAMES_STANDARD]).
     *
     * @param pitchClass An integer from 0 to 11 representing a pitch class.
     * @return The note name (e.g., "C", "F#", "Bb").
     * @throws IndexOutOfBoundsException if [pitchClass] is not in 0..11.
     */
    fun pitchClassToName(pitchClass: Int): String =
        NOTE_NAMES_STANDARD[pitchClass]

    /**
     * Key roots (pitch classes) whose major scales use flats.
     *
     * F(5), Bb(10), Eb(3), Ab(8), Db(1), Gb(6).
     * All other keys use sharps (or have no accidentals).
     */
    private val FLAT_KEY_ROOTS = setOf(5, 10, 3, 8, 1, 6)

    /**
     * Key roots whose natural minor scales use flats.
     *
     * D minor(2), G minor(7), C minor(0), F minor(5), Bb minor(10), Eb minor(3).
     * The relative major of each of these is a flat key.
     */
    private val FLAT_MINOR_KEY_ROOTS = setOf(2, 7, 0, 5, 10, 3)

    /**
     * Returns the correctly-spelled note name for a pitch class based on
     * the current key context.
     *
     * In flat keys (F major, Bb major, etc.) accidentals are shown as flats;
     * in sharp keys (G major, D major, etc.) they are shown as sharps.
     *
     * Falls back to [NOTE_NAMES_STANDARD] when no key context is provided.
     *
     * @param pitchClass The pitch class (0–11).
     * @param keyRoot The root pitch class of the active key, or null for no key context.
     * @param isMinor Whether the active key is minor.
     */
    fun enharmonicForKey(
        pitchClass: Int,
        keyRoot: Int?,
        isMinor: Boolean = false,
    ): String {
        if (keyRoot == null) return pitchClassToName(pitchClass)

        val keyUsesFlats = if (isMinor) {
            keyRoot in FLAT_MINOR_KEY_ROOTS
        } else {
            keyRoot in FLAT_KEY_ROOTS
        }
        return if (keyUsesFlats) NOTE_NAMES_FLAT[pitchClass] else NOTE_NAMES_SHARP[pitchClass]
    }
}

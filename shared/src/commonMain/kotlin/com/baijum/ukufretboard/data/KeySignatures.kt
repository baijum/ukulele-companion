package com.baijum.ukufretboard.data

/**
 * Represents the key signature for a major key.
 *
 * @property pitchClass The pitch class of the major key root (0–11, where C=0).
 * @property sharps Number of sharps in the key signature (0–7). Zero if flats are used.
 * @property flats Number of flats in the key signature (0–7). Zero if sharps are used.
 * @property accidentals The specific sharp or flat note names, in standard order.
 * @property relativeMinorPitchClass Pitch class of the relative minor key.
 */
data class KeySignature(
    val pitchClass: Int,
    val sharps: Int,
    val flats: Int,
    val accidentals: List<String>,
    val relativeMinorPitchClass: Int,
)

/**
 * Key signature data for all 12 major keys and the Circle of Fifths order.
 *
 * Key signatures tell you which notes are sharp or flat in a given key.
 * The Circle of Fifths arranges all 12 keys by their relationship:
 * moving clockwise adds one sharp, moving counter-clockwise adds one flat.
 */
object KeySignatures {

    /** Order of sharps as they appear in key signatures. */
    val ORDER_OF_SHARPS: List<String> = listOf("F#", "C#", "G#", "D#", "A#", "E#", "B#")

    /** Order of flats as they appear in key signatures. */
    val ORDER_OF_FLATS: List<String> = listOf("Bb", "Eb", "Ab", "Db", "Gb", "Cb", "Fb")

    /**
     * Circle of Fifths order — pitch classes arranged clockwise
     * starting from C (no sharps/flats) through the sharp keys,
     * then the flat keys back to F.
     *
     * C → G → D → A → E → B → F#/Gb → Db → Ab → Eb → Bb → F
     */
    val CIRCLE_ORDER: List<Int> = listOf(0, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10, 5)

    /**
     * Key signatures for all 12 major keys, indexed by pitch class.
     *
     * Each minor key shares its key signature with its relative major
     * (e.g., A minor = C major, both have 0 sharps/flats).
     * The relative minor is always 3 semitones below the major root.
     */
    val ALL: Map<Int, KeySignature> = mapOf(
        // ── Sharp keys (clockwise from C) ──
        0  to KeySignature(0,  sharps = 0, flats = 0,
            accidentals = emptyList(),
            relativeMinorPitchClass = 9),  // C major / A minor
        7  to KeySignature(7,  sharps = 1, flats = 0,
            accidentals = listOf("F#"),
            relativeMinorPitchClass = 4),  // G major / E minor
        2  to KeySignature(2,  sharps = 2, flats = 0,
            accidentals = listOf("F#", "C#"),
            relativeMinorPitchClass = 11), // D major / B minor
        9  to KeySignature(9,  sharps = 3, flats = 0,
            accidentals = listOf("F#", "C#", "G#"),
            relativeMinorPitchClass = 6),  // A major / F# minor
        4  to KeySignature(4,  sharps = 4, flats = 0,
            accidentals = listOf("F#", "C#", "G#", "D#"),
            relativeMinorPitchClass = 1),  // E major / C# minor
        11 to KeySignature(11, sharps = 5, flats = 0,
            accidentals = listOf("F#", "C#", "G#", "D#", "A#"),
            relativeMinorPitchClass = 8),  // B major / G# minor
        // ── Enharmonic boundary ──
        6  to KeySignature(6,  sharps = 6, flats = 0,
            accidentals = listOf("F#", "C#", "G#", "D#", "A#", "E#"),
            relativeMinorPitchClass = 3),  // F# major / D# minor (or Gb major)
        // ── Flat keys (counter-clockwise from C) ──
        5  to KeySignature(5,  sharps = 0, flats = 1,
            accidentals = listOf("Bb"),
            relativeMinorPitchClass = 2),  // F major / D minor
        10 to KeySignature(10, sharps = 0, flats = 2,
            accidentals = listOf("Bb", "Eb"),
            relativeMinorPitchClass = 7),  // Bb major / G minor
        3  to KeySignature(3,  sharps = 0, flats = 3,
            accidentals = listOf("Bb", "Eb", "Ab"),
            relativeMinorPitchClass = 0),  // Eb major / C minor
        8  to KeySignature(8,  sharps = 0, flats = 4,
            accidentals = listOf("Bb", "Eb", "Ab", "Db"),
            relativeMinorPitchClass = 5),  // Ab major / F minor
        1  to KeySignature(1,  sharps = 0, flats = 5,
            accidentals = listOf("Bb", "Eb", "Ab", "Db", "Gb"),
            relativeMinorPitchClass = 10), // Db major / Bb minor
    )

    /**
     * Returns the key signature for the given major key pitch class.
     */
    fun forKey(pitchClass: Int): KeySignature? = ALL[pitchClass]

    /**
     * Formats the key signature as a display string.
     *
     * Examples: "2 sharps (F#, C#)", "3 flats (Bb, Eb, Ab)", "No sharps or flats"
     */
    fun formatSignature(keySig: KeySignature): String = when {
        keySig.sharps == 0 && keySig.flats == 0 -> "No sharps or flats"
        keySig.sharps > 0 -> "${keySig.sharps} sharp${if (keySig.sharps > 1) "s" else ""} (${keySig.accidentals.joinToString(", ")})"
        else -> "${keySig.flats} flat${if (keySig.flats > 1) "s" else ""} (${keySig.accidentals.joinToString(", ")})"
    }

    /**
     * Returns the diatonic chords (triads) for a major key, as Roman numeral labels
     * paired with the resolved chord name.
     *
     * Note names are spelled correctly for the key (flats in flat keys,
     * sharps in sharp keys).
     *
     * @param pitchClass Root pitch class of the major key.
     * @return List of pairs: (Roman numeral, resolved chord name).
     */
    fun diatonicChordsForMajor(pitchClass: Int): List<Pair<String, String>> {
        val degrees = listOf(
            Triple(0, "", "I"),
            Triple(2, "m", "ii"),
            Triple(4, "m", "iii"),
            Triple(5, "", "IV"),
            Triple(7, "", "V"),
            Triple(9, "m", "vi"),
            Triple(11, "dim", "vii\u00B0"),
        )
        return degrees.map { (interval, quality, numeral) ->
            val root = (pitchClass + interval) % Notes.PITCH_CLASS_COUNT
            numeral to (Notes.enharmonicForKey(root, pitchClass) + quality)
        }
    }

    /**
     * Returns the diatonic chords (triads) for a natural minor key.
     *
     * Natural minor harmony: i, ii°, III, iv, v, VI, VII
     *
     * @param pitchClass Root pitch class of the minor key.
     * @return List of pairs: (Roman numeral, resolved chord name).
     */
    fun diatonicChordsForMinor(pitchClass: Int): List<Pair<String, String>> {
        // Relative major is 3 semitones above the minor root
        val relativeMajor = (pitchClass + 3) % Notes.PITCH_CLASS_COUNT
        val degrees = listOf(
            Triple(0, "m", "i"),
            Triple(2, "dim", "ii\u00B0"),
            Triple(3, "", "III"),
            Triple(5, "m", "iv"),
            Triple(7, "m", "v"),
            Triple(8, "", "VI"),
            Triple(10, "", "VII"),
        )
        return degrees.map { (interval, quality, numeral) ->
            val root = (pitchClass + interval) % Notes.PITCH_CLASS_COUNT
            numeral to (Notes.enharmonicForKey(root, relativeMajor) + quality)
        }
    }

    /**
     * Returns the pitch classes of the two closely related keys
     * (adjacent on the Circle of Fifths).
     *
     * @param pitchClass The selected key's pitch class.
     * @return Pair of (counter-clockwise neighbor, clockwise neighbor) pitch classes.
     */
    fun closelyRelatedKeys(pitchClass: Int): Pair<Int, Int> {
        val index = CIRCLE_ORDER.indexOf(pitchClass)
        if (index < 0) return Pair(pitchClass, pitchClass)
        val ccw = CIRCLE_ORDER[(index - 1 + 12) % 12]
        val cw = CIRCLE_ORDER[(index + 1) % 12]
        return Pair(ccw, cw)
    }
}

package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ScaleType

/**
 * Harmonic functions describe the *role* a chord plays within a key.
 *
 * - **Tonic (T)**: Feels stable and resolved — the "home" chord.
 * - **Subdominant (S)**: Creates forward motion — moves away from home.
 * - **Dominant (D)**: Builds tension — wants to resolve back to Tonic.
 *
 * Understanding chord functions is the bridge between knowing *which*
 * chords go together and understanding *why* they go together.
 */
enum class HarmonicFunction(
    val label: String,
    val description: String,
) {
    TONIC("T", "Tonic — feels like home, stable and resolved"),
    SUBDOMINANT("S", "Subdominant — creates forward motion, moves away from home"),
    DOMINANT("D", "Dominant — builds tension, wants to resolve back to Tonic"),
}

/**
 * Determines the harmonic function of a chord degree based on its
 * Roman numeral and the scale type.
 *
 * **Major (Ionian) functions:**
 * - Tonic: I, iii, vi (share notes with I)
 * - Subdominant: ii, IV (share notes with IV)
 * - Dominant: V, vii° (share the leading tone)
 *
 * **Minor (Aeolian) functions:**
 * - Tonic: i, III, VI
 * - Subdominant: ii°, iv
 * - Dominant: V, v, VII
 *
 * **Dorian functions:**
 * - Tonic: i, III
 * - Subdominant: ii, IV
 * - Dominant: v, vi°, VII
 *
 * **Phrygian functions:**
 * - Tonic: i, III, VI
 * - Subdominant: II, iv
 * - Dominant: v°, vii
 *
 * **Lydian functions:**
 * - Tonic: I, iii, vi
 * - Subdominant: II, #iv°
 * - Dominant: V, vii
 *
 * **Mixolydian functions:**
 * - Tonic: I, vi
 * - Subdominant: ii, IV
 * - Dominant: iii°, v, VII
 *
 * **Locrian functions:**
 * - Tonic: i°, iii
 * - Subdominant: II, iv
 * - Dominant: V, VI, vii
 *
 * @param numeral The Roman numeral label of the chord degree.
 * @param scaleType The scale type context.
 * @return The [HarmonicFunction] of the chord degree.
 */
/**
 * Extracts the base Roman numeral from an extended numeral like "Imaj7" or "V7".
 *
 * Matches an optional accidental prefix (`#` sharp or `♭` flat), the Roman
 * numeral itself, and an optional trailing `°` (diminished), then drops the
 * quality tail (e.g., "maj7", "m7", "sus4", "9", "m7♭5"). This keeps harmonic
 * function lookup working for both triad and extended chord numerals, and for
 * chromatic numerals such as "♭VI" or "iim7♭5".
 */
private val BASE_NUMERAL_REGEX = Regex("^[#♭]?[IiVv]+°?")

private fun baseNumeral(numeral: String): String =
    BASE_NUMERAL_REGEX.find(numeral)?.value ?: numeral

private val FUNCTION_MAP: Map<ScaleType, Map<String, HarmonicFunction>> = mapOf(
    ScaleType.MAJOR to mapOf(
        "I" to HarmonicFunction.TONIC, "iii" to HarmonicFunction.TONIC, "vi" to HarmonicFunction.TONIC,
        "ii" to HarmonicFunction.SUBDOMINANT, "IV" to HarmonicFunction.SUBDOMINANT,
        // Borrowed from the parallel minor (Rock Cadence): pre-dominant motion.
        "♭VI" to HarmonicFunction.SUBDOMINANT, "♭VII" to HarmonicFunction.SUBDOMINANT,
        "V" to HarmonicFunction.DOMINANT, "vii°" to HarmonicFunction.DOMINANT,
        // Secondary dominants (V/x), e.g. the Montgomery-Ward circle-of-fifths chain.
        "II" to HarmonicFunction.DOMINANT, "III" to HarmonicFunction.DOMINANT, "VI" to HarmonicFunction.DOMINANT,
    ),
    ScaleType.MINOR to mapOf(
        "i" to HarmonicFunction.TONIC, "III" to HarmonicFunction.TONIC, "VI" to HarmonicFunction.TONIC,
        // Both "ii°" (triad spelling) and "ii" (the half-diminished iim7♭5 spelling) resolve here.
        "ii°" to HarmonicFunction.SUBDOMINANT, "ii" to HarmonicFunction.SUBDOMINANT,
        "iv" to HarmonicFunction.SUBDOMINANT,
        "V" to HarmonicFunction.DOMINANT, "v" to HarmonicFunction.DOMINANT, "VII" to HarmonicFunction.DOMINANT,
    ),
    ScaleType.DORIAN to mapOf(
        "i" to HarmonicFunction.TONIC, "III" to HarmonicFunction.TONIC,
        "ii" to HarmonicFunction.SUBDOMINANT, "IV" to HarmonicFunction.SUBDOMINANT,
        "v" to HarmonicFunction.DOMINANT, "vi°" to HarmonicFunction.DOMINANT, "VII" to HarmonicFunction.DOMINANT,
    ),
    ScaleType.PHRYGIAN to mapOf(
        "i" to HarmonicFunction.TONIC, "III" to HarmonicFunction.TONIC, "VI" to HarmonicFunction.TONIC,
        "II" to HarmonicFunction.SUBDOMINANT, "iv" to HarmonicFunction.SUBDOMINANT,
        "v°" to HarmonicFunction.DOMINANT, "vii" to HarmonicFunction.DOMINANT,
    ),
    ScaleType.LYDIAN to mapOf(
        "I" to HarmonicFunction.TONIC, "iii" to HarmonicFunction.TONIC, "vi" to HarmonicFunction.TONIC,
        "II" to HarmonicFunction.SUBDOMINANT, "#iv°" to HarmonicFunction.SUBDOMINANT,
        "V" to HarmonicFunction.DOMINANT, "vii" to HarmonicFunction.DOMINANT,
    ),
    ScaleType.MIXOLYDIAN to mapOf(
        "I" to HarmonicFunction.TONIC, "vi" to HarmonicFunction.TONIC,
        "ii" to HarmonicFunction.SUBDOMINANT, "IV" to HarmonicFunction.SUBDOMINANT,
        "iii°" to HarmonicFunction.DOMINANT, "v" to HarmonicFunction.DOMINANT, "VII" to HarmonicFunction.DOMINANT,
    ),
    ScaleType.LOCRIAN to mapOf(
        "i°" to HarmonicFunction.TONIC, "iii" to HarmonicFunction.TONIC,
        "II" to HarmonicFunction.SUBDOMINANT, "iv" to HarmonicFunction.SUBDOMINANT,
        "V" to HarmonicFunction.DOMINANT, "VI" to HarmonicFunction.DOMINANT, "vii" to HarmonicFunction.DOMINANT,
    ),
)

fun harmonicFunction(numeral: String, scaleType: ScaleType): HarmonicFunction {
    val base = baseNumeral(numeral)
    return FUNCTION_MAP[scaleType]?.get(base) ?: HarmonicFunction.TONIC
}

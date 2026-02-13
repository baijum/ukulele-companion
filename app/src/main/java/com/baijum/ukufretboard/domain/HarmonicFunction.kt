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
 * Strips quality suffixes (e.g., "maj7", "m7", "sus4", "9") so that harmonic
 * function lookup works for both triad and extended chord numerals.
 */
private val BASE_NUMERAL_REGEX = Regex("^#?[IiVv]+\u00B0?")

private fun baseNumeral(numeral: String): String =
    BASE_NUMERAL_REGEX.find(numeral)?.value ?: numeral

fun harmonicFunction(numeral: String, scaleType: ScaleType): HarmonicFunction {
    val base = baseNumeral(numeral)
    return when {
        // ── Major (Ionian) ──
        scaleType == ScaleType.MAJOR && base in listOf("I", "iii", "vi") ->
            HarmonicFunction.TONIC
        scaleType == ScaleType.MAJOR && base in listOf("ii", "IV") ->
            HarmonicFunction.SUBDOMINANT
        scaleType == ScaleType.MAJOR && base in listOf("V", "vii\u00B0") ->
            HarmonicFunction.DOMINANT

        // ── Minor (Aeolian) ──
        scaleType == ScaleType.MINOR && base in listOf("i", "III", "VI") ->
            HarmonicFunction.TONIC
        scaleType == ScaleType.MINOR && base in listOf("ii\u00B0", "iv") ->
            HarmonicFunction.SUBDOMINANT
        scaleType == ScaleType.MINOR && base in listOf("V", "v", "VII") ->
            HarmonicFunction.DOMINANT

        // ── Dorian ──
        scaleType == ScaleType.DORIAN && base in listOf("i", "III") ->
            HarmonicFunction.TONIC
        scaleType == ScaleType.DORIAN && base in listOf("ii", "IV") ->
            HarmonicFunction.SUBDOMINANT
        scaleType == ScaleType.DORIAN && base in listOf("v", "vi\u00B0", "VII") ->
            HarmonicFunction.DOMINANT

        // ── Phrygian ──
        scaleType == ScaleType.PHRYGIAN && base in listOf("i", "III", "VI") ->
            HarmonicFunction.TONIC
        scaleType == ScaleType.PHRYGIAN && base in listOf("II", "iv") ->
            HarmonicFunction.SUBDOMINANT
        scaleType == ScaleType.PHRYGIAN && base in listOf("v\u00B0", "vii") ->
            HarmonicFunction.DOMINANT

        // ── Lydian ──
        scaleType == ScaleType.LYDIAN && base in listOf("I", "iii", "vi") ->
            HarmonicFunction.TONIC
        scaleType == ScaleType.LYDIAN && base in listOf("II", "#iv\u00B0") ->
            HarmonicFunction.SUBDOMINANT
        scaleType == ScaleType.LYDIAN && base in listOf("V", "vii") ->
            HarmonicFunction.DOMINANT

        // ── Mixolydian ──
        scaleType == ScaleType.MIXOLYDIAN && base in listOf("I", "vi") ->
            HarmonicFunction.TONIC
        scaleType == ScaleType.MIXOLYDIAN && base in listOf("ii", "IV") ->
            HarmonicFunction.SUBDOMINANT
        scaleType == ScaleType.MIXOLYDIAN && base in listOf("iii\u00B0", "v", "VII") ->
            HarmonicFunction.DOMINANT

        // ── Locrian ──
        scaleType == ScaleType.LOCRIAN && base in listOf("i\u00B0", "iii") ->
            HarmonicFunction.TONIC
        scaleType == ScaleType.LOCRIAN && base in listOf("II", "iv") ->
            HarmonicFunction.SUBDOMINANT
        scaleType == ScaleType.LOCRIAN && base in listOf("V", "VI", "vii") ->
            HarmonicFunction.DOMINANT

        // Fallback
        else -> HarmonicFunction.TONIC
    }
}

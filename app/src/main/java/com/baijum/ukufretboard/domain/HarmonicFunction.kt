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
fun harmonicFunction(numeral: String, scaleType: ScaleType): HarmonicFunction = when {
    // ── Major (Ionian) ──
    scaleType == ScaleType.MAJOR && numeral in listOf("I", "iii", "vi") ->
        HarmonicFunction.TONIC
    scaleType == ScaleType.MAJOR && numeral in listOf("ii", "IV") ->
        HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.MAJOR && numeral in listOf("V", "vii\u00B0") ->
        HarmonicFunction.DOMINANT

    // ── Minor (Aeolian) ──
    scaleType == ScaleType.MINOR && numeral in listOf("i", "III", "VI") ->
        HarmonicFunction.TONIC
    scaleType == ScaleType.MINOR && numeral in listOf("ii\u00B0", "iv") ->
        HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.MINOR && numeral in listOf("V", "v", "VII") ->
        HarmonicFunction.DOMINANT

    // ── Dorian ──
    scaleType == ScaleType.DORIAN && numeral in listOf("i", "III") ->
        HarmonicFunction.TONIC
    scaleType == ScaleType.DORIAN && numeral in listOf("ii", "IV") ->
        HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.DORIAN && numeral in listOf("v", "vi\u00B0", "VII") ->
        HarmonicFunction.DOMINANT

    // ── Phrygian ──
    scaleType == ScaleType.PHRYGIAN && numeral in listOf("i", "III", "VI") ->
        HarmonicFunction.TONIC
    scaleType == ScaleType.PHRYGIAN && numeral in listOf("II", "iv") ->
        HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.PHRYGIAN && numeral in listOf("v\u00B0", "vii") ->
        HarmonicFunction.DOMINANT

    // ── Lydian ──
    scaleType == ScaleType.LYDIAN && numeral in listOf("I", "iii", "vi") ->
        HarmonicFunction.TONIC
    scaleType == ScaleType.LYDIAN && numeral in listOf("II", "#iv\u00B0") ->
        HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.LYDIAN && numeral in listOf("V", "vii") ->
        HarmonicFunction.DOMINANT

    // ── Mixolydian ──
    scaleType == ScaleType.MIXOLYDIAN && numeral in listOf("I", "vi") ->
        HarmonicFunction.TONIC
    scaleType == ScaleType.MIXOLYDIAN && numeral in listOf("ii", "IV") ->
        HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.MIXOLYDIAN && numeral in listOf("iii\u00B0", "v", "VII") ->
        HarmonicFunction.DOMINANT

    // ── Locrian ──
    scaleType == ScaleType.LOCRIAN && numeral in listOf("i\u00B0", "iii") ->
        HarmonicFunction.TONIC
    scaleType == ScaleType.LOCRIAN && numeral in listOf("II", "iv") ->
        HarmonicFunction.SUBDOMINANT
    scaleType == ScaleType.LOCRIAN && numeral in listOf("V", "VI", "vii") ->
        HarmonicFunction.DOMINANT

    // Fallback
    else -> HarmonicFunction.TONIC
}

package com.baijum.ukufretboard.data

/**
 * Scale type used for generating diatonic chord progressions.
 */
enum class ScaleType(val label: String) {
    MAJOR("Major"),
    MINOR("Minor"),
    DORIAN("Dorian"),
    PHRYGIAN("Phrygian"),
    LYDIAN("Lydian"),
    MIXOLYDIAN("Mixolydian"),
    LOCRIAN("Locrian"),
}

/**
 * A chord degree within a scale, defining the interval from the root
 * and the quality of the resulting chord.
 *
 * @property interval Semitones from the key root to this degree's root.
 * @property quality The chord quality symbol (e.g., "", "m", "dim").
 * @property numeral The Roman numeral label (e.g., "I", "vi", "vii°").
 */
data class ChordDegree(
    val interval: Int,
    val quality: String,
    val numeral: String,
)

/**
 * A named chord progression pattern.
 *
 * @property name Human-readable name (e.g., "Pop / Four Chords").
 * @property description Brief explanation of when/where this progression is used.
 * @property degrees The sequence of chord degrees in the progression.
 * @property scaleType The scale type this progression is derived from.
 */
data class Progression(
    val name: String,
    val description: String,
    val degrees: List<ChordDegree>,
    val scaleType: ScaleType = ScaleType.MAJOR,
)

/**
 * Library of common chord progressions.
 *
 * Each progression uses scale degree intervals. To resolve for a specific key,
 * add the key's pitch class to each degree's interval (mod 12), then apply
 * the degree's chord quality.
 */
object Progressions {

    // ── Major scale degrees ──
    // I=0, ii=2, iii=4, IV=5, V=7, vi=9, vii°=11
    private val I      = ChordDegree(0,  "",    "I")
    private val ii     = ChordDegree(2,  "m",   "ii")
    private val iii    = ChordDegree(4,  "m",   "iii")
    private val IV     = ChordDegree(5,  "",    "IV")
    private val V      = ChordDegree(7,  "",    "V")
    private val vi     = ChordDegree(9,  "m",   "vi")
    private val viiDim = ChordDegree(11, "dim", "vii\u00B0")

    // ── Minor (Aeolian) scale degrees ──
    // i=0, ii°=2, III=3, iv=5, v=7, VI=8, VII=10
    private val i_m     = ChordDegree(0,  "m",   "i")
    private val iiDim_m = ChordDegree(2,  "dim", "ii\u00B0")
    private val III_m   = ChordDegree(3,  "",    "III")
    private val iv_m    = ChordDegree(5,  "m",   "iv")
    private val v_m     = ChordDegree(7,  "m",   "v")
    private val V_m     = ChordDegree(7,  "",    "V")   // harmonic minor dominant
    private val VI_m    = ChordDegree(8,  "",    "VI")
    private val VII_m   = ChordDegree(10, "",    "VII")

    // ── Dorian scale degrees ──
    // i=0, ii=2, III=3, IV=5, v=7, vi°=9, VII=10
    private val i_d      = ChordDegree(0,  "m",   "i")
    private val ii_d     = ChordDegree(2,  "m",   "ii")
    private val III_d    = ChordDegree(3,  "",    "III")
    private val IV_d     = ChordDegree(5,  "",    "IV")
    private val v_d      = ChordDegree(7,  "m",   "v")
    private val viDim_d  = ChordDegree(9,  "dim", "vi\u00B0")
    private val VII_d    = ChordDegree(10, "",    "VII")

    // ── Phrygian scale degrees ──
    // i=0, II=1, III=3, iv=5, v°=7, VI=8, vii=10
    private val i_p      = ChordDegree(0,  "m",   "i")
    private val II_p     = ChordDegree(1,  "",    "II")
    private val III_p    = ChordDegree(3,  "",    "III")
    private val iv_p     = ChordDegree(5,  "m",   "iv")
    private val vDim_p   = ChordDegree(7,  "dim", "v\u00B0")
    private val VI_p     = ChordDegree(8,  "",    "VI")
    private val vii_p    = ChordDegree(10, "m",   "vii")

    // ── Lydian scale degrees ──
    // I=0, II=2, iii=4, #iv°=6, V=7, vi=9, vii=11
    private val I_l      = ChordDegree(0,  "",    "I")
    private val II_l     = ChordDegree(2,  "",    "II")
    private val iii_l    = ChordDegree(4,  "m",   "iii")
    private val ivDim_l  = ChordDegree(6,  "dim", "#iv\u00B0")
    private val V_l      = ChordDegree(7,  "",    "V")
    private val vi_l     = ChordDegree(9,  "m",   "vi")
    private val vii_l    = ChordDegree(11, "m",   "vii")

    // ── Mixolydian scale degrees ──
    // I=0, ii=2, iii°=4, IV=5, v=7, vi=9, VII=10
    private val I_x      = ChordDegree(0,  "",    "I")
    private val ii_x     = ChordDegree(2,  "m",   "ii")
    private val iiiDim_x = ChordDegree(4,  "dim", "iii\u00B0")
    private val IV_x     = ChordDegree(5,  "",    "IV")
    private val v_x      = ChordDegree(7,  "m",   "v")
    private val vi_x     = ChordDegree(9,  "m",   "vi")
    private val VII_x    = ChordDegree(10, "",    "VII")

    // ── Locrian scale degrees ──
    // i°=0, II=1, iii=3, iv=5, V=6, VI=8, vii=10
    private val iDim_lo  = ChordDegree(0,  "dim", "i\u00B0")
    private val II_lo    = ChordDegree(1,  "",    "II")
    private val iii_lo   = ChordDegree(3,  "m",   "iii")
    private val iv_lo    = ChordDegree(5,  "m",   "iv")
    private val V_lo     = ChordDegree(6,  "",    "V")
    private val VI_lo    = ChordDegree(8,  "",    "VI")
    private val vii_lo   = ChordDegree(10, "m",   "vii")

    // ── Extended chord degrees (Major key) ──
    // Seventh chords
    private val Imaj7   = ChordDegree(0,  "maj7", "Imaj7")
    private val iim7    = ChordDegree(2,  "m7",   "iim7")
    private val iiim7   = ChordDegree(4,  "m7",   "iiim7")
    private val IVmaj7  = ChordDegree(5,  "maj7", "IVmaj7")
    private val V7_maj  = ChordDegree(7,  "7",    "V7")
    private val vim7    = ChordDegree(9,  "m7",   "vim7")
    // Suspended chords
    private val Isus2   = ChordDegree(0,  "sus2", "Isus2")
    private val Isus4   = ChordDegree(0,  "sus4", "Isus4")
    private val Vsus4   = ChordDegree(7,  "sus4", "Vsus4")
    private val IVsus4  = ChordDegree(5,  "sus4", "IVsus4")
    // Extended chords
    private val I6_maj  = ChordDegree(0,  "6",    "I6")
    private val iim9    = ChordDegree(2,  "m9",   "iim9")
    private val V9_maj  = ChordDegree(7,  "9",    "V9")
    // Borrowed chords from parallel minor
    private val bVI     = ChordDegree(8,  "",     "\u266dVI")
    private val bVII    = ChordDegree(10, "",     "\u266dVII")
    // Secondary dominants
    private val II7     = ChordDegree(2,  "7",    "II7")
    private val III7    = ChordDegree(4,  "7",    "III7")
    private val VI7     = ChordDegree(9,  "7",    "VI7")

    // ── Extended chord degrees (Minor key) ──
    private val im7      = ChordDegree(0,  "m7",   "im7")
    private val iim7b5   = ChordDegree(2,  "m7b5", "iim7\u266d5")
    private val ivm7     = ChordDegree(5,  "m7",   "ivm7")
    private val V7_min   = ChordDegree(7,  "7",    "V7")
    private val IIImaj7  = ChordDegree(3,  "maj7", "IIImaj7")
    private val VIImaj7  = ChordDegree(10, "maj7", "VIImaj7")

    val MAJOR_PROGRESSIONS: List<Progression> = listOf(
        Progression(
            name = "Pop / Four Chords",
            description = "The most popular progression in modern pop music. Thousands of hit songs use this exact pattern.",
            degrees = listOf(I, V, vi, IV),
        ),
        Progression(
            name = "Classic Rock",
            description = "The backbone of rock 'n' roll. Simple, powerful, and timeless.",
            degrees = listOf(I, IV, V),
        ),
        Progression(
            name = "50s / Doo-Wop",
            description = "The iconic progression from the 1950s. Still widely used in ballads and slow songs.",
            degrees = listOf(I, vi, IV, V),
        ),
        Progression(
            name = "Folk / Country",
            description = "A warm, comfortable pattern common in folk, country, and campfire songs.",
            degrees = listOf(I, IV, I, V),
        ),
        Progression(
            name = "Sad / Emotional",
            description = "Starts on the minor vi chord, creating a melancholic and emotional mood.",
            degrees = listOf(vi, IV, I, V),
        ),
        Progression(
            name = "Jazz ii-V-I",
            description = "The fundamental jazz cadence. Found in virtually every jazz standard.",
            degrees = listOf(ii, V, I),
        ),
        Progression(
            name = "Reggae",
            description = "A laid-back, rhythmic pattern common in reggae and island music.",
            degrees = listOf(I, IV, V, IV),
        ),
        Progression(
            name = "Pachelbel's Canon",
            description = "Based on the famous Canon in D. Used in countless wedding and pop songs.",
            degrees = listOf(I, V, vi, iii, IV, I, IV, V),
        ),
        // ── Seventh chord progressions ──
        Progression(
            name = "Jazz ii\u2013V\u2013I (7ths)",
            description = "The classic jazz cadence with seventh chords. The foundation of jazz harmony.",
            degrees = listOf(iim7, V7_maj, Imaj7),
        ),
        Progression(
            name = "Jazz Turnaround",
            description = "A four-chord jazz cycle that loops back to the start. Used in countless standards.",
            degrees = listOf(Imaj7, vim7, iim7, V7_maj),
        ),
        Progression(
            name = "Smooth Soul",
            description = "A lush, soulful progression. Seventh chords add warmth and sophistication.",
            degrees = listOf(Imaj7, iiim7, vim7, IVmaj7),
        ),
        // ── Suspended chord progressions ──
        Progression(
            name = "Ambient Pop",
            description = "Suspended chords create an open, airy texture. Great for atmospheric music.",
            degrees = listOf(Isus2, V, vi, IVsus4),
        ),
        Progression(
            name = "Anthem Rock",
            description = "Alternating between suspended and resolved chords builds and releases tension.",
            degrees = listOf(Isus4, I, Vsus4, V),
        ),
        // ── Extended chord progressions ──
        Progression(
            name = "Bossa Nova",
            description = "A smooth Brazilian jazz feel. Minor 9th chords add rich color and movement.",
            degrees = listOf(Imaj7, iim9, V7_maj, Imaj7),
        ),
        Progression(
            name = "Jazz Swing",
            description = "A swinging jazz progression. The 6th chord gives a classic, upbeat swing feel.",
            degrees = listOf(I6_maj, vim7, iim7, V7_maj),
        ),
        // ── Blues progressions ──
        Progression(
            name = "Twelve-Bar Blues",
            description = "The foundational blues form. Used in thousands of blues, rock, and jazz songs. Each degree represents one bar.",
            degrees = listOf(I, I, I, I, IV, IV, I, I, V, IV, I, V),
        ),
        Progression(
            name = "Eight-Bar Blues",
            description = "A compact blues form. \u201cHeartbreak Hotel,\u201d \u201cSitting on Top of the World.\u201d Each degree represents one bar.",
            degrees = listOf(I, V, IV, IV, I, V, I, V),
        ),
        // ── Classic harmonic patterns ──
        Progression(
            name = "Circle Progression",
            description = "A foundational sequence descending in fifths. Smooth harmonic flow found across classical, jazz, and pop.",
            degrees = listOf(vi, ii, V, I),
        ),
        Progression(
            name = "Blues Turnaround",
            description = "A simple rock and blues turnaround used at phrase endings to cycle back to the start.",
            degrees = listOf(V, IV, I),
        ),
        Progression(
            name = "Montgomery-Ward Bridge",
            description = "A classic bridge formula found in countless pop and jazz standards. Named for its pervasive use.",
            degrees = listOf(I, IV, ii, V),
        ),
        Progression(
            name = "Ascending Bass",
            description = "A smooth ascending bass line (1\u20133\u20134\u20135). \u201cCrocodile Rock\u201d by Elton John, \u201cI Feel Fine\u201d by the Beatles.",
            degrees = listOf(I, iii, IV, V),
        ),
        // ── Borrowed chord progressions ──
        Progression(
            name = "Rock Cadence",
            description = "A powerful rock ending using chords borrowed from the parallel minor. \u201cEye of the Tiger,\u201d \u201cLivin\u2019 on a Prayer.\u201d",
            degrees = listOf(bVI, bVII, I),
        ),
        // ── Secondary dominant progressions ──
        Progression(
            name = "Ragtime",
            description = "A chain of secondary dominants descending in fifths. Sophisticated and fun on ukulele.",
            degrees = listOf(III7, VI7, II7, V7_maj),
        ),
    )

    val MINOR_PROGRESSIONS: List<Progression> = listOf(
        Progression(
            name = "Natural Minor",
            description = "A simple descending minor progression. Dark and moody.",
            degrees = listOf(i_m, VII_m, VI_m, V_m),
            scaleType = ScaleType.MINOR,
        ),
        Progression(
            name = "Andalusian Cadence",
            description = "A descending flamenco-style progression. Dramatic and passionate.",
            degrees = listOf(i_m, VII_m, VI_m, V_m),
            scaleType = ScaleType.MINOR,
        ),
        Progression(
            name = "Minor Pop",
            description = "The minor-key version of the four-chord pop progression.",
            degrees = listOf(i_m, VI_m, III_m, VII_m),
            scaleType = ScaleType.MINOR,
        ),
        Progression(
            name = "Minor Blues",
            description = "A simplified minor blues feel. Raw and expressive.",
            degrees = listOf(i_m, iv_m, i_m, V_m),
            scaleType = ScaleType.MINOR,
        ),
        // ── Minor seventh chord progressions ──
        Progression(
            name = "Minor Jazz",
            description = "A minor jazz cycle with seventh chords. The dominant V7 adds harmonic pull back to i.",
            degrees = listOf(im7, ivm7, V7_min, im7),
            scaleType = ScaleType.MINOR,
        ),
        Progression(
            name = "Noir Jazz",
            description = "A dark, cinematic progression. Major 7th chords on III and VII add mysterious color.",
            degrees = listOf(im7, VIImaj7, IIImaj7, V7_min),
            scaleType = ScaleType.MINOR,
        ),
        // ── Historical progressions ──
        Progression(
            name = "Folia",
            description = "One of the oldest known chord progressions (15th century). Dramatic and timeless, used from Baroque to modern film scores.",
            degrees = listOf(i_m, V_m, i_m, VII_m, III_m, VII_m, i_m, V_m, i_m),
            scaleType = ScaleType.MINOR,
        ),
        // ── Minor seventh chord progressions ──
        Progression(
            name = "Minor ii\u2013V\u2013I",
            description = "The minor-key jazz cadence. The half-diminished ii leads through V7 back to the tonic. Essential for jazz in minor keys.",
            degrees = listOf(iim7b5, V7_min, im7),
            scaleType = ScaleType.MINOR,
        ),
    )

    val DORIAN_PROGRESSIONS: List<Progression> = listOf(
        Progression(
            name = "Dorian Vamp",
            description = "The classic i\u2013IV modal vamp. Think \"So What\" by Miles Davis or \"Oye Como Va\" by Santana.",
            degrees = listOf(i_d, IV_d),
            scaleType = ScaleType.DORIAN,
        ),
        Progression(
            name = "Dorian Funk",
            description = "A groovy funk pattern. The IV chord gives Dorian its bright, jazzy character.",
            degrees = listOf(i_d, IV_d, v_d, IV_d),
            scaleType = ScaleType.DORIAN,
        ),
        Progression(
            name = "Dorian Soul",
            description = "A soulful cycle using the signature raised 6th. Warm and expressive.",
            degrees = listOf(i_d, VII_d, IV_d, i_d),
            scaleType = ScaleType.DORIAN,
        ),
        Progression(
            name = "Dorian Jazz",
            description = "An ascending modal jazz pattern exploring the full Dorian color.",
            degrees = listOf(i_d, ii_d, III_d, IV_d),
            scaleType = ScaleType.DORIAN,
        ),
    )

    val PHRYGIAN_PROGRESSIONS: List<Progression> = listOf(
        Progression(
            name = "Phrygian Flamenco",
            description = "The iconic flamenco cadence. The major II chord a half-step above the root creates dramatic tension.",
            degrees = listOf(i_p, II_p, III_p, II_p),
            scaleType = ScaleType.PHRYGIAN,
        ),
        Progression(
            name = "Phrygian Vamp",
            description = "A hypnotic two-chord oscillation. The II resolves down by a semitone to i.",
            degrees = listOf(i_p, II_p, i_p),
            scaleType = ScaleType.PHRYGIAN,
        ),
        Progression(
            name = "Phrygian Dark",
            description = "A brooding descent through the Phrygian mode. Heavy and atmospheric.",
            degrees = listOf(i_p, II_p, vii_p, i_p),
            scaleType = ScaleType.PHRYGIAN,
        ),
    )

    val LYDIAN_PROGRESSIONS: List<Progression> = listOf(
        Progression(
            name = "Lydian Float",
            description = "A dreamy two-chord shimmer. The major II chord gives Lydian its floating, ethereal quality.",
            degrees = listOf(I_l, II_l),
            scaleType = ScaleType.LYDIAN,
        ),
        Progression(
            name = "Lydian Shimmer",
            description = "A sparkling cycle emphasizing the raised 4th. Common in film scores and ambient music.",
            degrees = listOf(I_l, vii_l, II_l, I_l),
            scaleType = ScaleType.LYDIAN,
        ),
        Progression(
            name = "Lydian Ascent",
            description = "A bright, upward progression. The II and iii create a sense of wonder and openness.",
            degrees = listOf(I_l, II_l, iii_l, II_l),
            scaleType = ScaleType.LYDIAN,
        ),
    )

    val MIXOLYDIAN_PROGRESSIONS: List<Progression> = listOf(
        Progression(
            name = "Mixolydian Rock",
            description = "A powerful rock pattern. The flat VII gives a bluesy, earthy sound. Think classic rock anthems.",
            degrees = listOf(I_x, VII_x, IV_x, I_x),
            scaleType = ScaleType.MIXOLYDIAN,
        ),
        Progression(
            name = "Mixolydian Groove",
            description = "A laid-back groove cycling through IV and VII. Great for jam sessions and blues-rock.",
            degrees = listOf(I_x, IV_x, VII_x, IV_x),
            scaleType = ScaleType.MIXOLYDIAN,
        ),
        Progression(
            name = "Mixolydian Blues",
            description = "A bluesy shuffle pattern. The dominant quality of every chord creates a raw, rootsy feel.",
            degrees = listOf(I_x, ii_x, VII_x, IV_x),
            scaleType = ScaleType.MIXOLYDIAN,
        ),
    )

    val LOCRIAN_PROGRESSIONS: List<Progression> = listOf(
        Progression(
            name = "Locrian Tension",
            description = "An unsettled, dissonant progression. The diminished tonic creates constant instability.",
            degrees = listOf(iDim_lo, II_lo, iii_lo),
            scaleType = ScaleType.LOCRIAN,
        ),
        Progression(
            name = "Locrian Drift",
            description = "A restless journey through the darkest mode. Used in experimental and metal music.",
            degrees = listOf(iDim_lo, II_lo, V_lo, VI_lo),
            scaleType = ScaleType.LOCRIAN,
        ),
    )

    /**
     * Returns progressions for the given scale type.
     */
    fun forScale(scaleType: ScaleType): List<Progression> = when (scaleType) {
        ScaleType.MAJOR -> MAJOR_PROGRESSIONS
        ScaleType.MINOR -> MINOR_PROGRESSIONS
        ScaleType.DORIAN -> DORIAN_PROGRESSIONS
        ScaleType.PHRYGIAN -> PHRYGIAN_PROGRESSIONS
        ScaleType.LYDIAN -> LYDIAN_PROGRESSIONS
        ScaleType.MIXOLYDIAN -> MIXOLYDIAN_PROGRESSIONS
        ScaleType.LOCRIAN -> LOCRIAN_PROGRESSIONS
    }

    /**
     * Returns the diatonic chord degrees for a scale type.
     * Used by the custom progression builder to show available chords.
     */
    fun diatonicDegrees(scaleType: ScaleType): List<ChordDegree> = when (scaleType) {
        ScaleType.MAJOR -> listOf(I, ii, iii, IV, V, vi, viiDim)
        ScaleType.MINOR -> listOf(i_m, iiDim_m, III_m, iv_m, v_m, V_m, VI_m, VII_m)
        ScaleType.DORIAN -> listOf(i_d, ii_d, III_d, IV_d, v_d, viDim_d, VII_d)
        ScaleType.PHRYGIAN -> listOf(i_p, II_p, III_p, iv_p, vDim_p, VI_p, vii_p)
        ScaleType.LYDIAN -> listOf(I_l, II_l, iii_l, ivDim_l, V_l, vi_l, vii_l)
        ScaleType.MIXOLYDIAN -> listOf(I_x, ii_x, iiiDim_x, IV_x, v_x, vi_x, VII_x)
        ScaleType.LOCRIAN -> listOf(iDim_lo, II_lo, iii_lo, iv_lo, V_lo, VI_lo, vii_lo)
    }
}

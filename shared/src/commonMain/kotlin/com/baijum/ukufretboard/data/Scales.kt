package com.baijum.ukufretboard.data

/**
 * Categories for grouping scales in the practice UI.
 *
 * A scale can belong to multiple categories (e.g., Harmonic Minor
 * is both a diatonic variant and commonly used in jazz).
 */
enum class ScaleCategory(val label: String) {
    BASIC("Basic"),
    BLUES("Blues"),
    MODES("Modes"),
    JAZZ("Jazz"),
    HINDUSTANI("Hindustani"),
    CARNATIC("Carnatic"),
    WORLD("World"),
    SYMMETRIC("Symmetric"),
}

/**
 * A musical scale defined by its interval pattern.
 *
 * @property name Human-readable name.
 * @property intervals List of semitone intervals from the root (always starts with 0).
 * @property categories The categories this scale belongs to for filtering in practice UI.
 */
data class Scale(
    val name: String,
    val intervals: List<Int>,
    val categories: Set<ScaleCategory> = emptySet(),
)

/**
 * Library of musical scales for fretboard overlay and scale practice.
 */
object Scales {

    val ALL: List<Scale> = listOf(
        // ── Basic / Diatonic scales ──
        Scale("Major",             listOf(0, 2, 4, 5, 7, 9, 11),
            setOf(ScaleCategory.BASIC)),
        Scale("Natural Minor",     listOf(0, 2, 3, 5, 7, 8, 10),
            setOf(ScaleCategory.BASIC)),
        Scale("Harmonic Minor",    listOf(0, 2, 3, 5, 7, 8, 11),
            setOf(ScaleCategory.BASIC, ScaleCategory.JAZZ)),
        Scale("Melodic Minor",     listOf(0, 2, 3, 5, 7, 9, 11),
            setOf(ScaleCategory.BASIC, ScaleCategory.JAZZ)),
        // ── Pentatonic & Blues ──
        Scale("Pentatonic Major",  listOf(0, 2, 4, 7, 9),
            setOf(ScaleCategory.BASIC)),
        Scale("Pentatonic Minor",  listOf(0, 3, 5, 7, 10),
            setOf(ScaleCategory.BASIC)),
        Scale("Blues",             listOf(0, 3, 5, 6, 7, 10),
            setOf(ScaleCategory.BLUES)),
        Scale("Major Blues",       listOf(0, 2, 3, 4, 7, 9),
            setOf(ScaleCategory.BLUES)),
        // ── Modes ──
        Scale("Dorian",            listOf(0, 2, 3, 5, 7, 9, 10),
            setOf(ScaleCategory.MODES)),
        Scale("Phrygian",          listOf(0, 1, 3, 5, 7, 8, 10),
            setOf(ScaleCategory.MODES)),
        Scale("Lydian",            listOf(0, 2, 4, 6, 7, 9, 11),
            setOf(ScaleCategory.MODES)),
        Scale("Mixolydian",        listOf(0, 2, 4, 5, 7, 9, 10),
            setOf(ScaleCategory.MODES)),
        Scale("Locrian",           listOf(0, 1, 3, 5, 6, 8, 10),
            setOf(ScaleCategory.MODES)),
        // ── Jazz scales ──
        Scale("Bebop Dominant",    listOf(0, 2, 4, 5, 7, 9, 10, 11),
            setOf(ScaleCategory.JAZZ)),
        Scale("Bebop Major",       listOf(0, 2, 4, 5, 7, 8, 9, 11),
            setOf(ScaleCategory.JAZZ)),
        Scale("Altered",           listOf(0, 1, 3, 4, 6, 8, 10),
            setOf(ScaleCategory.JAZZ)),
        Scale("Lydian Dominant",   listOf(0, 2, 4, 6, 7, 9, 10),
            setOf(ScaleCategory.JAZZ)),
        Scale("Diminished HW",     listOf(0, 1, 3, 4, 6, 7, 9, 10),
            setOf(ScaleCategory.JAZZ, ScaleCategory.SYMMETRIC)),
        Scale("Diminished WH",     listOf(0, 2, 3, 5, 6, 8, 9, 11),
            setOf(ScaleCategory.JAZZ, ScaleCategory.SYMMETRIC)),
        // ── World scales ──
        Scale("Spanish Phrygian",  listOf(0, 1, 4, 5, 7, 8, 10),
            setOf(ScaleCategory.WORLD)),
        Scale("Hirajoshi",         listOf(0, 2, 3, 7, 8),
            setOf(ScaleCategory.WORLD)),
        Scale("Hungarian Minor",   listOf(0, 2, 3, 6, 7, 8, 11),
            setOf(ScaleCategory.WORLD)),
        // ── Hindustani scales (Thaats & Ragas) ──
        Scale("Bhairav",           listOf(0, 1, 4, 5, 7, 8, 11),
            setOf(ScaleCategory.HINDUSTANI, ScaleCategory.CARNATIC)),  // = Mayamalavagowla
        Scale("Marwa",             listOf(0, 1, 4, 6, 7, 9, 11),
            setOf(ScaleCategory.HINDUSTANI)),
        Scale("Poorvi",            listOf(0, 1, 4, 6, 7, 8, 11),
            setOf(ScaleCategory.HINDUSTANI)),
        Scale("Todi",              listOf(0, 1, 3, 6, 7, 8, 11),
            setOf(ScaleCategory.HINDUSTANI)),
        Scale("Malkauns",          listOf(0, 3, 5, 8, 10),
            setOf(ScaleCategory.HINDUSTANI, ScaleCategory.CARNATIC)),  // = Hindolam
        Scale("Bhimpalasi",        listOf(0, 3, 5, 7, 9, 10),
            setOf(ScaleCategory.HINDUSTANI)),
        Scale("Durga",             listOf(0, 2, 5, 7, 9),
            setOf(ScaleCategory.HINDUSTANI)),
        // ── Carnatic scales (Melakarta & Janya Ragas) ──
        Scale("Charukeshi",        listOf(0, 2, 4, 5, 7, 8, 10),
            setOf(ScaleCategory.CARNATIC)),
        Scale("Simhendramadhyamam", listOf(0, 2, 3, 6, 7, 8, 11),
            setOf(ScaleCategory.CARNATIC)),
        Scale("Shanmukhapriya",    listOf(0, 2, 3, 6, 7, 8, 10),
            setOf(ScaleCategory.CARNATIC)),
        Scale("Hamsadhwani",       listOf(0, 2, 4, 7, 11),
            setOf(ScaleCategory.CARNATIC)),
        Scale("Shivaranjani",      listOf(0, 2, 3, 7, 9),
            setOf(ScaleCategory.CARNATIC, ScaleCategory.HINDUSTANI)),
        Scale("Madhyamavati",      listOf(0, 2, 5, 7, 10),
            setOf(ScaleCategory.CARNATIC)),
        // ── Symmetric scales ──
        Scale("Whole Tone",        listOf(0, 2, 4, 6, 8, 10),
            setOf(ScaleCategory.SYMMETRIC)),
        Scale("Chromatic",         listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
            setOf(ScaleCategory.SYMMETRIC)),
    )

    /**
     * Returns scales belonging to the given category.
     *
     * @param category The category to filter by, or null for all scales.
     */
    fun forCategory(category: ScaleCategory?): List<Scale> =
        if (category == null) ALL else ALL.filter { category in it.categories }

    /**
     * Computes the set of pitch classes in a scale for a given root.
     *
     * @param root The root pitch class (0–11).
     * @param scale The scale definition.
     * @return A set of pitch classes (0–11) belonging to the scale.
     */
    fun scaleNotes(root: Int, scale: Scale): Set<Int> =
        scale.intervals.map { (root + it) % Notes.PITCH_CLASS_COUNT }.toSet()
}

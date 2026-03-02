package com.baijum.ukufretboard.data

/**
 * Categories of chord types, used to organize the chord library UI.
 *
 * @property label The human-readable label shown in the UI.
 */
enum class ChordCategory(val label: String) {
    TRIAD("Triad"),
    SEVENTH("Seventh"),
    SUSPENDED("Suspended"),
    EXTENDED("Extended"),
}

/**
 * Represents a chord type defined by its interval structure relative to the root note.
 *
 * In music theory, a chord is defined by the set of intervals (in semitones)
 * from its root note. For example, a major triad has intervals {0, 4, 7},
 * meaning: root, major third (4 semitones), perfect fifth (7 semitones).
 *
 * @property symbol The display suffix appended to the root note name (e.g., "" for major,
 *   "m" for minor, "7" for dominant seventh).
 * @property quality A human-readable description of the chord quality
 *   (e.g., "Major", "Minor 7th").
 * @property category The [ChordCategory] this formula belongs to.
 * @property intervals The set of semitone intervals from the root that define this chord type.
 * @property omittable Intervals that may be dropped when there are fewer strings than notes
 *   (e.g., the perfect fifth in a 9th chord on a 4-string ukulele). Empty for chords
 *   with 4 or fewer notes.
 * @property aliases Alternate notational symbols for this chord type used in different
 *   musical traditions (e.g., "°" for diminished, "Δ7" for major seventh). These are
 *   suffixes only — the root note is prepended at display time.
 */
data class ChordFormula(
    val symbol: String,
    val quality: String,
    val category: ChordCategory,
    val intervals: Set<Int>,
    val omittable: Set<Int> = emptySet(),
    val aliases: List<String> = emptyList(),
)

/**
 * All supported chord formulas used for chord detection and the chord library.
 *
 * The [ALL] list is ordered by detection priority: simpler triads appear first,
 * followed by extended chords. When multiple formulas could match (e.g., a
 * dominant 7th chord contains a major triad), the first match in this list
 * is preferred during detection.
 *
 * The [BY_CATEGORY] map groups formulas for the chord library UI.
 */
object ChordFormulas {

    val ALL: List<ChordFormula> = listOf(
        // --- Triads ---
        ChordFormula(
            symbol = "",
            quality = "Major",
            category = ChordCategory.TRIAD,
            intervals = setOf(0, 4, 7),
            aliases = listOf("M", "maj", "ma"),
        ),
        ChordFormula(
            symbol = "m",
            quality = "Minor",
            category = ChordCategory.TRIAD,
            intervals = setOf(0, 3, 7),
            aliases = listOf("min", "mi", "\u2212", "-"),
        ),
        ChordFormula(
            symbol = "dim",
            quality = "Diminished",
            category = ChordCategory.TRIAD,
            intervals = setOf(0, 3, 6),
            aliases = listOf("\u00B0"),
        ),
        ChordFormula(
            symbol = "aug",
            quality = "Augmented",
            category = ChordCategory.TRIAD,
            intervals = setOf(0, 4, 8),
            aliases = listOf("+"),
        ),
        // --- Suspended ---
        ChordFormula(
            symbol = "sus2",
            quality = "Suspended 2nd",
            category = ChordCategory.SUSPENDED,
            intervals = setOf(0, 2, 7),
        ),
        ChordFormula(
            symbol = "sus4",
            quality = "Suspended 4th",
            category = ChordCategory.SUSPENDED,
            intervals = setOf(0, 5, 7),
        ),
        ChordFormula(
            symbol = "7sus4",
            quality = "7th Suspended 4th",
            category = ChordCategory.SUSPENDED,
            intervals = setOf(0, 5, 7, 10),
        ),
        // --- Seventh chords ---
        ChordFormula(
            symbol = "7",
            quality = "Dominant 7th",
            category = ChordCategory.SEVENTH,
            intervals = setOf(0, 4, 7, 10),
            aliases = listOf("dom7"),
        ),
        ChordFormula(
            symbol = "m7",
            quality = "Minor 7th",
            category = ChordCategory.SEVENTH,
            intervals = setOf(0, 3, 7, 10),
            aliases = listOf("min7", "mi7", "\u22127", "-7"),
        ),
        ChordFormula(
            symbol = "maj7",
            quality = "Major 7th",
            category = ChordCategory.SEVENTH,
            intervals = setOf(0, 4, 7, 11),
            aliases = listOf("M7", "\u03947", "^7", "ma7"),
        ),
        ChordFormula(
            symbol = "aug7",
            quality = "Augmented 7th",
            category = ChordCategory.SEVENTH,
            intervals = setOf(0, 4, 8, 10),
            aliases = listOf("+7"),
        ),
        ChordFormula(
            symbol = "dim7",
            quality = "Diminished 7th",
            category = ChordCategory.SEVENTH,
            intervals = setOf(0, 3, 6, 9),
            aliases = listOf("\u00B07"),
        ),
        ChordFormula(
            symbol = "m7b5",
            quality = "Half-diminished 7th",
            category = ChordCategory.SEVENTH,
            intervals = setOf(0, 3, 6, 10),
            aliases = listOf("\u00F87", "\u00F8"),
        ),
        ChordFormula(
            symbol = "m(Maj7)",
            quality = "Minor Major 7th",
            category = ChordCategory.SEVENTH,
            intervals = setOf(0, 3, 7, 11),
            aliases = listOf("mM7", "m\u03947", "-M7", "-Maj7"),
        ),
        // --- Extended chords ---
        ChordFormula(
            symbol = "6",
            quality = "Major 6th",
            category = ChordCategory.EXTENDED,
            intervals = setOf(0, 4, 7, 9),
        ),
        ChordFormula(
            symbol = "m6",
            quality = "Minor 6th",
            category = ChordCategory.EXTENDED,
            intervals = setOf(0, 3, 7, 9),
            aliases = listOf("-6"),
        ),
        ChordFormula(
            symbol = "9",
            quality = "Dominant 9th",
            category = ChordCategory.EXTENDED,
            intervals = setOf(0, 2, 4, 7, 10),
            omittable = setOf(7),
        ),
        ChordFormula(
            symbol = "m9",
            quality = "Minor 9th",
            category = ChordCategory.EXTENDED,
            intervals = setOf(0, 2, 3, 7, 10),
            omittable = setOf(7),
            aliases = listOf("-9"),
        ),
        ChordFormula(
            symbol = "add9",
            quality = "Added 9th",
            category = ChordCategory.EXTENDED,
            intervals = setOf(0, 2, 4, 7),
        ),
    )

    /**
     * Formulas grouped by [ChordCategory] for the chord library UI.
     * Preserves the ordering from [ALL] within each category.
     */
    val BY_CATEGORY: Map<ChordCategory, List<ChordFormula>> =
        ALL.groupBy { it.category }
}

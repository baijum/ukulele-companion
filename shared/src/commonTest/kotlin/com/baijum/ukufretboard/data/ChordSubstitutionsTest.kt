package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.domain.ChordNameParser
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the [ChordSubstitutions] lesson data.
 *
 * This is educational content rendered verbatim, so a wrong interval or a
 * misnamed shared note teaches the wrong theory without any runtime symptom.
 * Each assertion recomputes the claim from the chord formulas rather than
 * trusting the prose.
 */
class ChordSubstitutionsTest {
    /** The numerals used by categories 1 and 2, spelled in the key of C. */
    private val numeralsInC =
        mapOf(
            "I" to "C",
            "ii" to "Dm",
            "iii" to "Em",
            "IV" to "F",
            "V" to "G",
            "vi" to "Am",
            "vii°" to "Bdim",
        )

    private val placeholder = "—"

    private fun normalize(name: String) = name.replace('♯', '#').replace('♭', 'b').trim()

    private fun rootOf(chordName: String): Int? = ChordNameParser.parse(normalize(chordName))?.rootPitchClass

    /** Sounding pitch classes of a chord named in full (e.g. "Dm", "G7"). */
    private fun tonesOf(chordName: String): Set<Int>? {
        val parsed = ChordNameParser.parse(normalize(chordName)) ?: return null
        return parsed.formula.intervals
            .map { (parsed.rootPitchClass + it) % 12 }
            .toSet()
    }

    /**
     * Resolves the concrete chord a table cell refers to. The cells come in four
     * shapes across the five categories: a bare numeral ("IV"), a numeral with the
     * chord in parentheses ("before V (G)"), a chord with the numeral in
     * parentheses ("G7 (V7)"), and an equation ("V/V = D7"). Returns null for the
     * em-dash placeholder.
     */
    private fun chordFor(field: String): String? {
        val trimmed = field.trim()
        if (trimmed == placeholder) return null
        if ("=" in trimmed) return chordFor(trimmed.substringAfter("="))
        val beforeParens = trimmed.substringBefore("(").trim()
        if (beforeParens.isNotEmpty() && parses(beforeParens)) return beforeParens
        Regex("\\(([^)]+)\\)").find(trimmed)?.groupValues?.get(1)?.let {
            if (parses(it)) return it.trim()
        }
        numeralsInC[trimmed]?.let { return it }
        return trimmed.takeIf { parses(it) }
    }

    private fun parses(name: String) = ChordNameParser.parse(normalize(name)) != null

    private fun notesNamedIn(sharedNotes: String): List<Int>? {
        if (sharedNotes == placeholder) return null
        val names = Regex("[A-G][#b♯♭]?").findAll(sharedNotes.substringBefore("(")).map { it.value }
        val pitches = names.mapNotNull { rootOf(it) }.toList()
        return pitches.takeIf { it.isNotEmpty() }
    }

    private fun rowsOf(index: Int) = ChordSubstitutions.CATEGORIES[index].substitutions

    // ── Structure ────────────────────────────────────────────────────

    @Test
    fun categoryTitlesAreNumberedSequentiallyFromOne() {
        ChordSubstitutions.CATEGORIES.forEachIndexed { index, category ->
            assertTrue(
                category.title.startsWith("${index + 1}."),
                "category ${index + 1} is titled '${category.title}'",
            )
        }
    }

    @Test
    fun everyCategoryHasNonBlankTitleExplanationAndSubstitutions() {
        assertTrue(ChordSubstitutions.CATEGORIES.isNotEmpty(), "no substitution categories")
        for (category in ChordSubstitutions.CATEGORIES) {
            assertTrue(category.title.isNotBlank(), "blank category title")
            assertTrue(category.explanation.isNotBlank(), "blank explanation in ${category.title}")
            assertTrue(
                category.substitutions.isNotEmpty(),
                "${category.title} lists no substitutions",
            )
        }
    }

    @Test
    fun everySubstitutionFieldIsNonBlank() {
        for (category in ChordSubstitutions.CATEGORIES) {
            for (row in category.substitutions) {
                assertTrue(row.original.isNotBlank(), "blank original in ${category.title}")
                assertTrue(row.substitute.isNotBlank(), "blank substitute in ${category.title}")
                assertTrue(row.sharedNotes.isNotBlank(), "blank sharedNotes in ${category.title}")
                assertTrue(row.exampleInC.isNotBlank(), "blank exampleInC in ${category.title}")
            }
        }
    }

    @Test
    fun emptyFieldsUseTheEmDashPlaceholderNotAHyphenOrBlank() {
        // The UI renders these directly; a bare "-" reads as a typo in the table.
        for (category in ChordSubstitutions.CATEGORIES) {
            for (row in category.substitutions) {
                for (field in listOf(row.original, row.substitute, row.sharedNotes)) {
                    assertTrue(field != "-" && field != "", "use $placeholder, not '$field'")
                }
            }
        }
    }

    @Test
    fun noDuplicateSubstitutionRowsWithinACategory() {
        for (category in ChordSubstitutions.CATEGORIES) {
            val pairs = category.substitutions.map { it.original to it.substitute }
            assertEquals(
                pairs.size,
                pairs.toSet().size,
                "${category.title} repeats a substitution: $pairs",
            )
        }
    }

    @Test
    fun everyNumeralUsedByTheFirstTwoCategoriesIsKnown() {
        for (index in 0..1) {
            for (row in rowsOf(index)) {
                assertNotNull(chordFor(row.original), "unknown numeral: ${row.original}")
                assertNotNull(chordFor(row.substitute), "unknown numeral: ${row.substitute}")
            }
        }
    }

    // ── Interval claims ──────────────────────────────────────────────

    @Test
    fun diatonicSubstitutionPairsShareExactlyTwoOfThreeNotes() {
        for (row in rowsOf(0)) {
            val original = tonesOf(chordFor(row.original)!!)!!
            val substitute = tonesOf(chordFor(row.substitute)!!)!!
            assertEquals(
                2,
                original.intersect(substitute).size,
                "${row.original} -> ${row.substitute} does not share two notes",
            )
        }
    }

    @Test
    fun relativeSwapsAreThreeSemitonesApart() {
        for (row in rowsOf(1)) {
            val from = rootOf(chordFor(row.original)!!)!!
            val to = rootOf(chordFor(row.substitute)!!)!!
            val distance = minOf((to - from + 12) % 12, (from - to + 12) % 12)
            assertEquals(3, distance, "${row.original} -> ${row.substitute} is not a relative swap")
        }
    }

    @Test
    fun tritoneSubstitutesAreSixSemitonesFromTheirOriginal() {
        for (row in rowsOf(2)) {
            val from = rootOf(chordFor(row.original)!!)!!
            val to = rootOf(chordFor(row.substitute)!!)!!
            assertEquals(
                6,
                (to - from + 12) % 12,
                "${row.original} -> ${row.substitute} is not a tritone apart",
            )
        }
    }

    @Test
    fun tritoneSubstitutesShareTheTritoneOfTheOriginal() {
        // The substitution works precisely because both dominants contain the
        // same third and seventh, so the shared pair must itself be a tritone.
        for (row in rowsOf(2)) {
            val original = tonesOf(chordFor(row.original)!!)!!
            val substitute = tonesOf(chordFor(row.substitute)!!)!!
            val shared = original.intersect(substitute)
            assertEquals(2, shared.size, "${row.original} -> ${row.substitute} shares ${shared.size} notes")
            val (a, b) = shared.toList()
            assertEquals(6, (a - b + 12) % 12, "the shared pair is not a tritone: $shared")
        }
    }

    @Test
    fun secondaryDominantsSitAFifthAboveTheChordTheyTonicize() {
        for (row in rowsOf(4)) {
            val target = rootOf(chordFor(row.original)!!)!!
            val dominant = rootOf(chordFor(row.substitute)!!)!!
            assertEquals(
                7,
                (dominant - target + 12) % 12,
                "${row.substitute} is not the dominant of ${row.original}",
            )
        }
    }

    @Test
    fun secondaryDominantsAreDominantSeventhChords() {
        for (row in rowsOf(4)) {
            val chord = chordFor(row.substitute)!!
            val tones = tonesOf(chord)!!
            val root = rootOf(chord)!!
            val intervals = tones.map { (it - root + 12) % 12 }.toSet()
            assertTrue(
                intervals.containsAll(setOf(0, 4, 10)),
                "$chord is not a dominant seventh (intervals $intervals)",
            )
        }
    }

    @Test
    fun modalInterchangeChordsAreBorrowedFromTheParallelMinor() {
        // Every category-4 substitute must exist in C natural minor.
        val cMinorPitches = setOf(0, 2, 3, 5, 7, 8, 10)
        for (row in rowsOf(3)) {
            val chord = chordFor(row.substitute) ?: continue
            val tones = tonesOf(chord) ?: continue
            assertTrue(
                cMinorPitches.containsAll(tones),
                "$chord is not diatonic to C minor (tones $tones)",
            )
        }
    }

    // ── Shared-note claims ───────────────────────────────────────────

    @Test
    @Ignore // Enabled by "Fix: Correct the shared notes in three chord substitutions".
    fun everyNamedSharedNoteIsPresentInBothChords() {
        for (category in ChordSubstitutions.CATEGORIES) {
            for (row in category.substitutions) {
                val named = notesNamedIn(row.sharedNotes) ?: continue
                val original = chordFor(row.original)?.let { tonesOf(it) } ?: continue
                val substitute = chordFor(row.substitute)?.let { tonesOf(it) } ?: continue
                for (note in named) {
                    assertTrue(
                        note in original && note in substitute,
                        "${category.title}: ${row.original} -> ${row.substitute} claims to share " +
                            "'${row.sharedNotes}', but pitch class $note is not in both chords",
                    )
                }
            }
        }
    }

    @Test
    @Ignore // Enabled by "Fix: Correct the shared notes in three chord substitutions".
    fun diatonicAndRelativeSharedNotesAreExactlyTheTriadIntersection() {
        for (index in 0..1) {
            for (row in rowsOf(index)) {
                val named = notesNamedIn(row.sharedNotes)?.toSet() ?: continue
                val original = tonesOf(chordFor(row.original)!!)!!
                val substitute = tonesOf(chordFor(row.substitute)!!)!!
                assertEquals(
                    original.intersect(substitute),
                    named,
                    "${row.original} -> ${row.substitute} names the wrong shared notes " +
                        "('${row.sharedNotes}')",
                )
            }
        }
    }
}

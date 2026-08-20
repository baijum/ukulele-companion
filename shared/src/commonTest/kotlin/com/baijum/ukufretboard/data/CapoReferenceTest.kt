package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.domain.ChordNameParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the [CapoReference] lesson data.
 *
 * Every table in the Capo Guide states a music-theory relationship — this shape
 * at this fret sounds this key — and the app renders those claims verbatim.
 * A typo in a fret string or a sounding key teaches the wrong thing with no
 * runtime symptom, so the assertions here recompute each claim rather than
 * checking that fields are merely non-blank.
 */
class CapoReferenceTest {
    /** The guide writes accidentals with the typographic sharp and flat signs. */
    private fun normalize(name: String) = name.replace('♯', '#').replace('♭', 'b').trim()

    /** "C♯/D♭" lists one pitch under two spellings; either may be used. */
    private fun rootsOf(keyName: String): Set<Int> =
        keyName.split("/").mapNotNull { ChordNameParser.parse(normalize(it))?.rootPitchClass }.toSet()

    private fun rootOf(chordName: String): Int? = ChordNameParser.parse(normalize(chordName))?.rootPitchClass

    /** Sounding pitch classes of an open-position shape in standard tuning. */
    private fun tonesOf(fretString: String): Set<Int> =
        fretString
            .mapIndexed { string, fret ->
                (CapoReference.STANDARD_OPEN_PITCHES[string] + fret.digitToInt()) % 12
            }.toSet()

    // ── Tuning agreement ─────────────────────────────────────────────

    @Test
    fun standardOpenPitchesMatchHighGTuning() {
        // Two copies of the same fact; this fails if either drifts.
        assertEquals(
            UkuleleTuning.HIGH_G.pitchClasses,
            CapoReference.STANDARD_OPEN_PITCHES,
            "the capo guide disagrees with the tuning table",
        )
    }

    @Test
    fun stringNamesMatchHighGTuning() {
        assertEquals(UkuleleTuning.HIGH_G.stringNames, CapoReference.STRING_NAMES)
    }

    // ── Common positions ─────────────────────────────────────────────

    @Test
    fun everyCommonPositionSoundsItsShapeKeyTransposedByTheCapoFret() {
        for (position in CapoReference.COMMON_POSITIONS) {
            val shapeRoot = rootOf(position.shapeKey.substringBefore(" "))
            assertNotNull(shapeRoot, "unparseable shape key: ${position.shapeKey}")
            val expected = (shapeRoot + position.capoFret) % 12
            assertTrue(
                expected in rootsOf(position.soundingKey),
                "${position.shapeKey} at fret ${position.capoFret} sounds pitch class " +
                    "$expected, not ${position.soundingKey}",
            )
        }
    }

    @Test
    fun commonPositionExampleChordsStartOnTheSoundingKey() {
        for (position in CapoReference.COMMON_POSITIONS) {
            val first = position.exampleChords.split(",").first()
            val firstRoot = rootOf(first)
            assertNotNull(firstRoot, "unparseable example chord: $first")
            assertTrue(
                firstRoot in rootsOf(position.soundingKey),
                "${position.exampleChords} does not begin on ${position.soundingKey}",
            )
        }
    }

    @Test
    fun commonPositionExampleChordsAreTheOneFourAndFiveOfTheSoundingKey() {
        for (position in CapoReference.COMMON_POSITIONS) {
            val roots = position.exampleChords.split(",").mapNotNull { rootOf(it) }
            assertEquals(3, roots.size, "expected three example chords: ${position.exampleChords}")
            assertEquals(
                5,
                (roots[1] - roots[0] + 12) % 12,
                "${position.exampleChords}: second chord is not the IV",
            )
            assertEquals(
                7,
                (roots[2] - roots[0] + 12) % 12,
                "${position.exampleChords}: third chord is not the V",
            )
        }
    }

    @Test
    fun commonPositionCapoFretsArePlayable() {
        for (position in CapoReference.COMMON_POSITIONS) {
            assertTrue(
                position.capoFret in 1..12,
                "capo fret ${position.capoFret} is off the neck",
            )
        }
    }

    @Test
    fun commonPositionsAreUniqueByShapeAndFret() {
        val keys = CapoReference.COMMON_POSITIONS.map { it.shapeKey to it.capoFret }
        assertEquals(keys.size, keys.toSet().size, "duplicate capo positions: $keys")
    }

    @Test
    fun commonPositionsHaveNoBlankFields() {
        for (position in CapoReference.COMMON_POSITIONS) {
            assertTrue(position.shapeKey.isNotBlank(), "blank shape key")
            assertTrue(position.soundingKey.isNotBlank(), "blank sounding key")
            assertTrue(position.exampleChords.isNotBlank(), "blank example chords")
        }
    }

    // ── Friendly shapes ──────────────────────────────────────────────

    @Test
    fun everyFriendlyShapeSpellsItsNamedChordInStandardTuning() {
        for ((name, frets) in CapoReference.FRIENDLY_SHAPES) {
            val parsed = ChordNameParser.parse(name)
            assertNotNull(parsed, "unparseable friendly shape name: $name")
            val expected =
                parsed.formula.intervals
                    .map { (parsed.rootPitchClass + it) % 12 }
                    .toSet()
            assertEquals(expected, tonesOf(frets), "the $name shape ($frets) does not sound $name")
        }
    }

    @Test
    fun friendlyShapeFretStringsAreFourSingleDigits() {
        // The guide renders the raw string, so a two-digit fret would be ambiguous.
        for ((name, frets) in CapoReference.FRIENDLY_SHAPES) {
            assertEquals(4, frets.length, "$name has ${frets.length} frets, expected 4")
            assertTrue(frets.all { it.isDigit() }, "$name has a non-digit fret: $frets")
        }
    }

    @Test
    fun friendlyShapesAreExactlyEightForTheTwoRowLayout() {
        // CapoGuideView renders take(4) then drop(4) into two equally weighted rows.
        assertEquals(8, CapoReference.FRIENDLY_SHAPES.size, "the two-row shape grid expects 8 shapes")
    }

    @Test
    fun friendlyShapeNamesAreUnique() {
        val names = CapoReference.FRIENDLY_SHAPES.map { it.first }
        assertEquals(names.size, names.toSet().size, "duplicate friendly shapes: $names")
    }

    // ── Lesson prose ─────────────────────────────────────────────────

    @Test
    fun howItChangesPitchExamplesMatchTheCapoArithmetic() {
        // The worked examples are embedded in prose, where they rot silently.
        val examples = Regex("Capo (\\d+): (\\S+)").findAll(CapoReference.HOW_IT_CHANGES_PITCH).toList()
        assertTrue(examples.isNotEmpty(), "the pitch lesson no longer lists worked examples")
        for (match in examples) {
            val fret = match.groupValues[1].toInt()
            val named = match.groupValues[2].split("-").mapNotNull { rootOf(it) }
            val expected = CapoReference.STANDARD_OPEN_PITCHES.map { (it + fret) % 12 }
            assertEquals(expected, named, "the Capo $fret example is wrong")
        }
    }

    @Test
    fun lessonTextsAreNonBlank() {
        for (text in listOf(
            CapoReference.WHAT_IS_A_CAPO,
            CapoReference.HOW_IT_CHANGES_PITCH,
            CapoReference.WHEN_TO_USE,
            CapoReference.TRY_IT_TIP,
        )) {
            assertTrue(text.isNotBlank(), "a capo lesson text is blank")
        }
    }

    // ── Scenarios ────────────────────────────────────────────────────

    @Test
    fun scenariosHaveDistinctProblemsAndNonBlankSolutions() {
        val problems = CapoReference.SCENARIOS.map { it.problem }
        assertTrue(problems.isNotEmpty(), "the guide lists no scenarios")
        assertEquals(problems.size, problems.toSet().size, "duplicate scenarios: $problems")
        for (scenario in CapoReference.SCENARIOS) {
            assertTrue(scenario.problem.isNotBlank(), "blank scenario problem")
            assertTrue(scenario.solution.isNotBlank(), "blank scenario solution")
        }
    }
}

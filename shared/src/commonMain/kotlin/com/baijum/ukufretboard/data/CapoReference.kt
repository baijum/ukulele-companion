package com.baijum.ukufretboard.data

/**
 * Static reference data for the educational Capo Guide.
 *
 * Contains common capo positions, open chord shapes that work well with capo,
 * practical tips, and explanatory text for each lesson section.
 */
object CapoReference {

    // ── Lesson texts ──

    const val WHAT_IS_A_CAPO = "A capo is a clamp placed across all strings of your ukulele " +
        "at a specific fret. It shortens the vibrating length of the strings, effectively " +
        "raising the pitch of every open string by the same number of semitones.\n\n" +
        "Think of it as a movable nut. When you place a capo on fret 2, your open strings " +
        "sound 2 semitones higher. The chord shapes you play behind the capo stay the same, " +
        "but the sounding pitches are different."

    const val HOW_IT_CHANGES_PITCH = "Each fret on a ukulele raises the pitch by one semitone. " +
        "When you place a capo on fret N, every open string is raised by N semitones.\n\n" +
        "For example, with standard tuning (G-C-E-A):\n" +
        "\u2022 Capo 1: G\u266F-C\u266F-F-A\u266F\n" +
        "\u2022 Capo 2: A-D-F\u266F-B\n" +
        "\u2022 Capo 3: A\u266F-D\u266F-G-C\n\n" +
        "Try moving the capo below to see the pitches change!"

    const val WHEN_TO_USE = "Common reasons to use a capo:\n\n" +
        "\u2022 To match a singer's vocal range without learning new chord shapes\n" +
        "\u2022 To play a song in a \"difficult\" key using easy open chord shapes\n" +
        "\u2022 To get a brighter, more jangly tone (higher up the neck)\n" +
        "\u2022 To adapt guitar-oriented chord sheets for ukulele\n" +
        "\u2022 To play along with recordings that use a capo"

    const val TRY_IT_TIP = "Head to the Chord Library, select any chord, and tap " +
        "\"Capo\" to find the easiest capo position. Or tap \"Viz\" to see " +
        "how a chord shape sounds with a capo at different positions!"

    /**
     * Standard ukulele tuning open pitch classes (G=7, C=0, E=4, A=9).
     */
    val STANDARD_OPEN_PITCHES = listOf(7, 0, 4, 9) // G, C, E, A

    /**
     * String names for standard tuning.
     */
    val STRING_NAMES = listOf("G", "C", "E", "A")

    /**
     * A row in the common capo positions reference table.
     *
     * @property capoFret The fret where the capo is placed.
     * @property shapeKey The open chord shape key (e.g., "C Major shapes").
     * @property soundingKey What key actually sounds (e.g., "D Major").
     * @property exampleChords Example progression chord names in the sounding key.
     */
    data class CapoPosition(
        val capoFret: Int,
        val shapeKey: String,
        val soundingKey: String,
        val exampleChords: String,
    )

    /**
     * Common capo positions showing which open chord shapes produce which sounding keys.
     * Focused on the most practical positions for ukulele.
     */
    val COMMON_POSITIONS: List<CapoPosition> = listOf(
        CapoPosition(1, "C shapes", "C\u266F/D\u266D", "D\u266D, G\u266D, A\u266D"),
        CapoPosition(2, "C shapes", "D", "D, G, A"),
        CapoPosition(3, "C shapes", "E\u266D", "E\u266D, A\u266D, B\u266D"),
        CapoPosition(4, "C shapes", "E", "E, A, B"),
        CapoPosition(5, "C shapes", "F", "F, B\u266D, C"),
        CapoPosition(2, "A shapes", "B", "B, E, F\u266F"),
        CapoPosition(3, "A shapes", "C", "C, F, G"),
        CapoPosition(4, "G shapes", "B", "B, E, F\u266F"),
        CapoPosition(5, "G shapes", "C", "C, F, G"),
        CapoPosition(7, "G shapes", "D", "D, G, A"),
    )

    /**
     * "Friendly" open chord shapes that beginners know well.
     * These are the shapes most likely to benefit from capo transposition.
     */
    val FRIENDLY_SHAPES = listOf(
        "C" to "0003",
        "Am" to "2000",
        "F" to "2010",
        "G" to "0232",
        "A" to "2100",
        "Dm" to "2210",
        "Em" to "0402",
        "D" to "2220",
    )

    /**
     * Practical scenarios demonstrating when a capo is useful.
     */
    data class Scenario(
        val problem: String,
        val solution: String,
    )

    val SCENARIOS: List<Scenario> = listOf(
        Scenario(
            problem = "Song is in E\u266D but you only know C, Am, F, G shapes",
            solution = "Place capo on fret 3 and play your familiar C shapes!",
        ),
        Scenario(
            problem = "Song is in B and the barre chords are hard",
            solution = "Capo on fret 4 and play easy G shapes instead.",
        ),
        Scenario(
            problem = "You want a brighter, higher-pitched sound",
            solution = "Move the capo up the neck (fret 5+) for a sparkling tone.",
        ),
        Scenario(
            problem = "Playing along with a guitarist who uses a capo",
            solution = "Match their capo position and play the same shapes.",
        ),
    )
}

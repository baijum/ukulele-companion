package com.baijum.ukufretboard.data

/**
 * A single chord substitution example.
 *
 * @property original The original chord numeral (e.g., "I").
 * @property substitute The substitute chord numeral (e.g., "vi").
 * @property sharedNotes Description of shared notes (e.g., "C and E").
 * @property exampleInC Example in the key of C.
 */
data class Substitution(
    val original: String,
    val substitute: String,
    val sharedNotes: String,
    val exampleInC: String,
)

/**
 * A category of chord substitutions with explanation.
 *
 * @property title Section title.
 * @property explanation How this substitution technique works.
 * @property substitutions Specific examples.
 */
data class SubstitutionCategory(
    val title: String,
    val explanation: String,
    val substitutions: List<Substitution>,
)

/**
 * Educational content about chord substitution techniques.
 *
 * Organized into 5 categories from simple to advanced:
 * 1. Diatonic Substitutions
 * 2. Relative Major/Minor Swaps
 * 3. Tritone Substitution
 * 4. Modal Interchange (Borrowed Chords)
 * 5. Secondary Dominants
 */
object ChordSubstitutions {

    val CATEGORIES: List<SubstitutionCategory> = listOf(
        SubstitutionCategory(
            title = "1. Diatonic Substitutions",
            explanation = "Replace a chord with another chord from the same key that shares 2 of 3 notes. " +
                "Because they share most of their notes, diatonic substitutions maintain the same harmonic feel " +
                "while adding subtle variation.\n\n" +
                "The key principle: chords a third apart (3 scale degrees) in the same key share 2 notes.",
            substitutions = listOf(
                Substitution("I", "iii", "E and G", "C \u2192 Em (both contain E and G)"),
                Substitution("I", "vi", "C and E", "C \u2192 Am (both contain C and E)"),
                Substitution("IV", "ii", "D and F", "F \u2192 Dm (both contain D and F)"),
                Substitution("V", "vii\u00B0", "B and D", "G \u2192 Bdim (both contain B and D)"),
            ),
        ),
        SubstitutionCategory(
            title = "2. Relative Major/Minor Swaps",
            explanation = "Every major chord has a relative minor (and vice versa) that shares 2 of 3 notes. " +
                "The relative minor is found 3 semitones below the major root.\n\n" +
                "This is the simplest and most common substitution — " +
                "it always works because the chords are so closely related.",
            substitutions = listOf(
                Substitution("I (C)", "vi (Am)", "C and E", "Replace C with Am for a darker feel"),
                Substitution("IV (F)", "ii (Dm)", "D and F", "Replace F with Dm for a softer sound"),
                Substitution("V (G)", "iii (Em)", "E and G", "Replace G with Em (less common, more subtle)"),
            ),
        ),
        SubstitutionCategory(
            title = "3. Tritone Substitution",
            explanation = "Replace a dominant 7th chord with the dominant 7th chord a tritone (6 semitones) away. " +
                "Both chords share the same tritone interval (the 3rd and 7th are swapped), " +
                "so they create similar harmonic tension.\n\n" +
                "This creates smooth chromatic bass movement. Used extensively in jazz.",
            substitutions = listOf(
                Substitution("G7 (V7)", "Db7", "B and F (the tritone)", "G7 \u2192 Db7 before resolving to C"),
                Substitution("D7 (V7/V)", "Ab7", "F# and C", "D7 \u2192 Ab7 before resolving to G"),
                Substitution("A7 (V7/ii)", "Eb7", "C# and G", "A7 \u2192 Eb7 before resolving to Dm"),
            ),
        ),
        SubstitutionCategory(
            title = "4. Modal Interchange (Borrowed Chords)",
            explanation = "Borrow chords from the parallel minor (or other mode) into a major key. " +
                "For example, in C major, use chords from C minor.\n\n" +
                "Borrowed chords add unexpected color and emotion. " +
                "The most common borrowings come from the natural minor (Aeolian mode).",
            substitutions = listOf(
                Substitution("IV (F)", "iv (Fm)", "F", "Replace F with Fm for a bittersweet feel"),
                Substitution("\u2014", "\u266DVI (Ab)", "\u2014", "Add Ab major (from C minor) for a dramatic shift"),
                Substitution("\u2014", "\u266DVII (Bb)", "\u2014", "Add Bb major (from C minor) for a rock feel"),
                Substitution("I (C)", "i (Cm)", "C", "End on Cm instead of C for a surprise minor ending"),
            ),
        ),
        SubstitutionCategory(
            title = "5. Secondary Dominants",
            explanation = "Use the V chord of any diatonic chord as a temporary tonicization. " +
                "A secondary dominant \"points to\" a chord other than the tonic, making " +
                "its arrival feel stronger and more intentional.\n\n" +
                "Written as V/X where X is the chord being tonicized. " +
                "For example, V/V means \"the V chord of the V chord.\"",
            substitutions = listOf(
                Substitution("before V (G)", "V/V = D7", "\u2014", "Play D7 before G to emphasize the arrival on V"),
                Substitution("before vi (Am)", "V/vi = E7", "\u2014", "Play E7 before Am for a dramatic minor cadence"),
                Substitution("before ii (Dm)", "V/ii = A7", "\u2014", "Play A7 before Dm in jazz progressions"),
                Substitution("before IV (F)", "V/IV = C7", "\u2014", "Play C7 before F (common in blues)"),
            ),
        ),
    )
}

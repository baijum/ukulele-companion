package com.baijum.ukufretboard.data

/**
 * Direction of a single strum beat.
 */
enum class StrumDirection(val symbol: String) {
    DOWN("↓"),
    UP("↑"),
    CHUCK("X"),
    MISS("×"),
    PAUSE("—"),
}

/**
 * Difficulty level for strumming patterns.
 */
enum class Difficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

/**
 * A single beat in a strumming pattern.
 *
 * @property direction The strum direction for this beat.
 * @property emphasis Whether this beat is accented.
 */
data class StrumBeat(
    val direction: StrumDirection,
    val emphasis: Boolean = false,
)

/**
 * A complete strumming pattern with metadata.
 *
 * @property name Human-readable name of the pattern.
 * @property description Brief description of when/how to use the pattern.
 * @property difficulty Skill level required.
 * @property beatsPerMeasure Time signature numerator (e.g., 4 for 4/4 time).
 * @property beats The sequence of strum beats.
 * @property notation Compact text notation (e.g., "D - D U - U D U").
 * @property suggestedBpm Recommended tempo range in BPM.
 */
data class StrumPattern(
    val name: String,
    val description: String,
    val difficulty: Difficulty,
    val beatsPerMeasure: Int = 4,
    val beats: List<StrumBeat>,
    val notation: String,
    val suggestedBpm: IntRange,
    val counting: String = "",
    val genres: List<String> = emptyList(),
)

/**
 * Library of common ukulele strumming patterns.
 */
object StrumPatterns {

    val ALL: List<StrumPattern> = listOf(
        // ── Beginner ────────────────────────────────────────────────
        StrumPattern(
            name = "All Downs",
            description = "The most fundamental strum \u2014 one down strum per quarter-note beat. " +
                "While simple, it is essential for locking into the \"pocket\" and establishing " +
                "a steady pulse. Focus on even spacing and consistent volume before adding complexity.",
            difficulty = Difficulty.BEGINNER,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.DOWN),
            ),
            notation = "D  D  D  D",
            suggestedBpm = 80..120,
            counting = "1, 2, 3, 4",
            genres = listOf("Intro", "Ballad", "Punk"),
        ),
        StrumPattern(
            name = "Down-Up",
            description = "Alternating down and up strums on every eighth note. This \"continuous " +
                "eighths\" pattern acts as a rhythmic motor, filling every subdivision of the " +
                "measure. The challenge is consistency: upstrums must match downstrums in volume " +
                "to avoid a lopsided sound.",
            difficulty = Difficulty.BEGINNER,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "D U D U D U D U",
            suggestedBpm = 80..120,
            counting = "1 &, 2 &, 3 &, 4 &",
            genres = listOf("Rock", "Pop", "Bluegrass"),
        ),
        StrumPattern(
            name = "Island Strum",
            description = "The most iconic ukulele strum. The ghost strum on beat 3 creates a " +
                "syncopated \"lift\" that ties the off-beats together, giving it a relaxed, " +
                "swaying feel. Often called \"Old Faithful\" because it fits nearly any 4/4 song. " +
                "Heard in \"Somewhere Over the Rainbow\" (IZ) and \"Riptide\" (Vance Joy).",
            difficulty = Difficulty.BEGINNER,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "D - D U - U D U",
            suggestedBpm = 90..130,
            counting = "1, 2 &, (3) &, 4 &",
            genres = listOf("Hawaiian", "Pop", "Folk"),
        ),
        StrumPattern(
            name = "Folk Strum",
            description = "A steady, driving pattern common in folk and campfire songs. By filling " +
                "the final beat with a down-up, the rhythm propels itself into the next measure, " +
                "creating forward momentum. Easy to maintain at moderate tempos.",
            difficulty = Difficulty.BEGINNER,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "D - D U D - D U",
            suggestedBpm = 80..120,
            counting = "1, 2 &, 3, 4 &",
            genres = listOf("Folk", "Rock", "Acoustic"),
        ),
        StrumPattern(
            name = "Reggae",
            description = "The signature reggae \"skank\" \u2014 emphasis on the off-beats with silence " +
                "on the downbeats. The downbeats are left empty for the bass or kick drum. " +
                "Keep each strum short and staccato for an authentic sound.",
            difficulty = Difficulty.BEGINNER,
            beats = listOf(
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
            ),
            notation = "-  D  -  D",
            suggestedBpm = 70..100,
            counting = "(1) &, (2) &",
            genres = listOf("Reggae"),
        ),
        StrumPattern(
            name = "Waltz",
            description = "A triple-meter pattern with a strong accent on beat 1 and lighter " +
                "strums on beats 2 and 3. Creates a circular, swaying motion distinct from " +
                "the linear feel of 4/4 time. Essential for waltzes, some ballads, and " +
                "traditional folk songs.",
            difficulty = Difficulty.BEGINNER,
            beatsPerMeasure = 3,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "D - d u d u",
            suggestedBpm = 80..130,
            counting = "1, 2 &, 3 &",
            genres = listOf("Waltz", "Folk", "Ballad"),
        ),
        // ── Intermediate ────────────────────────────────────────────
        StrumPattern(
            name = "Calypso",
            description = "A syncopated Caribbean pattern with a bouncy, upbeat feel. A shorter " +
                "variation of the Island Strum that emphasizes the beat-1 downstroke and the " +
                "syncopated off-beat ties. Once you lock in the ghost strum, it becomes addictive.",
            difficulty = Difficulty.INTERMEDIATE,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "D - D U - U",
            suggestedBpm = 100..140,
            counting = "1, 2 &, (3) &",
            genres = listOf("Calypso", "Caribbean", "Pop"),
        ),
        StrumPattern(
            name = "Ska",
            description = "All upstrokes on the off-beats at high energy \u2014 the frantic, bright " +
                "sound that defines ska and punk. The downbeats are ghost strums (the hand moves " +
                "down but misses the strings), keeping the constant-motion principle intact at " +
                "tempos often exceeding 160 BPM.",
            difficulty = Difficulty.INTERMEDIATE,
            beats = listOf(
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP, emphasis = true),
            ),
            notation = "- U - U - U - U",
            suggestedBpm = 120..160,
            counting = "(1) &, (2) &, (3) &, (4) &",
            genres = listOf("Ska", "Punk"),
        ),
        StrumPattern(
            name = "Swing",
            description = "A shuffled down-up feel where the downbeat is lengthened and the upbeat " +
                "shortened (approximately 2:1 ratio based on triplets). This \"loping\" groove " +
                "is essential for jazz, blues, and traditional Hawaiian swing. Vocalize " +
                "\"Hump-ty Dump-ty\" to internalize the feel.",
            difficulty = Difficulty.INTERMEDIATE,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "D - U D - U",
            suggestedBpm = 80..120,
            counting = "1 - a, 2 - a",
            genres = listOf("Jazz", "Blues", "Hawaiian Swing"),
        ),
        StrumPattern(
            name = "Reggae Chuck",
            description = "The percussive reggae pattern using the \"chuck\" (palm mute) on the " +
                "off-beats. The chuck acts as the ukulele's snare drum, producing a sharp " +
                "\"click\" by damping the strings with the palm immediately after striking. " +
                "Essential for Jack Johnson-style acoustic pop and roots reggae.",
            difficulty = Difficulty.INTERMEDIATE,
            beats = listOf(
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.CHUCK, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.CHUCK, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.CHUCK, emphasis = true),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.CHUCK, emphasis = true),
            ),
            notation = "- X - X - X - X",
            suggestedBpm = 70..110,
            counting = "(1) &, (2) &, (3) &, (4) &",
            genres = listOf("Reggae", "Acoustic Pop", "Blues"),
        ),
        StrumPattern(
            name = "Jawaiian",
            description = "Hawaiian reggae that softens the rigid off-beat skank into a rolling " +
                "down-up shuffle. Instead of a single staccato chop, each off-beat becomes a " +
                "quick down-up pair, creating a lilting, relaxed feel less aggressive than " +
                "Jamaican roots reggae.",
            difficulty = Difficulty.INTERMEDIATE,
            beats = listOf(
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.PAUSE),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "- D U - D U - D U - D U",
            suggestedBpm = 80..120,
            counting = "(1) & a, (2) & a, (3) & a, (4) & a",
            genres = listOf("Hawaiian Reggae", "Pop"),
        ),
        // ── Advanced ────────────────────────────────────────────────
        StrumPattern(
            name = "Split Stroke",
            description = "The George Formby syncopated ragtime technique that creates the illusion " +
                "of two instruments playing at once. Relies on forearm rotation (the \"doorknob\" " +
                "motion): a full down strum, an up catch on the high string, a split touch on " +
                "the low string, then a full down strum. The accented off-beats mimic a ragtime " +
                "piano's right hand.",
            difficulty = Difficulty.ADVANCED,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.UP),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.DOWN, emphasis = true),
            ),
            notation = "D U d D",
            suggestedBpm = 100..160,
            counting = "1 & a 2",
            genres = listOf("Vaudeville", "Ragtime"),
        ),
        StrumPattern(
            name = "Triplet Roll",
            description = "The blazing-fast triplet technique popularized by Jake Shimabukuro. " +
                "The index finger strums down, then the thumb strums down, then the index " +
                "strums up \u2014 all in tight succession. Because both digits work as an " +
                "alternating unit, the hand barely moves, allowing speeds that exceed normal " +
                "wrist oscillation.",
            difficulty = Difficulty.ADVANCED,
            beats = listOf(
                StrumBeat(StrumDirection.DOWN, emphasis = true),
                StrumBeat(StrumDirection.DOWN),
                StrumBeat(StrumDirection.UP),
            ),
            notation = "D D U",
            suggestedBpm = 80..160,
            counting = "1-trip-let",
            genres = listOf("Virtuoso", "Spanish", "Solo"),
        ),
    )
}

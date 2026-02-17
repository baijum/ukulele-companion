package com.baijum.ukufretboard.data

/**
 * A theory lesson with explanation, key points, and a quiz question.
 *
 * @property id Unique lesson ID.
 * @property module Module name (e.g., "Notes & Pitch").
 * @property title Lesson title.
 * @property content Multi-paragraph explanation text.
 * @property keyPoints Key takeaway bullet points.
 * @property quizQuestion A quick check question.
 * @property quizOptions Answer options.
 * @property quizCorrectIndex Index of the correct answer.
 * @property quizExplanation Explanation of the correct answer.
 */
data class TheoryLesson(
    val id: String,
    val module: String,
    val title: String,
    val content: String,
    val keyPoints: List<String>,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val quizCorrectIndex: Int,
    val quizExplanation: String,
)

/**
 * Complete music theory curriculum organized into 7 modules.
 */
object TheoryLessons {

    val MODULES: List<String> = listOf(
        "Notes & Pitch",
        "Intervals",
        "Scales",
        "Chords",
        "Keys & Signatures",
        "Progressions & Harmony",
        "Rhythm",
    )

    val ALL: List<TheoryLesson> = listOf(
        // ── Module 1: Notes & Pitch ──
        TheoryLesson(
            id = "notes_12",
            module = "Notes & Pitch",
            title = "The 12 Notes",
            content = "Western music uses 12 distinct notes, repeating in higher and lower octaves. " +
                "Starting from C, the 12 notes are: C, C#/Db, D, D#/Eb, E, F, F#/Gb, G, G#/Ab, A, A#/Bb, B.\n\n" +
                "The distance between any two adjacent notes is called a semitone (or half step). " +
                "Two semitones make a whole step. These 12 notes form the chromatic scale \u2014 " +
                "every other scale and chord is built from subsets of these 12 notes.",
            keyPoints = listOf(
                "12 unique notes in Western music",
                "A semitone is the smallest distance between notes",
                "Sharps (#) raise a note, flats (b) lower it",
                "C#/Db are the same pitch (enharmonic equivalents)",
            ),
            quizQuestion = "How many unique notes are in the chromatic scale?",
            quizOptions = listOf("7", "10", "12", "14"),
            quizCorrectIndex = 2,
            quizExplanation = "The chromatic scale has 12 unique notes before repeating at the octave.",
        ),
        TheoryLesson(
            id = "notes_sharp_flat",
            module = "Notes & Pitch",
            title = "Sharps and Flats",
            content = "A sharp (#) raises a note by one semitone. A flat (b) lowers it by one semitone. " +
                "For example, C# is one semitone above C, and Db is one semitone below D.\n\n" +
                "C# and Db are the same pitch \u2014 they're called enharmonic equivalents. " +
                "Which name you use depends on the musical context (the key you're in). " +
                "Notice that E/F and B/C have no sharp/flat between them \u2014 " +
                "they're already one semitone apart.",
            keyPoints = listOf(
                "Sharp (#) = one semitone up",
                "Flat (b) = one semitone down",
                "C# = Db (enharmonic equivalents)",
                "E-F and B-C are natural semitones (no note between them)",
            ),
            quizQuestion = "What is the enharmonic equivalent of G#?",
            quizOptions = listOf("Gb", "Ab", "F#", "A"),
            quizCorrectIndex = 1,
            quizExplanation = "G# and Ab are the same pitch. G# is one semitone above G, and Ab is one semitone below A.",
        ),
        // ── Module 2: Intervals ──
        TheoryLesson(
            id = "intervals_intro",
            module = "Intervals",
            title = "What Is an Interval?",
            content = "An interval is the distance between two notes, measured in semitones. " +
                "Intervals are the building blocks of scales, chords, and melodies.\n\n" +
                "Each interval has a name that tells you both its size and quality: " +
                "Perfect 5th (P5) = 7 semitones, Major 3rd (M3) = 4 semitones, Minor 3rd (m3) = 3 semitones.\n\n" +
                "Intervals determine the character of music: the major 3rd sounds happy, the minor 3rd sounds sad. " +
                "The perfect 5th sounds stable and open. Learning to recognize intervals by ear " +
                "is one of the most valuable skills in music.",
            keyPoints = listOf(
                "Intervals measure distance between notes in semitones",
                "Major intervals sound bright/happy",
                "Minor intervals sound dark/sad",
                "Perfect intervals (P4, P5, P8) sound stable and open",
            ),
            quizQuestion = "How many semitones in a Perfect 5th?",
            quizOptions = listOf("5", "6", "7", "8"),
            quizCorrectIndex = 2,
            quizExplanation = "A Perfect 5th spans 7 semitones (e.g., C to G).",
        ),
        TheoryLesson(
            id = "intervals_table",
            module = "Intervals",
            title = "The 12 Interval Names",
            content = "Here are all 12 intervals within one octave:\n\n" +
                "0 semitones = Unison (same note)\n" +
                "1 = Minor 2nd (m2)\n" +
                "2 = Major 2nd (M2)\n" +
                "3 = Minor 3rd (m3) \u2014 the \"sad\" interval\n" +
                "4 = Major 3rd (M3) \u2014 the \"happy\" interval\n" +
                "5 = Perfect 4th (P4)\n" +
                "6 = Tritone (d5/A4) \u2014 the \"devil's interval\"\n" +
                "7 = Perfect 5th (P5) \u2014 power chords\n" +
                "8 = Minor 6th (m6)\n" +
                "9 = Major 6th (M6)\n" +
                "10 = Minor 7th (m7)\n" +
                "11 = Major 7th (M7)\n" +
                "12 = Octave (P8)",
            keyPoints = listOf(
                "m3 (3 semitones) = Minor/sad",
                "M3 (4 semitones) = Major/happy",
                "P5 (7 semitones) = Stable, open",
                "Tritone (6 semitones) = Tense, unstable",
            ),
            quizQuestion = "What interval is called the 'devil's interval'?",
            quizOptions = listOf("Minor 3rd", "Tritone", "Minor 7th", "Major 2nd"),
            quizCorrectIndex = 1,
            quizExplanation = "The tritone (6 semitones, also called diminished 5th) was historically called the 'devil's interval' due to its dissonant sound.",
        ),
        // ── Module 3: Scales ──
        TheoryLesson(
            id = "scales_major",
            module = "Scales",
            title = "The Major Scale",
            content = "The major scale is the most important scale in Western music. It uses 7 of the 12 available notes, " +
                "selected by a specific pattern of whole and half steps: W-W-H-W-W-W-H.\n\n" +
                "In C major: C(W)D(W)E(H)F(W)G(W)A(W)B(H)C.\n\n" +
                "The major scale sounds bright, happy, and resolved. " +
                "It's the foundation for understanding keys, chords, and harmony. " +
                "Every major key uses this same interval pattern starting from a different note.",
            keyPoints = listOf(
                "7 notes selected from 12",
                "Pattern: Whole-Whole-Half-Whole-Whole-Whole-Half",
                "Sounds bright and happy",
                "Foundation of Western harmony",
            ),
            quizQuestion = "What is the interval pattern of a major scale?",
            quizOptions = listOf("W-W-W-H-W-W-H", "W-W-H-W-W-W-H", "W-H-W-W-H-W-W", "H-W-W-H-W-W-W"),
            quizCorrectIndex = 1,
            quizExplanation = "The major scale follows the pattern Whole-Whole-Half-Whole-Whole-Whole-Half.",
        ),
        TheoryLesson(
            id = "scales_minor",
            module = "Scales",
            title = "Minor Scales",
            content = "The natural minor scale uses the pattern: W-H-W-W-H-W-W. " +
                "In A minor: A(W)B(H)C(W)D(W)E(H)F(W)G(W)A.\n\n" +
                "The minor scale sounds dark, sad, or mysterious compared to the major scale. " +
                "Notice A minor uses the same notes as C major \u2014 they're relative keys.\n\n" +
                "There are also harmonic minor (raised 7th for a stronger V chord) " +
                "and melodic minor (raised 6th and 7th ascending) variants.",
            keyPoints = listOf(
                "Natural minor: W-H-W-W-H-W-W",
                "Sounds dark, sad, mysterious",
                "Every minor key has a relative major (3 semitones up)",
                "Harmonic minor raises the 7th degree",
            ),
            quizQuestion = "A minor is the relative minor of which major key?",
            quizOptions = listOf("G major", "C major", "D major", "F major"),
            quizCorrectIndex = 1,
            quizExplanation = "A minor and C major share the same notes (no sharps or flats). The relative minor is always 3 semitones below the major root.",
        ),
        TheoryLesson(
            id = "scales_modes",
            module = "Scales",
            title = "Modes",
            content = "Modes are scales built by starting the major scale pattern on different degrees. " +
                "Each mode has a distinct character:\n\n" +
                "\u2022 Ionian (1st degree) = Major scale\n" +
                "\u2022 Dorian (2nd) = Minor with a bright 6th. Jazz, funk.\n" +
                "\u2022 Phrygian (3rd) = Minor with a dark b2. Flamenco, metal.\n" +
                "\u2022 Lydian (4th) = Major with a dreamy #4. Film scores.\n" +
                "\u2022 Mixolydian (5th) = Major with a b7. Blues, rock.\n" +
                "\u2022 Aeolian (6th) = Natural minor.\n" +
                "\u2022 Locrian (7th) = Diminished. Rarely used alone.",
            keyPoints = listOf(
                "7 modes, each starting on a different scale degree",
                "Dorian = Minor with bright 6th (jazz)",
                "Mixolydian = Major with flat 7th (blues)",
                "Phrygian = Minor with flat 2nd (flamenco)",
            ),
            quizQuestion = "Which mode is commonly used in flamenco music?",
            quizOptions = listOf("Dorian", "Lydian", "Phrygian", "Mixolydian"),
            quizCorrectIndex = 2,
            quizExplanation = "Phrygian mode, with its characteristic flat 2nd degree, gives the dark, Spanish sound associated with flamenco.",
        ),
        // ── Module 4: Chords ──
        TheoryLesson(
            id = "chords_triads",
            module = "Chords",
            title = "Triads",
            content = "A chord is three or more notes played together. The simplest chords are triads \u2014 " +
                "three notes built by stacking two intervals of a 3rd.\n\n" +
                "The four triad types:\n" +
                "\u2022 Major (1-3-5): Bright, happy. Formula: root + M3 + P5.\n" +
                "\u2022 Minor (1-b3-5): Dark, sad. Formula: root + m3 + P5.\n" +
                "\u2022 Diminished (1-b3-b5): Tense, unstable. Formula: root + m3 + d5.\n" +
                "\u2022 Augmented (1-3-#5): Eerie, unresolved. Formula: root + M3 + A5.",
            keyPoints = listOf(
                "Triads = 3 notes stacked in 3rds",
                "Major = happy (M3 + m3)",
                "Minor = sad (m3 + M3)",
                "Diminished = tense (m3 + m3)",
            ),
            quizQuestion = "What intervals make up a major triad?",
            quizOptions = listOf("Root, m3, P5", "Root, M3, P5", "Root, M3, m6", "Root, m3, d5"),
            quizCorrectIndex = 1,
            quizExplanation = "A major triad consists of a root, major 3rd (4 semitones), and perfect 5th (7 semitones).",
        ),
        TheoryLesson(
            id = "chords_seventh",
            module = "Chords",
            title = "Seventh Chords",
            content = "Seventh chords add a 4th note \u2014 a 7th interval above the root \u2014 " +
                "to a triad. They add richness and harmonic color.\n\n" +
                "Common 7th chords:\n" +
                "\u2022 Dominant 7th (1-3-5-b7): Bluesy, wants to resolve. Key chord in blues and jazz.\n" +
                "\u2022 Major 7th (1-3-5-7): Lush, dreamy. Common in jazz and pop.\n" +
                "\u2022 Minor 7th (1-b3-5-b7): Mellow, smooth. The ii chord in jazz.\n" +
                "\u2022 Diminished 7th (1-b3-b5-bb7): Dramatic, symmetrical.",
            keyPoints = listOf(
                "7th chords = triad + a 7th interval",
                "Dominant 7th creates tension (wants to resolve)",
                "Major 7th sounds lush and dreamy",
                "Minor 7th sounds smooth and mellow",
            ),
            quizQuestion = "Which 7th chord creates the most tension to resolve?",
            quizOptions = listOf("Major 7th", "Minor 7th", "Dominant 7th", "Diminished 7th"),
            quizCorrectIndex = 2,
            quizExplanation = "The dominant 7th chord contains a tritone between its 3rd and 7th, creating strong tension that 'wants' to resolve to the tonic.",
        ),
        TheoryLesson(
            id = "chords_inversions",
            module = "Chords",
            title = "Chord Inversions",
            content = "A chord inversion is the same chord played with a different note in the bass " +
                "(the lowest-sounding note). The notes don\u2019t change \u2014 only which one is on the bottom.\n\n" +
                "\u2022 Root position: root in the bass (C-E-G).\n" +
                "\u2022 1st inversion: 3rd in the bass (E-G-C). Written as C/E.\n" +
                "\u2022 2nd inversion: 5th in the bass (G-C-E). Written as C/G.\n" +
                "\u2022 3rd inversion (7th chords only): 7th in the bass (Bb-C-E-G). Written as C7/Bb.\n\n" +
                "On ukulele with standard re-entrant tuning (G4-C4-E4-A4), the bass note is usually on the " +
                "C string \u2014 the lowest-pitched string \u2014 not the top G string. This means many common " +
                "voicings are already inversions without players realizing it.\n\n" +
                "Inversions matter because they enable smoother voice leading: by choosing an inversion " +
                "where the bass note is close to the next chord\u2019s bass note, transitions sound more connected " +
                "and musical.",
            keyPoints = listOf(
                "Inversions reorder which chord tone is the lowest note",
                "Slash notation (C/E) shows the chord and its bass note",
                "Ukulele re-entrant tuning makes the C string the true bass",
                "Using inversions creates smoother chord transitions (voice leading)",
            ),
            quizQuestion = "In a first-inversion C major triad, which note is in the bass?",
            quizOptions = listOf("C", "E", "G", "B"),
            quizCorrectIndex = 1,
            quizExplanation = "First inversion places the 3rd of the chord in the bass. For C major (C-E-G), the 3rd is E, giving E-G-C (written C/E).",
        ),
        // ── Module 5: Keys & Signatures ──
        TheoryLesson(
            id = "keys_intro",
            module = "Keys & Signatures",
            title = "What Is a Key?",
            content = "A key is a group of notes that sound good together, centered around one \"home\" note (the tonic). " +
                "When we say a song is \"in the key of G major,\" it means G is the home note " +
                "and the song uses notes from the G major scale.\n\n" +
                "There are 12 major keys and 12 minor keys (one for each note). " +
                "Every major key has a relative minor key that shares the same notes. " +
                "For example, C major and A minor both use C-D-E-F-G-A-B.",
            keyPoints = listOf(
                "A key = a set of notes centered around a tonic",
                "12 major keys, 12 minor keys",
                "Each major key has a relative minor (same notes)",
                "The key determines which chords are available",
            ),
            quizQuestion = "What is the relative minor of G major?",
            quizOptions = listOf("D minor", "B minor", "E minor", "A minor"),
            quizCorrectIndex = 2,
            quizExplanation = "The relative minor is always 3 semitones below the major root. G - 3 semitones = E, so E minor is the relative minor of G major.",
        ),
        TheoryLesson(
            id = "keys_circle",
            module = "Keys & Signatures",
            title = "The Circle of Fifths",
            content = "The Circle of Fifths arranges all 12 keys in a circle where each adjacent key " +
                "differs by just one sharp or flat.\n\n" +
                "Moving clockwise (by perfect 5ths): C \u2192 G \u2192 D \u2192 A \u2192 E \u2192 B. Each adds one sharp.\n" +
                "Moving counter-clockwise (by perfect 4ths): C \u2192 F \u2192 Bb \u2192 Eb \u2192 Ab \u2192 Db. Each adds one flat.\n\n" +
                "Adjacent keys on the circle are closely related \u2014 they share 6 of 7 notes. " +
                "This makes modulating between them smooth and natural.",
            keyPoints = listOf(
                "Clockwise = add one sharp per step",
                "Counter-clockwise = add one flat per step",
                "Adjacent keys share 6 of 7 notes",
                "The most important visual tool in music theory",
            ),
            quizQuestion = "Moving clockwise on the Circle of Fifths from C, what is the next key?",
            quizOptions = listOf("F", "D", "G", "A"),
            quizCorrectIndex = 2,
            quizExplanation = "Moving clockwise (up a perfect 5th) from C gives G major, which has 1 sharp (F#).",
        ),
        // ── Module 6: Progressions & Harmony ──
        TheoryLesson(
            id = "harmony_diatonic",
            module = "Progressions & Harmony",
            title = "Diatonic Harmony",
            content = "When you build a triad on each note of a major scale, you get 7 diatonic chords. " +
                "These are the chords that \"belong\" to a key.\n\n" +
                "In C major: C (I), Dm (ii), Em (iii), F (IV), G (V), Am (vi), Bdim (vii\u00B0).\n\n" +
                "Roman numerals show the chord's position in the key: " +
                "uppercase = major, lowercase = minor, \u00B0 = diminished. " +
                "These 7 chords are the foundation of virtually all Western harmony.",
            keyPoints = listOf(
                "7 diatonic chords per major key",
                "I, IV, V are major; ii, iii, vi are minor; vii\u00B0 is diminished",
                "Roman numerals show position and quality",
                "Almost all songs use only diatonic chords",
            ),
            quizQuestion = "In a major key, what quality is the ii chord?",
            quizOptions = listOf("Major", "Minor", "Diminished", "Augmented"),
            quizCorrectIndex = 1,
            quizExplanation = "In a major key, the ii chord is always minor (e.g., Dm in the key of C major).",
        ),
        TheoryLesson(
            id = "harmony_functions",
            module = "Progressions & Harmony",
            title = "Chord Functions",
            content = "Every chord in a key has a harmonic function \u2014 a role it plays in creating musical tension and resolution.\n\n" +
                "\u2022 Tonic (T): I, iii, vi \u2014 \"Home.\" Feels stable and resolved. Songs typically start and end here.\n" +
                "\u2022 Subdominant (S): ii, IV \u2014 \"Departure.\" Creates forward motion, moves away from home.\n" +
                "\u2022 Dominant (D): V, vii\u00B0 \u2014 \"Tension.\" Needs to resolve back to Tonic.\n\n" +
                "The fundamental harmonic cycle is T \u2192 S \u2192 D \u2192 T. " +
                "This is why I-IV-V-I and ii-V-I feel so satisfying \u2014 they follow the natural resolution path.",
            keyPoints = listOf(
                "Tonic (I, iii, vi) = home, stable",
                "Subdominant (ii, IV) = departure, motion",
                "Dominant (V, vii\u00B0) = tension, needs resolution",
                "Natural flow: T \u2192 S \u2192 D \u2192 T",
            ),
            quizQuestion = "Which chord function creates the most tension?",
            quizOptions = listOf("Tonic", "Subdominant", "Dominant", "None"),
            quizCorrectIndex = 2,
            quizExplanation = "The Dominant function (V and vii\u00B0 chords) creates the most tension and 'wants' to resolve back to the Tonic.",
        ),
        // ── Module 7: Rhythm ──
        TheoryLesson(
            id = "rhythm_time_sig",
            module = "Rhythm",
            title = "Time Signatures",
            content = "A time signature tells you how beats are organized in a piece of music. " +
                "The top number says how many beats per measure; the bottom says which note value gets one beat.\n\n" +
                "\u2022 4/4 (Common time): 4 beats per measure. By far the most common. Pop, rock, folk.\n" +
                "\u2022 3/4 (Waltz time): 3 beats per measure. Waltzes, some ballads.\n" +
                "\u2022 6/8 (Compound duple): 6 eighth notes per measure, felt as 2 groups of 3. Irish jigs, slow ballads.\n\n" +
                "Most songs you'll play on ukulele are in 4/4 time.",
            keyPoints = listOf(
                "4/4 = 4 beats per measure (most common)",
                "3/4 = 3 beats per measure (waltz)",
                "6/8 = 6 eighth notes, felt in 2 groups of 3",
                "Top number = beats per measure",
            ),
            quizQuestion = "How many beats per measure in 3/4 time?",
            quizOptions = listOf("2", "3", "4", "6"),
            quizCorrectIndex = 1,
            quizExplanation = "3/4 time has 3 beats per measure, with a quarter note getting one beat. This is waltz time.",
        ),
        TheoryLesson(
            id = "rhythm_note_values",
            module = "Rhythm",
            title = "Note Values",
            content = "Each note has a duration (how long it lasts):\n\n" +
                "\u2022 Whole note: 4 beats (fills an entire 4/4 measure)\n" +
                "\u2022 Half note: 2 beats\n" +
                "\u2022 Quarter note: 1 beat (the basic pulse in 4/4)\n" +
                "\u2022 Eighth note: 1/2 beat (2 per beat)\n" +
                "\u2022 Sixteenth note: 1/4 beat (4 per beat)\n\n" +
                "Each value is half the duration of the one above it. " +
                "For strumming, down strums typically fall on beats and up strums on the \"and\" (eighth notes).",
            keyPoints = listOf(
                "Whole = 4 beats, Half = 2, Quarter = 1",
                "Eighth = 1/2 beat, Sixteenth = 1/4 beat",
                "Each value is half the previous",
                "Down strums on beats, up strums on 'ands'",
            ),
            quizQuestion = "How many eighth notes fit in one measure of 4/4 time?",
            quizOptions = listOf("4", "6", "8", "16"),
            quizCorrectIndex = 2,
            quizExplanation = "In 4/4 time, there are 4 beats per measure. Each beat has 2 eighth notes, so 4 \u00D7 2 = 8 eighth notes.",
        ),
        TheoryLesson(
            id = "rhythm_constant_motion",
            module = "Rhythm",
            title = "The Constant Motion Principle",
            content = "The secret to professional rhythm on ukulele is the constant motion principle. " +
                "Your strumming hand functions as a pendulum, moving down on the beat (1, 2, 3, 4) " +
                "and up on the off-beat (the \"and\"). This motion never stops.\n\n" +
                "To create complex rhythms, you don\u2019t stop the hand \u2014 instead, you lift it " +
                "slightly away from the strings to \"miss\" them on specific beats. These missed " +
                "strikes are called ghost strums or air strums.\n\n" +
                "For example, the Island Strum (D - D U - U D U) has a ghost strum on beat 3. " +
                "The hand still moves down on beat 3 but doesn\u2019t make contact. This ensures the " +
                "hand is correctly positioned for the upstrum on the \"and\" of 3.\n\n" +
                "This principle links physical direction to rhythmic time: down is always on the beat, " +
                "up is always on the off-beat. This somatic link lets you feel the rhythm physically " +
                "rather than intellectually counting it.",
            keyPoints = listOf(
                "The strumming hand never stops moving",
                "Down strums land on beats, up strums on off-beats",
                "Ghost strums (air strums) create silence without stopping the hand",
                "Direction is locked to time: down = beat, up = off-beat",
            ),
            quizQuestion = "In the constant motion technique, how do you create silence on a beat?",
            quizOptions = listOf(
                "Stop the hand completely",
                "Move the hand but miss the strings (ghost strum)",
                "Press the strings against the fretboard",
                "Slow down the tempo",
            ),
            quizCorrectIndex = 1,
            quizExplanation = "A ghost strum (or air strum) means the hand continues its pendulum motion but lifts away to miss the strings, maintaining the rhythmic pulse without producing sound.",
        ),
        TheoryLesson(
            id = "rhythm_straight_swing",
            module = "Rhythm",
            title = "Straight vs. Swing Feel",
            content = "The subdivision of the beat \u2014 the space between the \"1\" and the \"2\" \u2014 " +
                "can be interpreted in two distinct ways that radically alter the genre feel.\n\n" +
                "Straight eighths: The time is divided equally. Each down strum and up strum " +
                "occupies exactly 50% of the beat. This produces a driving, \"motorik\" feel " +
                "common in rock, Latin, and modern pop. Vocalize \"1-and-2-and\" with even spacing.\n\n" +
                "Swing (shuffle) eighths: The first eighth note (down strum) is lengthened and " +
                "the second (up strum) is shortened. The ratio is approximately 2:1 (based on " +
                "triplets). This creates a \"loping\" or \"galloping\" feel. Vocalize " +
                "\"1-a-2-a\" or \"Hump-ty Dump-ty\" to internalize it.\n\n" +
                "Swing feel is essential for blues, jazz, and traditional Hawaiian swing. " +
                "Many shuffle patterns in 12/8 time (doo-wop, slow blues, R&B) are built on " +
                "this same triplet subdivision.",
            keyPoints = listOf(
                "Straight = even 50/50 subdivision (rock, pop, Latin)",
                "Swing = uneven long-short subdivision, roughly 2:1 (jazz, blues)",
                "Swing is based on triplet subdivision",
                "Vocalize to internalize: \"1-and\" (straight) vs. \"1-a\" (swing)",
            ),
            quizQuestion = "Which feel divides the beat into uneven long-short subdivisions?",
            quizOptions = listOf(
                "Straight feel",
                "Swing feel",
                "Compound feel",
                "Syncopated feel",
            ),
            quizCorrectIndex = 1,
            quizExplanation = "Swing feel uses an approximately 2:1 ratio where the downbeat note is lengthened and the upbeat shortened, creating the characteristic \"loping\" sound of jazz and blues.",
        ),
        TheoryLesson(
            id = "rhythm_percussive",
            module = "Rhythm",
            title = "Percussive Techniques",
            content = "The ukulele is a percussive instrument. The body, fretboard, and strings " +
                "serve as surfaces for rhythmic articulation, allowing you to create a full " +
                "groove from a single instrument.\n\n" +
                "The Chuck (also called chunk or palm mute): The ukulele\u2019s snare drum. " +
                "Execute a downstrum but immediately damp the strings with the edge of the palm. " +
                "The goal is a sharp \"click\" with no pitched note ringing. Typically placed " +
                "on beats 2 and 4 to provide a backbeat.\n\n" +
                "Left-Hand Muting: By relaxing fretting-hand pressure without lifting the " +
                "fingers, you stop the strings from vibrating, creating a dry staccato sound. " +
                "Strumming while fully muting produces a percussive \"scratch\" (sometimes called " +
                "a Z-chord) used in funk and hip-hop styles.\n\n" +
                "The Slap: Physically hitting the strings or the body of the instrument with " +
                "the fingertips or thumb to emulate a kick drum or tom-tom. Used in modern " +
                "fingerstyle percussion playing.\n\n" +
                "These techniques transform the ukulele from a purely melodic instrument " +
                "into a rhythmic one.",
            keyPoints = listOf(
                "Chuck = palm mute immediately after strumming (the ukulele\u2019s snare drum)",
                "Left-hand muting creates staccato or scratch effects",
                "The slap emulates kick drum accents",
                "Percussive techniques are essential for reggae, blues, and acoustic pop",
            ),
            quizQuestion = "What is a 'chuck' on the ukulele?",
            quizOptions = listOf(
                "A loud open strum across all strings",
                "A percussive strum where the palm immediately mutes the strings",
                "A technique where you pluck individual strings",
                "A chord played only on the top two strings",
            ),
            quizCorrectIndex = 1,
            quizExplanation = "A chuck (or chunk/palm mute) is a percussive technique where you strum down and immediately damp the strings with the edge of your palm, producing a sharp click that acts like a snare drum hit.",
        ),
    )

    /** Returns lessons grouped by module. */
    fun byModule(): Map<String, List<TheoryLesson>> = ALL.groupBy { it.module }
}

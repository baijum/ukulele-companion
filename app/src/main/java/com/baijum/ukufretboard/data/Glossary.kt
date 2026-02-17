package com.baijum.ukufretboard.data

/**
 * A glossary entry for a music theory term.
 *
 * @property term The term being defined.
 * @property definition Concise definition.
 * @property example Optional concrete example.
 */
data class GlossaryEntry(
    val term: String,
    val definition: String,
    val example: String? = null,
)

/**
 * Music glossary covering terms used throughout the app.
 *
 * Entries are sorted alphabetically. Categories span Notes & Pitch,
 * Intervals, Scales, Chords, Harmony, Rhythm, and Ukulele-specific terms.
 */
object Glossary {

    val ALL: List<GlossaryEntry> = listOf(
        // ── A ──
        GlossaryEntry("Accidental", "A sharp (#), flat (b), or natural sign that alters a note's pitch.", "F# is an accidental applied to F."),
        GlossaryEntry("Arpeggio", "Playing the notes of a chord one at a time instead of simultaneously.", "C-E-G played sequentially is a C major arpeggio."),
        GlossaryEntry("Augmented", "A chord or interval raised by a semitone above major/perfect.", "C augmented = C-E-G#."),
        // ── B ──
        GlossaryEntry("Barre Chord", "A chord shape where one finger presses across all strings at a fret.", "F major on ukulele is a common barre chord."),
        GlossaryEntry("BPM", "Beats per minute — the speed of a piece of music.", "120 BPM is a common pop tempo."),
        GlossaryEntry("Borrowed Chord", "A chord taken from a parallel key (e.g., parallel minor) and used in another key.", "Using Fm (from C minor) in a C major progression."),
        // ── C ──
        GlossaryEntry("Cadence", "A chord progression that ends a musical phrase, creating a sense of resolution or pause.", "V → I is a perfect cadence."),
        GlossaryEntry("Capo", "A clamp placed across all strings at a fret to raise the pitch of open strings.", "Capo on fret 2 raises all strings by 2 semitones."),
        GlossaryEntry("Chord", "Three or more notes played simultaneously.", "C major (C-E-G) is a chord."),
        GlossaryEntry("Chuck", "A percussive strum where the palm damps the strings immediately after striking, producing a sharp click. Also called chunk or chnk.", "Adding a chuck on beats 2 and 4 creates a backbeat like a snare drum."),
        GlossaryEntry("Constant Motion", "A strumming technique where the hand moves like a pendulum on every beat and off-beat, missing the strings (ghost strums) to create silence rather than stopping.", "The hand always goes down on beats and up on off-beats, even during rests."),
        GlossaryEntry("Chord Formula", "The intervals that define a chord type, expressed as scale degrees.", "Major = 1, 3, 5. Minor = 1, b3, 5."),
        GlossaryEntry("Chord Progression", "A sequence of chords played in order.", "I-IV-V-I in C major = C-F-G-C."),
        GlossaryEntry("Chromatic", "Relating to all 12 notes in Western music, moving by semitones.", "The chromatic scale: C, C#, D, D#, E, F, F#, G, G#, A, A#, B."),
        GlossaryEntry("Consonance", "A combination of notes that sounds stable and pleasing.", "A perfect 5th (C-G) is consonant."),
        // ── D ──
        GlossaryEntry("Degree", "A note's position within a scale, numbered 1 through 7.", "In C major, E is the 3rd degree."),
        GlossaryEntry("Dynamics", "Variation in loudness during a performance, ranging from pianissimo (very soft) to fortissimo (very loud).", "Playing the chorus louder than the verse adds an emotional arc."),
        GlossaryEntry("Diatonic", "Belonging to or derived from a particular key's scale.", "In C major, Dm is diatonic (built from scale notes)."),
        GlossaryEntry("Diminished", "A chord or interval lowered by a semitone below minor/perfect.", "Bdim = B-D-F. The tritone (b5) is a diminished 5th."),
        GlossaryEntry("Dissonance", "A combination of notes that sounds tense or unstable, wanting to resolve.", "A minor 2nd (C-C#) is dissonant."),
        GlossaryEntry("Dominant", "The 5th degree of a scale, or the harmonic function that creates tension.", "G is the dominant in C major. V7 → I is the strongest resolution."),
        GlossaryEntry("Downbeat", "The first beat of a measure, typically the strongest.", "In 4/4 time, beat 1 is the downbeat."),
        // ── E ──
        GlossaryEntry("Enharmonic", "Two notes that sound the same but are spelled differently.", "C# and Db are enharmonic equivalents."),
        // ── G ──
        GlossaryEntry("Ghost Strum", "A strum where the hand moves through the motion but deliberately misses the strings, maintaining the rhythmic pulse without producing sound.", "In the Island Strum, beat 3 is a ghost strum that keeps the hand in position for the next upstroke."),
        // ── F ──
        GlossaryEntry("Fingering", "The specific finger placement used to play a chord or note.", "C major on ukulele: ring finger on fret 3, string A."),
        GlossaryEntry("Flat", "A symbol (b) that lowers a note by one semitone.", "Bb is one semitone below B."),
        GlossaryEntry("Fret", "A metal strip on the neck of a string instrument that divides it into semitone intervals.", "Pressing the A string at fret 2 produces a B note."),
        // ── H ──
        GlossaryEntry("Half Step", "The smallest interval in Western music, equal to one semitone.", "E to F is a half step (no fret between them)."),
        GlossaryEntry("Harmony", "The combination of simultaneously sounded notes to produce chords and chord progressions.", "Harmony is the vertical aspect of music (melody is horizontal)."),
        // ── I ──
        GlossaryEntry("Interval", "The distance between two notes, measured in semitones.", "C to G = 7 semitones = Perfect 5th."),
        GlossaryEntry("Inversion", "A chord voicing where a note other than the root is the lowest note.", "C/E (1st inversion) = E-G-C instead of C-E-G."),
        // ── K ──
        GlossaryEntry("Key", "A group of notes centered around a tonic that form the basis of a piece of music.", "A song in the key of G major uses notes from the G major scale."),
        GlossaryEntry("Key Signature", "The set of sharps or flats at the beginning of a piece that indicates its key.", "G major has 1 sharp (F#)."),
        // ── L ──
        GlossaryEntry("Low-G Tuning", "A ukulele tuning where the G string is tuned an octave lower than standard, extending the range downward.", "Low-G: G3-C4-E4-A4 vs standard G4-C4-E4-A4."),
        // ── M ──
        GlossaryEntry("Major", "A scale or chord quality that sounds bright and happy. Based on the interval pattern W-W-H-W-W-W-H.", "C major scale: C-D-E-F-G-A-B."),
        GlossaryEntry("Measure", "A segment of time in music defined by a given number of beats. Also called a bar.", "In 4/4 time, each measure has 4 beats."),
        GlossaryEntry("Minor", "A scale or chord quality that sounds dark and sad. Based on the interval pattern W-H-W-W-H-W-W.", "A minor scale: A-B-C-D-E-F-G."),
        GlossaryEntry("Modal Interchange", "Borrowing chords from a parallel mode (e.g., using chords from C minor in a C major progression).", "Using bVI (Ab) in C major borrows from C natural minor."),
        GlossaryEntry("Mode", "A scale derived by starting the major scale pattern on a different degree.", "Dorian = major scale starting on the 2nd degree."),
        GlossaryEntry("Modulation", "Changing from one key to another within a piece of music.", "A song might modulate from C major to G major."),
        // ── N ──
        GlossaryEntry("Natural", "A note without a sharp or flat. Also a symbol that cancels a previous accidental.", "C natural, D natural, E natural..."),
        GlossaryEntry("Nut", "The small piece at the top of the fretboard that the strings rest on. Defines the open string pitch.", "The nut is at fret 0."),
        // ── O ──
        GlossaryEntry("Octave", "The interval of 12 semitones. Two notes an octave apart sound 'the same' but higher/lower.", "C4 to C5 is one octave."),
        GlossaryEntry("Open Chord", "A chord shape that includes one or more open (unfretted) strings.", "C major (0003) on ukulele is an open chord."),
        // ── P ──
        GlossaryEntry("Palm Mute", "Resting the edge of the strumming hand on the strings near the bridge to dampen vibration, producing a muted, percussive tone.", "Palm muting while strumming creates a tight, controlled sound often used in rock and blues."),
        GlossaryEntry("Pentatonic", "A five-note scale commonly used in many musical traditions.", "C pentatonic major: C-D-E-G-A."),
        GlossaryEntry("Pitch", "How high or low a note sounds, determined by its frequency.", "A4 = 440 Hz."),
        GlossaryEntry("Pitch Class", "A number (0–11) representing a note regardless of octave. C=0, C#=1, ..., B=11.", "All C notes share pitch class 0."),
        // ── R ──
        GlossaryEntry("Rasgueado", "A flamenco strumming technique where fingers are flicked out from the palm in rapid succession using extensor muscles, creating an explosive, continuous sheet of sound.", "A 4-stroke rasgueado fans out pinky, ring, middle, then index finger in rapid fire."),
        GlossaryEntry("Re-entrant Tuning", "A tuning where strings are not ordered from lowest to highest pitch. Standard ukulele tuning (G4-C4-E4-A4) is re-entrant because the G string is higher than C.", "The high G gives ukulele its characteristic jangly sound."),
        GlossaryEntry("Resolution", "The movement from a tense or dissonant chord/note to a stable one.", "G7 → C (V7 → I) is a resolution."),
        GlossaryEntry("Roman Numeral", "A notation system for chord degrees in a key. Uppercase = major, lowercase = minor.", "I-IV-V-I = Tonic-Subdominant-Dominant-Tonic."),
        GlossaryEntry("Root", "The fundamental note of a chord or scale that gives it its name.", "In a C major chord (C-E-G), C is the root."),
        // ── S ──
        GlossaryEntry("Scale", "An ordered sequence of notes with a specific interval pattern.", "Major scale pattern: W-W-H-W-W-W-H."),
        GlossaryEntry("Secondary Dominant", "The V chord of a diatonic chord other than I, used for temporary tonicization.", "D7 (V/V) before G in C major."),
        GlossaryEntry("Semitone", "The smallest interval in Western music. One fret on a string instrument.", "C to C# is one semitone."),
        GlossaryEntry("Sharp", "A symbol (#) that raises a note by one semitone.", "F# is one semitone above F."),
        GlossaryEntry("Skank", "The short, staccato off-beat strum that defines reggae and ska rhythms. Also called the chop.", "The reggae skank leaves downbeats silent for the bass guitar or kick drum."),
        GlossaryEntry("Straight Feel", "An even subdivision of the beat where down and up strums each occupy exactly 50% of the beat duration. Common in rock, Latin, and modern pop.", "Count \"1-and-2-and\" with equal spacing for straight feel."),
        GlossaryEntry("Strum", "A technique of playing multiple strings in a sweeping motion.", "A down-strum across all 4 ukulele strings."),
        GlossaryEntry("Subdominant", "The 4th degree of a scale, or the harmonic function that creates forward motion.", "F is the subdominant in C major."),
        GlossaryEntry("Suspended Chord", "A chord where the 3rd is replaced by a 2nd (sus2) or 4th (sus4), creating an ambiguous sound.", "Csus4 = C-F-G."),
        GlossaryEntry("Sweet Spot", "The optimal strumming location where the neck meets the body (around the 12th fret), producing the warmest and most resonant tone.", "Strumming at the sweet spot activates the soundboard most efficiently."),
        GlossaryEntry("Swing Feel", "An uneven subdivision where the downbeat note is lengthened and the upbeat shortened (approximately 2:1 ratio based on triplets). Essential for jazz, blues, and Hawaiian swing.", "Vocalize \"Hump-ty Dump-ty\" to feel the swing rhythm."),
        GlossaryEntry("Syncopation", "Emphasis on beats or parts of beats that are normally weak.", "Playing on the 'and' instead of the beat."),
        // ── T ──
        GlossaryEntry("Tempo", "The speed of a piece of music, usually measured in BPM.", "Allegro = fast (120–156 BPM)."),
        GlossaryEntry("Time Signature", "A notation indicating how many beats per measure and which note value gets one beat.", "4/4 = four quarter-note beats per measure."),
        GlossaryEntry("Tonic", "The first degree of a scale, the 'home' note. Also the harmonic function of stability and rest.", "C is the tonic in C major."),
        GlossaryEntry("Transposition", "Moving a piece of music to a different key while preserving the intervals.", "Transposing from C to G raises everything by 7 semitones."),
        GlossaryEntry("Triad", "A three-note chord built by stacking two intervals of a 3rd.", "C major triad = C-E-G (root + M3 + P5)."),
        GlossaryEntry("Tritone", "An interval of 6 semitones (3 whole steps). Sounds very tense and dissonant.", "C to F# is a tritone. Also called diminished 5th or augmented 4th."),
        GlossaryEntry("Tritone Substitution", "Replacing a dominant 7th chord with another dominant 7th a tritone away.", "Replace G7 with Db7 (both share the B-F tritone)."),
        // ── U ──
        GlossaryEntry("Upbeat", "The last beat of a measure, or the 'and' between beats.", "Reggae strumming emphasizes the upbeat."),
        // ── V ──
        GlossaryEntry("Voice Leading", "The smooth movement of individual notes from one chord to the next, minimizing large jumps.", "C→Am: E stays, G moves to A, C stays."),
        GlossaryEntry("Voicing", "The specific arrangement of notes in a chord, including which octave each note is in.", "C major can be voiced as C-E-G or E-G-C (1st inversion)."),
        // ── W ──
        GlossaryEntry("Whole Step", "An interval of two semitones (two frets on a string instrument).", "C to D is a whole step."),
    ).sortedBy { it.term }
}

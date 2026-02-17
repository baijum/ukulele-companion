package com.baijum.ukufretboard.data

/**
 * Built-in database of popular ukulele songs with their chord sets.
 *
 * Used by the Song Finder feature to suggest songs based on chords
 * the user already knows.
 */
object SongChordDatabase {

    /**
     * A song entry with its chord requirements.
     *
     * @property title Song title.
     * @property artist Artist name.
     * @property chords Set of chord names needed to play the song.
     * @property difficulty Approximate difficulty level.
     * @property genre Musical genre.
     */
    data class SongEntry(
        val title: String,
        val artist: String,
        val chords: Set<String>,
        val difficulty: Difficulty,
        val genre: String,
    )

    enum class Difficulty(val label: String) {
        BEGINNER("Beginner"),
        INTERMEDIATE("Intermediate"),
        ADVANCED("Advanced"),
    }

    /**
     * Curated list of popular ukulele songs with their chords.
     */
    val SONGS: List<SongEntry> = listOf(
        // ── Beginner (2-3 chords) ──────────────────────────────────
        SongEntry("Riptide", "Vance Joy", setOf("Am", "G", "C", "F"), Difficulty.BEGINNER, "Pop"),
        SongEntry("I'm Yours", "Jason Mraz", setOf("C", "G", "Am", "F"), Difficulty.BEGINNER, "Pop"),
        SongEntry("Somewhere Over the Rainbow", "Israel Kamakawiwo'ole", setOf("C", "Em", "Am", "F", "G"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Three Little Birds", "Bob Marley", setOf("A", "D", "E"), Difficulty.BEGINNER, "Reggae"),
        SongEntry("You Are My Sunshine", "Traditional", setOf("C", "F", "G"), Difficulty.BEGINNER, "Folk"),
        SongEntry("Stand By Me", "Ben E. King", setOf("C", "Am", "F", "G"), Difficulty.BEGINNER, "Soul"),
        SongEntry("Let It Be", "The Beatles", setOf("C", "G", "Am", "F"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Lean On Me", "Bill Withers", setOf("C", "F", "G"), Difficulty.BEGINNER, "Soul"),
        SongEntry("Leaving on a Jet Plane", "John Denver", setOf("C", "F", "G"), Difficulty.BEGINNER, "Folk"),
        SongEntry("Happy Birthday", "Traditional", setOf("C", "F", "G"), Difficulty.BEGINNER, "Traditional"),
        SongEntry("Love Me Do", "The Beatles", setOf("G", "C", "D"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Bad Moon Rising", "CCR", setOf("G", "C", "D"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Jambalaya", "Hank Williams", setOf("C", "G"), Difficulty.BEGINNER, "Country"),
        SongEntry("Achy Breaky Heart", "Billy Ray Cyrus", setOf("A", "E"), Difficulty.BEGINNER, "Country"),
        SongEntry("Twist and Shout", "The Beatles", setOf("D", "G", "A"), Difficulty.BEGINNER, "Rock"),
        SongEntry("La Bamba", "Ritchie Valens", setOf("C", "F", "G"), Difficulty.BEGINNER, "Latin"),
        SongEntry("What I Got", "Sublime", setOf("D", "G"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Wagon Wheel", "Old Crow Medicine Show", setOf("G", "D", "Em", "C"), Difficulty.BEGINNER, "Folk"),
        SongEntry("Ho Hey", "The Lumineers", setOf("C", "F", "Am", "G"), Difficulty.BEGINNER, "Folk"),
        SongEntry("Count on Me", "Bruno Mars", setOf("C", "Em", "Am", "G", "F"), Difficulty.BEGINNER, "Pop"),

        // ── Intermediate (3-5 chords, some barre) ─────────────────
        SongEntry("Hey Soul Sister", "Train", setOf("C", "G", "Am", "F"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("Hallelujah", "Leonard Cohen", setOf("C", "Am", "F", "G", "E"), Difficulty.INTERMEDIATE, "Folk"),
        SongEntry("Can't Help Falling in Love", "Elvis Presley", setOf("C", "Em", "Am", "F", "G", "A7"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("House of the Rising Sun", "The Animals", setOf("Am", "C", "D", "F", "E"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Have You Ever Seen the Rain", "CCR", setOf("C", "G", "Am", "F"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Redemption Song", "Bob Marley", setOf("G", "Em", "C", "Am", "D"), Difficulty.INTERMEDIATE, "Reggae"),
        SongEntry("Perfect", "Ed Sheeran", setOf("G", "Em", "C", "D"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("Someone Like You", "Adele", setOf("G", "D", "Em", "C"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("Hey Jude", "The Beatles", setOf("C", "G", "F", "Am", "Dm"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Blowin' in the Wind", "Bob Dylan", setOf("C", "F", "G"), Difficulty.INTERMEDIATE, "Folk"),
        SongEntry("Peaceful Easy Feeling", "Eagles", setOf("E", "A", "B7"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Photograph", "Ed Sheeran", setOf("G", "C", "D", "Em"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("Wish You Were Here", "Pink Floyd", setOf("G", "Em", "A", "C", "D"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Brown Eyed Girl", "Van Morrison", setOf("G", "C", "D", "Em"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Ring of Fire", "Johnny Cash", setOf("G", "C", "D"), Difficulty.INTERMEDIATE, "Country"),

        // ── More diverse ──────────────────────────────────────────
        SongEntry("Coconut", "Harry Nilsson", setOf("C", "G7"), Difficulty.BEGINNER, "Pop"),
        SongEntry("Pearly Shells", "Don Ho", setOf("C", "F", "G7"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Tiny Bubbles", "Don Ho", setOf("C", "F", "G7", "C7"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Over the Rainbow / What a Wonderful World", "Israel Kamakawiwo'ole", setOf("C", "Em", "Am", "F", "G"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Yellow Submarine", "The Beatles", setOf("G", "D", "C", "Em", "Am"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Octopus's Garden", "The Beatles", setOf("C", "Am", "F", "G"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Creep", "Radiohead", setOf("G", "B", "C", "Cm"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Wonderwall", "Oasis", setOf("Em", "G", "D", "A7sus4", "C"), Difficulty.INTERMEDIATE, "Rock"),
        SongEntry("Budapest", "George Ezra", setOf("G", "C", "D"), Difficulty.BEGINNER, "Pop"),
        SongEntry("All Along the Watchtower", "Bob Dylan", setOf("Am", "G", "F"), Difficulty.BEGINNER, "Rock"),
        SongEntry("No Woman No Cry", "Bob Marley", setOf("C", "G", "Am", "F"), Difficulty.BEGINNER, "Reggae"),
        SongEntry("Africa", "Toto", setOf("A", "C#m", "F#m", "D", "E"), Difficulty.ADVANCED, "Pop"),
        SongEntry("Don't Worry Be Happy", "Bobby McFerrin", setOf("C", "Dm", "F"), Difficulty.BEGINNER, "Pop"),
        SongEntry("Moon River", "Andy Williams", setOf("C", "Am", "F", "G", "Em", "Dm"), Difficulty.INTERMEDIATE, "Jazz"),
        SongEntry("Take Me Home Country Roads", "John Denver", setOf("G", "Em", "D", "C"), Difficulty.BEGINNER, "Country"),

        // ── Hawaiian / Island ───────────────────────────────────────
        SongEntry("Aloha Oe", "Queen Lili'uokalani", setOf("C", "F", "G", "G7"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Hawaiian War Chant", "Traditional", setOf("C", "G7"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Blue Hawaii", "Elvis Presley", setOf("C", "F", "G7", "A7", "Dm"), Difficulty.INTERMEDIATE, "Hawaiian"),
        SongEntry("Hanalei Moon", "Bob Nelson", setOf("C", "F", "G7", "C7"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Lovely Hula Hands", "R. Alex Anderson", setOf("C", "F", "G", "Am"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("White Sandy Beach", "Israel Kamakawiwo'ole", setOf("C", "F", "G", "Am"), Difficulty.BEGINNER, "Hawaiian"),
        SongEntry("Henehene Kou Aka", "Traditional", setOf("C", "F", "G7"), Difficulty.BEGINNER, "Hawaiian"),

        // ── Jazz Standards ──────────────────────────────────────────
        SongEntry("Fly Me to the Moon", "Frank Sinatra", setOf("Am", "Dm", "G7", "C", "F", "E7"), Difficulty.INTERMEDIATE, "Jazz"),
        SongEntry("What a Wonderful World", "Louis Armstrong", setOf("C", "Em", "F", "Am", "Dm", "G"), Difficulty.INTERMEDIATE, "Jazz"),
        SongEntry("Autumn Leaves", "Joseph Kosma", setOf("Am", "D7", "G", "C", "F", "B7", "Em"), Difficulty.ADVANCED, "Jazz"),
        SongEntry("The Girl from Ipanema", "Antonio Carlos Jobim", setOf("F", "G7", "Gm7", "Gb7"), Difficulty.ADVANCED, "Jazz"),
        SongEntry("All of Me", "John Legend", setOf("Am", "F", "C", "G"), Difficulty.BEGINNER, "Jazz"),
        SongEntry("Dream a Little Dream of Me", "The Mamas & The Papas", setOf("C", "B7", "Am", "G", "F", "E7"), Difficulty.INTERMEDIATE, "Jazz"),
        SongEntry("Blue Moon", "Rodgers & Hart", setOf("C", "Am", "F", "G"), Difficulty.BEGINNER, "Jazz"),
        SongEntry("Georgia on My Mind", "Ray Charles", setOf("C", "E7", "Am", "F", "G", "Dm"), Difficulty.ADVANCED, "Jazz"),

        // ── Country ─────────────────────────────────────────────────
        SongEntry("Jolene", "Dolly Parton", setOf("Am", "C", "G", "Em"), Difficulty.BEGINNER, "Country"),
        SongEntry("Folsom Prison Blues", "Johnny Cash", setOf("E", "A", "B7"), Difficulty.BEGINNER, "Country"),
        SongEntry("I Walk the Line", "Johnny Cash", setOf("A", "D", "E"), Difficulty.BEGINNER, "Country"),
        SongEntry("Tennessee Whiskey", "Chris Stapleton", setOf("A", "Bm", "D", "E"), Difficulty.INTERMEDIATE, "Country"),
        SongEntry("Friends in Low Places", "Garth Brooks", setOf("A", "Bm", "D", "E"), Difficulty.INTERMEDIATE, "Country"),
        SongEntry("Hey Good Lookin'", "Hank Williams", setOf("C", "D7", "G"), Difficulty.BEGINNER, "Country"),
        SongEntry("Your Cheatin' Heart", "Hank Williams", setOf("C", "F", "G7", "D7"), Difficulty.BEGINNER, "Country"),

        // ── Latin / Bossa Nova ──────────────────────────────────────
        SongEntry("Guantanamera", "Traditional", setOf("C", "F", "G"), Difficulty.BEGINNER, "Latin"),
        SongEntry("Cielito Lindo", "Traditional", setOf("C", "G7", "F"), Difficulty.BEGINNER, "Latin"),
        SongEntry("Besame Mucho", "Consuelo Velazquez", setOf("Dm", "A7", "Gm", "D7"), Difficulty.INTERMEDIATE, "Latin"),
        SongEntry("Oye Como Va", "Santana", setOf("Am", "D"), Difficulty.BEGINNER, "Latin"),
        SongEntry("Mas Que Nada", "Jorge Ben", setOf("Am", "E7", "Dm", "G"), Difficulty.INTERMEDIATE, "Latin"),
        SongEntry("De Colores", "Traditional", setOf("C", "F", "G7"), Difficulty.BEGINNER, "Latin"),

        // ── Traditional / Folk ──────────────────────────────────────
        SongEntry("Amazing Grace", "Traditional", setOf("G", "C", "D"), Difficulty.BEGINNER, "Traditional"),
        SongEntry("This Land Is Your Land", "Woody Guthrie", setOf("C", "F", "G"), Difficulty.BEGINNER, "Folk"),
        SongEntry("Home on the Range", "Traditional", setOf("C", "F", "G7", "C7"), Difficulty.BEGINNER, "Traditional"),
        SongEntry("Oh! Susanna", "Stephen Foster", setOf("C", "F", "G"), Difficulty.BEGINNER, "Traditional"),
        SongEntry("Kumbaya", "Traditional", setOf("C", "F", "G"), Difficulty.BEGINNER, "Traditional"),
        SongEntry("Scarborough Fair", "Traditional", setOf("Am", "G", "C", "D"), Difficulty.BEGINNER, "Folk"),
        SongEntry("Danny Boy", "Traditional", setOf("C", "F", "G", "Am"), Difficulty.BEGINNER, "Traditional"),
        SongEntry("Greensleeves", "Traditional", setOf("Am", "G", "Em", "C"), Difficulty.INTERMEDIATE, "Traditional"),
        SongEntry("Auld Lang Syne", "Traditional", setOf("C", "F", "G", "Am"), Difficulty.BEGINNER, "Traditional"),
        SongEntry("Down in the Valley", "Traditional", setOf("G", "D"), Difficulty.BEGINNER, "Folk"),

        // ── Pop / Modern ────────────────────────────────────────────
        SongEntry("Thinking Out Loud", "Ed Sheeran", setOf("D", "G", "A", "Bm"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("Shallow", "Lady Gaga & Bradley Cooper", setOf("G", "Am", "C", "D", "Em"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("Shape of You", "Ed Sheeran", setOf("Am", "Dm", "F", "G"), Difficulty.INTERMEDIATE, "Pop"),
        SongEntry("Let Her Go", "Passenger", setOf("G", "Em", "C", "D"), Difficulty.BEGINNER, "Pop"),
        SongEntry("Viva la Vida", "Coldplay", setOf("C", "D", "G", "Em"), Difficulty.BEGINNER, "Pop"),
        SongEntry("Hey There Delilah", "Plain White T's", setOf("D", "F#m", "Bm", "G", "A"), Difficulty.INTERMEDIATE, "Pop"),

        // ── Reggae ──────────────────────────────────────────────────
        SongEntry("Stir It Up", "Bob Marley", setOf("A", "D", "E"), Difficulty.BEGINNER, "Reggae"),
        SongEntry("Is This Love", "Bob Marley", setOf("F#m", "D", "A", "E"), Difficulty.INTERMEDIATE, "Reggae"),
        SongEntry("Red Red Wine", "UB40", setOf("C", "F", "G"), Difficulty.BEGINNER, "Reggae"),

        // ── Soul / R&B ──────────────────────────────────────────────
        SongEntry("Ain't No Sunshine", "Bill Withers", setOf("Am", "Em", "Dm"), Difficulty.BEGINNER, "Soul"),
        SongEntry("Sitting on the Dock of the Bay", "Otis Redding", setOf("G", "B7", "C", "A"), Difficulty.INTERMEDIATE, "Soul"),
        SongEntry("What's Going On", "Marvin Gaye", setOf("E", "C#m", "F#m", "B"), Difficulty.ADVANCED, "Soul"),

        // ── Rock / Classic ──────────────────────────────────────────
        SongEntry("Knockin' on Heaven's Door", "Bob Dylan", setOf("G", "D", "Am", "C"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Horse with No Name", "America", setOf("Em", "D6"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Free Fallin'", "Tom Petty", setOf("D", "G", "A"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Sweet Home Alabama", "Lynyrd Skynyrd", setOf("D", "C", "G"), Difficulty.BEGINNER, "Rock"),
        SongEntry("Hotel California", "Eagles", setOf("Am", "E", "G", "D", "F", "C", "Dm"), Difficulty.ADVANCED, "Rock"),

        // ── Advanced ────────────────────────────────────────────────
        SongEntry("Blackbird", "The Beatles", setOf("G", "Am", "C", "D", "A7", "Cm"), Difficulty.ADVANCED, "Rock"),
        SongEntry("Tears in Heaven", "Eric Clapton", setOf("A", "E", "F#m", "D", "Bm"), Difficulty.ADVANCED, "Pop"),
        SongEntry("While My Guitar Gently Weeps", "The Beatles", setOf("Am", "G", "D", "E", "C", "A"), Difficulty.ADVANCED, "Rock"),
    )

    /**
     * Creates a template [ChordSheet] from a [SongEntry].
     *
     * The sheet contains the song's chords as inline markers
     * with a placeholder for the user to add lyrics.
     */
    fun toChordSheetTemplate(song: SongEntry): ChordSheet {
        val chordLine = song.chords.joinToString("  ") { "[$it]" }
        val content = "Chords: $chordLine\n\n(Add your lyrics here)"
        return ChordSheet(title = song.title, artist = song.artist, content = content)
    }

    /**
     * Finds songs that the user can play with their known chords.
     *
     * @param knownChords Set of chord names the user knows.
     * @return Songs where all required chords are in the known set, sorted by chord count.
     */
    fun findPlayable(knownChords: Set<String>): List<SongEntry> =
        SONGS.filter { song ->
            song.chords.all { chord -> chord in knownChords }
        }.sortedBy { it.chords.size }

    /**
     * Finds songs where the user is missing just a few chords.
     *
     * @param knownChords Set of chord names the user knows.
     * @param maxMissing Maximum number of missing chords to allow (default 1).
     * @return Pairs of (song, missing chords), sorted by number of missing chords.
     */
    fun findAlmostPlayable(
        knownChords: Set<String>,
        maxMissing: Int = 1,
    ): List<Pair<SongEntry, Set<String>>> =
        SONGS.mapNotNull { song ->
            val missing = song.chords - knownChords
            if (missing.isNotEmpty() && missing.size <= maxMissing) {
                song to missing
            } else {
                null
            }
        }.sortedBy { it.second.size }
}

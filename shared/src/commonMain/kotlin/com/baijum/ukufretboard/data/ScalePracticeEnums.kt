package com.baijum.ukufretboard.data

enum class PracticeMode(val label: String) {
    PLAY_ALONG("Play Along"),
    QUIZ("Scale Quiz"),
    EAR_TRAINING("Ear Training"),
}

enum class PlayDirection(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending"),
    BOTH("Both"),
}

enum class FretPosition(val label: String) {
    ALL("All"),
    OPEN("Open"),
    MID("Mid"),
    HIGH("High");

    /** Returns the fret range for this position, or null for ALL. */
    fun range(lastFret: Int = 12): IntRange? = when (this) {
        ALL -> null
        OPEN -> 0..4
        MID -> 4..8
        HIGH -> 7..lastFret
    }
}

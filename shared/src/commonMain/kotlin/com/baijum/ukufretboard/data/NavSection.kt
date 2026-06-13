package com.baijum.ukufretboard.data

/**
 * Navigation sections in the app. Each entry has a stable [id] used for
 * persisting the selected section across configuration changes and process death.
 */
enum class NavSection(val id: Int) {
    EXPLORER(0),
    LIBRARY(1),
    PATTERNS(2),
    PROGRESSIONS(3),
    FAVORITES(4),
    SONGBOOK(5),
    CAPO_GUIDE(6),
    CIRCLE_OF_FIFTHS(7),
    THEORY_QUIZ(8),
    INTERVAL_TRAINER(9),
    CHORD_SUBS(10),
    THEORY_LESSONS(11),
    MELODY_NOTEPAD(12),
    TUNER(13),
    LEARNING_PROGRESS(14),
    SCALE_CHORDS(15),
    GLOSSARY(16),
    NOTE_MAP(17),
    NOTE_QUIZ(18),
    CHORD_EAR(19),
    HELP(20),
    PITCH_MONITOR(21),
    SCALE_PRACTICE(22),
    ACHIEVEMENTS(23),
    CHORD_TRANSITION(25),
    DAILY_CHALLENGE(26),
    SONGWRITER_MODE(27),
    PRACTICE_ROUTINE(28),
    PLAY_ALONG(29),
    METRONOME(30),
    SETLISTS(31),
    ;

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: Int): NavSection? = byId[id]
    }
}

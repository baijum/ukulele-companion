package com.baijum.ukufretboard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.viewmodel.ChordLibraryViewModel

internal const val NAV_EXPLORER = 0
internal const val NAV_LIBRARY = 1
internal const val NAV_PATTERNS = 2
internal const val NAV_PROGRESSIONS = 3
internal const val NAV_FAVORITES = 4
internal const val NAV_SONGBOOK = 5
internal const val NAV_CAPO_GUIDE = 6
internal const val NAV_CIRCLE_OF_FIFTHS = 7
internal const val NAV_THEORY_QUIZ = 8
internal const val NAV_INTERVAL_TRAINER = 9
internal const val NAV_CHORD_SUBS = 10
internal const val NAV_THEORY_LESSONS = 11
internal const val NAV_MELODY_NOTEPAD = 12
internal const val NAV_TUNER = 13
internal const val NAV_LEARNING_PROGRESS = 14
internal const val NAV_SCALE_CHORDS = 15
internal const val NAV_GLOSSARY = 16
internal const val NAV_NOTE_MAP = 17
internal const val NAV_NOTE_QUIZ = 18
internal const val NAV_CHORD_EAR = 19
internal const val NAV_HELP = 20
internal const val NAV_PITCH_MONITOR = 21
internal const val NAV_SCALE_PRACTICE = 22
internal const val NAV_ACHIEVEMENTS = 23
internal const val NAV_CHORD_TRANSITION = 25
internal const val NAV_DAILY_CHALLENGE = 26
internal const val NAV_SONGWRITER_MODE = 27
internal const val NAV_PRACTICE_ROUTINE = 28
internal const val NAV_PLAY_ALONG = 29
internal const val NAV_METRONOME = 30
internal const val NAV_SETLISTS = 31

internal val PLAY_CREATE_NAV_INDICES = setOf(
    NAV_EXPLORER, NAV_TUNER, NAV_PITCH_MONITOR, NAV_METRONOME, NAV_LIBRARY, NAV_FAVORITES,
    NAV_SONGWRITER_MODE, NAV_SONGBOOK, NAV_MELODY_NOTEPAD, NAV_PATTERNS, NAV_PROGRESSIONS,
)

internal data class DrawerItem(
    val index: Int,
    val label: String,
    val icon: ImageVector,
)

internal data class DrawerSection(
    val title: String,
    val items: List<DrawerItem>,
)

internal data class SheetVoicingInfo(
    val rootPitchClass: Int,
    val chordSymbol: String,
    val frets: List<Int>,
)

@Composable
internal fun drawerSections(): List<DrawerSection> = listOf(
    DrawerSection(stringResource(R.string.nav_section_play), listOf(
        DrawerItem(NAV_EXPLORER, stringResource(R.string.nav_explorer), Icons.Filled.Home),
        DrawerItem(NAV_TUNER, stringResource(R.string.nav_tuner), Icons.Filled.Mic),
        DrawerItem(NAV_PITCH_MONITOR, stringResource(R.string.nav_pitch_monitor), Icons.Filled.Equalizer),
        DrawerItem(NAV_METRONOME, stringResource(R.string.nav_metronome), Icons.Filled.Speed),
        DrawerItem(NAV_LIBRARY, stringResource(R.string.nav_chords), Icons.Filled.Search),
        DrawerItem(NAV_FAVORITES, stringResource(R.string.nav_favorites), Icons.Filled.Favorite),
    )),
    DrawerSection(stringResource(R.string.nav_section_create), listOf(
        DrawerItem(NAV_SONGWRITER_MODE, stringResource(R.string.nav_songwriter_mode), Icons.Filled.Create),
        DrawerItem(NAV_SONGBOOK, stringResource(R.string.nav_songs), Icons.Filled.Create),
        DrawerItem(NAV_SETLISTS, stringResource(R.string.nav_setlists), Icons.AutoMirrored.Filled.List),
        DrawerItem(NAV_MELODY_NOTEPAD, stringResource(R.string.nav_melody_notepad), Icons.Filled.Create),
        DrawerItem(NAV_PATTERNS, stringResource(R.string.nav_patterns), Icons.AutoMirrored.Filled.List),
        DrawerItem(NAV_PROGRESSIONS, stringResource(R.string.nav_progressions), Icons.Filled.PlayArrow),
    )),
    DrawerSection(stringResource(R.string.nav_section_learn), listOf(
        DrawerItem(NAV_THEORY_LESSONS, stringResource(R.string.nav_learn_theory), Icons.Filled.Info),
        DrawerItem(NAV_THEORY_QUIZ, stringResource(R.string.nav_theory_quiz), Icons.Filled.Create),
        DrawerItem(NAV_INTERVAL_TRAINER, stringResource(R.string.nav_interval_trainer), Icons.Filled.PlayArrow),
        DrawerItem(NAV_NOTE_QUIZ, stringResource(R.string.nav_note_quiz), Icons.Filled.Search),
        DrawerItem(NAV_CHORD_EAR, stringResource(R.string.nav_chord_ear_training), Icons.Filled.PlayArrow),
        DrawerItem(NAV_SCALE_PRACTICE, stringResource(R.string.nav_scale_practice), Icons.Filled.PlayArrow),
        DrawerItem(NAV_LEARNING_PROGRESS, stringResource(R.string.nav_progress), Icons.Filled.Favorite),
        DrawerItem(NAV_DAILY_CHALLENGE, stringResource(R.string.nav_daily_challenge), Icons.Filled.Star),
        DrawerItem(NAV_PRACTICE_ROUTINE, stringResource(R.string.nav_practice_routine), Icons.Filled.PlayArrow),
        DrawerItem(NAV_CHORD_TRANSITION, stringResource(R.string.nav_chord_transitions), Icons.Filled.PlayArrow),
        DrawerItem(NAV_PLAY_ALONG, stringResource(R.string.nav_play_along), Icons.Filled.Mic),
        DrawerItem(NAV_ACHIEVEMENTS, stringResource(R.string.nav_achievements), Icons.Filled.Star),
    )),
    DrawerSection(stringResource(R.string.nav_section_reference), listOf(
        DrawerItem(NAV_CAPO_GUIDE, stringResource(R.string.nav_capo_guide), Icons.Filled.Info),
        DrawerItem(NAV_CIRCLE_OF_FIFTHS, stringResource(R.string.nav_circle_of_fifths), Icons.Filled.Refresh),
        DrawerItem(NAV_CHORD_SUBS, stringResource(R.string.nav_chord_substitutions), Icons.AutoMirrored.Filled.List),
        DrawerItem(NAV_SCALE_CHORDS, stringResource(R.string.nav_chords_in_scale), Icons.Filled.PlayArrow),
        DrawerItem(NAV_NOTE_MAP, stringResource(R.string.nav_fretboard_notes), Icons.Filled.Search),
        DrawerItem(NAV_GLOSSARY, stringResource(R.string.nav_glossary), Icons.Filled.Info),
    )),
)

internal fun navigateToChord(
    chordName: String,
    libraryViewModel: ChordLibraryViewModel,
    switchTab: () -> Unit,
) {
    val result = ChordNameParser.parse(chordName) ?: return
    libraryViewModel.selectRoot(result.rootPitchClass)
    libraryViewModel.selectCategory(result.formula.category)
    libraryViewModel.selectFormula(result.formula)
    switchTab()
}

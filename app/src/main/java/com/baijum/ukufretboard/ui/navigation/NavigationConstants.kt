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
import com.baijum.ukufretboard.data.NavSection
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.viewmodel.ChordLibraryViewModel

internal data class DrawerItem(
    val section: NavSection,
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
        DrawerItem(NavSection.EXPLORER, stringResource(R.string.nav_explorer), Icons.Filled.Home),
        DrawerItem(NavSection.TUNER, stringResource(R.string.nav_tuner), Icons.Filled.Mic),
        DrawerItem(NavSection.PITCH_MONITOR, stringResource(R.string.nav_pitch_monitor), Icons.Filled.Equalizer),
        DrawerItem(NavSection.METRONOME, stringResource(R.string.nav_metronome), Icons.Filled.Speed),
        DrawerItem(NavSection.LIBRARY, stringResource(R.string.nav_chords), Icons.Filled.Search),
        DrawerItem(NavSection.FAVORITES, stringResource(R.string.nav_favorites), Icons.Filled.Favorite),
    )),
    DrawerSection(stringResource(R.string.nav_section_create), listOf(
        DrawerItem(NavSection.SONGWRITER_MODE, stringResource(R.string.nav_songwriter_mode), Icons.Filled.Create),
        DrawerItem(NavSection.SONGBOOK, stringResource(R.string.nav_songs), Icons.Filled.Create),
        DrawerItem(NavSection.SETLISTS, stringResource(R.string.nav_setlists), Icons.AutoMirrored.Filled.List),
        DrawerItem(NavSection.MELODY_NOTEPAD, stringResource(R.string.nav_melody_notepad), Icons.Filled.Create),
        DrawerItem(NavSection.PATTERNS, stringResource(R.string.nav_patterns), Icons.AutoMirrored.Filled.List),
        DrawerItem(NavSection.PROGRESSIONS, stringResource(R.string.nav_progressions), Icons.Filled.PlayArrow),
    )),
    DrawerSection(stringResource(R.string.nav_section_learn), listOf(
        DrawerItem(NavSection.THEORY_LESSONS, stringResource(R.string.nav_learn_theory), Icons.Filled.Info),
        DrawerItem(NavSection.THEORY_QUIZ, stringResource(R.string.nav_theory_quiz), Icons.Filled.Create),
        DrawerItem(NavSection.INTERVAL_TRAINER, stringResource(R.string.nav_interval_trainer), Icons.Filled.PlayArrow),
        DrawerItem(NavSection.NOTE_QUIZ, stringResource(R.string.nav_note_quiz), Icons.Filled.Search),
        DrawerItem(NavSection.CHORD_EAR, stringResource(R.string.nav_chord_ear_training), Icons.Filled.PlayArrow),
        DrawerItem(NavSection.SCALE_PRACTICE, stringResource(R.string.nav_scale_practice), Icons.Filled.PlayArrow),
        DrawerItem(NavSection.LEARNING_PROGRESS, stringResource(R.string.nav_progress), Icons.Filled.Favorite),
        DrawerItem(NavSection.DAILY_CHALLENGE, stringResource(R.string.nav_daily_challenge), Icons.Filled.Star),
        DrawerItem(NavSection.PRACTICE_ROUTINE, stringResource(R.string.nav_practice_routine), Icons.Filled.PlayArrow),
        DrawerItem(NavSection.CHORD_TRANSITION, stringResource(R.string.nav_chord_transitions), Icons.Filled.PlayArrow),
        DrawerItem(NavSection.PLAY_ALONG, stringResource(R.string.nav_play_along), Icons.Filled.Mic),
        DrawerItem(NavSection.ACHIEVEMENTS, stringResource(R.string.nav_achievements), Icons.Filled.Star),
    )),
    DrawerSection(stringResource(R.string.nav_section_reference), listOf(
        DrawerItem(NavSection.CAPO_GUIDE, stringResource(R.string.nav_capo_guide), Icons.Filled.Info),
        DrawerItem(NavSection.CIRCLE_OF_FIFTHS, stringResource(R.string.nav_circle_of_fifths), Icons.Filled.Refresh),
        DrawerItem(NavSection.CHORD_SUBS, stringResource(R.string.nav_chord_substitutions), Icons.AutoMirrored.Filled.List),
        DrawerItem(NavSection.SCALE_CHORDS, stringResource(R.string.nav_chords_in_scale), Icons.Filled.PlayArrow),
        DrawerItem(NavSection.NOTE_MAP, stringResource(R.string.nav_fretboard_notes), Icons.Filled.Search),
        DrawerItem(NavSection.GLOSSARY, stringResource(R.string.nav_glossary), Icons.Filled.Info),
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

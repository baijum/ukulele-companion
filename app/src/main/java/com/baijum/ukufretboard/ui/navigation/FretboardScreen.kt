package com.baijum.ukufretboard.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baijum.ukufretboard.R
import kotlinx.coroutines.launch
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.AchievementRepository
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.PracticeTimerRepository
import com.baijum.ukufretboard.data.ReviewPromptRepository
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.domain.Achievements
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.toAchievementContext
import com.baijum.ukufretboard.ui.AchievementsView
import com.baijum.ukufretboard.ui.CapoGuideView
import com.baijum.ukufretboard.ui.ChordEarTrainingView
import com.baijum.ukufretboard.ui.ChordLibraryTab
import com.baijum.ukufretboard.ui.ChordSubstitutionsView
import com.baijum.ukufretboard.ui.ChordTransitionView
import com.baijum.ukufretboard.ui.CircleOfFifthsView
import com.baijum.ukufretboard.ui.DailyChallengeView
import com.baijum.ukufretboard.ui.FavoriteFolderSheet
import com.baijum.ukufretboard.ui.FavoritesTab
import com.baijum.ukufretboard.ui.FretboardNoteMapView
import com.baijum.ukufretboard.ui.FullScreenFretboard
import com.baijum.ukufretboard.ui.GlossaryView
import com.baijum.ukufretboard.ui.HelpView
import com.baijum.ukufretboard.ui.IntervalTrainerView
import com.baijum.ukufretboard.ui.LearningProgressView
import com.baijum.ukufretboard.ui.MetronomeTab
import com.baijum.ukufretboard.ui.NoteQuizView
import com.baijum.ukufretboard.ui.PitchMonitorTab
import com.baijum.ukufretboard.ui.PlayAlongSetup
import com.baijum.ukufretboard.ui.PracticeRoutineView
import com.baijum.ukufretboard.ui.ProgressionsTab
import com.baijum.ukufretboard.ui.ReviewPromptDialog
import com.baijum.ukufretboard.ui.ScaleChordView
import com.baijum.ukufretboard.ui.ScalePracticeView
import com.baijum.ukufretboard.ui.SetlistTab
import com.baijum.ukufretboard.ui.SettingsSheet
import com.baijum.ukufretboard.ui.ShareChordBottomSheet
import com.baijum.ukufretboard.ui.ShareChordInfo
import com.baijum.ukufretboard.ui.SongwriterModeFlow
import com.baijum.ukufretboard.ui.songbook.SongbookTab
import com.baijum.ukufretboard.ui.TheoryLessonsView
import com.baijum.ukufretboard.ui.TheoryQuizView
import com.baijum.ukufretboard.ui.TunerTab
import com.baijum.ukufretboard.ui.melody.MelodyNotepadView
import com.baijum.ukufretboard.ui.patterns.StrumPatternsTab
import com.baijum.ukufretboard.viewmodel.ChordLibraryViewModel
import com.baijum.ukufretboard.viewmodel.CustomProgressionViewModel
import com.baijum.ukufretboard.viewmodel.FavoritesViewModel
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.SettingsViewModel
import com.baijum.ukufretboard.viewmodel.SongbookViewModel
import com.baijum.ukufretboard.viewmodel.BackupRestoreViewModel
import com.baijum.ukufretboard.viewmodel.TunerViewModel
import com.baijum.ukufretboard.viewmodel.PitchMonitorViewModel
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import com.baijum.ukufretboard.viewmodel.MelodyViewModel
import com.baijum.ukufretboard.viewmodel.MetronomeViewModel
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel
import com.baijum.ukufretboard.viewmodel.SetlistViewModel

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

internal const val NAV_PRACTICE_ROUTINE = 28
internal const val NAV_PLAY_ALONG = 29
internal const val NAV_METRONOME = 30
internal const val NAV_SONGWRITER_MODE = 27
internal const val NAV_SETLISTS = 31

private val PLAY_CREATE_NAV_INDICES = setOf(
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

@Composable
private fun drawerSections(): List<DrawerSection> = listOf(
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

private data class SheetVoicingInfo(
    val rootPitchClass: Int,
    val chordSymbol: String,
    val frets: List<Int>,
)

private fun navigateToChord(
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

/**
 * Top-level screen composable for Ukulele Companion.
 *
 * Uses a [ModalNavigationDrawer] to navigate between sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FretboardScreen(
    fretboardViewModel: FretboardViewModel = viewModel(),
    libraryViewModel: ChordLibraryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel = viewModel(),
    songbookViewModel: SongbookViewModel = viewModel(),
    backupRestoreViewModel: BackupRestoreViewModel = viewModel(),
    customProgressionViewModel: CustomProgressionViewModel = viewModel(),
    tunerViewModel: TunerViewModel = viewModel(),
    pitchMonitorViewModel: PitchMonitorViewModel = viewModel(),
    learningProgressViewModel: LearningProgressViewModel = viewModel(),
    scalePracticeViewModel: ScalePracticeViewModel = viewModel(),
    melodyViewModel: MelodyViewModel = viewModel(),
    setlistViewModel: SetlistViewModel = viewModel(),
    metronomeViewModel: MetronomeViewModel = viewModel(),
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium,
) {
    val isCompactWidth = widthSizeClass == WindowWidthSizeClass.Compact
    val isTabletWidth = widthSizeClass == WindowWidthSizeClass.Expanded &&
        heightSizeClass != WindowHeightSizeClass.Compact
    var selectedSection by rememberSaveable { mutableIntStateOf(NAV_EXPLORER) }
    var previousSection by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showFullScreen by rememberSaveable { mutableStateOf(false) }
    var shareChordInfo by remember { mutableStateOf<ShareChordInfo?>(null) }
    var sheetVoicing by remember { mutableStateOf<SheetVoicingInfo?>(null) }
    val currentFolders by favoritesViewModel.folders.collectAsState()

    BackHandler(enabled = previousSection != null && selectedSection == NAV_LIBRARY) {
        selectedSection = previousSection!!
        previousSection = null
    }

    LaunchedEffect(Unit) {
        backupRestoreViewModel.init(settingsViewModel)
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ToneGenerator.init(context)
    }

    val achievementRepository = remember { AchievementRepository(context) }
    var unlockedAchievementIds by remember {
        mutableStateOf(achievementRepository.getUnlocked().keys)
    }

    val reviewPromptRepository = remember { ReviewPromptRepository(context) }
    LaunchedEffect(Unit) {
        reviewPromptRepository.initFirstLaunch()
        reviewPromptRepository.recordActiveDay()
    }
    var reviewPromptAchievement by remember { mutableStateOf<String?>(null) }

    val practiceTimerRepository = remember { PracticeTimerRepository(context) }
    val sessionStartMs = remember { System.currentTimeMillis() }
    var practiceStats by remember { mutableStateOf(practiceTimerRepository.stats()) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            val durationMs = System.currentTimeMillis() - sessionStartMs
            if (durationMs >= 60_000L) {
                practiceTimerRepository.recordSession(durationMs)
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val allSections = drawerSections()
    val allItems = allSections.flatMap { it.items }

    val appSettings by settingsViewModel.settings.collectAsState()

    val currentFavorites by favoritesViewModel.favorites.collectAsState()

    val customProgressions by customProgressionViewModel.progressions.collectAsState()

    LaunchedEffect(appSettings.sound) {
        fretboardViewModel.setSoundSettings(appSettings.sound)
        melodyViewModel.setSoundSettings(appSettings.sound)
    }

    LaunchedEffect(Unit) {
        melodyViewModel.setApplicationContext(context)
    }

    LaunchedEffect(appSettings.tuning) {
        fretboardViewModel.setTuningSettings(appSettings.tuning)
        libraryViewModel.setTuning(fretboardViewModel.tuning)
    }

    LaunchedEffect(appSettings.fretboard.lastFret) {
        fretboardViewModel.setLastFret(appSettings.fretboard.lastFret)
    }

    LaunchedEffect(appSettings.fretboard.allowMutedStrings) {
        libraryViewModel.setAllowMutedStrings(appSettings.fretboard.allowMutedStrings)
    }

    LaunchedEffect(appSettings.fretboard.showNoteNames) {
        fretboardViewModel.setShowNoteNames(appSettings.fretboard.showNoteNames)
    }

    LaunchedEffect(appSettings.sound.noiseGateFiltering) {
        val rms = SoundSettings.filteringToRms(appSettings.sound.noiseGateFiltering)
        tunerViewModel.setNoiseGateRms(rms)
        pitchMonitorViewModel.setNoiseGateRms(rms)
        melodyViewModel.setNoiseGateRms(rms)
    }

    LaunchedEffect(Unit) {
        scalePracticeViewModel.restoreSettings(appSettings.scalePractice)
    }

    if (showFullScreen) {
        FullScreenFretboard(
            viewModel = fretboardViewModel,
            soundEnabled = appSettings.sound.enabled,
            leftHanded = appSettings.fretboard.leftHanded,
            isLargeScreen = isTabletWidth,
            onExit = { showFullScreen = false },
        )
        return
    }

    val learnTitle = stringResource(R.string.nav_section_learn)
    val referenceTitle = stringResource(R.string.nav_section_reference)
    val visibleSections = remember(allSections, appSettings.display) {
        allSections.filter { section ->
            when (section.title) {
                learnTitle -> appSettings.display.showLearnSection
                referenceTitle -> appSettings.display.showReferenceSection
                else -> true
            }
        }
    }
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (selectedSection) {
                                NAV_HELP -> stringResource(R.string.nav_help)
                                else -> allItems.firstOrNull { it.index == selectedSection }?.label
                                    ?: stringResource(R.string.nav_explorer)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                    },
                    navigationIcon = {
                        if (previousSection != null && selectedSection == NAV_LIBRARY) {
                            IconButton(onClick = {
                                selectedSection = previousSection!!
                                previousSection = null
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        } else if (!isTabletWidth) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = stringResource(R.string.cd_open_nav_menu),
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (selectedSection) {
                    NAV_EXPLORER -> ExplorerTabContent(
                        viewModel = fretboardViewModel,
                        settingsViewModel = settingsViewModel,
                        soundEnabled = appSettings.sound.enabled,
                        leftHanded = appSettings.fretboard.leftHanded,
                        showTips = !appSettings.explorerTipsDismissed,
                        showDidYouKnow = appSettings.display.showExplorerTips,
                        onDismissTips = { settingsViewModel.dismissExplorerTips() },
                        onFullScreen = { showFullScreen = true },
                        isTabletWidth = isTabletWidth,
                        onShareChord = { voicing, chordName, invLabel ->
                            shareChordInfo = ShareChordInfo(
                                voicing = voicing,
                                chordName = chordName,
                                inversionLabel = invLabel,
                                tuningPitchClasses = fretboardViewModel.tuning.map { it.openPitchClass },
                            )
                        },
                        onShowInLibrary = { rootPitchClass, formula ->
                            libraryViewModel.selectRoot(rootPitchClass)
                            libraryViewModel.selectCategory(formula.category)
                            libraryViewModel.selectFormula(formula)
                            previousSection = selectedSection
                            selectedSection = NAV_LIBRARY
                        },
                    )
                    NAV_TUNER -> ConstrainedWidthContent(isCompactWidth) {
                        TunerTab(
                            viewModel = tunerViewModel,
                            tuning = appSettings.tuning.tuning,
                            leftHanded = appSettings.fretboard.leftHanded,
                            soundEnabled = appSettings.sound.enabled,
                            tunerSettings = appSettings.tuner,
                        )
                    }
                    NAV_PITCH_MONITOR -> PitchMonitorTab(
                        viewModel = pitchMonitorViewModel,
                    )
                    NAV_METRONOME -> ConstrainedWidthContent(isCompactWidth) {
                        MetronomeTab(
                            viewModel = metronomeViewModel,
                        )
                    }
                    NAV_LIBRARY -> ChordLibraryTab(
                        viewModel = libraryViewModel,
                        tuning = fretboardViewModel.tuning,
                        onVoicingSelected = { voicing ->
                            val state = libraryViewModel.uiState.value
                            fretboardViewModel.applyVoicing(
                                voicing = voicing,
                                rootPitchClass = state.selectedRoot,
                                formula = state.selectedFormula,
                            )
                            selectedSection = NAV_EXPLORER
                        },
                        onVoicingLongPressed = { voicing ->
                            val state = libraryViewModel.uiState.value
                            val rootName = Notes.pitchClassToName(state.selectedRoot)
                            val symbol = state.selectedFormula?.symbol ?: ""
                            val tuning = fretboardViewModel.tuning
                            val invLabel = state.selectedFormula?.let { formula ->
                                val inv = ChordInfo.determineInversion(
                                    voicing.frets, state.selectedRoot, formula, tuning,
                                )
                                if (inv != ChordInfo.Inversion.ROOT) inv.label else null
                            }
                            shareChordInfo = ShareChordInfo(
                                voicing = voicing,
                                chordName = "$rootName$symbol",
                                inversionLabel = invLabel,
                                tuningPitchClasses = fretboardViewModel.tuning.map { it.openPitchClass },
                            )
                        },
                    isFavorite = { voicing ->
                        val state = libraryViewModel.uiState.value
                        val symbol = state.selectedFormula?.symbol ?: ""
                        val key = "${state.selectedRoot}|$symbol|${voicing.frets.joinToString(",")}"
                        currentFavorites.any { it.key == key }
                    },
                        onFavoriteClick = { voicing ->
                            val state = libraryViewModel.uiState.value
                            val symbol = state.selectedFormula?.symbol ?: ""
                            sheetVoicing = SheetVoicingInfo(
                                rootPitchClass = state.selectedRoot,
                                chordSymbol = symbol,
                                frets = voicing.frets,
                            )
                        },
                        onPlayVoicing = { voicing ->
                            fretboardViewModel.playVoicing(voicing)
                        },
                        onPlayVoicingsSequentially = { voicings ->
                            fretboardViewModel.playVoicingsSequentially(voicings)
                        },
                        leftHanded = appSettings.fretboard.leftHanded,
                    )
                    NAV_PATTERNS -> ConstrainedWidthContent(isCompactWidth) {
                        StrumPatternsTab(
                            tuning = appSettings.tuning.tuning,
                        )
                    }
                    NAV_PROGRESSIONS -> ConstrainedWidthContent(isCompactWidth) {
                        ProgressionsTab(
                        leftHanded = appSettings.fretboard.leftHanded,
                        tuning = fretboardViewModel.tuning,
                        lastFret = appSettings.fretboard.lastFret,
                        customProgressions = customProgressions,
                        onChordTapped = { rootPitchClass, quality ->
                            libraryViewModel.selectRoot(rootPitchClass)
                            val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
                                .firstOrNull { it.symbol == quality }
                            if (formula != null) {
                                libraryViewModel.selectCategory(formula.category)
                                libraryViewModel.selectFormula(formula)
                            }
                            previousSection = selectedSection
                            selectedSection = NAV_LIBRARY
                        },
                        onSaveProgression = { name, description, degrees, scaleType ->
                            customProgressionViewModel.create(name, description, degrees, scaleType)
                        },
                        onEditProgression = { id, name, description, degrees, scaleType ->
                            customProgressionViewModel.update(id, name, description, degrees, scaleType)
                        },
                        onDeleteProgression = { id ->
                            customProgressionViewModel.delete(id)
                        },
                        onPlayVoicing = { voicing ->
                            fretboardViewModel.playVoicing(voicing)
                        },
                        onPlayAll = { voicings ->
                            fretboardViewModel.playVoicingsSequentially(voicings)
                        },
                    )
                    }
                    NAV_FAVORITES -> FavoritesTab(
                        viewModel = favoritesViewModel,
                        tuning = fretboardViewModel.tuning,
                        onVoicingSelected = { voicing ->
                            fretboardViewModel.applyVoicing(voicing)
                            selectedSection = NAV_EXPLORER
                        },
                        onShareVoicing = { voicing, chordName ->
                            shareChordInfo = ShareChordInfo(
                                voicing = voicing,
                                chordName = chordName,
                                tuningPitchClasses = fretboardViewModel.tuning.map { it.openPitchClass },
                            )
                        },
                        leftHanded = appSettings.fretboard.leftHanded,
                    )
                    NAV_SONGWRITER_MODE -> ConstrainedWidthContent(isCompactWidth) {
                        SongwriterModeFlow(
                            songbookViewModel = songbookViewModel,
                            onSaveProgression = { name, description, degrees, scaleType ->
                                customProgressionViewModel.create(name, description, degrees, scaleType)
                            },
                        )
                    }
                    NAV_SONGBOOK -> ConstrainedWidthContent(isCompactWidth) {
                        SongbookTab(
                            viewModel = songbookViewModel,
                            onChordTapped = { chordName ->
                                navigateToChord(chordName, libraryViewModel) {
                                    previousSection = selectedSection
                                    selectedSection = NAV_LIBRARY
                                }
                            },
                            onPlayChord = { chordName ->
                                fretboardViewModel.playChordByName(chordName)
                            },
                            onStartMetronome = { bpm ->
                                metronomeViewModel.setBpm(bpm)
                                if (!metronomeViewModel.isPlaying.value) {
                                    metronomeViewModel.togglePlayback()
                                }
                                previousSection = selectedSection
                                selectedSection = NAV_METRONOME
                            },
                            tuning = fretboardViewModel.tuning,
                            leftHanded = appSettings.fretboard.leftHanded,
                            chordDisplayStyle = appSettings.display.chordDisplayStyle,
                            chordColor = appSettings.display.chordColor,
                            showChordDiagramRail = appSettings.display.showChordDiagramRail,
                        )
                    }
                    NAV_SETLISTS -> ConstrainedWidthContent(isCompactWidth) {
                        SetlistTab(
                            setlistViewModel = setlistViewModel,
                            songbookViewModel = songbookViewModel,
                        )
                    }
                    NAV_CAPO_GUIDE -> CapoGuideView(
                        tuning = appSettings.tuning.tuning,
                    )
                    NAV_THEORY_QUIZ -> TheoryQuizView(
                        progressViewModel = learningProgressViewModel,
                    )
                    NAV_INTERVAL_TRAINER -> IntervalTrainerView(
                        progressViewModel = learningProgressViewModel,
                    )
                    NAV_CHORD_SUBS -> ChordSubstitutionsView()
                    NAV_THEORY_LESSONS -> TheoryLessonsView(
                        progressViewModel = learningProgressViewModel,
                    )
                    NAV_LEARNING_PROGRESS -> LearningProgressView(
                        viewModel = learningProgressViewModel,
                        practiceStats = practiceStats,
                    )
                    NAV_MELODY_NOTEPAD -> MelodyNotepadView(
                        viewModel = melodyViewModel,
                    )
                    NAV_CIRCLE_OF_FIFTHS -> CircleOfFifthsView(
                        onChordTapped = { rootPitchClass, quality ->
                            libraryViewModel.selectRoot(rootPitchClass)
                            val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
                                .firstOrNull { it.symbol == quality }
                            if (formula != null) {
                                libraryViewModel.selectCategory(formula.category)
                                libraryViewModel.selectFormula(formula)
                            }
                            previousSection = selectedSection
                            selectedSection = NAV_LIBRARY
                        },
                    )
                    NAV_NOTE_QUIZ -> NoteQuizView(
                        tuning = appSettings.tuning.tuning,
                        progressViewModel = learningProgressViewModel,
                    )
                    NAV_CHORD_EAR -> ChordEarTrainingView(
                        progressViewModel = learningProgressViewModel,
                    )
                    NAV_SCALE_PRACTICE -> ScalePracticeView(
                        viewModel = scalePracticeViewModel,
                        progressViewModel = learningProgressViewModel,
                        onSettingsChanged = { newSettings ->
                            settingsViewModel.updateScalePractice { newSettings }
                        },
                        tuning = fretboardViewModel.tuning,
                        lastFret = appSettings.fretboard.lastFret,
                    )
                    NAV_SCALE_CHORDS -> ScaleChordView()
                    NAV_GLOSSARY -> GlossaryView()
                    NAV_NOTE_MAP -> FretboardNoteMapView(
                        tuning = appSettings.tuning.tuning,
                        lastFret = appSettings.fretboard.lastFret,
                    )
                    NAV_PRACTICE_ROUTINE -> PracticeRoutineView(
                        onNavigate = { navIndex ->
                            selectedSection = navIndex
                        },
                    )

                    NAV_DAILY_CHALLENGE -> DailyChallengeView(
                        onNavigate = { navIndex ->
                            selectedSection = navIndex
                        },
                    )
                    NAV_CHORD_TRANSITION -> ChordTransitionView(
                        tuning = fretboardViewModel.tuning,
                        lastFret = appSettings.fretboard.lastFret,
                        leftHanded = appSettings.fretboard.leftHanded,
                        onPlayVoicing = { voicing ->
                            fretboardViewModel.playVoicing(voicing)
                        },
                    )
                    NAV_PLAY_ALONG -> PlayAlongSetup(
                        tuning = fretboardViewModel.tuning,
                        onPlayVoicing = { voicing ->
                            fretboardViewModel.playVoicing(voicing)
                        },
                    )
                    NAV_ACHIEVEMENTS -> {
                        val progressState by learningProgressViewModel.state.collectAsState()
                        val sheetsState by songbookViewModel.sheets.collectAsState()
                        val achievementContext = progressState.toAchievementContext(
                            songsCount = sheetsState.size,
                            favoritesCount = currentFavorites.size,
                        )
                        val newlyEarned = Achievements.checkNewlyEarned(
                            achievementContext,
                            unlockedAchievementIds,
                        )
                        if (newlyEarned.isNotEmpty()) {
                            LaunchedEffect(newlyEarned) {
                                newlyEarned.forEach { achievementRepository.unlock(it.id) }
                                unlockedAchievementIds = achievementRepository.getUnlocked().keys
                                if (reviewPromptRepository.isEligible()) {
                                    reviewPromptAchievement = newlyEarned.first().id
                                }
                            }
                        }
                        AchievementsView(
                            progressViewModel = learningProgressViewModel,
                            unlockedIds = unlockedAchievementIds,
                            songsCount = sheetsState.size,
                            favoritesCount = currentFavorites.size,
                        )
                    }
                    NAV_HELP -> HelpView()
                }
            }
        }
    }

    val drawerItemOnClick: (Int) -> Unit = { index ->
        previousSection = null
        selectedSection = index
        if (index in PLAY_CREATE_NAV_INDICES) {
            reviewPromptRepository.recordActiveDay()
        }
        if (!isTabletWidth) scope.launch { drawerState.close() }
    }

    if (!isTabletWidth) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerContent(
                        visibleSections = visibleSections,
                        expandedState = expandedState,
                        selectedSection = selectedSection,
                        onItemSelected = drawerItemOnClick,
                    )
                }
            },
        ) {
            scaffoldContent()
        }
    } else {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet {
                    DrawerContent(
                        visibleSections = visibleSections,
                        expandedState = expandedState,
                        selectedSection = selectedSection,
                        onItemSelected = drawerItemOnClick,
                    )
                }
            },
        ) {
            scaffoldContent()
        }
    }

    if (showSettings) {
        SettingsSheet(
            soundSettings = appSettings.sound,
            onSoundSettingsChange = { newSound ->
                settingsViewModel.updateSound { newSound }
            },
            displaySettings = appSettings.display,
            onDisplaySettingsChange = { newDisplay ->
                settingsViewModel.updateDisplay { newDisplay }
            },
            tuningSettings = appSettings.tuning,
            onTuningSettingsChange = { newTuning ->
                settingsViewModel.updateTuning { newTuning }
            },
            fretboardSettings = appSettings.fretboard,
            onFretboardSettingsChange = { newFretboard ->
                settingsViewModel.updateFretboard { newFretboard }
            },

            tunerSettings = appSettings.tuner,
            onTunerSettingsChange = { newTuner ->
                settingsViewModel.updateTuner { newTuner }
            },
            backupRestoreViewModel = backupRestoreViewModel,
            onDismiss = { showSettings = false },
        )
    }

    reviewPromptAchievement?.let { achievementId ->
        ReviewPromptDialog(
            achievementId = achievementId,
            repository = reviewPromptRepository,
            onDismiss = { reviewPromptAchievement = null },
        )
    }

    shareChordInfo?.let { info ->
        ShareChordBottomSheet(
            info = info,
            onDismiss = { shareChordInfo = null },
        )
    }

    sheetVoicing?.let { info ->
        val isAlreadyFavorited = favoritesViewModel.isFavorite(
            info.rootPitchClass, info.chordSymbol, info.frets,
        )
        val currentFolderIds = favoritesViewModel.getFolderIdsForVoicing(
            info.rootPitchClass, info.chordSymbol, info.frets,
        )
        FavoriteFolderSheet(
            folders = currentFolders,
            selectedFolderIds = currentFolderIds,
            isAlreadyFavorited = isAlreadyFavorited,
            onSave = { selectedIds ->
                favoritesViewModel.saveFavoriteToFolders(
                    rootPitchClass = info.rootPitchClass,
                    chordSymbol = info.chordSymbol,
                    frets = info.frets,
                    folderIds = selectedIds,
                )
                sheetVoicing = null
            },
            onRemove = {
                favoritesViewModel.removeFavorite(
                    rootPitchClass = info.rootPitchClass,
                    chordSymbol = info.chordSymbol,
                    frets = info.frets,
                )
                sheetVoicing = null
            },
            onCreateFolder = { name ->
                favoritesViewModel.createFolder(name)
            },
            onDismiss = { sheetVoicing = null },
        )
    }
}

@Composable
private fun ConstrainedWidthContent(
    isCompactWidth: Boolean,
    content: @Composable () -> Unit,
) {
    if (isCompactWidth) {
        content()
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = Modifier.widthIn(max = 840.dp)) {
                content()
            }
        }
    }
}

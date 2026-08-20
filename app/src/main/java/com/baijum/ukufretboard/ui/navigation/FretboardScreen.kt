package com.baijum.ukufretboard.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.AchievementRepository
import com.baijum.ukufretboard.data.ChordColorOption
import com.baijum.ukufretboard.data.ChordDisplayStyle
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.CustomProgression
import com.baijum.ukufretboard.data.FavoriteVoicing
import com.baijum.ukufretboard.data.NavSection
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.PracticeTimerRepository
import com.baijum.ukufretboard.data.ReviewPromptRepository
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.domain.Achievements
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing
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
import com.baijum.ukufretboard.ui.ScaleChordView
import com.baijum.ukufretboard.ui.ScalePracticeView
import com.baijum.ukufretboard.ui.SetlistTab
import com.baijum.ukufretboard.ui.SettingsSheet
import com.baijum.ukufretboard.ui.ShareChordBottomSheet
import com.baijum.ukufretboard.ui.ShareChordInfo
import com.baijum.ukufretboard.ui.SongwriterModeFlow
import com.baijum.ukufretboard.ui.TheoryLessonsView
import com.baijum.ukufretboard.ui.TheoryQuizView
import com.baijum.ukufretboard.ui.TunerTab
import com.baijum.ukufretboard.ui.launchReviewFlow
import com.baijum.ukufretboard.ui.melody.MelodyNotepadView
import com.baijum.ukufretboard.ui.patterns.StrumPatternsTab
import com.baijum.ukufretboard.ui.songbook.SongbookTab
import com.baijum.ukufretboard.viewmodel.BackupRestoreViewModel
import com.baijum.ukufretboard.viewmodel.ChordLibraryViewModel
import com.baijum.ukufretboard.viewmodel.CustomProgressionViewModel
import com.baijum.ukufretboard.viewmodel.FavoritesViewModel
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import com.baijum.ukufretboard.viewmodel.MelodyViewModel
import com.baijum.ukufretboard.viewmodel.MetronomeViewModel
import com.baijum.ukufretboard.viewmodel.PitchMonitorViewModel
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel
import com.baijum.ukufretboard.viewmodel.SetlistViewModel
import com.baijum.ukufretboard.viewmodel.SettingsViewModel
import com.baijum.ukufretboard.viewmodel.SongbookViewModel
import com.baijum.ukufretboard.viewmodel.TunerViewModel
import com.baijum.ukufretboard.viewmodel.toAchievementContext
import kotlinx.coroutines.launch

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
    val isTabletWidth =
        widthSizeClass == WindowWidthSizeClass.Expanded &&
            heightSizeClass != WindowHeightSizeClass.Compact
    var selectedSection by rememberSaveable(
        stateSaver =
            Saver(
                save = { it.id },
                restore = { NavSection.fromId(it) ?: NavSection.EXPLORER },
            ),
    ) { mutableStateOf(NavSection.EXPLORER) }
    var previousSection by rememberSaveable { mutableStateOf<NavSection?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showFullScreen by rememberSaveable { mutableStateOf(false) }
    var shareChordInfo by remember { mutableStateOf<ShareChordInfo?>(null) }
    var sheetVoicing by remember { mutableStateOf<SheetVoicingInfo?>(null) }
    val currentFolders by favoritesViewModel.folders.collectAsState()

    BackHandler(enabled = previousSection != null && selectedSection == NavSection.LIBRARY) {
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
    val visibleSections =
        remember(allSections, appSettings.display) {
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
                            text =
                                when (selectedSection) {
                                    NavSection.HELP -> {
                                        stringResource(R.string.nav_help)
                                    }

                                    else -> {
                                        allItems.firstOrNull { it.section == selectedSection }?.label
                                            ?: stringResource(R.string.nav_explorer)
                                    }
                                },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                    },
                    navigationIcon = {
                        if (previousSection != null && selectedSection == NavSection.LIBRARY) {
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
                    colors =
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                when (selectedSection) {
                    NavSection.EXPLORER -> {
                        ExplorerRoute(
                            fretboardViewModel = fretboardViewModel,
                            settingsViewModel = settingsViewModel,
                            soundEnabled = appSettings.sound.enabled,
                            leftHanded = appSettings.fretboard.leftHanded,
                            showTips = !appSettings.explorerTipsDismissed,
                            showDidYouKnow = appSettings.display.showExplorerTips,
                            isTabletWidth = isTabletWidth,
                            onFullScreen = { showFullScreen = true },
                            onShareChord = { shareChordInfo = it },
                            onShowInLibrary = { rootPitchClass, formula ->
                                libraryViewModel.selectRoot(rootPitchClass)
                                libraryViewModel.selectCategory(formula.category)
                                libraryViewModel.selectFormula(formula)
                                previousSection = selectedSection
                                selectedSection = NavSection.LIBRARY
                            },
                        )
                    }

                    NavSection.TUNER -> {
                        ConstrainedWidthContent(isCompactWidth) {
                            TunerTab(
                                viewModel = tunerViewModel,
                                tuning = appSettings.tuning.tuning,
                                leftHanded = appSettings.fretboard.leftHanded,
                                soundEnabled = appSettings.sound.enabled,
                                tunerSettings = appSettings.tuner,
                            )
                        }
                    }

                    NavSection.PITCH_MONITOR -> {
                        PitchMonitorTab(
                            viewModel = pitchMonitorViewModel,
                        )
                    }

                    NavSection.METRONOME -> {
                        ConstrainedWidthContent(isCompactWidth) {
                            MetronomeTab(
                                viewModel = metronomeViewModel,
                            )
                        }
                    }

                    NavSection.LIBRARY -> {
                        LibraryRoute(
                            libraryViewModel = libraryViewModel,
                            fretboardViewModel = fretboardViewModel,
                            currentFavorites = currentFavorites,
                            leftHanded = appSettings.fretboard.leftHanded,
                            onNavigateToExplorer = { selectedSection = NavSection.EXPLORER },
                            onShareChord = { shareChordInfo = it },
                            onShowFavoriteSheet = { sheetVoicing = it },
                        )
                    }

                    NavSection.PATTERNS -> {
                        ConstrainedWidthContent(isCompactWidth) {
                            StrumPatternsTab(
                                tuning = appSettings.tuning.tuning,
                            )
                        }
                    }

                    NavSection.PROGRESSIONS -> {
                        ProgressionsRoute(
                            isCompactWidth = isCompactWidth,
                            fretboardViewModel = fretboardViewModel,
                            customProgressionViewModel = customProgressionViewModel,
                            customProgressions = customProgressions,
                            leftHanded = appSettings.fretboard.leftHanded,
                            lastFret = appSettings.fretboard.lastFret,
                            onChordTapped = { rootPitchClass, quality ->
                                libraryViewModel.selectRoot(rootPitchClass)
                                val formula =
                                    com.baijum.ukufretboard.data.ChordFormulas.ALL
                                        .firstOrNull { it.symbol == quality }
                                if (formula != null) {
                                    libraryViewModel.selectCategory(formula.category)
                                    libraryViewModel.selectFormula(formula)
                                }
                                previousSection = selectedSection
                                selectedSection = NavSection.LIBRARY
                            },
                        )
                    }

                    NavSection.FAVORITES -> {
                        FavoritesRoute(
                            favoritesViewModel = favoritesViewModel,
                            fretboardViewModel = fretboardViewModel,
                            leftHanded = appSettings.fretboard.leftHanded,
                            onNavigateToExplorer = { selectedSection = NavSection.EXPLORER },
                            onShareChord = { shareChordInfo = it },
                        )
                    }

                    NavSection.SONGWRITER_MODE -> {
                        ConstrainedWidthContent(isCompactWidth) {
                            SongwriterModeFlow(
                                songbookViewModel = songbookViewModel,
                                onSaveProgression = { name, description, degrees, scaleType ->
                                    customProgressionViewModel.create(name, description, degrees, scaleType)
                                },
                            )
                        }
                    }

                    NavSection.SONGBOOK -> {
                        SongbookRoute(
                            isCompactWidth = isCompactWidth,
                            songbookViewModel = songbookViewModel,
                            libraryViewModel = libraryViewModel,
                            fretboardViewModel = fretboardViewModel,
                            metronomeViewModel = metronomeViewModel,
                            leftHanded = appSettings.fretboard.leftHanded,
                            chordDisplayStyle = appSettings.display.chordDisplayStyle,
                            chordColor = appSettings.display.chordColor,
                            showChordDiagramRail = appSettings.display.showChordDiagramRail,
                            onNavigateToLibrary = {
                                previousSection = selectedSection
                                selectedSection = NavSection.LIBRARY
                            },
                            onNavigateToMetronome = {
                                previousSection = selectedSection
                                selectedSection = NavSection.METRONOME
                            },
                        )
                    }

                    NavSection.SETLISTS -> {
                        ConstrainedWidthContent(isCompactWidth) {
                            SetlistTab(
                                setlistViewModel = setlistViewModel,
                                songbookViewModel = songbookViewModel,
                            )
                        }
                    }

                    NavSection.CAPO_GUIDE -> {
                        CapoGuideView(
                            tuning = appSettings.tuning.tuning,
                        )
                    }

                    NavSection.THEORY_QUIZ -> {
                        TheoryQuizView(
                            progressViewModel = learningProgressViewModel,
                        )
                    }

                    NavSection.INTERVAL_TRAINER -> {
                        IntervalTrainerView(
                            progressViewModel = learningProgressViewModel,
                        )
                    }

                    NavSection.CHORD_SUBS -> {
                        ChordSubstitutionsView()
                    }

                    NavSection.THEORY_LESSONS -> {
                        TheoryLessonsView(
                            progressViewModel = learningProgressViewModel,
                        )
                    }

                    NavSection.LEARNING_PROGRESS -> {
                        LearningProgressView(
                            viewModel = learningProgressViewModel,
                            practiceStats = practiceStats,
                        )
                    }

                    NavSection.MELODY_NOTEPAD -> {
                        MelodyNotepadView(
                            viewModel = melodyViewModel,
                        )
                    }

                    NavSection.CIRCLE_OF_FIFTHS -> {
                        CircleOfFifthsView(
                            onChordTapped = { rootPitchClass, quality ->
                                libraryViewModel.selectRoot(rootPitchClass)
                                val formula =
                                    com.baijum.ukufretboard.data.ChordFormulas.ALL
                                        .firstOrNull { it.symbol == quality }
                                if (formula != null) {
                                    libraryViewModel.selectCategory(formula.category)
                                    libraryViewModel.selectFormula(formula)
                                }
                                previousSection = selectedSection
                                selectedSection = NavSection.LIBRARY
                            },
                        )
                    }

                    NavSection.NOTE_QUIZ -> {
                        NoteQuizView(
                            tuning = appSettings.tuning.tuning,
                            progressViewModel = learningProgressViewModel,
                        )
                    }

                    NavSection.CHORD_EAR -> {
                        ChordEarTrainingView(
                            progressViewModel = learningProgressViewModel,
                        )
                    }

                    NavSection.SCALE_PRACTICE -> {
                        ScalePracticeView(
                            viewModel = scalePracticeViewModel,
                            progressViewModel = learningProgressViewModel,
                            onSettingsChanged = { newSettings ->
                                settingsViewModel.updateScalePractice { newSettings }
                            },
                            tuning = fretboardViewModel.tuning,
                            lastFret = appSettings.fretboard.lastFret,
                        )
                    }

                    NavSection.SCALE_CHORDS -> {
                        ScaleChordView()
                    }

                    NavSection.GLOSSARY -> {
                        GlossaryView()
                    }

                    NavSection.NOTE_MAP -> {
                        FretboardNoteMapView(
                            tuning = appSettings.tuning.tuning,
                            lastFret = appSettings.fretboard.lastFret,
                        )
                    }

                    NavSection.PRACTICE_ROUTINE -> {
                        PracticeRoutineView(
                            onNavigate = { selectedSection = it },
                        )
                    }

                    NavSection.DAILY_CHALLENGE -> {
                        DailyChallengeView(
                            onNavigate = { selectedSection = it },
                        )
                    }

                    NavSection.CHORD_TRANSITION -> {
                        ChordTransitionView(
                            tuning = fretboardViewModel.tuning,
                            lastFret = appSettings.fretboard.lastFret,
                            leftHanded = appSettings.fretboard.leftHanded,
                            onPlayVoicing = { voicing ->
                                fretboardViewModel.playVoicing(voicing)
                            },
                        )
                    }

                    NavSection.PLAY_ALONG -> {
                        PlayAlongSetup(
                            tuning = fretboardViewModel.tuning,
                            onPlayVoicing = { voicing ->
                                fretboardViewModel.playVoicing(voicing)
                            },
                        )
                    }

                    NavSection.ACHIEVEMENTS -> {
                        AchievementsRoute(
                            learningProgressViewModel = learningProgressViewModel,
                            songbookViewModel = songbookViewModel,
                            favoritesCount = currentFavorites.size,
                            unlockedAchievementIds = unlockedAchievementIds,
                            achievementRepository = achievementRepository,
                            reviewPromptRepository = reviewPromptRepository,
                            onUnlockedIdsChanged = { unlockedAchievementIds = it },
                            onReviewPrompt = { launchReviewFlow(context, reviewPromptRepository) },
                        )
                    }

                    NavSection.HELP -> {
                        HelpView()
                    }
                }
            }
        }
    }

    val drawerItemOnClick: (NavSection) -> Unit = { section ->
        previousSection = null
        selectedSection = section
        if (section in PLAY_CREATE_SECTIONS) {
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

    shareChordInfo?.let { info ->
        ShareChordBottomSheet(
            info = info,
            onDismiss = { shareChordInfo = null },
        )
    }

    sheetVoicing?.let { info ->
        val isAlreadyFavorited =
            favoritesViewModel.isFavorite(
                info.rootPitchClass,
                info.chordSymbol,
                info.frets,
            )
        val currentFolderIds =
            favoritesViewModel.getFolderIdsForVoicing(
                info.rootPitchClass,
                info.chordSymbol,
                info.frets,
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

@Composable
private fun ExplorerRoute(
    fretboardViewModel: FretboardViewModel,
    settingsViewModel: SettingsViewModel,
    soundEnabled: Boolean,
    leftHanded: Boolean,
    showTips: Boolean,
    showDidYouKnow: Boolean,
    isTabletWidth: Boolean,
    onFullScreen: () -> Unit,
    onShareChord: (ShareChordInfo) -> Unit,
    onShowInLibrary: (rootPitchClass: Int, formula: ChordFormula) -> Unit,
) {
    ExplorerTabContent(
        viewModel = fretboardViewModel,
        settingsViewModel = settingsViewModel,
        soundEnabled = soundEnabled,
        leftHanded = leftHanded,
        showTips = showTips,
        showDidYouKnow = showDidYouKnow,
        onDismissTips = { settingsViewModel.dismissExplorerTips() },
        onFullScreen = onFullScreen,
        isTabletWidth = isTabletWidth,
        onShareChord = { voicing, chordName, invLabel ->
            onShareChord(
                ShareChordInfo(
                    voicing = voicing,
                    chordName = chordName,
                    inversionLabel = invLabel,
                    tuningPitchClasses = fretboardViewModel.tuning.map { it.openPitchClass },
                ),
            )
        },
        onShowInLibrary = onShowInLibrary,
    )
}

@Composable
private fun LibraryRoute(
    libraryViewModel: ChordLibraryViewModel,
    fretboardViewModel: FretboardViewModel,
    currentFavorites: List<FavoriteVoicing>,
    leftHanded: Boolean,
    onNavigateToExplorer: () -> Unit,
    onShareChord: (ShareChordInfo) -> Unit,
    onShowFavoriteSheet: (SheetVoicingInfo) -> Unit,
) {
    ChordLibraryTab(
        viewModel = libraryViewModel,
        tuning = fretboardViewModel.tuning,
        onVoicingSelected = { voicing ->
            val state = libraryViewModel.uiState.value
            fretboardViewModel.applyVoicing(
                voicing = voicing,
                rootPitchClass = state.selectedRoot,
                formula = state.selectedFormula,
            )
            onNavigateToExplorer()
        },
        onVoicingLongPressed = { voicing ->
            val state = libraryViewModel.uiState.value
            val rootName = Notes.pitchClassToName(state.selectedRoot)
            val symbol = state.selectedFormula?.symbol ?: ""
            val tuning = fretboardViewModel.tuning
            val invLabel =
                state.selectedFormula?.let { formula ->
                    val inv =
                        ChordInfo.determineInversion(
                            voicing.frets,
                            state.selectedRoot,
                            formula,
                            tuning,
                        )
                    if (inv != ChordInfo.Inversion.ROOT) inv.label else null
                }
            onShareChord(
                ShareChordInfo(
                    voicing = voicing,
                    chordName = "$rootName$symbol",
                    inversionLabel = invLabel,
                    tuningPitchClasses = tuning.map { it.openPitchClass },
                ),
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
            onShowFavoriteSheet(
                SheetVoicingInfo(
                    rootPitchClass = state.selectedRoot,
                    chordSymbol = symbol,
                    frets = voicing.frets,
                ),
            )
        },
        onPlayVoicing = { fretboardViewModel.playVoicing(it) },
        onPlayVoicingsSequentially = { fretboardViewModel.playVoicingsSequentially(it) },
        leftHanded = leftHanded,
    )
}

@Composable
private fun ProgressionsRoute(
    isCompactWidth: Boolean,
    fretboardViewModel: FretboardViewModel,
    customProgressionViewModel: CustomProgressionViewModel,
    customProgressions: List<CustomProgression>,
    leftHanded: Boolean,
    lastFret: Int,
    onChordTapped: (rootPitchClass: Int, quality: String) -> Unit,
) {
    ConstrainedWidthContent(isCompactWidth) {
        ProgressionsTab(
            leftHanded = leftHanded,
            tuning = fretboardViewModel.tuning,
            lastFret = lastFret,
            customProgressions = customProgressions,
            onChordTapped = onChordTapped,
            onSaveProgression = { name, description, degrees, scaleType ->
                customProgressionViewModel.create(name, description, degrees, scaleType)
            },
            onEditProgression = { id, name, description, degrees, scaleType ->
                customProgressionViewModel.update(id, name, description, degrees, scaleType)
            },
            onDeleteProgression = { customProgressionViewModel.delete(it) },
            onPlayVoicing = { fretboardViewModel.playVoicing(it) },
            onPlayAll = { fretboardViewModel.playVoicingsSequentially(it) },
        )
    }
}

@Composable
private fun FavoritesRoute(
    favoritesViewModel: FavoritesViewModel,
    fretboardViewModel: FretboardViewModel,
    leftHanded: Boolean,
    onNavigateToExplorer: () -> Unit,
    onShareChord: (ShareChordInfo) -> Unit,
) {
    FavoritesTab(
        viewModel = favoritesViewModel,
        tuning = fretboardViewModel.tuning,
        onVoicingSelected = { voicing ->
            fretboardViewModel.applyVoicing(voicing)
            onNavigateToExplorer()
        },
        onShareVoicing = { voicing, chordName ->
            onShareChord(
                ShareChordInfo(
                    voicing = voicing,
                    chordName = chordName,
                    tuningPitchClasses = fretboardViewModel.tuning.map { it.openPitchClass },
                ),
            )
        },
        leftHanded = leftHanded,
    )
}

@Composable
private fun SongbookRoute(
    isCompactWidth: Boolean,
    songbookViewModel: SongbookViewModel,
    libraryViewModel: ChordLibraryViewModel,
    fretboardViewModel: FretboardViewModel,
    metronomeViewModel: MetronomeViewModel,
    leftHanded: Boolean,
    chordDisplayStyle: ChordDisplayStyle,
    chordColor: ChordColorOption,
    showChordDiagramRail: Boolean,
    onNavigateToLibrary: () -> Unit,
    onNavigateToMetronome: () -> Unit,
) {
    ConstrainedWidthContent(isCompactWidth) {
        SongbookTab(
            viewModel = songbookViewModel,
            onChordTapped = { chordName ->
                navigateToChord(chordName, libraryViewModel, onNavigateToLibrary)
            },
            onPlayChord = { fretboardViewModel.playChordByName(it) },
            onStartMetronome = { bpm ->
                metronomeViewModel.setBpm(bpm)
                if (!metronomeViewModel.isPlaying.value) {
                    metronomeViewModel.togglePlayback()
                }
                onNavigateToMetronome()
            },
            tuning = fretboardViewModel.tuning,
            leftHanded = leftHanded,
            chordDisplayStyle = chordDisplayStyle,
            chordColor = chordColor,
            showChordDiagramRail = showChordDiagramRail,
        )
    }
}

@Composable
private fun AchievementsRoute(
    learningProgressViewModel: LearningProgressViewModel,
    songbookViewModel: SongbookViewModel,
    favoritesCount: Int,
    unlockedAchievementIds: Set<String>,
    achievementRepository: AchievementRepository,
    reviewPromptRepository: ReviewPromptRepository,
    onUnlockedIdsChanged: (Set<String>) -> Unit,
    onReviewPrompt: () -> Unit,
) {
    val progressState by learningProgressViewModel.state.collectAsState()
    val sheetsState by songbookViewModel.sheets.collectAsState()
    val achievementContext =
        progressState.toAchievementContext(
            songsCount = sheetsState.size,
            favoritesCount = favoritesCount,
        )
    val newlyEarned =
        Achievements.checkNewlyEarned(
            achievementContext,
            unlockedAchievementIds,
        )
    if (newlyEarned.isNotEmpty()) {
        LaunchedEffect(newlyEarned) {
            newlyEarned.forEach { achievementRepository.unlock(it.id) }
            onUnlockedIdsChanged(achievementRepository.getUnlocked().keys)
            if (reviewPromptRepository.isEligible()) {
                onReviewPrompt()
            }
        }
    }
    AchievementsView(
        progressViewModel = learningProgressViewModel,
        unlockedIds = unlockedAchievementIds,
        songsCount = sheetsState.size,
        favoritesCount = favoritesCount,
    )
}

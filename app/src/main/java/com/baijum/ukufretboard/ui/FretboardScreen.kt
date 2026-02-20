package com.baijum.ukufretboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baijum.ukufretboard.R
import kotlinx.coroutines.launch
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.AchievementRepository
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.PitchMonitorSettings
import com.baijum.ukufretboard.data.PracticeTimerRepository

import com.baijum.ukufretboard.domain.AchievementChecker
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.toAchievementContext
import com.baijum.ukufretboard.domain.ChordVoicing
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
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel

/** Navigation section indices. */
private const val NAV_EXPLORER = 0
private const val NAV_LIBRARY = 1
private const val NAV_PATTERNS = 2
private const val NAV_PROGRESSIONS = 3
private const val NAV_FAVORITES = 4
private const val NAV_SONGBOOK = 5
private const val NAV_CAPO_GUIDE = 6
private const val NAV_CIRCLE_OF_FIFTHS = 7
private const val NAV_THEORY_QUIZ = 8
private const val NAV_INTERVAL_TRAINER = 9
private const val NAV_CHORD_SUBS = 10
private const val NAV_THEORY_LESSONS = 11
private const val NAV_MELODY_NOTEPAD = 12
private const val NAV_TUNER = 13
private const val NAV_LEARNING_PROGRESS = 14
private const val NAV_SCALE_CHORDS = 15
private const val NAV_GLOSSARY = 16
private const val NAV_NOTE_MAP = 17
private const val NAV_NOTE_QUIZ = 18
private const val NAV_CHORD_EAR = 19
private const val NAV_HELP = 20
private const val NAV_PITCH_MONITOR = 21
private const val NAV_SCALE_PRACTICE = 22
private const val NAV_ACHIEVEMENTS = 23
private const val NAV_CHORD_TRANSITION = 25
private const val NAV_DAILY_CHALLENGE = 26

private const val NAV_PRACTICE_ROUTINE = 28
private const val NAV_PLAY_ALONG = 29

/**
 * Drawer navigation item metadata.
 */
private data class DrawerItem(
    val index: Int,
    val label: String,
    val icon: ImageVector,
)

/**
 * A labeled group of drawer items rendered under a section header.
 */
private data class DrawerSection(
    val title: String,
    val items: List<DrawerItem>,
)

/** Drawer items organised into four groups: Play, Create, Learn, Reference. */
@Composable
private fun drawerSections(): List<DrawerSection> = listOf(
    DrawerSection(stringResource(R.string.nav_section_play), listOf(
        DrawerItem(NAV_EXPLORER, stringResource(R.string.nav_explorer), Icons.Filled.Home),
        DrawerItem(NAV_TUNER, stringResource(R.string.nav_tuner), Icons.Filled.Mic),
        DrawerItem(NAV_PITCH_MONITOR, stringResource(R.string.nav_pitch_monitor), Icons.Filled.Equalizer),
        DrawerItem(NAV_LIBRARY, stringResource(R.string.nav_chords), Icons.Filled.Search),
        DrawerItem(NAV_FAVORITES, stringResource(R.string.nav_favorites), Icons.Filled.Favorite),
    )),
    DrawerSection(stringResource(R.string.nav_section_create), listOf(
        DrawerItem(NAV_SONGBOOK, stringResource(R.string.nav_songs), Icons.Filled.Create),
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

/**
 * Top-level screen composable for Ukulele Companion.
 *
 * Uses a [ModalNavigationDrawer] to navigate between sections:
 * Explorer, Chords, Patterns, Progressions, Favorites, and Songs.
 *
 * A hamburger menu icon in the top app bar opens the drawer.
 * Both Explorer and Chord Library share the same [FretboardViewModel]
 * so that selecting a voicing in the library can load it onto the
 * explorer's fretboard.
 *
 * @param fretboardViewModel The shared [FretboardViewModel] instance.
 * @param libraryViewModel The [ChordLibraryViewModel] for the library section.
 * @param settingsViewModel The shared [SettingsViewModel] for app-wide settings.
 * @param favoritesViewModel The [FavoritesViewModel] for managing favorites.
 * @param songbookViewModel The [SongbookViewModel] for managing chord sheets.
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
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(NAV_EXPLORER) }
    var previousSection by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showFullScreen by rememberSaveable { mutableStateOf(false) }
    var shareChordInfo by remember { mutableStateOf<ShareChordInfo?>(null) }
    // State for the "Save to Folders" bottom sheet (shared between Library and Favorites)
    var sheetVoicing by remember { mutableStateOf<SheetVoicingInfo?>(null) }
    val currentFolders by favoritesViewModel.folders.collectAsState()

    BackHandler(enabled = previousSection != null && selectedSection == NAV_LIBRARY) {
        selectedSection = previousSection!!
        previousSection = null
    }

    // Initialize BackupRestoreViewModel with SettingsViewModel reference
    LaunchedEffect(Unit) {
        backupRestoreViewModel.init(settingsViewModel)
    }

    // Initialize sampled audio engine (loads OGG samples into SoundPool)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ToneGenerator.init(context)
    }

    // Achievement system
    val achievementRepository = remember { AchievementRepository(context) }
    var unlockedAchievementIds by remember {
        mutableStateOf(achievementRepository.getUnlocked().keys)
    }


    // Practice session timer — tracks time spent in the app
    val practiceTimerRepository = remember { PracticeTimerRepository(context) }
    val sessionStartMs = remember { System.currentTimeMillis() }
    var practiceStats by remember { mutableStateOf(practiceTimerRepository.stats()) }

    // Record session when leaving the app (onStop/onDestroy via DisposableEffect)
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            val durationMs = System.currentTimeMillis() - sessionStartMs
            if (durationMs >= 60_000L) { // Only record sessions >= 1 minute
                practiceTimerRepository.recordSession(durationMs)
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val allSections = drawerSections()
    val allItems = allSections.flatMap { it.items }

    val appSettings by settingsViewModel.settings.collectAsState()

    // Collect favorites so that isFavorite checks trigger recomposition
    val currentFavorites by favoritesViewModel.favorites.collectAsState()

    // Collect custom progressions
    val customProgressions by customProgressionViewModel.progressions.collectAsState()

    // Keep FretboardViewModel in sync with sound settings
    LaunchedEffect(appSettings.sound) {
        fretboardViewModel.setSoundSettings(appSettings.sound)
    }

    // Sync tuning settings
    LaunchedEffect(appSettings.tuning) {
        fretboardViewModel.setTuningSettings(appSettings.tuning)
    }

    // Sync fret count setting
    LaunchedEffect(appSettings.fretboard.lastFret) {
        fretboardViewModel.setLastFret(appSettings.fretboard.lastFret)
    }

    // Sync muted strings setting with chord library
    LaunchedEffect(appSettings.fretboard.allowMutedStrings) {
        libraryViewModel.setAllowMutedStrings(appSettings.fretboard.allowMutedStrings)
    }

    // Sync show-note-names setting
    LaunchedEffect(appSettings.fretboard.showNoteNames) {
        fretboardViewModel.setShowNoteNames(appSettings.fretboard.showNoteNames)
    }

    // Sync noise gate sensitivity to Pitch Monitor
    LaunchedEffect(appSettings.pitchMonitor.noiseGateSensitivity) {
        pitchMonitorViewModel.setNoiseGateRms(
            PitchMonitorSettings.sensitivityToRms(appSettings.pitchMonitor.noiseGateSensitivity)
        )
    }

    // Restore scale practice settings once
    LaunchedEffect(Unit) {
        scalePracticeViewModel.restoreSettings(appSettings.scalePractice)
    }

    // Full-screen landscape fretboard mode
    if (showFullScreen) {
        FullScreenFretboard(
            viewModel = fretboardViewModel,
            soundEnabled = appSettings.sound.enabled,
            leftHanded = appSettings.fretboard.leftHanded,
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
    val expandedState = rememberSaveable { mutableMapOf<String, Boolean>() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(R.string.app_full_name),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = 28.dp, vertical = 24.dp)
                            .semantics { heading() },
                    )
                    visibleSections.forEachIndexed { sectionIndex, section ->
                        if (sectionIndex > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        val expanded = expandedState.getOrPut(section.title) { true }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedState[section.title] = !expanded }
                                .padding(horizontal = 28.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { heading() },
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ExpandLess
                                else Icons.Filled.ExpandMore,
                                contentDescription = if (expanded) {
                                    stringResource(R.string.cd_collapse_section, section.title)
                                } else {
                                    stringResource(R.string.cd_expand_section, section.title)
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column {
                                section.items.forEach { item ->
                                    NavigationDrawerItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = selectedSection == item.index,
                                        onClick = {
                                            previousSection = null
                                            selectedSection = item.index
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.nav_help)) },
                        label = { Text(stringResource(R.string.nav_help)) },
                        selected = selectedSection == NAV_HELP,
                        onClick = {
                            previousSection = null
                            selectedSection = NAV_HELP
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
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
                        } else {
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
                // Section content
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
                        onShareChord = { voicing, chordName, invLabel ->
                            shareChordInfo = ShareChordInfo(
                                voicing = voicing,
                                chordName = chordName,
                                inversionLabel = invLabel,
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
                    NAV_TUNER -> TunerTab(
                        viewModel = tunerViewModel,
                        tuning = appSettings.tuning.tuning,
                        leftHanded = appSettings.fretboard.leftHanded,
                        soundEnabled = appSettings.sound.enabled,
                        tunerSettings = appSettings.tuner,
                    )
                    NAV_PITCH_MONITOR -> PitchMonitorTab(
                        viewModel = pitchMonitorViewModel,
                    )
                    NAV_LIBRARY -> ChordLibraryTab(
                        viewModel = libraryViewModel,
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
                            val tuning = FretboardViewModel.STANDARD_TUNING
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
                            )
                        },
                    isFavorite = { voicing ->
                        // Reference currentFavorites to trigger recomposition on change
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
                    NAV_PATTERNS -> StrumPatternsTab(
                        tuning = appSettings.tuning.tuning,
                    )
                    NAV_PROGRESSIONS -> ProgressionsTab(
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
                    NAV_FAVORITES -> FavoritesTab(
                        viewModel = favoritesViewModel,
                        onVoicingSelected = { voicing ->
                            fretboardViewModel.applyVoicing(voicing)
                            selectedSection = NAV_EXPLORER
                        },
                        onShareVoicing = { voicing, chordName ->
                            shareChordInfo = ShareChordInfo(
                                voicing = voicing,
                                chordName = chordName,
                            )
                        },
                        leftHanded = appSettings.fretboard.leftHanded,
                    )
                    NAV_SONGBOOK -> SongbookTab(
                        viewModel = songbookViewModel,
                        onChordTapped = { chordName ->
                            navigateToChord(chordName, libraryViewModel) {
                                previousSection = selectedSection
                                selectedSection = NAV_LIBRARY
                            }
                        },
                    )
                    NAV_CAPO_GUIDE -> CapoGuideView()
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
                        onPlayNote = { pitchClass ->
                            fretboardViewModel.playMelodyNote(pitchClass)
                        },
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
                        // Check for new achievements whenever the view is shown
                        val progressState by learningProgressViewModel.state.collectAsState()
                        val sheetsState by songbookViewModel.sheets.collectAsState()
                        val achievementContext = progressState.toAchievementContext(
                            songsCount = sheetsState.size,
                            favoritesCount = currentFavorites.size,
                        )
                        val newlyEarned = AchievementChecker.checkNewlyEarned(
                            achievementContext,
                            unlockedAchievementIds,
                        )
                        if (newlyEarned.isNotEmpty()) {
                            LaunchedEffect(newlyEarned) {
                                newlyEarned.forEach { achievementRepository.unlock(it.id) }
                                unlockedAchievementIds = achievementRepository.getUnlocked().keys
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

    // Settings bottom sheet
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
            pitchMonitorSettings = appSettings.pitchMonitor,
            onPitchMonitorSettingsChange = { newPm ->
                settingsViewModel.updatePitchMonitor { newPm }
            },
            backupRestoreViewModel = backupRestoreViewModel,
            onDismiss = { showSettings = false },
        )
    }

    // Share chord bottom sheet
    shareChordInfo?.let { info ->
        ShareChordBottomSheet(
            info = info,
            onDismiss = { shareChordInfo = null },
        )
    }

    // "Save to Folders" bottom sheet (shared between Chord Library and Favorites tabs)
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

/**
 * Holds the identifying information for a voicing whose folder sheet is open.
 */
private data class SheetVoicingInfo(
    val rootPitchClass: Int,
    val chordSymbol: String,
    val frets: List<Int>,
)

/**
 * Navigates to the Chord Library tab with the given chord name parsed into root + quality.
 */
private fun navigateToChord(
    chordName: String,
    libraryViewModel: ChordLibraryViewModel,
    switchTab: () -> Unit,
) {
    // Parse root note: first char (A-G), optionally followed by # or b
    val rootMatch = Regex("^([A-G][#b]?)").find(chordName) ?: return
    val rootStr = rootMatch.groupValues[1]
    val quality = chordName.removePrefix(rootStr)

    // Find the pitch class for the root
    val noteNames = com.baijum.ukufretboard.data.Notes.NOTE_NAMES_SHARP
    val noteNamesFlat = com.baijum.ukufretboard.data.Notes.NOTE_NAMES_FLAT
    val rootPitchClass = noteNames.indexOf(rootStr).takeIf { it >= 0 }
        ?: noteNamesFlat.indexOf(rootStr).takeIf { it >= 0 }
        ?: return

    libraryViewModel.selectRoot(rootPitchClass)
    val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
        .firstOrNull { it.symbol == quality }
    if (formula != null) {
        libraryViewModel.selectCategory(formula.category)
        libraryViewModel.selectFormula(formula)
    }
    switchTab()
}

/**
 * Content for the Explorer tab — the original interactive fretboard UI.
 *
 * @param viewModel The [FretboardViewModel] managing fretboard state.
 * @param soundEnabled Whether sound playback is enabled (controls play button visibility).
 */
@Composable
private fun ExplorerTabContent(
    viewModel: FretboardViewModel,
    settingsViewModel: SettingsViewModel,
    soundEnabled: Boolean,
    leftHanded: Boolean = false,
    showTips: Boolean = false,
    showDidYouKnow: Boolean = true,
    onDismissTips: () -> Unit = {},
    onFullScreen: () -> Unit = {},
    onShareChord: ((ChordVoicing, String, String?) -> Unit)? = null,
    onShowInLibrary: ((rootPitchClass: Int, formula: com.baijum.ukufretboard.data.ChordFormula) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTuning = uiState.tuning.ifEmpty { viewModel.tuning }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (showTips) {
            ExplorerTipsCard(onDismiss = onDismissTips)
        }

        // Scale selector (collapsible)
        ScaleSelector(
            state = uiState.scaleOverlay,
            tuningPitchClasses = currentTuning.map { it.openPitchClass },
            onRootChanged = viewModel::setScaleRoot,
            onScaleChanged = viewModel::setScale,
            onToggle = viewModel::toggleScaleOverlay,
            onPositionChanged = { position ->
                viewModel.setScalePositionRange(position?.fretRange)
            },
            onChordTapped = { chord ->
                val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
                    .firstOrNull { it.symbol == chord.quality }
                if (formula != null) {
                    val voicings = com.baijum.ukufretboard.data.VoicingGenerator.generate(
                        rootPitchClass = chord.rootPitchClass,
                        formula = formula,
                        tuning = currentTuning,
                    )
                    voicings.firstOrNull()?.let { viewModel.applyVoicing(it) }
                }
            },
        )

        // Interactive fretboard
        FretboardView(
            tuning = currentTuning,
            selections = uiState.selections,
            showNoteNames = uiState.showNoteNames,
            onFretTap = viewModel::toggleFret,
            getNoteAt = viewModel::getNoteAt,
            leftHanded = leftHanded,
            scaleNotes = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.scaleNotes else emptySet(),
            scaleRoot = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.root else null,
            scalePositionFretRange = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.positionFretRange else null,
            capoFret = uiState.capoFret,
            lastFret = uiState.lastFret,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
        )

        // Capo selector
        CapoSelector(
            capoFret = uiState.capoFret,
            lastFret = uiState.lastFret,
            onCapoChange = viewModel::setCapoFret,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(
                space = 12.dp,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            OutlinedButton(onClick = viewModel::clearAll) {
                Text(stringResource(R.string.explorer_reset))
            }
            OutlinedButton(onClick = viewModel::toggleScaleOverlay) {
                Text(if (uiState.scaleOverlay.enabled) stringResource(R.string.explorer_hide_scale) else stringResource(R.string.explorer_scales))
            }
            IconButton(onClick = onFullScreen) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.cd_full_screen),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Chord detection result with play button and detailed info
        val fretsList = uiState.selections.entries
            .sortedBy { it.key }
            .map { it.value ?: 0 }

        // Build share callback only when a chord is found
        val shareCallback: (() -> Unit)? = if (
            onShareChord != null &&
            uiState.detectionResult is com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
        ) {
            val chordFound = uiState.detectionResult as com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
            val chordResult = chordFound.result
            val formula = chordResult.matchedFormula
            // Use capo-adjusted frets for inversion/bass calculation
            val capoAdjustedFrets = fretsList.map { it + uiState.capoFret }
            val inversion = if (capoAdjustedFrets.size == 4 && formula != null) {
                ChordInfo.determineInversion(capoAdjustedFrets, chordResult.root.pitchClass, formula, currentTuning)
            } else null
            val invLabel = if (inversion != null && inversion != ChordInfo.Inversion.ROOT) inversion.label else null
            val displayName = if (inversion != null && inversion != ChordInfo.Inversion.ROOT) {
                val bassPc = ChordInfo.bassPitchClass(capoAdjustedFrets, currentTuning)
                ChordInfo.slashNotation(chordResult.name, inversion, bassPc)
            } else {
                chordResult.name
            }
            val shareVoicing = ChordVoicing(
                frets = fretsList,
                notes = chordResult.notes,
                minFret = fretsList.filter { it > 0 }.minOrNull() ?: 0,
                maxFret = fretsList.maxOrNull() ?: 0,
            );
            { onShareChord(shareVoicing, displayName, invLabel) }
        } else {
            null
        }

        // Build library navigation callback only when a chord with a known formula is found
        val showInLibraryCallback: (() -> Unit)? = if (
            onShowInLibrary != null &&
            uiState.detectionResult is com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
        ) {
            val chordFound = uiState.detectionResult as com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
            val formula = chordFound.result.matchedFormula
            if (formula != null) {
                { onShowInLibrary(chordFound.result.root.pitchClass, formula) }
            } else null
        } else null

        ChordResultView(
            detectionResult = uiState.detectionResult,
            fingerPositions = uiState.fingerPositions,
            onPlayChord = viewModel::playChord,
            soundEnabled = soundEnabled,
            frets = fretsList,
            tuning = currentTuning,
            capoFret = uiState.capoFret,
            onShareChord = shareCallback,
            onShowInLibrary = showInLibraryCallback,
            onAlternateChordTapped = if (onShowInLibrary != null) {
                { alt -> onShowInLibrary(alt.rootPitchClass, alt.formula) }
            } else null,
            onSuggestedChordTapped = { rootPitchClass, symbol ->
                val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
                    .firstOrNull { it.symbol == symbol }
                if (formula != null) {
                    val voicings = com.baijum.ukufretboard.data.VoicingGenerator.generate(
                        rootPitchClass = rootPitchClass,
                        formula = formula,
                        tuning = currentTuning,
                    )
                    voicings.firstOrNull()?.let { viewModel.applyVoicing(it) }
                }
            },
            showDidYouKnow = showDidYouKnow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Dismissible tips card shown on the Explorer screen for first-time users.
 * Explains tapping, scales, and capo features.
 */
@Composable
private fun ExplorerTipsCard(onDismiss: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.explorer_tips_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            TipRow(
                icon = Icons.Filled.TouchApp,
                text = stringResource(R.string.explorer_tips_tap),
            )
            TipRow(
                icon = Icons.Filled.MusicNote,
                text = stringResource(R.string.explorer_tips_scales),
            )
            TipRow(
                icon = Icons.Filled.Equalizer,
                text = stringResource(R.string.explorer_tips_capo),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.explorer_tips_dismiss))
                }
            }
        }
    }
}

@Composable
private fun TipRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Compact capo position selector with minus/plus buttons and a fret label.
 *
 * Displays "No capo" when fret is 0, otherwise shows "Capo: fret N".
 */
@Composable
private fun CapoSelector(
    capoFret: Int,
    lastFret: Int = FretboardViewModel.LAST_FRET,
    onCapoChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.label_capo),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = { onCapoChange((capoFret - 1).coerceAtLeast(0)) },
            enabled = capoFret > 0,
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.titleLarge,
                color = if (capoFret > 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
        Text(
            text = if (capoFret == 0) stringResource(R.string.explorer_capo_off) else "$capoFret",
            style = MaterialTheme.typography.titleMedium,
            color = if (capoFret > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(
            onClick = { onCapoChange((capoFret + 1).coerceAtMost(lastFret)) },
            enabled = capoFret < lastFret,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                color = if (capoFret < lastFret)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

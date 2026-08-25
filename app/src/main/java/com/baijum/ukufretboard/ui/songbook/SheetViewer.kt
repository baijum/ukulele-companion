package com.baijum.ukufretboard.ui.songbook

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordColorOption
import com.baijum.ukufretboard.data.ChordDisplayStyle
import com.baijum.ukufretboard.data.ChordProExporter
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.ShareUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREF_DETAILS_COLLAPSED = "song_details_collapsed"

/**
 * Viewer for a chord sheet with tappable chord names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetViewer(
    sheet: ChordSheet,
    allLabels: Set<String>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onChordTapped: (String) -> Unit,
    onPlayChord: (String) -> Unit,
    onStartMetronome: (Int) -> Unit,
    tuning: List<UkuleleString>,
    leftHanded: Boolean,
    onStrumPatternChange: (String) -> Unit,
    onLabelsChange: (List<String>) -> Unit,
    onApplyTranspose: (Int) -> Unit,
    chordDisplayStyle: ChordDisplayStyle = ChordDisplayStyle.ABOVE,
    chordColor: ChordColorOption = ChordColorOption.THEME,
    showChordDiagramRail: Boolean = true,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val chordSheetLabel = stringResource(R.string.songbook_chord_sheet)
    val exportChooserLabel = stringResource(R.string.songbook_export_chooser)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var tappedChord by remember { mutableStateOf<String?>(null) }
    var performanceMode by rememberSaveable { mutableStateOf(false) }

    // Transpose controls
    var transposeSemitones by rememberSaveable { mutableIntStateOf(0) }

    val songbookPrefs = context.getSharedPreferences("songbook_prefs", android.content.Context.MODE_PRIVATE)
    var songFontSize by rememberSaveable { mutableFloatStateOf(songbookPrefs.getFloat("song_font_size", 14f)) }

    // Persisted rather than session-only: a player who prefers the compact reading
    // layout wants it on the next song too, not just while scrolling through this one.
    var detailsCollapsed by
        rememberSaveable {
            mutableStateOf(songbookPrefs.getBoolean(PREF_DETAILS_COLLAPSED, false))
        }

    val displayContent =
        if (transposeSemitones != 0) {
            ChordSheetTranspose.transpose(sheet.content, transposeSemitones)
        } else {
            sheet.content
        }

    val songTextStyle =
        MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = songFontSize.sp,
        )

    if (performanceMode) {
        PerformanceModeView(
            displayContent = displayContent,
            textStyle = songTextStyle,
            onExit = { performanceMode = false },
            chordDisplayStyle = chordDisplayStyle,
            chordColor = chordColor,
            onChordTap = { tappedChord = it },
        )
        // Emitted alongside fullscreen too: the sheet is a separate window, and the
        // chord tap targets only exist there now that fullscreen renders parsed chords.
        ChordDetailSheetHost(
            tappedChord = tappedChord,
            tuning = tuning,
            leftHanded = leftHanded,
            onPlayChord = onPlayChord,
            onViewInLibrary = {
                tappedChord = null
                onChordTapped(it)
            },
            onDismiss = { tappedChord = null },
        )
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                text = sheet.title.ifEmpty { stringResource(R.string.songbook_untitled) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { heading() },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { showShareSheet = true }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            IconButton(onClick = { performanceMode = true }) {
                Icon(Icons.Filled.Fullscreen, contentDescription = stringResource(R.string.performance_mode))
            }
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.songbook_duplicate_cd)) },
                        onClick = {
                            showOverflowMenu = false
                            onDuplicate()
                        },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.dialog_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        }

        if (showDeleteDialog) {
            DeleteSongDialog(
                songName = sheet.title.ifEmpty { stringResource(R.string.songbook_untitled) },
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    onDelete()
                },
            )
        }

        // Scroll, section and tempo state. Held here rather than inside the
        // details panel: the lyrics area and the auto-scroll controls need it
        // whether the panel is showing or collapsed.
        var autoScrolling by rememberSaveable { mutableStateOf(false) }
        var scrollSpeed by rememberSaveable { mutableFloatStateOf(1f) }
        val scrollState = rememberScrollState()
        val programmaticScroll = remember { mutableStateOf(false) }

        val sections =
            remember(displayContent) {
                ChordSheetFormatter.extractSections(displayContent)
            }
        val sectionOffsets = remember { mutableMapOf<Int, Int>() }
        val sectionScrollScope = rememberCoroutineScope()
        val sectionLineIndices = remember(sections) { sections.map { it.lineIndex }.toSet() }

        val songTempo =
            remember(displayContent) {
                ChordSheetFormatter.extractTempo(displayContent)
            }

        // Auto-scroll effect
        LaunchedEffect(autoScrolling, scrollSpeed) {
            if (autoScrolling) {
                while (autoScrolling) {
                    programmaticScroll.value = true
                    try {
                        scrollState.animateScrollTo(
                            scrollState.value + scrollSpeed.toInt().coerceAtLeast(1),
                            animationSpec =
                                androidx.compose.animation.core.tween(
                                    durationMillis = 16,
                                    easing = androidx.compose.animation.core.LinearEasing,
                                ),
                        )
                    } finally {
                        programmaticScroll.value = false
                    }
                    delay(16L)
                }
            }
        }

        // Pause auto-scroll when user manually scrolls (not programmatic)
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress && autoScrolling && !programmaticScroll.value) {
                autoScrolling = false
            }
        }

        SongDetailsToggle(
            collapsed = detailsCollapsed,
            onToggle = {
                detailsCollapsed = !detailsCollapsed
                songbookPrefs.edit().putBoolean(PREF_DETAILS_COLLAPSED, detailsCollapsed).apply()
            },
        )

        val savedInKeyMsg = stringResource(R.string.songbook_saved_in_key)
        CollapsibleDetails(visible = !detailsCollapsed) {
            SongMetaLines(
                sheet = sheet,
                displayContent = displayContent,
                transposeSemitones = transposeSemitones,
            )

            StrumPatternRow(
                patternName = sheet.strumPatternName,
                onPatternChange = onStrumPatternChange,
            )

            LabelDisplayRow(
                labels = sheet.labels,
                allLabels = allLabels,
                onLabelsChange = onLabelsChange,
            )

            if (sheet.viewCount > 0) {
                SongStatsRow(sheet = sheet)
            }

            TransposeRow(
                transposeSemitones = transposeSemitones,
                onTransposeChange = { transposeSemitones = it },
            )

            if (transposeSemitones != 0) {
                TransposeApplyRow(
                    transposeSemitones = transposeSemitones,
                    onSaveInKey = {
                        onApplyTranspose(transposeSemitones)
                        transposeSemitones = 0
                        Toast.makeText(context, savedInKeyMsg, Toast.LENGTH_SHORT).show()
                    },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (sections.isNotEmpty()) {
                SectionShortcutsRow(
                    sections = sections,
                    onSectionSelected = { lineIndex ->
                        sectionOffsets[lineIndex]?.let { offset ->
                            sectionScrollScope.launch {
                                programmaticScroll.value = true
                                try {
                                    scrollState.animateScrollTo(offset)
                                } finally {
                                    programmaticScroll.value = false
                                }
                            }
                        }
                    },
                )
            }

            if (songTempo != null) {
                TempoRow(songTempo = songTempo, onStartMetronome = onStartMetronome)
            }

            if (showChordDiagramRail) {
                ChordDiagramRail(
                    displayContent = displayContent,
                    tuning = tuning,
                    leftHanded = leftHanded,
                    onChordTap = { tappedChord = it },
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
            ) {
                displayContent.lines().forEachIndexed { lineIndex, line ->
                    val sectionModifier =
                        if (lineIndex in sectionLineIndices) {
                            Modifier.onGloballyPositioned { coords ->
                                sectionOffsets[lineIndex] = coords.positionInParent().y.toInt()
                            }
                        } else {
                            Modifier
                        }
                    ChordSheetLine(
                        line = line,
                        isSectionHeading = lineIndex in sectionLineIndices,
                        textStyle = songTextStyle,
                        chordDisplayStyle = chordDisplayStyle,
                        chordColor = chordColor,
                        onChordTap = { tappedChord = it },
                        modifier = sectionModifier,
                    )
                }
            }

            // Font size controls overlay
            val fontDecreaseDesc = stringResource(R.string.songbook_font_decrease)
            val fontIncreaseDesc = stringResource(R.string.songbook_font_increase)
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        songFontSize = (songFontSize - 2f).coerceAtLeast(10f)
                        songbookPrefs.edit().putFloat("song_font_size", songFontSize).apply()
                    },
                    modifier = Modifier.semantics { contentDescription = fontDecreaseDesc },
                ) {
                    Text("A−", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                SmallFloatingActionButton(
                    onClick = {
                        songFontSize = (songFontSize + 2f).coerceAtMost(32f)
                        songbookPrefs.edit().putFloat("song_font_size", songFontSize).apply()
                    },
                    modifier = Modifier.semantics { contentDescription = fontIncreaseDesc },
                ) {
                    Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Auto-scroll controls overlay
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (autoScrolling) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.5f to "0.5x", 1f to "1x", 2f to "2x", 3f to "3x").forEach { (speed, label) ->
                            val speedDesc = stringResource(R.string.cd_scroll_speed, label)
                            FilterChip(
                                selected = scrollSpeed == speed,
                                onClick = { scrollSpeed = speed },
                                label = { Text(label) },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = speedDesc
                                        if (scrollSpeed == speed) stateDescription = "selected"
                                    },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (autoScrolling) {
                        FloatingActionButton(
                            onClick = {
                                autoScrolling = false
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = stringResource(R.string.cd_stop_scroll),
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { autoScrolling = !autoScrolling },
                    ) {
                        Icon(
                            imageVector = if (autoScrolling) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription =
                                if (autoScrolling) {
                                    stringResource(
                                        R.string.cd_pause_scroll,
                                    )
                                } else {
                                    stringResource(R.string.cd_auto_scroll)
                                },
                        )
                    }
                }
            }
        }

        val copiedMsg = stringResource(R.string.share_copied)
        if (showShareSheet) {
            val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val effectiveSheet =
                if (transposeSemitones != 0) {
                    sheet.copy(
                        content = displayContent,
                        key = ChordSheetTranspose.transposeKey(sheet.key, transposeSemitones),
                    )
                } else {
                    sheet
                }
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = shareSheetState,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.share_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .padding(bottom = 16.dp, start = 8.dp)
                                .semantics { heading() },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_chordpro)) },
                        leadingContent = {
                            Icon(Icons.Filled.MusicNote, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable(role = Role.Button) {
                                showShareSheet = false
                                val chordProText = ChordProExporter.export(effectiveSheet)
                                val intent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, chordProText)
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            ChordProExporter.suggestedFilename(sheet),
                                        )
                                    }
                                context.startActivity(
                                    Intent.createChooser(intent, exportChooserLabel),
                                )
                            },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_text)) },
                        leadingContent = {
                            Icon(Icons.Filled.Description, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable(role = Role.Button) {
                                showShareSheet = false
                                val formatted = ChordSheetFormatter.formatChordsAboveLyrics(effectiveSheet)
                                ShareUtils.shareText(
                                    context = context,
                                    title = sheet.title.ifEmpty { chordSheetLabel },
                                    text = formatted,
                                )
                            },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_copy_clipboard)) },
                        leadingContent = {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable(role = Role.Button) {
                                showShareSheet = false
                                val formatted = ChordSheetFormatter.formatChordsAboveLyrics(effectiveSheet)
                                ShareUtils.copyToClipboard(context, chordSheetLabel, formatted)
                                Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                                view.announceForAccessibility(copiedMsg)
                            },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_pdf)) },
                        leadingContent = {
                            Icon(Icons.Filled.Description, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable(role = Role.Button) {
                                showShareSheet = false
                                val formatted = ChordSheetFormatter.formatChordsAboveLyrics(effectiveSheet)
                                val title = effectiveSheet.title.ifEmpty { chordSheetLabel }
                                ShareUtils.sharePdf(context, title, formatted)
                            },
                    )
                }
            }
        }

        ChordDetailSheetHost(
            tappedChord = tappedChord,
            tuning = tuning,
            leftHanded = leftHanded,
            onPlayChord = onPlayChord,
            onViewInLibrary = {
                tappedChord = null
                onChordTapped(it)
            },
            onDismiss = { tappedChord = null },
        )
    }
}

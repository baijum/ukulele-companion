package com.baijum.ukufretboard.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.ChordProExporter
import com.baijum.ukufretboard.data.ChordProParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.StrumPatterns
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.KeyDetector
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.viewmodel.SongSortOrder
import com.baijum.ukufretboard.viewmodel.SongbookViewModel

/**
 * Songbook tab showing a list of chord sheets, a viewer, and an editor.
 *
 * @param viewModel The [SongbookViewModel] managing chord sheets.
 * @param onChordTapped Callback when a chord name in a sheet is tapped (navigates to library).
 * @param onPlayChord Callback to play a named chord via audio.
 * @param tuning Current ukulele tuning for generating chord voicings.
 * @param leftHanded Whether left-handed mode is active (mirrors chord diagrams).
 */
@Composable
fun SongbookTab(
    viewModel: SongbookViewModel,
    onChordTapped: (String) -> Unit,
    onPlayChord: (String) -> Unit = {},
    onStartMetronome: (Int) -> Unit = {},
    tuning: List<UkuleleString> = emptyList(),
    leftHanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val sheets by viewModel.sheets.collectAsState()
    val currentSheet by viewModel.currentSheet.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val allLabels by viewModel.allLabels.collectAsState()
    val selectedLabels by viewModel.selectedLabels.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    when {
        isEditing -> {
            SheetEditor(
                sheet = currentSheet,
                allLabels = allLabels,
                onSave = { title, artist, content, strumPatternName, labels ->
                    viewModel.saveSheet(
                        title = title,
                        artist = artist,
                        content = content,
                        strumPatternName = strumPatternName,
                        labels = labels,
                    )
                },
                onCancel = { viewModel.closeSheet() },
            )
        }
        currentSheet != null -> {
            SheetViewer(
                sheet = currentSheet!!,
                allLabels = allLabels,
                onBack = { viewModel.closeSheet() },
                onEdit = { viewModel.startEditing(currentSheet) },
                onDelete = {
                    viewModel.deleteSheet(currentSheet!!.id)
                    viewModel.closeSheet()
                },
                onDuplicate = { viewModel.duplicateSheet(currentSheet!!) },
                onChordTapped = onChordTapped,
                onPlayChord = onPlayChord,
                onStartMetronome = onStartMetronome,
                tuning = tuning,
                leftHanded = leftHanded,
                onStrumPatternChange = { viewModel.updateStrumPattern(it) },
                onLabelsChange = { viewModel.updateLabels(it) },
                onApplyTranspose = { viewModel.applyTranspose(it) },
            )
        }
        else -> {
            SheetList(
                sheets = sheets,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                sortOrder = sortOrder,
                onSortOrderChange = { viewModel.setSortOrder(it) },
                allLabels = allLabels,
                selectedLabels = selectedLabels,
                onToggleLabelFilter = { viewModel.toggleLabelFilter(it) },
                onClearLabelFilter = { viewModel.clearLabelFilter() },
                onSheetTapped = { viewModel.openSheet(it) },
                onNewSheet = { viewModel.startEditing() },
                onImport = { content, filename ->
                    if (filename != null && ChordProParser.isChordProFile(filename)) {
                        viewModel.importChordPro(content, filename)
                    } else {
                        viewModel.importPlainText(content, filename)
                    }
                },
                isSelectionMode = isSelectionMode,
                selectedIds = selectedIds,
                onLongPress = { viewModel.enterSelectionMode(it.id) },
                onToggleSelect = { viewModel.toggleSelection(it) },
                onSelectAll = { viewModel.selectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onDeleteSelected = { viewModel.deleteSelected() },
                modifier = modifier,
            )
        }
    }
}

/**
 * List of saved chord sheets with search and sort controls.
 */
@Composable
private fun SheetList(
    sheets: List<ChordSheet>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: SongSortOrder,
    onSortOrderChange: (SongSortOrder) -> Unit,
    allLabels: Set<String>,
    selectedLabels: Set<String>,
    onToggleLabelFilter: (String) -> Unit,
    onClearLabelFilter: () -> Unit,
    onSheetTapped: (ChordSheet) -> Unit,
    onNewSheet: () -> Unit,
    onImport: (String, String?) -> Unit,
    isSelectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onLongPress: (ChordSheet) -> Unit = {},
    onToggleSelect: (String) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val importFailedMsg = stringResource(R.string.songbook_import_failed)
    var showPasteDialog by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText() ?: return@let
                inputStream.close()
                val filename = it.lastPathSegment
                onImport(content, filename)
            } catch (_: Exception) {
                Toast.makeText(context, importFailedMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.songbook_search_hint)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.songbook_search_clear),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Sort menu
                Box {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.songbook_sort),
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        SongSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sortOrderLabel(order),
                                        fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onSortOrderChange(order)
                                    showSortMenu = false
                                },
                            )
                        }
                    }
                }
            }

            // Label filter chips
            if (allLabels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selectedLabels.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = onClearLabelFilter,
                            label = { Text(stringResource(R.string.songbook_filter_clear)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                    allLabels.forEach { label ->
                        val isSelected = label in selectedLabels
                        val filterDesc = stringResource(R.string.cd_filter_label, label)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleLabelFilter(label) },
                            label = { Text(label) },
                            modifier = Modifier.semantics {
                                contentDescription = filterDesc
                                if (isSelected) stateDescription = "selected"
                            },
                        )
                    }
                }
            }

            if (sheets.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.songbook_empty_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.songbook_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = onSelectAll) { Text("All") }
                            TextButton(onClick = onClearSelection) { Text("Cancel") }
                            TextButton(onClick = onDeleteSelected) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(sheets) { sheet ->
                        SheetCard(
                            sheet = sheet,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelect(sheet.id)
                                } else {
                                    onSheetTapped(sheet)
                                }
                            },
                            onLongClick = { onLongPress(sheet) },
                            onLabelTapped = onToggleLabelFilter,
                            isSelected = sheet.id in selectedIds,
                            showCheckbox = isSelectionMode,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                var showImportMenu by remember { mutableStateOf(false) }
                SmallFloatingActionButton(
                    onClick = { showImportMenu = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(
                        Icons.Filled.FileOpen,
                        contentDescription = stringResource(R.string.cd_import_chordpro),
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = showImportMenu,
                    onDismissRequest = { showImportMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.songbook_import_from_file)) },
                        leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            importLauncher.launch(arrayOf("text/*", "*/*"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.songbook_paste_chordpro)) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            showPasteDialog = true
                        },
                    )
                }
            }
            FloatingActionButton(
                onClick = onNewSheet,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_new_chord_sheet))
            }
        }

        if (showPasteDialog) {
            PasteChordProDialog(
                onImport = { content ->
                    onImport(content, "pasted.cho")
                    showPasteDialog = false
                },
                onDismiss = { showPasteDialog = false },
            )
        }
    }
}

@Composable
private fun sortOrderLabel(order: SongSortOrder): String = when (order) {
    SongSortOrder.LAST_MODIFIED -> stringResource(R.string.songbook_sort_modified)
    SongSortOrder.DATE_ADDED -> stringResource(R.string.songbook_sort_added)
    SongSortOrder.TITLE -> stringResource(R.string.songbook_sort_title)
    SongSortOrder.ARTIST -> stringResource(R.string.songbook_sort_artist)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SheetCard(
    sheet: ChordSheet,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onLabelTapped: (String) -> Unit,
    isSelected: Boolean = false,
    showCheckbox: Boolean = false,
) {
    val untitledLabel = stringResource(R.string.songbook_untitled)
    val displayTitle = sheet.title.ifEmpty { untitledLabel }
    val cardDescription = if (sheet.artist.isNotEmpty()) {
        "$displayTitle by ${sheet.artist}"
    } else {
        displayTitle
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = cardDescription },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCheckbox) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (sheet.artist.isNotEmpty()) {
                Text(
                    text = sheet.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sheet.labels.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    sheet.labels.forEach { label ->
                        val labelDesc = stringResource(R.string.cd_filter_label, label)
                        AssistChip(
                            onClick = { onLabelTapped(label) },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            modifier = Modifier
                                .height(24.dp)
                                .semantics { contentDescription = labelDesc },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * Viewer for a chord sheet with tappable chord names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetViewer(
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
) {
    val context = LocalContext.current
    val view = LocalView.current
    val chordSheetLabel = stringResource(R.string.songbook_chord_sheet)
    val exportChooserLabel = stringResource(R.string.songbook_export_chooser)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var tappedChord by remember { mutableStateOf<String?>(null) }
    var performanceMode by rememberSaveable { mutableStateOf(false) }

    // Transpose controls
    var transposeSemitones by rememberSaveable { mutableStateOf(0) }

    val fontSizePrefs = context.getSharedPreferences("songbook_prefs", android.content.Context.MODE_PRIVATE)
    var songFontSize by rememberSaveable { mutableStateOf(fontSizePrefs.getFloat("song_font_size", 14f)) }

    val displayContent = if (transposeSemitones != 0) {
        ChordSheetTranspose.transpose(sheet.content, transposeSemitones)
    } else {
        sheet.content
    }

    val songTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = songFontSize.sp,
    )

    if (performanceMode) {
        PerformanceModeView(
            displayContent = displayContent,
            textStyle = songTextStyle,
            onExit = { performanceMode = false },
        )
        return
    }

    Column(
        modifier = Modifier
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
            )
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.songbook_duplicate_cd))
            }
            IconButton(onClick = { showShareSheet = true }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            IconButton(onClick = { performanceMode = true }) {
                Icon(Icons.Filled.Fullscreen, contentDescription = stringResource(R.string.performance_mode))
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.dialog_delete))
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

        if (sheet.artist.isNotEmpty()) {
            Text(
                text = sheet.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
            )
        }

        // Detect song key from chords
        val songChords = remember(sheet.content) { ChordParser.extractChords(sheet.content) }
        val detectedKey = remember(songChords) { KeyDetector.detectKey(songChords) }

        // Key display
        if (detectedKey != null) {
            Text(
                text = stringResource(R.string.songbook_key_prefix) + detectedKey.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
            )
        }

        // Strum pattern display
        StrumPatternRow(
            patternName = sheet.strumPatternName,
            onPatternChange = onStrumPatternChange,
        )

        // Labels display
        LabelDisplayRow(
            labels = sheet.labels,
            allLabels = allLabels,
            onLabelsChange = onLabelsChange,
        )

        // Song statistics
        if (sheet.viewCount > 0) {
            SongStatsRow(sheet = sheet)
        }

        // Transpose controls
        val transposeDownDesc = stringResource(R.string.cd_transpose_down)
        val transposeUpDesc = stringResource(R.string.cd_transpose_up)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.songbook_transpose),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { transposeSemitones-- },
                modifier = Modifier.semantics { contentDescription = transposeDownDesc },
            ) {
                Text("\u2212")
            }
            Text(
                text = ChordSheetTranspose.semitoneLabel(transposeSemitones),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            OutlinedButton(
                onClick = { transposeSemitones++ },
                modifier = Modifier.semantics { contentDescription = transposeUpDesc },
            ) {
                Text("+")
            }
            if (transposeSemitones != 0) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { transposeSemitones = 0 }) {
                    Text(stringResource(R.string.dialog_reset))
                }
            }
        }

        // Capo equivalent and "Save in this key" (shown when transposed)
        if (transposeSemitones != 0) {
            val capoFret = ((transposeSemitones % 12) + 12) % 12
            if (capoFret > 0) {
                Text(
                    text = stringResource(R.string.songbook_capo_hint, capoFret),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                )
            }

            val savedMsg = stringResource(R.string.songbook_saved_in_key)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilledTonalButton(
                    onClick = {
                        onApplyTranspose(transposeSemitones)
                        transposeSemitones = 0
                        Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(stringResource(R.string.songbook_save_in_key))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Auto-scroll state
        var autoScrolling by rememberSaveable { mutableStateOf(false) }
        var scrollSpeed by rememberSaveable { mutableStateOf(1f) }
        val scrollState = rememberScrollState()
        val programmaticScroll = remember { mutableStateOf(false) }

        // Section navigation
        val sections = remember(displayContent) {
            ChordSheetFormatter.extractSections(displayContent)
        }
        val sectionOffsets = remember { mutableMapOf<Int, Int>() }
        val sectionScrollScope = rememberCoroutineScope()

        if (sections.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sections.forEach { section ->
                    AssistChip(
                        onClick = {
                            sectionOffsets[section.lineIndex]?.let { offset ->
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
                        label = { Text(section.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // Tempo / metronome integration
        val songTempo = remember(displayContent) {
            ChordSheetFormatter.extractTempo(displayContent)
        }
        if (songTempo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.songbook_tempo_label, songTempo),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { onStartMetronome(songTempo) },
                ) {
                    Text(stringResource(R.string.songbook_start_metronome))
                }
            }
        }

        // Auto-scroll effect
        LaunchedEffect(autoScrolling, scrollSpeed) {
            if (autoScrolling) {
                while (autoScrolling) {
                    programmaticScroll.value = true
                    try {
                        scrollState.animateScrollTo(
                            scrollState.value + scrollSpeed.toInt().coerceAtLeast(1),
                            animationSpec = androidx.compose.animation.core.tween(
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

        val sectionLineIndices = remember(sections) { sections.map { it.lineIndex }.toSet() }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                displayContent.lines().forEachIndexed { lineIndex, line ->
                    val sectionModifier = if (lineIndex in sectionLineIndices) {
                        Modifier.onGloballyPositioned { coords ->
                            sectionOffsets[lineIndex] = coords.positionInParent().y.toInt()
                        }
                    } else {
                        Modifier
                    }
                    val segments = ChordParser.parseLine(line)
                    Column(modifier = sectionModifier) {
                        if (segments.isEmpty()) {
                            Text(
                                text = " ",
                                style = songTextStyle,
                            )
                        } else {
                            val chordColor = MaterialTheme.colorScheme.primary
                            val chordPositions = mutableListOf<Pair<Int, String>>()
                            val lyricLine = StringBuilder()
                            var hasChords = false

                            segments.forEach { segment ->
                                when (segment) {
                                    is ChordParser.TextSegment.PlainText -> {
                                        lyricLine.append(segment.text)
                                    }
                                    is ChordParser.TextSegment.Chord -> {
                                        hasChords = true
                                        chordPositions.add(lyricLine.length to segment.name)
                                    }
                                }
                            }

                            if (hasChords) {
                                val chordAnnotated = buildAnnotatedString {
                                    var cursor = 0
                                    chordPositions.forEach { (pos, name) ->
                                        if (pos > cursor) {
                                            append(" ".repeat(pos - cursor))
                                            cursor = pos
                                        }
                                        withLink(
                                            LinkAnnotation.Clickable(
                                                tag = name,
                                                styles = TextLinkStyles(
                                                    style = SpanStyle(
                                                        color = chordColor,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                ),
                                                linkInteractionListener = {
                                                    tappedChord = name
                                                },
                                            )
                                        ) {
                                            append(name)
                                        }
                                        cursor += name.length
                                    }
                                }

                                Text(
                                    text = chordAnnotated,
                                    style = songTextStyle,
                                )
                            }

                            Text(
                                text = lyricLine.toString(),
                                style = songTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                }
            }

            // Font size controls overlay
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        songFontSize = (songFontSize - 2f).coerceAtLeast(10f)
                        fontSizePrefs.edit().putFloat("song_font_size", songFontSize).apply()
                    },
                    modifier = Modifier.semantics { contentDescription = context.getString(R.string.songbook_font_decrease) },
                ) {
                    Text("A−", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                SmallFloatingActionButton(
                    onClick = {
                        songFontSize = (songFontSize + 2f).coerceAtMost(32f)
                        fontSizePrefs.edit().putFloat("song_font_size", songFontSize).apply()
                    },
                    modifier = Modifier.semantics { contentDescription = context.getString(R.string.songbook_font_increase) },
                ) {
                    Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Auto-scroll controls overlay
            Column(
                modifier = Modifier
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
                                modifier = Modifier.semantics {
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
                            contentDescription = if (autoScrolling) stringResource(R.string.cd_pause_scroll) else stringResource(R.string.cd_auto_scroll),
                        )
                    }
                }
            }
        }

        val copiedMsg = stringResource(R.string.share_copied)
        if (showShareSheet) {
            val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val effectiveSheet = if (transposeSemitones != 0) {
                sheet.copy(content = displayContent)
            } else {
                sheet
            }
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = shareSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.share_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 8.dp)
                            .semantics { heading() },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_chordpro)) },
                        leadingContent = {
                            Icon(Icons.Filled.MusicNote, contentDescription = null)
                        },
                        modifier = Modifier.clickable(role = Role.Button) {
                            showShareSheet = false
                            val chordProText = ChordProExporter.export(effectiveSheet)
                            val intent = Intent(Intent.ACTION_SEND).apply {
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
                        modifier = Modifier.clickable(role = Role.Button) {
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
                        modifier = Modifier.clickable(role = Role.Button) {
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
                        modifier = Modifier.clickable(role = Role.Button) {
                            showShareSheet = false
                            val formatted = ChordSheetFormatter.formatChordsAboveLyrics(effectiveSheet)
                            val title = effectiveSheet.title.ifEmpty { chordSheetLabel }
                            ShareUtils.sharePdf(context, title, formatted)
                        },
                    )
                }
            }
        }

        if (tappedChord != null) {
            val chordName = tappedChord!!
            val chordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            val voicing = remember(chordName, tuning) {
                val parsed = ChordNameParser.parse(chordName) ?: return@remember null
                if (tuning.isEmpty()) return@remember null
                VoicingGenerator.generate(parsed.rootPitchClass, parsed.formula, tuning)
                    .firstOrNull()
            }

            val playChordDesc = stringResource(R.string.songbook_play_chord, chordName)
            val viewInLibraryDesc = stringResource(R.string.songbook_view_in_library, chordName)

            ModalBottomSheet(
                onDismissRequest = { tappedChord = null },
                sheetState = chordSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = chordName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .semantics { heading() },
                    )

                    if (voicing != null) {
                        VerticalChordDiagram(
                            voicing = voicing,
                            onClick = {},
                            chordName = chordName,
                            leftHanded = leftHanded,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.songbook_play_chord, chordName)) },
                        leadingContent = {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                onPlayChord(chordName)
                            }
                            .semantics { contentDescription = playChordDesc },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.songbook_view_in_library, chordName)) },
                        leadingContent = {
                            Icon(Icons.Filled.MusicNote, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                tappedChord = null
                                onChordTapped(chordName)
                            }
                            .semantics { contentDescription = viewInLibraryDesc },
                    )
                }
            }
        }
    }
}

/**
 * Editor for creating/editing a chord sheet with preview and chord insertion.
 */
@Composable
private fun SheetEditor(
    sheet: ChordSheet?,
    allLabels: Set<String>,
    onSave: (title: String, artist: String, content: String, strumPatternName: String, labels: List<String>) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember(sheet?.id) { mutableStateOf(sheet?.title ?: "") }
    var artist by remember(sheet?.id) { mutableStateOf(sheet?.artist ?: "") }
    var content by remember(sheet?.id) { mutableStateOf(sheet?.content ?: "") }
    var strumPatternName by remember(sheet?.id) { mutableStateOf(sheet?.strumPatternName ?: "") }
    var labels by remember(sheet?.id) { mutableStateOf(sheet?.labels ?: emptyList()) }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasChanges = title != (sheet?.title ?: "") ||
        artist != (sheet?.artist ?: "") ||
        content != (sheet?.content ?: "") ||
        strumPatternName != (sheet?.strumPatternName ?: "") ||
        labels != (sheet?.labels ?: emptyList<String>())

    val handleCancel = {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            onCancel()
        }
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDismiss = { showDiscardDialog = false },
            onDiscard = {
                showDiscardDialog = false
                onCancel()
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.songbook_field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text(stringResource(R.string.songbook_field_artist)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Strum pattern picker
        StrumPatternRow(
            patternName = strumPatternName,
            onPatternChange = { strumPatternName = it },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Label editor
        LabelEditorSection(
            labels = labels,
            allLabels = allLabels,
            onLabelsChange = { labels = it },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Chord count and insert helper
        val detectedChords = remember(content) { ChordParser.extractChords(content) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.songbook_field_lyrics_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (detectedChords.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.songbook_chords_detected, detectedChords.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Chord insertion helper row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val commonChords = listOf("C", "G", "Am", "F", "Em", "Dm", "D", "A", "E", "Bm")
            commonChords.forEach { chord ->
                val insertDesc = stringResource(R.string.songbook_insert_chord)
                val insertChordDesc = stringResource(R.string.cd_insert_chord, insertDesc, chord)
                FilterChip(
                    selected = false,
                    onClick = { content += "[$chord]" },
                    label = { Text(chord) },
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = insertChordDesc
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Edit / Preview toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilterChip(
                selected = !showPreview,
                onClick = { showPreview = false },
                label = { Text(stringResource(R.string.songbook_editor_edit)) },
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                selected = showPreview,
                onClick = { showPreview = true },
                label = { Text(stringResource(R.string.songbook_editor_preview)) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (showPreview) {
            // Live preview of chords above lyrics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                content.lines().forEach { line ->
                    val segments = ChordParser.parseLine(line)
                    if (segments.isEmpty()) {
                        Text(
                            text = " ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    } else {
                        val chordColor = MaterialTheme.colorScheme.primary
                        val chordPositions = mutableListOf<Pair<Int, String>>()
                        val lyricLine = StringBuilder()
                        var hasChords = false

                        segments.forEach { segment ->
                            when (segment) {
                                is ChordParser.TextSegment.PlainText -> {
                                    lyricLine.append(segment.text)
                                }
                                is ChordParser.TextSegment.Chord -> {
                                    hasChords = true
                                    chordPositions.add(lyricLine.length to segment.name)
                                }
                            }
                        }

                        if (hasChords) {
                            val chordAnnotated = buildAnnotatedString {
                                var cursor = 0
                                chordPositions.forEach { (pos, name) ->
                                    if (pos > cursor) {
                                        append(" ".repeat(pos - cursor))
                                        cursor = pos
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = chordColor,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    ) {
                                        append(name)
                                    }
                                    cursor += name.length
                                }
                            }

                            Text(
                                text = chordAnnotated,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }

                        Text(
                            text = lyricLine.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.songbook_field_lyrics)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { handleCancel() }) {
                Text(stringResource(R.string.dialog_cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onSave(title, artist, content, strumPatternName, labels) },
                enabled = title.isNotBlank(),
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.songbook_discard_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.songbook_discard_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    stringResource(R.string.songbook_discard),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun DeleteSongDialog(
    songName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.song_delete_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.song_delete_message, songName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.dialog_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun PasteChordProDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pasteContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.songbook_paste_chordpro),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            OutlinedTextField(
                value = pasteContent,
                onValueChange = { pasteContent = it },
                label = { Text(stringResource(R.string.songbook_paste_chordpro_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(pasteContent) },
                enabled = pasteContent.isNotBlank(),
            ) {
                Text(stringResource(R.string.songbook_paste_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * Displays the associated strum pattern for a song, with options to select, change, or remove.
 */
@Composable
private fun StrumPatternRow(
    patternName: String,
    onPatternChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    val resolvedPattern = remember(patternName) {
        if (patternName.isEmpty()) {
            null
        } else {
            StrumPatterns.ALL.find { it.name == patternName }
                ?: CustomStrumPatternRepository(context).getAll()
                    .find { it.pattern.name == patternName }?.pattern
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
    ) {
        if (resolvedPattern != null) {
            val patternDesc = stringResource(R.string.cd_strum_pattern, resolvedPattern.name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { contentDescription = patternDesc },
            ) {
                Text(
                    text = stringResource(R.string.songbook_strum_pattern) + ": ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = resolvedPattern.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = resolvedPattern.notation,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showPicker = true }) {
                    Text(
                        stringResource(R.string.songbook_strum_change),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TextButton(onClick = { onPatternChange("") }) {
                    Text(
                        stringResource(R.string.songbook_strum_remove),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.songbook_strum_pattern) + ": ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showPicker = true }) {
                    Text(stringResource(R.string.songbook_strum_select))
                }
            }
        }
    }

    if (showPicker) {
        StrumPatternPickerDialog(
            currentName = patternName,
            onSelect = { name ->
                onPatternChange(name)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun StrumPatternPickerDialog(
    currentName: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val customPatterns = remember {
        CustomStrumPatternRepository(context).getAll()
    }
    val allPatterns = remember(customPatterns) {
        val builtIn = StrumPatterns.ALL.map { it.name to it.notation }
        val custom = customPatterns.map { it.pattern.name to it.pattern.notation }
        builtIn + custom
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.songbook_strum_select),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            LazyColumn {
                items(allPatterns) { (name, notation) ->
                    val isSelected = name == currentName
                    val itemDesc = stringResource(R.string.cd_strum_pattern, name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(name) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .semantics {
                                contentDescription = itemDesc
                                if (isSelected) stateDescription = "selected"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = notation,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * Displays labels on a song with a "+" button to add more (used in the viewer).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelDisplayRow(
    labels: List<String>,
    allLabels: Set<String>,
    onLabelsChange: (List<String>) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { label ->
            val labelDesc = stringResource(R.string.cd_label, label)
            AssistChip(
                onClick = {},
                label = {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                modifier = Modifier
                    .height(28.dp)
                    .semantics { contentDescription = labelDesc },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
        AssistChip(
            onClick = { showAddDialog = true },
            label = {
                Text(
                    stringResource(R.string.songbook_add_label),
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_label),
                    modifier = Modifier.size(14.dp),
                )
            },
            modifier = Modifier.height(28.dp),
        )
    }

    if (showAddDialog) {
        AddLabelDialog(
            currentLabels = labels,
            allLabels = allLabels,
            onAdd = { newLabel ->
                onLabelsChange(labels + newLabel)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

/**
 * Label editor used inside the SheetEditor with InputChips and auto-suggest.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelEditorSection(
    labels: List<String>,
    allLabels: Set<String>,
    onLabelsChange: (List<String>) -> Unit,
) {
    var labelInput by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    val suggestions = remember(labelInput, allLabels, labels) {
        if (labelInput.isBlank()) {
            emptyList()
        } else {
            val query = labelInput.trim().lowercase()
            (allLabels - labels.toSet())
                .filter { it.lowercase().contains(query) }
                .take(5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.songbook_labels),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (labels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                labels.forEach { label ->
                    val removeDesc = stringResource(R.string.cd_remove_label, label)
                    InputChip(
                        selected = false,
                        onClick = { onLabelsChange(labels - label) },
                        label = {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = removeDesc,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }
        }

        Box {
            OutlinedTextField(
                value = labelInput,
                onValueChange = {
                    labelInput = it
                    showSuggestions = it.isNotBlank()
                },
                placeholder = { Text(stringResource(R.string.songbook_label_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (labelInput.isNotBlank()) {
                        IconButton(onClick = {
                            val trimmed = labelInput.trim()
                            if (trimmed.isNotEmpty() && trimmed !in labels) {
                                onLabelsChange(labels + trimmed)
                            }
                            labelInput = ""
                            showSuggestions = false
                        }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.cd_add_label),
                            )
                        }
                    }
                },
            )
            DropdownMenu(
                expanded = showSuggestions && suggestions.isNotEmpty(),
                onDismissRequest = { showSuggestions = false },
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            if (suggestion !in labels) {
                                onLabelsChange(labels + suggestion)
                            }
                            labelInput = ""
                            showSuggestions = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddLabelDialog(
    currentLabels: List<String>,
    allLabels: Set<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    val existingSuggestions = remember(allLabels, currentLabels) {
        (allLabels - currentLabels.toSet()).toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.songbook_add_label),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.songbook_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existingSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        existingSuggestions.forEach { label ->
                            AssistChip(
                                onClick = { onAdd(label) },
                                label = {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                },
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = input.trim()
                    if (trimmed.isNotEmpty() && trimmed !in currentLabels) {
                        onAdd(trimmed)
                    }
                },
                enabled = input.isNotBlank(),
            ) {
                Text(stringResource(R.string.songbook_add_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun SongStatsRow(sheet: ChordSheet) {
    val lastViewed = if (sheet.lastViewedAt > 0) {
        val fmt = android.text.format.DateUtils.getRelativeTimeSpanString(
            sheet.lastViewedAt,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS,
        )
        fmt.toString()
    } else {
        null
    }

    val totalMinutes = (sheet.totalViewTimeMs / 60_000).toInt()
    val totalTimeLabel = when {
        totalMinutes < 1 -> stringResource(R.string.stats_time_under_minute)
        totalMinutes == 1 -> stringResource(R.string.stats_time_one_minute)
        totalMinutes < 60 -> stringResource(R.string.stats_time_minutes, totalMinutes)
        else -> {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            stringResource(R.string.stats_time_hours_minutes, hours, mins)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.stats_views, sheet.viewCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (lastViewed != null) {
            Text(
                text = lastViewed,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = totalTimeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PerformanceModeView(
    displayContent: String,
    textStyle: TextStyle,
    onExit: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var autoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableStateOf(1f) }
    val programmaticScroll = remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(autoScrolling, scrollSpeed) {
        if (autoScrolling) {
            while (autoScrolling) {
                programmaticScroll.value = true
                try {
                    scrollState.animateScrollTo(
                        scrollState.value + scrollSpeed.toInt().coerceAtLeast(1),
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 16,
                            easing = androidx.compose.animation.core.LinearEasing,
                        ),
                    )
                } finally {
                    programmaticScroll.value = false
                }
                if (scrollState.value >= scrollState.maxValue) {
                    autoScrolling = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { showControls = !showControls },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            displayContent.lines().forEach { line ->
                if (line.isBlank()) {
                    Spacer(modifier = Modifier.height(textStyle.fontSize.value.dp))
                } else {
                    Text(
                        text = line,
                        style = textStyle,
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalButton(
                    onClick = { autoScrolling = !autoScrolling },
                ) {
                    Icon(
                        imageVector = if (autoScrolling) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (autoScrolling) "Pause" else "Scroll")
                }
                if (autoScrolling) {
                    FilledTonalButton(
                        onClick = { scrollSpeed = (scrollSpeed - 0.5f).coerceAtLeast(0.5f) },
                    ) { Text("\u2212") }
                    Text("${scrollSpeed}x", style = MaterialTheme.typography.labelMedium)
                    FilledTonalButton(
                        onClick = { scrollSpeed = (scrollSpeed + 0.5f).coerceAtMost(5f) },
                    ) { Text("+") }
                }
                IconButton(onClick = onExit) {
                    Icon(Icons.Filled.FullscreenExit, contentDescription = stringResource(R.string.performance_mode_exit))
                }
            }
        }
    }
}

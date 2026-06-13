package com.baijum.ukufretboard.ui.songbook

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordColorOption
import com.baijum.ukufretboard.data.ChordDisplayStyle
import com.baijum.ukufretboard.data.ChordProParser
import com.baijum.ukufretboard.data.ChordSheet
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
    chordDisplayStyle: ChordDisplayStyle = ChordDisplayStyle.ABOVE,
    chordColor: ChordColorOption = ChordColorOption.THEME,
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
                chordDisplayStyle = chordDisplayStyle,
                chordColor = chordColor,
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
                            stringResource(R.string.batch_selected, selectedIds.size),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = onSelectAll) { Text(stringResource(R.string.batch_select_all)) }
                            TextButton(onClick = onClearSelection) { Text(stringResource(R.string.batch_cancel)) }
                            TextButton(onClick = onDeleteSelected) {
                                Text(stringResource(R.string.batch_delete), color = MaterialTheme.colorScheme.error)
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

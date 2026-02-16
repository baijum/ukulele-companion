package com.baijum.ukufretboard.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.FavoriteFolder
import com.baijum.ukufretboard.data.FavoriteVoicing
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.FavoritesViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

private const val FILTER_ALL = "__all__"

/**
 * Tab displaying the user's saved favorite chord voicings, organized by folders.
 *
 * Folder chips at the top filter the grid: All and user-created folders.
 * Supports drag-and-drop reordering (via drag handle) when viewing a specific folder.
 */
@Composable
fun FavoritesTab(
    viewModel: FavoritesViewModel,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onShareVoicing: ((ChordVoicing, String) -> Unit)? = null,
    leftHanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val favorites by viewModel.favorites.collectAsState()
    val folders by viewModel.folders.collectAsState()
    var selectedFilter by remember { mutableStateOf(FILTER_ALL) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf<FavoriteVoicing?>(null) }
    var renamingFolder by remember { mutableStateOf<FavoriteFolder?>(null) }
    var deletingFolder by remember { mutableStateOf<FavoriteFolder?>(null) }

    if (favorites.isEmpty()) {
        // Empty state
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.favorites_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.favorites_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // Folder chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == FILTER_ALL,
                        onClick = { selectedFilter = FILTER_ALL },
                        label = { Text(stringResource(R.string.favorites_all_count, favorites.size)) },
                    )
                }
                items(folders) { folder ->
                    val count = favorites.count { folder.id in it.folderIds }
                    FilterChip(
                        selected = selectedFilter == folder.id,
                        onClick = { selectedFilter = folder.id },
                        label = { Text("${folder.name} ($count)") },
                        trailingIcon = {
                            Row {
                                IconButton(
                                    onClick = { renamingFolder = folder },
                                    modifier = Modifier.size(18.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.cd_rename_folder),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { deletingFolder = folder },
                                    modifier = Modifier.size(18.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_folder),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        },
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showCreateFolderDialog = true },
                        label = { Text("+") },
                    )
                }
            }

            // Filtered & ordered list
            val isSpecificFolder = selectedFilter != FILTER_ALL
            val filtered: List<FavoriteVoicing> = if (isSpecificFolder) {
                viewModel.getOrderedVoicings(selectedFilter)
            } else {
                favorites
            }

            if (isSpecificFolder) {
                // ── Reorderable grid for specific folders ──
                ReorderableFavoritesGrid(
                    voicings = filtered,
                    folderId = selectedFilter,
                    viewModel = viewModel,
                    onVoicingSelected = onVoicingSelected,
                    onShareVoicing = onShareVoicing,
                    leftHanded = leftHanded,
                    onShowFolderSheet = { showFolderSheet = it },
                )
            } else {
                // ── Static grid for "All" and "Unfiled" ──
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.key }) { favorite ->
                        FavoriteVoicingCard(
                            favorite = favorite,
                            viewModel = viewModel,
                            onVoicingSelected = onVoicingSelected,
                            onShareVoicing = onShareVoicing,
                            leftHanded = leftHanded,
                            onShowFolderSheet = { showFolderSheet = it },
                        )
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────

    // Create folder dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(stringResource(R.string.favorites_new_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.favorites_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName.trim())
                            showCreateFolderDialog = false
                        }
                    },
                    enabled = folderName.isNotBlank(),
                ) { Text(stringResource(R.string.dialog_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }

    // Rename folder dialog
    renamingFolder?.let { folder ->
        var newName by remember { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { renamingFolder = null },
            title = { Text(stringResource(R.string.favorites_rename_folder)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.favorites_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameFolder(folder.id, newName.trim())
                            renamingFolder = null
                        }
                    },
                    enabled = newName.isNotBlank(),
                ) { Text(stringResource(R.string.dialog_rename)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingFolder = null }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }

    // Delete folder confirmation dialog
    deletingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text(stringResource(R.string.favorites_delete_folder)) },
            text = {
                Text(stringResource(R.string.favorites_delete_folder_msg, folder.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
                        if (selectedFilter == folder.id) selectedFilter = FILTER_ALL
                        deletingFolder = null
                    },
                ) {
                    Text(stringResource(R.string.dialog_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFolder = null }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }

    // Folder management bottom sheet
    showFolderSheet?.let { favorite ->
        FavoriteFolderSheet(
            folders = folders,
            selectedFolderIds = favorite.folderIds,
            isAlreadyFavorited = true,
            onSave = { selectedIds ->
                viewModel.setFolders(favorite, selectedIds)
                showFolderSheet = null
            },
            onRemove = {
                viewModel.removeFavorite(favorite)
                showFolderSheet = null
            },
            onCreateFolder = { name ->
                viewModel.createFolder(name)
            },
            onDismiss = { showFolderSheet = null },
        )
    }
}

/**
 * Reorderable grid that supports drag-and-drop reordering via a drag handle icon.
 * Uses the sh.calvin.reorderable library for smooth item displacement animations.
 */
@Composable
private fun ReorderableFavoritesGrid(
    voicings: List<FavoriteVoicing>,
    folderId: String,
    viewModel: FavoritesViewModel,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onShareVoicing: ((ChordVoicing, String) -> Unit)?,
    leftHanded: Boolean,
    onShowFolderSheet: (FavoriteVoicing) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // Local mutable copy for instant visual updates during drag
    val localList = remember(voicings) { voicings.toMutableStateList() }

    val lazyGridState = rememberLazyGridState()
    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        localList.apply {
            add(to.index, removeAt(from.index))
        }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = lazyGridState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(localList, key = { _, fav -> fav.key }) { _, favorite ->
            ReorderableItem(reorderableState, key = favorite.key) { isDragging ->
                val elevation by animateDpAsState(
                    if (isDragging) 8.dp else 0.dp,
                    label = "dragElevation",
                )

                Surface(
                    shadowElevation = elevation,
                    tonalElevation = if (isDragging) 2.dp else 0.dp,
                ) {
                    FavoriteVoicingCard(
                        favorite = favorite,
                        viewModel = viewModel,
                        onVoicingSelected = onVoicingSelected,
                        onShareVoicing = onShareVoicing,
                        leftHanded = leftHanded,
                        onShowFolderSheet = onShowFolderSheet,
                        dragHandle = {
                            IconButton(
                                modifier = Modifier
                                    .size(28.dp)
                                    .draggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            viewModel.reorderInFolder(folderId, localList.map { it.key })
                                        },
                                    ),
                                onClick = {},
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = stringResource(R.string.cd_reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * A single favorite voicing card with chord diagram and action icons.
 *
 * @param dragHandle Optional composable for a drag handle icon (shown when reorderable).
 */
@Composable
private fun FavoriteVoicingCard(
    favorite: FavoriteVoicing,
    viewModel: FavoritesViewModel,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onShareVoicing: ((ChordVoicing, String) -> Unit)?,
    leftHanded: Boolean,
    onShowFolderSheet: (FavoriteVoicing) -> Unit,
    dragHandle: @Composable (() -> Unit)? = null,
) {
    val voicing = viewModel.toChordVoicing(favorite)
    val chordName = Notes.pitchClassToName(favorite.rootPitchClass) + favorite.chordSymbol

    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = chordName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            VerticalChordDiagram(
                voicing = voicing,
                onClick = { onVoicingSelected(voicing) },
                onLongClick = onShareVoicing?.let { share ->
                    { share(voicing, chordName) }
                },
                leftHanded = leftHanded,
            )
        }
        // Drag handle at top-start (only in reorderable mode)
        if (dragHandle != null) {
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                dragHandle()
            }
        }
        // Actions: manage folders + remove
        Row(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(
                onClick = { onShowFolderSheet(favorite) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = stringResource(R.string.cd_manage_folders),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(
                onClick = { viewModel.removeFavorite(favorite) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.cd_remove_favorites),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

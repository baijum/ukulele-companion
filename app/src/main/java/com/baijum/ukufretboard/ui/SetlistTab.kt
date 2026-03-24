package com.baijum.ukufretboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.Setlist
import com.baijum.ukufretboard.viewmodel.SetlistViewModel
import com.baijum.ukufretboard.viewmodel.SongbookViewModel

@Composable
fun SetlistTab(
    setlistViewModel: SetlistViewModel,
    songbookViewModel: SongbookViewModel,
    modifier: Modifier = Modifier,
) {
    val setlists by setlistViewModel.setlists.collectAsState()
    val currentSetlist by setlistViewModel.currentSetlist.collectAsState()
    val allSongs by songbookViewModel.sheets.collectAsState()

    if (currentSetlist != null) {
        SetlistDetailView(
            setlist = currentSetlist!!,
            allSongs = allSongs,
            onBack = { setlistViewModel.close() },
            onAddSong = { setlistViewModel.addSong(currentSetlist!!.id, it) },
            onRemoveSong = { setlistViewModel.removeSong(currentSetlist!!.id, it) },
            onMoveSong = { from, to -> setlistViewModel.moveSong(currentSetlist!!.id, from, to) },
        )
    } else {
        SetlistListView(
            setlists = setlists,
            onSelect = { setlistViewModel.open(it) },
            onCreate = { setlistViewModel.create(it) },
            onDelete = { setlistViewModel.delete(it) },
            modifier = modifier,
        )
    }
}

@Composable
private fun SetlistListView(
    setlists: List<Setlist>,
    onSelect: (Setlist) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (setlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No setlists yet",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Create a setlist to organize songs for gigs or practice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(setlists) { setlist ->
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(setlist) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = setlist.name,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "${setlist.songIds.size} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete setlist")
                            }
                        }
                    }
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Setlist?") },
                            text = { Text("Delete \"${setlist.name}\"? Songs won't be deleted.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    onDelete(setlist.id)
                                    showDeleteDialog = false
                                }) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                            },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create setlist")
        }

        if (showCreateDialog) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Setlist") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Setlist name") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreate(name.trim())
                                showCreateDialog = false
                            }
                        },
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun SetlistDetailView(
    setlist: Setlist,
    allSongs: List<ChordSheet>,
    onBack: () -> Unit,
    onAddSong: (String) -> Unit,
    onRemoveSong: (String) -> Unit,
    onMoveSong: (Int, Int) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val songMap = remember(allSongs) { allSongs.associateBy { it.id } }
    val setlistSongs = setlist.songIds.mapNotNull { songMap[it] }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = setlist.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add song to setlist")
            }
        }

        if (setlistSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No songs in this setlist. Tap + to add songs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                itemsIndexed(setlistSongs) { index, song ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title.ifEmpty { "Untitled" },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (song.artist.isNotEmpty()) {
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (index > 0) {
                                IconButton(onClick = { onMoveSong(index, index - 1) }) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                                }
                            }
                            if (index < setlistSongs.size - 1) {
                                IconButton(onClick = { onMoveSong(index, index + 1) }) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                                }
                            }
                            IconButton(onClick = { onRemoveSong(song.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove from setlist")
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            val availableSongs = allSongs.filter { it.id !in setlist.songIds }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Song") },
                text = {
                    if (availableSongs.isEmpty()) {
                        Text("All songs are already in this setlist.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(availableSongs) { song ->
                                Text(
                                    text = song.title.ifEmpty { "Untitled" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddSong(song.id)
                                            showAddDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Close") }
                },
            )
        }
    }
}

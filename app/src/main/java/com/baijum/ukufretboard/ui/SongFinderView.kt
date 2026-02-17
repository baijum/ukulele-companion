package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.SongChordDatabase
import com.baijum.ukufretboard.viewmodel.FavoritesViewModel
import com.baijum.ukufretboard.viewmodel.KnownChordsViewModel

/**
 * Song Finder screen — suggests songs based on chords the user knows.
 *
 * Uses the user's favorites and explicit "known chords" to match against
 * a built-in database of popular ukulele songs.
 *
 * @param favoritesViewModel Provides the user's saved chord voicings.
 * @param knownChordsViewModel Provides explicitly marked known chords.
 * @param onChordTapped Callback when a chord name is tapped (navigate to library).
 * @param onAddToSongbook Callback when the user wants to create a chord sheet from a song.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongFinderView(
    favoritesViewModel: FavoritesViewModel,
    knownChordsViewModel: KnownChordsViewModel,
    onChordTapped: (String) -> Unit = {},
    onAddToSongbook: (SongChordDatabase.SongEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val favorites by favoritesViewModel.favorites.collectAsState()
    val explicitKnownChords by knownChordsViewModel.knownChords.collectAsState()
    var showAlmostPlayable by remember { mutableStateOf(true) }
    var showAddChordDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf<SongChordDatabase.Difficulty?>(null) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val allGenres = remember {
        SongChordDatabase.SONGS.map { it.genre }.distinct().sorted()
    }

    // Derive known chord names from favorites
    val favoriteDerivedChords by remember(favorites) {
        derivedStateOf {
            favorites.map { fav ->
                Notes.pitchClassToName(fav.rootPitchClass) + fav.chordSymbol
            }.toSet()
        }
    }

    // Merge both sources
    val knownChords by remember(favoriteDerivedChords, explicitKnownChords) {
        derivedStateOf { favoriteDerivedChords + explicitKnownChords }
    }

    val playableSongs by remember(knownChords) {
        derivedStateOf { SongChordDatabase.findPlayable(knownChords) }
    }

    val almostPlayableSongs by remember(knownChords) {
        derivedStateOf { SongChordDatabase.findAlmostPlayable(knownChords, maxMissing = 2) }
    }

    val filteredPlayable by remember(playableSongs, searchQuery, selectedDifficulty, selectedGenre) {
        derivedStateOf {
            playableSongs.filter { song ->
                (searchQuery.isBlank() || song.title.contains(searchQuery, ignoreCase = true)
                    || song.artist.contains(searchQuery, ignoreCase = true))
                    && (selectedDifficulty == null || song.difficulty == selectedDifficulty)
                    && (selectedGenre == null || song.genre == selectedGenre)
            }
        }
    }

    val filteredAlmostPlayable by remember(almostPlayableSongs, searchQuery, selectedDifficulty, selectedGenre) {
        derivedStateOf {
            almostPlayableSongs.filter { (song, _) ->
                (searchQuery.isBlank() || song.title.contains(searchQuery, ignoreCase = true)
                    || song.artist.contains(searchQuery, ignoreCase = true))
                    && (selectedDifficulty == null || song.difficulty == selectedDifficulty)
                    && (selectedGenre == null || song.genre == selectedGenre)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header
        item {
            Text(
                text = stringResource(R.string.song_finder_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.song_finder_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.song_finder_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.song_finder_filter_all))
                        }
                    }
                },
            )
        }

        // Difficulty filter
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedDifficulty == null,
                    onClick = { selectedDifficulty = null },
                    label = { Text(stringResource(R.string.song_finder_filter_all)) },
                )
                SongChordDatabase.Difficulty.entries.forEach { difficulty ->
                    FilterChip(
                        selected = selectedDifficulty == difficulty,
                        onClick = {
                            selectedDifficulty = if (selectedDifficulty == difficulty) null else difficulty
                        },
                        label = { Text(difficulty.label) },
                    )
                }
            }
        }

        // Genre filter
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedGenre == null,
                    onClick = { selectedGenre = null },
                    label = { Text(stringResource(R.string.song_finder_filter_all)) },
                )
                allGenres.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = {
                            selectedGenre = if (selectedGenre == genre) null else genre
                        },
                        label = { Text(genre) },
                    )
                }
            }
        }

        // Known chords summary
        item {
            Spacer(modifier = Modifier.height(4.dp))
            if (knownChords.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.song_finder_no_chords_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.song_finder_no_chords_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.song_finder_your_chords, knownChords.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    knownChords.sorted().forEach { chord ->
                        val viewChordDesc = stringResource(R.string.cd_view_chord, chord)
                        AssistChip(
                            onClick = { onChordTapped(chord) },
                            label = {
                                Text(
                                    chord,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription = viewChordDesc
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        // Manage known chords section
        if (explicitKnownChords.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.song_finder_explicit_chords, explicitKnownChords.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    explicitKnownChords.sorted().forEach { chord ->
                        val removeDesc = stringResource(R.string.cd_remove_known_chord, chord)
                        AssistChip(
                            onClick = { knownChordsViewModel.removeChord(chord) },
                            label = {
                                Text(
                                    chord,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = removeDesc,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription = removeDesc
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        // Add chord button
        item {
            AssistChip(
                onClick = { showAddChordDialog = true },
                label = { Text(stringResource(R.string.song_finder_add_chord)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }

        // Playable songs section
        if (filteredPlayable.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.song_finder_songs_can_play, filteredPlayable.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                )
            }

            items(filteredPlayable) { song ->
                SongCard(
                    song = song,
                    knownChords = knownChords,
                    onChordTapped = onChordTapped,
                    onAddToSongbook = onAddToSongbook,
                )
            }
        }

        // Almost playable songs section
        if (filteredAlmostPlayable.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.song_finder_learn_more, filteredAlmostPlayable.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
                    )
                    val showingDesc = stringResource(R.string.cd_showing_songs)
                    val hiddenDesc = stringResource(R.string.cd_songs_hidden)
                    FilterChip(
                        selected = showAlmostPlayable,
                        onClick = { showAlmostPlayable = !showAlmostPlayable },
                        label = { Text(if (showAlmostPlayable) stringResource(R.string.song_finder_hide) else stringResource(R.string.song_finder_show)) },
                        modifier = Modifier.semantics {
                            stateDescription = if (showAlmostPlayable) showingDesc else hiddenDesc
                        },
                    )
                }
            }

            if (showAlmostPlayable) {
                items(filteredAlmostPlayable) { (song, missing) ->
                    SongCard(
                        song = song,
                        knownChords = knownChords,
                        missingChords = missing,
                        onChordTapped = onChordTapped,
                        onAddToSongbook = onAddToSongbook,
                    )
                }
            }
        }

        // Empty state
        if (knownChords.isNotEmpty() && filteredPlayable.isEmpty() && filteredAlmostPlayable.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.song_finder_keep_learning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showAddChordDialog) {
        AddChordDialog(
            existingChords = knownChords,
            onAdd = { chordName ->
                knownChordsViewModel.addChord(chordName)
                showAddChordDialog = false
            },
            onDismiss = { showAddChordDialog = false },
        )
    }
}

/**
 * Dialog for adding a chord to the known chords set.
 *
 * Presents the 12 root notes and common chord types for the user to pick.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddChordDialog(
    existingChords: Set<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val rootNotes = remember { Notes.NOTE_NAMES_STANDARD.toList() }
    val chordTypes = remember {
        listOf("" to "Major", "m" to "Minor", "7" to "7th", "m7" to "m7",
            "maj7" to "Maj7", "dim" to "Dim", "aug" to "Aug", "sus2" to "Sus2",
            "sus4" to "Sus4", "6" to "6th", "m6" to "m6", "9" to "9th")
    }

    var selectedRoot by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<Pair<String, String>?>(null) }

    val chordName = if (selectedRoot != null && selectedType != null) {
        selectedRoot!! + selectedType!!.first
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.song_finder_add_chord),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.song_finder_select_root),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rootNotes.forEach { note ->
                        FilterChip(
                            selected = selectedRoot == note,
                            onClick = { selectedRoot = note },
                            label = { Text(note) },
                        )
                    }
                }

                Text(
                    stringResource(R.string.song_finder_select_type),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    chordTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.second) },
                        )
                    }
                }

                if (chordName != null) {
                    val alreadyKnown = chordName in existingChords
                    Text(
                        text = if (alreadyKnown) "$chordName (already known)" else chordName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (alreadyKnown) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { chordName?.let(onAdd) },
                enabled = chordName != null && chordName !in existingChords,
            ) {
                Text(stringResource(R.string.song_finder_add_chord))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.song_finder_hide))
            }
        },
    )
}

/**
 * Card displaying a song with its chord requirements.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SongCard(
    song: SongChordDatabase.SongEntry,
    knownChords: Set<String>,
    missingChords: Set<String> = emptySet(),
    onChordTapped: (String) -> Unit = {},
    onAddToSongbook: (SongChordDatabase.SongEntry) -> Unit = {},
) {
    val cardDescription = stringResource(
        R.string.cd_song_card,
        song.title,
        song.artist,
        song.difficulty.label,
        song.genre,
        song.chords.size,
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardDescription },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (missingChords.isEmpty()) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = song.difficulty.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = song.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = { onAddToSongbook(song) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Filled.Create,
                            contentDescription = stringResource(R.string.cd_add_to_songbook),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chord chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                song.chords.forEach { chord ->
                    val isMissing = chord in missingChords
                    val chordDesc = if (isMissing) {
                        stringResource(R.string.cd_chord_to_learn, chord)
                    } else {
                        stringResource(R.string.cd_chord_known, chord)
                    }
                    AssistChip(
                        onClick = { onChordTapped(chord) },
                        label = {
                            Text(
                                chord,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isMissing) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.semantics {
                            contentDescription = chordDesc
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when {
                                isMissing -> MaterialTheme.colorScheme.errorContainer
                                chord in knownChords -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            labelColor = when {
                                isMissing -> MaterialTheme.colorScheme.onErrorContainer
                                chord in knownChords -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    )
                }
            }

            if (missingChords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.song_finder_learn_prefix) + missingChords.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

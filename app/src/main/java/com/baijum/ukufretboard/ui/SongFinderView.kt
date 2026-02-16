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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.SongChordDatabase
import com.baijum.ukufretboard.viewmodel.FavoritesViewModel

/**
 * Song Finder screen — suggests songs based on chords the user knows.
 *
 * Uses the user's favorites as "known chords" and matches against
 * a built-in database of popular ukulele songs.
 *
 * @param favoritesViewModel Provides the user's saved chord voicings.
 * @param onChordTapped Callback when a chord name is tapped (navigate to library).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongFinderView(
    favoritesViewModel: FavoritesViewModel,
    onChordTapped: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val favorites by favoritesViewModel.favorites.collectAsState()
    var showAlmostPlayable by remember { mutableStateOf(true) }

    // Derive known chord names from favorites
    val knownChords by remember(favorites) {
        derivedStateOf {
            favorites.map { fav ->
                Notes.pitchClassToName(fav.rootPitchClass) + fav.chordSymbol
            }.toSet()
        }
    }

    val playableSongs by remember(knownChords) {
        derivedStateOf { SongChordDatabase.findPlayable(knownChords) }
    }

    val almostPlayableSongs by remember(knownChords) {
        derivedStateOf { SongChordDatabase.findAlmostPlayable(knownChords, maxMissing = 2) }
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
            )
            Text(
                text = stringResource(R.string.song_finder_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    knownChords.sorted().forEach { chord ->
                        AssistChip(
                            onClick = { onChordTapped(chord) },
                            label = {
                                Text(
                                    chord,
                                    style = MaterialTheme.typography.labelSmall,
                                )
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

        // Playable songs section
        if (playableSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.song_finder_songs_can_play, playableSongs.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            items(playableSongs) { song ->
                SongCard(
                    song = song,
                    knownChords = knownChords,
                    onChordTapped = onChordTapped,
                )
            }
        }

        // Almost playable songs section
        if (almostPlayableSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.song_finder_learn_more, almostPlayableSongs.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    FilterChip(
                        selected = showAlmostPlayable,
                        onClick = { showAlmostPlayable = !showAlmostPlayable },
                        label = { Text(if (showAlmostPlayable) stringResource(R.string.song_finder_hide) else stringResource(R.string.song_finder_show)) },
                    )
                }
            }

            if (showAlmostPlayable) {
                items(almostPlayableSongs) { (song, missing) ->
                    SongCard(
                        song = song,
                        knownChords = knownChords,
                        missingChords = missing,
                        onChordTapped = onChordTapped,
                    )
                }
            }
        }

        // Empty state
        if (knownChords.isNotEmpty() && playableSongs.isEmpty() && almostPlayableSongs.isEmpty()) {
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                song.difficulty.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                song.genre,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
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
                    AssistChip(
                        onClick = { onChordTapped(chord) },
                        label = {
                            Text(
                                chord,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isMissing) FontWeight.Bold else FontWeight.Normal,
                            )
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

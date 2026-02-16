package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.audio.MetronomeEngine
import com.baijum.ukufretboard.data.MelodyNote
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.Notes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Melody Notepad — a simple note sequencer for composing melodies.
 *
 * Users tap notes from a 12-note palette to add them to a sequence.
 * Notes can have different durations. The sequence can be played back
 * as single-note sounds at the set BPM.
 *
 * @param onPlayNote Callback to play a single note (pitch class).
 */
@Composable
fun MelodyNotepadView(
    onPlayNote: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val notes = remember { mutableStateListOf<MelodyNote>() }
    var selectedDuration by remember { mutableStateOf(NoteDuration.QUARTER) }
    var bpm by remember { mutableFloatStateOf(120f) }
    var isPlaying by remember { mutableStateOf(false) }
    var playingIndex by remember { mutableIntStateOf(-1) }
    var selectedNoteIndex by remember { mutableIntStateOf(-1) }

    val noteNames = Notes.NOTE_NAMES_STANDARD

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Melody Notepad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Tap notes to compose a melody. Press play to hear it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Note sequence display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Sequence (${notes.size} notes)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (notes.isEmpty()) {
                    Text(
                        text = "Tap a note below to start composing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        notes.forEachIndexed { index, note ->
                            val isCurrent = index == playingIndex
                            val isSelected = index == selectedNoteIndex
                            val noteName = if (note.pitchClass != null) {
                                noteNames[note.pitchClass]
                            } else {
                                "\u2014" // rest
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedNoteIndex = if (isSelected) -1 else index }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .padding(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = when {
                                                isCurrent -> MaterialTheme.colorScheme.primary
                                                note.pitchClass == null -> MaterialTheme.colorScheme.outlineVariant
                                                else -> MaterialTheme.colorScheme.secondaryContainer
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = noteName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                    )
                                }
                                Text(
                                    text = when (note.duration) {
                                        NoteDuration.WHOLE -> "W"
                                        NoteDuration.HALF -> "H"
                                        NoteDuration.QUARTER -> "Q"
                                        NoteDuration.EIGHTH -> "8"
                                        NoteDuration.SIXTEENTH -> "16"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Action buttons for selected note
                if (selectedNoteIndex in notes.indices) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                notes.removeAt(selectedNoteIndex)
                                selectedNoteIndex = -1
                            },
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete note", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Duration selector
        Text(
            text = "Note Duration",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NoteDuration.entries.forEach { dur ->
                FilterChip(
                    selected = selectedDuration == dur,
                    onClick = { selectedDuration = dur },
                    label = { Text(dur.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Note palette (12 notes + rest)
        Text(
            text = "Tap to Add Note",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            noteNames.forEachIndexed { pc, name ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                        )
                        .clickable {
                            notes.add(MelodyNote(pitchClass = pc, duration = selectedDuration))
                            onPlayNote?.invoke(pc)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // Rest button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable {
                        notes.add(MelodyNote(pitchClass = null, duration = selectedDuration))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Rest",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BPM and playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "BPM: ${bpm.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(64.dp),
            )
            Slider(
                value = bpm,
                onValueChange = { bpm = it },
                valueRange = 40f..220f,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Play button
            OutlinedButton(
                onClick = {
                    if (isPlaying || notes.isEmpty()) return@OutlinedButton
                    isPlaying = true
                    scope.launch {
                        notes.forEachIndexed { index, note ->
                            if (!isPlaying) return@launch
                            playingIndex = index
                            if (note.pitchClass != null) {
                                onPlayNote?.invoke(note.pitchClass)
                            }
                            val durationMs = (note.duration.beats * 60_000f / bpm).toLong()
                            delay(durationMs)
                        }
                        playingIndex = -1
                        isPlaying = false
                    }
                },
                enabled = notes.isNotEmpty() && !isPlaying,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play melody", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play")
            }

            // Stop button
            if (isPlaying) {
                OutlinedButton(
                    onClick = {
                        isPlaying = false
                        playingIndex = -1
                    },
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Stop playing", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }

            // Clear button
            OutlinedButton(
                onClick = {
                    notes.clear()
                    selectedNoteIndex = -1
                    playingIndex = -1
                },
                enabled = notes.isNotEmpty() && !isPlaying,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear all notes", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }
        }
    }
}

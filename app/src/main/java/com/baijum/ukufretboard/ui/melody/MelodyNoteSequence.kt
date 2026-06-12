package com.baijum.ukufretboard.ui.melody

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.MelodyNote
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.viewmodel.MelodyUiState

@Composable
internal fun NoteSequenceCard(
    state: MelodyUiState,
    onSelectNote: (Int) -> Unit,
    onDeleteNote: () -> Unit,
) {
    val noteNames = Notes.NOTE_NAMES_STANDARD

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.melody_sequence, state.notes.size),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.notes.isEmpty()) {
                Text(
                    text = stringResource(R.string.melody_placeholder),
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
                    state.notes.forEachIndexed { index, note ->
                        NoteBlock(
                            note = note,
                            index = index,
                            isCurrent = index == state.playingIndex,
                            isSelected = index == state.selectedNoteIndex,
                            noteNames = noteNames,
                            onSelect = { onSelectNote(index) },
                        )
                    }
                }
            }

            if (state.selectedNoteIndex in state.notes.indices) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDeleteNote) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.cd_delete_note),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.dialog_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteBlock(
    note: MelodyNote,
    index: Int,
    isCurrent: Boolean,
    isSelected: Boolean,
    noteNames: List<String>,
    onSelect: () -> Unit,
) {
    val pc = note.pitchClass
    val noteName = if (pc != null) {
        noteNames.getOrElse(pc) { "?" }
    } else {
        "\u2014"
    }
    val displayText = if (pc != null) {
        "$noteName${note.octave}"
    } else {
        "\u2014"
    }

    val durationLabel = when (note.duration) {
        NoteDuration.WHOLE -> stringResource(R.string.melody_duration_whole)
        NoteDuration.HALF -> stringResource(R.string.melody_duration_half)
        NoteDuration.QUARTER -> stringResource(R.string.melody_duration_quarter)
        NoteDuration.EIGHTH -> stringResource(R.string.melody_duration_eighth)
        NoteDuration.SIXTEENTH -> stringResource(R.string.melody_duration_sixteenth)
    }

    val noteDescription = if (note.pitchClass != null) {
        stringResource(R.string.cd_note_position, "$noteName${note.octave}", durationLabel, index + 1)
    } else {
        stringResource(R.string.cd_rest_position, durationLabel, index + 1)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClickLabel = noteDescription) { onSelect() }
            .semantics {
                contentDescription = noteDescription
                role = Role.Button
                if (isSelected) stateDescription = "selected"
            }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp),
            )
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
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
                text = displayText,
                style = MaterialTheme.typography.labelSmall,
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

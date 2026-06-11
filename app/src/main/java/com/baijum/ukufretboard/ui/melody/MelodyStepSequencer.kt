package com.baijum.ukufretboard.ui.melody

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.viewmodel.MelodyUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StepSequencerGrid(
    state: MelodyUiState,
    onSetStep: (Int, Int) -> Unit,
    onClearStep: (Int) -> Unit,
    onExpandSteps: () -> Unit,
) {
    val gridLabel = "Melody step sequencer, ${state.steps.size} steps"

    Column(modifier = Modifier.semantics { contentDescription = gridLabel }) {
        Text(
            text = "Step Sequencer (${state.steps.size} steps)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.steps.forEachIndexed { index, note ->
                val isPlaying = state.isPlaying && state.playingIndex == index
                val noteName = note?.pitchClass?.let { Notes.pitchClassToName(it) }
                val stepDesc = if (noteName != null) {
                    "Step ${index + 1}: $noteName${note.octave} ${note.duration.name.lowercase()}"
                } else {
                    "Step ${index + 1}: empty"
                }

                var showPitchPicker by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = when {
                                isPlaying -> MaterialTheme.colorScheme.primaryContainer
                                note != null -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = if (isPlaying) 2.dp else 1.dp,
                            color = if (isPlaying) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .combinedClickable(
                            onClick = { showPitchPicker = true },
                            onLongClick = { onClearStep(index) },
                        )
                        .semantics {
                            contentDescription = stepDesc
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (noteName != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = noteName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${note!!.octave}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (showPitchPicker) {
                    StepPitchPickerPopup(
                        onSelect = { pc ->
                            onSetStep(index, pc)
                            showPitchPicker = false
                        },
                        onDismiss = { showPitchPicker = false },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val expandLabel = if (state.steps.size >= MelodyUiState.STEP_COUNT_16) {
                "8 steps"
            } else {
                "16 steps"
            }
            OutlinedButton(onClick = onExpandSteps) {
                Text(expandLabel)
            }
        }
    }
}

@Composable
private fun StepPitchPickerPopup(
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
    ) {
        for (pc in 0 until Notes.PITCH_CLASS_COUNT) {
            val name = Notes.pitchClassToName(pc)
            DropdownMenuItem(
                text = { Text(name) },
                onClick = { onSelect(pc) },
            )
        }
    }
}

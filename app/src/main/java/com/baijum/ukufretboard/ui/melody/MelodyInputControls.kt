package com.baijum.ukufretboard.ui.melody

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.ui.LocalReduceMotion
import com.baijum.ukufretboard.ui.RequireMicPermission
import com.baijum.ukufretboard.ui.localizedLabel
import com.baijum.ukufretboard.viewmodel.MelodyInputMode
import com.baijum.ukufretboard.viewmodel.MelodyUiState

@Composable
internal fun DurationSelector(
    selectedDuration: NoteDuration,
    onSelectDuration: (NoteDuration) -> Unit,
) {
    Text(
        text = stringResource(R.string.melody_note_duration),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NoteDuration.entries.forEach { dur ->
            val label =
                when (dur) {
                    NoteDuration.WHOLE -> stringResource(R.string.melody_whole)
                    NoteDuration.HALF -> stringResource(R.string.melody_half)
                    NoteDuration.QUARTER -> stringResource(R.string.melody_quarter)
                    NoteDuration.EIGHTH -> stringResource(R.string.melody_eighth)
                    NoteDuration.SIXTEENTH -> stringResource(R.string.melody_sixteenth)
                }
            val durationChipDesc = stringResource(R.string.cd_note_duration_selected, dur.localizedLabel())
            FilterChip(
                selected = selectedDuration == dur,
                onClick = { onSelectDuration(dur) },
                label = { Text(label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier =
                    Modifier.semantics {
                        contentDescription = durationChipDesc
                        if (selectedDuration == dur) stateDescription = "selected"
                    },
            )
        }
    }
}

@Composable
internal fun InputModeSection(
    state: MelodyUiState,
    onSetInputMode: (MelodyInputMode) -> Unit,
    onAddNote: (Int) -> Unit,
    onAddRest: () -> Unit,
    onOctaveUp: () -> Unit,
    onOctaveDown: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    Text(
        text = stringResource(R.string.melody_tap_to_add),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = state.inputMode == MelodyInputMode.TAP,
            onClick = { onSetInputMode(MelodyInputMode.TAP) },
            label = { Text(stringResource(R.string.melody_input_tap)) },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
        )
        FilterChip(
            selected = state.inputMode == MelodyInputMode.RECORD,
            onClick = { onSetInputMode(MelodyInputMode.RECORD) },
            label = { Text(stringResource(R.string.melody_input_record)) },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
        )
    }

    when (state.inputMode) {
        MelodyInputMode.TAP -> {
            TapInputContent(
                state = state,
                onAddNote = onAddNote,
                onAddRest = onAddRest,
                onOctaveUp = onOctaveUp,
                onOctaveDown = onOctaveDown,
            )
        }

        MelodyInputMode.RECORD -> {
            RequireMicPermission {
                RecordInputContent(
                    state = state,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                )
            }
        }
    }
}

@Composable
private fun TapInputContent(
    state: MelodyUiState,
    onAddNote: (Int) -> Unit,
    onAddRest: () -> Unit,
    onOctaveUp: () -> Unit,
    onOctaveDown: () -> Unit,
) {
    val noteNames = Notes.NOTE_NAMES_STANDARD

    Row(
        modifier = Modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.melody_octave),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val decreaseOctaveDesc = stringResource(R.string.cd_decrease_octave)
        IconButton(
            onClick = onOctaveDown,
            enabled = state.currentOctave > 3,
            modifier =
                Modifier
                    .size(32.dp)
                    .semantics {
                        contentDescription = decreaseOctaveDesc
                        role = Role.Button
                    },
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        val octaveValueDesc = stringResource(R.string.cd_octave_value, state.currentOctave)
        Text(
            text = state.currentOctave.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.semantics {
                    contentDescription = octaveValueDesc
                    liveRegion = LiveRegionMode.Polite
                },
        )
        val increaseOctaveDesc = stringResource(R.string.cd_increase_octave)
        IconButton(
            onClick = onOctaveUp,
            enabled = state.currentOctave < 6,
            modifier =
                Modifier
                    .size(32.dp)
                    .semantics {
                        contentDescription = increaseOctaveDesc
                        role = Role.Button
                    },
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        noteNames.forEachIndexed { pc, name ->
            val addNoteDesc = stringResource(R.string.cd_add_note, name, state.currentOctave)
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                        ).clickable { onAddNote(pc) }
                        .semantics {
                            contentDescription = addNoteDesc
                            role = Role.Button
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
        val addRestDesc = stringResource(R.string.cd_add_rest)
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ).clickable { onAddRest() }
                    .semantics {
                        contentDescription = addRestDesc
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.melody_rest),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RecordInputContent(
    state: MelodyUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (state.isRecording) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isRecording) {
                Text(
                    text = stringResource(R.string.melody_listening),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                )

                Spacer(modifier = Modifier.height(12.dp))

                val detectedNote = state.detectedNote
                if (detectedNote != null) {
                    val noteName = Notes.pitchClassToName(detectedNote.pitchClass)
                    val detectedNoteDesc =
                        stringResource(R.string.cd_melody_detected_note, noteName, detectedNote.octave)
                    Text(
                        text = "$noteName${detectedNote.octave}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = detectedNoteDesc
                            },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { state.stabilizationProgress },
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )

                    Text(
                        text = stringResource(R.string.melody_stabilizing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.melody_no_sound),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AnimatedVisibility(
                    visible = state.lastAddedFeedback != null,
                    enter = if (reduceMotion) EnterTransition.None else fadeIn(),
                    exit = if (reduceMotion) ExitTransition.None else fadeOut(),
                ) {
                    state.lastAddedFeedback?.let { feedback ->
                        Text(
                            text = stringResource(R.string.melody_added_feedback, feedback),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .padding(top = 8.dp)
                                    .semantics {
                                        liveRegion = LiveRegionMode.Assertive
                                    },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(onClick = onStopRecording) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.melody_stop_recording))
                }
            } else {
                Text(
                    text = stringResource(R.string.melody_no_sound),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                val startRecordingDesc = stringResource(R.string.cd_start_recording)
                OutlinedButton(
                    onClick = onStartRecording,
                    modifier =
                        Modifier.semantics {
                            contentDescription = startRecordingDesc
                        },
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.melody_start_recording))
                }
            }
        }
    }
}

@Composable
internal fun BpmAndPlaybackControls(
    state: MelodyUiState,
    bpmSliderValue: Float,
    onBpmSliderChange: (Float) -> Unit,
    onBpmChange: (Int) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.label_bpm_value, bpmSliderValue.toInt()),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(64.dp),
        )
        Slider(
            value = bpmSliderValue,
            onValueChange = onBpmSliderChange,
            onValueChangeFinished = { onBpmChange(bpmSliderValue.toInt()) },
            valueRange = 40f..220f,
            modifier = Modifier.weight(1f),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onPlay,
            enabled = state.notes.isNotEmpty() && !state.isPlaying,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_melody),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.action_play))
        }

        if (state.isPlaying) {
            OutlinedButton(onClick = onStop) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_stop_playing),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_stop))
            }
        }

        OutlinedButton(
            onClick = onClear,
            enabled = state.notes.isNotEmpty() && !state.isPlaying,
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.cd_clear_all),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.melody_clear))
        }
    }
}

@Composable
internal fun OctaveSelector(
    currentOctave: Int,
    onOctaveUp: () -> Unit,
    onOctaveDown: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.melody_octave),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onOctaveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_decrease_octave),
            )
        }
        val octaveDescription = stringResource(R.string.cd_octave_value, currentOctave)
        Text(
            text = currentOctave.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.semantics {
                    contentDescription = octaveDescription
                    liveRegion = LiveRegionMode.Polite
                },
        )
        IconButton(onClick = onOctaveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.cd_increase_octave),
            )
        }
    }
}

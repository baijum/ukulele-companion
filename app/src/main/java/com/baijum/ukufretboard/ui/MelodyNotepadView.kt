package com.baijum.ukufretboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Melody
import com.baijum.ukufretboard.data.MelodyNote
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.viewmodel.MelodyInputMode
import com.baijum.ukufretboard.viewmodel.MelodyUiState
import com.baijum.ukufretboard.viewmodel.MelodyViewModel

/**
 * Melody Notepad — a note sequencer for composing melodies.
 *
 * Users can add notes by tapping from a 12-note palette or by recording
 * single notes from their ukulele via microphone. Melodies can be saved,
 * loaded, renamed, and deleted.
 */
@Composable
fun MelodyNotepadView(
    viewModel: MelodyViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Melody?>(null) }
    var showRenameDialog by remember { mutableStateOf<Melody?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var bpmSliderValue by remember(state.bpm) { mutableFloatStateOf(state.bpm.toFloat()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // --- Title bar with save + menu ---
        MelodyTitleBar(
            state = state,
            onSave = {
                if (state.loadedMelodyName != null) {
                    viewModel.saveMelody(state.loadedMelodyName!!)
                } else {
                    showSaveDialog = true
                }
            },
            showMenu = showMenu,
            onMenuToggle = { showMenu = it },
            onSaveAs = { showSaveDialog = true },
            onLoad = { showLoadDialog = true },
            onRename = {
                state.loadedMelodyId?.let { id ->
                    val melody = state.savedMelodies.find { it.id == id }
                    if (melody != null) showRenameDialog = melody
                }
            },
            onDelete = {
                state.loadedMelodyId?.let { id ->
                    val melody = state.savedMelodies.find { it.id == id }
                    if (melody != null) showDeleteDialog = melody
                }
            },
            onNew = {
                if (state.hasUnsavedChanges) {
                    showDiscardDialog = { viewModel.newMelody() }
                } else {
                    viewModel.newMelody()
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Note sequence display ---
        NoteSequenceCard(
            state = state,
            onSelectNote = viewModel::selectNote,
            onDeleteNote = viewModel::deleteSelectedNote,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Duration selector ---
        DurationSelector(
            selectedDuration = state.selectedDuration,
            onSelectDuration = viewModel::setDuration,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Input mode toggle + content ---
        InputModeSection(
            state = state,
            onSetInputMode = viewModel::setInputMode,
            onAddNote = { pc -> viewModel.addNote(pc) },
            onAddRest = viewModel::addRest,
            onOctaveUp = viewModel::incrementOctave,
            onOctaveDown = viewModel::decrementOctave,
            onStartRecording = viewModel::startRecording,
            onStopRecording = viewModel::stopRecording,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- BPM and playback controls ---
        BpmAndPlaybackControls(
            state = state,
            bpmSliderValue = bpmSliderValue,
            onBpmSliderChange = { bpmSliderValue = it },
            onBpmChange = { viewModel.setBpm(it) },
            onPlay = viewModel::playMelody,
            onStop = viewModel::stopPlayback,
            onClear = viewModel::clearAll,
        )
    }

    // --- Dialogs ---
    if (showSaveDialog) {
        SaveMelodyDialog(
            initialName = state.loadedMelodyName ?: "",
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveMelody(name)
                showSaveDialog = false
            },
        )
    }

    if (showLoadDialog) {
        LoadMelodyDialog(
            melodies = state.savedMelodies,
            onDismiss = { showLoadDialog = false },
            onLoad = { melody ->
                if (state.hasUnsavedChanges) {
                    showDiscardDialog = {
                        viewModel.loadMelody(melody)
                        showLoadDialog = false
                    }
                } else {
                    viewModel.loadMelody(melody)
                    showLoadDialog = false
                }
            },
            onDelete = { melody -> showDeleteDialog = melody },
        )
    }

    showDiscardDialog?.let { onConfirm ->
        DiscardChangesDialog(
            onDismiss = { showDiscardDialog = null },
            onConfirm = {
                onConfirm()
                showDiscardDialog = null
            },
        )
    }

    showDeleteDialog?.let { melody ->
        DeleteMelodyDialog(
            melodyName = melody.name,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                viewModel.deleteMelody(melody.id)
                showDeleteDialog = null
            },
        )
    }

    showRenameDialog?.let { melody ->
        RenameMelodyDialog(
            currentName = melody.name,
            onDismiss = { showRenameDialog = null },
            onRename = { newName ->
                viewModel.renameMelody(melody.id, newName)
                showRenameDialog = null
            },
        )
    }
}

// =============================================================================
// Title Bar
// =============================================================================

@Composable
private fun MelodyTitleBar(
    state: MelodyUiState,
    onSave: () -> Unit,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSaveAs: () -> Unit,
    onLoad: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onNew: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.melody_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.melody_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.loadedMelodyName != null) {
                Text(
                    text = buildString {
                        append(state.loadedMelodyName)
                        if (state.hasUnsavedChanges) append(" *")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(
            onClick = onSave,
            enabled = state.notes.isNotEmpty(),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_save_melody),
            )
        }

        Box {
            IconButton(onClick = { onMenuToggle(true) }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_options),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onMenuToggle(false) },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_save_as)) },
                    onClick = { onMenuToggle(false); onSaveAs() },
                    enabled = state.notes.isNotEmpty(),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_load)) },
                    onClick = { onMenuToggle(false); onLoad() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_rename)) },
                    onClick = { onMenuToggle(false); onRename() },
                    enabled = state.loadedMelodyId != null,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_new)) },
                    onClick = { onMenuToggle(false); onNew() },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.dialog_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { onMenuToggle(false); onDelete() },
                    enabled = state.loadedMelodyId != null,
                )
            }
        }
    }
}

// =============================================================================
// Note Sequence Card
// =============================================================================

@Composable
private fun NoteSequenceCard(
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
    val noteName = if (note.pitchClass != null) {
        noteNames[note.pitchClass]
    } else {
        "\u2014"
    }
    val displayText = if (note.pitchClass != null) {
        "$noteName${note.octave}"
    } else {
        "\u2014"
    }

    val durationLabel = when (note.duration) {
        NoteDuration.WHOLE -> "whole"
        NoteDuration.HALF -> "half"
        NoteDuration.QUARTER -> "quarter"
        NoteDuration.EIGHTH -> "eighth"
        NoteDuration.SIXTEENTH -> "sixteenth"
    }

    val noteDescription = if (note.pitchClass != null) {
        "$noteName${note.octave} $durationLabel note, position ${index + 1}"
    } else {
        "Rest, $durationLabel, position ${index + 1}"
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

// =============================================================================
// Duration Selector
// =============================================================================

@Composable
private fun DurationSelector(
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
            val label = when (dur) {
                NoteDuration.WHOLE -> stringResource(R.string.melody_whole)
                NoteDuration.HALF -> stringResource(R.string.melody_half)
                NoteDuration.QUARTER -> stringResource(R.string.melody_quarter)
                NoteDuration.EIGHTH -> stringResource(R.string.melody_eighth)
                NoteDuration.SIXTEENTH -> stringResource(R.string.melody_sixteenth)
            }
            FilterChip(
                selected = selectedDuration == dur,
                onClick = { onSelectDuration(dur) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.semantics {
                    contentDescription = "${dur.label} note duration"
                    if (selectedDuration == dur) stateDescription = "selected"
                },
            )
        }
    }
}

// =============================================================================
// Input Mode Section
// =============================================================================

@Composable
private fun InputModeSection(
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

    // Input mode toggle
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = state.inputMode == MelodyInputMode.TAP,
            onClick = { onSetInputMode(MelodyInputMode.TAP) },
            label = { Text(stringResource(R.string.melody_input_tap)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        FilterChip(
            selected = state.inputMode == MelodyInputMode.RECORD,
            onClick = { onSetInputMode(MelodyInputMode.RECORD) },
            label = { Text(stringResource(R.string.melody_input_record)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }

    when (state.inputMode) {
        MelodyInputMode.TAP -> TapInputContent(
            state = state,
            onAddNote = onAddNote,
            onAddRest = onAddRest,
            onOctaveUp = onOctaveUp,
            onOctaveDown = onOctaveDown,
        )
        MelodyInputMode.RECORD -> RecordInputContent(
            state = state,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
        )
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

    // Octave control
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
        IconButton(
            onClick = onOctaveDown,
            enabled = state.currentOctave > 3,
            modifier = Modifier
                .size(32.dp)
                .semantics {
                    contentDescription = "Decrease octave"
                    role = Role.Button
                },
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = state.currentOctave.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = "Octave ${state.currentOctave}"
                liveRegion = LiveRegionMode.Polite
            },
        )
        IconButton(
            onClick = onOctaveUp,
            enabled = state.currentOctave < 6,
            modifier = Modifier
                .size(32.dp)
                .semantics {
                    contentDescription = "Increase octave"
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

    // Note palette
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
                    .clickable { onAddNote(pc) }
                    .semantics {
                        contentDescription = "Add $name${state.currentOctave}"
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
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .clickable { onAddRest() }
                .semantics {
                    contentDescription = "Add rest"
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isRecording) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isRecording) {
                Text(
                    text = stringResource(R.string.melody_listening),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                val detectedNote = state.detectedNote
                if (detectedNote != null) {
                    val noteName = Notes.pitchClassToName(detectedNote.pitchClass)
                    Text(
                        text = "$noteName${detectedNote.octave}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Detected $noteName${detectedNote.octave}"
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

                // Feedback for last added note
                AnimatedVisibility(
                    visible = state.lastAddedFeedback != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    state.lastAddedFeedback?.let { feedback ->
                        Text(
                            text = "Added $feedback",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
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

                OutlinedButton(
                    onClick = onStartRecording,
                    modifier = Modifier.semantics {
                        contentDescription = "Record notes from ukulele"
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

// =============================================================================
// BPM and Playback Controls
// =============================================================================

@Composable
private fun BpmAndPlaybackControls(
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

// =============================================================================
// Dialogs
// =============================================================================

@Composable
private fun SaveMelodyDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.melody_save_dialog_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.melody_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.melody_save))
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
private fun LoadMelodyDialog(
    melodies: List<Melody>,
    onDismiss: () -> Unit,
    onLoad: (Melody) -> Unit,
    onDelete: (Melody) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.melody_load_dialog_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            if (melodies.isEmpty()) {
                Text(
                    stringResource(R.string.melody_load_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(300.dp),
                ) {
                    items(melodies, key = { it.id }) { melody ->
                        MelodyListItem(
                            melody = melody,
                            onClick = { onLoad(melody) },
                            onDelete = { onDelete(melody) },
                        )
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

@Composable
private fun MelodyListItem(
    melody: Melody,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = melody.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${melody.notes.size} notes \u00b7 ${melody.bpm} BPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${melody.name}",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.melody_discard_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.melody_discard_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_delete))
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
private fun DeleteMelodyDialog(
    melodyName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.melody_delete_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.melody_delete_message, melodyName)) },
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
private fun RenameMelodyDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.melody_rename_dialog_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.melody_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name) },
                enabled = name.isNotBlank() && name != currentName,
            ) {
                Text(stringResource(R.string.melody_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

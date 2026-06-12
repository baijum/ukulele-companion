package com.baijum.ukufretboard.ui.melody

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Melody
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

    DisposableEffect(Unit) {
        onDispose { viewModel.stopRecording() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val linearModeDesc = stringResource(R.string.cd_linear_mode)
            FilterChip(
                selected = !state.isStepSequencerMode,
                onClick = { if (state.isStepSequencerMode) viewModel.toggleStepSequencerMode() },
                label = { Text(stringResource(R.string.melody_mode_linear)) },
                modifier = Modifier.semantics {
                    contentDescription = linearModeDesc
                },
            )
            val stepSequencerModeDesc = stringResource(R.string.cd_step_sequencer_mode)
            FilterChip(
                selected = state.isStepSequencerMode,
                onClick = { if (!state.isStepSequencerMode) viewModel.toggleStepSequencerMode() },
                label = { Text(stringResource(R.string.melody_mode_step_sequencer)) },
                modifier = Modifier.semantics {
                    contentDescription = stepSequencerModeDesc
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isStepSequencerMode) {
            StepSequencerGrid(
                state = state,
                onSetStep = viewModel::setStep,
                onClearStep = viewModel::clearStep,
                onExpandSteps = viewModel::expandSteps,
            )

            Spacer(modifier = Modifier.height(12.dp))

            DurationSelector(
                selectedDuration = state.selectedDuration,
                onSelectDuration = viewModel::setDuration,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OctaveSelector(
                currentOctave = state.currentOctave,
                onOctaveUp = viewModel::incrementOctave,
                onOctaveDown = viewModel::decrementOctave,
            )

            Spacer(modifier = Modifier.height(12.dp))

            BpmAndPlaybackControls(
                state = state,
                bpmSliderValue = bpmSliderValue,
                onBpmSliderChange = { bpmSliderValue = it },
                onBpmChange = { viewModel.setBpm(it) },
                onPlay = viewModel::playSteps,
                onStop = viewModel::stopPlayback,
                onClear = {
                    for (i in state.steps.indices) {
                        viewModel.clearStep(i)
                    }
                },
            )
        } else {
            NoteSequenceCard(
                state = state,
                onSelectNote = viewModel::selectNote,
                onDeleteNote = viewModel::deleteSelectedNote,
            )

            Spacer(modifier = Modifier.height(12.dp))

            DurationSelector(
                selectedDuration = state.selectedDuration,
                onSelectDuration = viewModel::setDuration,
            )

            Spacer(modifier = Modifier.height(8.dp))

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
    }

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
        LoadMelodySheet(
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

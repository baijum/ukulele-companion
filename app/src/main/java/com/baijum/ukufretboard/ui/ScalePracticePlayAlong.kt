package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.ScalePracticeSettings
import com.baijum.ukufretboard.data.Scales
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.viewmodel.FretPosition
import com.baijum.ukufretboard.viewmodel.PlayDirection
import com.baijum.ukufretboard.viewmodel.PlaybackState
import com.baijum.ukufretboard.viewmodel.ScalePracticeUiState
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel
import com.baijum.ukufretboard.domain.UkuleleString

internal fun fretPositionsForNote(
    pitchClass: Int,
    tuning: List<UkuleleString>,
    maxFret: Int = 12,
): Map<Int, List<Int>> {
    return tuning.mapIndexedNotNull { stringIndex, string ->
        val base = (pitchClass - string.openPitchClass + Notes.PITCH_CLASS_COUNT) %
            Notes.PITCH_CLASS_COUNT
        val frets = generateSequence(base) { it + Notes.PITCH_CLASS_COUNT }
            .takeWhile { it <= maxFret }
            .toList()
        if (frets.isNotEmpty()) stringIndex to frets else null
    }.toMap()
}

@Composable
internal fun PlayAlongContent(
    viewModel: ScalePracticeViewModel,
    state: ScalePracticeUiState,
    onSettingsChanged: (ScalePracticeSettings) -> Unit,
    tuning: List<UkuleleString>,
    lastFret: Int,
) {
    val scope = rememberCoroutineScope()

    Text(
        text = stringResource(R.string.scale_practice_root),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .semantics { heading() }
            .padding(bottom = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Notes.NOTE_NAMES_STANDARD.forEachIndexed { index, name ->
            FilterChip(
                selected = index == state.selectedRoot,
                onClick = {
                    viewModel.setRoot(index)
                    onSettingsChanged(viewModel.currentSettings())
                },
                label = {
                    Text(
                        name,
                        fontWeight = if (index == state.selectedRoot) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = stringResource(R.string.label_scale),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .semantics { heading() }
            .padding(bottom = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.availableScales.forEach { scale ->
            FilterChip(
                selected = state.selectedScale == scale,
                onClick = {
                    viewModel.setScale(scale)
                    onSettingsChanged(viewModel.currentSettings())
                },
                label = { Text(scale.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                ),
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = stringResource(R.string.label_direction),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .semantics { heading() }
            .padding(bottom = 4.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlayDirection.entries.forEach { dir ->
            FilterChip(
                selected = state.direction == dir,
                onClick = {
                    viewModel.setDirection(dir)
                    onSettingsChanged(viewModel.currentSettings())
                },
                label = { Text(dir.localizedLabel()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                ),
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = stringResource(R.string.scale_practice_options),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .semantics { heading() }
            .padding(bottom = 4.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = state.loopPlayback,
            onClick = {
                viewModel.toggleLoop()
                onSettingsChanged(viewModel.currentSettings())
            },
            label = { Text(stringResource(R.string.scale_practice_loop)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
        FilterChip(
            selected = state.showFretboard,
            onClick = {
                viewModel.toggleFretboard()
                onSettingsChanged(viewModel.currentSettings())
            },
            label = { Text(stringResource(R.string.scale_practice_fretboard)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }

    if (state.showFretboard) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.label_position),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .semantics { heading() }
                .padding(bottom = 4.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FretPosition.entries.forEach { pos ->
                FilterChip(
                    selected = state.fretPosition == pos,
                    onClick = {
                        viewModel.setFretPosition(pos)
                        onSettingsChanged(viewModel.currentSettings())
                    },
                    label = { Text(pos.localizedLabel()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.label_bpm_value, state.bpm),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
        value = state.bpm.toFloat(),
        onValueChange = { viewModel.setBpm(it.toInt()) },
        valueRange = ScalePracticeSettings.MIN_BPM.toFloat()..ScalePracticeSettings.MAX_BPM.toFloat(),
        onValueChangeFinished = { onSettingsChanged(viewModel.currentSettings()) },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    val rootName = Notes.enharmonicForKey(state.selectedRoot, state.selectedRoot, false)
    val isMinor = state.selectedScale.intervals.size > 2 && state.selectedScale.intervals[2] == 3
    val noteNames = state.selectedScale.intervals.map { interval ->
        val pc = (state.selectedRoot + interval) % Notes.PITCH_CLASS_COUNT
        Notes.enharmonicForKey(pc, state.selectedRoot, isMinor)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$rootName ${state.selectedScale.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = noteNames.joinToString(" – "),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            if (state.playbackState == PlaybackState.PLAYING &&
                state.currentNoteIndex in state.playAlongNotes.indices
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                val currentPc = state.playAlongNotes[state.currentNoteIndex]
                val currentName = Notes.enharmonicForKey(currentPc, state.selectedRoot, isMinor)
                Text(
                    text = "${stringResource(R.string.scale_practice_now)} $currentName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = currentName
                    },
                )
                Text(
                    text = stringResource(R.string.scale_practice_note_of, state.currentNoteIndex + 1, state.playAlongNotes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (state.showFretboard) {
        Spacer(modifier = Modifier.height(12.dp))

        val scaleNotes = Scales.scaleNotes(state.selectedRoot, state.selectedScale)

        val posRange = state.fretPosition.range(lastFret)
        val selections: Map<Int, Int?> = if (
            state.playbackState == PlaybackState.PLAYING &&
            state.currentNoteIndex in state.playAlongNotes.indices
        ) {
            val currentPc = state.playAlongNotes[state.currentNoteIndex]
            val allPositions = fretPositionsForNote(currentPc, tuning, maxFret = lastFret)
            val filtered = if (posRange != null) {
                allPositions.mapValues { (_, frets) -> frets.filter { it in posRange } }
                    .filterValues { it.isNotEmpty() }
            } else {
                allPositions
            }
            val effective = filtered.ifEmpty { allPositions }
            effective.mapValues { (_, frets) -> frets.first() }
        } else {
            tuning.indices.associateWith { null }
        }

        val getNoteAt: (Int, Int) -> Note = { stringIndex, fret ->
            val pc = (tuning[stringIndex].openPitchClass + fret) % Notes.PITCH_CLASS_COUNT
            val name = Notes.enharmonicForKey(pc, state.selectedRoot, isMinor)
            Note(pitchClass = pc, name = name)
        }

        FretboardView(
            tuning = tuning,
            selections = selections,
            showNoteNames = true,
            onFretTap = { _, _ -> },
            getNoteAt = getNoteAt,
            scaleNotes = scaleNotes,
            scaleRoot = state.selectedRoot,
            scalePositionFretRange = posRange,
            lastFret = lastFret,
            cellWidth = 42.dp,
            cellHeight = 38.dp,
            scrollable = true,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    when (state.playbackState) {
        PlaybackState.STOPPED -> {
            Button(
                onClick = { viewModel.startPlayback(scope) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.action_play),
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.scale_practice_play))
            }
        }
        PlaybackState.PLAYING -> {
            Button(
                onClick = { viewModel.stopPlayback() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.action_stop))
            }
        }
        PlaybackState.PAUSED -> {
            Button(
                onClick = { viewModel.startPlayback(scope) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scale_practice_resume))
            }
        }
    }
}

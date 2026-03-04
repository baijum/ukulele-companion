package com.baijum.ukufretboard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.audio.BeatType
import com.baijum.ukufretboard.viewmodel.MetronomeViewModel

/**
 * Standalone metronome screen with BPM control, time signature selection,
 * accent pattern editing, subdivisions, and visual beat indicators.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetronomeTab(
    viewModel: MetronomeViewModel,
    modifier: Modifier = Modifier,
) {
    val bpm by viewModel.bpm.collectAsState()
    val beatsPerMeasure by viewModel.beatsPerMeasure.collectAsState()
    val subdivision by viewModel.subdivision.collectAsState()
    val accentPattern by viewModel.accentPattern.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentBeat by viewModel.currentBeat.collectAsState()
    val measureCount by viewModel.measureCount.collectAsState()
    val isCompound by viewModel.isCompound.collectAsState()
    val timeSignatureLabel by viewModel.timeSignatureLabel.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- BPM Display ---
        Text(
            text = "$bpm",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = { viewModel.setBpm(bpm - 1) }) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.metronome_decrease_bpm),
                )
            }
            Text(
                text = stringResource(R.string.metronome_bpm),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { viewModel.setBpm(bpm + 1) }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.metronome_increase_bpm),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BPM Slider ---
        Slider(
            value = bpm.toFloat(),
            onValueChange = { viewModel.setBpm(it.toInt()) },
            valueRange = 30f..300f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Tap Tempo ---
        TapTempoButton(
            onBpmDetected = { viewModel.setBpm(it) },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Time Signature ---
        Text(
            text = stringResource(R.string.metronome_time_signature),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(8.dp))

        val timeSignatures = listOf("2/4", "3/4", "4/4", "5/4", "6/4", "7/4", "6/8", "12/8")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            timeSignatures.forEach { label ->
                FilterChip(
                    selected = timeSignatureLabel == label,
                    onClick = { viewModel.setTimeSignature(label) },
                    label = { Text(label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Beat Indicators ---
        Text(
            text = stringResource(R.string.metronome_accent_pattern),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.metronome_tap_to_change),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            accentPattern.forEachIndexed { index, type ->
                BeatIndicator(
                    beatIndex = index,
                    type = type,
                    isActive = isPlaying && currentBeat == index,
                    onClick = { viewModel.toggleBeatType(index) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Subdivision ---
        Text(
            text = stringResource(R.string.metronome_subdivision),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(8.dp))

        val subdivisionLabels = listOf(
            1 to stringResource(R.string.metronome_quarter),
            2 to stringResource(R.string.metronome_eighth),
            3 to stringResource(R.string.metronome_triplet),
            4 to stringResource(R.string.metronome_sixteenth),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            subdivisionLabels.forEach { (value, label) ->
                FilterChip(
                    selected = subdivision == value,
                    onClick = { viewModel.setSubdivision(value) },
                    enabled = !isCompound,
                    label = { Text(label) },
                )
            }
        }
        if (isCompound) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.metronome_compound_subdivision_locked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Play/Stop ---
        FloatingActionButton(
            onClick = { viewModel.togglePlayback() },
            modifier = Modifier.size(72.dp),
            containerColor = if (isPlaying) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.cd_stop_metronome)
                else stringResource(R.string.cd_start_metronome),
                modifier = Modifier.size(36.dp),
                tint = if (isPlaying) MaterialTheme.colorScheme.onError
                else MaterialTheme.colorScheme.onPrimary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Measure Counter ---
        if (isPlaying && measureCount > 0) {
            Text(
                text = stringResource(R.string.metronome_measure_count, measureCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BeatIndicator(
    beatIndex: Int,
    type: BeatType,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val targetScale by animateFloatAsState(
        targetValue = if (isActive) 1.3f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "beatScale",
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive && type == BeatType.ACCENT -> MaterialTheme.colorScheme.primary
            isActive && type == BeatType.NORMAL -> MaterialTheme.colorScheme.secondary
            isActive && type == BeatType.MUTE -> MaterialTheme.colorScheme.outlineVariant
            type == BeatType.ACCENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            type == BeatType.NORMAL -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 100),
        label = "beatColor",
    )
    val borderColor = when (type) {
        BeatType.ACCENT -> MaterialTheme.colorScheme.primary
        BeatType.NORMAL -> MaterialTheme.colorScheme.secondary
        BeatType.MUTE -> MaterialTheme.colorScheme.outlineVariant
    }

    val typeLabel = when (type) {
        BeatType.ACCENT -> "accented"
        BeatType.NORMAL -> "normal"
        BeatType.MUTE -> "muted"
    }
    val beatTypeDesc = stringResource(R.string.cd_beat_type, beatIndex + 1, typeLabel)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(targetScale)
                .border(2.dp, borderColor, CircleShape)
                .background(backgroundColor, CircleShape)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = beatTypeDesc
                    role = Role.Button
                    if (isActive) stateDescription = "current beat"
                },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${beatIndex + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp),
        )
    }
}

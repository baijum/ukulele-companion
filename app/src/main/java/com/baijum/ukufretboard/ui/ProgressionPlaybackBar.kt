package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.audio.MetronomeEngine
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * A playback toolbar for chord progressions with tempo control.
 *
 * Displays BPM slider, beats-per-chord selector, loop toggle,
 * and play/stop controls. Shows a visual indicator of the current
 * chord being played.
 *
 * @param progression The chord progression to play.
 * @param keyRoot The root pitch class of the selected key.
 * @param tuning Current ukulele tuning for voicing generation.
 * @param onPlayVoicing Callback to play a chord voicing.
 * @param onDismiss Callback to close the playback bar.
 */
@Composable
fun ProgressionPlaybackBar(
    progression: Progression,
    keyRoot: Int,
    tuning: List<UkuleleString>,
    onPlayVoicing: ((ChordVoicing) -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val engine = remember { MetronomeEngine() }
    var isPlaying by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(100f) }
    var beatsPerChord by remember { mutableIntStateOf(4) }
    var loop by remember { mutableStateOf(true) }
    var currentChordIndex by remember { mutableIntStateOf(-1) }

    // Generate voicings for the progression
    val voicings = remember(progression, keyRoot) {
        progression.degrees.map { degree ->
            val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
                .firstOrNull { it.symbol == degree.quality }
            if (formula != null) {
                VoicingGenerator.generate(chordRoot, formula, tuning).firstOrNull()
            } else null
        }
    }

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            engine.stop()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Title row with dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.playback_prefix) + progression.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        engine.stop()
                        isPlaying = false
                        onDismiss()
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_close),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Chord indicators with current-chord highlight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                progression.degrees.forEachIndexed { index, degree ->
                    val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                    val chordName = Notes.enharmonicForKey(chordRoot, keyRoot) + degree.quality
                    val isCurrent = index == currentChordIndex

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = chordName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BPM slider + Tap Tempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.playback_bpm_prefix) + "${bpm.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(64.dp),
                )
                Slider(
                    value = bpm,
                    onValueChange = { bpm = it },
                    valueRange = 40f..220f,
                    steps = 0,
                    modifier = Modifier.weight(1f),
                )
                TapTempoButton(
                    onBpmDetected = { detected -> bpm = detected.toFloat() },
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // Beats per chord + loop + play controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.label_beats),
                    style = MaterialTheme.typography.labelSmall,
                )
                listOf(1, 2, 4, 8).forEach { beats ->
                    FilterChip(
                        selected = beatsPerChord == beats,
                        onClick = { beatsPerChord = beats },
                        label = { Text("$beats") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.height(30.dp),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Loop toggle
                IconButton(
                    onClick = { loop = !loop },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = if (loop) stringResource(R.string.cd_loop_on) else stringResource(R.string.cd_loop_off),
                        tint = if (loop) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Play/Stop button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            engine.stop()
                            isPlaying = false
                            currentChordIndex = -1
                        } else {
                            isPlaying = true
                            var lastChordPlayed = -1
                            engine.start(
                                scope = scope,
                                bpm = bpm.toInt(),
                                beatsPerChord = beatsPerChord,
                                chordCount = progression.degrees.size,
                                loop = loop,
                                onBeat = { chordIdx, _ ->
                                    currentChordIndex = chordIdx
                                    // Play voicing only on first beat of each chord
                                    if (chordIdx != lastChordPlayed) {
                                        lastChordPlayed = chordIdx
                                        val voicing = voicings.getOrNull(chordIdx)
                                        if (voicing != null && onPlayVoicing != null) {
                                            onPlayVoicing(voicing)
                                        }
                                    }
                                },
                                onComplete = {
                                    isPlaying = false
                                    currentChordIndex = -1
                                },
                            )
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isPlaying) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.cd_stop) else stringResource(R.string.action_play),
                        tint = if (isPlaying) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                    )
                }
            }
        }
    }
}

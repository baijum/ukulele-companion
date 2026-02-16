package com.baijum.ukufretboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Scale
import com.baijum.ukufretboard.data.ScalePosition
import com.baijum.ukufretboard.data.ScalePositions
import com.baijum.ukufretboard.data.Scales
import com.baijum.ukufretboard.domain.ScaleChords
import com.baijum.ukufretboard.viewmodel.ScaleOverlayState

/**
 * Collapsible scale selector that controls the fretboard scale overlay.
 *
 * Shows a root note selector and scale type selector when expanded.
 *
 * @param state The current scale overlay state.
 * @param onRootChanged Callback when the scale root note is changed.
 * @param onScaleChanged Callback when the scale type is changed.
 * @param onToggle Callback to toggle the overlay on/off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleSelector(
    state: ScaleOverlayState,
    tuningPitchClasses: List<Int> = listOf(7, 0, 4, 9),
    onRootChanged: (Int) -> Unit,
    onScaleChanged: (Scale) -> Unit,
    onToggle: () -> Unit,
    onPositionChanged: (ScalePosition?) -> Unit = {},
    onChordTapped: (ScaleChords.DiatonicChord) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        AnimatedVisibility(visible = state.enabled) {
            Column {
                // Root note selector
                Text(
                    text = stringResource(R.string.scale_selector_root),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                val noteNames = Notes.NOTE_NAMES_STANDARD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    noteNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = index == state.root,
                            onClick = { onRootChanged(index) },
                            label = { Text(name, fontWeight = if (index == state.root) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scale type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Scales.ALL.forEach { scale ->
                        FilterChip(
                            selected = state.scale == scale,
                            onClick = { onScaleChanged(scale) },
                            label = { Text(scale.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                        )
                    }
                }

                // Position selector (only shown when a scale is selected)
                val currentScale = state.scale
                if (currentScale != null) {
                    Spacer(modifier = Modifier.height(6.dp))

                    val positions = remember(state.root, currentScale, tuningPitchClasses) {
                        ScalePositions.generate(state.root, currentScale.intervals, tuningPitchClasses)
                    }
                    var selectedPosition by remember(state.root, currentScale) { mutableStateOf<ScalePosition?>(null) }

                    Text(
                        text = stringResource(R.string.label_position),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilterChip(
                            selected = selectedPosition == null,
                            onClick = {
                                selectedPosition = null
                                onPositionChanged(null)
                            },
                            label = { Text(stringResource(R.string.label_all)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                            ),
                        )
                        positions.forEach { pos ->
                            FilterChip(
                                selected = selectedPosition == pos,
                                onClick = {
                                    selectedPosition = pos
                                    onPositionChanged(pos)
                                },
                                label = {
                                    Text("${pos.name} (${pos.fretRange.first}–${pos.fretRange.last})")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                            )
                        }
                    }
                }

                // Chords in this scale
                if (currentScale != null) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.scale_selector_chords_in, currentScale.name),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    val diatonicChords = ScaleChords.diatonicTriads(state.root, currentScale)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        diatonicChords.forEach { chord ->
                            val chordName = ScaleChords.formatChord(chord, state.root)
                            FilterChip(
                                selected = false,
                                onClick = { onChordTapped(chord) },
                                label = {
                                    Text(
                                        text = chordName,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

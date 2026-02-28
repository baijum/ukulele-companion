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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.UkuleleTuning
import kotlinx.coroutines.launch

/**
 * Fretboard Note Map — displays all note names on the ukulele fretboard.
 *
 * Features:
 * - Uses the currently selected app tuning
 * - Optional quick toggle between High-G and Low-G when applicable
 * - Highlight a specific note across all positions
 * - Tap any note to hear it
 * - Shows frets 0–12
 */
@Composable
fun FretboardNoteMapView(
    tuning: UkuleleTuning,
    lastFret: Int = 12,
    modifier: Modifier = Modifier,
) {
    val canToggleHighLow = tuning == UkuleleTuning.HIGH_G || tuning == UkuleleTuning.LOW_G
    var isLowG by remember(tuning) { mutableStateOf(tuning == UkuleleTuning.LOW_G) }
    val effectiveTuning = if (canToggleHighLow) {
        if (isLowG) UkuleleTuning.LOW_G else UkuleleTuning.HIGH_G
    } else {
        tuning
    }
    var highlightNote by remember { mutableIntStateOf(-1) } // -1 = none
    val scope = rememberCoroutineScope()
    val maxFret = lastFret

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.note_map_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.note_map_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tuning selector (shown only when the current setting is High-G/Low-G)
        if (canToggleHighLow) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !isLowG,
                    onClick = { isLowG = false },
                    label = { Text(stringResource(R.string.note_map_high_g)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                FilterChip(
                    selected = isLowG,
                    onClick = { isLowG = true },
                    label = { Text(stringResource(R.string.note_map_low_g)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = effectiveTuning.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Highlight filter
        Text(
            text = stringResource(R.string.note_map_highlight),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = highlightNote == -1,
                onClick = { highlightNote = -1 },
                label = { Text(stringResource(R.string.label_all), style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                ),
            )
            (0..11).forEach { pc ->
                FilterChip(
                    selected = highlightNote == pc,
                    onClick = { highlightNote = pc },
                    label = { Text(Notes.pitchClassToName(pc), style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fretboard grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val tuningPitchClasses = effectiveTuning.pitchClasses
                val octaves = effectiveTuning.octaves
                val stringNames = effectiveTuning.stringNames

                // Header row: fret numbers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    // String label column
                    Box(modifier = Modifier.size(36.dp))
                    (0..maxFret).forEach { fret ->
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$fret",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // String rows (reversed: A, E, C, G top-to-bottom)
                tuningPitchClasses.indices.reversed().forEach { stringIndex ->
                    val openPc = tuningPitchClasses[stringIndex]
                    val baseOctave = octaves[stringIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        // String name
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringNames[stringIndex],
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        (0..maxFret).forEach { fret ->
                            val pitchClass = (openPc + fret) % 12
                            val octave = baseOctave + (openPc + fret) / 12
                            val noteName = Notes.pitchClassToName(pitchClass)
                            val isHighlighted = highlightNote == -1 || highlightNote == pitchClass
                            val isOpen = fret == 0
                            val bgColor = when {
                                !isHighlighted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                isOpen -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val textColor = when {
                                !isHighlighted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                isOpen -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bgColor)
                                    .border(
                                        width = if (isOpen) 1.5.dp else 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .clickable(enabled = isHighlighted) {
                                        scope.launch {
                                            ToneGenerator.playNote(pitchClass, octave)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = noteName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Helpful info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.note_map_tips_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\u2022 ${stringResource(R.string.note_map_tip_1)}\n" +
                        "\u2022 ${stringResource(R.string.note_map_tip_2)}\n" +
                        "\u2022 ${stringResource(R.string.note_map_tip_3)}\n" +
                        "\u2022 ${stringResource(R.string.note_map_tip_4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

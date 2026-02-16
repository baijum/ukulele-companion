package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Scale
import com.baijum.ukufretboard.data.Scales
import com.baijum.ukufretboard.domain.ScaleChord
import com.baijum.ukufretboard.domain.ScaleChordBuilder

/**
 * Scale-Chord Relationship Viewer.
 *
 * Select a root note and scale type to see all diatonic chords
 * built from that scale, with Roman numerals, quality, and notes.
 */
@Composable
fun ScaleChordView(
    modifier: Modifier = Modifier,
) {
    var selectedRoot by remember { mutableIntStateOf(0) } // C
    var selectedScale by remember { mutableStateOf(Scales.ALL.first()) } // Major

    val scaleNotes = remember(selectedRoot, selectedScale) {
        Scales.scaleNotes(selectedRoot, selectedScale)
            .sorted()
            .map { Notes.pitchClassToName(it) }
    }

    val chords = remember(selectedRoot, selectedScale) {
        ScaleChordBuilder.buildTriads(selectedRoot, selectedScale)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.scale_chord_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.scale_chord_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Root selector
        Text(
            text = stringResource(R.string.label_root),
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
            (0..11).forEach { pc ->
                FilterChip(
                    selected = selectedRoot == pc,
                    onClick = { selectedRoot = pc },
                    label = { Text(Notes.pitchClassToName(pc), style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scale selector
        Text(
            text = stringResource(R.string.label_scale),
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
            // Only show scales with 5+ notes (can build triads)
            Scales.ALL.filter { it.intervals.size >= 5 }.forEach { scale ->
                FilterChip(
                    selected = selectedScale == scale,
                    onClick = { selectedScale = scale },
                    label = { Text(scale.name, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scale notes display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${Notes.pitchClassToName(selectedRoot)} ${selectedScale.name} Scale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.scale_chord_notes, scaleNotes.joinToString("  ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diatonic chords
        if (chords.isEmpty()) {
            Text(
                text = stringResource(R.string.scale_chord_too_few),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.scale_chord_diatonic),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            chords.forEach { chord ->
                ScaleChordCard(chord = chord)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ScaleChordCard(chord: ScaleChord) {
    val functionColor = when (chord.quality) {
        "Major" -> MaterialTheme.colorScheme.primary
        "Minor" -> MaterialTheme.colorScheme.secondary
        "Diminished" -> MaterialTheme.colorScheme.error
        "Augmented" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = chord.numeral,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = functionColor,
                    )
                    Text(
                        text = "${chord.rootName}${chord.symbol}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chord.notes.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = chord.quality,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = functionColor,
            )
        }
    }
}

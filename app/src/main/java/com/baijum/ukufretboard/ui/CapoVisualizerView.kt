package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * Capo Chord Shape Visualizer — shows how a chord shape looks and sounds
 * with a capo at different fret positions.
 *
 * Displays two diagrams side-by-side:
 * - **Left:** The original chord shape (without capo)
 * - **Right:** The same shape with a visual capo bar, showing the sounding chord
 *
 * A slider lets the user move the capo position and see the sounding
 * chord name update in real time.
 *
 * @param voicing The chord voicing (shape) to visualize.
 * @param rootPitchClass The root pitch class of the original chord.
 * @param formula The chord formula.
 * @param tuning Current ukulele tuning.
 * @param leftHanded Whether to mirror diagrams for left-handed players.
 * @param onBack Callback to return to the previous view.
 * @param modifier Optional modifier.
 */
@Composable
fun CapoVisualizerView(
    voicing: ChordVoicing,
    rootPitchClass: Int,
    formula: ChordFormula,
    tuning: List<UkuleleString>,
    leftHanded: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var capoPosition by remember { mutableFloatStateOf(0f) }
    val capoFret = capoPosition.toInt()

    // Compute the sounding root when capo is applied
    val soundingRoot = (rootPitchClass + capoFret) % Notes.PITCH_CLASS_COUNT
    val originalName = Notes.pitchClassToName(rootPitchClass) + formula.symbol
    val soundingName = Notes.pitchClassToName(soundingRoot) + formula.symbol

    // Compute sounding note names for each string
    val soundingNoteNames = voicing.frets.mapIndexed { i, fret ->
        if (fret == ChordVoicing.MUTED) "x"
        else {
            val pc = (tuning[i].openPitchClass + fret + capoFret) % Notes.PITCH_CLASS_COUNT
            Notes.pitchClassToName(pc)
        }
    }.joinToString(" ")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.capo_viz_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current chord info
        Text(
            text = stringResource(R.string.capo_viz_shape, originalName),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        if (capoFret > 0) {
            Text(
                text = stringResource(R.string.capo_viz_with_capo, capoFret, soundingName),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = stringResource(R.string.capo_viz_no_capo, originalName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Capo position slider
        Text(
            text = stringResource(R.string.capo_viz_position),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = capoPosition,
                onValueChange = { capoPosition = it },
                valueRange = 0f..11f,
                steps = 10, // 11 discrete positions (0-11), 10 steps between them
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text(
                text = "11",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (capoFret == 0) stringResource(R.string.capo_viz_no_capo_label) else stringResource(R.string.capo_viz_fret, capoFret),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Side-by-side diagrams
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Original shape (no capo)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = originalName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.capo_guide_shape),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                VerticalChordDiagram(
                    voicing = voicing,
                    onClick = {},
                    leftHanded = leftHanded,
                    modifier = Modifier.width(150.dp),
                )
            }

            // With capo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (capoFret > 0) soundingName else originalName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (capoFret > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (capoFret > 0) stringResource(R.string.capo_viz_sounding) else stringResource(R.string.capo_viz_same),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                VerticalChordDiagram(
                    voicing = voicing,
                    onClick = {},
                    leftHanded = leftHanded,
                    capoFret = if (capoFret > 0) capoFret else null,
                    soundingNotes = if (capoFret > 0) soundingNoteNames else null,
                    modifier = Modifier.width(150.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Educational note
        if (capoFret > 0) {
            Text(
                text = stringResource(R.string.capo_viz_tip, capoFret, originalName, soundingName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

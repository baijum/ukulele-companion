package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.UkuleleString

@Composable
internal fun InversionCompareView(
    grouped: Map<ChordInfo.Inversion, List<ChordVoicing>>,
    tuning: List<UkuleleString>,
    rootPitchClass: Int,
    formula: ChordFormula,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onPlayVoicing: ((ChordVoicing) -> Unit)?,
    onPlayAllInversions: (() -> Unit)?,
    leftHanded: Boolean,
    onExitCompare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableInversions = ChordInfo.Inversion.entries.filter { grouped.containsKey(it) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.chord_library_inversion_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onPlayAllInversions != null && availableInversions.size > 1) {
                    Button(
                        onClick = onPlayAllInversions,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_all_inversions),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.chord_library_play_all_inversions))
                    }
                }
                OutlinedButton(onClick = onExitCompare) {
                    Text(stringResource(R.string.action_back))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        availableInversions.forEach { inversion ->
            val voicings = grouped[inversion] ?: return@forEach
            val bestVoicing = voicings.first()
            val bassPc = ChordInfo.bassPitchClass(bestVoicing.frets, tuning)
            val bassName = Notes.pitchClassToName(bassPc)

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = inversion.localizedLabel(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.chord_library_bass_prefix) + bassName + " • " +
                                if (voicings.size == 1) stringResource(R.string.chord_library_voicing_singular, voicings.size)
                                else stringResource(R.string.chord_library_voicings, voicings.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (onPlayVoicing != null) {
                        IconButton(onClick = { onPlayVoicing(bestVoicing) }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_item, inversion.localizedLabel()),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(voicings) { voicing ->
                        val bassIndex = ChordInfo.findBassStringIndex(voicing.frets, tuning)
                        VerticalChordDiagram(
                            voicing = voicing,
                            onClick = { onVoicingSelected(voicing) },
                            leftHanded = leftHanded,
                            inversionLabel = null,
                            bassStringIndex = bassIndex,
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }
        }

        if (availableInversions.size == 1) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.chord_library_reentrant_limited,
                        grouped[availableInversions.first()]?.size ?: 0,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

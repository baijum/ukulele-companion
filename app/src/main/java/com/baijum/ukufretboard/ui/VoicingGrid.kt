package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.UkuleleString

@Composable
internal fun VoicingGrid(
    voicings: List<ChordVoicing>,
    tuning: List<UkuleleString>,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onVoicingLongPressed: ((ChordVoicing) -> Unit)? = null,
    isFavorite: ((ChordVoicing) -> Boolean)? = null,
    onFavoriteClick: ((ChordVoicing) -> Unit)? = null,
    leftHanded: Boolean = false,
    rootPitchClass: Int = 0,
    formula: ChordFormula? = null,
    emptyTitle: String = "No voicings found",
    emptySubtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    if (voicings.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = emptyTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (emptySubtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = emptySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val chordName = Notes.pitchClassToName(rootPitchClass) + (formula?.symbol ?: "")

            items(voicings) { voicing ->
                val inversionLabel = formula?.let {
                    ChordInfo.determineInversion(voicing.frets, rootPitchClass, it, tuning).localizedLabel()
                }
                val bassIndex = formula?.let {
                    ChordInfo.findBassStringIndex(voicing.frets, tuning)
                }

                VerticalChordDiagram(
                    voicing = voicing,
                    onClick = { onVoicingSelected(voicing) },
                    onLongClick = onVoicingLongPressed?.let { callback -> { callback(voicing) } },
                    chordName = chordName,
                    leftHanded = leftHanded,
                    inversionLabel = inversionLabel,
                    bassStringIndex = bassIndex,
                    isFavorite = isFavorite?.invoke(voicing) == true,
                    onFavoriteClick = onFavoriteClick?.let { callback -> { callback(voicing) } },
                )
            }
        }
    }
}

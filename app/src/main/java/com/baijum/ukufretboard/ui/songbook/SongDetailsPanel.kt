package com.baijum.ukufretboard.ui.songbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import com.baijum.ukufretboard.domain.KeyDetector
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.ReduceMotionTransitions
import com.baijum.ukufretboard.ui.VerticalChordDiagram

private val TOGGLE_HEIGHT = 48.dp

/**
 * Wrapper for everything the song viewer shows between the toolbar and the lyrics:
 * subtitle, artist, key, capo, strum pattern, labels, statistics, transpose controls,
 * section shortcuts, tempo and the chord diagram rail.
 *
 * Grouped into one collapsible block because while actually playing a song all of it
 * has already been read and only costs the vertical space the lyrics need. Toggled by
 * [SongDetailsToggle].
 */
@Composable
internal fun CollapsibleDetails(
    visible: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = ReduceMotionTransitions.enter(expandVertically()),
        exit = ReduceMotionTransitions.exit(shrinkVertically()),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/**
 * The handle that collapses and expands [CollapsibleDetails].
 *
 * Placed directly above the panel, matching the disclosure rows in `DrawerContent`.
 * It cannot go below the panel: [SheetViewer] lays these out in a plain `Column`, so
 * on a short screen a details block taller than the viewport leaves every later child
 * measured against `maxHeight = 0` — the handle would collapse to nothing exactly on
 * the screens where a reader most needs to reach it.
 *
 * It is not in the toolbar either: that row already carries six icon buttons, and a
 * seventh overflows it on a 360dp screen before the title gets any width at all.
 */
@Composable
internal fun SongDetailsToggle(
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        if (collapsed) {
            stringResource(R.string.songbook_expand_details)
        } else {
            stringResource(R.string.songbook_collapse_details)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TOGGLE_HEIGHT)
                .clickable(role = Role.Button, onClick = onToggle),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val chevron = if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess
        Icon(
            imageVector = chevron,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SongMetaLines(
    sheet: ChordSheet,
    displayContent: String,
    transposeSemitones: Int,
) {
    if (sheet.subtitle.isNotEmpty()) {
        Text(
            text = sheet.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
        )
    }

    if (sheet.artist.isNotEmpty()) {
        Text(
            text = sheet.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
        )
    }

    // Key display. A key stored on the sheet (from a ChordPro {key: ...} directive
    // or typed in the editor) is authoritative and wins over the detector's guess;
    // the detector only fills in when the song does not declare one. Both are keyed
    // off displayContent so a transpose preview updates them together.
    val songChords = remember(displayContent) { ChordParser.extractChords(displayContent) }
    val detectedKey = remember(songChords) { KeyDetector.detectKey(songChords) }
    val storedKey =
        remember(sheet.key, transposeSemitones) {
            ChordSheetTranspose.transposeKey(sheet.key, transposeSemitones)
        }
    val keyLabel = storedKey.ifBlank { detectedKey?.displayName.orEmpty() }

    if (keyLabel.isNotEmpty()) {
        Text(
            text = stringResource(R.string.songbook_key_prefix) + keyLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .padding(start = 48.dp, bottom = 4.dp)
                    // The key now tracks a transpose preview, so it changes under the
                    // user's fingers; without this the new key is never spoken.
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    if (sheet.capo > 0) {
        Text(
            text = stringResource(R.string.songbook_capo_value, sheet.capo),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
        )
    }
}

@Composable
internal fun TransposeRow(
    transposeSemitones: Int,
    onTransposeChange: (Int) -> Unit,
) {
    val transposeDownDesc = stringResource(R.string.cd_transpose_down)
    val transposeUpDesc = stringResource(R.string.cd_transpose_up)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.songbook_transpose),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = { onTransposeChange(transposeSemitones - 1) },
            modifier = Modifier.semantics { contentDescription = transposeDownDesc },
        ) {
            Text("−")
        }
        Text(
            text = ChordSheetTranspose.semitoneLabel(transposeSemitones),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        OutlinedButton(
            onClick = { onTransposeChange(transposeSemitones + 1) },
            modifier = Modifier.semantics { contentDescription = transposeUpDesc },
        ) {
            Text("+")
        }
        if (transposeSemitones != 0) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { onTransposeChange(0) }) {
                Text(stringResource(R.string.dialog_reset))
            }
        }
    }
}

/** Capo equivalent and "Save in this key", shown only while a transpose is previewed. */
@Composable
internal fun TransposeApplyRow(
    transposeSemitones: Int,
    onSaveInKey: () -> Unit,
) {
    val capoFret = ((transposeSemitones % 12) + 12) % 12
    if (capoFret > 0) {
        Text(
            text = stringResource(R.string.songbook_capo_hint, capoFret),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        FilledTonalButton(onClick = onSaveInKey) {
            Text(stringResource(R.string.songbook_save_in_key))
        }
    }
}

@Composable
internal fun SectionShortcutsRow(
    sections: List<ChordSheetFormatter.SectionMarker>,
    onSectionSelected: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sections.forEach { section ->
            AssistChip(
                onClick = { onSectionSelected(section.lineIndex) },
                label = { Text(section.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
internal fun TempoRow(
    songTempo: Int,
    onStartMetronome: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.songbook_tempo_label, songTempo),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(onClick = { onStartMetronome(songTempo) }) {
            Text(stringResource(R.string.songbook_start_metronome))
        }
    }
}

@Composable
internal fun ChordDiagramRail(
    displayContent: String,
    tuning: List<UkuleleString>,
    leftHanded: Boolean,
    onChordTap: (String) -> Unit,
) {
    val uniqueChords =
        remember(displayContent) {
            ChordParser.extractChords(displayContent)
        }
    val chordVoicings =
        remember(uniqueChords, tuning) {
            uniqueChords.mapNotNull { name ->
                val parsed = ChordNameParser.parse(name) ?: return@mapNotNull null
                if (tuning.isEmpty()) return@mapNotNull null
                val voicing =
                    VoicingGenerator
                        .generate(
                            parsed.rootPitchClass,
                            parsed.formula,
                            tuning,
                        ).firstOrNull() ?: return@mapNotNull null
                name to voicing
            }
        }
    if (chordVoicings.isEmpty()) return

    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(chordVoicings.size, key = { chordVoicings[it].first }) { idx ->
            val (name, voicing) = chordVoicings[idx]
            VerticalChordDiagram(
                voicing = voicing,
                onClick = { onChordTap(name) },
                chordName = name,
                leftHanded = leftHanded,
            )
        }
    }
    HorizontalDivider()
}

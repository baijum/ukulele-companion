package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.VerticalChordDiagram

/**
 * The bottom sheet shown after tapping a chord name in a song: its diagram, a play
 * action, and a jump to the chord library.
 *
 * Hoisted out of [SheetViewer] so fullscreen can show it too — the tap target only
 * became reachable there once fullscreen started rendering parsed chords (issue #520).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChordDetailSheet(
    chordName: String,
    tuning: List<UkuleleString>,
    leftHanded: Boolean,
    onPlayChord: (String) -> Unit,
    onViewInLibrary: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val chordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val voicing =
        remember(chordName, tuning) {
            val parsed = ChordNameParser.parse(chordName) ?: return@remember null
            if (tuning.isEmpty()) return@remember null
            VoicingGenerator
                .generate(parsed.rootPitchClass, parsed.formula, tuning)
                .firstOrNull()
        }

    val playChordDesc = stringResource(R.string.songbook_play_chord, chordName)
    val viewInLibraryDesc = stringResource(R.string.songbook_view_in_library, chordName)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = chordSheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chordName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .padding(bottom = 16.dp)
                        .semantics { heading() },
            )

            if (voicing != null) {
                VerticalChordDiagram(
                    voicing = voicing,
                    onClick = {},
                    chordName = chordName,
                    leftHanded = leftHanded,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.songbook_play_chord, chordName)) },
                leadingContent = {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                },
                modifier =
                    Modifier
                        .clickable(role = Role.Button) {
                            onPlayChord(chordName)
                        }.semantics { contentDescription = playChordDesc },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.songbook_view_in_library, chordName)) },
                leadingContent = {
                    Icon(Icons.Filled.MusicNote, contentDescription = null)
                },
                modifier =
                    Modifier
                        .clickable(role = Role.Button) {
                            onViewInLibrary(chordName)
                        }.semantics { contentDescription = viewInLibraryDesc },
            )
        }
    }
}

package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.Transpose

/**
 * Compact transpose controls showing the current chord name with +/- semitone buttons.
 *
 * When the user taps + or -, the [onTranspose] callback is invoked with +1 or -1
 * so the parent can shift the selected root note accordingly.
 *
 * @param rootPitchClass The current root pitch class (0–11).
 * @param chordSymbol The chord quality symbol (e.g., "m7", "sus2").
 * @param semitoneOffset The current transpose offset from the original root.
 * @param originalRoot The original root pitch class before transposition.
 * @param onTranspose Callback with +1 or -1 when the user taps the buttons.
 */
@Composable
fun TransposeControls(
    rootPitchClass: Int,
    chordSymbol: String,
    semitoneOffset: Int,
    originalRoot: Int,
    onTranspose: (Int) -> Unit,
) {
    val currentName = Notes.pitchClassToName(rootPitchClass) + chordSymbol
    val originalName = Notes.pitchClassToName(originalRoot) + chordSymbol
    val showOriginal = semitoneOffset != 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(onClick = { onTranspose(-1) }) {
            Text("−")
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (showOriginal) {
            Text(
                text = "$originalName${stringResource(R.string.transpose_arrow)}$currentName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = currentName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        FilledTonalButton(onClick = { onTranspose(1) }) {
            Text("+")
        }

        if (showOriginal) {
            Spacer(modifier = Modifier.width(8.dp))
            val capo = Transpose.capoFret(originalRoot, rootPitchClass)
            if (capo > 0) {
                Text(
                    text = stringResource(R.string.transpose_capo_prefix) + "$capo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

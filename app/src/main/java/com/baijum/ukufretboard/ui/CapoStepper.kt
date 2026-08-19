package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.viewmodel.FretboardViewModel

/** Widest the value label needs before "Off" or a two-digit fret starts to clip. */
private val VALUE_MIN_WIDTH = 40.dp

/** Alpha applied to a stepper glyph once its button is disabled. */
private const val DISABLED_GLYPH_ALPHA = 0.3f

/**
 * Minus / value / plus stepper for a capo position, `0..maxFret`.
 *
 * Shared by the Explorer and the song editor. The Explorer had its own copy that
 * announced nothing after a press, read the value as a bare number, and clipped
 * longer translations of "Off" in a fixed 32.dp box (issue #523).
 *
 * Callers supply their own "Capo" label — the two screens place it differently.
 */
@Composable
internal fun CapoStepper(
    capo: Int,
    onCapoChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxFret: Int = FretboardViewModel.LAST_FRET,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val decreaseCapoDescription = stringResource(R.string.cd_decrease_capo)
        IconButton(
            onClick = { onCapoChange((capo - 1).coerceAtLeast(0)) },
            enabled = capo > 0,
            modifier = Modifier.semantics { contentDescription = decreaseCapoDescription },
        ) {
            StepperGlyph(glyph = "−", enabled = capo > 0)
        }

        // TalkBack reads the value as "Capo 3" rather than a bare "3", which carries
        // no meaning once focus has moved off the stepper's buttons.
        val capoValueDescription =
            if (capo == 0) {
                stringResource(R.string.capo_calc_no_capo)
            } else {
                stringResource(R.string.songbook_capo_value, capo)
            }
        Text(
            text = if (capo == 0) stringResource(R.string.explorer_capo_off) else "$capo",
            style = MaterialTheme.typography.titleMedium,
            color =
                if (capo > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    // A fixed width clips "Désactivé" / "Выкл.", and clips even "Off"
                    // at the large font scales low-vision users run.
                    .widthIn(min = VALUE_MIN_WIDTH)
                    .semantics {
                        contentDescription = capoValueDescription
                        // Focus stays on the +/- button after a press, so without this
                        // the new value is never spoken and the stepper is silent to
                        // TalkBack — including when a button silently disables itself
                        // at 0 or at maxFret.
                        liveRegion = LiveRegionMode.Polite
                    },
        )

        val increaseCapoDescription = stringResource(R.string.cd_increase_capo)
        IconButton(
            onClick = { onCapoChange((capo + 1).coerceAtMost(maxFret)) },
            enabled = capo < maxFret,
            modifier = Modifier.semantics { contentDescription = increaseCapoDescription },
        ) {
            StepperGlyph(glyph = "+", enabled = capo < maxFret)
        }
    }
}

/** The +/- label of a stepper button, dimmed when the button is disabled. */
@Composable
private fun StepperGlyph(
    glyph: String,
    enabled: Boolean,
) {
    Text(
        text = glyph,
        style = MaterialTheme.typography.titleLarge,
        color =
            if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_GLYPH_ALPHA)
            },
    )
}

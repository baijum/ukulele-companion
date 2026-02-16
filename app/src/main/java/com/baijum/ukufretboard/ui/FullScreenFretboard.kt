package com.baijum.ukufretboard.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import kotlinx.coroutines.delay

/**
 * Full-screen landscape fretboard view.
 *
 * Forces landscape orientation and enters immersive mode on entry,
 * restoring the original orientation and system bars on exit.
 *
 * The fretboard cells are dynamically sized to fill the available width
 * so all 13 fret columns (0–12) are visible without scrolling.
 *
 * A semi-transparent overlay at the bottom shows:
 * - Detected chord name (or status)
 * - Play, Reset, Show/Hide Notes buttons
 * - Exit (X) button
 *
 * The overlay auto-hides after a few seconds; tapping the fretboard
 * area toggles it back.
 *
 * @param viewModel The shared [FretboardViewModel] — state is preserved
 *   when entering/exiting full-screen mode.
 * @param soundEnabled Whether sound playback is enabled.
 * @param leftHanded Whether to mirror the fretboard for left-handed players.
 * @param onExit Callback to exit full-screen mode.
 */
@Composable
fun FullScreenFretboard(
    viewModel: FretboardViewModel,
    soundEnabled: Boolean,
    leftHanded: Boolean = false,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Force landscape + immersive mode
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Enter immersive mode — hide system bars
        val window = activity?.window
        val insetsController = window?.insetsController
        insetsController?.let {
            it.hide(android.view.WindowInsets.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            // Restore orientation
            activity?.requestedOrientation =
                originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // Show system bars again
            insetsController?.show(android.view.WindowInsets.Type.systemBars())
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    // Overlay visibility with auto-hide
    var showOverlay by remember { mutableStateOf(true) }

    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(4000)
            showOverlay = false
        }
    }

    // Chord name from detection result
    val chordLabel = when (val result = uiState.detectionResult) {
        is ChordDetector.DetectionResult.NoSelection -> ""
        is ChordDetector.DetectionResult.SingleNote -> result.note.name
        is ChordDetector.DetectionResult.Interval ->
            result.notes.joinToString(" ") { it.name }
        is ChordDetector.DetectionResult.ChordFound -> result.result.name
        is ChordDetector.DetectionResult.NoMatch -> "?"
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                showOverlay = !showOverlay
            },
    ) {
        // Dynamic cell sizing: fill available width with all fret columns
        val fretCount = uiState.lastFret + 1 // frets 0 through lastFret
        val availableWidth = maxWidth - FRETBOARD_LABEL_WIDTH - 8.dp // label + padding
        val dynamicCellWidth = availableWidth / fretCount
        // Scale height proportionally but cap to available height
        val headerHeight = 40.dp // fret numbers + markers
        val availableHeight = maxHeight - headerHeight
        val stringCount = viewModel.tuning.size
        val dynamicCellHeight = if (stringCount > 0) {
            minOf(availableHeight / stringCount, dynamicCellWidth)
        } else {
            dynamicCellWidth
        }

        // Fretboard fills the screen
        FretboardView(
            tuning = viewModel.tuning,
            selections = uiState.selections,
            showNoteNames = uiState.showNoteNames,
            onFretTap = { stringIndex, fret ->
                viewModel.toggleFret(stringIndex, fret)
                showOverlay = true
            },
            getNoteAt = viewModel::getNoteAt,
            leftHanded = leftHanded,
            scaleNotes = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.scaleNotes else emptySet(),
            scaleRoot = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.root else null,
            capoFret = uiState.capoFret,
            lastFret = uiState.lastFret,
            cellWidth = dynamicCellWidth,
            cellHeight = dynamicCellHeight,
            scrollable = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
        )

        // Bottom overlay controls
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Chord name
                Text(
                    text = chordLabel.ifEmpty { stringResource(R.string.explorer_tap_frets) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (chordLabel.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Play button
                    if (soundEnabled && uiState.detectionResult is ChordDetector.DetectionResult.ChordFound) {
                        IconButton(onClick = { viewModel.playChord() }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_chord),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Reset
                    TextButton(onClick = {
                        viewModel.clearAll()
                        showOverlay = true
                    }) {
                        Text(stringResource(R.string.full_screen_reset))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Exit full screen
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_exit_full_screen),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

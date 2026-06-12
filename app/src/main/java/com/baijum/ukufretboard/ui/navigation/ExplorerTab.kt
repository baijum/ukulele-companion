package com.baijum.ukufretboard.ui.navigation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.ui.localizedLabel
import com.baijum.ukufretboard.ui.ChordResultView
import com.baijum.ukufretboard.ui.FretboardView
import com.baijum.ukufretboard.ui.ScaleSelector
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.SettingsViewModel

/**
 * Content for the Explorer tab — the original interactive fretboard UI.
 */
@Composable
internal fun ExplorerTabContent(
    viewModel: FretboardViewModel,
    settingsViewModel: SettingsViewModel,
    soundEnabled: Boolean,
    leftHanded: Boolean = false,
    showTips: Boolean = false,
    showDidYouKnow: Boolean = true,
    onDismissTips: () -> Unit = {},
    onFullScreen: () -> Unit = {},
    onShareChord: ((ChordVoicing, String, String?) -> Unit)? = null,
    onShowInLibrary: ((rootPitchClass: Int, formula: com.baijum.ukufretboard.data.ChordFormula) -> Unit)? = null,
    isTabletWidth: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTuning = uiState.tuning.ifEmpty { viewModel.tuning }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (showTips) {
            ExplorerTipsCard(onDismiss = onDismissTips)
        }

        ScaleSelector(
            state = uiState.scaleOverlay,
            tuningPitchClasses = currentTuning.map { it.openPitchClass },
            onRootChanged = viewModel::setScaleRoot,
            onScaleChanged = viewModel::setScale,
            onToggle = viewModel::toggleScaleOverlay,
            onPositionChanged = { position ->
                viewModel.setScalePositionRange(position?.fretRange)
            },
            onChordTapped = { chord ->
                val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
                    .firstOrNull { it.symbol == chord.quality }
                if (formula != null) {
                    val voicings = com.baijum.ukufretboard.data.VoicingGenerator.generate(
                        rootPitchClass = chord.rootPitchClass,
                        formula = formula,
                        tuning = currentTuning,
                    )
                    voicings.firstOrNull()?.let { viewModel.applyVoicing(it) }
                }
            },
        )

        val fretboardCellSize = if (isTabletWidth) 64.dp else 48.dp
        FretboardView(
            tuning = currentTuning,
            selections = uiState.selections,
            showNoteNames = uiState.showNoteNames,
            onFretTap = viewModel::toggleFret,
            getNoteAt = viewModel::getNoteAt,
            leftHanded = leftHanded,
            scaleNotes = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.scaleNotes else emptySet(),
            scaleRoot = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.root else null,
            scalePositionFretRange = if (uiState.scaleOverlay.enabled) uiState.scaleOverlay.positionFretRange else null,
            capoFret = uiState.capoFret,
            lastFret = uiState.lastFret,
            cellWidth = fretboardCellSize,
            cellHeight = fretboardCellSize,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
        )

        CapoSelector(
            capoFret = uiState.capoFret,
            lastFret = uiState.lastFret,
            onCapoChange = viewModel::setCapoFret,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(
                space = 12.dp,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            OutlinedButton(onClick = viewModel::clearAll) {
                Text(stringResource(R.string.explorer_reset))
            }
            OutlinedButton(onClick = viewModel::toggleScaleOverlay) {
                Text(if (uiState.scaleOverlay.enabled) stringResource(R.string.explorer_hide_scale) else stringResource(R.string.explorer_scales))
            }
            IconButton(onClick = onFullScreen) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.cd_full_screen),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val fretsList = uiState.selections.entries
            .sortedBy { it.key }
            .map { it.value ?: ChordVoicing.MUTED }

        val shareCallback: (() -> Unit)? = if (
            onShareChord != null &&
            uiState.detectionResult is com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
        ) {
            val chordFound = uiState.detectionResult as com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
            val chordResult = chordFound.result
            val formula = chordResult.matchedFormula
            val capoAdjustedFrets = fretsList.map { it + uiState.capoFret }
            val inversion = if (capoAdjustedFrets.size == 4 && formula != null) {
                ChordInfo.determineInversion(capoAdjustedFrets, chordResult.root.pitchClass, formula, currentTuning)
            } else null
            val invLabel = if (inversion != null && inversion != ChordInfo.Inversion.ROOT) inversion.localizedLabel() else null
            val displayName = if (inversion != null && inversion != ChordInfo.Inversion.ROOT) {
                val bassPc = ChordInfo.bassPitchClass(capoAdjustedFrets, currentTuning)
                ChordInfo.slashNotation(chordResult.name, inversion, bassPc)
            } else {
                chordResult.name
            }
            val shareVoicing = ChordVoicing(
                frets = fretsList,
                notes = chordResult.notes,
                minFret = fretsList.filter { it > 0 }.minOrNull() ?: 0,
                maxFret = fretsList.maxOrNull() ?: 0,
            );
            { onShareChord(shareVoicing, displayName, invLabel) }
        } else {
            null
        }

        val showInLibraryCallback: (() -> Unit)? = if (
            onShowInLibrary != null &&
            uiState.detectionResult is com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
        ) {
            val chordFound = uiState.detectionResult as com.baijum.ukufretboard.domain.ChordDetector.DetectionResult.ChordFound
            val formula = chordFound.result.matchedFormula
            if (formula != null) {
                { onShowInLibrary(chordFound.result.root.pitchClass, formula) }
            } else null
        } else null

        ChordResultView(
            detectionResult = uiState.detectionResult,
            fingerPositions = uiState.fingerPositions,
            onPlayChord = viewModel::playChord,
            soundEnabled = soundEnabled,
            frets = fretsList,
            tuning = currentTuning,
            capoFret = uiState.capoFret,
            onShareChord = shareCallback,
            onShowInLibrary = showInLibraryCallback,
            onAlternateChordTapped = if (onShowInLibrary != null) {
                { alt -> onShowInLibrary(alt.rootPitchClass, alt.formula) }
            } else null,
            onSuggestedChordTapped = { rootPitchClass, symbol ->
                val formula = com.baijum.ukufretboard.data.ChordFormulas.ALL
                    .firstOrNull { it.symbol == symbol }
                if (formula != null) {
                    val voicings = com.baijum.ukufretboard.data.VoicingGenerator.generate(
                        rootPitchClass = rootPitchClass,
                        formula = formula,
                        tuning = currentTuning,
                    )
                    voicings.firstOrNull()?.let { viewModel.applyVoicing(it) }
                }
            },
            showDidYouKnow = showDidYouKnow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExplorerTipsCard(onDismiss: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.explorer_tips_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            TipRow(
                icon = Icons.Filled.TouchApp,
                text = stringResource(R.string.explorer_tips_tap),
            )
            TipRow(
                icon = Icons.Filled.MusicNote,
                text = stringResource(R.string.explorer_tips_scales),
            )
            TipRow(
                icon = Icons.Filled.Equalizer,
                text = stringResource(R.string.explorer_tips_capo),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.explorer_tips_dismiss))
                }
            }
        }
    }
}

@Composable
private fun TipRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Compact capo position selector with minus/plus buttons and a fret label.
 */
@Composable
internal fun CapoSelector(
    capoFret: Int,
    lastFret: Int = FretboardViewModel.LAST_FRET,
    onCapoChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.label_capo),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = { onCapoChange((capoFret - 1).coerceAtLeast(0)) },
            enabled = capoFret > 0,
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.titleLarge,
                color = if (capoFret > 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
        Text(
            text = if (capoFret == 0) stringResource(R.string.explorer_capo_off) else "$capoFret",
            style = MaterialTheme.typography.titleMedium,
            color = if (capoFret > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(
            onClick = { onCapoChange((capoFret + 1).coerceAtMost(lastFret)) },
            enabled = capoFret < lastFret,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                color = if (capoFret < lastFret)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

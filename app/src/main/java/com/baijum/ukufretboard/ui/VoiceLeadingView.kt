package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.VoiceLeading
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * Dedicated view for exploring voice leading through a chord progression.
 *
 * Shows optimal voicing transitions step-by-step with:
 * - A chord timeline highlighting the current transition pair
 * - Side-by-side chord diagrams with common tone highlighting
 * - Per-string movement breakdown
 * - Audio playback for individual chords and the full progression
 *
 * @param path The computed optimal voice leading path.
 * @param tuning Current ukulele tuning (for string name labels).
 * @param onBack Callback to exit the voice leading view.
 * @param onPlayVoicing Callback to play a single voicing (null if sound disabled).
 * @param onPlayAll Callback to play all voicings in sequence (null if sound disabled).
 * @param leftHanded Whether to mirror diagrams for left-handed players.
 * @param modifier Optional modifier.
 */
@Composable
fun VoiceLeadingView(
    path: VoiceLeading.Path,
    tuning: List<UkuleleString>,
    onBack: () -> Unit,
    onPlayVoicing: ((ChordVoicing) -> Unit)?,
    onPlayAll: ((List<ChordVoicing>) -> Unit)?,
    leftHanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val hasTransitions = path.transitions.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        // Header
        VoiceLeadingHeader(
            path = path,
            onBack = onBack,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Chord timeline
        ChordTimeline(
            steps = path.steps,
            currentStep = currentStep,
            hasTransitions = hasTransitions,
            onStepSelected = { i ->
                if (i < path.transitions.size) {
                    currentStep = i
                } else if (i > 0) {
                    currentStep = i - 1
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (hasTransitions) {
                val fromStep = path.steps[currentStep]
                val toStep = path.steps[currentStep + 1]
                val transition = path.transitions[currentStep]

                // Two diagrams side by side
                TransitionDiagrams(
                    fromStep = fromStep,
                    toStep = toStep,
                    transition = transition,
                    leftHanded = leftHanded,
                    onPlayFrom = onPlayVoicing?.let { play -> { play(fromStep.voicing) } },
                    onPlayTo = onPlayVoicing?.let { play -> { play(toStep.voicing) } },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Transition details card
                TransitionSummaryCard(
                    transition = transition,
                    fromVoicing = fromStep.voicing,
                    toVoicing = toStep.voicing,
                    tuning = tuning,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Total path summary
                TotalPathSummary(path = path)
            } else {
                // Single chord — no transitions to show
                Text(
                    text = stringResource(R.string.voice_leading_single_chord),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                VerticalChordDiagram(
                    voicing = path.steps[0].voicing,
                    onClick = { onPlayVoicing?.invoke(path.steps[0].voicing) },
                    leftHanded = leftHanded,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom navigation controls
        if (hasTransitions) {
            HorizontalDivider()
            BottomControls(
                currentStep = currentStep,
                totalTransitions = path.transitions.size,
                onPrev = { if (currentStep > 0) currentStep-- },
                onNext = { if (currentStep < path.transitions.size - 1) currentStep++ },
                onPlayAll = onPlayAll?.let { play ->
                    { play(path.steps.map { it.voicing }) }
                },
            )
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────

@Composable
private fun VoiceLeadingHeader(
    path: VoiceLeading.Path,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back_progressions),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = path.progressionName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.voice_leading_title, Notes.enharmonicForKey(path.keyRoot, path.keyRoot)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Chord Timeline ──────────────────────────────────────────────────────

/**
 * Horizontal row of chord names showing the full progression.
 * The current "from" chord has a primary background and the "to" chord
 * has a secondary background, making the active transition pair obvious.
 */
@Composable
private fun ChordTimeline(
    steps: List<VoiceLeading.Step>,
    currentStep: Int,
    hasTransitions: Boolean,
    onStepSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        steps.forEachIndexed { i, step ->
            if (i > 0) {
                Text(
                    text = "\u2192", // →
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val isFrom = hasTransitions && i == currentStep
            val isTo = hasTransitions && i == currentStep + 1

            Box(
                modifier = Modifier
                    .background(
                        color = when {
                            isFrom -> MaterialTheme.colorScheme.primaryContainer
                            isTo -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onStepSelected(i) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = step.numeral,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isFrom -> MaterialTheme.colorScheme.onPrimaryContainer
                            isTo -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = step.chordName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isFrom || isTo) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isFrom -> MaterialTheme.colorScheme.onPrimaryContainer
                            isTo -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

// ── Transition Diagrams ─────────────────────────────────────────────────

/**
 * Two chord diagrams side by side with an arrow between them.
 * The "to" diagram highlights common tones in a distinct color.
 */
@Composable
private fun TransitionDiagrams(
    fromStep: VoiceLeading.Step,
    toStep: VoiceLeading.Step,
    transition: VoiceLeading.TransitionInfo,
    leftHanded: Boolean,
    onPlayFrom: (() -> Unit)?,
    onPlayTo: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        // "From" diagram
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = fromStep.numeral,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalChordDiagram(
                voicing = fromStep.voicing,
                onClick = { onPlayFrom?.invoke() },
                leftHanded = leftHanded,
            )
            if (onPlayFrom != null) {
                IconButton(
                    onClick = onPlayFrom,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_item, fromStep.chordName),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.cd_to),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 60.dp)
                .size(24.dp),
        )

        // "To" diagram with common tone highlighting
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = toStep.numeral,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalChordDiagram(
                voicing = toStep.voicing,
                onClick = { onPlayTo?.invoke() },
                leftHanded = leftHanded,
                commonToneIndices = transition.commonToneIndices,
            )
            if (onPlayTo != null) {
                IconButton(
                    onClick = onPlayTo,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_item, toStep.chordName),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ── Transition Summary ──────────────────────────────────────────────────

/**
 * Card showing per-string movement details for a transition.
 * Common tones are highlighted with a colored dot and descriptive text.
 * Moving fingers show the fret change with directional arrows.
 */
@Composable
private fun TransitionSummaryCard(
    transition: VoiceLeading.TransitionInfo,
    fromVoicing: ChordVoicing,
    toVoicing: ChordVoicing,
    tuning: List<UkuleleString>,
) {
    val commonCount = transition.commonToneIndices.size
    val commonLabel = if (commonCount != 1)
        stringResource(R.string.voice_leading_common_tones_plural, commonCount)
    else
        stringResource(R.string.voice_leading_common_tones, commonCount)
    val distLabel = if (transition.totalDistance != 1)
        stringResource(R.string.voice_leading_frets_movement, transition.totalDistance)
    else
        stringResource(R.string.voice_leading_fret_movement, transition.totalDistance)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Summary headline
            Text(
                text = "$commonLabel \u00B7 $distLabel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Per-string breakdown
            tuning.forEachIndexed { i, string ->
                val fromFret = fromVoicing.frets[i]
                val toFret = toVoicing.frets[i]
                val movement = transition.movements[i]
                val isCommon = i in transition.commonToneIndices

                val fromNoteName = if (fromFret == ChordVoicing.MUTED) "x"
                    else Notes.pitchClassToName(
                        (string.openPitchClass + fromFret) % Notes.PITCH_CLASS_COUNT
                    )
                val toNoteName = if (toFret == ChordVoicing.MUTED) "x"
                    else Notes.pitchClassToName(
                        (string.openPitchClass + toFret) % Notes.PITCH_CLASS_COUNT
                    )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // String name label
                    Text(
                        text = "${string.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp),
                    )

                    if (isCommon) {
                        // Common tone — finger stays put
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondary,
                                    CircleShape,
                                ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val fretLabel = when (fromFret) {
                            ChordVoicing.MUTED -> stringResource(R.string.voice_leading_muted)
                            0 -> stringResource(R.string.label_open)
                            else -> stringResource(R.string.voice_leading_fret_prefix) + "$fromFret"
                        }
                        Text(
                            text = stringResource(R.string.voice_leading_stays) + "$fretLabel ($fromNoteName)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    } else {
                        // Moving finger
                        val fromLabel = when (fromFret) {
                            ChordVoicing.MUTED -> stringResource(R.string.voice_leading_muted)
                            0 -> stringResource(R.string.label_open)
                            else -> stringResource(R.string.voice_leading_fret_prefix) + "$fromFret"
                        }
                        val toLabel = when (toFret) {
                            ChordVoicing.MUTED -> stringResource(R.string.voice_leading_muted)
                            0 -> stringResource(R.string.label_open)
                            else -> stringResource(R.string.voice_leading_fret_prefix) + "$toFret"
                        }
                        val arrow = when {
                            movement > 0 -> "\u2191$movement"  // ↑
                            movement < 0 -> "\u2193${-movement}" // ↓
                            else -> ""
                        }
                        Text(
                            text = "$fromLabel ($fromNoteName)" + stringResource(R.string.voice_leading_arrow) + "$toLabel ($toNoteName)  $arrow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Educational tip
            if (transition.commonToneIndices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val commonStrings = transition.commonToneIndices
                    .sorted()
                    .joinToString(", ") { tuning[it].name }
                val plural = transition.commonToneIndices.size > 1
                Text(
                    text = if (plural)
                        stringResource(R.string.voice_leading_tip_plural, commonStrings)
                    else
                        stringResource(R.string.voice_leading_tip_singular, commonStrings),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ── Total Path Summary ──────────────────────────────────────────────────

@Composable
private fun TotalPathSummary(path: VoiceLeading.Path) {
    val transCount = path.transitions.size
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.voice_leading_total_path, path.totalDistance, transCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Bottom Controls ─────────────────────────────────────────────────────

@Composable
private fun BottomControls(
    currentStep: Int,
    totalTransitions: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlayAll: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onPrev,
                enabled = currentStep > 0,
            ) {
                Text("\u25C0 " + stringResource(R.string.voice_leading_prev))
            }

            if (onPlayAll != null) {
                Button(onClick = onPlayAll) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_all_voice_leadings),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.voice_leading_play_all))
                }
            }

            OutlinedButton(
                onClick = onNext,
                enabled = currentStep < totalTransitions - 1,
            ) {
                Text(stringResource(R.string.voice_leading_next) + " \u25B6")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.voice_leading_step, currentStep + 1, totalTransitions),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

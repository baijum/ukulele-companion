package com.baijum.ukufretboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ExplorerTips
import com.baijum.ukufretboard.domain.AlternateChord
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * Displays the chord detection result below the fretboard.
 *
 * Adapts its content based on the detection state:
 * - **No selection**: instructional prompt to tap the fretboard.
 * - **Single note**: shows the note name with "Single note selected" label.
 * - **Interval (2 notes)**: shows both notes with "Incomplete chord" label.
 * - **Chord found**: shows chord name, quality, constituent notes, interval
 *   breakdown, formula, fingering, and difficulty.
 * - **No match**: shows "No exact chord match" with the notes played.
 *
 * @param detectionResult The current [ChordDetector.DetectionResult] to display.
 * @param fingerPositions A string like "0 - 0 - 0 - 3" showing fret positions per string.
 * @param onPlayChord Callback invoked when the user taps the play/strum button.
 * @param soundEnabled Whether sound playback is enabled. When false, the play button is hidden.
 * @param frets The raw fret list (4 values, one per string) for computing fingering
 *   and difficulty. Null when no chord is selected or frets are not available.
 * @param tuning The current ukulele tuning, used for inversion detection.
 * @param onAlternateChordTapped Callback invoked when the user taps an alternate chord
 *   name (e.g., tapping "Am7" when "C6" is the primary). Navigates to that chord's
 *   voicings in the chord library. Null disables the alternate names display.
 * @param modifier Optional [Modifier] for layout customization.
 */
@Composable
fun ChordResultView(
    detectionResult: ChordDetector.DetectionResult,
    fingerPositions: String,
    onPlayChord: () -> Unit,
    soundEnabled: Boolean = true,
    frets: List<Int>? = null,
    tuning: List<UkuleleString> = FretboardViewModel.STANDARD_TUNING,
    capoFret: Int = 0,
    onShareChord: (() -> Unit)? = null,
    onShowInLibrary: (() -> Unit)? = null,
    onAlternateChordTapped: ((AlternateChord) -> Unit)? = null,
    onSuggestedChordTapped: ((rootPitchClass: Int, formulaSymbol: String) -> Unit)? = null,
    showDidYouKnow: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val hasNotes = detectionResult !is ChordDetector.DetectionResult.NoSelection

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (detectionResult) {
            is ChordDetector.DetectionResult.NoSelection -> {
                Text(
                    text = stringResource(R.string.chord_result_tap_fretboard),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onSuggestedChordTapped != null) {
                    SuggestedChordsSection(onChordTapped = onSuggestedChordTapped)
                }
                if (showDidYouKnow) {
                    ExplorerDidYouKnowCard()
                }
            }

            is ChordDetector.DetectionResult.SingleNote -> {
                ChordHeadlineWithPlay(
                    text = detectionResult.note.name,
                    onPlay = onPlayChord,
                    showPlay = soundEnabled,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chord_result_single_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is ChordDetector.DetectionResult.Interval -> {
                val noteNames = detectionResult.notes.joinToString(" \u2013 ") { it.name }
                ChordHeadlineWithPlay(
                    text = noteNames,
                    onPlay = onPlayChord,
                    showPlay = soundEnabled,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chord_result_incomplete),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is ChordDetector.DetectionResult.ChordFound -> {
                val result = detectionResult.result
                val formula = result.matchedFormula

                // Compute inversion if we have frets and a formula
                // Use capo-adjusted frets so inversion/bass calculations
                // reflect the actual sounding pitches
                val invFrets = frets?.map { it + capoFret }
                val inversion = if (invFrets != null && invFrets.size == 4 && formula != null) {
                    ChordInfo.determineInversion(invFrets, result.root.pitchClass, formula, tuning)
                } else {
                    null
                }

                // Show chord name with slash notation for inversions
                val displayName = if (inversion != null && inversion != ChordInfo.Inversion.ROOT) {
                    val bassPc = ChordInfo.bassPitchClass(invFrets!!, tuning)
                    ChordInfo.slashNotation(result.name, inversion, bassPc)
                } else {
                    result.name
                }

                ChordHeadlineWithPlay(
                    text = displayName,
                    onPlay = onPlayChord,
                    showPlay = soundEnabled,
                    onShare = onShareChord,
                    onShowInLibrary = onShowInLibrary,
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Quality + inversion label
                val qualityText = if (inversion != null && inversion != ChordInfo.Inversion.ROOT) {
                    "${result.quality} (${inversion.label})"
                } else {
                    result.quality
                }
                Text(
                    text = qualityText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Alternate chord names (e.g., "Also: Am7" when primary is C6)
                if (result.alternates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.chord_result_also),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        result.alternates.forEachIndexed { index, alt ->
                            if (index > 0) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // Compute slash notation for this alternate interpretation
                            val altDisplayName = if (invFrets != null && invFrets.size == 4) {
                                val altInversion = ChordInfo.determineInversion(
                                    invFrets, alt.rootPitchClass, alt.formula, tuning,
                                )
                                if (altInversion != ChordInfo.Inversion.ROOT) {
                                    val bassPc = ChordInfo.bassPitchClass(invFrets, tuning)
                                    ChordInfo.slashNotation(alt.name, altInversion, bassPc)
                                } else {
                                    alt.name
                                }
                            } else {
                                alt.name
                            }
                            if (onAlternateChordTapped != null) {
                                Text(
                                    text = altDisplayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        onAlternateChordTapped(alt)
                                    },
                                )
                            } else {
                                Text(
                                    text = altDisplayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.notes.joinToString(" \u2013 ") { it.name },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is ChordDetector.DetectionResult.NoMatch -> {
                ChordHeadlineWithPlay(
                    text = stringResource(R.string.chord_result_no_match),
                    onPlay = onPlayChord,
                    showPlay = soundEnabled,
                    isSmall = true,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detectionResult.notes.joinToString(" \u2013 ") { it.name },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Finger positions (shown for all states except NoSelection)
        if (hasNotes) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = fingerPositions,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Detailed chord info (only for ChordFound with a matched formula)
        if (detectionResult is ChordDetector.DetectionResult.ChordFound) {
            val result = detectionResult.result
            val formula = result.matchedFormula
            if (formula != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Intervals
                ChordInfoRow(
                    label = stringResource(R.string.chord_result_intervals),
                    value = ChordInfo.buildIntervalBreakdown(
                        root = result.root,
                        notes = result.notes,
                    ),
                )

                // Formula
                ChordInfoRow(
                    label = stringResource(R.string.chord_result_formula),
                    value = ChordInfo.buildFormulaString(formula),
                )

                // Fingering and inversion (requires frets)
                if (frets != null && frets.size == 4) {
                    val fingering = ChordInfo.suggestFingering(frets)
                    ChordInfoRow(
                        label = stringResource(R.string.chord_result_fingering),
                        value = ChordInfo.formatFingering(fingering),
                    )

                    // Difficulty
                    ChordInfoRow(
                        label = stringResource(R.string.chord_result_difficulty),
                        value = ChordInfo.rateDifficulty(frets),
                    )

                    // Inversion (use capo-adjusted frets for correct sounding pitch)
                    val detailedInvFrets = frets.map { it + capoFret }
                    val detailedInversion = ChordInfo.determineInversion(
                        detailedInvFrets, result.root.pitchClass, formula, tuning,
                    )
                    val bassNoteName = com.baijum.ukufretboard.data.Notes.pitchClassToName(
                        ChordInfo.bassPitchClass(detailedInvFrets, tuning)
                    )
                    ChordInfoRow(
                        label = stringResource(R.string.chord_result_inversion),
                        value = "${detailedInversion.label} (${stringResource(R.string.chord_result_bass)}$bassNoteName)",
                    )
                }
            }
        }
    }
}

/**
 * A single row in the chord info section with a bold label and a value.
 */
@Composable
private fun ChordInfoRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Displays a chord/note headline with an optional play button to the right.
 *
 * @param text The chord or note name to display.
 * @param onPlay Callback when the play button is tapped.
 * @param showPlay Whether to show the play button (hidden when sound is disabled).
 * @param isSmall Whether to use a smaller text style (for "No match" state).
 */
@Composable
private fun ChordHeadlineWithPlay(
    text: String,
    onPlay: () -> Unit,
    showPlay: Boolean = true,
    isSmall: Boolean = false,
    onShare: (() -> Unit)? = null,
    onShowInLibrary: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = if (isSmall) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.headlineLarge,
            color = if (isSmall) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary,
            textDecoration = if (onShowInLibrary != null) TextDecoration.Underline else null,
            modifier = if (onShowInLibrary != null) Modifier.clickable { onShowInLibrary() }
            else Modifier,
        )
        if (showPlay) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onPlay,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_sound),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        if (onShare != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onShare,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.cd_share_chord_image),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private data class SuggestedChord(
    val name: String,
    val rootPitchClass: Int,
    val formulaSymbol: String,
)

private val BEGINNER_CHORDS = listOf(
    SuggestedChord("C", 0, ""),
    SuggestedChord("Am", 9, "m"),
    SuggestedChord("F", 5, ""),
    SuggestedChord("G7", 7, "7"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestedChordsSection(
    onChordTapped: (rootPitchClass: Int, formulaSymbol: String) -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.explorer_try_chords),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BEGINNER_CHORDS.forEach { chord ->
            FilterChip(
                selected = false,
                onClick = { onChordTapped(chord.rootPitchClass, chord.formulaSymbol) },
                label = { Text(chord.name) },
            )
        }
    }
}

@Composable
private fun ExplorerDidYouKnowCard() {
    val tips = ExplorerTips.ALL
    var tipIndex by rememberSaveable {
        mutableIntStateOf((System.currentTimeMillis() % tips.size).toInt())
    }

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.explorer_did_you_know_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tips[tipIndex],
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { tipIndex = (tipIndex + 1) % tips.size },
            ) {
                Text(stringResource(R.string.explorer_did_you_know_next))
            }
        }
    }
}

package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.LearningStats
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.ScaleCategory
import com.baijum.ukufretboard.data.ScalePracticeSettings
import com.baijum.ukufretboard.data.Scales
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import com.baijum.ukufretboard.viewmodel.PlayDirection
import com.baijum.ukufretboard.viewmodel.FretPosition
import com.baijum.ukufretboard.viewmodel.PlaybackState
import com.baijum.ukufretboard.viewmodel.PracticeMode
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel
import com.baijum.ukufretboard.viewmodel.UkuleleString
import kotlinx.coroutines.launch

/**
 * Scale Practice screen with three modes: Play Along, Scale Quiz, Ear Training.
 *
 * Uses category-based filtering to scope scales by genre/style.
 * Persists quiz and ear training scores via [LearningProgressViewModel].
 *
 * @param viewModel The [ScalePracticeViewModel] managing practice state.
 * @param progressViewModel Optional [LearningProgressViewModel] for persisting stats.
 * @param onSettingsChanged Callback to persist settings when they change.
 * @param tuning Current ukulele tuning for the fretboard diagram in Play Along mode.
 * @param lastFret The highest fret shown on the fretboard (from user settings, 12–22).
 */
@Composable
fun ScalePracticeView(
    viewModel: ScalePracticeViewModel,
    progressViewModel: LearningProgressViewModel? = null,
    onSettingsChanged: (com.baijum.ukufretboard.data.ScalePracticeSettings) -> Unit = {},
    tuning: List<UkuleleString> = FretboardViewModel.STANDARD_TUNING,
    lastFret: Int = 12,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Observe persisted stats for recomposition
    @Suppress("UNUSED_VARIABLE")
    val progressState = progressViewModel?.state?.collectAsState()
    val allTimeQuizStats = progressViewModel?.scalePracticeStats("quiz")
    val allTimeEarStats = progressViewModel?.scalePracticeStats("ear")

    // Stop playback when leaving the screen
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPlayback() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // ── Title ────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.scale_practice_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.scale_practice_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Mode selector ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PracticeMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = {
                        viewModel.setMode(mode)
                        onSettingsChanged(viewModel.currentSettings())
                    },
                    label = {
                        Text(
                            mode.label,
                            fontWeight = if (state.mode == mode) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Category filter ──────────────────────────────────────────
        Text(
            text = stringResource(R.string.label_category),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // "All" chip
            FilterChip(
                selected = state.selectedCategory == null,
                onClick = {
                    viewModel.setCategory(null)
                    onSettingsChanged(viewModel.currentSettings())
                },
                label = {
                    Text(
                        stringResource(R.string.label_all),
                        fontWeight = if (state.selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                ),
            )
            ScaleCategory.entries.forEach { cat ->
                FilterChip(
                    selected = state.selectedCategory == cat,
                    onClick = {
                        viewModel.setCategory(cat)
                        onSettingsChanged(viewModel.currentSettings())
                    },
                    label = {
                        Text(
                            cat.label,
                            fontWeight = if (state.selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Mode-specific content ────────────────────────────────────
        when (state.mode) {
            PracticeMode.PLAY_ALONG -> PlayAlongContent(
                viewModel = viewModel,
                state = state,
                onSettingsChanged = onSettingsChanged,
                tuning = tuning,
                lastFret = lastFret,
            )
            PracticeMode.QUIZ -> QuizContent(
                viewModel = viewModel,
                state = state,
                progressViewModel = progressViewModel,
                allTimeStats = allTimeQuizStats,
            )
            PracticeMode.EAR_TRAINING -> EarTrainingContent(
                viewModel = viewModel,
                state = state,
                progressViewModel = progressViewModel,
                allTimeStats = allTimeEarStats,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Play Along Mode
// ═══════════════════════════════════════════════════════════════════

/**
 * Computes fret positions for a given pitch class across all strings.
 *
 * For each string, finds the lowest fret (0–[maxFret]) that produces the target
 * pitch class. Returns a map of stringIndex to fret number.
 */
private fun fretPositionsForNote(
    pitchClass: Int,
    tuning: List<UkuleleString>,
    maxFret: Int = 12,
): Map<Int, Int> {
    return tuning.mapIndexedNotNull { stringIndex, string ->
        val fret = (pitchClass - string.openPitchClass + Notes.PITCH_CLASS_COUNT) %
            Notes.PITCH_CLASS_COUNT
        if (fret <= maxFret) stringIndex to fret else null
    }.toMap()
}

@Composable
private fun PlayAlongContent(
    viewModel: ScalePracticeViewModel,
    state: com.baijum.ukufretboard.viewmodel.ScalePracticeUiState,
    onSettingsChanged: (com.baijum.ukufretboard.data.ScalePracticeSettings) -> Unit,
    tuning: List<UkuleleString>,
    lastFret: Int,
) {
    val scope = rememberCoroutineScope()

    // Root note selector
    Text(
        text = stringResource(R.string.scale_practice_root),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Notes.NOTE_NAMES_STANDARD.forEachIndexed { index, name ->
            FilterChip(
                selected = index == state.selectedRoot,
                onClick = {
                    viewModel.setRoot(index)
                    onSettingsChanged(viewModel.currentSettings())
                },
                label = {
                    Text(
                        name,
                        fontWeight = if (index == state.selectedRoot) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Scale type selector
    Text(
        text = stringResource(R.string.label_scale),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.availableScales.forEach { scale ->
            FilterChip(
                selected = state.selectedScale == scale,
                onClick = {
                    viewModel.setScale(scale)
                    onSettingsChanged(viewModel.currentSettings())
                },
                label = { Text(scale.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                ),
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Direction selector
    Text(
        text = stringResource(R.string.label_direction),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlayDirection.entries.forEach { dir ->
            FilterChip(
                selected = state.direction == dir,
                onClick = { viewModel.setDirection(dir) },
                label = { Text(dir.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                ),
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Options row: Loop and Fretboard toggles
    Text(
        text = stringResource(R.string.scale_practice_options),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = state.loopPlayback,
            onClick = { viewModel.toggleLoop() },
            label = { Text(stringResource(R.string.scale_practice_loop)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
        FilterChip(
            selected = state.showFretboard,
            onClick = {
                viewModel.toggleFretboard()
                onSettingsChanged(viewModel.currentSettings())
            },
            label = { Text(stringResource(R.string.scale_practice_fretboard)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }

    // Position filter (visible when fretboard is enabled)
    if (state.showFretboard) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.label_position),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FretPosition.entries.forEach { pos ->
                FilterChip(
                    selected = state.fretPosition == pos,
                    onClick = { viewModel.setFretPosition(pos) },
                    label = { Text(pos.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // BPM slider
    Text(
        text = stringResource(R.string.label_bpm_value, state.bpm),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
        value = state.bpm.toFloat(),
        onValueChange = { viewModel.setBpm(it.toInt()) },
        valueRange = ScalePracticeSettings.MIN_BPM.toFloat()..ScalePracticeSettings.MAX_BPM.toFloat(),
        onValueChangeFinished = { onSettingsChanged(viewModel.currentSettings()) },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Scale notes preview
    val rootName = Notes.enharmonicForKey(state.selectedRoot, state.selectedRoot, false)
    val isMinor = state.selectedScale.intervals.size > 2 && state.selectedScale.intervals[2] == 3
    val noteNames = state.selectedScale.intervals.map { interval ->
        val pc = (state.selectedRoot + interval) % Notes.PITCH_CLASS_COUNT
        Notes.enharmonicForKey(pc, state.selectedRoot, isMinor)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$rootName ${state.selectedScale.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = noteNames.joinToString(" – "),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            // Current note indicator during playback
            if (state.playbackState == PlaybackState.PLAYING &&
                state.currentNoteIndex in state.playAlongNotes.indices
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                val currentPc = state.playAlongNotes[state.currentNoteIndex]
                val currentName = Notes.enharmonicForKey(currentPc, state.selectedRoot, isMinor)
                Text(
                    text = "${stringResource(R.string.scale_practice_now)} $currentName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(R.string.scale_practice_note_of, state.currentNoteIndex + 1, state.playAlongNotes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // ── Compact fretboard diagram (optional) ────────────────────────
    if (state.showFretboard) {
        Spacer(modifier = Modifier.height(12.dp))

        // Compute scale pitch classes for the overlay
        val scaleNotes = Scales.scaleNotes(state.selectedRoot, state.selectedScale)

        // During playback, highlight current note positions as selections
        // Filter to only show positions within the selected fret range
        val posRange = state.fretPosition.range(lastFret)
        val selections: Map<Int, Int?> = if (
            state.playbackState == PlaybackState.PLAYING &&
            state.currentNoteIndex in state.playAlongNotes.indices
        ) {
            val currentPc = state.playAlongNotes[state.currentNoteIndex]
            val allPositions = fretPositionsForNote(currentPc, tuning, maxFret = lastFret)
            val filtered = if (posRange != null) {
                allPositions.filter { (_, fret) -> fret in posRange }
            } else {
                allPositions
            }
            // Fall back to all positions if none are in the selected range
            val effective = filtered.ifEmpty { allPositions }
            effective.mapValues { (_, fret) -> fret }
        } else {
            // No active selections when stopped
            tuning.indices.associateWith { null }
        }

        // Local getNoteAt using scale-aware enharmonic naming
        val getNoteAt: (Int, Int) -> Note = { stringIndex, fret ->
            val pc = (tuning[stringIndex].openPitchClass + fret) % Notes.PITCH_CLASS_COUNT
            val name = Notes.enharmonicForKey(pc, state.selectedRoot, isMinor)
            Note(pitchClass = pc, name = name)
        }

        FretboardView(
            tuning = tuning,
            selections = selections,
            showNoteNames = true,
            onFretTap = { _, _ -> }, // read-only
            getNoteAt = getNoteAt,
            scaleNotes = scaleNotes,
            scaleRoot = state.selectedRoot,
            scalePositionFretRange = posRange,
            lastFret = lastFret,
            cellWidth = 42.dp,
            cellHeight = 38.dp,
            scrollable = true,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Play / Stop button
    when (state.playbackState) {
        PlaybackState.STOPPED -> {
            Button(
                onClick = { viewModel.startPlayback(scope) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.action_play),
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.scale_practice_play))
            }
        }
        PlaybackState.PLAYING -> {
            Button(
                onClick = { viewModel.stopPlayback() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.action_stop))
            }
        }
        PlaybackState.PAUSED -> {
            Button(
                onClick = { viewModel.startPlayback(scope) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scale_practice_resume))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Scale Quiz Mode
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun QuizContent(
    viewModel: ScalePracticeViewModel,
    state: com.baijum.ukufretboard.viewmodel.ScalePracticeUiState,
    progressViewModel: LearningProgressViewModel?,
    allTimeStats: LearningStats?,
) {
    // Session stats
    if (state.quizTotal > 0) {
        StatsRow(
            label = stringResource(R.string.label_this_session),
            correct = state.quizCorrect,
            total = state.quizTotal,
            streak = state.quizStreak,
            bestStreak = state.quizBestStreak,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    // All-time stats
    if (allTimeStats != null && allTimeStats.total > 0) {
        AllTimeStatsRow(stats = allTimeStats)
        Spacer(modifier = Modifier.height(16.dp))
    }

    val question = state.quizQuestion
    if (question == null) {
        // Start / Next button
        Button(
            onClick = { viewModel.generateQuizQuestion() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(if (state.quizTotal == 0) stringResource(R.string.scale_practice_start_quiz) else stringResource(R.string.scale_practice_next))
        }
    } else {
        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Answer buttons
                question.options.forEachIndexed { index, option ->
                    val isSelected = state.quizSelectedAnswer == index
                    val isCorrect = index == question.correctIndex
                    val hasAnswered = state.quizSelectedAnswer != null

                    val containerColor = when {
                        !hasAnswered -> MaterialTheme.colorScheme.surface
                        isCorrect -> MaterialTheme.colorScheme.primaryContainer
                        isSelected -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    OutlinedButton(
                        onClick = {
                            if (!hasAnswered) {
                                val correct = viewModel.submitQuizAnswer(index)
                                progressViewModel?.recordScalePracticeAnswer("quiz", correct)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = containerColor,
                        ),
                        enabled = !hasAnswered || isSelected || isCorrect,
                    ) {
                        Text(
                            text = option,
                            fontWeight = if (hasAnswered && isCorrect) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }

                // Explanation after answering
                if (state.quizSelectedAnswer != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Next button after answering
        if (state.quizSelectedAnswer != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.generateQuizQuestion() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scale_practice_next))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Ear Training Mode
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun EarTrainingContent(
    viewModel: ScalePracticeViewModel,
    state: com.baijum.ukufretboard.viewmodel.ScalePracticeUiState,
    progressViewModel: LearningProgressViewModel?,
    allTimeStats: LearningStats?,
) {
    val scope = rememberCoroutineScope()

    // Session stats
    if (state.earTotal > 0) {
        StatsRow(
            label = stringResource(R.string.label_this_session),
            correct = state.earCorrect,
            total = state.earTotal,
            streak = state.earStreak,
            bestStreak = state.earBestStreak,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    // All-time stats
    if (allTimeStats != null && allTimeStats.total > 0) {
        AllTimeStatsRow(stats = allTimeStats)
        Spacer(modifier = Modifier.height(16.dp))
    }

    val question = state.earQuestion
    if (question == null) {
        Button(
            onClick = {
                viewModel.generateEarQuestion()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(if (state.earTotal == 0) stringResource(R.string.scale_practice_start_training) else stringResource(R.string.scale_practice_next_scale))
        }
    } else {
        // Auto-play the scale when a new question appears
        LaunchedEffect(question) {
            viewModel.playEarScale(scope)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.scale_practice_what),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Show root note
                val rootName = Notes.pitchClassToName(question.root)
                Text(
                    text = "${stringResource(R.string.scale_practice_root_label)} $rootName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Replay button
                Button(
                    onClick = { viewModel.playEarScale(scope) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.label_replay),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.scale_practice_replay))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Answer buttons
                question.options.forEachIndexed { index, option ->
                    val isSelected = state.earSelectedAnswer == index
                    val isCorrect = index == question.correctIndex
                    val hasAnswered = state.earSelectedAnswer != null

                    val containerColor = when {
                        !hasAnswered -> MaterialTheme.colorScheme.surface
                        isCorrect -> MaterialTheme.colorScheme.primaryContainer
                        isSelected -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    OutlinedButton(
                        onClick = {
                            if (!hasAnswered) {
                                val correct = viewModel.submitEarAnswer(index)
                                progressViewModel?.recordScalePracticeAnswer("ear", correct)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = containerColor,
                        ),
                        enabled = !hasAnswered || isSelected || isCorrect,
                    ) {
                        Text(
                            text = option,
                            fontWeight = if (hasAnswered && isCorrect) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }

                // Show scale notes after answering
                if (state.earSelectedAnswer != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val scaleName = question.scale.name
                    val isMinor = question.scale.intervals.size > 2 && question.scale.intervals[2] == 3
                    val notes = question.scale.intervals.map { interval ->
                        val pc = (question.root + interval) % Notes.PITCH_CLASS_COUNT
                        Notes.enharmonicForKey(pc, question.root, isMinor)
                    }
                    val resultText = if (state.earSelectedAnswer == question.correctIndex) {
                        "${stringResource(R.string.scale_practice_correct)} $rootName $scaleName."
                    } else {
                        "${stringResource(R.string.label_the_answer_is)} $rootName $scaleName."
                    }
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${stringResource(R.string.scale_practice_notes_label)} ${notes.joinToString(" – ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Next button after answering
        if (state.earSelectedAnswer != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.generateEarQuestion() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scale_practice_next_scale))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Shared stat components
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun StatsRow(
    label: String,
    correct: Int,
    total: Int,
    streak: Int,
    bestStreak: Int,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ScoreItem(label = stringResource(R.string.label_correct), value = "$correct/$total")
        ScoreItem(
            label = stringResource(R.string.label_accuracy),
            value = if (total > 0) "${correct * 100 / total}%" else "—",
        )
        ScoreItem(label = stringResource(R.string.label_streak), value = "$streak")
        ScoreItem(label = stringResource(R.string.label_best), value = "$bestStreak")
    }
}

@Composable
private fun AllTimeStatsRow(stats: LearningStats) {
    Text(
        text = stringResource(R.string.label_all_time),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ScoreItem(label = stringResource(R.string.label_score), value = "${stats.correct}/${stats.total}")
        ScoreItem(label = stringResource(R.string.label_accuracy), value = "${stats.accuracyPercent}%")
        ScoreItem(label = stringResource(R.string.label_best_streak), value = "${stats.bestStreak}")
    }
}

@Composable
private fun ScoreItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

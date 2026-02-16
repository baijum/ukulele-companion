package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.LearningStats
import com.baijum.ukufretboard.domain.IntervalTrainer
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

/**
 * Interactive interval trainer with visual note display and multiple-choice answers.
 *
 * Users identify intervals between two notes shown on screen.
 * Difficulty increases progressively as they get more correct answers.
 * Persists scores via [LearningProgressViewModel].
 */
@Composable
fun IntervalTrainerView(
    progressViewModel: LearningProgressViewModel? = null,
    modifier: Modifier = Modifier,
) {
    // Observe persisted stats to trigger recomposition
    val progressState = progressViewModel?.state?.collectAsState()
    val allTimeStats = progressViewModel?.intervalStats()
    var isAudioMode by remember { mutableStateOf(false) }
    var direction by remember { mutableStateOf(IntervalTrainer.IntervalDirection.ASCENDING) }
    var level by remember { mutableIntStateOf(1) }
    var question by remember { mutableStateOf<IntervalTrainer.IntervalQuestion?>(null) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var totalCorrect by remember { mutableIntStateOf(0) }
    var totalAnswered by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.interval_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.interval_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mode toggle: Visual / Audio
        Text(
            text = stringResource(R.string.label_mode),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = !isAudioMode,
                onClick = {
                    isAudioMode = false
                    question = null
                    selectedAnswer = null
                },
                label = { Text(stringResource(R.string.interval_visual)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            FilterChip(
                selected = isAudioMode,
                onClick = {
                    isAudioMode = true
                    question = null
                    selectedAnswer = null
                },
                label = { Text(stringResource(R.string.interval_audio)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        // Direction selector (audio mode only)
        if (isAudioMode) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_direction),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val directions = listOf(
                    stringResource(R.string.interval_ascending) to IntervalTrainer.IntervalDirection.ASCENDING,
                    stringResource(R.string.interval_descending) to IntervalTrainer.IntervalDirection.DESCENDING,
                    stringResource(R.string.interval_harmonic) to IntervalTrainer.IntervalDirection.HARMONIC,
                )
                directions.forEach { (label, dir) ->
                    FilterChip(
                        selected = direction == dir,
                        onClick = {
                            direction = dir
                            question = null
                            selectedAnswer = null
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Level selector
        Text(
            text = stringResource(R.string.label_difficulty),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val levelNames = listOf(
                stringResource(R.string.label_easy),
                stringResource(R.string.label_medium),
                stringResource(R.string.label_hard),
                stringResource(R.string.label_expert),
            )
            levelNames.forEachIndexed { index, name ->
                FilterChip(
                    selected = level == index + 1,
                    onClick = {
                        level = index + 1
                        question = null
                        selectedAnswer = null
                    },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Session score display
        if (totalAnswered > 0) {
            Text(
                text = stringResource(R.string.label_this_session),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScoreItem(label = stringResource(R.string.label_correct), value = "$totalCorrect/$totalAnswered")
                ScoreItem(label = stringResource(R.string.label_accuracy), value = "${(totalCorrect * 100 / totalAnswered)}%")
                ScoreItem(label = stringResource(R.string.label_streak), value = "$streak")
                ScoreItem(label = stringResource(R.string.label_best), value = "$bestStreak")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // All-time stats
        if (allTimeStats != null && allTimeStats.total > 0) {
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
                ScoreItem(label = stringResource(R.string.label_score), value = "${allTimeStats.correct}/${allTimeStats.total}")
                ScoreItem(label = stringResource(R.string.label_accuracy), value = "${allTimeStats.accuracyPercent}%")
                ScoreItem(label = stringResource(R.string.label_best_streak), value = "${allTimeStats.bestStreak}")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (question == null) {
            // Start button
            Button(
                onClick = {
                    val dir = if (isAudioMode) direction else IntervalTrainer.IntervalDirection.ASCENDING
                    question = IntervalTrainer.generateQuestion(level, dir)
                    selectedAnswer = null
                    // Auto-play in audio mode
                    if (isAudioMode) {
                        val q = question!!
                        scope.launch { playInterval(q) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(if (totalAnswered == 0) stringResource(R.string.interval_start) else stringResource(R.string.interval_next))
            }
        } else {
            // Note display
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
                        text = stringResource(R.string.interval_what),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAudioMode) {
                        // Audio mode: replay button, no note names shown until answered
                        Button(
                            onClick = {
                                scope.launch { playInterval(question!!) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.label_replay),
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(stringResource(R.string.interval_replay))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Show direction label
                        val dirLabel = when (question!!.direction) {
                            IntervalTrainer.IntervalDirection.ASCENDING -> stringResource(R.string.interval_ascending)
                            IntervalTrainer.IntervalDirection.DESCENDING -> stringResource(R.string.interval_descending)
                            IntervalTrainer.IntervalDirection.HARMONIC -> stringResource(R.string.interval_harmonic)
                        }
                        Text(
                            text = dirLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Show note names after answering
                        if (selectedAnswer != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NoteCircle(
                                    noteName = question!!.note1Name,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "\u2192",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                NoteCircle(
                                    noteName = question!!.note2Name,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    } else {
                        // Visual mode: show two notes prominently
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NoteCircle(
                                noteName = question!!.note1Name,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "\u2192",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            NoteCircle(
                                noteName = question!!.note2Name,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Semitone count hint
                        Text(
                            text = "${question!!.intervalSemitones} semitone${if (question!!.intervalSemitones != 1) "s" else ""} apart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Answer options
                    question!!.options.forEachIndexed { index, option ->
                        val isSelected = selectedAnswer == index
                        val isCorrect = index == question!!.correctIndex
                        val hasAnswered = selectedAnswer != null

                        val containerColor = when {
                            hasAnswered && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                            hasAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        OutlinedButton(
                            onClick = {
                                if (selectedAnswer != null) return@OutlinedButton
                                selectedAnswer = index
                                totalAnswered++
                                val isCorrect = index == question!!.correctIndex
                                if (isCorrect) {
                                    totalCorrect++
                                    streak++
                                    if (streak > bestStreak) bestStreak = streak
                                    // Auto-level up after 5 correct in a row
                                    if (streak % 5 == 0 && level < 4) level++
                                } else {
                                    streak = 0
                                }
                                progressViewModel?.recordIntervalAnswer(level, isCorrect)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = containerColor,
                            ),
                            enabled = selectedAnswer == null,
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }

                    // Result and next button
                    if (selectedAnswer != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isCorrect = selectedAnswer == question!!.correctIndex
                        val theAnswerIs = stringResource(R.string.label_the_answer_is)
                        Text(
                            text = if (isCorrect) stringResource(R.string.label_correct_answer) else "$theAnswerIs ${question!!.correctAnswer}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val dir = if (isAudioMode) direction else IntervalTrainer.IntervalDirection.ASCENDING
                                question = IntervalTrainer.generateQuestion(level, dir)
                                selectedAnswer = null
                                if (isAudioMode) {
                                    scope.launch { playInterval(question!!) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.interval_next))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCircle(noteName: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = noteName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun ScoreItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Plays the two notes of an interval question using [ToneGenerator].
 */
private suspend fun playInterval(question: IntervalTrainer.IntervalQuestion) {
    when (question.direction) {
        IntervalTrainer.IntervalDirection.ASCENDING -> {
            ToneGenerator.playNote(question.note1PitchClass, question.note1Octave, durationMs = 700)
            ToneGenerator.playNote(question.note2PitchClass, question.note2Octave, durationMs = 700)
        }
        IntervalTrainer.IntervalDirection.DESCENDING -> {
            // Play higher note first, then lower
            ToneGenerator.playNote(question.note2PitchClass, question.note2Octave, durationMs = 700)
            ToneGenerator.playNote(question.note1PitchClass, question.note1Octave, durationMs = 700)
        }
        IntervalTrainer.IntervalDirection.HARMONIC -> {
            // Play both notes simultaneously as a 2-note chord
            ToneGenerator.playChord(
                notes = listOf(
                    question.note1PitchClass to question.note1Octave,
                    question.note2PitchClass to question.note2Octave,
                ),
                noteDurationMs = 1000,
                strumDelayMs = 0,
            )
        }
    }
}

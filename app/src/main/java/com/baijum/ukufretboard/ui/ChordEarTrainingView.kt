package com.baijum.ukufretboard.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.domain.ChordEarTrainer
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import kotlinx.coroutines.launch

/**
 * Interactive chord ear training exercise.
 *
 * A chord is played and the user must identify its quality.
 * Difficulty progresses through four levels with increasing chord types.
 * Persists scores via [LearningProgressViewModel].
 */
@Composable
fun ChordEarTrainingView(
    progressViewModel: LearningProgressViewModel? = null,
    modifier: Modifier = Modifier,
) {
    // Observe persisted stats to trigger recomposition
    @Suppress("UNUSED_VARIABLE")
    val progressState = progressViewModel?.state?.collectAsState()
    val allTimeStats = progressViewModel?.chordEarStats()

    var level by remember { mutableIntStateOf(1) }
    var question by remember { mutableStateOf<ChordEarTrainer.ChordEarQuestion?>(null) }
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
            text = "Chord Ear Training",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Identify the chord quality by ear.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Level selector
        Text(
            text = "Difficulty",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val levelNames = listOf("Easy", "Medium", "Hard", "Expert")
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

        // Session stats
        if (totalAnswered > 0) {
            Text(
                text = "This Session",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ChordEarScoreItem(label = "Correct", value = "$totalCorrect/$totalAnswered")
                ChordEarScoreItem(label = "Accuracy", value = "${(totalCorrect * 100 / totalAnswered)}%")
                ChordEarScoreItem(label = "Streak", value = "$streak")
                ChordEarScoreItem(label = "Best", value = "$bestStreak")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // All-time stats
        if (allTimeStats != null && allTimeStats.total > 0) {
            Text(
                text = "All Time",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ChordEarScoreItem(label = "Score", value = "${allTimeStats.correct}/${allTimeStats.total}")
                ChordEarScoreItem(label = "Accuracy", value = "${allTimeStats.accuracyPercent}%")
                ChordEarScoreItem(label = "Best Streak", value = "${allTimeStats.bestStreak}")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (question == null) {
            // Start button
            Button(
                onClick = {
                    val q = ChordEarTrainer.generateQuestion(level)
                    question = q
                    selectedAnswer = null
                    // Auto-play the chord
                    scope.launch {
                        ToneGenerator.playChord(
                            notes = q.notes,
                            strumDelayMs = 40,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(if (totalAnswered == 0) "Start Training" else "Next Chord")
            }
        } else {
            val q = question!!
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
                        text = "What type of chord is this?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show root note name
                    Text(
                        text = "${q.rootName} chord",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Replay button
                    Button(
                        onClick = {
                            scope.launch {
                                ToneGenerator.playChord(
                                    notes = q.notes,
                                    strumDelayMs = 40,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Replay",
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Replay Chord")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Answer buttons
                    q.options.forEachIndexed { index, option ->
                        val isSelected = selectedAnswer == index
                        val isCorrect = index == q.correctIndex
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
                                val correct = index == q.correctIndex
                                if (correct) {
                                    totalCorrect++
                                    streak++
                                    if (streak > bestStreak) bestStreak = streak
                                    // Auto-level up after 5 correct in a row
                                    if (streak % 5 == 0 && level < 4) level++
                                } else {
                                    streak = 0
                                }
                                progressViewModel?.recordChordEarAnswer(level, correct)
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
                        val isCorrectAnswer = selectedAnswer == q.correctIndex
                        val fullChordName = "${q.rootName}${q.symbol} (${q.quality})"
                        Text(
                            text = if (isCorrectAnswer) "Correct!" else "The answer is ${q.correctAnswer}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrectAnswer) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        // Always show the full chord name after answering
                        Text(
                            text = fullChordName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val newQ = ChordEarTrainer.generateQuestion(level)
                                question = newQ
                                selectedAnswer = null
                                scope.launch {
                                    ToneGenerator.playChord(
                                        notes = newQ.notes,
                                        strumDelayMs = 40,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Next Chord")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChordEarScoreItem(label: String, value: String) {
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

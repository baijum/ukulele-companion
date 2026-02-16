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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.domain.NoteQuizGenerator
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel

/**
 * Interactive note quiz that tests fretboard note knowledge.
 *
 * Two modes:
 * - **Name It**: Given a string and fret, identify the note name.
 * - **Find It**: Given a note name, find its position on the fretboard.
 *
 * Persists scores via [LearningProgressViewModel].
 */
@Composable
fun NoteQuizView(
    progressViewModel: LearningProgressViewModel? = null,
    modifier: Modifier = Modifier,
) {
    // Observe persisted stats to trigger recomposition
    @Suppress("UNUSED_VARIABLE")
    val progressState = progressViewModel?.state?.collectAsState()
    val allTimeStats = progressViewModel?.noteQuizStats()

    var mode by remember { mutableStateOf(NoteQuizGenerator.Mode.NAME_IT) }
    var difficulty by remember { mutableIntStateOf(1) }

    // Name It state
    var nameItQuestion by remember { mutableStateOf<NoteQuizGenerator.NameItQuestion?>(null) }
    // Find It state
    var findItQuestion by remember { mutableStateOf<NoteQuizGenerator.FindItQuestion?>(null) }

    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var totalCorrect by remember { mutableIntStateOf(0) }
    var totalAnswered by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }

    val hasQuestion = when (mode) {
        NoteQuizGenerator.Mode.NAME_IT -> nameItQuestion != null
        NoteQuizGenerator.Mode.FIND_IT -> findItQuestion != null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Note Quiz",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Test your knowledge of fretboard notes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mode toggle
        Text(
            text = "Mode",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = mode == NoteQuizGenerator.Mode.NAME_IT,
                onClick = {
                    mode = NoteQuizGenerator.Mode.NAME_IT
                    nameItQuestion = null
                    findItQuestion = null
                    selectedAnswer = null
                },
                label = { Text("Name It") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            FilterChip(
                selected = mode == NoteQuizGenerator.Mode.FIND_IT,
                onClick = {
                    mode = NoteQuizGenerator.Mode.FIND_IT
                    nameItQuestion = null
                    findItQuestion = null
                    selectedAnswer = null
                },
                label = { Text("Find It") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Difficulty selector
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
            val difficultyNames = listOf("Easy", "Medium", "Hard")
            difficultyNames.forEachIndexed { index, name ->
                FilterChip(
                    selected = difficulty == index + 1,
                    onClick = {
                        difficulty = index + 1
                        nameItQuestion = null
                        findItQuestion = null
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
                NoteQuizScoreItem(label = "Correct", value = "$totalCorrect/$totalAnswered")
                NoteQuizScoreItem(label = "Accuracy", value = "${(totalCorrect * 100 / totalAnswered)}%")
                NoteQuizScoreItem(label = "Streak", value = "$streak")
                NoteQuizScoreItem(label = "Best", value = "$bestStreak")
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
                NoteQuizScoreItem(label = "Score", value = "${allTimeStats.correct}/${allTimeStats.total}")
                NoteQuizScoreItem(label = "Accuracy", value = "${allTimeStats.accuracyPercent}%")
                NoteQuizScoreItem(label = "Best Streak", value = "${allTimeStats.bestStreak}")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!hasQuestion) {
            // Start button
            Button(
                onClick = {
                    selectedAnswer = null
                    when (mode) {
                        NoteQuizGenerator.Mode.NAME_IT ->
                            nameItQuestion = NoteQuizGenerator.generateNameIt(difficulty)
                        NoteQuizGenerator.Mode.FIND_IT ->
                            findItQuestion = NoteQuizGenerator.generateFindIt(difficulty)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(if (totalAnswered == 0) "Start Quiz" else "Next Question")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (mode) {
                        NoteQuizGenerator.Mode.NAME_IT -> {
                            val q = nameItQuestion!!
                            Text(
                                text = "What note is at this position?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Show the position
                            Text(
                                text = "${NoteQuizGenerator.STRING_NAMES[q.string]} string, fret ${q.fret}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

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
                                        } else {
                                            streak = 0
                                        }
                                        progressViewModel?.recordNoteQuizAnswer(correct)
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
                        }

                        NoteQuizGenerator.Mode.FIND_IT -> {
                            val q = findItQuestion!!
                            Text(
                                text = "Where is this note?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Show the target note
                            Text(
                                text = q.targetNote,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

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
                                        } else {
                                            streak = 0
                                        }
                                        progressViewModel?.recordNoteQuizAnswer(correct)
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
                        }
                    }

                    // Result and next button
                    if (selectedAnswer != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val correctIndex = when (mode) {
                            NoteQuizGenerator.Mode.NAME_IT -> nameItQuestion!!.correctIndex
                            NoteQuizGenerator.Mode.FIND_IT -> findItQuestion!!.correctIndex
                        }
                        val correctAnswer = when (mode) {
                            NoteQuizGenerator.Mode.NAME_IT -> nameItQuestion!!.correctNote
                            NoteQuizGenerator.Mode.FIND_IT -> {
                                val q = findItQuestion!!
                                "${NoteQuizGenerator.STRING_NAMES[q.correctString]} string, fret ${q.correctFret}"
                            }
                        }
                        val isCorrectAnswer = selectedAnswer == correctIndex
                        Text(
                            text = if (isCorrectAnswer) "Correct!" else "The answer is $correctAnswer",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrectAnswer) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                selectedAnswer = null
                                when (mode) {
                                    NoteQuizGenerator.Mode.NAME_IT ->
                                        nameItQuestion = NoteQuizGenerator.generateNameIt(difficulty)
                                    NoteQuizGenerator.Mode.FIND_IT ->
                                        findItQuestion = NoteQuizGenerator.generateFindIt(difficulty)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Next Question")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteQuizScoreItem(label: String, value: String) {
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

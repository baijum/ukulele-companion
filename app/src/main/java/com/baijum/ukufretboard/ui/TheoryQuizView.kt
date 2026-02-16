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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import androidx.compose.material3.Card
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
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.LearningStats
import com.baijum.ukufretboard.domain.QuizGenerator
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import androidx.compose.runtime.collectAsState

/**
 * Interactive theory quiz view with category selection, scoring, and streaks.
 *
 * Users select a category (or "All"), answer multiple-choice questions,
 * and see their score and current streak.
 * Persists scores via [LearningProgressViewModel].
 */
@Composable
fun TheoryQuizView(
    progressViewModel: LearningProgressViewModel? = null,
    modifier: Modifier = Modifier,
) {
    // Observe persisted stats to trigger recomposition
    val progressState = progressViewModel?.state?.collectAsState()
    val allTimeStats = progressViewModel?.quizStats()
    var selectedCategory by remember { mutableStateOf<QuizGenerator.QuizCategory?>(null) }
    var currentQuestion by remember { mutableStateOf<QuizGenerator.QuizQuestion?>(null) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var totalCorrect by remember { mutableIntStateOf(0) }
    var totalAnswered by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }

    // Blitz mode state
    var isBlitzMode by remember { mutableStateOf(false) }
    var blitzActive by remember { mutableStateOf(false) }
    var blitzTimeMs by remember { mutableLongStateOf(BLITZ_DURATION_MS) }
    var blitzScore by remember { mutableIntStateOf(0) }
    var blitzFinished by remember { mutableStateOf(false) }

    // Blitz countdown timer
    LaunchedEffect(blitzActive) {
        if (blitzActive) {
            while (blitzTimeMs > 0) {
                delay(100L)
                blitzTimeMs -= 100
            }
            blitzActive = false
            blitzFinished = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.theory_quiz_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.theory_quiz_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mode toggle: Standard / Blitz
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
                selected = !isBlitzMode,
                onClick = {
                    isBlitzMode = false
                    currentQuestion = null
                    selectedAnswer = null
                    blitzActive = false
                    blitzFinished = false
                },
                label = { Text(stringResource(R.string.theory_quiz_standard)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            FilterChip(
                selected = isBlitzMode,
                onClick = {
                    isBlitzMode = true
                    currentQuestion = null
                    selectedAnswer = null
                    blitzActive = false
                    blitzFinished = false
                    blitzScore = 0
                    blitzTimeMs = BLITZ_DURATION_MS
                    totalCorrect = 0
                    totalAnswered = 0
                    streak = 0
                },
                label = { Text(stringResource(R.string.theory_quiz_blitz)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.error,
                    selectedLabelColor = MaterialTheme.colorScheme.onError,
                ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category selector
        Text(
            text = stringResource(R.string.label_category),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text(stringResource(R.string.label_all)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            QuizGenerator.QuizCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.label) },
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
                ScoreStat(label = stringResource(R.string.label_score), value = "$totalCorrect/$totalAnswered")
                ScoreStat(label = stringResource(R.string.label_accuracy), value = "${(totalCorrect * 100 / totalAnswered)}%")
                ScoreStat(label = stringResource(R.string.label_streak), value = "$streak")
                ScoreStat(label = stringResource(R.string.label_best), value = "$bestStreak")
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
                ScoreStat(label = stringResource(R.string.label_score), value = "${allTimeStats.correct}/${allTimeStats.total}")
                ScoreStat(label = stringResource(R.string.label_accuracy), value = "${allTimeStats.accuracyPercent}%")
                ScoreStat(label = stringResource(R.string.label_best_streak), value = "${allTimeStats.bestStreak}")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Blitz timer
        if (isBlitzMode && blitzActive) {
            val seconds = (blitzTimeMs / 1000).toInt()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (seconds <= 10) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = "⏱ ${seconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.theory_quiz_final_score, blitzScore),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Blitz finished
        if (isBlitzMode && blitzFinished) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.theory_quiz_times_up),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.theory_quiz_final_score, blitzScore),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (totalAnswered > 0) {
                        Text(
                            text = stringResource(R.string.theory_quiz_final_accuracy, "${totalCorrect * 100 / totalAnswered}%"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        blitzFinished = false
                        blitzScore = 0
                        blitzTimeMs = BLITZ_DURATION_MS
                        totalCorrect = 0
                        totalAnswered = 0
                        streak = 0
                        currentQuestion = QuizGenerator.generate(selectedCategory)
                        selectedAnswer = null
                        blitzActive = true
                    }) {
                        Text(stringResource(R.string.theory_quiz_play_again))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Question card or start button
        if (isBlitzMode && blitzFinished) {
            // Don't show question when blitz is finished — handled above
        } else if (currentQuestion == null) {
            Button(
                onClick = {
                    currentQuestion = QuizGenerator.generate(selectedCategory)
                    selectedAnswer = null
                    if (isBlitzMode && !blitzActive) {
                        blitzActive = true
                        blitzTimeMs = BLITZ_DURATION_MS
                        blitzScore = 0
                        totalCorrect = 0
                        totalAnswered = 0
                        streak = 0
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    when {
                        isBlitzMode -> stringResource(R.string.theory_quiz_start_blitz)
                        totalAnswered == 0 -> stringResource(R.string.theory_quiz_start)
                        else -> stringResource(R.string.theory_quiz_next)
                    }
                )
            }
        } else {
            QuestionCard(
                question = currentQuestion!!,
                selectedAnswer = selectedAnswer,
                onAnswerSelected = { answerIndex ->
                    if (selectedAnswer != null) return@QuestionCard // Already answered
                    selectedAnswer = answerIndex
                    totalAnswered++
                    val isCorrect = answerIndex == currentQuestion!!.correctIndex
                    if (isCorrect) {
                        totalCorrect++
                        streak++
                        if (streak > bestStreak) bestStreak = streak
                        if (isBlitzMode) {
                            blitzScore++
                            // +3 second bonus for correct answer
                            blitzTimeMs = (blitzTimeMs + 3000).coerceAtMost(BLITZ_DURATION_MS)
                        }
                    } else {
                        streak = 0
                    }
                    progressViewModel?.recordQuizAnswer(currentQuestion!!.category, isCorrect)
                    // In blitz mode, auto-advance immediately
                    if (isBlitzMode && blitzActive) {
                        currentQuestion = QuizGenerator.generate(selectedCategory)
                        selectedAnswer = null
                    }
                },
                onNext = {
                    currentQuestion = QuizGenerator.generate(selectedCategory)
                    selectedAnswer = null
                },
            )
        }
    }
}

@Composable
private fun ScoreStat(label: String, value: String) {
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

@Composable
private fun QuestionCard(
    question: QuizGenerator.QuizQuestion,
    selectedAnswer: Int?,
    onAnswerSelected: (Int) -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category badge
            Text(
                text = question.category.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Question text
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Answer options
            question.options.forEachIndexed { index, option ->
                val isSelected = selectedAnswer == index
                val isCorrect = index == question.correctIndex
                val hasAnswered = selectedAnswer != null

                val containerColor = when {
                    hasAnswered && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                    hasAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                val contentColor = when {
                    hasAnswered && isCorrect -> MaterialTheme.colorScheme.onPrimaryContainer
                    hasAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }

                OutlinedButton(
                    onClick = { onAnswerSelected(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
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

            // Explanation (shown after answering)
            if (selectedAnswer != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.theory_quiz_next))
                }
            }
        }
    }
}

/** Blitz mode duration in milliseconds (60 seconds). */
private const val BLITZ_DURATION_MS = 60_000L

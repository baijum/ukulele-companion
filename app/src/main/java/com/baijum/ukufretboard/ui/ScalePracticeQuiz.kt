package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.LearningStats
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import com.baijum.ukufretboard.viewmodel.ScalePracticeUiState
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel

@Composable
internal fun QuizContent(
    viewModel: ScalePracticeViewModel,
    state: ScalePracticeUiState,
    progressViewModel: LearningProgressViewModel?,
    allTimeStats: LearningStats?,
) {
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

    if (allTimeStats != null && allTimeStats.total > 0) {
        AllTimeStatsRow(stats = allTimeStats)
        Spacer(modifier = Modifier.height(16.dp))
    }

    val question = state.quizQuestion
    if (question == null) {
        Button(
            onClick = { viewModel.generateQuizQuestion() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(if (state.quizTotal == 0) stringResource(R.string.scale_practice_start_quiz) else stringResource(R.string.scale_practice_next))
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
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

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

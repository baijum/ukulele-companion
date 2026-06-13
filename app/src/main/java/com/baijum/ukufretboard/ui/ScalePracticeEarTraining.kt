package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.LearningStats
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import com.baijum.ukufretboard.viewmodel.ScalePracticeUiState
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel

@Composable
internal fun EarTrainingContent(
    viewModel: ScalePracticeViewModel,
    state: ScalePracticeUiState,
    progressViewModel: LearningProgressViewModel?,
    allTimeStats: LearningStats?,
) {
    val scope = rememberCoroutineScope()

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

                val rootName = Notes.pitchClassToName(question.root)
                Text(
                    text = "${stringResource(R.string.scale_practice_root_label)} $rootName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(12.dp))

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

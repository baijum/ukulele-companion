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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.LearningStats
import com.baijum.ukufretboard.data.PracticeStats
import com.baijum.ukufretboard.data.PracticeTimerRepository
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel

/**
 * Learning Progress dashboard showing lesson completion, quiz stats,
 * interval trainer stats, and daily streak.
 */
@Composable
fun LearningProgressView(
    viewModel: LearningProgressViewModel,
    practiceStats: PracticeStats = PracticeStats(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.progress_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.progress_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Practice Time ──
        if (practiceStats.totalMinutes > 0 || practiceStats.todayMinutes > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.progress_practice_time),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ProgressStat(
                            value = "${practiceStats.todayMinutes}m",
                            label = stringResource(R.string.progress_today),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        ProgressStat(
                            value = practiceStats.totalTimeFormatted,
                            label = stringResource(R.string.progress_total),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        ProgressStat(
                            value = "${practiceStats.totalSessions}",
                            label = stringResource(R.string.progress_sessions),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    if (practiceStats.dailyGoal > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { practiceStats.dailyProgress.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                        )
                        Text(
                            text = "Daily goal: ${practiceStats.todayMinutes}/${practiceStats.dailyGoal} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Daily Streak ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProgressStat(
                    value = "${state.currentDayStreak}",
                    label = stringResource(R.string.progress_day_streak),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                ProgressStat(
                    value = "${state.bestDayStreak}",
                    label = stringResource(R.string.label_best_streak),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                ProgressStat(
                    value = "${state.lessonCompletionPercent}%",
                    label = stringResource(R.string.progress_lessons_done),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Theory Lessons ──
        SectionHeader(stringResource(R.string.progress_theory_lessons))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.progress_lessons_completed),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${state.completedLessons} / ${state.totalLessons}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        if (state.totalLessons > 0) {
                            state.completedLessons.toFloat() / state.totalLessons
                        } else 0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.progress_quizzes_passed),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${state.passedLessonQuizzes} / ${state.totalLessons}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        if (state.totalLessons > 0) {
                            state.passedLessonQuizzes.toFloat() / state.totalLessons
                        } else 0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Theory Quiz ──
        SectionHeader(stringResource(R.string.nav_theory_quiz))
        StatsCard(stats = state.quizStatsOverall, label = stringResource(R.string.progress_overall))
        Spacer(modifier = Modifier.height(8.dp))
        state.quizStatsByCategory.forEach { (category, stats) ->
            if (stats.total > 0) {
                MiniStatsRow(label = category.label, stats = stats)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Interval Trainer ──
        SectionHeader(stringResource(R.string.nav_interval_trainer))
        StatsCard(stats = state.intervalStatsOverall, label = stringResource(R.string.progress_overall))
        Spacer(modifier = Modifier.height(8.dp))
        val levelNames = listOf(
            stringResource(R.string.label_easy),
            stringResource(R.string.label_medium),
            stringResource(R.string.label_hard),
            stringResource(R.string.label_expert),
        )
        state.intervalStatsByLevel.forEach { (level, stats) ->
            if (stats.total > 0) {
                MiniStatsRow(label = levelNames.getOrElse(level - 1) { "Level $level" }, stats = stats)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Note Quiz ──
        SectionHeader(stringResource(R.string.nav_note_quiz))
        StatsCard(stats = state.noteQuizStats, label = stringResource(R.string.progress_overall))

        Spacer(modifier = Modifier.height(16.dp))

        // ── Chord Ear Training ──
        SectionHeader(stringResource(R.string.nav_chord_ear_training))
        StatsCard(stats = state.chordEarStatsOverall, label = stringResource(R.string.progress_overall))
        Spacer(modifier = Modifier.height(8.dp))
        state.chordEarStatsByLevel.forEach { (level, stats) ->
            if (stats.total > 0) {
                MiniStatsRow(label = levelNames.getOrElse(level - 1) { "Level $level" }, stats = stats)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Reset ──
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.progress_reset))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.progress_reset_title)) },
            text = { Text(stringResource(R.string.progress_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllProgress()
                    showResetDialog = false
                }) {
                    Text(stringResource(R.string.dialog_reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
    )
}

@Composable
private fun StatsCard(stats: LearningStats, label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (stats.total == 0) {
                Text(
                    text = stringResource(R.string.progress_no_attempts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ProgressStat(value = "${stats.correct}/${stats.total}", label = stringResource(R.string.label_score))
                    ProgressStat(value = "${stats.accuracyPercent}%", label = stringResource(R.string.label_accuracy))
                    ProgressStat(value = "${stats.bestStreak}", label = stringResource(R.string.label_best_streak))
                }
            }
        }
    }
}

@Composable
private fun MiniStatsRow(label: String, stats: LearningStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${stats.correct}/${stats.total}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(
            text = "${stats.accuracyPercent}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}

@Composable
private fun ProgressStat(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
        )
    }
}

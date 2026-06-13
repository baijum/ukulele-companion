package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.LearningStats

@Composable
internal fun StatsRow(
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
        modifier = Modifier.semantics { heading() },
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
internal fun AllTimeStatsRow(stats: LearningStats) {
    Text(
        text = stringResource(R.string.label_all_time),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
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

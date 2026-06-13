package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordSheet

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LabelDisplayRow(
    labels: List<String>,
    allLabels: Set<String>,
    onLabelsChange: (List<String>) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { label ->
            val labelDesc = stringResource(R.string.cd_label, label)
            AssistChip(
                onClick = {},
                label = {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                modifier = Modifier
                    .height(28.dp)
                    .semantics { contentDescription = labelDesc },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
        AssistChip(
            onClick = { showAddDialog = true },
            label = {
                Text(
                    stringResource(R.string.songbook_add_label),
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_label),
                    modifier = Modifier.size(14.dp),
                )
            },
            modifier = Modifier.height(28.dp),
        )
    }

    if (showAddDialog) {
        AddLabelDialog(
            currentLabels = labels,
            allLabels = allLabels,
            onAdd = { newLabel ->
                onLabelsChange(labels + newLabel)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
internal fun SongStatsRow(sheet: ChordSheet) {
    val lastViewed = if (sheet.lastViewedAt > 0) {
        val fmt = android.text.format.DateUtils.getRelativeTimeSpanString(
            sheet.lastViewedAt,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS,
        )
        fmt.toString()
    } else {
        null
    }

    val totalMinutes = (sheet.totalViewTimeMs / 60_000).toInt()
    val totalTimeLabel = when {
        totalMinutes < 1 -> stringResource(R.string.stats_time_under_minute)
        totalMinutes == 1 -> stringResource(R.string.stats_time_one_minute)
        totalMinutes < 60 -> stringResource(R.string.stats_time_minutes, totalMinutes)
        else -> {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            stringResource(R.string.stats_time_hours_minutes, hours, mins)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = pluralStringResource(R.plurals.stats_views, sheet.viewCount, sheet.viewCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (lastViewed != null) {
            Text(
                text = lastViewed,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = totalTimeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

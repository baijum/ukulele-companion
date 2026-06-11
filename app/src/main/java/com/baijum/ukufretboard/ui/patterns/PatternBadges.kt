package com.baijum.ukufretboard.ui.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Difficulty
import com.baijum.ukufretboard.ui.localizedLabel

@Composable
internal fun DifficultyBadge(difficulty: Difficulty) {
    val bgColor = when (difficulty) {
        Difficulty.BEGINNER -> MaterialTheme.colorScheme.primaryContainer
        Difficulty.INTERMEDIATE -> MaterialTheme.colorScheme.secondaryContainer
        Difficulty.ADVANCED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val textColor = when (difficulty) {
        Difficulty.BEGINNER -> MaterialTheme.colorScheme.onPrimaryContainer
        Difficulty.INTERMEDIATE -> MaterialTheme.colorScheme.onSecondaryContainer
        Difficulty.ADVANCED -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = difficulty.localizedLabel(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
internal fun TimeSignatureBadge(timeSignature: String) {
    val timeSignatureDesc = stringResource(R.string.cd_time_signature, timeSignature)
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.tertiaryContainer,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = timeSignatureDesc },
    ) {
        Text(
            text = timeSignature,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

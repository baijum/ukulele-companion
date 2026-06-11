package com.baijum.ukufretboard.ui.patterns

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.StrumBeat
import com.baijum.ukufretboard.data.StrumDirection
import com.baijum.ukufretboard.data.StrumPattern
import com.baijum.ukufretboard.ui.LocalReduceMotion

/**
 * A card displaying a single strumming pattern with play/stop controls.
 *
 * @param isPlaying Whether this specific pattern is currently playing.
 * @param activeBeatIndex The beat index currently sounding, or -1.
 * @param onPlay Called with the selected BPM to start playback.
 * @param onStop Called to stop playback.
 * @param onDuplicate If non-null, a duplicate button is shown.
 * @param onEdit If non-null, an edit button is shown (for custom patterns).
 * @param onDelete If non-null, a delete button is shown (for custom patterns).
 */
@Composable
internal fun StrumPatternCard(
    pattern: StrumPattern,
    isPlaying: Boolean = false,
    activeBeatIndex: Int = -1,
    onPlay: (bpm: Int) -> Unit = {},
    onStop: () -> Unit = {},
    onDuplicate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val midBpm = (pattern.suggestedBpm.first + pattern.suggestedBpm.last) / 2
    var bpm by remember(pattern.name) { mutableFloatStateOf(midBpm.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pattern.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TimeSignatureBadge(timeSignature = pattern.timeSignature)
                Spacer(modifier = Modifier.width(4.dp))
                DifficultyBadge(difficulty = pattern.difficulty)
                IconButton(
                    onClick = { if (isPlaying) onStop() else onPlay(bpm.toInt()) },
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) {
                            stringResource(R.string.cd_stop_pattern, pattern.name)
                        } else {
                            stringResource(R.string.cd_play_pattern, pattern.name)
                        },
                        tint = if (isPlaying) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                if (onDuplicate != null) {
                    IconButton(onClick = onDuplicate) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.cd_duplicate_pattern),
                        )
                    }
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.dialog_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            BeatDisplay(beats = pattern.beats, activeBeatIndex = activeBeatIndex)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pattern.notation,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
            )

            if (pattern.counting.isNotEmpty()) {
                Text(
                    text = pattern.counting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pattern.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (pattern.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    pattern.genres.forEach { genre ->
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${bpm.toInt()} ${stringResource(R.string.label_bpm)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.width(56.dp),
                )
                Slider(
                    value = bpm,
                    onValueChange = { bpm = it },
                    onValueChangeFinished = {
                        if (isPlaying) onPlay(bpm.toInt())
                    },
                    valueRange = 40f..220f,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BeatDisplay(beats: List<StrumBeat>, activeBeatIndex: Int = -1) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        beats.forEachIndexed { index, beat ->
            BeatArrow(beat = beat, isActive = index == activeBeatIndex)
        }
    }
}

@Composable
private fun BeatArrow(beat: StrumBeat, isActive: Boolean = false) {
    val baseColor = when (beat.direction) {
        StrumDirection.DOWN -> MaterialTheme.colorScheme.primary
        StrumDirection.UP -> MaterialTheme.colorScheme.secondary
        StrumDirection.CHUCK -> MaterialTheme.colorScheme.error
        StrumDirection.MISS -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        StrumDirection.PAUSE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val fontWeight = if (beat.emphasis) FontWeight.ExtraBold else FontWeight.Normal
    val fontSize = if (beat.emphasis) 20.sp else 18.sp

    val reduceMotion = LocalReduceMotion.current
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
        animationSpec = if (reduceMotion) snap() else tween(),
        label = "beatHighlight",
    )

    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 32.dp)
            .background(bgColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = beat.direction.symbol,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = baseColor,
            textAlign = TextAlign.Center,
        )
    }
}

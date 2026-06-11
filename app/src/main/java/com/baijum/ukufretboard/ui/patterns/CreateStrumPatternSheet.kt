package com.baijum.ukufretboard.ui.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Difficulty
import com.baijum.ukufretboard.data.StrumBeat
import com.baijum.ukufretboard.data.StrumDirection
import com.baijum.ukufretboard.data.StrumPattern

private fun resizeBeats(beats: MutableList<StrumBeat>, newSize: Int) {
    while (beats.size > newSize) beats.removeAt(beats.lastIndex)
    while (beats.size < newSize) {
        beats.add(StrumBeat(if (beats.size % 2 == 0) StrumDirection.DOWN else StrumDirection.UP))
    }
}

/**
 * Bottom sheet for creating a custom strumming pattern.
 *
 * Users tap beat slots to cycle through DOWN/UP/MISS/PAUSE directions,
 * then name and save the pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateStrumPatternSheet(
    onDismiss: () -> Unit,
    onSave: (StrumPattern) -> Unit,
    initialPattern: StrumPattern? = null,
) {
    val isEditMode = initialPattern != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var patternName by remember { mutableStateOf(initialPattern?.name ?: "") }
    var timeSignature by remember { mutableStateOf(initialPattern?.timeSignature ?: "4/4") }
    val customPatternDesc = stringResource(R.string.patterns_custom)
    val beats = remember {
        mutableStateListOf<StrumBeat>().also { list ->
            if (initialPattern != null) {
                list.addAll(initialPattern.beats)
            } else {
                val count = defaultBeatCount(timeSignature)
                for (i in 0 until count) {
                    list.add(
                        StrumBeat(
                            direction = if (i % 2 == 0) StrumDirection.DOWN else StrumDirection.UP,
                            emphasis = i == 0,
                        ),
                    )
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(
                    if (isEditMode) R.string.patterns_edit_strum else R.string.patterns_create_strum,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).semantics { heading() },
            )

            OutlinedTextField(
                value = patternName,
                onValueChange = { patternName = it },
                label = { Text(stringResource(R.string.patterns_pattern_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.patterns_time_signature),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TIME_SIGNATURES.forEach { ts ->
                    FilterChip(
                        selected = timeSignature == ts,
                        onClick = {
                            timeSignature = ts
                            resizeBeats(beats, defaultBeatCount(ts))
                        },
                        label = { Text(ts) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.patterns_beats, beats.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { if (beats.size > 2) beats.removeAt(beats.lastIndex) },
                        enabled = beats.size > 2,
                    ) {
                        Text("−")
                    }
                    TextButton(
                        onClick = {
                            if (beats.size < 16) {
                                beats.add(
                                    StrumBeat(
                                        if (beats.size % 2 == 0) StrumDirection.DOWN
                                        else StrumDirection.UP,
                                    ),
                                )
                            }
                        },
                        enabled = beats.size < 16,
                    ) {
                        Text("+")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.patterns_tap_beat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                beats.forEachIndexed { index, beat ->
                    val directions = StrumDirection.entries
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (beat.emphasis) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp),
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable {
                                val nextDir = directions[(directions.indexOf(beat.direction) + 1) % directions.size]
                                beats[index] = beat.copy(direction = nextDir)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = beat.direction.symbol,
                            fontSize = 18.sp,
                            fontWeight = if (beat.emphasis) FontWeight.ExtraBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.patterns_tap_accent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                beats.forEachIndexed { index, beat ->
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 24.dp)
                            .background(
                                if (beat.emphasis) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp),
                            )
                            .clickable {
                                beats[index] = beat.copy(emphasis = !beat.emphasis)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (beat.emphasis) "!" else "·",
                            fontSize = 12.sp,
                            color = if (beat.emphasis) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (patternName.isNotBlank()) {
                            val notation = beats.joinToString(" ") { it.direction.symbol }
                            onSave(
                                StrumPattern(
                                    name = patternName.trim(),
                                    description = customPatternDesc,
                                    difficulty = Difficulty.BEGINNER,
                                    timeSignature = timeSignature,
                                    beats = beats.toList(),
                                    notation = notation,
                                    suggestedBpm = 80..120,
                                ),
                            )
                        }
                    },
                    enabled = patternName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            }
        }
    }
}

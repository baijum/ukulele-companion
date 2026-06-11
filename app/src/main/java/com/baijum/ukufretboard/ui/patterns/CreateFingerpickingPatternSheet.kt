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
import androidx.compose.runtime.mutableIntStateOf
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
import com.baijum.ukufretboard.data.Finger
import com.baijum.ukufretboard.data.FingerpickStep
import com.baijum.ukufretboard.data.FingerpickingPattern
import com.baijum.ukufretboard.data.FingerpickingPatterns

private fun resizeSteps(steps: MutableList<FingerpickStep>, newSize: Int) {
    while (steps.size > newSize) steps.removeAt(steps.lastIndex)
    while (steps.size < newSize) {
        steps.add(FingerpickStep(Finger.INDEX, 2))
    }
}

/**
 * Bottom sheet for creating a custom fingerpicking pattern.
 *
 * Users select a step, then pick finger and string from chip rows below.
 * Steps can be added or removed (2–8). Emphasis toggles accent individual steps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateFingerpickingPatternSheet(
    onDismiss: () -> Unit,
    onSave: (FingerpickingPattern) -> Unit,
    initialPattern: FingerpickingPattern? = null,
) {
    val isEditMode = initialPattern != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var patternName by remember { mutableStateOf(initialPattern?.name ?: "") }
    var timeSignature by remember { mutableStateOf(initialPattern?.timeSignature ?: "4/4") }
    val customPatternDesc = stringResource(R.string.patterns_custom)
    val steps = remember {
        mutableStateListOf<FingerpickStep>().also { list ->
            if (initialPattern != null) {
                list.addAll(initialPattern.steps)
            } else {
                val count = defaultBeatCount(timeSignature).coerceIn(2, 8)
                list.add(FingerpickStep(Finger.THUMB, 0, emphasis = true))
                for (i in 1 until count) {
                    list.add(FingerpickStep(Finger.INDEX, 2.coerceAtMost(3)))
                }
            }
        }
    }
    var selectedStepIndex by remember { mutableIntStateOf(0) }

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
                    if (isEditMode) R.string.patterns_edit_fingerpick else R.string.patterns_create_fingerpick,
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
                            val newSize = defaultBeatCount(ts).coerceIn(2, 8)
                            resizeSteps(steps, newSize)
                            if (selectedStepIndex >= steps.size) {
                                selectedStepIndex = steps.size - 1
                            }
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
                    text = stringResource(R.string.patterns_steps, steps.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            if (steps.size > 2) {
                                if (selectedStepIndex >= steps.size - 1) {
                                    selectedStepIndex = steps.size - 2
                                }
                                steps.removeAt(steps.lastIndex)
                            }
                        },
                        enabled = steps.size > 2,
                    ) {
                        Text("−")
                    }
                    TextButton(
                        onClick = {
                            if (steps.size < 8) {
                                steps.add(FingerpickStep(Finger.INDEX, 2))
                            }
                        },
                        enabled = steps.size < 8,
                    ) {
                        Text("+")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.patterns_tap_step),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                steps.forEachIndexed { index, step ->
                    val isSelected = index == selectedStepIndex
                    val fingerColor = when (step.finger) {
                        Finger.THUMB -> MaterialTheme.colorScheme.primary
                        Finger.INDEX -> MaterialTheme.colorScheme.secondary
                        Finger.MIDDLE -> MaterialTheme.colorScheme.tertiary
                        Finger.RING -> MaterialTheme.colorScheme.error
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 48.dp)
                            .background(
                                if (step.emphasis) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp),
                            )
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { selectedStepIndex = index },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = step.finger.label,
                                fontSize = if (step.emphasis) 16.sp else 14.sp,
                                fontWeight = if (step.emphasis) FontWeight.ExtraBold else FontWeight.Normal,
                                color = fingerColor,
                            )
                            Text(
                                text = FingerpickingPatterns.STRING_NAMES[step.stringIndex],
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.patterns_tap_accent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                steps.forEachIndexed { index, step ->
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 24.dp)
                            .background(
                                if (step.emphasis) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp),
                            )
                            .clickable {
                                steps[index] = step.copy(emphasis = !step.emphasis)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (step.emphasis) "!" else "·",
                            fontSize = 12.sp,
                            color = if (step.emphasis) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.patterns_finger),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Finger.entries.forEach { finger ->
                    val isActive = steps[selectedStepIndex].finger == finger
                    FilterChip(
                        selected = isActive,
                        onClick = {
                            steps[selectedStepIndex] =
                                steps[selectedStepIndex].copy(finger = finger)
                        },
                        label = {
                            Text(
                                text = when (finger) {
                                    Finger.THUMB -> stringResource(R.string.patterns_thumb)
                                    Finger.INDEX -> stringResource(R.string.patterns_index)
                                    Finger.MIDDLE -> stringResource(R.string.patterns_middle)
                                    Finger.RING -> stringResource(R.string.patterns_ring)
                                },
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.patterns_string),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FingerpickingPatterns.STRING_NAMES.forEachIndexed { stringIndex, stringName ->
                    val isActive = steps[selectedStepIndex].stringIndex == stringIndex
                    FilterChip(
                        selected = isActive,
                        onClick = {
                            steps[selectedStepIndex] =
                                steps[selectedStepIndex].copy(stringIndex = stringIndex)
                        },
                        label = { Text(text = stringName) },
                    )
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
                            val notation = steps.joinToString(" ") { s ->
                                val sn = FingerpickingPatterns.STRING_NAMES[s.stringIndex]
                                "${s.finger.label}($sn)"
                            }
                            onSave(
                                FingerpickingPattern(
                                    name = patternName.trim(),
                                    description = customPatternDesc,
                                    difficulty = Difficulty.BEGINNER,
                                    timeSignature = timeSignature,
                                    steps = steps.toList(),
                                    notation = notation,
                                    suggestedBpm = 60..100,
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

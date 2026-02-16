package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.domain.PracticeRoutine
import com.baijum.ukufretboard.domain.PracticeRoutineGenerator
import com.baijum.ukufretboard.domain.PracticeStep

/**
 * Guided Practice Routine screen.
 *
 * Generates a personalised practice routine and guides the user through
 * each step with timers and navigation to relevant app sections.
 *
 * @param onNavigate Callback to navigate to a specific section index.
 */
@Composable
fun PracticeRoutineView(
    onNavigate: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var routine by remember { mutableStateOf<PracticeRoutine?>(null) }
    var durationMinutes by remember { mutableIntStateOf(15) }
    var skillLevel by remember { mutableStateOf(PracticeRoutineGenerator.SkillLevel.BEGINNER) }
    var selectedFocusAreas by remember {
        mutableStateOf(PracticeRoutineGenerator.FocusArea.entries.toSet())
    }
    val completedSteps = remember { mutableStateListOf<Int>() }
    var activeStep by remember { mutableIntStateOf(-1) }

    if (routine == null) {
        // Setup screen
        RoutineSetup(
            durationMinutes = durationMinutes,
            onDurationChange = { durationMinutes = it },
            skillLevel = skillLevel,
            onSkillLevelChange = { skillLevel = it },
            selectedFocusAreas = selectedFocusAreas,
            onFocusAreasChange = { selectedFocusAreas = it },
            onGenerate = {
                routine = PracticeRoutineGenerator.generate(
                    durationMinutes = durationMinutes,
                    skillLevel = skillLevel,
                    focusAreas = selectedFocusAreas,
                )
                completedSteps.clear()
                activeStep = 0
            },
            modifier = modifier,
        )
    } else {
        // Active routine
        ActiveRoutine(
            routine = routine!!,
            completedSteps = completedSteps.toSet(),
            activeStep = activeStep,
            onCompleteStep = { index ->
                if (index !in completedSteps) completedSteps.add(index)
                if (activeStep < routine!!.steps.lastIndex) activeStep++
            },
            onNavigate = onNavigate,
            onReset = {
                routine = null
                completedSteps.clear()
                activeStep = -1
            },
            modifier = modifier,
        )
    }
}

/**
 * Setup screen for configuring the practice routine.
 */
@Composable
private fun RoutineSetup(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    skillLevel: PracticeRoutineGenerator.SkillLevel,
    onSkillLevelChange: (PracticeRoutineGenerator.SkillLevel) -> Unit,
    selectedFocusAreas: Set<PracticeRoutineGenerator.FocusArea>,
    onFocusAreasChange: (Set<PracticeRoutineGenerator.FocusArea>) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.routine_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.routine_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Duration slider
        Text(
            text = stringResource(R.string.routine_duration, durationMinutes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Slider(
            value = durationMinutes.toFloat(),
            onValueChange = { onDurationChange(it.toInt()) },
            valueRange = 5f..60f,
            steps = 10,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Skill level
        Text(
            text = stringResource(R.string.routine_skill_level),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PracticeRoutineGenerator.SkillLevel.entries.forEach { level ->
                FilterChip(
                    selected = skillLevel == level,
                    onClick = { onSkillLevelChange(level) },
                    label = { Text(level.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Focus areas
        Text(
            text = stringResource(R.string.routine_focus_areas),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PracticeRoutineGenerator.FocusArea.entries.forEach { area ->
                FilterChip(
                    selected = area in selectedFocusAreas,
                    onClick = {
                        onFocusAreasChange(
                            if (area in selectedFocusAreas) {
                                selectedFocusAreas - area
                            } else {
                                selectedFocusAreas + area
                            },
                        )
                    },
                    label = { Text(area.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Generate button
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedFocusAreas.isNotEmpty(),
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.cd_start_routine),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.routine_start))
        }
    }
}

/**
 * Active routine view showing the steps to complete.
 */
@Composable
private fun ActiveRoutine(
    routine: PracticeRoutine,
    completedSteps: Set<Int>,
    activeStep: Int,
    onCompleteStep: (Int) -> Unit,
    onNavigate: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = completedSteps.size.toFloat() / routine.steps.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = "${routine.totalMinutes} min total · ${completedSteps.size}/${routine.steps.size} steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.cd_new_routine),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Steps
        itemsIndexed(routine.steps) { index, step ->
            val isCompleted = index in completedSteps
            val isActive = index == activeStep

            StepCard(
                step = step,
                stepNumber = index + 1,
                isCompleted = isCompleted,
                isActive = isActive,
                onComplete = { onCompleteStep(index) },
                onNavigate = { step.navTarget?.let { onNavigate(it) } },
            )
        }

        // Completion message
        if (completedSteps.size == routine.steps.size) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.cd_practice_complete),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.routine_complete_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.routine_complete_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single practice step card.
 */
@Composable
private fun StepCard(
    step: PracticeStep,
    stepNumber: Int,
    isCompleted: Boolean,
    isActive: Boolean,
    onComplete: () -> Unit,
    onNavigate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isActive -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Step number / check
            if (isCompleted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.cd_completed),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                ) {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${step.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isCompleted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (step.navTarget != null) {
                        OutlinedButton(onClick = onNavigate) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.label_go),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    OutlinedButton(onClick = onComplete) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.label_done),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

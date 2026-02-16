package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.data.CustomFingerpickingPattern
import com.baijum.ukufretboard.data.CustomFingerpickingPatternRepository
import com.baijum.ukufretboard.data.CustomStrumPattern
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.Difficulty
import com.baijum.ukufretboard.data.Finger
import com.baijum.ukufretboard.data.FingerpickStep
import com.baijum.ukufretboard.data.FingerpickingPattern
import com.baijum.ukufretboard.data.FingerpickingPatterns
import com.baijum.ukufretboard.data.StrumBeat
import com.baijum.ukufretboard.data.StrumDirection
import com.baijum.ukufretboard.data.StrumPattern
import com.baijum.ukufretboard.data.StrumPatterns

private const val TAB_STRUMMING = 0
private const val TAB_FINGERPICKING = 1

/**
 * Tab showing a reference list of common ukulele strumming and fingerpicking patterns.
 *
 * A toggle at the top switches between Strumming and Fingerpicking views.
 * Users can create custom patterns via the FAB on either tab.
 */
@Composable
fun StrumPatternsTab(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strumRepo = remember { CustomStrumPatternRepository(context) }
    val fingerpickRepo = remember { CustomFingerpickingPatternRepository(context) }
    var customStrumPatterns by remember { mutableStateOf(strumRepo.getAll()) }
    var customFingerpickPatterns by remember { mutableStateOf(fingerpickRepo.getAll()) }
    var selectedTab by remember { mutableIntStateOf(TAB_STRUMMING) }
    var showCreateStrumSheet by remember { mutableStateOf(false) }
    var showCreateFingerpickSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            // Toggle chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                FilterChip(
                    selected = selectedTab == TAB_STRUMMING,
                    onClick = { selectedTab = TAB_STRUMMING },
                    label = { Text("Strumming") },
                )
                FilterChip(
                    selected = selectedTab == TAB_FINGERPICKING,
                    onClick = { selectedTab = TAB_FINGERPICKING },
                    label = { Text("Fingerpicking") },
                )
            }

            when (selectedTab) {
                TAB_STRUMMING -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    // Custom patterns section
                    if (customStrumPatterns.isNotEmpty()) {
                        item {
                            Text(
                                text = "My Patterns",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        items(customStrumPatterns, key = { it.id }) { custom ->
                            StrumPatternCard(
                                pattern = custom.pattern,
                                onDelete = {
                                    strumRepo.delete(custom.id)
                                    customStrumPatterns = strumRepo.getAll()
                                },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Presets",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                    }
                    items(StrumPatterns.ALL) { pattern ->
                        StrumPatternCard(pattern = pattern)
                    }
                }
                TAB_FINGERPICKING -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    // Custom fingerpicking patterns section
                    if (customFingerpickPatterns.isNotEmpty()) {
                        item {
                            Text(
                                text = "My Patterns",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        items(customFingerpickPatterns, key = { it.id }) { custom ->
                            FingerpickingPatternCard(
                                pattern = custom.pattern,
                                onDelete = {
                                    fingerpickRepo.delete(custom.id)
                                    customFingerpickPatterns = fingerpickRepo.getAll()
                                },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Presets",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                    }
                    items(FingerpickingPatterns.ALL) { pattern ->
                        FingerpickingPatternCard(pattern = pattern)
                    }
                }
            }
        }

        // FAB for creating custom patterns
        FloatingActionButton(
            onClick = {
                when (selectedTab) {
                    TAB_STRUMMING -> showCreateStrumSheet = true
                    TAB_FINGERPICKING -> showCreateFingerpickSheet = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create Pattern")
        }
    }

    if (showCreateStrumSheet) {
        CreateStrumPatternSheet(
            onDismiss = { showCreateStrumSheet = false },
            onSave = { pattern ->
                strumRepo.save(CustomStrumPattern(pattern = pattern))
                customStrumPatterns = strumRepo.getAll()
                showCreateStrumSheet = false
            },
        )
    }

    if (showCreateFingerpickSheet) {
        CreateFingerpickingPatternSheet(
            onDismiss = { showCreateFingerpickSheet = false },
            onSave = { pattern ->
                fingerpickRepo.save(CustomFingerpickingPattern(pattern = pattern))
                customFingerpickPatterns = fingerpickRepo.getAll()
                showCreateFingerpickSheet = false
            },
        )
    }
}

/**
 * A card displaying a single strumming pattern.
 *
 * @param onDelete If non-null, a delete button is shown (for custom patterns).
 */
@Composable
private fun StrumPatternCard(pattern: StrumPattern, onDelete: (() -> Unit)? = null) {
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
            // Header: name + difficulty badge + optional delete
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
                DifficultyBadge(difficulty = pattern.difficulty)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual beat display
            BeatDisplay(beats = pattern.beats)

            Spacer(modifier = Modifier.height(6.dp))

            // Notation text
            Text(
                text = pattern.notation,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = pattern.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tempo range
            Text(
                text = "${pattern.suggestedBpm.first}–${pattern.suggestedBpm.last} BPM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * Visual display of beat arrows in a row.
 */
@Composable
private fun BeatDisplay(beats: List<StrumBeat>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        beats.forEach { beat ->
            BeatArrow(beat = beat)
        }
    }
}

/**
 * A single beat arrow indicator.
 */
@Composable
private fun BeatArrow(beat: StrumBeat) {
    val color = when (beat.direction) {
        StrumDirection.DOWN -> MaterialTheme.colorScheme.primary
        StrumDirection.UP -> MaterialTheme.colorScheme.secondary
        StrumDirection.MISS -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        StrumDirection.PAUSE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val fontWeight = if (beat.emphasis) FontWeight.ExtraBold else FontWeight.Normal
    val fontSize = if (beat.emphasis) 20.sp else 16.sp

    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = beat.direction.symbol,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A small badge showing the difficulty level.
 */
@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val bgColor = when (difficulty) {
        Difficulty.BEGINNER -> MaterialTheme.colorScheme.primaryContainer
        Difficulty.INTERMEDIATE -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when (difficulty) {
        Difficulty.BEGINNER -> MaterialTheme.colorScheme.onPrimaryContainer
        Difficulty.INTERMEDIATE -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = difficulty.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}

// ── Fingerpicking ───────────────────────────────────────────────────

/**
 * A card displaying a single fingerpicking pattern.
 *
 * @param onDelete If non-null, a delete button is shown (for custom patterns).
 */
@Composable
private fun FingerpickingPatternCard(pattern: FingerpickingPattern, onDelete: (() -> Unit)? = null) {
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
            // Header: name + difficulty badge + optional delete
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
                DifficultyBadge(difficulty = pattern.difficulty)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual finger step display
            FingerpickStepDisplay(steps = pattern.steps)

            Spacer(modifier = Modifier.height(6.dp))

            // Notation text
            Text(
                text = pattern.notation,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = pattern.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tempo range
            Text(
                text = "${pattern.suggestedBpm.first}–${pattern.suggestedBpm.last} BPM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * Visual display of fingerpicking steps showing finger labels and target strings.
 */
@Composable
private fun FingerpickStepDisplay(steps: List<FingerpickStep>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        steps.forEach { step ->
            FingerpickStepIndicator(step = step)
        }
    }
}

/**
 * A single fingerpicking step indicator showing the finger label
 * and target string name.
 */
@Composable
private fun FingerpickStepIndicator(step: FingerpickStep) {
    val fingerColor = when (step.finger) {
        Finger.THUMB -> MaterialTheme.colorScheme.primary
        Finger.INDEX -> MaterialTheme.colorScheme.secondary
        Finger.MIDDLE -> MaterialTheme.colorScheme.tertiary
        Finger.RING -> MaterialTheme.colorScheme.error
    }
    val fontWeight = if (step.emphasis) FontWeight.ExtraBold else FontWeight.Normal

    Column(
        modifier = Modifier
            .size(width = 36.dp, height = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = step.finger.label,
            fontSize = if (step.emphasis) 16.sp else 14.sp,
            fontWeight = fontWeight,
            color = fingerColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = FingerpickingPatterns.STRING_NAMES[step.stringIndex],
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Create custom strum pattern sheet ───────────────────────────────

/**
 * Bottom sheet for creating a custom strumming pattern.
 *
 * Users tap beat slots to cycle through DOWN/UP/MISS/PAUSE directions,
 * then name and save the pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateStrumPatternSheet(
    onDismiss: () -> Unit,
    onSave: (StrumPattern) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var patternName by remember { mutableStateOf("") }
    val beats = remember {
        mutableStateListOf(
            StrumBeat(StrumDirection.DOWN, emphasis = true),
            StrumBeat(StrumDirection.UP),
            StrumBeat(StrumDirection.DOWN),
            StrumBeat(StrumDirection.UP),
            StrumBeat(StrumDirection.DOWN),
            StrumBeat(StrumDirection.UP),
            StrumBeat(StrumDirection.DOWN),
            StrumBeat(StrumDirection.UP),
        )
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
                text = "Create Strum Pattern",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).semantics { heading() },
            )

            OutlinedTextField(
                value = patternName,
                onValueChange = { patternName = it },
                label = { Text("Pattern Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap each beat to change direction",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Editable beat slots
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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

            // Emphasis toggles
            Text(
                text = "Tap below to toggle accent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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

            // Save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (patternName.isNotBlank()) {
                            val notation = beats.joinToString(" ") { it.direction.symbol }
                            onSave(
                                StrumPattern(
                                    name = patternName.trim(),
                                    description = "Custom pattern",
                                    difficulty = Difficulty.BEGINNER,
                                    beats = beats.toList(),
                                    notation = notation,
                                    suggestedBpm = 80..120,
                                ),
                            )
                        }
                    },
                    enabled = patternName.isNotBlank(),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

// ── Create custom fingerpicking pattern sheet ──────────────────────

/**
 * Bottom sheet for creating a custom fingerpicking pattern.
 *
 * Users select a step, then pick finger and string from chip rows below.
 * Steps can be added or removed (2–8). Emphasis toggles accent individual steps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFingerpickingPatternSheet(
    onDismiss: () -> Unit,
    onSave: (FingerpickingPattern) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var patternName by remember { mutableStateOf("") }
    val steps = remember {
        mutableStateListOf(
            FingerpickStep(Finger.THUMB, 0, emphasis = true),
            FingerpickStep(Finger.THUMB, 1),
            FingerpickStep(Finger.INDEX, 2),
            FingerpickStep(Finger.MIDDLE, 3),
        )
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
                text = "Create Fingerpicking Pattern",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).semantics { heading() },
            )

            OutlinedTextField(
                value = patternName,
                onValueChange = { patternName = it },
                label = { Text("Pattern Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step count controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${steps.size} steps",
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
                text = "Tap a step, then pick finger and string below",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Editable step slots
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

            // Emphasis toggles
            Text(
                text = "Tap below to toggle accent",
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

            // Finger selector
            Text(
                text = "Finger",
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
                                    Finger.THUMB -> "Thumb"
                                    Finger.INDEX -> "Index"
                                    Finger.MIDDLE -> "Middle"
                                    Finger.RING -> "Ring"
                                },
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // String selector
            Text(
                text = "String",
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

            // Save / Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
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
                                    description = "Custom pattern",
                                    difficulty = Difficulty.BEGINNER,
                                    steps = steps.toList(),
                                    notation = notation,
                                    suggestedBpm = 60..100,
                                ),
                            )
                        }
                    },
                    enabled = patternName.isNotBlank(),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

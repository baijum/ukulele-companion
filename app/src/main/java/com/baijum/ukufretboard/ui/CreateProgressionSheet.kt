package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progressions
import com.baijum.ukufretboard.data.ScaleType

/**
 * Modal bottom sheet for creating or editing a custom chord progression.
 *
 * The user selects a scale type, taps diatonic chord chips to build
 * a sequence, names the progression, and saves it. The progression
 * is stored as scale-degree intervals, so it works in any key.
 *
 * When [initialName] and [initialDegrees] are provided, the sheet
 * opens in edit mode with pre-filled values.
 *
 * @param selectedRoot The currently selected key root (for preview chord names).
 * @param selectedScale The currently selected scale type.
 * @param initialName Pre-filled name for edit mode (empty for create mode).
 * @param initialDescription Pre-filled description for edit mode (empty for create mode).
 * @param initialDegrees Pre-filled chord degrees for edit mode (empty for create mode).
 * @param initialScaleType Pre-filled scale type for edit mode (null uses [selectedScale]).
 * @param onSave Callback with (name, description, degrees, scaleType) when the user saves.
 * @param onDismiss Callback when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateProgressionSheet(
    selectedRoot: Int,
    selectedScale: ScaleType,
    initialName: String = "",
    initialDescription: String = "",
    initialDegrees: List<ChordDegree> = emptyList(),
    initialScaleType: ScaleType? = null,
    onSave: (name: String, description: String, degrees: List<ChordDegree>, scaleType: ScaleType) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditMode = initialName.isNotEmpty() || initialDegrees.isNotEmpty()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var scaleType by remember { mutableStateOf(initialScaleType ?: selectedScale) }
    val chosenDegrees = remember { mutableStateListOf<ChordDegree>().also { it.addAll(initialDegrees) } }

    val availableDegrees = Progressions.diatonicDegrees(scaleType)

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
            // Title
            Text(
                text = if (isEditMode) "Edit Progression" else "Create Progression",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Progression name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scale type toggle
            Text(
                text = "Scale",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScaleType.entries.forEach { scale ->
                    FilterChip(
                        selected = scaleType == scale,
                        onClick = {
                            if (scaleType != scale) {
                                scaleType = scale
                                chosenDegrees.clear() // Reset when scale changes
                            }
                        },
                        label = { Text(scale.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Available diatonic chords
            Text(
                text = "Tap chords to add",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableDegrees.forEach { degree ->
                    val chordRoot = (selectedRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                    val chordName = Notes.enharmonicForKey(chordRoot, selectedRoot) + degree.quality
                    SuggestionChip(
                        onClick = { chosenDegrees.add(degree) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = chordName,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = degree.numeral,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chosen progression sequence
            Text(
                text = "Your progression (${chosenDegrees.size} chords)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (chosenDegrees.isEmpty()) {
                Text(
                    text = "Tap the chords above to build your progression",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    chosenDegrees.forEachIndexed { index, degree ->
                        if (index > 0) {
                            Text(
                                text = "\u2192", // →
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val chordRoot = (selectedRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                        val chordName = Notes.enharmonicForKey(chordRoot, selectedRoot) + degree.quality

                        InputChip(
                            selected = false,
                            onClick = { chosenDegrees.removeAt(index) },
                            label = { Text(chordName) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    modifier = androidx.compose.ui.Modifier.padding(0.dp),
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                // Numeral notation preview
                Text(
                    text = chosenDegrees.joinToString(" \u2013 ") { it.numeral }, // –
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    onSave(name.trim(), description.trim(), chosenDegrees.toList(), scaleType)
                },
                enabled = name.isNotBlank() && chosenDegrees.size >= 2,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditMode) "Update Progression" else "Save Progression")
            }

            if (name.isNotBlank() && chosenDegrees.size < 2) {
                Text(
                    text = "Add at least 2 chords",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

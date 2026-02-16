package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.CustomProgression
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.Progressions
import com.baijum.ukufretboard.data.ScaleType
import com.baijum.ukufretboard.domain.CapoCalculator
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.domain.HarmonicFunction
import com.baijum.ukufretboard.domain.VoiceLeading
import com.baijum.ukufretboard.domain.harmonicFunction
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * Tab showing common chord progressions for a selected key.
 *
 * Users select a key (root note) and scale (Major/Minor), then browse
 * common progressions. Tapping a chord chip calls [onChordTapped] with
 * the pitch class and quality so the caller can navigate to the fretboard.
 *
 * A "Voice Leading" button on each progression card computes and displays
 * the optimal voicing path through the progression using the
 * [VoiceLeadingView].
 *
 * Custom (user-created) progressions appear above the presets and can be
 * created via a bottom sheet and deleted individually.
 *
 * @param leftHanded Whether to mirror diagrams for left-handed players.
 * @param tuning Current ukulele tuning for voicing generation.
 * @param customProgressions User-created progressions from the ViewModel.
 * @param onChordTapped Callback with (rootPitchClass, qualitySymbol) when a chord is tapped.
 * @param onSaveProgression Callback to save a new custom progression.
 * @param onDeleteProgression Callback to delete a custom progression by ID.
 * @param onPlayVoicing Callback to play a single voicing (null if sound disabled).
 * @param onPlayAll Callback to play all voicings in sequence (null if sound disabled).
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionsTab(
    leftHanded: Boolean = false,
    tuning: List<UkuleleString>,
    lastFret: Int = 12,
    customProgressions: List<CustomProgression> = emptyList(),
    onChordTapped: (rootPitchClass: Int, quality: String) -> Unit,
    onSaveProgression: (name: String, description: String, degrees: List<ChordDegree>, scaleType: ScaleType) -> Unit = { _, _, _, _ -> },
    onEditProgression: (id: String, name: String, description: String, degrees: List<ChordDegree>, scaleType: ScaleType) -> Unit = { _, _, _, _, _ -> },
    onDeleteProgression: (id: String) -> Unit = {},
    onPlayVoicing: ((ChordVoicing) -> Unit)? = null,
    onPlayAll: ((List<ChordVoicing>) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedRoot by remember { mutableIntStateOf(0) } // C
    var selectedScale by remember { mutableStateOf(ScaleType.MAJOR) }
    var voiceLeadingPath by remember { mutableStateOf<VoiceLeading.Path?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingProgression by remember { mutableStateOf<CustomProgression?>(null) }
    var capoResults by remember { mutableStateOf<List<CapoCalculator.ProgressionResult>?>(null) }
    var playbackProgression by remember { mutableStateOf<Progression?>(null) }
    var practiceProgression by remember { mutableStateOf<Progression?>(null) }
    var practiceKeyRoot by remember { mutableIntStateOf(0) }

    val progressions = Progressions.forScale(selectedScale)
    // Custom progressions filtered by current scale type
    val filteredCustom = customProgressions.filter { it.progression.scaleType == selectedScale }

    // Capo helper mode — replaces normal content
    if (capoResults != null) {
        CapoCalculatorProgressionView(
            results = capoResults!!,
            onBack = { capoResults = null },
            leftHanded = leftHanded,
            modifier = modifier,
        )
        return
    }

    // Voice leading mode — replaces normal content
    if (voiceLeadingPath != null) {
        VoiceLeadingView(
            path = voiceLeadingPath!!,
            tuning = tuning,
            onBack = { voiceLeadingPath = null },
            onPlayVoicing = onPlayVoicing,
            onPlayAll = onPlayAll,
            leftHanded = leftHanded,
            modifier = modifier,
        )
        return
    }

    // Practice mode — replaces normal content
    if (practiceProgression != null) {
        ProgressionPracticeView(
            progression = practiceProgression!!,
            keyRoot = practiceKeyRoot,
            tuning = tuning,
            lastFret = lastFret,
            leftHanded = leftHanded,
            onPlayVoicing = onPlayVoicing,
            onBack = { practiceProgression = null },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        // Key selector
        Text(
            text = "Key",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )

        val noteNames = Notes.NOTE_NAMES_STANDARD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            noteNames.forEachIndexed { index, name ->
                FilterChip(
                    selected = index == selectedRoot,
                    onClick = { selectedRoot = index },
                    label = {
                        Text(
                            text = name,
                            fontWeight = if (index == selectedRoot) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scale toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScaleType.entries.forEach { scale ->
                FilterChip(
                    selected = selectedScale == scale,
                    onClick = { selectedScale = scale },
                    label = { Text(scale.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback bar (shown when a progression is being played)
        if (playbackProgression != null) {
            ProgressionPlaybackBar(
                progression = playbackProgression!!,
                keyRoot = selectedRoot,
                tuning = tuning,
                onPlayVoicing = onPlayVoicing,
                onDismiss = { playbackProgression = null },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // Progression cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // "New Progression" button
            item {
                OutlinedButton(
                    onClick = { showCreateSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add new progression",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Progression")
                }
            }

            // Custom progressions
            if (filteredCustom.isNotEmpty()) {
                item {
                    Text(
                        text = "My Progressions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(filteredCustom, key = { it.id }) { custom ->
                    ProgressionCard(
                        progression = custom.progression,
                        keyRoot = selectedRoot,
                        onChordTapped = onChordTapped,
                        onVoiceLeading = {
                            val path = VoiceLeading.computeOptimalPath(
                                progression = custom.progression,
                                keyRoot = selectedRoot,
                                tuning = tuning,
                            )
                            if (path != null) {
                                voiceLeadingPath = path
                            }
                        },
                        onCapo = {
                            capoResults = CapoCalculator.forProgression(
                                progression = custom.progression,
                                keyRoot = selectedRoot,
                                tuning = tuning,
                            )
                        },
                        onShare = {
                            val text = ChordSheetFormatter.formatProgression(custom.progression, selectedRoot)
                            ChordSheetFormatter.shareText(context, custom.progression.name, text)
                        },
                        onPlay = { playbackProgression = custom.progression },
                        onPractice = {
                            practiceProgression = custom.progression
                            practiceKeyRoot = selectedRoot
                        },
                        onDelete = { onDeleteProgression(custom.id) },
                        onDuplicate = {
                            onSaveProgression(
                                "${custom.progression.name} (Copy)",
                                custom.progression.description,
                                custom.progression.degrees,
                                custom.progression.scaleType,
                            )
                        },
                        onEdit = { editingProgression = custom },
                    )
                }

                item {
                    Text(
                        text = "Presets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Preset progressions
            items(progressions) { progression ->
                ProgressionCard(
                    progression = progression,
                    keyRoot = selectedRoot,
                    onChordTapped = onChordTapped,
                    onVoiceLeading = {
                        val path = VoiceLeading.computeOptimalPath(
                            progression = progression,
                            keyRoot = selectedRoot,
                            tuning = tuning,
                        )
                        if (path != null) {
                            voiceLeadingPath = path
                        }
                    },
                    onCapo = {
                        capoResults = CapoCalculator.forProgression(
                            progression = progression,
                            keyRoot = selectedRoot,
                            tuning = tuning,
                        )
                    },
                    onShare = {
                        val text = ChordSheetFormatter.formatProgression(progression, selectedRoot)
                        ChordSheetFormatter.shareText(context, progression.name, text)
                    },
                    onPlay = { playbackProgression = progression },
                    onPractice = {
                        practiceProgression = progression
                        practiceKeyRoot = selectedRoot
                    },
                    onDuplicate = {
                        onSaveProgression(
                            "${progression.name} (Copy)",
                            progression.description,
                            progression.degrees,
                            progression.scaleType,
                        )
                    },
                )
            }
        }
    }

    // Create progression bottom sheet
    if (showCreateSheet) {
        CreateProgressionSheet(
            selectedRoot = selectedRoot,
            selectedScale = selectedScale,
            onSave = { name, description, degrees, scaleType ->
                onSaveProgression(name, description, degrees, scaleType)
                showCreateSheet = false
            },
            onDismiss = { showCreateSheet = false },
        )
    }

    // Edit progression bottom sheet
    val editing = editingProgression
    if (editing != null) {
        CreateProgressionSheet(
            selectedRoot = selectedRoot,
            selectedScale = selectedScale,
            initialName = editing.progression.name,
            initialDescription = editing.progression.description,
            initialDegrees = editing.progression.degrees,
            initialScaleType = editing.progression.scaleType,
            onSave = { name, description, degrees, scaleType ->
                onEditProgression(editing.id, name, description, degrees, scaleType)
                editingProgression = null
            },
            onDismiss = { editingProgression = null },
        )
    }
}

/**
 * A card displaying a single chord progression with voice leading and optional delete.
 *
 * @param onDelete If non-null, a delete button is shown (for custom progressions).
 */
@Composable
private fun ProgressionCard(
    progression: Progression,
    keyRoot: Int,
    onChordTapped: (Int, String) -> Unit,
    onVoiceLeading: () -> Unit,
    onCapo: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onPractice: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Name row (with optional delete button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = progression.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete progression",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Numeral notation
            Text(
                text = progression.degrees.joinToString(" \u2013 ") { it.numeral },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Resolved chord chips with harmonic function labels
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                progression.degrees.forEach { degree ->
                    val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                    val chordName = Notes.enharmonicForKey(chordRoot, keyRoot) + degree.quality
                    val function = harmonicFunction(degree.numeral, progression.scaleType)
                    val functionColor = when (function) {
                        HarmonicFunction.TONIC -> MaterialTheme.colorScheme.primary
                        HarmonicFunction.SUBDOMINANT -> MaterialTheme.colorScheme.tertiary
                        HarmonicFunction.DOMINANT -> MaterialTheme.colorScheme.error
                    }

                    SuggestionChip(
                        onClick = { onChordTapped(chordRoot, degree.quality) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = chordName,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = function.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = functionColor,
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = progression.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action buttons — two rows
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Primary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    TextButton(onClick = onPractice) {
                        Text("Practice")
                    }
                    if (onDuplicate != null) {
                        IconButton(onClick = onDuplicate) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Duplicate",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                // Row 2: Secondary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    TextButton(onClick = onCapo) {
                        Text("Capo")
                    }
                    TextButton(onClick = onVoiceLeading) {
                        Text("Voice Leading")
                    }
                }
            }
        }
    }
}

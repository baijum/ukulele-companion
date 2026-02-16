package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordCategory
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.CapoCalculator
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.ChordLibraryViewModel
import com.baijum.ukufretboard.viewmodel.FretboardViewModel

/**
 * The chord library tab content.
 *
 * Provides a three-level selection flow:
 * 1. Root note (12 chromatic notes)
 * 2. Category (Triad, Seventh, Suspended, Extended)
 * 3. Chord type (formulas within the selected category)
 *
 * Below the selectors, a grid of mini chord diagrams shows all playable
 * voicings. Tapping a diagram calls [onVoicingSelected] to apply it
 * to the fretboard.
 *
 * @param viewModel The [ChordLibraryViewModel] managing selection state.
 * @param onVoicingSelected Callback invoked when the user taps a voicing diagram.
 * @param modifier Optional [Modifier] for layout customization.
 */
@Composable
fun ChordLibraryTab(
    viewModel: ChordLibraryViewModel,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onVoicingLongPressed: ((ChordVoicing) -> Unit)? = null,
    isFavorite: ((ChordVoicing) -> Boolean)? = null,
    onFavoriteClick: ((ChordVoicing) -> Unit)? = null,
    onPlayVoicing: ((ChordVoicing) -> Unit)? = null,
    onPlayVoicingsSequentially: ((List<ChordVoicing>) -> Unit)? = null,
    leftHanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val tuning = FretboardViewModel.STANDARD_TUNING

    // Inversion filter state: null = show all
    var inversionFilter by remember { mutableStateOf<ChordInfo.Inversion?>(null) }
    // Compare mode toggle
    var compareMode by remember { mutableStateOf(false) }
    // Capo calculator mode
    var capoResults by remember { mutableStateOf<List<CapoCalculator.SingleChordResult>?>(null) }
    // Capo visualizer mode — stores the voicing to visualize
    var capoVisualVoicing by remember { mutableStateOf<ChordVoicing?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        // Section: Root note selector
        SectionLabel(stringResource(R.string.chord_library_root_note))
        RootNoteSelector(
            selectedRoot = uiState.selectedRoot,
            onRootSelected = viewModel::selectRoot,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Section: Category selector
        SectionLabel(stringResource(R.string.chord_library_type))
        CategorySelector(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = viewModel::selectCategory,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Section: Chord formula selector
        FormulaSelector(
            category = uiState.selectedCategory,
            selectedFormula = uiState.selectedFormula,
            selectedRoot = uiState.selectedRoot,
            onFormulaSelected = viewModel::selectFormula,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Section: Transpose controls
        if (uiState.selectedFormula != null) {
            TransposeControls(
                rootPitchClass = uiState.selectedRoot,
                chordSymbol = uiState.selectedFormula?.symbol ?: "",
                semitoneOffset = 0, // Stateless — offset is reflected in root selection
                originalRoot = uiState.selectedRoot,
                onTranspose = { viewModel.transpose(it) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.voicings.isNotEmpty() && uiState.selectedFormula != null) {
            val rootName = Notes.pitchClassToName(uiState.selectedRoot)
            val symbol = uiState.selectedFormula?.symbol ?: ""

            if (capoVisualVoicing != null && uiState.selectedFormula != null) {
                // ── Capo Visualizer mode ──
                CapoVisualizerView(
                    voicing = capoVisualVoicing!!,
                    rootPitchClass = uiState.selectedRoot,
                    formula = uiState.selectedFormula!!,
                    tuning = tuning,
                    leftHanded = leftHanded,
                    onBack = { capoVisualVoicing = null },
                    modifier = Modifier.weight(1f),
                )
            } else if (capoResults != null) {
                // ── Capo Calculator mode ──
                CapoCalculatorSingleView(
                    results = capoResults!!,
                    onBack = { capoResults = null },
                    leftHanded = leftHanded,
                    modifier = Modifier.weight(1f),
                )
            } else if (compareMode) {
                // ── Compare Inversions mode ──
                SectionLabel(rootName + symbol + stringResource(R.string.chord_library_compare_inversions))

                Spacer(modifier = Modifier.height(4.dp))

                // Group voicings by inversion
                val grouped = uiState.voicings.groupBy { voicing ->
                    ChordInfo.determineInversion(
                        voicing.frets,
                        uiState.selectedRoot,
                        uiState.selectedFormula!!,
                        tuning,
                    )
                }

                InversionCompareView(
                    grouped = grouped,
                    rootPitchClass = uiState.selectedRoot,
                    formula = uiState.selectedFormula!!,
                    onVoicingSelected = onVoicingSelected,
                    onPlayVoicing = onPlayVoicing,
                    onPlayAllInversions = onPlayVoicingsSequentially?.let { play ->
                        {
                            // Play the best (first) voicing from each inversion group
                            val bestVoicings = ChordInfo.Inversion.entries
                                .mapNotNull { inv -> grouped[inv]?.firstOrNull() }
                            play(bestVoicings)
                        }
                    },
                    leftHanded = leftHanded,
                    onExitCompare = { compareMode = false },
                    modifier = Modifier.weight(1f),
                )
            } else {
                // ── Normal grid mode ──

                // Pre-compute inversion counts for filter chip badges
                val inversionCounts = remember(uiState.voicings, uiState.selectedRoot, uiState.selectedFormula) {
                    uiState.voicings.groupingBy { voicing ->
                        ChordInfo.determineInversion(
                            voicing.frets,
                            uiState.selectedRoot,
                            uiState.selectedFormula!!,
                            tuning,
                        )
                    }.eachCount()
                }

                // Apply inversion filter to voicings
                val filteredVoicings = if (inversionFilter != null) {
                    uiState.voicings.filter { voicing ->
                        ChordInfo.determineInversion(
                            voicing.frets,
                            uiState.selectedRoot,
                            uiState.selectedFormula!!,
                            tuning,
                        ) == inversionFilter
                    }
                } else {
                    uiState.voicings
                }

                SectionLabel("$rootName$symbol — " + stringResource(R.string.chord_library_voicings, filteredVoicings.size))

                // Inversion filter chips + action buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        InversionFilterChips(
                            selected = inversionFilter,
                            onSelected = { inversionFilter = it },
                            hasSeventhIntervals = uiState.selectedFormula?.intervals?.any { it >= 10 } == true,
                            inversionCounts = inversionCounts,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val formula = uiState.selectedFormula ?: return@OutlinedButton
                                capoResults = CapoCalculator.forSingleChord(
                                    rootPitchClass = uiState.selectedRoot,
                                    formula = formula,
                                    tuning = tuning,
                                )
                            },
                        ) {
                            Text(stringResource(R.string.label_capo), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                // Open visualizer with the first voicing
                                capoVisualVoicing = filteredVoicings.firstOrNull()
                            },
                        ) {
                            Text(stringResource(R.string.chord_library_viz), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { compareMode = true }) {
                            Text(stringResource(R.string.chord_library_compare), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Build a contextual empty-state message when an inversion filter is active
                val inversionEmptySubtitle = if (inversionFilter != null && filteredVoicings.isEmpty()) {
                    stringResource(R.string.chord_library_reentrant_empty)
                } else {
                    null
                }

                VoicingGrid(
                    voicings = filteredVoicings,
                    onVoicingSelected = onVoicingSelected,
                    onVoicingLongPressed = onVoicingLongPressed,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick,
                    leftHanded = leftHanded,
                    rootPitchClass = uiState.selectedRoot,
                    formula = uiState.selectedFormula,
                    emptyTitle = if (inversionFilter != null) {
                        "No ${inversionFilter!!.label.lowercase()} voicings"
                    } else {
                        stringResource(R.string.chord_library_no_voicings)
                    },
                    emptySubtitle = inversionEmptySubtitle,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            // No voicings / no formula selected
            VoicingGrid(
                voicings = uiState.voicings,
                onVoicingSelected = onVoicingSelected,
                onVoicingLongPressed = onVoicingLongPressed,
                isFavorite = isFavorite,
                onFavoriteClick = onFavoriteClick,
                leftHanded = leftHanded,
                rootPitchClass = uiState.selectedRoot,
                formula = uiState.selectedFormula,
                emptyTitle = stringResource(R.string.chord_library_no_voicings),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A small bold section label.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

/**
 * Horizontal scrollable row of 12 note chips (C, C#, D, ... B).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootNoteSelector(
    selectedRoot: Int,
    onRootSelected: (Int) -> Unit,
) {
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
                onClick = { onRootSelected(index) },
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
}

/**
 * Row of 4 category filter chips (Triad, Seventh, Suspended, Extended).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selectedCategory: ChordCategory,
    onCategorySelected: (ChordCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ChordCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.label,
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                ),
            )
        }
    }
}

/**
 * Horizontal row of formula chips for the selected category.
 * Shows the chord name (root + symbol) for each formula.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormulaSelector(
    category: ChordCategory,
    selectedFormula: ChordFormula?,
    selectedRoot: Int,
    onFormulaSelected: (ChordFormula) -> Unit,
) {
    val formulas = ChordFormulas.BY_CATEGORY[category] ?: emptyList()
    val rootName = Notes.pitchClassToName(selectedRoot)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        formulas.forEach { formula ->
            val isSelected = formula == selectedFormula
            FilterChip(
                selected = isSelected,
                onClick = { onFormulaSelected(formula) },
                label = {
                    Text(
                        text = "$rootName${formula.symbol}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

/**
 * Emits inversion filter chips inline (no wrapping Row — caller provides the container).
 *
 * Each chip shows a voicing count badge (e.g., "1st Inv (3)") so users can see
 * at a glance how many voicings exist for each inversion before tapping.
 *
 * @param selected The currently selected inversion filter, or null for "All".
 * @param onSelected Callback when a filter chip is tapped.
 * @param hasSeventhIntervals Whether to show the "3rd Inv" chip (only for 7th chords).
 * @param inversionCounts Map of inversion type to voicing count, used for badge display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InversionFilterChips(
    selected: ChordInfo.Inversion?,
    onSelected: (ChordInfo.Inversion?) -> Unit,
    hasSeventhIntervals: Boolean = false,
    inversionCounts: Map<ChordInfo.Inversion, Int> = emptyMap(),
) {
    val totalCount = inversionCounts.values.sum()

    val options = buildList {
        add(null to stringResource(R.string.label_all))
        add(ChordInfo.Inversion.ROOT to stringResource(R.string.label_root))
        add(ChordInfo.Inversion.FIRST to "1st Inv")
        add(ChordInfo.Inversion.SECOND to "2nd Inv")
        if (hasSeventhIntervals) {
            add(ChordInfo.Inversion.THIRD to "3rd Inv")
        }
    }

    options.forEach { (inversion, label) ->
        val isSelected = inversion == selected
        val count = if (inversion == null) totalCount else inversionCounts[inversion] ?: 0
        val displayLabel = if (inversionCounts.isNotEmpty()) "$label ($count)" else label
        FilterChip(
            selected = isSelected,
            onClick = { onSelected(inversion) },
            label = {
                Text(
                    text = displayLabel,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        )
        Spacer(modifier = Modifier.width(6.dp))
    }
}

/**
 * Inversion comparison view — groups voicings by inversion type and
 * displays them in scrollable sections for side-by-side comparison.
 *
 * Each section shows the inversion type, bass note, a horizontal row
 * of voicing cards, and a play button. A "Play All Inversions" button
 * at the top plays one representative voicing from each inversion
 * sequentially so users can hear the harmonic difference.
 */
@Composable
private fun InversionCompareView(
    grouped: Map<ChordInfo.Inversion, List<ChordVoicing>>,
    rootPitchClass: Int,
    formula: ChordFormula,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onPlayVoicing: ((ChordVoicing) -> Unit)?,
    onPlayAllInversions: (() -> Unit)?,
    leftHanded: Boolean,
    onExitCompare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = FretboardViewModel.STANDARD_TUNING

    // Ordered list of inversions that actually have voicings
    val availableInversions = ChordInfo.Inversion.entries.filter { grouped.containsKey(it) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Educational tip + Play All + Back
        item {
            Text(
                text = stringResource(R.string.chord_library_inversion_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onPlayAllInversions != null && availableInversions.size > 1) {
                    Button(
                        onClick = onPlayAllInversions,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_all_inversions),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.chord_library_play_all_inversions))
                    }
                }
                OutlinedButton(onClick = onExitCompare) {
                    Text(stringResource(R.string.action_back))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // One section per inversion type
        availableInversions.forEach { inversion ->
            val voicings = grouped[inversion] ?: return@forEach
            val bestVoicing = voicings.first()
            val bassPc = ChordInfo.bassPitchClass(bestVoicing.frets, tuning)
            val bassName = Notes.pitchClassToName(bassPc)

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Section header: inversion label + bass note + play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = inversion.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.chord_library_bass_prefix) + bassName + " \u2022 " +
                                if (voicings.size == 1) stringResource(R.string.chord_library_voicing_singular, voicings.size)
                                else stringResource(R.string.chord_library_voicings, voicings.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (onPlayVoicing != null) {
                        IconButton(onClick = { onPlayVoicing(bestVoicing) }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_item, inversion.label),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Horizontal scrollable row of voicing cards
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(voicings) { voicing ->
                        val bassIndex = ChordInfo.findBassStringIndex(voicing.frets, tuning)
                        VerticalChordDiagram(
                            voicing = voicing,
                            onClick = { onVoicingSelected(voicing) },
                            leftHanded = leftHanded,
                            inversionLabel = null, // Already shown in section header
                            bassStringIndex = bassIndex,
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }
        }

        // Handle case: only one inversion type available
        if (availableInversions.size == 1) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.chord_library_reentrant_limited,
                        grouped[availableInversions.first()]?.size ?: 0,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

/**
 * Grid of mini chord diagram cards, 2 columns wide.
 *
 * Each card shows a chord diagram with an optional heart icon for
 * toggling favorites. When [rootPitchClass] and [formula] are provided,
 * each voicing is labeled with its inversion type and the bass note is highlighted.
 *
 * @param emptyTitle Primary message shown when no voicings are available.
 * @param emptySubtitle Optional secondary message providing context (e.g., why
 *   an inversion filter returned no results).
 */
@Composable
private fun VoicingGrid(
    voicings: List<ChordVoicing>,
    onVoicingSelected: (ChordVoicing) -> Unit,
    onVoicingLongPressed: ((ChordVoicing) -> Unit)? = null,
    isFavorite: ((ChordVoicing) -> Boolean)? = null,
    onFavoriteClick: ((ChordVoicing) -> Unit)? = null,
    leftHanded: Boolean = false,
    rootPitchClass: Int = 0,
    formula: ChordFormula? = null,
    emptyTitle: String = "No voicings found",
    emptySubtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val tuning = FretboardViewModel.STANDARD_TUNING

    if (voicings.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = emptyTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (emptySubtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = emptySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(voicings) { voicing ->
                // Compute inversion info for this voicing
                val inversionLabel = formula?.let {
                    ChordInfo.determineInversion(voicing.frets, rootPitchClass, it, tuning).label
                }
                val bassIndex = formula?.let {
                    ChordInfo.findBassStringIndex(voicing.frets, tuning)
                }

                VerticalChordDiagram(
                    voicing = voicing,
                    onClick = { onVoicingSelected(voicing) },
                    onLongClick = onVoicingLongPressed?.let { callback -> { callback(voicing) } },
                    leftHanded = leftHanded,
                    inversionLabel = inversionLabel,
                    bassStringIndex = bassIndex,
                    isFavorite = isFavorite?.invoke(voicing) == true,
                    onFavoriteClick = onFavoriteClick?.let { callback -> { callback(voicing) } },
                )
            }
        }
    }
}

package com.baijum.ukufretboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.CapoCalculator
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.viewmodel.ChordLibraryViewModel
import com.baijum.ukufretboard.domain.UkuleleString

@Composable
fun ChordLibraryTab(
    viewModel: ChordLibraryViewModel,
    tuning: List<UkuleleString>,
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val focusManager = LocalFocusManager.current

    var inversionFilter by rememberSaveable(stateSaver = nullableEnumSaver<ChordInfo.Inversion>()) { mutableStateOf<ChordInfo.Inversion?>(null) }
    var compareMode by rememberSaveable { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(true) }
    var capoResults by remember { mutableStateOf<List<CapoCalculator.SingleChordResult>?>(null) }
    var capoVisualVoicing by remember { mutableStateOf<ChordVoicing?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        ChordSearchBar(
            query = searchQuery,
            results = searchResults,
            onQueryChange = viewModel::updateSearchQuery,
            onResultSelected = { result ->
                val inversion = viewModel.selectSearchResult(result)
                inversionFilter = inversion
                filtersExpanded = false
                focusManager.clearFocus()
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        FilterToggleRow(
            expanded = filtersExpanded,
            onToggle = { filtersExpanded = !filtersExpanded },
            selectedRoot = uiState.selectedRoot,
            selectedCategory = uiState.selectedCategory,
            selectedFormula = uiState.selectedFormula,
        )

        AnimatedVisibility(visible = filtersExpanded) {
            Column {
                SectionLabel(stringResource(R.string.chord_library_root_note))
                RootNoteSelector(
                    selectedRoot = uiState.selectedRoot,
                    onRootSelected = viewModel::selectRoot,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SectionLabel(stringResource(R.string.chord_library_type))
                CategorySelector(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::selectCategory,
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormulaSelector(
                    category = uiState.selectedCategory,
                    selectedFormula = uiState.selectedFormula,
                    selectedRoot = uiState.selectedRoot,
                    onFormulaSelected = viewModel::selectFormula,
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.selectedFormula != null) {
                    TransposeControls(
                        rootPitchClass = uiState.selectedRoot,
                        chordSymbol = uiState.selectedFormula?.symbol ?: "",
                        semitoneOffset = 0,
                        originalRoot = uiState.selectedRoot,
                        onTranspose = { viewModel.transpose(it) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.voicings.isNotEmpty() && uiState.selectedFormula != null) {
            val rootName = Notes.pitchClassToName(uiState.selectedRoot)
            val symbol = uiState.selectedFormula?.symbol ?: ""

            if (capoVisualVoicing != null && uiState.selectedFormula != null) {
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
                CapoCalculatorSingleView(
                    results = capoResults!!,
                    onBack = { capoResults = null },
                    leftHanded = leftHanded,
                    modifier = Modifier.weight(1f),
                )
            } else if (compareMode) {
                SectionLabel(rootName + symbol + stringResource(R.string.chord_library_compare_inversions))

                Spacer(modifier = Modifier.height(4.dp))

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
                    tuning = tuning,
                    rootPitchClass = uiState.selectedRoot,
                    formula = uiState.selectedFormula!!,
                    onVoicingSelected = onVoicingSelected,
                    onPlayVoicing = onPlayVoicing,
                    onPlayAllInversions = onPlayVoicingsSequentially?.let { play ->
                        {
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

                val inversionEmptySubtitle = if (inversionFilter != null && filteredVoicings.isEmpty()) {
                    stringResource(R.string.chord_library_reentrant_empty)
                } else {
                    null
                }

                VoicingGrid(
                    voicings = filteredVoicings,
                    tuning = tuning,
                    onVoicingSelected = onVoicingSelected,
                    onVoicingLongPressed = onVoicingLongPressed,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick,
                    leftHanded = leftHanded,
                    rootPitchClass = uiState.selectedRoot,
                    formula = uiState.selectedFormula,
                    emptyTitle = if (inversionFilter != null) {
                        "No ${inversionFilter!!.localizedLabel().lowercase()} voicings"
                    } else {
                        stringResource(R.string.chord_library_no_voicings)
                    },
                    emptySubtitle = inversionEmptySubtitle,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            VoicingGrid(
                voicings = uiState.voicings,
                tuning = tuning,
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

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .semantics { heading() }
            .padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

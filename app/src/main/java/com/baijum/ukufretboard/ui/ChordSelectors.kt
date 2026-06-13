package com.baijum.ukufretboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordCategory
import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordInfo

@Composable
internal fun FilterToggleRow(
    expanded: Boolean,
    onToggle: () -> Unit,
    selectedRoot: Int,
    selectedCategory: ChordCategory,
    selectedFormula: ChordFormula?,
    modifier: Modifier = Modifier,
) {
    val rootName = Notes.pitchClassToName(selectedRoot)
    val summary = if (selectedFormula != null) {
        "$rootName${selectedFormula.symbol} — ${selectedCategory.localizedLabel()}"
    } else {
        "$rootName — ${selectedCategory.localizedLabel()}"
    }
    val showFilters = stringResource(R.string.chord_library_show_filters)
    val hideFilters = stringResource(R.string.chord_library_hide_filters)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics {
                contentDescription = if (expanded) hideFilters else "$summary. $showFilters"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (expanded) hideFilters else summary,
            style = if (expanded) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = if (expanded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootNoteSelector(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategorySelector(
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
                        text = category.localizedLabel(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormulaSelector(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InversionFilterChips(
    selected: ChordInfo.Inversion?,
    onSelected: (ChordInfo.Inversion?) -> Unit,
    hasSeventhIntervals: Boolean = false,
    inversionCounts: Map<ChordInfo.Inversion, Int> = emptyMap(),
) {
    val totalCount = inversionCounts.values.sum()

    val options = buildList {
        add(null to stringResource(R.string.label_all))
        add(ChordInfo.Inversion.ROOT to stringResource(R.string.label_root))
        add(ChordInfo.Inversion.FIRST to stringResource(R.string.chord_library_first_inv))
        add(ChordInfo.Inversion.SECOND to stringResource(R.string.chord_library_second_inv))
        if (hasSeventhIntervals) {
            add(ChordInfo.Inversion.THIRD to stringResource(R.string.chord_library_third_inv))
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

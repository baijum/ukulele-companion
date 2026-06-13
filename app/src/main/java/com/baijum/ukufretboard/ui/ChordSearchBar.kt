package com.baijum.ukufretboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordSearchResult

@Composable
internal fun ChordSearchBar(
    query: String,
    results: List<ChordSearchResult>,
    onQueryChange: (String) -> Unit,
    onResultSelected: (ChordSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchHint = stringResource(R.string.chord_library_search_hint)
    val clearDesc = stringResource(R.string.chord_library_search_clear)
    val noResultsText = stringResource(R.string.chord_library_search_no_results)

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = searchHint },
            placeholder = { Text(searchHint) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = clearDesc,
                        )
                    }
                }
            },
        )

        if (query.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                if (results.isEmpty()) {
                    Text(
                        text = noResultsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(16.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                } else {
                    Column {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 480.dp),
                        ) {
                            items(results, key = { "${it.rootPitchClass}_${it.formula.symbol}" }) { result ->
                                ChordSuggestionRow(
                                    result = result,
                                    onClick = { onResultSelected(result) },
                                )
                            }
                        }
                        if (results.size > 8) {
                            Text(
                                text = "${results.size} matches",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChordSuggestionRow(
    result: ChordSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qualityLabel = buildString {
        append(result.quality)
        result.inversion?.let { append(", ${it.localizedLabel()}") }
    }

    val rootName = Notes.pitchClassToName(result.rootPitchClass)
    val aliasText = if (result.formula.aliases.isNotEmpty()) {
        result.formula.aliases.joinToString(", ") { rootName + it }
    } else {
        null
    }

    val accessibilityLabel = buildString {
        append("${result.displayName}, $qualityLabel")
        if (aliasText != null) append(". Also: $aliasText")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics {
                contentDescription = accessibilityLabel
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (aliasText != null) {
                Text(
                    text = aliasText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = qualityLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

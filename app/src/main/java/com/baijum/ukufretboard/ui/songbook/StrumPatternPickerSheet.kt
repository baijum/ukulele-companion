package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.StrumPatterns

/**
 * Displays the associated strum pattern for a song, with options to select, change, or remove.
 */
@Composable
internal fun StrumPatternRow(
    patternName: String,
    onPatternChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    val resolvedPattern = remember(patternName) {
        if (patternName.isEmpty()) {
            null
        } else {
            StrumPatterns.ALL.find { it.name == patternName }
                ?: CustomStrumPatternRepository(context).getAll()
                    .find { it.pattern.name == patternName }?.pattern
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
    ) {
        if (resolvedPattern != null) {
            val patternDesc = stringResource(R.string.cd_strum_pattern, resolvedPattern.name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { contentDescription = patternDesc },
            ) {
                Text(
                    text = stringResource(R.string.songbook_strum_pattern) + ": ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = resolvedPattern.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = resolvedPattern.notation,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showPicker = true }) {
                    Text(
                        stringResource(R.string.songbook_strum_change),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TextButton(onClick = { onPatternChange("") }) {
                    Text(
                        stringResource(R.string.songbook_strum_remove),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.songbook_strum_pattern) + ": ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showPicker = true }) {
                    Text(stringResource(R.string.songbook_strum_select))
                }
            }
        }
    }

    if (showPicker) {
        StrumPatternPickerSheet(
            currentName = patternName,
            onSelect = { name ->
                onPatternChange(name)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrumPatternPickerSheet(
    currentName: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val customPatterns = remember {
        CustomStrumPatternRepository(context).getAll()
    }
    val allPatterns = remember(customPatterns) {
        val builtIn = StrumPatterns.ALL.map { it.name to it.notation }
        val custom = customPatterns.map { it.pattern.name to it.pattern.notation }
        builtIn + custom
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.songbook_strum_select),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 8.dp)
                    .semantics { heading() },
            )
            LazyColumn {
                items(allPatterns) { (name, notation) ->
                    val isSelected = name == currentName
                    val itemDesc = stringResource(R.string.cd_strum_pattern, name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onSelect(name) }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .semantics {
                                contentDescription = itemDesc
                                if (isSelected) stateDescription = "selected"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = notation,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

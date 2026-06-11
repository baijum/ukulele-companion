package com.baijum.ukufretboard.ui.melody

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.viewmodel.MelodyUiState

@Composable
internal fun MelodyTitleBar(
    state: MelodyUiState,
    onSave: () -> Unit,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSaveAs: () -> Unit,
    onLoad: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onNew: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.melody_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.melody_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.loadedMelodyName != null) {
                Text(
                    text = buildString {
                        append(state.loadedMelodyName)
                        if (state.hasUnsavedChanges) append(" *")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(
            onClick = onSave,
            enabled = state.notes.isNotEmpty(),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_save_melody),
            )
        }

        Box {
            IconButton(onClick = { onMenuToggle(true) }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_options),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onMenuToggle(false) },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_save_as)) },
                    onClick = { onMenuToggle(false); onSaveAs() },
                    enabled = state.notes.isNotEmpty(),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_load)) },
                    onClick = { onMenuToggle(false); onLoad() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_rename)) },
                    onClick = { onMenuToggle(false); onRename() },
                    enabled = state.loadedMelodyId != null,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.melody_new)) },
                    onClick = { onMenuToggle(false); onNew() },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.dialog_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { onMenuToggle(false); onDelete() },
                    enabled = state.loadedMelodyId != null,
                )
            }
        }
    }
}

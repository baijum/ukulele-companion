package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R

@Composable
internal fun DiscardChangesDialog(
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.songbook_discard_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.songbook_discard_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    stringResource(R.string.songbook_discard),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
internal fun DeleteSongDialog(
    songName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.song_delete_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.song_delete_message, songName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.dialog_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
internal fun PasteChordProDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pasteContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.songbook_paste_chordpro),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            OutlinedTextField(
                value = pasteContent,
                onValueChange = { pasteContent = it },
                label = { Text(stringResource(R.string.songbook_paste_chordpro_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(pasteContent) },
                enabled = pasteContent.isNotBlank(),
            ) {
                Text(stringResource(R.string.songbook_paste_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddLabelDialog(
    currentLabels: List<String>,
    allLabels: Set<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    val existingSuggestions = remember(allLabels, currentLabels) {
        (allLabels - currentLabels.toSet()).toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.songbook_add_label),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.songbook_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existingSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        existingSuggestions.forEach { label ->
                            AssistChip(
                                onClick = { onAdd(label) },
                                label = {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                },
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = input.trim()
                    if (trimmed.isNotEmpty() && trimmed !in currentLabels) {
                        onAdd(trimmed)
                    }
                },
                enabled = input.isNotBlank(),
            ) {
                Text(stringResource(R.string.songbook_add_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

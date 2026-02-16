package com.baijum.ukufretboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.FavoriteFolder

/**
 * A Material 3 bottom sheet for managing which folders a voicing belongs to.
 *
 * Shows checkboxes for each folder, an inline "New Folder" creator, and
 * optional "Remove from Favorites" action.
 *
 * @param folders All available folders.
 * @param selectedFolderIds The folder IDs the voicing currently belongs to.
 * @param isAlreadyFavorited Whether the voicing is already saved as a favorite.
 * @param onSave Called with the updated list of selected folder IDs.
 * @param onRemove Called when the user taps "Remove from Favorites".
 * @param onCreateFolder Called to create a new folder by name.
 * @param onDismiss Called when the sheet is dismissed without saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteFolderSheet(
    folders: List<FavoriteFolder>,
    selectedFolderIds: List<String>,
    isAlreadyFavorited: Boolean,
    onSave: (List<String>) -> Unit,
    onRemove: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val checkedIds = remember { mutableStateListOf<String>().apply { addAll(selectedFolderIds) } }
    var showNewFolderField by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Title
            Text(
                text = if (isAlreadyFavorited) stringResource(R.string.folder_sheet_manage) else stringResource(R.string.folder_sheet_save_to),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Folder checkboxes
            if (folders.isEmpty() && !showNewFolderField) {
                Text(
                    text = stringResource(R.string.folder_sheet_no_folders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                folders.forEach { folder ->
                    val isChecked = folder.id in checkedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) checkedIds.remove(folder.id) else checkedIds.add(folder.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (it) checkedIds.add(folder.id) else checkedIds.remove(folder.id)
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            // Divider before new folder
            if (folders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Inline new folder creation
            if (showNewFolderField) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text(stringResource(R.string.favorites_folder_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                onCreateFolder(newFolderName.trim())
                                newFolderName = ""
                                showNewFolderField = false
                            }
                        },
                        enabled = newFolderName.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.dialog_create))
                    }
                }
            } else {
                TextButton(
                    onClick = { showNewFolderField = true },
                ) {
                    Text(stringResource(R.string.folder_sheet_new_folder))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remove from favorites (only if already saved)
            if (isAlreadyFavorited) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.folder_sheet_remove),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { onSave(checkedIds.toList()) }) {
                    Text(stringResource(R.string.dialog_save))
                }
            }
        }
    }
}

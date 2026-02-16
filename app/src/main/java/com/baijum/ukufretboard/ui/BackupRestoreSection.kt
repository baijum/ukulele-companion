package com.baijum.ukufretboard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.viewmodel.BackupRestoreState
import com.baijum.ukufretboard.viewmodel.BackupRestoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup & Restore section displayed in the Settings bottom sheet.
 *
 * Provides two buttons:
 * - **Backup Data**: Exports all user data to a JSON file via SAF [CreateDocument].
 * - **Restore Data**: Imports data from a JSON file via SAF [OpenDocument].
 *
 * Shows the date of the last successful backup and the status of the current
 * operation (idle, exporting, importing, success, error).
 *
 * A confirmation dialog is shown before restoring to warn the user that
 * settings will be overwritten and other data will be merged.
 */
@Composable
fun BackupRestoreSection(
    viewModel: BackupRestoreViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // SAF launcher for creating a backup file
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    // SAF launcher for opening a backup file
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreConfirm = true
        }
    }

    val isOperating = state.state is BackupRestoreState.Exporting
            || state.state is BackupRestoreState.Importing

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = "BACKUP & RESTORE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        Text(
            text = "Save all your data (favorites, songs, progressions, patterns, " +
                "settings, and learning progress) to a file, or restore from a backup.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Last backup date
        state.lastBackupDate?.let { date ->
            Text(
                text = "Last backup: $date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Backup button
            Button(
                onClick = {
                    val filename = "ukulele-companion-backup-${todayString()}.json"
                    backupLauncher.launch(filename)
                },
                enabled = !isOperating,
                modifier = Modifier.weight(1f),
            ) {
                if (state.state is BackupRestoreState.Exporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        Icons.Filled.CloudUpload,
                        contentDescription = "Backup data",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup")
            }

            // Restore button
            OutlinedButton(
                onClick = {
                    restoreLauncher.launch(arrayOf("application/json"))
                },
                enabled = !isOperating,
                modifier = Modifier.weight(1f),
            ) {
                if (state.state is BackupRestoreState.Importing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = "Restore data",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore")
            }
        }

        // Status message
        when (val s = state.state) {
            is BackupRestoreState.Success -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is BackupRestoreState.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {}
        }
    }

    // Restore confirmation dialog
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirm = false
                pendingRestoreUri = null
            },
            title = { Text("Restore Data?") },
            text = {
                Text(
                    "This will merge favorites, songs, progressions, and patterns " +
                        "from the backup file with your existing data. Settings will " +
                        "be replaced with the backup values.\n\n" +
                        "Your existing data will not be deleted.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri?.let { viewModel.importBackup(it) }
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    pendingRestoreUri = null
                }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private fun todayString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

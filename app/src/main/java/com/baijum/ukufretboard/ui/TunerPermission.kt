package com.baijum.ukufretboard.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.baijum.ukufretboard.R

/**
 * Result of checking the RECORD_AUDIO permission state.
 */
enum class MicPermissionState {
    /** Permission has been granted — the tuner can proceed. */
    GRANTED,
    /** Permission has not yet been requested. */
    NOT_REQUESTED,
    /** Permission was denied (user can still be asked again). */
    DENIED,
    /** Permission was permanently denied — user must go to Settings. */
    PERMANENTLY_DENIED,
}

/**
 * A composable that handles the RECORD_AUDIO runtime permission flow.
 *
 * If permission is already granted, [onPermissionGranted] is invoked
 * immediately and no UI is shown. Otherwise, a rationale card is displayed
 * explaining why the microphone is needed, with a button to request the
 * permission or (if permanently denied) to open system Settings.
 *
 * @param onPermissionGranted Called once when the permission is confirmed.
 * @param content             Content to show when permission is granted.
 */
@Composable
fun RequireMicPermission(
    onPermissionGranted: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    var permState by remember {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        mutableStateOf(
            if (granted) MicPermissionState.GRANTED
            else MicPermissionState.NOT_REQUESTED
        )
    }

    // Track whether we've asked at least once so we can detect "permanent" denial.
    var hasAskedOnce by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permState = if (granted) {
            onPermissionGranted()
            MicPermissionState.GRANTED
        } else {
            hasAskedOnce = true
            MicPermissionState.PERMANENTLY_DENIED
        }
    }

    when (permState) {
        MicPermissionState.GRANTED -> {
            content()
        }
        MicPermissionState.PERMANENTLY_DENIED -> {
            MicPermissionDeniedCard(
                permanently = true,
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
            )
        }
        else -> {
            MicPermissionRationaleCard(
                onRequestPermission = {
                    permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            )
        }
    }
}

/**
 * Card explaining why the microphone is needed, with a button to request it.
 */
@Composable
private fun MicPermissionRationaleCard(
    onRequestPermission: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.mic_permission_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.mic_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.mic_permission_allow))
            }
        }
    }
}

/**
 * Card shown when the permission was permanently denied, directing the
 * user to system Settings.
 */
@Composable
private fun MicPermissionDeniedCard(
    permanently: Boolean,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.mic_permission_denied_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.mic_permission_denied_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.mic_permission_open_settings))
            }
        }
    }
}

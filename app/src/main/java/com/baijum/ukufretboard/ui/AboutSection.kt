package com.baijum.ukufretboard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R

@Composable
internal fun AboutSection() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: stringResource(R.string.settings_unknown_version)

    SettingsSectionHeader(stringResource(R.string.settings_about))

    Text(
        text = stringResource(R.string.app_full_name),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.settings_version, versionName),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.settings_developer),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.settings_website),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://baijum.github.io/ukulele-companion"))
            context.startActivity(intent)
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.settings_free_book),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/ukulele-book"))
            context.startActivity(intent)
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.settings_video_guide),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/playlist?list=PL4GycHdD--uonaifRHrBvNU7ym5jAVs8c"),
            )
            context.startActivity(intent)
        },
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.settings_credits),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    CreditItem(
        label = stringResource(R.string.settings_audio_samples),
        value = stringResource(R.string.settings_audio_samples_value),
    )
    CreditItem(
        label = stringResource(R.string.settings_license),
        value = stringResource(R.string.settings_license_value),
    )
    Spacer(modifier = Modifier.height(8.dp))

    var showLicenses by remember { mutableStateOf(false) }
    Text(
        text = stringResource(R.string.settings_open_source_licenses),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(role = Role.Button) { showLicenses = true },
    )
    if (showLicenses) {
        OpenSourceLicensesDialog(onDismiss = { showLicenses = false })
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.settings_tagline),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CreditItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class LicenseEntry(
    val name: String,
    val copyright: String,
    val licenseType: String,
    val url: String,
)

private val licenseEntries = listOf(
    LicenseEntry(
        name = "ONNX Runtime",
        copyright = "Copyright (c) Microsoft Corporation",
        licenseType = "MIT License",
        url = "https://github.com/microsoft/onnxruntime",
    ),
    LicenseEntry(
        name = "SwiftF0",
        copyright = "Copyright (c) lars76",
        licenseType = "MIT License",
        url = "https://github.com/lars76/swift-f0",
    ),
    LicenseEntry(
        name = "Reorderable",
        copyright = "Copyright 2023 Calvin Liang",
        licenseType = "Apache License 2.0",
        url = "https://github.com/Calvin-LL/Reorderable",
    ),
    LicenseEntry(
        name = "Kotlin",
        copyright = "Copyright 2010-2024 JetBrains s.r.o.",
        licenseType = "Apache License 2.0",
        url = "https://github.com/JetBrains/kotlin",
    ),
    LicenseEntry(
        name = "Jetpack Compose",
        copyright = "Copyright The Android Open Source Project",
        licenseType = "Apache License 2.0",
        url = "https://developer.android.com/compose",
    ),
    LicenseEntry(
        name = "AndroidX Libraries",
        copyright = "Copyright The Android Open Source Project",
        licenseType = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx",
    ),
    LicenseEntry(
        name = "Material Components for Android",
        copyright = "Copyright Google LLC",
        licenseType = "Apache License 2.0",
        url = "https://github.com/material-components/material-components-android",
    ),
    LicenseEntry(
        name = "Kotlinx Serialization",
        copyright = "Copyright 2017-2024 JetBrains s.r.o.",
        licenseType = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.serialization",
    ),
    LicenseEntry(
        name = "Kotlinx Coroutines",
        copyright = "Copyright 2016-2024 JetBrains s.r.o.",
        licenseType = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
    ),
)

@Composable
private fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_open_source_licenses),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                licenseEntries.forEach { entry ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = entry.licenseType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = entry.copyright,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = entry.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entry.url))
                                context.startActivity(intent)
                            },
                        )
                    }
                    if (entry != licenseEntries.last()) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

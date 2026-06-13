package com.baijum.ukufretboard.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.DisplaySettings
import com.baijum.ukufretboard.data.ThemeMode

@Composable
internal fun DisplaySection(
    settings: DisplaySettings,
    onSettingsChange: (DisplaySettings) -> Unit,
) {
    SettingsSectionHeader(stringResource(R.string.settings_display))

    Text(
        text = stringResource(R.string.settings_theme),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeMode.entries.take(2).forEach { mode ->
            FilterChip(
                selected = settings.themeMode == mode,
                onClick = { onSettingsChange(settings.copy(themeMode = mode)) },
                label = { Text(mode.localizedLabel()) },
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeMode.entries.drop(2).forEach { mode ->
            FilterChip(
                selected = settings.themeMode == mode,
                onClick = { onSettingsChange(settings.copy(themeMode = mode)) },
                label = { Text(mode.localizedLabel()) },
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    SettingsSwitch(
        label = stringResource(R.string.settings_show_explorer_tips),
        checked = settings.showExplorerTips,
        onCheckedChange = { onSettingsChange(settings.copy(showExplorerTips = it)) },
    )

    SettingsSwitch(
        label = stringResource(R.string.settings_show_learn_section),
        checked = settings.showLearnSection,
        onCheckedChange = { onSettingsChange(settings.copy(showLearnSection = it)) },
    )

    SettingsSwitch(
        label = stringResource(R.string.settings_show_reference_section),
        checked = settings.showReferenceSection,
        onCheckedChange = { onSettingsChange(settings.copy(showReferenceSection = it)) },
    )
}

private val SUPPORTED_LANGUAGES = linkedMapOf(
    "" to "System default",
    "en" to "English",
    "es" to "Español",
    "fr" to "Français",
    "pt" to "Português",
    "de" to "Deutsch",
    "ja" to "日本語",
    "zh-Hans" to "中文 (简体)",
    "ko" to "한국어",
    "hi" to "हिन्दी",
    "ar" to "العربية",
    "ru" to "Русский",
    "it" to "Italiano",
    "in" to "Bahasa Indonesia",
    "tr" to "Türkçe",
    "nl" to "Nederlands",
    "pl" to "Polski",
)

@Composable
internal fun LanguageSection() {
    SettingsSectionHeader(stringResource(R.string.settings_language))

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (currentLocales.isEmpty) "" else currentLocales.toLanguageTags().split(",").first()
    val currentLabel = SUPPORTED_LANGUAGES.entries.firstOrNull {
        it.key.equals(currentTag, ignoreCase = true)
    }?.value ?: SUPPORTED_LANGUAGES[""]!!

    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { showDialog = true },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = currentLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_select_language),
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    SUPPORTED_LANGUAGES.forEach { (tag, label) ->
                        val isSelected = tag.equals(currentTag, ignoreCase = true)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    val localeList = if (tag.isEmpty()) {
                                        LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        LocaleListCompat.forLanguageTags(tag)
                                    }
                                    AppCompatDelegate.setApplicationLocales(localeList)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

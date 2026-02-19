package com.baijum.ukufretboard.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.DisplaySettings
import com.baijum.ukufretboard.data.FretboardSettings

import com.baijum.ukufretboard.data.PitchMonitorSettings
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning

/**
 * A modal bottom sheet displaying all app settings, organized by section.
 *
 * Contains Sound, Display, Tuning, and Fretboard sections.
 *
 * @param soundSettings The current [SoundSettings] values to display.
 * @param onSoundSettingsChange Callback invoked when the user changes any sound setting.
 * @param displaySettings The current [DisplaySettings] values to display.
 * @param onDisplaySettingsChange Callback invoked when the user changes any display setting.
 * @param tuningSettings The current [TuningSettings] values to display.
 * @param onTuningSettingsChange Callback invoked when the user changes any tuning setting.
 * @param fretboardSettings The current [FretboardSettings] values to display.
 * @param onFretboardSettingsChange Callback invoked when the user changes any fretboard setting.
 * @param onDismiss Callback invoked when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    soundSettings: SoundSettings,
    onSoundSettingsChange: (SoundSettings) -> Unit,
    displaySettings: DisplaySettings,
    onDisplaySettingsChange: (DisplaySettings) -> Unit,
    tuningSettings: TuningSettings,
    onTuningSettingsChange: (TuningSettings) -> Unit,
    fretboardSettings: FretboardSettings,
    onFretboardSettingsChange: (FretboardSettings) -> Unit,

    tunerSettings: TunerSettings = TunerSettings(),
    onTunerSettingsChange: (TunerSettings) -> Unit = {},
    pitchMonitorSettings: PitchMonitorSettings = PitchMonitorSettings(),
    onPitchMonitorSettingsChange: (PitchMonitorSettings) -> Unit = {},
    backupRestoreViewModel: com.baijum.ukufretboard.viewmodel.BackupRestoreViewModel? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Sheet title
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .semantics { heading() },
            )

            // ── Sound section ──
            SoundSection(
                settings = soundSettings,
                onSettingsChange = onSoundSettingsChange,
            )

            // ── Display section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            DisplaySection(
                settings = displaySettings,
                onSettingsChange = onDisplaySettingsChange,
            )

            // ── Language section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            LanguageSection()

            // ── Tuning section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            TuningSection(
                settings = tuningSettings,
                onSettingsChange = onTuningSettingsChange,
            )

            // ── Tuner section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            TunerSection(
                settings = tunerSettings,
                onSettingsChange = onTunerSettingsChange,
            )

            // ── Pitch Monitor section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            PitchMonitorSection(
                settings = pitchMonitorSettings,
                onSettingsChange = onPitchMonitorSettingsChange,
            )

            // ── Fretboard section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            FretboardSection(
                settings = fretboardSettings,
                onSettingsChange = onFretboardSettingsChange,
            )


            // ── Backup & Restore section ──
            if (backupRestoreViewModel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                BackupRestoreSection(viewModel = backupRestoreViewModel)
            }

            // ── About section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            AboutSection()
        }
    }
}

/**
 * Section header label used to separate settings categories.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/**
 * The Sound settings section with all playback controls.
 */
@Composable
private fun SoundSection(
    settings: SoundSettings,
    onSettingsChange: (SoundSettings) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_sound))

    // Master toggle
    SettingsSwitch(
        label = stringResource(R.string.settings_sound),
        checked = settings.enabled,
        onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) },
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Volume slider
    SettingsSlider(
        label = stringResource(R.string.settings_volume),
        value = settings.volume,
        valueRange = SoundSettings.MIN_VOLUME..SoundSettings.MAX_VOLUME,
        valueLabel = "${(settings.volume * 100).toInt()}%",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(volume = it)) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Note duration slider
    SettingsSlider(
        label = stringResource(R.string.settings_note_duration),
        value = settings.noteDurationMs.toFloat(),
        valueRange = SoundSettings.MIN_NOTE_DURATION_MS.toFloat()..SoundSettings.MAX_NOTE_DURATION_MS.toFloat(),
        valueLabel = "${settings.noteDurationMs}ms",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(noteDurationMs = it.toInt())) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Strum delay slider
    SettingsSlider(
        label = stringResource(R.string.settings_strum_delay),
        value = settings.strumDelayMs.toFloat(),
        valueRange = SoundSettings.MIN_STRUM_DELAY_MS.toFloat()..SoundSettings.MAX_STRUM_DELAY_MS.toFloat(),
        valueLabel = "${settings.strumDelayMs}ms",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(strumDelayMs = it.toInt())) },
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Strum direction
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_strum_direction),
            style = MaterialTheme.typography.bodyLarge,
            color = if (settings.enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            FilterChip(
                selected = settings.strumDown,
                onClick = { onSettingsChange(settings.copy(strumDown = true)) },
                label = { Text(stringResource(R.string.settings_strum_down)) },
                enabled = settings.enabled,
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = !settings.strumDown,
                onClick = { onSettingsChange(settings.copy(strumDown = false)) },
                label = { Text(stringResource(R.string.settings_strum_up)) },
                enabled = settings.enabled,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Play on tap
    SettingsSwitch(
        label = stringResource(R.string.settings_play_on_tap),
        checked = settings.playOnTap,
        onCheckedChange = { onSettingsChange(settings.copy(playOnTap = it)) },
        enabled = settings.enabled,
    )
}

/**
 * The Display settings section with note naming and theme controls.
 */
@Composable
private fun DisplaySection(
    settings: DisplaySettings,
    onSettingsChange: (DisplaySettings) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_display))

    // Theme
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
                label = { Text(mode.label) },
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
                label = { Text(mode.label) },
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    SettingsSwitch(
        label = stringResource(R.string.settings_show_explorer_tips),
        checked = settings.showExplorerTips,
        onCheckedChange = { onSettingsChange(settings.copy(showExplorerTips = it)) },
    )
}

/**
 * Map of supported language tags to their native display names.
 *
 * The key is the BCP 47 tag used by [AppCompatDelegate.setApplicationLocales].
 * The value is the language name shown in the picker, written in that language
 * so users can recognise their own language regardless of the current locale.
 */
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

/**
 * Language picker section that lets users override the app locale.
 *
 * Uses [AppCompatDelegate.setApplicationLocales] which handles persistence
 * automatically and integrates with the Android 13+ per-app language system setting.
 */
@Composable
private fun LanguageSection() {
    SectionHeader(stringResource(R.string.settings_language))

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (currentLocales.isEmpty) "" else currentLocales.toLanguageTags().split(",").first()
    val currentLabel = SUPPORTED_LANGUAGES.entries.firstOrNull {
        it.key.equals(currentTag, ignoreCase = true)
    }?.value ?: SUPPORTED_LANGUAGES[""]!!

    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
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
                                .clickable {
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

/**
 * The Tuning settings section with tuning variant selection.
 */
@Composable
private fun TuningSection(
    settings: TuningSettings,
    onSettingsChange: (TuningSettings) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_tuning))

    Text(
        text = stringResource(R.string.settings_tuning),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Display tunings in rows of two chips
    val tunings = UkuleleTuning.entries
    for (i in tunings.indices step 2) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = settings.tuning == tunings[i],
                onClick = { onSettingsChange(settings.copy(tuning = tunings[i])) },
                label = { Text(tunings[i].label) },
            )
            if (i + 1 < tunings.size) {
                FilterChip(
                    selected = settings.tuning == tunings[i + 1],
                    onClick = { onSettingsChange(settings.copy(tuning = tunings[i + 1])) },
                    label = { Text(tunings[i + 1].label) },
                )
            }
        }
        if (i + 2 < tunings.size) {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * The Tuner settings section with spoken feedback, precision mode, A4 calibration,
 * and auto-advance controls.
 */
@Composable
private fun TunerSection(
    settings: TunerSettings,
    onSettingsChange: (TunerSettings) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_tuner))

    SettingsSwitch(
        label = stringResource(R.string.settings_spoken_feedback),
        checked = settings.spokenFeedback,
        onCheckedChange = { onSettingsChange(settings.copy(spokenFeedback = it)) },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.settings_spoken_feedback_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(12.dp))

    SettingsSwitch(
        label = stringResource(R.string.settings_precision_mode),
        checked = settings.precisionMode,
        onCheckedChange = { onSettingsChange(settings.copy(precisionMode = it)) },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.settings_precision_mode_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(12.dp))

    SettingsSwitch(
        label = stringResource(R.string.settings_auto_advance),
        checked = settings.autoAdvance,
        onCheckedChange = { onSettingsChange(settings.copy(autoAdvance = it)) },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.settings_auto_advance_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(12.dp))

    SettingsSwitch(
        label = stringResource(R.string.settings_auto_start),
        checked = settings.autoStart,
        onCheckedChange = { onSettingsChange(settings.copy(autoStart = it)) },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.settings_auto_start_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(12.dp))

    SettingsSlider(
        label = stringResource(R.string.settings_a4_reference),
        value = settings.a4Reference,
        valueRange = TunerSettings.MIN_A4_REFERENCE..TunerSettings.MAX_A4_REFERENCE,
        valueLabel = "${"%.1f".format(settings.a4Reference)} Hz",
        enabled = true,
        onValueChange = {
            onSettingsChange(settings.copy(a4Reference = (it * 10).toInt() / 10f))
        },
    )
}

/**
 * The Pitch Monitor settings section with noise gate sensitivity slider.
 */
@Composable
private fun PitchMonitorSection(
    settings: PitchMonitorSettings,
    onSettingsChange: (PitchMonitorSettings) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_pitch_monitor))

    SettingsSlider(
        label = stringResource(R.string.settings_noise_gate_sensitivity),
        value = settings.noiseGateSensitivity,
        valueRange = PitchMonitorSettings.MIN_SENSITIVITY..PitchMonitorSettings.MAX_SENSITIVITY,
        valueLabel = "${(settings.noiseGateSensitivity * 100).toInt()}%",
        enabled = true,
        onValueChange = {
            onSettingsChange(settings.copy(noiseGateSensitivity = (it * 100).toInt() / 100f))
        },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.settings_noise_gate_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * The Fretboard settings section with left-handed mode toggle.
 */
@Composable
private fun FretboardSection(
    settings: FretboardSettings,
    onSettingsChange: (FretboardSettings) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_fretboard))

    SettingsSwitch(
        label = stringResource(R.string.settings_left_handed),
        checked = settings.leftHanded,
        onCheckedChange = { onSettingsChange(settings.copy(leftHanded = it)) },
    )

    SettingsSwitch(
        label = stringResource(R.string.settings_show_note_names),
        checked = settings.showNoteNames,
        onCheckedChange = { onSettingsChange(settings.copy(showNoteNames = it)) },
    )

    SettingsSwitch(
        label = stringResource(R.string.settings_allow_muted),
        checked = settings.allowMutedStrings,
        onCheckedChange = { onSettingsChange(settings.copy(allowMutedStrings = it)) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    SettingsSlider(
        label = stringResource(R.string.settings_frets),
        value = settings.lastFret.toFloat(),
        valueRange = FretboardSettings.MIN_LAST_FRET.toFloat()..FretboardSettings.MAX_LAST_FRET.toFloat(),
        valueLabel = "${settings.lastFret}",
        enabled = true,
        onValueChange = { onSettingsChange(settings.copy(lastFret = it.toInt())) },
    )
}


/**
 * A labeled switch row used for boolean settings.
 */
@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/**
 * A labeled slider with a current-value badge, used for numeric settings.
 */
@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The About section showing app version and credits.
 */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: stringResource(R.string.settings_unknown_version)

    SectionHeader(stringResource(R.string.settings_about))

    // App name and version
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

    Spacer(modifier = Modifier.height(16.dp))

    // Credits
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

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.settings_tagline),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A single credit entry with a label and value.
 */
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

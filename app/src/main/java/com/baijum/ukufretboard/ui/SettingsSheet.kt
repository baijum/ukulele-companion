package com.baijum.ukufretboard.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.data.DisplaySettings
import com.baijum.ukufretboard.data.FretboardSettings
import com.baijum.ukufretboard.data.NotificationSettings
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning

/**
 * A modal bottom sheet displaying all app settings, organized by section.
 *
 * Contains Sound, Display, Tuning, Fretboard, and Notification sections.
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
    notificationSettings: NotificationSettings = NotificationSettings(),
    onNotificationSettingsChange: (NotificationSettings) -> Unit = {},
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
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 20.dp),
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

            // ── Tuning section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            TuningSection(
                settings = tuningSettings,
                onSettingsChange = onTuningSettingsChange,
            )

            // ── Fretboard section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            FretboardSection(
                settings = fretboardSettings,
                onSettingsChange = onFretboardSettingsChange,
            )

            // ── Notifications section ──
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            NotificationSection(
                settings = notificationSettings,
                onSettingsChange = onNotificationSettingsChange,
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
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
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
    SectionHeader("Sound")

    // Master toggle
    SettingsSwitch(
        label = "Sound",
        checked = settings.enabled,
        onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) },
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Volume slider
    SettingsSlider(
        label = "Volume",
        value = settings.volume,
        valueRange = SoundSettings.MIN_VOLUME..SoundSettings.MAX_VOLUME,
        valueLabel = "${(settings.volume * 100).toInt()}%",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(volume = it)) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Note duration slider
    SettingsSlider(
        label = "Note Duration",
        value = settings.noteDurationMs.toFloat(),
        valueRange = SoundSettings.MIN_NOTE_DURATION_MS.toFloat()..SoundSettings.MAX_NOTE_DURATION_MS.toFloat(),
        valueLabel = "${settings.noteDurationMs}ms",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(noteDurationMs = it.toInt())) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Strum delay slider
    SettingsSlider(
        label = "Strum Delay",
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
            text = "Strum Direction",
            style = MaterialTheme.typography.bodyLarge,
            color = if (settings.enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            FilterChip(
                selected = settings.strumDown,
                onClick = { onSettingsChange(settings.copy(strumDown = true)) },
                label = { Text("Down") },
                enabled = settings.enabled,
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = !settings.strumDown,
                onClick = { onSettingsChange(settings.copy(strumDown = false)) },
                label = { Text("Up") },
                enabled = settings.enabled,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Play on tap
    SettingsSwitch(
        label = "Play on Tap",
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
    SectionHeader("Display")

    // Theme
    Text(
        text = "Theme",
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
}

/**
 * The Tuning settings section with tuning variant selection.
 */
@Composable
private fun TuningSection(
    settings: TuningSettings,
    onSettingsChange: (TuningSettings) -> Unit,
) {
    SectionHeader("Tuning")

    Text(
        text = "Tuning",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Two rows of two chips to accommodate four tuning options
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UkuleleTuning.entries.take(2).forEach { tuning ->
            FilterChip(
                selected = settings.tuning == tuning,
                onClick = { onSettingsChange(settings.copy(tuning = tuning)) },
                label = { Text(tuning.label) },
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UkuleleTuning.entries.drop(2).forEach { tuning ->
            FilterChip(
                selected = settings.tuning == tuning,
                onClick = { onSettingsChange(settings.copy(tuning = tuning)) },
                label = { Text(tuning.label) },
            )
        }
    }
}

/**
 * The Fretboard settings section with left-handed mode toggle.
 */
@Composable
private fun FretboardSection(
    settings: FretboardSettings,
    onSettingsChange: (FretboardSettings) -> Unit,
) {
    SectionHeader("Fretboard")

    SettingsSwitch(
        label = "Left-Handed",
        checked = settings.leftHanded,
        onCheckedChange = { onSettingsChange(settings.copy(leftHanded = it)) },
    )

    SettingsSwitch(
        label = "Show Note Names",
        checked = settings.showNoteNames,
        onCheckedChange = { onSettingsChange(settings.copy(showNoteNames = it)) },
    )

    SettingsSwitch(
        label = "Allow Muted Strings",
        checked = settings.allowMutedStrings,
        onCheckedChange = { onSettingsChange(settings.copy(allowMutedStrings = it)) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    SettingsSlider(
        label = "Frets",
        value = settings.lastFret.toFloat(),
        valueRange = FretboardSettings.MIN_LAST_FRET.toFloat()..FretboardSettings.MAX_LAST_FRET.toFloat(),
        valueLabel = "${settings.lastFret}",
        enabled = true,
        onValueChange = { onSettingsChange(settings.copy(lastFret = it.toInt())) },
    )
}

/**
 * The Notification settings section.
 */
@Composable
private fun NotificationSection(
    settings: NotificationSettings,
    onSettingsChange: (NotificationSettings) -> Unit,
) {
    SectionHeader("Notifications")

    SettingsSwitch(
        label = "Chord of the Day",
        checked = settings.chordOfDayEnabled,
        onCheckedChange = { onSettingsChange(settings.copy(chordOfDayEnabled = it)) },
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
    val versionName = packageInfo.versionName ?: "Unknown"

    SectionHeader("About")

    // App name and version
    Text(
        text = "Ukulele Companion",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Version $versionName",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Website",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://baijum.github.io/ukulele-companion"))
            context.startActivity(intent)
        },
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Credits
    Text(
        text = "Credits",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    CreditItem(
        label = "Audio Samples",
        value = "\"Ukelele single notes, close-mic\" by stomachache (Freesound.org)",
    )
    CreditItem(
        label = "License",
        value = "CC BY 3.0",
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Your all-in-one ukulele companion for chords, scales, " +
            "music theory, and composition. Free, offline, no ads, no tracking.",
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

package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.FretboardSettings
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning

@Composable
internal fun TuningSection(
    settings: TuningSettings,
    onSettingsChange: (TuningSettings) -> Unit,
) {
    SettingsSectionHeader(stringResource(R.string.settings_tuning))

    Text(
        text = stringResource(R.string.settings_tuning),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))

    val tunings = UkuleleTuning.entries
    for (i in tunings.indices step 2) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = settings.tuning == tunings[i],
                onClick = { onSettingsChange(settings.copy(tuning = tunings[i])) },
                label = { Text(tunings[i].localizedLabel()) },
            )
            if (i + 1 < tunings.size) {
                FilterChip(
                    selected = settings.tuning == tunings[i + 1],
                    onClick = { onSettingsChange(settings.copy(tuning = tunings[i + 1])) },
                    label = { Text(tunings[i + 1].localizedLabel()) },
                )
            }
        }
        if (i + 2 < tunings.size) {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun TunerSection(
    settings: TunerSettings,
    onSettingsChange: (TunerSettings) -> Unit,
) {
    SettingsSectionHeader(stringResource(R.string.settings_tuner))

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

@Composable
internal fun FretboardSection(
    settings: FretboardSettings,
    onSettingsChange: (FretboardSettings) -> Unit,
) {
    SettingsSectionHeader(stringResource(R.string.settings_fretboard))

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

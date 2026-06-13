package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.SoundSettings

@Composable
internal fun SoundSection(
    settings: SoundSettings,
    onSettingsChange: (SoundSettings) -> Unit,
) {
    SettingsSectionHeader(stringResource(R.string.settings_sound))

    SettingsSwitch(
        label = stringResource(R.string.settings_sound),
        checked = settings.enabled,
        onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) },
    )

    Spacer(modifier = Modifier.height(12.dp))

    SettingsSlider(
        label = stringResource(R.string.settings_volume),
        value = settings.volume,
        valueRange = SoundSettings.MIN_VOLUME..SoundSettings.MAX_VOLUME,
        valueLabel = "${(settings.volume * 100).toInt()}%",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(volume = it)) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    SettingsSlider(
        label = stringResource(R.string.settings_note_duration),
        value = settings.noteDurationMs.toFloat(),
        valueRange = SoundSettings.MIN_NOTE_DURATION_MS.toFloat()..SoundSettings.MAX_NOTE_DURATION_MS.toFloat(),
        valueLabel = "${settings.noteDurationMs}ms",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(noteDurationMs = it.toInt())) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    SettingsSlider(
        label = stringResource(R.string.settings_strum_delay),
        value = settings.strumDelayMs.toFloat(),
        valueRange = SoundSettings.MIN_STRUM_DELAY_MS.toFloat()..SoundSettings.MAX_STRUM_DELAY_MS.toFloat(),
        valueLabel = "${settings.strumDelayMs}ms",
        enabled = settings.enabled,
        onValueChange = { onSettingsChange(settings.copy(strumDelayMs = it.toInt())) },
    )

    Spacer(modifier = Modifier.height(12.dp))

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

    SettingsSwitch(
        label = stringResource(R.string.settings_play_on_tap),
        checked = settings.playOnTap,
        onCheckedChange = { onSettingsChange(settings.copy(playOnTap = it)) },
        enabled = settings.enabled,
    )

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    SettingsSlider(
        label = stringResource(R.string.settings_noise_gate_filtering),
        value = settings.noiseGateFiltering,
        valueRange = SoundSettings.MIN_NOISE_GATE_FILTERING..SoundSettings.MAX_NOISE_GATE_FILTERING,
        valueLabel = "${(settings.noiseGateFiltering * 100).toInt()}%",
        enabled = true,
        onValueChange = {
            onSettingsChange(settings.copy(noiseGateFiltering = (it * 100).toInt() / 100f))
        },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.settings_noise_gate_filtering_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

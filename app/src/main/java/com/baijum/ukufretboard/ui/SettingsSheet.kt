package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.DisplaySettings
import com.baijum.ukufretboard.data.FretboardSettings
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.TuningSettings

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
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .semantics { heading() },
            )

            SoundSection(
                settings = soundSettings,
                onSettingsChange = onSoundSettingsChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            DisplaySection(
                settings = displaySettings,
                onSettingsChange = onDisplaySettingsChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            LanguageSection()

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            TuningSection(
                settings = tuningSettings,
                onSettingsChange = onTuningSettingsChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            TunerSection(
                settings = tunerSettings,
                onSettingsChange = onTunerSettingsChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            FretboardSection(
                settings = fretboardSettings,
                onSettingsChange = onFretboardSettingsChange,
            )

            if (backupRestoreViewModel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                BackupRestoreSection(viewModel = backupRestoreViewModel)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            AboutSection()
        }
    }
}

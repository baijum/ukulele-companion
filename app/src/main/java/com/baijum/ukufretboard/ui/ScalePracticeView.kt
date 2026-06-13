package com.baijum.ukufretboard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ScaleCategory
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import com.baijum.ukufretboard.data.PracticeMode
import com.baijum.ukufretboard.viewmodel.ScalePracticeViewModel
import com.baijum.ukufretboard.domain.UkuleleString

@Composable
fun ScalePracticeView(
    viewModel: ScalePracticeViewModel,
    progressViewModel: LearningProgressViewModel? = null,
    onSettingsChanged: (com.baijum.ukufretboard.data.ScalePracticeSettings) -> Unit = {},
    tuning: List<UkuleleString> = FretboardViewModel.STANDARD_TUNING,
    lastFret: Int = 12,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    @Suppress("UNUSED_VARIABLE")
    val progressState = progressViewModel?.state?.collectAsState()
    val allTimeQuizStats = progressViewModel?.scalePracticeStats("quiz")
    val allTimeEarStats = progressViewModel?.scalePracticeStats("ear")

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPlayback() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.scale_practice_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.scale_practice_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PracticeMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = {
                        viewModel.setMode(mode)
                        onSettingsChanged(viewModel.currentSettings())
                    },
                    label = {
                        Text(
                            mode.localizedLabel(),
                            fontWeight = if (state.mode == mode) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.label_category),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = state.selectedCategory == null,
                onClick = {
                    viewModel.setCategory(null)
                    onSettingsChanged(viewModel.currentSettings())
                },
                label = {
                    Text(
                        stringResource(R.string.label_all),
                        fontWeight = if (state.selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                ),
            )
            ScaleCategory.entries.forEach { cat ->
                FilterChip(
                    selected = state.selectedCategory == cat,
                    onClick = {
                        viewModel.setCategory(cat)
                        onSettingsChanged(viewModel.currentSettings())
                    },
                    label = {
                        Text(
                            cat.localizedLabel(),
                            fontWeight = if (state.selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (state.mode) {
            PracticeMode.PLAY_ALONG -> PlayAlongContent(
                viewModel = viewModel,
                state = state,
                onSettingsChanged = onSettingsChanged,
                tuning = tuning,
                lastFret = lastFret,
            )
            PracticeMode.QUIZ -> QuizContent(
                viewModel = viewModel,
                state = state,
                progressViewModel = progressViewModel,
                allTimeStats = allTimeQuizStats,
            )
            PracticeMode.EAR_TRAINING -> EarTrainingContent(
                viewModel = viewModel,
                state = state,
                progressViewModel = progressViewModel,
                allTimeStats = allTimeEarStats,
            )
        }
    }
}

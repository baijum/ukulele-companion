package com.baijum.ukufretboard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6

/**
 * Full-screen onboarding wizard shown on first launch.
 *
 * Uses a [HorizontalPager] with six pages: Welcome, Features,
 * Accessibility, Privacy, Navigation, and Quick Setup.
 * The last page applies user preferences via [settingsViewModel]
 * and calls [onFinished] to transition to the main app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    onFinished: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    if (!isLastPage) {
                        TextButton(onClick = {
                            settingsViewModel.completeOnboarding()
                            onFinished()
                        }) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                    }
                },
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                pagerState = pagerState,
                isLastPage = isLastPage,
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                onBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onGetStarted = {
                    settingsViewModel.completeOnboarding()
                    onFinished()
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> FeaturesPage()
                2 -> AccessibilityPage()
                3 -> PrivacyPage()
                4 -> NavigationGuidePage()
                5 -> SetupPage(settingsViewModel)
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    pagerState: PagerState,
    isLastPage: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGetStarted: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PageIndicator(
            pageCount = PAGE_COUNT,
            currentPage = pagerState.currentPage,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.onboarding_back))
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (isLastPage) {
                Button(onClick = onGetStarted) {
                    Text(stringResource(R.string.onboarding_get_started))
                }
            } else {
                Button(onClick = onNext) {
                    Text(stringResource(R.string.onboarding_next))
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    val description = stringResource(
        R.string.onboarding_page_indicator,
        currentPage + 1,
        pageCount,
    )
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = description
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val color by animateColorAsState(
                targetValue = if (index == currentPage)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant,
                label = "dot",
            )
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

// ── Page 1: Welcome ────────────────────────────────────────────────────

@Composable
private fun WelcomePage() {
    CenteredPage {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Page 2: Features ───────────────────────────────────────────────────

@Composable
private fun FeaturesPage() {
    ScrollablePage {
        Text(
            text = stringResource(R.string.onboarding_features_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureRow(Icons.Filled.Mic, stringResource(R.string.onboarding_feature_tuner))
        FeatureRow(Icons.Filled.LibraryMusic, stringResource(R.string.onboarding_feature_chords))
        FeatureRow(Icons.Filled.GraphicEq, stringResource(R.string.onboarding_feature_fretboard))
        FeatureRow(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.onboarding_feature_songbook))
        FeatureRow(Icons.Filled.School, stringResource(R.string.onboarding_feature_learn))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// ── Page 3: Accessibility ──────────────────────────────────────────────

@Composable
private fun AccessibilityPage() {
    ScrollablePage {
        Icon(
            imageVector = Icons.Filled.Accessibility,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_accessibility_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureRow(Icons.Filled.CheckCircle, stringResource(R.string.onboarding_accessibility_talkback))
        FeatureRow(Icons.Filled.VolumeUp, stringResource(R.string.onboarding_accessibility_spoken))
        FeatureRow(Icons.Filled.Contrast, stringResource(R.string.onboarding_accessibility_contrast))
    }
}

// ── Page 4: Privacy ────────────────────────────────────────────────────

@Composable
private fun PrivacyPage() {
    ScrollablePage {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_privacy_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureRow(Icons.Filled.WifiOff, stringResource(R.string.onboarding_privacy_offline))
        FeatureRow(Icons.Filled.Lock, stringResource(R.string.onboarding_privacy_no_ads))
        FeatureRow(Icons.Filled.SignalWifiOff, stringResource(R.string.onboarding_privacy_yours))
    }
}

// ── Page 5: Navigation Guide ───────────────────────────────────────────

@Composable
private fun NavigationGuidePage() {
    ScrollablePage {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_nav_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_nav_instruction),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureRow(Icons.Filled.PlayArrow, stringResource(R.string.onboarding_nav_play))
        FeatureRow(Icons.Filled.Create, stringResource(R.string.onboarding_nav_create))
        FeatureRow(Icons.Filled.School, stringResource(R.string.onboarding_nav_learn))
        FeatureRow(Icons.Filled.Search, stringResource(R.string.onboarding_nav_reference))
    }
}

// ── Page 6: Quick Setup ────────────────────────────────────────────────

@Composable
private fun SetupPage(settingsViewModel: SettingsViewModel) {
    val appSettings by settingsViewModel.settings.collectAsState()

    ScrollablePage {
        Text(
            text = stringResource(R.string.onboarding_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tuning
        Text(
            text = stringResource(R.string.onboarding_setup_tuning),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val tunings = UkuleleTuning.entries
        for (i in tunings.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (j in i until minOf(i + 2, tunings.size)) {
                    val tuning = tunings[j]
                    FilterChip(
                        selected = appSettings.tuning.tuning == tuning,
                        onClick = {
                            settingsViewModel.updateTuning { it.copy(tuning = tuning) }
                        },
                        label = { Text(tuning.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (i + 1 >= tunings.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Left-handed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.onboarding_setup_left_handed),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(
                checked = appSettings.fretboard.leftHanded,
                onCheckedChange = { checked ->
                    settingsViewModel.updateFretboard { it.copy(leftHanded = checked) }
                },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Theme
        Text(
            text = stringResource(R.string.onboarding_setup_theme),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appSettings.display.themeMode == mode,
                    onClick = {
                        settingsViewModel.updateDisplay { it.copy(themeMode = mode) }
                    },
                    label = { Text(mode.label) },
                )
            }
        }
    }
}

// ── Layout helpers ─────────────────────────────────────────────────────

@Composable
private fun CenteredPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = { content() },
    )
}

@Composable
private fun ScrollablePage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
        content = { content() },
    )
}

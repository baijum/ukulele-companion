package com.baijum.ukufretboard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.baijum.ukufretboard.ui.FretboardScreen
import com.baijum.ukufretboard.ui.theme.UkuleleCompanionTheme
import com.baijum.ukufretboard.viewmodel.SettingsViewModel

/**
 * Single-activity entry point for the Ukulele Companion app.
 *
 * Sets up edge-to-edge display and applies the app theme before
 * rendering the main [FretboardScreen].
 *
 * Extends [AppCompatActivity] to enable per-app language support
 * via [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]
 * on all API levels (min SDK 26+).
 *
 * [SettingsViewModel] is hoisted here so the theme mode is available
 * at the top level, before any screen composable is rendered.
 */
class MainActivity : AppCompatActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings by settingsViewModel.settings.collectAsState()
            UkuleleCompanionTheme(themeMode = appSettings.display.themeMode) {
                FretboardScreen(settingsViewModel = settingsViewModel)
            }
        }
    }
}

package com.baijum.ukufretboard.ui

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * `true` while TalkBack (or another touch-exploration service) is running.
 *
 * Screens that hide controls behind a tap gesture need this: a screen-reader user
 * cannot perform that gesture, so the controls must stay put instead of vanishing.
 * Tracks the setting live, since the user can turn TalkBack on with the app open.
 */
@Composable
fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager =
        remember(context) {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        }
    var enabled by remember(manager) {
        mutableStateOf(manager?.isTouchExplorationEnabled == true)
    }

    DisposableEffect(manager) {
        if (manager == null) return@DisposableEffect onDispose {}
        val listener =
            AccessibilityManager.TouchExplorationStateChangeListener { active ->
                enabled = active
            }
        manager.addTouchExplorationStateChangeListener(listener)
        enabled = manager.isTouchExplorationEnabled
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }

    return enabled
}

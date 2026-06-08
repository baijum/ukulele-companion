package com.baijum.ukufretboard.ui

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `true` when the user has set "Remove animations" / animator duration scale = 0
 * in system accessibility settings.
 */
val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}

/**
 * Returns [fallback] when animations are enabled, or [snap] when reduced.
 */
@Composable
fun <T> reduceMotionSpec(fallback: AnimationSpec<T>): AnimationSpec<T> {
    return if (LocalReduceMotion.current) snap() else fallback
}

/** No-op enter/exit when reduce motion is on. */
object ReduceMotionTransitions {
    @Composable
    fun enter(fallback: EnterTransition): EnterTransition =
        if (LocalReduceMotion.current) EnterTransition.None else fallback

    @Composable
    fun exit(fallback: ExitTransition): ExitTransition =
        if (LocalReduceMotion.current) ExitTransition.None else fallback
}

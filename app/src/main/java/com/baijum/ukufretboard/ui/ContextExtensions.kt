package com.baijum.ukufretboard.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Unwraps the [Activity] hosting this [Context].
 *
 * `LocalContext.current` is not always the Activity itself — it is frequently a
 * [ContextWrapper] such as a `ContextThemeWrapper` — so a direct `as? Activity`
 * cast silently returns null. Walk the wrapper chain instead.
 */
tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

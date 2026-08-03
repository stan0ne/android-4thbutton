package com.stan0ne.fourthbutton.util

import android.content.Context
import android.content.res.Configuration

/** Theme helpers for the overlay windows (which live outside any Activity). */
object ThemeUtil {

    fun isDark(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }
}
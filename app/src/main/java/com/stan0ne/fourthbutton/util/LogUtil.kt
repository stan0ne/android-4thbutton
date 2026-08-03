package com.stan0ne.fourthbutton.util

import android.util.Log

/**
 * Central logging used throughout the app. Everything is gated behind the
 * shared `AssistivePower/` prefix so a device logcat filter of `AssistivePower`
 * shows all categories.
 */
object LogUtil {

    private const val BASE = "AssistivePower"

    const val SERVICE = "SERVICE"
    const val OVERLAY = "OVERLAY"
    const val SCREENSHOT = "SCREENSHOT"
    const val ACTION = "ACTION"
    const val TORCH = "TORCH"
    const val SETTINGS = "SETTINGS"

    private fun tag(category: String) = "$BASE/$category"

    fun d(category: String, message: String) = Log.d(tag(category), message)
    fun i(category: String, message: String) = Log.i(tag(category), message)
    fun w(category: String, message: String, tr: Throwable? = null) = Log.w(tag(category), message, tr)
    fun e(category: String, message: String, tr: Throwable? = null) = Log.e(tag(category), message, tr)
}
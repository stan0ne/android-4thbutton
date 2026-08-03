package com.stan0ne.fourthbutton.screenshot

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates screenshot filenames like `Screenshot_2026-08-02_23-41-15.png`.
 * Dependency-free so it can be unit tested.
 */
object ScreenshotFileNameGenerator {

    private const val PATTERN = "yyyy-MM-dd_HH-mm-ss"

    fun generate(timestamp: Date = Date(), index: Int = 0): String {
        val name = "Screenshot_" + SimpleDateFormat(PATTERN, Locale.US).format(timestamp)
        return if (index > 0) "${name}_$index.png" else "$name.png"
    }
}
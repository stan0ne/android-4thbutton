package com.stan0ne.fourthbutton.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Typed access to persisted app settings.
 *
 * Holds floating button appearance, position, selected actions, action order
 * and screenshot delay. All values are stored in a plain [SharedPreferences]
 * file so they survive service restarts and app relaunches.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "assistive_power_prefs"

        private const val KEY_BUTTON_X = "button_x"
        private const val KEY_BUTTON_Y = "button_y"
        private const val KEY_BUTTON_SIZE = "button_size"
        private const val KEY_BUTTON_OPACITY = "button_opacity"
        private const val KEY_SCREENSHOT_DELAY = "screenshot_delay_ms"
        private const val KEY_ACTION_ENABLED_PREFIX = "action_enabled_"
        private const val KEY_ACTION_ORDER = "action_order"
        private const val KEY_HAS_SEEN_UI = "has_seen_ui"
        private const val ORDER_SEPARATOR = ","

        val DEFAULTS = Defaults()
    }

    class Defaults {
        val defaultButtonSize: Int = 56
        val defaultButtonOpacity: Float = 0.85f
        val defaultScreenshotDelayMs: Int = 3000
    }

    // --- Floating button ---

    fun getButtonX(): Int? = if (prefs.contains(KEY_BUTTON_X)) prefs.getInt(KEY_BUTTON_X, 0) else null
    fun getButtonY(): Int? = if (prefs.contains(KEY_BUTTON_Y)) prefs.getInt(KEY_BUTTON_Y, 0) else null

    fun setButtonPosition(x: Int, y: Int) {
        prefs.edit().putInt(KEY_BUTTON_X, x).putInt(KEY_BUTTON_Y, y).apply()
    }

    fun clearButtonPosition() {
        prefs.edit().remove(KEY_BUTTON_X).remove(KEY_BUTTON_Y).apply()
    }

    fun getButtonSize(): Int = prefs.getInt(KEY_BUTTON_SIZE, DEFAULTS.defaultButtonSize)

    fun setButtonSize(sizePx: Int) {
        prefs.edit().putInt(KEY_BUTTON_SIZE, sizePx).apply()
    }

    fun getButtonOpacity(): Float =
        prefs.getFloat(KEY_BUTTON_OPACITY, DEFAULTS.defaultButtonOpacity)

    fun setButtonOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_BUTTON_OPACITY, opacity).apply()
    }

    // --- Screenshot delay ---

    fun getScreenshotDelayMs(): Int =
        prefs.getInt(KEY_SCREENSHOT_DELAY, DEFAULTS.defaultScreenshotDelayMs)

    fun setScreenshotDelayMs(delayMs: Int) {
        prefs.edit().putInt(KEY_SCREENSHOT_DELAY, delayMs).apply()
    }

    // --- Action configuration ---

    fun isActionEnabled(actionId: String): Boolean =
        prefs.getBoolean(KEY_ACTION_ENABLED_PREFIX + actionId, true)

    fun setActionEnabled(actionId: String, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ACTION_ENABLED_PREFIX + actionId, enabled).apply()
    }

    /**
     * Returns all known action ids ordered according to the stored order,
     * falling back to [defaultOrder] when nothing has been persisted yet.
     */
    fun getActionOrder(defaultOrder: List<String>): List<String> {
        val stored = prefs.getString(KEY_ACTION_ORDER, null)
        if (stored.isNullOrBlank()) return defaultOrder
        val storedOrder = stored.split(ORDER_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        // Preserve stored order but append any newly added ids not seen before.
        return storedOrder + defaultOrder.filter { !storedOrder.contains(it) }
    }

    fun setActionOrder(order: List<String>) {
        prefs.edit().putString(KEY_ACTION_ORDER, order.joinToString(ORDER_SEPARATOR)).apply()
    }

    // --- Misc ---

    fun hasSeenUi(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_UI, false)
    fun markUiSeen() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_UI, true).apply()
    }
}
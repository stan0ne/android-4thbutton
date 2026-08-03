package com.stan0ne.fourthbutton.actions

import androidx.annotation.DrawableRes
import com.stan0ne.fourthbutton.R

/**
 * Central, extensible model for the actions shown in the floating menu.
 *
 * Each value holds everything the UI and handler need: a stable id, a
 * translatable title, a vector icon and a default ordering slot. Device
 * capability checks are performed separately by [ActionRepository].
 */
enum class AssistiveAction(
    val id: String,
    @DrawableRes val iconRes: Int,
    val defaultOrder: Int,
) {
    SCREEN_LOCK("screen_lock", R.drawable.ic_lock, 0),
    SCREENSHOT("screenshot", R.drawable.ic_screenshot, 1),
    POWER_MENU("power_menu", R.drawable.ic_power, 2),
    FLASHLIGHT("flashlight", R.drawable.ic_flashlight, 3),
    REBOOT("reboot", R.drawable.ic_restart, 4);

    companion object {
        /** All actions in default order. */
        val DEFAULT_ORDER: List<AssistiveAction> = entries.sortedBy { it.defaultOrder }

        fun fromId(id: String): AssistiveAction? = entries.firstOrNull { it.id == id }
    }
}
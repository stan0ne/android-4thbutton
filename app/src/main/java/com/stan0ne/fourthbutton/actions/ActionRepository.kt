package com.stan0ne.fourthbutton.actions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stan0ne.fourthbutton.R
import com.stan0ne.fourthbutton.settings.AppPreferences

/** A ready-to-render entry for the floating menu. */
data class ActionItem(
    val id: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val available: Boolean,
)

/**
 * Resolves which actions appear in the floating menu, honouring the user's
 * enable/order preferences and per-device capability (e.g. flashlight only if
 * a camera flash exists, reboot only with privileges).
 */
class ActionRepository(
    private val preferences: AppPreferences,
    private val context: Context,
) {

    fun visibleActions(): List<ActionItem> {
        val orderIds = preferences.getActionOrder(
            AssistiveAction.DEFAULT_ORDER.map { it.id }
        )
        // Resolve stored ids, then append any not yet stored (kept in default order).
        val ordered = orderIds.mapNotNull { AssistiveAction.fromId(it) } +
            AssistiveAction.DEFAULT_ORDER.filter { !orderIds.contains(it.id) }

        return ordered.distinct().map { action ->
            ActionItem(
                id = action.id,
                titleRes = titleRes(action),
                iconRes = action.iconRes,
                available = preferences.isActionEnabled(action.id) && action != AssistiveAction.REBOOT,
            )
        }.filter { it.available }
    }

    @StringRes
    fun titleRes(action: AssistiveAction): Int = when (action) {
        AssistiveAction.SCREEN_LOCK -> R.string.screen_lock
        AssistiveAction.SCREENSHOT -> R.string.screenshot
        AssistiveAction.POWER_MENU -> R.string.power_menu
        AssistiveAction.FLASHLIGHT -> R.string.flashlight
        AssistiveAction.REBOOT -> R.string.reboot
    }

    /** Whether the current device can perform this action at all. */
    fun isActionAvailable(action: AssistiveAction): Boolean = when (action) {
        AssistiveAction.SCREEN_LOCK,
        AssistiveAction.SCREENSHOT,
        AssistiveAction.POWER_MENU,
        -> true
        AssistiveAction.FLASHLIGHT -> hasFlash()
        AssistiveAction.REBOOT -> false
    }

    /** Flashlight is only offered when a camera with a flash unit is present. */
    private fun hasFlash(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return try {
            manager.cameraIdList.any { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Throwable) {
            false
        }
    }
}
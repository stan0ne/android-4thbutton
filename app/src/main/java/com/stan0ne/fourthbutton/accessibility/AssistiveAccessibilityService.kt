package com.stan0ne.fourthbutton.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.stan0ne.fourthbutton.R
import com.stan0ne.fourthbutton.actions.ActionRepository
import com.stan0ne.fourthbutton.actions.FlashlightController
import com.stan0ne.fourthbutton.overlay.CountdownOverlay
import com.stan0ne.fourthbutton.overlay.FloatingButtonManager
import com.stan0ne.fourthbutton.overlay.FloatingMenuManager
import com.stan0ne.fourthbutton.overlay.OverlayWindowManager
import com.stan0ne.fourthbutton.overlay.ScreenshotResultOverlay
import com.stan0ne.fourthbutton.screenshot.ScreenshotDelayScheduler
import com.stan0ne.fourthbutton.screenshot.ScreenshotStorage
import com.stan0ne.fourthbutton.settings.AppPreferences
import com.stan0ne.fourthbutton.util.LogUtil

/**
 * The core of the app: it hosts the floating control over all applications and
 * performs the user-selected system actions (screen lock, screenshot, power
 * menu, flashlight) through Android's official public APIs.
 *
 * It deliberately ignores window content: [onAccessibilityEvent] is a no-op and
 * the service never reads or stores any screen content.
 */
class AssistiveAccessibilityService : AccessibilityService() {

    private lateinit var preferences: AppPreferences
    private lateinit var windowManager: OverlayWindowManager
    private lateinit var actionRepository: ActionRepository
    private lateinit var buttonManager: FloatingButtonManager
    private lateinit var menuManager: FloatingMenuManager
    private lateinit var flashlight: FlashlightController
    private var countdownOverlay: CountdownOverlay? = null
    private var resultOverlay: ScreenshotResultOverlay? = null
    private val scheduler = ScreenshotDelayScheduler()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var capturePending = false

    private lateinit var keyguardManager: KeyguardManager
    private var keyguardReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        LogUtil.i(LogUtil.SERVICE, "AccessibilityService connected")

        preferences = AppPreferences(this)
        windowManager = OverlayWindowManager(this)
        actionRepository = ActionRepository(preferences, applicationContext)
        flashlight = FlashlightController(applicationContext)
        countdownOverlay = CountdownOverlay(this, windowManager)
        resultOverlay = ScreenshotResultOverlay(this, windowManager)

        buttonManager = FloatingButtonManager(this, windowManager, preferences, buttonListener)
        menuManager = FloatingMenuManager(this, windowManager)

        keyguardManager = getSystemService(KeyguardManager::class.java)
        registerKeyguardReceiver()

        applyButtonAppearance()

        if (!buttonManager.isShowing()) {
            buttonManager.show()
        }
        if (keyguardManager.isKeyguardLocked()) {
            hideOverlaysForLock()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Intentionally empty: we do not read window content.
    }

    override fun onInterrupt() {
        // Nothing to interrupt: no continuous monitoring is performed.
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        buttonManager.onDisplaySizeChanged()
        LogUtil.d(LogUtil.OVERLAY, "display/orientation changed")
    }

    override fun onDestroy() {
        instance = null
        LogUtil.i(LogUtil.SERVICE, "AccessibilityService destroyed")
        unregisterKeyguardReceiver()
        scheduler.cancel()
        countdownOverlay?.hide()
        resultOverlay?.destroy()
        buttonManager.hide()
        menuManager.destroy()
        windowManager.removeAll()
        super.onDestroy()
    }

    private val buttonListener = object : FloatingButtonManager.Listener {
        override fun onButtonTap() {
            if (menuManager.isVisible()) {
                menuManager.dismiss()
                return
            }
            resultOverlay?.dismiss()
            val centre = buttonManager.currentCentre() ?: return
            val items = actionRepository.visibleActions()
            if (items.isEmpty()) return
            menuManager.show(items, centre.first.toFloat(), centre.second.toFloat(), menuListener)
        }

        override fun onButtonDragEnd(centerX: Int, centerY: Int) {
            preferences.setButtonPosition(centerX, centerY)
        }
    }

    private val menuListener = object : FloatingMenuManager.Listener {
        override fun onActionClick(actionId: String) {
            performAction(actionId)
        }

        override fun onMenuDismissed() {
            // Floating button stays visible; nothing else to do.
        }
    }

    private fun performAction(actionId: String) {
        LogUtil.d(LogUtil.ACTION, "action requested: $actionId")
        menuManager.destroy()
        when (actionId) {
            "screen_lock" -> lockScreen()
            "power_menu" -> openPowerMenu()
            "screenshot" -> startScreenshotFlow()
            "flashlight" -> toggleFlashlight()
            "reboot" -> toast(R.string.reboot_not_available)
            else -> toast(R.string.action_unavailable)
        }
    }

    private fun lockScreen() {
        val ok = runCatching {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }.getOrDefault(false)
        LogUtil.d(LogUtil.ACTION, "screen lock requested -> $ok")
        if (!ok) toast(R.string.unable_to_lock)
    }

    private fun openPowerMenu() {
        val ok = runCatching {
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        }.getOrDefault(false)
        LogUtil.d(LogUtil.ACTION, "power menu requested -> $ok")
        if (!ok) toast(R.string.power_menu_failed)
    }

    private fun toggleFlashlight() {
        if (!flashlight.isAvailable()) {
            toast(R.string.flashlight_unavailable)
            return
        }
        if (!flashlight.toggle()) toast(R.string.flashlight_error)
    }

    // --- Screenshot flow ---

    private fun startScreenshotFlow() {
        buttonManager.hide()     // keep the floating UI out of the capture
        menuManager.destroy()
        resultOverlay?.dismiss()
        val delayMs = preferences.getScreenshotDelayMs()
        val seconds = (delayMs / 1000).coerceAtLeast(0)
        if (seconds <= 0) {
            mainHandler.postDelayed({ captureScreenshot() }, 120)
            return
        }
        countdownOverlay?.show(seconds, ::cancelScreenshot)
        scheduler.start(
            totalSeconds = seconds,
            onTick = { remaining -> countdownOverlay?.update(remaining) },
            onComplete = ::onCountdownComplete,
        )
    }

    private fun onCountdownComplete() {
        LogUtil.d(LogUtil.SCREENSHOT, "countdown finished")
        countdownOverlay?.hide()
        mainHandler.postDelayed({ captureScreenshot() }, 120)
    }

    private fun cancelScreenshot() {
        if (scheduler.cancel()) {
            LogUtil.i(LogUtil.SCREENSHOT, "countdown cancelled")
            toast(R.string.countdown_cancelled)
            countdownOverlay?.hide()
        }
        showButton()
    }

    private fun captureScreenshot() {
        if (capturePending) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            LogUtil.e(LogUtil.SCREENSHOT, "takeScreenshot requires API 30+")
            toast(R.string.screenshot_failed)
            showButton()
            return
        }
        capturePending = true
        LogUtil.d(LogUtil.SCREENSHOT, "capturing screenshot")
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, newScreenshotCallback())
        } catch (t: Throwable) {
            LogUtil.e(LogUtil.SCREENSHOT, "takeScreenshot threw: ${t.message}")
            capturePending = false
            toast(R.string.screenshot_failed)
            showButton()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun newScreenshotCallback(): TakeScreenshotCallback {
        return object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                capturePending = false
                val result = screenshot.hardwareBuffer?.let { buffer ->
                    ScreenshotStorage.save(applicationContext, buffer).also {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) buffer.close()
                    }
                } ?: Result.failure(IllegalStateException("No hardware buffer"))
                LogUtil.d(LogUtil.SCREENSHOT, "screenshot result: ${result.getOrNull()}")
                if (result.isSuccess) {
                    toast(R.string.screenshot_saved)
                    showScreenshotResultBar(result.getOrNull())
                } else {
                    toast(R.string.screenshot_failed_save)
                }
                showButton()
            }

            override fun onFailure(errorCode: Int) {
                capturePending = false
                LogUtil.e(LogUtil.SCREENSHOT, "takeScreenshot failed with code $errorCode")
                toast(R.string.screenshot_failed)
                showButton()
            }
        }
    }

    private fun showScreenshotResultBar(uri: Uri?) {
        if (uri == null) return
        resultOverlay?.show(object : ScreenshotResultOverlay.Listener {
            override fun onShare() = shareScreenshot(uri)
            override fun onEdit() = editScreenshot(uri)
            override fun onDelete() = deleteScreenshot(uri)
        })
    }

    private fun shareScreenshot(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchChooser(intent, R.string.screenshot_share)
    }

    private fun editScreenshot(uri: Uri) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {
            openScreenshot(uri)
        }
    }

    private fun openScreenshot(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {
            toast(R.string.no_app_available)
        }
    }

    private fun deleteScreenshot(uri: Uri) {
        val deleted = runCatching {
            contentResolver.delete(uri, null, null)
        }.getOrDefault(0)
        resultOverlay?.dismiss()
        LogUtil.d(LogUtil.SCREENSHOT, "screenshot deleted: $deleted ($uri)")
        toast(if (deleted > 0) R.string.screenshot_deleted else R.string.action_unavailable)
    }

    private fun launchChooser(intent: Intent, titleRes: Int) {
        try {
            val chooser = Intent.createChooser(intent, getString(titleRes))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(chooser)
        } catch (_: Throwable) {
            toast(R.string.no_app_available)
        }
    }

    private fun applyButtonAppearance() {
        if (::buttonManager.isInitialized) {
            buttonManager.update(preferences.getButtonSize(), preferences.getButtonOpacity())
        }
    }

    private fun showButton() {
        if (!buttonManager.isShowing()) buttonManager.show()
    }

    private fun hideOverlaysForLock() {
        menuManager.destroy()
        resultOverlay?.dismiss()
        countdownOverlay?.hide()
        if (buttonManager.isShowing()) buttonManager.hide()
    }

    /** Keeps the floating UI off the keyguard: hidden while the device is locked. */
    private fun registerKeyguardReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        keyguardReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> hideOverlaysForLock()
                    Intent.ACTION_SCREEN_ON ->
                        if (keyguardManager.isKeyguardLocked()) hideOverlaysForLock() else showButton()
                    Intent.ACTION_USER_PRESENT -> showButton()
                }
            }
        }
        registerReceiver(keyguardReceiver, filter)
    }

    private fun unregisterKeyguardReceiver() {
        keyguardReceiver?.let { receiver ->
            runCatching { unregisterReceiver(receiver) }
        }
        keyguardReceiver = null
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private var instance: AssistiveAccessibilityService? = null

        /** Re-applies floating button size/opacity after settings change. */
        fun applySettings() {
            instance?.applyButtonAppearance()
        }
    }
}
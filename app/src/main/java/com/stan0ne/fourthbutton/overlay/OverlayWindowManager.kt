package com.stan0ne.fourthbutton.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.stan0ne.fourthbutton.util.LogUtil
import java.util.Collections
import java.util.WeakHashMap

/**
 * Thin, lifecycle-safe wrapper around [WindowManager] for adding, updating and
 * removing overlay views. All overlay managers delegate to this so that
 * `BadTokenException` / `IllegalStateException` / double-add are handled in one
 * place and nothing leaks on teardown.
 */
@SuppressLint("WrongConstant")
class OverlayWindowManager(private val context: Context) {

    private var wm: WindowManager? = null
    private val added = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    private fun windowManager(): WindowManager =
        wm ?: context.getSystemService(WindowManager::class.java).also { wm = it }

    fun makeParams(
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        touchable: Boolean,
    ): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        return WindowManager.LayoutParams(
            width,
            height,
            type,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    fun addView(view: View, params: WindowManager.LayoutParams): Boolean {
        return try {
            if (view.isAttachedToWindow) return true
            windowManager().addView(view, params)
            added.add(view)
            true
        } catch (t: Throwable) {
            LogUtil.w(LogUtil.OVERLAY, "addView failed: ${t.message}")
            false
        }
    }

    fun updateView(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager().updateViewLayout(view, params)
        } catch (t: Throwable) {
            LogUtil.w(LogUtil.OVERLAY, "updateViewLayout failed: ${t.message}")
        }
    }

    fun removeView(view: View) {
        runCatching {
            if (view.isAttachedToWindow) windowManager().removeView(view)
        }.onFailure {
            LogUtil.w(LogUtil.OVERLAY, "removeView failed: ${it.message}")
        }
        added.remove(view)
    }

    fun removeAll() {
        added.toList().forEach { removeView(it) }
        added.clear()
    }
}
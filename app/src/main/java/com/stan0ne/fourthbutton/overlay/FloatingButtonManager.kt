package com.stan0ne.fourthbutton.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import com.stan0ne.fourthbutton.R
import com.stan0ne.fourthbutton.settings.AppPreferences
import com.stan0ne.fourthbutton.util.LogUtil
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Owns the small circular floating button that sits over other apps.
 *
 * It decides between tap and drag using the platform touch slop, moves the
 * button while dragging, and reports the released centre so the caller can
 * persist and restore it next time the service connects.
 *
 * All stored/preference coordinates are button centres; [WindowManager]
 * top-left view coordinates are derived from them.
 */
@SuppressLint("InlinedApi")
class FloatingButtonManager(
    private val context: Context,
    private val windowManager: OverlayWindowManager,
    private val preferences: AppPreferences,
    private val listener: Listener,
) {

    interface Listener {
        fun onButtonTap()
        fun onButtonDragEnd(centerX: Int, centerY: Int)
    }

    private var buttonView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var sizeDp: Int = preferences.getButtonSize()
    private var opacity: Float = preferences.getButtonOpacity()

    private val sizePx: Int get() = (sizeDp * context.resources.displayMetrics.density).roundToInt()

    private val edgeMargin = 24

    private var downRawX = 0f
    private var downRawY = 0f
    private var downViewX = 0
    private var downViewY = 0
    private var dragging = false
    private var touchSlopPx = 0

    private val screenWidth get() = metrics.widthPixels
    private val screenHeight get() = metrics.heightPixels
    private val metrics: DisplayMetrics get() = context.resources.displayMetrics

    private val onTouch = View.OnTouchListener { view, event -> handleTouch(view, event) }

    fun show() {
        if (buttonView != null) return
        val view = ImageView(context).apply {
            setImageResource(R.drawable.ic_assistive)
            setBackgroundResource(R.drawable.bg_button)
            alpha = opacity
            contentDescription = context.getString(R.string.accessibility_service_label)
            setOnTouchListener(onTouch)
        }
        touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
        val (cx, cy) = restoreCentre()
        val (l, t) = centreToTopLeft(cx, cy)
        val lp = windowManager.makeParams(sizePx, sizePx, l, t, touchable = true)
        if (windowManager.addView(view, lp)) {
            buttonView = view
            params = lp
            LogUtil.d(LogUtil.OVERLAY, "floating button shown")
        }
    }

    fun update(sizeDp: Int, opacityValue: Float) {
        this.sizeDp = sizeDp
        this.opacity = opacityValue
        val view = buttonView ?: return
        view.alpha = opacityValue
        params = params?.apply {
            width = sizePx
            height = sizePx
        }
        windowManager.updateView(view, params ?: return)
    }

    fun hide() {
        buttonView?.let { windowManager.removeView(it) }
        buttonView = null
        params = null
    }

    fun isShowing(): Boolean = buttonView != null

    fun currentCentre(): Pair<Int, Int>? {
        val view = buttonView ?: return null
        val lp = params ?: return null
        return (lp.x + view.width / 2) to (lp.y + view.height / 2)
    }

    /** Re-fits the button into the current screen after an orientation change. */
    fun onDisplaySizeChanged() {
        val lp = params ?: return
        val cx = lp.x + sizePx / 2f
        val cy = lp.y + sizePx / 2f
        val (ncx, ncy) = clampCentre(cx, cy)
        val (l, t) = centreToTopLeft(ncx, ncy)
        params = lp.apply { x = l; y = t }
        windowManager.updateView(buttonView ?: return, lp)
        listener.onButtonDragEnd(ncx, ncy)
    }

    private fun restoreCentre(): Pair<Int, Int> {
        val sx = preferences.getButtonX()
        val sy = preferences.getButtonY()
        return if (sx != null && sy != null) {
            clampCentre(sx.toFloat(), sy.toFloat())
        } else {
            val cx = screenWidth - sizePx / 2 - edgeMargin
            val cy = (screenHeight * 0.85f).toInt()
            clampCentre(cx.toFloat(), cy.toFloat())
        }
    }

    private fun clampCentre(x: Float, y: Float): Pair<Int, Int> {
        val half = sizePx / 2f
        val min = half + edgeMargin
        val cx = x.coerceIn(min, (screenWidth - half - edgeMargin).coerceAtLeast(min))
        val cy = y.coerceIn(min, (screenHeight - half - edgeMargin).coerceAtLeast(min))
        return cx.toInt() to cy.toInt()
    }

    private fun centreToTopLeft(centreX: Int, centreY: Int): Pair<Int, Int> =
        (centreX - sizePx / 2) to (centreY - sizePx / 2)

    private fun topLeftToCentre(topLeftX: Float, topLeftY: Float): Pair<Int, Int> =
        (topLeftX.toInt() + sizePx / 2) to (topLeftY.toInt() + sizePx / 2)

    private fun clampTopLeft(x: Float, y: Float): Pair<Int, Int> {
        val maxX = screenWidth - sizePx
        val maxY = screenHeight - sizePx
        return x.coerceIn(0f, maxX.coerceAtLeast(0).toFloat()).toInt() to
            y.coerceIn(0f, maxY.coerceAtLeast(0).toFloat()).toInt()
    }

    private fun handleTouch(v: View, event: MotionEvent): Boolean {
        val lp = params ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                downViewX = lp.x
                downViewY = lp.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!dragging && (abs(dx) > touchSlopPx || abs(dy) > touchSlopPx)) {
                    dragging = true
                }
                if (dragging) {
                    val (nx, ny) = clampTopLeft(downViewX + dx, downViewY + dy)
                    params = lp.apply { x = nx; y = ny }
                    windowManager.updateView(v, lp)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    val half = if (v.height > 0) v.height / 2 else sizePx / 2
                    listener.onButtonDragEnd(lp.x + half, lp.y + half)
                } else {
                    listener.onButtonTap()
                }
                dragging = false
            }
        }
        return true
    }
}
package com.stan0ne.fourthbutton.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.stan0ne.fourthbutton.R
import com.stan0ne.fourthbutton.util.LogUtil
import com.stan0ne.fourthbutton.util.ThemeUtil

/**
 * Small bottom bar shown after a screenshot is saved, offering the same
 * post-capture actions as the system screenshot UI: Share, Edit, Delete and
 * Close. Like the floating menu it is a full-screen touchable window, so a tap
 * anywhere outside the bar dismisses it.
 */
@SuppressLint("ClickableViewAccessibility")
class ScreenshotResultOverlay(
    private val context: Context,
    private val overlayWindow: OverlayWindowManager,
) {

    interface Listener {
        fun onShare()
        fun onEdit()
        fun onDelete()
    }

    private var root: FrameLayout? = null
    private var visible = false
    private var dismissing = false
    private var listener: Listener? = null

    fun show(listener: Listener) {
        if (visible) return
        this.listener = listener
        val isDark = ThemeUtil.isDark(context)

        val rootView = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event -> onRootTouch(event) }
        }
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = barBackground(isDark)
            elevation = 20f
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        bar.addView(barButton(R.string.screenshot_share, isDark, "share"))
        bar.addView(barButton(R.string.screenshot_edit, isDark, "edit"))
        bar.addView(barButton(R.string.screenshot_delete, isDark, "delete"))
        bar.addView(closeButton(isDark))

        rootView.addView(
            bar,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply {
                bottomMargin = dp(40)
            },
        )

        val lp = overlayWindow.makeParams(MATCH, MATCH, 0, 0, touchable = true)
        if (!overlayWindow.addView(rootView, lp)) return

        root = rootView
        visible = true
        dismissing = false

        animateIn(bar)
        LogUtil.d(LogUtil.OVERLAY, "screenshot result bar shown")
    }

    fun dismiss() {
        dismissInternal()
    }

    fun isVisible(): Boolean = visible

    fun destroy() {
        listener = null
        if (!dismissing) {
            root?.let { overlayWindow.removeView(it) }
            teardownViews()
        }
    }

    private fun onRootTouch(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val bar = card() ?: return false
            val loc = IntArray(2)
            bar.getLocationInWindow(loc)
            val inside = event.x >= loc[0] && event.x <= loc[0] + bar.width &&
                event.y >= loc[1] && event.y <= loc[1] + bar.height
            if (!inside) {
                dismissInternal()
                return true
            }
        }
        return false
    }

    private fun card(): View? {
        val r = root ?: return null
        return (r.getChildAt(0))
    }

    private fun barButton(resId: Int, dark: Boolean, action: String): View =
        textButton(resId, dark, textColor(dark)) { fire(action) }

    private fun closeButton(dark: Boolean): View =
        textButton(R.string.cancel, dark, Color.parseColor("#9E9E9E")) { dismissInternal() }

    private fun textButton(resId: Int, dark: Boolean, color: Int, onClick: () -> Unit): View =
        TextView(context).apply {
            text = context.getString(resId)
            setTextColor(color)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))
            isClickable = true
            isFocusable = true
            foreground = ripple(dark)
            setOnClickListener { onClick() }
        }

    private fun fire(action: String) {
        when (action) {
            "share" -> listener?.onShare()
            "edit" -> listener?.onEdit()
            "delete" -> listener?.onDelete()
        }
    }

    private fun animateIn(view: View) {
        view.alpha = 0f
        view.translationY = dp(24).toFloat()
        view.animate().alpha(1f).translationY(0f)
            .setDuration(180).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun dismissInternal() {
        val r = root ?: return
        if (dismissing) return
        dismissing = true
        LogUtil.d(LogUtil.OVERLAY, "screenshot result bar dismissed")
        r.animate().alpha(0f)
            .setDuration(150)
            .withEndAction { teardownViews() }
            .start()
    }

    private fun teardownViews() {
        root?.let { overlayWindow.removeView(it) }
        root = null
        visible = false
        dismissing = false
        listener = null
    }

    private fun barBackground(dark: Boolean): android.graphics.drawable.Drawable = GradientDrawable().apply {
        cornerRadius = dpf(28)
        setColor(if (dark) Color.parseColor("#F02A2A2C") else Color.WHITE)
        setStroke(dp(1).toInt(), if (dark) 0x33FFFFFF else 0x1F000000)
    }

    private fun ripple(dark: Boolean): android.graphics.drawable.Drawable = RippleDrawable(
        ColorStateList.valueOf(if (dark) 0x33FFFFFF else 0x22000000),
        null,
        null,
    )

    private fun textColor(dark: Boolean) = if (dark) Color.WHITE else Color.parseColor("#1A1A1A")

    private fun dp(value: Number): Int = (value.toFloat() * context.resources.displayMetrics.density).toInt()
    private fun dpf(value: Number): Float = value.toFloat() * context.resources.displayMetrics.density

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

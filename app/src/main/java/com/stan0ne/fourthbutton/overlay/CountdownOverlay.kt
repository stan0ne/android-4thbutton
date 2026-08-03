package com.stan0ne.fourthbutton.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.stan0ne.fourthbutton.util.LogUtil
import com.stan0ne.fourthbutton.util.ThemeUtil

/**
 * Small, visually-light countdown overlay shown while a delayed screenshot is
 * pending. It includes a cancel button and is removed before the capture
 * happens so it never ends up inside the saved image.
 */
class CountdownOverlay(
    private val context: Context,
    private val overlayWindow: OverlayWindowManager,
) {

    private var root: FrameLayout? = null
    private var clock: TextView? = null
    private var shown = false

    fun show(seconds: Int, onCancel: () -> Unit) {
        if (shown) return
        val isDark = ThemeUtil.isDark(context)
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpf(16)
                setColor(if (isDark) Color.parseColor("#E6262629") else Color.WHITE)
                setStroke(dp(1).toInt(), if (isDark) 0x33FFFFFF else 0x1F000000)
            }
            elevation = 10f
            gravity = Gravity.CENTER
            setPadding(dp(20).toInt(), dp(14).toInt(), dp(20).toInt(), dp(14).toInt())
        }
        val countdown = TextView(context).apply {
            text = seconds.toString()
            setTextSize(38f)
            gravity = Gravity.CENTER
            setTextColor(if (isDark) Color.WHITE else Color.parseColor("#1A1A1A"))
        }
        val cancel = TextView(context).apply {
            text = context.getString(com.stan0ne.fourthbutton.R.string.cancel)
            setTextColor(if (isDark) Color.parseColor("#8AB4F8") else Color.parseColor("#1A73E8"))
            textSize = 14f
            isClickable = true
            minimumHeight = dp(40).toInt()
            setOnClickListener { onCancel() }
        }
        panel.addView(countdown)
        panel.addView(cancel)

        val frame = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        val lp = overlayWindow.makeParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            0,
            0,
            touchable = true,
        )
        if (!overlayWindow.addView(frame, lp)) return

        // Place in the top-middle region.
        frame.post {
            val metrics = context.resources.displayMetrics
            panel.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.AT_MOST),
            )
            val w = panel.measuredWidth
            val h = panel.measuredHeight
            frame.addView(
                panel,
                FrameLayout.LayoutParams(w.coerceAtLeast(dp(120).toInt()), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
                    leftMargin = (metrics.widthPixels - w.coerceAtLeast(dp(120).toInt())) / 2
                    topMargin = (metrics.heightPixels / 3) - h / 2
                },
            )
        }

        root = frame
        clock = countdown
        shown = true
    }

    fun update(seconds: Int) {
        clock?.text = seconds.toString()
    }

    fun hide() {
        root?.let { overlayWindow.removeView(it) }
        root = null
        clock = null
        shown = false
    }

    fun isShown(): Boolean = shown

    private fun dp(value: Number): Int = (value.toFloat() * context.resources.displayMetrics.density).toInt()
    private fun dpf(value: Number): Float = value.toFloat() * context.resources.displayMetrics.density
}
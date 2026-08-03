package com.stan0ne.fourthbutton.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.stan0ne.fourthbutton.actions.ActionItem
import com.stan0ne.fourthbutton.util.LogUtil
import com.stan0ne.fourthbutton.util.ThemeUtil

/**
 * Renders the compact floating menu over other apps.
 *
 * The menu is a single transparent full-screen window: the card is placed next
 * to the floating button and taps on the surrounding scrim dismiss it. Rows
 * are built from [ActionItem]s so it reflects the user's selection and order.
 */
class FloatingMenuManager(
    private val context: Context,
    private val overlayWindow: OverlayWindowManager,
) {

    interface Listener {
        fun onActionClick(actionId: String)
        fun onMenuDismissed()
    }

    private var root: FrameLayout? = null
    private var card: LinearLayout? = null
    private var visible = false
    private var dismissing = false
    private var listener: Listener? = null

    fun show(items: List<ActionItem>, buttonCenterX: Float, buttonCenterY: Float, listener: Listener) {
        if (visible) return
        val isDark = ThemeUtil.isDark(context)
        this.listener = listener

        val rootView = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event -> onRootTouch(event) }
        }
        val cardView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(isDark)
            elevation = 20f
            setPadding(dp(8).toInt(), dp(6).toInt(), dp(8).toInt(), dp(6).toInt())
        }
        items.forEach { cardView.addView(buildRow(it, isDark)) }
        rootView.addView(
            cardView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START,
            ),
        )

        val lp = overlayWindow.makeParams(MATCH, MATCH, 0, 0, touchable = true)
        if (!overlayWindow.addView(rootView, lp)) return

        root = rootView
        card = cardView
        visible = true
        dismissing = false

        cardView.post {
            val w = if (cardView.width > 0) cardView.width else cardView.measuredWidth
            val h = if (cardView.height > 0) cardView.height else cardView.measuredHeight
            placeCard(cardView, w, h, buttonCenterX, buttonCenterY)
        }
        animateIn(rootView)
        LogUtil.d(LogUtil.OVERLAY, "menu opened (${items.size} items)")
    }

    fun dismiss() {
        dismissInternal()
    }

    fun isVisible(): Boolean = visible

    fun destroy() {
        listener = null
        if (!dismissing) {
            root?.let { overlayWindow.removeView(it) }
            teardownViews(notify = false)
        }
    }

    private fun onRootTouch(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val cardView = card ?: return false
            val loc = IntArray(2)
            cardView.getLocationInWindow(loc)
            val inside = event.x >= loc[0] && event.x <= loc[0] + cardView.width &&
                event.y >= loc[1] && event.y <= loc[1] + cardView.height
            if (!inside) {
                dismissInternal()
                return true
            }
        }
        return false
    }

    private fun placeCard(
        cardView: LinearLayout,
        width: Int,
        height: Int,
        buttonCenterX: Float,
        buttonCenterY: Float,
    ) {
        val metrics = context.resources.displayMetrics
        val (px, py) = OverlayPositionCalculator.placeMenu(
            buttonCenterX,
            buttonCenterY,
            width,
            height,
            metrics.widthPixels,
            metrics.heightPixels,
            dp(12).toInt(),
        )
        cardView.layoutParams = FrameLayout.LayoutParams(
            if (width <= 0) FrameLayout.LayoutParams.WRAP_CONTENT else width,
            if (height <= 0) FrameLayout.LayoutParams.WRAP_CONTENT else height,
            Gravity.TOP or Gravity.START,
        ).apply {
            leftMargin = px.toInt()
            topMargin = py.toInt()
        }
    }

    private fun animateIn(view: View) {
        view.alpha = 0f
        view.scaleX = 0.92f
        view.scaleY = 0.92f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(180).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun dismissInternal() {
        val vs = root ?: return
        if (dismissing) return
        dismissing = true
        LogUtil.d(LogUtil.OVERLAY, "menu dismissed")
        vs.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f)
            .setDuration(150)
            .withEndAction { teardownViews(notify = true) }
            .start()
    }

    private fun teardownViews(notify: Boolean) {
        root?.let { overlayWindow.removeView(it) }
        root = null
        card = null
        visible = false
        dismissing = false
        if (notify) listener?.onMenuDismissed()
        listener = null
    }

    private fun buildRow(item: ActionItem, dark: Boolean): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            foreground = ripple(dark)
            setOnClickListener { listener?.onActionClick(item.id) }
        }
        row.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48).toInt(),
        )

        val icon = ImageView(context).apply {
            setImageResource(item.iconRes)
            imageTintList = ColorStateList.valueOf(iconColor(dark))
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(24).toInt(), dp(24).toInt()).apply {
            rightMargin = dp(12).toInt()
        })

        val label = TextView(context).apply {
            text = context.getString(item.titleRes)
            setTextColor(textColor(dark))
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        return row
    }

    private fun cardBackground(dark: Boolean): Drawable = GradientDrawable().apply {
        cornerRadius = dpf(16)
        setColor(if (dark) Color.parseColor("#F02A2A2C") else Color.WHITE)
        setStroke(dp(1).toInt(), if (dark) 0x33FFFFFF else 0x1F000000)
    }

    private fun ripple(dark: Boolean): Drawable = RippleDrawable(
        ColorStateList.valueOf(if (dark) 0x33FFFFFF else 0x22000000),
        null,
        null,
    )

    private fun textColor(dark: Boolean) = if (dark) Color.WHITE else Color.parseColor("#1A1A1A")
    private fun iconColor(dark: Boolean) = if (dark) Color.parseColor("#DDDDDD") else Color.parseColor("#444444")

    private fun dp(value: Number): Int = (value.toFloat() * context.resources.displayMetrics.density).toInt()
    private fun dpf(value: Number): Float = value.toFloat() * context.resources.displayMetrics.density

    private companion object {
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val LOG = "OVERLAY"
    }
}
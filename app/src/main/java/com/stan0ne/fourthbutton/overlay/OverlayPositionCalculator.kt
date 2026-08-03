package com.stan0ne.fourthbutton.overlay

/**
 * Pure, framework-free helpers for placing the floating button and the menu
 * so they never fall outside the screen. Kept dependency-free so the math can
 * be unit tested.
 */
object OverlayPositionCalculator {

    fun clampFloat(value: Float, min: Float, max: Float): Float =
        value.coerceIn(min.coerceAtMost(max), max.coerceAtLeast(min))

    /**
     * Clamps the requested button centre so the button rectangle stays inside
     * the screen, keeping [edgeMargin] of free space on every side.
     */
    fun clampButtonPosition(
        desiredCenterX: Float,
        desiredCenterY: Float,
        buttonSize: Int,
        screenWidth: Int,
        screenHeight: Int,
        edgeMargin: Int,
    ): Pair<Float, Float> {
        val half = buttonSize / 2f
        val min = half + edgeMargin
        val maxX = screenWidth - half - edgeMargin
        val maxY = screenHeight - half - edgeMargin
        val x = clampFloat(desiredCenterX, min, maxX)
        val y = clampFloat(desiredCenterY, min, maxY)
        return x to y
    }

    /**
     * Positions the floating-menu top-left corner relative to the button.
     *
     * Prefers opening directly above the button. If there is not enough room
     * above it drops below. Horizontally it aligns to the button and is then
     * clamped so it never overflows either edge of the screen.
     */
    fun placeMenu(
        buttonCenterX: Float,
        buttonCenterY: Float,
        menuWidth: Int,
        menuHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        verticalGap: Int,
    ): Pair<Float, Float> {
        val maxMenuWidth = menuWidth.coerceAtMost(screenWidth)
        var x = (buttonCenterX - maxMenuWidth / 2f)
            .coerceIn(0f, (screenWidth - maxMenuWidth).coerceAtLeast(0).toFloat())

        // Try to open above first.
        var y = buttonCenterY - verticalGap - menuHeight
        if (y < 0f) {
            // Not enough room above - open below.
            y = buttonCenterY + verticalGap
        }
        if (y + menuHeight > screenHeight) {
            y = (screenHeight - menuHeight).coerceAtLeast(0).toFloat()
        }
        return x to y
    }
}
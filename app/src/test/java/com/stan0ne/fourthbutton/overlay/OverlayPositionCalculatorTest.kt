package com.stan0ne.fourthbutton.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPositionCalculatorTest {

    @Test
    fun `clampButton keeps center inside screen`() {
        val (x, y) = OverlayPositionCalculator.clampButtonPosition(
            desiredCenterX = -100f,
            desiredCenterY = 99999f,
            buttonSize = 56,
            screenWidth = 1080,
            screenHeight = 1920,
            edgeMargin = 16,
        )
        assertTrue(x >= 56 / 2f + 16)
        assertTrue(y <= 1920 - 56 / 2f - 16)
    }

    @Test
    fun `menu opens above the button`() {
        val (x, y) = OverlayPositionCalculator.placeMenu(
            buttonCenterX = 540f,
            buttonCenterY = 1600f,
            menuWidth = 180,
            menuHeight = 200,
            screenWidth = 1080,
            screenHeight = 1920,
            verticalGap = 12,
        )
        assertEquals(450f, x, 0.01f)
        assertEquals(1388f, y, 0.01f)
        assertTrue(y > 0)
    }

    @Test
    fun `menu drops below when there is no space above`() {
        val (_, y) = OverlayPositionCalculator.placeMenu(
            buttonCenterX = 540f,
            buttonCenterY = 100f,
            menuWidth = 180,
            menuHeight = 200,
            screenWidth = 1080,
            screenHeight = 1920,
            verticalGap = 12,
        )
        assertEquals(112f, y, 0.01f)
    }

    @Test
    fun `menu is clamped to not overflow the right edge`() {
        val (x, _) = OverlayPositionCalculator.placeMenu(
            buttonCenterX = 1080f,
            buttonCenterY = 800f,
            menuWidth = 180,
            menuHeight = 200,
            screenWidth = 1080,
            screenHeight = 1920,
            verticalGap = 12,
        )
        assertEquals(900f, x, 0.01f)
    }
}
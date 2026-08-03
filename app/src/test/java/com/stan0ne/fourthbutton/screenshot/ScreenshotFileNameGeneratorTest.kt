package com.stan0ne.fourthbutton.screenshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.regex.Pattern

class ScreenshotFileNameGeneratorTest {

    private val pattern = Pattern.compile(
        "Screenshot_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}(_\\d+)?\\.png"
    )

    @Test
    fun `filename uses Screenshot prefix and png extension`() {
        val name = ScreenshotFileNameGenerator.generate(Date(0L))
        assertTrue(name.startsWith("Screenshot_"))
        assertTrue(name.endsWith(".png"))
        assertTrue(pattern.matcher(name).matches())
    }

    @Test
    fun `index suffix is appended on collision`() {
        val a = ScreenshotFileNameGenerator.generate(Date(0L), 0)
        val b = ScreenshotFileNameGenerator.generate(Date(0L), 1)
        assertTrue(pattern.matcher(a).matches())
        assertTrue(pattern.matcher(b).matches())
        assertEquals(a.removeSuffix(".png") + "_1.png", b)
    }

    @Test
    fun `default index is zero`() {
        val a = ScreenshotFileNameGenerator.generate(Date(0L))
        val b = ScreenshotFileNameGenerator.generate(Date(0L), 0)
        assertEquals(a, b)
        assertTrue(a.endsWith(".png"))
    }
}
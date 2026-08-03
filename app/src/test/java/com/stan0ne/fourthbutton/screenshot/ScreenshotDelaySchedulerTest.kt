package com.stan0ne.fourthbutton.screenshot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotDelaySchedulerTest {

    @Test
    fun `ticks at each whole second then completes`() = runTest {
        val ticks = mutableListOf<Int>()
        var completed = false
        val scheduler = ScreenshotDelayScheduler(scope = this, delayMs = 1000)

        scheduler.start(3, ticks::add) { completed = true }

        advanceTimeBy(1000); runCurrent()
        advanceTimeBy(1000); runCurrent()
        advanceTimeBy(1000); runCurrent()

        assertEquals(listOf(3, 2, 1), ticks)
        assertTrue(completed)
    }

    @Test
    fun `cancel stops the countdown`() = runTest {
        val ticks = mutableListOf<Int>()
        var completed = false
        val scheduler = ScreenshotDelayScheduler(scope = this, delayMs = 1000)

        scheduler.start(3, ticks::add) { completed = true }
        advanceTimeBy(1000); runCurrent()

        assertTrue(scheduler.cancel())
        advanceTimeBy(3000); runCurrent()

        assertTrue(ticks.size <= 2)
        assertEquals(false, completed)
    }
}
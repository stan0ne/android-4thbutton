package com.stan0ne.fourthbutton.screenshot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Counts down a delay in whole seconds, invoking [onTick] each second and
 * [onComplete] when it reaches zero. Pure and injectable so the tick math is
 * unit-testable without Android.
 */
class ScreenshotDelayScheduler(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val delayMs: Long = 1000L,
) {

    private var job: Job? = null
    private var cancelled = false

    fun start(totalSeconds: Int, onTick: (remaining: Int) -> Unit, onComplete: () -> Unit) {
        cancel()
        cancelled = false
        job = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive && !cancelled) {
                onTick(remaining)
                delay(delayMs)
                remaining--
            }
            if (!cancelled && isActive) onComplete()
        }
    }

    /**
     * Cancels the pending screenshot. Returns true if there was an active
     * countdown, false otherwise (e.g. it already completed).
     */
    fun cancel(): Boolean {
        val hadJob = cancelled.not() && (job?.isActive == true)
        cancelled = true
        job?.cancel()
        job = null
        return hadJob
    }

    fun isCancelled(): Boolean = cancelled
}
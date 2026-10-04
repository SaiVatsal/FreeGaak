package com.saivatsal.soundorbit.core.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {

    @Test
    fun `execute runs action and returns result`() = runTest {
        val limiter = RateLimiter(minIntervalMs = 50)
        val result = limiter.execute { "success" }
        assertEquals("success", result)
    }

    @Test
    fun `rate limiter enforces delay between sequential calls`() = runTest {
        val minInterval = 100L
        val limiter = RateLimiter(
            minIntervalMs = minInterval,
            timeProvider = { testScheduler.currentTime }
        )

        val start = testScheduler.currentTime
        limiter.execute { 1 }
        limiter.execute { 2 }
        val elapsed = testScheduler.currentTime - start

        assertTrue(elapsed >= minInterval)
    }
}

package com.saivatsal.soundorbit.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe rate limiter using token bucket / delay algorithm.
 * Guarantees minimum interval between successive calls.
 */
class RateLimiter(
    private val minIntervalMs: Long,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()
    private var lastExecutionTimestamp = 0L

    suspend fun <T> execute(block: suspend () -> T): T {
        mutex.withLock {
            val now = timeProvider()
            val elapsed = now - lastExecutionTimestamp
            if (elapsed < minIntervalMs) {
                delay(minIntervalMs - elapsed)
            }
            lastExecutionTimestamp = timeProvider()
            return block()
        }
    }
}

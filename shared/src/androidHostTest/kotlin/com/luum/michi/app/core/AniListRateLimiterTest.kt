package com.luum.michi.app.core

import com.luum.michi.app.core.network.repository.AniListRateLimiter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The limiter's wait signal (`NetworkThrottle.waitingRequests`), driven with an
 * injected virtual clock and a no-op pause so the tests are instant and
 * deterministic. `WINDOW_LIMIT` requests fit a fresh window; the next one waits.
 */
class AniListRateLimiterTest {

    private val windowLimit = 28

    /** A limiter whose "now" is [clock] and whose wait advances [clock] instead of sleeping. */
    private fun virtualLimiter(
        clock: TestTimeSource,
        observed: MutableList<Int>,
    ): AniListRateLimiter {
        lateinit var limiter: AniListRateLimiter
        limiter = AniListRateLimiter(clock) { ms ->
            observed += limiter.waitingRequests.value
            clock += ms.milliseconds
        }
        return limiter
    }

    private suspend fun fillWindow(limiter: AniListRateLimiter) {
        repeat(windowLimit) { limiter.withPermit { } }
    }

    @Test
    fun waitingCountIsOneDuringTheWaitAndZeroAfterAdmission() = runBlocking {
        val clock = TestTimeSource()
        val observed = mutableListOf<Int>()
        val limiter = virtualLimiter(clock, observed)
        fillWindow(limiter)

        // The 29th request cannot fit the rolling window: it parks and then admits
        // once the clock crosses the window.
        limiter.withPermit { }

        assertEquals(listOf(1), observed, "the wait signals exactly one parked request")
        assertEquals(0, limiter.waitingRequests.value, "admission clears the count")
    }

    @Test
    fun aSubSecondWaitIsNotSurfaced() = runBlocking {
        val clock = TestTimeSource()
        val observed = mutableListOf<Int>()
        val limiter = virtualLimiter(clock, observed)
        fillWindow(limiter)
        // Slide close to the window's edge so the next wait is under a second.
        clock += 59_100.milliseconds

        limiter.withPermit { }

        assertEquals(listOf(0), observed, "a sub-second wait must not flicker the notice")
        assertEquals(0, limiter.waitingRequests.value)
    }

    @Test
    fun aCancelledWaitLeavesTheCountAtZero() = runBlocking {
        val clock = TestTimeSource()
        val gate = CompletableDeferred<Unit>()
        val limiter = AniListRateLimiter(clock) { gate.await() }
        fillWindow(limiter)

        val job = launch { limiter.withPermit { } }
        withTimeout(5_000) {
            while (limiter.waitingRequests.value == 0) yield()
        }
        assertEquals(1, limiter.waitingRequests.value)

        // Cancellation runs the `finally` that releases the count.
        job.cancelAndJoin()

        assertEquals(0, limiter.waitingRequests.value)
    }
}

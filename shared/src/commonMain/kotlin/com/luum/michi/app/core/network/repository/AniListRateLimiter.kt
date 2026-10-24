package com.luum.michi.app.core.network.repository

import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.network.domain.NetworkThrottle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Proactive client-side throttle for AniList's 30 req/min limit.
 *
 * Three layers of protection:
 *  1. Sliding-window token bucket: no more than [WINDOW_LIMIT] requests in any
 *     rolling [WINDOW_MILLIS] window. Callers suspend (not fail) until a slot opens.
 *  2. Concurrency cap: at most [MAX_IN_FLIGHT] requests in flight at once.
 *  3. Proactive backoff: when the server signals remaining budget is critically low
 *     (remaining <= [LOW_REMAINING_THRESHOLD]), or on a 429, all callers pause
 *     until a computed resume time.
 *
 * Usage pattern in the HTTP client:
 *   val response = rateLimiter.withPermit { httpClient.post(...) }
 *   rateLimiter.onResponseHeaders(remaining, resetEpoch)
 *
 * [withPermit] holds the concurrency permit only for the duration of the block (the
 * network round-trip) and releases it in a `finally` even on cancellation/exception,
 * so a canceled coroutine can never leak a permit and stall future requests.
 *
 * It also implements [NetworkThrottle]: [waitingRequests] counts the callers still
 * parked in phase 1, so the UI can show an honest notice while the brake is on.
 *
 * [timeSource] and [pause] are injectable only for tests: production uses the
 * monotonic clock and the real [delay]. A test can drive a virtual clock and skip
 * the wait entirely (see `AniListRateLimiterTest`).
 */
internal class AniListRateLimiter(
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val pause: suspend (Long) -> Unit = { delay(it.milliseconds) },
) : NetworkThrottle {

    private companion object {
        const val WINDOW_LIMIT = 28          // slots per rolling window (30 - 2 safety margin)
        const val WINDOW_MILLIS = 60_000L    // rolling window size in ms
        const val MAX_IN_FLIGHT = 5         // max concurrent HTTP calls
        const val LOW_REMAINING_THRESHOLD = 2
        const val FIXED_BACKOFF_MILLIS = 5_000L  // fallback when no reset header
        // Waits shorter than this are not surfaced: they would flicker the notice.
        const val MIN_VISIBLE_WAIT_MILLIS = 1_000L
    }

    // Monotonic base so we can translate mark values to delays.
    private val startMark = timeSource.markNow()

    private val mutex = Mutex()
    // Monotonic timestamps (ms) of the last WINDOW_LIMIT send events.
    private val sendTimes = ArrayDeque<Long>(WINDOW_LIMIT)
    // Monotonic mark (ms since [startMark]): suspend new acquire() calls until this
    // instant. 0L means "no active pause" (it is always in the past since nowMs() grows
    // from 0). Never use a negative sentinel here: computeWaitMs() subtracts it from now,
    // and Long.MIN_VALUE - now underflows to a huge positive wait (≈infinite delay).
    private var pauseUntilMark: Long = 0L

    private val semaphore = Semaphore(permits = MAX_IN_FLIGHT)

    // One state for the waiting callers: a counter-and-boolean pair could be
    // interleaved by two threads (A reaches 0 and sets false after B reached 1
    // and set true), leaving the notice off while a request still waits.
    private val _waitingRequests = MutableStateFlow(0)
    override val waitingRequests: StateFlow<Int> = _waitingRequests.asStateFlow()

    private fun nowMs(): Long = startMark.elapsedNow().inWholeMilliseconds

    /**
     * Runs [block] once a rate-window slot AND a concurrency permit are available.
     * May suspend for an extended time if the budget is exhausted or a backoff is active.
     * The concurrency permit is released in a `finally` (via [Semaphore.withPermit]) no
     * matter how [block] exits — including coroutine cancellation — so permits can never
     * leak and stall future requests. Never throws for throttle reasons; cancellation
     * propagates as usual.
     */
    suspend fun <T> withPermit(block: suspend () -> T): T {
        // Phase 1: wait for the sliding window + any active backoff (guarded by
        // mutex). The send timestamp is recorded inside the same lock that
        // decides admission, so concurrent callers can never all read "room
        // left" before any of them records — a burst cannot jump the cap.
        while (true) {
            val waitMs = mutex.withLock {
                val wait = computeWaitMs()
                if (wait <= 0L) sendTimes.addLast(nowMs())
                wait
            }
            if (waitMs <= 0L) break
            // Count only waits long enough to be worth surfacing; the exit runs
            // in `finally` so cancellation (or a thrown delay) cannot leak it.
            val visible = waitMs >= MIN_VISIBLE_WAIT_MILLIS
            if (visible) _waitingRequests.update { it + 1 }
            try {
                pause(waitMs)
            } finally {
                if (visible) _waitingRequests.update { it - 1 }
            }
        }

        // Phase 2: hold a concurrency permit for the round-trip; released in finally.
        return semaphore.withPermit { block() }
    }

    /**
     * Called after every HTTP response (success or non-429 error) to update proactive backoff
     * state from the server's rate-limit headers.
     *
     * @param remaining  Value of `X-RateLimit-Remaining` header, or null if absent.
     * @param resetEpochSeconds  Value of `X-RateLimit-Reset` header (Unix epoch seconds), or null.
     */
    suspend fun onResponseHeaders(remaining: Int?, resetEpochSeconds: Long?) {
        if (remaining == null || remaining > LOW_REMAINING_THRESHOLD) return

        val backoffMs = if (resetEpochSeconds != null) {
            // Convert server epoch to a monotonic delay duration.
            val nowEpoch = currentEpochSeconds()
            val secondsUntilReset = (resetEpochSeconds - nowEpoch).coerceAtLeast(0L)
            secondsUntilReset.seconds.inWholeMilliseconds
        } else {
            FIXED_BACKOFF_MILLIS
        }

        if (backoffMs > 0L) extendPause(backoffMs)
    }

    /**
     * Called by the existing 429 handler so concurrent callers also pause
     * for (at least) the server-mandated retry-after interval.
     */
    suspend fun onRateLimited(retryAfterSeconds: Long) {
        extendPause(retryAfterSeconds.seconds.inWholeMilliseconds)
    }

    // Only extends the pause window, never shortens an existing one. Guarded by
    // [mutex] so it stays consistent with the read in [computeWaitMs].
    private suspend fun extendPause(backoffMs: Long) {
        mutex.withLock {
            val resumeAt = nowMs() + backoffMs
            if (resumeAt > pauseUntilMark) {
                pauseUntilMark = resumeAt
            }
        }
    }

    /**
     * Returns how many milliseconds the caller should wait before sending.
     * Must be called while holding [mutex].
     */
    private fun computeWaitMs(): Long {
        val now = nowMs()

        // 1. Honor any active proactive backoff first. Compare before subtracting so a
        // stale/zero mark can never underflow into a huge positive wait.
        if (pauseUntilMark > now) return pauseUntilMark - now

        // 2. Evict timestamps that have aged out of the rolling window.
        val windowStart = now - WINDOW_MILLIS
        while (sendTimes.isNotEmpty() && sendTimes.first() <= windowStart) {
            sendTimes.removeFirst()
        }

        // 3. If there's still room in the window, no wait needed.
        if (sendTimes.size < WINDOW_LIMIT) return 0L

        // 4. Oldest slot in the window: wait until it ages out.
        val oldestTs = sendTimes.first()
        return (oldestTs + WINDOW_MILLIS - now + 1L).coerceAtLeast(1L)
    }
}

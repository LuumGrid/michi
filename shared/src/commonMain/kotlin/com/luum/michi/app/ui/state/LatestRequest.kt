package com.luum.michi.app.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns one "latest request wins" job: launching a new request supersedes the
 * previous one, and only the latest may write its result or settle (turn its
 * spinner off). Replaces the hand-rolled `launch(LAZY) { ... }` + job-identity
 * check that the async holders repeated.
 *
 * The new job is stored **before** the previous is cancelled, so a previous
 * request that ignores cancellation (and resumes synchronously) already reads
 * itself as stale and cannot write or settle over the new one.
 */
internal class LatestRequest {
    private var job: Job? = null

    /** True while the latest launched request is still running. */
    val isActive: Boolean get() = job?.isActive == true

    /**
     * Cancels the latest request (if any) and forgets it. The cancelled request
     * is no longer current, so it never writes nor settles: the caller owns its
     * spinner and must turn it off.
     */
    fun cancel() {
        job?.cancel()
        job = null
    }

    /**
     * Supersedes the previous request with [block]. [block] receives
     * `isCurrent()`: write only while it returns true (a superseded request must
     * not land). [onSettled] runs in the `finally` only while this is still the
     * latest request — a replacement's spinner is never touched.
     */
    fun launch(
        scope: CoroutineScope,
        onSettled: () -> Unit = {},
        block: suspend (isCurrent: () -> Boolean) -> Unit,
    ) {
        val previous = job
        // LAZY then start: the job is stored before the body runs, so the
        // identity check is valid even on an eagerly dispatched scope.
        val request = scope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext[Job]
            try {
                block { job === self }
            } finally {
                if (job === self) onSettled()
            }
        }
        job = request
        previous?.cancel()
        request.start()
    }
}

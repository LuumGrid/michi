package com.luum.michi.app.core.network.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only window into the transport's rate limiter, for the UI to be honest
 * when a request is actually being held back.
 *
 * [waitingRequests] is the number of requests currently parked in the rate
 * limiter's wait (the sliding window / backoff phase), i.e. requests that have
 * been admitted to the client but cannot be sent yet. It is `0` whenever
 * nothing is throttled, so the UI can show a notice only while there is a real
 * wait — never on a timer and never for a merely slow network. A single state
 * flow (not a counter + boolean) keeps the message consistent across threads.
 */
internal interface NetworkThrottle {
    val waitingRequests: StateFlow<Int>
}

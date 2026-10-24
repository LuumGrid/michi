package com.luum.michi.app.ui.state

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A gate that ignores cancellation: the awaiting request stays suspended until
 * it completes, so a superseded body still runs its write/settle path.
 */
private suspend fun CompletableDeferred<Unit>.awaitIgnoringCancellation() {
    suspendCoroutine<Unit> { cont -> invokeOnCompletion { cont.resume(Unit) } }
}

class LatestRequestTest {

    @Test
    fun aReplacedRequestWritesNothingAndItsSettleDoesNotFire() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val request = LatestRequest()
        val gateA = CompletableDeferred<Unit>()
        val gateB = CompletableDeferred<Unit>()
        val writes = mutableListOf<String>()
        var settled = 0

        request.launch(scope, onSettled = { settled++ }) { isCurrent ->
            gateA.awaitIgnoringCancellation()
            if (isCurrent()) writes += "A"
        }
        // B supersedes A while A is in flight.
        request.launch(scope, onSettled = { settled++ }) { isCurrent ->
            gateB.awaitIgnoringCancellation()
            if (isCurrent()) writes += "B"
        }
        assertTrue(request.isActive)

        gateB.complete(Unit) // B completes normally
        assertEquals(listOf("B"), writes)
        assertEquals(1, settled, "only B settles")
        assertFalse(request.isActive)

        gateA.complete(Unit) // A's late response: it is no longer current
        assertEquals(listOf("B"), writes, "the replaced request must not write")
        assertEquals(1, settled, "the replaced request's onSettled must not fire")
    }

    @Test
    fun aCancelWithoutARelaunchLeavesTheSpinnerToTheCaller() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val request = LatestRequest()
        val gate = CompletableDeferred<Unit>()
        var settled = 0

        request.launch(scope, onSettled = { settled++ }) { isCurrent ->
            gate.awaitIgnoringCancellation()
            if (isCurrent()) error("a cancelled request must not be current")
        }
        assertTrue(request.isActive)

        request.cancel()
        assertFalse(request.isActive)

        gate.complete(Unit)
        assertEquals(0, settled, "cancel() forgets the job: the caller owns its spinner")
    }
}

package com.luum.michi.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * FIFO dispatcher that only runs when [runAll] is called: lets a test order
 * `launch`ed work deterministically (a page/clear race, a coalesced save)
 * without pulling in `kotlinx-coroutines-test`.
 */
internal class DeferredDispatcher : CoroutineDispatcher() {
    private val tasks = ArrayDeque<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.addLast(block)
    }

    fun runAll() {
        while (tasks.isNotEmpty()) tasks.removeFirst().run()
    }
}

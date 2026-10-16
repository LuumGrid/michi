package com.luum.michi.app.ui.state

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * PagedList owns the page-in-flight job and the append/dedup rules; the holders
 * only pass the fetch. These cover the three rules the holders rely on.
 */
class PagedListTest {

    @Test
    fun aPageThatArrivesAfterClearIsNotAppended() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val list = PagedList<Int>()
            list.set(Page(listOf(1), hasNextPage = true, currentPage = 1))

            list.loadMore(
                scope = scope,
                isCurrent = { true },
                fetch = { NetworkResult.Success(Page(listOf(2), hasNextPage = false, currentPage = 2)) },
            )
            // clear() cancels the queued page: it must never land.
            list.clear()
            dispatcher.runAll()

            assertEquals(emptyList(), list.items)
            assertFalse(list.hasNextPage)
            assertEquals(1, list.currentPage)
            assertFalse(list.isLoadingMore)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun mergeDeduplicatesAcrossPages() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val list = PagedList<Int> { a, b -> (a + b).distinct() }
            list.set(Page(listOf(1, 2), hasNextPage = true, currentPage = 1))

            list.loadMore(
                scope = scope,
                isCurrent = { true },
                fetch = { NetworkResult.Success(Page(listOf(2, 3), hasNextPage = false, currentPage = 2)) },
            )
            dispatcher.runAll()

            assertEquals(listOf(1, 2, 3), list.items)
            assertFalse(list.hasNextPage)
            assertEquals(2, list.currentPage)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aFailureCallsOnErrorAndDropsTheSpinner() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val list = PagedList<Int>()
            list.set(Page(listOf(1), hasNextPage = true, currentPage = 1))
            var error: NetworkError? = null

            list.loadMore(
                scope = scope,
                isCurrent = { true },
                fetch = { NetworkResult.Failure(NetworkError.NoConnection) },
                onError = { error = it },
            )
            dispatcher.runAll()

            assertEquals(NetworkError.NoConnection, error)
            assertFalse(list.isLoadingMore)
            assertEquals(listOf(1), list.items)
        } finally {
            scope.cancel()
        }
    }
}

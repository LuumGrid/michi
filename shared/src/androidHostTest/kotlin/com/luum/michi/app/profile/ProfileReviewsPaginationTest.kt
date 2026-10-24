package com.luum.michi.app.profile

import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileReviewsStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reviews pagination through the real repository + holder seam. */
class ProfileReviewsPaginationTest {

    private fun holder(client: FakeProfileGraphQL) = ProfileReviewsStateHolder(
        ProfileRepositoryImpl(client),
        CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun firstPageLoadsReviewsWithTheirMedia() {
        val client = FakeProfileGraphQL(listOf(reviewsPage(false, Triple(1, 10, "A"))))
        val holder = holder(client)
        holder.load(7)
        val review = holder.reviews.single()
        assertEquals(10, review.mediaId)
        assertEquals("A", review.mediaTitle)
        assertEquals(80, review.score)
        assertEquals("summary-1", review.summary)
        assertNull(holder.error)
    }

    @Test
    fun theQuerySortsNewestFirst() {
        val client = FakeProfileGraphQL(listOf(reviewsPage(false, Triple(1, 10, "A"))))
        holder(client).load(7)
        assertTrue(client.requests.single().query.contains("sort: [ID_DESC]"))
    }

    @Test
    fun loadMoreAppendsAndDeduplicates() {
        val client = FakeProfileGraphQL(
            listOf(
                reviewsPage(true, Triple(1, 10, "A")),
                reviewsPage(false, Triple(1, 10, "A"), Triple(2, 11, "B")),
            ),
        )
        val holder = holder(client)
        holder.load(7)
        assertTrue(holder.hasNextPage)
        holder.loadMore()
        assertEquals(listOf("A", "B"), holder.reviews.map { it.mediaTitle })
        assertEquals(false, holder.hasNextPage)
    }

    @Test
    fun aFailedPageGoesToActionErrorNotTheBlockingError() {
        val client = FakeProfileGraphQL(
            listOf(reviewsPage(true, Triple(1, 10, "A"))),
            failureAt = 1,
        )
        val holder = holder(client)
        holder.load(7)
        assertTrue(holder.hasNextPage)

        holder.loadMore()

        assertNull(holder.error)
        assertTrue(holder.actionError != null)
    }

    @Test
    fun withinTheTtlReopeningCostsNoRequest() {
        val client = FakeProfileGraphQL(listOf(reviewsPage(false, Triple(1, 10, "A"))))
        val holder = holder(client)
        holder.load(7)
        holder.load(7)
        assertEquals(1, client.calls)
    }
}

package com.luum.michi.app.mediaDetail

import com.luum.michi.app.mediaDetail.domain.media.model.MediaTag
import com.luum.michi.app.mediaDetail.domain.media.model.hasTags
import com.luum.michi.app.mediaDetail.domain.media.model.orderedTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaDetailTagsTest {

    private fun tag(name: String, spoiler: Boolean = false) = MediaTag(
        name = name,
        rank = 50,
        isMediaSpoiler = spoiler,
    )

    @Test
    fun withoutSpoilersEveryTagIsVisibleAndTheSectionExists() {
        val tags = listOf(tag("Action"), tag("Comedy"))

        assertTrue(tags.hasTags())
        assertEquals(listOf("Action", "Comedy"), tags.orderedTags(showSpoilers = false).map { it.name })
    }

    @Test
    fun hiddenSpoilersAreFilteredOut() {
        val tags = listOf(tag("Action"), tag("Twist", spoiler = true), tag("Comedy"))

        assertTrue(tags.hasTags())
        assertEquals(listOf("Action", "Comedy"), tags.orderedTags(showSpoilers = false).map { it.name })
    }

    @Test
    fun spoilerTagsComeAfterTheRegularOnesWhenShown() {
        val tags = listOf(tag("Twist", spoiler = true), tag("Action"), tag("Secret", spoiler = true), tag("Comedy"))

        assertEquals(
            listOf("Action", "Comedy", "Twist", "Secret"),
            tags.orderedTags(showSpoilers = true).map { it.name },
        )
    }

    @Test
    fun onlySpoilersStillShowsTheSectionButNoChipsUntilRevealed() {
        val tags = listOf(tag("Twist", spoiler = true))

        assertTrue(tags.hasTags())
        assertTrue(tags.orderedTags(showSpoilers = false).isEmpty())
        assertEquals(listOf("Twist"), tags.orderedTags(showSpoilers = true).map { it.name })
    }

    @Test
    fun anEmptyTagListHidesTheSection() {
        assertFalse(emptyList<MediaTag>().hasTags())
    }
}

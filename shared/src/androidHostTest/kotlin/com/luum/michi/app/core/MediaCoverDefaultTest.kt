package com.luum.michi.app.core

import com.luum.michi.app.core.media.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.media.repository.toCoverSizes
import com.luum.michi.app.core.model.CoverSizes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val DefaultMedium = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/default.jpg"
private const val DefaultLarge = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/default.jpg"
private const val DefaultExtraLarge = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/extraLarge/default.jpg"
private const val RealMedium = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx16498-73IhOXpJZiMF.jpg"
private const val RealLarge = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx16498-73IhOXpJZiMF.jpg"

/** Work covers drop AniList's shared art exactly like people images do. */
class MediaCoverDefaultTest {

    @Test
    fun everyDefaultVariantMapsToNull() {
        assertNull(
            MediaCoverImageDto(
                medium = DefaultMedium,
                large = DefaultLarge,
                extraLarge = DefaultExtraLarge,
            ).toCoverSizes(),
        )
    }

    @Test
    fun aRealVariantSurvivesADefaultTwin() {
        assertEquals(
            CoverSizes(medium = RealMedium, large = RealLarge),
            MediaCoverImageDto(medium = RealMedium, large = RealLarge, extraLarge = DefaultExtraLarge)
                .toCoverSizes(),
        )
    }
}

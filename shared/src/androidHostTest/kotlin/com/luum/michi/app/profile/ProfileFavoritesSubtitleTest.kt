package com.luum.michi.app.profile

import com.luum.michi.app.core.media.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.person.repository.dto.StudioDto
import com.luum.michi.app.profile.repository.FavoriteCharacterDto
import com.luum.michi.app.profile.repository.FavoriteStaffDto
import com.luum.michi.app.profile.repository.toProfileFavorite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The second line of a profile favourite comes from the same query that
 *  already paints the card: format + year, the character's most popular anime,
 *  or the staff member's first non-blank occupation. */
class ProfileFavoritesSubtitleTest {

    @Test
    fun aMediaFavoriteUsesFormatAndSeasonYear() {
        val favorite = AniListJson.decodeFromString(
            MediaDto.serializer(),
            """{"id":1,"title":{"userPreferred":"A"},"format":"TV","seasonYear":2013}""",
        ).toProfileFavorite()

        assertEquals("TV · 2013", favorite.subtitle)
    }

    @Test
    fun aMediaFavoriteFallsBackToTheStartDateYear() {
        val favorite = AniListJson.decodeFromString(
            MediaDto.serializer(),
            """{"id":2,"title":{"userPreferred":"B"},"format":"MANGA","startDate":{"year":2009}}""",
        ).toProfileFavorite()

        assertEquals("Manga · 2009", favorite.subtitle)
    }

    @Test
    fun aMediaFavoriteWithoutFormatOrYearHasNoSecondLine() {
        val favorite = AniListJson.decodeFromString(
            MediaDto.serializer(),
            """{"id":3,"title":{"userPreferred":"C"}}""",
        ).toProfileFavorite()

        assertNull(favorite.subtitle)
    }

    @Test
    fun aCharacterFavoriteUsesItsMostPopularMediaTitle() {
        val favorite = AniListJson.decodeFromString(
            FavoriteCharacterDto.serializer(),
            """{"id":4,"name":{"userPreferred":"Levi"},"media":{"nodes":[{"id":9,"title":{"userPreferred":"Shingeki no Kyojin"}}]}}""",
        ).toProfileFavorite()

        assertEquals("Shingeki no Kyojin", favorite.subtitle)
    }

    @Test
    fun aCharacterFavoriteWithoutMediaHasNoSecondLine() {
        val favorite = AniListJson.decodeFromString(
            FavoriteCharacterDto.serializer(),
            """{"id":5,"name":{"userPreferred":"X"},"media":{"nodes":[]}}""",
        ).toProfileFavorite()

        assertNull(favorite.subtitle)
    }

    @Test
    fun aStaffFavoriteUsesTheFirstNonBlankOccupation() {
        val favorite = AniListJson.decodeFromString(
            FavoriteStaffDto.serializer(),
            """{"id":6,"name":{"userPreferred":"Y"},"primaryOccupations":["","Director"]}""",
        ).toProfileFavorite()

        assertEquals("Director", favorite.subtitle)
    }

    @Test
    fun aStaffFavoriteWithoutOccupationsHasNoSecondLine() {
        val favorite = AniListJson.decodeFromString(
            FavoriteStaffDto.serializer(),
            """{"id":7,"name":{"userPreferred":"Z"},"primaryOccupations":null}""",
        ).toProfileFavorite()

        assertNull(favorite.subtitle)
    }

    @Test
    fun aStudioFavoriteHasNoSecondLine() {
        val favorite = AniListJson.decodeFromString(
            StudioDto.serializer(),
            """{"id":8,"name":"MAPPA"}""",
        ).toProfileFavorite()

        assertNull(favorite.subtitle)
    }
}

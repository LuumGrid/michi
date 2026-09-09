package com.luum.michi.app.account

import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.account.repository.toAccountFavoritePerson
import com.luum.michi.app.account.repository.toAccountFavoriteStudio
import com.luum.michi.app.account.repository.toAccountMediaTypeStats
import com.luum.michi.app.account.repository.toDomain
import com.luum.michi.app.core.network.repository.dto.CharacterDto
import com.luum.michi.app.core.network.repository.dto.FavouritesDto
import com.luum.michi.app.core.network.repository.dto.MediaConnectionDto
import com.luum.michi.app.core.network.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.dto.MediaTitleDto
import com.luum.michi.app.core.network.repository.dto.PersonImageDto
import com.luum.michi.app.core.network.repository.dto.PersonNameDto
import com.luum.michi.app.core.network.repository.dto.StaffDto
import com.luum.michi.app.core.network.repository.dto.StudioDto
import com.luum.michi.app.core.network.repository.dto.UserStatisticFormatDto
import com.luum.michi.app.core.network.repository.dto.UserStatisticGenreDto
import com.luum.michi.app.core.network.repository.dto.UserStatisticScoreDto
import com.luum.michi.app.core.network.repository.dto.UserStatisticStatusDto
import com.luum.michi.app.core.network.repository.dto.UserStatisticsDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountMappersTest {

    @Test
    fun nullStatsMapToDefaults() {
        val stats = (null as UserStatisticsDto?).toAccountMediaTypeStats(isManga = false)
        assertEquals(0, stats.count)
        assertEquals(0, stats.episodesOrChapters)
        assertTrue(stats.scoreDistribution.isEmpty())
    }

    @Test
    fun distributionsSortScoresAscendingAndRestByCount() {
        val dto = UserStatisticsDto(
            scores = listOf(
                UserStatisticScoreDto(score = 90, count = 5),
                UserStatisticScoreDto(score = 80, count = 2),
            ),
            formats = listOf(
                UserStatisticFormatDto(format = "MOVIE", count = 3),
                UserStatisticFormatDto(format = "TV", count = 10),
            ),
            statuses = listOf(
                UserStatisticStatusDto(status = "DROPPED", count = 1),
                UserStatisticStatusDto(status = "COMPLETED", count = 7),
            ),
            genres = listOf(
                UserStatisticGenreDto(genre = "Drama", count = 4),
                UserStatisticGenreDto(genre = "Action", count = 9),
            ),
        )
        val stats = dto.toAccountMediaTypeStats(isManga = false)
        assertEquals(listOf("80", "90"), stats.scoreDistribution.map { it.label })
        assertEquals(listOf("TV", "MOVIE"), stats.formatDistribution.map { it.label })
        assertEquals(listOf("COMPLETED", "DROPPED"), stats.statusDistribution.map { it.label })
        assertEquals(listOf("Action", "Drama"), stats.topGenres.map { it.label })
    }

    @Test
    fun favoritesMapAllBucketsWithFallbacks() {
        val favourites = FavouritesDto(
            anime = MediaConnectionDto(
                nodes = listOf(MediaDto(id = 1, title = MediaTitleDto(userPreferred = "A"))),
            ),
            characters = null,
            staff = null,
            studios = null,
        ).toDomain()
        assertEquals("A", favourites.anime.single().title)
        assertTrue(favourites.manga.isEmpty())
        assertTrue(favourites.characters.isEmpty())
    }

    @Test
    fun personWithoutNameFallsBackToBlank() {
        val favourites = FavouritesDto(
            characters = null,
            staff = null,
            studios = null,
        ).toDomain()
        assertTrue(favourites.characters.isEmpty())
        val person = CharacterDto(id = 2, name = null, image = PersonImageDto(large = "img"))
        assertEquals("", person.toAccountFavoritePerson().name)
        assertEquals("img", person.toAccountFavoritePerson().imageUrl)
        val staff = StaffDto(
            id = 3,
            name = PersonNameDto(first = "Hayao", last = "Miyazaki"),
            image = null,
        )
        assertEquals("Hayao Miyazaki", staff.toAccountFavoritePerson().name)
    }

    @Test
    fun studioWithoutMediaHasNoCover() {
        val studio = StudioDto(id = 4, name = "Studio")
        assertEquals("Studio", studio.toAccountFavoriteStudio().name)
        assertEquals(null, studio.toAccountFavoriteStudio().coverUrl)
    }

    @Test
    fun emptyFavoritesConstantIsEmpty() {
        assertTrue(AccountFavorites.EMPTY.anime.isEmpty())
        assertTrue(AccountFavorites.EMPTY.studios.isEmpty())
    }
}

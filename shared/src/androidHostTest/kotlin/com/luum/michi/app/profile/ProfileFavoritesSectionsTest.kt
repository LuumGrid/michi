package com.luum.michi.app.profile

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.itemsFor
import com.luum.michi.app.profile.domain.model.nonEmptyCategories
import com.luum.michi.app.profile.ui.model.favoritesCategoryOfOrNull
import com.luum.michi.app.profile.ui.model.title
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileFavoritesSectionsTest {

    private val one = ProfileFavorite(id = 1, name = "One", image = null, paletteHex = null)

    @Test
    fun emptyCategoriesAreNeverPainted() {
        val favorites = ProfileFavorites(
            anime = listOf(one),
            manga = emptyList(),
            characters = listOf(one),
            staff = emptyList(),
            studios = emptyList(),
        )

        assertEquals(
            listOf(ProfileFavoritesCategory.ANIME, ProfileFavoritesCategory.CHARACTERS),
            favorites.nonEmptyCategories(),
        )
    }

    @Test
    fun aProfileWithNoFavoritesHasNoSections() {
        assertEquals(emptyList(), ProfileFavorites.EMPTY.nonEmptyCategories())
    }

    @Test
    fun itemsForReadsTheRightBucket() {
        val studio = ProfileFavorite(id = 9, name = "Studio", image = null, paletteHex = null)
        val favorites = ProfileFavorites(
            anime = emptyList(),
            manga = emptyList(),
            characters = emptyList(),
            staff = emptyList(),
            studios = listOf(studio),
        )
        assertEquals(listOf(studio), favorites.itemsFor(ProfileFavoritesCategory.STUDIOS))
        assertEquals(emptyList(), favorites.itemsFor(ProfileFavoritesCategory.ANIME))
    }

    @Test
    fun aCategoryIdRoundTripsThroughItsEnumName() {
        ProfileFavoritesCategory.entries.forEach { category ->
            assertEquals(category, favoritesCategoryOfOrNull(category.name))
        }
        assertNull(favoritesCategoryOfOrNull("NOT_A_CATEGORY"))
    }

    @Test
    fun everyCategoryHasItsLocalizedTitle() {
        assertEquals("Favorite anime", ProfileFavoritesCategory.ANIME.title(EnglishStrings))
        assertEquals("Favorite manga", ProfileFavoritesCategory.MANGA.title(EnglishStrings))
        assertEquals("Favorite characters", ProfileFavoritesCategory.CHARACTERS.title(EnglishStrings))
        assertEquals("Favorite staff", ProfileFavoritesCategory.STAFF.title(EnglishStrings))
        assertEquals("Favorite studios", ProfileFavoritesCategory.STUDIOS.title(EnglishStrings))
    }
}

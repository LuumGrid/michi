package com.luum.michi.app.profile.domain.model
import com.luum.michi.app.core.model.CoverSizes


/** One favourited entity of any kind (anime/manga/character/staff/studio).
 *  Studios have no image on AniList, so [image]/[paletteHex] stay null for them. */
internal data class ProfileFavorite(
    val id: Int,
    val name: String,
    val image: CoverSizes?,
    val paletteHex: String?,
)

internal data class ProfileFavorites(
    val anime: List<ProfileFavorite>,
    val manga: List<ProfileFavorite>,
    val characters: List<ProfileFavorite>,
    val staff: List<ProfileFavorite>,
    val studios: List<ProfileFavorite>,
) {
    companion object {
        val EMPTY = ProfileFavorites(
            anime = emptyList(),
            manga = emptyList(),
            characters = emptyList(),
            staff = emptyList(),
            studios = emptyList(),
        )
    }
}

/** Which favourites bucket a "see more" grid should page through. */
internal enum class ProfileFavoritesCategory {
    ANIME,
    MANGA,
    CHARACTERS,
    STAFF,
    STUDIOS,
}

/** The favourites of one category, in the order the rail/grid paints them. */
internal fun ProfileFavorites.itemsFor(category: ProfileFavoritesCategory): List<ProfileFavorite> =
    when (category) {
        ProfileFavoritesCategory.ANIME -> anime
        ProfileFavoritesCategory.MANGA -> manga
        ProfileFavoritesCategory.CHARACTERS -> characters
        ProfileFavoritesCategory.STAFF -> staff
        ProfileFavoritesCategory.STUDIOS -> studios
    }

/**
 * The categories that actually carry favourites, in display order. Empty
 * categories are never painted, so the screen only iterates over these.
 */
internal fun ProfileFavorites.nonEmptyCategories(): List<ProfileFavoritesCategory> =
    ProfileFavoritesCategory.entries.filter { itemsFor(it).isNotEmpty() }

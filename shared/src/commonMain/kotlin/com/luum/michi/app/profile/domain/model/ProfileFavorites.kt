package com.luum.michi.app.profile.domain.model
import com.luum.michi.app.core.model.CoverSizes


internal data class ProfileFavoriteMedia(
    val id: Int,
    val title: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
)

internal data class ProfileFavoritePerson(
    val id: Int,
    val name: String,
    val image: CoverSizes?,
    val paletteHex: String?,
)

internal data class ProfileFavoriteStudio(
    val id: Int,
    val name: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
)

internal data class ProfileFavorites(
    val anime: List<ProfileFavoriteMedia>,
    val manga: List<ProfileFavoriteMedia>,
    val characters: List<ProfileFavoritePerson>,
    val staff: List<ProfileFavoritePerson>,
    val studios: List<ProfileFavoriteStudio>,
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

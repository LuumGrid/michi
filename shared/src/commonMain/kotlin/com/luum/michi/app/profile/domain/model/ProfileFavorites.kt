package com.luum.michi.app.profile.domain.model


internal data class ProfileFavoriteMedia(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val paletteHex: String?,
)

internal data class ProfileFavoritePerson(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val paletteHex: String?,
)

internal data class ProfileFavoriteStudio(
    val id: Int,
    val name: String,
    val coverUrl: String?,
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

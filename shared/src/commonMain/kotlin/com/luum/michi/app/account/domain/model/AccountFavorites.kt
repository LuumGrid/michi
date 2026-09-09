package com.luum.michi.app.account.domain.model


internal data class AccountFavoriteMedia(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val paletteHex: String?,
)

internal data class AccountFavoritePerson(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val paletteHex: String?,
)

internal data class AccountFavoriteStudio(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val paletteHex: String?,
)

internal data class AccountFavorites(
    val anime: List<AccountFavoriteMedia>,
    val manga: List<AccountFavoriteMedia>,
    val characters: List<AccountFavoritePerson>,
    val staff: List<AccountFavoritePerson>,
    val studios: List<AccountFavoriteStudio>,
) {
    companion object {
        val EMPTY = AccountFavorites(
            anime = emptyList(),
            manga = emptyList(),
            characters = emptyList(),
            staff = emptyList(),
            studios = emptyList(),
        )
    }
}

/** Which favourites bucket a "see more" grid should page through. */
internal enum class AccountFavoritesCategory {
    ANIME,
    MANGA,
    CHARACTERS,
    STAFF,
    STUDIOS,
}

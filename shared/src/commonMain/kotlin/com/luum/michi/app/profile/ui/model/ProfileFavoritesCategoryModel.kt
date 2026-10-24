package com.luum.michi.app.profile.ui.model

import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory

/** Localized title of a favourites category: the rail header and the "View all"
 *  page title in the toolbar. */
internal fun ProfileFavoritesCategory.title(strings: LanguageStrings): String = when (this) {
    ProfileFavoritesCategory.ANIME -> strings.profileFavoriteAnimeTitle
    ProfileFavoritesCategory.MANGA -> strings.profileFavoriteMangaTitle
    ProfileFavoritesCategory.CHARACTERS -> strings.profileFavoriteCharactersTitle
    ProfileFavoritesCategory.STAFF -> strings.profileFavoriteStaffTitle
    ProfileFavoritesCategory.STUDIOS -> strings.profileFavoriteStudiosTitle
}

/**
 * Resolves the category id that travels in `DetailDestination.ProfileFavorites`
 * (core must not import `profile`, so the enum travels as its name).
 */
internal fun favoritesCategoryOfOrNull(id: String): ProfileFavoritesCategory? =
    ProfileFavoritesCategory.entries.firstOrNull { it.name == id }

/** The favourite mutation target of a category item (removing a favourite). */
internal fun ProfileFavoritesCategory.toFavouriteTarget(id: Int): FavouriteTarget = when (this) {
    ProfileFavoritesCategory.ANIME -> FavouriteTarget.Anime(id)
    ProfileFavoritesCategory.MANGA -> FavouriteTarget.Manga(id)
    ProfileFavoritesCategory.CHARACTERS -> FavouriteTarget.Character(id)
    ProfileFavoritesCategory.STAFF -> FavouriteTarget.Staff(id)
    ProfileFavoritesCategory.STUDIOS -> FavouriteTarget.Studio(id)
}

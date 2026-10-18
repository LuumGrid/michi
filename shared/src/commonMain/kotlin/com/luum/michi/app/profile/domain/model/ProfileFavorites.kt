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

/** One favorited entity in one category: the key of a removal/rollback. */
internal data class ProfileFavoriteKey(
    val category: ProfileFavoritesCategory,
    val id: Int,
)

/** Removes one favourite (optimistic edit). Never reorders the rest. */
internal fun ProfileFavorites.removeFavorite(
    category: ProfileFavoritesCategory,
    id: Int,
): ProfileFavorites = withBucket(category) { list -> list.filterNot { it.id == id } }

/**
 * Puts [favorite] back at [index] (a failed removal's rollback). AniList exposes
 * no favourite position, so the loaded order is restored as it was.
 */
internal fun ProfileFavorites.restoreFavorite(
    category: ProfileFavoritesCategory,
    index: Int,
    favorite: ProfileFavorite,
): ProfileFavorites = withBucket(category) { list ->
    list.toMutableList().also { it.add(index.coerceIn(0, it.size), favorite) }
}

/**
 * Drops the favourites named by [keys]. A profile load that started before a
 * removal must not repaint it on arrival: the result is filtered through the
 * in-flight and already-removed keys before it replaces the visible state.
 */
internal fun ProfileFavorites.without(keys: Set<ProfileFavoriteKey>): ProfileFavorites {
    if (keys.isEmpty()) return this
    val idsByCategory = keys.groupBy({ it.category }, { it.id }).mapValues { it.value.toSet() }
    fun keep(category: ProfileFavoritesCategory, list: List<ProfileFavorite>): List<ProfileFavorite> {
        val ids = idsByCategory[category] ?: return list
        return list.filterNot { it.id in ids }
    }
    return ProfileFavorites(
        anime = keep(ProfileFavoritesCategory.ANIME, anime),
        manga = keep(ProfileFavoritesCategory.MANGA, manga),
        characters = keep(ProfileFavoritesCategory.CHARACTERS, characters),
        staff = keep(ProfileFavoritesCategory.STAFF, staff),
        studios = keep(ProfileFavoritesCategory.STUDIOS, studios),
    )
}

private inline fun ProfileFavorites.withBucket(
    category: ProfileFavoritesCategory,
    transform: (List<ProfileFavorite>) -> List<ProfileFavorite>,
): ProfileFavorites = when (category) {
    ProfileFavoritesCategory.ANIME -> copy(anime = transform(anime))
    ProfileFavoritesCategory.MANGA -> copy(manga = transform(manga))
    ProfileFavoritesCategory.CHARACTERS -> copy(characters = transform(characters))
    ProfileFavoritesCategory.STAFF -> copy(staff = transform(staff))
    ProfileFavoritesCategory.STUDIOS -> copy(studios = transform(studios))
}

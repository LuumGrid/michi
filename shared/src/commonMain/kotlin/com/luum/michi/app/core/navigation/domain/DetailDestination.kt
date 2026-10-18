package com.luum.michi.app.core.navigation.domain

import com.luum.michi.app.core.media.domain.MediaType

internal sealed interface DetailDestination {
    data class Media(val id: Int) : DetailDestination
    data class Character(val id: Int) : DetailDestination
    data class Staff(val id: Int) : DetailDestination
    data class Studio(val id: Int) : DetailDestination

    /**
     * Works of one genre. [type] is the catalog the page opens on; the screen
     * offers an Anime/Manga switch, so the destination only seeds it.
     */
    data class Genre(val name: String, val type: MediaType) : DetailDestination

    /**
     * "View all" for a Dashboard rail. Core must not import `discover`, so the
     * rail travels as its enum name ([kindId]) and the host resolves it to the
     * shared catalog filter + rail title.
     */
    data class Catalog(val kindId: String) : DetailDestination

    /**
     * "View all" of the viewer's favourites of one category. Core must not
     * import `profile`, so the category travels as its enum name; the host
     * resolves it to the paginated favourites grid and its toolbar title.
     */
    data class ProfileFavorites(val categoryId: String) : DetailDestination

    /**
     * The viewer's statistics page, seeded on one catalog type (the header
     * counter the user tapped). Data comes from the profile query, so this page
     * costs no request of its own.
     */
    data class ProfileStatistics(val type: MediaType) : DetailDestination

    /**
     * The viewer's following or followers list. Core must not import `profile`,
     * so the kind travels as its enum name.
     */
    data class ProfileFollows(val kindId: String) : DetailDestination

    /** The viewer's reviews. Tapping one opens the reviewed media's detail. */
    data object ProfileReviews : DetailDestination

    /**
     * "View all" list reached from a detail page: the rail shows the first
     * items, this screen shows the whole (paginated) list. [parentId] keys
     * the source so `Root` can read the right holder.
     */
    data class DetailList(val kind: DetailListKind, val parentId: Int) : DetailDestination
}

/** Which section a [DetailDestination.DetailList] renders. */
internal enum class DetailListKind {
    MEDIA_CHARACTERS,
    MEDIA_STAFF,
    MEDIA_RELATED,
    MEDIA_RECOMMENDATIONS,
    CHARACTER_MEDIA,
    STAFF_MEDIA,
    STAFF_CHARACTERS,
}

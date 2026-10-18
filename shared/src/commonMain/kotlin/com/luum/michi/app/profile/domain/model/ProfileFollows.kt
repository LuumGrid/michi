package com.luum.michi.app.profile.domain.model

import com.luum.michi.app.core.model.CoverSizes

/** One user of a following/followers list. The avatar is the row's cover. */
internal data class ProfileFollow(
    val id: Int,
    val name: String,
    val avatar: CoverSizes?,
)

/**
 * Which side of the viewer's social graph a list page shows. Core must not
 * import `profile`, so the destination travels it as its enum name (same
 * pattern as [ProfileFavoritesCategory]).
 */
internal enum class ProfileFollowsKind {
    FOLLOWING,
    FOLLOWERS,
}

/** Resolves the kind id that travels in `DetailDestination.ProfileFollows`. */
internal fun followsKindOfOrNull(id: String): ProfileFollowsKind? =
    ProfileFollowsKind.entries.firstOrNull { it.name == id }

/** Items per page of the profile's paged lists (follows, reviews). */
internal const val ProfileListPageSize = 25

package com.luum.michi.app.profile.repository

import com.luum.michi.app.core.media.repository.bestTitle
import com.luum.michi.app.core.media.repository.toCoverSizes
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.profile.domain.model.ProfileFollow
import com.luum.michi.app.profile.domain.model.ProfileReview

/** AniList avatars ship only `large`/`medium`. */
internal fun ProfileAvatarDto?.toCoverSizes(): CoverSizes? {
    if (this == null || (large == null && medium == null)) return null
    return CoverSizes(medium = medium, large = large, extraLarge = null)
}

internal fun ProfileUserDto.toProfileFollow(): ProfileFollow = ProfileFollow(
    id = id,
    name = name,
    avatar = avatar.toCoverSizes(),
)

internal fun ProfileReviewDto.toProfileReview(): ProfileReview = ProfileReview(
    id = id,
    mediaId = media?.id ?: 0,
    mediaTitle = media?.title.bestTitle(),
    mediaCover = media?.coverImage?.toCoverSizes(),
    paletteHex = media?.coverImage?.color,
    summary = summary.orEmpty(),
    score = score,
)

package com.luum.michi.app.profile.repository

import com.luum.michi.app.core.media.domain.catalogMetaLine
import com.luum.michi.app.core.media.domain.toTitleCase
import com.luum.michi.app.core.media.repository.bestTitle
import com.luum.michi.app.core.media.repository.bestTitleOrNull
import com.luum.michi.app.core.media.repository.dto.MediaDto
import com.luum.michi.app.core.media.repository.toCoverSizes
import com.luum.michi.app.core.person.repository.dto.StudioDto
import com.luum.michi.app.core.person.repository.toCoverSizes
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites

internal fun FavouritesDto.toDomain(): ProfileFavorites = ProfileFavorites(
    anime = anime?.nodes?.map(MediaDto::toProfileFavorite).orEmpty(),
    manga = manga?.nodes?.map(MediaDto::toProfileFavorite).orEmpty(),
    characters = characters?.nodes?.map(FavoriteCharacterDto::toProfileFavorite).orEmpty(),
    staff = staff?.nodes?.map(FavoriteStaffDto::toProfileFavorite).orEmpty(),
    studios = studios?.nodes?.map(StudioDto::toProfileFavorite).orEmpty(),
)

internal fun MediaDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = title.bestTitle(),
    image = coverImage?.toCoverSizes(),
    paletteHex = coverImage?.color,
    subtitle = catalogMetaLine(
        format = format?.toTitleCase(),
        season = null,
        year = seasonYear ?: startDate?.year,
        status = null,
    ).ifBlank { null },
)

internal fun FavoriteCharacterDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = name?.bestName.orEmpty(),
    image = image?.toCoverSizes(),
    paletteHex = null,
    subtitle = media?.nodes?.firstOrNull()?.title.bestTitleOrNull()?.takeIf { it.isNotBlank() },
)

internal fun FavoriteStaffDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = name?.bestName.orEmpty(),
    image = image?.toCoverSizes(),
    paletteHex = null,
    subtitle = primaryOccupations?.firstOrNull { !it.isNullOrBlank() },
)

/** A studio has no image on AniList: it always maps to a text-only favourite. */
internal fun StudioDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = name,
    image = null,
    paletteHex = null,
)

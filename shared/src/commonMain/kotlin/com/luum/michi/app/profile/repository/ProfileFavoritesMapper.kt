package com.luum.michi.app.profile.repository

import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.core.person.repository.dto.CharacterDto
import com.luum.michi.app.core.media.repository.dto.MediaDto
import com.luum.michi.app.core.person.repository.dto.StaffDto
import com.luum.michi.app.core.media.repository.bestTitle
import com.luum.michi.app.core.person.repository.dto.StudioDto
import com.luum.michi.app.core.media.repository.toCoverSizes
import com.luum.michi.app.core.person.repository.toCoverSizes

internal fun FavouritesDto.toDomain(): ProfileFavorites = ProfileFavorites(
    anime = anime?.nodes?.map(MediaDto::toProfileFavorite).orEmpty(),
    manga = manga?.nodes?.map(MediaDto::toProfileFavorite).orEmpty(),
    characters = characters?.nodes?.map(CharacterDto::toProfileFavorite).orEmpty(),
    staff = staff?.nodes?.map(StaffDto::toProfileFavorite).orEmpty(),
    studios = studios?.nodes?.map(StudioDto::toProfileFavorite).orEmpty(),
)

internal fun MediaDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = title.bestTitle(),
    image = coverImage?.toCoverSizes(),
    paletteHex = coverImage?.color,
)

internal fun CharacterDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = name?.bestName.orEmpty(),
    image = image?.toCoverSizes(),
    paletteHex = null,
)

internal fun StaffDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = name?.bestName.orEmpty(),
    image = image?.toCoverSizes(),
    paletteHex = null,
)

/** A studio has no image on AniList: it always maps to a text-only favourite. */
internal fun StudioDto.toProfileFavorite(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = name,
    image = null,
    paletteHex = null,
)

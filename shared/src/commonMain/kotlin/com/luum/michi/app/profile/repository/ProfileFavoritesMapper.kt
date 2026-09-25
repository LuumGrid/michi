package com.luum.michi.app.profile.repository

import com.luum.michi.app.profile.domain.model.ProfileFavoriteMedia
import com.luum.michi.app.profile.domain.model.ProfileFavoritePerson
import com.luum.michi.app.profile.domain.model.ProfileFavoriteStudio
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.core.network.repository.dto.CharacterDto
import com.luum.michi.app.core.network.repository.dto.FavouritesDto
import com.luum.michi.app.core.network.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.dto.StaffDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.network.repository.dto.StudioDto

internal fun FavouritesDto.toDomain(): ProfileFavorites = ProfileFavorites(
    anime = anime?.nodes?.map(MediaDto::toProfileFavoriteMedia).orEmpty(),
    manga = manga?.nodes?.map(MediaDto::toProfileFavoriteMedia).orEmpty(),
    characters = characters?.nodes?.map(CharacterDto::toProfileFavoritePerson).orEmpty(),
    staff = staff?.nodes?.map(StaffDto::toProfileFavoritePerson).orEmpty(),
    studios = studios?.nodes?.map(StudioDto::toProfileFavoriteStudio).orEmpty(),
)

internal fun MediaDto.toProfileFavoriteMedia(): ProfileFavoriteMedia = ProfileFavoriteMedia(
    id = id,
    title = title.bestTitle(),
    coverUrl = coverImage?.thumbnailUrl,
    paletteHex = coverImage?.color,
)

internal fun CharacterDto.toProfileFavoritePerson(): ProfileFavoritePerson = ProfileFavoritePerson(
    id = id,
    name = name?.bestName.orEmpty(),
    imageUrl = image?.bestUrl,
    paletteHex = null,
)

internal fun StaffDto.toProfileFavoritePerson(): ProfileFavoritePerson = ProfileFavoritePerson(
    id = id,
    name = name?.bestName.orEmpty(),
    imageUrl = image?.bestUrl,
    paletteHex = null,
)

internal fun StudioDto.toProfileFavoriteStudio(): ProfileFavoriteStudio = ProfileFavoriteStudio(
    id = id,
    name = name,
    coverUrl = latestCoverImage?.thumbnailUrl,
    paletteHex = latestCoverImage?.color,
)

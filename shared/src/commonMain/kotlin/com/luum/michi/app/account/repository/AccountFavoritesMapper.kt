package com.luum.michi.app.account.repository

import com.luum.michi.app.account.domain.model.AccountFavoriteMedia
import com.luum.michi.app.account.domain.model.AccountFavoritePerson
import com.luum.michi.app.account.domain.model.AccountFavoriteStudio
import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.core.repository.anilist.dto.CharacterDto
import com.luum.michi.app.core.repository.anilist.dto.FavouritesDto
import com.luum.michi.app.core.repository.anilist.dto.MediaDto
import com.luum.michi.app.core.repository.anilist.dto.StaffDto
import com.luum.michi.app.core.repository.anilist.dto.bestTitle
import com.luum.michi.app.core.repository.anilist.dto.StudioDto

internal fun FavouritesDto.toDomain(): AccountFavorites = AccountFavorites(
    anime = anime?.nodes?.map(MediaDto::toAccountFavoriteMedia).orEmpty(),
    manga = manga?.nodes?.map(MediaDto::toAccountFavoriteMedia).orEmpty(),
    characters = characters?.nodes?.map(CharacterDto::toAccountFavoritePerson).orEmpty(),
    staff = staff?.nodes?.map(StaffDto::toAccountFavoritePerson).orEmpty(),
    studios = studios?.nodes?.map(StudioDto::toAccountFavoriteStudio).orEmpty(),
)

internal fun MediaDto.toAccountFavoriteMedia(): AccountFavoriteMedia = AccountFavoriteMedia(
    id = id,
    title = title.bestTitle(),
    coverUrl = coverImage?.thumbnailUrl,
    paletteHex = coverImage?.color,
)

internal fun CharacterDto.toAccountFavoritePerson(): AccountFavoritePerson = AccountFavoritePerson(
    id = id,
    name = name?.bestName.orEmpty(),
    imageUrl = image?.bestUrl,
    paletteHex = null,
)

internal fun StaffDto.toAccountFavoritePerson(): AccountFavoritePerson = AccountFavoritePerson(
    id = id,
    name = name?.bestName.orEmpty(),
    imageUrl = image?.bestUrl,
    paletteHex = null,
)

internal fun StudioDto.toAccountFavoriteStudio(): AccountFavoriteStudio = AccountFavoriteStudio(
    id = id,
    name = name,
    coverUrl = latestCoverImage?.thumbnailUrl,
    paletteHex = latestCoverImage?.color,
)

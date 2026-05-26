package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountFavoriteMedia
import com.luum.michi.app.account.presentation.model.AccountFavoritePerson
import com.luum.michi.app.account.presentation.model.AccountFavoriteStudio
import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.core.anilist.dto.CharacterDto
import com.luum.michi.app.core.anilist.dto.FavouritesDto
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.anilist.dto.StaffDto
import com.luum.michi.app.core.anilist.dto.StudioDto
import com.luum.michi.app.core.platform.hexToPalette

internal fun FavouritesDto.toDomain(): AccountFavorites = AccountFavorites(
    anime = anime?.nodes?.map(MediaDto::toAccountFavoriteMedia).orEmpty(),
    manga = manga?.nodes?.map(MediaDto::toAccountFavoriteMedia).orEmpty(),
    characters = characters?.nodes?.map(CharacterDto::toAccountFavoritePerson).orEmpty(),
    staff = staff?.nodes?.map(StaffDto::toAccountFavoritePerson).orEmpty(),
    studios = studios?.nodes?.map(StudioDto::toAccountFavoriteStudio).orEmpty(),
)

private fun MediaDto.toAccountFavoriteMedia(): AccountFavoriteMedia = AccountFavoriteMedia(
    id = id,
    title = title.bestTitle(),
    coverUrl = coverImage?.thumbnailUrl,
    palette = hexToPalette(coverImage?.color),
)

private fun CharacterDto.toAccountFavoritePerson(): AccountFavoritePerson = AccountFavoritePerson(
    id = id,
    name = name?.bestName.orEmpty(),
    imageUrl = image?.bestUrl,
    palette = hexToPalette(null),
)

private fun StaffDto.toAccountFavoritePerson(): AccountFavoritePerson = AccountFavoritePerson(
    id = id,
    name = name?.bestName.orEmpty(),
    imageUrl = image?.bestUrl,
    palette = hexToPalette(null),
)

private fun StudioDto.toAccountFavoriteStudio(): AccountFavoriteStudio = AccountFavoriteStudio(
    id = id,
    name = name,
    coverUrl = latestCoverImage?.thumbnailUrl,
    palette = hexToPalette(latestCoverImage?.color),
)

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

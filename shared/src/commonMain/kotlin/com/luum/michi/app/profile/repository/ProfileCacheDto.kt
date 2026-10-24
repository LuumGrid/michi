package com.luum.michi.app.profile.repository

import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileMediaTypeStats
import com.luum.michi.app.profile.domain.model.ProfileStatDistributionEntry
import com.luum.michi.app.profile.domain.model.ProfileStats
import kotlinx.serialization.Serializable

/**
 * On-disk shape of the viewer's profile. It mirrors the whole [ProfileData] the
 * network returns (counts, detailed distributions and every favourites bucket),
 * so reading the cache never produces a partial profile: the stats surfaces that
 * land later (7.4) paint straight from disk instead of waiting for the network.
 */
@Serializable
internal data class ProfileCacheDto(
    val stats: ProfileStatsCacheDto,
    val favorites: ProfileFavoritesCacheDto,
)

@Serializable
internal data class ProfileStatsCacheDto(
    val animeCount: Int,
    val mangaCount: Int,
    val followingCount: Int,
    val followersCount: Int,
    val anime: ProfileMediaTypeStatsCacheDto = ProfileMediaTypeStatsCacheDto(),
    val manga: ProfileMediaTypeStatsCacheDto = ProfileMediaTypeStatsCacheDto(),
)

@Serializable
internal data class ProfileMediaTypeStatsCacheDto(
    val count: Int = 0,
    val episodesOrChapters: Int = 0,
    val daysOrVolumes: Double = 0.0,
    val meanScore: Double = 0.0,
    val standardDeviation: Double = 0.0,
    val scoreDistribution: List<ProfileDistributionEntryCacheDto> = emptyList(),
    val formatDistribution: List<ProfileDistributionEntryCacheDto> = emptyList(),
    val statusDistribution: List<ProfileDistributionEntryCacheDto> = emptyList(),
    val topGenres: List<ProfileDistributionEntryCacheDto> = emptyList(),
)

@Serializable
internal data class ProfileDistributionEntryCacheDto(
    val label: String,
    val count: Int,
)

@Serializable
internal data class ProfileFavoritesCacheDto(
    val anime: List<ProfileFavoriteCacheDto> = emptyList(),
    val manga: List<ProfileFavoriteCacheDto> = emptyList(),
    val characters: List<ProfileFavoriteCacheDto> = emptyList(),
    val staff: List<ProfileFavoriteCacheDto> = emptyList(),
    val studios: List<ProfileFavoriteCacheDto> = emptyList(),
)

/** One favourite: [medium]/[large]/[extraLarge] carry the cover variants, all
 *  null for a studio (AniList has no image for them). [subtitle] is the card's
 *  second line. */
@Serializable
internal data class ProfileFavoriteCacheDto(
    val id: Int,
    val name: String,
    val medium: String? = null,
    val large: String? = null,
    val extraLarge: String? = null,
    val paletteHex: String? = null,
    val subtitle: String? = null,
)

internal fun ProfileData.toCacheDto(): ProfileCacheDto = ProfileCacheDto(
    stats = stats.toCacheDto(),
    favorites = favorites.toCacheDto(),
)

internal fun ProfileCacheDto.toDomain(): ProfileData = ProfileData(
    stats = stats.toDomain(),
    favorites = favorites.toDomain(),
)

private fun ProfileStats.toCacheDto(): ProfileStatsCacheDto = ProfileStatsCacheDto(
    animeCount = animeCount,
    mangaCount = mangaCount,
    followingCount = followingCount,
    followersCount = followersCount,
    anime = anime.toCacheDto(),
    manga = manga.toCacheDto(),
)

private fun ProfileStatsCacheDto.toDomain(): ProfileStats = ProfileStats(
    animeCount = animeCount,
    mangaCount = mangaCount,
    followingCount = followingCount,
    followersCount = followersCount,
    anime = anime.toDomain(),
    manga = manga.toDomain(),
)

private fun ProfileMediaTypeStats.toCacheDto(): ProfileMediaTypeStatsCacheDto = ProfileMediaTypeStatsCacheDto(
    count = count,
    episodesOrChapters = episodesOrChapters,
    daysOrVolumes = daysOrVolumes,
    meanScore = meanScore,
    standardDeviation = standardDeviation,
    scoreDistribution = scoreDistribution.map(ProfileStatDistributionEntry::toCacheDto),
    formatDistribution = formatDistribution.map(ProfileStatDistributionEntry::toCacheDto),
    statusDistribution = statusDistribution.map(ProfileStatDistributionEntry::toCacheDto),
    topGenres = topGenres.map(ProfileStatDistributionEntry::toCacheDto),
)

private fun ProfileMediaTypeStatsCacheDto.toDomain(): ProfileMediaTypeStats = ProfileMediaTypeStats(
    count = count,
    episodesOrChapters = episodesOrChapters,
    daysOrVolumes = daysOrVolumes,
    meanScore = meanScore,
    standardDeviation = standardDeviation,
    scoreDistribution = scoreDistribution.map(ProfileDistributionEntryCacheDto::toDomain),
    formatDistribution = formatDistribution.map(ProfileDistributionEntryCacheDto::toDomain),
    statusDistribution = statusDistribution.map(ProfileDistributionEntryCacheDto::toDomain),
    topGenres = topGenres.map(ProfileDistributionEntryCacheDto::toDomain),
)

private fun ProfileStatDistributionEntry.toCacheDto(): ProfileDistributionEntryCacheDto =
    ProfileDistributionEntryCacheDto(label = label, count = count)

private fun ProfileDistributionEntryCacheDto.toDomain(): ProfileStatDistributionEntry =
    ProfileStatDistributionEntry(label = label, count = count)

private fun ProfileFavorites.toCacheDto(): ProfileFavoritesCacheDto = ProfileFavoritesCacheDto(
    anime = anime.map(ProfileFavorite::toCacheDto),
    manga = manga.map(ProfileFavorite::toCacheDto),
    characters = characters.map(ProfileFavorite::toCacheDto),
    staff = staff.map(ProfileFavorite::toCacheDto),
    studios = studios.map(ProfileFavorite::toCacheDto),
)

private fun ProfileFavoritesCacheDto.toDomain(): ProfileFavorites = ProfileFavorites(
    anime = anime.map(ProfileFavoriteCacheDto::toDomain),
    manga = manga.map(ProfileFavoriteCacheDto::toDomain),
    characters = characters.map(ProfileFavoriteCacheDto::toDomain),
    staff = staff.map(ProfileFavoriteCacheDto::toDomain),
    studios = studios.map(ProfileFavoriteCacheDto::toDomain),
)

private fun ProfileFavorite.toCacheDto(): ProfileFavoriteCacheDto = ProfileFavoriteCacheDto(
    id = id,
    name = name,
    medium = image?.medium,
    large = image?.large,
    extraLarge = image?.extraLarge,
    paletteHex = paletteHex,
    subtitle = subtitle,
)

private fun ProfileFavoriteCacheDto.toDomain(): ProfileFavorite = ProfileFavorite(
    id = id,
    name = name,
    image = if (medium == null && large == null && extraLarge == null) {
        null
    } else {
        CoverSizes(medium = medium, large = large, extraLarge = extraLarge)
    },
    paletteHex = paletteHex,
    subtitle = subtitle,
)

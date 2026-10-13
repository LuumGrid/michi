package com.luum.michi.app.mediaDetail.domain.character.model
import com.luum.michi.app.core.model.CoverSizes



internal data class CharacterDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val image: CoverSizes?,
    val descriptionPlain: String,
    val gender: String?,
    val age: String?,
    val bloodType: String?,
    val birthday: String?,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: CharacterMediaPage,
)

internal data class CharacterMediaItem(
    val mediaId: Int,
    val title: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
    val role: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val viewerStatus: String?,
    val voiceActorName: String?,
    val voiceActorImage: CoverSizes?,
    val voiceActorLanguage: String?,
    val voiceActorId: Int?,
)

internal data class CharacterMediaPage(
    val items: List<CharacterMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

/** Canonical AniList page for this character. */
internal fun CharacterDetail.aniListUrl(): String = "https://anilist.co/character/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun CharacterDetail.shareText(): String = "$name\n${aniListUrl()}"

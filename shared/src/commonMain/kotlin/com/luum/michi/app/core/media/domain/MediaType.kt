package com.luum.michi.app.core.media.domain

/**
 * Which catalog a media entry belongs to. Lives in `core` because it crosses a
 * feature seam: a navigation destination carries it, and `core/navigation` may
 * not import a feature model. Only the two catalogs AniList exposes as `type`;
 * a media with an unknown type has no genre page (the caller drops to null).
 */
internal enum class MediaType { ANIME, MANGA }

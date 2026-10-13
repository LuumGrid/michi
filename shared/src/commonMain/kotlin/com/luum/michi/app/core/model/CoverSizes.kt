package com.luum.michi.app.core.model

/**
 * AniList cover variants: `medium` (~100px), `large` (~230px) and
 * `extraLarge` (~460px). People images (character/staff) only ship
 * `large`/`medium`, so `extraLarge` is nullable and callers fall back.
 * The UI picks a variant by the user's image-quality setting.
 */
internal data class CoverSizes(
    val medium: String? = null,
    val large: String? = null,
    val extraLarge: String? = null,
)

/**
 * URL for [quality]. AUTO must be resolved first (`ImageQuality.resolved`);
 * it falls back to the top size here only defensively. Each level falls back
 * across the available sizes so people images (no extraLarge) still render.
 */
internal fun CoverSizes?.forQuality(quality: ImageQuality): String? = when (quality) {
    ImageQuality.HIGH, ImageQuality.AUTO -> this?.extraLarge ?: this?.large ?: this?.medium
    ImageQuality.LOW -> this?.large ?: this?.medium ?: this?.extraLarge
}

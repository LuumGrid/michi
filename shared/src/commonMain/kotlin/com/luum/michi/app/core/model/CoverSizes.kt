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

/** AniList cover widths (px): medium ≈100, large ≈230, extraLarge ≈460. */
private const val MediumWidthPx = 100
private const val LargeWidthPx = 230

/**
 * URL for a cover drawn at [widthPx]: the smallest variant that still covers
 * it (so small rails fetch `medium`, the hero fetches `extraLarge`). [quality]
 * `LOW` steps one tier down; missing variants fall back across the sizes (so
 * people images without `extraLarge` still render).
 */
internal fun CoverSizes?.forWidthPx(widthPx: Int, quality: ImageQuality): String? {
    val tier = when {
        widthPx <= MediumWidthPx -> 0
        widthPx <= LargeWidthPx -> 1
        else -> 2
    }
    val adjusted = if (quality == ImageQuality.LOW) (tier - 1).coerceAtLeast(0) else tier
    return when (adjusted) {
        0 -> this?.medium ?: this?.large ?: this?.extraLarge
        1 -> this?.large ?: this?.extraLarge ?: this?.medium
        else -> this?.extraLarge ?: this?.large ?: this?.medium
    }
}

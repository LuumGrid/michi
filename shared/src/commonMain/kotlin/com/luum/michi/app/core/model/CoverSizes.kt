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
) {
    /** Highest resolution available (AniList's cover ceiling). */
    val bestUrl: String? get() = extraLarge ?: large ?: medium
}

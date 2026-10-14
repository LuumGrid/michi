package com.luum.michi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.model.ImageQuality
import com.luum.michi.app.core.model.forWidthPx

/**
 * Resolved image quality for cover rendering (AUTO already mapped to
 * HIGH/LOW by `App`). Defaults to HIGH so previews/tests render sharp.
 */
internal val LocalImageQuality = compositionLocalOf { ImageQuality.HIGH }

/**
 * Cover URL for a surface of [widthDp]: converts to px with the current density
 * and picks the smallest AniList variant that still covers it (via
 * [forWidthPx]). Large surfaces (hero, featured 1.5× rails) naturally land on
 * `extraLarge`.
 */
@Composable
internal fun rememberCoverUrl(cover: CoverSizes?, widthDp: Dp): String? {
    val density = LocalDensity.current
    val quality = LocalImageQuality.current
    return cover.forWidthPx(widthPx = with(density) { widthDp.roundToPx() }, quality = quality)
}

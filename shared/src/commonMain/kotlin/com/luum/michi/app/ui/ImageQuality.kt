package com.luum.michi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.model.ImageQuality
import com.luum.michi.app.core.model.forWidthPx
import com.luum.michi.app.ui.components.CoverSize

/**
 * Resolved image quality for cover rendering (AUTO already mapped to
 * HIGH/LOW by `App`). Defaults to HIGH so previews/tests render sharp.
 */
internal val LocalImageQuality = compositionLocalOf { ImageQuality.HIGH }

/**
 * Cover URL for a surface drawn at the standard [size]: the token carries both
 * dimensions, so a surface picks a size instead of a pixel width and the
 * variant (medium/large/extraLarge) always matches the rendered poster.
 */
@Composable
internal fun rememberCoverUrl(cover: CoverSizes?, size: CoverSize): String? {
    val density = LocalDensity.current
    val quality = LocalImageQuality.current
    return cover.forWidthPx(widthPx = with(density) { size.width.roundToPx() }, quality = quality)
}

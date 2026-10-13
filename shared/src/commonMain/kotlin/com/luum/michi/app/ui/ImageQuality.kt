package com.luum.michi.app.ui

import androidx.compose.runtime.compositionLocalOf
import com.luum.michi.app.core.model.ImageQuality

/**
 * Resolved image quality for cover rendering (AUTO already mapped to
 * HIGH/LOW by `App`). Defaults to HIGH so previews/tests render sharp.
 */
internal val LocalImageQuality = compositionLocalOf { ImageQuality.HIGH }

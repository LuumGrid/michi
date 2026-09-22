package com.luum.michi.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

/**
 * Warning orange, generated — never hand-tuned. Sync failures (transient,
 * nothing blocked) read orange; destructive confirms (sign out) keep the
 * scheme's `error` red.
 *
 * The only literal is the seed; the role below is produced by MaterialKolor
 * (light + dark variants automatic), so warnings stay coherent with the
 * theme in both modes — same contract as the palette seeds.
 */
internal val WarningSeed = Color(0xFFFB8C00) // Material Orange 600

/**
 * Single warning role (border/icon/text). If a filled warning is ever needed,
 * its container roles come from the same generated scheme.
 */
internal val LocalWarning = compositionLocalOf { WarningSeed }

internal fun warningScheme(dark: Boolean) =
    dynamicColorScheme(seedColor = WarningSeed, isDark = dark)

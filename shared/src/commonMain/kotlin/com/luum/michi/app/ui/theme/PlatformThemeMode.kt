package com.luum.michi.app.ui.theme

import androidx.compose.runtime.Composable

/**
 * Applies the persisted theme choice to the platform's per-app night mode.
 *
 * Android uses this to make the starting window (splash + window background)
 * follow the app setting, not only the system setting. iOS has no equivalent
 * hook here, so it is a no-op.
 */
@Composable
internal expect fun PlatformThemeModeEffect(type: ThemeType)

package com.luum.michi.app.ui.theme

import android.app.UiModeManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * `setApplicationNightMode` (API 31+) persists the choice per app, so the next
 * starting window resolves values/ vs values-night/ with the app's mode before
 * Compose runs. minSdk is 34, so no version guard is needed.
 */
@Composable
internal actual fun PlatformThemeModeEffect(type: ThemeType) {
    val context = LocalContext.current
    LaunchedEffect(type) {
        context.getSystemService(UiModeManager::class.java)
            .setApplicationNightMode(
                when (type) {
                    ThemeType.LIGHT -> UiModeManager.MODE_NIGHT_NO
                    ThemeType.DARK -> UiModeManager.MODE_NIGHT_YES
                    ThemeType.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                },
            )
    }
}

package com.luum.michi.app.core.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSystemBackHandler(
    enabled: Boolean,
    onBack: PlatformBackHandler,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}

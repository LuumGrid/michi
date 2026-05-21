package com.luum.michi.app.core.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSystemBackHandler(
    enabled: Boolean,
    onBack: PlatformBackHandler,
) = Unit

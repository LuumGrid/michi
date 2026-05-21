package com.luum.michi.app.core.platform

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSystemBackHandler(
    enabled: Boolean,
    onBack: PlatformBackHandler,
)

package com.luum.michi.app.ui

import androidx.activity.compose.BackHandler as ActivityBackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: BackHandler,
) {
    ActivityBackHandler(enabled = enabled, onBack = onBack)
}

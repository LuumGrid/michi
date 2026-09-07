package com.luum.michi.app.ui

import androidx.compose.runtime.Composable

@Composable
expect fun SystemBackHandler(
    enabled: Boolean,
    onBack: BackHandler,
)

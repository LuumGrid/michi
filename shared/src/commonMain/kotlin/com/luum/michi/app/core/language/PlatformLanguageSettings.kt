package com.luum.michi.app.core.language

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPlatformLanguageSettingsLauncher(): (() -> Unit)?

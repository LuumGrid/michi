package com.luum.michi.app.core.platform

import androidx.compose.ui.platform.Clipboard

expect suspend fun Clipboard.setPlainText(text: String)

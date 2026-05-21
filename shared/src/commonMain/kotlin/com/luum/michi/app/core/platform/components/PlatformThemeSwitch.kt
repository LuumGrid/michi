package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PlatformThemeSwitch(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = isDarkMode,
        onCheckedChange = { onToggleTheme() },
        thumbContent = {
            Text(
                text = if (isDarkMode) "🌙" else "☀",
                fontSize = 12.sp,
            )
        },
        modifier = modifier.padding(end = 12.dp),
    )
}

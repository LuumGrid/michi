package com.luum.michi.app.settings.presentation.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import com.luum.michi.app.settings.presentation.model.SettingsItem

@Composable
internal fun SettingsToggleRow(
    item: SettingsItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        item = item,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

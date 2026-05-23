package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider

@Composable
internal fun AccountVisibilityField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPublic: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val strings = LanguageProvider.strings

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountEditField(value = value, onValueChange = onValueChange, label = label)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPublic) strings.accountVisibilityPublic else strings.accountVisibilityPrivate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = strings.accountVisibilitySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = isPublic, onCheckedChange = onVisibilityChange)
        }
    }
}

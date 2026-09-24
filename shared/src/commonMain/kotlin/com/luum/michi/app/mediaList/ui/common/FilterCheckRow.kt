package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Multi-select row for filter sheets (genres, formats): same IG-style
 * visuals as [com.luum.michi.app.ui.components.OptionRow] but checkbox
 * semantics, since several rows can be on at once. The checkbox is
 * display-only (null onCheckedChange); the whole row toggles.
 */
@Composable
internal fun FilterCheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (divider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = checked, onClick = onToggle, role = Role.Checkbox)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        }
    }
}

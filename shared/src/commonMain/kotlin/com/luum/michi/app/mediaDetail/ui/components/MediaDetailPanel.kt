package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.PanelShape
import com.luum.michi.app.ui.components.flatBorder

/**
 * Framed panel shared by the detail data blocks — the information sections
 * (media info, character info, staff info), the link rows — and the prose
 * sections (media synopsis, character/staff biographies). Flat, like the list
 * cards: `surface` fill + `outlineVariant` border, never glass — the panel marks
 * a data block, it does not float. A prose section's [SpoilerBlock] passes
 * `bordered = false` so it does not box a panel inside this one.
 *
 * Callers own the `ColumnScope` content.
 */
@Composable
internal fun MediaDetailPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Surface(
            shape = PanelShape,
            color = MaterialTheme.colorScheme.surface,
            border = flatBorder(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content,
            )
        }
    }
}

package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.glassContainerColor
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.components.solidContainerColor
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

/**
 * Framed panel shared by the detail information sections (media info,
 * character info, staff info). Style-aware: same glass/solid recipe the old
 * media information panel inlined, extracted so every detail scope speaks
 * the same surface language. Callers own the `ColumnScope` content.
 */
@Composable
internal fun MediaDetailPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (glass) glassContainerColor() else solidContainerColor(),
            border = if (glass) glassBorder() else solidBorder(),
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

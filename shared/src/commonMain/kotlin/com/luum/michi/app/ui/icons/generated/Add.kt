package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.Add: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Add",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(440f, 520f)
            lineTo(200f, 520f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(240f)
            verticalLineToRelative(-240f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(240f)
            horizontalLineToRelative(240f)
            verticalLineToRelative(80f)
            lineTo(520f, 520f)
            verticalLineToRelative(240f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(-240f)
            close()
        }
    }.build()
}

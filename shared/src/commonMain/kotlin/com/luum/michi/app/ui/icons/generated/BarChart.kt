package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.BarChart: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "BarChart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(160f, 800f)
            verticalLineToRelative(-280f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(280f)
            horizontalLineTo(160f)
            close()
            moveToRelative(240f, 0f)
            verticalLineToRelative(-640f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(640f)
            horizontalLineTo(400f)
            close()
            moveToRelative(240f, 0f)
            verticalLineToRelative(-400f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(400f)
            horizontalLineTo(640f)
            close()
        }
    }.build()
}

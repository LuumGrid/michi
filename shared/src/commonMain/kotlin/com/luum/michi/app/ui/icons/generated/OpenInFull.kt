package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.OpenInFull: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "OpenInFull",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(120f, 840f)
            verticalLineToRelative(-320f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(184f)
            lineToRelative(504f, -504f)
            lineTo(520f, 200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(320f)
            verticalLineToRelative(320f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(-184f)
            lineTo(256f, 760f)
            horizontalLineToRelative(184f)
            verticalLineToRelative(80f)
            lineTo(120f, 840f)
            close()
        }
    }.build()
}

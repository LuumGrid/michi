package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.CloseFullscreen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "CloseFullscreen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveToRelative(136f, 880f)
            lineToRelative(-56f, -56f)
            lineToRelative(264f, -264f)
            lineTo(160f, 560f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(320f)
            verticalLineToRelative(320f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(-184f)
            lineTo(136f, 880f)
            close()
            moveTo(480f, 480f)
            verticalLineToRelative(-320f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(184f)
            lineToRelative(264f, -264f)
            lineToRelative(56f, 56f)
            lineToRelative(-264f, 264f)
            horizontalLineToRelative(184f)
            verticalLineToRelative(80f)
            lineTo(480f, 480f)
            close()
        }
    }.build()
}

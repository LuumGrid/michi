package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.ExpandMoreRounded: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "ExpandMoreRounded",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(480f, 599f)
            quadToRelative(-8f, 0f, -15f, -2.5f)
            reflectiveQuadToRelative(-13f, -8.5f)
            lineTo(268f, 404f)
            quadToRelative(-11f, -11f, -11f, -28f)
            reflectiveQuadToRelative(11f, -28f)
            quadToRelative(11f, -11f, 28f, -11f)
            reflectiveQuadToRelative(28f, 11f)
            lineToRelative(156f, 156f)
            lineToRelative(156f, -156f)
            quadToRelative(11f, -11f, 28f, -11f)
            reflectiveQuadToRelative(28f, 11f)
            quadToRelative(11f, 11f, 11f, 28f)
            reflectiveQuadToRelative(-11f, 28f)
            lineTo(508f, 588f)
            quadToRelative(-6f, 6f, -13f, 8.5f)
            reflectiveQuadToRelative(-15f, 2.5f)
            close()
        }
    }.build()
}

package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.Forward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Forward",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveToRelative(640f, 680f)
            lineToRelative(-57f, -56f)
            lineToRelative(184f, -184f)
            lineToRelative(-184f, -184f)
            lineToRelative(57f, -56f)
            lineToRelative(240f, 240f)
            lineToRelative(-240f, 240f)
            close()
            moveTo(80f, 760f)
            verticalLineToRelative(-160f)
            quadToRelative(0f, -83f, 58.5f, -141.5f)
            reflectiveQuadTo(280f, 400f)
            horizontalLineToRelative(247f)
            lineTo(383f, 256f)
            lineToRelative(57f, -56f)
            lineToRelative(240f, 240f)
            lineToRelative(-240f, 240f)
            lineToRelative(-57f, -56f)
            lineToRelative(144f, -144f)
            lineTo(280f, 480f)
            quadToRelative(-50f, 0f, -85f, 35f)
            reflectiveQuadToRelative(-35f, 85f)
            verticalLineToRelative(160f)
            lineTo(80f, 760f)
            close()
        }
    }.build()
}

package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.ArrowBack: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "ArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveToRelative(313f, 520f)
            lineToRelative(224f, 224f)
            lineToRelative(-57f, 56f)
            lineToRelative(-320f, -320f)
            lineToRelative(320f, -320f)
            lineToRelative(57f, 56f)
            lineToRelative(-224f, 224f)
            horizontalLineToRelative(487f)
            verticalLineToRelative(80f)
            lineTo(313f, 520f)
            close()
        }
    }.build()
}

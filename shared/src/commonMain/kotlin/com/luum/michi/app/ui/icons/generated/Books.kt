package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.Books: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Books",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(80f, 800f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(800f)
            verticalLineToRelative(80f)
            lineTo(80f, 800f)
            close()
            moveTo(160f, 640f)
            verticalLineToRelative(-320f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(320f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(320f, 640f)
            verticalLineToRelative(-480f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(480f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(480f, 640f)
            verticalLineToRelative(-480f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(480f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(760f, 640f)
            lineTo(600f, 360f)
            lineToRelative(70f, -40f)
            lineToRelative(160f, 280f)
            lineToRelative(-70f, 40f)
            close()
        }
    }.build()
}

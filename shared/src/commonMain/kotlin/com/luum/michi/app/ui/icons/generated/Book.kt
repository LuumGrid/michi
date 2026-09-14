package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.Book: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Book",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(240f, 880f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(160f, 800f)
            verticalLineToRelative(-640f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(240f, 80f)
            horizontalLineToRelative(480f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(800f, 160f)
            verticalLineToRelative(640f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(720f, 880f)
            lineTo(240f, 880f)
            close()
            moveTo(240f, 800f)
            horizontalLineToRelative(480f)
            verticalLineToRelative(-640f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(280f)
            lineToRelative(-100f, -60f)
            lineToRelative(-100f, 60f)
            verticalLineToRelative(-280f)
            lineTo(240f, 160f)
            verticalLineToRelative(640f)
            close()
            moveTo(240f, 800f)
            verticalLineToRelative(-640f)
            verticalLineToRelative(640f)
            close()
            moveTo(440f, 440f)
            lineTo(540f, 380f)
            lineTo(640f, 440f)
            lineTo(540f, 380f)
            lineTo(440f, 440f)
            close()
        }
    }.build()
}

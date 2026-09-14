package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.SeasonView: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "SeasonView",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(200f, 680f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(120f, 600f)
            verticalLineToRelative(-240f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(200f, 280f)
            horizontalLineToRelative(560f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(840f, 360f)
            verticalLineToRelative(240f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(760f, 680f)
            lineTo(200f, 680f)
            close()
            moveTo(200f, 600f)
            horizontalLineToRelative(560f)
            verticalLineToRelative(-240f)
            lineTo(200f, 360f)
            verticalLineToRelative(240f)
            close()
            moveTo(120f, 200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(720f)
            verticalLineToRelative(80f)
            lineTo(120f, 200f)
            close()
            moveTo(120f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(720f)
            verticalLineToRelative(80f)
            lineTo(120f, 840f)
            close()
            moveTo(200f, 360f)
            verticalLineToRelative(240f)
            verticalLineToRelative(-240f)
            close()
        }
    }.build()
}

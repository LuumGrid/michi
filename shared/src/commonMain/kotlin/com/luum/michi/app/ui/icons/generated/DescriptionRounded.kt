package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

val IconPack.DescriptionRounded: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "DescriptionRounded",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M360,720h240q17,0,28.5,-11.5T640,680q0,-17,-11.5,-28.5T600,640H360q-17,0,-28.5,11.5T320,680q0,17,11.5,28.5T360,720Zm0,-160h240q17,0,28.5,-11.5T640,520q0,-17,-11.5,-28.5T600,480H360q-17,0,-28.5,11.5T320,520q0,17,11.5,28.5T360,560ZM240,880q-33,0,-56.5,-23.5T160,800v-640q0,-33,23.5,-56.5T240,80h287q16,0,30.5,6t25.5,17l194,194q11,11,17,25.5t6,30.5v447q0,33,-23.5,56.5T720,880H240Zm280,-560v-160H240v640h480v-440H560q-17,0,-28.5,-11.5T520,320ZM240,160v200 -200 640 -640Z",
            ),
            fill = SolidColor(Color(0xFFE3E3E3)),
        )
    }.build()
}

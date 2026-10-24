package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

val IconPack.ImageRounded: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "ImageRounded",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M200,840q-33,0 -56.5,-23.5T120,760v-560q0,-33 23.5,-56.5T200,120h560q33,0 " +
                    "56.5,23.5T840,200v560q0,33 -23.5,56.5T760,840H200Zm0,-80h560v-560H200v560Zm80,-80h400q12,0 " +
                    "18,-11t-2,-21L586,501q-6,-8 -16,-8t-16,8L450,640l-74,-99q-6,-8 -16,-8t-16,8l-80,107q-8,10 " +
                    "-2,21t18,11Zm-80,80v-560 560Z",
            ),
            fill = SolidColor(Color(0xFFE3E3E3)),
        )
    }.build()
}

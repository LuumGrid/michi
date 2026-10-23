package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

val IconPack.PlayRounded: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "PlayRounded",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M320,687v-414q0,-17,12,-28.5t28,-11.5q5,0,10.5,1.5T381,239l326,207q9,6,13.5,15t4.5,19q0,10,-4.5,19T707,514L381,721q-5,3,-10.5,4.5T360,727q-16,0,-28,-11.5T320,687Zm80,-207Zm0,134 210,-134 -210,-134v268Z",
            ),
            fill = SolidColor(Color(0xFFE3E3E3)),
        )
    }.build()
}

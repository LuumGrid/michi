package com.luum.michi.app.ui.icons.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

val IconPack.ShieldRounded: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "ShieldRounded",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M480,876q-7,0,-13,-1t-12,-3q-135,-45,-215,-166.5T160,444v-189q0,-25,14.5,-45t37.5,-29l240,-90q14,-5,28,-5t28,5l240,90q23,9,37.5,29t14.5,45v189q0,140,-80,261.5T505,872q-6,2,-12,3t-13,1Zm0,-80q104,-33,172,-132t68,-220v-189l-240,-90 -240,90v189q0,121,68,220t172,132Zm0,-316Z",
            ),
            fill = SolidColor(Color(0xFFE3E3E3)),
        )
    }.build()
}

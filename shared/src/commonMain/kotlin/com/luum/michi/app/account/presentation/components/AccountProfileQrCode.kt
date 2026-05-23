package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.luum.michi.app.account.presentation.util.createAccountQrMatrix

@Composable
internal fun AccountProfileQrCode(
    seed: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(seed) { createAccountQrMatrix(seed) }
    val onColor = Color(0xFF111111)
    val offColor = Color.White

    Canvas(modifier = modifier) {
        val moduleSize = size.minDimension / matrix.size

        drawRect(color = offColor, size = size)

        for (row in 0 until matrix.size) {
            for (column in 0 until matrix.size) {
                if (matrix[row, column]) {
                    drawRect(
                        color = onColor,
                        topLeft = Offset(column * moduleSize, row * moduleSize),
                        size = Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}

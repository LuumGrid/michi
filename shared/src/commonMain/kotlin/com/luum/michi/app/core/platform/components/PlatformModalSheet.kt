package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlatformModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeightFraction: Float = 0.78f,
    content: @Composable (Modifier) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val sheetMaxHeight = with(density) {
        (windowInfo.containerSize.height * maxHeightFraction).toDp()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        content(
            modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight),
        )
    }
}

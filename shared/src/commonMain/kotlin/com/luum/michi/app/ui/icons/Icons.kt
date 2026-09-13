package com.luum.michi.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shared toolbar primitives. Only icons needed by [com.luum.michi.app.ui.components.Toolbar]
 * and [com.luum.michi.app.ui.components.SearchField] live here so tab/feature icons
 * stay with their caller (the tab bar receives its [ImageVector]s per item).
 */
internal object AppIcons {
    val Search: ImageVector get() = Icons.Filled.Search
    val Back: ImageVector get() = Icons.AutoMirrored.Filled.ArrowBack
    val Clear: ImageVector get() = Icons.Filled.Close
}

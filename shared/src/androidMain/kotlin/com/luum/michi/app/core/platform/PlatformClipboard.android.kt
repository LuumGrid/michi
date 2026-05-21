package com.luum.michi.app.core.platform

import android.content.ClipData
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry

actual suspend fun Clipboard.setPlainText(text: String) {
    setClipEntry(ClipData.newPlainText("Michi account", text).toClipEntry())
}

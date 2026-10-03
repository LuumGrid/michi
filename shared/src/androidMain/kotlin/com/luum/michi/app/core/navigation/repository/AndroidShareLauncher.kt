package com.luum.michi.app.core.navigation.repository

import android.content.Context
import android.content.Intent
import com.luum.michi.app.core.navigation.domain.ShareLauncher

/**
 * Plain text share via `ACTION_SEND` behind a chooser. Uses the application
 * context with `FLAG_ACTIVITY_NEW_TASK`, same as [AndroidUrlOpener]; a
 * missing share target leaves the app untouched instead of crashing.
 */
internal class AndroidShareLauncher(
    private val context: Context,
) : ShareLauncher {

    override fun share(text: String, subject: String?) {
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            }
            val chooser = Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            // No share target: stay in app.
        }
    }
}

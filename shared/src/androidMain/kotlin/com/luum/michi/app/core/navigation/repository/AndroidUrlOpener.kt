package com.luum.michi.app.core.navigation.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.luum.michi.app.core.navigation.domain.UrlOpener
import com.luum.michi.app.core.navigation.domain.isWebUrl

/**
 * Plain browser opener: streaming platform links have no return flow, so
 * a plain `ACTION_VIEW` is enough (unlike OAuth, which needs Custom Tabs
 * for the redirect dance). Non-web URLs never leave the app.
 */
internal class AndroidUrlOpener(
    private val context: Context,
) : UrlOpener {

    override fun open(url: String) {
        if (!isWebUrl(url)) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // No browser installed (or race on the resolver): stay in app.
        }
    }
}

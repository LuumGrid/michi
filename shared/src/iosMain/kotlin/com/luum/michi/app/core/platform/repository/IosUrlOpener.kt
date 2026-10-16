package com.luum.michi.app.core.platform.repository

import com.luum.michi.app.core.platform.domain.UrlOpener

/**
 * iOS opener: not wired yet (Android-only for now). Compiles the shared
 * contract; opening links here means `UIApplication.shared.open` behind
 * a platform check, same shape as [AndroidUrlOpener].
 */
internal class IosUrlOpener : UrlOpener {
    override fun open(url: String) = Unit
}

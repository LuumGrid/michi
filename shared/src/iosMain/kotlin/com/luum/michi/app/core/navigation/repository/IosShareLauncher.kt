package com.luum.michi.app.core.navigation.repository

import com.luum.michi.app.core.navigation.domain.ShareLauncher

/**
 * iOS share sheet: not wired yet (Android-only for now). Compiles the shared
 * contract; sharing here means a `UIActivityViewController` behind a platform
 * check, same shape as [AndroidShareLauncher].
 */
internal class IosShareLauncher : ShareLauncher {
    override fun share(text: String, subject: String?) = Unit
}

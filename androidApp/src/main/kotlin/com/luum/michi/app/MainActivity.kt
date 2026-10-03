package com.luum.michi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Compose entry: renders the shared [App] (auth landing vs shell routing)
 * and keeps dispatching the OAuth deep link against the process-wide
 * [MichiDependencies] owned by [MichiApplication].
 */
class MainActivity : ComponentActivity() {

    private val dependencies: MichiDependencies
        get() = (application as MichiApplication).dependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            App(dependencies = dependencies)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Routes an incoming intent: the OAuth callback (`michi://…`) goes to the
     * auth flow; a shared text (`ACTION_SEND`) or a web link (`ACTION_VIEW`) is
     * offered to the media deep-link parser, which ignores non-media content.
     */
    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            dependencies.onDeepLink(text)
            return
        }
        val uri = intent.dataString ?: return
        if (uri.startsWith("michi://", ignoreCase = true)) {
            dependencies.onOAuthCallback(uri)
        } else {
            dependencies.onDeepLink(uri)
        }
    }
}

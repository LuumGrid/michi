package com.luum.michi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * UI placeholder: keeps the architecture entry point (OAuth deep-link dispatch
 * against the process-wide [MichiDependencies] owned by [MichiApplication])
 * without any Compose UI. Screens will be re-added on top of the pure-logic
 * state holders.
 */
class MainActivity : ComponentActivity() {

    private val dependencies: MichiDependencies
        get() = (application as MichiApplication).dependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleOAuthCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent?) {
        val uri = intent?.dataString ?: return
        dependencies.onOAuthCallback(uri)
    }
}

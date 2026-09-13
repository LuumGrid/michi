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

        handleOAuthCallback(intent)
        enableEdgeToEdge()
        setContent {
            App(dependencies = dependencies)
        }
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

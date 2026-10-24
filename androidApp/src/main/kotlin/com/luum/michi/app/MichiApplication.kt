package com.luum.michi.app

import android.app.Application

/**
 * Process-lifetime owner of [MichiDependencies].
 *
 * The graph (CoroutineScope + OkHttp HttpClient) is created exactly once per
 * process, so Activity recreation (rotation, dark mode, locale, multi-window)
 * neither leaks the previous graph nor re-triggers [MichiDependencies.bootstrap].
 */
class MichiApplication : Application() {

    lateinit var dependencies: MichiDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        dependencies = createMichiDependencies(this)
        dependencies.bootstrap()
    }
}

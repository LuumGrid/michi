package com.luum.michi.app.core.network.domain

/**
 * Network metering for the AUTO image-quality setting. Platform impls are
 * wired through `MichiDependencies`; the default assumes unmetered (no
 * platform check required, safe for tests and the iOS holder).
 */
internal interface Connectivity {
    /** True when the active network is unmetered (Wi-Fi / Ethernet). */
    fun isUnmetered(): Boolean
}

internal class AlwaysUnmeteredConnectivity : Connectivity {
    override fun isUnmetered(): Boolean = true
}

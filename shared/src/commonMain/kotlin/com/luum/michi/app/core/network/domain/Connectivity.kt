package com.luum.michi.app.core.network.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Network metering for the AUTO image-quality setting. Platform impls are
 * wired through `MichiDependencies`; the default assumes unmetered (no
 * platform check required, safe for tests and the iOS holder).
 */
internal interface Connectivity {
    /** True while the active network is unmetered (Wi-Fi / Ethernet). */
    val isUnmetered: StateFlow<Boolean>
}

internal class AlwaysUnmeteredConnectivity : Connectivity {
    override val isUnmetered = MutableStateFlow(true)
}

package com.luum.michi.app.core.platform.repository

import com.luum.michi.app.core.platform.domain.Connectivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * iOS metering for the AUTO image-quality setting: `NWPathMonitor` reports
 * expensive (cellular/hotspot) or constrained (Low Data Mode) paths as metered;
 * no path / not satisfied counts as unmetered, mirroring Android.
 *
 * NOTE: Kotlin/Native Network interop — written but not compiled on the Linux
 * CI (iOS targets are not built here). Verify with a real iOS build.
 */
internal class IosConnectivity : Connectivity {
    private val unmeteredState = MutableStateFlow(true)
    override val isUnmetered: StateFlow<Boolean> = unmeteredState.asStateFlow()

    private val monitor = nw_path_monitor_create()

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val metered = path != null &&
                nw_path_get_status(path) == nw_path_status_satisfied &&
                (nw_path_is_expensive(path) || nw_path_is_constrained(path))
            unmeteredState.value = !metered
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }
}

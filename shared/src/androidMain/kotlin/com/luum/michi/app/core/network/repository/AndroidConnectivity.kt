package com.luum.michi.app.core.network.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.luum.michi.app.core.network.domain.Connectivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android metering for the AUTO image-quality setting. Seeds from the active
 * network's capabilities and updates on every connectivity change. Unknown or
 * absent capabilities count as unmetered, so images never degrade on a false
 * negative (there is nothing to load while offline anyway).
 */
internal class AndroidConnectivity(context: Context) : Connectivity {
    private val manager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val unmeteredState = MutableStateFlow(currentUnmetered())
    override val isUnmetered: StateFlow<Boolean> = unmeteredState.asStateFlow()

    init {
        manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                unmeteredState.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }

            override fun onLost(network: Network) {
                // No active network: treat as unmetered (nothing to load anyway).
                unmeteredState.value = true
            }
        })
    }

    private fun currentUnmetered(): Boolean {
        val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork) ?: return true
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}

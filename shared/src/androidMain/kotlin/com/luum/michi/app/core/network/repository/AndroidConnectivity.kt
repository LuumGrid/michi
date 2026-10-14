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
 * network and recomputes from it on every connectivity change (never trusting
 * the callback's own value). Unknown or absent capabilities count as unmetered,
 * so images never degrade on a false negative.
 *
 * Data Saver is intentionally not consulted: Android only applies it to metered
 * networks, which are already metered here, so a Wi-Fi connection stays HIGH.
 */
internal class AndroidConnectivity(context: Context) : Connectivity {
    private val manager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val unmeteredState = MutableStateFlow(currentUnmetered())
    override val isUnmetered: StateFlow<Boolean> = unmeteredState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = recompute()
        override fun onLost(network: Network) = recompute()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = recompute()
    }

    init {
        manager?.registerDefaultNetworkCallback(networkCallback)
    }

    private fun recompute() {
        unmeteredState.value = currentUnmetered()
    }

    private fun currentUnmetered(): Boolean {
        val cm = manager ?: return true
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return true
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
    }
}

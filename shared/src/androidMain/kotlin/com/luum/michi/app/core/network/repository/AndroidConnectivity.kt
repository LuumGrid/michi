package com.luum.michi.app.core.network.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
 * the callback's own value). Data Saver counts as metered; unknown or absent
 * capabilities count as unmetered, so images never degrade on a false negative.
 */
internal class AndroidConnectivity(context: Context) : Connectivity {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val unmeteredState = MutableStateFlow(currentUnmetered())
    override val isUnmetered: StateFlow<Boolean> = unmeteredState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = recompute()
        override fun onLost(network: Network) = recompute()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = recompute()
    }

    private val dataSaverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = recompute()
    }

    init {
        manager?.registerDefaultNetworkCallback(networkCallback)
        // System broadcast: must be registered at runtime (no manifest entry).
        appContext.registerReceiver(
            dataSaverReceiver,
            IntentFilter(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun recompute() {
        unmeteredState.value = currentUnmetered()
    }

    private fun currentUnmetered(): Boolean {
        val cm = manager ?: return true
        // Data Saver on: honor the user's data cap → metered.
        if (cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return true
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
    }
}

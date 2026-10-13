package com.luum.michi.app.core.network.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.luum.michi.app.core.network.domain.Connectivity

/**
 * Android metering check for the AUTO image-quality setting. Reads the active
 * network's capabilities; anything unknown falls back to unmetered so the app
 * never degrades images on a false negative.
 */
internal class AndroidConnectivity(context: Context) : Connectivity {
    private val appContext = context.applicationContext

    override fun isUnmetered(): Boolean {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}

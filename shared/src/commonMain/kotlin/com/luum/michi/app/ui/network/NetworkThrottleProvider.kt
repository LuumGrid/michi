package com.luum.michi.app.ui.network

import androidx.compose.runtime.compositionLocalOf
import com.luum.michi.app.core.network.domain.NetworkThrottle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes the transport's [NetworkThrottle] to the UI, mirroring the language
 * provider. Defaults to "never waiting" so previews and tests need no wiring.
 */
internal val LocalNetworkThrottle = compositionLocalOf<NetworkThrottle> { NoOpNetworkThrottle }

private object NoOpNetworkThrottle : NetworkThrottle {
    override val waitingRequests: StateFlow<Int> = MutableStateFlow(0)
}

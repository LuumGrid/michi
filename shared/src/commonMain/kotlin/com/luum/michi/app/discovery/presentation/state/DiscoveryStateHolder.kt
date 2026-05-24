package com.luum.michi.app.discovery.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem
import com.luum.michi.app.discovery.data.DiscoveryFeed
import com.luum.michi.app.discovery.data.DiscoveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val EmptyFeed = DiscoveryFeed(
    releasingToday = emptyList(),
    trendingAnimation = emptyList(),
    trendingReading = emptyList(),
)

internal class DiscoveryStateHolder(
    private val repository: DiscoveryRepository,
    private val scope: CoroutineScope,
) {
    private var feedState by mutableStateOf(EmptyFeed)
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

    val releasingToday: List<PlatformHomeReleaseItem> get() = feedState.releasingToday
    val trendingAnimation: List<PlatformHomeMediaItem> get() = feedState.trendingAnimation
    val trendingReading: List<PlatformHomeMediaItem> get() = feedState.trendingReading
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

    fun load() {
        loadingState = true
        errorState = null
        scope.launch {
            when (val result = repository.loadFeed()) {
                is NetworkResult.Success -> feedState = result.value
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }
    }
}

@Composable
internal fun rememberDiscoveryStateHolder(
    repository: DiscoveryRepository,
): DiscoveryStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        DiscoveryStateHolder(repository, scope).also { it.load() }
    }
}

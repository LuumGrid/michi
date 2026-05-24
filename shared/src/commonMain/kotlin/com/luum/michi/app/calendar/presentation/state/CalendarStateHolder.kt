package com.luum.michi.app.calendar.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.luum.michi.app.calendar.data.CalendarDay
import com.luum.michi.app.calendar.data.CalendarRepository
import com.luum.michi.app.core.network.NetworkResult

internal class CalendarStateHolder(
    private val repository: CalendarRepository,
    private val scope: CoroutineScope,
) {
    private var daysState by mutableStateOf<List<CalendarDay>>(emptyList())
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

    val days: List<CalendarDay> get() = daysState
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

    fun load() {
        loadingState = true
        errorState = null
        scope.launch {
            repository.loadFeed()
                .catch { loadingState = false }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            daysState = result.value.days
                            loadingState = false
                        }
                        is NetworkResult.Failure -> {
                            if (daysState.isEmpty()) errorState = result.error.toString()
                            loadingState = false
                        }
                    }
                }
        }
    }
}

@Composable
internal fun rememberCalendarStateHolder(
    repository: CalendarRepository,
): CalendarStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        CalendarStateHolder(repository, scope)
    }
}

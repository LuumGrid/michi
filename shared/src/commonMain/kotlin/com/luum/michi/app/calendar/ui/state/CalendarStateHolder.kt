package com.luum.michi.app.calendar.ui.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.luum.michi.app.calendar.domain.CalendarDay
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult

internal class CalendarStateHolder(
    private val repository: CalendarRepository,
    private val scope: CoroutineScope,
) {
    private var daysState: List<CalendarDay> = emptyList()
    private var loadingState = false
    private var errorState: NetworkError? = null

    val days: List<CalendarDay> get() = daysState
    val isLoading: Boolean get() = loadingState
    val error: NetworkError? get() = errorState

    fun load() {
        // Drop overlapping loads so a slower (stale) emission cannot overwrite a newer one.
        if (loadingState) return
        loadingState = true
        errorState = null
        scope.launch {
            repository.loadFeed()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    if (daysState.isEmpty()) errorState = NetworkError.Unknown(cause)
                    loadingState = false
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            daysState = result.value.days
                            loadingState = false
                        }
                        is NetworkResult.Failure -> {
                            if (daysState.isEmpty()) errorState = result.error
                            loadingState = false
                        }
                    }
                }
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createCalendarStateHolder(
    repository: CalendarRepository,
    scope: CoroutineScope,
): CalendarStateHolder {
    return CalendarStateHolder(repository, scope)
}

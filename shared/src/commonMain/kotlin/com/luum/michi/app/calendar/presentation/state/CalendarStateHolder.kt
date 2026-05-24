package com.luum.michi.app.calendar.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.calendar.data.CalendarDay
import com.luum.michi.app.calendar.data.CalendarRepository
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
            when (val result = repository.loadFeed()) {
                is NetworkResult.Success -> daysState = result.value.days
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }
    }
}

@Composable
internal fun rememberCalendarStateHolder(
    repository: CalendarRepository,
): CalendarStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        CalendarStateHolder(repository, scope).also { it.load() }
    }
}

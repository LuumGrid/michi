package com.luum.michi.app.mediaList.ui.common.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared optimistic `+1` saver for both user lists. A tap applies the increment
 * to the entry right away, then the save is queued per media behind one lock so
 * saves for the same entry never interleave (a second tap coalesces into the
 * latest value). The last server-confirmed entry is the rollback target, never
 * an optimistic value that was never saved.
 */
internal class ListProgressSaver<T : SortableMediaListEntry>(
    private val loader: MediaListLoader<T>,
    private val scope: CoroutineScope,
    private val save: suspend (T) -> NetworkResult<Unit>,
    // Fired after a successful save, so Root can invalidate surfaces that show
    // the viewer's status (the calendar).
    private val onSaved: () -> Unit = {},
) {
    /** Non-blocking failure of a `+1` save, shown as a compact dismissible row. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    // One lock per media: saves for the same entry never interleave, and the
    // second `+1` sends the latest progress once the first save finishes.
    private val mutexes = mutableMapOf<Int, Mutex>()
    // Last server-confirmed entry per media: the rollback target.
    private val confirmed = mutableMapOf<Int, T>()
    // Pending saves per media. The mutex and the confirmed entry are dropped
    // only when this reaches 0, so a tap that lands as a save ends still finds
    // the confirmed base instead of being skipped as a no-op.
    private val pending = mutableMapOf<Int, Int>()

    fun clearActionError() {
        actionError = null
    }

    /** A reload replaces every entry: drop the rollback targets and the error. */
    fun onReload() {
        actionError = null
        confirmed.clear()
    }

    /**
     * Applies [transform] to the entry and enqueues a save. The value just
     * before the tap is captured here as the fallback base.
     */
    fun enqueue(id: Int, transform: (T) -> T) {
        val index = loader.indexOf { it.id == id }
        if (index == -1) return
        val before = loader[index]
        // Seed the confirmed value before the first tap.
        confirmed.getOrPut(id) { before }
        loader[index] = transform(before)
        pending[id] = (pending[id] ?: 0) + 1
        scope.launch {
            val mutex = mutexes.getOrPut(id) { Mutex() }
            mutex.withLock { saveOnce(id, before) }
            val remaining = (pending[id] ?: 1) - 1
            if (remaining <= 0) {
                pending.remove(id)
                mutexes.remove(id)
                confirmed.remove(id)
            } else {
                pending[id] = remaining
            }
        }
    }

    private suspend fun saveOnce(id: Int, before: T) {
        val index = loader.indexOf { it.id == id }
        if (index == -1) return
        // Send the latest state (a queued +1 may have advanced it).
        val sent = loader[index]
        // The current confirmed value (a previous save may have advanced it).
        // If it is gone, fall back to the value captured at this tap, never
        // `sent` — that would mask the pending change.
        val base = confirmed[id] ?: before
        // Nothing new since the last confirmed value: skip, leave actionError alone.
        if (sent == base) return
        when (val result = save(sent)) {
            is NetworkResult.Success -> {
                confirmed[id] = sent
                actionError = null
                onSaved()
            }
            is NetworkResult.Failure -> {
                // Revert to the confirmed value, only if unchanged since.
                val idx = loader.indexOf { it.id == id }
                if (idx != -1 && loader[idx] == sent) loader[idx] = base
                actionError = result.error
            }
        }
    }
}

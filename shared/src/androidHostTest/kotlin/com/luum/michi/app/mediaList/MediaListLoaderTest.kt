package com.luum.michi.app.mediaList

import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.mediaList.domain.common.MediaListLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaListLoaderTest {

    @Test
    fun overlappingLoadIssuesASingleFetch() {
        val gate = CompletableDeferred<Unit>()
        var fetches = 0
        val loader = MediaListLoader<String>(CoroutineScope(Dispatchers.Unconfined)) {
            fetches++
            gate.await()
            NetworkResult.Success(listOf("a"))
        }
        loader.load(1)
        loader.load(1)
        gate.complete(Unit)
        assertEquals(1, fetches)
        assertEquals(listOf("a"), loader.entries)
    }
}

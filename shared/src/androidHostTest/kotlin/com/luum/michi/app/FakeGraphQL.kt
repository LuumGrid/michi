package com.luum.michi.app

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonElement

/**
 * Shared scripted GraphQL fake for feature integration tests (single definition
 * on purpose): canned [pages] per call, an optional [gate] to hold a request in
 * flight (set it to a fresh `CompletableDeferred` and complete it to release),
 * and a recorded [requests] list for query-shape assertions.
 */
internal class FakeGraphQL(
    private val pages: List<JsonElement>,
    private val failureAt: Int = -1,
) : AniListGraphQLClient {
    var calls = 0
    var gate: CompletableDeferred<Unit>? = null
    val requests = mutableListOf<AniListGraphQLRequest>()

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        val index = calls++
        requests += request
        gate?.await()
        if (index == failureAt) return NetworkResult.Failure(NetworkError.Http(500, null))
        return NetworkResult.Success(parseData(pages[index]))
    }
}

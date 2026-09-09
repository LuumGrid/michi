package com.luum.michi.app.calendar

import com.luum.michi.app.core.domain.network.AniListGraphQLClient
import com.luum.michi.app.core.domain.network.AniListGraphQLRequest
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonElement

/** Shared scripted GraphQL fake for calendar tests (single definition on purpose). */
internal class FakeGraphQL(
    private val pages: List<JsonElement>,
    private val failureAt: Int = -1,
) : AniListGraphQLClient {
    var calls = 0
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        val index = calls++
        gate?.await()
        if (index == failureAt) return NetworkResult.Failure(NetworkError.Http(500, null))
        return NetworkResult.Success(parseData(pages[index]))
    }
}

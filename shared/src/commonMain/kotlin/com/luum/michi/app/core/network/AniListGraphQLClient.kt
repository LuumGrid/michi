package com.luum.michi.app.core.network

import kotlinx.serialization.json.JsonElement

/**
 * Contract for executing GraphQL operations against AniList. The real Ktor-backed
 * implementation is wired in Iteration 2; for now this is the surface that
 * `core/session` and feature repositories code against.
 *
 * @param T A parsed payload type. Implementations are responsible for taking the
 *   raw `data` JSON element and returning a typed value.
 */
internal interface AniListGraphQLClient {
    suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T>
}

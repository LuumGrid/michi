package com.luum.michi.app.core.network

/**
 * Contract for executing GraphQL operations against AniList. The real Ktor-backed
 * implementation is wired in Iteration 2; for now this is the surface that
 * `core/session` and feature repositories code against.
 *
 * @param T A parsed payload type. Implementations are responsible for taking a
 *   raw `data` JSON string and returning a typed value.
 */
internal interface AniListGraphQLClient {
    suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (String) -> T,
    ): NetworkResult<T>
}

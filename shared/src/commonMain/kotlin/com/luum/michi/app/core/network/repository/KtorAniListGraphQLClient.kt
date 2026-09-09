package com.luum.michi.app.core.network.repository

import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.network.domain.AniListEndpoints
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLEnvelope
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Real Ktor-backed implementation of [AniListGraphQLClient]. Authenticates each
 * request with the current token (when present) via a Bearer header.
 *
 * The `parseData` lambda receives the raw `data` JSON element from the response
 * envelope and is expected to deserialize it into the typed payload.
 *
 * On HTTP 429 (rate-limited), the client reads the `Retry-After` header and
 * waits up to [MAX_RATE_LIMIT_RETRIES] times before giving up with
 * [NetworkError.RateLimited].
 */
internal class KtorAniListGraphQLClient(
    private val httpClient: HttpClient,
    private val tokenProvider: suspend () -> AniListToken?,
    private val json: Json = AniListJson,
    private val rateLimiter: AniListRateLimiter = AniListRateLimiter(),
) : AniListGraphQLClient {

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        return try {
            executeWithRetry(request, parseData, retriesLeft = AniListNetworkPolicy.MAX_RATE_LIMIT_RETRIES)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            NetworkResult.Failure(NetworkError.Unknown(throwable))
        }
    }

    private suspend fun <T> executeWithRetry(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
        retriesLeft: Int,
    ): NetworkResult<T> {
        val token = tokenProvider()

        // The permit is held only for the round-trip and released in withPermit's finally,
        val response = rateLimiter.withPermit {
            httpClient.post(AniListEndpoints.GraphQL) {
                headers {
                    token?.let { append("Authorization", it.authorizationHeaderValue) }
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

        val body = response.bodyAsText()

        if (!response.status.isSuccess()) {
            return when (response.status.value) {
                401 -> NetworkResult.Failure(NetworkError.Unauthorized(body))
                429 -> {
                    val retryAfterSeconds = response.headers["Retry-After"]?.toLongOrNull()
                        ?: AniListNetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS
                    // Inform the limiter so concurrent callers also pause.
                    rateLimiter.onRateLimited(retryAfterSeconds)
                    if (retriesLeft > 0) {
                        delay(retryAfterSeconds.seconds)
                        executeWithRetry(request, parseData, retriesLeft - 1)
                    } else {
                        NetworkResult.Failure(NetworkError.RateLimited(retryAfterSeconds.toInt()))
                    }
                }
                else -> NetworkResult.Failure(NetworkError.Http(response.status.value, body))
            }
        }

        // Update proactive backoff from server headers on every successful response.
        val remaining = response.headers["X-RateLimit-Remaining"]?.toIntOrNull()
        val resetEpoch = response.headers["X-RateLimit-Reset"]?.toLongOrNull()
        rateLimiter.onResponseHeaders(remaining, resetEpoch)

        val envelope = json.decodeFromString(AniListGraphQLEnvelope.serializer(), body)

        if (!envelope.errors.isNullOrEmpty()) {
            return NetworkResult.Failure(
                NetworkError.GraphQL(envelope.errors.map { it.message }),
            )
        }

        val data = envelope.data
            ?: return NetworkResult.Failure(
                NetworkError.GraphQL(listOf("Empty data field in GraphQL response")),
            )

        return NetworkResult.Success(parseData(data))
    }
}

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
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Maps transport throwables to [NetworkError]. Connectivity signals become
 * [NetworkError.NoConnection]; everything else stays [NetworkError.Unknown].
 *
 * Matching is by class simple name (no `java.net` imports) so this stays
 * common-safe across Android and iOS engines; the cause chain is walked
 * because engines wrap the root socket error. Deliberately narrow:
 * TLS/auth failures are not connectivity issues.
 */
internal fun throwableToNetworkError(throwable: Throwable): NetworkError {
    var current: Throwable? = throwable
    while (current != null) {
        when (current::class.simpleName) {
            "UnknownHostException",
            "ConnectException",
            "NoRouteToHostException",
            "SocketTimeoutException",
            "ConnectTimeoutException",
            "TimeoutException",
            "HttpRequestTimeoutException",
            "HttpConnectTimeoutException",
            -> return NetworkError.NoConnection
        }
        current = current.cause
    }
    return NetworkError.Unknown(throwable)
}

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
    // Debug builds only: logs each operation's duration (operationName + ms).
    private val debugLogging: Boolean = false,
    // Fired when a request that carried a token gets a 401, so the session can
    // drop the dead token. Never fired for anonymous requests.
    private val onUnauthorized: (AniListToken) -> Unit = {},
) : AniListGraphQLClient {

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        val start = TimeSource.Monotonic.markNow()
        return try {
            executeWithRetry(request, parseData, retriesLeft = AniListNetworkPolicy.MAX_RATE_LIMIT_RETRIES)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            NetworkResult.Failure(throwableToNetworkError(throwable))
        } finally {
            if (debugLogging) {
                val ms = start.elapsedNow().inWholeMilliseconds
                println("[Michi] GraphQL ${request.operationName ?: "?"} ${ms}ms")
            }
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
                401 -> {
                    // Only a token-authenticated request means the session died;
                    // an anonymous 401 must not close a session it never had.
                    // The token travels so a stale 401 cannot close a newer one.
                    if (token != null) onUnauthorized(token)
                    NetworkResult.Failure(NetworkError.Unauthorized(body))
                }
                429 -> {
                    val retryAfterSeconds = response.headers["Retry-After"]?.toLongOrNull()
                        ?: AniListNetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS
                    // Inform the limiter so this caller (and every concurrent one)
                    // pauses for the server-mandated interval. The retry below goes
                    // through withPermit again, which waits that pause in phase 1 —
                    // so the wait is the same but now it is visible in
                    // NetworkThrottle.waitingRequests instead of an unseen delay.
                    rateLimiter.onRateLimited(retryAfterSeconds)
                    if (retriesLeft > 0) {
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

package com.luum.michi.app.core.network

import com.luum.michi.app.core.auth.AniListToken
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
import kotlinx.serialization.json.Json

/**
 * Real Ktor-backed implementation of [AniListGraphQLClient]. Authenticates each
 * request with the current token (when present) via a Bearer header.
 *
 * The `parseData` lambda receives the raw `data` JSON string from the response
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
) : AniListGraphQLClient {

    private companion object {
        const val MAX_RATE_LIMIT_RETRIES = 2
        const val DEFAULT_RETRY_AFTER_SECONDS = 60L
    }

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (String) -> T,
    ): NetworkResult<T> {
        return try {
            executeWithRetry(request, parseData, retriesLeft = MAX_RATE_LIMIT_RETRIES)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            NetworkResult.Failure(NetworkError.Unknown(throwable))
        }
    }

    private suspend fun <T> executeWithRetry(
        request: AniListGraphQLRequest,
        parseData: (String) -> T,
        retriesLeft: Int,
    ): NetworkResult<T> {
        val token = tokenProvider()
        val response = httpClient.post(AniListEndpoints.GraphQL) {
            headers {
                token?.let { append("Authorization", it.authorizationHeaderValue) }
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val body = response.bodyAsText()

        if (!response.status.isSuccess()) {
            return when (response.status.value) {
                401 -> NetworkResult.Failure(NetworkError.Unauthorized(body))
                429 -> {
                    val retryAfterSeconds = response.headers["Retry-After"]?.toLongOrNull()
                        ?: DEFAULT_RETRY_AFTER_SECONDS
                    if (retriesLeft > 0) {
                        delay(retryAfterSeconds * 1_000L)
                        executeWithRetry(request, parseData, retriesLeft - 1)
                    } else {
                        NetworkResult.Failure(NetworkError.RateLimited(retryAfterSeconds.toInt()))
                    }
                }
                else -> NetworkResult.Failure(NetworkError.Http(response.status.value, body))
            }
        }

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

        return NetworkResult.Success(parseData(data.toString()))
    }
}

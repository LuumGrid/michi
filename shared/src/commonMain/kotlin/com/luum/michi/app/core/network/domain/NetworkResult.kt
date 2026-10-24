package com.luum.michi.app.core.network.domain

internal sealed class NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>()
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>()
}

internal sealed class NetworkError {
    data object NoConnection : NetworkError()
    data class Http(val status: Int, val body: String?) : NetworkError()
    data class Unauthorized(val body: String?) : NetworkError()
    data class GraphQL(val messages: List<String>) : NetworkError()
    /** A node the query asked for came back null (media/character/staff/studio).
     *  [entity] and [id] are for logs only; the user sees a generic message. */
    data class NotFound(val entity: String, val id: Int) : NetworkError()
    data class Unknown(val cause: Throwable?) : NetworkError()
    /** Returned when all retry attempts after a 429 are exhausted. */
    data class RateLimited(val retryAfterSeconds: Int?) : NetworkError()
}

internal inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(value))
    is NetworkResult.Failure -> this
}

/**
 * Turns a nullable payload into a required node: a null value becomes
 * [NetworkError.NotFound] (the query ran fine, the node is gone), a failure
 * passes through untouched. Keeps the "not found for id" branch in one place.
 */
internal fun <T : Any> NetworkResult<T?>.requireNode(entity: String, id: Int): NetworkResult<T> = when (this) {
    is NetworkResult.Success -> value?.let { NetworkResult.Success(it) }
        ?: NetworkResult.Failure(NetworkError.NotFound(entity, id))
    is NetworkResult.Failure -> this
}

package com.luum.michi.app.core.network

internal sealed class NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>()
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>()
}

internal sealed class NetworkError {
    data object NoConnection : NetworkError()
    data class Http(val status: Int, val body: String?) : NetworkError()
    data class Unauthorized(val body: String?) : NetworkError()
    data class GraphQL(val messages: List<String>) : NetworkError()
    data class Unknown(val cause: Throwable?) : NetworkError()
}

internal inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(value))
    is NetworkResult.Failure -> this
}

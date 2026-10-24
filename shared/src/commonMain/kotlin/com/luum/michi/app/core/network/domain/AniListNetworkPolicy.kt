package com.luum.michi.app.core.network.domain

import kotlin.time.Duration.Companion.minutes

/**
 * Única fuente de verdad para los tiempos de red contra AniList.
 * Todo timeout/reintento se configura aquí, no en cada cliente o llamada.
 */
internal object AniListNetworkPolicy {
    /** Las queries filtradas del catálogo tardan 10s+ en AniList: muy por encima
     *  del read timeout de 10s que OkHttp trae por defecto. */
    const val REQUEST_TIMEOUT_MILLIS = 60_000L
    const val CONNECT_TIMEOUT_MILLIS = 15_000L
    const val SOCKET_TIMEOUT_MILLIS = 60_000L

    /** Reintentos ante HTTP 429 antes de rendirse con [NetworkError.RateLimited]. */
    const val MAX_RATE_LIMIT_RETRIES = 2

    /** Espera cuando el 429 llega sin header `Retry-After`. */
    const val DEFAULT_RETRY_AFTER_SECONDS = 60L

    /** Frescura de listas/cuenta/dashboard en memoria entre recargas.
     *  Es `Duration` (no `Long` + sufijo como los timeouts de socket) porque se
     *  compara contra `TimeMark.elapsedNow()`, no contra APIs que pidan millis. */
    val CACHE_TTL = 5.minutes
}

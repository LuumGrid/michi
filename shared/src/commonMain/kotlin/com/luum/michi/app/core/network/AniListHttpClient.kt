package com.luum.michi.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Default JSON configuration used across AniList GraphQL parsing.
 */
internal val AniListJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
}

/**
 * Configures the shared HttpClient used by AniList API access. The platform
 * provides the engine (`OkHttp` on Android, `Darwin` on iOS).
 */
internal fun createAniListHttpClient(
    engine: HttpClientEngine = createAniListHttpClientEngine(),
    logLevel: LogLevel = LogLevel.NONE,
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(AniListJson)
    }
    install(Logging) {
        level = logLevel
    }
    defaultRequest {
        header("Accept", "application/json")
    }
}

internal expect fun createAniListHttpClientEngine(): HttpClientEngine

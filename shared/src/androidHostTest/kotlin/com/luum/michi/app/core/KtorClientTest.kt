package com.luum.michi.app.core

import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.AniListRateLimiter
import com.luum.michi.app.core.network.repository.KtorAniListGraphQLClient
import com.luum.michi.app.core.network.repository.AniListJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun client(
    handler: MockEngine,
    rateLimiter: AniListRateLimiter = AniListRateLimiter(),
) = KtorAniListGraphQLClient(
    httpClient = HttpClient(handler) {
        install(ContentNegotiation) {
            json(AniListJson)
        }
    },
    tokenProvider = { null },
    rateLimiter = rateLimiter,
)

private fun request() = AniListGraphQLRequest(
    query = "query X { ok }",
    variables = JsonObject(mapOf("a" to JsonPrimitive(1))),
    operationName = "X",
)

private fun <T> runExecute(client: KtorAniListGraphQLClient, parse: (JsonElement) -> T): NetworkResult<T> =
    runBlocking { client.execute(request(), parse) }

class KtorClientTest {

    @Test
    fun successParsesData() {
        val engine = MockEngine { respond("""{"data":{"ok":true}}""", HttpStatusCode.OK) }
        val result = runExecute(client(engine)) { data ->
            data.jsonObject["ok"]!!.jsonPrimitive.boolean
        }
        assertEquals(NetworkResult.Success(true), result)
    }

    @Test
    fun graphQLErrorsBecomeFailure() {
        val engine = MockEngine {
            respond("""{"errors":[{"message":"Bad"}]}""", HttpStatusCode.OK)
        }
        val result = runExecute(client(engine)) { data -> data.toString() }
        assertEquals(NetworkResult.Failure(NetworkError.GraphQL(listOf("Bad"))), result)
    }

    @Test
    fun emptyDataBecomesFailure() {
        val engine = MockEngine { respond("""{}""", HttpStatusCode.OK) }
        val result = runExecute(client(engine)) { data -> data.toString() }
        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun unauthorizedMapsBody() {
        val engine = MockEngine {
            respond("denied", HttpStatusCode.Unauthorized, Headers.Empty)
        }
        val result = runExecute(client(engine)) { data -> data.toString() }
        assertEquals(NetworkResult.Failure(NetworkError.Unauthorized("denied")), result)
    }

    @Test
    fun rateLimitedRetriesThenGivesUp() {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("slow", HttpStatusCode.TooManyRequests, headersOf("Retry-After", "0"))
        }
        val result = runExecute(client(engine)) { data -> data.toString() }
        assertEquals(NetworkResult.Failure(NetworkError.RateLimited(0)), result)
        assertEquals(3, calls)
    }

    @Test
    fun malformedBodyBecomesUnknown() {
        val engine = MockEngine { respond("not json", HttpStatusCode.OK) }
        val result = runExecute(client(engine)) { data -> data.toString() }
        assertTrue(result is NetworkResult.Failure)
        assertTrue((result as NetworkResult.Failure).error is NetworkError.Unknown)
    }
}

package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeTheme
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeThemeKind
import com.luum.michi.app.mediaDetail.repository.media.AnimeThemesRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val FrierenJson = """
{
  "anime": [
    {
      "name": "Sousou no Frieren",
      "slug": "sousou_no_frieren",
      "animethemes": [
        {
          "type": "OP",
          "slug": "OP1",
          "song": { "title": "Yuusha", "artists": [ { "name": "YOASOBI" } ] },
          "animethemeentries": [ { "episodes": "1-16" } ]
        },
        {
          "type": "ED",
          "slug": "ED1",
          "song": { "title": "Anytime Anywhere", "artists": [ { "name": "milet" } ] },
          "animethemeentries": [
            { "episodes": "1-14" },
            { "episodes": "16" },
            { "episodes": "17-27" },
            { "episodes": "28" }
          ]
        },
        {
          "type": "ED",
          "slug": "ED1-TV",
          "song": { "title": "bliss", "artists": [ { "name": "milet" } ] },
          "animethemeentries": [ { "episodes": "1", "sequence": null } ]
        },
        {
          "type": "OP",
          "slug": "OP2",
          "song": { "title": "Haru", "artists": [ { "name": "Yorushika" } ] },
          "animethemeentries": [ { "episodes": "17-28" } ]
        }
      ]
    }
  ]
}
"""

private fun client(engine: MockEngine) = HttpClient(engine) {
    install(ContentNegotiation) {
        json(AniListJson)
    }
}

private fun repository(engine: MockEngine) = AnimeThemesRepositoryImpl(client(engine))

class AnimeThemesRepositoryTest {

    @Test
    fun mapsTheAnimeSongsInAnimeThemesOrder() {
        val result = runBlocking {
            repository(MockEngine { respond(FrierenJson, HttpStatusCode.OK) }).themes(154587)
        }
        val themes = assertIs<NetworkResult.Success<List<AnimeTheme>>>(result).value

        assertEquals(
            listOf("OP1", "OP2"),
            themes.filter { it.kind == AnimeThemeKind.OPENING }.map { it.label },
        )
        assertEquals(
            listOf("ED1", "ED1-TV"),
            themes.filter { it.kind == AnimeThemeKind.ENDING }.map { it.label },
        )
        val op1 = themes.first { it.label == "OP1" }
        assertEquals("Sousou no Frieren", op1.animeName)
        assertEquals("https://animethemes.moe/anime/sousou_no_frieren/OP1", op1.pageUrl)
        assertEquals(listOf("YOASOBI"), op1.artists)
        assertEquals("Yuusha", op1.title)
        assertEquals("1-16", op1.episodes)
        assertEquals("1-14, 16, 17-27, 28", themes.first { it.label == "ED1" }.episodes)
    }

    @Test
    fun requestFiltersByAniListId() {
        var site: String? = null
        var externalId: String? = null
        val engine = MockEngine { request ->
            site = request.url.parameters["filter[site]"]
            externalId = request.url.parameters["filter[external_id]"]
            respond("""{"anime":[]}""", HttpStatusCode.OK)
        }

        runBlocking { repository(engine).themes(154587) }

        assertEquals("AniList", site)
        assertEquals("154587", externalId)
    }

    @Test
    fun noAnimeIsAnEmptySuccess() {
        val result = runBlocking {
            repository(MockEngine { respond("""{"anime":[]}""", HttpStatusCode.OK) }).themes(1)
        }

        val themes = assertIs<NetworkResult.Success<List<AnimeTheme>>>(result).value
        assertTrue(themes.isEmpty())
    }

    @Test
    fun serverErrorBecomesHttpFailure() {
        val result = runBlocking {
            repository(MockEngine { respond("oops", HttpStatusCode.InternalServerError) }).themes(1)
        }

        assertEquals(NetworkResult.Failure(NetworkError.Http(500, null)), result)
    }

    @Test
    fun theSameAnimeIsFetchedOnce() {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(FrierenJson, HttpStatusCode.OK)
        }
        val themesRepository = repository(engine)

        runBlocking {
            themesRepository.themes(154587)
            themesRepository.themes(154587)
        }

        assertEquals(1, calls)
    }

    @Test
    fun anArtistAliasWinsOverTheName() {
        val json = """
            {
              "anime": [
                {
                  "name": "Anime",
                  "slug": "anime",
                  "animethemes": [
                    {
                      "type": "OP",
                      "slug": "OP1",
                      "song": {
                        "title": "Song",
                        "artists": [ { "name": "milet", "artistsong": { "alias": "Alias" } } ]
                      },
                      "animethemeentries": []
                    }
                  ]
                }
              ]
            }
        """
        val result = runBlocking {
            repository(MockEngine { respond(json, HttpStatusCode.OK) }).themes(1)
        }

        val themes = assertIs<NetworkResult.Success<List<AnimeTheme>>>(result).value
        assertEquals(listOf("Alias"), themes.single().artists)
    }
}

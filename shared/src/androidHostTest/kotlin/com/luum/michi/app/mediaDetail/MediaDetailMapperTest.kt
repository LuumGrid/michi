package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaExternalLink
import com.luum.michi.app.mediaDetail.domain.media.model.bestPopularRank
import com.luum.michi.app.mediaDetail.domain.media.model.bestRatedRank
import com.luum.michi.app.mediaDetail.domain.media.model.siteLabel
import com.luum.michi.app.mediaDetail.domain.media.model.videoUrl
import com.luum.michi.app.mediaDetail.repository.media.MediaDetailRepositoryImpl
import com.luum.michi.app.mediaList.FakeGraphQL
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Canned AniList `Media` JSON (the shape returned by the real `MediaDetail`
 * query) driven through the real repository + mapper.
 */
private fun mediaDetailPageJson() = buildJsonObject {
    put("Media", buildJsonObject {
        put("id", 42)
        put("title", buildJsonObject {
            put("romaji", "Romaji Title")
            put("native", "ネイティブタイトル")
        })
        put("synonyms", JsonArray(listOf(JsonPrimitive("Alias One"), JsonPrimitive("Alias Two"))))
        put("rankings", JsonArray(listOf(
            buildJsonObject {
                put("rank", 1)
                put("type", "RATED")
                put("context", "highest rated anime")
                put("format", "TV")
                put("season", "WINTER")
                put("year", 2020)
                put("allTime", false)
            },
            buildJsonObject {
                put("rank", 5)
                put("type", "RATED")
                put("context", "highest rated anime")
                put("format", "TV")
                put("season", "WINTER")
                put("year", 2019)
                put("allTime", true)
            },
            buildJsonObject {
                put("rank", 3)
                put("type", "POPULAR")
                put("context", "most popular all time")
                put("format", "TV")
                put("season", "SPRING")
                put("year", 2018)
                put("allTime", true)
            },
        )))
        put("tags", JsonArray(listOf(
            buildJsonObject {
                put("name", "Action")
                put("rank", 80)
                put("isMediaSpoiler", false)
            },
            buildJsonObject {
                put("name", "Spoiler Tag")
                put("rank", 10)
                put("isMediaSpoiler", true)
            },
            buildJsonObject {
                put("name", "")
                put("rank", 0)
                put("isMediaSpoiler", false)
            },
        )))
        put("externalLinks", JsonArray(listOf(
            buildJsonObject {
                put("id", 1)
                put("site", "Crunchyroll")
                put("url", "https://crunchyroll.com/42")
                put("type", "STREAMING")
                put("icon", "https://crunchyroll.com/favicon.ico")
                put("color", "#F47521")
                put("language", "English")
            },
            buildJsonObject {
                put("id", 2)
                put("site", "Blank Url")
                put("url", "   ")
                put("type", "INFO")
            },
            buildJsonObject {
                put("id", 3)
                put("site", "")
                put("url", "https://example.com/42")
                put("type", "INFO")
            },
            buildJsonObject {
                put("id", 4)
                put("site", "Dead Link")
                put("url", "https://example.com/dead")
                put("type", "STREAMING")
                put("isDisabled", true)
            },
        )))
        put("trailer", buildJsonObject {
            put("id", "abc123")
            put("site", "youtube")
            put("thumbnail", "https://i.ytimg.com/vi/abc123/hqdefault.jpg")
        })
    })
}

class MediaDetailMapperTest {

    private suspend fun loadDetail(): MediaDetail {
        val repository = MediaDetailRepositoryImpl(FakeGraphQL(listOf(mediaDetailPageJson())))
        val result = repository.loadDetail(42, voiceLanguage = "JAPANESE", strings = EnglishStrings)
        assertIs<NetworkResult.Success<MediaDetail>>(result)
        return result.value
    }

    @Test
    fun mapsTitlesAndSynonyms() = runBlocking {
        val detail = loadDetail()

        assertEquals("Romaji Title", detail.titleRomaji)
        assertEquals("ネイティブタイトル", detail.titleNative)
        assertEquals(listOf("Alias One", "Alias Two"), detail.synonyms)
    }

    @Test
    fun mapsRankingsAndPicksAllTimeLowestRank() = runBlocking {
        val detail = loadDetail()

        assertEquals(3, detail.rankings.size)
        val first = detail.rankings.first()
        assertEquals(1, first.rank)
        assertEquals("RATED", first.type)
        assertEquals("highest rated anime", first.context)
        assertEquals("TV", first.format)
        assertEquals("WINTER", first.season)
        assertEquals(2020, first.year)
        assertEquals(false, first.allTime)

        // All-time entries are rank 5 (RATED) and rank 3 (POPULAR); each
        // helper filters its own type, so rated picks 5 and popular picks 3.
        assertEquals(5, detail.rankings.bestRatedRank()?.rank)
        assertEquals(3, detail.rankings.bestPopularRank()?.rank)
    }

    @Test
    fun bestRankFallsBackToLowestWhenNoAllTime() {
        val rankings = listOf(
            com.luum.michi.app.mediaDetail.domain.media.model.MediaRank(
                rank = 7, type = "RATED", context = null, format = null,
                season = null, year = null, allTime = false,
            ),
            com.luum.michi.app.mediaDetail.domain.media.model.MediaRank(
                rank = 2, type = "RATED", context = null, format = null,
                season = null, year = null, allTime = false,
            ),
        )

        assertEquals(2, rankings.bestRatedRank()?.rank)
        assertNull(emptyList<com.luum.michi.app.mediaDetail.domain.media.model.MediaRank>().bestRatedRank())
    }

    @Test
    fun mapsTagsAndDropsBlankNames() = runBlocking {
        val detail = loadDetail()

        assertEquals(2, detail.tags.size)
        assertEquals("Action", detail.tags[0].name)
        assertEquals(80, detail.tags[0].rank)
        assertEquals(false, detail.tags[0].isMediaSpoiler)
        assertEquals("Spoiler Tag", detail.tags[1].name)
        assertEquals(10, detail.tags[1].rank)
        assertTrue(detail.tags[1].isMediaSpoiler)
    }

    @Test
    fun mapsExternalLinksAndDropsBlankSiteOrUrl() = runBlocking {
        val detail = loadDetail()

        assertEquals(1, detail.externalLinks.size)
        val link = detail.externalLinks.single()
        assertEquals("Crunchyroll", link.site)
        assertEquals("https://crunchyroll.com/42", link.url)
        assertEquals("STREAMING", link.type)
        assertEquals("https://crunchyroll.com/favicon.ico", link.iconUrl)
        assertEquals("#F47521", link.color)
        assertEquals("English", link.language)
    }

    @Test
    fun detailQueryRequestsExternalLinkLanguage() = runBlocking {
        val graphQL = FakeGraphQL(listOf(mediaDetailPageJson()))
        MediaDetailRepositoryImpl(graphQL).loadDetail(42, voiceLanguage = "JAPANESE", strings = EnglishStrings)

        val query = graphQL.requests.single().query
        assertTrue(query.contains("externalLinks { id site url type icon color language }"), query)
    }

    @Test
    fun officialSiteLabelLocalizes() {
        val official = MediaExternalLink(
            site = "Official Site",
            url = "https://example.com",
            type = "OFFICIAL",
            iconUrl = null,
            color = null,
        )
        assertEquals("Official site", official.siteLabel(EnglishStrings))
        assertEquals("Sitio oficial", official.siteLabel(SpanishStrings))
        assertEquals("Twitter", official.copy(site = "Twitter").siteLabel(EnglishStrings))
    }

    @Test
    fun mapsTrailerAndBuildsYoutubeUrl() = runBlocking {
        val detail = loadDetail()

        val trailer = detail.trailer
        assertEquals("abc123", trailer?.videoId)
        assertEquals("youtube", trailer?.site)
        assertEquals("https://i.ytimg.com/vi/abc123/hqdefault.jpg", trailer?.thumbnailUrl)
        assertEquals("https://youtu.be/abc123", trailer?.videoUrl())
    }
}

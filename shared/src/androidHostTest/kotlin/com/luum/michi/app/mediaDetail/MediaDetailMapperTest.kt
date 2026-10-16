package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.core.model.ExternalLink
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationsPage
import com.luum.michi.app.core.model.isOfficialSite
import com.luum.michi.app.core.model.siteLabel
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Canned AniList `Media` JSON (the shape returned by the real `MediaDetail`
 * query) driven through the real repository + mapper.
 */
private fun studioEdge(id: Int, name: String, isMain: Boolean, isAnimation: Boolean) = buildJsonObject {
    put("isMain", isMain)
    put("node", buildJsonObject {
        put("id", id)
        put("name", name)
        put("isAnimationStudio", isAnimation)
    })
}

private fun mediaDetailPageJson() = buildJsonObject {
    put("Media", buildJsonObject {
        put("id", 42)
        put("title", buildJsonObject {
            put("romaji", "Romaji Title")
            put("native", "ネイティブタイトル")
        })
        put("synonyms", JsonArray(listOf(JsonPrimitive("Alias One"), JsonPrimitive("Alias Two"))))
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
        put("nextAiringEpisode", buildJsonObject {
            put("episode", 3)
            put("airingAt", 1_700_000_000L)
            put("timeUntilAiring", 345_600L)
        })
        // AniList can return the same studio twice (main + sub on the same
        // node); the mapper must collapse it or the studio chips rail
        // crashes on the duplicate LazyRow key.
        // AniList sends ONE list mixing animation studios and producers, with
        // the main one flagged per edge (NAME order from the server). The mapper
        // splits it: animation studios first with the main one leading, then the
        // producers; the repeated node collapses.
        put("studios", buildJsonObject {
            put("edges", JsonArray(listOf(
                studioEdge(id = 8, name = "Bones", isMain = false, isAnimation = true),
                studioEdge(id = 7, name = "Studio Pierrot", isMain = true, isAnimation = true),
                studioEdge(id = 7, name = "Studio Pierrot", isMain = true, isAnimation = true),
                studioEdge(id = 9, name = "Aniplex", isMain = true, isAnimation = false),
                studioEdge(id = 10, name = "Madhouse", isMain = false, isAnimation = false),
            )))
        })
        put("relations", buildJsonObject {
            put("edges", JsonArray(listOf(
                buildJsonObject {
                    put("relationType", "SEQUEL")
                    put("node", buildJsonObject {
                        put("id", 77)
                        put("format", "TV")
                        put("status", "RELEASING")
                        put("season", "SUMMER")
                        put("seasonYear", 2026)
                        put("title", buildJsonObject { put("romaji", "Sequel Show") })
                    })
                },
            )))
        })
    })
}

class MediaDetailMapperTest {

    private suspend fun loadDetail(): MediaDetail {
        val repository = MediaDetailRepositoryImpl(FakeGraphQL(listOf(mediaDetailPageJson())))
        val result = repository.loadDetail(42, strings = EnglishStrings)
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
        assertEquals("#F47521", link.colorHex)
        assertEquals("English", link.language)
    }

    @Test
    fun detailQueryRequestsExternalLinkLanguage() = runBlocking {
        val graphQL = FakeGraphQL(listOf(mediaDetailPageJson()))
        MediaDetailRepositoryImpl(graphQL).loadDetail(42, strings = EnglishStrings)

        val query = graphQL.requests.single().query
        assertTrue(query.contains("externalLinks { id site url type icon color language }"), query)
    }

    @Test
    fun officialSiteLabelLocalizes() {
        val official = ExternalLink(
            site = "Official Site",
            url = "https://example.com",
            type = "INFO",
            iconUrl = null,
            colorHex = null,
        )
        assertTrue(official.isOfficialSite())
        assertEquals("Official site", official.siteLabel(EnglishStrings))
        assertEquals("Sitio oficial", official.siteLabel(SpanishStrings))
        val twitter = official.copy(site = "Twitter")
        assertFalse(twitter.isOfficialSite())
        assertEquals("Twitter", twitter.siteLabel(EnglishStrings))
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

    @Test
    fun deduplicatesStudiosById() = runBlocking {
        val detail = loadDetail()

        assertEquals(2, detail.studios.size)
        assertEquals(listOf(7, 8), detail.studios.map { it.id })
    }

    @Test
    fun splitsAnimationStudiosFromProducersWithTheMainOneFirst() = runBlocking {
        val detail = loadDetail()

        // Animation studios, the main one (Studio Pierrot) ahead of the rest even
        // though the server sent them in NAME order (Bones first).
        assertEquals(listOf("Studio Pierrot", "Bones"), detail.studios.map { it.name })
        // The rest are producers (isAnimationStudio == false).
        assertEquals(listOf("Aniplex", "Madhouse"), detail.producers.map { it.name })
    }

    @Test
    fun mapsNextAiringEpisode() = runBlocking {        val detail = loadDetail()

        val next = detail.nextEpisode
        assertEquals(3, next?.episode)
        assertEquals(1_700_000_000L, next?.airingAtEpochSeconds)
        assertEquals(345_600L, next?.timeUntilAiringSeconds)
    }

    @Test
    fun mapsRelationSeasonAndWorkStatus() = runBlocking {
        val detail = loadDetail()

        val relation = detail.relations.single()
        assertEquals("Sequel Show", relation.title)
        assertEquals("TV", relation.format)
        assertEquals(MediaSeason.SUMMER, relation.season)
        assertEquals(2026, relation.year)
        assertEquals(MediaWorkStatus.RELEASING, relation.workStatus)
    }

    @Test
    fun mapsRecommendationsPageWithPaging() = runBlocking {
        val page = buildJsonObject {
            put("Media", buildJsonObject {
                put("id", 42)
                put("recommendations", buildJsonObject {
                    put("pageInfo", buildJsonObject {
                        put("hasNextPage", true)
                        put("currentPage", 2)
                    })
                    put("nodes", JsonArray(listOf(
                        buildJsonObject {
                            put("id", 5)
                            put("rating", 7)
                            put("mediaRecommendation", buildJsonObject {
                                put("id", 99)
                                put("title", buildJsonObject { put("romaji", "Recommended") })
                            })
                        },
                    )))
                })
            })
        }

        val result = MediaDetailRepositoryImpl(FakeGraphQL(listOf(page)))
            .loadRecommendations(42, page = 2)

        assertIs<NetworkResult.Success<MediaRecommendationsPage>>(result)
        assertEquals(listOf(99), result.value.items.map { it.id })
        assertEquals(true, result.value.hasNextPage)
        assertEquals(2, result.value.currentPage)
    }
}

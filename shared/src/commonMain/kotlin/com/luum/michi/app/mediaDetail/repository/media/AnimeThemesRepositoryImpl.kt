package com.luum.michi.app.mediaDetail.repository.media

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.mediaDetail.domain.media.AnimeThemesRepository
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeTheme
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeThemeKind
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val AnimeThemesUrl = "https://api.animethemes.moe/anime"
private const val AnimeThemesAnimeUrl = "https://animethemes.moe/anime"

/** Last N anime kept in memory; failures are not cached. */
private const val AnimeThemesCacheSize = 50

@Serializable
private data class AnimeThemesResponseDto(
    val anime: List<AnimeDto> = emptyList(),
)

@Serializable
private data class AnimeDto(
    val name: String? = null,
    val slug: String? = null,
    val animethemes: List<ThemeDto> = emptyList(),
)

@Serializable
private data class ThemeDto(
    val type: String? = null,
    val slug: String? = null,
    val song: SongDto? = null,
    val animethemeentries: List<EntryDto> = emptyList(),
)

@Serializable
private data class SongDto(
    val title: String? = null,
    val artists: List<ArtistDto> = emptyList(),
)

@Serializable
private data class ArtistDto(
    val name: String? = null,
    val artistsong: ArtistSongDto? = null,
)

@Serializable
private data class ArtistSongDto(
    val alias: String? = null,
)

@Serializable
private data class EntryDto(
    val episodes: String? = null,
)

/**
 * AnimeThemes (api.animethemes.moe), the only non-AniList API: one GET per anime,
 * no auth, no rate limiter (one request when the section comes into view).
 * Successes are kept in memory (last 50 anime); failures are not cached.
 */
internal class AnimeThemesRepositoryImpl(
    private val httpClient: HttpClient,
    private val json: Json = AniListJson,
) : AnimeThemesRepository {

    private val cache = LinkedHashMap<Int, List<AnimeTheme>>()
    private val cacheMutex = Mutex()

    override suspend fun themes(aniListId: Int): NetworkResult<List<AnimeTheme>> {
        cacheMutex.withLock { cache[aniListId] }?.let { return NetworkResult.Success(it) }
        val result = fetch(aniListId)
        if (result is NetworkResult.Success) {
            cacheMutex.withLock {
                cache[aniListId] = result.value
                if (cache.size > AnimeThemesCacheSize) cache.remove(cache.keys.first())
            }
        }
        return result
    }

    private suspend fun fetch(aniListId: Int): NetworkResult<List<AnimeTheme>> = try {
        val response = httpClient.get(AnimeThemesUrl) {
            parameter("filter[has]", "resources")
            parameter("filter[site]", "AniList")
            parameter("filter[external_id]", aniListId.toString())
            parameter("include", "animethemes.song.artists,animethemes.animethemeentries")
        }
        if (!response.status.isSuccess()) {
            NetworkResult.Failure(NetworkError.Http(response.status.value, null))
        } else {
            val body = json.decodeFromString<AnimeThemesResponseDto>(response.bodyAsText())
            NetworkResult.Success(body.toThemes())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Failure(NetworkError.Unknown(e))
    }
}

private fun AnimeThemesResponseDto.toThemes(): List<AnimeTheme> {
    val anime = anime.firstOrNull() ?: return emptyList()
    val animeName = anime.name?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
    val animeSlug = anime.slug?.takeIf { it.isNotEmpty() } ?: return emptyList()
    return anime.animethemes.mapNotNull { theme ->
        val kind = when (theme.type) {
            "OP" -> AnimeThemeKind.OPENING
            "ED" -> AnimeThemeKind.ENDING
            else -> return@mapNotNull null
        }
        val title = theme.song?.title?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val slug = theme.slug?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val artists = theme.song.artists.mapNotNull { artist ->
            (artist.artistsong?.alias ?: artist.name)?.trim()?.takeIf(String::isNotEmpty)
        }
        val episodes = theme.animethemeentries
            .mapNotNull { it.episodes?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
        AnimeTheme(
            kind = kind,
            label = slug,
            title = title,
            artists = artists,
            episodes = episodes,
            animeName = animeName,
            pageUrl = "$AnimeThemesAnimeUrl/$animeSlug/$slug",
        )
    }
}

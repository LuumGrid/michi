package com.luum.michi.app.mediaDetail

import com.luum.michi.app.mediaDetail.domain.media.model.AnimeTheme
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeThemeKind
import com.luum.michi.app.mediaDetail.domain.media.model.MusicService
import com.luum.michi.app.mediaDetail.domain.media.model.searchQuery
import com.luum.michi.app.mediaDetail.domain.media.model.videoSearchUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicSearchTest {

    private fun theme(title: String, vararg artists: String) = AnimeTheme(
        kind = AnimeThemeKind.OPENING,
        label = "OP1",
        title = title,
        artists = artists.toList(),
        episodes = null,
        animeName = "Sousou no Frieren",
        pageUrl = "https://animethemes.moe/anime/sousou_no_frieren/OP1",
    )

    @Test
    fun musicServicesSearchTheTitleAndArtists() {
        val song = theme("Anytime Anywhere", "milet")

        assertEquals(
            "https://open.spotify.com/search/Anytime%20Anywhere%20milet",
            MusicService.SPOTIFY.searchUrl(song.searchQuery()),
        )
        assertEquals(
            "https://music.youtube.com/search?q=Anytime%20Anywhere%20milet",
            MusicService.YOUTUBE_MUSIC.searchUrl(song.searchQuery()),
        )
        assertEquals(
            "https://music.apple.com/search?term=Anytime%20Anywhere%20milet",
            MusicService.APPLE_MUSIC.searchUrl(song.searchQuery()),
        )
    }

    @Test
    fun videoSearchUsesAnimeNameLabelAndSong() {
        val song = theme("Yuusha", "YOASOBI")

        assertEquals(
            "https://www.youtube.com/results?search_query=Sousou%20no%20Frieren%20OP1%20Yuusha",
            song.videoSearchUrl(),
        )
    }
}

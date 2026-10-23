package com.luum.michi.app.mediaDetail.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeThemeKind
import com.luum.michi.app.mediaDetail.domain.media.model.hasTags
import com.luum.michi.app.mediaDetail.domain.media.model.upcomingNextEpisode
import com.luum.michi.app.mediaDetail.ui.components.DetailLoadState
import com.luum.michi.app.mediaDetail.ui.components.DetailPageColumn
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailExpandableText
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailLinkGroup
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailLinkPanel
import com.luum.michi.app.mediaDetail.ui.components.rememberSpoilerRevealState
import com.luum.michi.app.mediaDetail.ui.components.toLinkItem
import com.luum.michi.app.ui.components.MediaCardRail
import com.luum.michi.app.ui.components.PersonCardRail
import com.luum.michi.app.mediaDetail.ui.components.toCardRailItem
import com.luum.michi.app.mediaDetail.ui.components.toPersonItem
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailGenres
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHero
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHeroStats
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailInformationPanel
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailNextEpisodeBand
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailTags
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailThemePanel
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailVideos
import com.luum.michi.app.mediaDetail.ui.media.components.hasInformation
import com.luum.michi.app.mediaDetail.ui.media.components.trailerUrl
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.core.model.RailPageSize

/**
 * Full media detail page. Section order: hero → next-episode band → quick stats
 * → synopsis → information → genres → characters → staff → trailer → links
 * (streaming + external) → related → recommendations → tags.
 * Reached from any card via `State.openMedia`; back pops the detail stack.
 * Framed panels mark the data blocks (synopsis, information, links); the media
 * rails, the hero, the trailer and the genre/tag chip rows stay edge-to-edge.
 * The editor pencil in the toolbar opens the entry editor for signed-in viewers;
 * the +1 lives on the list cards, not here.
 */
@Composable
internal fun MediaDetailScreen(
    holder: MediaDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit,
    onOpenGenre: (String, MediaType) -> Unit,
    onOpenUrl: (String) -> Unit,
    onViewAll: (DetailListKind) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    DetailLoadState(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        error = holder.error,
        actionError = holder.actionError,
        onRetry = onRetry,
        onDismissActionError = { holder.clearActionError() },
        strings = strings,
        modifier = modifier,
    ) {
        if (detail != null) {
            MediaDetailContent(
                detail = detail,
                holder = holder,
                strings = strings,
                bottomPadding = bottomPadding,
                onOpenMedia = onOpenMedia,
                onOpenCharacter = onOpenCharacter,
                onOpenStaff = onOpenStaff,
                onOpenStudio = onOpenStudio,
                onOpenGenre = onOpenGenre,
                onOpenUrl = onOpenUrl,
                onViewAll = onViewAll,
            )
        }
    }
}

@Composable
private fun MediaDetailContent(
    detail: MediaDetail,
    holder: MediaDetailStateHolder,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit,
    onOpenGenre: (String, MediaType) -> Unit,
    onOpenUrl: (String) -> Unit,
    onViewAll: (DetailListKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isManga = detail.type == MediaDetailType.MANGA
    // Memory only, keyed by text: a refresh that changes the synopsis starts
    // hidden again instead of revealing the wrong range index.
    val spoilerState = rememberSpoilerRevealState(detail.id, detail.descriptionPlain)
    var showSpoilerTags by remember(detail.id) { mutableStateOf(false) }
    // Streaming (Crunchyroll…) and everything else (social/official) share one
    // framed panel with two subtitles; AniList marks the difference by `type`.
    val streamingLinks = detail.externalLinks.filter { it.type.equals("STREAMING", ignoreCase = true) }
    val otherLinks = detail.externalLinks.filterNot { it.type.equals("STREAMING", ignoreCase = true) }
    val linkGroups = listOf(
        MediaDetailLinkGroup(strings.mediaDetailStreamingLinksTitle, streamingLinks.map { it.toLinkItem(strings) }),
        MediaDetailLinkGroup(strings.mediaDetailExternalLinksTitle, otherLinks.map { it.toLinkItem(strings) }),
    )
    DetailPageColumn(bottomPadding = bottomPadding, modifier = modifier) {
        item(key = "hero") { MediaDetailHero(detail = detail, strings = strings) }
        if (detail.upcomingNextEpisode() != null) {
            item(key = "next-episode") {
                MediaDetailNextEpisodeBand(detail = detail, strings = strings)
            }
        }
        if (detail.averageScore != null || detail.popularity != null || detail.favourites != null) {
            item(key = "stats") { MediaDetailHeroStats(detail = detail, strings = strings) }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item(key = "synopsis") {
                MediaDetailExpandableText(
                    title = strings.mediaDetailDescriptionTitle,
                    text = detail.descriptionPlain,
                    strings = strings,
                    spoilerState = spoilerState,
                    spoilers = detail.descriptionSpoilers,
                )
            }
        }
        if (detail.hasInformation(isManga)) {
            item(key = "information") {
                MediaDetailInformationPanel(
                    detail = detail,
                    strings = strings,
                    isManga = isManga,
                    onOpenStudio = onOpenStudio,
                )
            }
        }
        if (detail.genres.isNotEmpty()) {
            item(key = "genres") {
                MediaDetailGenres(detail = detail, strings = strings, onOpenGenre = onOpenGenre)
            }
        }
        if (holder.characters.items.isNotEmpty()) {
            item(key = "characters") {
                PersonCardRail(
                    title = strings.mediaDetailCharactersTitle,
                    items = holder.characters.items.take(RailPageSize).map { it.toPersonItem(strings) },
                    onSelectPrimary = onOpenCharacter,
                    onSelectSecondary = onOpenStaff,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_CHARACTERS) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
        if (holder.staff.items.isNotEmpty()) {
            item(key = "staff") {
                PersonCardRail(
                    title = strings.mediaDetailTabStaff,
                    items = holder.staff.items.take(RailPageSize).map { it.toPersonItem() },
                    onSelectPrimary = onOpenStaff,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_STAFF) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
        if (detail.trailerUrl() != null) {
            item(key = "videos") {
                MediaDetailVideos(detail = detail, strings = strings, onOpenUrl = onOpenUrl)
            }
        }
        if (detail.type == MediaDetailType.ANIME) {
            val themes = holder.themes.orEmpty()
            val openings = themes.filter { it.kind == AnimeThemeKind.OPENING }
            val endings = themes.filter { it.kind == AnimeThemeKind.ENDING }
            if (openings.isNotEmpty()) {
                item(key = "openings") {
                    MediaDetailThemePanel(
                        strings.mediaDetailOpeningsTitle,
                        openings,
                        strings,
                        onOpenUrl,
                    )
                }
            }
            if (endings.isNotEmpty()) {
                item(key = "endings") {
                    MediaDetailThemePanel(
                        strings.mediaDetailEndingsTitle,
                        endings,
                        strings,
                        onOpenUrl,
                    )
                }
            }
        }
        if (linkGroups.any { it.links.isNotEmpty() }) {
            item(key = "links") { MediaDetailLinkPanel(groups = linkGroups, onOpenUrl = onOpenUrl) }
        }
        if (detail.relations.isNotEmpty()) {
            item(key = "related") {
                MediaCardRail(
                    title = strings.mediaDetailRelationsTitle,
                    items = detail.relations.take(RailPageSize).map { it.toCardRailItem(strings, isManga) },
                    onSelect = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_RELATED) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
        if (holder.recommendations.items.isNotEmpty()) {
            item(key = "recommendations") {
                MediaCardRail(
                    title = strings.mediaDetailTabRecommendations,
                    items = holder.recommendations.items.take(RailPageSize)
                        .map { it.toCardRailItem(strings, isManga) },
                    onSelect = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_RECOMMENDATIONS) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
        if (detail.tags.hasTags()) {
            item(key = "tags") {
                MediaDetailTags(
                    detail = detail,
                    strings = strings,
                    showSpoilers = showSpoilerTags,
                    onToggleSpoilers = { showSpoilerTags = !showSpoilerTags },
                )
            }
        }
    }
}

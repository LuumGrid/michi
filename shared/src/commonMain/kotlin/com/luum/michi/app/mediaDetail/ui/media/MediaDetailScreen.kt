package com.luum.michi.app.mediaDetail.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.upcomingNextEpisode
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailExpandableText
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailLinkGroup
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailLinkPanel
import com.luum.michi.app.mediaDetail.ui.components.toLinkItem
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailGenres
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHero
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHeroStats
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailInformationPanel
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailNextEpisodeBand
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailTags
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailVideos
import com.luum.michi.app.mediaDetail.ui.media.components.hasInformation
import com.luum.michi.app.mediaDetail.ui.media.components.trailerUrl
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.core.model.RailPageSize
import com.luum.michi.app.core.language.domain.networkErrorMessage

/**
 * Full media detail page. Section order: hero → next-episode band → quick stats
 * → synopsis → information → genres → characters → staff → trailer → links
 * (streaming + external) → related → recommendations → tags.
 * Reached from any card via `State.openMedia`; back pops the detail stack.
 * Framed cards mark the data blocks (information, links); the media rails, the
 * trailer, the synopsis and the genre/tag chip rows stay edge-to-edge. The
 * editor pencil in the toolbar opens the entry editor for signed-in viewers;
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
    LoadStateContainer(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        errorMessage = holder.error?.let { strings.networkErrorMessage(it) },
        loadingLabel = strings.mediaDetailLoadingLabel,
        errorTitle = strings.mediaDetailErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (detail != null) holder.error?.let { strings.networkErrorMessage(it) } else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
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
    // Recommendations are a second, lazy query: fire once per media opened.
    LaunchedEffect(detail.id) { holder.loadRecommendations() }
    // Streaming (Crunchyroll…) and everything else (social/official) share one
    // framed panel with two subtitles; AniList marks the difference by `type`.
    val streamingLinks = detail.externalLinks.filter { it.type.equals("STREAMING", ignoreCase = true) }
    val otherLinks = detail.externalLinks.filterNot { it.type.equals("STREAMING", ignoreCase = true) }
    val linkGroups = listOf(
        MediaDetailLinkGroup(strings.mediaDetailStreamingLinksTitle, streamingLinks.map { it.toLinkItem(strings) }),
        MediaDetailLinkGroup(strings.mediaDetailExternalLinksTitle, otherLinks.map { it.toLinkItem(strings) }),
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
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
                MediaRail(
                    title = strings.mediaDetailCharactersTitle,
                    items = holder.characters.items.take(RailPageSize).map { it.toRailItem(strings) },
                    onSelect = onOpenCharacter,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_CHARACTERS) },
                    viewAllLabel = strings.viewAllAction,
                    // A language with no cast empties the rail: say so
                    // instead of leaving a bare header.
                    emptyLabel = strings.mediaDetailNoCharactersLabel.takeIf { !holder.characters.isLoadingMore },
                )
            }
        }
        if (holder.staff.items.isNotEmpty()) {
            item(key = "staff") {
                MediaRail(
                    title = strings.mediaDetailTabStaff,
                    items = holder.staff.items.take(RailPageSize).map { it.toRailItem() },
                    onSelect = onOpenStaff,
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
        if (linkGroups.any { it.links.isNotEmpty() }) {
            item(key = "links") { MediaDetailLinkPanel(groups = linkGroups, onOpenUrl = onOpenUrl) }
        }
        if (detail.relations.isNotEmpty()) {
            item(key = "related") {
                MediaRail(
                    title = strings.mediaDetailRelationsTitle,
                    items = detail.relations.take(RailPageSize).map { it.toRailItem(strings, isManga) },
                    onSelect = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_RELATED) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
        if (holder.recommendations.items.isNotEmpty()) {
            item(key = "recommendations") {
                MediaRail(
                    title = strings.mediaDetailTabRecommendations,
                    items = holder.recommendations.items.take(RailPageSize).map { it.toRailItem(strings, isManga) },
                    onSelect = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_RECOMMENDATIONS) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
        if (detail.tags.any { !it.isMediaSpoiler }) {
            item(key = "tags") { MediaDetailTags(detail = detail, strings = strings) }
        }
    }
}

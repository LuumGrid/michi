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
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailExpandableText
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailExternalLinks
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailGenres
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHero
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHeroStats
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailInformationPanel
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailTags
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailVideos
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.core.network.domain.RailPageSize
import com.luum.michi.app.core.language.domain.networkErrorMessage

/**
 * Full media detail page. Section order: hero → quick stats (+ next-episode
 * band) → synopsis → videos → information → characters → staff → streaming
 * → external links → genres → tags → related → recommendations.
 * Reached from any card via `State.openMedia`; back pops the detail stack.
 * Data blocks (information, links, genres, tags, stats) are framed cards;
 * media rails, the trailer and the synopsis stay edge-to-edge. The editor
 * pencil in the toolbar opens the entry editor for signed-in viewers; the +1
 * lives on the list cards, not here.
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
    onOpenUrl: (String) -> Unit,
    onViewAll: (DetailListKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isManga = detail.type == MediaDetailType.MANGA
    // Recommendations are a second, lazy query: fire once per media opened.
    LaunchedEffect(detail.id) { holder.loadRecommendations() }
    // Streaming (Crunchyroll…) and everything else (social/official) split
    // into two framed sections; AniList marks the difference by `type`.
    val streamingLinks = detail.externalLinks.filter { it.type.equals("STREAMING", ignoreCase = true) }
    val otherLinks = detail.externalLinks.filterNot { it.type.equals("STREAMING", ignoreCase = true) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "hero") { MediaDetailHero(detail = detail, strings = strings) }
        if (
            detail.averageScore != null || detail.popularity != null ||
            detail.favourites != null || detail.nextEpisode != null
        ) {
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
        if (detail.trailer != null) {
            item(key = "videos") {
                MediaDetailVideos(detail = detail, strings = strings, onOpenUrl = onOpenUrl)
            }
        }
        item(key = "information") {
            MediaDetailInformationPanel(
                detail = detail,
                strings = strings,
                isManga = isManga,
                onOpenStudio = onOpenStudio,
            )
        }
        if (detail.characters.items.isNotEmpty()) {
            item(key = "characters") {
                MediaRail(
                    title = strings.mediaDetailCharactersTitle,
                    items = holder.characters.take(RailPageSize).map { it.toRailItem(strings) },
                    onSelect = onOpenCharacter,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_CHARACTERS) },
                    viewAllLabel = strings.viewAllAction,
                    // A language with no cast empties the rail: say so
                    // instead of leaving a bare header.
                    emptyLabel = strings.mediaDetailNoCharactersLabel.takeIf { !holder.isLoadingCharacters },
                )
            }
        }
        if (holder.staff.isNotEmpty()) {
            item(key = "staff") {
                MediaRail(
                    title = strings.mediaDetailTabStaff,
                    items = holder.staff.take(RailPageSize).map { it.toRailItem() },
                    onSelect = onOpenStaff,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_STAFF) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
        if (streamingLinks.isNotEmpty()) {
            item(key = "streaming") {
                MediaDetailExternalLinks(
                    links = streamingLinks,
                    title = strings.mediaDetailStreamingLinksTitle,
                    strings = strings,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
        if (otherLinks.isNotEmpty()) {
            item(key = "links") {
                MediaDetailExternalLinks(
                    links = otherLinks,
                    title = strings.mediaDetailExternalLinksTitle,
                    strings = strings,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
        if (detail.genres.isNotEmpty()) {
            item(key = "genres") { MediaDetailGenres(detail = detail, strings = strings) }
        }
        if (detail.tags.any { !it.isMediaSpoiler }) {
            item(key = "tags") { MediaDetailTags(detail = detail, strings = strings) }
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
        if (holder.recommendations.isNotEmpty()) {
            item(key = "recommendations") {
                MediaRail(
                    title = strings.mediaDetailTabRecommendations,
                    items = holder.recommendations.take(RailPageSize).map { it.toRailItem(strings, isManga) },
                    onSelect = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.MEDIA_RECOMMENDATIONS) },
                    viewAllLabel = strings.viewAllAction,
                )
            }
        }
    }
}


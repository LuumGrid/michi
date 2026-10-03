package com.luum.michi.app.mediaDetail.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.label
import com.luum.michi.app.mediaDetail.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.MediaRailItem
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailExternalLinks
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHero
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailHeroStats
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailInformationPanel
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailScoreDistribution
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailSummaryStats
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailTags
import com.luum.michi.app.mediaDetail.ui.media.components.MediaDetailVideos
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.language.Strings

/**
 * Full media detail page: banner hero + score stats + viewer entry + videos +
 * synopsis + information + tags + external links + rails + distributions.
 * Reached from any card via `State.openMedia`; back pops the detail stack.
 * Character/staff cards render but do not navigate yet — their screens land
 * in a later phase.
 */
@Composable
internal fun MediaDetailScreen(
    holder: MediaDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditEntry: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    when {
        detail == null && holder.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.mediaDetailLoadingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        detail == null && holder.error != null -> {
            MessagePanel(
                title = strings.mediaDetailErrorLabel,
                message = holder.error?.let { strings.networkErrorMessage(it) },
                icon = Icons.Filled.Warning,
                actionLabel = strings.retryAction,
                onAction = onRetry,
                modifier = modifier,
            )
        }
        detail != null -> {
            PullRefresh(isRefreshing = holder.isLoading, onRefresh = onRetry) {
                MediaDetailContent(
                    detail = detail,
                    holder = holder,
                    strings = strings,
                    bottomPadding = bottomPadding,
                    onOpenMedia = onOpenMedia,
                    onOpenUrl = onOpenUrl,
                    modifier = modifier,
                )
            }
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
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isManga = detail.type == MediaDetailType.MANGA
    // Recommendations are a second, lazy query: fire once per media opened.
    LaunchedEffect(detail.id) { holder.loadRecommendations() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "hero") { MediaDetailHero(detail = detail, strings = strings) }
        item(key = "stats") { MediaDetailHeroStats(detail = detail, strings = strings) }
        if (detail.trailer != null) {
            item(key = "videos") {
                MediaDetailVideos(detail = detail, strings = strings, onOpenUrl = onOpenUrl)
            }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item(key = "synopsis") { Synopsis(text = detail.descriptionPlain, strings = strings) }
        }
        item(key = "information") {
            MediaDetailInformationPanel(detail = detail, strings = strings, isManga = isManga)
        }
        if (detail.tags.any { !it.isMediaSpoiler }) {
            item(key = "tags") { MediaDetailTags(detail = detail, strings = strings) }
        }
        if (detail.externalLinks.isNotEmpty()) {
            item(key = "links") {
                MediaDetailExternalLinks(detail = detail, strings = strings, onOpenUrl = onOpenUrl)
            }
        }
        if (detail.relations.isNotEmpty()) {
            item(key = "related") {
                MediaRail(
                    title = strings.mediaDetailRelationsTitle,
                    items = detail.relations.map { relation ->
                        MediaRailItem(
                            id = relation.mediaId,
                            title = relation.title,
                            imageUrl = relation.coverUrl,
                            subtitle = relation.kind.label(strings),
                        )
                    },
                    onSelect = onOpenMedia,
                )
            }
        }
        if (holder.recommendations.isNotEmpty()) {
            item(key = "recommendations") {
                MediaRail(
                    title = strings.mediaDetailTabRecommendations,
                    items = holder.recommendations.map { recommendation ->
                        MediaRailItem(
                            id = recommendation.id,
                            title = recommendation.title,
                            imageUrl = recommendation.coverUrl,
                            subtitle = recommendationSubtitle(recommendation.format, recommendation.year),
                        )
                    },
                    onSelect = onOpenMedia,
                )
            }
        }
        if (detail.characters.items.isNotEmpty()) {
            item(key = "voice-language") {
                VoiceLanguageRail(holder = holder, strings = strings)
            }
            item(key = "characters") {
                MediaRail(
                    title = strings.mediaDetailCharactersTitle,
                    items = holder.characters.map { character ->
                        MediaRailItem(
                            id = character.characterId,
                            title = character.name,
                            imageUrl = character.imageUrl,
                            subtitle = character.voiceActor?.name,
                            key = character.edgeKey,
                        )
                    },
                    onLoadMore = if (holder.charactersHasNextPage && !holder.isLoadingCharacters) {
                        { holder.loadMoreCharacters() }
                    } else {
                        null
                    },
                    loadMoreLabel = strings.mediaDetailLoadMoreAction,
                )
            }
        }
        if (holder.staff.isNotEmpty()) {
            item(key = "staff") {
                MediaRail(
                    title = strings.mediaDetailTabStaff,
                    items = holder.staff.map { entry ->
                        MediaRailItem(
                            id = entry.staffId,
                            title = entry.name,
                            imageUrl = entry.imageUrl,
                            subtitle = entry.role,
                            key = entry.edgeKey,
                        )
                    },
                    onLoadMore = if (holder.staffHasNextPage && !holder.isLoadingStaff) {
                        { holder.loadMoreStaff() }
                    } else {
                        null
                    },
                    loadMoreLabel = strings.mediaDetailLoadMoreAction,
                )
            }
        }
        if (detail.statusDistribution.isNotEmpty()) {
            item(key = "summary-stats") {
                MediaDetailSummaryStats(detail = detail, strings = strings, isManga = isManga)
            }
        }
        if (detail.scoreDistribution.isNotEmpty()) {
            item(key = "score-distribution") {
                MediaDetailScoreDistribution(detail = detail, strings = strings)
            }
        }
    }
}

@Composable
private fun Synopsis(text: String, strings: LanguageStrings) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = strings.mediaDetailDescriptionTitle,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (expanded) strings.mediaDetailReadLessAction else strings.mediaDetailReadMoreAction,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun VoiceLanguageRail(holder: MediaDetailStateHolder, strings: LanguageStrings) {
    val options = listOf(
        "JAPANESE" to strings.voiceLanguageJapanese,
        "ENGLISH" to strings.voiceLanguageEnglish,
        "KOREAN" to strings.voiceLanguageKorean,
        "ITALIAN" to strings.voiceLanguageItalian,
        "SPANISH" to strings.voiceLanguageSpanish,
        "PORTUGUESE" to strings.voiceLanguagePortuguese,
        "FRENCH" to strings.voiceLanguageFrench,
        "GERMAN" to strings.voiceLanguageGerman,
        "HEBREW" to strings.voiceLanguageHebrew,
        "HUNGARIAN" to strings.voiceLanguageHungarian,
    )
    ChipRail(
        tabs = options.map { (value, label) -> ChipTab(value = value, label = label) },
        selected = holder.voiceLanguage,
        onSelect = { holder.selectVoiceLanguage(it) },
    )
}

/** Recommendations have no score field mapped; format + year is the subtitle. */
private fun recommendationSubtitle(format: String?, year: Int?): String? =
    listOfNotNull(format, year?.toString()).joinToString(" · ").ifBlank { null }

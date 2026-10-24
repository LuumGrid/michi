package com.luum.michi.app.profile.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.ScoreFormat
import com.luum.michi.app.core.medialist.domain.parseScoreFormat
import com.luum.michi.app.profile.domain.model.ProfileMediaTypeStats
import com.luum.michi.app.profile.domain.model.ProfileStatDistributionEntry
import com.luum.michi.app.profile.ui.model.formatDistributionLabel
import com.luum.michi.app.profile.ui.model.formatProfileDays
import com.luum.michi.app.profile.ui.model.formatProfileMeanScore
import com.luum.michi.app.profile.ui.model.profileScoreBars
import com.luum.michi.app.profile.ui.model.statusDistributionLabel
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import com.luum.michi.app.ui.components.CatalogChipRail
import com.luum.michi.app.ui.components.DistributionEntry
import com.luum.michi.app.ui.components.DistributionPanel
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.SettingsRow
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * The viewer's statistics page. Data already travels on the profile query (0
 * requests of its own), so the screen only reads the profile holder. The
 * Anime/Manga selector seeds on the header counter the user tapped; changing it
 * returns to the top. The score distribution and the mean score are shown in the
 * viewer's own [ScoreFormat].
 */
@Composable
internal fun ProfileStatisticsScreen(
    holder: ProfileStateHolder,
    scoreFormat: String?,
    initialType: MediaType,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var type by remember { mutableStateOf(initialType) }
    val format = remember(scoreFormat) { parseScoreFormat(scoreFormat) }
    val stats = if (type == MediaType.ANIME) holder.stats.anime else holder.stats.manga
    val isManga = type == MediaType.MANGA
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    Column(modifier = modifier.fillMaxSize()) {
        CatalogChipRail(
            anime = MediaType.ANIME,
            manga = MediaType.MANGA,
            selected = type,
            onSelect = { type = it },
            strings = strings,
        )
        LoadStateContainer(
            // A real account with everything at 0 is loaded data, so the empty
            // panel is only reached once the profile is painted.
            hasContent = holder.hasContent && stats.count > 0,
            isLoading = holder.isLoading || holder.isRefreshing,
            errorMessage = errorMessage,
            loadingLabel = strings.profileStatsLoadingLabel,
            waitingLabel = strings.networkThrottleWaitingLabel,
            errorTitle = strings.profileStatsErrorLabel,
            retryLabel = strings.retryAction,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
            refreshError = if (holder.hasContent && stats.count > 0) errorMessage else null,
            empty = {
                MessagePanel(
                    title = strings.profileStatsEmptyLabel,
                    message = null,
                    icon = AppIcons.StarOutline,
                )
            },
        ) {
            // Changing type is a new view: the scroll returns to the top.
            val listState = rememberResetListState(type)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp + bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "profile-stats-summary") {
                    StatsSummary(stats = stats, format = format, isManga = isManga, strings = strings)
                }
                if (stats.scoreDistribution.isNotEmpty()) {
                    item(key = "profile-stats-scores") {
                        DistributionPanel(
                            title = strings.profileStatsScoreDistributionTitle,
                            entries = profileScoreBars(stats.scoreDistribution, format).toEntries(),
                        )
                    }
                }
                if (stats.formatDistribution.isNotEmpty()) {
                    item(key = "profile-stats-formats") {
                        DistributionPanel(
                            title = strings.profileStatsFormatDistributionTitle,
                            entries = stats.formatDistribution
                                .map { it.copy(label = it.formatDistributionLabel(strings)) }
                                .toEntries(),
                        )
                    }
                }
                if (stats.statusDistribution.isNotEmpty()) {
                    item(key = "profile-stats-statuses") {
                        DistributionPanel(
                            title = strings.profileStatsStatusDistributionTitle,
                            entries = stats.statusDistribution
                                .map { it.copy(label = it.statusDistributionLabel(strings, isManga)) }
                                .toEntries(),
                        )
                    }
                }
                if (stats.topGenres.isNotEmpty()) {
                    item(key = "profile-stats-genres") {
                        DistributionPanel(
                            title = strings.profileStatsTopGenresTitle,
                            entries = stats.topGenres.toEntries(),
                        )
                    }
                }
            }
        }
    }
}

/** The four summary figures of one catalog type. */
@Composable
private fun StatsSummary(
    stats: ProfileMediaTypeStats,
    format: ScoreFormat,
    isManga: Boolean,
    strings: LanguageStrings,
) {
    OptionGroup(title = null) {
        SettingsRow(
            label = if (isManga) strings.mangaLabel else strings.animeLabel,
            value = stats.count.toString(),
            onClick = null,
            divider = false,
        )
        SettingsRow(
            label = strings.profileStatsMeanScoreLabel,
            value = formatProfileMeanScore(stats.meanScore, format),
            onClick = null,
        )
        if (isManga) {
            SettingsRow(
                label = strings.profileStatsVolumesReadLabel,
                value = formatProfileDays(stats.daysOrVolumes),
                onClick = null,
            )
            SettingsRow(
                label = strings.profileStatsChaptersReadLabel,
                value = stats.episodesOrChapters.toString(),
                onClick = null,
            )
        } else {
            SettingsRow(
                label = strings.profileStatsDaysWatchedLabel,
                value = formatProfileDays(stats.daysOrVolumes),
                onClick = null,
            )
            SettingsRow(
                label = strings.profileStatsEpisodesWatchedLabel,
                value = stats.episodesOrChapters.toString(),
                onClick = null,
            )
        }
    }
}

/** The profile distribution entries as the shared panel's primitive shape. */
private fun List<ProfileStatDistributionEntry>.toEntries(): List<DistributionEntry> =
    map { DistributionEntry(label = it.label, count = it.count) }

package com.luum.michi.app.discover.ui.explore

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaGenres
import com.luum.michi.app.discover.domain.model.ExploreFilters
import com.luum.michi.app.discover.domain.model.exploreFormats
import com.luum.michi.app.discover.domain.model.exploreSeasonOptions
import com.luum.michi.app.discover.domain.model.exploreYears
import com.luum.michi.app.ui.components.AppButton
import com.luum.michi.app.ui.components.ButtonEmphasis
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.language.Strings

/**
 * Explore filter sheet. It edits a local draft (passed in / out) — no request is
 * fired per tap; the caller commits the draft once when the sheet closes. Groups:
 * genres and formats (multi), year and season (single; season is anime-only),
 * and "In my list" (single; session only). "Hentai" joins the genre list only
 * while the 18+ gate allows adult content. Reset restores the draft to empty.
 */
@Composable
internal fun ExploreFilterSheet(
    draft: ExploreFilters,
    onDraftChange: (ExploreFilters) -> Unit,
    onReset: () -> Unit,
    showSeason: Boolean,
    hasSession: Boolean,
    hideAdult: Boolean,
    onDismiss: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    val genres = remember(hideAdult) {
        if (hideAdult) MediaGenres else MediaGenres + "Hentai"
    }
    ModalSheet(
        title = strings.filterAction,
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        modifier = modifier,
        footer = {
            AppButton(
                label = strings.filterResetAction,
                onClick = onReset,
                emphasis = ButtonEmphasis.Secondary,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        OptionGroup(title = strings.exploreFilterGenreLabel) {
            genres.forEachIndexed { index, genre ->
                OptionRow(
                    label = genre,
                    selected = genre in draft.genres,
                    onClick = { onDraftChange(draft.copy(genres = draft.genres.toggled(genre))) },
                    divider = index != 0,
                    role = Role.Checkbox,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = strings.formatLabel) {
            exploreFormats(strings).forEachIndexed { index, option ->
                OptionRow(
                    label = option.label,
                    selected = option.value in draft.formats,
                    onClick = { onDraftChange(draft.copy(formats = draft.formats.toggled(option.value))) },
                    divider = index != 0,
                    role = Role.Checkbox,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = strings.exploreFilterYearLabel) {
            exploreYears().forEachIndexed { index, year ->
                OptionRow(
                    label = year?.toString() ?: strings.exploreAnyYearLabel,
                    selected = draft.year == year,
                    onClick = { onDraftChange(draft.copy(year = year)) },
                    divider = index != 0,
                )
            }
        }
        if (showSeason) {
            Spacer(modifier = Modifier.height(16.dp))
            OptionGroup(title = strings.seasonLabel) {
                exploreSeasonOptions(strings).forEachIndexed { index, option ->
                    val seasonId = option.id.takeIf { it != ANY_SEASON_ID }
                    OptionRow(
                        label = option.label,
                        selected = draft.season == seasonId,
                        onClick = { onDraftChange(draft.copy(season = seasonId)) },
                        divider = index != 0,
                    )
                }
            }
        }
        if (hasSession) {
            Spacer(modifier = Modifier.height(16.dp))
            OptionGroup(title = strings.exploreOnListTitle) {
                OptionRow(
                    label = strings.exploreOnListAny,
                    selected = draft.onList == null,
                    onClick = { onDraftChange(draft.copy(onList = null)) },
                    divider = false,
                )
                OptionRow(
                    label = strings.exploreOnListYes,
                    selected = draft.onList == true,
                    onClick = { onDraftChange(draft.copy(onList = true)) },
                )
                OptionRow(
                    label = strings.exploreOnListNo,
                    selected = draft.onList == false,
                    onClick = { onDraftChange(draft.copy(onList = false)) },
                )
            }
        }
    }
}

private const val ANY_SEASON_ID = "any"

private fun List<String>.toggled(item: String): List<String> =
    if (item in this) this - item else this + item

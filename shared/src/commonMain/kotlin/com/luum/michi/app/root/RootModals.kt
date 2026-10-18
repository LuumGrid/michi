package com.luum.michi.app.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.platform.domain.ShareLauncher
import com.luum.michi.app.core.platform.domain.UrlOpener
import com.luum.michi.app.discover.domain.genre.model.genreAniListUrl
import com.luum.michi.app.discover.domain.genre.model.genreShareText
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.aniListUrl
import com.luum.michi.app.mediaDetail.domain.media.model.shareText
import com.luum.michi.app.mediaDetail.domain.character.model.aniListUrl as characterAniListUrl
import com.luum.michi.app.mediaDetail.domain.character.model.shareText as characterShareText
import com.luum.michi.app.mediaDetail.domain.staff.model.aniListUrl as staffAniListUrl
import com.luum.michi.app.mediaDetail.domain.staff.model.shareText as staffShareText
import com.luum.michi.app.mediaDetail.domain.studio.model.aniListUrl as studioAniListUrl
import com.luum.michi.app.mediaDetail.domain.studio.model.shareText as studioShareText
import com.luum.michi.app.mediaDetail.ui.media.MediaEntryEditorSheet
import com.luum.michi.app.mediaDetail.ui.media.state.createMediaEntryEditorState
import com.luum.michi.app.root.state.State
import com.luum.michi.app.settings.domain.model.formatScoreValue
import com.luum.michi.app.settings.domain.model.parseScoreValue
import com.luum.michi.app.settings.domain.model.scoreMaxFor
import com.luum.michi.app.settings.domain.model.scoreStepFor
import com.luum.michi.app.settings.domain.model.scoreSuffixFor
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.ActionRow
import com.luum.michi.app.ui.components.DetailGridSortOptions
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.SortSheet

/**
 * The modals that float over the shell content: sign-in, the entry editor, the
 * detail grid sort sheet and the detail overflow menu. Emitted inline in Root's
 * content Box (no wrapper), in their original order.
 */
@Composable
internal fun RootModals(
    state: State,
    holders: RootHolders,
    viewerId: Int?,
    settingsState: SettingsState,
    authServices: List<AuthService>,
    isAuthConfigured: Boolean,
    mediaListEntryRepository: MediaListEntryRepository,
    mediaDetailRepository: MediaDetailRepository,
    urlOpener: UrlOpener,
    shareLauncher: ShareLauncher,
    strings: LanguageStrings,
) {
    // Editor score display in the user's format (translation lives in
    // settings pure functions so mediaDetail stays primitive-only).
    val editorScoreFormat = settingsState.scoreFormat
    val formatScore: (Float) -> String = { formatScoreValue(editorScoreFormat, it) }
    val parseScore: (String) -> Float? = { parseScoreValue(editorScoreFormat, it) }
    val scoreSuffix: String? = scoreSuffixFor(editorScoreFormat)
    val scoreMax: Float = scoreMaxFor(editorScoreFormat)
    val scoreStep: Float = scoreStepFor(editorScoreFormat)

    if (state.isSignInOpen) {
        AuthModal(
            services = authServices,
            isConfigured = isAuthConfigured,
            configMissingLabel = strings.authConfigurationMissing,
            language = settingsState.language,
            onLanguageChange = { settingsState.language = it },
            guestLabel = strings.authContinueAsGuestAction,
            onDismiss = { state.closeSignIn() },
        )
    }
    val editorId = state.editorMediaId
    if (state.isEditorOpen && editorId != null && viewerId != null) {
        val editor = remember(editorId) {
            createMediaEntryEditorState(
                mediaId = editorId,
                entryRepository = mediaListEntryRepository,
                detailRepository = mediaDetailRepository,
                scope = holders.listScope,
                initialStatusOverride = state.editorInitialStatus,
                initialProgressOverride = state.editorInitialProgress,
                advancedScoringEnabled = settingsState.advancedScoring,
                advancedScoringAnimeNames = settingsState.advancedScoringAnime,
                advancedScoringMangaNames = settingsState.advancedScoringManga,
                scoreMax = scoreMax,
                onFavouriteChanged = { id, value -> holders.mediaDetail.applyFavouriteChange(id, value) },
            )
        }
        LaunchedEffect(editorId) { editor.load() }
        MediaEntryEditorSheet(
            editor = editor,
            formatScore = formatScore,
            parseScore = parseScore,
            scoreSuffix = scoreSuffix,
            scoreStep = scoreStep,
            onSaved = {
                state.closeEditor()
                refreshLists(holders, viewerId, settingsState)
                holders.mediaDetail.onEntryChanged(editorId)
            },
            onDeleted = {
                state.closeEditor()
                refreshLists(holders, viewerId, settingsState)
                holders.mediaDetail.onEntryChanged(editorId)
            },
            onDismiss = { state.closeEditor() },
        )
    }
    val gridSortHolder = if (state.isDetailOpen && state.isDetailSortOpen) {
        holders.gridSortHolderFor(state.currentDetail)
    } else {
        null
    }
    if (gridSortHolder != null) {
        // Shared sort sheet over the destination's grid holder (studio
        // or genre): picking a row re-queries the grid from page 1
        // (server order).
        SortSheet(
            options = DetailGridSortOptions,
            selected = gridSortHolder.sortOption,
            onSelect = { gridSortHolder.changeSort(it, gridSortHolder.sortOrder) },
            order = gridSortHolder.sortOrder,
            onSelectOrder = { gridSortHolder.changeSort(gridSortHolder.sortOption, it) },
            onDismiss = { state.closeDetailSort() },
        )
    }
    val detailMenu = detailOverflowFor(state.currentDetail, holders)
    if (state.isDetailOpen && state.isDetailMenuOpen && detailMenu != null) {
        ModalSheet(
            title = detailMenu.title,
            dismissLabel = strings.dismissAction,
            onDismiss = { state.closeDetailMenu() },
        ) {
            OptionGroup(title = null) {
                ActionRow(
                    label = strings.mediaDetailOpenInAniListAction,
                    onClick = {
                        state.closeDetailMenu()
                        urlOpener.open(detailMenu.url)
                    },
                    divider = false,
                )
                ActionRow(
                    label = strings.mediaDetailShareAction,
                    onClick = {
                        state.closeDetailMenu()
                        shareLauncher.share(text = detailMenu.shareText, subject = detailMenu.title)
                    },
                )
            }
        }
    }
}

// Editor save/delete land here: both lists re-fetch so the edited
// entry (possibly moved sections) shows the truth. Rare action,
// two queries, zero staleness.
private fun refreshLists(holders: RootHolders, viewerId: Int?, settingsState: SettingsState) {
    val id = viewerId ?: return
    holders.anime?.load(id, forceRefresh = true, splitCompleted = settingsState.splitCompletedAnime)
    holders.manga?.load(id, forceRefresh = true, splitCompleted = settingsState.splitCompletedManga)
    // A saved/edited entry changes the calendar's user status/score.
    holders.calendar.invalidateEntries()
}

// Overflow menu target per active destination: title + canonical AniList
// URL + share body. Null while the detail has not hydrated yet.
private fun detailOverflowFor(destination: DetailDestination?, holders: RootHolders): DetailOverflow? =
    when (destination) {
        is DetailDestination.Media -> holders.mediaDetail.detail?.let {
            DetailOverflow(it.title, it.aniListUrl(), it.shareText())
        }
        is DetailDestination.Character -> holders.character.detail?.let {
            DetailOverflow(it.name, it.characterAniListUrl(), it.characterShareText())
        }
        is DetailDestination.Staff -> holders.staff.detail?.let {
            DetailOverflow(it.name, it.staffAniListUrl(), it.staffShareText())
        }
        is DetailDestination.Studio -> holders.studio.detail?.let {
            DetailOverflow(it.name, it.studioAniListUrl(), it.studioShareText())
        }
        // A genre is not an entity: the closest canonical page is the catalog
        // search filtered by genre, so it is ready before anything hydrates. The
        // URL follows the catalog chip the user has selected.
        is DetailDestination.Genre -> DetailOverflow(
            destination.name,
            genreAniListUrl(destination.name, holders.genre.type),
            genreShareText(destination.name, holders.genre.type),
        )
        is DetailDestination.DetailList -> null
        null -> null
    }

/** Per-destination overflow payload: what the detail menu opens/shares. */
private data class DetailOverflow(
    val title: String,
    val url: String,
    val shareText: String,
)

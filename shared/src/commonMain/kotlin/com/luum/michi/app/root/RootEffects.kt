package com.luum.michi.app.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.medialist.domain.SortPersistence
import com.luum.michi.app.core.medialist.domain.SortScope
import com.luum.michi.app.core.medialist.domain.hydrateSort
import com.luum.michi.app.core.medialist.domain.resolveInitialSort
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.mediaList.ui.common.state.MediaListHolder
import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.State
import com.luum.michi.app.root.state.detailSurfaceKeys
import com.luum.michi.app.settings.domain.model.toUserSort
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.PlatformBackHandler

/**
 * Every `Root` effect in its original order: system back, settings refresh,
 * sign-in lifecycle, search focus, session transitions, list/dashboard/detail
 * loads and the sort memory. The holder creation lives in [rememberRootHolders];
 * this composable only reacts to it.
 */
@Composable
internal fun RootEffects(
    state: State,
    holders: RootHolders,
    settingsState: SettingsState,
    viewer: Viewer?,
    language: AppLanguage,
    sortPersistence: SortPersistence,
    surfaceStates: SaveableStateHolder,
    autoOpenSignIn: Boolean,
    onAutoSignInShown: () -> Unit,
    deepLinkMediaId: Int?,
    onDeepLinkHandled: () -> Unit,
) {
    val tab = state.selectedTab

    // System back funnels through the same progressive step as the toolbar
    // chevron. When nothing is left to consume (SystemDefault) the handler is
    // disabled, so the platform closes the app as usual.
    PlatformBackHandler(enabled = state.nextBackStep() != BackStep.SystemDefault) {
        state.applyBackStep()
    }

    // Synced settings load once per session, on first open — never on
    // composition, never for guests (canSync). The holder dedups repeats.
    LaunchedEffect(state.isSettingsOpen) {
        if (state.isSettingsOpen) settingsState.refresh()
    }

    // Sign-in modal lifecycle: opens once on first anonymous launch,
    // closes on login (viewer appears) wherever it was opened from.
    LaunchedEffect(autoOpenSignIn) {
        if (autoOpenSignIn) {
            state.openSignIn()
            onAutoSignInShown()
        }
    }
    LaunchedEffect(viewer) {
        if (viewer != null) state.closeSignIn()
    }

    // Keyboard follows search: every close path (chevron, scrim, system
    // back, tab switch) funnels through searchActiveTab, so releasing focus
    // here dismisses the keyboard in sync with the toolbar transition.
    // No-op when nothing holds focus.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.searchActiveTab) {
        if (state.searchActiveTab == null) {
            focusManager.clearFocus()
        }
    }

    // ── Session transitions ────────────────────────────────────────────────
    // viewerId drives three things: seeding/clearing the synced prefs, resetting
    // the transient navigation state, and re-keying the holders (their favourite
    // state is per-user). It only fires on a real change, so a cold start (or a
    // deep link opened during it) never loses navigation.
    val viewerId = viewer?.id

    // Session change: drop the transient navigation state (the synced prefs are
    // seeded/cleared in App, next to the settings holder) and mark the calendar
    // stale — its rows carry the viewer's status/score.
    var lastViewerId by remember { mutableStateOf(viewerId) }
    LaunchedEffect(viewerId) {
        if (viewerId != lastViewerId) {
            state.reset()
            holders.calendar.invalidateEntries()
            lastViewerId = viewerId
        }
    }

    // Server-mapped titles (userPreferred) and person names only re-map on load:
    // a confirmed language change (syncedGeneration) forces one reload so cards
    // stop showing stale titles. A list that is not visible reloads when it
    // opens, via its pending flag.
    var lastSyncedGeneration by remember(settingsState) { mutableStateOf(settingsState.syncedGeneration) }
    var pendingAnimeReload by remember(settingsState) { mutableStateOf(false) }
    var pendingMangaReload by remember(settingsState) { mutableStateOf(false) }
    LaunchedEffect(
        tab,
        viewerId,
        settingsState.splitCompletedAnime,
        settingsState.splitCompletedManga,
        settingsState.syncedGeneration,
    ) {
        val languageSynced = settingsState.syncedGeneration != lastSyncedGeneration
        if (languageSynced) {
            pendingAnimeReload = true
            pendingMangaReload = true
        }
        // Always load the visible list (the TTL dedups repeats): opening the tab,
        // a new viewer or a split flip must fetch, not only a language change.
        if (tab == TabSection.ANIME && viewerId != null) {
            holders.anime?.load(
                viewerId,
                forceRefresh = languageSynced || pendingAnimeReload,
                splitCompleted = settingsState.splitCompletedAnime,
            )
            pendingAnimeReload = false
        }
        if (tab == TabSection.MANGA && viewerId != null) {
            holders.manga?.load(
                viewerId,
                forceRefresh = languageSynced || pendingMangaReload,
                splitCompleted = settingsState.splitCompletedManga,
            )
            pendingMangaReload = false
        }
        lastSyncedGeneration = settingsState.syncedGeneration
    }

    val detailEntryIds = state.detailEntryIds.toList()
    var lastDetailEntryIds by remember { mutableStateOf(detailEntryIds) }
    LaunchedEffect(detailEntryIds) {
        val gone = lastDetailEntryIds.toSet() - detailEntryIds.toSet()
        // An entry is saved under the tab of the slot that composed it, which
        // may no longer be the selected tab (a deep link or a tab change), so
        // drop every tab variant instead of guessing.
        gone.forEach { id -> detailSurfaceKeys(id).forEach(surfaceStates::removeState) }
        lastDetailEntryIds = detailEntryIds
    }
    // The cached rail meta is localized: a language change drops disk + memory
    // and bumps the holder's generation, which reloads the screen only when it
    // is visible (the screen's effect keys on the generation).
    var lastDashboardLanguage by remember { mutableStateOf(language) }
    LaunchedEffect(language) {
        if (language != lastDashboardLanguage) {
            lastDashboardLanguage = language
            holders.dashboard.invalidate()
        }
    }

    // 18+ gate: flipping it re-queries the open genre page (a no-op while no
    // genre is loaded), so the setting applies without leaving the page.
    LaunchedEffect(settingsState.displayAdultContent) {
        holders.genre.setHideAdult(!settingsState.displayAdultContent)
    }
    // A confirmed language change (titles/staff names) also re-localizes the
    // open detail page and the calendar day; the dashboard re-hydrates when its
    // screen is visible again (invalidate bumps its generation).
    LaunchedEffect(settingsState.syncedGeneration) {
        if (settingsState.syncedGeneration == 0) return@LaunchedEffect
        holders.dashboard.invalidate()
        // Every cached detail snapshot is re-localized: drop all of them so the
        // back stack never restores stale titles, then refresh the visible one.
        holders.detailHolders.forEach { it.invalidateCache() }
        holders.detailHolderFor(state.currentDetail)?.refresh()
        // Always: the calendar rows are re-localized too (open or not).
        holders.calendar.invalidateEntries()
    }
    LaunchedEffect(state.currentDetail) {
        when (val destination = state.currentDetail) {
            is DetailDestination.Media -> holders.mediaDetail.load(destination.id)
            is DetailDestination.Character -> holders.character.load(destination.id)
            is DetailDestination.Staff -> holders.staff.load(destination.id)
            is DetailDestination.Studio -> holders.studio.load(destination.id)
            is DetailDestination.Genre -> holders.genre.load(destination.name, destination.type)
            // The list renders the parent's holder. Re-bind that holder to
            // [parentId] because an item opened from the list may have swapped
            // it for another media/character/staff; `load` is a no-op when the
            // parent is already current (or restores its snapshot from cache).
            is DetailDestination.DetailList -> when (destination.kind) {
                DetailListKind.MEDIA_CHARACTERS,
                DetailListKind.MEDIA_STAFF,
                DetailListKind.MEDIA_RELATED,
                DetailListKind.MEDIA_RECOMMENDATIONS -> {
                    holders.mediaDetail.load(destination.parentId)
                    // Recommendations are a per-visit query, not cached: refetch
                    // the parent's page 1 so the "View all" list is not empty.
                    if (destination.kind == DetailListKind.MEDIA_RECOMMENDATIONS) {
                        holders.mediaDetail.loadRecommendations()
                    }
                }
                DetailListKind.CHARACTER_MEDIA -> holders.character.load(destination.parentId)
                DetailListKind.STAFF_MEDIA,
                DetailListKind.STAFF_CHARACTERS -> holders.staff.load(destination.parentId)
            }
            null -> {}
        }
    }

    // Deep link (anilist.co/anime|manga/{id}): open the media detail once the
    // shell composes, then clear the pending id so it never reopens.
    LaunchedEffect(deepLinkMediaId) {
        deepLinkMediaId?.let {
            state.openMedia(it)
            onDeepLinkHandled()
        }
    }

    // Sort memory (Settings persist toggle, per tab) + Default sort:
    // saved persist-sort wins, then the default, then holder defaults.
    // Changing Default sort applies live (effect key); changing it while a
    // saved sort exists leaves that tab on the saved sort. Clearing on
    // toggle-off so stale values never resurrect. Filters never persist.
    val persistSort = settingsState.persistListSort
    val defaultSort = settingsState.listSort
    LaunchedEffect(viewerId, persistSort, defaultSort) {
        if (viewerId == null) return@LaunchedEffect
        if (!persistSort) {
            sortPersistence.clearSort(SortScope.ANIME)
            sortPersistence.clearSort(SortScope.MANGA)
        }
        val sortTargets: List<Pair<SortScope, MediaListHolder<*, *>?>> = listOf(
            SortScope.ANIME to holders.anime,
            SortScope.MANGA to holders.manga,
        )
        sortTargets.forEach { (scope, holder) ->
            val initial = resolveInitialSort(
                saved = hydrateSort(sortPersistence, scope).takeIf { persistSort },
                defaultSort = defaultSort.toUserSort(),
            )
            holder?.updateSort(initial.option, initial.order, persist = initial.persist)
        }
    }
}

package com.luum.michi.app.root.state

import com.luum.michi.app.core.navigation.domain.TabSection

/**
 * Saved-surface key of a detail-stack entry: the tab of the `AnimatedContent`
 * **slot** that composes it plus the entry identity.
 *
 * The slot tab is part of the key because a tab transition composes two slots at
 * once; two providers with the same key would crash ("Key was used multiple
 * times"), so the key cannot be built from the entry alone.
 */
internal fun detailSurfaceKey(tab: TabSection, entryId: Long): String = "${tab.name}:detail:$entryId"

/**
 * Every key an entry could have been saved under — one per tab, since the slot
 * that composed it (and therefore the key) may not be the tab selected now.
 * Pruning uses this so a stale entry can never leak its saved state.
 */
internal fun detailSurfaceKeys(entryId: Long): List<String> =
    TabSection.entries.map { detailSurfaceKey(it, entryId) }

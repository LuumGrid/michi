package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.discover.domain.model.DashboardMediaType

/**
 * One-time cleanup of the removed dashboard disk cache. The old
 * [SettingsStore]-backed cache wrote a multi-KB JSON payload per media type and
 * 18+ gate under `dashboard_<type>_<gate>`; Discover is now memory-only, so
 * those keys are blanked on startup. Reads as absent everywhere (there is no
 * `remove` on [SettingsStore]), and only non-empty keys are written, so a normal
 * launch never touches the store.
 */
internal fun clearLegacyDashboardCache(store: SettingsStore) {
    DashboardMediaType.entries.forEach { type ->
        listOf(true, false).forEach { hideAdult ->
            val key = "dashboard_${type.name}_$hideAdult"
            if (store.getString(key)?.isNotEmpty() == true) {
                store.putString(key, "")
            }
        }
    }
}

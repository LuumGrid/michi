package com.luum.michi.app.discover.domain

import com.luum.michi.app.discover.domain.model.DashboardMediaType

/** A persisted dashboard feed plus when it was written (for TTL revalidation). */
internal data class CachedDashboardFeed(
    val feed: DashboardFeed,
    val savedAtMillis: Long,
)

/**
 * Disk cache for the dashboard rails, keyed by media type **and** the 18+ gate
 * (a change of gate never reuses the other key's data). Enables an instant,
 * spinner-free rail on a cold start, revalidated when stale.
 */
internal interface DashboardFeedCache {
    fun load(mediaType: DashboardMediaType, hideAdult: Boolean): CachedDashboardFeed?
    fun save(mediaType: DashboardMediaType, hideAdult: Boolean, feed: DashboardFeed)

    /** Drop every key (e.g. the localized `meta` goes stale on a language change). */
    fun clear()
}

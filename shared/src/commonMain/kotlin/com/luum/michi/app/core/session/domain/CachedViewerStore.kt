package com.luum.michi.app.core.session.domain

/**
 * Persists the last resolved [Viewer] so a cold start can enter the shell
 * immediately (offline/launch) and refresh in the background. Only the fields
 * the shell needs are stored; `about` and the mod/donator flags stay at their
 * defaults until the refresh lands.
 */
internal interface CachedViewerStore {
    fun load(): Viewer?
    fun save(viewer: Viewer)
    fun clear()
}

package com.luum.michi.app.core.navigation.domain

/**
 * Read/refresh surface shared by every pushed detail page (media, character,
 * staff, studio, genre), so `Root` can re-localize, re-fetch and toggle the
 * favourite of the active destination without a five-branch `when` per action.
 * Pure Kotlin: no Compose and no feature types.
 */
internal interface DetailHolder {
    /** Re-fetches the visible page (used after a language change). */
    fun refresh()

    /** Drops every cached snapshot: a language change re-localizes all of them. */
    fun invalidateCache()

    /** Whether the entity on screen is favourited (false for a genre). */
    val isFavourite: Boolean

    /** Toggles the favourite of the entity on screen (no-op for a genre). */
    fun toggleFavourite()
}

package com.luum.michi.app.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource

/**
 * Central icon catalog: local composeResources vectors (zero downloads),
 * served as [ImageVector] so every existing slot keeps working unchanged.
 * Grouped by surface; see [LocalDrawables] for the 1:1 file mirror.
 */
internal object AppIcons {
    // ---- Toolbar: floating actions + in-context search ----
    // Glass circles/capsule of Toolbar and SearchField. Generic primitives:
    // any surface may use them without coupling destinations.
    val Search: ImageVector
        @Composable get() = vectorResource(LocalDrawables.search)

    /** Chevron-left: the modern back (cancels search, standard back). */
    val Back: ImageVector
        @Composable get() = vectorResource(LocalDrawables.keyboardArrowLeft)

    /** X: clears the search query. */
    val Clear: ImageVector
        @Composable get() = vectorResource(LocalDrawables.close)

    /** Gear: trailing action of the Account tab. */
    val Settings: ImageVector
        @Composable get() = vectorResource(LocalDrawables.settings)

    // ---- TabBar: the 4 top-level destinations ----
    // Vectors live here, but the section->icon MAPPING belongs to
    // root/Root.kt::tabs() — ui holds the catalog, never decides destinations.
    val Discover: ImageVector
        @Composable get() = vectorResource(LocalDrawables.explore)

    val Anime: ImageVector
        @Composable get() = vectorResource(LocalDrawables.tv)

    val Manga: ImageVector
        @Composable get() = vectorResource(LocalDrawables.manga)

    val Account: ImageVector
        @Composable get() = vectorResource(LocalDrawables.person)
}

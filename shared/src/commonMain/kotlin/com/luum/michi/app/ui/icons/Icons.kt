package com.luum.michi.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.luum.michi.app.ui.icons.generated.Anilist
import com.luum.michi.app.ui.icons.generated.AutoAwesomeMosaic
import com.luum.michi.app.ui.icons.generated.Close
import com.luum.michi.app.ui.icons.generated.Explore
import com.luum.michi.app.ui.icons.generated.KeyboardArrowLeft
import com.luum.michi.app.ui.icons.generated.Language
import com.luum.michi.app.ui.icons.generated.Manga
import com.luum.michi.app.ui.icons.generated.IconPack
import com.luum.michi.app.ui.icons.generated.Person
import com.luum.michi.app.ui.icons.generated.Search
import com.luum.michi.app.ui.icons.generated.Settings
import com.luum.michi.app.ui.icons.generated.Tv

/**
 * Central icon catalog: Valkyrie-generated [IconPack] ([ImageVector] code,
 * no resource system involved), curated here by surface with stable generic
 * names — call sites never touch the generated pack directly, so renaming
 * or swapping an icon is a one-line change below.
 * Grouped by surface; root/Root.kt::tabs() owns the section->icon mapping.
 */
internal object AppIcons {
    // ---- Toolbar: floating actions + in-context search ----
    // Glass circles/capsule of Toolbar and SearchField. Generic primitives:
    // any surface may use them without coupling destinations.
    val Search: ImageVector
        get() = IconPack.Search

    /** Chevron-left: the modern back (cancels search, standard back). */
    val Back: ImageVector
        get() = IconPack.KeyboardArrowLeft

    /** X: clears the search query. */
    val Clear: ImageVector
        get() = IconPack.Close

    /** Gear: trailing action of the Account tab. */
    val Settings: ImageVector
        get() = IconPack.Settings

    /** Globe: app-language switch. */
    val Language: ImageVector
        get() = IconPack.Language

    /** Style mosaic: theme (palette + mode) switch. */
    val Theme: ImageVector
        get() = IconPack.AutoAwesomeMosaic

    // ---- TabBar: the 4 top-level destinations ----
    // Vectors live in the generated pack, but the section->icon MAPPING
    // belongs to root/Root.kt::tabs() — ui holds the catalog, never decides
    // destinations.
    val Discover: ImageVector
        get() = IconPack.Explore

    val Anime: ImageVector
        get() = IconPack.Tv

    val Manga: ImageVector
        get() = IconPack.Manga

    val Account: ImageVector
        get() = IconPack.Person

    // ---- Brand ----
    /** AniList mark. Tint it for mono use (e.g. white on the brand button). */
    val AniList: ImageVector
        get() = IconPack.Anilist
}

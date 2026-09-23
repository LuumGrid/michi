package com.luum.michi.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.luum.michi.app.ui.icons.generated.Anilist
import com.luum.michi.app.ui.icons.generated.CalendarMonth
import com.luum.michi.app.ui.icons.generated.Close
import com.luum.michi.app.ui.icons.generated.Edit
import com.luum.michi.app.ui.icons.generated.Explore
import com.luum.michi.app.ui.icons.generated.Favorite
import com.luum.michi.app.ui.icons.generated.FavoriteFilled
import com.luum.michi.app.ui.icons.generated.FilterList
import com.luum.michi.app.ui.icons.generated.KeyboardArrowLeft
import com.luum.michi.app.ui.icons.generated.KeyboardArrowRight
import com.luum.michi.app.ui.icons.generated.Language
import com.luum.michi.app.ui.icons.generated.Manga
import com.luum.michi.app.ui.icons.generated.IconPack
import com.luum.michi.app.ui.icons.generated.MoreVert
import com.luum.michi.app.ui.icons.generated.Notifications
import com.luum.michi.app.ui.icons.generated.Palette
import com.luum.michi.app.ui.icons.generated.Person
import com.luum.michi.app.ui.icons.generated.Search
import com.luum.michi.app.ui.icons.generated.Settings
import com.luum.michi.app.ui.icons.generated.SortList
import com.luum.michi.app.ui.icons.generated.StarFilled
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

    /** Palette: theme (palette + mode + font) switch. */
    val Theme: ImageVector
        get() = IconPack.Palette

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

    // ---- Lists: filter + sort share one capsule (lone search circle aside) ----
    /** Funnel-list: opens the list filter sheet (anime/manga rails). */
    val Filter: ImageVector
        get() = IconPack.FilterList

    /** Up-down arrows: opens list sort (anime/manga rails). */
    val Sort: ImageVector
        get() = IconPack.SortList

    // ---- Detail: back is ToolbarNavigation, these are trailing actions ----
    /** Pencil: opens the list-entry editor. */
    val Edit: ImageVector
        get() = IconPack.Edit

    /** Heart outline: adds to favorites (see FavoriteFilled for the on-state). */
    val Favorite: ImageVector
        get() = IconPack.Favorite

    /** Heart filled: the favorited on-state (toggle with Favorite). */
    val FavoriteFilled: ImageVector
        get() = IconPack.FavoriteFilled

    /** Vertical ellipsis: detail overflow menu. */
    val More: ImageVector
        get() = IconPack.MoreVert

    // ---- Misc: catalogued, wired when their surface lands ----
    /** Chevron-right: rows that open a picker sheet. */
    val ChevronRight: ImageVector
        get() = IconPack.KeyboardArrowRight

    /** Filled star: the user's own rating (global averages use the outline twin). */
    val Star: ImageVector
        get() = IconPack.StarFilled

    /** Calendar: seasonal/schedule surfaces. */
    val Calendar: ImageVector
        get() = IconPack.CalendarMonth

    /** Bell: opens the notifications overlay (trigger lives in account). */
    val Notifications: ImageVector
        get() = IconPack.Notifications
}

package com.luum.michi.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.luum.michi.app.ui.icons.generated.Anilist
import com.luum.michi.app.ui.icons.generated.AnimeRounded
import com.luum.michi.app.ui.icons.generated.AnimeRoundedFilled
import com.luum.michi.app.ui.icons.generated.BackRounded
import com.luum.michi.app.ui.icons.generated.CalendarRounded
import com.luum.michi.app.ui.icons.generated.ChevronRightRounded
import com.luum.michi.app.ui.icons.generated.ClearRounded
import com.luum.michi.app.ui.icons.generated.EditRounded
import com.luum.michi.app.ui.icons.generated.ExpandLessRounded
import com.luum.michi.app.ui.icons.generated.ExpandMoreRounded
import com.luum.michi.app.ui.icons.generated.ExploreRounded
import com.luum.michi.app.ui.icons.generated.ExploreRoundedFilled
import com.luum.michi.app.ui.icons.generated.FavoriteRounded
import com.luum.michi.app.ui.icons.generated.FavoriteRoundedFilled
import com.luum.michi.app.ui.icons.generated.FilterRounded
import com.luum.michi.app.ui.icons.generated.GroupsRounded
import com.luum.michi.app.ui.icons.generated.IconPack
import com.luum.michi.app.ui.icons.generated.LanguageRounded
import com.luum.michi.app.ui.icons.generated.MangaRounded
import com.luum.michi.app.ui.icons.generated.MangaRoundedFilled
import com.luum.michi.app.ui.icons.generated.MoreRounded
import com.luum.michi.app.ui.icons.generated.NotificationsRounded
import com.luum.michi.app.ui.icons.generated.ProfileRounded
import com.luum.michi.app.ui.icons.generated.ProfileRoundedFilled
import com.luum.michi.app.ui.icons.generated.SearchRounded
import com.luum.michi.app.ui.icons.generated.SettingsRounded
import com.luum.michi.app.ui.icons.generated.SortRounded
import com.luum.michi.app.ui.icons.generated.StarOutlineRounded
import com.luum.michi.app.ui.icons.generated.StarRoundedFilled
import com.luum.michi.app.ui.icons.generated.ThemeRounded

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
        get() = IconPack.SearchRounded

    /** Chevron-left: the modern back (cancels search, standard back). */
    val Back: ImageVector
        get() = IconPack.BackRounded

    /** X: clears the search query. */
    val Clear: ImageVector
        get() = IconPack.ClearRounded

    /** Gear: trailing action of the Profile tab. */
    val Settings: ImageVector
        get() = IconPack.SettingsRounded

    /** Globe: app-language switch. */
    val Language: ImageVector
        get() = IconPack.LanguageRounded

    /** Palette: theme (palette + mode + font) switch. */
    val Theme: ImageVector
        get() = IconPack.ThemeRounded

    // ---- TabBar: the 4 top-level destinations ----
    // Vectors live in the generated pack, but the section->icon MAPPING
    // belongs to root/Root.kt::tabs() — ui holds the catalog, never decides
    // destinations.
    val Discover: ImageVector
        get() = IconPack.ExploreRounded

    /** Filled twin for the active tab (TabBar selected state). */
    val DiscoverSelected: ImageVector
        get() = IconPack.ExploreRoundedFilled

    val Anime: ImageVector
        get() = IconPack.AnimeRounded

    /** Filled twin for the active tab (TabBar selected state). */
    val AnimeSelected: ImageVector
        get() = IconPack.AnimeRoundedFilled

    val Manga: ImageVector
        get() = IconPack.MangaRounded

    /** Filled twin for the active tab (TabBar selected state). */
    val MangaSelected: ImageVector
        get() = IconPack.MangaRoundedFilled

    val Profile: ImageVector
        get() = IconPack.ProfileRounded

    /** Filled twin for the active tab (TabBar selected state). */
    val ProfileSelected: ImageVector
        get() = IconPack.ProfileRoundedFilled

    // ---- Brand ----
    /** AniList mark. Tint it for mono use (e.g. white on the brand button). */
    val AniList: ImageVector
        get() = IconPack.Anilist

    // ---- Lists: filter + sort share one capsule (lone search circle aside) ----
    /** Funnel-list: opens the list filter sheet (anime/manga rails). */
    val Filter: ImageVector
        get() = IconPack.FilterRounded

    /** Up-down arrows: opens list sort (anime/manga rails). */
    val Sort: ImageVector
        get() = IconPack.SortRounded

    // ---- Detail: back is ToolbarNavigation, these are trailing actions ----
    /** Pencil: opens the list-entry editor. */
    val Edit: ImageVector
        get() = IconPack.EditRounded

    /** Heart outline: adds to favorites (see FavoriteFilled for the on-state). */
    val Favorite: ImageVector
        get() = IconPack.FavoriteRounded

    /** Heart filled: the favorited on-state (toggle with Favorite). */
    val FavoriteFilled: ImageVector
        get() = IconPack.FavoriteRoundedFilled

    /** Vertical ellipsis: detail overflow menu. */
    val More: ImageVector
        get() = IconPack.MoreRounded

    // ---- Misc: catalogued, wired when their surface lands ----
    /** Chevron-right: rows that open a picker sheet. */
    val ChevronRight: ImageVector
        get() = IconPack.ChevronRightRounded

    /** Chevron-down: expands an accordion row (year filter decades). */
    val ExpandMore: ImageVector
        get() = IconPack.ExpandMoreRounded

    /** Chevron-up: collapses an accordion row. */
    val ExpandLess: ImageVector
        get() = IconPack.ExpandLessRounded

    /** Filled star: the user's own rating (global averages use the outline twin). */
    val StarFilled: ImageVector
        get() = IconPack.StarRoundedFilled

    /** Outline star: global average ratings (user scores use the filled twin). */
    val StarOutline: ImageVector
        get() = IconPack.StarOutlineRounded

    /** Two people: popularity / member counts on cards. */
    val Groups: ImageVector
        get() = IconPack.GroupsRounded

    /** Calendar: seasonal/schedule surfaces. */
    val Calendar: ImageVector
        get() = IconPack.CalendarRounded

    /** Bell: opens the notifications overlay (trigger lives in profile). */
    val Notifications: ImageVector
        get() = IconPack.NotificationsRounded
}

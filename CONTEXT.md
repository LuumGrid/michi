# Michi Context Instructions

These instructions are for any person working in this repository.

Before designing any AniList feature surface, consult the reference clients:
- **AL-Chan** (Android/Kotlin): https://github.com/zend10/AL-chan
- **Otraku** (Flutter/Dart): https://github.com/lotusprey/otraku
- **AniHyou** (iOS/Swift): https://github.com/axiel7/AniHyou-iOS
- **MyAniList** (iOS/Swift): https://apps.apple.com/us/app/myanilist/id741257899

## Project Identity

- Gradle root project: `Michi`.
- Kotlin Multiplatform structure: `androidApp`, `shared`, `iosApp`.
- Android namespace/applicationId: `com.luum.michi.app`.
- Shared Android library namespace: `com.luum.michi.app.shared`.
- Compose resources package: `com.luum.michi.app.resources`.
- iOS framework name: `Shared`.
- Product: AniList client for anime/manga discovery, lists, profile, feed, search, details, and settings.

## Current Stack

- Kotlin Multiplatform with Compose Multiplatform shared UI.
- Material 3.
- Coil 3 for image loading.
- Manual state and dependency wiring; state holders are plain classes that own Compose-backed state.
- Android minSdk: `34` (from `gradle/libs.versions.toml`).
- Koin, Ktor, DataStore, and formal navigation are target technologies, not current dependencies.

## Architecture Pattern

Feature-layer architecture with pragmatic Clean Architecture inside features when behavior justifies it:

```text
feature/
  domain/         pure models and repository contracts        (add when real behavior arrives)
  data/           implementations, DTOs, mappers              (add when real behavior arrives)
  presentation/
    model/        UI models + derived helpers (extension funcs, label/formatters)
    sample/       hardcoded sample data (`*SampleEntries`, `*SampleStats`, `*SampleFavorites`)
    state/        plain-class state holders + `remember*` factory
    components/   feature-scoped composables (cards, sheets, sections, chips)
    util/         pure helpers (validators, math, encoders)
    {Feature}Screen.kt   = Screen (wires holder) + Content (stateless)
```

Add `domain/` and `data/` only when real AniList/session/storage behavior appears. Do not pre-create empty layers or `placeholder.txt` files.

### Feature-group exceptions: `discover/`, `mediaList/` and `mediaDetail/`

`mediaList/` and `mediaDetail/` already render UI (`AnimeListScreen`, `MangaListScreen`, `MediaEntryEditorSheet`, `MediaDetailScreen`, `CharacterDetailScreen`, `StaffDetailScreen`, `StudioDetailScreen`); `settings/` renders its full screen. The groups below are domain + repository + state holders; remaining screens land later without restructuring.

`discover/` is a feature-group with two sub-scopes (`dashboard/` and `explore/`) sharing one `domain/` and one `repository/`:
```text
discover/
  domain/               shared repo contracts (DashboardRepository, ExploreRepository) + model/
  repository/           one impl per scope (queries are NOT merged)
  ui/
    dashboard/state/    DashboardStateHolder
    explore/state/      ExploreStateHolder
```

Dashboard and Explore never import each other directly — only via `domain/`. This is a one-off exception for this pair of scopes (genuinely coupled: two angles of the same catalog). Every other feature in the app stays flat, with no sub-scopes.

`mediaList/` is the second feature-group, same pattern, for the user's anime/manga lists (`anime/` and `manga/` sub-scopes):

```text
mediaList/
  domain/
    anime/              AnimeListRepository + model/ (entry, section, `completedSection()`)
    manga/              MangaListRepository + model/ (entry, section, `isVolumeBased()`)
    common/             shared list machinery (MediaListLoader, `matchesMediaListFilters`, sorting)
  repository/
    anime/              `toAnimeListEntry` mappers, AnimeListRepositoryImpl (own GraphQL query)
    manga/              `toMangaListEntry` mappers, MangaListRepositoryImpl (own GraphQL query)
  ui/
    anime/state/        AnimeListStateHolder
    manga/state/        MangaListStateHolder
```

`anime/` and `manga/` never import each other directly. Genuinely different things stay split: media formats, list sections (anime splits COMPLETED by format, manga doesn't), GraphQL queries, and progress-increment logic (one counter vs chapters+volumes).

`mediaDetail/` is the third feature-group, same pattern, for detail scopes (`media/`, `character/`, `studio/` and `staff/` sub-scopes, each with the same `loadDetail`-shaped contract):

```text
mediaDetail/
  domain/
    media/              MediaDetailRepository + model/ (detail, relations, recommendations)
    character/          CharacterDetailRepository + model/
    studio/             StudioDetailRepository + model/
    staff/              StaffDetailRepository + model/
  repository/
    media/              `toDomain` mappers, MediaDetailRepositoryImpl, MediaListEntryRepositoryImpl (own GraphQL queries)
    character/          CharacterDetailRepositoryImpl (own GraphQL query)
    studio/             StudioDetailRepositoryImpl (own GraphQL query)
    staff/              StaffDetailRepositoryImpl (own GraphQL query)
  ui/
    media/state/        MediaDetailStateHolder (+ MediaEntryEditorState)
    character/state/    CharacterDetailStateHolder
    studio/state/       StudioDetailStateHolder
    staff/state/        StaffDetailStateHolder
```

`media/`, `character/`, `studio/` and `staff/` never import each other directly. Cross-scope UI lives in `mediaDetail/ui/components/` (`MediaDetailPanel`, `DetailListScreen`, the `toRailItem` mappers, `MediaDetailStateContainer`, `MediaDetailPersonHero`, `MediaDetailVoiceLanguage*`); scopes only ship their own screens and scope-specific components. The horizontal rail (`MediaRail` + `MediaRailItem`) and the shared `SectionHeader` live in the app-wide `ui/components/` because Discover's dashboard reuses them.

## State Pattern

- A stateful feature owns a `*StateHolder` (plain Kotlin class) holding query state in plain vars, exposing derived read-only views and intent-style mutations (`load`, `refresh`, filters, edits). Real examples: `ExploreStateHolder`, `AnimeListStateHolder`, `ProfileStateHolder`, `CalendarStateHolder`.
- The holder is constructed with its repository (plus scope/store where needed); several have a pure `create*` factory for tests (`createProfileStateHolder`, `createCalendarStateHolder`, `createNotificationsStateHolder`, `createSettingsState`). Only `SettingsState` is Compose-backed today (its async load/save/error drives UI recomposition) with a `rememberSettingsState()` Composable factory.
- `SettingsState` is a Compose-backed holder (`settings/ui/state/`) over `SettingsRepository` + `SettingsStore` (hydrate + debounced save, `canSync` gate, `refresh()` deduped per session with `force` retry). `App` creates one per session via `rememberSettingsState()` (keyed by auth flip) and passes it to `Root`, which calls `refresh()` on first open (`LaunchedEffect(isSettingsOpen)`) and forwards it to `SettingsScreen`; guests get `canSync = false` (local-only, never touches the network).
- **Direction (parked): overlay stack.** `State` currently grows one `isXxxOpen` boolean per overlay (plus editor fields and `detailStack`). When the first overlay-over-overlay case appears (e.g. editor over detail) or flags reach ~12, migrate to `overlayStack: SnapshotStateList<Overlay>` (sealed: Explore, Calendar, Notifications, Editor(...), ShareProfile) so back pops one at a time (true progressive back — today `CloseOverlay` closes everything at once) and `nextBackStep`/`applyBackStep` become exhaustive `when`s (compiler catches new overlays). Keep `openX()`/`closeX()` as thin wrappers so callers do not churn.

## Dependency Rules

- `feature -> core`: allowed.
- `feature -> ui`: allowed.
- `ui -> core`: allowed.
- `root -> feature`: allowed.
- `feature -> feature`: not allowed.
- `core -> feature`: not allowed.
- `core -> ui`: not allowed (core has zero Compose imports; enforced by grep, see validation).
- `ui -> feature`: not allowed.
- `feature -> root`: not allowed.

Cross-feature coordination belongs in `root` or neutral `core` contracts.

Practical test before placing code: *"If I deleted feature X tomorrow, would this file break?"* If yes, it does not belong in `core/` or `ui/` — it belongs in feature X.

## Layer Map (`core/` vs `ui/`)

`core/` is infrastructure + neutral domain, organized feature-first like `app/`
(each concept owns its layers; no empty layers). No Compose, no UI types
(not even `Color` in models — palette travels as the raw hex `String`,
converted at the UI boundary). Contents:

```text
core/
  auth/domain|repository/       AniList OAuth flow + token storage (launchers, stores)
  session/domain|repository/    viewer identity, SessionManager, SessionState
  network/domain|repository/    Ktor GraphQL client, NetworkResult, rate limiter,
                                endpoints + anilist/dto/ (API transport shapes shared by
                                several features — never import these from UI directly,
                                map at feature boundaries)
  storage/domain/               settings/filter keys and stores
  language/domain/              string tables only (LanguageStrings, EN/ES, AppLanguage) — no CompositionLocal
  navigation/domain/            routes, back-handler contracts, url-opening + sharing (`UrlOpener`/`isWebUrl`, `ShareLauncher`), AniList deep-link parsing (`parseAniListMediaId`); platform openers/launchers in `repository/`
  medialist/domain/             list-status contracts shared by list features
  model/                        neutral domain vocabulary (MediaFormat, MediaSeason, dates,
                                sort, FilterOption, compact count) — cross-concept, belongs to no feature
  util/                         pure helpers (HTML stripping, …)
```

`ui/` is the shared Compose UI. Everything visual and reusable lives here, nothing else:

```text
ui/
  components/   shared composables (Toolbar, TabBar, SearchField, MessagePanel, ChipRail, DatePickerField, …) — no layer prefix, package disambiguates
  icons/        shared toolbar primitives (AppIcons: Search, Back, Clear)
  language/     LanguageProvider (CompositionLocal) + searchHintFor()
  theme/        Theme, ThemeColors seeds (Default/Ocean/Sakura/Forest/Amber/Violet/Mono) + global PaletteStyle, ThemeProvider
```

UI is 100% shared Compose Multiplatform in the `shared` module — no native
SwiftUI/UIKit surfaces are planned (`iosApp` is only the entry point: token
storage, OAuth launcher, `onOpenURL`). No extraction of `ui/` into its own
Gradle module: single-dev overhead with no benefit. The boundary stays
package-level, enforced by the dependency rules above.

## Package Map

```text
com.luum.michi.app
  App.kt
  root/
    Root.kt                 slim orchestrator (toolbar + tabs + content router)
    AuthModal.kt            on-demand sign-in modal + language picker (MAL appends to AuthServices)
    state/State.kt          BackStep, State, rememberState (+ isSettingsOpen flag, no profile routes)
  core/
    auth/domain|repository/   AniList OAuth flow + token storage
    session/domain|repository/ viewer identity + SessionManager
    network/domain|repository/ Ktor GraphQL client + NetworkResult (+ anilist/dto/)
    storage/domain/           settings/filter stores (+ platform `SettingsStore` impls:
                                `SharedPreferencesSettingsStore` on Android,
                                `NSUserDefaultsSettingsStore` on iOS, wired via `MichiDependencies`)
    language/domain/          string tables only (LanguageStrings, EN/ES, AppLanguage)
    navigation/domain/        routes, back-handler contracts, url-opening + sharing (`UrlOpener`/`isWebUrl`, `ShareLauncher`), deep-link parsing (`parseAniListMediaId`)
    medialist/domain/         list-status contracts
    model/                    neutral domain vocabulary (MediaFormat, MediaSeason, dates, sort)
    util/                     pure helpers
  ui/
    components/             reusable app chrome and UI primitives (shared composables, no prefix)
    language/               LanguageProvider (CompositionLocal) + searchHintFor()
    theme/                  Theme, ThemeColors seeds (Default/Ocean/Sakura/Forest/Amber/Violet/Mono) + global PaletteStyle, ThemeProvider
  profile/                domain(+model)/repository(+mappers)/ui/state holders (no screens yet)
  calendar/               domain(+model)/repository/ui/state holder + CalendarScreen overlay (`CalendarDateBar` at TabBar-identical 24dp with deferred entrance + `CalendarReleaseRow` with shared `ChipRail` platform chips that open urls via `UrlOpener`, + filter sheet, per-day queries with session cache, UTC-safe picker)
  discover/               domain(+model)/repository/ui per dashboard+explore state holders (dashboard renders the DISCOVER tab: type-scoped Anime/Manga rails keyed by `DashboardSectionKind`; Explore search UI pending, Explore honors `hideAdult`)
  mediaList/              domain/{anime,manga,common}/repository/ui per anime+manga state holders (both screens live)
  mediaDetail/            domain+repository/ui per media+character+studio+staff state holders (all four detail screens live: media banner hero + next-episode band + stats row + synopsis + framed information + genre chips + characters/staff rails + trailer + one framed links panel + related/recommendations + tag chips, character person hero + role chips + voice actors (voice-language picker, kept visible when the language has no cast) + animeography, staff person hero + occupation chips + roles + characters, studio poster grid ordered by the server from the shared sort sheet (no client sort, no filters); multi-studios, editor sheet, favourite + share + open-in-AniList toolbar per scope)
  notifications/          domain(+model)/repository/ui/state holder (no screens yet)
  settings/
    domain/               SettingsRepository + model/ (SettingsData, ThemeMode, TitleLanguage,
                          ScoreFormat, ScoreFormatDisplay, ListSort, DiscoverTabOption, NotificationPreferences)
    repository/           SettingsRepositoryImpl (`UserSettings` query + `UpdateUser` mutation)
    ui/SettingsScreen.kt  shell (rows land step by step; no own header)
    ui/state/             SettingsState (Compose-backed) + rememberSettingsState (App-owned instance; rows land step by step)
```

Sub-packages follow the same feature-first rule inside `core/` (`core/auth/`,
`core/network/`, …); `core/model/` and `core/util/` are the explicit exceptions
for cross-concept vocabulary and pure helpers. Feature-level `domain/`/`data/`
appear only when real behavior justifies them.

## Domain Vocabulary

- `animation` / `Animation` for anime.
- `reading` / `Reading` for manga/reading.
- `feed` / `Feed` for AniList activity feed.
- `profile` / `Profile` for user profile surfaces.
- `brandColor()` for app-wide brand primitives (raw hex in, `Color` out, converted at the UI boundary).
- No layer prefixes on class names: shared UI lives in `ui/` (`Toolbar`, `Icons`, `TabBar`), root scope in `root/` (`Root`, `State`). The package is the disambiguator — `ui.Icons` vs feature icons, `root.State` vs any other state.
- `AniList*` for API/client/DTO-specific classes.

## Current State

- `App.kt` owns theme mode/palette/style/font + language via `remember`, hydrated from `SettingsStore` at startup and persisted on every change (unknown values fall back to defaults). It routes loading/shell/error on `SessionState` with an `AnimatedContent` crossfade (same `tabFadeSpec` motion as tabs) over an opaque theme backdrop — no login wall: anonymous users enter `Root` as guests and sign in on demand via `AuthModal` (auto-opens once on first anonymous launch, never after logout). The authenticated `Viewer` enters `Root` so the shell profile draft is real.
- `root` is a slim orchestrator. It wires the session `SettingsState` and composes `Toolbar` + `TabBar`. Root-scoped state lives in `root/state/State.kt`.
- Bottom tabs (4): `DISCOVER`, `ANIME`, `MANGA`, `PROFILE`.
- Settings is independent of profile (the old profile-route concept is gone). The gear in the PROFILE toolbar sets `State.isSettingsOpen`; open means Root's `Toolbar` with back + `settingsTitle` ("App settings" / "Configuración de la app") and zero actions, and the content area renders `SettingsScreen`. Back closes the flag. Settings never imports `profile/`.
- `discover/` has repositories + state holders (`DashboardStateHolder`, `ExploreStateHolder`) and renders the Dashboard screen on the DISCOVER tab: an Anime/Manga selector whose rails are **type-scoped** (one query per type keyed by the `DashboardSectionKind` enum, cached per type, so adding rails never grows a single request). Anime: current season (dynamic title, e.g. "Fall 2026", featured 50%-bigger cover) / next season / Rising / all-time popular anime / all-time popular movies / best rated anime / best rated movies / best rated anime now (`RELEASING`) / best rated movies now (12-month window). Manga: New releases (leads, featured) / Rising / all-time popular manga / light novels (`NOVEL`) / manhwa (`KR`) / manhua (`CN`+`TW`) / best rated manga / light novels / manhwa / manhua. Rails avoid overlap (anime "main" shelves exclude movies; manga "main" shelves are JP manga/one-shots; manhwa/manhua are KR/CN+TW comics; light novels are `NOVEL`). "Best rated" shelves carry a popularity floor (5000); covers match the list width (`MediaCoverCardCoverWidth`) and every card shows up to two stat badges (score / popularity, chosen per rail by `cardStats()`). Public so guests see it, explicit load, TTL cache, PullRefresh, non-blocking refresh error, per-type errors (an Anime failure never shows on Manga), a type/18+ change mid-load deferred rather than lost, and the `isAdult` gate — hidden pins `isAdult: false`, allowed omits it. Explore's search + filter UI is still pending; `mediaDetail/` renders all four detail pages — media (`MediaDetailScreen` single scroll, in order: full-bleed banner hero with centered cover — title + meta, no studio line (studios are interactive chips in the information panel) + next-episode band (flat rectangle, only while the drop is ahead) + a frameless stats row (score/popularity/favourites with the cover-badge icons, value and label; painted only when a column has data) + synopsis (edge-to-edge) + framed information grid (with the interactive studio chips) + genre chips (edge-to-edge) + characters/staff rails + trailer (edge-to-edge) + one framed links panel (streaming + external, each with its own subtitle; nothing when both are empty) + related/recommendations rails + tag chips (edge-to-edge); voice-actor language is a Character-detail concern, not shown here), character (`CharacterDetailScreen`: person hero + role chips (edge-to-edge) + voice actors with a voice-language picker that stays visible with an empty label when the chosen language has no cast + "Description" + info + animeography rail), staff (`StaffDetailScreen`: person hero + occupation chips (edge-to-edge) + "Biography" + info + roles rail + characters rail) — person biographies render their external links (HTML anchors → the `core/util` extractor, with host-derived labels) through the shared `MediaDetailLinkSection` and studio (`StudioDetailScreen`: person-style name hero, whose sub-line carries the anime-studio flag, + a poster grid whose order is server-side, picked in the shared `SortSheet`) — plus the entry editor sheet. Rails preview the first 10 items and carry a "View all" action in the header as a `TrailingPill` (the same pill the voice-language selector uses, label + chevron); the rail itself never fetches, shows a single compact subtitle line (the relation kind for related, the role for people; the voice-actor rail has none, the picker already states the language) and no catalog meta. "View all" pushes a `DetailDestination.DetailList` (one `DetailListKind` per section: media characters/staff/related/recommendations, character appearances, staff roles/characters) rendered by the generic `DetailListScreen` over the parent's already-loaded holder (re-bound from `parentId` on entry, since an item opened from the list can swap that holder), auto-paging on scroll. Its toolbar carries the section title (the only detail surface with a toolbar title) plus back, no actions. Media rows (`ROWS`) take the `format · season year · status` catalog line as the card subtitle (the lists/calendar wording; manga status when the parent is a manga) with the relation kind as a second row; people use a 2-column `GRID` with the character role (`Main`/`Supporting`/`Background`) or staff role as subtitle — room for the role is why the grid is 2-up, not 3. Rails navigate media→media, characters→character, staff→staff, studios→studio. Character and staff rails keep the server order (fetch-by-popularity, no client sort); studio's grid is server-ordered too (the shared sort sheet re-queries page 1 and the grid returns to the top). The toolbar heart + share + open-in-AniList work for all four destinations (media also keeps the editor pencil). Pull-to-refresh is manual only. `mediaList/` has the first real list UI: `AnimeListScreen` and `MangaListScreen` (shared `ChipRail` section rail + `MediaCoverCard` rows over their holders, wired in `Root`'s ANIME/MANGA tabs, guest sees calendar action only; lists force-reload on title-language change so server-mapped titles never go stale). Screens land later without restructuring (see feature-group exceptions).
- `profile/` has domain + repository (+mappers) + state holders (`ProfileStateHolder`, `ProfileFavoritesGridStateHolder`) and no profile UI yet; `notifications/` likewise (holder, no screens). `calendar/` renders the schedule overlay (`CalendarScreen` over `CalendarStateHolder`, wired to the toolbar calendar action, guests included). Media detail is live (taps on list/calendar cards push `State.detailStack`); no feed/search/library surfaces exist yet.
- `settings` renders the General group today (`settings/ui/SettingsScreen.kt`, no own header): language, theme (mode-first groups: mode then palette, value reads `"$mode · $palette"`), font, surfaces, color style, image quality and **start tab** (+ its dependent start section) rows inside a shared `OptionGroup` frame, each opening its own `ModalSheet` picker, over the App-owned holder (same state the auth modal edits, already persisted). The start tab/section pair is local-only (never synced) and the sub-choice options are resolved by Root (settings never imports a feature). `SettingsState` (`UserSettings` query + `UpdateUser` mutation with 600ms save debounce) already flows `App → Root` with `refresh()` on first open; all five groups live (General local-only; AniList/Lists/Notifications synced and hidden for guests; Information static with Version row + About modal). Pull-to-refresh forces `refresh()` with indicator; advanced category names persist locally via the store.
- Notifications feed is parked as a future `profile` subfeature (sheet from the Profile toolbar). A top-level `notifications/` package with repository + holder already exists and moves under `profile/` when its UI lands.
- Every tab except Settings, the two lists and the Discover dashboard renders a `MessagePanel` placeholder (title + icon, no actions) — PROFILE today.
- App language support: Spanish (`es`) and English (`en`).
- App name `Michi` is a brand name; do not move it to translatable XML unless the user changes direction.
- Dropped directions (do not re-propose): multi-account (multi-service auth via `AuthServices` is the path instead); Niro-style adult-cover censoring (hide-behind-18+ stays); a dedicated guest profile surface (guest profile = sign-in prompt via `AuthModal`); favourite hearts on list cards (saturates the lists).

## Lazy Loading Network Strategy (HTTP 429 Prevention)

State holders do NOT load automatically inside constructor `remember` blocks:
- State holders never load in constructors or `remember` blocks: loads are explicit (`load*()` / `refresh()`), so nothing fires without a user or screen effect behind it.
- `SettingsState.refresh()` fires once per session from `Root` on first settings open (`LaunchedEffect(isSettingsOpen)`); saves debounce 600ms. Guests never load (`canSync = false`).

## Root Responsibilities

- `Root` is the app-level orchestrator. It wires the session `SettingsState`, derives the topbar title, and routes the content area.
- `root/state/State.kt` holds: selected tab, selected animation/reading sections, `isSettingsOpen`, `isCalendarOpen` (+ `isCalendarFilterOpen`), `detailStack` (media/character/staff/studio push + `isDetailOpen` + `isDetailMenuOpen` + `isDetailSortOpen`), search active/query, and current profile draft.
- `root` owns topbar search state for DISCOVER, ANIME and MANGA via `Toolbar` (PROFILE has no search):
  - Normal state: topbar actions.
  - Active search: replaces title with `SearchField`; left icon becomes ChevronLeft; system back closes search.
- `root` owns the settings overlay via `State.isSettingsOpen` (single source for title, content and back; closes with `CloseOverlay`). Open means `Toolbar` with back + `settingsTitle` and zero actions; the content area renders `SettingsScreen`.
- `root` owns media detail via `State.detailStack` (single source for title, content and back). Open means a full-screen detail page over the current tab with the tab bar hidden, back chevron with no title (the title lives in the hero) and trailing actions: media → edit / favourite / overflow; character/staff → favourite / overflow, studio → sort / favourite / overflow (the shared sort sheet edits the studio holder and the grid re-queries; overflow: open in AniList + share, routed per active destination); a "View all" list (`DetailDestination.DetailList`) shows only the back chevron and renders the full paginated section; back pops one destination at a time (`BackStep.CloseDetail`) and, since the detail renders over calendar/explore/notifications, it closes before them (one back per visible layer).
- Root owns per-surface saved UI state: the content area is wrapped in a `rememberSaveableStateHolder()` keyed by slot tab + view (`tab` / `settings` / `detail:<entryId>` / overlay) with `key(surface)` inside, so each tab, each detail-stack entry (identity per push, `State.detailEntryIds`) and each overlay keeps its own saveable scroll state; a popped entry drops it. The provider lives inside `AnimatedContent`'s content lambda (two slots compose during a tab transition, so equal keys would crash) and the modals (sign-in, editor, share) stay outside it.
- `Toolbar` takes primitives + callbacks (`onNavigation`, `onAction(id)`, search callbacks); `Root` maps action ids (`ACTION_SEARCH`, `ACTION_SETTINGS`, …). Filter/sort actions are currently no-op until their screens land; the calendar action opens the schedule overlay (`State.isCalendarOpen`).

## Profile Layout (parked — no profile UI exists yet)

- `profile/` holds domain + repository (+mappers) + state holders only.
- When the profile lands, cross-feature navigation (e.g. a "See more" rail jumping to a list tab) goes through `Root` callbacks — never feature→feature imports.

## Settings Layout

| Group | Items | Rendering |
|---|---|---|
| **General** | Language · Theme (mode · palette) · Font · Surfaces · Color style · Image quality · Start tab · Start section | picker sheets (`ModalSheet` + `OptionGroup`) |
| **AniList** | Title language · 18+ content · Score format | picker / inline toggle |
| **Lists** | Default sort · Split completed anime · Split completed manga · Advanced scoring | picker / inline toggle |
| **Notifications** | Airing · Messages · Media | 3 inline toggles (activity/following/forum have no living feature behind them) |
| **Information** | Version · About (modal) | static row + `ModalSheet` (brand mark + description + attribution, version from `MichiBuildConfig`) |

- **Inline toggles** (private `SettingsToggleRow`: title + optional subtitle + display-only `Switch`): 18+ content, split completed anime/manga, advanced scoring, plus the 3 notification prefs (airing, messages, media).
- **"Pestaña al abrir"** (General, local-only, never synced): the start tab (`StartTabOption`, default DISCOVER) plus a dependent **"Sección al abrir"** row that only shows when the tab has one — Anime/Manga list sections (`AnimeListSection`/`MangaListSection`, default ALL) or the Dashboard's Anime/Manga catalog type (`StartCatalogOption`, default ANIME); PROFILE has none. `settings` stores opaque ids (the feature enums' names) because it never imports a feature; `Root` supplies the localized options (`StartDestinationChoice`) and `resolveStartDestination` maps the stored value to the startup state, falling back to DISCOVER for a guest and to ALL for a section id that no longer exists (split-completed off / renamed). It applies at startup only — changing it never moves the user mid-session.
- **Pickers** open `ModalSheet` with `OptionGroup` + `OptionRow`. Enums: `AppLanguage`, `ThemeType`, `AppFont`, `SurfaceStyle`, `TitleLanguage`, `ScoreFormat`, `ListSort` (plus `ThemeColors` via the shared palette list and `PaletteStyle` via the style list).
- **Theme**: `SYSTEM/LIGHT/DARK` + palette as one combined row (mode group first, palette second; value reads `"$mode · $palette"`), font, surfaces and the global MaterialKolor `PaletteStyle` as their own rows — same pickers the auth modal's theme sheet had. Style names travel raw ("Tonal spot", "Monochrome", …) like palette names. Writes persist to the store; the auth modal keeps only its language picker.
- Settings is its own feature. Other features must not import `settings`; `root` renders the screen standalone (never inside profile routes). `settings` never imports `root`.

## UI Decisions

- Bottom tab labels (4):
  - Spanish: `Descubrir`, `Anime`, `Manga`, `Perfil`.
  - English: `Discover`, `Anime`, `Manga`, `Profile`.
- Home has no banner/avatar and no embedded search row. Search belongs in the topbar.
- **Glass rule (validated sep-2026):** glass (`glassContainerColor`, `glassBorder`, shadow) is reserved for floating surfaces and actions (Toolbar, TabBar, buttons). Content cards stay opaque `surface` — a glass pilot on the session card washed out in light mode (translucency flattened, shadow + 40% border rendered a ghost edge).
- **Glass corruption rule (validated sep-2026):** never chain `Modifier.glass()` (`.shadow().clip()`, forces an offscreen compositing layer) on inline content — the container alpha seals inside the layer and composites as an opaque inner square. Inline surfaces take the glass *recipe* (translucent color + border, no shadow); `.glass()` lives only on floating elements.
- **Card vs edge-to-edge (validated oct-2026):** on detail pages, a framed card (`MediaDetailPanel`) marks a **data block** — the information grid and the link rows — because grouping tabular content reads better boxed. The panel is **flat**: `surface` fill + `outlineVariant` border (exactly like the list cards), never glass — it groups, it does not float. **Media and prose stay edge-to-edge**: the banner hero, the horizontal rails (characters/staff/related/recommendations), the trailer, the synopsis, the **stats row** (no frame: three columns with an icon, the value and the label), the **chip rows** (character/staff roles and occupations under the hero, media genres and tags under their header) and the **next-episode band**: a single chip group needs no frame, and grouping labels reads worse boxed; the band is a flat full-width rectangle (`surface` + `outlineVariant`, 12dp radius, inert) so it does not read as a button. - **Chip language**: `ui/components/ChipRail` owns the pill recipe. Interactive chips (list sections, filters, calendar, voice-language picker) keep the selected = primary-container / rest = outlined look and the glass recipe, and ripple on tap. `interactive = false` renders **flat, inert** chips (`surface` + `outlineVariant`, no glass, no click) for labels: genres, tags, roles and occupations. The **studio chips** stay interactive on purpose — they open the studio page. `ui/components/TrailingPill` is the compact trailing control (a rail's "View all", the voice-language selector): label with optional leading/trailing glyphs on the same glass/solid recipe.
A rail inside a card gets boxed in (first/last cards clip on the rounded corners, the inner padding hides content) and loses the lateral bleed that signals "scroll for more". This applies to all four `mediaDetail/` scopes (media, character, staff, studio), not just media.
- **Card insignias**: cover badges are built in one place, `ui/components/CoverStats` (`scoreBadge` → `85%`, `popularityBadge`/`favouritesBadge` → `compactCount()`), and painted by `CoverStatBadge` (translucent icon+value pill) on the top corners of a poster. The dashboard picks which two a rail shows via `DashboardSectionKind.cardStats()`; the studio grid always shows score + favourites. Global favs use outline `AppIcons.Favorite`, the user's favourited state the filled twin. The anime/manga list rows render a glyph-only pill (no star/number) when the score format is smiley.
- **Poster card**: `ui/components/PosterCard` is the one poster card of the app — token-sized cover + up to two `CoverStatBadge` + two reserved title lines + an optional subtitle, one radius (12dp), inside `CardCappedDensity`. The type scale steps with the token: a `Rail` card reads `bodySmall`/`labelSmall`, a `Featured` one (a grid cell, the featured rail) `bodyMedium`/`bodySmall`. Rails (`MediaRail`), the "View all" grids (`DetailListScreen`) and the studio grid all draw it, so a poster looks the same everywhere; it takes only primitives (a null `onClick` renders a display-only card).
- **Card type scale**: title stays `titleMedium` (weight 500 beats `titleLarge`'s 400); episode/date line is `bodyMedium` + `onSurface`, secondary meta `bodyMedium` + `onSurfaceVariant`. Hierarchy by color, not size.
- **Images**: cover-bearing models carry a `CoverSizes` (medium/large/extraLarge; people images only ship large/medium). **Cover sizes are tokens** in `ui/components/CoverMetrics`: `CoverSize` carries the width **and** its 3:4 height (`Modifier.coverSize(token)`), so no surface can invent a size and a rendered poster always matches its token — `CoverSize.List` (111dp) for row cards (lists, calendar, "View all" rows), `CoverSize.Rail` (131.5dp, the exact midpoint of the other two) for every rail card, and `CoverSize.Featured` (152dp) for the dashboard's featured rail, the media/person heroes and a grid cell. Grids use `GridCells.FixedSize(cell.width)` + `Arrangement.SpaceEvenly` (no horizontal content padding, so the air between columns equals the one at the edges) and size their cells with `ui/components`' `gridCoverSize(availableWidth)`: it starts from the standard token — `Featured` while `minColumns` (2) fit with a 12dp gap between and around, otherwise the next token down (`Rail`) — then grows the cell to fill the width, never past `GridMaxGrowth` (+25%). The surplus beyond the cap goes back to even air, so a narrow phone keeps two readable columns and a wide one never leaves a huge margin. The "no invented sizes" rule holds: the cell (and its 3:4 height) always derives from the token. `rememberCoverUrl(cover, size)` picks the image variant from the same token, and `PosterCard`/`MediaCoverCard` cap the OS font scale at 1.3× so cover badges never collide. A person image that is AniList's shared placeholder (`.../anilistcdn/{character,staff}/{large,medium}/default.jpg`, verified against the live API) maps to null in `personCoverSizes`, so a person without an image falls back to the grey placeholder instead of the blue "NO IMAGE" art. A **Settings → General → Image quality** row (`AUTO`/`HIGH`/`LOW`, local-only) drives which variant the shared cover composables resolve via `LocalImageQuality`; `AUTO` follows the connection reactively (unmetered → HIGH, metered → LOW) through `Connectivity` (a `StateFlow` wired in `MichiDependencies`; Android recomputes from the active network on every change (`registerDefaultNetworkCallback`, never trusting the callback's own value; `NOT_METERED`/`TEMPORARILY_NOT_METERED`, unknown counts as unmetered, Data Saver is intentionally not consulted because Android only applies it to already-metered networks) and iOS uses `NWPathMonitor`). One Coil `ImageLoader` (crossfade + memory cache capped at 20% of the app heap) is installed once from `App`; Coil 3's default disk cache (`singletonDiskCache()`) stays. Covers resolve per rendered width (`CoverSizes.forWidthPx`, density-aware); the dashboard rails paint the cover's `color` while loading.
- **Session cache**: a cold start with a stored token shows the shell immediately from a cached viewer (`CachedViewerStore` over the store — id/name/avatar/banner + the four settings, no `about`) and refreshes in the background; a 401 clears token + cache → Anonymous, and a network error keeps the cached shell instead of flashing Error.
- **Dashboard cache + preload**: rails are disk-cached per media type **and** 18+ gate (`DashboardFeedCache`); loading/refreshing are tracked **per type**, so an Anime request finishing never leaves (or sets) Manga's indicator — the spinner-forever bug after switching to a cached type. `load()` hydrates from disk first (fresh → no network, no spinner) and a response whose gate changed mid-flight is discarded. The holder is created with the applied gate (so the screen's first sync never forces) and reads its strings provider **once per request** (a load never mixes languages), so it is not recreated on a language change and the Anime/Manga selection survives. A language change calls `invalidate()` — drops disk + memory and bumps an observable **generation**, launching nothing — and the screen reloads when it is visible again (its effect keys on the generation), so a change made on another tab or under Settings never fires a background request. A gate change also drops memory and reloads **without forcing**, hydrating the new key (a fresh cache for the new value skips the network). The other type is preloaded once after the first successful load (respects the cache; a select during that preload joins it instead of fetching twice; never surfaces an error).
- **External links**: one `core/model/ExternalLink` model for media links (from the API) and person biography links, mapped to the UI by a single `ExternalLink.toLinkItem(strings)` (`detail` = language for media, `@handle` for people). Person links are enriched against the AniList site catalog (`ExternalLinkSourceCollection`, memory + 7-day disk cache) matched by normalized host (www/mobile/m, twitter↔x, youtu.be↔youtube.com) **including subdomains on a dot boundary** (`usuario.fanbox.cc` → `fanbox.cc`, never `notfanbox.cc`) with the most specific host winning; no match/offline falls back to a host-derived label with no icon. The catalog is `prefetch()`ed in the app's own scope at the start of a staff/character detail, overlapping the detail query and awaited only when the biography actually carries links; one shared in-flight request (Mutex-guarded creation) means never two at once, and a failed fetch is remembered for 10 minutes. The `core/util` extractor pulls anchors **and** bare URLs (with/without a "Label:") out of the biography, keeps real prose, and cleans leftover rótulos/separators.
- **Rail badges**: `ui/components/CoverStatBadge` (translucent icon+value pill) is shared by the studio grid and the dashboard rails; the caller owns the corner and skips nulls, and the values come from `ui/components/CoverStats` (`scoreBadge`/`popularityBadge`/`favouritesBadge`, one format everywhere). Dashboard cards show up to two badges chosen per rail in one place, `DashboardSectionKind.cardStats()` (`POPULAR_*` lead with popularity, `BEST_*`/season/trending/new-releases lead with the score), from the `averageScore`/`popularity`/`favourites` the rail query carries (the disk cache tolerates an older payload: the new fields decode to null). Popularity/favourites render with `AppIcons.Groups`/`AppIcons.Favorite` + `core/model`'s `compactCount()` (`950`/`1.8K`/`12K`/`1.3M`), the score as `85%`.
- **Scroll rule (validated oct-2026)**: changing **what** is shown — a section chip, a filter, a sort, the calendar day, the Anime/Manga selector, a new detail destination — returns to the top; refreshing, closing a sheet or coming back (back, another tab) keeps the position. `ui/components/ScrollMemory` implements it: `rememberResetListState`/`rememberResetGridState` are `rememberSaveable` states keyed by their reset key, so a change re-creates the state **in the same composition** (the top is painted in the next frame, no effect tick) while a restore keeps the position. Survival across surfaces is Root's: the content area is wrapped in a `rememberSaveableStateHolder()` keyed by `"<slot tab>:<view>"` (`tab`, `settings`, `detail:<entryId>`, overlays) with `key(surface)` inside, so every tab, every detail-stack entry (identity per push, from `State.detailEntryIds`) and every overlay keeps its own saveable scroll state. A detail key carries the **composing slot's tab** (`detailSurfaceKey`) because a tab transition composes two slots and equal keys would crash, so a popped entry is dropped under every tab variant (`detailSurfaceKeys`). The provider must live inside `AnimatedContent`'s content lambda, and only the if/else chain is wrapped: the modals (sign-in, entry editor, share) stay outside it so their `remember`ed holders survive.
- **Debug timing**: debug builds log each GraphQL operation's duration (`MichiDependencies.isDebug` → Ktor client; Android reads `FLAG_DEBUGGABLE`, iOS `Platform.isDebugBinary`).
- **Load states**: `ui/components/LoadStateContainer` (spinner / retry error / pull-to-refresh content, plus an optional compact non-blocking refresh error) is shared by the four detail screens and the dashboard; loading/error labels arrive as parameters so it stays feature-agnostic (the detail screens pass no refresh error).
- **Information grid**: `mediaDetail/ui/components/MediaDetailInfoGrid` is the one panel grid (label above uppercase value; short entries two per row with an odd tail kept half width, `wideEntries` full width) used by media, character, staff and studio. Panels wrap it in `MediaDetailPanel` + `SectionHeader`.
- **Detail cache**: the four detail holders share `core/util/DetailCache` (pure, insertion-order eviction approximating LRU, plus `update(key, transform)`). A toggle-favourite response updates **its own** cached snapshot through `update` and only touches the visible state while the holder still shows that id, so opening another destination mid-request never mixes snapshots.
- **Rail paging**: every query that feeds a rail pages at a single `RailPageSize` (10, `core/network/domain`) — the same value is the `perPage` and the rail's item count, and "View all" starts at page 1 and pages with it (characters/staff/recommendations, character/staff media, dashboard sections). Full grids page at `GridPageSize` (30) instead: the studio media grid uses it for both queries and fills itself with the auto-pagination footer. `Media.relations` accepts no pagination args in the AniList schema, so the media detail trims its relation node to exactly what the rail renders (id/format/status/season/seasonYear/startDate.year/title/cover).
- **Sort sheet**: `ui/components/SortSheet` is the one sort picker (criterion group + order group, primitives in/callbacks out). The anime/manga lists map it to their holder (local sort); the studio grid maps it to `StudioDetailStateHolder`, whose `changeSort` re-queries page 1 with the derived AniList `MediaSort` (server order, no client sort). `UserListSort.toMediaSort(order, isAnime)` (`core/model`) is the single place that derives the API key.
- **Auto-pagination footer**: `ui/components/LoadMoreFooter` + `LoadMoreKey` are shared by every auto-paging list/grid ("View all", the studio grid). The last item is the footer; a `snapshotFlow` on the visible items loads the next page when its key shows up, `distinctUntilChanged` keeps it from repeating while a page is in flight and from looping when a page fails (the footer stays visible but the flow does not re-emit).
- **Font scale cap**: dense cards (`MediaCoverCard` rows and `PosterCard` cards, via `ui/components/CardDensity`) cap the OS font scale at 1.3× so content degrades to ellipsis instead of clipping and cover badges never collide; rail titles reserve two lines so the row stays even. Full-scale text lives on the detail screens (unbounded layouts). Validate at 200% OS font size.
- **Card pill priority**: in card bodies the action pills are measured first and pinned to the cover bottom (placed with `Arrangement.SpaceBetween`, like the lists); text slots are weighted (`weight(1f, fill = false)`) so overflow drops the last tertiary line (behind, `maxLines = 2`) instead of pushing the pills out.
- Reading: separate `+1 CH` and `+1 VO` buttons (manga has chapters and volumes). Animation: `+1 EP`.
- Counters are numeric-only next to their buttons, styled with stronger weight.
- Account stats: compact count label (e.g. `1.8K`) via `ProfileStats.toCompactCountLabel`.
- `MediaReleaseDateTime` prepares next-release and behind-label support.
- **Direction (parked): list bottom padding.** `Root` intentionally lets content scroll behind the floating TabBar (veil effect) and only pads top. When the first real list lands, its `LazyColumn` takes `contentPadding(bottom = scaffoldBottom)` threaded from `Root` (`padding.calculateBottomPadding()`), so the last item clears the capsule (~72dp + 24dp margin) while mid-scroll still passes behind it. Never hardcode per-feature bottom values. (Every list follows this rule, including `SettingsScreen`.)

## AniList / API Direction

- AniList uses GraphQL. Prefer typed/structured handling over ad hoc string concatenation.
- AniList api is limited to 30 request per minute
- The API serves adult entries to clients; filtering via `isAdult` is the client's job (`docs.anilist.co/guide/considerations`). Omit the var when 18+ is allowed (null = both) — never send `isAdult: true` (that filters to adult ONLY).
- Keep query/mutation definitions close to the repository/API surface that owns them.
- Keep AniList API concerns out of UI state and screens.
- Auth/session belongs behind `core.auth` or `core.session` abstractions (to be added).
- API DTOs must not leak into presentation state — map at feature/core boundaries.
- Model network errors, GraphQL errors, rate limits, and empty states explicitly for consistent UI rendering.
- Android system back uses `androidx.activity.compose.BackHandler`. iOS system back is no-op until native behavior is needed.
- Android deep links: the manifest registers `https://anilist.co/anime|manga/*` plus a `text/plain` `ACTION_SEND` share target (and the `michi://oauth/callback` filter). `MainActivity` routes OAuth vs media; the media id flows through `MichiDependencies.pendingMediaId` and `Root` opens the detail. The web link is not verifiable (we don't own anilist.co), so Android sends it to the browser; **sharing** the link (Share → Michi) is the reliable path. `parseAniListMediaId` extracts the id from free text. iOS universal links are parked for the same reason.

## Naming

- Include feature/scope in every file and class name.
- Good examples: `SettingsScreen`, `AnimeListStateHolder`, `MediaDetailStateHolder`, `ExploreStateHolder`, `ProfileRepositoryImpl`, `NotificationPreferences`.
- Avoid generic names: `ViewModel.kt`, `Repository.kt`, `Screen.kt`, `TopBar.kt`, `UserForm`, `LanguageSelector`.

## Workflow & Validation

- Before structural edits, read nearby code first.
- Before designing a feature surface, consult AL-Chan and Otraku to confirm the shape exists in AniList.
- Preserve user changes in the working tree. Do not revert unrelated edits.
- Prefer existing project patterns over introducing new frameworks.
- Test policy: unit tests live in `core/` (stable, pure logic — mappers, parsers, keys, labels). Features are covered by integration tests at the repository seam (canned JSON → fake `AniListGraphQLClient` → real repository + holder), following `CalendarIntegrationTest`; no fine-grained holder unit tests (they rot on every rewrite).
- Build from `/home/psyxho_skull/StudioProjects/luum/michi`:
  - `./gradlew :shared:compileAndroidMain --offline` — shared code changes.
  - `./gradlew :androidApp:compileDebugKotlin --offline` — when root/feature signatures change.
  - Enforce the `core/` boundary with grep after structural moves: no `androidx.compose` imports under `core/`, and no `core.*` imports of `ui.*`.
  - Build offline with the default Gradle home (`~/.gradle`). An isolated home (`GRADLE_USER_HOME=/tmp/...`) only works if pre-populated online first, and `/tmp` is wiped on reboot — never rely on it as a persistent cache.

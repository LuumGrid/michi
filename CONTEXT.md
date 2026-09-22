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
- Product: AniList client for anime/manga discovery, lists, account, feed, search, details, and settings.

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

No feature has screens or components yet (only `settings/` renders UI). The groups below are domain + repository + state holders; screens land later without restructuring.

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

`media/`, `character/`, `studio/` and `staff/` never import each other directly. No shared `ui/` pieces between the four scopes yet, so none were created (do not pre-create empty layers).

## State Pattern

- A stateful feature owns a `*StateHolder` (plain Kotlin class) holding query state in plain vars, exposing derived read-only views and intent-style mutations (`load`, `refresh`, filters, edits). Real examples: `ExploreStateHolder`, `AnimeListStateHolder`, `AccountStateHolder`, `CalendarStateHolder`.
- The holder is constructed with its repository (plus scope/store where needed); several have a pure `create*` factory for tests (`createAccountStateHolder`, `createCalendarStateHolder`, `createNotificationsStateHolder`, `createSettingsState`). Only `SettingsState` is Compose-backed today (its async load/save/error drives UI recomposition) with a `rememberSettingsState()` Composable factory.
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
  navigation/domain/            routes, back-handler contracts
  medialist/domain/             list-status contracts shared by list features
  model/                        neutral domain vocabulary (MediaFormat, MediaSeason, dates,
                                sort, FilterOption) — cross-concept, belongs to no feature
  util/                         pure helpers (HTML stripping, …)
```

`ui/` is the shared Compose UI. Everything visual and reusable lives here, nothing else:

```text
ui/
  components/   shared composables (Toolbar, TabBar, SearchField, MessagePanel, …) — no layer prefix, package disambiguates
  icons/        shared toolbar primitives (AppIcons: Search, Back, Clear)
  language/     LanguageProvider (CompositionLocal) + searchHintFor()
  theme/        Theme, ThemeColors seeds (Default/Ocean/Sakura), ThemeProvider
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
    AuthLandingScreen.kt    login landing + language picker (title only; theme lives in Settings)
    state/State.kt          BackStep, State, rememberState (+ isSettingsOpen flag, no account routes)
  core/
    auth/domain|repository/   AniList OAuth flow + token storage
    session/domain|repository/ viewer identity + SessionManager
    network/domain|repository/ Ktor GraphQL client + NetworkResult (+ anilist/dto/)
    storage/domain/           settings/filter stores (+ platform `SettingsStore` impls:
                                `SharedPreferencesSettingsStore` on Android,
                                `NSUserDefaultsSettingsStore` on iOS, wired via `MichiDependencies`)
    language/domain/          string tables only (LanguageStrings, EN/ES, AppLanguage)
    navigation/domain/        routes, back-handler contracts
    medialist/domain/         list-status contracts
    model/                    neutral domain vocabulary (MediaFormat, MediaSeason, dates, sort)
    util/                     pure helpers
  ui/
    components/             reusable app chrome and UI primitives (shared composables, no prefix)
    language/               LanguageProvider (CompositionLocal) + searchHintFor()
    theme/                  Theme, ThemeColors seeds (Default/Ocean/Sakura), ThemeProvider
  account/                domain(+model)/repository(+mappers)/ui/state holders (no screens yet)
  calendar/               domain(+model)/repository/ui/state holder (no screens yet)
  discover/               domain(+model)/repository/ui per dashboard+explore state holders (no screens yet)
  mediaList/              domain/{anime,manga,common}/repository/ui per anime+manga state holders (no screens yet)
  mediaDetail/            domain+repository/ui per media+character+studio+staff state holders (no screens yet)
  notifications/          domain(+model)/repository/ui/state holder (no screens yet)
  settings/
    domain/               SettingsRepository + model/ (SettingsData, ThemeMode, TitleLanguage,
                          ScoreFormat, ListSort, DiscoverTabOption, NotificationPreferences)
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
- `account` / `Account` for user profile surfaces.
- `brandColor()` for app-wide brand primitives (raw hex in, `Color` out, converted at the UI boundary).
- No layer prefixes on class names: shared UI lives in `ui/` (`Toolbar`, `Icons`, `TabBar`), root scope in `root/` (`Root`, `State`). The package is the disambiguator — `ui.Icons` vs feature icons, `root.State` vs any other state.
- `AniList*` for API/client/DTO-specific classes.

## Current State

- `App.kt` owns theme mode/palette/font + language via `remember`, hydrated from `SettingsStore` at startup and persisted on every change (unknown values fall back to defaults). It routes landing vs shell on `SessionState` with an `AnimatedContent` crossfade (same `tabFadeSpec` motion as tabs) over an opaque theme backdrop. The authenticated `Viewer` enters `Root` so the shell profile draft is real.
- `root` is a slim orchestrator. It wires the session `SettingsState` and composes `Toolbar` + `TabBar`. Root-scoped state lives in `root/state/State.kt`.
- Bottom tabs (4): `DISCOVER`, `ANIME`, `MANGA`, `ACCOUNT`.
- Settings is independent of account (the old account-route concept is gone). The gear in the ACCOUNT toolbar sets `State.isSettingsOpen`; open means Root's `Toolbar` with back + `settingsTitle` ("App settings" / "Configuración de la app") and zero actions, and the content area renders `SettingsScreen`. Back closes the flag. Settings never imports `account/`.
- `discover/` has repositories + state holders (`DashboardStateHolder`, `ExploreStateHolder`) and no screens yet; same for `mediaList/` (`AnimeListStateHolder`, `MangaListStateHolder`) and `mediaDetail/` (media/character/studio/staff holders). Screens land later without restructuring (see feature-group exceptions).
- `account/` has domain + repository (+mappers) + state holders (`AccountStateHolder`, `AccountFavoritesGridStateHolder`) and no profile UI yet; `calendar/` and `notifications/` likewise (holder, no screens). No feed/search/library/media surfaces exist yet.
- `settings` renders the General group today (`settings/ui/SettingsScreen.kt`, no own header): language, theme (mode-first groups: mode then palette, value reads `"$mode · $palette"`) and font rows inside a shared `OptionGroup` frame, each opening its own `ModalSheet` picker, over the App-owned holder (same state the landing edits, already persisted). `SettingsState` (`UserSettings` query + `UpdateUser` mutation with 600ms save debounce) already flows `App → Root` with `refresh()` on first open; all five groups live (General local-only; AniList/Lists/Notifications synced and hidden for guests; Information static with Version row + About modal).
- Notifications feed is parked as a future `account` subfeature (sheet from the Account toolbar). A top-level `notifications/` package with repository + holder already exists and moves under `account/` when its UI lands.
- Every tab except Settings renders a `MessagePanel` placeholder (title + icon, no actions).
- App language support: Spanish (`es`) and English (`en`).
- App name `Michi` is a brand name; do not move it to translatable XML unless the user changes direction.

## Lazy Loading Network Strategy (HTTP 429 Prevention)

State holders do NOT load automatically inside constructor `remember` blocks:
- State holders never load in constructors or `remember` blocks: loads are explicit (`load*()` / `refresh()`), so nothing fires without a user or screen effect behind it.
- `SettingsState.refresh()` fires once per session from `Root` on first settings open (`LaunchedEffect(isSettingsOpen)`); saves debounce 600ms. Guests never load (`canSync = false`).

## Root Responsibilities

- `Root` is the app-level orchestrator. It wires the session `SettingsState`, derives the topbar title, and routes the content area.
- `root/state/State.kt` holds: selected tab, selected animation/reading sections, `isSettingsOpen`, search active/query, and current account profile draft.
- `root` owns topbar search state for DISCOVER, ANIME and MANGA via `Toolbar` (ACCOUNT has no search):
  - Normal state: topbar actions.
  - Active search: replaces title with `SearchField`; left icon becomes ChevronLeft; system back closes search.
- `root` owns the settings overlay via `State.isSettingsOpen` (single source for title, content and back; closes with `CloseOverlay`). Open means `Toolbar` with back + `settingsTitle` and zero actions; the content area renders `SettingsScreen`.
- `Toolbar` takes primitives + callbacks (`onNavigation`, `onAction(id)`, search callbacks); `Root` maps action ids (`ACTION_SEARCH`, `ACTION_SETTINGS`, …). Filter/sort/calendar actions are currently no-op until their screens land.

## Account Profile Layout (parked — no profile UI exists yet)

- `account/` holds domain + repository (+mappers) + state holders only.
- When the profile lands, cross-feature navigation (e.g. a "See more" rail jumping to a list tab) goes through `Root` callbacks — never feature→feature imports.

## Settings Layout

| Group | Items | Rendering |
|---|---|---|
| **General** | Language · Theme (mode · palette) · Font | picker sheets (`ModalSheet` + `OptionGroup`) |
| **AniList** | Title language · 18+ content · Score format | picker / inline toggle |
| **Lists** | Default sort · Split completed anime · Split completed manga · Advanced scoring | picker / inline toggle |
| **Notifications** | Airing · Messages · Media | 3 inline toggles (activity/following/forum have no living feature behind them) |
| **Information** | Version · About (modal) | static row + `ModalSheet` (brand mark + description + attribution, version from `MichiBuildConfig`) |

- **Inline toggles** (private `SettingsToggleRow`: title + optional subtitle + display-only `Switch`): 18+ content, split completed anime/manga, advanced scoring, plus the 3 notification prefs (airing, messages, media).
- **Pickers** open `ModalSheet` with `OptionGroup` + `OptionRow`. Enums: `AppLanguage`, `ThemeType`, `AppFont`, `TitleLanguage`, `ScoreFormat`, `ListSort` (plus `ThemeColors` via the shared palette list).
- **Theme**: `SYSTEM/LIGHT/DARK` + palette as one combined row (mode group first, palette second; value reads `"$mode · $palette"`), font as its own row — same pickers the auth landing's theme sheet had. Writes persist to the store; the auth landing keeps only its language picker.
- Settings is its own feature. Other features must not import `settings`; `root` renders the screen standalone (never inside account routes). `settings` never imports `root`.

## UI Decisions

- Bottom tab labels (4):
  - Spanish: `Descubrir`, `Anime`, `Manga`, `Cuenta`.
  - English: `Discover`, `Anime`, `Manga`, `Account`.
- Home has no banner/avatar and no embedded search row. Search belongs in the topbar.
- **Card insignias**: `SearchResultCard.kt` shows average rating (top-right, `Icons.Star`) and popularity / members count (bottom-left, `Icons.Groups`, k/M formatter). `Icons.Like` (heart) is reserved for user favorites only.
- Reading: separate `+1 CH` and `+1 VO` buttons (manga has chapters and volumes). Animation: `+1 EP`.
- Counters are numeric-only next to their buttons, styled with stronger weight.
- Account stats: compact count label (e.g. `1.8K`) via `AccountStats.toCompactCountLabel`.
- `MediaReleaseDateTime` prepares next-release and behind-label support.
- **Direction (parked): list bottom padding.** `Root` intentionally lets content scroll behind the floating TabBar (veil effect) and only pads top. When the first real list lands, its `LazyColumn` takes `contentPadding(bottom = scaffoldBottom)` threaded from `Root` (`padding.calculateBottomPadding()`), so the last item clears the capsule (~72dp + 24dp margin) while mid-scroll still passes behind it. Never hardcode per-feature bottom values. (`SettingsScreen` still carries no list today; when its rows land, it follows this rule like every other list.)

## AniList / API Direction

- AniList uses GraphQL. Prefer typed/structured handling over ad hoc string concatenation.
- AniList api is limited to 30 request per minute
- Keep query/mutation definitions close to the repository/API surface that owns them.
- Keep AniList API concerns out of UI state and screens.
- Auth/session belongs behind `core.auth` or `core.session` abstractions (to be added).
- API DTOs must not leak into presentation state — map at feature/core boundaries.
- Model network errors, GraphQL errors, rate limits, and empty states explicitly for consistent UI rendering.
- Android system back uses `androidx.activity.compose.BackHandler`. iOS system back is no-op until native behavior is needed.

## Naming

- Include feature/scope in every file and class name.
- Good examples: `SettingsScreen`, `AnimeListStateHolder`, `MediaDetailStateHolder`, `ExploreStateHolder`, `AccountRepositoryImpl`, `NotificationPreferences`.
- Avoid generic names: `ViewModel.kt`, `Repository.kt`, `Screen.kt`, `TopBar.kt`, `UserForm`, `LanguageSelector`.

## Workflow & Validation

- Before structural edits, read nearby code first.
- Before designing a feature surface, consult AL-Chan and Otraku to confirm the shape exists in AniList.
- Preserve user changes in the working tree. Do not revert unrelated edits.
- Prefer existing project patterns over introducing new frameworks.
- Test policy: unit tests live in `core/` (stable, pure logic — mappers, parsers, keys, labels). Features are covered by integration tests at the repository seam (canned JSON → fake `AniListGraphQLClient` → real repository + holder), following `CalendarIntegrationTest`; no fine-grained holder unit tests (they rot on every rewrite).
- Build from `/home/psyxho_skull/AndroidStudioProjects/luum/Michi`:
  - `./gradlew :shared:compileAndroidMain --offline` — shared code changes.
  - `./gradlew :androidApp:compileDebugKotlin --offline` — when root/feature signatures change.
  - Enforce the `core/` boundary with grep after structural moves: no `androidx.compose` imports under `core/`, and no `core.*` imports of `ui.*`.
  - Build offline with the default Gradle home (`~/.gradle`). An isolated home (`GRADLE_USER_HOME=/tmp/...`) only works if pre-populated online first, and `/tmp` is wiped on reboot — never rely on it as a persistent cache.

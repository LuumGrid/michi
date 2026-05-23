# Michi App Architecture Context

Project path: `/home/psyxho_skull/AndroidStudioProjects/luum/Michi`

## Identity

- Gradle root project: `Michi`.
- Kotlin Multiplatform structure: `androidApp`, `shared`, and `iosApp`.
- Android namespace/applicationId: `com.luum.michi.app`.
- Kotlin package base: `com.luum.michi.app`.
- Shared Android library namespace: `com.luum.michi.app.shared`.
- Compose resources package: `com.luum.michi.app.resources`.
- iOS framework name: `Shared`.
- Product direction: AniList client for anime/manga discovery, lists, account, search, details, and settings.

## Current Stack

- Kotlin Multiplatform with Compose Multiplatform shared UI.
- Material 3.
- Coil 3 is present for image loading.
- Manual state and dependency wiring; state holders are plain classes that own Compose-backed state.
- Android minSdk is read from `gradle/libs.versions.toml` and is currently `34`.
- Koin, Ktor, DataStore, and formal navigation are target technologies, not current dependencies.

## Current State

- `App.kt` owns dark/light theme state and app language state.
- `shell` is a slim orchestrator. It creates feature state holders and composes `ShellTopBar`, `ShellBottomNavBar`, and `ShellAccountRouter`. Shell-scoped state lives in `shell/state/ShellState.kt`.
- Bottom tabs currently are `HOME`, `ANIMATION`, `READING`, and `ACCOUNT`.
- `SETTINGS` is not a bottom tab. It is opened inside the account route.
- `DiscoveryScreen` is the Home/Discovery surface, adapted from AL-Chan's Home structure at a component level. Sample data is extracted to `discovery/presentation/sample/DiscoverySampleData.kt`.
- `AnimationScreen` and `ReadingScreen` are prototype list/edit surfaces backed by `AnimationListStateHolder` / `ReadingListStateHolder` (mutable state list + editing slot) with sample entries. Screens split into `Screen` (wires the holder) and `Content` (stateless).
- `account` was originally copied/adapted from Platform and has been redesigned for AniList alignment. It now owns: profile (header + stats + favorites rails), edit profile (form), and share profile (QR card). The Platform-style POSTS tab and gradient post grid have been removed.
- `settings` has been redesigned for AniList alignment. Legacy Platform groups (Creator tools, Subtitles, Data/Playback, Interactions, Accessibility section) have been removed and replaced with: App, AniList, Lists, Notifications, Account, About. `SettingsState` (plain class in `settings/presentation/state/`) holds all settings values and is created by `ShellScreen` via `rememberSettingsState()`. Inline toggles use `SettingsToggleRow`; pickers use `SettingsRadioPicker<T>`. Enum options live in `settings/presentation/model/SettingsOptions.kt`.
- `SearchScreen`, `LibraryScreen`, and `MediaScreen` are still placeholder-level surfaces.
- `library` still exists as scaffold debt, but it is not a bottom-tab destination.
- `profile` should not be recreated; use `account`.
- App language support is currently limited to Spanish and English. Android per-app language support is enabled for `es` and `en`; iOS declares both localizations.
- KMP template debris (`Greeting.kt`, `GreetingUtil.kt`, `Platform.kt` plus Android/iOS actuals) has been deleted. Empty `placeholder.txt` scaffolds have also been removed.

## Architecture Pattern

Use a feature-layer architecture with pragmatic Clean Architecture inside features when the feature has enough behavior to justify it:

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

Do not force `domain/` and `data/` for prototype screens. Add them only when real AniList/session/storage behavior appears. Do not create empty `placeholder.txt` files to reserve future layers.

## State Pattern

- A stateful feature owns a `*ListStateHolder` (plain Kotlin class) that holds `mutableStateListOf` / `mutableStateOf` for its data and editing slot.
- The holder exposes derived read-only views (`entries`, `entriesInSection(section)`, `countInSection(section)`) and intent-style mutations (`startEditing`, `stopEditing`, `incrementProgress`, etc.).
- A companion `remember*` Composable factory constructs the holder seeded with sample data.
- `shell` may create the holder when the topbar needs live data from it (e.g. `AnimationSectionChips` reads `countForSection` from the holder so chips reflect mutations).
- `SettingsState` follows the same holder pattern but is not tied to any single screen — it is created in `ShellScreen` (via `rememberSettingsState()`) and passed through `ShellAccountRouter` to `SettingsScreen`, so settings survive navigating away and back within the same session.
- When AniList lands, the holder constructor takes a repository instead of sample data; the screen/topbar do not change.

## Dependency Rules

- `feature -> core`: allowed.
- `shell -> feature`: allowed.
- `feature -> feature`: not allowed.
- `core -> feature`: not allowed.
- `feature -> shell`: not allowed.

Cross-feature coordination belongs in `shell` or in neutral `core` contracts. A feature must not import another top-level feature directly.

## Package Map

```text
com.luum.michi.app
  App.kt
  shell/
    ShellScreen.kt          slim orchestrator (state holders + topbar + content)
    state/ShellState.kt     ShellAccountRoute, ShellState, rememberShellState
    components/             ShellTopBar, ShellSearchField, ShellCollapsibleChips,
                            ShellAccountRouter, ShellBottomNavBar
  core/
    language/               manual language/string model
    model/                  neutral shared domain models (currently MediaReleaseDateTime)
    platform/               brand/theme primitives, icons, back handlers, clipboard
      components/           reusable app chrome and UI primitives
  account/presentation/     header, stats, favorites rails, edit form, share QR
  animation/presentation/   list/edit surfaces over a state holder
  reading/presentation/     list/edit surfaces over a state holder
  discovery/presentation/   trending, seasonal, recommendations (assembler over PlatformHomeComponents)
  search/presentation/      placeholder surface
  media/presentation/       placeholder surface
  library/presentation/     scaffold debt; not a bottom-tab destination
  settings/presentation/
    SettingsScreen.kt         orchestrator; dispatches to detail via SettingsDetailContent
    state/SettingsState.kt    all settings values + rememberSettingsState()
    model/SettingsModels.kt   SettingsGroup, SettingsItem, SettingsItemType (17 types), settingsGroups()
    model/SettingsOptions.kt  ThemeMode, TitleLanguage, ScoreFormat, ListSort, HomeTabOption, NotificationPreferences
    components/               SettingsRow, SettingsGroupHeader, SettingsDetailContent,
                              SettingsRadioPicker, SettingsToggleRow,
                              SettingsNotificationsDetail, SettingsAboutDetail
```

Sub-packages such as `core/anilist`, `core/auth`, `core/network`, `core/session`, and feature-level `domain/` / `data/` directories will appear when real behavior is implemented. They are intentionally absent today.

## Michi vs Platform

This project may reuse architectural ideas from `/home/psyxho_skull/AndroidStudioProjects/luum/Platform`, but Michi is a separate AniList app.

Do not copy Platform domain concepts into Michi unless the user explicitly asks for them. Avoid Platform-only terms such as `feed`, `hubs`, `pods`, `shots`, channels, memberships, `PlatformBrand.NAAT`, `PlatformBrand.NIK`, and the social-network "posts feed" pattern.

Michi uses:

- `MichiBrand`, not `PlatformBrand`.
- `Animation` for anime.
- `Reading` for manga/reading.
- `Account`, not `Profile`.
- `Platform*` prefixes only for reusable platform/chrome components, icons, and abstractions already named that way.

## AniList References

Two reference AniList clients to consult when designing or revising any user-facing surface in Michi:

- **AL-Chan** (Android, Kotlin) — https://github.com/zend10/AL-chan
- **Otraku** (Flutter, Dart) — https://github.com/lotusprey/otraku

Both clients agree on the AniList domain shape (user profile has favorites, not posts; lists are by status, etc.). If a Michi surface diverges from both, that is a design smell worth flagging before coding.

## Branding

- App-wide display name: `MichiAppName = "Michi"`.
- Treat `Michi` as a brand name, not as localized copy. The Android manifest may use the literal label unless the user changes direction.
- Current brand enum: `MichiBrand` with `ANIMATION` and `READING`.
- Current brand labels include `postsLabel`, `animationLabel`, and `readingLabel`. The `postsLabel` is reserved for any future Michi-branded social surface; it is **not** used in the AniList profile view today.
- Account top bar shows `Michi | @username` through the profile display name state, not a Platform app name.

## Shell And Home Responsibilities

- `ShellScreen` is the app-level orchestrator, closest equivalent to AL-Chan's `MainFragment`. It creates feature state holders, derives the topbar title, and routes the content area.
- `shell/state/ShellState.kt` holds: selected tab, selected animation/reading sections, account route, top-bar back handler, search active/query, and the current account profile draft.
- `shell` owns topbar search state for Home, Animation, and Reading via `ShellTopBar`. Home, Animation, and Reading share a topbar search interaction:
  - normal state shows topbar actions.
  - active search replaces the title area with an inline search field (`ShellSearchField`).
  - active search uses a left ChevronLeft/back affordance and system back closes search.
- `shell` owns account subroutes via `ShellAccountRouter`. Account main shows notification/settings actions; account detail routes show a back affordance and hide bottom navigation.
- Notifications and filter handlers are hoisted as parameters of `ShellTopBar` (`onNotificationsClick`, `onFilterClick`). They are currently passed as no-op lambdas from `ShellScreen`; wire them when the relevant features exist.
- Reusable Home/Discovery UI pieces belong in `core.platform.components`, currently in `PlatformHomeComponents`.
- `DiscoveryScreen` should remain mostly an assembler for Home sections and sample data.
- Home should not own an embedded search row while search is a shell-level topbar behavior.

## Account Profile Layout (AniList-aligned)

- Banner (`AccountBanner`) with gradient fallback when `bannerUrl` is null. Avatar (96dp circle) overhangs the banner by 48dp.
- Identity block: `@username`, display name, joined-date label (e.g. "Joined Mar 2024"), bio, and `Edit profile` / `Share profile` buttons.
- Stats row (`AccountStatsRow`): 4 cells — anime count, manga count, following, followers — formatted via `Int.toCompactCountLabel()`.
- Favorites rails (`AccountFavoriteSection`, generic over item type):
  - Favorite Animation → `AccountFavoriteMediaCard`
  - Favorite Reading → `AccountFavoriteMediaCard`
  - Favorite Characters → `AccountFavoritePersonCard`
  - Favorite Staff → `AccountFavoritePersonCard`
  - Favorite Studios → `AccountFavoriteStudioCard`
- Each rail has a "See more" affordance that will navigate to a dedicated browse surface when those exist.
- The Platform-style POSTS tab, gradient post grid, and `AccountTab` enum have been removed. Do not reintroduce them — AniList has no notion of profile posts.

## Settings Layout (AniList-aligned)

Settings groups and the rendering rules for each item type:

| Group | Items | Rendering |
|---|---|---|
| **App** | Theme · Language · Default tab | picker (radio, 3+ options) |
| **AniList** | Title language · 18+ content · Score format | picker / inline toggle |
| **Lists** | Default sort · Split completed anime · Split completed manga · Advanced scoring | picker / inline toggle |
| **Notifications** | Notifications (subscreen) | detail with 6 toggles |
| **Account** | Manage account · Add account · Log out | action (no detail) |
| **About** | Help · About | action / detail with version+credits |

- **Inline toggles** (`SettingsItemType.isInlineToggle`): `ADULT_CONTENT`, `SPLIT_COMPLETED_ANIME`, `SPLIT_COMPLETED_MANGA`, `ADVANCED_SCORING`. Rendered with `SettingsToggleRow` (Switch in trailing); values live in `SettingsState`.
- **Pickers** open a `SettingsDetailContent` subscreen with `SettingsRadioPicker<T>`. Enums for each option: `ThemeMode`, `TitleLanguage`, `ScoreFormat`, `ListSort`, `HomeTabOption`.
- **Theme**: maps `SYSTEM/LIGHT/DARK` → calls `onToggleTheme` only if the resolved dark state differs from the current `isDarkMode`. `App.kt` still owns `isDarkMode: Boolean`; `SettingsState.themeMode` reflects the user's picker selection.
- **Notifications**: `SettingsNotificationsDetail` renders 6 `SettingsToggleRow`s backed by `NotificationPreferences` in `SettingsState`.
- Legacy Platform strings removed: Privacy, Security, ContentPreferences, History, Interactions, ExperienceSection, Subtitles, Accessibility, DataPlayback, ToolsSection, CreatorTools, DarkModeTitle. Do not reintroduce.

## AniList/API Direction

- AniList uses GraphQL. Prefer typed or structured GraphQL handling over ad hoc string concatenation where feasible.
- Keep query/mutation definitions close to the repository/API surface that owns them.
- Keep AniList API concerns out of UI state and screens.
- Auth/session handling belongs behind `core.auth` or `core.session` abstractions (to be added).
- API DTOs should not leak into presentation state. Map DTOs to domain or presentation models at feature/core boundaries.
- Model network errors, GraphQL errors, rate limits, and empty states explicitly enough for UI to render them consistently.

## Naming Conventions

- Include feature/scope in file and class names.
- Use `Michi` for app-wide brand/theme primitives.
- Use `Shell` for shell-scoped components.
- Use `Platform` for reusable platform/chrome components.
- Use `AniList` for API/client/DTO-specific classes.
- Use feature prefixes such as `Discovery`, `Search`, `Animation`, `Reading`, `Media`, `Account`, and `Settings`.
- Avoid generic names like `TopBar`, `UserForm`, `ThemeSwitcher`, `LanguageSelector`, `Repository`, `ViewModel`, or `Screen` when a scoped name is clearer.

## Settings And Back Handling

- Settings is its own feature.
- Other features should not import `settings`; `shell` opens/closes settings inside the account route.
- Reusable topbar/system back behavior belongs in `core.platform`.
- Android system back should use `androidx.activity.compose.BackHandler`.
- iOS system back can remain no-op until native behavior is needed.

## Validation

- Run build commands from `/home/psyxho_skull/AndroidStudioProjects/luum/Michi`.
- Shared Android compile target used for this project: `./gradlew :shared:compileAndroidMain --offline`.
- When `shell` or feature signatures change, also verify `./gradlew :androidApp:compileDebugKotlin --offline`.
- Gradle may need `GRADLE_USER_HOME=/tmp/michi-gradle` in this environment.
- If Gradle fails on sandboxed locks or cache writes, rerun with the approved escalated Gradle prefix.

## Agent Memory

Primary Codex memory for this project:

- `/home/psyxho_skull/.codex/memories/michi-app-architecture.md`

Related source-of-truth memory for the donor project:

- `/home/psyxho_skull/.codex/memories/platform-app-architecture.md`

Use Platform memory only for reusable architectural principles. Michi's current code and this document win over Platform-specific product details. For AniList UX/feature direction, the reference clients (AL-Chan, Otraku) take precedence over Platform conventions.

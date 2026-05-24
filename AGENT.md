# Michi Agent Instructions

These instructions are for any AI agent working in this repository.

## Read First

1. Read `ARCHITECTURE.md`.
2. Read the shared project memory at `/home/psyxho_skull/.codex/memories/michi-app-architecture.md` if your environment can access it.
3. If you need to compare against the donor project, read `/home/psyxho_skull/.codex/memories/platform-app-architecture.md` and the local Platform project at `/home/psyxho_skull/AndroidStudioProjects/luum/Platform`.
4. For AniList UX/feature decisions, consult the reference clients before designing:
   - AL-Chan (Android/Kotlin): https://github.com/zend10/AL-chan
   - Otraku (Flutter/Dart): https://github.com/lotusprey/otraku

Michi's current code and `ARCHITECTURE.md` override Platform-specific details. AniList references win over Platform legacy for any user-facing surface.

## Project Identity

- Michi is a Kotlin Multiplatform app for anime/manga discovery and tracking.
- The external product/API target is AniList.
- Package base: `com.luum.michi.app`.
- Shared UI lives in `shared`.
- Platform entry points live in `androidApp` and `iosApp`.

## Architecture Rules

- `shell` coordinates top-level navigation, bottom tabs, account subroutes, top bar, and app-level back handling.
- Reusable UI chrome and platform abstractions belong in `core.platform` or `core.platform.components`.
- Features may depend on `core`.
- `shell` may depend on features.
- Top-level features must not import each other directly.
- `core` must not import features.
- Keep AniList API, auth/session, storage, and network concerns out of screens.
- Each feature's UI is structured as `presentation/{model,sample,state,components,util}/` plus the screen file. `domain/` and `data/` are added per feature only when real AniList/session/storage behavior appears — do not pre-create empty layers.
- Stateful features expose a `*ListStateHolder` (plain class with Compose state) plus a `remember*` factory. `shell` owns the holder when the topbar needs to read from it (e.g. section chips reading live counts).
- Sample/stub data lives in `presentation/sample/` with `*SampleEntries` / `*SampleStats` / `*SampleFavorites` naming so the swap to real repositories is mechanical.

## Current Feature Names

Use the current Michi vocabulary:

- `animation` for anime.
- `reading` for manga/reading.
- `account` for profile/account surfaces.
- `settings` for settings opened from account through `shell`.

Do not recreate `profile`. Do not introduce Platform concepts such as `feed`, `hubs`, `pods`, `shots`, posts feed, `PlatformBrand.NAAT`, or `PlatformBrand.NIK` unless the user explicitly requests them.

## Naming

- Prefer explicit file/class names with the feature or scope prefix.
- Good examples: `AccountEditProfileScreen`, `AnimationScreen`, `ReadingScreen`, `SettingsDetailContent`, `ShellBottomNavBar`, `AnimationListStateHolder`, `AccountFavoriteSection`.
- Avoid generic names like `ViewModel.kt`, `Repository.kt`, `Screen.kt`, `TopBar.kt`, or `LanguageSelector.kt`.
- Use `Michi*` for app-wide Michi brand primitives.
- Use `Platform*` only for reusable platform/chrome components and abstractions already following that convention.
- Use `AniList*` for API/client/DTO-specific classes.

## Current Implementation Notes

- Bottom tabs are `HOME`, `ANIMATION`, `READING`, and `ACCOUNT`.
- `SETTINGS` is an account subroute, not a bottom tab.
- `library` exists as scaffold debt and is not currently a bottom-tab destination.
- `DashboardScreen` (Home) has been consolidated to query AniList's 9 landing sections (trending, releasing, seasons, all-time popular, top media) in a single, unified GraphQL round-trip query (`DashboardQuery` inside `DashboardRepositoryImpl.kt`), massively reducing API latency and request count at startup.
- `AnimationScreen` and `ReadingScreen` are list/edit surfaces backed by `AnimationListStateHolder` and `ReadingListStateHolder` plus dynamic AniList integration.
- `ExploreScreen` is a fully interactive Catalog Browser supporting 5 navigation categories: `ANIMATION`, `READING`, `CHARACTERS`, `STAFF`, and `STUDIOS`. Changing tabs to non-media entities automatically collapses advanced filters. Blank search queries fallback to the most favorited entities of that category. Manga publishes range-based year filtering via fuzzy date constraints (`startDate_greater` / `startDate_lesser`) to bypass AniList's Manga season limitations.
- **Lazy Loading Network Strategy (HTTP 429 Prevention)**: To prevent strict AniList rate limits and unexpected logouts, state holders (Explore, Calendar, Animation, Reading, Account) do NOT load automatically inside constructor remember blocks. Instead:
  * Animation list, Reading list, and Account profile stats are loaded deferredly via a central `LaunchedEffect(shellState.selectedTab)` inside `ShellScreen.kt` only when the user selects their respective bottom tabs.
  * Explore and Calendar load on demand via screen-level `LaunchedEffect`s when composed.
  * `ExploreStateHolder.kt` debounces ALL search and filter changes by 300ms to prevent duplicate rapid requests when clicking chips.
- `AccountScreen` is AniList-aligned: banner + avatar + identity + stats row (anime / manga / following / followers) + favorites rails (anime, manga, characters, staff, studios). There is **no** posts grid; the Platform-style `AccountTab.POSTS` was removed.
- `AccountEditProfileScreen` and `AccountShareProfileScreen` are decomposed into `account/presentation/{components,model,sample,util}/`; the QR algorithm lives in `account/presentation/util/AccountQrMatrix.kt`.
- `SettingsScreen` is AniList-aligned with 6 groups: **App** (theme, language, default tab), **AniList** (title language, 18+ content, score format), **Lists** (sort, split completed, advanced scoring), **Notifications** (6 toggle types), **Account** (manage, add, logout), **About** (help, about). Legacy Platform groups (Creator tools, Subtitles, Data/Playback, Interactions, Accessibility section) have been removed. `SettingsState` holds all settings values; `ShellScreen` creates it via `rememberSettingsState()`. Pickers use `SettingsRadioPicker<T>`; inline toggles use `SettingsToggleRow`; `SettingsDetailContent` dispatches by `SettingsItemType`.
- `SearchScreen`, `LibraryScreen`, and `MediaScreen` are placeholder-level.
- `App.kt` owns theme and language state for now.
- The app supports Spanish and English through `core.language`. Android per-app language support is enabled for `es` and `en`; iOS declares both localizations.
- The app name `Michi` is a brand name. Do not move it into translatable XML unless the user changes direction.
- KMP template debris (`Greeting.kt`, `GreetingUtil.kt`, `Platform.kt` + Android/iOS actuals) has been deleted. Do not reintroduce.

## Current UI Decisions

- Bottom tab labels are singular:
  - Spanish: `Inicio`, `Animación`, `Lectura`, `Cuenta`.
  - English: `Home`, `Animation`, `Reading`, `Account`.
- Use `Animation`, not `Anime`, because AniList includes Japanese, Korean, and Chinese animated media.
- `ShellScreen` is a slim orchestrator; topbar logic lives in `shell/components/ShellTopBar.kt`, search field in `ShellSearchField.kt`, collapsing chips behavior in `ShellCollapsibleChips.kt`, and account routing in `ShellAccountRouter.kt`. State lives in `shell/state/ShellState.kt`.
- Topbar behavior:
  - Home left: Notifications. Home right: Search.
  - Active search replaces the title with an inline search field and changes the left icon to ChevronLeft.
  - Explore search and filters are hosted directly in `ShellTopBar` when the Explore overlay is open. The topbar search field has `autoFocus = false` to allow passive browsing without pop-up keyboard intrusions.
  - **Progressive Back Navigation**: Inside `ShellScreen.kt`, pressing system back or clicking the back arrow on the topbar when Explore is open first collapses the advanced filters panel if it is expanded, rather than closing the Explore overlay immediately.
  - Account main left: Notifications. Account main right: Settings.
  - Account detail routes use Back on the left and hide bottom navigation.
- Home has no banner/avatar and no embedded search row. Search belongs in the topbar.
- Reusable Home pieces live in `core.platform.components.PlatformHomeComponents`; keep `DiscoveryScreen` as an assembler.
- **Card Insignias & Icons**:
  * `SearchResultCard.kt` displays average rating in the top-right corner using the `PlatformIcons.Star` (star.xml) icon.
  * It displays popularity (members count) in the bottom-left corner (`Alignment.BottomStart`) using `PlatformIcons.Groups` (groups.xml) with a compact custom k/M formatter (e.g. `125.4k` / `1.2M`).
  * Personal Animation and Reading list cards render ratings with `PlatformScorePill` (defined in `PlatformMediaComponents.kt`), which has been updated to use `PlatformIcons.Star`, keeping heart icons (`PlatformIcons.Like`) reserved solely for user favorites.
- Reading has chapter progress only, but keeps separate `+1 CH` and `+1 VO` buttons because manga can have chapters and volumes.
- Animation increment button text is `+1 EP`.
- Keep counters numeric-only next to their corresponding buttons, styled with stronger weight.
- Next-release and behind-label support is prepared with `MediaReleaseDateTime`.
- Account stats use a compact count label (e.g. `1.8K`); see `AccountStats.toCompactCountLabel`.
- Account profile banner: `AccountBanner` has a gradient fallback when no `bannerUrl` is present; the avatar overhangs the banner by 48dp.
- Settings inline toggles (18+ content, split completed anime/manga, advanced scoring) are rendered with `SettingsToggleRow` directly in the list — no detail screen needed. Picker items (theme, language, title language, score format, list sort, default tab) open a `SettingsDetailContent` subscreen with a `SettingsRadioPicker`. The `THEME` picker maps `ThemeMode.SYSTEM/LIGHT/DARK` and calls `onToggleTheme` if the target dark state differs from the current one; App.kt still owns the `isDarkMode` boolean.

## Workflow

- Before structural edits, check `ARCHITECTURE.md` and nearby code.
- Before designing AniList feature surfaces, consult AL-Chan and Otraku to confirm the shape exists in AniList domain — do not invent social-network features that AniList lacks.
- Preserve user changes in the working tree. Do not revert unrelated edits.
- Prefer existing project patterns over introducing new frameworks.
- Validate shared code with `./gradlew :shared:compileAndroidMain --offline` from the repo root when changing Kotlin code. Also verify `./gradlew :androidApp:compileDebugKotlin --offline` when shell/feature signatures change.

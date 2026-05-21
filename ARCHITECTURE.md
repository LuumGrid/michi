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
- Manual state and dependency wiring.
- Android minSdk is read from `gradle/libs.versions.toml` and is currently `34`.
- Koin, Ktor, DataStore, and formal navigation are target technologies, not current dependencies.

## Current State

- `App.kt` owns dark/light theme state and app language state.
- `shell` coordinates the app host, top bar, bottom tabs, account subroutes, and back handling.
- Bottom tabs currently are `HOME`, `ANIMATION`, `READING`, and `ACCOUNT`.
- `SETTINGS` is not a bottom tab. It is opened inside the account route.
- `DiscoveryScreen`, `SearchScreen`, `LibraryScreen`, and `MediaScreen` are still placeholder-level surfaces.
- `AnimationScreen` and `ReadingScreen` have local prototype list/edit UI with stub data.
- `account` was copied/adapted from Platform and currently owns profile, edit profile, and share profile screens.
- `library` still exists as scaffold debt, but it is not a bottom-tab destination.
- `profile` should not be recreated; use `account`.

## Architecture Pattern

Use a feature-layer architecture with pragmatic Clean Architecture inside features when the feature has enough behavior to justify it:

```text
feature/
  domain/        pure models and repository contracts
  data/          implementations, DTOs, mappers
  presentation/  UI state, ViewModels, screens, components
```

Do not force empty layers for simple prototype screens. Add `domain` and `data` when real AniList/session/storage behavior appears.

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
  shell/             app host, top-level navigation, account subroutes, back handling
  core/
    anilist/          AniList GraphQL transport contracts and shared DTO/mappers when needed
    auth/             OAuth/session abstractions and token storage contracts
    language/         manual language/string model
    model/            neutral shared domain models, only when truly cross-feature
    network/          shared HTTP/GraphQL client setup and error mapping
    platform/         platform abstractions, brand/theme primitives, icons
      components/     reusable app chrome and UI primitives
    session/          current-user/session state when implemented
  account/            account/profile UI, edit profile, share profile
  animation/          anime list surfaces and future anime-specific flows
  reading/            manga/reading list surfaces and future manga-specific flows
  discovery/          trending, seasonal, recommendations, browse surfaces
  search/             anime/manga/character/staff/studio search
  media/              shared anime/manga media detail foundations
  library/            scaffold debt for personal lists; not currently in bottom nav
  settings/           app/account settings opened through shell
```

## Michi vs Platform

This project may reuse architectural ideas from `/home/psyxho_skull/AndroidStudioProjects/luum/Platform`, but Michi is a separate AniList app.

Do not copy Platform domain concepts into Michi unless the user explicitly asks for them. Avoid Platform-only terms such as `feed`, `hubs`, `pods`, `shots`, channels, memberships, `PlatformBrand.NAAT`, and `PlatformBrand.NIK`.

Michi uses:

- `MichiBrand`, not `PlatformBrand`.
- `Animation` for anime.
- `Reading` for manga/reading.
- `Account`, not `Profile`.
- `Platform*` prefixes only for reusable platform/chrome components, icons, and abstractions already named that way.

## Branding

- App-wide display name: `MichiAppName = "Michi"`.
- Current brand enum: `MichiBrand` with `ANIMATION` and `READING`.
- Current brand labels include `postsLabel`, `animationLabel`, and `readingLabel`.
- Account top bar should show `Michi | @username` through the profile display name state, not a Platform app name.

## AniList/API Direction

- AniList uses GraphQL. Prefer typed or structured GraphQL handling over ad hoc string concatenation where feasible.
- Keep query/mutation definitions close to the repository/API surface that owns them.
- Keep AniList API concerns out of UI state and screens.
- Auth/session handling belongs behind `core.auth` or `core.session` abstractions.
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
- Shared Android compile target used for this project: `./gradlew :shared:compileAndroidMain`.
- Gradle may need `GRADLE_USER_HOME=/tmp/michi-gradle` in this environment.
- If Gradle fails on sandboxed locks or cache writes, rerun with the approved escalated Gradle prefix.

## Agent Memory

Primary Codex memory for this project:

- `/home/psyxho_skull/.codex/memories/michi-app-architecture.md`

Related source-of-truth memory for the donor project:

- `/home/psyxho_skull/.codex/memories/platform-app-architecture.md`

Use Platform memory only for reusable architectural principles. Michi's current code and this document win over Platform-specific product details.

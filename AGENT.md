# Michi Agent Instructions

These instructions are for any AI agent working in this repository.

## Read First

1. Read `ARCHITECTURE.md`.
2. Read the shared project memory at `/home/psyxho_skull/.codex/memories/michi-app-architecture.md` if your environment can access it.
3. If you need to compare against the donor project, read `/home/psyxho_skull/.codex/memories/platform-app-architecture.md` and the local Platform project at `/home/psyxho_skull/AndroidStudioProjects/luum/Platform`.

Michi's current code and `ARCHITECTURE.md` override Platform-specific details.

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

## Current Feature Names

Use the current Michi vocabulary:

- `animation` for anime.
- `reading` for manga/reading.
- `account` for profile/account surfaces.
- `settings` for settings opened from account through `shell`.

Do not recreate `profile`. Do not introduce Platform concepts such as `feed`, `hubs`, `pods`, `shots`, `PlatformBrand.NAAT`, or `PlatformBrand.NIK` unless the user explicitly requests them.

## Naming

- Prefer explicit file/class names with the feature or scope prefix.
- Good examples: `AccountEditProfileScreen`, `AnimationScreen`, `ReadingScreen`, `SettingsDetailContent`, `ShellBottomNavBar`.
- Avoid generic names like `ViewModel.kt`, `Repository.kt`, `Screen.kt`, `TopBar.kt`, or `LanguageSelector.kt`.
- Use `Michi*` for app-wide Michi brand primitives.
- Use `Platform*` only for reusable platform/chrome components and abstractions already following that convention.
- Use `AniList*` for API/client/DTO-specific classes.

## Current Implementation Notes

- Bottom tabs are `HOME`, `ANIMATION`, `READING`, and `ACCOUNT`.
- `SETTINGS` is an account subroute, not a bottom tab.
- `library` exists as scaffold debt and is not currently a bottom-tab destination.
- `AnimationScreen` and `ReadingScreen` are local prototype list/edit surfaces with stub data.
- `DiscoveryScreen`, `SearchScreen`, `LibraryScreen`, and `MediaScreen` are placeholder-level.
- `App.kt` owns theme and language state for now.

## Workflow

- Before structural edits, check `ARCHITECTURE.md` and nearby code.
- Preserve user changes in the working tree. Do not revert unrelated edits.
- Prefer existing project patterns over introducing new frameworks.
- Validate shared code with `./gradlew :shared:compileAndroidMain` from the repo root when changing Kotlin code.

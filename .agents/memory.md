# Michi Agent Memory Pointer

This file exists for agents that cannot read Codex's internal memory list automatically.

## Primary Michi Memory

Read:

```text
/home/psyxho_skull/.codex/memories/michi-app-architecture.md
```

That memory contains the project-specific architectural context and recent decisions.

## Related Platform Memory

Read only when comparing Michi against the donor/reference project:

```text
/home/psyxho_skull/.codex/memories/platform-app-architecture.md
```

The Platform project itself is at:

```text
/home/psyxho_skull/AndroidStudioProjects/luum/Platform
```

## Rule Of Precedence

Michi's current code and `ARCHITECTURE.md` are the active source of truth for this repository.
Use Platform only for reusable architecture ideas, not for product vocabulary.

Avoid bringing Platform-only concepts into Michi unless the user explicitly asks for them:

- `feed`
- `hubs`
- `pods`
- `shots`
- channels
- memberships
- `PlatformBrand.NAAT`
- `PlatformBrand.NIK`

Current Michi vocabulary:

- `animation` means anime.
- `reading` means manga/reading.
- `account` replaces profile.
- `settings` is opened from account through `shell`.

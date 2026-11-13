# Agent Workflow

## Beads issue tracking

This project uses `bd` (Beads) as the source of truth for work tracking.

1. At the start of a task, run `bd prime`, then `bd ready` to find unblocked work.
2. If Beads is not initialized for the repository, run `bd init` before creating or updating issues.
3. Work on a Bead. Create one when no existing issue covers the requested work:
   ```bash
   bd create "<imperative title>" --type <bug|feature|task|chore> --priority <P0-P4>
   ```
   Include the intended outcome and acceptance criteria.
4. Claim work atomically before editing:
   ```bash
   bd update <bead-id> --claim
   ```
5. Record dependencies with `bd dep`, and record material decisions, blockers, and verification in `bd note` or `bd comment`.
6. Close an issue only after the requested behavior is verified:
   ```bash
   bd close <bead-id> --reason "<verification performed>"
   ```
7. Follow the synchronization guidance emitted by `bd prime`; use `bd dolt push` when the configured Beads remote must receive local issue updates.

Do not create Beads for trivial one-command changes. Do not close a Bead merely because code was written; close it when its acceptance criteria are met.

## API specification

`api.yml` is the source of truth for the current API. When API behavior, contracts, or documentation conflict, follow `api.yml`.

## Release versions and tags

Before committing a release-impacting change, inspect `versionCode` and `versionName` in `app/build.gradle` and the existing Git tags.

- Do not bump a version or create a tag for documentation, tooling, or ordinary development commits.
- For a release, increment `versionCode` monotonically and set `versionName` before creating the release commit.
- Build and verify the signed release APK from that commit before tagging it.
- Create an annotated Git tag after verification. Match the tag to `versionName` without a `v` prefix; existing project tags use this form, for example `1.6.9.2`.
- Never move, replace, or reuse an existing release tag. Do not commit generated APKs.


## Commit convention

Use Conventional Commits:

```text
<type>(<optional scope>): <imperative summary>
```

Allowed types: `feat`, `fix`, `docs`, `build`, `chore`, `refactor`, `test`, `ci`, `perf`.

Rules:

- One logical, independently reviewable change per commit.
- Write the summary in imperative mood, lowercase, without a trailing period; keep it at 72 characters or fewer.
- Use an optional scope only when it makes the affected area clearer, for example `fix(build): ...`.
- Add a body when the reason, compatibility impact, migration, or risk is not clear from the summary.
- Link tracked work in a footer: `Refs: <bead-id>`.
- Mark incompatible changes with `!` after the type or scope and a `BREAKING CHANGE:` footer.
- Run the narrow verification for the changed behavior before committing; mention it in the commit body when it is non-obvious.
- Stage explicit paths. Never commit secrets, local SDK paths, keystores, generated APKs, or unrelated user changes.

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:ca08a54f -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

## Session Completion

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd dolt push
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds
<!-- END BEADS INTEGRATION -->

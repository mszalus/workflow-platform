# Engineering Process

How a change travels from an idea to `main`. What to build (FR and NFR) lives in GitHub Issues
and `docs/design/`. This file covers only how we work.

## Sessions

| Session | Owns | Branches |
|---------|------|----------|
| Process session | This document, `.claude/` (settings, hooks, agents, commands), which CI gates exist, branch protection, issues labelled `process` | `chore/*` |
| Work sessions | All other issues: code, tests, docs, CI fixes | `feat/*`, `fix/*`, `docs/*` |

- Sessions share nothing but committed files and GitHub. A decision that is not written down in the repo or an issue does not exist for the other session.
- A session reads `CLAUDE.md` and `.claude/` when it starts. After a process PR merges, restart the work sessions or tell them to re-read `CLAUDE.md` and this file.
- A session works through its items one at a time (the "One item at a time" agreement in `CLAUDE.md`), in the main checkout. A worktree (`.claude/worktrees/`) is only for a second session running at the same time, or for a background session, which Claude Code requires to use one; either way it is one worktree per session, not per item.

## Tasks

- Open work is GitHub Issues. Milestones are the phases, worked in order; the `process` label marks work for the process session.
- Any session that finds a problem it is not fixing right now opens an issue for it (`gh issue create`), with the evidence.
- A PR that finishes an issue says `Closes #<n>` in its description, so the merge closes it.
- Designs longer than an issue belong in `docs/design/`, linked from the issues. Finished work from before the move to issues is in `docs/project-history.md`.

## Branches and merging

- `main` is protected. Changes land only through a PR, and `backend-build`, `frontend-build`, `helm-lint`, `acceptance-tests` and `e2e-playwright` must pass. Nobody pushes to `main` directly, admins and Claude included.
- One branch per logical change, named `feat/`, `fix/`, `docs/` or `chore/` plus a short slug. Branch from current `main` and merge within days.
- Claude opens the PR: as a draft while work is in progress, and ready for review once the local gates pass.
- **Only the human merges**, after reading the diff. Claude never merges, including its own `chore/` PRs.
- Merge with a merge commit. GitHub deletes the remote branch on merge; the session that owns the worktree then removes it (`git worktree remove <path>`) and deletes the local branch (`git branch -d`).

## Gates

| When | What runs | Defined in | Blocks |
|------|-----------|------------|--------|
| Claude runs `git commit` | `./gradlew test` and frontend unit tests | `.claude/hooks/pre-commit-tests.sh` | the commit |
| Claude runs `git push` | `./gradlew build` and frontend unit tests | `.claude/hooks/pre-push-tests.sh` | the push |
| PR opened or updated | `backend-build`; `frontend-build` (lint, format, typecheck, `npm audit`); `helm-lint` | `.github/workflows/ci.yml` | the merge |
| PR opened or updated | `acceptance-tests` (BDD against a fresh Docker stack) | `.github/workflows/ci.yml` | the merge |
| PR opened or updated | `e2e-playwright` (Playwright against a fresh Docker stack) | `.github/workflows/ci.yml` | the merge |
| PR opened or updated | Claude code review | `.github/workflows/claude-code-review.yml` | nothing, advisory |
| Push to `main` | `docker-build` (images and Trivy), plus the PR jobs | `.github/workflows/ci.yml` | nothing; a red run becomes an issue in the current Phase 0 milestone |
| Weekly | OWASP dependency check, `npm audit` | `.github/workflows/security.yml` | nothing |

- Both hooks skip the tests when every changed file is `*.md`, under `docs/` or under `.claude/`.
- The hooks fire only for commands Claude runs, not for git in your own terminal. Branch protection is the safety net; the hooks exist to fail fast.
- E2E tests run in CI, not locally before a push: CI starts a fresh stack, while every local checkout shares one Docker stack.
- Doc screenshots are not part of the E2E run. Refresh them on purpose with `cd e2e && npm run screenshots` and commit them in a `docs/` PR.
- Known gaps in these gates are issues labelled `process`: `gh issue list --label process`.

## Local Docker stack

There is one local stack per machine. The compose file uses fixed container names and host ports, so every checkout and worktree starts, stops and rebuilds the same containers. It is for manual testing and debugging; E2E tests run in CI.

- Rebuild before testing a branch locally: `docker compose -f docker/docker-compose.yml up -d --build`. Otherwise you test whatever was built last, possibly from another branch.
- Never run `docker compose down -v` while another session may be testing: it wipes the shared database.
- Data piles up across runs, so tests must not assume an empty database.

## Planning and review

Human attention goes to two checkpoints: approving the plan and reviewing the PR.

1. **Plan.** Work that involves a design choice or spans more than one file of logic starts in plan mode. The human approves the plan before code is written.
2. **Build.** The work session implements the plan and commits and pushes through the hooks.
3. **Self-review.** For changes to security, tenancy, events or the database schema, run `/code-review` before marking the PR ready.
4. **PR review.** The Claude review comments (advisory), CI gates the merge, and the human reads the diff and merges. The merge closes the issue.

## Delegating to subagents

Work sessions may delegate to the project subagents in `.claude/agents/`:

| Agent | Model | Use for |
|-------|-------|---------|
| `test-runner` | Haiku | Running test suites and reporting only the failures |
| `ci-triager` | Sonnet | Finding the root cause of a failed GitHub Actions run (read-only) |
| `Explore` (built in) | default | Broad read-only searches across the codebase |

- Delegate work that is mechanical and ends in a short summary, or that can run in parallel.
- Keep design, cross-service changes and anything touching tenancy or security in the main session.
- Don't delegate a task that takes less effort than explaining it. A subagent starts with no context.

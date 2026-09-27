# Engineering Process

How a change travels from an idea to `main`. What to build (FR and NFR) lives in
[PLAN.md](../PLAN.md). This file covers only how we work.

## Sessions

| Session | Owns | Branches |
|---------|------|----------|
| Process session | This document, `.claude/` (settings, hooks, agents, commands), which CI gates exist, branch protection | `chore/*` |
| Work sessions | PLAN.md items: code, tests, docs, CI fixes | `feat/*`, `fix/*`, `docs/*` |

- Sessions share nothing but committed files. A decision that is not written down in the repo does not exist for the other session.
- A session reads `CLAUDE.md` and `.claude/` when it starts. After a process PR merges, restart the work sessions or tell them to re-read `CLAUDE.md` and this file.
- Parallel sessions each work in their own git worktree (`.claude/worktrees/`), never in the same checkout.

## Branches and merging

- `main` is protected. Changes land only through a PR, and `backend-build`, `frontend-build` and `helm-lint` must pass. Nobody pushes to `main` directly, admins and Claude included.
- One branch per logical change, named `feat/`, `fix/`, `docs/` or `chore/` plus a short slug. Branch from current `main` and merge within days.
- Claude opens the PR: as a draft while work is in progress, and ready for review once the local gates pass.
- **Only the human merges**, after reading the diff. Claude never merges, including its own `chore/` PRs.
- Merge with a merge commit and delete the branch afterwards.

## Gates

| When | What runs | Defined in | Blocks |
|------|-----------|------------|--------|
| Claude runs `git commit` | `./gradlew test` and frontend unit tests | `.claude/hooks/pre-commit-tests.sh` | the commit |
| Claude runs `git push` | `./gradlew build`, frontend unit tests, Playwright E2E (starts the Docker stack if it is down) | `.claude/hooks/pre-push-tests.sh` | the push |
| PR opened or updated | `backend-build`; `frontend-build` (lint, format, typecheck, `npm audit`); `helm-lint` | `.github/workflows/ci.yml` | the merge |
| PR opened or updated | `acceptance-tests` (BDD against the Docker stack) | `.github/workflows/ci.yml` | nothing yet; becomes required once it is green on `main` |
| PR opened or updated | Claude code review | `.github/workflows/claude-code-review.yml` | nothing, advisory |
| Push to `main` | `docker-build` (images and Trivy), `acceptance-tests` | `.github/workflows/ci.yml` | nothing; a red run becomes a Phase 0 item in PLAN.md |
| Weekly | OWASP dependency check, `npm audit` | `.github/workflows/security.yml` | nothing |

- Both hooks skip the tests when every changed file is `*.md`, under `docs/` or under `.claude/`.
- The hooks fire only for commands Claude runs, not for git in your own terminal. Branch protection is the safety net; the hooks exist to fail fast.

Known gaps:

- `acceptance-tests` runs on PRs but is not required, because it is still red on `main`. Once it is green, add it to the required checks (`gh api -X PUT repos/mszalus/workflow-platform/branches/main/protection`).
- Playwright E2E runs only in the pre-push hook, so a PR can pass CI and still break it. Move it into CI next to `acceptance-tests`, then cut the pre-push hook down to `./gradlew build`.
- `claude-review` fails after about 2 seconds at $0 cost: the API call is rejected before any model use, which points to `CLAUDE_CODE_OAUTH_TOKEN`. Regenerate it (PLAN.md, Phase 0).
- All worktrees share one Docker stack (fixed container names), so pre-push E2E tests whichever images are running.

## Planning and review

Human attention goes to two checkpoints: approving the plan and reviewing the PR.

1. **Plan.** Work that involves a design choice or spans more than one file of logic starts in plan mode. The human approves the plan before code is written.
2. **Build.** The work session implements the plan, commits and pushes through the hooks, and ticks the PLAN.md checkbox in the same PR.
3. **Self-review.** For changes to security, tenancy, events or the database schema, run `/code-review` before marking the PR ready.
4. **PR review.** The Claude review comments (advisory), CI gates the merge, and the human reads the diff and merges.

## Delegating to subagents

Work sessions may delegate to the project subagents in `.claude/agents/`:

| Agent | Model | Use for |
|-------|-------|---------|
| `test-runner` | Haiku | Running test suites and reporting only the failures |
| `vault-rebuilder` | Haiku | Regenerating `docs/vault` and fixing broken links in the generator or source docs |
| `ci-triager` | Sonnet | Finding the root cause of a failed GitHub Actions run (read-only) |
| `Explore` (built in) | default | Broad read-only searches across the codebase |

- Delegate work that is mechanical and ends in a short summary, or that can run in parallel.
- Keep design, cross-service changes and anything touching tenancy or security in the main session.
- Don't delegate a task that takes less effort than explaining it. A subagent starts with no context.

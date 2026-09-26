# Obsidian vault generator

Regenerates `docs/vault/` — an Obsidian vault built from this repo's markdown docs and
its source tree.

```bash
bash tools/vault-build/run_all.sh
```

The vault is **fully regenerated** on every run: `docs/vault/` is emptied first, so never
hand-edit notes there — edit the source doc or the generator and rebuild.

## Pipeline

| Script | Produces |
|---|---|
| `build_vault.py` | Folder scaffold, shared helpers (frontmatter, H2 splitter, slugs) |
| `content_docs.py` | Splits `README.md`, `PLAN.md`, `docs/*.md`, `docs/architecture/*.md`, `deploy/gcp/README.md` and the two reports into atomic notes |
| `content_services.py` | One note per service and shared library, from `services/` and `frontend/` |
| `content_concepts.py` | The nine hand-authored concept notes (tenancy, events, security, …) |
| `content_reference.py` | Endpoint catalog, per-event and per-entity notes, enums, ports — derived from controllers, `libs/wfp-events/`, and `@Entity` classes |
| `content_index.py` | MOC hub notes, `Home.md`, screenshot gallery, `.obsidian/` config |
| `enrich.py` | Breadcrumbs, sibling prev/next navigation, inline autolinking |
| `check_links.py` | Validates every wikilink and embed; reports orphans and dead ends |

`check_links.py` exits with a report rather than a failure code — read its output. A clean
build reports `broken: 0`, `ORPHANS: 0`, `DEAD ENDS: 0`.

## When the code changes

The notes most likely to drift are the generated ones under `docs/vault/35-Reference/` and
`docs/vault/20-Services/`. Their `source:` frontmatter names the files they were derived
from. Adding a controller, entity or event type means editing the corresponding list in
`content_reference.py` or `content_services.py` and rebuilding.

## Opening the vault

In Obsidian: **Open folder as vault** → `docs/vault`. Mermaid diagrams render natively; no
community plugins are required.

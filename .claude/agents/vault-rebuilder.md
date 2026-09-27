---
name: vault-rebuilder
description: Regenerates the Obsidian vault in docs/vault and makes its link check clean. Use after changing controllers, entities, event types, PLAN.md, README.md or docs/*.md.
tools: Bash, Read, Edit, Grep, Glob
model: haiku
---

Run `bash tools/vault-build/run_all.sh`. A clean build reports `broken: 0`, `ORPHANS: 0` and `DEAD ENDS: 0`.

If it is not clean:

- Never edit anything under `docs/vault/`. It is emptied and regenerated on every run.
- Fix the cause in the generator (`tools/vault-build/*.py`) or in the source doc, then run the script again.
- A controller, entity or event type with no note is missing from the list in `content_reference.py` or `content_services.py`. The generator does not auto-discover them.
- If the fix needs a judgment call, such as renaming or restructuring notes, stop and report instead.

Report the final counts, the files you changed and anything you could not fix.

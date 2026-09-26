#!/usr/bin/env python3
"""Validate wikilinks and embeds, and report orphans/dead ends."""
import re, collections, pathlib

VAULT = pathlib.Path(__file__).resolve().parents[2] / "docs" / "vault"
notes = {p.stem: p for p in VAULT.rglob("*.md")}
attach = {p.name for p in (VAULT / "attachments").iterdir()} if (VAULT / "attachments").exists() else set()

link_re = re.compile(r"(!?)\[\[([^\]|#]+)(?:#[^\]|]*)?(?:\|([^\]]*))?\]\]")
broken = collections.defaultdict(list)
inbound = collections.Counter()
outbound = collections.Counter()

for stem, p in sorted(notes.items()):
    text = p.read_text(encoding="utf-8")
    body, fences = [], False
    for ln in text.split("\n"):
        if ln.startswith("```"):
            fences = not fences
        if not fences:
            body.append(ln)
    for embed, target, _alias in link_re.findall("\n".join(body)):
        target = target.strip()
        if embed:
            if target not in attach:
                broken[stem].append(f"!{target}")
            continue
        outbound[stem] += 1
        if target in notes:
            inbound[target] += 1
        else:
            broken[stem].append(target)

print(f"notes: {len(notes)}   attachments: {len(attach)}")
print(f"total wikilinks: {sum(outbound.values())}   broken: {sum(len(v) for v in broken.values())}\n")
if broken:
    print("BROKEN LINKS")
    for stem, targets in sorted(broken.items()):
        print(f"  {stem}")
        for t in sorted(set(targets)):
            print(f"      -> {t}")
orphans = [s for s in notes if inbound[s] == 0 and s != "Home"]
deadends = [s for s in notes if outbound[s] == 0]
print(f"\nORPHANS (no inbound links): {len(orphans)}")
for s in sorted(orphans):
    print(f"  {s}")
print(f"\nDEAD ENDS (no outbound links): {len(deadends)}")
for s in sorted(deadends):
    print(f"  {s}")

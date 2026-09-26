#!/usr/bin/env python3
"""Build an Obsidian vault at docs/vault from repo docs + code-derived notes."""
import os, re, shutil, json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
VAULT = ROOT / "docs" / "vault"

FOLDERS = ["00-Index", "10-Architecture", "20-Services", "30-Concepts",
           "35-Reference", "40-Operations", "50-Manuals", "60-Project",
           "attachments", ".obsidian"]

def slugify(s):
    s = re.sub(r"[`*_]", "", s).strip()
    s = re.sub(r"\s*[—–-]\s*(CODE DONE|DONE|BLOCKED|PARTIAL|NOT STARTED).*$", "", s, flags=re.I)
    s = re.sub(r"[^\w\s&/-]", "", s)
    s = s.replace("/", " ").replace("&", "and")
    s = re.sub(r"\s+", " ", s).strip()
    return s

def fm(title, tags, note_type, source=None, extra=None):
    lines = ["---", f"title: {title}", "tags:"]
    lines += [f"  - {t}" for t in tags]
    lines.append(f"type: {note_type}")
    if source:
        lines.append(f"source: {source}")
    if extra:
        for k, v in extra.items():
            lines.append(f"{k}: {v}")
    lines.append("---")
    return "\n".join(lines) + "\n\n"

def write(folder, name, body):
    p = VAULT / folder / f"{name}.md"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(body, encoding="utf-8")
    return name

def split_h2(path, drop_headings=()):
    """Split a markdown file into (title, body) chunks at H2 boundaries."""
    text = (ROOT / path).read_text(encoding="utf-8")
    lines = text.split("\n")
    chunks, cur_title, cur, in_fence = [], None, [], False
    preamble = []
    for ln in lines:
        if ln.startswith("```"):
            in_fence = not in_fence
        if not in_fence and ln.startswith("## "):
            if cur_title:
                chunks.append((cur_title, "\n".join(cur).strip()))
            cur_title, cur = ln[3:].strip(), []
        elif cur_title is None:
            preamble.append(ln)
        else:
            cur.append(ln)
    if cur_title:
        chunks.append((cur_title, "\n".join(cur).strip()))
    chunks = [c for c in chunks if slugify(c[0]) not in drop_headings]
    return "\n".join(preamble).strip(), chunks

def demote(body):
    """Shift H3+ up one level so a split section reads as its own note."""
    out, in_fence = [], False
    for ln in body.split("\n"):
        if ln.startswith("```"):
            in_fence = not in_fence
        if not in_fence and re.match(r"^#{3,6} ", ln):
            ln = ln[1:]
        out.append(ln)
    return "\n".join(out)

# --- link table: term -> note name (applied outside code fences, first hit only)
LINKS = {}

def register(names):
    for n in names:
        LINKS.setdefault(n, n)

ALIASES = {}

def autolink(body, self_name):
    out, in_fence, used = [], False, set()
    for ln in body.split("\n"):
        if ln.startswith("```"):
            in_fence = not in_fence
            out.append(ln); continue
        if in_fence or ln.startswith("|--") or ln.startswith("#"):
            out.append(ln); continue
        for term, note in sorted(ALIASES.items(), key=lambda kv: -len(kv[0])):
            if note == self_name or note in used:
                continue
            pat = re.compile(r"(?<![\w`\[/-])" + re.escape(term) + r"(?![\w`\]/-])")
            m = pat.search(ln)
            if m and "[[" not in ln[max(0, m.start()-2):m.start()+2]:
                repl = f"[[{note}]]" if term == note else f"[[{note}|{term}]]"
                ln = pat.sub(repl, ln, count=1)
                used.add(note)
        out.append(ln)
    return "\n".join(out)

if __name__ == "__main__":
    if VAULT.exists():
        for child in VAULT.iterdir():
            shutil.rmtree(child) if child.is_dir() else child.unlink()
    for f in FOLDERS:
        (VAULT / f).mkdir(parents=True, exist_ok=True)
    print("scaffold ok:", VAULT)

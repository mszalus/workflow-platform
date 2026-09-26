#!/usr/bin/env python3
"""Split the repo's existing markdown docs into atomic Obsidian notes."""
import re, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from build_vault import ROOT, VAULT, slugify, fm, write, split_h2, demote

anchor_map = {}   # "#anchor" -> note name, per source doc
created = []

def note(folder, name, title, tags, ntype, source, body, extra=None):
    write(folder, name, fm(title, tags, ntype, source, extra) + body.strip() + "\n")
    created.append((folder, name))
    return name

# ---------------------------------------------------------------- architecture
ARCH = {
    "docs/architecture/c4-context.md":        ("C4 L1 System Context", "c4/l1-context"),
    "docs/architecture/c4-container.md":      ("C4 L2 Container", "c4/l2-container"),
    "docs/architecture/c4-component-workflow-service.md":     ("C4 L3 Workflow Service", "c4/l3-component"),
    "docs/architecture/c4-component-gateway.md":              ("C4 L3 API Gateway", "c4/l3-component"),
    "docs/architecture/c4-component-notification-service.md": ("C4 L3 Notification Service", "c4/l3-component"),
    "docs/architecture/data-model.md":        ("Data Model ERD", "data-model"),
}
FILE_TO_NOTE = {
    "c4-context.md": "C4 L1 System Context",
    "c4-container.md": "C4 L2 Container",
    "c4-component-workflow-service.md": "C4 L3 Workflow Service",
    "c4-component-gateway.md": "C4 L3 API Gateway",
    "c4-component-notification-service.md": "C4 L3 Notification Service",
    "c4-deployment.md": "Deployment Topologies",
    "data-model.md": "Data Model ERD",
}

def fix_md_links(body):
    def rep(m):
        target = m.group(2).split("#")[0]
        n = FILE_TO_NOTE.get(target)
        return f"[[{n}|{m.group(1)}]]" if n else m.group(0)
    return re.sub(r"\[([^\]]+)\]\(([a-z0-9-]+\.md)(#[^)]*)?\)", rep, body)

for path, (name, tag) in ARCH.items():
    text = (ROOT / path).read_text(encoding="utf-8")
    body = "\n".join(text.split("\n")[1:]).strip()
    note("10-Architecture", name, name, ["architecture", tag], "architecture",
         path, fix_md_links(body))

# deployment doc -> three notes, one per topology
_, dep = split_h2("docs/architecture/c4-deployment.md")
DEP_NAMES = {"Docker Compose Deployment": "Deployment — Docker Compose",
             "GCP Deployment": "Deployment — GCP",
             "Kubernetes Deployment Helm": "Deployment — Kubernetes"}
dep_notes = []
for title, body in dep:
    n = DEP_NAMES.get(slugify(title))
    if not n:
        continue
    dep_notes.append(n)
    note("10-Architecture", n, n, ["architecture", "deployment"], "architecture",
         "docs/architecture/c4-deployment.md", fix_md_links(demote(body)))

note("10-Architecture", "Deployment Topologies", "Deployment Topologies",
     ["architecture", "deployment", "moc"], "moc", "docs/architecture/c4-deployment.md",
     "C4 Level 4. Three deployment targets share the same images and Helm/compose "
     "definitions but differ in data stores and ingress.\n\n"
     + "\n".join(f"- [[{n}]]" for n in dep_notes)
     + "\n\nSee also [[C4 L2 Container]] for the logical container inventory.")

arch_readme = (ROOT / "docs/architecture/README.md").read_text(encoding="utf-8")
note("10-Architecture", "Diagram Conventions", "Diagram Conventions",
     ["architecture", "conventions"], "reference", "docs/architecture/README.md",
     fix_md_links("\n".join(arch_readme.split("\n")[1:]).strip()))

# ---------------------------------------------------------------- manuals
def split_doc(path, folder, prefix, tags, ntype, name_overrides=None, drop=("Table of Contents",)):
    name_overrides = name_overrides or {}
    pre, chunks = split_h2(path, drop_headings=tuple(slugify(d) for d in drop))
    names = []
    for title, body in chunks:
        base = name_overrides.get(slugify(title), slugify(title))
        n = f"{prefix}{base}" if prefix else base
        names.append((n, title))
        note(folder, n, n, tags, ntype, path, demote(body))
    return pre, names

_, admin_names = split_doc("docs/admin-manual.md", "50-Manuals", "Admin — ",
                           ["manual", "admin"], "manual")
_, user_names = split_doc("docs/user-manual.md", "50-Manuals", "User — ",
                          ["manual", "end-user"], "manual")

# ---------------------------------------------------------------- operations
_, gcp_vm = split_doc("deploy/gcp/README.md", "40-Operations", "GCP VM — ",
                      ["ops", "gcp", "docker"], "runbook", drop=())
_, obs = split_doc("observability-report.md", "40-Operations", "Observability — ",
                   ["ops", "observability"], "report", drop=())
_, tfrep = split_doc("terraform-helm-gcp-report.md", "40-Operations", "Terraform — ",
                     ["ops", "gcp", "terraform", "helm"], "report", drop=())

# ---------------------------------------------------------------- project plan
def plan_status(title):
    if re.search(r"CODE DONE|PARTIAL", title):
        return "partial"
    if re.search(r"DONE", title):
        return "done"
    if re.search(r"BLOCKED", title):
        return "blocked"
    return "open"

_, plan = split_h2("PLAN.md")
plan_names = []
for title, body in plan:
    status = plan_status(title)
    n = {"Context": "Project Context", "Verification": "Verification Log",
         "Commit strategy": "Commit Strategy"}.get(slugify(title), slugify(title))
    m = re.match(r"Step (\d+) (.*)", n)
    if m:
        n = f"Step {int(m.group(1)):02d} — {m.group(2)}"
    plan_names.append((n, status))
    note("60-Project", n, n, ["project", "plan", f"status/{status}"], "plan",
         "PLAN.md", demote(body), extra={"status": status})

# ---------------------------------------------------------------- readme
_, readme = split_doc("README.md", "40-Operations", "", ["ops"], "reference",
                      name_overrides={
                          "Tech Stack": "Tech Stack",
                          "Prerequisites": "Prerequisites",
                          "Quick Start Docker Compose": "Quick Start",
                          "Local Development": "Local Development",
                          "Project Structure": "Repository Layout",
                          "CI CD": "CI Pipeline",
                          "Kubernetes Deployment": "Helm and Kubernetes",
                      },
                      drop=("Architecture", "API Endpoints", "Multi-Tenancy", "Testing"))

import json
(VAULT / ".build-index.json").write_text(json.dumps({
    "created": created,
    "admin": [n for n, _ in admin_names],
    "user": [n for n, _ in user_names],
    "plan": plan_names,
    "gcp_vm": [n for n, _ in gcp_vm],
    "obs": [n for n, _ in obs],
    "tfrep": [n for n, _ in tfrep],
    "readme": [n for n, _ in readme],
    "deployment": dep_notes,
}, indent=2), encoding="utf-8")
print(f"{len(created)} notes from existing docs")
for f, n in created:
    print(f"  {f}/{n}")

#!/usr/bin/env python3
"""Second pass: breadcrumbs, sibling navigation, and inline autolinking.

Split notes arrive as leaf nodes. This turns them into graph citizens.
"""
import re, json, pathlib

VAULT = pathlib.Path(__file__).resolve().parents[2] / "docs" / "vault"
NOTES = {p.stem: p for p in VAULT.rglob("*.md")}
IDX = json.loads((VAULT / ".build-index.json").read_text(encoding="utf-8"))

# Document order, as the source doc presented it — not alphabetical.
DOC_ORDER = {}
for key in ("admin", "user", "gcp_vm", "obs", "tfrep", "readme", "deployment"):
    for i, name in enumerate(IDX[key]):
        DOC_ORDER[name] = i
for i, (name, _status) in enumerate(IDX["plan"]):
    DOC_ORDER[name] = i

# ---------------------------------------------------------------- families
def family(prefix, folder):
    members = [p.stem for p in (VAULT / folder).glob(f"{prefix}*.md")]
    return sorted(members, key=lambda m: (DOC_ORDER.get(m, 999), m))

FAMILIES = [
    # (member list, breadcrumb trail, footer heading)
    (family("Admin — ", "50-Manuals"), ["Manuals MOC", "Admin Portal"], "Admin manual"),
    (family("User — ", "50-Manuals"), ["Manuals MOC", "User Portal"], "User manual"),
    (family("GCP VM — ", "40-Operations"), ["Operations MOC"], "GCP VM runbook"),
    (family("Observability — ", "40-Operations"), ["Operations MOC", "Observability Stack"], "Observability report"),
    (family("Terraform — ", "40-Operations"), ["Operations MOC"], "Terraform + Helm on GCP"),
    (family("Deployment — ", "10-Architecture"), ["Architecture MOC", "Deployment Topologies"], "Deployment topologies"),
]

PLAN = sorted((p.stem for p in (VAULT / "60-Project").glob("*.md")),
             key=lambda m: (DOC_ORDER.get(m, 999), m))
FAMILIES.append((PLAN, ["Project MOC"], "Plan"))

README_OPS = ["Prerequisites", "Quick Start", "Local Development", "Tech Stack",
              "Repository Layout", "Build Commands", "Docker Compose Stack",
              "CI Pipeline", "Helm and Kubernetes", "Observability Stack",
              "Ports and Endpoints"]
FAMILIES.append(([x for x in README_OPS if x in NOTES], ["Operations MOC"], "Running and shipping"))

ARCH = ["C4 L1 System Context", "C4 L2 Container", "C4 L3 API Gateway",
        "C4 L3 Workflow Service", "C4 L3 Notification Service",
        "Deployment Topologies", "Data Model ERD", "Diagram Conventions"]
FAMILIES.append(([x for x in ARCH if x in NOTES], ["Architecture MOC"], "Architecture set"))

# ---------------------------------------------------------------- autolink map
TERMS = {
    "workflow-service": "Workflow Service", "workflow service": "Workflow Service",
    "Workflow Service": "Workflow Service",
    "custom-fields-service": "Custom Fields Service",
    "Custom Fields Service": "Custom Fields Service",
    "custom fields service": "Custom Fields Service",
    "notification-service": "Notification Service",
    "Notification Service": "Notification Service",
    "notification service": "Notification Service",
    "audit-service": "Audit Service", "Audit Service": "Audit Service",
    "audit service": "Audit Service",
    "API Gateway": "API Gateway", "api gateway": "API Gateway",
    "admin-portal": "Admin Portal", "Admin Portal": "Admin Portal",
    "admin portal": "Admin Portal",
    "user-portal": "User Portal", "User Portal": "User Portal",
    "user portal": "User Portal",
    "Flowable": "Flowable Engine", "Flowable engine": "Flowable Engine",
    "RabbitMQ": "Event System",
    "Keycloak": "Security and JWT",
    "multi-tenancy": "Multi-Tenancy", "Multi-Tenancy": "Multi-Tenancy",
    "tenant isolation": "Multi-Tenancy", "Tenant Isolation": "Multi-Tenancy",
    "Testcontainers": "Testing Strategy",
    "Playwright": "Testing Strategy",
    "Helm": "Helm and Kubernetes",
    "Grafana": "Observability Stack", "Prometheus": "Observability Stack",
    "Tempo": "Observability Stack",
    "Gradle": "Build System",
    "bpmn-js": "bpmn-editor",
    "shared-ui": "shared-ui",
    "FieldSchema": "FieldSchema", "FieldValue": "FieldValue", "FieldOption": "FieldOption",
    "AuditEntry": "AuditEntry", "ProcessMetadata": "ProcessMetadata",
    "NotificationPreference": "NotificationPreference",
    "task.created": "task.created", "task.assigned": "task.assigned",
    "task.completed": "task.completed", "task.delegated": "task.delegated",
    "process.started": "process.started", "process.completed": "process.completed",
    "process.cancelled": "process.cancelled",
    "PagedResponse": "wfp-common", "ErrorResponse": "wfp-common",
    "TenantContext": "wfp-security", "JwtTestHelper": "wfp-test-support",
    "BaseEvent": "wfp-events", "EventConstants": "wfp-events",
}
TERMS = {t: n for t, n in TERMS.items() if n in NOTES}
ORDERED = sorted(TERMS.items(), key=lambda kv: -len(kv[0]))

CODE_SPAN = re.compile(r"`[^`]*`")

def autolink(body, self_name, budget=14):
    out, in_fence, used, count = [], False, set(), 0
    for ln in body.split("\n"):
        if ln.lstrip().startswith("```"):
            in_fence = not in_fence
            out.append(ln); continue
        if in_fence or ln.startswith("#") or ln.startswith(">") or "[[" in ln \
           or re.match(r"^\s*\|[\s:|-]+\|\s*$", ln) or count >= budget:
            out.append(ln); continue
        masked, spans = [], []

        def mask(m):
            spans.append(m.group(0))
            return f"\x00{len(spans)-1}\x00"
        work = CODE_SPAN.sub(mask, ln)
        for term, target in ORDERED:
            if target == self_name or target in used or count >= budget:
                continue
            pat = re.compile(r"(?<![\w\[/-])(?<!\w\.)" + re.escape(term)
                             + r"(?![\w\]/-]|\.\w)")
            if pat.search(work):
                repl = f"[[{target}]]" if term == target else f"[[{target}|{term}]]"
                work = pat.sub(repl.replace("\\", "\\\\"), work, count=1)
                used.add(target); count += 1
        work = re.sub(r"\x00(\d+)\x00", lambda m: spans[int(m.group(1))], work)
        out.append(work)
    return "\n".join(out), count

# ---------------------------------------------------------------- apply
def split_frontmatter(text):
    if text.startswith("---\n"):
        end = text.index("\n---\n", 4) + 5
        return text[:end], text[end:]
    return "", text

member_of = {}
for members, trail, label in FAMILIES:
    for m in members:
        member_of.setdefault(m, (members, trail, label))

stats = {"nav": 0, "links": 0, "notes": 0}
for stem, path in sorted(NOTES.items()):
    if stem == "Home" or path.parent.name == "00-Index":
        continue
    text = path.read_text(encoding="utf-8")
    front, body = split_frontmatter(text)
    body = body.replace("[[README — API Endpoints|README summary]]", "[[Ports and Endpoints]]")

    body, added = autolink(body, stem)
    stats["links"] += added

    if stem in member_of:
        members, trail, label = member_of[stem]
        i = members.index(stem)
        crumb = " › ".join(f"[[{t}]]" for t in trail) + f" › **{stem}**"
        nav = []
        if i > 0:
            nav.append(f"← [[{members[i-1]}]]")
        if i < len(members) - 1:
            nav.append(f"[[{members[i+1]}]] →")
        siblings = " · ".join(f"[[{m}]]" for m in members if m != stem)
        footer = (f"\n\n---\n\n**{label}** — {' · '.join(nav) if nav else 'single note'}\n\n"
                  f"> [!abstract]- All notes in this set\n"
                  + "\n".join(f"> {line}" for line in siblings.split(" · ")) + "\n")
        body = crumb + "\n\n" + body.lstrip("\n") + footer
        stats["nav"] += 1

    path.write_text(front + body.rstrip() + "\n", encoding="utf-8")
    stats["notes"] += 1

print(stats)

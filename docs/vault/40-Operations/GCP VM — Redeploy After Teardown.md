---
title: GCP VM — Redeploy After Teardown
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — Redeploy After Teardown**

If you previously ran `teardown.sh` (which deletes everything), just run `setup.sh` again:

```bash
cd deploy/gcp
bash setup.sh
```

That's it. The script is idempotent — it creates only what doesn't exist.


---

**GCP VM runbook** — ← [[GCP VM — Login Credentials]] · [[GCP VM — Cost Control]] →

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Deploy from Scratch]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Cost Control]]
> [[GCP VM — Updating After Code Changes]]
> [[GCP VM — Troubleshooting]]
> [[GCP VM — Key Deployment Notes]]
> [[GCP VM — Architecture]]
> [[GCP VM — File Reference]]

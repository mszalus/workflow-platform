---
title: GCP VM — Troubleshooting
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — Troubleshooting**

## Check if all containers are running
```bash
gcloud compute ssh wfp-vm --zone=us-central1-a \
  --command="sudo docker compose -f /opt/wfp/docker-compose.yml ps"
```

## View service logs
```bash
# All services
gcloud compute ssh wfp-vm --zone=us-central1-a \
  --command="sudo docker compose -f /opt/wfp/docker-compose.yml logs --tail=20"

# Specific service
gcloud compute ssh wfp-vm --zone=us-central1-a \
  --command="sudo docker compose -f /opt/wfp/docker-compose.yml logs --tail=20 workflow-service"
```

## Check health endpoints
```bash
EXTERNAL_IP=$(gcloud compute instances describe wfp-vm --zone=us-central1-a \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

curl http://$EXTERNAL_IP:9080/actuator/health
```

## Services return 401 Unauthorized
The JWT issuer URI must match the VM's external IP. The deploy script auto-detects this,
but if the IP changes (e.g., after stop/start), you need to update the docker-compose:
```bash
gcloud compute ssh wfp-vm --zone=us-central1-a --command="sudo bash /tmp/deploy-services.sh"
```

## Services return 403 on POST/PUT/DELETE
The gateway's CORS config only allows localhost origins. The nginx proxy strips the
Origin header to work around this. If you rebuild frontend images, make sure the
nginx.conf includes `proxy_set_header Origin "";`.


---

**GCP VM runbook** — ← [[GCP VM — Updating After Code Changes]] · [[GCP VM — Key Deployment Notes]] →

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Deploy from Scratch]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Redeploy After Teardown]]
> [[GCP VM — Cost Control]]
> [[GCP VM — Updating After Code Changes]]
> [[GCP VM — Key Deployment Notes]]
> [[GCP VM — Architecture]]
> [[GCP VM — File Reference]]

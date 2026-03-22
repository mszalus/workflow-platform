# GCP Deployment — Single VM with Docker Compose

Deploy the entire workflow platform on a single GCP VM running Docker Compose.
Cost: ~$25/mo (e2-medium) or $0 when stopped/torn down.

## Prerequisites

1. **GCP account** with billing enabled
2. **gcloud CLI** installed and authenticated: https://cloud.google.com/sdk/docs/install
3. **Docker** installed locally (to build and push images)

## Deploy from Scratch

One command sets up everything (registry, images, VM, firewall, scheduler):

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

cd deploy/gcp
bash setup.sh
```

This takes ~15 minutes. The script will:
1. Enable required GCP APIs
2. Create an Artifact Registry and push all 7 Docker images
3. Create a firewall rule for ports 5173, 5174, 8180, 9080
4. Create an e2-medium VM with Docker installed
5. SCP config files and start all 10 containers
6. Set up a Cloud Scheduler job to auto-stop the VM at midnight UTC

Wait ~5 minutes after the script finishes for Java services to start (first boot creates DB schemas). Then access:

| Service | URL |
|---------|-----|
| Admin Portal | `http://EXTERNAL_IP:5173` |
| User Portal | `http://EXTERNAL_IP:5174` |
| Keycloak | `http://EXTERNAL_IP:8180` |
| Gateway API | `http://EXTERNAL_IP:9080` |

## Login Credentials

| User | Password | Role |
|------|----------|------|
| `admin-a` | `password` | Admin (process designer, audit) |
| `user-a` | `password` | User (task inbox, start process) |
| `admin` | `admin` | Keycloak admin console |

## Redeploy After Teardown

If you previously ran `teardown.sh` (which deletes everything), just run `setup.sh` again:

```bash
cd deploy/gcp
bash setup.sh
```

That's it. The script is idempotent — it creates only what doesn't exist.

## Cost Control

### Option 1: Stop the VM ($0 compute, ~$1/mo disk)
```bash
# Stop
gcloud compute instances stop wfp-vm --zone=us-central1-a

# Restart (services auto-start via docker compose restart policy)
gcloud compute instances start wfp-vm --zone=us-central1-a
```

The auto-stop scheduler also stops the VM at midnight UTC daily. To disable:
```bash
gcloud scheduler jobs pause wfp-auto-stop --location=us-central1
```

### Option 2: Full teardown ($0 total)
```bash
cd deploy/gcp
bash teardown.sh
```

This deletes the VM, disk, firewall rule, scheduler job, and optionally the Artifact Registry.
All data (deployed processes, tasks) is lost. Redeploy with `bash setup.sh`.

## Updating After Code Changes

If you've made code changes and want to update the running deployment:

```bash
# 1. Rebuild and push only the changed images
REGISTRY="us-central1-docker.pkg.dev/YOUR_PROJECT_ID/wfp-images"

# Backend service example:
docker build -t $REGISTRY/workflow-service:latest -f services/workflow-service/Dockerfile .
docker push $REGISTRY/workflow-service:latest

# Frontend example:
docker build -t $REGISTRY/admin-portal:latest -f frontend/apps/admin-portal/Dockerfile .
docker push $REGISTRY/admin-portal:latest

# 2. Pull and restart on the VM
gcloud compute ssh wfp-vm --zone=us-central1-a --command="
  cd /opt/wfp
  sudo docker compose pull
  sudo docker compose up -d
"
```

## Troubleshooting

### Check if all containers are running
```bash
gcloud compute ssh wfp-vm --zone=us-central1-a \
  --command="sudo docker compose -f /opt/wfp/docker-compose.yml ps"
```

### View service logs
```bash
# All services
gcloud compute ssh wfp-vm --zone=us-central1-a \
  --command="sudo docker compose -f /opt/wfp/docker-compose.yml logs --tail=20"

# Specific service
gcloud compute ssh wfp-vm --zone=us-central1-a \
  --command="sudo docker compose -f /opt/wfp/docker-compose.yml logs --tail=20 workflow-service"
```

### Check health endpoints
```bash
EXTERNAL_IP=$(gcloud compute instances describe wfp-vm --zone=us-central1-a \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

curl http://$EXTERNAL_IP:9080/actuator/health
```

### Services return 401 Unauthorized
The JWT issuer URI must match the VM's external IP. The deploy script auto-detects this,
but if the IP changes (e.g., after stop/start), you need to update the docker-compose:
```bash
gcloud compute ssh wfp-vm --zone=us-central1-a --command="sudo bash /tmp/deploy-services.sh"
```

### Services return 403 on POST/PUT/DELETE
The gateway's CORS config only allows localhost origins. The nginx proxy strips the
Origin header to work around this. If you rebuild frontend images, make sure the
nginx.conf includes `proxy_set_header Origin "";`.

## Key Deployment Notes

- **VM size**: e2-medium (2 vCPU, 4 GB RAM) required. e2-small will OOM with 10 containers.
- **JWT issuer URI**: Auto-detected from VM metadata. Keycloak tokens carry the external URL as `iss` claim, which must match the backend's configured issuer URI.
- **Nginx Origin header**: Frontend nginx proxies strip the `Origin` header to avoid CORS rejections at the gateway.
- **First boot**: Java services take 3-5 minutes to start due to Hibernate schema creation and Flowable engine initialization. Subsequent boots are faster (~90s).

## Architecture

```
Internet
   |
   v (Firewall: 5173, 5174, 8180, 9080)
+------------------------------+
|  e2-medium VM (wfp-vm)       |
|                              |
|  docker-compose              |
|  +- postgres:16              |
|  +- rabbitmq:3.13            |
|  +- keycloak:25              |
|  +- gateway         :9080    |
|  +- workflow-service :8081   |
|  +- custom-fields    :8082   |
|  +- notification     :8083   |
|  +- audit            :8084   |
|  +- admin-portal     :5173   |
|  +- user-portal      :5174   |
+------------------------------+
       |
       v
  Artifact Registry
  (us-central1-docker.pkg.dev)
```

## File Reference

| File | Purpose |
|------|---------|
| `setup.sh` | Full deployment: APIs, registry, images, VM, firewall, scheduler |
| `deploy-services.sh` | Run on VM: writes docker-compose, pulls images, starts services |
| `startup-script.sh` | Alternative VM startup script (for metadata-based startup) |
| `teardown.sh` | Deletes all GCP resources |
| `nginx-fix.conf` | Nginx config with Origin header stripping (for hot-fixing) |

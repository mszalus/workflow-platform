# GCP Deployment — Single VM with Docker Compose

Deploy the entire workflow platform on a single GCP VM running Docker Compose.
Cost: ~$25/mo (e2-medium) or $0 when stopped.

## Prerequisites

1. **GCP account** with billing enabled
2. **gcloud CLI** installed: https://cloud.google.com/sdk/docs/install
3. **Docker** installed locally (to build and push images)

## Quick Start

```bash
# 1. Authenticate and set project
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# 2. Run the setup script (creates everything)
cd deploy/gcp
bash setup.sh

# 3. SCP config files to the VM
gcloud compute scp ../../docker/init-db.sql wfp-vm:/tmp/init-db.sql --zone=us-central1-a
gcloud compute scp ../../docker/keycloak/realm-export.json wfp-vm:/tmp/realm-export.json --zone=us-central1-a

# 4. SCP and run the deploy script on the VM
gcloud compute scp deploy-services.sh wfp-vm:/tmp/deploy-services.sh --zone=us-central1-a
gcloud compute ssh wfp-vm --zone=us-central1-a --command="sudo bash /tmp/deploy-services.sh"

# 5. Wait ~5 minutes for Java services to start (first boot creates DB schemas)

# 6. Get the external IP
gcloud compute instances describe wfp-vm --zone=us-central1-a \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'

# 7. Access the platform
#    Admin Portal: http://EXTERNAL_IP:5173
#    User Portal:  http://EXTERNAL_IP:5174
#    Keycloak:     http://EXTERNAL_IP:8180
#    Gateway API:  http://EXTERNAL_IP:9080
```

## Login Credentials

- **Admin**: `admin-a` / `password`
- **User**: `user-a` / `password`
- **Keycloak console**: `admin` / `admin`

## Cost Control

### Stop when not in use
```bash
# Stop (saves ~100% compute cost, disk still billed at ~$1/mo)
gcloud compute instances stop wfp-vm --zone=us-central1-a

# Start again (services auto-restart via docker compose restart policy)
gcloud compute instances start wfp-vm --zone=us-central1-a
```

### Auto-stop after inactivity
The setup creates a Cloud Scheduler job that stops the VM nightly at midnight UTC.
To disable: `gcloud scheduler jobs pause wfp-auto-stop --location=us-central1`

### Tear down everything
```bash
bash teardown.sh
```

## Key Deployment Notes

- **VM size**: e2-medium (2 vCPU, 4 GB RAM) — required for running 10 containers. e2-small may OOM.
- **JWT issuer URI**: The deploy script auto-detects the VM's external IP and configures the JWT issuer URI to match. This is needed because Keycloak tokens carry the external URL as the `iss` claim, which must match the backend's configured issuer URI.
- **Nginx Origin header**: The frontend nginx proxies strip the `Origin` header to avoid CORS rejections at the gateway (the gateway's CORS config only allows localhost origins by default).
- **Private repo**: The deploy script does NOT clone the GitHub repo. Config files (init-db.sql, realm-export.json) must be SCP'd to `/tmp/` on the VM before running the deploy script.
- **First boot**: Java services take 2-5 minutes to start on first boot due to Hibernate schema creation and Flowable engine initialization.

## Architecture on GCP

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
|  +- gateway:9080             |
|  +- workflow-service:8081    |
|  +- custom-fields:8082       |
|  +- notification:8083        |
|  +- audit:8084               |
|  +- admin-portal:5173        |
|  +- user-portal:5174         |
+------------------------------+
       |
       v
  Artifact Registry
  (us-central1-docker.pkg.dev)
```

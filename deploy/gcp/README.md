# GCP Deployment — Single VM with Docker Compose

Deploy the entire workflow platform on a single GCP VM running Docker Compose.
Cost: ~$13/mo (e2-small) or $0 when stopped.

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

# 3. Wait ~3-5 minutes for VM to pull images and start services

# 4. Get the external IP
gcloud compute instances describe wfp-vm --zone=us-central1-a --format='get(networkInterfaces[0].accessConfigs[0].natIP)'

# 5. Access the platform
#    Admin Portal: http://EXTERNAL_IP:5173
#    User Portal:  http://EXTERNAL_IP:5174
#    Keycloak:     http://EXTERNAL_IP:8180
```

## Cost Control

### Stop when not in use
```bash
# Stop (saves ~100% compute cost, disk still billed at ~$1/mo)
gcloud compute instances stop wfp-vm --zone=us-central1-a

# Start again
gcloud compute instances start wfp-vm --zone=us-central1-a
```

### Auto-stop after inactivity
The setup creates a Cloud Scheduler job that stops the VM nightly at midnight UTC.
To disable: `gcloud scheduler jobs pause wfp-auto-stop --location=us-central1`

### Tear down everything
```bash
bash teardown.sh
```

## Architecture on GCP

```
Internet
   │
   ▼ (Firewall: 5173, 5174, 8180, 9080)
┌──────────────────────────────┐
│  e2-small VM (wfp-vm)       │
│                              │
│  docker-compose              │
│  ├─ postgres:16              │
│  ├─ rabbitmq:3.13            │
│  ├─ keycloak:25              │
│  ├─ gateway:9080             │
│  ├─ workflow-service:8081    │
│  ├─ custom-fields:8082       │
│  ├─ notification:8083        │
│  ├─ audit:8084               │
│  ├─ admin-portal:5173        │
│  └─ user-portal:5174         │
└──────────────────────────────┘
```

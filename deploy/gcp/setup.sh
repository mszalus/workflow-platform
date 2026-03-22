#!/usr/bin/env bash
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
REGION="us-central1"
ZONE="${REGION}-a"
VM_NAME="wfp-vm"
MACHINE_TYPE="e2-medium"      # 2 vCPU, 4 GB — needed for 10 containers
DISK_SIZE="30"                # GB
REPO_NAME="wfp-images"
NETWORK_TAG="wfp-server"

if [ -z "$PROJECT_ID" ]; then
  echo "ERROR: No GCP project set. Run: gcloud config set project YOUR_PROJECT_ID"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=== Deploying Workflow Platform to GCP ==="
echo "Project: $PROJECT_ID"
echo "Zone:    $ZONE"
echo "VM:      $VM_NAME ($MACHINE_TYPE)"
echo ""

# ── 1. Enable required APIs ───────────────────────────────────
echo ">>> Enabling APIs..."
gcloud services enable \
  compute.googleapis.com \
  artifactregistry.googleapis.com \
  cloudscheduler.googleapis.com \
  --quiet

# ── 2. Create Artifact Registry repository ────────────────────
echo ">>> Creating Artifact Registry repository..."
gcloud artifacts repositories describe "$REPO_NAME" \
  --location="$REGION" --format="value(name)" 2>/dev/null || \
gcloud artifacts repositories create "$REPO_NAME" \
  --repository-format=docker \
  --location="$REGION" \
  --description="Workflow Platform Docker images"

# ── 3. Build and push Docker images ───────────────────────────
echo ">>> Configuring Docker auth for Artifact Registry..."
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}"

echo ">>> Building and pushing images..."
cd "$PROJECT_ROOT"

SERVICES=(gateway workflow-service custom-fields-service notification-service audit-service)
for svc in "${SERVICES[@]}"; do
  echo "  Building $svc..."
  docker build -t "${REGISTRY}/${svc}:latest" -f "services/${svc}/Dockerfile" .
  docker push "${REGISTRY}/${svc}:latest"
done

FRONTENDS=(admin-portal user-portal)
for app in "${FRONTENDS[@]}"; do
  echo "  Building $app..."
  docker build -t "${REGISTRY}/${app}:latest" -f "frontend/apps/${app}/Dockerfile" .
  docker push "${REGISTRY}/${app}:latest"
done

echo ">>> All images pushed to ${REGISTRY}"

# ── 4. Create firewall rule ───────────────────────────────────
echo ">>> Creating firewall rule..."
gcloud compute firewall-rules describe allow-wfp 2>/dev/null || \
gcloud compute firewall-rules create allow-wfp \
  --allow=tcp:5173,tcp:5174,tcp:8180,tcp:9080 \
  --target-tags="$NETWORK_TAG" \
  --description="Allow traffic to Workflow Platform" \
  --direction=INGRESS

# ── 5. Create the VM ──────────────────────────────────────────
echo ">>> Creating VM..."
gcloud compute instances create "$VM_NAME" \
  --zone="$ZONE" \
  --machine-type="$MACHINE_TYPE" \
  --boot-disk-size="${DISK_SIZE}GB" \
  --boot-disk-type=pd-standard \
  --image-family=ubuntu-2404-lts-amd64 \
  --image-project=ubuntu-os-cloud \
  --tags="$NETWORK_TAG" \
  --scopes=cloud-platform

echo ">>> Waiting for VM to be ready..."
sleep 30

# ── 6. Install Docker on the VM ───────────────────────────────
echo ">>> Installing Docker on VM..."
gcloud compute ssh "$VM_NAME" --zone="$ZONE" --command="
  sudo bash -c '
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg 2>/dev/null
    echo \"deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \$(lsb_release -cs) stable\" > /etc/apt/sources.list.d/docker.list
    apt-get update -y -qq
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    systemctl enable docker
    systemctl start docker
  '
"

# ── 7. Copy config files and deploy script to VM ──────────────
echo ">>> Copying config files to VM..."
gcloud compute scp "$PROJECT_ROOT/docker/init-db.sql" "$VM_NAME:/tmp/init-db.sql" --zone="$ZONE"
gcloud compute scp "$PROJECT_ROOT/docker/keycloak/realm-export.json" "$VM_NAME:/tmp/realm-export.json" --zone="$ZONE"
gcloud compute scp "$SCRIPT_DIR/deploy-services.sh" "$VM_NAME:/tmp/deploy-services.sh" --zone="$ZONE"

# ── 8. Run deploy script on VM ────────────────────────────────
echo ">>> Starting services on VM..."
gcloud compute ssh "$VM_NAME" --zone="$ZONE" --command="sudo bash /tmp/deploy-services.sh"

# ── 9. Set up auto-stop schedule (midnight UTC) ──────────────
echo ">>> Setting up auto-stop schedule..."

SA_NAME="wfp-scheduler"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts describe "$SA_EMAIL" 2>/dev/null || \
gcloud iam service-accounts create "$SA_NAME" \
  --display-name="WFP VM Scheduler"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/compute.instanceAdmin.v1" \
  --condition=None \
  --quiet

gcloud scheduler jobs describe wfp-auto-stop --location="$REGION" 2>/dev/null || \
gcloud scheduler jobs create http wfp-auto-stop \
  --location="$REGION" \
  --schedule="0 0 * * *" \
  --uri="https://compute.googleapis.com/compute/v1/projects/${PROJECT_ID}/zones/${ZONE}/instances/${VM_NAME}/stop" \
  --http-method=POST \
  --oauth-service-account-email="$SA_EMAIL" \
  --description="Auto-stop WFP VM at midnight UTC to save costs"

# ── 10. Print access info ─────────────────────────────────────
EXTERNAL_IP=$(gcloud compute instances describe "$VM_NAME" \
  --zone="$ZONE" \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

echo ""
echo "============================================"
echo "  Workflow Platform deployed to GCP!"
echo "============================================"
echo ""
echo "  VM:            $VM_NAME ($MACHINE_TYPE)"
echo "  Zone:          $ZONE"
echo "  External IP:   $EXTERNAL_IP"
echo ""
echo "  Java services take ~5 minutes to start on first boot."
echo "  Check progress:"
echo "    gcloud compute ssh $VM_NAME --zone=$ZONE --command='sudo docker compose -f /opt/wfp/docker-compose.yml logs --tail=1 gateway workflow-service'"
echo ""
echo "  Admin Portal:  http://${EXTERNAL_IP}:5173"
echo "  User Portal:   http://${EXTERNAL_IP}:5174"
echo "  Keycloak:      http://${EXTERNAL_IP}:8180"
echo "  Gateway API:   http://${EXTERNAL_IP}:9080"
echo ""
echo "  Login:  admin-a / password  (admin)"
echo "          user-a  / password  (user)"
echo ""
echo "  Auto-stop:     midnight UTC daily"
echo "  Manual stop:   gcloud compute instances stop $VM_NAME --zone=$ZONE"
echo "  Manual start:  gcloud compute instances start $VM_NAME --zone=$ZONE"
echo "  Tear down:     bash teardown.sh"
echo "============================================"

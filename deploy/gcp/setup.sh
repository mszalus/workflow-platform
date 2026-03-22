#!/usr/bin/env bash
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
REGION="us-central1"
ZONE="${REGION}-a"
VM_NAME="wfp-vm"
MACHINE_TYPE="e2-small"       # 2 vCPU, 2 GB — bump to e2-medium if OOM
DISK_SIZE="30"                # GB
REPO_NAME="wfp-images"
NETWORK_TAG="wfp-server"

if [ -z "$PROJECT_ID" ]; then
  echo "ERROR: No GCP project set. Run: gcloud config set project YOUR_PROJECT_ID"
  exit 1
fi

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
  cloudfunctions.googleapis.com \
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
# Go to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
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
  docker build -t "${REGISTRY}/${app}:latest" -f "frontend/apps/${app}/Dockerfile" frontend/
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

# Generate the startup script with the correct registry
cat > /tmp/wfp-startup.sh <<STARTUP
#!/bin/bash
set -e

# Install Docker if not present
if ! command -v docker &>/dev/null; then
  echo "Installing Docker..."
  apt-get update -y
  apt-get install -y docker.io docker-compose-plugin
  systemctl enable docker
  systemctl start docker
fi

# Authenticate to Artifact Registry
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# Create app directory
mkdir -p /opt/wfp
cd /opt/wfp

# Write docker-compose for GCP
cat > docker-compose.yml <<'COMPOSE'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: wfp
      POSTGRES_PASSWORD: wfp_secret
      POSTGRES_DB: wfp
    volumes:
      - pg_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wfp"]
      interval: 5s
      timeout: 3s
      retries: 10

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: wfp
      RABBITMQ_DEFAULT_PASS: wfp_password
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_running"]
      interval: 10s
      timeout: 5s
      retries: 10

  keycloak:
    image: quay.io/keycloak/keycloak:25.0.6
    command: start-dev --import-realm
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=keycloak
      KC_DB_USERNAME: wfp
      KC_DB_PASSWORD: wfp_secret
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8180:8080"
    volumes:
      - ./realm-export.json:/opt/keycloak/data/import/realm-export.json
    depends_on:
      postgres:
        condition: service_healthy

  gateway:
    image: REGISTRY_PLACEHOLDER/gateway:latest
    ports:
      - "9080:8080"
    environment:
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
      WORKFLOW_SERVICE_URL: http://workflow-service:8081
      CUSTOM_FIELDS_SERVICE_URL: http://custom-fields-service:8082
      NOTIFICATION_SERVICE_URL: http://notification-service:8083
      AUDIT_SERVICE_URL: http://audit-service:8084
    depends_on:
      - keycloak

  workflow-service:
    image: REGISTRY_PLACEHOLDER/workflow-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=workflow
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: wfp
      SPRING_RABBITMQ_PASSWORD: wfp_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  custom-fields-service:
    image: REGISTRY_PLACEHOLDER/custom-fields-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=custom_fields
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy

  notification-service:
    image: REGISTRY_PLACEHOLDER/notification-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=notification
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: wfp
      SPRING_RABBITMQ_PASSWORD: wfp_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  audit-service:
    image: REGISTRY_PLACEHOLDER/audit-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=audit
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: wfp
      SPRING_RABBITMQ_PASSWORD: wfp_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  admin-portal:
    image: REGISTRY_PLACEHOLDER/admin-portal:latest
    ports:
      - "5173:80"
    depends_on:
      - gateway

  user-portal:
    image: REGISTRY_PLACEHOLDER/user-portal:latest
    ports:
      - "5174:80"
    depends_on:
      - gateway

volumes:
  pg_data:
COMPOSE

# Replace registry placeholder
sed -i "s|REGISTRY_PLACEHOLDER|${REGISTRY}|g" docker-compose.yml

# Pull images and start
docker compose pull
docker compose up -d

echo "Workflow Platform is starting..."
STARTUP

# Also need init-db.sql and realm-export.json — copy from repo via metadata
# We'll use a simpler approach: clone the repo on the VM

# Rewrite startup to clone repo and use its files
cat > /tmp/wfp-startup.sh <<STARTUP2
#!/bin/bash
set -e
exec > /var/log/wfp-startup.log 2>&1

echo "=== WFP Startup Script ==="
date

# Install Docker if not present
if ! command -v docker &>/dev/null; then
  echo "Installing Docker..."
  apt-get update -y
  apt-get install -y docker.io docker-compose-plugin git
  systemctl enable docker
  systemctl start docker
fi

# Authenticate to Artifact Registry
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# Clone repo for config files (init-db.sql, realm-export.json, nginx configs)
mkdir -p /opt/wfp
cd /opt/wfp

if [ ! -d repo ]; then
  git clone https://github.com/mszalus/workflow-platform.git repo
else
  cd repo && git pull && cd ..
fi

# Copy required config files
cp repo/docker/init-db.sql .
cp repo/docker/keycloak/realm-export.json .

# Write the GCP docker-compose
REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}"

cat > docker-compose.yml <<COMPOSE
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: wfp
      POSTGRES_PASSWORD: wfp_secret
      POSTGRES_DB: wfp
    volumes:
      - pg_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wfp"]
      interval: 5s
      timeout: 3s
      retries: 10
    restart: unless-stopped

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: wfp
      RABBITMQ_DEFAULT_PASS: wfp_password
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_running"]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped

  keycloak:
    image: quay.io/keycloak/keycloak:25.0.6
    command: start-dev --import-realm
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=keycloak
      KC_DB_USERNAME: wfp
      KC_DB_PASSWORD: wfp_secret
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8180:8080"
    volumes:
      - ./realm-export.json:/opt/keycloak/data/import/realm-export.json
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  gateway:
    image: ${REGISTRY}/gateway:latest
    ports:
      - "9080:8080"
    environment:
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
      WORKFLOW_SERVICE_URL: http://workflow-service:8081
      CUSTOM_FIELDS_SERVICE_URL: http://custom-fields-service:8082
      NOTIFICATION_SERVICE_URL: http://notification-service:8083
      AUDIT_SERVICE_URL: http://audit-service:8084
    depends_on:
      - keycloak
    restart: unless-stopped

  workflow-service:
    image: ${REGISTRY}/workflow-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=workflow
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: wfp
      SPRING_RABBITMQ_PASSWORD: wfp_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  custom-fields-service:
    image: ${REGISTRY}/custom-fields-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=custom_fields
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  notification-service:
    image: ${REGISTRY}/notification-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=notification
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: wfp
      SPRING_RABBITMQ_PASSWORD: wfp_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  audit-service:
    image: ${REGISTRY}/audit-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wfp?currentSchema=audit
      SPRING_DATASOURCE_USERNAME: wfp
      SPRING_DATASOURCE_PASSWORD: wfp_secret
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: wfp
      SPRING_RABBITMQ_PASSWORD: wfp_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/workflow-platform
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/workflow-platform/protocol/openid-connect/certs
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  admin-portal:
    image: ${REGISTRY}/admin-portal:latest
    ports:
      - "5173:80"
    depends_on:
      - gateway
    restart: unless-stopped

  user-portal:
    image: ${REGISTRY}/user-portal:latest
    ports:
      - "5174:80"
    depends_on:
      - gateway
    restart: unless-stopped

volumes:
  pg_data:
COMPOSE

# Pull and start
docker compose pull
docker compose up -d

echo "=== WFP started at $(date) ==="
STARTUP2

gcloud compute instances create "$VM_NAME" \
  --zone="$ZONE" \
  --machine-type="$MACHINE_TYPE" \
  --boot-disk-size="${DISK_SIZE}GB" \
  --boot-disk-type=pd-standard \
  --image-family=ubuntu-2404-lts-amd64 \
  --image-project=ubuntu-os-cloud \
  --tags="$NETWORK_TAG" \
  --scopes=cloud-platform \
  --metadata-from-file=startup-script=/tmp/wfp-startup.sh

echo ""
echo ">>> VM created. Waiting for startup..."
echo ">>> Startup logs: gcloud compute ssh $VM_NAME --zone=$ZONE -- 'sudo tail -f /var/log/wfp-startup.log'"

# ── 6. Set up auto-stop schedule (midnight UTC) ──────────────
echo ">>> Setting up auto-stop schedule..."

# Create a service account for the scheduler if it doesn't exist
SA_NAME="wfp-scheduler"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts describe "$SA_EMAIL" 2>/dev/null || \
gcloud iam service-accounts create "$SA_NAME" \
  --display-name="WFP VM Scheduler"

# Grant it permission to stop compute instances
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/compute.instanceAdmin.v1" \
  --condition=None \
  --quiet

# Create the scheduler job to stop the VM at midnight UTC
gcloud scheduler jobs describe wfp-auto-stop --location="$REGION" 2>/dev/null || \
gcloud scheduler jobs create http wfp-auto-stop \
  --location="$REGION" \
  --schedule="0 0 * * *" \
  --uri="https://compute.googleapis.com/compute/v1/projects/${PROJECT_ID}/zones/${ZONE}/instances/${VM_NAME}/stop" \
  --http-method=POST \
  --oauth-service-account-email="$SA_EMAIL" \
  --description="Auto-stop WFP VM at midnight UTC to save costs"

# ── 7. Wait and print access info ────────────────────────────
echo ""
echo ">>> Waiting for external IP..."
sleep 10

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
echo "  Services will be ready in ~3-5 minutes."
echo ""
echo "  Admin Portal:  http://${EXTERNAL_IP}:5173"
echo "  User Portal:   http://${EXTERNAL_IP}:5174"
echo "  Keycloak:      http://${EXTERNAL_IP}:8180"
echo "  Gateway API:   http://${EXTERNAL_IP}:9080"
echo ""
echo "  Login:  admin-a / password  (admin)"
echo "          user-a  / password  (user)"
echo ""
echo "  Auto-stop:  midnight UTC daily"
echo "  Manual stop: gcloud compute instances stop $VM_NAME --zone=$ZONE"
echo "  Manual start: gcloud compute instances start $VM_NAME --zone=$ZONE"
echo ""
echo "  Startup logs:"
echo "    gcloud compute ssh $VM_NAME --zone=$ZONE -- 'sudo tail -f /var/log/wfp-startup.log'"
echo "============================================"

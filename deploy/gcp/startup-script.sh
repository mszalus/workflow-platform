#!/bin/bash
set -e
exec > /var/log/wfp-startup.log 2>&1

echo "=== WFP Startup Script ==="
date

REGISTRY="us-central1-docker.pkg.dev/workflow-platform-demo/wfp-images"

# Detect external IP for JWT issuer URI
EXTERNAL_IP=$(curl -s -H "Metadata-Flavor: Google" http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip)
echo "External IP: ${EXTERNAL_IP}"

# Install Docker if not present
if ! command -v docker &>/dev/null; then
  echo "Installing Docker from official repo..."
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg 2>/dev/null
  echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
  apt-get update -y -qq
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
  systemctl enable docker
  systemctl start docker
fi

# Authenticate to Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev --quiet

# Setup working directory (config files must be pre-staged at /tmp/)
mkdir -p /opt/wfp
cd /opt/wfp

if [ -f /tmp/init-db.sql ]; then
  cp /tmp/init-db.sql .
fi
if [ -f /tmp/realm-export.json ]; then
  cp /tmp/realm-export.json .
fi

# Only proceed if config files exist
if [ ! -f init-db.sql ] || [ ! -f realm-export.json ]; then
  echo "ERROR: Missing config files (init-db.sql or realm-export.json) in /opt/wfp/"
  echo "SCP them to /tmp/ on the VM first, then re-run this script."
  exit 1
fi

# Write docker-compose
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
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://${EXTERNAL_IP}:8180/realms/workflow-platform
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
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://${EXTERNAL_IP}:8180/realms/workflow-platform
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
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://${EXTERNAL_IP}:8180/realms/workflow-platform
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
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://${EXTERNAL_IP}:8180/realms/workflow-platform
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
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://${EXTERNAL_IP}:8180/realms/workflow-platform
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

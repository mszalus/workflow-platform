FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
COPY frontend/packages packages
COPY frontend/apps/admin-portal apps/admin-portal
RUN npm install --workspace=apps/admin-portal
RUN npm run build --workspace=packages/shared-ui && npm run build --workspace=packages/bpmn-editor && npm run build --workspace=apps/admin-portal

FROM nginxinc/nginx-unprivileged:alpine
COPY --from=build /app/apps/admin-portal/dist /usr/share/nginx/html
COPY frontend/apps/admin-portal/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080

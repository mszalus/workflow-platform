FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
COPY frontend/packages packages
COPY frontend/apps/user-portal apps/user-portal
RUN npm install --workspace=apps/user-portal
RUN npm run build --workspace=packages/shared-ui && npm run build --workspace=apps/user-portal

FROM nginxinc/nginx-unprivileged:alpine
COPY --from=build /app/apps/user-portal/dist /usr/share/nginx/html
COPY frontend/apps/user-portal/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080

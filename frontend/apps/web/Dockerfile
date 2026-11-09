FROM node:24-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
COPY frontend/apps/web apps/web
RUN npm install --workspace=apps/web --include-workspace-root
RUN npm run build --workspace=apps/web

FROM nginxinc/nginx-unprivileged:alpine
COPY --from=build /app/apps/web/dist /usr/share/nginx/html
COPY frontend/apps/web/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080

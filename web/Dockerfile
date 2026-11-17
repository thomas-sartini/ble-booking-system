# syntax=docker/dockerfile:1

# ---- build: compile the Vite app into static files -------------------------------------------------
FROM node:26-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# The API base URL is baked into the bundle at build time (Vite replaces import.meta.env.VITE_API_URL).
# Railway passes service variables as build args when the ARG is declared. Without it the app would
# silently fall back to http://localhost:8080 in production, so fail the build instead.
ARG VITE_API_URL
RUN test -n "$VITE_API_URL" || { echo "ERROR: build arg VITE_API_URL is empty. Set it to the backend URL." >&2; exit 1; }
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# Mocks must never ship: no mock service worker file, and no reference to it in the bundle.
RUN if [ -e dist/mockServiceWorker.js ] || grep -rqI "mockServiceWorker" dist; then \
      echo "ERROR: the MSW mock worker leaked into the production build." >&2; exit 1; \
    fi

# ---- runtime: nginx serves dist/, listens on $PORT, runs as the unprivileged nginx user ---------------
FROM nginx:stable-alpine AS runtime

# Railway sets PORT; 8080 is the default. Only PORT is substituted into the template, so nginx's own
# $variables ($uri, ...) stay untouched.
ENV PORT=8080 \
    NGINX_ENVSUBST_FILTER='^PORT$'

# Non-root: no "user" directive, pid file in /tmp, and the paths nginx writes to are owned by nginx.
RUN sed -i -e '/^user /d' -e 's#^pid .*#pid /tmp/nginx.pid;#' /etc/nginx/nginx.conf \
 && rm -f /etc/nginx/conf.d/default.conf \
 && mkdir -p /etc/nginx/snippets \
 && chown -R nginx:nginx /var/cache/nginx /etc/nginx/conf.d

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

USER nginx
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s \
  CMD wget -qO- "http://127.0.0.1:${PORT}/healthz" >/dev/null || exit 1

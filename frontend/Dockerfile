FROM node:24-bookworm-slim AS build
WORKDIR /app
ENV HUSKY=0
RUN npm install --global pnpm@11.7.0
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
ENV VITE_BASE_API=/api \
    VITE_API_URL=/api \
    VITE_GLOBALE_REQUEST_MOCK=false
RUN NODE_OPTIONS=--max-old-space-size=768 pnpm build

FROM nginx:1.28-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1

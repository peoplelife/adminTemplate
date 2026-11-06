#!/usr/bin/env bash
set -Eeuo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

on_error() {
    local status=$?
    printf '\nDeployment failed (exit %s). Check the error above. Data volumes are preserved.\n' "$status" >&2
    docker compose ps -a || true
    exit "$status"
}
trap on_error ERR

if [[ ! -f .env ]]; then
    printf 'Missing deploy/.env. Create it and fill in production credentials first.\n' >&2
    exit 1
fi

docker compose version
docker info > /dev/null
chmod 600 .env
docker compose config --quiet

printf '\nStopping applications for maintenance; databases and data volumes remain.\n'
docker compose stop frontend java python

for service in frontend java python; do
    printf '\nBuilding %s...\n' "$service"
    docker compose build "$service"
done

printf '\nStarting databases and waiting for health checks...\n'
docker compose up -d --wait --wait-timeout 300 mysql postgres redis

printf '\nRunning Alembic migrations...\n'
docker compose up --no-build --no-deps --force-recreate --exit-code-from migrate migrate

printf '\nStarting applications and waiting for health checks...\n'
docker compose up -d --no-build --wait --wait-timeout 300 frontend java python
docker compose ps -a
printf '\nDeployment healthy. Configure BaoTa to proxy to 127.0.0.1:8080 (or your HTTP_PORT).\n'
printf 'Business checks (login, upload, RAG) still need to be performed.\n'

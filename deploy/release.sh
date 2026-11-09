#!/usr/bin/env bash
set -Eeuo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

on_error() {
    local status=$?
    printf '\nRelease failed (exit %s). Existing data volumes were preserved.\n' "$status" >&2
    docker compose ps -a || true
    exit "$status"
}
trap on_error ERR

if [[ ! -f .env ]]; then
    printf 'Missing deploy/.env.\n' >&2
    exit 1
fi
if [[ -z "${IMAGE_TAG:-}" ]]; then
    printf 'IMAGE_TAG is required.\n' >&2
    exit 1
fi

export FRONTEND_IMAGE="ghcr.io/peoplelife/admin-template-frontend:${IMAGE_TAG}"
export JAVA_IMAGE="ghcr.io/peoplelife/admin-template-java:${IMAGE_TAG}"
export PYTHON_IMAGE="ghcr.io/peoplelife/admin-template-python:${IMAGE_TAG}"

chmod 600 .env
docker compose config --quiet
docker compose pull frontend java python migrate
docker compose stop frontend java python
docker compose up -d --wait --wait-timeout 300 mysql postgres redis
docker compose run --rm --no-deps migrate
docker compose up -d --no-build --wait --wait-timeout 300 frontend java python
docker compose ps -a
printf '\nRelease %s is healthy.\n' "$IMAGE_TAG"

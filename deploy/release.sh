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

run_mysql() {
    docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 --batch --skip-column-names -u "$MYSQL_USER" "$MYSQL_DATABASE" "$@"' sh "$@"
}

run_mysql <<'SQL'
CREATE TABLE IF NOT EXISTS schema_migrations (
    version varchar(100) NOT NULL PRIMARY KEY,
    applied_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SQL

# Keep the reviewed list explicit; successful versions must not restore later menu revocations.
for migration in mysql/migrations/2026-11-12-page-agent-menu.sql mysql/migrations/2026-11-13-github-login.sql mysql/migrations/2026-11-13-github-registration.sql; do
    version="${migration##*/}"
    applied="$(run_mysql -e "SELECT COUNT(*) FROM schema_migrations WHERE version = '$version'")"
    if [[ "$applied" == 1 ]]; then
        printf 'Skipping applied MySQL change: %s\n' "$migration"
        continue
    fi
    [[ "$applied" == 0 ]] || { printf 'Invalid MySQL migration status.\n' >&2; exit 1; }
    printf 'Applying MySQL change: %s\n' "$migration"
    run_mysql < "$migration"
    run_mysql -e "INSERT INTO schema_migrations (version) VALUES ('$version')"
done

docker compose run --rm --no-deps migrate
docker compose up -d --no-build --wait --wait-timeout 300 frontend java python
docker compose ps -a
printf '\nRelease %s is healthy.\n' "$IMAGE_TAG"

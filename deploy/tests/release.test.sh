#!/usr/bin/env bash
set -Eeuo pipefail

deploy_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
test_root="$(mktemp -d)"
trap 'rm -rf -- "$test_root"' EXIT
mkdir -p "$test_root/bin" "$test_root/deploy/mysql"
cp "$deploy_root/release.sh" "$test_root/deploy/release.sh"
cp -R "$deploy_root/mysql/migrations" "$test_root/deploy/mysql/migrations"
touch "$test_root/deploy/.env"

cat > "$test_root/bin/docker" <<'DOCKER'
#!/usr/bin/env bash
set -Eeuo pipefail
printf '%s\n' "$*" >> "$RELEASE_TEST_LOG"
if [[ "${1:-}" == compose && "${2:-}" == exec ]]; then
    if [[ "${@: -2:1}" == -e ]]; then
        query="${@: -1}"
        if [[ "$query" == SELECT*schema_migrations* ]]; then
            version="${query#*version = \'}"
            version="${version%%\'*}"
            if grep -q -F -x "$version" "$RELEASE_TEST_VERSIONS"; then printf '1\n'; else printf '0\n'; fi
        elif [[ "$query" == INSERT*schema_migrations* ]]; then
            version="${query#*VALUES (\'}"
            version="${version%%\'*}"
            printf '%s\n' "$version" >> "$RELEASE_TEST_VERSIONS"
        else
            printf 'Unexpected migration query\n' >&2
            exit 18
        fi
        exit 0
    fi
    sql="$(cat)"
    if [[ "$sql" == *'CREATE TABLE IF NOT EXISTS schema_migrations'* ]]; then
        exit 0
    elif [[ "$sql" == *'ALTER TABLE'*users* ]]; then
        printf 'sql:github-registration\n' >> "$RELEASE_TEST_LOG"
    elif [[ "$sql" == *menu_ai_page_agent* ]]; then
        printf 'sql:page-agent-menu\n' >> "$RELEASE_TEST_LOG"
        [[ "${RELEASE_TEST_FAIL_MENU:-0}" != 1 ]] || exit 17
    elif [[ "$sql" == *user_oauth_accounts* ]]; then
        printf 'sql:github-login\n' >> "$RELEASE_TEST_LOG"
    else
        printf 'Unexpected migration input\n' >&2
        exit 18
    fi
fi
DOCKER
chmod +x "$test_root/bin/docker"
export PATH="$test_root/bin:$PATH"
export IMAGE_TAG=test-version
export RELEASE_TEST_LOG="$test_root/docker.log"
export RELEASE_TEST_VERSIONS="$test_root/versions"
touch "$RELEASE_TEST_VERSIONS"

assert_before() {
    local first second
    first="$(grep -n -F "$1" "$RELEASE_TEST_LOG" | head -n 1 | cut -d: -f1 || true)"
    second="$(grep -n -F "$2" "$RELEASE_TEST_LOG" | head -n 1 | cut -d: -f1 || true)"
    [[ -n "$first" && -n "$second" && "$first" -lt "$second" ]] || {
        printf 'Expected %s before %s\n' "$1" "$2" >&2
        exit 1
    }
}

bash "$test_root/deploy/release.sh" > "$test_root/release.log" 2>&1
assert_before 'up -d --wait --wait-timeout 300 mysql postgres redis' 'sql:page-agent-menu'
assert_before 'sql:page-agent-menu' 'sql:github-login'
assert_before 'sql:github-login' 'sql:github-registration'
assert_before 'sql:github-registration' 'run --rm --no-deps migrate'
assert_before 'run --rm --no-deps migrate' 'up -d --no-build --wait --wait-timeout 300 frontend java python'
[[ "$(wc -l < "$RELEASE_TEST_VERSIONS")" -eq 3 ]] || { printf 'Successful migrations were not recorded\n' >&2; exit 1; }
printf 'PASS: MySQL menu, OAuth and registration changes run and record before application startup.\n'

: > "$RELEASE_TEST_LOG"
bash "$test_root/deploy/release.sh" > "$test_root/repeat.log" 2>&1
if grep -q '^sql:' "$RELEASE_TEST_LOG"; then
    printf 'Repeated deployment reran applied SQL and could restore revoked menu grants\n' >&2
    exit 1
fi
grep -q -F 'up -d --no-build' "$RELEASE_TEST_LOG"
printf 'PASS: Repeated release skips applied MySQL versions.\n'

: > "$RELEASE_TEST_LOG"
: > "$RELEASE_TEST_VERSIONS"
status=0
RELEASE_TEST_FAIL_MENU=1 bash "$test_root/deploy/release.sh" > "$test_root/failure.log" 2>&1 || status=$?
[[ "$status" == 17 ]] || { printf 'Expected migration failure exit 17, got %s\n' "$status" >&2; exit 1; }
if [[ -s "$RELEASE_TEST_VERSIONS" ]] || grep -q -E 'sql:github-login|sql:github-registration|run --rm --no-deps migrate|up -d --no-build' "$RELEASE_TEST_LOG"; then
    printf 'Release continued after failed MySQL migration\n' >&2
    exit 1
fi
printf 'PASS: A failed MySQL change stops the release before starting applications.\n'

: > "$RELEASE_TEST_LOG"
bash "$test_root/deploy/release.sh" > "$test_root/retry.log" 2>&1
assert_before 'sql:page-agent-menu' 'sql:github-registration'
assert_before 'sql:github-registration' 'up -d --no-build'
[[ "$(wc -l < "$RELEASE_TEST_VERSIONS")" -eq 3 ]] || exit 1
printf 'PASS: Failed migrations remain pending and a release retry completes them.\n'

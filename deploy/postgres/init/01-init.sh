#!/bin/sh
set -eu

# psql quotes identifiers and literals; never interpolate passwords into SQL text.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=ON_ERROR_STOP=1 --set=app_password="$POSTGRES_APP_PASSWORD" <<'SQL'
CREATE EXTENSION IF NOT EXISTS vector;
CREATE ROLE ai_chat LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE ai_chat TO ai_chat;
GRANT USAGE, CREATE ON SCHEMA public TO ai_chat;
SQL

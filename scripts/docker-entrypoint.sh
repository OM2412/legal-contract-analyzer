#!/bin/sh
set -eu

if [ "${SPRING_PROFILES_ACTIVE:-}" = "db" ]; then
    : "${FOLIO_DB_URL:?FOLIO_DB_URL is required for the db profile}"

    case "$FOLIO_DB_URL" in
        postgresql://*@*/*) ;;
        *)
            echo "FOLIO_DB_URL must be a PostgreSQL connection URL" >&2
            exit 1
            ;;
    esac

    db_target=${FOLIO_DB_URL##*@}
    export FOLIO_DB_JDBC_URL="jdbc:postgresql://${db_target}"
fi

exec java -jar /app/app.jar \
    --server.address=0.0.0.0 \
    --server.port="${PORT:-8080}" \
    --folio.ai.enabled="${FOLIO_AI_ENABLED:-false}" \
    --folio.ollama.url="${FOLIO_OLLAMA_URL:-http://127.0.0.1:11434}" \
    --folio.ollama.model="${FOLIO_OLLAMA_MODEL:-qwen3:4b}" \
    --folio.ollama.api-key="${FOLIO_OLLAMA_API_KEY:-}"
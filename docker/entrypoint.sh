#!/usr/bin/env bash
set -euo pipefail

echo "Starting backend container..."

DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-3306}"
WAIT_SECONDS="${DB_WAIT_SECONDS:-60}"

echo "Waiting for MySQL at ${DB_HOST}:${DB_PORT} (timeout ${WAIT_SECONDS}s)..."

start_ts="$(date +%s)"
while true; do
  # bash TCP check (no extra packages needed)
  if (echo >"/dev/tcp/${DB_HOST}/${DB_PORT}") >/dev/null 2>&1; then
    echo "MySQL port is reachable."
    break
  fi

  now_ts="$(date +%s)"
  elapsed="$((now_ts - start_ts))"
  if [ "${elapsed}" -ge "${WAIT_SECONDS}" ]; then
    echo "ERROR: MySQL not reachable after ${WAIT_SECONDS}s at ${DB_HOST}:${DB_PORT}"
    exit 1
  fi

  sleep 2
done

echo "Launching Spring Boot..."
exec java -jar /app/app.jar


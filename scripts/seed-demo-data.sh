#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DB_CONTAINER="${DB_CONTAINER:-postgres}"
DB_USER="${DB_USER:-training_user}"
DB_NAME="${DB_NAME:-training_platform}"

docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$SCRIPT_DIR/seed-demo-data.sql"

echo "Demo data loaded into $DB_NAME using container $DB_CONTAINER."

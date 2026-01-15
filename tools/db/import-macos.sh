#!/bin/zsh
# =============================================================================
# PostgreSQL Database Import Script (macOS/zsh)
# =============================================================================
# Imports a spring_mcp database dump into a Docker container with pgvector
# Usage: ./import-macos.sh <input_file>
# =============================================================================

set -e

# Configuration (can be overridden via environment variables)
CONTAINER_NAME="${DB_CONTAINER:-spring-mcp-db}"
DB_NAME="${DB_NAME:-spring_mcp}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

# Input file (required)
INPUT_FILE="${1:-}"

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║           PostgreSQL Database Import (macOS/zsh)                   ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

if [[ -z "${INPUT_FILE}" ]]; then
    echo "❌ Error: No input file specified"
    echo ""
    echo "Usage: ./import-macos.sh <backup_file.dump>"
    echo ""
    echo "Example:"
    echo "  ./import-macos.sh spring_mcp_backup_20250115_120000.dump"
    exit 1
fi

if [[ ! -f "${INPUT_FILE}" ]]; then
    echo "❌ Error: File '${INPUT_FILE}' not found"
    exit 1
fi

echo "Configuration:"
echo "  Container:  ${CONTAINER_NAME}"
echo "  Database:   ${DB_NAME}"
echo "  User:       ${DB_USER}"
echo "  Input:      ${INPUT_FILE}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running"
    exit 1
fi

# Check if container exists and is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "❌ Error: Container '${CONTAINER_NAME}' is not running"
    echo "   Start it with: docker-compose up -d postgres"
    exit 1
fi

echo "⚠️  WARNING: This will DROP and recreate the database '${DB_NAME}'!"
echo -n "   Continue? [y/N]: "
read CONFIRM
if [[ "${CONFIRM}" != "y" && "${CONFIRM}" != "Y" ]]; then
    echo "Aborted."
    exit 0
fi

echo ""
echo "🔄 Preparing database..."

# Terminate existing connections and drop/recreate database
docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();" \
    > /dev/null 2>&1 || true

docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d postgres -c \
    "DROP DATABASE IF EXISTS ${DB_NAME};"

docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d postgres -c \
    "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};"

echo "🔄 Enabling pgvector extension..."

# Enable pgvector extension
docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -c \
    "CREATE EXTENSION IF NOT EXISTS vector;"

echo "🔄 Restoring database from backup..."

# Copy dump file to container and restore
docker cp "${INPUT_FILE}" "${CONTAINER_NAME}:/tmp/restore.dump"

# Restore using pg_restore
# -v  = verbose
# -x  = do not restore privileges
# -O  = do not restore ownership
# --if-exists = use IF EXISTS when dropping objects
# --clean = drop objects before recreating
docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" \
    pg_restore -U "${DB_USER}" -d "${DB_NAME}" -v --no-owner --no-privileges \
    /tmp/restore.dump 2>&1 || true

# Cleanup temp file
docker exec "${CONTAINER_NAME}" rm -f /tmp/restore.dump

echo ""
echo "✅ Import completed successfully!"
echo ""
echo "📝 Verify the import with:"
echo "   docker exec -e PGPASSWORD=${DB_PASSWORD} ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME} -c '\\dt'"

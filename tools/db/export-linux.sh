#!/bin/bash
# =============================================================================
# PostgreSQL Database Export Script (Linux/bash)
# =============================================================================
# Exports the spring_mcp database from a Docker container with pgvector support
# Usage: ./export-linux.sh [output_file]
# =============================================================================

set -e

# Configuration (can be overridden via environment variables)
CONTAINER_NAME="${DB_CONTAINER:-spring-mcp-db}"
DB_NAME="${DB_NAME:-spring_mcp}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

# Output file (default: timestamped backup in current directory)
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_FILE="${1:-spring_mcp_backup_${TIMESTAMP}.dump}"

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║           PostgreSQL Database Export (Linux/bash)                  ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""
echo "Configuration:"
echo "  Container:  ${CONTAINER_NAME}"
echo "  Database:   ${DB_NAME}"
echo "  User:       ${DB_USER}"
echo "  Output:     ${OUTPUT_FILE}"
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

echo "🔄 Starting database export..."

# Export using pg_dump with custom format (includes compression)
# -Fc = custom format (compressed, suitable for pg_restore)
# -v  = verbose
# -b  = include large objects
# stdout = binary dump data -> file
# stderr = verbose log messages -> terminal
docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" \
    pg_dump -U "${DB_USER}" -d "${DB_NAME}" -Fc -v -b \
    > "${OUTPUT_FILE}"

# Verify the export file
if [[ -f "${OUTPUT_FILE}" && -s "${OUTPUT_FILE}" ]]; then
    FILE_SIZE=$(ls -lh "${OUTPUT_FILE}" | awk '{print $5}')
    echo ""
    echo "✅ Export completed successfully!"
    echo "   File: ${OUTPUT_FILE}"
    echo "   Size: ${FILE_SIZE}"
    echo ""
    echo "📝 To import on another system, use:"
    echo "   ./import-linux.sh ${OUTPUT_FILE}"
else
    echo "❌ Error: Export failed or file is empty"
    exit 1
fi

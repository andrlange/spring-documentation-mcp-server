#!/bin/zsh
# =============================================================================
# PostgreSQL Database Cleanup Script (macOS/zsh)
# =============================================================================
# Removes duplicate entries and recreates unique constraints
# Usage: ./cleanup-duplicates-macos.sh
# =============================================================================

set -e

# Configuration (can be overridden via environment variables)
CONTAINER_NAME="${DB_CONTAINER:-spring-mcp-db}"
DB_NAME="${DB_NAME:-spring_mcp}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║         PostgreSQL Duplicate Cleanup (macOS/zsh)                   ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""
echo "Configuration:"
echo "  Container:  ${CONTAINER_NAME}"
echo "  Database:   ${DB_NAME}"
echo "  User:       ${DB_USER}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running"
    exit 1
fi

# Check if container exists and is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "❌ Error: Container '${CONTAINER_NAME}' is not running"
    exit 1
fi

# Function to run SQL
run_sql() {
    docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" \
        psql -U "${DB_USER}" -d "${DB_NAME}" -c "$1"
}

# Function to run SQL quietly (no output)
run_sql_quiet() {
    docker exec -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER_NAME}" \
        psql -U "${DB_USER}" -d "${DB_NAME}" -t -c "$1" 2>/dev/null
}

echo "🔍 Checking for duplicates..."
echo ""

# Check migration_recipes duplicates
RECIPE_DUPES=$(run_sql_quiet "SELECT COUNT(*) FROM (SELECT name FROM migration_recipes GROUP BY name HAVING COUNT(*) > 1) t;")
RECIPE_DUPES=$(echo $RECIPE_DUPES | tr -d ' ')

# Check javadoc_packages duplicates
JAVADOC_DUPES=$(run_sql_quiet "SELECT COUNT(*) FROM (SELECT library_name, version, package_name FROM javadoc_packages GROUP BY library_name, version, package_name HAVING COUNT(*) > 1) t;")
JAVADOC_DUPES=$(echo $JAVADOC_DUPES | tr -d ' ')

echo "   migration_recipes: ${RECIPE_DUPES} duplicate groups"
echo "   javadoc_packages:  ${JAVADOC_DUPES} duplicate groups"
echo ""

if [[ "$RECIPE_DUPES" == "0" && "$JAVADOC_DUPES" == "0" ]]; then
    echo "✅ No duplicates found. Database is clean."
    exit 0
fi

echo "🔄 Cleaning up duplicates..."
echo ""

# Clean migration_recipes duplicates (keep lowest id)
if [[ "$RECIPE_DUPES" != "0" ]]; then
    echo "   [1/4] Removing duplicate migration_recipes..."
    run_sql "DELETE FROM migration_recipes a USING migration_recipes b WHERE a.id > b.id AND a.name = b.name;"
fi

# Clean javadoc_packages duplicates (keep lowest id)
if [[ "$JAVADOC_DUPES" != "0" ]]; then
    echo "   [2/4] Removing duplicate javadoc_packages..."
    run_sql "DELETE FROM javadoc_packages a USING javadoc_packages b WHERE a.id > b.id AND a.library_name = b.library_name AND a.version = b.version AND a.package_name = b.package_name;"
fi

echo ""
echo "🔄 Recreating unique constraints..."
echo ""

# Drop and recreate uk_recipe_name if it doesn't exist or is broken
echo "   [3/4] Recreating uk_recipe_name constraint..."
run_sql "ALTER TABLE migration_recipes DROP CONSTRAINT IF EXISTS uk_recipe_name;" 2>/dev/null || true
run_sql "ALTER TABLE migration_recipes ADD CONSTRAINT uk_recipe_name UNIQUE (name);"

# Drop and recreate uq_javadoc_package if it doesn't exist or is broken
echo "   [4/4] Recreating uq_javadoc_package constraint..."
run_sql "ALTER TABLE javadoc_packages DROP CONSTRAINT IF EXISTS uq_javadoc_package;" 2>/dev/null || true
run_sql "ALTER TABLE javadoc_packages ADD CONSTRAINT uq_javadoc_package UNIQUE (library_name, version, package_name);"

echo ""
echo "✅ Cleanup completed successfully!"
echo ""
echo "📝 Verify with:"
echo "   docker exec -e PGPASSWORD=${DB_PASSWORD} ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME} -c \"\\d migration_recipes\""

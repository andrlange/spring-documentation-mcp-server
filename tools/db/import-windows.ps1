# =============================================================================
# PostgreSQL Database Import Script (Windows/PowerShell)
# =============================================================================
# Imports a spring_mcp database dump into a Docker container with pgvector
# Usage: .\import-windows.ps1 -InputFile <path>
# =============================================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$InputFile,
    [string]$ContainerName = "spring-mcp-db",
    [string]$DbName = "spring_mcp",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "postgres",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

# Override from environment variables if set
if ($env:DB_CONTAINER) { $ContainerName = $env:DB_CONTAINER }
if ($env:DB_NAME) { $DbName = $env:DB_NAME }
if ($env:DB_USER) { $DbUser = $env:DB_USER }
if ($env:DB_PASSWORD) { $DbPassword = $env:DB_PASSWORD }

Write-Host "╔════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         PostgreSQL Database Import (Windows/PowerShell)            ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Check if input file exists
if (-not (Test-Path $InputFile)) {
    Write-Host "❌ Error: File '$InputFile' not found" -ForegroundColor Red
    exit 1
}

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Container:  $ContainerName"
Write-Host "  Database:   $DbName"
Write-Host "  User:       $DbUser"
Write-Host "  Input:      $InputFile"
Write-Host ""

# Check if Docker is running
try {
    $null = docker info 2>&1
} catch {
    Write-Host "❌ Error: Docker is not running" -ForegroundColor Red
    exit 1
}

# Check if container exists and is running
$runningContainers = docker ps --format '{{.Names}}' 2>&1
if ($runningContainers -notcontains $ContainerName) {
    Write-Host "❌ Error: Container '$ContainerName' is not running" -ForegroundColor Red
    Write-Host "   Start it with: docker-compose up -d postgres" -ForegroundColor Yellow
    exit 1
}

# Confirm unless -Force is specified
if (-not $Force) {
    Write-Host "⚠️  WARNING: This will DROP and recreate the database '$DbName'!" -ForegroundColor Yellow
    $confirm = Read-Host "   Continue? [y/N]"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Host "Aborted."
        exit 0
    }
}

Write-Host ""
Write-Host "🔄 Preparing database..." -ForegroundColor Green

try {
    # Terminate existing connections and drop/recreate database
    docker exec -e PGPASSWORD="$DbPassword" $ContainerName psql -U $DbUser -d postgres -c `
        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DbName' AND pid <> pg_backend_pid();" 2>&1 | Out-Null

    docker exec -e PGPASSWORD="$DbPassword" $ContainerName psql -U $DbUser -d postgres -c `
        "DROP DATABASE IF EXISTS $DbName;"

    docker exec -e PGPASSWORD="$DbPassword" $ContainerName psql -U $DbUser -d postgres -c `
        "CREATE DATABASE $DbName OWNER $DbUser;"

    Write-Host "🔄 Enabling pgvector extension..." -ForegroundColor Green

    # Enable pgvector extension
    docker exec -e PGPASSWORD="$DbPassword" $ContainerName psql -U $DbUser -d $DbName -c `
        "CREATE EXTENSION IF NOT EXISTS vector;"

    Write-Host "🔄 Restoring database from backup..." -ForegroundColor Green

    # Copy dump file to container and restore
    docker cp $InputFile "${ContainerName}:/tmp/restore.dump"

    # Restore using pg_restore
    docker exec -e PGPASSWORD="$DbPassword" $ContainerName `
        pg_restore -U $DbUser -d $DbName -v --no-owner --no-privileges `
        /tmp/restore.dump 2>&1

    # Cleanup temp file
    docker exec $ContainerName rm -f /tmp/restore.dump

    Write-Host ""
    Write-Host "✅ Import completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📝 Verify the import with:" -ForegroundColor Cyan
    Write-Host "   docker exec -e PGPASSWORD=$DbPassword $ContainerName psql -U $DbUser -d $DbName -c '\dt'"

} catch {
    Write-Host "❌ Error during import: $_" -ForegroundColor Red
    exit 1
}

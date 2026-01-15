# =============================================================================
# PostgreSQL Database Export Script (Windows/PowerShell)
# =============================================================================
# Exports the spring_mcp database from a Docker container with pgvector support
# Usage: .\export-windows.ps1 [-OutputFile <path>]
# =============================================================================

param(
    [string]$OutputFile = "",
    [string]$ContainerName = "spring-mcp-db",
    [string]$DbName = "spring_mcp",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "postgres"
)

$ErrorActionPreference = "Stop"

# Override from environment variables if set
if ($env:DB_CONTAINER) { $ContainerName = $env:DB_CONTAINER }
if ($env:DB_NAME) { $DbName = $env:DB_NAME }
if ($env:DB_USER) { $DbUser = $env:DB_USER }
if ($env:DB_PASSWORD) { $DbPassword = $env:DB_PASSWORD }

# Generate default output filename if not specified
if (-not $OutputFile) {
    $Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $OutputFile = "spring_mcp_backup_${Timestamp}.dump"
}

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "         PostgreSQL Database Export (Windows/PowerShell)              " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Container:  $ContainerName"
Write-Host "  Database:   $DbName"
Write-Host "  User:       $DbUser"
Write-Host "  Output:     $OutputFile"
Write-Host ""

# Check if Docker is running
$dockerCheck = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker is not running" -ForegroundColor Red
    exit 1
}

# Check if container exists and is running
$runningContainers = docker ps --format "{{.Names}}" 2>&1
if ($runningContainers -notcontains $ContainerName) {
    Write-Host "[ERROR] Container '$ContainerName' is not running" -ForegroundColor Red
    Write-Host "   Start it with: docker-compose up -d postgres" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/2] Starting database export..." -ForegroundColor Green

try {
    # Export using pg_dump with custom format (includes compression)
    # Create dump inside container first, then copy out
    docker exec -e PGPASSWORD=$DbPassword $ContainerName pg_dump -U $DbUser -d $DbName -Fc -v -b -f /tmp/export.dump 2>&1

    Write-Host "[2/2] Copying backup file from container..." -ForegroundColor Green

    # Copy from container to host
    docker cp "${ContainerName}:/tmp/export.dump" $OutputFile

    # Cleanup temp file in container
    docker exec $ContainerName rm -f /tmp/export.dump

    # Verify the export file
    if ((Test-Path $OutputFile) -and ((Get-Item $OutputFile).Length -gt 0)) {
        $FileSize = (Get-Item $OutputFile).Length
        $FileSizeMB = [math]::Round($FileSize / 1MB, 2)
        Write-Host ""
        Write-Host "[SUCCESS] Export completed successfully!" -ForegroundColor Green
        Write-Host "   File: $OutputFile"
        Write-Host "   Size: ${FileSizeMB} MB"
        Write-Host ""
        Write-Host "To import on another system, use:" -ForegroundColor Cyan
        Write-Host "   .\import-windows.ps1 -InputFile $OutputFile"
    } else {
        Write-Host "[ERROR] Export failed or file is empty" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[ERROR] Error during export: $_" -ForegroundColor Red
    exit 1
}

tools/db/
├── export-macos.sh      # macOS/zsh
├── export-linux.sh      # Linux/bash
├── export-windows.ps1   # Windows/PowerShell
├── import-macos.sh      # macOS/zsh
├── import-linux.sh      # Linux/bash
└── import-windows.ps1   # Windows/PowerShell

## Verwendung

| Plattform | Export | Import |
|-----------|--------|--------|
| macOS | `./tools/db/export-macos.sh [output.dump]` | `./tools/db/import-macos.sh backup.dump` |
| Linux | `./tools/db/export-linux.sh [output.dump]` | `./tools/db/import-linux.sh backup.dump` |
| Windows | `.\tools\db\export-windows.ps1 [-OutputFile output.dump]` | `.\tools\db\import-windows.ps1 -InputFile backup.dump` |

## Features
- Verwendet pg_dump mit Custom-Format (komprimiert, portabel)
- pgvector-Erweiterung wird beim Import automatisch aktiviert
- Konfigurierbar via Umgebungsvariablen (DB_CONTAINER, DB_NAME, DB_USER, DB_PASSWORD)
- Bestätigung vor dem Import (löscht existierende Datenbank)
- Datei wird ins Container kopiert (funktioniert zwischen verschiedenen Hosts)
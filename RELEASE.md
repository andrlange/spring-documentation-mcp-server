# Release 1.8.1 - Documentation Tools Improvements

## Commit Message

```
Release 1.8.1 - Documentation Tools Improvements

- Enhanced findProjectsByUseCase with semantic embeddings for improved project discovery
- Enhanced getCodeExamples with vector similarity search for better code matching
- Made getLatestSpringBootVersion parameters optional (returns latest GA + Current when omitted)
- Added Spring Projects embedding support with new database migration V27
- Updated Embeddings Dashboard to show Projects coverage statistics
- Added Python testing framework for Documentation MCP tools (usecases/test_documentation_tools.py)
```

## Release Notes

### Version 1.8.1 (2026-01-12)

#### New Features

**Semantic Search for Documentation Tools**
- `findProjectsByUseCase` now leverages semantic embeddings when enabled, providing more accurate project discovery based on use case descriptions
- `getCodeExamples` enhanced with vector similarity search for better matching of code examples to user queries
- New embedding support for Spring Projects (name + description) stored in `spring_projects` table

**Improved getLatestSpringBootVersion Tool**
- Both `majorVersion` and `minorVersion` parameters are now optional
- When omitted, returns the latest GA and Current Spring Boot versions
- Results are ordered by version descending (newest first)
- Maintains full backward compatibility when parameters are provided

**Python Testing Framework**
- New `usecases/test_documentation_tools.py` for comprehensive testing of all 12 Documentation MCP tools
- Two pre-configured use cases: App Modernization and App Seeding
- Uses MCP Streamable-HTTP protocol for direct server communication

#### Technical Changes

- Database Migration `V27__spring_projects_embedding.sql`: Adds embedding columns to `spring_projects` table
- Updated `EmbeddingSyncService` with PROJECT entity support
- Extended `HybridSearchService` with `searchProjects()` method using RRF algorithm
- Updated Embeddings Dashboard UI to display Projects embedding coverage

#### Files Changed

**Version Updates:**
- `build.gradle` - version = '1.8.1'
- `application.yml` - info.app.version, spring.ai.mcp.server.version, mcp.server.version
- `build-container.sh` - APP_VERSION="1.8.1"
- `docker-compose-all.yaml` - image tag updated
- `README.md` - version header, jar filename, Recent Releases table
- `CHANGELOG.md` - new version entry and Version Summary table
- `VERSIONS.md` - Current Versions table and Changelog section
- `CLAUDE.md` - version header and Recent Changes section

**Feature Implementation:**
- `src/main/java/com/spring/mcp/mcp/tools/SpringDocumentationTools.java`
- `src/main/java/com/spring/mcp/service/embedding/EmbeddingSyncService.java`
- `src/main/java/com/spring/mcp/service/embedding/HybridSearchService.java`
- `src/main/java/com/spring/mcp/model/entity/SpringProject.java`
- `src/main/resources/db/migration/V27__spring_projects_embedding.sql`
- `src/main/resources/templates/embeddings/index.html`
- `usecases/test_documentation_tools.py`
- `usecases/MCP_Improvement_Documentation.md`

## Build & Deploy

```bash
# Build the application
./gradlew clean build

# Build Docker container
./build-container.sh

# Run with Docker Compose
docker-compose -f docker-compose-all.yaml up -d
```

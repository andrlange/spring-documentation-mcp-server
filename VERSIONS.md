# Spring MCP Server - Version Configuration

This document tracks the version configuration across all project files to ensure consistency.

## Current Versions

| Component | Version | Notes |
|-----------|---------|-------|
| **Application** | 1.7.1 | Spring MCP Server |
| **Java (JDK)** | 25 | LTS version |
| **Spring Boot** | 3.5.9 | Latest stable |
| **Spring AI** | 1.1.2 | MCP Server support |
| **PostgreSQL** | 18-alpine | Docker image |
| **Gradle** | 9.2.0 | Build tool |

## Files with Version References

### CHANGELOG.md
Needs to track the changelog  

### build.gradle
```groovy
version = '1.7.1'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

ext {
    springAiVersion = '1.1.2'
}
```

### Dockerfile
```dockerfile
FROM eclipse-temurin:25-jdk-alpine AS builder
FROM eclipse-temurin:25-jre-alpine
```

### docker-compose-all.yaml
```yaml
services:
  postgres:
    image: postgres:18-alpine
  spring-boot-documentation-mcp-server:
    image: spring-boot-documentation-mcp-server:1.7.1
```

### application.yml
```yaml
# Application Info (used in footer)
info:
  app:
    name: Spring MCP Server
    version: 1.7.1
  spring-boot:
    version: 3.5.9

spring:
  threads:
    virtual:
      enabled: true  # Virtual threads enabled (Java 21+)
  ai:
    mcp:
      server:
        version: "1.7.1"
```

### build-container.sh
```bash
APP_VERSION="1.7.1"
JAVA_VERSION="25"
```

## Updating Versions

When updating versions, ensure all files listed above are updated consistently:

1. **Application Version Update**:
   - `build.gradle` - `version` property
   - `application.yml` - `info.app.version` and `spring.ai.mcp.server.version`
   - `build-container.sh` - `APP_VERSION`
   - `docker-compose-all.yaml` - `spring-boot-documentation-mcp-server` image tag
   - `README.md` - version header, jar filename, and Recent Releases table
   - `CHANGELOG.md` - add new version entry and update Version Summary table
   - `VERSIONS.md` - update changelog section at the bottom

2. **Spring Boot Version Update**:
   - `build.gradle` - Spring Boot plugin version
   - `application.yml` - `info.spring-boot.version` (for footer display)
   - `README.md` - version references

3. **Java/JDK Version Update**:
   - `build.gradle` - `languageVersion`
   - `Dockerfile` - base images (both builder and runtime)
   - `build-container.sh` - `JAVA_VERSION`
   - `.claude/memory/project-memory.md` - Java version reference

4. **PostgreSQL Version Update**:
   - `docker-compose.yml` - postgres image tag

## MCP Tools Count

Current: **46 tools** (12 documentation + 7 migration + 7 language evolution + 8 flavors + 3 flavor groups + 5 initializr + 4 javadoc)

Update these locations when adding/removing tools:
- `application.yml` - `spring.ai.mcp.server.instructions`
- `README.md` - tool documentation
- `ADDITIONAL_CONTENT.md` - tool detailed documentation
- `.claude/memory/project-memory.md` - MCP Tools count

## Changelog

### v1.7.1 (2026-01-08)
- Fixed SpringBootVersionSyncService to create versions with active OSS support (not just enterprise)
  - Undocumented but still-supported Spring Boot versions (e.g., 3.4.x) are now created during Phase 0
  - Enables Phase 1 (Spring Generations) to properly find and map compatibility data
- Fixed GitHubCodeExampleService substring range error for empty/minimal javadocs
  - Handles edge case of javadocs like `/***/ where closing `*/` is immediately after opening `/**`
- Suppressed AsciidoctorJ cross-document reference warnings (set log level to ERROR)

### v1.7.0 (2026-01-06)
- Spring Boot Wiki Integration (Release Notes & Migration Guides from GitHub wiki)
- New UI page `/wiki` for browsing release notes and migration guides
- 2 new MCP tools: `getWikiReleaseNotes`, `getWikiMigrationGuide`
- Phase 10 in comprehensive sync for wiki content synchronization
- AsciiDoc to Markdown conversion for wiki content
- Flexmark for Markdown to HTML rendering
- Full-text search across wiki content with PostgreSQL TSVECTOR
- Documentation Tools: 10 → 12, Total MCP Tools: 44 → 46

### v1.6.3 (2026-01-05)
- MCP Tool Response Size Optimization
  - `getFlavorsByCategory`: Returns summaries only (~94% reduction)
  - `listProjectsBySpringBootVersion`: Returns only latest GA per project (~62% reduction)
  - Added `allVersions` parameter for full version list when needed
- Fixed Spring Boot `isCurrent` flag - only latest GA version marked as current

### v1.6.2 (2026-01-03)
- MCP Tool Masquerading feature for dynamic tool visibility and description control
- New `/mcp-tools` UI page for managing MCP tool configurations (ADMIN role)
- Per-tool and group-level toggle switches
- Custom description editing with reset-to-original capability
- Database migration V23 for mcp_tools table
- New entities: McpTool, McpToolGroup enum
- New services: McpToolService, McpToolMasqueradingService
- Comprehensive unit tests for all new components

### v1.6.1 (2026-01-02)
- Virtual Threads support for all async operations (JEP 444)
- Central AsyncConfig with Spring-managed virtual thread executors
- Refactored BootstrapController to use @Async instead of manual threads
- Refactored DocumentationIndexer to use injected executor instead of per-batch pools
- Simplified EmbeddingConfig with virtual threads (removed manual pool sizing)
- Added "Virtual Threads (Java 21+)" documentation section to README.md

### v1.6.0 (2026-01-01)
- Added Semantic Search / Embeddings Feature
- Hybrid search combining keyword (TSVECTOR) and semantic (vector) search
- pgvector integration for vector similarity search
- Support for Ollama (nomic-embed-text) and OpenAI (text-embedding-3-small) embedding providers
- Intelligent text chunking with overlap for large documents
- Reciprocal Rank Fusion (RRF) algorithm for combining search results
- Configurable via `mcp.features.embeddings.enabled` (default: false)
- Migration transformations and flavors now support semantic search when enabled

### v1.5.4 (2025-12-25)
- Added collapsible sidebar menu with toggle button
- Sidebar state persisted in localStorage
- Icons-only view when collapsed with tooltips on hover
- Smooth CSS transitions for collapse/expand animation

### v1.5.3 (2025-12-19)
- Added optional Display Name field for users (shown in header and dashboard welcome message)
- Bumped Spring Boot from 3.5.8 to 3.5.9
- Database migration V20 for display_name column

### v1.5.2 (2025-12-17)
- Language Evolution Enhancement: JEP/KEP specification storage and detail pages
- New JEP/KEP detail routes: `/languages/jep/{number}` and `/languages/kep/{number}`
- JEP fetcher: Downloads full JEP content from openjdk.org
- KEP fetcher: Downloads from GitHub KEEP repo + YouTrack fallback
- Synthesized code examples for all missing language features
- Official/Synthesized badges on code examples in UI
- Internal JEP/KEP links with external link icons
- New MCP tool: `getLanguageFeatureExample` - Get code example for a feature by JEP/KEP number or name
- Total MCP tools: 44 (was 43)

### v1.5.1 (2025-12-17)
- Fixed Javadoc MCP tools "Transaction silently rolled back" error
- Added `@Transactional(propagation = REQUIRES_NEW)` to metrics recording
- Added eager-loading query methods to prevent lazy loading issues

### v1.5.0 (2025-12-16)
- MCP Monitoring Dashboard with real-time metrics and analytics
- Tool usage tracking by group with time period selection (5m, 1h, 24h)
- Connection event monitoring and API key usage statistics
- Performance metrics: latency tracking, success/error rates
- Data retention configuration with manual cleanup option
- Fixed monitoring metrics query to always use FIVE_MIN buckets
- Fixed checkVersionCompatibility tool response format

### v1.4.0 (2025-12-06)
- Boot Initializr integration with start.spring.io
- Caffeine caching architecture for high-performance metadata caching
- 5 new MCP tools for dependency search and project generation
- Two-tab UI design for project configuration and dependencies
- Build file preview (pom.xml/build.gradle) modal
- Total MCP tools: 39

### v1.3.4 (2025-12-05)
- Spring AI updated from 1.1.0 to 1.1.1
- Library updates to fix CVE-2025-48924

### v1.3.3 (2025-12-04)
- Added Flavor Groups feature (3 new MCP tools) - Team-based authorization and organization for Flavors
- New tools: `listFlavorGroups`, `getFlavorsGroup`, `getFlavorGroupStatistics`
- Public groups (no members) visible to everyone, Private groups (has members) visible only to members
- Inactive groups and their flavors are completely hidden from UI and MCP
- Database: V9 migration adds flavor_groups, group_user_members, group_apikey_members, group_flavors tables
- Total MCP tools: 34

### v1.3.2 (2025-12-02)
- Enhanced Flavors Import/Export with YAML front matter header support
- Import: Automatic metadata parsing from YAML header (unique-name, display-name, category, pattern-name, description, tags)
- Export: Modal with toggle to include/exclude metadata header (default: enabled)
- Kebab-case field naming convention in YAML headers
- Auto-rename with warning for duplicate unique names on import
- Category validation supports enum names and display names

### v1.3.1 (2025-12-01)
- GitHub Documentation Scanner: New documentation and code example scanner using GitHub sources from spring-projects
- Extended Documentation Page with cascaded documentation view (spring.io docs + GitHub source docs)
- Extended Code Examples Page with topic grouping and code view/copy modal
- Syntax highlighting with highlight.js (Atom One Dark theme)
- Fixed code examples preserving line breaks (using getTextContent() instead of asNormalizedText())
- Added Kotlin "K" gradient icon in Dashboard Language Evolution section
- Added Java coffee cup icon and Kotlin "K" icon on Languages page

### v1.3.0 (2025-11-30)
- Added Flavors feature (8 new MCP tools) - Company guidelines, architecture patterns, compliance rules, agent configurations, and initialization templates
- Fixed login page CSS loading issue (added /vendor/** to security permitAll)
- Fixed HTTP Basic Auth modal popup on login page
- Added Flavors section to Dashboard with category statistics
- Total MCP tools: 31

### v1.2.0 (2025-11-29)
- Added Language Evolution feature (6 new MCP tools)
- Updated Java from 21 to 25 (LTS)
- Total MCP tools: 23

### v1.1.0 (Previous)
- Added OpenRewrite Migration Recipes feature (7 new MCP tools)
- Total MCP tools: 17

### v1.0.0 (Initial)
- Core Spring Documentation MCP Server
- Total MCP tools: 10

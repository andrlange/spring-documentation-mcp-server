# Changelog

All notable changes to the Spring Documentation MCP Server are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.8.0] - 2026-01-09

### Added
- **MCP Streamable-HTTP Transport**: Migrated from SSE-based transport to Streamable-HTTP protocol
    - **MCP Protocol 2025-11-25**: Updated to latest MCP specification for Streamable-HTTP transport
    - **Single Unified Endpoint**: `/mcp/spring` replaces `/mcp/spring/sse` and `/mcp/spring/messages`
    - **Tool Change Notifications**: Enabled `tool-change-notification: true` for MCP Tool Masquerading support
    - **Session Management**: `Mcp-Session-Id` header for stateful session handling
    - **Keep-Alive**: Configurable keep-alive interval (30s default) for connection maintenance

### Changed
- MCP endpoint changed from `/mcp/spring/sse` to `/mcp/spring`
- Protocol version header updated from `2025-06-18` to `2025-11-25`
- Claude Code configuration now uses `type: "streamable-http"` instead of SSE transport
- Updated UI templates to display new Streamable-HTTP endpoint

### Technical Details
- Uses Spring AI `spring-ai-starter-mcp-server-webmvc` with `protocol: STREAMABLE`
- Full backwards compatibility with MCP Tool Masquerading feature (`/mcp-tools`)
- Runtime tool modifications (`addTool`/`removeTool`) continue to work with stateful sessions
- All 46 MCP tools unchanged - no tool code modifications needed
- Legacy SSE endpoints retained for transition period

---

## [1.7.1] - 2026-01-08

### Fixed
- **SpringBootVersionSyncService OSS Support**: Fixed Phase 0 sync to create versions with active OSS support
    - Previously, undocumented but still-supported Spring Boot versions (e.g., 3.4.x, 3.3.x) were only created when enterprise mode was enabled
    - Now creates version entries for any generation with active OSS or enterprise support
    - Enables Phase 1 (Spring Generations) to properly find and map compatibility data
    - Resolves "Spring Boot version X.X.x not found in spring_boot_versions table" errors during comprehensive sync
- **GitHubCodeExampleService Range Error**: Fixed substring range error for empty/minimal javadocs
    - Error: `Range [X, Y) out of bounds for length Z` when processing Java files with minimal javadocs like `/***/`
    - Added bounds check before substring extraction to handle edge case where `*/` appears immediately after `/**`
- **AsciidoctorJ Logging**: Suppressed cross-document reference warnings
    - Set `asciidoctor` log level to ERROR to hide "possible invalid reference" warnings
    - These warnings occur when AsciiDoc files reference sections in other documents (expected behavior)

---

## [1.7.0] - 2026-01-06

### Added
- **Spring Boot Wiki Integration**: Access release notes and migration guides from the official Spring Boot GitHub wiki
    - **New UI Page**: Browse wiki content at `/wiki` with search functionality
    - **Release Notes**: Version-specific release notes from the Spring Boot wiki
    - **Migration Guides**: Step-by-step migration guides between Spring Boot versions
    - **Full-Text Search**: PostgreSQL TSVECTOR indexing for fast wiki content search
    - **AsciiDoc Support**: Automatic AsciiDoc to Markdown conversion using AsciidoctorJ
    - **Markdown Rendering**: Flexmark-based HTML rendering with syntax highlighting
    - **Phase 10 Sync**: Wiki content synchronized in comprehensive sync process
    - **Sidebar Navigation**: New "Wiki" menu entry with journal icon
- **2 New MCP Tools** (Documentation Group):
    - `getWikiReleaseNotes`: Get Spring Boot release notes for a specific version
    - `getWikiMigrationGuide`: Get migration guide between Spring Boot versions
- **Database Migration**: `V26__wiki_integration.sql`
    - New tables: `wiki_release_notes`, `wiki_migration_guides`
    - Full-text search indexes with TSVECTOR
    - Embedding columns for future semantic search support

### Changed
- Total MCP tools increased from 44 to **46**
- Documentation tools increased from 10 to **12**
- Comprehensive sync now has 11 phases (0-10)

---

## [1.6.3] - 2026-01-05

### Fixed
- **MCP Tool Response Size Optimization**: Reduced context consumption for large MCP tool responses
    - **`getFlavorsByCategory`**: Returns summaries only (metadata without full content)
        - Before: ~122K chars | After: ~7.5K chars (~94% reduction)
        - Use `getFlavorByName(uniqueName)` to get full content when needed
    - **`listProjectsBySpringBootVersion`**: Returns only latest GA version per project by default
        - Before: ~10.6k tokens (206 versions) | After: ~4k tokens (48 versions) (~62% reduction)
        - Removed `.x` placeholder versions (e.g., `3.5.x`) with empty URLs
        - Added `allVersions: Boolean` parameter to include all versions when needed
- **Spring Boot Version `isCurrent` Flag**: Fixed multiple versions incorrectly marked as current
    - Issue: Both 4.0.0 and 4.0.1 were marked as `isCurrent=true` after sync
    - Root cause: Sync service blindly trusted spring.io's `current` flag without validation
    - Solution: Added `fixCurrentVersionFlag()` post-processing to ensure only the latest GA version is marked as current

### Changed
- `SpringDocumentationTools.listProjectsBySpringBootVersion()` now accepts optional `allVersions` parameter
- `SpringBootVersionSyncService` now validates and corrects `isCurrent` flag after sync

---

## [1.6.2] - 2026-01-03

### Added
- **MCP Tool Masquerading**: Management UI for MCP tool visibility and descriptions
    - **Per-Tool Toggle**: Track enabled/disabled status for individual MCP tools
    - **Group Toggle**: Track status for entire tool groups (DOCUMENTATION, MIGRATION, LANGUAGE, FLAVORS, FLAVOR_GROUPS, INITIALIZR, JAVADOC)
    - **Custom Descriptions**: Store custom tool descriptions to provide guidance to AI clients
    - **Reset to Original**: Restore original tool descriptions at any time
    - **Persistent State**: Tool configurations stored in database and persist across restarts
- **New UI Page**: MCP Tools management page at `/mcp-tools` (ADMIN role required)
    - Organized by tool group with expandable sections
    - Toggle switches for individual tools and group-wide toggles
    - Edit modal for customizing tool descriptions
    - Statistics showing enabled/disabled tool counts
- **Database Migration**: `V23__mcp_masquerading.sql`
    - New `mcp_tools` table storing tool configurations
    - Columns: tool_name, tool_group, enabled, description, original_description, display_order
    - Auto-populated on first startup with all 44 tools
- **New Entities**: `McpTool`, `McpToolGroup` enum
- **New Services**: `McpToolService`, `McpToolMasqueradingService`
- **New Controller**: `McpToolsController` with REST endpoints for tool management
- **Unit Tests**: Comprehensive test coverage for new services, controller, and initializer

### Technical Details
- Tool groups: DOCUMENTATION (10), MIGRATION (7), LANGUAGE (7), FLAVORS (8), FLAVOR_GROUPS (3), INITIALIZR (5), JAVADOC (4)
- Total: 44 tools manageable via the UI
- Security: `/mcp-tools` page requires ADMIN role and is excluded from API key authentication filter

---

## [1.6.1] - 2026-01-02

### Added
- **Virtual Threads Support**: Migrated all asynchronous operations to Java Virtual Threads (JEP 444)
    - **Central AsyncConfig**: New `AsyncConfig.java` providing Spring-managed virtual thread executors
    - **Named Executors**: `virtualThreadExecutor`, `taskExecutor`, `indexingExecutor`, `bootstrapExecutor`
    - **Spring Lifecycle Integration**: Graceful shutdown and proper thread management
    - **Uncaught Exception Handler**: Centralized logging for async method failures
- **Documentation**: Added "Virtual Threads (Java 21+)" section to README.md explaining architecture and benefits

### Changed
- **BootstrapController**: Replaced manual `new Thread()` with `@Async("bootstrapExecutor")` method
    - Bootstrap operations now run on Spring-managed virtual threads
    - Improved error handling and logging
- **DocumentationIndexer**: Refactored to use injected `indexingExecutor` instead of creating per-batch thread pools
    - Removed `Executors.newFixedThreadPool()` calls that created new pools for each batch
    - Parallel indexing now uses shared, Spring-managed virtual thread executor
    - Added proper timeout handling for indexing tasks
- **EmbeddingConfig**: Simplified to use virtual threads
    - Removed `@EnableAsync` (centralized in `AsyncConfig`)
    - Replaced `ThreadPoolTaskExecutor` with `Executors.newVirtualThreadPerTaskExecutor()`
    - No pool sizing required with virtual threads - JVM handles scheduling automatically
- **application.yml**: Added `spring.threads.virtual.enabled: true` to enable virtual threads globally

### Technical Details
- Virtual threads are lightweight (~1KB vs ~1MB for platform threads)
- Ideal for I/O-bound operations: HTTP calls, database queries, file operations
- All async operations now benefit from virtual thread scalability
- Spring Boot 3.2+ automatically configures Tomcat/Jetty/Undertow to use virtual threads when enabled

---

## [1.6.0] - 2026-01-01

### Added
- **Semantic Search / Embeddings Feature**: Optional AI-powered semantic search capability
    - **Hybrid Search**: Combines PostgreSQL full-text search (TSVECTOR) with vector similarity search
    - **pgvector Integration**: Uses PostgreSQL pgvector extension for efficient vector storage and similarity queries
    - **Embedding Providers**: Support for Ollama (nomic-embed-text) and OpenAI (text-embedding-3-small)
    - **Intelligent Chunking**: Text chunking service with configurable chunk size and overlap for large documents
    - **RRF Algorithm**: Reciprocal Rank Fusion for combining keyword and semantic search results
    - **Enhanced MCP Tools**: Migration knowledge and flavor search now use hybrid search when enabled
- **Database Migrations**:
    - `V21__embeddings_infrastructure.sql`: Adds pgvector extension and embedding columns
    - `V22__embedding_indexes.sql`: Creates vector similarity indexes
- **Configuration**:
    - Enable via `mcp.features.embeddings.enabled=true`
    - Configure provider: `mcp.features.embeddings.provider=ollama` or `openai`
    - Ollama: `mcp.features.embeddings.ollama.base-url` and `model`
    - OpenAI: `mcp.features.embeddings.openai.api-key` and `model`
    - Hybrid search alpha: `mcp.features.embeddings.hybrid.alpha` (0.3 = 70% semantic, 30% keyword)
    - Chunk settings: `mcp.features.embeddings.chunk-size` and `chunk-overlap`
- **New Services**:
    - `EmbeddingService`: Interface for embedding generation with NoOp fallback
    - `EmbeddingServiceImpl`: Implementation with chunking and averaging support
    - `OllamaEmbeddingProvider`: Ollama integration via Spring AI
    - `OpenAIEmbeddingProvider`: OpenAI integration via Spring AI
    - `ChunkingService`: Intelligent text splitting by paragraph/sentence/character
    - `HybridSearchService`: Combines keyword and vector search with RRF
- **Testing**:
    - Comprehensive unit tests for all embedding services
    - Integration tests with Testcontainers (pgvector)

### Changed
- Docker Compose: postgres image now uses `pgvector/pgvector:pg18` for vector support
- Migration tools and Flavor tools now optionally use hybrid search when embeddings enabled

### Technical Details
- Embeddings are generated lazily on first search when enabled
- Cosine similarity is used for vector distance calculation
- Default embedding dimensions: 768 (nomic-embed-text) or 1536 (text-embedding-3-small)
- Feature is disabled by default to avoid requiring embedding infrastructure

---

## [1.5.4] - 2025-12-25

### Added
- **Collapsible Sidebar Menu**: New toggle button to collapse/expand the sidebar navigation
    - Toggle button (chevron icon) at the top of the sidebar
    - When collapsed: Shows only icons (60px width) with tooltips on hover
    - When expanded: Shows full menu items with icons and text
    - Main content area dynamically expands/contracts when sidebar is toggled
    - Smooth CSS transitions (0.3s) for both sidebar and main content
- **State Persistence**: Sidebar collapsed/expanded state saved to localStorage
    - State persists across page navigations without flicker
    - Early state application via inline script prevents visual flickering during navigation
- **Session-Aware Reset**: Sidebar state automatically resets to expanded on the login page
    - Ensures a consistent expanded menu after logout and fresh login

### Fixed
- **SNAPSHOT → GA Version Transition**: Fixed bug where SNAPSHOT versions remained orphaned when GA versions were released
    - Example: Spring Modulith 2.0.1-SNAPSHOT was not being upgraded to 2.0.1 GA during sync
    - The sync now properly detects corresponding SNAPSHOT versions and upgrades them in-place
    - Preserves existing data (compatibility mappings, documentation links) during the upgrade

### Changed
- Files modified:
    - `src/main/resources/templates/fragments/sidebar.html` - Added toggle button and wrapped menu text in span elements
    - `src/main/resources/templates/layouts/main.html` - Added inline script and CSS for early state application
    - `src/main/resources/templates/login.html` - Added localStorage reset script
    - `src/main/resources/static/css/custom.css` - Added collapsible sidebar styles
    - `src/main/resources/static/js/app.js` - Added toggle functionality with localStorage persistence
    - `src/main/java/com/spring/mcp/service/sync/SpringProjectPageCrawlerService.java` - Added SNAPSHOT → GA upgrade logic

---

## [1.5.3] - 2025-12-19

### Added
- **User Display Name**: Optional display name field for users
    - New `displayName` column in users table (max 100 characters)
    - Display name shown in header navbar and dashboard welcome message
    - Falls back to username if display name is not set
    - User form updated with Display Name input field
    - User detail page shows Display Name (or "Not set" if empty)
    - Helper method `getDisplayNameOrUsername()` for unified access
- **Database Migration**: `V20__user_display_name.sql` adds display_name column

### Changed
- **Spring Boot**: Bumped from 3.5.8 to 3.5.9
- Header fragment now uses global model attribute `currentUserDisplayName` instead of `sec:authentication="name"`
- Dashboard welcome banner now shows display name (if set) instead of username
- UsersController now saves displayName field on user update

---

## [1.5.2] - 2025-12-17 - HotFix 1 & HotFix 2 - 2025-12-18

### Added
- **Language Evolution Enhancement**: JEP/KEP specification storage and detail pages
    - **JEP Specifications**: Download and store full JEP content from openjdk.org
    - **KEP Specifications**: Fetch from GitHub KEEP repository with YouTrack fallback
    - **Detail Pages**: New routes `/languages/jep/{number}` and `/languages/kep/{number}` with dark theme
    - **Synthesized Examples**: Code examples for all missing language features marked as SYNTHESIZED
    - **UI Improvements**: Internal JEP/KEP links with external link icons, Official/Synthesized badges on code examples
- **New MCP Tool**: `getLanguageFeatureExample` - Get code example for a feature
    - Search by JEP number (e.g., '444'), KEP number (e.g., 'KT-11550'), or feature name (e.g., 'Virtual Threads')
    - Returns code example with feature description, source type (Official/Synthesized), and metadata
    - Language Evolution tools: 6 → 7 tools
- **Database Migration**: `V18__jep_kep_specifications.sql`
    - New tables: `jep_specifications`, `kep_specifications`
    - New column: `example_source_type` on `language_features`
    - Full-text search indexes for JEP/KEP content
- **New Entities**: `JepSpecification`, `KepSpecification`
- **New Services**: `JepFetcherService`, `KepFetcherService`
- **Enhanced LanguageSyncService**: Phase 5 for JEP/KEP specification sync
- **Debug Logging**: Added detailed logging to `loadKotlinExamples()` for code example matching troubleshooting

### Changed
- Language evolution code examples now track source type (OFFICIAL vs SYNTHESIZED)
- JEP/KEP links in features list are now internal with external link option
- Total MCP tools: 43 → 44

### Fixed
- **JEP/KEP Detail Page Thymeleaf Error**: Fixed `T(System).lineSeparator()` causing "Instantiation of new objects and access to static classes is forbidden" error
    - Replaced forbidden static class access with HTML entity matching (`&#10;` for newline, `&#13;` for carriage return)
    - Affected templates: `jep-detail.html` (4 occurrences), `kep-detail.html` (2 occurrences)
- **JEP/KEP Detail Page Dark Theme**: Fixed light/white backgrounds not matching dark theme
    - Updated CSS in both `jep-detail.html` and `kep-detail.html` with proper dark colors
    - Section cards, tables, content areas now use consistent dark theme colors (#1a1d21, #0d1117, #161b22)
- **KepFetcherService Bean Creation Error**: Fixed "No default constructor found" error at startup
    - Root cause: Lombok `@RequiredArgsConstructor` conflicted with manual 2-parameter constructor
    - Solution: Removed `@RequiredArgsConstructor` annotation, kept manual constructor
- **Kotlin Code Examples Not Loading**: Fixed code examples not matching Kotlin 2.1/2.2/2.3 features
    - Added missing `multi-dollar-string-interpolation-preview` entry to `language-examples.json`
    - All 21 Kotlin features for versions 2.1-2.3 now have code examples
- **Monitoring page creates flickering by css events**: Fixed by removing looping events from css objects
- **Partisally wrong JEP Numbers and Examples**: Fixed JEP Numbering and added missing examples

---

## [1.5.1] - 2025-12-17

### Fixed
- **Javadoc MCP Tools Transaction Rollback**: Fixed "Transaction silently rolled back because it has been marked as rollback-only" error affecting all 4 Javadoc tools (`listJavadocLibraries`, `searchJavadocs`, `getClassDoc`, `getPackageDoc`)
    - **Root Cause**: `McpToolMonitoringAspect` was trying to INSERT metrics data within read-only transactions from Javadoc tool methods
    - **Solution**: Added `@Transactional(propagation = Propagation.REQUIRES_NEW)` to `McpMonitoringService.recordToolCall()` so metrics are recorded in a separate transaction
    - Also added eager-loading query methods to `JavadocClassRepository` to prevent lazy loading issues:
        - `findByLibraryVersionAndFqcnWithMembers()` - fetches class with methods, fields, constructors
        - `searchByKeywordWithPackage()` - searches with eager-loaded package
        - `searchByKeywordGlobalWithPackage()` - global search with eager-loaded package
    - Updated `JavadocTools.java` to use the new eager-loading methods

---

## [1.5.0] - 2025-12-16

### Added
- **MCP Monitoring Dashboard**: Comprehensive real-time monitoring for MCP server operations
    - **Dashboard UI**: New dark-themed monitoring page at `/monitoring` (ADMIN role required)
    - **Overview Cards**: Total requests, active connections, latency metrics, error rates
    - **Tool Usage by Group**: Metrics organized by tool categories with expandable details
        - Documentation, Versions, Migration, Language, Flavors, Initializr/Javadoc groups
        - Individual tool metrics: request count, success/error rates, latency stats
    - **Time Period Selection**: View metrics for 5 minutes, 1 hour, or 24 hours
    - **Performance Tracking**: Average, minimum, maximum latency per tool
    - **Connection Monitoring**: Active connections, connection events, error tracking
    - **API Key Usage**: Request counts per API key with last-used timestamps
    - **Client Usage Statistics**: Top clients by connection count
    - **Data Retention**: Configurable retention period with manual cleanup option
    - **Auto-Refresh**: Configurable refresh interval (default: 30 seconds)
- **Monitoring Infrastructure**:
    - New `MonitoringController` for dashboard and API endpoints
    - New `McpMonitoringService` for metrics recording and retrieval
    - New `McpMonitoringCleanupService` for data retention management
    - AOP aspect (`McpToolMetricsAspect`) for automatic tool call metrics recording
    - New entities: `McpMetricsAggregate`, `McpConnectionEvent`, `MonitoringSettings`
    - New enums: `BucketType`, `ConnectionEventType`, `MetricType`
    - New DTOs: `MonitoringOverviewDto`, `ToolGroupDto`, `ToolMetricDto`, `ToolDetailDto`, `ApiKeyUsageDto`, `ClientUsageDto`
- **Database Migrations**:
    - `V15__monitoring_tables.sql`: Monitoring infrastructure tables
    - `V16__api_key_request_count.sql`: API key request count tracking

### Fixed
- **checkVersionCompatibility Tool**: Verified tool correctly uses `spring_boot_compatibility` data from spring.io sync
- **GitHub Sync Hibernate Session Corruption**: Fixed transaction isolation issues that caused cascading failures during GitHub documentation sync
    - One project failure no longer corrupts the entire sync process
    - Each project sync now runs in its own isolated transaction (`REQUIRES_NEW` propagation)
    - Entity manager is cleared after failures to prevent "null identifier" errors
    - Better error logging with full stack traces for debugging
- **GitHub Sync Performance**: Added skip logic for already-synced documentation
    - GA (stable) versions are now skipped if documentation already exists (static content won't change)
    - SNAPSHOT/RC/MILESTONE versions are always re-synced (may have updates)
    - Significantly reduces GitHub API calls and sync time for subsequent syncs
    - Sync summary now shows "Versions Skipped (already synced)" count
    - Code examples follow the same skip logic for GA versions

### Technical Details
- Metrics aggregation uses 5-minute buckets for efficient storage and querying
- SSE connection tracking via `SseConnectionTrackingFilter`
- MCP protocol header validation via `McpProtocolHeaderFilter`
- HTMX integration for dynamic dashboard updates without full page reload

---

## [1.4.3] - 2025-12-12

### Added
- **Javadoc Sync Version Filter**: New settings to control which version types are included in Javadoc synchronization
    - Toggle switches for SNAPSHOT, RC (Release Candidate), and Milestone (M) versions
    - By default, only GA (General Availability) and CURRENT versions are synced
    - Settings UI in `/settings` page with instant AJAX save
    - Example: For Spring AI with versions `1.1.2` (GA), `2.0.0-SNAPSHOT`, `2.0.0-M1`, filters let you choose exactly which to sync
    - Helps reduce sync time and storage by excluding pre-release versions when not needed
- **Login Screen Version Display**: App version number now shown below the title on the login page
    - Created dedicated `LoginController` to replace view controller (required for model attributes)
    - Reads version from `info.app.version` property in `application.yml`
    - Subtle styling to not distract from login form
- **Sidebar Synchronization Link**: Added "Synchronization" menu item in admin sidebar
    - Visible only to ADMIN role users
    - Links to `/sync` page (same as Dashboard "Sync Projects" button)
    - Blue sync icon (`bi-arrow-repeat`) consistent with other menu styling
- **Sync Guide Update**: Comprehensive update to the Sync Guide on `/sync` page
    - Added detailed phases overview table with all 10 phases (0-9)
    - Shows phase name, description, and estimated duration for each phase
    - Added "Additional Sync Options" section for Languages and Fix Documentation Versions
    - Updated best practices to reference all phases and new Javadoc Version Filter

### Changed
- **Spring AI**: Updated from 1.1.1 to 1.1.2

### Fixed
- **Flavor Groups Toggle Error**: Fixed JPA exception when activating/deactivating flavor groups on `/groups` page
    - Issue: `updateGroup()` method was overwriting non-null fields with null values when only `isActive` was being updated
    - Solution: Changed to selective field update - only non-null fields in the update request are applied
    - Location: `FlavorGroupService.updateGroup()` method
  
 ### HotFix 1
- Issue: Importing a Markdown as Flavor throws an exception, missing category now set to GENERAL on markdown 
  documents not including YAML front matter definition.
  

### Technical Details
- New Flyway migration: `V14__javadoc_version_filter.sql`
- New Settings entity fields: `javadocSyncSnapshot`, `javadocSyncRc`, `javadocSyncMilestone`
- New SettingsService methods: `shouldSyncJavadocVersion()`, `updateJavadocSyncFilters()`
- JavadocSyncService now filters versions using `filterVersionsBySettings()` before crawling
- New `LoginController` replaces view controller for `/login` (enables model attribute injection)

---

## [1.4.2] - 2025-12-08 + HotFix 1 + HotFix 2

### Added
- **Javadoc API Documentation**: Comprehensive Javadoc crawler, storage, and search for Spring projects
    - **Automated Crawling**: Discovers and parses Javadoc HTML from docs.spring.io
    - **Structured Storage**: Packages, classes, methods, fields, constructors stored in PostgreSQL
    - **Full-Text Search**: PostgreSQL tsvector/GIN indexes for efficient search across all content
    - **Version Awareness**: Track multiple versions per library with latest version resolution
    - **Per-Project Toggle**: Enable/disable sync for each Spring project on the projects list
    - **Project Detail Integration**: View synced Javadocs directly from the project detail page (`/projects/{id}`)
    - **Local Javadoc Viewer**: Browse packages, classes, methods, and fields at `/javadoc/view/{library}/{version}/`
    - **Sync Page Integration**: New Phase 9 card for manual Javadoc synchronization
    - Database schema: 6 new tables (`javadoc_sync_status`, `javadoc_packages`, `javadoc_classes`, `javadoc_methods`, `javadoc_fields`, `javadoc_constructors`)
    - 4 new MCP tools for AI assistants:
        - `getClassDoc`: Get full class documentation including methods, fields, constructors
        - `getPackageDoc`: Get package documentation with list of classes/interfaces
        - `searchJavadocs`: Full-text search across all Javadoc content
        - `listJavadocLibraries`: List all libraries with available versions
    - Configurable via `mcp.features.javadocs.enabled` (default: true)
    - Services: `JavadocFetcherService`, `JavadocParserService`, `JavadocCrawlService`, `JavadocStorageService`, `JavadocVersionService`

### Changed
- Total MCP tools increased from 39 to **43**

### Fixed
- Fix 1 - Scheduler HotFix, removed daily scheduler 1am
- Fix 2 - Security Filter failure for Flavor Groups 

---

## [1.4.1] - 2025-12-07

### Fixed
- **GitHub Docs Keyword Matching**: Fixed false positive matches in topic-based GitHub documentation lookup
    - Keywords like "AI" no longer incorrectly match substrings in words like "Container", "Testcontainers", "Email"
    - Implemented word boundary regex matching (`\b`) for accurate keyword detection
    - Spring AI now correctly shows 0 GitHub docs instead of 4 unrelated docs
- **Documentation Version Links**: Fixed placeholder versions (e.g., "1.0.x") displaying instead of actual versions
    - Documentation links now correctly show actual GA versions (e.g., "1.1.1")
    - URLs now point to correct version-specific documentation

### Added
- **Sync Feature Configuration**: New configuration option to show/hide maintenance utilities
    - `mcp.features.sync.fix-versions.enabled` property (default: `false`)
    - "Fix Documentation Versions" button on sync page now hidden by default
    - Can be enabled via environment variable `SYNC_FIX_VERSIONS_ENABLED=true`
    - Useful for one-time cleanup of legacy placeholder version links

---

## [1.4.0] - 2025-12-06

### Added
- **Boot Initializr Integration**: Direct integration with [start.spring.io](https://start.spring.io) for project generation
    - **Two-Tab UI**: Project configuration and dependency selection in organized tabs
    - **Live Dependency Search**: Real-time search across all Spring Boot starters
    - **Build File Preview**: Preview generated pom.xml or build.gradle before download
    - **Version Selection**: Choose from stable, RC, and snapshot Spring Boot versions
    - **Caffeine Caching**: High-performance caching with configurable TTL for metadata
    - Web UI at `/initializr` with Generate and Explore buttons
    - Settings integration showing Initializr status and MCP tools
    - 5 new MCP tools for AI assistants:
        - `initializrGetDependency`: Get dependency with Maven/Gradle snippet
        - `initializrSearchDependencies`: Search dependencies by name/description
        - `initializrCheckCompatibility`: Check dependency version compatibility
        - `initializrGetBootVersions`: List available Spring Boot versions
        - `initializrGetDependencyCategories`: Browse dependencies by category
    - Configurable via `mcp.features.initializr.enabled` (default: true)
- **Caffeine Cache Architecture**: Introduced Caffeine as the caching solution for the project
    - High-performance, near-optimal caching using Window TinyLfu eviction policy
    - Configurable TTL per cache (metadata: 60m, dependencies: 30m)
    - Cache statistics available via management endpoints
    - Automatic warm-up at application startup

### Changed
- Total MCP tools increased from 34 to **39**

---

## [1.3.4] - 2025-12-05

### Changed
- **Spring AI**: Updated from 1.1.0 to 1.1.1
- **Library Updates**: Security patches to address CVE-2025-48924

---

## [1.3.3] - 2025-12-04

### Added
- **Flavor Groups**: Team-based organization and access control for Flavors
    - **Public Groups**: Groups without members are visible to all authenticated API keys
    - **Private Groups**: Groups with API key members restrict visibility to members only
    - **Active/Inactive Status**: Inactive groups are completely hidden from MCP tools
    - Web UI for group management (`/groups`) with create, edit, delete, and member assignment
    - Assign flavors to multiple groups via the Flavors edit form
    - Filter flavors by group on the Flavors list page
    - API key membership management through group edit interface
    - 3 new MCP tools for AI assistants:
        - `listFlavorGroups`: List accessible groups (public + private where API key is member)
        - `getFlavorsGroup`: Get all flavors in a specific group with metadata
        - `getFlavorGroupStatistics`: Get group counts (total, active, public, private)
    - Security model extracts API key ID from security context for access control
    - Database tables: `flavor_groups`, `group_api_key_members`, `group_user_members`, `group_flavors`

### Changed
- **Documentation Restructure**: Reorganized README.md for better feature discovery
    - Feature-focused sections with inline screenshots per feature
    - New "Using with Claude Code" section with natural language query examples
    - Technical reference moved to ADDITIONAL_CONTENT.md
- Total MCP tools increased from 31 to **34**

### Fixed - Hotfix 1
- **Flavors Search Filter**: Fixed search not filtering flavor groups
    - Search term now correctly filters groups to only show those containing matching flavors
    - Flavors within groups are filtered to only display matches
    - Group badge shows "X matching" instead of total count when filters are active
    - Category and status filters also now work correctly for grouped flavors

## [1.3.2] - 2025-12-02

### Added
- **Enhanced Flavors Import/Export**: YAML front matter metadata header support
    - **Import**: Automatic parsing of YAML header metadata (unique-name, display-name, category, pattern-name, description, tags)
    - **Export**: Modal dialog with toggle to include/exclude metadata header (default: enabled)
    - Kebab-case field naming convention in YAML headers for cross-tool compatibility
    - Auto-rename with warning message for duplicate unique names on import
    - Category validation supports both enum names and display names (e.g., "ARCHITECTURE" or "Architecture")
    - Added new API Versioning Example in examples/basic to demonstrate new API Versioning in Spring Boot 4.0

### Changed
- **Spring AI MCP Refactoring**: Migrated to Spring AI 1.1.0 annotation-based MCP server
    - Replaced `@Tool`/`@ToolParam` with `@McpTool`/`@McpToolParam` annotations
    - New annotation package: `org.springaicommunity.mcp.annotation` (Spring AI Community)
    - Automatic tool discovery via `McpServerAnnotationScannerAutoConfiguration`
    - Removed manual `MethodToolCallbackProvider` registration from `McpConfig.java`
    - Simplified configuration - tools are now auto-registered as Spring beans

## [1.3.1] - 2025-12-01

### Added
- **GitHub Documentation Scanner**: New documentation and code example scanner using GitHub sources from spring-projects organization
    - Fetches AsciiDoc documentation directly from Spring project repositories
    - Supports version-specific documentation paths with automatic tag resolution
    - Configurable documentation paths per project with version threshold overrides
    - Processes `.adoc` files with full AsciiDoc-to-HTML conversion
- **Extended Documentation Page**: Cascaded documentation view combining spring.io docs and GitHub source docs
    - Two-tier documentation structure: spring.io reference links + GitHub source documentation
    - Expandable content sections with rendered AsciiDoc/Markdown
    - Full-text search across both documentation sources
    - Version-aware documentation browsing
- **Extended Code Examples Page**: Enhanced with topic grouping and improved code viewing
    - Topic/category grouping for better organization
    - Syntax highlighting with highlight.js (Atom One Dark theme)
    - Code view modal with copy-to-clipboard functionality
    - Preserved line breaks and formatting in code snippets

### Changed
- **UI Enhancements**: Added Kotlin "K" gradient icon in Dashboard Language Evolution section
- **UI Enhancements**: Added Java coffee cup icon and Kotlin "K" icon on Languages page statistics cards

## [1.3.0] - 2025-11-30

### Added
- **Flavors Feature**: New optional feature for managing company-specific guidelines and configurations
    - Support for 5 categories: Architecture, Compliance, Agents, Initialization, General
    - Markdown-based content with full-text search using PostgreSQL tsvector
    - Import/Export functionality for sharing flavors between teams
    - Create flavors from scratch or import from markdown files
    - Dedicated UI with category filtering and search
    - 8 new MCP tools for AI assistants to query flavor data
    - Dashboard integration showing flavor statistics by category
    - Configurable via `mcp.features.flavors.enabled` (default: true)

### Fixed
- Fixed login page CSS loading issue (added /vendor/** to security permitAll)
- Fixed HTTP Basic Auth modal popup on login page

## [1.2.0] - 2025-11-29

### Added
- **Language Evolution Tracking**: New optional feature for tracking Java (8+) and Kotlin (1.6+) language evolution
    - Track new features, deprecations, removals, and preview features for each language version
    - JEP (Java Enhancement Proposal) and KEP (Kotlin Enhancement Proposal) tracking
    - Code pattern examples showing old vs new idioms (e.g., pre-records vs records)
    - Spring Boot language version requirements mapping
    - Version comparison to see what changed between versions
    - Dedicated UI page with filters by language, version, status, and category
    - 6 new MCP tools for AI assistants to query language evolution data
    - Configurable scheduler (DAILY/WEEKLY/MONTHLY) for language data sync
    - Configurable via `mcp.features.language-evolution.enabled` (default: true)

### Changed
- Updated Java from 21 to 25 (LTS)
- Total MCP tools increased to 23

## [1.1.0] - 2025-11-28

### Added
- **OpenRewrite Migration Recipes**: New optional feature providing migration knowledge for Spring ecosystem upgrades
    - Dynamic recipe generation based on Spring projects in the database
    - Covers all 55+ Spring projects with version upgrade paths
    - Includes transformations for dependencies, imports, properties, and annotations
    - Dedicated UI for browsing recipes with dark mode styling
    - Configurable via `mcp.features.openrewrite.enabled` (default: true)

### Changed
- **UI Enhancements**: Dark mode styling for recipe details page
- Total MCP tools increased to 17

## [1.0.2] - 2025-11-27

### Changed
- **Spring Boot**: Bumped from 3.5.7 to 3.5.8
- **Spring AI**: Upgraded from 1.0.3 to 1.1.0

### Added
- **Example App**: Added first 100% AI-generated example application (`examples/todo-app-example/`)
    - Multi-user Todo app with Spring Boot 4.0.0
    - Spring Security authentication
    - PostgreSQL database with Flyway migrations
    - Custom Actuator endpoints
    - Dark-themed Thymeleaf UI
    - Built entirely using this MCP server for Spring documentation lookup

## [1.0.1] - 2025-11-26

### Added
- Initial public release
- 10 MCP tools for Spring documentation access
- Full-text search with PostgreSQL
- Web management UI
- API Key authentication

---

## Version Summary

| Version | Date | Highlights |
|---------|------|------------|
| 1.8.0 | 2026-01-09 | MCP Streamable-HTTP Transport (replaces SSE, protocol 2025-11-25) |
| 1.7.1 | 2026-01-08 | Sync fixes (OSS version support, javadoc parsing, logging) |
| 1.7.0 | 2026-01-06 | Spring Boot Wiki Integration (Release Notes & Migration Guides) |
| 1.6.3 | 2026-01-05 | MCP Tool Response Size Optimization, isCurrent flag fix |
| 1.6.2 | 2026-01-03 | MCP Tool Masquerading - Dynamic tool visibility & descriptions |
| 1.6.1 | 2026-01-02 | Virtual Threads support, Spring-managed async operations |
| 1.6.0 | 2026-01-01 | Semantic embeddings with pgvector (Ollama/OpenAI providers) |
| 1.5.4 | 2025-12-25 | Collapsible sidebar menu, SNAPSHOT → GA version sync fix |
| 1.5.3 | 2025-12-19 | User display name, Spring Boot 3.5.9 |
| 1.5.2 | 2025-12-17 | JEP/KEP detail pages, synthesized code examples, dark theme fixes |
| 1.5.1 | 2025-12-17 | Javadoc MCP tools transaction rollback fix |
| 1.5.0 | 2025-12-16 | MCP Monitoring Dashboard, GitHub sync fixes and performance improvements |
| 1.4.3 | 2025-12-12 | Javadoc sync version filter, login version display, flavor groups fix |
| 1.4.2 | 2025-12-08 | Javadoc API Documentation feature (4 MCP tools) |
| 1.4.1 | 2025-12-07 | GitHub docs keyword fix, configurable sync features |
| 1.4.0 | 2025-12-06 | Boot Initializr integration, Caffeine caching (5 MCP tools) |
| 1.3.4 | 2025-12-05 | Spring AI 1.1.1, CVE-2025-48924 security fix |
| 1.3.3 | 2025-12-04 | Flavor Groups with team-based access control (3 MCP tools) |
| 1.3.2 | 2025-12-02 | YAML metadata headers for Flavors, Spring AI 1.1.0 MCP refactoring |
| 1.3.1 | 2025-12-01 | GitHub documentation scanner, enhanced code examples |
| 1.3.0 | 2025-11-30 | Flavors feature (8 MCP tools) |
| 1.2.0 | 2025-11-29 | Language Evolution tracking (6 MCP tools) |
| 1.1.0 | 2025-11-28 | OpenRewrite migration recipes (7 MCP tools) |
| 1.0.2 | 2025-11-27 | Spring Boot 3.5.8, example app |
| 1.0.1 | 2025-11-26 | Initial release (10 MCP tools) |

## MCP Tools Evolution

- **v1.0.1**: 10 documentation tools
- **v1.1.0**: +7 migration tools = 17 total
- **v1.2.0**: +6 language evolution tools = 23 total
- **v1.3.0**: +8 flavors tools = 31 total
- **v1.3.3**: +3 flavor groups tools = 34 total
- **v1.4.0**: +5 initializr tools = 39 total
- **v1.4.2**: +4 javadoc tools = 43 total
- **v1.5.2**: +1 language feature example tool = 44 total
- **v1.7.0**: +2 wiki tools = **46 total**

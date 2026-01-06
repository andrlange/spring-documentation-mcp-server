# Spring Boot Wiki Integration - Implementation Plan

> **Target Version**: 1.7.0
> **Feature**: Spring Boot Wiki Integration (Release Notes & Migration Guides)
> **Status**: Ready for Implementation

## Overview

Integrate the Spring Boot GitHub Wiki (https://github.com/spring-projects/spring-boot/wiki) into the MCP Server, providing searchable Release Notes and Migration Guides with semantic search via embeddings.

## Key Components

### 1. Database (V26__wiki_integration.sql)
- `wiki_release_notes` - Store release notes with version metadata and embeddings
- `wiki_migration_guides` - Store migration guides with source/target versions and embeddings

### 2. New MCP Tools (2 tools in Documentation group)
- `getWikiReleaseNotes` - Get release notes for a Spring Boot version
- `getWikiMigrationGuide` - Get migration guide between versions

### 3. UI
- New "Wiki" menu entry below Spring Boot (bi-journal-text icon, green #10b981)
- Wiki list page with search bar, Release Notes section, Migration Guides section
- Detail page with readonly Markdown view

### 4. Sync Integration
- Phase 10 in comprehensive sync process
- Manual sync button on /sync page
- AsciiDoc to Markdown conversion

### 5. Embeddings Integration
- Integrate with EmbeddingSyncService pattern
- Add wiki stats to /embeddings page

---

## Implementation Phases

### Phase 1: Database & Entities
**Files to Create:**
- `src/main/resources/db/migration/V26__wiki_integration.sql`
- `src/main/java/com/spring/mcp/model/entity/WikiReleaseNotes.java`
- `src/main/java/com/spring/mcp/model/entity/WikiMigrationGuide.java`
- `src/main/java/com/spring/mcp/repository/WikiReleaseNotesRepository.java`
- `src/main/java/com/spring/mcp/repository/WikiMigrationGuideRepository.java`

**Tasks:**
- [ ] Create V26 migration with both tables, TSVECTOR, embedding columns, indexes
- [ ] Create entity classes following Flavor pattern
- [ ] Create repository interfaces with custom queries

### Phase 2: Service Layer
**Files to Create:**
- `src/main/java/com/spring/mcp/service/wiki/AsciiDocConverter.java`
- `src/main/java/com/spring/mcp/service/wiki/WikiSyncService.java`
- `src/main/java/com/spring/mcp/service/wiki/WikiService.java`

**Dependencies to Add (build.gradle):**
```groovy
// AsciidoctorJ for AsciiDoc to HTML conversion (self-contained, no CLI dependency)
implementation 'org.asciidoctor:asciidoctorj:3.0.0'
```

**Note:** User confirmed AsciidoctorJ library approach (self-contained, ~15MB).

**Tasks:**
- [ ] Implement AsciiDocConverter (AsciiDoc -> HTML -> Markdown)
- [ ] Implement WikiSyncService (git clone, file parsing, conversion)
- [ ] Implement WikiService (CRUD, search, stats)
- [ ] Add unit tests (>80% coverage)

### Phase 3: MCP Tools
**Files to Create:**
- `src/main/java/com/spring/mcp/service/tools/WikiMcpTools.java`
- `src/main/java/com/spring/mcp/model/dto/mcp/WikiReleaseNotesResponse.java`
- `src/main/java/com/spring/mcp/model/dto/mcp/WikiMigrationGuideResponse.java`

**Files to Modify:**
- `src/main/java/com/spring/mcp/model/enums/McpToolGroup.java` - Update DOCUMENTATION count 10->12
- `src/main/java/com/spring/mcp/config/McpToolsInitializer.java` - Add 2 new tool definitions

**Tasks:**
- [ ] Create WikiMcpTools with @McpTool annotations
- [ ] Create response DTOs
- [ ] Update McpToolGroup.DOCUMENTATION expected count
- [ ] Add tool definitions to McpToolsInitializer
- [ ] Add unit tests

### Phase 4: UI
**Files to Create:**
- `src/main/resources/templates/wiki/index.html`
- `src/main/resources/templates/wiki/view.html`
- `src/main/java/com/spring/mcp/controller/web/WikiController.java`

**Files to Modify:**
- `src/main/resources/templates/fragments/sidebar.html` - Add Wiki menu entry

**Tasks:**
- [ ] Add sidebar menu entry below Spring Boot
- [ ] Create wiki list page with search, two sections
- [ ] Create wiki detail page with Markdown rendering
- [ ] Create WikiController with routes
- [ ] Add controller tests

### Phase 5: Sync Integration
**Files to Modify:**
- `src/main/java/com/spring/mcp/service/sync/ComprehensiveSyncService.java` - Add Phase 10
- `src/main/java/com/spring/mcp/controller/web/SyncController.java` - Add /sync/wiki endpoint
- `src/main/resources/templates/sync/index.html` - Add Phase 10 card

**Tasks:**
- [ ] Add Phase 10 to comprehensive sync
- [ ] Add manual sync button on /sync page
- [ ] Update sync progress modal for 11 phases
- [ ] Add wiki sync endpoint

### Phase 6: Embeddings Integration
**Files to Modify:**
- `src/main/java/com/spring/mcp/service/embedding/EmbeddingSyncService.java` - Add wiki methods
- `src/main/java/com/spring/mcp/service/embedding/HybridSearchService.java` - Add wiki search
- `src/main/resources/templates/embeddings/index.html` - Add wiki stats cards

**Tasks:**
- [ ] Add syncWikiReleaseNotesEmbeddings() method
- [ ] Add syncWikiMigrationGuideEmbeddings() method
- [ ] Add wiki hybrid search methods
- [ ] Update embeddings page with wiki coverage stats
- [ ] Add integration tests

### Phase 7: Documentation & Release
**Files to Modify:**
- `build.gradle` - version = '1.7.0'
- `CLAUDE.md` - Add wiki tools, update version
- `README.md` - Add wiki feature section
- `CHANGELOG.md` - Add 1.7.0 release entry
- `VERSIONS.md` - Add 1.7.0 entry
- `application.yml` - Update version references

**Tasks:**
- [ ] Bump version to 1.7.0 in all locations per VERSIONS.md
- [ ] Update CLAUDE.md with new tools (46 total)
- [ ] Add wiki chapter to README.md
- [ ] Add 1.7.0 changelog entry
- [ ] Create capabilities/WIKI_INTEGRATION.md evaluation document

---

## Critical Files Reference

| Purpose | File Path |
|---------|-----------|
| Embedding pattern | `src/main/java/com/spring/mcp/service/embedding/EmbeddingSyncService.java` |
| Sync pattern | `src/main/java/com/spring/mcp/service/sync/ComprehensiveSyncService.java` |
| MCP tool pattern | `src/main/java/com/spring/mcp/service/tools/SpringDocumentationTools.java` |
| UI list pattern | `src/main/resources/templates/flavors/list.html` |
| UI detail pattern | `src/main/resources/templates/flavors/view.html` |
| Tool init pattern | `src/main/java/com/spring/mcp/config/McpToolsInitializer.java` |
| Migration pattern | `src/main/resources/db/migration/V21__pgvector_extension.sql` |

---

## Test Coverage Requirements

- Overall: >80% line coverage
- Service layer: >90%
- Repository layer: >85%
- All existing tests must pass

---

## Ralph Wiggum Loop Prompt

```
/ralph-wiggum:ralph-loop

Execute the Spring Boot Wiki Integration implementation plan from the planning document.

## Implementation Phases (Execute Sequentially)

### Phase 1: Database & Entities
- Create V26__wiki_integration.sql with wiki_release_notes and wiki_migration_guides tables
- Create WikiReleaseNotes and WikiMigrationGuide entities
- Create repository interfaces with custom queries
- Mark phase as DONE when migration runs successfully

### Phase 2: Service Layer
- Add asciidoctorj:3.0.0 dependency to build.gradle
- Create AsciiDocConverter service
- Create WikiSyncService for git clone and parsing
- Create WikiService for CRUD and search
- Write unit tests (>80% coverage)
- Mark phase as DONE when tests pass

### Phase 3: MCP Tools
- Create WikiMcpTools with getWikiReleaseNotes and getWikiMigrationGuide
- Create response DTOs
- Update McpToolGroup.DOCUMENTATION count to 12
- Add tool definitions to McpToolsInitializer
- Write unit tests
- Mark phase as DONE when tools are registered

### Phase 4: UI
- Add Wiki menu entry to sidebar.html (below Spring Boot, bi-journal-text icon)
- Create wiki/index.html with search bar and two sections
- Create wiki/view.html with Markdown rendering
- Create WikiController with all routes
- Mark phase as DONE when pages render correctly

### Phase 5: Sync Integration
- Add Phase 10 to ComprehensiveSyncService
- Add /sync/wiki endpoint to SyncController
- Update sync page UI with Phase 10 card
- Mark phase as DONE when sync triggers work

### Phase 6: Embeddings Integration
- Add wiki embedding methods to EmbeddingSyncService
- Add wiki search to HybridSearchService
- Update embeddings page with wiki coverage stats
- Write integration tests
- Mark phase as DONE when embeddings generate

### Phase 7: Documentation & Release
- Bump version to 1.7.0 in: build.gradle, application.yml, docker-compose-all.yaml, build-container.sh
- Update CLAUDE.md with wiki tools (46 total)
- Add wiki chapter to README.md
- Add 1.7.0 entry to CHANGELOG.md and VERSIONS.md
- Create capabilities/WIKI_INTEGRATION.md
- Run ./gradlew build to verify
- Mark phase as DONE when build succeeds

## Completion Criteria
Print "DONE" when ALL phases complete and:
1. All 7 phases marked as DONE
2. ./gradlew build succeeds
3. Application starts without errors
4. Wiki page accessible at /wiki
5. MCP tools registered (46 total)
6. Version is 1.7.0

## Important Notes
- The Spring MCP Server runs on port 8080 - do NOT kill it
- Use existing patterns from referenced files
- Follow the evaluation document format from FLAVORS_EVALUATION.md
- Mark each phase DONE in your progress tracking
```

**Completion Promise**: DONE
**Max Iterations**: 10

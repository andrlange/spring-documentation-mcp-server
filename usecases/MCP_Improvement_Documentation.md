# MCP Documentation Tools Improvement Report

**Version:** 1.8.1
**Date:** 2026-01-12
**Tool Group:** Documentation (12 tools)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Tool Analysis](#tool-analysis)
3. [Improvements Implemented](#improvements-implemented)
4. [Technical Details](#technical-details)
5. [Testing Framework](#testing-framework)
6. [Migration Guide](#migration-guide)

---

## Executive Summary

This document describes the analysis and improvements made to the **Documentation MCP Tools** group, consisting of 12 tools that provide access to Spring ecosystem documentation, versions, and code examples.

### Key Improvements

| Improvement | Tool(s) Affected | Benefit |
|-------------|------------------|---------|
| Optional parameters with smart defaults | `getLatestSpringBootVersion` | Returns latest GA+Current versions without requiring parameters |
| Semantic search via embeddings | `findProjectsByUseCase`, `getCodeExamples` | Find related content by meaning, not just keywords |
| New embedding entity type | All embedding-enabled tools | Spring Projects now have semantic embeddings |
| Comprehensive testing framework | All 12 tools | Automated testing for App Modernization and App Seeding use cases |

---

## Tool Analysis

### Documentation Tools Overview (12 Tools)

| # | Tool Name | Purpose | Embedding Support | Status |
|---|-----------|---------|-------------------|--------|
| 1 | `listSpringProjects` | List all Spring ecosystem projects | N/A | OK |
| 2 | `getSpringVersions` | Get versions for a specific project | N/A | OK |
| 3 | `listSpringBootVersions` | List all Spring Boot versions | N/A | OK |
| 4 | `filterSpringBootVersionsBySupport` | Filter versions by support status | N/A | OK |
| 5 | `searchSpringDocs` | Search documentation content | **Yes (existing)** | OK |
| 6 | `getCodeExamples` | Search code examples | **Yes (added)** | **Improved** |
| 7 | `findProjectsByUseCase` | Find projects by use case keyword | **Yes (added)** | **Improved** |
| 8 | `getDocumentationByVersion` | Get docs for specific version | N/A | OK |
| 9 | `getLatestSpringBootVersion` | Get latest patch version | N/A | **Improved** |
| 10 | `listProjectsBySpringBootVersion` | List compatible projects | N/A | OK |
| 11 | `getWikiReleaseNotes` | Get Spring Boot release notes | N/A | OK |
| 12 | `getWikiMigrationGuide` | Get migration guide between versions | N/A | OK |

### Analysis Findings

#### Tools Requiring No Changes
- `listSpringProjects` - Simple list, no search needed
- `getSpringVersions` - Lookup by project slug
- `listSpringBootVersions` - Already ordered by version descending
- `filterSpringBootVersionsBySupport` - Filter operation
- `searchSpringDocs` - Already has hybrid search (RRF algorithm)
- `getDocumentationByVersion` - Lookup by project + version
- `listProjectsBySpringBootVersion` - Lookup by version
- `getWikiReleaseNotes` - Lookup by version
- `getWikiMigrationGuide` - Lookup by from/to versions

#### Tools Identified for Improvement

1. **`getLatestSpringBootVersion`**
   - **Issue:** Required both `majorVersion` and `minorVersion` parameters
   - **Problem:** Users wanting "latest Spring Boot versions" had to know the version numbers
   - **Solution:** Made parameters optional; returns latest GA+Current versions by default

2. **`findProjectsByUseCase`**
   - **Issue:** Used simple string matching (`contains()`)
   - **Problem:** "database" wouldn't find "Spring Data JPA" or "persistence"
   - **Solution:** Added semantic search using project embeddings

3. **`getCodeExamples`**
   - **Issue:** Not using existing `HybridSearchService.searchCodeExamples()`
   - **Problem:** Missed semantic matches for code examples
   - **Solution:** Integrated hybrid search when only query is provided

---

## Improvements Implemented

### 1. `getLatestSpringBootVersion` Enhancement

**Location:** `SpringDocumentationTools.java:424-513`

#### Before
```java
@McpTool(description = "Get the latest patch version for a specific Spring Boot major.minor version.")
public LatestSpringBootVersionResponse getLatestSpringBootVersion(
    @McpToolParam(description = "Major version number (required)") Integer majorVersion,
    @McpToolParam(description = "Minor version number (required)") Integer minorVersion)
```

#### After
```java
@McpTool(description = """
    Get the latest patch version for a specific Spring Boot major.minor version.
    When called without majorVersion/minorVersion parameters, returns the latest GA versions
    that are currently supported (isCurrent=true), ordered by version descending (newest first).
    """)
public LatestSpringBootVersionResponse getLatestSpringBootVersion(
    @McpToolParam(description = "Major version number (optional). If omitted, returns latest GA+Current versions.") Integer majorVersion,
    @McpToolParam(description = "Minor version number (optional). Required if majorVersion is provided.") Integer minorVersion)
```

#### Usage Examples

**Get all current GA versions (new default behavior):**
```json
{
  "name": "getLatestSpringBootVersion",
  "arguments": {}
}
// Returns: [4.0.1, 3.5.9, 3.4.x, ...] - newest first
```

**Get latest patch for specific minor version (existing behavior):**
```json
{
  "name": "getLatestSpringBootVersion",
  "arguments": {"majorVersion": 4, "minorVersion": 0}
}
// Returns: 4.0.1 (latest 4.0.x patch)
```

---

### 2. `findProjectsByUseCase` Semantic Search

**Location:** `SpringDocumentationTools.java:712-786`

#### New Behavior
When embeddings are enabled, the tool uses **Reciprocal Rank Fusion (RRF)** to combine:
- Keyword matching on project name and description
- Semantic similarity using project embeddings

#### Example Semantic Matches

| Query | Keyword Match | Semantic Match (with embeddings) |
|-------|---------------|----------------------------------|
| "database" | Spring Data | Spring Data, Spring Data JPA, Spring Data JDBC, Spring Data R2DBC |
| "security" | Spring Security | Spring Security, Spring Authorization Server, Spring Session |
| "messaging" | Spring AMQP | Spring AMQP, Spring Kafka, Spring Integration, Spring Cloud Stream |
| "REST API" | - | Spring Web, Spring HATEOAS, Spring REST Docs |

#### Database Changes

**New Migration:** `V27__spring_projects_embedding.sql`
```sql
ALTER TABLE spring_projects
ADD COLUMN IF NOT EXISTS project_embedding vector(768);

ALTER TABLE spring_projects
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);

ALTER TABLE spring_projects
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;
```

---

### 3. `getCodeExamples` Hybrid Search

**Location:** `SpringDocumentationTools.java:282-385`

#### New Behavior
When embeddings are enabled and **only a query is provided** (no project/version/language filters), the tool uses hybrid search.

```java
boolean useHybridSearch = embeddingProperties.isEnabled()
    && hybridSearchService != null
    && query != null && !query.isBlank()
    && project == null
    && version == null
    && language == null;
```

#### Example
```json
{
  "name": "getCodeExamples",
  "arguments": {"query": "database connection pooling"}
}
// Finds: HikariCP config, DataSource setup, Connection pool examples
// Even if they don't contain the exact words "database connection pooling"
```

---

### 4. Embeddings Dashboard Update

**Location:** `templates/embeddings/index.html`

Added a new "Projects" card to the embeddings dashboard showing:
- Number of projects with embeddings
- Total projects
- Coverage percentage
- Progress bar

---

## Technical Details

### Files Modified

| File | Type | Changes |
|------|------|---------|
| `V27__spring_projects_embedding.sql` | Migration | New - adds embedding columns to `spring_projects` |
| `SpringProject.java` | Entity | Added `embeddingModel`, `embeddedAt` fields |
| `EmbeddingSyncService.java` | Service | Added PROJECT entity type support, `syncProjectEmbeddings()` |
| `HybridSearchService.java` | Service | Added `searchProjects()` method |
| `SpringDocumentationTools.java` | MCP Tools | Improved 3 tools with optional params and hybrid search |
| `embeddings/index.html` | Template | Added Projects embedding stats card |

### Embedding Sync Flow

```
1. Sync triggered (manual or scheduled)
   ↓
2. EmbeddingSyncService.syncProjectEmbeddings()
   ↓
3. For each project:
   - Extract text: name + description
   - Generate embedding via Ollama (nomic-embed-text, 768 dimensions)
   - Store in spring_projects.project_embedding
   ↓
4. HybridSearchService.searchProjects() now uses:
   - Keyword search (TSVECTOR)
   - Semantic search (pgvector cosine similarity)
   - RRF to combine results
```

### Hybrid Search Algorithm (RRF)

The Reciprocal Rank Fusion algorithm combines keyword and semantic search results:

```
RRF_score = Σ (1 / (k + rank_i))

Where:
- k = 60 (constant to prevent division by zero issues)
- rank_i = rank of document in result set i
```

Configuration (from `application.yml`):
```yaml
mcp:
  embedding:
    hybrid:
      alpha: 0.3          # Keyword weight (30%)
      min-similarity: 0.5 # Minimum cosine similarity threshold
```

---

## Testing Framework

### Python Testing Tool

**Location:** `usecases/test_documentation_tools.py`

#### Installation

```bash
cd usecases
pip install -r requirements.txt
```

#### Usage

```bash
python test_documentation_tools.py \
    --base-url http://localhost:8080 \
    --api-key YOUR_API_KEY \
    --output Documentation.md
```

#### Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `--base-url` | `http://localhost:8080` | MCP server base URL |
| `--api-key` | (required) | API key for authentication |
| `--output` | `usecases/Documentation.md` | Output file path |

### Test Cases

The testing tool covers two primary use cases:

#### Use Case 1: App Modernization (Spring Boot 2.x/3.x → 4.x)

| Tool | Tests | Focus |
|------|-------|-------|
| `listSpringProjects` | 1 | Ecosystem overview |
| `getSpringVersions` | 2 | Version compatibility |
| `listSpringBootVersions` | 1 | Upgrade path |
| `filterSpringBootVersionsBySupport` | 1 | Supported targets |
| `searchSpringDocs` | 3 | Migration docs, breaking changes |
| `getCodeExamples` | 2 | Migration examples |
| `findProjectsByUseCase` | 1 | Migration-related projects |
| `getDocumentationByVersion` | 1 | Target version docs |
| `getLatestSpringBootVersion` | 3 | Latest versions (new default!) |
| `listProjectsBySpringBootVersion` | 1 | Compatible dependencies |
| `getWikiReleaseNotes` | 2 | Version features |
| `getWikiMigrationGuide` | 2 | Official migration guides |

#### Use Case 2: App Seeding (New Application)

| Tool | Tests | Focus |
|------|-------|-------|
| `listSpringProjects` | 1 | Available projects |
| `getSpringVersions` | 3 | Project versions |
| `listSpringBootVersions` | 1 | Version selection |
| `filterSpringBootVersionsBySupport` | 1 | Supported versions |
| `searchSpringDocs` | 3 | Getting started, tutorials |
| `getCodeExamples` | 3 | REST, JPA, Security examples |
| `findProjectsByUseCase` | 4 | Web, database, security, messaging |
| `getDocumentationByVersion` | 2 | Version-specific docs |
| `getLatestSpringBootVersion` | 2 | Latest available |
| `listProjectsBySpringBootVersion` | 2 | Compatible dependencies |
| `getWikiReleaseNotes` | 1 | New features |
| `getWikiMigrationGuide` | 1 | Version differences |

### Output Format

The test results are written to a Markdown file with:
- Test parameters
- Response snippets (truncated at 2000 chars)
- Summary table with test counts per tool

---

## Migration Guide

### Upgrading to Use These Improvements

#### 1. Run Database Migration

The new migration `V27__spring_projects_embedding.sql` will run automatically on startup.

```bash
./gradlew bootRun
# Migration runs automatically via Flyway
```

#### 2. Generate Project Embeddings

Navigate to the Embeddings Dashboard and click "Sync Missing":

```
http://localhost:8080/embeddings
```

Or trigger via API:
```bash
curl -X POST http://localhost:8080/embeddings/sync \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 3. Verify Embedding Coverage

Check the Embeddings Dashboard for the new "Projects" card showing coverage.

#### 4. Test the Improvements

```bash
cd usecases
python test_documentation_tools.py --api-key YOUR_KEY
```

### Backward Compatibility

All improvements are **backward compatible**:

| Tool | Old Behavior | New Behavior |
|------|--------------|--------------|
| `getLatestSpringBootVersion` | Required major/minor params | Optional params; defaults to GA+Current |
| `findProjectsByUseCase` | Keyword only | Keyword + semantic (when embeddings enabled) |
| `getCodeExamples` | Keyword only | Keyword + semantic (when embeddings enabled, no filters) |

When embeddings are disabled (`mcp.features.embeddings.enabled=false`), the tools fall back to keyword-only behavior.

---

## Appendix: Entity Types with Embeddings

| Entity Type | Table | Embedding Column | Used By |
|-------------|-------|------------------|---------|
| DOCUMENTATION | `documentation_content` | `content_embedding` | `searchSpringDocs` |
| TRANSFORMATION | `migration_transformations` | `transformation_embedding` | Migration tools |
| FLAVOR | `flavors` | `flavor_embedding` | Flavor tools |
| CODE_EXAMPLE | `code_examples` | `example_embedding` | `getCodeExamples` |
| WIKI_RELEASE_NOTES | `wiki_release_notes` | `content_embedding` | Wiki tools |
| WIKI_MIGRATION_GUIDE | `wiki_migration_guides` | `content_embedding` | Wiki tools |
| **PROJECT** (new) | `spring_projects` | `project_embedding` | `findProjectsByUseCase` |

---

*Document generated as part of MCP Tool Improvement Initiative v1.9.0*

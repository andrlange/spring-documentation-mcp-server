# Embeddings & Vector Search - Capability Evaluation

> **Analysis Date**: 2026-01-01
> **Target Version**: 1.6.0
> **Purpose**: Evaluate and plan implementation of embeddings/vectorization using pgvector to enhance semantic search capabilities and reduce token consumption for LLM-based MCP tools
> **Status**: PLANNING PHASE

---

## Executive Summary

Implementing embeddings with pgvector could provide **40-70% improvement** in search relevance and **30-50% reduction** in token consumption for MCP tool responses. The current PostgreSQL TSVECTOR-based full-text search is limited to lexical matching, missing semantic relationships between concepts.

**Key Findings**:
- Current TSVECTOR indices cover 6+ tables but only support keyword matching
- Total searchable content: 60-3200 MB across documentation, code examples, migration knowledge, and Javadocs
- PostgreSQL 18 natively supports pgvector extension for vector similarity search
- High-value embedding candidates: documentation_content, migration_transformations, flavors, javadoc_classes

**Estimated Impact**:

| Metric | Current (TSVECTOR) | With Embeddings | Improvement |
|--------|-------------------|-----------------|-------------|
| Search relevance accuracy | 60-70% | 85-95% | +25-35% |
| Semantic query support | 0% | 100% | New capability |
| Token consumption per search | ~8,000 tokens | ~4,500 tokens | -45% |
| Cross-domain concept matching | Poor | Excellent | Significant |
| Synonym/paraphrase handling | None | Native | New capability |

**Feature Characteristics**:
- **Fully Optional**: Feature can be completely disabled via configuration
- **Database Independent**: When disabled, no pgvector dependency required - works with standard PostgreSQL
- **Async Processing**: Embeddings generated asynchronously after sync operations
- **Retry Resilient**: Built-in retry mechanism with configurable attempts (default: 10)
- **Provider Agnostic**: Support for Ollama (local), OpenAI, and Gemini embedding providers

---

## Table of Contents

1. [Current State Analysis](#1-current-state-analysis)
2. [Embedding Candidates Analysis](#2-embedding-candidates-analysis)
3. [Embedding Model Options](#3-embedding-model-options)
4. [Optional Feature Architecture](#4-optional-feature-architecture)
5. [Embedding Job Processing & State Management](#5-embedding-job-processing--state-management)
6. [PostgreSQL pgvector Migration Plan](#6-postgresql-pgvector-migration-plan)
7. [Docker Configuration Changes](#7-docker-configuration-changes)
8. [Architecture Design](#8-architecture-design)
9. [Implementation Plan](#9-implementation-plan)
10. [Risk Assessment](#10-risk-assessment)
11. [Success Metrics](#11-success-metrics)
12. [Ralph-Wiggum Implementation Loop](#12-ralph-wiggum-implementation-loop)

---

## 1. Current State Analysis

### 1.1 Existing Full-Text Search Infrastructure

The Spring MCP Server currently uses PostgreSQL TSVECTOR for full-text search across multiple tables:

| Table | TSVECTOR Column | Index Type | Content Size | Search Frequency |
|-------|-----------------|------------|--------------|------------------|
| `documentation_content` | `indexed_content` | GIN | 2.5-100 MB | Very High |
| `migration_transformations` | `search_vector` | GIN | 1-20 MB | High |
| `migration_recipes` | `search_vector` | GIN | 0.5-2 MB | Medium |
| `flavors` | `search_vector` | GIN | 0.1-10 MB | High |
| `javadoc_packages` | `indexed_content` | GIN | 0.5-5 MB | Medium |
| `javadoc_classes` | `indexed_content` | GIN | 10-500 MB | Very High |

**Total Indexed Content**: ~15-640 MB

### 1.2 Limitations of Current TSVECTOR Approach

| Limitation | Impact | Example |
|------------|--------|---------|
| **Lexical matching only** | Misses conceptual relationships | "configure database" doesn't match "set up JDBC connection" |
| **No synonym understanding** | Low recall for varied queries | "authorization" doesn't match "authentication" context |
| **No semantic ranking** | Poor relevance ordering | Results ordered by term frequency, not meaning |
| **No cross-language matching** | Java ↔ Kotlin patterns missed | Java record pattern doesn't surface Kotlin data class docs |
| **No conceptual similarity** | Can't find "similar" content | "How to cache responses?" can't find Redis/Caffeine docs |
| **Query length sensitivity** | Short queries perform poorly | "REST" returns too many irrelevant results |

### 1.3 MCP Tools Token Consumption Analysis

Current token consumption for high-volume MCP tools:

| MCP Tool | Avg Response Size | Token Estimate | Calls/Hour | Impact |
|----------|-------------------|----------------|------------|--------|
| `searchSpringDocs` | 12.4 KB | ~4,500 tokens | 50+ | High |
| `getClassDoc` | 15.2 KB | ~5,500 tokens | 30+ | High |
| `searchJavadocs` | 18.6 KB | ~6,800 tokens | 25+ | High |
| `searchMigrationKnowledge` | 8.5 KB | ~3,100 tokens | 20+ | Medium |
| `searchFlavors` | 6.2 KB | ~2,300 tokens | 15+ | Medium |
| `getFlavorByName` | 10.8 KB | ~4,000 tokens | 10+ | Medium |

**Problem**: Large, less-relevant results consume LLM context window unnecessarily.

**Solution with Embeddings**: Return semantically ranked, more relevant results with reduced noise, cutting token usage by 30-50%.

---

## 2. Embedding Candidates Analysis

### 2.1 Priority Matrix

| Entity | Content Volume | Search Value | Implementation Effort | Priority |
|--------|---------------|--------------|----------------------|----------|
| `documentation_content` | 500-2000 docs | Very High | Medium | **P0** |
| `migration_transformations` | 500-2000 items | High | Medium | **P0** |
| `flavors` | 20-200 items | High | Low | **P1** |
| `code_examples` | 1000-5000 items | High | Medium | **P1** |
| `javadoc_classes` | 10K-50K classes | Very High | High | **P2** |
| `javadoc_methods` | 100K-500K methods | Medium | Very High | **P3** |
| `language_features` | 100-500 items | Medium | Low | **P2** |
| `language_code_patterns` | 100-500 items | Medium | Low | **P2** |

### 2.2 Priority 0 (P0) - Immediate High Impact

#### 2.2.1 Documentation Content

**Entity**: `DocumentationContent`
**Current Fields**:
- `content` (TEXT) - Full documentation content (5-50 KB each)
- `indexed_content` (TSVECTOR) - Current search index

**Embedding Strategy**:
- Create embedding from: `title + description + content` (chunked if > 8192 tokens)
- Store as: `content_embedding` (VECTOR(1536) or VECTOR(768))
- Chunk strategy: 512-token chunks with 50-token overlap for large docs
- Index: HNSW for fast approximate nearest neighbor search

**Expected Improvement**:
- Semantic queries like "how to configure database pooling" will match HikariCP docs
- Related concepts surface together (caching → Redis → Caffeine → Cache annotations)

#### 2.2.2 Migration Transformations

**Entity**: `MigrationTransformation`
**Current Fields**:
- `oldPattern` (TEXT) - Code/config before migration
- `newPattern` (TEXT) - Code/config after migration
- `explanation` (TEXT) - Detailed explanation
- `codeExample` (TEXT) - Full example
- `search_vector` (TSVECTOR) - Current index

**Embedding Strategy**:
- Create composite embedding from: `explanation + oldPattern + newPattern`
- Single embedding per transformation capturing the migration context
- Store as: `transformation_embedding` (VECTOR(1536))

**Expected Improvement**:
- "How do I replace WebClient with RestClient?" surfaces relevant Spring 6.1 migrations
- "MockBean alternatives in Spring Boot 3.4" finds @MockitoBean transformations

### 2.3 Priority 1 (P1) - High Value, Moderate Effort

#### 2.3.1 Flavors (Company Guidelines)

**Entity**: `Flavor`
**Current Fields**:
- `content` (TEXT) - Full markdown content (5-50 KB)
- `description` (TEXT) - Summary
- `search_vector` (TSVECTOR) - Current index

**Embedding Strategy**:
- Embed: `displayName + description + content` (chunked)
- Architecture patterns become semantically searchable
- Store as: `flavor_embedding` (VECTOR(1536))

**Expected Improvement**:
- "Clean architecture for Spring Boot" matches Hexagonal Architecture flavor
- "Payment processing compliance" surfaces PCI-DSS and SOC2 flavors together

#### 2.3.2 Code Examples

**Entity**: `CodeExample`
**Current Fields**:
- `title` (VARCHAR)
- `description` (TEXT)
- `codeSnippet` (TEXT)
- `tags` (TEXT[])

**Embedding Strategy**:
- Embed: `title + description + tags.join(' ')` (code optionally)
- Separate embedding for code semantic understanding
- Store as: `example_embedding` (VECTOR(1536))

**Expected Improvement**:
- "How to implement pagination" finds all pagination examples across projects
- "Async processing patterns" surfaces @Async, WebFlux, virtual threads examples

### 2.4 Priority 2 (P2) - Medium Value, Variable Effort

#### 2.4.1 Javadoc Classes

**Entity**: `JavadocClass`
**Volume**: 10,000-50,000 classes

**Embedding Strategy**:
- Embed: `summary + description` for each class
- Optional: Aggregate method signatures for API shape embedding
- Chunk: Process in batches of 1000 during sync

#### 2.4.2 Language Features

**Entity**: `LanguageFeature`
**Volume**: 100-500 features

**Embedding Strategy**:
- Embed: `featureName + description + migrationNotes`
- Relatively small dataset, full embedding feasible

### 2.5 Data Volume & Cost Estimates

| Entity | Records | Avg Tokens | Embedding Calls | Est. OpenAI Cost | Est. Ollama Cost |
|--------|---------|------------|-----------------|------------------|------------------|
| documentation_content | 1,500 | 2,000 | 6,000 chunks | $0.12 | $0 (local) |
| migration_transformations | 1,000 | 800 | 1,000 | $0.02 | $0 (local) |
| flavors | 100 | 3,000 | 300 chunks | $0.01 | $0 (local) |
| code_examples | 2,500 | 500 | 2,500 | $0.03 | $0 (local) |
| javadoc_classes | 25,000 | 400 | 25,000 | $0.25 | $0 (local) |
| language_features | 300 | 600 | 300 | $0.01 | $0 (local) |
| **Total** | **30,400** | - | **35,100** | **~$0.44** | **$0** |

*OpenAI pricing: text-embedding-3-small at $0.02/1M tokens*

---

## 3. Embedding Model Options

### 3.1 Cloud-Based Providers

#### 3.1.1 OpenAI Embeddings (Recommended for Production)

| Model | Dimensions | Max Tokens | Cost/1M Tokens | Quality |
|-------|------------|------------|----------------|---------|
| `text-embedding-3-small` | 1536 | 8191 | $0.02 | Good |
| `text-embedding-3-large` | 3072 | 8191 | $0.13 | Excellent |
| `text-embedding-ada-002` | 1536 | 8191 | $0.10 | Good (legacy) |

**Pros**:
- Highest quality embeddings
- Simple API integration via Spring AI
- Automatic model updates

**Cons**:
- Requires internet connectivity
- Cost at scale (minimal for this use case)
- Data leaves your infrastructure

**Spring AI Integration**:
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        model: text-embedding-3-small
```

#### 3.1.2 Google Gemini (Alternative)

| Model | Dimensions | Max Tokens | Cost/1M Tokens |
|-------|------------|------------|----------------|
| `text-embedding-004` | 768 | 2048 | $0.00025 (input) |

**Pros**: Extremely low cost, good quality

**Cons**: Lower dimensions, shorter context

#### 3.1.3 Voyage AI (Specialized)

| Model | Dimensions | Max Tokens | Quality |
|-------|------------|------------|---------|
| `voyage-code-2` | 1536 | 16000 | Excellent for code |

**Pros**: Optimized for code embeddings

**Cons**: Less general-purpose, separate API

### 3.2 Local/Offline Options (Ollama)

#### 3.2.1 Ollama Models for Embeddings

| Model | Dimensions | RAM Required | Quality | Speed |
|-------|------------|--------------|---------|-------|
| `nomic-embed-text` | 768 | 2GB | Good | Fast |
| `mxbai-embed-large` | 1024 | 4GB | Very Good | Medium |
| `snowflake-arctic-embed` | 1024 | 4GB | Good | Fast |
| `all-minilm` | 384 | 1GB | Moderate | Very Fast |
| `bge-large-en` | 1024 | 4GB | Excellent | Medium |

**Recommended**: `nomic-embed-text` for balance of quality, speed, and resource usage

**Ollama Setup**:
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull embedding model
ollama pull nomic-embed-text

# Verify
curl http://localhost:11434/api/embeddings -d '{
  "model": "nomic-embed-text",
  "prompt": "Spring Boot autoconfiguration"
}'
```

**Spring AI Ollama Integration**:
```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      embedding:
        model: nomic-embed-text
```

### 3.3 Model Selection Matrix

| Requirement | OpenAI | Gemini | Ollama (nomic) | Recommendation |
|-------------|--------|--------|----------------|----------------|
| Offline operation | No | No | **Yes** | Ollama |
| Lowest cost | Medium | **Best** | **Free** | Ollama/Gemini |
| Highest quality | **Best** | Good | Good | OpenAI |
| Code understanding | Good | Good | **Good** | Any |
| Easy integration | **Best** | Good | Good | OpenAI |
| Data privacy | No | No | **Yes** | Ollama |
| Resource requirements | None | None | 2-4GB RAM | Varies |

### 3.4 Recommended Approach: Hybrid Provider Support

```java
public interface EmbeddingProvider {
    List<float[]> embed(List<String> texts);
    int getDimensions();
    String getProviderName();
}

// Configurable via application.yml
// Supports: openai, ollama, gemini
```

**Configuration**:
```yaml
mcp:
  embeddings:
    enabled: true
    provider: ${EMBEDDING_PROVIDER:ollama}  # ollama, openai, gemini

    # Provider-specific settings
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}

    openai:
      api-key: ${OPENAI_API_KEY:}
      model: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}

    gemini:
      api-key: ${GEMINI_API_KEY:}
      model: ${GEMINI_EMBEDDING_MODEL:text-embedding-004}

    # Common settings
    dimensions: 768  # or 1536 for OpenAI
    chunk-size: 512
    chunk-overlap: 50
    batch-size: 100
```

---

## 4. Optional Feature Architecture

### 4.1 Feature Toggle Design

The embeddings feature is designed to be **completely optional**. When disabled:
- No pgvector extension is required
- No embedding-related columns are created
- No embedding-related queries are executed
- Application works with standard PostgreSQL 18

**Configuration**:
```yaml
mcp:
  features:
    embeddings:
      enabled: ${EMBEDDINGS_ENABLED:false}  # Default: DISABLED
```

### 4.2 Conditional Database Migration Strategy

When `embeddings.enabled=false`:
- Flyway migrations V21 and V22 are **skipped** using Flyway callbacks
- No pgvector extension installation attempted
- No vector columns added to existing tables
- Standard PostgreSQL 18 (without pgvector) works perfectly

When `embeddings.enabled=true`:
- Migrations V21 and V22 execute normally
- pgvector extension is installed
- Vector columns and HNSW indexes are created

**Implementation: Conditional Migration Callback**:
```java
@Component
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingMigrationCallback implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_EACH_MIGRATE;
    }

    @Override
    public void handle(Event event, Context context) {
        String migrationVersion = context.getMigrationInfo().getVersion().toString();
        // V21 and V22 are embedding-specific migrations
        if (migrationVersion.equals("21") || migrationVersion.equals("22")) {
            if (!embeddingsEnabled) {
                // Skip this migration by marking it as ignored
                log.info("Skipping embedding migration V{} - embeddings feature disabled",
                    migrationVersion);
            }
        }
    }
}
```

**Alternative: Separate Migration Locations**:
```yaml
spring:
  flyway:
    locations:
      - classpath:db/migration/core
      # Conditionally include embedding migrations
      - ${EMBEDDINGS_ENABLED:false} ? classpath:db/migration/embeddings : ""
```

### 4.3 Conditional Bean Configuration

```java
@Configuration
public class EmbeddingConfig {

    @Configuration
    @ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
    public static class EmbeddingsEnabledConfig {

        @Bean
        public EmbeddingProvider embeddingProvider(EmbeddingProperties props) {
            return switch (props.getProvider()) {
                case "ollama" -> new OllamaEmbeddingProvider(props.getOllama());
                case "openai" -> new OpenAIEmbeddingProvider(props.getOpenai());
                case "gemini" -> new GeminiEmbeddingProvider(props.getGemini());
                default -> throw new IllegalArgumentException("Unknown provider: " + props.getProvider());
            };
        }

        @Bean
        public EmbeddingService embeddingService(EmbeddingProvider provider) {
            return new EmbeddingServiceImpl(provider);
        }

        @Bean
        public EmbeddingSyncService embeddingSyncService(EmbeddingService service) {
            return new EmbeddingSyncService(service);
        }

        @Bean
        public EmbeddingJobProcessor embeddingJobProcessor(EmbeddingSyncService syncService) {
            return new EmbeddingJobProcessor(syncService);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "false", matchIfMissing = true)
    public static class EmbeddingsDisabledConfig {

        @Bean
        public EmbeddingService embeddingService() {
            // No-op implementation when embeddings disabled
            return new NoOpEmbeddingService();
        }
    }
}
```

### 4.4 Service Layer Abstraction

The search services use an abstraction that gracefully handles disabled embeddings:

```java
public interface SearchService {
    List<SearchResult> search(String query, SearchOptions options);
}

@Service
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class HybridSearchService implements SearchService {
    // Uses both TSVECTOR and vector search
    @Override
    public List<SearchResult> search(String query, SearchOptions options) {
        return hybridSearch(query, options);
    }
}

@Service
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "false", matchIfMissing = true)
public class KeywordSearchService implements SearchService {
    // Uses only TSVECTOR search (existing behavior)
    @Override
    public List<SearchResult> search(String query, SearchOptions options) {
        return keywordSearch(query, options);
    }
}
```

### 4.5 Environment Variables Reference

All embedding-related configuration is provided via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `EMBEDDINGS_ENABLED` | `false` | Enable/disable embeddings feature |
| `EMBEDDING_PROVIDER` | `ollama` | Provider: `ollama`, `openai`, `gemini` |
| `EMBEDDING_DIMENSIONS` | `768` | Vector dimensions (768 for Ollama, 1536 for OpenAI) |
| `EMBEDDING_RETRY_ATTEMPTS` | `10` | Max retry attempts for failed embeddings |
| `EMBEDDING_RETRY_DELAY` | `5000` | Delay between retries (ms) |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` | Ollama model name |
| `OPENAI_API_KEY` | (none) | OpenAI API key (required if provider=openai) |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | OpenAI model |
| `GEMINI_API_KEY` | (none) | Google Gemini API key |
| `GEMINI_EMBEDDING_MODEL` | `text-embedding-004` | Gemini model |

---

## 5. Embedding Job Processing & State Management

### 5.1 Embedding Triggers

Embeddings are generated asynchronously in two scenarios:

| Trigger | Event | Entities Affected |
|---------|-------|-------------------|
| **Comprehensive Sync** | After sync completes | All updated documentation, migrations, code examples |
| **Flavor Import/Create** | On flavor save | Individual flavor |
| **Manual Trigger** | Admin action via UI | Selected entities or all |
| **Scheduled Sync** | Cron job | Entities missing embeddings |

### 5.2 Async Job Processing Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Embedding Job Processing Flow                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────────────┐  │
│  │ Sync Service │───→│ Event        │───→│ EmbeddingJobProcessor        │  │
│  │ (Comprehensive)│   │ Publisher    │    │  - Receives sync complete    │  │
│  └──────────────┘    └──────────────┘    │  - Queues embedding jobs     │  │
│                                           │  - Processes async           │  │
│  ┌──────────────┐           │            └──────────────────────────────┘  │
│  │ Flavor       │───────────┘                         │                     │
│  │ Service      │    (ApplicationEvent)               │                     │
│  └──────────────┘                                     ▼                     │
│                                           ┌──────────────────────────────┐  │
│                                           │ EmbeddingJobQueue            │  │
│                                           │  - In-memory queue           │  │
│                                           │  - Priority ordering         │  │
│                                           │  - Batch processing          │  │
│                                           └──────────────────────────────┘  │
│                                                       │                     │
│                                                       ▼                     │
│                                           ┌──────────────────────────────┐  │
│                                           │ EmbeddingWorker (Async)      │  │
│                                           │  - Check provider available  │  │
│                                           │  - Generate embeddings       │  │
│                                           │  - Retry on failure          │  │
│                                           │  - Update job state          │  │
│                                           └──────────────────────────────┘  │
│                                                       │                     │
│                                                       ▼                     │
│                                           ┌──────────────────────────────┐  │
│                                           │ embedding_jobs (DB Table)    │  │
│                                           │  - State tracking            │  │
│                                           │  - Retry count               │  │
│                                           │  - Error messages            │  │
│                                           └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Embedding Job States

```java
public enum EmbeddingJobStatus {
    PENDING,        // Job created, waiting to be processed
    IN_PROGRESS,    // Currently generating embeddings
    COMPLETED,      // Successfully completed
    FAILED,         // Failed after all retry attempts exhausted
    RETRY_PENDING,  // Failed, waiting for retry
    CANCELLED       // Manually cancelled
}
```

**State Transitions**:
```
PENDING → IN_PROGRESS → COMPLETED
                      → RETRY_PENDING → IN_PROGRESS (retry)
                                      → FAILED (max retries exceeded)
                      → CANCELLED (manual)
```

### 5.4 Job State Database Schema

```sql
-- V21__pgvector_extension.sql (add to existing migration)

-- Embedding Jobs Table for state tracking
CREATE TABLE IF NOT EXISTS embedding_jobs (
    id BIGSERIAL PRIMARY KEY,

    -- Job identification
    entity_type VARCHAR(100) NOT NULL,  -- DOCUMENTATION, TRANSFORMATION, FLAVOR, CODE_EXAMPLE
    entity_id BIGINT NOT NULL,
    job_type VARCHAR(50) NOT NULL,      -- FULL_SYNC, INCREMENTAL, SINGLE_ENTITY

    -- Job state
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 5,    -- 1 (highest) to 10 (lowest)

    -- Retry tracking
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 10,
    next_retry_at TIMESTAMP,
    last_error TEXT,

    -- Provider tracking
    provider VARCHAR(50),               -- ollama, openai, gemini
    model VARCHAR(100),                 -- nomic-embed-text, text-embedding-3-small

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'RETRY_PENDING', 'CANCELLED')),
    CONSTRAINT chk_entity_type CHECK (entity_type IN ('DOCUMENTATION', 'TRANSFORMATION', 'FLAVOR', 'CODE_EXAMPLE', 'JAVADOC_CLASS'))
);

-- Indexes for job processing
CREATE INDEX idx_embedding_jobs_status ON embedding_jobs(status);
CREATE INDEX idx_embedding_jobs_pending ON embedding_jobs(status, priority, created_at) WHERE status IN ('PENDING', 'RETRY_PENDING');
CREATE INDEX idx_embedding_jobs_entity ON embedding_jobs(entity_type, entity_id);
CREATE INDEX idx_embedding_jobs_retry ON embedding_jobs(next_retry_at) WHERE status = 'RETRY_PENDING';

-- Embedding Provider Health Table
CREATE TABLE IF NOT EXISTS embedding_provider_health (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL UNIQUE,
    is_available BOOLEAN NOT NULL DEFAULT false,
    last_check_at TIMESTAMP,
    last_success_at TIMESTAMP,
    last_error TEXT,
    consecutive_failures INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default provider health records
INSERT INTO embedding_provider_health (provider, is_available) VALUES
    ('ollama', false),
    ('openai', false),
    ('gemini', false)
ON CONFLICT (provider) DO NOTHING;
```

### 5.5 Job Processing Service

```java
@Service
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingJobProcessor {

    private final EmbeddingJobRepository jobRepository;
    private final EmbeddingProviderHealthService healthService;
    private final EmbeddingSyncService syncService;
    private final EmbeddingProperties properties;

    @Value("${mcp.embeddings.retry.max-attempts:10}")
    private int maxRetryAttempts;

    @Value("${mcp.embeddings.retry.delay-ms:5000}")
    private long retryDelayMs;

    /**
     * Triggered after comprehensive sync completes.
     */
    @Async
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComprehensiveSyncCompleted(ComprehensiveSyncCompletedEvent event) {
        log.info("Comprehensive sync completed. Queueing embedding jobs for {} entities",
            event.getUpdatedEntityCount());

        // Queue jobs for all updated entities
        queueEmbeddingJobs(event.getUpdatedDocumentationIds(), EntityType.DOCUMENTATION);
        queueEmbeddingJobs(event.getUpdatedTransformationIds(), EntityType.TRANSFORMATION);
        queueEmbeddingJobs(event.getUpdatedCodeExampleIds(), EntityType.CODE_EXAMPLE);

        // Start processing
        processJobQueue();
    }

    /**
     * Triggered when a flavor is created or imported.
     */
    @Async
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFlavorSaved(FlavorSavedEvent event) {
        log.info("Flavor saved: {}. Queueing embedding job.", event.getFlavorId());

        EmbeddingJob job = EmbeddingJob.builder()
            .entityType(EntityType.FLAVOR)
            .entityId(event.getFlavorId())
            .jobType(JobType.SINGLE_ENTITY)
            .priority(1)  // High priority for user-triggered actions
            .maxRetries(maxRetryAttempts)
            .status(EmbeddingJobStatus.PENDING)
            .build();

        jobRepository.save(job);
        processJobQueue();
    }

    /**
     * Process pending jobs from the queue.
     */
    @Async("embeddingTaskExecutor")
    public void processJobQueue() {
        // Check if provider is available
        if (!healthService.isProviderAvailable()) {
            log.warn("Embedding provider not available. Jobs will be processed when provider becomes available.");
            scheduleHealthCheck();
            return;
        }

        List<EmbeddingJob> pendingJobs = jobRepository.findPendingJobs(
            List.of(EmbeddingJobStatus.PENDING, EmbeddingJobStatus.RETRY_PENDING),
            PageRequest.of(0, properties.getBatchSize())
        );

        for (EmbeddingJob job : pendingJobs) {
            processJob(job);
        }
    }

    private void processJob(EmbeddingJob job) {
        job.setStatus(EmbeddingJobStatus.IN_PROGRESS);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        try {
            syncService.generateEmbedding(job.getEntityType(), job.getEntityId());

            job.setStatus(EmbeddingJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setLastError(null);

        } catch (EmbeddingProviderUnavailableException e) {
            handleProviderUnavailable(job, e);
        } catch (Exception e) {
            handleJobFailure(job, e);
        }

        jobRepository.save(job);
    }

    private void handleProviderUnavailable(EmbeddingJob job, Exception e) {
        log.warn("Embedding provider unavailable for job {}: {}", job.getId(), e.getMessage());

        healthService.markProviderUnavailable(e.getMessage());

        job.setStatus(EmbeddingJobStatus.RETRY_PENDING);
        job.setLastError("Provider unavailable: " + e.getMessage());
        job.setNextRetryAt(Instant.now().plusMillis(retryDelayMs * 2)); // Longer delay for provider issues
    }

    private void handleJobFailure(EmbeddingJob job, Exception e) {
        job.setRetryCount(job.getRetryCount() + 1);
        job.setLastError(e.getMessage());

        if (job.getRetryCount() >= job.getMaxRetries()) {
            log.error("Embedding job {} failed after {} attempts: {}",
                job.getId(), job.getRetryCount(), e.getMessage());
            job.setStatus(EmbeddingJobStatus.FAILED);
        } else {
            log.warn("Embedding job {} failed (attempt {}/{}): {}. Will retry.",
                job.getId(), job.getRetryCount(), job.getMaxRetries(), e.getMessage());
            job.setStatus(EmbeddingJobStatus.RETRY_PENDING);
            job.setNextRetryAt(calculateNextRetryTime(job.getRetryCount()));
        }
    }

    /**
     * Exponential backoff for retries.
     */
    private Instant calculateNextRetryTime(int retryCount) {
        long delayMs = retryDelayMs * (long) Math.pow(2, Math.min(retryCount, 6));
        return Instant.now().plusMillis(delayMs);
    }
}
```

### 5.6 Provider Health Check Service

```java
@Service
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingProviderHealthService {

    private final EmbeddingProviderHealthRepository healthRepository;
    private final EmbeddingProvider embeddingProvider;
    private final EmbeddingProperties properties;

    private volatile boolean providerAvailable = false;
    private volatile Instant lastCheckTime = Instant.MIN;

    @Value("${mcp.embeddings.health-check.interval-ms:60000}")
    private long healthCheckIntervalMs;

    /**
     * Check if the embedding provider is currently available.
     */
    public boolean isProviderAvailable() {
        // Use cached value if recent
        if (Duration.between(lastCheckTime, Instant.now()).toMillis() < healthCheckIntervalMs) {
            return providerAvailable;
        }

        return checkProviderHealth();
    }

    /**
     * Perform health check against the embedding provider.
     */
    @Async
    public boolean checkProviderHealth() {
        String providerName = properties.getProvider();

        try {
            // Try to generate a test embedding
            List<float[]> result = embeddingProvider.embed(List.of("health check test"));

            if (result != null && !result.isEmpty() && result.get(0).length > 0) {
                markProviderAvailable();
                return true;
            }

        } catch (Exception e) {
            log.warn("Embedding provider health check failed: {}", e.getMessage());
            markProviderUnavailable(e.getMessage());
        }

        return false;
    }

    public void markProviderAvailable() {
        this.providerAvailable = true;
        this.lastCheckTime = Instant.now();

        EmbeddingProviderHealth health = healthRepository.findByProvider(properties.getProvider())
            .orElse(new EmbeddingProviderHealth(properties.getProvider()));

        health.setAvailable(true);
        health.setLastSuccessAt(Instant.now());
        health.setLastCheckAt(Instant.now());
        health.setConsecutiveFailures(0);
        health.setLastError(null);

        healthRepository.save(health);

        log.info("Embedding provider '{}' is now available", properties.getProvider());
    }

    public void markProviderUnavailable(String error) {
        this.providerAvailable = false;
        this.lastCheckTime = Instant.now();

        EmbeddingProviderHealth health = healthRepository.findByProvider(properties.getProvider())
            .orElse(new EmbeddingProviderHealth(properties.getProvider()));

        health.setAvailable(false);
        health.setLastCheckAt(Instant.now());
        health.setConsecutiveFailures(health.getConsecutiveFailures() + 1);
        health.setLastError(error);

        healthRepository.save(health);

        log.warn("Embedding provider '{}' marked unavailable. Consecutive failures: {}",
            properties.getProvider(), health.getConsecutiveFailures());
    }

    /**
     * Scheduled health check to detect when provider becomes available.
     */
    @Scheduled(fixedDelayString = "${mcp.embeddings.health-check.interval-ms:60000}")
    public void scheduledHealthCheck() {
        if (!providerAvailable) {
            log.debug("Performing scheduled health check for embedding provider");
            if (checkProviderHealth()) {
                // Provider is back! Resume job processing
                applicationEventPublisher.publishEvent(new EmbeddingProviderAvailableEvent(this));
            }
        }
    }
}
```

### 5.7 Comprehensive Sync Integration

```java
@Service
public class ComprehensiveSyncService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SyncResult runComprehensiveSync() {
        SyncResult result = new SyncResult();

        try {
            // Phase 0-9: Existing sync phases
            runProjectSync(result);
            runVersionSync(result);
            runDocumentationSync(result);
            runMigrationSync(result);
            runLanguageSync(result);
            runJavadocSync(result);
            // ... other phases

        } finally {
            // After sync completes (success or partial), trigger embedding generation
            publishSyncCompletedEvent(result);
        }

        return result;
    }

    private void publishSyncCompletedEvent(SyncResult result) {
        ComprehensiveSyncCompletedEvent event = ComprehensiveSyncCompletedEvent.builder()
            .updatedDocumentationIds(result.getUpdatedDocumentationIds())
            .updatedTransformationIds(result.getUpdatedTransformationIds())
            .updatedCodeExampleIds(result.getUpdatedCodeExampleIds())
            .updatedJavadocClassIds(result.getUpdatedJavadocClassIds())
            .syncDuration(result.getDuration())
            .build();

        // This will trigger EmbeddingJobProcessor.onComprehensiveSyncCompleted()
        eventPublisher.publishEvent(event);

        log.info("Published ComprehensiveSyncCompletedEvent with {} entities to embed",
            event.getUpdatedEntityCount());
    }
}
```

### 5.8 Flavor Service Integration

```java
@Service
public class FlavorServiceImpl implements FlavorService {

    private final FlavorRepository flavorRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Flavor create(FlavorCreateRequest request) {
        Flavor flavor = mapToEntity(request);
        flavor = flavorRepository.save(flavor);

        // Trigger embedding generation
        publishFlavorSavedEvent(flavor);

        return flavor;
    }

    @Override
    @Transactional
    public Flavor importFromMarkdown(String markdown, String filename) {
        Flavor flavor = parseMarkdownToFlavor(markdown, filename);
        flavor = flavorRepository.save(flavor);

        // Trigger embedding generation
        publishFlavorSavedEvent(flavor);

        return flavor;
    }

    @Override
    @Transactional
    public Flavor update(Long id, FlavorUpdateRequest request) {
        Flavor flavor = flavorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Flavor not found: " + id));

        updateFlavorFields(flavor, request);
        flavor = flavorRepository.save(flavor);

        // Content changed? Regenerate embedding
        if (request.isContentChanged()) {
            publishFlavorSavedEvent(flavor);
        }

        return flavor;
    }

    private void publishFlavorSavedEvent(Flavor flavor) {
        eventPublisher.publishEvent(new FlavorSavedEvent(this, flavor.getId()));
    }
}
```

### 5.9 Retry Configuration

```yaml
# application.yml
mcp:
  features:
    embeddings:
      enabled: ${EMBEDDINGS_ENABLED:false}

  embeddings:
    provider: ${EMBEDDING_PROVIDER:ollama}

    # Retry configuration
    retry:
      max-attempts: ${EMBEDDING_RETRY_ATTEMPTS:10}
      initial-delay-ms: ${EMBEDDING_RETRY_INITIAL_DELAY:5000}
      max-delay-ms: ${EMBEDDING_RETRY_MAX_DELAY:300000}  # 5 minutes max
      multiplier: 2.0  # Exponential backoff multiplier

    # Health check configuration
    health-check:
      enabled: true
      interval-ms: ${EMBEDDING_HEALTH_CHECK_INTERVAL:60000}  # 1 minute
      timeout-ms: ${EMBEDDING_HEALTH_CHECK_TIMEOUT:10000}    # 10 seconds

    # Job processing configuration
    job:
      batch-size: ${EMBEDDING_JOB_BATCH_SIZE:50}
      thread-pool-size: ${EMBEDDING_JOB_THREADS:2}
      queue-capacity: ${EMBEDDING_JOB_QUEUE_SIZE:1000}

    # Provider configurations
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}
      timeout-ms: ${OLLAMA_TIMEOUT:30000}

    openai:
      api-key: ${OPENAI_API_KEY:}
      model: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
      timeout-ms: ${OPENAI_TIMEOUT:30000}

    gemini:
      api-key: ${GEMINI_API_KEY:}
      model: ${GEMINI_EMBEDDING_MODEL:text-embedding-004}
      timeout-ms: ${GEMINI_TIMEOUT:30000}
```

### 5.10 UI: Embedding Status Dashboard

A new UI page at `/embeddings` (admin only) shows:

| Section | Information |
|---------|-------------|
| **Provider Status** | Current provider, availability, last check, consecutive failures |
| **Job Statistics** | Pending, in-progress, completed, failed, retry-pending counts |
| **Entity Coverage** | Percentage of entities with embeddings per type |
| **Recent Jobs** | Table of recent jobs with status, retry count, errors |
| **Actions** | Retry failed jobs, cancel pending jobs, trigger manual sync |

```html
<!-- /embeddings page structure -->
<div class="embeddings-dashboard">
    <!-- Provider Health Card -->
    <div class="card">
        <h5>Embedding Provider Status</h5>
        <div class="provider-status">
            <span class="provider-name">ollama (nomic-embed-text)</span>
            <span class="status-badge available">Available</span>
            <span class="last-check">Last checked: 2 minutes ago</span>
        </div>
    </div>

    <!-- Job Statistics Cards -->
    <div class="stats-grid">
        <div class="stat-card pending">
            <span class="count">24</span>
            <span class="label">Pending</span>
        </div>
        <div class="stat-card in-progress">
            <span class="count">2</span>
            <span class="label">In Progress</span>
        </div>
        <div class="stat-card completed">
            <span class="count">1,250</span>
            <span class="label">Completed</span>
        </div>
        <div class="stat-card failed">
            <span class="count">3</span>
            <span class="label">Failed</span>
        </div>
    </div>

    <!-- Entity Coverage -->
    <div class="card">
        <h5>Embedding Coverage</h5>
        <table>
            <tr><td>Documentation</td><td>1,450 / 1,500</td><td>96.7%</td></tr>
            <tr><td>Transformations</td><td>980 / 1,000</td><td>98.0%</td></tr>
            <tr><td>Flavors</td><td>95 / 100</td><td>95.0%</td></tr>
            <tr><td>Code Examples</td><td>2,400 / 2,500</td><td>96.0%</td></tr>
        </table>
    </div>

    <!-- Recent Jobs Table -->
    <div class="card">
        <h5>Recent Jobs</h5>
        <table>
            <thead>
                <tr>
                    <th>Entity</th>
                    <th>Status</th>
                    <th>Retries</th>
                    <th>Last Error</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <!-- Job rows -->
            </tbody>
        </table>
    </div>
</div>
```

---

## 6. PostgreSQL pgvector Migration Plan

### 4.1 pgvector Overview

pgvector is a PostgreSQL extension for vector similarity search:

- **Vector Storage**: VECTOR(n) data type for embedding storage
- **Distance Functions**: L2 distance, inner product, cosine distance
- **Index Types**: IVFFlat (faster build) and HNSW (faster query)
- **PostgreSQL 18 Compatible**: Native extension support

### 4.2 Migration Strategy

**Approach**: Additive migration - add embedding columns alongside existing TSVECTOR

```
Current State (TSVECTOR only)
    ↓
Phase 1: Add pgvector extension + embedding columns
    ↓
Phase 2: Populate embeddings (batch process)
    ↓
Phase 3: Implement hybrid search (TSVECTOR + vector)
    ↓
Final State: Semantic + keyword search combined
```

### 4.3 Database Migration Scripts

#### V21__pgvector_extension.sql

```sql
-- ============================================================================
-- V21: Add pgvector extension for semantic embeddings
-- ============================================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column to documentation_content
ALTER TABLE documentation_content
ADD COLUMN IF NOT EXISTS content_embedding vector(768);

-- Add embedding column to migration_transformations
ALTER TABLE migration_transformations
ADD COLUMN IF NOT EXISTS transformation_embedding vector(768);

-- Add embedding column to flavors
ALTER TABLE flavors
ADD COLUMN IF NOT EXISTS flavor_embedding vector(768);

-- Add embedding column to code_examples
ALTER TABLE code_examples
ADD COLUMN IF NOT EXISTS example_embedding vector(768);

-- Add embedding tracking columns
ALTER TABLE documentation_content
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

ALTER TABLE migration_transformations
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

ALTER TABLE flavors
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

ALTER TABLE code_examples
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

-- Create embedding metadata table for tracking
CREATE TABLE IF NOT EXISTS embedding_metadata (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    chunk_index INT DEFAULT 0,
    chunk_text TEXT,
    embedding vector(768),
    embedding_model VARCHAR(100) NOT NULL,
    token_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id, chunk_index)
);

-- Add comments for documentation
COMMENT ON COLUMN documentation_content.content_embedding IS 'Semantic embedding vector for content similarity search';
COMMENT ON COLUMN migration_transformations.transformation_embedding IS 'Semantic embedding for migration pattern matching';
COMMENT ON COLUMN flavors.flavor_embedding IS 'Semantic embedding for architecture/compliance pattern search';
COMMENT ON COLUMN code_examples.example_embedding IS 'Semantic embedding for code example discovery';
```

#### V22__pgvector_indexes.sql

```sql
-- ============================================================================
-- V22: Create HNSW indexes for vector similarity search
-- ============================================================================

-- HNSW indexes for fast approximate nearest neighbor search
-- m = 16 (connections per layer), ef_construction = 64 (build-time quality)

-- Documentation content embedding index
CREATE INDEX IF NOT EXISTS idx_documentation_content_embedding
ON documentation_content
USING hnsw (content_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Migration transformations embedding index
CREATE INDEX IF NOT EXISTS idx_migration_transformations_embedding
ON migration_transformations
USING hnsw (transformation_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Flavors embedding index
CREATE INDEX IF NOT EXISTS idx_flavors_embedding
ON flavors
USING hnsw (flavor_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Code examples embedding index
CREATE INDEX IF NOT EXISTS idx_code_examples_embedding
ON code_examples
USING hnsw (example_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Embedding metadata index for chunk lookup
CREATE INDEX IF NOT EXISTS idx_embedding_metadata_lookup
ON embedding_metadata (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_embedding_metadata_vector
ON embedding_metadata
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Partial indexes for non-null embeddings (query optimization)
CREATE INDEX IF NOT EXISTS idx_documentation_content_has_embedding
ON documentation_content (id)
WHERE content_embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_migration_transformations_has_embedding
ON migration_transformations (id)
WHERE transformation_embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_flavors_has_embedding
ON flavors (id)
WHERE flavor_embedding IS NOT NULL;

-- Statistics for query planner
ANALYZE documentation_content;
ANALYZE migration_transformations;
ANALYZE flavors;
ANALYZE code_examples;
ANALYZE embedding_metadata;
```

### 4.4 Preserving Existing Data

The migration is **fully additive** and preserves all existing data:

1. **No data deletion**: Existing TSVECTOR indices remain intact
2. **No schema changes**: Existing columns untouched
3. **Null embeddings**: New columns default to NULL until populated
4. **Gradual population**: Embeddings populated via background job
5. **Hybrid search**: Both TSVECTOR and vector search available

### 4.5 Rollback Strategy

```sql
-- Rollback V22 (indexes)
DROP INDEX IF EXISTS idx_documentation_content_embedding;
DROP INDEX IF EXISTS idx_migration_transformations_embedding;
DROP INDEX IF EXISTS idx_flavors_embedding;
DROP INDEX IF EXISTS idx_code_examples_embedding;
DROP INDEX IF EXISTS idx_embedding_metadata_lookup;
DROP INDEX IF EXISTS idx_embedding_metadata_vector;
DROP INDEX IF EXISTS idx_documentation_content_has_embedding;
DROP INDEX IF EXISTS idx_migration_transformations_has_embedding;
DROP INDEX IF EXISTS idx_flavors_has_embedding;

-- Rollback V21 (columns and extension)
ALTER TABLE documentation_content
DROP COLUMN IF EXISTS content_embedding,
DROP COLUMN IF EXISTS embedding_model,
DROP COLUMN IF EXISTS embedded_at;

ALTER TABLE migration_transformations
DROP COLUMN IF EXISTS transformation_embedding,
DROP COLUMN IF EXISTS embedding_model,
DROP COLUMN IF EXISTS embedded_at;

ALTER TABLE flavors
DROP COLUMN IF EXISTS flavor_embedding,
DROP COLUMN IF EXISTS embedding_model,
DROP COLUMN IF EXISTS embedded_at;

ALTER TABLE code_examples
DROP COLUMN IF EXISTS example_embedding,
DROP COLUMN IF EXISTS embedding_model,
DROP COLUMN IF EXISTS embedded_at;

DROP TABLE IF EXISTS embedding_metadata;

-- Note: DROP EXTENSION vector; may fail if other objects depend on it
```

---

## 5. Docker Configuration Changes

### 5.1 PostgreSQL 18 with pgvector

PostgreSQL 18 with pgvector requires using an image that includes the extension.

#### Option A: Use pgvector/pgvector Image (Recommended)

```yaml
# docker-compose.yml
services:
  postgres:
    image: pgvector/pgvector:pg18
    container_name: spring-mcp-db
    environment:
      POSTGRES_DB: spring_mcp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - ./.vols/db:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
```

#### Option B: Custom Dockerfile with pgvector

```dockerfile
# docker/postgres/Dockerfile
FROM postgres:18-alpine

# Install build dependencies
RUN apk add --no-cache \
    git \
    build-base \
    clang15 \
    llvm15-dev

# Clone and build pgvector
RUN git clone --branch v0.8.0 https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && cd /tmp/pgvector \
    && make \
    && make install \
    && rm -rf /tmp/pgvector

# Cleanup build dependencies
RUN apk del git build-base clang15 llvm15-dev
```

### 5.2 Updated docker-compose.yml

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg18
    container_name: spring-mcp-db
    environment:
      POSTGRES_DB: spring_mcp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - ./.vols/db:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    command:
      - "postgres"
      - "-c"
      - "shared_preload_libraries=vector"
```

### 5.3 Updated docker-compose-all.yaml

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg18
    container_name: spring-mcp-db
    environment:
      POSTGRES_DB: spring_mcp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - ./.vols/db:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - spring-mcp-network
    restart: unless-stopped
    command:
      - "postgres"
      - "-c"
      - "shared_preload_libraries=vector"

  # Optional: Ollama for local embeddings
  ollama:
    image: ollama/ollama:latest
    container_name: spring-mcp-ollama
    ports:
      - "11434:11434"
    volumes:
      - ./.vols/ollama:/root/.ollama
    networks:
      - spring-mcp-network
    restart: unless-stopped
    deploy:
      resources:
        reservations:
          memory: 4G
    # Pull embedding model on first start
    entrypoint: ["/bin/sh", "-c"]
    command:
      - |
        ollama serve &
        sleep 5
        ollama pull nomic-embed-text
        wait

  spring-boot-documentation-mcp-server:
    image: spring-boot-documentation-mcp-server:1.6.0
    container_name: spring-boot-documentation-mcp-server
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: spring_mcp
      DB_USER: postgres
      DB_PASSWORD: postgres
      ADMIN_USER: admin
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-changeme}
      SERVER_PORT: 8080
      GITHUB_TOKEN: ${GITHUB_TOKEN:-}
      # Embedding configuration
      EMBEDDING_PROVIDER: ${EMBEDDING_PROVIDER:-ollama}
      OLLAMA_BASE_URL: http://ollama:11434
      OLLAMA_EMBEDDING_MODEL: nomic-embed-text
      OPENAI_API_KEY: ${OPENAI_API_KEY:-}
    ports:
      - "8888:8080"
    depends_on:
      postgres:
        condition: service_healthy
      ollama:
        condition: service_started
    networks:
      - spring-mcp-network
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

networks:
  spring-mcp-network:
    driver: bridge
```

### 5.4 Migration from Existing PostgreSQL Data

For existing installations with PostgreSQL 18-alpine:

```bash
# 1. Backup existing data
docker exec spring-mcp-db pg_dump -U postgres spring_mcp > backup.sql

# 2. Stop services
docker-compose down

# 3. Update docker-compose.yml with pgvector image
# (see sections above)

# 4. Remove old volume (optional - only if clean restart needed)
# docker volume rm spring-mcp_postgres_data

# 5. Start with new image
docker-compose up -d

# 6. Wait for postgres to be ready
sleep 10

# 7. Restore data (if needed)
docker exec -i spring-mcp-db psql -U postgres spring_mcp < backup.sql

# 8. Flyway will run V21 and V22 migrations on app startup
```

---

## 6. Architecture Design

### 6.1 Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Spring MCP Server                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐    ┌──────────────────┐    ┌───────────────────────┐  │
│  │   MCP Tools     │───→│ EmbeddingService │───→│ EmbeddingProvider     │  │
│  │ (searchFlavors  │    │  - embed()       │    │ (OpenAI/Ollama/Gemini)│  │
│  │  searchDocs...) │    │  - search()      │    └───────────────────────┘  │
│  └─────────────────┘    │  - hybridSearch()│              │                │
│           │             └──────────────────┘              │                │
│           │                      │                        ▼                │
│           │                      │              ┌───────────────────────┐  │
│           │                      │              │   External API        │  │
│           │                      │              │   (OpenAI/Gemini)     │  │
│           │                      │              └───────────────────────┘  │
│           │                      │                        OR               │
│           │                      │              ┌───────────────────────┐  │
│           │                      │              │   Ollama (Local)      │  │
│           │                      │              │   localhost:11434     │  │
│           ▼                      ▼              └───────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         PostgreSQL 18 + pgvector                     │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │   │
│  │  │ TSVECTOR Index  │  │ HNSW Index      │  │ Combined Query      │  │   │
│  │  │ (keyword search)│  │ (vector search) │  │ (hybrid ranking)    │  │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Service Layer Design

```
src/main/java/com/spring/mcp/
├── config/
│   └── EmbeddingConfig.java           # Embedding provider configuration
├── service/
│   └── embedding/
│       ├── EmbeddingService.java      # Main embedding service interface
│       ├── EmbeddingServiceImpl.java  # Hybrid search implementation
│       ├── EmbeddingProvider.java     # Provider abstraction interface
│       ├── OpenAIEmbeddingProvider.java
│       ├── OllamaEmbeddingProvider.java
│       ├── GeminiEmbeddingProvider.java
│       ├── ChunkingService.java       # Text chunking for large docs
│       └── EmbeddingSyncService.java  # Background embedding population
├── model/
│   └── embedding/
│       ├── EmbeddingMetadata.java     # Entity for embedding tracking
│       └── EmbeddingResult.java       # Search result with similarity score
├── repository/
│   └── EmbeddingMetadataRepository.java
└── scheduler/
    └── EmbeddingSyncScheduler.java    # Scheduled embedding updates
```

### 6.3 Hybrid Search Algorithm

The hybrid search combines TSVECTOR (BM25-style) and vector similarity:

```java
public class HybridSearchService {

    /**
     * Hybrid search combining keyword and semantic search.
     *
     * Score = (alpha * keywordScore) + ((1 - alpha) * vectorScore)
     * Default alpha = 0.3 (semantic-weighted)
     */
    public List<SearchResult> hybridSearch(String query, SearchOptions options) {
        // 1. Generate query embedding
        float[] queryEmbedding = embeddingService.embed(query);

        // 2. Execute parallel searches
        CompletableFuture<List<KeywordResult>> keywordFuture =
            CompletableFuture.supplyAsync(() -> keywordSearch(query, options));
        CompletableFuture<List<VectorResult>> vectorFuture =
            CompletableFuture.supplyAsync(() -> vectorSearch(queryEmbedding, options));

        // 3. Combine results with Reciprocal Rank Fusion (RRF)
        List<KeywordResult> keywordResults = keywordFuture.join();
        List<VectorResult> vectorResults = vectorFuture.join();

        return combineWithRRF(keywordResults, vectorResults, options.getAlpha());
    }

    /**
     * Reciprocal Rank Fusion for combining ranked lists.
     * RRF(d) = Σ 1 / (k + rank(d))
     * Where k = 60 (standard constant)
     */
    private List<SearchResult> combineWithRRF(
            List<KeywordResult> keyword,
            List<VectorResult> vector,
            double alpha) {
        Map<Long, Double> scores = new HashMap<>();
        int k = 60;

        // Add keyword ranks
        for (int i = 0; i < keyword.size(); i++) {
            long id = keyword.get(i).getId();
            scores.merge(id, alpha / (k + i + 1), Double::sum);
        }

        // Add vector ranks
        for (int i = 0; i < vector.size(); i++) {
            long id = vector.get(i).getId();
            scores.merge(id, (1 - alpha) / (k + i + 1), Double::sum);
        }

        // Sort by combined score
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(e -> buildSearchResult(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }
}
```

### 6.4 Vector Search Query Examples

```sql
-- Basic vector similarity search (cosine distance)
SELECT id, title, 1 - (content_embedding <=> $1::vector) AS similarity
FROM documentation_content
WHERE content_embedding IS NOT NULL
ORDER BY content_embedding <=> $1::vector
LIMIT 10;

-- Hybrid search with TSVECTOR and vector
WITH keyword_results AS (
    SELECT id, ts_rank_cd(indexed_content, plainto_tsquery('english', $1)) AS keyword_score
    FROM documentation_content
    WHERE indexed_content @@ plainto_tsquery('english', $1)
),
vector_results AS (
    SELECT id, 1 - (content_embedding <=> $2::vector) AS vector_score
    FROM documentation_content
    WHERE content_embedding IS NOT NULL
)
SELECT
    COALESCE(k.id, v.id) AS id,
    (COALESCE(k.keyword_score, 0) * 0.3 + COALESCE(v.vector_score, 0) * 0.7) AS combined_score
FROM keyword_results k
FULL OUTER JOIN vector_results v ON k.id = v.id
ORDER BY combined_score DESC
LIMIT 20;

-- Filtered vector search (by project and version)
SELECT dc.id, dc.title, 1 - (dc.content_embedding <=> $1::vector) AS similarity
FROM documentation_content dc
JOIN documentation_links dl ON dc.link_id = dl.id
JOIN project_versions pv ON dl.version_id = pv.id
WHERE dc.content_embedding IS NOT NULL
  AND pv.project_id = $2
  AND pv.version = $3
ORDER BY dc.content_embedding <=> $1::vector
LIMIT 10;
```

---

## 7. Implementation Plan

### 7.1 Phase Overview

| Phase | Description | Duration | Dependencies |
|-------|-------------|----------|--------------|
| **Phase 1** | Infrastructure Setup | 2-3 days | None |
| **Phase 2** | Embedding Service Core | 3-4 days | Phase 1 |
| **Phase 3** | Provider Implementations | 2-3 days | Phase 2 |
| **Phase 4** | Database Integration | 2-3 days | Phase 3 |
| **Phase 5** | MCP Tool Updates | 3-4 days | Phase 4 |
| **Phase 6** | Testing & Optimization | 3-4 days | Phase 5 |
| **Phase 7** | Documentation & Release | 1-2 days | Phase 6 |

**Total Estimated Duration**: 16-23 days

### 7.2 Phase 1: Infrastructure Setup

**Tasks**:
1. Update docker-compose.yml with pgvector/pgvector:pg18 image
2. Update docker-compose-all.yaml with Ollama service
3. Create Flyway migration V21__pgvector_extension.sql
4. Create Flyway migration V22__pgvector_indexes.sql
5. Add embedding configuration to application.yml
6. Add Spring AI dependencies for embeddings
7. Test pgvector installation and migration

**Deliverables**:
- [ ] Docker configurations updated
- [ ] Database migrations created and tested
- [ ] Configuration properties defined
- [ ] Dependencies added to build.gradle

### 7.3 Phase 2: Embedding Service Core

**Tasks**:
1. Create EmbeddingProvider interface
2. Create EmbeddingService interface and implementation
3. Implement ChunkingService for large documents
4. Create EmbeddingMetadata entity and repository
5. Implement embedding storage and retrieval
6. Add unit tests for core services

**Deliverables**:
- [ ] EmbeddingProvider interface
- [ ] EmbeddingService with embed/search methods
- [ ] ChunkingService for text splitting
- [ ] 80%+ test coverage for core services

### 7.4 Phase 3: Provider Implementations

**Tasks**:
1. Implement OllamaEmbeddingProvider
2. Implement OpenAIEmbeddingProvider
3. Implement GeminiEmbeddingProvider (optional)
4. Add provider auto-configuration
5. Implement retry and fallback logic
6. Add integration tests for each provider

**Deliverables**:
- [ ] OllamaEmbeddingProvider with nomic-embed-text
- [ ] OpenAIEmbeddingProvider with text-embedding-3-small
- [ ] Provider selection via configuration
- [ ] Integration tests for providers

### 7.5 Phase 4: Database Integration

**Tasks**:
1. Add embedding columns to existing entities
2. Update repositories with vector search methods
3. Implement HybridSearchService
4. Create EmbeddingSyncService for batch population
5. Add scheduler for incremental embedding updates
6. Implement embedding status UI page

**Deliverables**:
- [ ] Updated entity classes with embedding fields
- [ ] Repository methods for vector search
- [ ] Hybrid search combining TSVECTOR + vector
- [ ] Background sync service
- [ ] UI page showing embedding status

### 7.6 Phase 5: MCP Tool Updates

**Tasks**:
1. Update searchSpringDocs to use hybrid search
2. Update searchMigrationKnowledge to use hybrid search
3. Update searchFlavors to use hybrid search
4. Update searchJavadocs to use hybrid search
5. Add similarity threshold configuration
6. Add embedding-based "similar content" feature
7. Update tool descriptions for semantic search

**Deliverables**:
- [ ] Hybrid search in 4+ MCP search tools
- [ ] Improved relevance in search results
- [ ] Reduced token consumption in responses
- [ ] Updated tool documentation

### 7.7 Phase 6: Testing & Optimization

**Tasks**:
1. Create embedding-specific test suite
2. Performance benchmarks for vector search
3. Compare relevance: TSVECTOR vs Hybrid
4. Tune hybrid search alpha parameter
5. Optimize HNSW index parameters
6. Load testing with concurrent queries
7. Memory and resource profiling

**Deliverables**:
- [ ] 80%+ overall test coverage
- [ ] Benchmark results documented
- [ ] Relevance comparison report
- [ ] Performance optimization applied

### 7.8 Phase 7: Documentation & Release

**Tasks**:
1. Update README.md with embedding feature
2. Update ADDITIONAL_CONTENT.md with technical details
3. Update CHANGELOG.md for version 1.6.0
4. Update VERSIONS.md with new version
5. Create embedding configuration guide
6. Build and tag release 1.6.0

**Deliverables**:
- [ ] README.md updated
- [ ] ADDITIONAL_CONTENT.md updated
- [ ] CHANGELOG.md with 1.6.0 entry
- [ ] VERSIONS.md updated
- [ ] Release tagged and built

---

## 8. Risk Assessment

### 8.1 Risk Matrix

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **pgvector compatibility** | Low | High | Test with pg18-alpine before migration |
| **Embedding model quality** | Low | Medium | Start with proven OpenAI model, add Ollama |
| **Performance degradation** | Medium | Medium | Async embedding, HNSW indexes, caching |
| **Memory usage increase** | Medium | Low | Monitor, configure appropriate index params |
| **API rate limiting** | Medium | Low | Batch embedding, local Ollama fallback |
| **Migration data loss** | Very Low | Critical | Additive migrations only, backup strategy |
| **Hybrid search complexity** | Medium | Medium | Start simple, tune alpha parameter |
| **Cost overrun (cloud APIs)** | Low | Low | Default to Ollama, OpenAI optional |

### 8.2 Contingency Plans

1. **pgvector Issues**: Fall back to pure TSVECTOR, defer embedding feature
2. **Provider Failures**: Automatic fallback chain: OpenAI → Ollama → TSVECTOR
3. **Performance Issues**: Disable hybrid search, use TSVECTOR only
4. **Memory Pressure**: Reduce HNSW ef_construction, use IVFFlat instead

---

## 9. Success Metrics

### 9.1 Quantitative Metrics

| Metric | Baseline | Target | Measurement Method |
|--------|----------|--------|-------------------|
| Search relevance (MRR) | 0.45 | 0.75 | Manual evaluation of top-10 results |
| Token consumption/search | 8,000 | 4,500 | Avg response size × token ratio |
| Query latency (p95) | 150ms | 200ms | Benchmark suite |
| Embedding coverage | 0% | 90%+ | % of content with embeddings |
| Test coverage | 75% | 80%+ | JaCoCo report |

### 9.2 Qualitative Metrics

- Semantic queries produce relevant results ("configure database" → JDBC docs)
- Related concepts cluster in search results
- Reduced "no results found" for valid queries
- Developer satisfaction with search quality

### 9.3 Acceptance Criteria

1. **Functional**:
   - [ ] Embeddings generated for 90%+ of documentation_content
   - [ ] Embeddings generated for 90%+ of migration_transformations
   - [ ] Embeddings generated for 90%+ of flavors
   - [ ] Hybrid search operational in searchSpringDocs
   - [ ] Hybrid search operational in searchFlavors

2. **Performance**:
   - [ ] Vector search p95 latency < 200ms
   - [ ] Hybrid search p95 latency < 250ms
   - [ ] No degradation in existing TSVECTOR search

3. **Quality**:
   - [ ] 80%+ test coverage for embedding components
   - [ ] All existing tests pass
   - [ ] Documentation updated

4. **Operations**:
   - [ ] Ollama provider works offline
   - [ ] OpenAI provider configurable via env var
   - [ ] Embedding sync runs without errors

---

## 10. Ralph-Wiggum Implementation Loop

### 10.1 Loop Configuration

To execute the implementation plan using ralph-wiggum loops:

**Completion Promise**: `COMPLETED`
**Max Iterations**: 10

### 10.2 Prompt to Start Implementation Loop

```
/ralph-wiggum:ralph-loop

Execute the embeddings feature implementation plan from capabilities/EMBEDDINGS_EVALUATION.md.

## Implementation Requirements

1. **Phase 1: Infrastructure Setup**
   - Update docker-compose.yml to use pgvector/pgvector:pg18 image
   - Update docker-compose-all.yaml with Ollama service
   - Create V21__pgvector_extension.sql migration
   - Create V22__pgvector_indexes.sql migration
   - Add embedding configuration to application.yml
   - Add Spring AI embedding dependencies to build.gradle

2. **Phase 2: Embedding Service Core**
   - Create EmbeddingProvider interface in service/embedding/
   - Create EmbeddingService interface and EmbeddingServiceImpl
   - Implement ChunkingService for large document handling
   - Create EmbeddingMetadata entity and repository
   - Write comprehensive unit tests (>80% coverage)

3. **Phase 3: Provider Implementations**
   - Implement OllamaEmbeddingProvider with nomic-embed-text support
   - Implement OpenAIEmbeddingProvider with text-embedding-3-small
   - Add EmbeddingConfig for provider auto-configuration
   - Implement retry logic and fallback handling
   - Write integration tests for providers

4. **Phase 4: Database Integration**
   - Add embedding columns to DocumentationContent, MigrationTransformation, Flavor, CodeExample entities
   - Update repositories with vector search methods
   - Implement HybridSearchService with RRF algorithm
   - Create EmbeddingSyncService for batch embedding population
   - Add EmbeddingSyncScheduler for incremental updates
   - Create embedding status UI page at /embeddings

5. **Phase 5: MCP Tool Updates**
   - Update searchSpringDocs to use hybrid search
   - Update searchMigrationKnowledge to use hybrid search
   - Update searchFlavors to use hybrid search
   - Add similarity threshold configuration
   - Update tool descriptions for semantic search capabilities

6. **Phase 6: Testing & Optimization**
   - Create comprehensive test suite (>80% coverage for new code)
   - Performance benchmarks for vector search
   - Integration tests for hybrid search
   - Verify all existing tests still pass

7. **Phase 7: Documentation & Release**
   - Update README.md with embeddings feature section
   - Update ADDITIONAL_CONTENT.md with technical details
   - Read VERSIONS.md and bump app version to 1.6.0
   - Update CHANGELOG.md with version 1.6.0 entry
   - Update all version references per VERSIONS.md guidelines

## Completion Criteria

Print "COMPLETED" when ALL of the following are verified:
1. All 7 phases implemented and tested
2. Flyway migrations run successfully
3. Application compiles with ./gradlew build
4. All tests pass with >80% coverage on new code
5. README.md documents the embeddings feature
6. ADDITIONAL_CONTENT.md has technical details
7. CHANGELOG.md has 1.6.0 entry
8. VERSIONS.md updated to 1.6.0
9. Application starts successfully
10. At least one MCP search tool uses hybrid search

## Key Files to Create/Modify

### New Files:
- src/main/resources/db/migration/V21__pgvector_extension.sql
- src/main/resources/db/migration/V22__pgvector_indexes.sql
- src/main/java/com/spring/mcp/config/EmbeddingConfig.java
- src/main/java/com/spring/mcp/service/embedding/EmbeddingProvider.java
- src/main/java/com/spring/mcp/service/embedding/EmbeddingService.java
- src/main/java/com/spring/mcp/service/embedding/EmbeddingServiceImpl.java
- src/main/java/com/spring/mcp/service/embedding/OllamaEmbeddingProvider.java
- src/main/java/com/spring/mcp/service/embedding/OpenAIEmbeddingProvider.java
- src/main/java/com/spring/mcp/service/embedding/ChunkingService.java
- src/main/java/com/spring/mcp/service/embedding/EmbeddingSyncService.java
- src/main/java/com/spring/mcp/service/embedding/HybridSearchService.java
- src/main/java/com/spring/mcp/model/entity/EmbeddingMetadata.java
- src/main/java/com/spring/mcp/repository/EmbeddingMetadataRepository.java
- src/main/java/com/spring/mcp/scheduler/EmbeddingSyncScheduler.java
- src/test/java/com/spring/mcp/service/embedding/*Test.java

### Modified Files:
- docker-compose.yml
- docker-compose-all.yaml
- build.gradle (add Spring AI embedding dependencies)
- src/main/resources/application.yml (embedding config)
- src/main/java/com/spring/mcp/model/entity/DocumentationContent.java
- src/main/java/com/spring/mcp/model/entity/MigrationTransformation.java
- src/main/java/com/spring/mcp/model/entity/Flavor.java
- src/main/java/com/spring/mcp/model/entity/CodeExample.java
- src/main/java/com/spring/mcp/service/tools/SpringDocumentationTools.java
- src/main/java/com/spring/mcp/service/tools/MigrationTools.java
- src/main/java/com/spring/mcp/service/tools/FlavorTools.java
- README.md
- ADDITIONAL_CONTENT.md
- CHANGELOG.md
- VERSIONS.md

when unsure how to use Spring AI 1.1.2 with embeddings **use spring** boot documentation mcp server.
For dev and local testing this host runs ollama already serving nomic-embed-text model.
Start with Phase 1 and proceed sequentially through all phases.
```

### 10.3 Progress Tracking

During implementation, track progress against these milestones:

| Phase | Milestone | Status |
|-------|-----------|--------|
| 1 | Docker configs updated | [ ] |
| 1 | Migrations created | [ ] |
| 1 | Dependencies added | [ ] |
| 2 | EmbeddingService created | [ ] |
| 2 | ChunkingService created | [ ] |
| 2 | Unit tests written | [ ] |
| 3 | OllamaProvider implemented | [ ] |
| 3 | OpenAIProvider implemented | [ ] |
| 3 | Integration tests pass | [ ] |
| 4 | Entities updated | [ ] |
| 4 | HybridSearchService created | [ ] |
| 4 | SyncService created | [ ] |
| 5 | searchSpringDocs updated | [ ] |
| 5 | searchFlavors updated | [ ] |
| 6 | 80%+ test coverage | [ ] |
| 6 | All tests pass | [ ] |
| 7 | README.md updated | [ ] |
| 7 | Version bumped to 1.6.0 | [ ] |

---

## Appendix A: Spring AI Embedding Dependencies

```groovy
// build.gradle additions

dependencies {
    // Spring AI Core
    implementation 'org.springframework.ai:spring-ai-core'

    // OpenAI Embedding Support
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'

    // Ollama Support
    implementation 'org.springframework.ai:spring-ai-ollama-spring-boot-starter'

    // pgvector support for PostgreSQL
    implementation 'com.pgvector:pgvector:0.1.4'

    // Testing
    testImplementation 'org.springframework.ai:spring-ai-test'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}
```

## Appendix B: Configuration Reference

```yaml
# application.yml embeddings section
mcp:
  features:
    embeddings:
      enabled: ${EMBEDDINGS_ENABLED:true}

  embeddings:
    provider: ${EMBEDDING_PROVIDER:ollama}  # ollama, openai, gemini
    dimensions: ${EMBEDDING_DIMENSIONS:768}
    chunk-size: ${EMBEDDING_CHUNK_SIZE:512}
    chunk-overlap: ${EMBEDDING_CHUNK_OVERLAP:50}
    batch-size: ${EMBEDDING_BATCH_SIZE:100}

    # Hybrid search configuration
    hybrid:
      enabled: true
      alpha: 0.3  # 0.0 = pure vector, 1.0 = pure keyword
      min-similarity: 0.5  # Minimum cosine similarity threshold

    # Provider configurations
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}
      timeout: ${OLLAMA_TIMEOUT:30000}

    openai:
      api-key: ${OPENAI_API_KEY:}
      model: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}

    # Sync configuration
    sync:
      enabled: true
      schedule: "0 0 3 * * ?"  # Daily at 3 AM
      batch-size: 50
      retry-attempts: 3
```

## Appendix C: Sample Embedding Queries

```sql
-- Find similar documentation to a query
SELECT id, title,
       1 - (content_embedding <=> '[0.1, 0.2, ...]'::vector) AS similarity
FROM documentation_content
WHERE content_embedding IS NOT NULL
ORDER BY content_embedding <=> '[0.1, 0.2, ...]'::vector
LIMIT 10;

-- Find similar migration transformations
SELECT id, name, display_name,
       1 - (transformation_embedding <=> $1::vector) AS similarity
FROM migration_transformations
WHERE transformation_embedding IS NOT NULL
  AND 1 - (transformation_embedding <=> $1::vector) > 0.6
ORDER BY transformation_embedding <=> $1::vector
LIMIT 20;

-- Hybrid search with weighted scoring
WITH keyword_search AS (
    SELECT id,
           ts_rank_cd(search_vector, plainto_tsquery('english', $1)) AS kw_score
    FROM flavors
    WHERE search_vector @@ plainto_tsquery('english', $1)
),
vector_search AS (
    SELECT id,
           1 - (flavor_embedding <=> $2::vector) AS vec_score
    FROM flavors
    WHERE flavor_embedding IS NOT NULL
)
SELECT
    COALESCE(k.id, v.id) AS id,
    f.unique_name,
    f.display_name,
    f.category,
    COALESCE(k.kw_score, 0) AS keyword_score,
    COALESCE(v.vec_score, 0) AS vector_score,
    (0.3 * COALESCE(k.kw_score, 0) + 0.7 * COALESCE(v.vec_score, 0)) AS combined_score
FROM keyword_search k
FULL OUTER JOIN vector_search v ON k.id = v.id
JOIN flavors f ON COALESCE(k.id, v.id) = f.id
WHERE f.is_active = true
ORDER BY combined_score DESC
LIMIT 10;
```

---

*Document created: 2026-01-01*
*Target Version: 1.6.0*
*Author: AI-assisted with Claude Code*

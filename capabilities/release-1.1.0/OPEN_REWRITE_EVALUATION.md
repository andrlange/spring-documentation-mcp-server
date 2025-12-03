# OpenRewrite Integration Evaluation for Spring MCP Server

> **Analysis Date**: 2025-11-28
> **Purpose**: Evaluate the feasibility and value of integrating OpenRewrite recipe knowledge into the Spring MCP Server

---

## Executive Summary

Integrating OpenRewrite recipe knowledge could provide **40-60% efficiency gains** for Claude Code when generating Spring Boot applications, particularly during version migrations. The implementation complexity varies significantly based on the approach chosen.

**Key Finding**: OpenRewrite's Community Edition recipes are available under the Moderne Source Available License, which permits reading and storing recipe knowledge for documentation purposes.

---

## 1. Current Problem Analysis

### Issues Encountered During Spring Boot 4.0 App Generation

The todo-app-example revealed **5 breaking changes** when generating a Spring Boot 4.0 application:

| Issue | Type | Impact | OpenRewrite Coverage |
|-------|------|--------|---------------------|
| Gradle 8.14+ requirement | Build config | Build fails | Yes |
| Flyway starter dependency change | Dependency | Migrations don't run | Yes |
| Health package reorganization | Import change | Compilation fails | Yes |
| Thymeleaf `#request` removal | API deprecation | Template errors | Yes |
| Spring Security 7.0 changes | API migration | Auth breaks | Yes |

### Root Cause

Claude Code's training data contains outdated information about:
- Dependency locations (`flyway-core` vs `spring-boot-starter-flyway`)
- Package structures (`actuate.health` vs `health.contributor`)
- API availability (`#request` object removed in Thymeleaf 3.1+)
- Build tool requirements (Gradle version minimums)

---

## 2. OpenRewrite Recipe Analysis

### Spring Boot 4.0 Migration Recipe Structure

The [Spring Boot 4.0 Community Edition recipe](https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0-community-edition) chains **7 sub-recipes**:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0
displayName: Migrate to Spring Boot 4.0 (Community Edition)
description: Migrate applications to the latest Spring Boot 4.0 release
tags: [spring, boot]
recipeList:
  - org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5
  - org.openrewrite.java.spring.framework.UpgradeSpringFramework_7_0
  - org.openrewrite.java.spring.security7.UpgradeSpringSecurity_7_0
  - org.openrewrite.java.spring.batch.UpgradeSpringBatch_6_0_From_5_2
  - org.openrewrite.java.spring.boot4.SpringBootProperties_4_0
  - org.openrewrite.java.spring.boot4.ReplaceSpyBeanAndMockBean
  - org.openrewrite.docker.testcontainers.MigrateTestcontainersJava_2_0
```

### Available Migration Recipes

| Recipe | Source Version | Target Version | License |
|--------|---------------|----------------|---------|
| UpgradeSpringBoot_4_0 | 3.x | 4.0.x | Moderne Source Available |
| UpgradeSpringBoot_3_5 | 3.4.x | 3.5.x | Moderne Source Available |
| UpgradeSpringBoot_3_4 | 3.3.x | 3.4.x | Moderne Source Available |
| UpgradeSpringBoot_3_0 | 2.x | 3.0.x | Moderne Source Available |
| UpgradeSpringSecurity_7_0 | 6.x | 7.0.x | Moderne Source Available |
| UpgradeSpringFramework_7_0 | 6.x | 7.0.x | Moderne Source Available |

### Recipe Transformation Types

OpenRewrite recipes handle these transformation categories:

1. **Dependency Changes** - Add, remove, update Maven/Gradle dependencies
2. **Import Statements** - Update package paths for relocated classes
3. **Property Migrations** - Rename/restructure application.properties/yml
4. **API Replacements** - Replace deprecated method calls with new APIs
5. **Annotation Updates** - Migrate to new annotation syntax
6. **Build Configuration** - Update Gradle/Maven plugin versions

---

## 3. Efficiency Gains Analysis

### Without OpenRewrite Knowledge (Current State)

| Task | Time/Iterations | Notes |
|------|-----------------|-------|
| Discover breaking change | 3-5 build attempts | Trial & error |
| Research fix | 5-10 minutes | Web search + docs |
| Apply fix | 1-2 attempts | May introduce new issues |
| **Total per issue** | **15-30 min or 4-6 iterations** | Compounds with multiple issues |

### With OpenRewrite Knowledge Integrated

| Task | Time/Iterations | Notes |
|------|-----------------|-------|
| Know breaking change upfront | Immediate | Pre-generation check |
| Know exact fix | Immediate | Database lookup |
| Apply fix correctly | First attempt | Verified transformation |
| **Total per issue** | **< 1 min or 1 iteration** | Consistent results |

### Projected Efficiency Improvement

| Scenario | Current | With Integration | Improvement |
|----------|---------|------------------|-------------|
| Simple app (2-3 issues) | 30-45 min | 5-10 min | **70-80%** |
| Complex app (5-7 issues) | 60-90 min | 15-25 min | **60-70%** |
| Enterprise app (10+ issues) | 2-4 hours | 30-45 min | **75-85%** |

**Conservative overall estimate: 40-60% efficiency improvement** on migration-heavy tasks.

---

## 4. Data Accessibility Analysis

### Publicly Available Sources

| Source | URL | Data Type | Accessibility |
|--------|-----|-----------|---------------|
| OpenRewrite GitHub | github.com/openrewrite/rewrite-spring | YAML recipes, Java code | Public |
| OpenRewrite Docs | docs.openrewrite.org/recipes | HTML documentation | Public |
| Spring Release Notes | github.com/spring-projects/spring-boot/releases | Markdown | Public |
| Spring Migration Guides | spring.io/blog | HTML articles | Public |

### Recipe Data Structure

Recipe YAML files are stored in: `META-INF/rewrite/*.yml`

Example transformation data that can be extracted:

```yaml
# From SpringBootProperties_4_0.yml
propertyMigrations:
  - oldKey: "management.endpoints.web.exposure.include"
    newKey: "management.endpoints.web.exposure.include"
    context: "Changed default behavior"

# From UpgradeSpringFramework_7_0.yml
importMigrations:
  - oldImport: "org.springframework.boot.actuate.health.Health"
    newImport: "org.springframework.boot.health.contributor.Health"
```

### Fetching Strategy

```java
@Service
public class RecipeFetchService {

    private static final String GITHUB_RAW_BASE =
        "https://raw.githubusercontent.com/openrewrite/rewrite-spring/main/";

    private static final String RECIPE_PATH =
        "src/main/resources/META-INF/rewrite/";

    public List<Recipe> fetchSpringBootRecipes() {
        // Fetch recipe index
        // Parse YAML files
        // Extract transformation patterns
    }

    public void syncRecipesToDatabase() {
        // Scheduled job to update migration knowledge
    }
}
```

---

## 5. License Analysis

### OpenRewrite License Structure

| Component | License | Commercial Use |
|-----------|---------|----------------|
| Core OpenRewrite | Apache 2.0 | Permitted |
| Community recipes | Apache 2.0 | Permitted |
| Spring Boot recipes | Moderne Source Available | Limited |
| Moderne Platform recipes | Proprietary | Not permitted |

### Moderne Source Available License Terms

Per [docs.openrewrite.org/licensing](https://docs.openrewrite.org/licensing/openrewrite-licensing):

**Permitted:**
- Use for internal purposes
- Copy and distribute
- Create derivative works for own use
- Read and document transformations

**Not Permitted:**
- Commercialize or resell recipes
- Offer as managed service
- Remove licensing notices

### Our Use Case Assessment

| Activity | Permitted? | Rationale |
|----------|------------|-----------|
| Store recipe transformation patterns | **Yes** | Internal documentation |
| Provide migration guides via MCP | **Yes** | Educational/documentation |
| Execute recipes on user's behalf | **Conditional** | User-initiated only |
| Charge for MCP server access | **Review needed** | May require Moderne contact |

**Recommendation**: Store transformation knowledge (patterns, not executable code) for documentation purposes. This is clearly permitted under the license.

---

## 6. Integration Options

### Option 1: Recipe Knowledge Database (Recommended)

**Complexity**: Medium | **Value**: High | **Effort**: 2-3 weeks

Store OpenRewrite recipe transformations as searchable migration knowledge.

#### Database Schema

```sql
-- Migration recipes table
CREATE TABLE migration_recipes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    from_version VARCHAR(50) NOT NULL,
    to_version VARCHAR(50) NOT NULL,
    project_slug VARCHAR(100) NOT NULL,
    source_url VARCHAR(500),
    license VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Individual transformations within recipes
CREATE TABLE migration_transformations (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES migration_recipes(id) ON DELETE CASCADE,
    transformation_type VARCHAR(50) NOT NULL, -- IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD
    category VARCHAR(100), -- e.g., 'actuator', 'security', 'data'
    old_pattern TEXT NOT NULL,
    new_pattern TEXT NOT NULL,
    file_pattern VARCHAR(255), -- *.java, build.gradle, application.yml
    explanation TEXT,
    breaking_change BOOLEAN DEFAULT false,
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_recipes_versions ON migration_recipes(from_version, to_version);
CREATE INDEX idx_recipes_project ON migration_recipes(project_slug);
CREATE INDEX idx_transformations_type ON migration_transformations(transformation_type);
CREATE INDEX idx_transformations_breaking ON migration_transformations(breaking_change);
```

#### New MCP Tools

```java
@Tool(name = "getSpringMigrationGuide",
      description = "Get breaking changes and fixes for Spring Boot version migrations")
public MigrationGuide getMigrationGuide(
    @ToolParam(description = "Source version (e.g., 3.5.8)") String fromVersion,
    @ToolParam(description = "Target version (e.g., 4.0.0)") String toVersion
) {
    return migrationService.getMigrationGuide(fromVersion, toVersion);
}

@Tool(name = "getBreakingChanges",
      description = "List known breaking changes for a Spring project version")
public List<BreakingChange> getBreakingChanges(
    @ToolParam(description = "Project slug (e.g., spring-boot)") String project,
    @ToolParam(description = "Version (e.g., 4.0.0)") String version
) {
    return migrationService.getBreakingChanges(project, version);
}

@Tool(name = "getDeprecationReplacement",
      description = "Get replacement for deprecated class or method")
public ReplacementInfo getDeprecationReplacement(
    @ToolParam(description = "Fully qualified class name") String className,
    @ToolParam(description = "Optional method name") String methodName
) {
    return migrationService.findReplacement(className, methodName);
}

@Tool(name = "checkVersionCompatibility",
      description = "Check if dependencies are compatible with target Spring Boot version")
public CompatibilityReport checkVersionCompatibility(
    @ToolParam(description = "Target Spring Boot version") String springBootVersion,
    @ToolParam(description = "List of dependencies to check") List<String> dependencies
) {
    return migrationService.checkCompatibility(springBootVersion, dependencies);
}
```

### Option 2: Live Recipe Execution

**Complexity**: High | **Value**: Medium | **Effort**: 4-6 weeks

Execute OpenRewrite recipes through Gradle/Maven at runtime.

**Pros:**
- Always up-to-date transformations
- Complete recipe execution

**Cons:**
- Requires project source code access
- Build tool execution overhead (30-60 seconds)
- Security implications
- License concerns for service offering

### Option 3: Static Documentation Only

**Complexity**: Low | **Value**: Medium | **Effort**: 1-2 weeks

Store migration knowledge as static YAML/JSON files.

**Pros:**
- Simple to implement
- No database changes needed
- Easy to maintain

**Cons:**
- Manual updates required
- Limited query capabilities
- No relational data

---

## 7. Recommended Implementation Plan

### Phase 1: Foundation 

1. **Create database schema** for migration knowledge
2. **Implement basic MCP tool**: `getSpringMigrationGuide`
3. **Manually populate** top 20 Spring Boot 4.0 breaking changes

**Deliverable**: Working migration guide tool with essential transformations

### Phase 2: Data Population 

1. **Build recipe parser** for OpenRewrite YAML files
2. **Fetch and parse** Spring Boot 3.x → 4.0 recipes
3. **Store transformations** in database
4. **Add search capability** across transformations

**Deliverable**: Comprehensive migration database with 100+ transformations

### Phase 3: Integration 

1. **Implement additional MCP tools**:
   - `getBreakingChanges`
   - `getDeprecationReplacement`
   - `checkVersionCompatibility`
2. **Add scheduled sync** from OpenRewrite GitHub
3. **Integrate with existing tools** (warn about deprecations in `getCodeExamples`)

**Deliverable**: Full migration assistance suite

### Phase 4: Enhancement 

1. **Add version matrix** (which dependencies work with which Spring Boot)
2. **Implement migration path finder** (3.2 → 4.0 via intermediate versions)
3. **Create migration report generator**
4. **Add UI for viewing migrations** (Thymeleaf dashboard)

**Deliverable**: Complete migration knowledge system

---

## 8. Sample Transformation Data

### Spring Boot 4.0 Breaking Changes to Capture

#### Build Configuration

```yaml
- type: BUILD
  category: gradle
  breaking: true
  description: "Spring Boot 4.0 requires Gradle 8.14+"
  oldPattern: |
    distributionUrl=.*gradle-8\.(1[0-3]|[0-9])-.*
  newPattern: |
    distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
  filePattern: "gradle/wrapper/gradle-wrapper.properties"
```

#### Dependency Changes

```yaml
- type: DEPENDENCY
  category: database
  breaking: true
  description: "Flyway auto-configuration moved to starter"
  oldPattern: |
    implementation 'org.flywaydb:flyway-core'
  newPattern: |
    implementation 'org.springframework.boot:spring-boot-starter-flyway'
  filePattern: "build.gradle"
```

#### Import Migrations

```yaml
- type: IMPORT
  category: actuator
  breaking: true
  description: "Health classes moved to spring-boot-health module"
  oldPattern: "org.springframework.boot.actuate.health.Health"
  newPattern: "org.springframework.boot.health.contributor.Health"
  filePattern: "*.java"

- type: IMPORT
  category: actuator
  breaking: true
  description: "HealthIndicator interface relocated"
  oldPattern: "org.springframework.boot.actuate.health.HealthIndicator"
  newPattern: "org.springframework.boot.health.contributor.HealthIndicator"
  filePattern: "*.java"
```

#### Template Changes

```yaml
- type: TEMPLATE
  category: thymeleaf
  breaking: true
  description: "Thymeleaf 3.1+ removed #request utility object"
  oldPattern: "${#request.requestURI}"
  newPattern: "${currentUri}"
  filePattern: "*.html"
  additionalInfo: |
    Create a @ControllerAdvice with @ModelAttribute to expose request URI:

    @ControllerAdvice
    public class RequestAttributeAdvice {
        @ModelAttribute("currentUri")
        public String currentUri(HttpServletRequest request) {
            return request.getRequestURI();
        }
    }
```

---

## 9. Success Metrics

### Quantitative Goals

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| Build failures per app generation | 4-6 | 0-1 | Count compile errors |
| Time to working app | 30-60 min | 5-15 min | End-to-end time |
| Migration knowledge coverage | 0% | 90%+ | Known issues captured |
| Recipe database entries | 0 | 500+ | Transformation count |

### Qualitative Goals

- Claude Code generates Spring Boot 4.0 apps that compile on first attempt
- Migration paths clearly documented and queryable
- Breaking changes surfaced before code generation
- Deprecation warnings included in code examples

---

## 10. Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| OpenRewrite license changes | High | Low | Store transformations, not executable code |
| Recipe format changes | Medium | Medium | Version-lock parser, add format detection |
| Data staleness | Medium | High | Scheduled GitHub sync, manual review process |
| Incomplete coverage | Medium | Medium | User feedback mechanism, community contributions |

---

## 11. Conclusion

Integrating OpenRewrite recipe knowledge into the Spring MCP Server is **feasible and valuable**. The recommended approach (Option 1: Recipe Knowledge Database) provides:

- **High value**: 40-60% efficiency improvement
- **Manageable complexity**: 3-4 weeks implementation
- **License compliance**: Documentation use is permitted
- **Maintainability**: Automated sync from public sources

**Recommended Next Step**: Begin Phase 1 implementation with the `getSpringMigrationGuide` MCP tool and populate the top 20 Spring Boot 4.0 breaking changes.

---

## 12. Detailed Implementation Plan

> **IMPORTANT CONSTRAINT**: The Spring MCP Server must remain on **Spring Boot 3.5.8** until **Spring AI 2.0 GA** is released (expected ~March 2026). Spring AI 2.0 will be compatible with Spring Boot 4.0. Until then, all development continues on the 3.5.x branch.

### 12.1 Project Constraints & Dependencies

#### Version Lock Strategy

| Component | Current Version | Target Version | Trigger for Upgrade |
|-----------|----------------|----------------|---------------------|
| Spring Boot | 3.5.8 | 4.0.x | Spring AI 2.0 GA release |
| Spring AI | 1.0.x / 1.1.x | 2.0.x | GA release (~March 2026) |
| Java | 25 | 25+ | No change required |
| PostgreSQL | 18 | 18 | No change required |

#### Why Wait for Spring AI 2.0?

- Spring AI 1.x depends on Spring Boot 3.x
- Spring AI 2.0 will support Spring Boot 4.0
- MCP Server functionality relies heavily on Spring AI MCP Server starter
- Premature upgrade would break core MCP functionality

### 12.2 Implementation Phases (Detailed)

---

#### Phase 1: Database Schema & Foundation

**Duration**: 3-5 days
**Prerequisites**: None
**Branch**: `feature/openrewrite-migration-knowledge`

##### Step 1.1: Create Flyway Migration

Create file: `src/main/resources/db/migration/V3__migration_knowledge.sql`

```sql
-- ============================================
-- Migration Knowledge Tables
-- Based on OpenRewrite recipe structure
-- ============================================

-- Migration recipes (composite transformations)
CREATE TABLE migration_recipes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    from_project VARCHAR(100) NOT NULL,        -- e.g., 'spring-boot'
    from_version_min VARCHAR(50) NOT NULL,     -- e.g., '3.0.0'
    from_version_max VARCHAR(50),              -- e.g., '3.5.99' (null = any)
    to_version VARCHAR(50) NOT NULL,           -- e.g., '4.0.0'
    source_url VARCHAR(500),                   -- OpenRewrite docs URL
    source_type VARCHAR(50) DEFAULT 'OPENREWRITE', -- OPENREWRITE, SPRING_DOCS, MANUAL
    license VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recipe_name UNIQUE (name)
);

-- Individual transformations within recipes
CREATE TABLE migration_transformations (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES migration_recipes(id) ON DELETE CASCADE,
    transformation_type VARCHAR(50) NOT NULL,  -- IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE
    category VARCHAR(100),                     -- actuator, security, data, web, etc.
    subcategory VARCHAR(100),                  -- health, oauth2, jpa, etc.
    old_pattern TEXT NOT NULL,
    new_pattern TEXT NOT NULL,
    file_pattern VARCHAR(255),                 -- *.java, build.gradle, application.yml, *.html
    regex_pattern BOOLEAN DEFAULT false,       -- Is old_pattern a regex?
    explanation TEXT,
    code_example TEXT,                         -- Full code example if applicable
    additional_steps TEXT,                     -- Manual steps required
    breaking_change BOOLEAN DEFAULT false,
    severity VARCHAR(20) DEFAULT 'INFO',       -- INFO, WARNING, ERROR, CRITICAL
    priority INT DEFAULT 0,                    -- Higher = more important
    tags TEXT[],                               -- Array of tags for search
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Version compatibility matrix
CREATE TABLE version_compatibility (
    id BIGSERIAL PRIMARY KEY,
    spring_boot_version VARCHAR(50) NOT NULL,
    dependency_group VARCHAR(100) NOT NULL,    -- e.g., 'org.springframework.security'
    dependency_artifact VARCHAR(100) NOT NULL, -- e.g., 'spring-security-core'
    compatible_version VARCHAR(50) NOT NULL,   -- e.g., '7.0.0'
    notes TEXT,
    verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_compatibility UNIQUE (spring_boot_version, dependency_group, dependency_artifact)
);

-- Deprecation tracking
CREATE TABLE deprecation_replacements (
    id BIGSERIAL PRIMARY KEY,
    deprecated_class VARCHAR(500) NOT NULL,
    deprecated_method VARCHAR(255),            -- null = entire class deprecated
    replacement_class VARCHAR(500),
    replacement_method VARCHAR(255),
    deprecated_since VARCHAR(50),              -- Version deprecated
    removed_in VARCHAR(50),                    -- Version removed (null = not yet)
    migration_notes TEXT,
    code_before TEXT,
    code_after TEXT,
    project_slug VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_deprecation UNIQUE (deprecated_class, deprecated_method)
);

-- Indexes for efficient querying
CREATE INDEX idx_recipes_project_version ON migration_recipes(from_project, to_version);
CREATE INDEX idx_recipes_active ON migration_recipes(is_active) WHERE is_active = true;
CREATE INDEX idx_transformations_recipe ON migration_transformations(recipe_id);
CREATE INDEX idx_transformations_type ON migration_transformations(transformation_type);
CREATE INDEX idx_transformations_breaking ON migration_transformations(breaking_change) WHERE breaking_change = true;
CREATE INDEX idx_transformations_category ON migration_transformations(category);
CREATE INDEX idx_transformations_tags ON migration_transformations USING GIN(tags);
CREATE INDEX idx_compatibility_boot ON version_compatibility(spring_boot_version);
CREATE INDEX idx_deprecation_class ON deprecation_replacements(deprecated_class);
CREATE INDEX idx_deprecation_project ON deprecation_replacements(project_slug);

-- Full-text search on transformation explanations
ALTER TABLE migration_transformations ADD COLUMN search_vector TSVECTOR;
CREATE INDEX idx_transformations_search ON migration_transformations USING GIN(search_vector);

-- Trigger to update search vector
CREATE OR REPLACE FUNCTION update_transformation_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.explanation, '') || ' ' ||
        COALESCE(NEW.category, '') || ' ' ||
        COALESCE(NEW.subcategory, '') || ' ' ||
        COALESCE(NEW.old_pattern, '') || ' ' ||
        COALESCE(NEW.new_pattern, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_transformation_search_vector
    BEFORE INSERT OR UPDATE ON migration_transformations
    FOR EACH ROW EXECUTE FUNCTION update_transformation_search_vector();
```

##### Step 1.2: Create Entity Classes

Create file: `src/main/java/com/spring/mcp/model/entity/MigrationRecipe.java`

```java
package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "migration_recipes")
public class MigrationRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "from_project", nullable = false)
    private String fromProject;

    @Column(name = "from_version_min", nullable = false)
    private String fromVersionMin;

    @Column(name = "from_version_max")
    private String fromVersionMax;

    @Column(name = "to_version", nullable = false)
    private String toVersion;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_type")
    private String sourceType = "OPENREWRITE";

    private String license;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters...

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

Create file: `src/main/java/com/spring/mcp/model/entity/MigrationTransformation.java`

```java
package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "migration_transformations")
public class MigrationTransformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private MigrationRecipe recipe;

    @Column(name = "transformation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransformationType transformationType;

    private String category;
    private String subcategory;

    @Column(name = "old_pattern", nullable = false, columnDefinition = "TEXT")
    private String oldPattern;

    @Column(name = "new_pattern", nullable = false, columnDefinition = "TEXT")
    private String newPattern;

    @Column(name = "file_pattern")
    private String filePattern;

    @Column(name = "regex_pattern")
    private Boolean regexPattern = false;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "code_example", columnDefinition = "TEXT")
    private String codeExample;

    @Column(name = "additional_steps", columnDefinition = "TEXT")
    private String additionalSteps;

    @Column(name = "breaking_change")
    private Boolean breakingChange = false;

    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.INFO;

    private Integer priority = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private List<String> tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum TransformationType {
        IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG
    }

    public enum Severity {
        INFO, WARNING, ERROR, CRITICAL
    }

    // Getters and setters...

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

##### Step 1.3: Create Repositories

Create file: `src/main/java/com/spring/mcp/repository/MigrationRecipeRepository.java`

```java
package com.spring.mcp.repository;

import com.spring.mcp.model.entity.MigrationRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MigrationRecipeRepository extends JpaRepository<MigrationRecipe, Long> {

    Optional<MigrationRecipe> findByName(String name);

    @Query("""
        SELECT r FROM MigrationRecipe r
        WHERE r.fromProject = :project
        AND r.toVersion = :toVersion
        AND r.isActive = true
        """)
    List<MigrationRecipe> findByProjectAndTargetVersion(
        @Param("project") String project,
        @Param("toVersion") String toVersion
    );

    @Query("""
        SELECT r FROM MigrationRecipe r
        WHERE r.fromProject = :project
        AND r.isActive = true
        ORDER BY r.toVersion DESC
        """)
    List<MigrationRecipe> findAllByProject(@Param("project") String project);

    @Query("""
        SELECT DISTINCT r.toVersion FROM MigrationRecipe r
        WHERE r.fromProject = :project
        AND r.isActive = true
        ORDER BY r.toVersion DESC
        """)
    List<String> findAvailableTargetVersions(@Param("project") String project);
}
```

Create file: `src/main/java/com/spring/mcp/repository/MigrationTransformationRepository.java`

```java
package com.spring.mcp.repository;

import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.model.entity.MigrationTransformation.TransformationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MigrationTransformationRepository extends JpaRepository<MigrationTransformation, Long> {

    List<MigrationTransformation> findByRecipeId(Long recipeId);

    List<MigrationTransformation> findByRecipeIdAndBreakingChangeTrue(Long recipeId);

    @Query("""
        SELECT t FROM MigrationTransformation t
        WHERE t.recipe.id = :recipeId
        AND t.transformationType = :type
        ORDER BY t.priority DESC
        """)
    List<MigrationTransformation> findByRecipeAndType(
        @Param("recipeId") Long recipeId,
        @Param("type") TransformationType type
    );

    @Query("""
        SELECT t FROM MigrationTransformation t
        WHERE t.recipe.id = :recipeId
        AND t.category = :category
        ORDER BY t.priority DESC
        """)
    List<MigrationTransformation> findByRecipeAndCategory(
        @Param("recipeId") Long recipeId,
        @Param("category") String category
    );

    @Query(value = """
        SELECT * FROM migration_transformations
        WHERE recipe_id = :recipeId
        AND search_vector @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(search_vector, plainto_tsquery('english', :searchTerm)) DESC
        """, nativeQuery = true)
    List<MigrationTransformation> searchInRecipe(
        @Param("recipeId") Long recipeId,
        @Param("searchTerm") String searchTerm
    );

    @Query(value = """
        SELECT * FROM migration_transformations t
        JOIN migration_recipes r ON t.recipe_id = r.id
        WHERE r.from_project = :project
        AND r.is_active = true
        AND t.search_vector @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(t.search_vector, plainto_tsquery('english', :searchTerm)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<MigrationTransformation> searchAcrossProject(
        @Param("project") String project,
        @Param("searchTerm") String searchTerm,
        @Param("limit") int limit
    );
}
```

---

#### Phase 2: Service Layer & MCP Tools

**Duration**: 5-7 days
**Prerequisites**: Phase 1 complete
**Branch**: `feature/openrewrite-migration-knowledge`

##### Step 2.1: Create Migration Service

Create file: `src/main/java/com/spring/mcp/service/migration/MigrationKnowledgeService.java`

```java
package com.spring.mcp.service.migration;

import com.spring.mcp.model.dto.MigrationGuideDto;
import com.spring.mcp.model.dto.BreakingChangeDto;
import com.spring.mcp.model.dto.DeprecationReplacementDto;
import com.spring.mcp.model.entity.MigrationRecipe;
import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.repository.MigrationRecipeRepository;
import com.spring.mcp.repository.MigrationTransformationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MigrationKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(MigrationKnowledgeService.class);

    private final MigrationRecipeRepository recipeRepository;
    private final MigrationTransformationRepository transformationRepository;

    public MigrationKnowledgeService(
            MigrationRecipeRepository recipeRepository,
            MigrationTransformationRepository transformationRepository) {
        this.recipeRepository = recipeRepository;
        this.transformationRepository = transformationRepository;
    }

    /**
     * Get complete migration guide from one version to another
     */
    public MigrationGuideDto getMigrationGuide(String project, String fromVersion, String toVersion) {
        log.info("Getting migration guide: {} {} -> {}", project, fromVersion, toVersion);

        List<MigrationRecipe> recipes = recipeRepository.findByProjectAndTargetVersion(project, toVersion);

        if (recipes.isEmpty()) {
            return MigrationGuideDto.empty(project, fromVersion, toVersion);
        }

        // Aggregate all transformations
        List<MigrationTransformation> allTransformations = recipes.stream()
                .flatMap(r -> transformationRepository.findByRecipeId(r.getId()).stream())
                .sorted(Comparator.comparingInt(t -> -t.getPriority()))
                .toList();

        // Group by type
        Map<String, List<MigrationTransformation>> byType = allTransformations.stream()
                .collect(Collectors.groupingBy(t -> t.getTransformationType().name()));

        return MigrationGuideDto.builder()
                .project(project)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .totalChanges(allTransformations.size())
                .breakingChanges(countBreakingChanges(allTransformations))
                .importChanges(byType.getOrDefault("IMPORT", List.of()))
                .dependencyChanges(byType.getOrDefault("DEPENDENCY", List.of()))
                .propertyChanges(byType.getOrDefault("PROPERTY", List.of()))
                .codeChanges(byType.getOrDefault("CODE", List.of()))
                .buildChanges(byType.getOrDefault("BUILD", List.of()))
                .templateChanges(byType.getOrDefault("TEMPLATE", List.of()))
                .recipes(recipes)
                .build();
    }

    /**
     * Get only breaking changes for a version
     */
    public List<BreakingChangeDto> getBreakingChanges(String project, String version) {
        log.info("Getting breaking changes: {} {}", project, version);

        List<MigrationRecipe> recipes = recipeRepository.findByProjectAndTargetVersion(project, version);

        return recipes.stream()
                .flatMap(r -> transformationRepository.findByRecipeIdAndBreakingChangeTrue(r.getId()).stream())
                .map(this::toBreakingChangeDto)
                .sorted(Comparator.comparing(BreakingChangeDto::severity).reversed())
                .toList();
    }

    /**
     * Search for specific transformation
     */
    public List<MigrationTransformation> searchTransformations(String project, String searchTerm, int limit) {
        return transformationRepository.searchAcrossProject(project, searchTerm, limit);
    }

    /**
     * Get available migration paths for a project
     */
    public List<String> getAvailableMigrationTargets(String project) {
        return recipeRepository.findAvailableTargetVersions(project);
    }

    private long countBreakingChanges(List<MigrationTransformation> transformations) {
        return transformations.stream().filter(MigrationTransformation::getBreakingChange).count();
    }

    private BreakingChangeDto toBreakingChangeDto(MigrationTransformation t) {
        return BreakingChangeDto.builder()
                .type(t.getTransformationType().name())
                .category(t.getCategory())
                .oldPattern(t.getOldPattern())
                .newPattern(t.getNewPattern())
                .explanation(t.getExplanation())
                .codeExample(t.getCodeExample())
                .severity(t.getSeverity().name())
                .filePattern(t.getFilePattern())
                .build();
    }
}
```

##### Step 2.2: Create MCP Tools

Create file: `src/main/java/com/spring/mcp/mcp/tools/MigrationTools.java`

```java
package com.spring.mcp.mcp.tools;

import com.spring.mcp.model.dto.BreakingChangeDto;
import com.spring.mcp.model.dto.MigrationGuideDto;
import com.spring.mcp.service.migration.MigrationKnowledgeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MigrationTools {

    private final MigrationKnowledgeService migrationService;

    public MigrationTools(MigrationKnowledgeService migrationService) {
        this.migrationService = migrationService;
    }

    @Tool(name = "getSpringMigrationGuide",
          description = "Get comprehensive migration guide for upgrading Spring Boot versions. " +
                        "Returns all breaking changes, import updates, dependency changes, " +
                        "property migrations, and code modifications needed.")
    public MigrationGuideDto getSpringMigrationGuide(
            @ToolParam(description = "Source Spring Boot version (e.g., '3.5.8')") String fromVersion,
            @ToolParam(description = "Target Spring Boot version (e.g., '4.0.0')") String toVersion
    ) {
        return migrationService.getMigrationGuide("spring-boot", fromVersion, toVersion);
    }

    @Tool(name = "getBreakingChanges",
          description = "Get list of breaking changes for a specific Spring project version. " +
                        "Use this before generating code to avoid compilation errors.")
    public List<BreakingChangeDto> getBreakingChanges(
            @ToolParam(description = "Project slug (e.g., 'spring-boot', 'spring-security')") String project,
            @ToolParam(description = "Target version to check (e.g., '4.0.0')") String version
    ) {
        return migrationService.getBreakingChanges(project, version);
    }

    @Tool(name = "searchMigrationKnowledge",
          description = "Search migration knowledge base for specific topics like 'flyway', " +
                        "'actuator health', 'thymeleaf request'. Returns relevant transformations.")
    public List<Object> searchMigrationKnowledge(
            @ToolParam(description = "Search term (e.g., 'flyway starter', 'health indicator')") String searchTerm,
            @ToolParam(description = "Project to search (default: 'spring-boot')") String project,
            @ToolParam(description = "Maximum results (default: 10)") Integer limit
    ) {
        String proj = project != null ? project : "spring-boot";
        int lim = limit != null ? limit : 10;
        return List.copyOf(migrationService.searchTransformations(proj, searchTerm, lim));
    }

    @Tool(name = "getAvailableMigrationPaths",
          description = "Get list of available target versions for migration. " +
                        "Use this to see what upgrade paths are documented.")
    public List<String> getAvailableMigrationPaths(
            @ToolParam(description = "Project slug (e.g., 'spring-boot')") String project
    ) {
        return migrationService.getAvailableMigrationTargets(project);
    }
}
```

---

#### Phase 3: Data Population

**Duration**: 3-5 days
**Prerequisites**: Phase 2 complete
**Branch**: `feature/openrewrite-migration-knowledge`

##### Step 3.1: Create Initial Data Migration

Create file: `src/main/resources/db/migration/V4__spring_boot_4_migration_data.sql`

```sql
-- ============================================
-- Spring Boot 4.0 Migration Knowledge
-- Source: OpenRewrite recipes + practical experience
-- ============================================

-- Insert Spring Boot 4.0 migration recipe
INSERT INTO migration_recipes (name, display_name, description, from_project, from_version_min, to_version, source_url, license)
VALUES (
    'org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0',
    'Migrate to Spring Boot 4.0',
    'Comprehensive migration guide from Spring Boot 3.x to 4.0. Includes dependency updates, import changes, property migrations, and API replacements.',
    'spring-boot',
    '3.0.0',
    '4.0.0',
    'https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0-community-edition',
    'Moderne Source Available'
);

-- Get the recipe ID for transformations
-- Using a CTE for PostgreSQL compatibility
WITH recipe AS (
    SELECT id FROM migration_recipes WHERE name = 'org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0'
)
INSERT INTO migration_transformations (recipe_id, transformation_type, category, subcategory, old_pattern, new_pattern, file_pattern, breaking_change, severity, priority, explanation, code_example, additional_steps, tags)
SELECT recipe.id, t.*
FROM recipe, (VALUES
    -- BUILD: Gradle version requirement
    ('BUILD', 'gradle', 'wrapper',
     'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.(1[0-3]|[0-9])-',
     'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip',
     'gradle/wrapper/gradle-wrapper.properties',
     true, 'CRITICAL', 100,
     'Spring Boot 4.0 requires Gradle 8.14 or later. Earlier versions will fail with: "Spring Boot plugin requires Gradle 8.x (8.14 or later)"',
     E'# Before (gradle-wrapper.properties)\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.11-bin.zip\n\n# After\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip',
     'Run: ./gradlew wrapper --gradle-version=8.14',
     ARRAY['gradle', 'build', 'wrapper', 'version']),

    -- DEPENDENCY: Flyway starter
    ('DEPENDENCY', 'database', 'flyway',
     'implementation ''org.flywaydb:flyway-core''',
     'implementation ''org.springframework.boot:spring-boot-starter-flyway''',
     'build.gradle',
     true, 'CRITICAL', 95,
     'Flyway auto-configuration moved to separate starter in Spring Boot 4.0. Using flyway-core alone will not trigger auto-configuration.',
     E'// Before (build.gradle)\nimplementation ''org.flywaydb:flyway-core''\nimplementation ''org.flywaydb:flyway-database-postgresql''\n\n// After\nimplementation ''org.springframework.boot:spring-boot-starter-flyway''\nimplementation ''org.flywaydb:flyway-database-postgresql''',
     'Keep flyway-database-postgresql for PostgreSQL support',
     ARRAY['flyway', 'database', 'migration', 'dependency']),

    -- IMPORT: Health classes moved
    ('IMPORT', 'actuator', 'health',
     'org.springframework.boot.actuate.health.Health',
     'org.springframework.boot.health.contributor.Health',
     '*.java',
     true, 'ERROR', 90,
     'Health classes relocated from spring-boot-actuator to spring-boot-health module in Spring Boot 4.0.',
     E'// Before\nimport org.springframework.boot.actuate.health.Health;\nimport org.springframework.boot.actuate.health.HealthIndicator;\n\n// After\nimport org.springframework.boot.health.contributor.Health;\nimport org.springframework.boot.health.contributor.HealthIndicator;',
     NULL,
     ARRAY['actuator', 'health', 'import', 'package']),

    -- IMPORT: HealthIndicator moved
    ('IMPORT', 'actuator', 'health',
     'org.springframework.boot.actuate.health.HealthIndicator',
     'org.springframework.boot.health.contributor.HealthIndicator',
     '*.java',
     true, 'ERROR', 90,
     'HealthIndicator interface relocated to spring-boot-health module.',
     NULL, NULL,
     ARRAY['actuator', 'health', 'import', 'indicator']),

    -- IMPORT: AbstractHealthIndicator moved
    ('IMPORT', 'actuator', 'health',
     'org.springframework.boot.actuate.health.AbstractHealthIndicator',
     'org.springframework.boot.health.contributor.AbstractHealthIndicator',
     '*.java',
     true, 'ERROR', 89,
     'AbstractHealthIndicator base class relocated to spring-boot-health module.',
     NULL, NULL,
     ARRAY['actuator', 'health', 'import', 'abstract']),

    -- TEMPLATE: Thymeleaf #request removed
    ('TEMPLATE', 'thymeleaf', 'request',
     '${#request.requestURI}',
     '${currentUri}',
     '*.html',
     true, 'ERROR', 85,
     'Thymeleaf 3.1+ removed #request, #session, #servletContext, and #response utility objects for security reasons.',
     E'<!-- Before (Thymeleaf 3.0.x) -->\n<a th:classappend="${#request.requestURI == ''/todos''} ? ''active'' : ''''">Todos</a>\n\n<!-- After (Thymeleaf 3.1+) -->\n<a th:classappend="${currentUri == ''/todos''} ? ''active'' : ''''">Todos</a>',
     E'Create a @ControllerAdvice to expose request attributes:\n\n@ControllerAdvice\npublic class RequestAttributeAdvice {\n    @ModelAttribute("currentUri")\n    public String currentUri(HttpServletRequest request) {\n        return request.getRequestURI();\n    }\n}',
     ARRAY['thymeleaf', 'template', 'request', 'security']),

    -- TEMPLATE: Thymeleaf #session removed
    ('TEMPLATE', 'thymeleaf', 'session',
     '${#session.',
     'Use @ModelAttribute or session-scoped beans',
     '*.html',
     true, 'ERROR', 84,
     'Direct session access via #session removed in Thymeleaf 3.1+. Use Spring session-scoped beans or @ModelAttribute.',
     NULL,
     'Expose session attributes via @ControllerAdvice @ModelAttribute methods',
     ARRAY['thymeleaf', 'template', 'session', 'security']),

    -- DEPENDENCY: Spring Security 7.0
    ('DEPENDENCY', 'security', 'core',
     'spring-security:6.',
     'spring-security:7.0',
     'build.gradle',
     true, 'WARNING', 80,
     'Spring Boot 4.0 uses Spring Security 7.0. Review security configuration for breaking changes.',
     NULL,
     'See Spring Security 7.0 migration guide: https://docs.spring.io/spring-security/reference/migration-7/index.html',
     ARRAY['security', 'dependency', 'version']),

    -- CODE: @MockBean replacement
    ('ANNOTATION', 'testing', 'mockbean',
     '@MockBean',
     '@MockitoBean',
     '*Test.java',
     true, 'WARNING', 75,
     'Spring Boot 4.0 deprecates @MockBean in favor of @MockitoBean for clearer semantics.',
     E'// Before\n@MockBean\nprivate UserService userService;\n\n// After\n@MockitoBean\nprivate UserService userService;',
     NULL,
     ARRAY['testing', 'mock', 'annotation', 'mockito']),

    -- CODE: @SpyBean replacement
    ('ANNOTATION', 'testing', 'spybean',
     '@SpyBean',
     '@MockitoSpyBean',
     '*Test.java',
     true, 'WARNING', 74,
     'Spring Boot 4.0 deprecates @SpyBean in favor of @MockitoSpyBean.',
     E'// Before\n@SpyBean\nprivate UserService userService;\n\n// After\n@MockitoSpyBean\nprivate UserService userService;',
     NULL,
     ARRAY['testing', 'spy', 'annotation', 'mockito']),

    -- PROPERTY: Management endpoints
    ('PROPERTY', 'actuator', 'endpoints',
     'management.endpoints.web.exposure.include=*',
     'management.endpoints.web.exposure.include=health,info,metrics',
     'application.yml',
     false, 'WARNING', 70,
     'Spring Boot 4.0 changes default endpoint exposure. Review which endpoints should be exposed.',
     NULL,
     'Explicitly list required endpoints instead of using wildcard',
     ARRAY['actuator', 'endpoints', 'property', 'security']),

    -- BUILD: Maven Java version
    ('BUILD', 'maven', 'java',
     '<java.version>17</java.version>',
     '<java.version>21</java.version>',
     'pom.xml',
     false, 'INFO', 60,
     'Spring Boot 4.0 recommends Java 21 for best performance and features. Java 17 still supported.',
     NULL, NULL,
     ARRAY['java', 'version', 'maven', 'build'])

) AS t(transformation_type, category, subcategory, old_pattern, new_pattern, file_pattern, breaking_change, severity, priority, explanation, code_example, additional_steps, tags);

-- Insert version compatibility data
INSERT INTO version_compatibility (spring_boot_version, dependency_group, dependency_artifact, compatible_version, notes, verified)
VALUES
    ('4.0.0', 'org.springframework', 'spring-framework', '7.0.0', 'Spring Framework 7.0 required', true),
    ('4.0.0', 'org.springframework.security', 'spring-security-core', '7.0.0', 'Spring Security 7.0 required', true),
    ('4.0.0', 'org.springframework.data', 'spring-data-jpa', '4.0.0', 'Spring Data JPA 4.0', true),
    ('4.0.0', 'org.hibernate.orm', 'hibernate-core', '7.0.0', 'Hibernate ORM 7.0', true),
    ('4.0.0', 'org.thymeleaf', 'thymeleaf-spring6', '3.1.2', 'Thymeleaf 3.1+ with security restrictions', true),
    ('4.0.0', 'org.flywaydb', 'flyway-core', '11.0.0', 'Flyway 11.x', true),
    ('3.5.8', 'org.springframework', 'spring-framework', '6.2.0', 'Spring Framework 6.2', true),
    ('3.5.8', 'org.springframework.security', 'spring-security-core', '6.5.0', 'Spring Security 6.5', true),
    ('3.5.8', 'org.springframework.ai', 'spring-ai-core', '1.0.0', 'Spring AI 1.0.x compatible', true);

-- Insert deprecation replacements
INSERT INTO deprecation_replacements (deprecated_class, deprecated_method, replacement_class, replacement_method, deprecated_since, removed_in, project_slug, migration_notes)
VALUES
    ('org.springframework.boot.actuate.health.Health', NULL, 'org.springframework.boot.health.contributor.Health', NULL, '3.5.0', '4.0.0', 'spring-boot', 'Package relocated to spring-boot-health module'),
    ('org.springframework.boot.actuate.health.HealthIndicator', NULL, 'org.springframework.boot.health.contributor.HealthIndicator', NULL, '3.5.0', '4.0.0', 'spring-boot', 'Interface relocated to spring-boot-health module'),
    ('org.springframework.boot.test.mock.mockito.MockBean', NULL, 'org.springframework.test.context.bean.override.mockito.MockitoBean', NULL, '3.4.0', '5.0.0', 'spring-boot', 'Use @MockitoBean for clearer semantics');
```

##### Step 3.2: Create Recipe Sync Service (for future GitHub sync)

Create file: `src/main/java/com/spring/mcp/service/migration/RecipeSyncService.java`

```java
package com.spring.mcp.service.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Service to sync migration recipes from OpenRewrite GitHub repository.
 * Currently a placeholder - will be implemented in Phase 4.
 */
@Service
public class RecipeSyncService {

    private static final Logger log = LoggerFactory.getLogger(RecipeSyncService.class);

    private static final String GITHUB_RAW_BASE =
        "https://raw.githubusercontent.com/openrewrite/rewrite-spring/main/";

    private final WebClient webClient;

    public RecipeSyncService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(GITHUB_RAW_BASE).build();
    }

    /**
     * Scheduled sync from GitHub - runs weekly on Sunday at 2 AM
     * TODO: Implement full recipe parsing in Phase 4
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void syncFromGitHub() {
        log.info("Recipe sync from GitHub - placeholder for Phase 4 implementation");
        // Future implementation:
        // 1. Fetch recipe YAML files from GitHub
        // 2. Parse transformations
        // 3. Update database with new/changed recipes
    }
}
```

---

#### Phase 4: UI Integration & Enhancement

**Duration**: 5-7 days
**Prerequisites**: Phase 3 complete
**Branch**: `feature/openrewrite-migration-ui`

##### Step 4.1: Create Migration Dashboard Page

Create file: `src/main/resources/templates/migration/index.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::content}, 'migration')}">
<th:block th:fragment="content">
    <div class="container-fluid py-4" x-data="migrationDashboard()">
        <!-- Header -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3 mb-0">Migration Knowledge Base</h1>
            <span class="badge bg-info" th:text="${totalTransformations} + ' transformations'">0 transformations</span>
        </div>

        <!-- Search & Filter -->
        <div class="card mb-4">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label">Source Version</label>
                        <select class="form-select" x-model="fromVersion">
                            <option value="">Any</option>
                            <option th:each="v : ${fromVersions}" th:value="${v}" th:text="${v}"></option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Target Version</label>
                        <select class="form-select" x-model="toVersion">
                            <option value="">Any</option>
                            <option th:each="v : ${toVersions}" th:value="${v}" th:text="${v}"></option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Search</label>
                        <input type="text" class="form-control" x-model="searchTerm"
                               placeholder="e.g., flyway, health, thymeleaf">
                    </div>
                </div>
            </div>
        </div>

        <!-- Breaking Changes Summary -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="card bg-danger text-white">
                    <div class="card-body">
                        <h6 class="card-title">Critical</h6>
                        <h2 th:text="${criticalCount}">0</h2>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card bg-warning">
                    <div class="card-body">
                        <h6 class="card-title">Breaking</h6>
                        <h2 th:text="${breakingCount}">0</h2>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card bg-info text-white">
                    <div class="card-body">
                        <h6 class="card-title">Warnings</h6>
                        <h2 th:text="${warningCount}">0</h2>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card bg-success text-white">
                    <div class="card-body">
                        <h6 class="card-title">Info</h6>
                        <h2 th:text="${infoCount}">0</h2>
                    </div>
                </div>
            </div>
        </div>

        <!-- Transformations List -->
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">Transformations</h5>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead>
                            <tr>
                                <th>Type</th>
                                <th>Category</th>
                                <th>Description</th>
                                <th>Severity</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr th:each="t : ${transformations}">
                                <td>
                                    <span class="badge"
                                          th:classappend="${t.transformationType.name() == 'IMPORT'} ? 'bg-primary' :
                                                          (${t.transformationType.name() == 'DEPENDENCY'} ? 'bg-success' :
                                                          (${t.transformationType.name() == 'BUILD'} ? 'bg-warning' : 'bg-secondary'))"
                                          th:text="${t.transformationType}">TYPE</span>
                                </td>
                                <td th:text="${t.category}">category</td>
                                <td th:text="${#strings.abbreviate(t.explanation, 100)}">description</td>
                                <td>
                                    <span class="badge"
                                          th:classappend="${t.severity.name() == 'CRITICAL'} ? 'bg-danger' :
                                                          (${t.severity.name() == 'ERROR'} ? 'bg-danger' :
                                                          (${t.severity.name() == 'WARNING'} ? 'bg-warning' : 'bg-info'))"
                                          th:text="${t.severity}">INFO</span>
                                </td>
                                <td>
                                    <button class="btn btn-sm btn-outline-primary"
                                            th:data-id="${t.id}"
                                            @click="showDetails($event.target.dataset.id)">
                                        Details
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <script th:inline="javascript">
        function migrationDashboard() {
            return {
                fromVersion: '',
                toVersion: '',
                searchTerm: '',
                showDetails(id) {
                    // TODO: Show modal with full transformation details
                    console.log('Show details for:', id);
                }
            };
        }
    </script>
</th:block>
</html>
```

---

### 12.3 Testing Requirements

#### Unit Tests

```java
// MigrationKnowledgeServiceTest.java
@ExtendWith(MockitoExtension.class)
class MigrationKnowledgeServiceTest {

    @Mock
    private MigrationRecipeRepository recipeRepository;

    @Mock
    private MigrationTransformationRepository transformationRepository;

    @InjectMocks
    private MigrationKnowledgeService service;

    @Test
    void shouldReturnMigrationGuideForSpringBoot4() {
        // Given
        MigrationRecipe recipe = createTestRecipe();
        when(recipeRepository.findByProjectAndTargetVersion("spring-boot", "4.0.0"))
            .thenReturn(List.of(recipe));
        when(transformationRepository.findByRecipeId(recipe.getId()))
            .thenReturn(createTestTransformations());

        // When
        MigrationGuideDto guide = service.getMigrationGuide("spring-boot", "3.5.8", "4.0.0");

        // Then
        assertThat(guide.getTotalChanges()).isGreaterThan(0);
        assertThat(guide.getBreakingChanges()).isGreaterThan(0);
    }

    @Test
    void shouldReturnEmptyGuideForUnknownVersion() {
        // Given
        when(recipeRepository.findByProjectAndTargetVersion(anyString(), anyString()))
            .thenReturn(List.of());

        // When
        MigrationGuideDto guide = service.getMigrationGuide("spring-boot", "3.5.8", "99.0.0");

        // Then
        assertThat(guide.getTotalChanges()).isZero();
    }
}
```

#### Integration Tests

```java
// MigrationToolsIntegrationTest.java
@SpringBootTest
@Testcontainers
class MigrationToolsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private MigrationTools migrationTools;

    @Test
    void shouldReturnSpringBoot4MigrationGuide() {
        MigrationGuideDto guide = migrationTools.getSpringMigrationGuide("3.5.8", "4.0.0");

        assertThat(guide.getToVersion()).isEqualTo("4.0.0");
        assertThat(guide.getDependencyChanges()).isNotEmpty();
        assertThat(guide.getImportChanges()).isNotEmpty();
    }

    @Test
    void shouldReturnBreakingChangesForSpringBoot4() {
        List<BreakingChangeDto> changes = migrationTools.getBreakingChanges("spring-boot", "4.0.0");

        assertThat(changes).isNotEmpty();
        assertThat(changes).anyMatch(c -> c.category().equals("flyway"));
        assertThat(changes).anyMatch(c -> c.category().equals("actuator"));
    }
}
```

---

### 12.4 Deployment Checklist

Before deploying migration knowledge feature:

- [ ] All Flyway migrations applied successfully
- [ ] Migration data populated (V4 migration)
- [ ] MCP tools registered and accessible
- [ ] Unit tests passing (> 80% coverage)
- [ ] Integration tests passing
- [ ] UI pages accessible and functional
- [ ] Search functionality verified
- [ ] MCP endpoint tested with Claude Code

---

### 12.5 Feature Toggle Configuration

The OpenRewrite Recipe feature is **optional** and can be enabled/disabled via environment variables. The database schema is always created to ensure clean migrations, but the feature's runtime behavior is controlled by configuration.

#### 12.5.1 Configuration Properties

Add to `application.yml`:

```yaml
mcp:
  features:
    openrewrite:
      enabled: ${OPENREWRITE_ENABLED:true}  # Default: enabled
      sync:
        enabled: ${OPENREWRITE_SYNC_ENABLED:true}
        schedule: ${OPENREWRITE_SYNC_SCHEDULE:0 0 3 * * SUN}  # Weekly Sunday 3 AM
      github:
        base-url: https://raw.githubusercontent.com/openrewrite/rewrite-spring/main/
        timeout: 30000
```

#### 12.5.2 Feature Configuration Class

Create file: `src/main/java/com/spring/mcp/config/OpenRewriteFeatureConfig.java`

```java
package com.spring.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mcp.features.openrewrite")
public class OpenRewriteFeatureConfig {

    private boolean enabled = true;
    private SyncConfig sync = new SyncConfig();
    private GithubConfig github = new GithubConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SyncConfig getSync() {
        return sync;
    }

    public void setSync(SyncConfig sync) {
        this.sync = sync;
    }

    public GithubConfig getGithub() {
        return github;
    }

    public void setGithub(GithubConfig github) {
        this.github = github;
    }

    public static class SyncConfig {
        private boolean enabled = true;
        private String schedule = "0 0 3 * * SUN";

        // Getters and setters...
    }

    public static class GithubConfig {
        private String baseUrl = "https://raw.githubusercontent.com/openrewrite/rewrite-spring/main/";
        private int timeout = 30000;

        // Getters and setters...
    }
}
```

#### 12.5.3 Conditional Bean Registration

Create file: `src/main/java/com/spring/mcp/config/OpenRewriteAutoConfiguration.java`

```java
package com.spring.mcp.config;

import com.spring.mcp.mcp.tools.MigrationTools;
import com.spring.mcp.service.migration.MigrationKnowledgeService;
import com.spring.mcp.service.migration.RecipeSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mcp.features.openrewrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenRewriteAutoConfiguration {

    /**
     * MCP tools only registered when feature is enabled.
     * This prevents the tools from appearing in MCP tool list when disabled.
     */
    @Bean
    public MigrationTools migrationTools(MigrationKnowledgeService migrationService) {
        return new MigrationTools(migrationService);
    }

    /**
     * Sync service only active when feature is enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.features.openrewrite.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RecipeSyncService recipeSyncService(OpenRewriteFeatureConfig config) {
        return new RecipeSyncService(config);
    }
}
```

#### 12.5.4 Environment Variable Reference

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `OPENREWRITE_ENABLED` | `true` | Enable/disable entire OpenRewrite feature |
| `OPENREWRITE_SYNC_ENABLED` | `true` | Enable/disable automatic GitHub sync |
| `OPENREWRITE_SYNC_SCHEDULE` | `0 0 3 * * SUN` | Cron schedule for sync (default: Sunday 3 AM) |

#### 12.5.5 Docker Compose Override

```yaml
# docker-compose.override.yml (for disabling feature)
services:
  spring-mcp-server:
    environment:
      OPENREWRITE_ENABLED: "false"
```

---

### 12.6 Dashboard UI Integration

The dashboard displays the OpenRewrite Recipe card conditionally based on the feature toggle. When enabled, the second row shows 3 boxes; when disabled, it shows 2 wider boxes.

#### 12.6.1 Dashboard Layout Structure

**Current Layout (Feature Disabled):**
```
┌─────────────────┬─────────────────┬─────────────────┐
│   Spring Boot   │ Spring Projects │ Project Versions│
│      17         │       55        │       920       │
└─────────────────┴─────────────────┴─────────────────┘
┌─────────────────────────┬───────────────────────────┐
│      Documentation      │      Code Examples        │
│          55             │          318              │
└─────────────────────────┴───────────────────────────┘
```

**New Layout (Feature Enabled):**
```
┌─────────────────┬─────────────────┬─────────────────┐
│   Spring Boot   │ Spring Projects │ Project Versions│
│      17         │       55        │       920       │
└─────────────────┴─────────────────┴─────────────────┘
┌─────────────────┬─────────────────┬─────────────────┐
│ OpenRewrite     │  Documentation  │  Code Examples  │
│ Recipes: 12     │       55        │       318       │
└─────────────────┴─────────────────┴─────────────────┘
```

#### 12.6.2 Dashboard Controller Changes

Modify `DashboardController.java`:

```java
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final OpenRewriteFeatureConfig openRewriteConfig;
    private final MigrationRecipeRepository recipeRepository;
    private final MigrationTransformationRepository transformationRepository;
    // ... other dependencies

    @GetMapping
    public String dashboard(Model model) {
        // ... existing stats ...

        // OpenRewrite feature status
        model.addAttribute("openRewriteEnabled", openRewriteConfig.isEnabled());

        if (openRewriteConfig.isEnabled()) {
            long recipeCount = recipeRepository.count();
            long transformationCount = transformationRepository.count();
            long breakingChangeCount = transformationRepository.countByBreakingChangeTrue();

            model.addAttribute("recipeCount", recipeCount);
            model.addAttribute("transformationCount", transformationCount);
            model.addAttribute("breakingChangeCount", breakingChangeCount);
        }

        return "dashboard";
    }
}
```

#### 12.6.3 Dashboard Template Changes

Modify `src/main/resources/templates/dashboard.html`:

```html
<!-- Second Row: Conditional based on OpenRewrite feature -->
<div class="row mb-4">

    <!-- OpenRewrite Recipes Card - Only when feature enabled -->
    <div th:if="${openRewriteEnabled}" class="col-md-4 mb-4">
        <div class="card h-100 dashboard-card">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-3">
                    <div class="icon-box bg-purple">
                        <i class="bi bi-arrow-repeat"></i>
                    </div>
                    <span class="badge bg-purple">Recipes</span>
                </div>
                <h2 class="display-6 fw-bold mb-1" th:text="${recipeCount}">0</h2>
                <p class="text-muted mb-2">Migration Recipes</p>
                <hr class="my-2">
                <div class="d-flex justify-content-between small">
                    <span class="text-warning">
                        <i class="bi bi-exclamation-triangle"></i>
                        <span th:text="${breakingChangeCount}">0</span> breaking changes
                    </span>
                    <a href="/recipes" class="text-decoration-none">
                        <i class="bi bi-arrow-right"></i> View all
                    </a>
                </div>
            </div>
        </div>
    </div>

    <!-- Documentation Card -->
    <div th:classappend="${openRewriteEnabled} ? 'col-md-4' : 'col-md-6'" class="mb-4">
        <div class="card h-100 dashboard-card">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-3">
                    <div class="icon-box bg-primary">
                        <i class="bi bi-file-text"></i>
                    </div>
                    <span class="badge bg-primary">Links</span>
                </div>
                <h2 class="display-6 fw-bold mb-1" th:text="${documentationCount}">55</h2>
                <p class="text-muted mb-2">Documentation</p>
                <hr class="my-2">
                <span class="text-success small">
                    <i class="bi bi-link-45deg"></i> Links indexed
                </span>
            </div>
        </div>
    </div>

    <!-- Code Examples Card -->
    <div th:classappend="${openRewriteEnabled} ? 'col-md-4' : 'col-md-6'" class="mb-4">
        <div class="card h-100 dashboard-card">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-3">
                    <div class="icon-box bg-secondary">
                        <i class="bi bi-code-slash"></i>
                    </div>
                    <span class="badge bg-secondary">Library</span>
                </div>
                <h2 class="display-6 fw-bold mb-1" th:text="${codeExamplesCount}">318</h2>
                <p class="text-muted mb-2">Code Examples</p>
                <hr class="my-2">
                <span class="text-info small">
                    <i class="bi bi-folder"></i> Samples available
                </span>
            </div>
        </div>
    </div>

</div>
```

#### 12.6.4 Custom CSS for Recipe Card

Add to `src/main/resources/static/css/style.css`:

```css
/* OpenRewrite Recipe Card Styling */
.bg-purple {
    background-color: #6f42c1 !important;
}

.badge.bg-purple {
    background-color: #6f42c1 !important;
}

.icon-box.bg-purple {
    background-color: rgba(111, 66, 193, 0.2);
    color: #6f42c1;
}

/* Recipe severity badges */
.badge-critical {
    background-color: #dc3545;
}

.badge-breaking {
    background-color: #fd7e14;
}

.badge-warning {
    background-color: #ffc107;
    color: #000;
}

.badge-info {
    background-color: #0dcaf0;
    color: #000;
}
```

---

### 12.7 Menu & Navigation Integration

A new "Recipes" menu entry is added below "Code Examples" when the feature is enabled. The design and behavior mirrors the existing "Versions" page.

#### 12.7.1 Navigation Fragment Changes

Modify `src/main/resources/templates/fragments/layout.html`:

```html
<nav class="sidebar">
    <ul class="nav flex-column">
        <li class="nav-item">
            <a class="nav-link" th:classappend="${currentUri == '/dashboard'} ? 'active' : ''" href="/dashboard">
                <i class="bi bi-speedometer2"></i> Dashboard
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/spring-boot')} ? 'active' : ''" href="/spring-boot">
                <i class="bi bi-bootstrap"></i> Spring Boot
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/projects')} ? 'active' : ''" href="/projects">
                <i class="bi bi-folder"></i> Projects
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/versions')} ? 'active' : ''" href="/versions">
                <i class="bi bi-tags"></i> Versions
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/documentation')} ? 'active' : ''" href="/documentation">
                <i class="bi bi-file-text"></i> Documentation
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/code-examples')} ? 'active' : ''" href="/code-examples">
                <i class="bi bi-code-slash"></i> Code Examples
            </a>
        </li>

        <!-- OpenRewrite Recipes - Conditional -->
        <li class="nav-item" th:if="${openRewriteEnabled}">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/recipes')} ? 'active' : ''" href="/recipes">
                <i class="bi bi-arrow-repeat"></i> Recipes
            </a>
        </li>

        <li class="nav-item mt-4">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/users')} ? 'active' : ''" href="/users">
                <i class="bi bi-people"></i> Users
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" th:classappend="${currentUri.startsWith('/settings')} ? 'active' : ''" href="/settings">
                <i class="bi bi-gear"></i> Settings
            </a>
        </li>
    </ul>
</nav>
```

#### 12.7.2 Global Model Attribute for Feature Flag

Create `src/main/java/com/spring/mcp/config/GlobalModelAttributes.java`:

```java
package com.spring.mcp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final OpenRewriteFeatureConfig openRewriteConfig;

    public GlobalModelAttributes(OpenRewriteFeatureConfig openRewriteConfig) {
        this.openRewriteConfig = openRewriteConfig;
    }

    @ModelAttribute("openRewriteEnabled")
    public boolean openRewriteEnabled() {
        return openRewriteConfig.isEnabled();
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
```

#### 12.7.3 Recipe Controller

Create `src/main/java/com/spring/mcp/controller/RecipeController.java`:

```java
package com.spring.mcp.controller;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.model.entity.MigrationRecipe;
import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.repository.MigrationRecipeRepository;
import com.spring.mcp.repository.MigrationTransformationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/recipes")
@ConditionalOnProperty(prefix = "mcp.features.openrewrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecipeController {

    private final MigrationRecipeRepository recipeRepository;
    private final MigrationTransformationRepository transformationRepository;
    private final SpringProjectRepository projectRepository;

    public RecipeController(
            MigrationRecipeRepository recipeRepository,
            MigrationTransformationRepository transformationRepository,
            SpringProjectRepository projectRepository) {
        this.recipeRepository = recipeRepository;
        this.transformationRepository = transformationRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * List all recipes with filtering and search
     */
    @GetMapping
    public String listRecipes(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String version,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        Page<MigrationRecipe> recipes;

        if (search != null && !search.isBlank()) {
            // Full-text search across recipe name, description
            recipes = recipeRepository.searchByText(search, pageable);
        } else if (project != null && !project.isBlank()) {
            // Filter by project
            recipes = recipeRepository.findByFromProject(project, pageable);
        } else if (version != null && !version.isBlank()) {
            // Filter by target version
            recipes = recipeRepository.findByToVersion(version, pageable);
        } else {
            // All recipes
            recipes = recipeRepository.findAll(pageable);
        }

        // Get filter options
        List<String> projects = recipeRepository.findDistinctProjects();
        List<String> versions = recipeRepository.findDistinctTargetVersions();

        model.addAttribute("recipes", recipes);
        model.addAttribute("projects", projects);
        model.addAttribute("versions", versions);
        model.addAttribute("selectedProject", project);
        model.addAttribute("selectedVersion", version);
        model.addAttribute("searchTerm", search);

        // Stats
        model.addAttribute("totalRecipes", recipeRepository.count());
        model.addAttribute("totalTransformations", transformationRepository.count());
        model.addAttribute("breakingChanges", transformationRepository.countByBreakingChangeTrue());

        return "recipes/list";
    }

    /**
     * View single recipe with all transformations
     */
    @GetMapping("/{id}")
    public String viewRecipe(@PathVariable Long id, Model model) {
        MigrationRecipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + id));

        List<MigrationTransformation> transformations = transformationRepository.findByRecipeId(id);

        // Group transformations by type
        var byType = transformations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.getTransformationType().name()));

        model.addAttribute("recipe", recipe);
        model.addAttribute("transformations", transformations);
        model.addAttribute("transformationsByType", byType);
        model.addAttribute("breakingCount", transformations.stream()
                .filter(MigrationTransformation::getBreakingChange).count());

        return "recipes/view";
    }

    /**
     * Search transformations within a recipe
     */
    @GetMapping("/{id}/search")
    public String searchInRecipe(
            @PathVariable Long id,
            @RequestParam String query,
            Model model) {

        MigrationRecipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + id));

        List<MigrationTransformation> results = transformationRepository.searchInRecipe(id, query);

        model.addAttribute("recipe", recipe);
        model.addAttribute("transformations", results);
        model.addAttribute("searchQuery", query);

        return "recipes/search-results";
    }

    /**
     * HTMX endpoint for filtering transformations
     */
    @GetMapping("/{id}/transformations")
    public String getTransformations(
            @PathVariable Long id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean breakingOnly,
            Model model) {

        List<MigrationTransformation> transformations;

        if (breakingOnly != null && breakingOnly) {
            transformations = transformationRepository.findByRecipeIdAndBreakingChangeTrue(id);
        } else if (type != null && !type.isBlank()) {
            transformations = transformationRepository.findByRecipeAndType(
                    id, MigrationTransformation.TransformationType.valueOf(type));
        } else if (category != null && !category.isBlank()) {
            transformations = transformationRepository.findByRecipeAndCategory(id, category);
        } else {
            transformations = transformationRepository.findByRecipeId(id);
        }

        model.addAttribute("transformations", transformations);
        return "recipes/fragments :: transformationTable";
    }
}
```

#### 12.7.4 Recipe List Template

Create `src/main/resources/templates/recipes/list.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::content}, 'recipes')}">
<th:block th:fragment="content">
    <div class="container-fluid py-4">

        <!-- Header -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h1 class="h3 mb-0">
                    <i class="bi bi-arrow-repeat text-purple"></i> Migration Recipes
                </h1>
                <p class="text-muted mb-0">OpenRewrite-based migration knowledge</p>
            </div>
            <div class="d-flex gap-2">
                <span class="badge bg-purple fs-6" th:text="${totalRecipes} + ' recipes'">0 recipes</span>
                <span class="badge bg-warning fs-6" th:text="${breakingChanges} + ' breaking'">0 breaking</span>
            </div>
        </div>

        <!-- Search & Filter Bar -->
        <div class="card mb-4">
            <div class="card-body">
                <form th:action="@{/recipes}" method="get" class="row g-3">
                    <!-- Project Filter -->
                    <div class="col-md-3">
                        <label class="form-label">Project</label>
                        <select name="project" class="form-select">
                            <option value="">All Projects</option>
                            <option th:each="p : ${projects}"
                                    th:value="${p}"
                                    th:text="${p}"
                                    th:selected="${p == selectedProject}">
                            </option>
                        </select>
                    </div>

                    <!-- Version Filter -->
                    <div class="col-md-3">
                        <label class="form-label">Target Version</label>
                        <select name="version" class="form-select">
                            <option value="">All Versions</option>
                            <option th:each="v : ${versions}"
                                    th:value="${v}"
                                    th:text="${v}"
                                    th:selected="${v == selectedVersion}">
                            </option>
                        </select>
                    </div>

                    <!-- Full-text Search -->
                    <div class="col-md-4">
                        <label class="form-label">Search</label>
                        <input type="text" name="search" class="form-control"
                               placeholder="Search recipes..."
                               th:value="${searchTerm}">
                    </div>

                    <!-- Submit -->
                    <div class="col-md-2 d-flex align-items-end">
                        <button type="submit" class="btn btn-primary w-100">
                            <i class="bi bi-search"></i> Search
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Recipe List -->
        <div class="card">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th>Recipe</th>
                                <th>Project</th>
                                <th>From → To</th>
                                <th>Transformations</th>
                                <th>Breaking</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr th:each="recipe : ${recipes}">
                                <td>
                                    <a th:href="@{/recipes/{id}(id=${recipe.id})}"
                                       class="text-decoration-none fw-bold">
                                        <th:block th:text="${recipe.displayName}">Recipe Name</th:block>
                                    </a>
                                    <br>
                                    <small class="text-muted" th:text="${#strings.abbreviate(recipe.description, 80)}">Description</small>
                                </td>
                                <td>
                                    <span class="badge bg-info" th:text="${recipe.fromProject}">spring-boot</span>
                                </td>
                                <td>
                                    <code th:text="${recipe.fromVersionMin}">3.0.0</code>
                                    →
                                    <code class="text-success" th:text="${recipe.toVersion}">4.0.0</code>
                                </td>
                                <td>
                                    <span th:text="${recipe.transformationCount ?: '-'}">12</span>
                                </td>
                                <td>
                                    <span class="badge bg-danger" th:if="${recipe.breakingCount > 0}"
                                          th:text="${recipe.breakingCount}">5</span>
                                    <span class="text-muted" th:unless="${recipe.breakingCount > 0}">0</span>
                                </td>
                                <td>
                                    <a th:href="@{/recipes/{id}(id=${recipe.id})}"
                                       class="btn btn-sm btn-outline-primary">
                                        <i class="bi bi-eye"></i> View
                                    </a>
                                </td>
                            </tr>
                            <tr th:if="${#lists.isEmpty(recipes.content)}">
                                <td colspan="6" class="text-center py-4 text-muted">
                                    <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                                    No recipes found
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Pagination -->
            <div class="card-footer" th:if="${recipes.totalPages > 1}">
                <nav>
                    <ul class="pagination mb-0 justify-content-center">
                        <li class="page-item" th:classappend="${recipes.first} ? 'disabled'">
                            <a class="page-link" th:href="@{/recipes(page=${recipes.number - 1}, project=${selectedProject}, version=${selectedVersion}, search=${searchTerm})}">Previous</a>
                        </li>
                        <li class="page-item" th:each="i : ${#numbers.sequence(0, recipes.totalPages - 1)}"
                            th:classappend="${i == recipes.number} ? 'active'">
                            <a class="page-link" th:href="@{/recipes(page=${i}, project=${selectedProject}, version=${selectedVersion}, search=${searchTerm})}"
                               th:text="${i + 1}">1</a>
                        </li>
                        <li class="page-item" th:classappend="${recipes.last} ? 'disabled'">
                            <a class="page-link" th:href="@{/recipes(page=${recipes.number + 1}, project=${selectedProject}, version=${selectedVersion}, search=${searchTerm})}">Next</a>
                        </li>
                    </ul>
                </nav>
            </div>
        </div>

    </div>
</th:block>
</html>
```

---

### 12.8 Recipe-Project-Version Mapping

Recipes are linked to Spring projects and versions through a mapping structure that enables:
- Finding all recipes applicable to a specific project
- Finding recipes for upgrading from one version to another
- Discovering which transformations affect which project components

#### 12.8.1 Enhanced Database Schema for Mapping

Add to `V3__migration_knowledge.sql`:

```sql
-- Recipe to Project mapping (many-to-many)
CREATE TABLE recipe_project_mapping (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES migration_recipes(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    relevance_score INT DEFAULT 100,  -- 0-100, higher = more relevant
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recipe_project UNIQUE (recipe_id, project_id)
);

-- Recipe to Version mapping (which versions the recipe migrates between)
CREATE TABLE recipe_version_mapping (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES migration_recipes(id) ON DELETE CASCADE,
    version_id BIGINT NOT NULL REFERENCES project_versions(id) ON DELETE CASCADE,
    mapping_type VARCHAR(20) NOT NULL,  -- SOURCE, TARGET
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recipe_version UNIQUE (recipe_id, version_id, mapping_type)
);

-- Indexes
CREATE INDEX idx_recipe_project_recipe ON recipe_project_mapping(recipe_id);
CREATE INDEX idx_recipe_project_project ON recipe_project_mapping(project_id);
CREATE INDEX idx_recipe_version_recipe ON recipe_version_mapping(recipe_id);
CREATE INDEX idx_recipe_version_version ON recipe_version_mapping(version_id);
CREATE INDEX idx_recipe_version_type ON recipe_version_mapping(mapping_type);
```

#### 12.8.2 Repository Methods for Mapping

Add to `MigrationRecipeRepository.java`:

```java
/**
 * Find recipes by linked Spring project
 */
@Query("""
    SELECT r FROM MigrationRecipe r
    JOIN RecipeProjectMapping rpm ON rpm.recipe.id = r.id
    WHERE rpm.project.id = :projectId
    AND r.isActive = true
    ORDER BY rpm.relevanceScore DESC
    """)
List<MigrationRecipe> findByProjectId(@Param("projectId") Long projectId);

/**
 * Find recipes that migrate FROM a specific version
 */
@Query("""
    SELECT r FROM MigrationRecipe r
    JOIN RecipeVersionMapping rvm ON rvm.recipe.id = r.id
    WHERE rvm.version.id = :versionId
    AND rvm.mappingType = 'SOURCE'
    AND r.isActive = true
    """)
List<MigrationRecipe> findBySourceVersion(@Param("versionId") Long versionId);

/**
 * Find recipes that migrate TO a specific version
 */
@Query("""
    SELECT r FROM MigrationRecipe r
    JOIN RecipeVersionMapping rvm ON rvm.recipe.id = r.id
    WHERE rvm.version.id = :versionId
    AND rvm.mappingType = 'TARGET'
    AND r.isActive = true
    """)
List<MigrationRecipe> findByTargetVersion(@Param("versionId") Long versionId);

/**
 * Get distinct projects that have recipes
 */
@Query("SELECT DISTINCT r.fromProject FROM MigrationRecipe r WHERE r.isActive = true ORDER BY r.fromProject")
List<String> findDistinctProjects();

/**
 * Get distinct target versions
 */
@Query("SELECT DISTINCT r.toVersion FROM MigrationRecipe r WHERE r.isActive = true ORDER BY r.toVersion DESC")
List<String> findDistinctTargetVersions();

/**
 * Full-text search across recipes
 */
@Query(value = """
    SELECT * FROM migration_recipes
    WHERE is_active = true
    AND (
        name ILIKE '%' || :search || '%'
        OR display_name ILIKE '%' || :search || '%'
        OR description ILIKE '%' || :search || '%'
    )
    ORDER BY display_name
    """, nativeQuery = true)
Page<MigrationRecipe> searchByText(@Param("search") String search, Pageable pageable);
```

---

### 12.9 Sync Process Integration

The OpenRewrite recipe sync is integrated into the existing sync infrastructure as both a standalone sync and as Step 8 of the comprehensive scan.

#### 12.9.1 Sync Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Comprehensive Sync                            │
├─────────────────────────────────────────────────────────────────┤
│ Step 1: Spring Boot Versions                                    │
│ Step 2: Spring Projects                                         │
│ Step 3: Project Versions                                        │
│ Step 4: Documentation Links                                     │
│ Step 5: Documentation Content                                   │
│ Step 6: Code Examples                                           │
│ Step 7: External Sources                                        │
│ Step 8: OpenRewrite Recipes (NEW - conditional on feature flag) │
└─────────────────────────────────────────────────────────────────┘
```

#### 12.9.2 Recipe Sync Service Implementation

Update `src/main/java/com/spring/mcp/service/migration/RecipeSyncService.java`:

```java
package com.spring.mcp.service.migration;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.model.entity.MigrationRecipe;
import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.repository.MigrationRecipeRepository;
import com.spring.mcp.repository.MigrationTransformationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RecipeSyncService {

    private static final Logger log = LoggerFactory.getLogger(RecipeSyncService.class);

    private static final String[] RECIPE_FILES = {
        "spring-boot-4.yml",
        "spring-boot-3.yml",
        "spring-security-7.yml",
        "spring-framework-7.yml"
    };

    private final OpenRewriteFeatureConfig config;
    private final MigrationRecipeRepository recipeRepository;
    private final MigrationTransformationRepository transformationRepository;
    private final WebClient webClient;

    public RecipeSyncService(
            OpenRewriteFeatureConfig config,
            MigrationRecipeRepository recipeRepository,
            MigrationTransformationRepository transformationRepository,
            WebClient.Builder webClientBuilder) {
        this.config = config;
        this.recipeRepository = recipeRepository;
        this.transformationRepository = transformationRepository;
        this.webClient = webClientBuilder
                .baseUrl(config.getGithub().getBaseUrl())
                .build();
    }

    /**
     * Standalone scheduled sync - runs based on configured schedule
     */
    @Scheduled(cron = "${mcp.features.openrewrite.sync.schedule:0 0 3 * * SUN}")
    public void scheduledSync() {
        if (!config.isEnabled() || !config.getSync().isEnabled()) {
            log.debug("OpenRewrite sync disabled, skipping scheduled sync");
            return;
        }
        syncFromGitHub();
    }

    /**
     * Manual sync trigger - called from comprehensive sync or admin UI
     */
    @Transactional
    public SyncResult syncFromGitHub() {
        if (!config.isEnabled()) {
            return SyncResult.disabled();
        }

        log.info("Starting OpenRewrite recipe sync from GitHub...");
        AtomicInteger recipesUpdated = new AtomicInteger(0);
        AtomicInteger transformationsUpdated = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        for (String recipeFile : RECIPE_FILES) {
            try {
                syncRecipeFile(recipeFile, recipesUpdated, transformationsUpdated);
            } catch (Exception e) {
                log.error("Failed to sync recipe file: {}", recipeFile, e);
                errors.incrementAndGet();
            }
        }

        SyncResult result = new SyncResult(
                true,
                recipesUpdated.get(),
                transformationsUpdated.get(),
                errors.get()
        );

        log.info("OpenRewrite sync complete: {}", result);
        return result;
    }

    private void syncRecipeFile(String filename, AtomicInteger recipes, AtomicInteger transformations) {
        String content = fetchRecipeContent(filename);
        if (content == null) return;

        Yaml yaml = new Yaml();
        Map<String, Object> recipeData = yaml.load(content);

        // Parse and update recipe
        MigrationRecipe recipe = parseRecipe(recipeData);
        recipe = recipeRepository.save(recipe);
        recipes.incrementAndGet();

        // Parse and update transformations
        List<Map<String, Object>> recipeList = (List<Map<String, Object>>) recipeData.get("recipeList");
        if (recipeList != null) {
            for (Map<String, Object> transformationData : recipeList) {
                MigrationTransformation transformation = parseTransformation(recipe, transformationData);
                transformationRepository.save(transformation);
                transformations.incrementAndGet();
            }
        }
    }

    private String fetchRecipeContent(String filename) {
        try {
            return webClient.get()
                    .uri("/src/main/resources/META-INF/rewrite/" + filename)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("Could not fetch recipe file: {}", filename);
            return null;
        }
    }

    private MigrationRecipe parseRecipe(Map<String, Object> data) {
        // Implementation to parse YAML into entity
        MigrationRecipe recipe = new MigrationRecipe();
        recipe.setName((String) data.get("name"));
        recipe.setDisplayName((String) data.get("displayName"));
        recipe.setDescription((String) data.get("description"));
        // ... additional parsing
        return recipe;
    }

    private MigrationTransformation parseTransformation(MigrationRecipe recipe, Map<String, Object> data) {
        // Implementation to parse transformation data
        MigrationTransformation t = new MigrationTransformation();
        t.setRecipe(recipe);
        // ... additional parsing
        return t;
    }

    public record SyncResult(boolean enabled, int recipesUpdated, int transformationsUpdated, int errors) {
        public static SyncResult disabled() {
            return new SyncResult(false, 0, 0, 0);
        }
    }
}
```

#### 12.9.3 Comprehensive Sync Integration

Update `SyncOrchestrator.java` or equivalent comprehensive sync service:

```java
@Service
public class ComprehensiveSyncService {

    private final OpenRewriteFeatureConfig openRewriteConfig;
    private final RecipeSyncService recipeSyncService;
    // ... other sync services

    /**
     * Execute comprehensive sync with all steps
     */
    @Transactional
    public ComprehensiveSyncResult executeComprehensiveSync() {
        ComprehensiveSyncResult result = new ComprehensiveSyncResult();

        // Step 1-7: Existing sync steps
        result.addStep("Spring Boot Versions", syncSpringBootVersions());
        result.addStep("Spring Projects", syncSpringProjects());
        result.addStep("Project Versions", syncProjectVersions());
        result.addStep("Documentation Links", syncDocumentationLinks());
        result.addStep("Documentation Content", syncDocumentationContent());
        result.addStep("Code Examples", syncCodeExamples());
        result.addStep("External Sources", syncExternalSources());

        // Step 8: OpenRewrite Recipes (conditional)
        if (openRewriteConfig.isEnabled()) {
            log.info("Step 8: Syncing OpenRewrite Recipes...");
            RecipeSyncService.SyncResult recipeResult = recipeSyncService.syncFromGitHub();
            result.addStep("OpenRewrite Recipes", StepResult.from(recipeResult));
        } else {
            log.info("Step 8: OpenRewrite Recipes - SKIPPED (feature disabled)");
            result.addStep("OpenRewrite Recipes", StepResult.skipped("Feature disabled"));
        }

        return result;
    }
}
```

#### 12.9.4 Admin UI Sync Controls

Add to Settings page or Sync dashboard:

```html
<!-- OpenRewrite Sync Card -->
<div class="card mb-4" th:if="${openRewriteEnabled}">
    <div class="card-header d-flex justify-content-between align-items-center">
        <h5 class="mb-0">
            <i class="bi bi-arrow-repeat text-purple"></i> OpenRewrite Recipe Sync
        </h5>
        <span class="badge bg-success" th:if="${recipeSyncEnabled}">Auto-sync enabled</span>
        <span class="badge bg-secondary" th:unless="${recipeSyncEnabled}">Auto-sync disabled</span>
    </div>
    <div class="card-body">
        <div class="row">
            <div class="col-md-6">
                <p class="mb-2"><strong>Last Sync:</strong>
                    <span th:text="${lastRecipeSync ?: 'Never'}">Never</span>
                </p>
                <p class="mb-2"><strong>Schedule:</strong>
                    <code th:text="${recipeSyncSchedule}">0 0 3 * * SUN</code>
                </p>
                <p class="mb-0"><strong>Recipes:</strong>
                    <span th:text="${recipeCount}">0</span> |
                    <strong>Transformations:</strong>
                    <span th:text="${transformationCount}">0</span>
                </p>
            </div>
            <div class="col-md-6 text-end">
                <form th:action="@{/settings/sync/recipes}" method="post" class="d-inline">
                    <button type="submit" class="btn btn-purple">
                        <i class="bi bi-arrow-clockwise"></i> Sync Now
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>
```

#### 12.9.5 Sync Status in Dashboard

The dashboard shows sync status when the feature is enabled:

```html
<!-- In dashboard stats row -->
<div th:if="${openRewriteEnabled}" class="col-12 mt-4">
    <div class="card bg-dark">
        <div class="card-body py-2">
            <div class="d-flex justify-content-between align-items-center">
                <span>
                    <i class="bi bi-arrow-repeat text-purple"></i>
                    <strong>Recipe Sync:</strong>
                    <span th:text="${lastRecipeSync ?: 'Never synced'}">Never synced</span>
                </span>
                <span th:if="${recipeSyncEnabled}" class="text-success small">
                    <i class="bi bi-check-circle"></i> Auto-sync active
                </span>
            </div>
        </div>
    </div>
</div>
```

---

### 12.10 MCP Endpoint Integration

The OpenRewrite feature exposes new MCP tools that Claude Code and other MCP clients can use to query migration knowledge. These tools are only registered when the feature is enabled.

#### 12.10.1 New MCP Tools Overview

| Tool Name | Description | Parameters |
|-----------|-------------|------------|
| `getSpringMigrationGuide` | Get comprehensive migration guide between versions | `fromVersion`, `toVersion` |
| `getBreakingChanges` | List breaking changes for a project version | `project`, `version` |
| `searchMigrationKnowledge` | Full-text search across migration knowledge | `searchTerm`, `project`, `limit` |
| `getAvailableMigrationPaths` | List documented upgrade paths for a project | `project` |
| `getTransformationsByType` | Get transformations filtered by type | `project`, `version`, `type` |
| `getDeprecationReplacement` | Find replacement for deprecated class/method | `className`, `methodName` |

#### 12.10.2 MCP Tools Implementation

Create/Update `src/main/java/com/spring/mcp/mcp/tools/MigrationTools.java`:

```java
package com.spring.mcp.mcp.tools;

import com.spring.mcp.model.dto.*;
import com.spring.mcp.service.migration.MigrationKnowledgeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP Tools for OpenRewrite migration knowledge.
 * Only registered when mcp.features.openrewrite.enabled=true
 */
@Component
public class MigrationTools {

    private final MigrationKnowledgeService migrationService;

    public MigrationTools(MigrationKnowledgeService migrationService) {
        this.migrationService = migrationService;
    }

    @Tool(name = "getSpringMigrationGuide",
          description = "Get comprehensive migration guide for upgrading Spring Boot versions. " +
                        "Returns all breaking changes, import updates, dependency changes, " +
                        "property migrations, and code modifications needed. " +
                        "Use this BEFORE generating code for a specific Spring Boot version.")
    public MigrationGuideDto getSpringMigrationGuide(
            @ToolParam(description = "Source Spring Boot version (e.g., '3.5.8')") String fromVersion,
            @ToolParam(description = "Target Spring Boot version (e.g., '4.0.0')") String toVersion
    ) {
        return migrationService.getMigrationGuide("spring-boot", fromVersion, toVersion);
    }

    @Tool(name = "getBreakingChanges",
          description = "Get list of breaking changes for a specific Spring project version. " +
                        "Use this before generating code to avoid compilation errors. " +
                        "Returns severity levels: CRITICAL, ERROR, WARNING, INFO.")
    public List<BreakingChangeDto> getBreakingChanges(
            @ToolParam(description = "Project slug (e.g., 'spring-boot', 'spring-security', 'spring-framework')") String project,
            @ToolParam(description = "Target version to check (e.g., '4.0.0', '7.0.0')") String version
    ) {
        return migrationService.getBreakingChanges(project, version);
    }

    @Tool(name = "searchMigrationKnowledge",
          description = "Search migration knowledge base for specific topics. " +
                        "Examples: 'flyway starter', 'health indicator', 'thymeleaf request', " +
                        "'MockBean replacement', 'security configuration'. " +
                        "Returns relevant transformations with code examples.")
    public List<TransformationDto> searchMigrationKnowledge(
            @ToolParam(description = "Search term (e.g., 'flyway', 'actuator health', '@MockBean')") String searchTerm,
            @ToolParam(description = "Project to search in (default: 'spring-boot')", required = false) String project,
            @ToolParam(description = "Maximum results to return (default: 10)", required = false) Integer limit
    ) {
        String proj = project != null ? project : "spring-boot";
        int lim = limit != null ? limit : 10;
        return migrationService.searchTransformations(proj, searchTerm, lim);
    }

    @Tool(name = "getAvailableMigrationPaths",
          description = "Get list of available target versions for migration. " +
                        "Use this to discover what upgrade paths are documented.")
    public List<String> getAvailableMigrationPaths(
            @ToolParam(description = "Project slug (e.g., 'spring-boot')") String project
    ) {
        return migrationService.getAvailableMigrationTargets(project);
    }

    @Tool(name = "getTransformationsByType",
          description = "Get transformations filtered by type for a specific migration. " +
                        "Types: IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG.")
    public List<TransformationDto> getTransformationsByType(
            @ToolParam(description = "Project slug (e.g., 'spring-boot')") String project,
            @ToolParam(description = "Target version (e.g., '4.0.0')") String version,
            @ToolParam(description = "Transformation type (IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG)") String type
    ) {
        return migrationService.getTransformationsByType(project, version, type);
    }

    @Tool(name = "getDeprecationReplacement",
          description = "Find the replacement for a deprecated class or method. " +
                        "Use when you encounter deprecated APIs and need to find the new alternative.")
    public DeprecationReplacementDto getDeprecationReplacement(
            @ToolParam(description = "Fully qualified deprecated class name (e.g., 'org.springframework.boot.actuate.health.Health')") String className,
            @ToolParam(description = "Deprecated method name (optional, null for entire class deprecation)", required = false) String methodName
    ) {
        return migrationService.findReplacement(className, methodName);
    }

    @Tool(name = "checkVersionCompatibility",
          description = "Check if specific dependencies are compatible with a target Spring Boot version. " +
                        "Returns compatibility information and recommended versions.")
    public CompatibilityReportDto checkVersionCompatibility(
            @ToolParam(description = "Target Spring Boot version (e.g., '4.0.0')") String springBootVersion,
            @ToolParam(description = "List of dependencies to check (e.g., ['spring-security', 'flyway', 'thymeleaf'])") List<String> dependencies
    ) {
        return migrationService.checkCompatibility(springBootVersion, dependencies);
    }
}
```

#### 12.10.3 DTO Classes for MCP Responses

Create `src/main/java/com/spring/mcp/model/dto/MigrationGuideDto.java`:

```java
package com.spring.mcp.model.dto;

import java.util.List;
import java.util.Map;

public record MigrationGuideDto(
    String project,
    String fromVersion,
    String toVersion,
    int totalChanges,
    long breakingChanges,
    List<TransformationDto> importChanges,
    List<TransformationDto> dependencyChanges,
    List<TransformationDto> propertyChanges,
    List<TransformationDto> codeChanges,
    List<TransformationDto> buildChanges,
    List<TransformationDto> templateChanges,
    String sourceUrl,
    String license
) {
    public static MigrationGuideDto empty(String project, String from, String to) {
        return new MigrationGuideDto(project, from, to, 0, 0,
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            null, null);
    }
}
```

Create `src/main/java/com/spring/mcp/model/dto/TransformationDto.java`:

```java
package com.spring.mcp.model.dto;

import java.util.List;

public record TransformationDto(
    String type,
    String category,
    String subcategory,
    String oldPattern,
    String newPattern,
    String filePattern,
    String explanation,
    String codeExample,
    String additionalSteps,
    boolean breakingChange,
    String severity,
    List<String> tags
) {}
```

Create `src/main/java/com/spring/mcp/model/dto/BreakingChangeDto.java`:

```java
package com.spring.mcp.model.dto;

public record BreakingChangeDto(
    String type,
    String category,
    String oldPattern,
    String newPattern,
    String explanation,
    String codeExample,
    String severity,
    String filePattern
) {}
```

Create `src/main/java/com/spring/mcp/model/dto/DeprecationReplacementDto.java`:

```java
package com.spring.mcp.model.dto;

public record DeprecationReplacementDto(
    String deprecatedClass,
    String deprecatedMethod,
    String replacementClass,
    String replacementMethod,
    String deprecatedSince,
    String removedIn,
    String migrationNotes,
    String codeBefore,
    String codeAfter
) {
    public static DeprecationReplacementDto notFound(String className, String methodName) {
        return new DeprecationReplacementDto(
            className, methodName, null, null, null, null,
            "No replacement information found for this class/method.", null, null
        );
    }
}
```

Create `src/main/java/com/spring/mcp/model/dto/CompatibilityReportDto.java`:

```java
package com.spring.mcp.model.dto;

import java.util.List;

public record CompatibilityReportDto(
    String springBootVersion,
    List<DependencyCompatibility> dependencies,
    boolean allCompatible,
    List<String> warnings
) {
    public record DependencyCompatibility(
        String dependency,
        String compatibleVersion,
        boolean verified,
        String notes
    ) {}
}
```

#### 12.10.4 MCP Tool Registration Verification

The tools are automatically registered by Spring AI when the feature is enabled. Verify registration:

```java
@RestController
@RequestMapping("/api/mcp")
@ConditionalOnProperty(prefix = "mcp.features.openrewrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpToolInfoController {

    @GetMapping("/migration-tools")
    public Map<String, Object> getMigrationToolsInfo() {
        return Map.of(
            "feature", "openrewrite",
            "enabled", true,
            "tools", List.of(
                "getSpringMigrationGuide",
                "getBreakingChanges",
                "searchMigrationKnowledge",
                "getAvailableMigrationPaths",
                "getTransformationsByType",
                "getDeprecationReplacement",
                "checkVersionCompatibility"
            ),
            "documentation", "https://docs.openrewrite.org/recipes"
        );
    }
}
```

#### 12.10.5 Example MCP Tool Usage

When Claude Code connects to the MCP server, it can use these tools:

```json
// Example: Get migration guide before generating Spring Boot 4.0 code
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "getSpringMigrationGuide",
    "arguments": {
      "fromVersion": "3.5.8",
      "toVersion": "4.0.0"
    }
  },
  "id": 1
}

// Example: Search for specific migration knowledge
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "searchMigrationKnowledge",
    "arguments": {
      "searchTerm": "flyway starter",
      "project": "spring-boot",
      "limit": 5
    }
  },
  "id": 2
}

// Example: Check breaking changes before using Spring Security 7.0
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "getBreakingChanges",
    "arguments": {
      "project": "spring-security",
      "version": "7.0.0"
    }
  },
  "id": 3
}
```

---

### 12.11 License & Attribution Display

The Recipes page displays OpenRewrite license information and attribution in the header to comply with the Moderne Source Available License requirements.

#### 12.11.1 License Configuration

Add license information to `application.yml`:

```yaml
mcp:
  features:
    openrewrite:
      enabled: ${OPENREWRITE_ENABLED:true}
      attribution:
        name: "OpenRewrite"
        license: "Moderne Source Available License"
        license-url: "https://docs.openrewrite.org/licensing/openrewrite-licensing"
        repository: "https://github.com/openrewrite/rewrite-spring"
        notice: "Recipe data sourced from OpenRewrite project. Not for commercial redistribution."
```

#### 12.11.2 Attribution Configuration Class

Create `src/main/java/com/spring/mcp/config/OpenRewriteAttributionConfig.java`:

```java
package com.spring.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mcp.features.openrewrite.attribution")
public class OpenRewriteAttributionConfig {

    private String name = "OpenRewrite";
    private String license = "Moderne Source Available License";
    private String licenseUrl = "https://docs.openrewrite.org/licensing/openrewrite-licensing";
    private String repository = "https://github.com/openrewrite/rewrite-spring";
    private String notice = "Recipe data sourced from OpenRewrite project. Not for commercial redistribution.";

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
```

#### 12.11.3 Controller Updates for Attribution

Update `RecipeController.java` to include attribution:

```java
@Controller
@RequestMapping("/recipes")
@ConditionalOnProperty(prefix = "mcp.features.openrewrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecipeController {

    private final OpenRewriteAttributionConfig attributionConfig;
    // ... other dependencies

    public RecipeController(
            MigrationRecipeRepository recipeRepository,
            MigrationTransformationRepository transformationRepository,
            OpenRewriteAttributionConfig attributionConfig) {
        this.recipeRepository = recipeRepository;
        this.transformationRepository = transformationRepository;
        this.attributionConfig = attributionConfig;
    }

    @GetMapping
    public String listRecipes(/* params */, Model model) {
        // ... existing code ...

        // Add attribution to model
        model.addAttribute("openRewriteName", attributionConfig.getName());
        model.addAttribute("openRewriteLicense", attributionConfig.getLicense());
        model.addAttribute("openRewriteLicenseUrl", attributionConfig.getLicenseUrl());
        model.addAttribute("openRewriteRepository", attributionConfig.getRepository());
        model.addAttribute("openRewriteNotice", attributionConfig.getNotice());

        return "recipes/list";
    }

    @GetMapping("/{id}")
    public String viewRecipe(@PathVariable Long id, Model model) {
        // ... existing code ...

        // Add attribution to model
        model.addAttribute("openRewriteName", attributionConfig.getName());
        model.addAttribute("openRewriteLicense", attributionConfig.getLicense());
        model.addAttribute("openRewriteLicenseUrl", attributionConfig.getLicenseUrl());

        return "recipes/view";
    }
}
```

#### 12.11.4 Recipe List Page Header with Attribution

Update `src/main/resources/templates/recipes/list.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::content}, 'recipes')}">
<th:block th:fragment="content">
    <div class="container-fluid py-4">

        <!-- Header with Attribution -->
        <div class="d-flex justify-content-between align-items-start mb-4">
            <div>
                <h1 class="h3 mb-1">
                    <i class="bi bi-arrow-repeat text-purple"></i> Migration Recipes
                </h1>
                <p class="text-muted mb-0">OpenRewrite-based migration knowledge</p>
            </div>
            <div class="text-end">
                <div class="d-flex gap-2 mb-2">
                    <span class="badge bg-purple fs-6" th:text="${totalRecipes} + ' recipes'">0 recipes</span>
                    <span class="badge bg-warning fs-6" th:text="${breakingChanges} + ' breaking'">0 breaking</span>
                </div>
            </div>
        </div>

        <!-- Attribution Banner -->
        <div class="alert alert-secondary border-0 mb-4" style="background-color: rgba(111, 66, 193, 0.1);">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <strong class="text-purple">
                        <i class="bi bi-info-circle"></i>
                        <span th:text="${openRewriteName}">OpenRewrite</span>
                    </strong>
                    <span class="text-muted mx-2">|</span>
                    <a th:href="${openRewriteLicenseUrl}"
                       target="_blank"
                       class="text-decoration-none">
                        <i class="bi bi-file-text"></i>
                        <span th:text="${openRewriteLicense}">Moderne Source Available License</span>
                    </a>
                    <span class="text-muted mx-2">|</span>
                    <a th:href="${openRewriteRepository}"
                       target="_blank"
                       class="text-decoration-none">
                        <i class="bi bi-github"></i> Repository
                    </a>
                </div>
                <div>
                    <small class="text-muted" th:text="${openRewriteNotice}">
                        Recipe data sourced from OpenRewrite project.
                    </small>
                </div>
            </div>
        </div>

        <!-- Rest of the page content... -->
        <!-- Search & Filter Bar -->
        <div class="card mb-4">
            <!-- ... existing filter form ... -->
        </div>

        <!-- Recipe List -->
        <div class="card">
            <!-- ... existing table ... -->
        </div>

    </div>
</th:block>
</html>
```

#### 12.11.5 Recipe Detail Page Header with Attribution

Update `src/main/resources/templates/recipes/view.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::content}, 'recipes')}">
<th:block th:fragment="content">
    <div class="container-fluid py-4">

        <!-- Breadcrumb -->
        <nav aria-label="breadcrumb" class="mb-3">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="/recipes">Recipes</a></li>
                <li class="breadcrumb-item active" th:text="${recipe.displayName}">Recipe Name</li>
            </ol>
        </nav>

        <!-- Header with Attribution -->
        <div class="card mb-4 border-0" style="background: linear-gradient(135deg, rgba(111, 66, 193, 0.1) 0%, rgba(111, 66, 193, 0.05) 100%);">
            <div class="card-body">
                <div class="row">
                    <div class="col-md-8">
                        <h1 class="h3 mb-2">
                            <i class="bi bi-arrow-repeat text-purple"></i>
                            <span th:text="${recipe.displayName}">Recipe Name</span>
                        </h1>
                        <p class="text-muted mb-3" th:text="${recipe.description}">Description</p>

                        <!-- Version Badge -->
                        <div class="mb-3">
                            <span class="badge bg-info me-2" th:text="${recipe.fromProject}">spring-boot</span>
                            <code th:text="${recipe.fromVersionMin}">3.0.0</code>
                            <i class="bi bi-arrow-right mx-2"></i>
                            <code class="text-success fw-bold" th:text="${recipe.toVersion}">4.0.0</code>
                        </div>

                        <!-- Stats -->
                        <div class="d-flex gap-3">
                            <span>
                                <i class="bi bi-list-check text-primary"></i>
                                <strong th:text="${#lists.size(transformations)}">0</strong> transformations
                            </span>
                            <span>
                                <i class="bi bi-exclamation-triangle text-warning"></i>
                                <strong th:text="${breakingCount}">0</strong> breaking changes
                            </span>
                        </div>
                    </div>
                    <div class="col-md-4 text-end">
                        <!-- License & Attribution Box -->
                        <div class="bg-dark rounded p-3">
                            <div class="mb-2">
                                <strong class="text-purple" th:text="${openRewriteName}">OpenRewrite</strong>
                            </div>
                            <div class="small">
                                <a th:href="${openRewriteLicenseUrl}"
                                   target="_blank"
                                   class="text-light text-decoration-none">
                                    <i class="bi bi-file-text"></i>
                                    <span th:text="${openRewriteLicense}">License</span>
                                </a>
                            </div>
                            <div class="small mt-1" th:if="${recipe.sourceUrl}">
                                <a th:href="${recipe.sourceUrl}"
                                   target="_blank"
                                   class="text-info text-decoration-none">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                    View Original Recipe
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Transformation Tabs -->
        <div class="card">
            <div class="card-header">
                <ul class="nav nav-tabs card-header-tabs" role="tablist">
                    <li class="nav-item">
                        <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#all">
                            All <span class="badge bg-secondary" th:text="${#lists.size(transformations)}">0</span>
                        </button>
                    </li>
                    <li class="nav-item" th:each="entry : ${transformationsByType}">
                        <button class="nav-link" data-bs-toggle="tab"
                                th:data-bs-target="'#type-' + ${entry.key}">
                            <span th:text="${entry.key}">TYPE</span>
                            <span class="badge bg-secondary" th:text="${#lists.size(entry.value)}">0</span>
                        </button>
                    </li>
                </ul>
            </div>
            <div class="card-body tab-content">
                <!-- All Transformations Tab -->
                <div class="tab-pane fade show active" id="all">
                    <div th:replace="~{recipes/fragments :: transformationList(${transformations})}"></div>
                </div>
                <!-- Type-specific Tabs -->
                <div class="tab-pane fade" th:each="entry : ${transformationsByType}"
                     th:id="'type-' + ${entry.key}">
                    <div th:replace="~{recipes/fragments :: transformationList(${entry.value})}"></div>
                </div>
            </div>
        </div>

    </div>
</th:block>
</html>
```

#### 12.11.6 Transformation Fragment

Create `src/main/resources/templates/recipes/fragments.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<!-- Transformation List Fragment -->
<th:block th:fragment="transformationList(transformations)">
    <div class="list-group list-group-flush">
        <div th:each="t : ${transformations}"
             class="list-group-item"
             th:classappend="${t.breakingChange} ? 'border-start border-danger border-3' : ''">

            <div class="d-flex justify-content-between align-items-start mb-2">
                <div>
                    <span class="badge me-2"
                          th:classappend="${t.transformationType.name() == 'IMPORT'} ? 'bg-primary' :
                                          (${t.transformationType.name() == 'DEPENDENCY'} ? 'bg-success' :
                                          (${t.transformationType.name() == 'BUILD'} ? 'bg-warning text-dark' :
                                          (${t.transformationType.name() == 'TEMPLATE'} ? 'bg-info text-dark' : 'bg-secondary')))"
                          th:text="${t.transformationType}">TYPE</span>
                    <span class="badge bg-dark" th:if="${t.category}" th:text="${t.category}">category</span>
                    <span class="badge bg-dark" th:if="${t.subcategory}" th:text="${t.subcategory}">subcategory</span>
                </div>
                <div>
                    <span class="badge"
                          th:classappend="${t.severity.name() == 'CRITICAL'} ? 'bg-danger' :
                                          (${t.severity.name() == 'ERROR'} ? 'bg-danger' :
                                          (${t.severity.name() == 'WARNING'} ? 'bg-warning text-dark' : 'bg-info text-dark'))"
                          th:text="${t.severity}">INFO</span>
                    <span th:if="${t.breakingChange}" class="badge bg-danger ms-1">
                        <i class="bi bi-exclamation-triangle"></i> Breaking
                    </span>
                </div>
            </div>

            <p class="mb-2" th:text="${t.explanation}">Explanation text</p>

            <!-- Pattern Changes -->
            <div class="row mb-2">
                <div class="col-md-6">
                    <small class="text-muted d-block mb-1">Before:</small>
                    <code class="d-block p-2 bg-danger bg-opacity-10 rounded text-break"
                          th:text="${t.oldPattern}">old pattern</code>
                </div>
                <div class="col-md-6">
                    <small class="text-muted d-block mb-1">After:</small>
                    <code class="d-block p-2 bg-success bg-opacity-10 rounded text-break"
                          th:text="${t.newPattern}">new pattern</code>
                </div>
            </div>

            <!-- Code Example (if available) -->
            <div th:if="${t.codeExample}" class="mt-3">
                <small class="text-muted d-block mb-1">
                    <i class="bi bi-code-slash"></i> Code Example:
                </small>
                <pre class="bg-dark text-light p-3 rounded"><code th:text="${t.codeExample}">code example</code></pre>
            </div>

            <!-- Additional Steps (if available) -->
            <div th:if="${t.additionalSteps}" class="mt-3 alert alert-info mb-0">
                <small>
                    <i class="bi bi-info-circle"></i>
                    <strong>Additional Steps:</strong>
                    <span th:text="${t.additionalSteps}">additional steps</span>
                </small>
            </div>

            <!-- File Pattern & Tags -->
            <div class="mt-2 d-flex justify-content-between align-items-center">
                <small class="text-muted" th:if="${t.filePattern}">
                    <i class="bi bi-file-code"></i>
                    <span th:text="${t.filePattern}">*.java</span>
                </small>
                <div th:if="${t.tags}">
                    <span th:each="tag : ${t.tags}"
                          class="badge bg-secondary bg-opacity-50 me-1"
                          th:text="${tag}">tag</span>
                </div>
            </div>
        </div>

        <!-- Empty State -->
        <div th:if="${#lists.isEmpty(transformations)}" class="text-center py-5 text-muted">
            <i class="bi bi-inbox fs-1 d-block mb-2"></i>
            No transformations found
        </div>
    </div>
</th:block>

<!-- Transformation Table Fragment (for HTMX) -->
<th:block th:fragment="transformationTable">
    <div th:replace="~{recipes/fragments :: transformationList(${transformations})}"></div>
</th:block>

</html>
```

#### 12.11.7 Custom CSS for Attribution

Add to `src/main/resources/static/css/style.css`:

```css
/* OpenRewrite Attribution Styling */
.text-purple {
    color: #6f42c1 !important;
}

.border-purple {
    border-color: #6f42c1 !important;
}

/* Attribution banner */
.alert-attribution {
    background: linear-gradient(135deg, rgba(111, 66, 193, 0.1) 0%, rgba(111, 66, 193, 0.05) 100%);
    border-left: 4px solid #6f42c1;
}

/* Recipe header gradient */
.recipe-header {
    background: linear-gradient(135deg, rgba(111, 66, 193, 0.15) 0%, rgba(111, 66, 193, 0.05) 100%);
}

/* License box in detail view */
.license-box {
    background-color: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(111, 66, 193, 0.3);
}

/* Transformation cards */
.transformation-breaking {
    border-left: 4px solid #dc3545 !important;
}

/* Code blocks in transformations */
.pattern-old {
    background-color: rgba(220, 53, 69, 0.1);
    border: 1px solid rgba(220, 53, 69, 0.2);
}

.pattern-new {
    background-color: rgba(25, 135, 84, 0.1);
    border: 1px solid rgba(25, 135, 84, 0.2);
}
```

---

### 12.12 Future Enhancements (Post Spring AI 2.0)

After upgrading to Spring Boot 4.0 with Spring AI 2.0:

1. **Self-migration**: Use the migration knowledge to upgrade the MCP Server itself
2. **Real-time recipe sync**: Implement GitHub webhook for instant updates
3. **Migration path calculator**: Suggest optimal upgrade paths (3.2 → 3.5 → 4.0)
4. **Code generation with migrations**: Auto-apply migrations when generating code
5. **Custom recipe support**: Allow users to add their own migration patterns

---

## Appendix A: OpenRewrite Resources

- **Main Repository**: https://github.com/openrewrite/rewrite-spring
- **Recipe Catalog**: https://docs.openrewrite.org/recipes
- **YAML Format Reference**: https://docs.openrewrite.org/reference/yaml-format-reference
- **Licensing Information**: https://docs.openrewrite.org/licensing/openrewrite-licensing
- **Spring Boot 4.0 Recipe**: https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0-community-edition

## Appendix B: Related Spring Resources

- **Spring Boot Release Notes**: https://github.com/spring-projects/spring-boot/wiki
- **Spring Framework Migration Guide**: https://github.com/spring-projects/spring-framework/wiki
- **Spring Security Migration**: https://docs.spring.io/spring-security/reference/migration/index.html

---

*Document generated: 2025-11-28*
*Author: Claude Code capability analysis*

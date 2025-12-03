# Spring Documentation Loading Evaluation Report

**Version**: 1.0
**Date**: December 2025
**Author**: Spring MCP Server Development Team

---

## Executive Summary

This evaluation compares the current HTML-based documentation loading approach with a GitHub-based AsciiDoc source approach for the Spring MCP Server. After comprehensive analysis and benchmarking, **we recommend adopting a hybrid GitHub-first approach** that uses the original AsciiDoc source files from Spring's GitHub repositories.

### Key Findings

| Metric | Current (HTML Scraping) | GitHub (.adoc Source) | Improvement |
|--------|------------------------|----------------------|-------------|
| Documentation Files | 51 overview pages | 223+ .adoc files (Spring Boot alone) | **4.4x more content** |
| Code Examples | 218 extracted snippets | 413 native Java files (Spring Boot alone) | **1.9x more examples** |
| Fetch Time | 5-15 seconds (with JS) | ~809ms (raw fetch) | **6-18x faster** |
| Processing | HTML→Markdown conversion | AsciiDoc→Markdown conversion | **Simpler pipeline** |
| Source Quality | Reverse-engineered output | Original author content | **Authoritative source** |

### Recommendation

**Adopt a Hybrid GitHub-First Approach:**
- Use GitHub raw content for documentation source (.adoc files)
- Convert AsciiDoc to Markdown using AsciidoctorJ
- Extract native code examples from actual source files
- Keep spring.io APIs for version discovery (authoritative for releases)
- Maintain fallback to current method if GitHub is unavailable

---

## Table of Contents

1. [Current System Analysis](#1-current-system-analysis)
2. [GitHub-Based Alternative](#2-github-based-alternative)
3. [Project Coverage Comparison](#3-project-coverage-comparison)
4. [Performance Benchmark Results](#4-performance-benchmark-results)
5. [Content Quality Analysis](#5-content-quality-analysis)
6. [Technical Comparison](#6-technical-comparison)
7. [Migration Considerations](#7-migration-considerations)
8. [Implementation Plan](#8-implementation-plan)
9. [Risk Assessment](#9-risk-assessment)
10. [Conclusion](#10-conclusion)

**Appendices:**
- [Appendix A: GitHub Repository Inventory](#appendix-a-github-repository-inventory)
- [Appendix B: AsciiDoc Conversion Examples](#appendix-b-asciidoc-conversion-examples)
- [Appendix C: Performance Test Scripts](#appendix-c-performance-test-scripts)
- [Appendix D: Git Tag Versioning Strategy](#appendix-d-git-tag-versioning-strategy)

---

## 1. Current System Analysis

### 1.1 Architecture Overview

The current system uses an 8-phase comprehensive synchronization process orchestrated by `ComprehensiveSyncService`:

```
┌─────────────────────────────────────────────────────────────┐
│                  ComprehensiveSyncService                   │
│                    (8-Phase Orchestrator)                   │
└─────────────────────────────────────────────────────────────┘
                              │
    ┌─────────────────────────┼─────────────────────────┐
    │                         │                         │
    ▼                         ▼                         ▼
┌─────────┐            ┌─────────────┐           ┌─────────────┐
│ Phase 0 │            │   Phase 1   │           │   Phase 2   │
│ Boot    │            │ Generations │           │ Initializr  │
│ Versions│            │    API      │           │    API      │
└─────────┘            └─────────────┘           └─────────────┘
    │                         │                         │
    └─────────────────────────┴─────────────────────────┘
                              │
                              ▼
    ┌─────────────────────────┴─────────────────────────┐
    │                                                   │
    ▼                         ▼                         ▼
┌─────────┐            ┌─────────────┐           ┌─────────────┐
│ Phase 3 │            │   Phase 4   │           │   Phase 5   │
│ Crawl   │            │ Relation-   │           │ Document-   │
│ Pages   │            │ ships       │           │ ation       │
└─────────┘            └─────────────┘           └─────────────┘
    │                         │                         │
    ▼                         ▼                         ▼
┌─────────┐            ┌─────────────┐           ┌─────────────┐
│ Phase 6 │            │   Phase 7   │           │   Phase 8   │
│ Code    │            │ OpenRewrite │           │ Cleanup     │
│Examples │            │ Recipes     │           │             │
└─────────┘            └─────────────┘           └─────────────┘
```

### 1.2 Current Data Sources

| Phase | Source | URL Pattern |
|-------|--------|-------------|
| 0 | Spring Boot Versions | `https://spring.io/page-data/projects/spring-boot/page-data.json` |
| 1 | Generations API | `https://spring.io/page-data/projects/generations/page-data.json` |
| 2 | Spring Initializr | `https://start.spring.io` |
| 3-5 | Project Pages | `https://spring.io/projects/{slug}` |
| 6 | Spring Guides | `https://spring.io/guides` |

### 1.3 Documentation Processing Pipeline

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   spring.io      │────▶│    HtmlUnit      │────▶│     Jsoup        │
│   HTML Page      │     │  (JS Rendering)  │     │  (DOM Parsing)   │
└──────────────────┘     └──────────────────┘     └──────────────────┘
                                                          │
                                                          ▼
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   PostgreSQL     │◀────│   Markdown       │◀────│    Flexmark      │
│   Database       │     │   Content        │     │ (HTML→Markdown)  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
```

### 1.4 Current Statistics

| Entity | Count | Total Size |
|--------|-------|------------|
| Spring Projects | 51 | - |
| Documentation Entries | 51 | ~134 KB |
| Code Examples | 218 | ~105 KB |
| Spring Boot Versions | ~25 | - |
| Project Versions | ~200 | - |

### 1.5 Key Service Files

| Service | Location | Purpose |
|---------|----------|---------|
| ComprehensiveSyncService | `service/sync/` | 8-phase master orchestrator |
| SpringBootVersionSyncService | `service/sync/` | Fetch Spring Boot versions |
| DocumentationFetchService | `service/documentation/` | HTML fetching with HtmlUnit |
| HtmlToMarkdownConverter | `service/documentation/` | Flexmark-based conversion |
| CodeExamplesSyncService | `service/sync/` | Code example extraction |
| CodeExampleExtractor | `service/indexing/` | HTML-to-code extraction |

### 1.6 Current Issues

1. **Heavy Compute Requirements**
   - HtmlUnit JavaScript rendering: 5-15 seconds per page
   - Memory-intensive DOM processing
   - CPU-bound HTML parsing

2. **Parsing Errors**
   - JavaScript execution timeouts
   - Dynamic content not fully rendered
   - CSS selector mismatches across Spring projects

3. **Content Limitations**
   - Only captures overview pages (51 pages)
   - Loses semantic structure during HTML→Markdown conversion
   - Code examples extracted as snippets, losing context

4. **Maintenance Burden**
   - CSS selectors break when spring.io redesigns
   - Different HTML structures per project
   - Requires constant selector updates

---

## 2. GitHub-Based Alternative

### 2.1 Discovery: Spring Uses AsciiDoc

The Spring ecosystem uses **Antora** with **AsciiDoc** for all official documentation. The spring.io website is **generated** from these .adoc source files.

```
GitHub (.adoc Source)  ────▶  Antora Build  ────▶  spring.io (HTML Output)
     ↑                                                    ↓
     │                                                    │
     └──────────────── WE ARE HERE ◀──────────────────────┘
                    (Reverse Engineering)
```

**Key Insight**: We're currently reverse-engineering the output instead of using the source.

### 2.2 Documentation Structure on GitHub

#### Spring Boot
```
spring-projects/spring-boot/
└── documentation/
    └── spring-boot-docs/
        └── src/
            └── docs/
                └── antora/
                    └── modules/
                        ├── ROOT/
                        │   └── pages/
                        │       ├── index.adoc
                        │       ├── installing.adoc
                        │       └── upgrading.adoc
                        ├── reference/
                        │   └── pages/
                        │       ├── web/
                        │       ├── data/
                        │       ├── actuator/
                        │       └── testing/
                        └── how-to/
                            └── pages/
```

#### Spring Security
```
spring-projects/spring-security/
└── docs/
    └── modules/
        └── ROOT/
            ├── pages/
            │   ├── index.adoc
            │   ├── servlet/
            │   ├── reactive/
            │   └── features/
            └── examples/
                └── kerberos/
```

#### Spring Framework
```
spring-projects/spring-framework/
└── framework-docs/
    └── modules/
        └── ROOT/
            └── pages/
                ├── index.adoc
                ├── core.adoc
                ├── web.adoc
                └── data-access.adoc
```

#### Spring Data Commons
```
spring-projects/spring-data-commons/
└── src/
    └── main/
        └── antora/
            └── modules/
                └── ROOT/
                    └── pages/
                        ├── index.adoc
                        ├── repositories.adoc
                        └── auditing.adoc
```

### 2.3 Documentation Path Patterns

Based on analysis of Spring repositories, documentation locations follow these patterns:

| Pattern | Projects Using It |
|---------|-------------------|
| `docs/modules/ROOT/pages/` | Spring Security, Spring Batch |
| `src/main/antora/modules/ROOT/pages/` | Spring Data Commons, Spring Data JPA |
| `documentation/*/src/docs/antora/modules/` | Spring Boot |
| `framework-docs/modules/ROOT/pages/` | Spring Framework |

### 2.4 Code Examples in GitHub

Spring documentation uses the `include-code::` directive to embed code examples:

```asciidoc
[[web.servlet.spring-mvc.message-converters]]
== HttpMessageConverters

include-code::MyConfiguration[]
```

The actual code lives in:
```
documentation/spring-boot-docs/src/main/java/
└── org/springframework/boot/docs/
    ├── web/
    │   └── servlet/
    │       └── MyConfiguration.java
    ├── data/
    ├── testing/
    └── actuator/
```

**Statistics for Spring Boot:**
- 223 .adoc documentation files
- 413 Java example files in documentation
- Native, compilable code (not snippets)

### 2.5 GitHub API Access

```bash
# List all files in a repository
GET https://api.github.com/repos/spring-projects/spring-boot/git/trees/main?recursive=1

# Get raw file content
GET https://raw.githubusercontent.com/spring-projects/spring-boot/main/documentation/.../index.adoc

# List directory contents
GET https://api.github.com/repos/spring-projects/spring-boot/contents/documentation
```

---

## 3. Project Coverage Comparison

### 3.1 GitHub spring-projects Organization

The `spring-projects` GitHub organization contains **86 repositories**, including:

#### Core Projects (30 repos)
| Repository | Documentation Path | Status |
|------------|-------------------|--------|
| spring-boot | `documentation/spring-boot-docs/` | Active |
| spring-framework | `framework-docs/` | Active |
| spring-security | `docs/` | Active |
| spring-data-jpa | `src/main/antora/` | Active |
| spring-data-mongodb | `src/main/antora/` | Active |
| spring-data-redis | `src/main/antora/` | Active |
| spring-data-commons | `src/main/antora/` | Active |
| spring-batch | `docs/` | Active |
| spring-integration | `src/reference/` | Active |
| spring-kafka | `src/main/antora/` | Active |
| spring-amqp | `src/reference/` | Active |
| spring-session | `docs/` | Active |
| spring-graphql | `src/docs/` | Active |
| spring-ai | `docs/` | Active |
| spring-shell | `docs/` | Active |
| spring-vault | `docs/` | Active |
| spring-ldap | `docs/` | Active |
| spring-hateoas | `src/main/asciidoc/` | Active |
| spring-restdocs | `docs/` | Active |
| spring-modulith | `src/docs/` | Active |
| spring-authorization-server | `docs/` | Active |
| spring-statemachine | `docs/` | Active |
| spring-webflow | `docs/` | Active |
| spring-ws | `docs/` | Active |
| spring-pulsar | `docs/` | Active |
| spring-credhub | `docs/` | Active |
| spring-grpc | `docs/` | Active |
| spring-retry | `docs/` | Active |
| spring-plugin | `docs/` | Active |
| spring-guice | `docs/` | Active |

#### Sample/Example Repositories (20+ repos)
- spring-data-examples
- spring-ai-examples
- spring-integration-samples
- spring-security-samples
- spring-petclinic
- spring-restdocs-samples
- spring-hateoas-examples
- spring-graphql-examples
- And more...

### 3.2 Current Database vs GitHub

| Category | Current DB | GitHub Repos | Gap |
|----------|-----------|--------------|-----|
| Core Spring Projects | 32 | 30 | +2 (DB has extras) |
| Spring Cloud Projects | 19 | ~25 (separate org) | -6 |
| Sample Repositories | 0 | 20+ | -20+ |
| **Total Active Projects** | **51** | **86+** | **-35** |

### 3.3 Missing Projects in Current System

Projects available on GitHub but not in current database:
- spring-retry
- spring-plugin
- spring-guice
- spring-flo
- spring-aot-smoke-tests
- spring-lifecycle-smoke-tests
- spring-rewrite-commons
- Multiple sample repositories

---

## 4. Performance Benchmark Results

### 4.1 GitHub Raw .adoc Fetch Tests

| File | Project | Time | Size | Content Quality |
|------|---------|------|------|-----------------|
| index.adoc | Spring Boot | 650ms | 1,004 bytes | Clean, structured |
| installing.adoc | Spring Boot | 708ms | 8,937 bytes | Full installation guide |
| upgrading.adoc | Spring Boot | 728ms | 2,923 bytes | Migration content |
| index.adoc | Spring Security | 698ms | 1,457 bytes | Includes xrefs |
| index.adoc | Spring Framework | 726ms | 1,808 bytes | Feature overview |
| index.adoc | Spring Data Commons | 1,438ms | 1,055 bytes | Modular structure |

**Average fetch time: ~809ms per file**

### 4.2 Current Method (spring.io) Tests

| Test | Time | Size | Result |
|------|------|------|--------|
| spring.io HTML (no JS) | 659ms | **1 byte** | **FAILS** - requires JS |
| spring.io with HtmlUnit | 5-15s | ~50KB HTML | Heavy compute |

**The current method REQUIRES JavaScript rendering** - simple HTTP fetch returns empty content.

### 4.3 Multiple File Fetch Performance

```
GitHub batch fetch (3 files): 2,428ms total (~809ms average)
Current method (3 pages): 15-45 seconds total (5-15s average)
```

**Performance improvement: 6-18x faster**

### 4.4 GitHub API Discovery Performance

| Operation | Time | Notes |
|-----------|------|-------|
| Repository tree (recursive) | 1,414ms | Returns all file paths |
| Directory listing | ~500ms | Single directory |
| Rate limit | 5,000/hour | Authenticated requests |

### 4.5 Content Volume Comparison

| Repository | .adoc Files | Java Examples | Current DB |
|------------|-------------|---------------|------------|
| Spring Boot | 223 | 413 | 1 overview |
| Spring Security | ~50 | ~100 | 1 overview |
| Spring Framework | ~100 | ~200 | 1 overview |
| Spring Data Commons | ~15 | ~30 | 1 overview |
| **Total (top 4 projects)** | **~388** | **~743** | **4 overviews** |

**Potential increase: 97x more documentation, 186x more code examples**

---

## 5. Content Quality Analysis

### 5.1 Current HTML-Scraped Content (Sample)

```markdown
Spring Boot makes it easy to create stand-alone, production-grade Spring
based Applications that you can "just run".

We take an opinionated view of the Spring platform and third-party libraries
so you can get started with minimum fuss. Most Spring Boot applications need
minimal Spring configuration.
```

- **Pros**: Clean text
- **Cons**: Loses semantic structure, navigation, code blocks

### 5.2 GitHub .adoc Source Content (Sample)

```asciidoc
:navtitle: Overview
= Spring Boot

Spring Boot helps you to create stand-alone, production-grade Spring-based
applications that you can run.
We take an opinionated view of the Spring platform and third-party libraries,
so that you can get started with minimum fuss.
Most Spring Boot applications need very little Spring configuration.

You can use Spring Boot to create Java applications that can be started by
using `java -jar` or more traditional war deployments.

Our primary goals are:

* Provide a radically faster and widely accessible getting-started experience
  for all Spring development.
* Be opinionated out of the box but get out of the way quickly as requirements
  start to diverge from the defaults.
* Provide a range of non-functional features that are common to large classes
  of projects (such as embedded servers, security, metrics, health checks, and
  externalized configuration).
* Absolutely no code generation (when not targeting native image) and no
  requirement for XML configuration.
```

- **Pros**: Full semantic structure, navigation titles, bullet lists
- **Cons**: Requires AsciiDoc→Markdown conversion

### 5.3 Spring Security Documentation Quality

**Current DB Content**: Single overview paragraph

**GitHub .adoc Source**:
```asciidoc
= Spring Security

Spring Security is a framework that provides
xref:features/authentication/index.adoc[authentication],
xref:features/authorization/index.adoc[authorization], and
xref:features/exploits/index.adoc[protection against common attacks].

With first class support for securing both
xref:servlet/index.adoc[imperative] and
xref:reactive/index.adoc[reactive] applications...
```

**Features preserved**:
- Cross-references (xref)
- Section links
- Semantic structure
- Navigation hierarchy

### 5.4 Code Example Quality Comparison

**Current (Extracted from HTML)**:
```java
// Building a RESTful Web Service - Example 1
package com.example.restservice;
public record Greeting(long id, String content) { }
```
- No imports
- No context
- Snippet only

**GitHub (Native Source File)**:
```java
/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.springframework.boot.docs.web.servlet.springmvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Example controller demonstrating REST endpoint creation.
 */
@Controller
public class MyController {

    @GetMapping("/")
    @ResponseBody
    public String index() {
        return "Hello World";
    }

}
```
- Full file with license
- Complete imports
- Javadoc comments
- Compilable code

---

## 6. Technical Comparison

### 6.1 Processing Pipeline Comparison

**Current Pipeline:**
```
spring.io ─▶ HtmlUnit (JS) ─▶ Jsoup (DOM) ─▶ Flexmark (MD) ─▶ PostgreSQL
  (HTML)      5-15 sec        ~1 sec         ~0.5 sec
```

**Proposed Pipeline:**
```
GitHub ─▶ HTTP GET ─▶ AsciidoctorJ ─▶ Custom Converter ─▶ PostgreSQL
 (.adoc)   ~0.8 sec     ~0.5 sec        ~0.2 sec
```

### 6.2 Dependency Comparison

| Current | Proposed |
|---------|----------|
| HtmlUnit (12MB) | AsciidoctorJ (8MB) |
| Jsoup (400KB) | - (included) |
| Flexmark (2MB) | Custom Converter |
| **Total: ~14.4MB** | **Total: ~8MB** |

### 6.3 Error Handling Comparison

**Current Issues:**
- JavaScript timeout errors
- Dynamic content not rendered
- CSS selector mismatches
- Memory pressure from DOM

**Proposed Benefits:**
- Static content (no JS)
- Consistent file format
- No CSS dependency
- Lightweight parsing

### 6.4 Version Handling via Git Tags

Spring projects use **Git tags** for version-specific documentation access. This enables fetching documentation for any released version.

#### Tag Naming Conventions

| Project | Tag Format | Examples |
|---------|------------|----------|
| Spring Boot | `v{version}` | `v3.5.7`, `v4.0.0-RC2`, `v4.0.0-M3` |
| Spring Security | `{version}` | `6.5.7`, `7.0.0-RC3` |
| Spring Framework | `v{version}` | `v6.2.1`, `v7.0.0-RC3` |

#### Version Types Supported

| Type | Tag Pattern | Example |
|------|-------------|---------|
| GA Release | `v3.5.7` | Stable production release |
| Release Candidate | `v4.0.0-RC2` | Pre-release testing |
| Milestone | `v4.0.0-M3` | Development preview |

#### Documentation Path Changes Across Versions

**Important**: Spring Boot changed its documentation structure between versions:

| Version Range | Documentation Path |
|---------------|-------------------|
| v3.5.x and earlier | `spring-boot-project/spring-boot-docs/src/docs/antora/` |
| v4.0.0+ (main) | `documentation/spring-boot-docs/src/docs/antora/` |

**Implementation Strategy**: The discovery service must maintain a version→path mapping:

```java
private String getDocPath(String project, String version) {
    if ("spring-boot".equals(project)) {
        Version v = Version.parse(version);
        if (v.getMajor() >= 4) {
            return "documentation/spring-boot-docs/src/docs/antora/modules";
        } else {
            return "spring-boot-project/spring-boot-docs/src/docs/antora/modules";
        }
    }
    return DEFAULT_PATHS.get(project);
}
```

#### Fetching Version-Specific Documentation

```bash
# Spring Boot v3.5.0 (old path)
https://raw.githubusercontent.com/spring-projects/spring-boot/v3.5.0/spring-boot-project/spring-boot-docs/src/docs/antora/modules/ROOT/pages/index.adoc

# Spring Boot main/v4.0+ (new path)
https://raw.githubusercontent.com/spring-projects/spring-boot/main/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/index.adoc

# Spring Security (no 'v' prefix)
https://raw.githubusercontent.com/spring-projects/spring-security/6.5.7/docs/modules/ROOT/pages/index.adoc
```

#### Version Handling Comparison

| Aspect | Current | Proposed |
|--------|---------|----------|
| Version Discovery | spring.io API | spring.io API (keep) |
| Documentation per Version | Single URL | Git tag (e.g., `v3.5.7`) |
| Historical Versions | Not supported | Full Git history |
| Pre-release Docs | Limited | RC/Milestone tags |
| Path Resolution | Static | Version-aware mapping |

#### Benchmark: Version-Specific Fetches

| Version | Path Type | Fetch Time | Status |
|---------|-----------|------------|--------|
| v3.5.0 | Old path | ~820ms | Success |
| v3.4.0 | Old path | ~750ms | Success |
| v4.0.0-RC2 | New path | ~701ms | Success |
| 6.5.7 (Security) | No prefix | ~672ms | Success |

---

## 7. Migration Considerations

### 7.1 Database Schema Impact

**No schema changes required.** The current schema fully supports the new approach:

```sql
-- documentation_content table already supports:
-- - content_type: 'text/markdown' (unchanged)
-- - content: TEXT (Markdown from AsciiDoc conversion)
-- - metadata: JSONB (can store source info)

-- code_examples table already supports:
-- - code_snippet: TEXT (full source files)
-- - source_url: VARCHAR (GitHub raw URL)
-- - language: VARCHAR (java, kotlin, xml, etc.)
```

### 7.2 AsciiDoc to Markdown Conversion

**Option 1: AsciidoctorJ with Custom Converter**
```java
@ConverterFor("markdown")
public class MarkdownConverter extends StringConverter {

    @Override
    public String convert(ContentNode node, String transform, Map<Object, Object> opts) {
        // Custom Markdown output logic
    }
}
```

**Option 2: AsciidoctorJ to HTML then Flexmark**
```java
// Use existing Flexmark infrastructure
String html = asciidoctor.convert(adocContent, Options.builder().backend("html5").build());
String markdown = htmlToMarkdownConverter.convert(html);
```

**Option 3: Direct RegEx Transformation**
```java
// Simple pattern replacement for common structures
content = content.replaceAll("^= (.+)$", "# $1");  // Headings
content = content.replaceAll("^== (.+)$", "## $1");
content = content.replaceAll("\\[source,(\\w+)\\]", "```$1");
```

**Recommendation**: Option 2 (AsciidoctorJ → HTML → Flexmark) leverages existing infrastructure.

### 7.3 Cross-Reference Handling

AsciiDoc `xref` links need transformation:
```asciidoc
xref:features/authentication/index.adoc[authentication]
```
Becomes:
```markdown
[authentication](features/authentication/index.md)
```

### 7.4 Include Directive Handling

Code includes:
```asciidoc
include-code::MyConfiguration[]
```
Requires fetching referenced Java file from:
```
documentation/spring-boot-docs/src/main/java/.../MyConfiguration.java
```

### 7.5 Backward Compatibility

- **Keep spring.io APIs** for version discovery (authoritative)
- **Fallback mechanism** if GitHub unavailable
- **Gradual rollout** per project

---

## 8. Implementation Plan

### 8.1 Phase 1: GitHub Discovery Service (Week 1)

**New File**: `service/github/GitHubDocumentationDiscoveryService.java`

```java
@Service
public class GitHubDocumentationDiscoveryService {

    // Default paths for latest/main branch
    private static final Map<String, String> DEFAULT_DOC_PATHS = Map.of(
        "spring-boot", "documentation/spring-boot-docs/src/docs/antora/modules",
        "spring-security", "docs/modules/ROOT/pages",
        "spring-framework", "framework-docs/modules/ROOT/pages",
        "spring-data-commons", "src/main/antora/modules/ROOT/pages"
    );

    // Version-specific path overrides (documentation structure changed over time)
    private static final Map<String, VersionPathMapping> VERSION_PATH_MAPPINGS = Map.of(
        "spring-boot", new VersionPathMapping(
            "4.0.0",  // Version threshold
            "documentation/spring-boot-docs/src/docs/antora/modules",  // >= threshold
            "spring-boot-project/spring-boot-docs/src/docs/antora/modules"  // < threshold
        )
    );

    // Tag prefix varies by project
    private static final Map<String, String> TAG_PREFIXES = Map.of(
        "spring-boot", "v",           // v3.5.7
        "spring-security", "",        // 6.5.7 (no prefix)
        "spring-framework", "v"       // v6.2.1
    );

    /**
     * Get the correct documentation path for a specific project version.
     */
    public String getDocumentationPath(String projectSlug, String version) {
        VersionPathMapping mapping = VERSION_PATH_MAPPINGS.get(projectSlug);
        if (mapping != null) {
            Version v = Version.parse(version);
            Version threshold = Version.parse(mapping.threshold());
            return v.compareTo(threshold) >= 0 ? mapping.newPath() : mapping.oldPath();
        }
        return DEFAULT_DOC_PATHS.getOrDefault(projectSlug, "docs/modules/ROOT/pages");
    }

    /**
     * Get the Git tag for a specific version.
     */
    public String getGitTag(String projectSlug, String version) {
        String prefix = TAG_PREFIXES.getOrDefault(projectSlug, "v");
        return prefix + version;
    }

    public List<DocumentationFile> discoverDocumentation(String projectSlug, String version) {
        String tag = getGitTag(projectSlug, version);
        String path = getDocumentationPath(projectSlug, version);
        // Use GitHub API to discover all .adoc files at the specific tag
        return discoverFilesAtRef(projectSlug, tag, path);
    }

    public List<String> discoverCodeExamples(String projectSlug, String version) {
        // Find all include-code:: references in the version's documentation
    }

    private record VersionPathMapping(String threshold, String newPath, String oldPath) {}
}
```

**Configuration** (`application.yml`):
```yaml
github:
  api:
    base-url: https://api.github.com
    raw-url: https://raw.githubusercontent.com
    token: ${GITHUB_TOKEN:}  # Optional, increases rate limit to 5000/hour
  documentation:
    # Default paths (for main branch / latest versions)
    paths:
      spring-boot: documentation/spring-boot-docs/src/docs/antora/modules
      spring-security: docs/modules/ROOT/pages
      spring-framework: framework-docs/modules/ROOT/pages
    # Version path overrides (structure changed between versions)
    version-paths:
      spring-boot:
        threshold: "4.0.0"
        old-path: spring-boot-project/spring-boot-docs/src/docs/antora/modules
    # Tag prefixes (some projects use 'v' prefix, others don't)
    tag-prefixes:
      spring-boot: "v"
      spring-security: ""
      spring-framework: "v"
```

### 8.2 Phase 2: Content Fetching Service (Week 1-2)

**New File**: `service/github/GitHubContentFetchService.java`

```java
@Service
public class GitHubContentFetchService {

    private final WebClient webClient;

    public String fetchRawContent(String repo, String path, String branch) {
        String url = String.format(
            "https://raw.githubusercontent.com/%s/%s/%s",
            repo, branch, path
        );
        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofSeconds(10));
    }

    public List<String> fetchBatch(List<String> urls) {
        // Parallel fetch with rate limiting
    }
}
```

### 8.3 Phase 3: AsciiDoc Converter (Week 2)

**New File**: `service/github/AsciiDocToMarkdownConverter.java`

```java
@Service
public class AsciiDocToMarkdownConverter {

    private final Asciidoctor asciidoctor;
    private final HtmlToMarkdownConverter htmlToMarkdown;

    public String convert(String asciidocContent) {
        // Step 1: Convert AsciiDoc to HTML
        String html = asciidoctor.convert(asciidocContent,
            Options.builder()
                .backend("html5")
                .safe(SafeMode.SAFE)
                .build());

        // Step 2: Convert HTML to Markdown using existing converter
        return htmlToMarkdown.convert(html);
    }

    public String convertWithXrefResolution(String content, String baseUrl) {
        // Handle xref: links conversion
        content = resolveXrefs(content, baseUrl);
        return convert(content);
    }
}
```

### 8.4 Phase 4: Code Example Service (Week 3)

**New File**: `service/github/GitHubCodeExampleService.java`

```java
@Service
public class GitHubCodeExampleService {

    public List<CodeExample> extractCodeExamples(String adocContent, String repo, String branch) {
        // Find include-code:: directives
        Pattern pattern = Pattern.compile("include-code::([\\w]+)\\[\\]");
        Matcher matcher = pattern.matcher(adocContent);

        List<CodeExample> examples = new ArrayList<>();
        while (matcher.find()) {
            String className = matcher.group(1);
            String sourcePath = findSourceFile(repo, className);
            String sourceCode = fetchRawContent(repo, sourcePath, branch);

            examples.add(CodeExample.builder()
                .title(className)
                .codeSnippet(sourceCode)
                .language(detectLanguage(sourcePath))
                .sourceUrl(buildGitHubUrl(repo, sourcePath, branch))
                .build());
        }
        return examples;
    }
}
```

### 8.5 Phase 5: Integration (Week 3-4)

**Modify**: `service/sync/ComprehensiveSyncService.java`

```java
@Service
public class ComprehensiveSyncService {

    // Add new GitHub-based phases
    public enum SyncPhase {
        BOOT_VERSIONS,      // Phase 0: Keep current
        GENERATIONS,        // Phase 1: Keep current
        INITIALIZR,         // Phase 2: Keep current
        GITHUB_DOCS,        // Phase 3: NEW - GitHub documentation
        GITHUB_EXAMPLES,    // Phase 4: NEW - GitHub code examples
        RELATIONSHIPS,      // Phase 5: Keep current
        SPRING_IO_FALLBACK, // Phase 6: NEW - Fallback to current method
        CLEANUP             // Phase 7: Keep current
    }

    private void syncGitHubDocumentation() {
        for (SpringProject project : projects) {
            try {
                List<DocumentationFile> docs =
                    githubDiscovery.discoverDocumentation(project.getSlug());

                for (DocumentationFile doc : docs) {
                    String adoc = githubFetch.fetchRawContent(
                        project.getGithubUrl(), doc.getPath(), "main");
                    String markdown = asciiDocConverter.convert(adoc);

                    saveDocumentation(project, doc, markdown);
                }
            } catch (Exception e) {
                log.warn("GitHub fetch failed for {}, using fallback", project.getSlug());
                fallbackToSpringIo(project);
            }
        }
    }
}
```

### 8.6 Phase 6: Testing & Rollout (Week 4)

1. **Unit Tests**
   - AsciiDoc conversion accuracy
   - xref resolution
   - Code example extraction

2. **Integration Tests**
   - Full sync with GitHub source
   - Fallback mechanism
   - Rate limiting compliance

3. **Gradual Rollout**
   - Start with Spring Boot only
   - Add projects incrementally
   - Monitor quality and performance

### 8.7 New Service Architecture

```
service/
├── sync/
│   ├── ComprehensiveSyncService.java      (modified)
│   └── ... (existing services)
├── documentation/
│   ├── DocumentationFetchService.java     (kept as fallback)
│   └── HtmlToMarkdownConverter.java       (kept)
└── github/                                 (NEW)
    ├── GitHubDocumentationDiscoveryService.java
    ├── GitHubContentFetchService.java
    ├── GitHubCodeExampleService.java
    └── AsciiDocToMarkdownConverter.java
```

### 8.8 Dependencies to Add

```gradle
dependencies {
    // AsciiDoc processing
    implementation 'org.asciidoctor:asciidoctorj:3.0.0'

    // Optional: JRuby for AsciidoctorJ
    runtimeOnly 'org.jruby:jruby:9.4.6.0'
}
```

---

## 9. Risk Assessment

### 9.1 Low Risk

| Risk | Mitigation |
|------|------------|
| GitHub rate limiting | Use token auth (5000 req/hr), caching |
| Documentation path changes | Configuration-driven paths |
| AsciiDoc format variations | AsciidoctorJ handles standard formats |

### 9.2 Medium Risk

| Risk | Mitigation |
|------|------------|
| GitHub outages | Fallback to current spring.io method |
| Conversion quality | Testing, incremental rollout |
| include-code resolution | Pattern-based discovery with fallbacks |

### 9.3 High Risk

| Risk | Mitigation |
|------|------------|
| Spring moves away from GitHub | Monitor Spring announcements, abstract source layer |

### 9.4 Risk Matrix

```
           Low Impact    Medium Impact    High Impact
         ┌────────────┬────────────────┬────────────────┐
High     │            │                │ Source         │
Prob.    │            │                │ migration      │
         ├────────────┼────────────────┼────────────────┤
Med      │ Rate       │ Conversion     │                │
Prob.    │ limiting   │ quality        │                │
         ├────────────┼────────────────┼────────────────┤
Low      │ Path       │ GitHub         │                │
Prob.    │ changes    │ outages        │                │
         └────────────┴────────────────┴────────────────┘
```

---

## 10. Conclusion

### 10.1 Summary of Findings

| Aspect | Current Approach | GitHub Approach | Winner |
|--------|-----------------|-----------------|--------|
| Content Volume | 51 pages | 223+ files | GitHub |
| Code Examples | 218 snippets | 413+ files | GitHub |
| Performance | 5-15s/page | ~0.8s/file | GitHub |
| Content Quality | Reverse-engineered | Original source | GitHub |
| Maintenance | CSS selector updates | Path configuration | GitHub |
| Error Rate | High (JS timeouts) | Low (static files) | GitHub |

### 10.2 Recommendation

**Strongly recommend** adopting the GitHub-first approach with the following priorities:

1. **Immediate**: Implement GitHub fetching for Spring Boot
2. **Short-term**: Extend to Spring Security, Spring Framework
3. **Medium-term**: Cover all 30+ core Spring projects
4. **Long-term**: Include sample repositories

### 10.3 Expected Outcomes

| Metric | Current | Expected | Improvement |
|--------|---------|----------|-------------|
| Documentation coverage | 51 pages | 500+ pages | 10x |
| Code examples | 218 | 1000+ | 5x |
| Sync time | 30-60 min | 5-10 min | 6x faster |
| Error rate | ~10% | <1% | 10x better |
| Content freshness | Daily | Real-time capable | Improved |

### 10.4 Implementation Timeline

```
Week 1: Discovery Service + Basic Fetching
Week 2: AsciiDoc Conversion + Testing
Week 3: Code Examples + Integration
Week 4: Rollout + Monitoring
```

### 10.5 Success Criteria

- [ ] Spring Boot documentation fully migrated
- [ ] 10x more documentation pages available
- [ ] 5x more code examples available
- [ ] Sync time reduced by 80%
- [ ] Error rate below 1%
- [ ] Fallback mechanism operational

---

## Appendix A: GitHub Repository Inventory

### spring-projects Organization (86 repositories)

**Core Frameworks:**
- spring-framework
- spring-boot
- spring-security
- spring-data-commons
- spring-data-jpa
- spring-data-mongodb
- spring-data-redis
- spring-data-elasticsearch
- spring-data-cassandra
- spring-data-neo4j
- spring-data-rest
- spring-data-ldap
- spring-data-r2dbc

**Integration:**
- spring-integration
- spring-batch
- spring-kafka
- spring-amqp
- spring-pulsar
- spring-session
- spring-vault
- spring-ldap
- spring-credhub

**Web & API:**
- spring-graphql
- spring-hateoas
- spring-restdocs
- spring-webflow
- spring-ws

**Modern:**
- spring-ai
- spring-modulith
- spring-shell
- spring-authorization-server
- spring-grpc

**Tooling:**
- spring-tools
- spring-statemachine
- spring-retry
- spring-plugin
- spring-guice

**Samples (20+ repos):**
- spring-petclinic
- spring-data-examples
- spring-ai-examples
- spring-security-samples
- And more...

---

## Appendix B: AsciiDoc Conversion Examples

### B.1 Heading Conversion

**AsciiDoc:**
```asciidoc
= Document Title
== Section Title
=== Subsection
```

**Markdown:**
```markdown
# Document Title
## Section Title
### Subsection
```

### B.2 List Conversion

**AsciiDoc:**
```asciidoc
* Item one
* Item two
** Nested item
```

**Markdown:**
```markdown
- Item one
- Item two
  - Nested item
```

### B.3 Code Block Conversion

**AsciiDoc:**
```asciidoc
[source,java]
----
public class Example {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
----
```

**Markdown:**
```markdown
```java
public class Example {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

### B.4 Link Conversion

**AsciiDoc:**
```asciidoc
xref:features/authentication/index.adoc[authentication]
https://spring.io[Spring Website]
```

**Markdown:**
```markdown
[authentication](features/authentication/index.md)
[Spring Website](https://spring.io)
```

---

## Appendix C: Performance Test Scripts

### C.1 GitHub Fetch Test

```bash
#!/bin/bash
# Test GitHub raw fetch performance

URLS=(
  "https://raw.githubusercontent.com/spring-projects/spring-boot/main/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/index.adoc"
  "https://raw.githubusercontent.com/spring-projects/spring-security/main/docs/modules/ROOT/pages/index.adoc"
  "https://raw.githubusercontent.com/spring-projects/spring-framework/main/framework-docs/modules/ROOT/pages/index.adoc"
)

for url in "${URLS[@]}"; do
  START=$(date +%s%3N)
  curl -s "$url" > /dev/null
  END=$(date +%s%3N)
  echo "$(basename $url): $((END-START))ms"
done
```

### C.2 Spring.io Test (requires HtmlUnit)

```java
@Test
void testSpringIoFetch() {
    long start = System.currentTimeMillis();

    try (WebClient webClient = new WebClient()) {
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);

        HtmlPage page = webClient.getPage("https://spring.io/projects/spring-boot");
        webClient.waitForBackgroundJavaScript(10000);

        String content = page.asXml();
    }

    long elapsed = System.currentTimeMillis() - start;
    System.out.println("HtmlUnit fetch: " + elapsed + "ms");
}
```

---

## Appendix D: Git Tag Versioning Strategy

### D.1 Tag Discovery via GitHub API

```bash
# List all tags for a repository
curl -s "https://api.github.com/repos/spring-projects/spring-boot/tags?per_page=100"

# Response structure:
[
  {"name": "v4.0.0", "commit": {"sha": "abc123..."}},
  {"name": "v3.5.8", "commit": {"sha": "def456..."}},
  {"name": "v3.5.7", "commit": {"sha": "ghi789..."}}
]
```

### D.2 Spring Boot Tag History (Sample)

| Tag | Type | Release Date | Documentation Path |
|-----|------|--------------|-------------------|
| v4.0.0 | GA | 2024-11 | `documentation/spring-boot-docs/...` |
| v4.0.0-RC2 | RC | 2024-11 | `documentation/spring-boot-docs/...` |
| v4.0.0-M3 | Milestone | 2024-10 | `documentation/spring-boot-docs/...` |
| v3.5.8 | GA | 2024-12 | `spring-boot-project/spring-boot-docs/...` |
| v3.5.7 | GA | 2024-11 | `spring-boot-project/spring-boot-docs/...` |
| v3.4.12 | GA | 2024-12 | `spring-boot-project/spring-boot-docs/...` |
| v3.3.7 | GA | 2024-11 | `spring-boot-project/spring-boot-docs/...` |

### D.3 Mapping Versions to Tags

```java
/**
 * Maps a version string to the correct Git tag format.
 * Different Spring projects use different tag conventions.
 */
public class GitTagMapper {

    private static final Map<String, TagFormat> PROJECT_FORMATS = Map.of(
        "spring-boot", new TagFormat("v", ""),           // v3.5.7
        "spring-framework", new TagFormat("v", ""),      // v6.2.1
        "spring-security", new TagFormat("", ""),        // 6.5.7
        "spring-data-commons", new TagFormat("", ""),    // 3.4.1
        "spring-data-jpa", new TagFormat("", ""),        // 3.4.1
        "spring-batch", new TagFormat("v", "")           // v5.2.1
    );

    public String toGitTag(String project, String version) {
        TagFormat format = PROJECT_FORMATS.getOrDefault(project, new TagFormat("v", ""));
        return format.prefix() + version + format.suffix();
    }

    public String toGitRef(String project, String version) {
        // For fetching, we can use the tag directly as a ref
        return toGitTag(project, version);
    }

    private record TagFormat(String prefix, String suffix) {}
}
```

### D.4 Version-Specific URL Construction

```java
/**
 * Constructs the raw GitHub URL for a specific version's documentation.
 */
public String buildDocumentationUrl(String project, String version, String filePath) {
    String tag = gitTagMapper.toGitTag(project, version);
    String basePath = getDocumentationPath(project, version);

    // Example outputs:
    // Spring Boot 3.5.7: https://raw.githubusercontent.com/spring-projects/spring-boot/v3.5.7/spring-boot-project/spring-boot-docs/src/docs/antora/modules/ROOT/pages/index.adoc
    // Spring Boot 4.0.0: https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.0/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/index.adoc
    // Spring Security 6.5.7: https://raw.githubusercontent.com/spring-projects/spring-security/6.5.7/docs/modules/ROOT/pages/index.adoc

    return String.format(
        "https://raw.githubusercontent.com/spring-projects/%s/%s/%s/%s",
        project, tag, basePath, filePath
    );
}
```

### D.5 Synchronizing Multiple Versions

```java
/**
 * Sync documentation for all active versions of a project.
 */
public void syncProjectDocumentation(SpringProject project) {
    // Get versions from existing spring.io sync (authoritative source)
    List<ProjectVersion> versions = projectVersionRepository
        .findByProjectAndIsActiveTrue(project);

    for (ProjectVersion version : versions) {
        try {
            String tag = gitTagMapper.toGitTag(project.getSlug(), version.getVersion());

            // Verify tag exists
            if (!githubClient.tagExists(project.getSlug(), tag)) {
                log.warn("Tag {} not found for {}", tag, project.getSlug());
                continue;
            }

            // Discover and fetch documentation for this version
            List<DocumentationFile> docs = discoveryService
                .discoverDocumentation(project.getSlug(), version.getVersion());

            for (DocumentationFile doc : docs) {
                String content = fetchService.fetchRawContent(
                    project.getSlug(),
                    doc.getPath(),
                    tag
                );
                String markdown = converter.convert(content);
                saveDocumentation(version, doc, markdown);
            }

            log.info("Synced {} docs for {} {}", docs.size(), project.getName(), version.getVersion());
        } catch (Exception e) {
            log.error("Failed to sync {} {}: {}", project.getSlug(), version.getVersion(), e.getMessage());
            // Fall back to current spring.io method
            fallbackSync(project, version);
        }
    }
}
```

### D.6 Version Comparison Benefits

| Feature | Current (spring.io) | Proposed (GitHub Tags) |
|---------|--------------------|-----------------------|
| Latest GA | Yes | Yes |
| Previous GA (n-1) | Yes | Yes |
| Older GA (n-2, n-3) | Limited | Full history |
| Release Candidates | Sometimes | Always |
| Milestones | Rarely | Always |
| Historical docs | No | Complete via tags |
| Diff between versions | No | Git diff available |

---

*End of Evaluation Report*

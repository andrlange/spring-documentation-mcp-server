# Spring MCP Server - Version Configuration

This document tracks the version configuration across all project files to ensure consistency.

## Current Versions

| Component | Version | Notes |
|-----------|---------|-------|
| **Application** | 1.3.0 | Spring MCP Server |
| **Java (JDK)** | 25 | LTS version |
| **Spring Boot** | 3.5.8 | Latest stable |
| **Spring AI** | 1.1.0 | MCP Server support |
| **PostgreSQL** | 18-alpine | Docker image |
| **Gradle** | 9.2.0 | Build tool |

## Files with Version References

### build.gradle
```groovy
version = '1.3.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

ext {
    springAiVersion = '1.1.0'
}
```

### Dockerfile
```dockerfile
FROM eclipse-temurin:25-jdk-alpine AS builder
FROM eclipse-temurin:25-jre-alpine
```

### docker-compose.yml
```yaml
services:
  postgres:
    image: postgres:18-alpine
```

### docker-compose-all.yaml
```yaml
services:
  postgres:
    image: postgres:18-alpine
  spring-mcp-server:
    image: spring-mcp-server:1.3.0
```

### application.yml
```yaml
# Application Info (used in footer)
info:
  app:
    name: Spring MCP Server
    version: 1.3.0
  spring-boot:
    version: 3.5.8

spring:
  ai:
    mcp:
      server:
        version: "1.3.0"
```

### build-container.sh
```bash
APP_VERSION="1.3.0"
JAVA_VERSION="25"
```

## Updating Versions

When updating versions, ensure all files listed above are updated consistently:

1. **Application Version Update**:
   - `build.gradle` - `version` property
   - `application.yml` - `info.app.version` and `spring.ai.mcp.server.version`
   - `build-container.sh` - `APP_VERSION`
   - `docker-compose-all.yaml` - `spring-mcp-server` image tag
   - `README.md` - jar filename and changelog

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

Current: **31 tools** (10 documentation + 7 migration + 6 language evolution + 8 flavors)

Update these locations when adding/removing tools:
- `application.yml` - `spring.ai.mcp.server.instructions`
- `README.md` - tool documentation
- `.claude/memory/project-memory.md` - MCP Tools count

## Changelog

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

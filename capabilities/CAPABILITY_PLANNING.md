# Spring MCP Server - Capability Analysis & Planning

> **Analysis Date**: 2025-11-27
> **Purpose**: Document Spring MCP Server capabilities for backend and fullstack development with Spring Boot 3.x/4.x

---

## Executive Summary

The Spring MCP Server provides **10 specialized tools** for accessing Spring ecosystem documentation, covering **55 Spring projects** with comprehensive version management for both **Spring Boot 3.x** (production) and **Spring Boot 4.x** (latest GA). This server is ideal for AI-assisted Spring Boot development with Java and Kotlin.

---

## 1. Available MCP Tools

### 1.1 Project Discovery Tools

| Tool | Purpose | Key Parameters |
|------|---------|----------------|
| `listSpringProjects` | List all 55 Spring projects | None |
| `findProjectsByUseCase` | Search projects by use case | `useCase` (e.g., "security", "data access", "cloud") |

### 1.2 Version Management Tools

| Tool | Purpose | Key Parameters |
|------|---------|----------------|
| `listSpringBootVersions` | List all Spring Boot versions | `state` (GA, RC, SNAPSHOT, MILESTONE), `limit` |
| `getSpringVersions` | Get versions for a specific project | `project` (slug) |
| `getLatestSpringBootVersion` | Get latest patch for major.minor | `majorVersion`, `minorVersion` |
| `filterSpringBootVersionsBySupport` | Filter by support status | `supportActive` (true/false), `limit` |
| `listProjectsBySpringBootVersion` | List compatible projects | `majorVersion`, `minorVersion` |

### 1.3 Documentation Tools

| Tool | Purpose | Key Parameters |
|------|---------|----------------|
| `searchSpringDocs` | Search across documentation | `query`, `project`, `version`, `docType` |
| `getDocumentationByVersion` | Get all docs for a version | `project`, `version` |
| `getCodeExamples` | Search code examples | `query`, `project`, `version`, `language`, `limit` |

---

## 2. Spring Boot Version Coverage

### 2.1 Currently Supported Versions (OSS Active)

| Version | State | Release Date | OSS Support End | Enterprise End |
|---------|-------|--------------|-----------------|----------------|
| **4.0.0** | GA (Current) | 2025-11 | 2026-12 | 2027-12 |
| **3.5.8** | GA | 2025-05 | 2026-06 | 2032-06 |
| **3.5.7** | GA | 2025-05 | 2026-06 | 2032-06 |
| **3.4.12** | GA | 2024-11 | 2025-12 | 2026-12 |
| **3.4.11** | GA | 2024-11 | 2025-12 | 2026-12 |

### 2.2 Pre-release Versions Available

| Version | State | Purpose |
|---------|-------|---------|
| 4.0.1-SNAPSHOT | SNAPSHOT | Next patch development |
| 4.0.0-RC2 | RC | Release candidate |
| 3.5.9-SNAPSHOT | SNAPSHOT | 3.5 maintenance |
| 3.4.13-SNAPSHOT | SNAPSHOT | 3.4 maintenance |

### 2.3 Legacy Versions (Enterprise Support Only)

| Version | OSS End | Enterprise End |
|---------|---------|----------------|
| 3.3.13 | 2025-06 | 2026-06 |
| 3.2.12 | 2024-12 | 2025-12 |
| 2.7.18 | 2023-06 | 2029-06 |

---

## 3. Spring Project Ecosystem Coverage

### 3.1 Core Projects (55 Total)

#### Framework & Boot
- **Spring Framework** - Core programming model
- **Spring Boot** - Opinionated application framework

#### Data Access (12 projects)
- Spring Data (Commons)
- Spring Data JPA
- Spring Data JDBC
- Spring Data R2DBC (Reactive)
- Spring Data MongoDB
- Spring Data Redis
- Spring Data Cassandra
- Spring Data LDAP
- Spring Data REST
- Spring LDAP
- Spring Batch
- Spring Integration

#### Security (3 projects)
- Spring Security
- Spring Authorization Server
- Spring Security Kerberos

#### Cloud & Microservices (22 projects)
- Spring Cloud (umbrella)
- Spring Cloud Config
- Spring Cloud Gateway
- Spring Cloud Netflix
- Spring Cloud OpenFeign
- Spring Cloud Kubernetes
- Spring Cloud Consul
- Spring Cloud Zookeeper
- Spring Cloud Vault
- Spring Cloud Bus
- Spring Cloud Stream
- Spring Cloud Function
- Spring Cloud Contract
- Spring Cloud Circuit Breaker
- Spring Cloud Task
- Spring Cloud Sleuth
- Spring Cloud Data Flow
- Spring Cloud App Broker
- Spring Cloud Open Service Broker
- Spring Cloud CLI
- Spring Cloud Commons
- Spring Cloud Stream Applications

#### Web & API (6 projects)
- Spring HATEOAS
- Spring REST Docs
- Spring for GraphQL
- Spring Web Services
- Spring Web Flow
- Spring Session

#### Messaging (3 projects)
- Spring AMQP
- Spring for Apache Kafka
- Spring for Apache Pulsar

#### Modern Development (4 projects)
- Spring AI
- Spring Modulith
- Spring Shell
- Spring Statemachine

#### Infrastructure (3 projects)
- Spring Vault
- Spring CredHub
- Spring Session for Apache Geode

---

## 4. Capability Matrix by Development Scenario

### 4.1 REST API Development

| Capability | Tool | Example Query |
|------------|------|---------------|
| Find REST projects | `findProjectsByUseCase` | `useCase: "web"` |
| Get Spring MVC docs | `searchSpringDocs` | `query: "REST API", project: "spring-boot"` |
| HATEOAS integration | `getSpringVersions` | `project: "spring-hateoas"` |
| API documentation | `getSpringVersions` | `project: "spring-restdocs"` |

### 4.2 Data Access Development

| Capability | Tool | Example Query |
|------------|------|---------------|
| Find data projects | `findProjectsByUseCase` | `useCase: "data access"` |
| JPA configuration | `getDocumentationByVersion` | `project: "spring-data-jpa"` |
| Reactive data | `listProjectsBySpringBootVersion` | Filter for R2DBC |
| Multi-database | `searchSpringDocs` | `query: "multiple datasources"` |

### 4.3 Security Implementation

| Capability | Tool | Example Query |
|------------|------|---------------|
| Security projects | `findProjectsByUseCase` | `useCase: "security"` |
| OAuth2/OIDC | `searchSpringDocs` | `query: "OAuth2", project: "spring-security"` |
| Authorization Server | `getSpringVersions` | `project: "spring-authorization-server"` |

### 4.4 Microservices Architecture

| Capability | Tool | Example Query |
|------------|------|---------------|
| Cloud projects | `findProjectsByUseCase` | `useCase: "cloud"` |
| Service discovery | `getDocumentationByVersion` | `project: "spring-cloud-consul"` |
| API Gateway | `getSpringVersions` | `project: "spring-cloud-gateway"` |
| Config management | `getDocumentationByVersion` | `project: "spring-cloud-config"` |

### 4.5 AI/ML Integration

| Capability | Tool | Example Query |
|------------|------|---------------|
| AI projects | `findProjectsByUseCase` | `useCase: "AI"` |
| Spring AI docs | `getDocumentationByVersion` | `project: "spring-ai", version: "1.1.0"` |
| Compatible versions | `listProjectsBySpringBootVersion` | Check Spring AI compatibility |

### 4.6 Event-Driven Architecture

| Capability | Tool | Example Query |
|------------|------|---------------|
| Kafka integration | `getSpringVersions` | `project: "spring-kafka"` |
| AMQP/RabbitMQ | `getDocumentationByVersion` | `project: "spring-amqp"` |
| Stream processing | `getSpringVersions` | `project: "spring-cloud-stream"` |

---

## 5. Spring Boot 3.x vs 4.x Compatibility

### 5.1 Spring Boot 3.5.x Compatible Projects (48 projects)

Key projects with their compatible versions:
- Spring Framework 6.2.x
- Spring Security 6.5.x
- Spring Data 2025.0.x
- Spring Cloud 2025.0.x
- Spring AI 1.1.x
- Spring Batch 5.2.x

### 5.2 Spring Boot 4.0.x Compatible Projects (42 projects)

Key projects with their compatible versions:
- Spring Framework 7.0.x
- Spring Security 7.0.x
- Spring Data 2025.1.x
- Spring Cloud 2025.1.x
- Spring Batch 6.0.x
- Spring Integration 7.0.x

### 5.3 Migration Considerations

| Component | 3.5.x Version | 4.0.x Version | Breaking Changes |
|-----------|---------------|---------------|------------------|
| Spring Framework | 6.2.x | 7.0.x | Check migration guide |
| Spring Security | 6.5.x | 7.0.x | Check migration guide |
| Spring Data | 3.5.x | 4.0.x | Check migration guide |
| Java Baseline | Java 17+ | Java 17+ (21 recommended) | None |

---

## 6. Use Case Search Capabilities

### 6.1 Supported Use Case Keywords

| Keyword | Projects Found | Primary Project |
|---------|----------------|-----------------|
| `security` | 2 | Spring Security |
| `data access` | 1 | Spring Data |
| `cloud` | 22 | Spring Cloud ecosystem |
| `batch` | 1 | Spring Batch |
| `GraphQL` | 1 | Spring for GraphQL |
| `AI` | 1 | Spring AI |
| `web` | 2 | Spring Web Flow, WS |

### 6.2 Search Limitations

- `messaging` returns 0 results (use specific projects: spring-kafka, spring-amqp)
- Use case search is based on project names and descriptions
- For detailed searches, use `searchSpringDocs` with specific queries

---

## 7. Documentation URL Patterns

### 7.1 Reference Documentation

```
https://docs.spring.io/{project}/reference/{version}/
https://docs.spring.io/{project}/docs/{version}/reference/html/
```

### 7.2 API Documentation

```
https://docs.spring.io/{project}/api/java/index.html
https://docs.spring.io/{project}/docs/{version}/api/
```

### 7.3 Version-Specific Patterns

| Version State | URL Pattern |
|---------------|-------------|
| GA (Current) | `/reference/` or `/index.html` |
| GA (Specific) | `/reference/{version}/` |
| SNAPSHOT | `/{version}-SNAPSHOT/` |
| RC | `/{version}/` |

---

## 8. Recommended Usage Patterns

### 8.1 Starting a New Project

```
1. listSpringBootVersions(state: "GA", limit: 5) -> Pick latest stable
2. listProjectsBySpringBootVersion(majorVersion: 3, minorVersion: 5) -> See compatible projects
3. findProjectsByUseCase(useCase: "your-use-case") -> Find relevant projects
4. getDocumentationByVersion(project: "selected-project", version: "x.y.z") -> Get docs
```

### 8.2 Upgrading Spring Boot Version

```
1. getLatestSpringBootVersion(majorVersion: 4, minorVersion: 0) -> Check latest 4.x
2. listProjectsBySpringBootVersion(majorVersion: 4, minorVersion: 0) -> Check compatibility
3. searchSpringDocs(query: "migration", project: "spring-boot") -> Find migration guides
```

### 8.3 Adding a New Feature

```
1. findProjectsByUseCase(useCase: "feature-keyword") -> Find relevant project
2. getSpringVersions(project: "project-slug") -> Check versions
3. getCodeExamples(query: "feature", project: "project-slug") -> Get examples
4. searchSpringDocs(query: "specific-topic") -> Deep dive documentation
```

---

## 9. Planned Example Use Cases (for /examples folder)

### 9.1 Basic Examples
- [ ] REST API with Spring Boot 3.5.x
- [ ] REST API with Spring Boot 4.0.x
- [ ] JPA + PostgreSQL data access
- [ ] Spring Security basic auth
- [ ] Spring Security OAuth2

### 9.2 Intermediate Examples
- [ ] Reactive WebFlux API
- [ ] Spring Data R2DBC
- [ ] Spring Cloud Config client
- [ ] Spring Cloud Gateway routing
- [ ] Spring Kafka producer/consumer

### 9.3 Advanced Examples
- [ ] Microservices with Spring Cloud
- [ ] Spring AI integration
- [ ] Spring Modulith architecture
- [ ] GraphQL API
- [ ] Event-driven with Spring Cloud Stream

### 9.4 Kotlin Examples
- [ ] Kotlin REST API
- [ ] Kotlin coroutines with WebFlux
- [ ] Kotlin DSL configurations

---

## 10. Server Performance Metrics

Based on tool execution times observed:

| Operation | Typical Time |
|-----------|--------------|
| `listSpringProjects` | ~22ms |
| `listSpringBootVersions` | ~20ms |
| `findProjectsByUseCase` | ~2-9ms |
| `listProjectsBySpringBootVersion` | ~17-62ms |
| `getSpringVersions` | ~varies |
| `searchSpringDocs` | ~72ms |
| `getCodeExamples` | ~26ms |

---

## 11. Current Limitations & Observations

### 11.1 Limitations
1. `searchSpringDocs` may return empty results for some queries - requires specific terms
2. `getCodeExamples` database appears to need population
3. `getDocumentationByVersion` returns empty for some project/version combinations
4. Use case search limited to project names/descriptions

### 11.2 Strengths
1. Comprehensive version tracking with support dates
2. Excellent Spring Boot compatibility mapping
3. Fast response times for most operations
4. Complete coverage of 55 Spring projects
5. Both OSS and Enterprise support tracking

---

## 12. Integration with Claude Code

### 12.1 MCP Configuration

```json
{
  "mcpServers": {
    "spring": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/spring/sse"
    }
  }
}
```

### 12.2 Available Endpoints

- **SSE Endpoint**: `/mcp/spring/sse`
- **Messages Endpoint**: `/mcp/spring/messages`

---

## Appendix A: Complete Project List

| # | Project | Slug | GitHub |
|---|---------|------|--------|
| 1 | Spring Boot | spring-boot | spring-projects/spring-boot |
| 2 | Spring Framework | spring-framework | spring-projects/spring-framework |
| 3 | Spring Data | spring-data | spring-projects/spring-data |
| 4 | Spring Security | spring-security | spring-projects/spring-security |
| 5 | Spring Cloud | spring-cloud | spring-cloud |
| 6 | Spring AI | spring-ai | spring-projects/spring-ai |
| 7 | Spring Batch | spring-batch | spring-projects/spring-batch |
| 8 | Spring Integration | spring-integration | spring-projects/spring-integration |
| 9 | Spring Data JPA | spring-data-jpa | spring-projects/spring-data-jpa |
| 10 | Spring Data MongoDB | spring-data-mongodb | spring-projects/spring-data-mongodb |
| ... | (55 total projects) | ... | ... |

---

*Document generated from live MCP server analysis*

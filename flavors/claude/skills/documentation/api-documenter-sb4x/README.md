# Spring Boot 4.x API Documenter Skill

> Auto-generate OpenAPI 3 documentation for Spring Boot 4.x REST APIs using springdoc-openapi v3.0

**Requires:** Spring Boot 4.0+, Java 17+, Spring Framework 7.x

> **Note**: For Spring Boot 3.x projects, use the `api-documenter-sb3x` skill with springdoc-openapi v2.8.x.

## Quick Example

```java
// You write:
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

// Skill configures springdoc-openapi and generates full OpenAPI spec
```

## What It Generates

- OpenAPI 3.0/3.1 specifications
- Swagger UI (v5.30.1) / Scalar UI (v0.4.3) integration
- Request/response schemas from DTOs
- Security scheme configurations
- Grouped API definitions
- Example payloads and error responses
- Initial API versioning support (Spring Framework 7)

## Quick Start

**Maven:**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.0</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0'
```

**Access:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Key Features

| Feature | Support |
|---------|---------|
| Spring WebMvc | `springdoc-openapi-starter-webmvc-ui` |
| Spring WebFlux | `springdoc-openapi-starter-webflux-ui` |
| Swagger UI | v5.30.1 (built-in) |
| Scalar UI | v0.4.3 (`springdoc-openapi-starter-webmvc-scalar`) |
| Security Schemes | JWT, OAuth2, API Key, Basic |
| Grouped APIs | `GroupedOpenApi` beans |
| Javadoc Support | With therapi-runtime-javadoc |
| GraalVM Native | Supported |
| API Versioning | Initial support (Spring Framework 7) |

## Basic Configuration

```properties
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.packagesToScan=com.example.api
springdoc.pathsToMatch=/api/**
springdoc.swagger-ui.tryItOutEnabled=true
```

## Compatibility

| Spring Boot | Spring Framework | springdoc-openapi | Java |
|-------------|------------------|-------------------|------|
| **4.0.x** | **7.x** | **3.0.x** | **17+** |
| 3.5.x | 6.2.x | 2.8.x | 17+ |
| 3.4.x | 6.1.x | 2.7.x - 2.8.x | 17+ |
| 3.3.x | 6.0.x | 2.6.x | 17+ |

See [SKILL.md](SKILL.md) for complete documentation including:
- Detailed configuration options
- Security scheme examples
- Functional endpoints (WebMvc.fn)
- Migration guides (SpringFox, v1 to v3, **v2.x to v3.0**)
- Build-time generation (Maven/Gradle plugins)
- Common issues and solutions

## What's New in v3.0

- **Spring Boot 4.0.0 support** (required)
- **Spring Framework 7 compatibility**
- **Initial API versioning support** (#2975)
- **WebFlux static resources support** (#3123)
- **Swagger UI v5.30.1** (upgraded from v5.x)
- **Scalar v0.4.3** (upgraded)
- **Swagger Core v2.2.38** (upgraded)

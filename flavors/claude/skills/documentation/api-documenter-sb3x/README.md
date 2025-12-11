# Spring Boot API Documenter Skill

> Auto-generate OpenAPI 3 documentation for Spring Boot REST APIs using springdoc-openapi

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
- Swagger UI / Scalar UI integration
- Request/response schemas from DTOs
- Security scheme configurations
- Grouped API definitions
- Example payloads and error responses

## Quick Start

**Maven:**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.14</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14'
```

**Access:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Key Features

| Feature | Support |
|---------|---------|
| Spring WebMvc | `springdoc-openapi-starter-webmvc-ui` |
| Spring WebFlux | `springdoc-openapi-starter-webflux-ui` |
| Swagger UI | Built-in |
| Scalar UI | `springdoc-openapi-starter-webmvc-scalar` |
| Security Schemes | JWT, OAuth2, API Key, Basic |
| Grouped APIs | `GroupedOpenApi` beans |
| Javadoc Support | With therapi-runtime-javadoc |
| GraalVM Native | Supported |

## Basic Configuration

```properties
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.packagesToScan=com.example.api
springdoc.pathsToMatch=/api/**
springdoc.swagger-ui.tryItOutEnabled=true
```

## Compatibility

| Spring Boot | springdoc-openapi |
|-------------|-------------------|
| 3.5.x | 2.8.x |
| 3.4.x | 2.7.x - 2.8.x |
| 3.3.x | 2.6.x |
| 3.2.x | 2.3.x - 2.5.x |

See [SKILL.md](SKILL.md) for complete documentation including:
- Detailed configuration options
- Security scheme examples
- Functional endpoints (WebMvc.fn)
- Migration guides (SpringFox, v1 to v2)
- Build-time generation (Maven/Gradle plugins)
- Common issues and solutions

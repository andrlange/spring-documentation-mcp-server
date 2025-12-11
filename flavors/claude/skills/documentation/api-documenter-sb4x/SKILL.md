---
name: spring-boot-4x-api-documenter
description: Auto-generate OpenAPI 3 documentation for Spring Boot 4.x REST APIs using springdoc-openapi v3.0. Use when API endpoints change, controllers are modified, or user mentions API docs, Swagger, or OpenAPI. Triggers on @RestController changes, documentation requests, endpoint additions. Requires Spring Boot 4.0+ and Java 17+.
allowed-tools: Read, Write, Grep, Edit
---

# Spring Boot 4.x API Documenter Skill

Auto-generate OpenAPI 3 documentation for Spring Boot 4.x REST APIs using springdoc-openapi v3.0.0.

> **Note**: This skill is for **Spring Boot 4.x** projects. For Spring Boot 3.x, use the `api-documenter-sb3x` skill with springdoc-openapi v2.8.x.

## When I Activate

- API endpoints added/modified in Spring Boot 4.x
- User mentions API docs, OpenAPI, Swagger, or springdoc
- @RestController files changed
- Request/response DTOs modified
- Documentation configuration needed
- Migration from springdoc-openapi v2.x to v3.0
- API versioning configuration (Spring Framework 7 feature)

## What I Generate

### OpenAPI 3.0/3.1 Specifications
- Endpoint descriptions from annotations and Javadoc
- Request/response schemas
- Authentication/security requirements
- Example payloads
- Error responses via @ControllerAdvice
- Grouped API definitions

### Spring Boot 4.x Integration
- springdoc-openapi v3.0 dependency configuration
- Swagger UI (v5.30.1) and Scalar UI (v0.4.3) setup
- application.properties/yml configuration
- Security scheme definitions
- GroupedOpenApi bean configurations
- Initial API versioning support (Spring Framework 7)

---

## Quick Start

### Requirements

- **Spring Boot**: 4.0.0 or higher
- **Java**: 17 or higher (Jakarta EE 9)
- **Spring Framework**: 7.x

### Adding springdoc-openapi to Spring Boot 4.x

**Maven (WebMvc with Swagger UI):**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.0</version>
</dependency>
```

**Maven (WebFlux with Swagger UI):**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>3.0.0</version>
</dependency>
```

**Gradle (WebMvc with Swagger UI):**

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0'
```

**Gradle (WebFlux with Swagger UI):**

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.0'
```

### Default URLs After Setup

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/v3/api-docs.yaml`

---

## Examples

### Basic REST Controller Documentation

```java
// You write:
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    /**
     * Get user by ID
     * @param id User's unique identifier
     * @return User object if found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a single user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new user
     * @param user User data to create
     * @return Created user with ID
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User created = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

// I auto-generate OpenAPI spec with full documentation
```

### DTO Schema Documentation

```java
@Schema(description = "User entity representing a system user")
public class User {

    @Schema(description = "Unique identifier", example = "12345", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "User's full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String name;

    @Schema(description = "User's email address", example = "john.doe@example.com", format = "email")
    @Email
    private String email;

    @Schema(description = "Account creation timestamp", example = "2025-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "User's role in the system", example = "ADMIN")
    private UserRole role;
}

@Schema(enumAsRef = true, description = "User roles in the system")
public enum UserRole {
    ADMIN, USER, READONLY
}
```

### Multiple API Groups

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .displayName("Public API")
                .pathsToMatch("/api/public/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .displayName("Admin API")
                .pathsToMatch("/api/admin/**")
                .addOpenApiMethodFilter(method -> method.isAnnotationPresent(AdminOnly.class))
                .build();
    }

    @Bean
    public GroupedOpenApi storeApi() {
        String[] paths = {"/store/**"};
        return GroupedOpenApi.builder()
                .group("stores")
                .pathsToMatch(paths)
                .build();
    }
}
```

### Security Scheme Configuration

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "My Application API",
        version = "1.0.0",
        description = "REST API documentation",
        contact = @Contact(name = "API Support", email = "support@example.com"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    ),
    servers = {
        @Server(url = "https://api.example.com", description = "Production"),
        @Server(url = "http://localhost:8080", description = "Development")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Authentication"
)
@SecurityScheme(
    name = "apiKey",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-API-Key",
    description = "API Key Authentication"
)
public class OpenApiSecurityConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")));
    }
}
```

### Applying Security to Endpoints

```java
@RestController
@RequestMapping("/api/secure")
@SecurityRequirement(name = "bearerAuth")
public class SecureController {

    @GetMapping("/data")
    @Operation(
        summary = "Get secure data",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<SecureData> getSecureData() {
        return ResponseEntity.ok(secureService.getData());
    }

    @GetMapping("/admin")
    @Operation(
        summary = "Admin only endpoint",
        security = {
            @SecurityRequirement(name = "bearerAuth"),
            @SecurityRequirement(name = "apiKey")
        }
    )
    public ResponseEntity<AdminData> getAdminData() {
        return ResponseEntity.ok(adminService.getData());
    }
}
```

### Functional Endpoints (WebMvc.fn/WebFlux)

```java
@Configuration
public class RouterConfig {

    @Bean
    @RouterOperation(
        beanClass = EmployeeService.class,
        beanMethod = "findAllEmployees",
        operation = @Operation(
            operationId = "getAllEmployees",
            summary = "Get all employees",
            tags = {"employees"},
            responses = {
                @ApiResponse(responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Employee.class))))
            }
        )
    )
    RouterFunction<ServerResponse> getAllEmployeesRoute(EmployeeHandler handler) {
        return route(GET("/employees").and(accept(MediaType.APPLICATION_JSON)), handler::findAll);
    }

    @Bean
    @RouterOperations({
        @RouterOperation(path = "/persons", method = RequestMethod.GET,
            beanClass = PersonService.class, beanMethod = "getAll"),
        @RouterOperation(path = "/persons/{id}", method = RequestMethod.GET,
            beanClass = PersonService.class, beanMethod = "getById"),
        @RouterOperation(path = "/persons", method = RequestMethod.POST,
            beanClass = PersonService.class, beanMethod = "save")
    })
    RouterFunction<ServerResponse> personRoutes(PersonHandler handler) {
        return RouterFunctions
                .route(GET("/persons").and(accept(APPLICATION_JSON)), handler::findAll)
                .andRoute(GET("/persons/{id}").and(accept(APPLICATION_JSON)), handler::findById)
                .andRoute(POST("/persons").and(accept(APPLICATION_JSON)), handler::save);
    }
}
```

### Error Handling with @ControllerAdvice

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponse(responseCode = "404", description = "Entity not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(responseCode = "400", description = "Validation failed",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    public ValidationErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .collect(Collectors.toList());
        return new ValidationErrorResponse("VALIDATION_FAILED", errors);
    }
}

@Schema(description = "Error response structure")
public class ErrorResponse {
    @Schema(description = "Error code", example = "NOT_FOUND")
    private String code;

    @Schema(description = "Error message", example = "User with ID 123 not found")
    private String message;
}
```

### Pageable Support

```java
@GetMapping("/users")
@Operation(summary = "Get paginated users")
public Page<User> getUsers(
        @ParameterObject Pageable pageable,
        @Parameter(description = "Filter by name") @RequestParam(required = false) String name) {
    return userService.findAll(name, pageable);
}

// Pageable parameters are automatically documented with:
// - page (default: 0)
// - size (default: 20)
// - sort (e.g., "name,asc")
```

### File Upload Documentation

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Operation(summary = "Upload a file")
public ResponseEntity<FileInfo> uploadFile(
        @Parameter(description = "File to upload", required = true)
        @RequestPart("file") MultipartFile file,
        @Parameter(description = "File description")
        @RequestParam(required = false) String description) {
    FileInfo info = fileService.store(file, description);
    return ResponseEntity.ok(info);
}
```

---

## Configuration Reference

### application.properties

```properties
# Swagger UI path customization
springdoc.swagger-ui.path=/swagger-ui.html

# OpenAPI docs path
springdoc.api-docs.path=/v3/api-docs

# Enable/disable endpoints
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true

# Filter by packages
springdoc.packagesToScan=com.example.api.controller

# Filter by paths
springdoc.pathsToMatch=/api/**, /v1/**
springdoc.pathsToExclude=/internal/**

# Show actuator endpoints
springdoc.show-actuator=true

# Swagger UI customization
springdoc.swagger-ui.operationsSorter=alpha
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.swagger-ui.filter=true
springdoc.swagger-ui.docExpansion=none
springdoc.swagger-ui.defaultModelsExpandDepth=-1
springdoc.swagger-ui.displayRequestDuration=true

# CSRF support
springdoc.swagger-ui.csrf.enabled=true

# Disable petstore URL
springdoc.swagger-ui.disable-swagger-default-url=true

# OpenAPI version
springdoc.api-docs.version=openapi_3_1

# Pretty print output
springdoc.writer-with-default-pretty-printer=true

# Cache control
springdoc.cache.disabled=false
```

### application.yml

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
    version: openapi_3_1
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operationsSorter: alpha
    tagsSorter: alpha
    tryItOutEnabled: true
    filter: true
    docExpansion: none
    displayRequestDuration: true
    csrf:
      enabled: true
    disable-swagger-default-url: true
  packages-to-scan: com.example.api.controller
  paths-to-match: /api/**, /v1/**
  show-actuator: true
  cache:
    disabled: false
  writer-with-default-pretty-printer: true

  # Group configurations via YAML
  group-configs:
    - group: public-api
      paths-to-match: /api/public/**
    - group: admin-api
      paths-to-match: /api/admin/**
      packages-to-scan: com.example.api.admin
```

### Actuator Integration

```properties
# Expose on actuator port (different from app port)
springdoc.use-management-port=true
management.server.port=9090
management.endpoints.web.exposure.include=openapi, swagger-ui

# URLs become:
# http://localhost:9090/actuator/openapi
# http://localhost:9090/actuator/swagger-ui
```

---

## Module Reference

### Available Modules (v3.0.0)

| Module | Use Case | Bundled UI Version |
|--------|----------|-------------------|
| `springdoc-openapi-starter-webmvc-ui` | WebMvc + Swagger UI | Swagger UI v5.30.1 |
| `springdoc-openapi-starter-webmvc-api` | WebMvc without UI | - |
| `springdoc-openapi-starter-webflux-ui` | WebFlux + Swagger UI | Swagger UI v5.30.1 |
| `springdoc-openapi-starter-webflux-api` | WebFlux without UI | - |
| `springdoc-openapi-starter-webmvc-scalar` | WebMvc + Scalar UI | Scalar v0.4.3 |
| `springdoc-openapi-starter-webflux-scalar` | WebFlux + Scalar UI | Scalar v0.4.3 |

### Integrated Support (No Extra Dependencies)

- Spring Security (`@AuthenticationPrincipal` hidden)
- Spring HATEOAS (HAL links)
- Spring Data REST (`@RepositoryRestResource`)
- Spring Cloud Function Web
- Kotlin types
- Groovy types
- Javadoc comments (with therapi-runtime-javadoc)

### Javadoc Support Setup

**Maven:**

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.github.therapi</groupId>
                        <artifactId>therapi-runtime-javadoc-scribe</artifactId>
                        <version>0.15.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>

<dependency>
    <groupId>com.github.therapi</groupId>
    <artifactId>therapi-runtime-javadoc</artifactId>
    <version>0.15.0</version>
</dependency>
```

---

## Build-Time Generation

### Maven Plugin

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>-Dspring.application.admin.enabled=true</jvmArguments>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>start</goal>
                <goal>stop</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.5</version>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Run:** `mvn verify`

### Gradle Plugin

```groovy
plugins {
    id("org.springframework.boot") version "4.0.0"
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}
```

**Run:** `gradle clean generateOpenApiDocs`

---

## Migration Guides

### From springdoc-openapi v2.x to v3.0 (Spring Boot 3.x to 4.x)

**Prerequisites:**
- Upgrade to Spring Boot 4.0.0+
- Upgrade to Spring Framework 7.x
- Ensure Java 17+ (Jakarta EE 9)

**Step 1: Update dependencies**

| v2.x Module | v3.0 Module |
|-------------|-------------|
| `springdoc-openapi-starter-webmvc-ui:2.8.x` | `springdoc-openapi-starter-webmvc-ui:3.0.0` |
| `springdoc-openapi-starter-webflux-ui:2.8.x` | `springdoc-openapi-starter-webflux-ui:3.0.0` |
| `springdoc-openapi-starter-webmvc-api:2.8.x` | `springdoc-openapi-starter-webmvc-api:3.0.0` |
| `springdoc-openapi-starter-webflux-api:2.8.x` | `springdoc-openapi-starter-webflux-api:3.0.0` |

**Step 2: No breaking changes in configuration**
- All existing `springdoc.*` properties remain compatible
- Existing `GroupedOpenApi` beans continue to work
- OpenAPI annotations unchanged

**Step 3: New features available**
- Initial API versioning support (Spring Framework 7)
- WebFlux static resources support
- Updated Swagger UI (v5.30.1) and Scalar (v0.4.3)

**Known Issues:**
- API versioning with `springdoc.api-docs.version` may cause HTTP 400 errors (issue #3163)
- Test thoroughly before production deployment

### From SpringFox

1. Remove SpringFox dependencies
2. Add `springdoc-openapi-starter-webmvc-ui:3.0.0`
3. Replace annotations:

| SpringFox (Swagger 2) | springdoc (Swagger 3) |
|----------------------|----------------------|
| `@Api` | `@Tag` |
| `@ApiIgnore` | `@Parameter(hidden = true)` or `@Hidden` |
| `@ApiImplicitParam` | `@Parameter` |
| `@ApiModel` | `@Schema` |
| `@ApiModelProperty` | `@Schema` |
| `@ApiOperation(value, notes)` | `@Operation(summary, description)` |
| `@ApiParam` | `@Parameter` |
| `@ApiResponse(code, message)` | `@ApiResponse(responseCode, description)` |

4. Replace Docket beans with GroupedOpenApi beans

### From springdoc-openapi v1 (Legacy)

Replace modules:

| v1 Module | v3 Module |
|-----------|-----------|
| `springdoc-openapi-ui` | `springdoc-openapi-starter-webmvc-ui:3.0.0` |
| `springdoc-openapi-webflux-ui` | `springdoc-openapi-starter-webflux-ui:3.0.0` |
| `springdoc-openapi-webmvc-core` | `springdoc-openapi-starter-webmvc-api:3.0.0` |
| `springdoc-openapi-common` | `springdoc-openapi-starter-common:3.0.0` |

Update class imports:

| v1 Class | v2/v3 Class |
|----------|----------|
| `org.springdoc.core.GroupedOpenApi` | `org.springdoc.core.models.GroupedOpenApi` |
| `org.springdoc.core.SpringDocUtils` | `org.springdoc.core.utils.SpringDocUtils` |
| `org.springdoc.api.annotations.ParameterObject` | `org.springdoc.core.annotations.ParameterObject` |

---

## Compatibility Matrix

| Spring Boot | Spring Framework | springdoc-openapi | Java |
|-------------|------------------|-------------------|------|
| **4.0.x** | **7.x** | **3.0.x** | **17+** |
| 3.5.x | 6.2.x | 2.8.x | 17+ |
| 3.4.x | 6.1.x | 2.7.x - 2.8.x | 17+ |
| 3.3.x | 6.0.x | 2.6.x | 17+ |
| 3.2.x | 6.0.x | 2.3.x - 2.5.x | 17+ |
| 3.1.x | 6.0.x | 2.2.x | 17+ |
| 3.0.x | 6.0.x | 2.0.x - 2.1.x | 17+ |

> **Important**: springdoc-openapi v3.0.x **requires** Spring Boot 4.0+. For Spring Boot 3.x projects, use springdoc-openapi v2.8.x.

---

## Common Issues & Solutions

### Parameters not showing in OpenAPI spec

Add `-parameters` to Maven compiler:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

### Welcome page broken after adding springdoc

Set custom swagger-ui path:

```properties
springdoc.swagger-ui.path=/swagger-ui/api-docs.html
```

### Reverse proxy issues

```properties
server.forward-headers-strategy=framework
```

```java
@Bean
ForwardedHeaderFilter forwardedHeaderFilter() {
    return new ForwardedHeaderFilter();
}
```

### @Controller not showing

Use `@RestController` or add configuration:

```java
static {
    SpringDocUtils.getConfig().addRestControllers(MyController.class);
}
```

---

## Best Practices

1. **Use OpenAPI annotations** - Enhance auto-generated docs with `@Operation`, `@Schema`, `@Parameter`
2. **Document DTOs** - Add `@Schema` to all request/response classes
3. **Group APIs logically** - Use `GroupedOpenApi` for large applications
4. **Secure documentation** - Protect Swagger UI in production
5. **Use Javadoc** - Enable therapi-runtime-javadoc for automatic description extraction
6. **Version your API** - Include version in paths and document changes
7. **Provide examples** - Use `example` attribute in `@Schema` annotations
8. **Document errors** - Use `@ControllerAdvice` with `@ResponseStatus`

---

## Integration Points

### With API Testing Tools
- **Postman**: Import OpenAPI spec directly
- **Insomnia**: Import from `/v3/api-docs`
- **Bruno**: Import OpenAPI 3 specs

### With Documentation Sites
- **Docusaurus**: Use OpenAPI plugin
- **MkDocs**: Use swagger-markdown plugin
- **Redoc**: Render OpenAPI specs beautifully
- **Stoplight**: Full API design platform

### With Code Generation
- **OpenAPI Generator**: Generate client SDKs
- **Swagger Codegen**: Generate servers/clients

---

## Relationship with Other Skills

**This Skill:** Auto-configure springdoc-openapi for Spring Boot REST APIs
**@docs-writer Sub-Agent:** Create comprehensive user guides and tutorials
**readme-updater Skill:** Keep README in sync with API changes

### Workflow
1. I configure springdoc-openapi and generate OpenAPI spec
2. Need user guide? Invoke **@docs-writer** sub-agent
3. Sub-agent creates complete API documentation site

---

## Sandboxing Compatibility

**Works without sandboxing:** Yes
**Works with sandboxing:** Yes

- **Filesystem**: Writes configuration files
- **Network**: None required (build-time generation)
- **Configuration**: application.properties/yml changes

---

## References

- [springdoc-openapi GitHub](https://github.com/springdoc/springdoc-openapi)
- [springdoc-openapi v3.0.0 Release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.0.0)
- [springdoc-openapi Demos](https://github.com/springdoc/springdoc-openapi-demos)
- [springdoc.org Documentation](https://springdoc.org/)
- [OpenAPI 3.1 Specification](https://spec.openapis.org/oas/v3.1.0)
- [Swagger UI Configuration](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/)
- [Spring Boot 4.0 Documentation](https://docs.spring.io/spring-boot/docs/4.0.0/reference/html/)
- [Spring Framework 7 Documentation](https://docs.spring.io/spring-framework/reference/)

---

*This skill uses springdoc-openapi v3.0.0 documentation as its knowledge base for Spring Boot 4.x API documentation generation.*

---
unique-name: spring-boot-4-api-versioning
display-name: API Versioning with Spring Boot 4
category: ARCHITECTURE
pattern-name: API Versioning Pattern
description: Comprehensive guide to Spring Framework 7's first-class API versioning support including server configuration, client integration, deprecation handling, testing strategies, and troubleshooting common configuration errors like MissingApiVersionException
tags: spring-boot-4, spring-framework-7, api-versioning, rest-api, backward-compatibility, api-evolution, troubleshooting
---

# API Versioning with Spring Boot 4 / Spring Framework 7

> **Version**: Spring Boot 4.0+ / Spring Framework 7.0+
> **Category**: ARCHITECTURE
> **Tags**: spring-boot, api-versioning, rest-api, spring-framework-7

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [Implementation Guide](#implementation-guide)
- [Client-Side Support](#client-side-support)
- [Deprecation Support](#deprecation-support)
- [Testing](#testing)
- [Best Practices](#best-practices)
- [Complete Implementation Checklist](#complete-implementation-checklist)
- [Quick Reference](#quick-reference)
- [**Troubleshooting**](#troubleshooting) ← Common errors and fixes
- [References](#references)

## Overview

Spring Framework 7 introduces **first-class API versioning support** as a core feature. This eliminates the need for custom versioning implementations and provides a consistent, declarative approach to API evolution.

## Why API Versioning?

APIs are contracts. Once published, consumers (mobile apps, partner integrations, services) depend on them. API versioning enables:

- **Backward compatibility**: Existing clients continue working without code changes
- **Forward evolution**: New clients access richer data models
- **Clarity**: Each version has an explicit schema contract
- **Future-proofing**: Add fields or change behaviors without breaking integrations

---

## Core Concepts

### ApiVersionStrategy

The central contract for all API versioning preferences. It handles:
- Resolving request versions
- Parsing and validating versions
- Managing supported version ranges
- Sending deprecation hints in responses

### Version Formats

| Format | Example | Description |
|--------|---------|-------------|
| Fixed Version | `"1.2"` | Exact version match only |
| Baseline Version | `"1.2+"` | Matches 1.2 and all future versions until overridden |
| Semantic Version | `"1.2.3"` | Major.minor.patch (minor/patch default to 0 if omitted) |

### Version Resolution Strategies

| Strategy | Configuration | Request Example |
|----------|--------------|-----------------|
| Header | `useRequestHeader("API-Version")` | `API-Version: 1.2` |
| Query Parameter | `useQueryParam("version")` | `?version=1.2` |
| Path Variable | `usePathVariable("version")` | `/api/v1.2/resource` |
| Media Type | `useMediaTypeParameter("v")` | `Accept: application/vnd.api.v1+json` |

---

## Implementation Guide

### Step 1: Configure API Versioning Strategy

> **CRITICAL**: You MUST configure `addSupportedVersions()` AND `setDefaultVersion()` to avoid `MissingApiVersionException`. See [Troubleshooting](#troubleshooting) section for details.

#### Option A: Java Configuration (WebMvcConfigurer)

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("API-Version")           // Primary: Header-based
            // OR .useQueryParam("version")            // Alternative: Query param
            // OR .usePathVariable("version")          // Alternative: Path variable
            .addSupportedVersions("1.0", "1.1", "2.0") // REQUIRED: All supported versions
            .setDefaultVersion("1.0");                 // REQUIRED: Default when not specified
            // .setDeprecatedVersions("1.0");          // Optional: Mark deprecated versions
    }
}
```

#### Option B: Application Properties (Spring Boot)

```yaml
# application.yml
spring:
  mvc:
    apiversion:
      use:
        header: API-Version          # Header-based versioning
        # query-param: version       # Query parameter versioning
        # path-variable: version     # Path variable versioning
      default-version: "1.0"
      supported-versions:
        - "1.0"
        - "1.1"
        - "2.0"
      deprecated-versions:
        - "1.0"
```

```properties
# application.properties
spring.mvc.apiversion.use.header=API-Version
spring.mvc.apiversion.default-version=1.0
```

#### Option C: WebFlux Configuration (Reactive)

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class ApiVersionConfig implements WebFluxConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("API-Version")
            .setDefaultVersion("1.0");
    }
}
```

### Step 2: Version Your Controller Endpoints

#### Annotated Controllers with `version` Attribute

```java
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // Version 1.0 - Basic user response
    @GetMapping(path = "/{id}", version = "1.0")
    public UserV1 getUserV1(@PathVariable Long id) {
        return new UserV1(id, "John Doe");
    }

    // Version 1.1 - Added email field
    @GetMapping(path = "/{id}", version = "1.1")
    public UserV1_1 getUserV1_1(@PathVariable Long id) {
        return new UserV1_1(id, "John Doe", "john@example.com");
    }

    // Version 2.0+ - Baseline version (handles 2.0 and all future versions)
    @GetMapping(path = "/{id}", version = "2.0+")
    public UserV2 getUserV2(@PathVariable Long id) {
        return new UserV2(id, "John Doe", "john@example.com",
                          new Address("123 Main St", "New York", "10001"));
    }
}

// DTOs for each version
public record UserV1(Long id, String name) {}

public record UserV1_1(Long id, String name, String email) {}

public record UserV2(Long id, String name, String email, Address address) {}

public record Address(String street, String city, String zipCode) {}
```

#### Complete Controller Example with All HTTP Methods

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    // CREATE - Version 1.0
    @PostMapping(version = "1.0")
    public ResponseEntity<ProductV1> createProductV1(@RequestBody CreateProductV1 request) {
        ProductV1 product = productService.createV1(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    // CREATE - Version 2.0+ with additional fields
    @PostMapping(version = "2.0+")
    public ResponseEntity<ProductV2> createProductV2(@RequestBody CreateProductV2 request) {
        ProductV2 product = productService.createV2(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    // READ - Version 1.0
    @GetMapping(path = "/{id}", version = "1.0")
    public ProductV1 getProductV1(@PathVariable Long id) {
        return productService.getV1(id);
    }

    // READ - Version 2.0+
    @GetMapping(path = "/{id}", version = "2.0+")
    public ProductV2 getProductV2(@PathVariable Long id) {
        return productService.getV2(id);
    }

    // UPDATE - Version 1.0
    @PutMapping(path = "/{id}", version = "1.0")
    public ProductV1 updateProductV1(@PathVariable Long id,
                                      @RequestBody UpdateProductV1 request) {
        return productService.updateV1(id, request);
    }

    // UPDATE - Version 2.0+
    @PutMapping(path = "/{id}", version = "2.0+")
    public ProductV2 updateProductV2(@PathVariable Long id,
                                      @RequestBody UpdateProductV2 request) {
        return productService.updateV2(id, request);
    }

    // DELETE - Same across all versions (no version attribute needed)
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Step 3: Functional Endpoints (RouterFunction)

```java
import static org.springframework.web.servlet.function.RequestPredicates.*;
import static org.springframework.web.servlet.function.RouterFunctions.*;

@Configuration
public class ProductRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return route()
            // Version 1.0
            .GET("/api/products/{id}", version("1.0"), handler::getProductV1)
            .POST("/api/products", version("1.0"), handler::createProductV1)

            // Version 2.0+
            .GET("/api/products/{id}", version("2.0+"), handler::getProductV2)
            .POST("/api/products", version("2.0+"), handler::createProductV2)

            .build();
    }
}

@Component
public class ProductHandler {

    public ServerResponse getProductV1(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        ProductV1 product = productService.getV1(id);
        return ServerResponse.ok().body(product);
    }

    public ServerResponse getProductV2(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        ProductV2 product = productService.getV2(id);
        return ServerResponse.ok().body(product);
    }

    // ... other handlers
}
```

### Step 4: Path-Based Versioning

When using path-based versioning, declare the version as a URI variable:

```java
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.usePathVariable("version");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Set common path prefix with version variable
        configurer.addPathPrefix("/api/{version}",
            c -> c.isAnnotationPresent(RestController.class));
    }
}

@RestController
@RequestMapping("/users")  // Actual path: /api/{version}/users
public class UserController {

    @GetMapping(path = "/{id}", version = "1.0")
    public UserV1 getUserV1(@PathVariable Long id) {
        return new UserV1(id, "John Doe");
    }

    @GetMapping(path = "/{id}", version = "2.0+")
    public UserV2 getUserV2(@PathVariable Long id) {
        return new UserV2(id, "John Doe", "john@example.com");
    }
}
```

**Request Examples:**
```bash
# Version 1.0
curl http://localhost:8080/api/v1.0/users/1

# Version 2.0
curl http://localhost:8080/api/v2.0/users/1
```

---

## Client-Side Support

### RestClient with API Versioning

```java
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ApiVersionInserter;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .baseUrl("http://localhost:8080")
            .apiVersionInserter(ApiVersionInserter.useHeader("API-Version"))
            .build();
    }
}

@Service
public class UserClientService {

    private final RestClient restClient;

    public UserClientService(RestClient restClient) {
        this.restClient = restClient;
    }

    public UserV1 getUserV1(Long id) {
        return restClient.get()
            .uri("/api/users/{id}", id)
            .apiVersion("1.0")  // Specify version per request
            .retrieve()
            .body(UserV1.class);
    }

    public UserV2 getUserV2(Long id) {
        return restClient.get()
            .uri("/api/users/{id}", id)
            .apiVersion("2.0")
            .retrieve()
            .body(UserV2.class);
    }
}
```

#### Spring Boot Properties for RestClient

```yaml
spring:
  http:
    client:
      restclient:
        apiversion:
          insert:
            header: API-Version
            # query-param: version
            # path-variable: version
```

### WebClient with API Versioning (Reactive)

```java
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.ApiVersionInserter;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8080")
            .apiVersionInserter(ApiVersionInserter.useHeader("API-Version"))
            .build();
    }
}

@Service
public class UserReactiveService {

    private final WebClient webClient;

    public Mono<UserV2> getUserV2(Long id) {
        return webClient.get()
            .uri("/api/users/{id}", id)
            .apiVersion("2.0")
            .retrieve()
            .bodyToMono(UserV2.class);
    }
}
```

### HTTP Interface Clients

```java
import org.springframework.web.service.annotation.*;

@HttpExchange("/api/users")
public interface UserClient {

    @GetExchange(url = "/{id}", version = "1.0")
    UserV1 getUserV1(@PathVariable Long id);

    @GetExchange(url = "/{id}", version = "2.0")
    UserV2 getUserV2(@PathVariable Long id);

    @PostExchange(version = "2.0")
    UserV2 createUser(@RequestBody CreateUserRequest request);
}

@Configuration
public class HttpInterfaceConfig {

    @Bean
    public UserClient userClient(RestClient.Builder builder) {
        RestClient restClient = builder
            .baseUrl("http://localhost:8080")
            .apiVersionInserter(ApiVersionInserter.useHeader("API-Version"))
            .build();

        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(UserClient.class);
    }
}
```

---

## Deprecation Support

### Configure Deprecation Headers

Spring Framework 7 supports RFC 9745 and RFC 8594 for deprecation signaling:

```java
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("API-Version")
            .setDeprecatedVersions("1.0", "1.1")
            .setDeprecationHandler(deprecationHandler());
    }

    @Bean
    public ApiVersionDeprecationHandler deprecationHandler() {
        return ApiVersionDeprecationHandler.builder()
            .addDeprecation("1.0", LocalDate.of(2025, 6, 1))  // Sunset date
            .addDeprecation("1.1", LocalDate.of(2025, 12, 1))
            .linkTemplate("https://api.example.com/docs/migration/{version}")
            .build();
    }
}
```

### Response Headers for Deprecated Versions

When a client uses a deprecated version, the response includes:

```http
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 01 Jun 2025 00:00:00 GMT
Link: <https://api.example.com/docs/migration/1.0>; rel="deprecation"
Content-Type: application/json

{"id": 1, "name": "John Doe"}
```

---

## Testing

### MockMvc Testing

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUserV1ForVersion1() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("API-Version", "1.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void shouldReturnUserV2ForVersion2() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("API-Version", "2.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.address").exists());
    }

    @Test
    void shouldReturnDeprecationHeadersForV1() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("API-Version", "1.0"))
            .andExpect(status().isOk())
            .andExpect(header().string("Deprecation", "true"))
            .andExpect(header().exists("Sunset"));
    }
}
```

### RestTestClient Testing (New in Spring Framework 7)

```java
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(UserController.class)
class UserControllerRestTestClientTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void shouldReturnUserV2() {
        restTestClient
            .get()
            .uri("/api/users/1")
            .apiVersion("2.0")
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserV2.class)
            .value(user -> {
                assertThat(user.id()).isEqualTo(1L);
                assertThat(user.email()).isNotNull();
            });
    }
}
```

### WebTestClient Testing (Reactive)

```java
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(UserController.class)
class UserControllerWebTestClientTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnUserV2() {
        webTestClient
            .get()
            .uri("/api/users/1")
            .apiVersion("2.0")
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserV2.class)
            .value(user -> assertThat(user.email()).isNotNull());
    }
}
```

### Standalone Setup with ApiVersionStrategy

```java
@BeforeEach
void setup() {
    ApiVersionStrategy strategy = ApiVersionStrategy.builder()
        .useRequestHeader("API-Version")
        .supportedVersions("1.0", "1.1", "2.0")
        .defaultVersion("1.0")
        .build();

    mockMvc = MockMvcBuilders
        .standaloneSetup(new UserController())
        .setApiVersionStrategy(strategy)
        .build();
}
```

---

## Best Practices

### 1. Version Schema Evolution

```
v1.0 -> v1.1 (additive, non-breaking)
  - Add new optional fields
  - Add new endpoints

v1.x -> v2.0 (breaking changes)
  - Remove fields
  - Change field types
  - Restructure response
```

### 2. Use Baseline Versions for Stability

```java
// Instead of creating handlers for every version:
@GetMapping(path = "/{id}", version = "1.0")  // Only v1.0
@GetMapping(path = "/{id}", version = "1.1")  // Only v1.1
@GetMapping(path = "/{id}", version = "1.2")  // Only v1.2

// Use baseline versions:
@GetMapping(path = "/{id}", version = "1.0+")  // v1.0, v1.1, v1.2, ...
@GetMapping(path = "/{id}", version = "2.0+")  // v2.0, v2.1, ...
```

### 3. Organize Version-Specific DTOs

```
src/main/java/com/example/api/
├── v1/
│   ├── dto/
│   │   ├── UserV1.java
│   │   └── ProductV1.java
│   └── controller/
│       └── UserControllerV1.java
├── v2/
│   ├── dto/
│   │   ├── UserV2.java
│   │   └── ProductV2.java
│   └── controller/
│       └── UserControllerV2.java
└── common/
    ├── service/
    └── repository/
```

### 4. Share Logic, Not Contracts

```java
@Service
public class UserService {

    private final UserRepository repository;

    // Internal domain model
    public User getUser(Long id) {
        return repository.findById(id).orElseThrow();
    }
}

@RestController
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;

    @GetMapping(path = "/users/{id}", version = "1.0")
    public UserV1 getUserV1(@PathVariable Long id) {
        User user = userService.getUser(id);
        return mapper.toV1(user);
    }

    @GetMapping(path = "/users/{id}", version = "2.0+")
    public UserV2 getUserV2(@PathVariable Long id) {
        User user = userService.getUser(id);
        return mapper.toV2(user);
    }
}
```

### 5. Document Your API Versions

```java
@Operation(summary = "Get user by ID",
           description = "Returns user details. V2+ includes address information.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "User found"),
    @ApiResponse(responseCode = "404", description = "User not found")
})
@GetMapping(path = "/{id}", version = "2.0+")
public UserV2 getUserV2(@PathVariable Long id) {
    // ...
}
```

---

## Complete Implementation Checklist

1. [ ] Add Spring Boot 4.0+ / Spring Framework 7.0+ dependency
2. [ ] Configure `ApiVersionConfigurer` in `WebMvcConfigurer`:
   - [ ] Choose versioning strategy: `useRequestHeader()`, `useQueryParam()`, or `usePathVariable()`
   - [ ] **REQUIRED**: Call `addSupportedVersions("1.0", "2.0", ...)` with all versions
   - [ ] **REQUIRED**: Call `setDefaultVersion("1.0")` to set fallback version
3. [ ] Add `version` attribute to `@GetMapping`, `@PostMapping`, etc.
4. [ ] Ensure each supported version has at least one matching endpoint
5. [ ] Create version-specific DTOs (records recommended)
6. [ ] Configure deprecation handler for sunset versions (optional)
7. [ ] Update API clients with `ApiVersionInserter`
8. [ ] Write version-specific tests
9. [ ] Document API versions in OpenAPI/Swagger
10. [ ] Test without version header to verify default version works

---

## Quick Reference

### Annotation Attributes

| Annotation | Attribute | Example |
|------------|-----------|---------|
| `@GetMapping` | `version` | `@GetMapping(path = "/users", version = "1.0")` |
| `@PostMapping` | `version` | `@PostMapping(version = "2.0+")` |
| `@PutMapping` | `version` | `@PutMapping(path = "/{id}", version = "1.1")` |
| `@DeleteMapping` | `version` | `@DeleteMapping(version = "2.0")` |
| `@PatchMapping` | `version` | `@PatchMapping(version = "1.0+")` |
| `@RequestMapping` | `version` | `@RequestMapping(version = "1.0")` |
| `@GetExchange` | `version` | `@GetExchange(url = "/{id}", version = "1.0")` |
| `@PostExchange` | `version` | `@PostExchange(version = "2.0")` |

### Configuration Properties

```yaml
spring:
  mvc:
    apiversion:
      use:
        header: API-Version           # Header name
        query-param: version          # Query parameter name
        path-variable: version        # Path variable name
        media-type-parameter: v       # Media type parameter
      default-version: "1.0"
      supported-versions:
        - "1.0"
        - "1.1"
        - "2.0"
      deprecated-versions:
        - "1.0"
  http:
    client:
      restclient:
        apiversion:
          insert:
            header: API-Version
```

### Request Examples

```bash
# Header-based
curl -H "API-Version: 2.0" http://localhost:8080/api/users/1

# Query parameter
curl "http://localhost:8080/api/users/1?version=2.0"

# Path-based
curl http://localhost:8080/api/v2.0/users/1

# Media type
curl -H "Accept: application/vnd.api.v2+json" http://localhost:8080/api/users/1
```

---

## Troubleshooting

### MissingApiVersionException: "API version is required"

**Error:**
```
org.springframework.web.accept.MissingApiVersionException: 400 BAD_REQUEST "API version is required."
```

**Cause:** Spring Framework 7's API versioning is enabled but not properly configured. This happens when:
1. No `setDefaultVersion()` is configured, AND
2. The client doesn't provide a version header/parameter

**Solution:** Always configure both supported versions AND a default version:

```java
@Override
public void configureApiVersioning(ApiVersionConfigurer configurer) {
    configurer
        .useRequestHeader("API-Version")
        .addSupportedVersions("1.0", "2.0")  // ← REQUIRED
        .setDefaultVersion("1.0");            // ← REQUIRED
}
```

### InvalidApiVersionException: "Unsupported API version"

**Error:**
```
org.springframework.web.accept.InvalidApiVersionException: 400 BAD_REQUEST "Unsupported API version: 3.0"
```

**Cause:** Client requested a version not in the supported versions list.

**Solution:** Either:
1. Add the version to `addSupportedVersions()`, OR
2. Use baseline versions (`2.0+`) to handle future versions

### NoHandlerFoundException for Versioned Endpoints

**Error:**
```
org.springframework.web.servlet.NoHandlerFoundException: No handler found for GET /api/products
```

**Cause:** The request version doesn't match any controller endpoint's `version` attribute.

**Solution:** Ensure your controller has a handler for the resolved version:

```java
// If client sends API-Version: 1.0, this handler will match
@GetMapping(version = "1.0")
public ProductV1 getProductV1() { ... }

// If client sends API-Version: 2.0, this handler will match
@GetMapping(version = "2.0")
public ProductV2 getProductV2() { ... }
```

### Common Configuration Mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Missing `setDefaultVersion()` | 400 error when no version header sent | Add `.setDefaultVersion("1.0")` |
| Missing `addSupportedVersions()` | All version requests rejected | Add `.addSupportedVersions("1.0", "2.0")` |
| Using `setSupportedVersions()` | Compilation error | Use `addSupportedVersions()` instead |
| Wrong header name in config vs request | Version not resolved | Ensure header names match exactly |
| Mixing path-based with other strategies | Routing confusion | Use only ONE versioning strategy |

### Minimal Working Configuration

If you're getting errors, start with this minimal configuration and build up:

```java
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("API-Version")     // Step 1: Choose strategy
            .addSupportedVersions("1.0", "2.0")  // Step 2: Define versions
            .setDefaultVersion("1.0");           // Step 3: Set default
    }
}
```

Then test with:
```bash
# Should work - uses default version 1.0
curl http://localhost:8080/api/products

# Should work - explicit version 2.0
curl -H "API-Version: 2.0" http://localhost:8080/api/products

# Should fail with 400 - unsupported version
curl -H "API-Version: 3.0" http://localhost:8080/api/products
```

### Debugging API Version Resolution

Enable debug logging to see version resolution:

```yaml
logging:
  level:
    org.springframework.web.accept: DEBUG
    org.springframework.web.servlet.mvc.method.annotation: DEBUG
```

This will show logs like:
```
DEBUG o.s.w.a.ApiVersionResolver : Resolved API version '2.0' from header 'API-Version'
```

---

## References

- [Spring Framework 7 API Versioning Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html#mvc-ann-requestmapping-version)
- [Spring Framework 7 API Version Reference](https://docs.spring.io/spring-framework/reference/7.0-SNAPSHOT/web/webmvc/mvc-config/api-version.html)
- [Spring Blog: API Versioning in Spring](https://spring.io/blog/2025/09/16/api-versioning-in-spring)
- [RFC 9745: Deprecation Header](https://www.rfc-editor.org/rfc/rfc9745)
- [RFC 8594: Sunset Header](https://www.rfc-editor.org/rfc/rfc8594)

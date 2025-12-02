# API Versioning Demo - Spring Boot 4 / Spring Framework 7

A demonstration application showcasing **Spring Framework 7's first-class API versioning support** with header-based versioning. This example shows how to maintain backward compatibility while evolving your API using native `version` attribute on request mappings.

> **Note**: This demo uses Spring Framework 7's native `ApiVersionConfigurer` and the `version` attribute directly on `@GetMapping` annotations - no more workarounds!

## Features

- **Header-Based API Versioning** - Uses `API-Version` header for version selection
- **Two API Versions** - Version 1.0 (deprecated) and Version 2.0 (current)
- **Interactive Demo UI** - Dark theme Thymeleaf UI with version toggle
- **Live API Responses** - Fetch and display real API data per version
- **Version Comparison** - Side-by-side comparison of response formats
- **Deprecation Support** - RFC 9745/8594 compliant deprecation headers

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 4.0.0   |
| Java | OpenJDK | 25      |
| Core | Spring Framework | 7.0.x   |
| UI | Thymeleaf | 3.x     |
| Build Tool | Gradle | 9.2.0   |

## Quick Start

### Run the Application

```bash
# From the api-versioning-spring-boot-40 directory
./gradlew bootRun
```

Or build and run:

```bash
./gradlew build
java -jar build/libs/api-versioning-demo-1.0.0.jar
```

### Access the Application

- **Demo UI**: http://localhost:8088
- **API V1**: `curl -H "API-Version: 1.0" http://localhost:8088/api/products`
- **API V2**: `curl -H "API-Version: 2.0" http://localhost:8088/api/products`

## API Versioning Explained

### Configuration

Spring Framework 7 introduces native API versioning via `ApiVersionConfigurer`:

```java
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("API-Version")       // Header-based versioning
            .addSupportedVersions("1.0", "2.0")    // All supported versions
            .setDefaultVersion("1.0");             // Default when no version specified
    }
}
```

### Controller Implementation

Use the `version` attribute directly on `@GetMapping` (and other mapping annotations):

```java
@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    // Version 1.0 - Basic response (deprecated)
    @GetMapping(version = "1.0")
    public ApiResponse<List<ProductV1>> getAllProductsV1() {
        return ApiResponse.of("1.0", products);
    }

    // Version 2.0 - Enhanced response (current)
    @GetMapping(version = "2.0")
    public ApiResponse<List<ProductV2>> getAllProductsV2() {
        return ApiResponse.of("2.0", enhancedProducts);
    }

    // Single product with version
    @GetMapping(path = "/{id}", version = "2.0")
    public ApiResponse<ProductV2> getProductV2(@PathVariable Long id) {
        return ApiResponse.of("2.0", ProductV2.sample());
    }
}
```

### Version Formats

| Format | Example | Description |
|--------|---------|-------------|
| Fixed | `"1.0"` | Exact version match only |
| Baseline | `"2.0+"` | Matches 2.0 and all future versions |
| Semantic | `"1.2.3"` | Major.minor.patch (optional) |

### Resolution Strategies

```java
// Header-based (this demo)
configurer.useRequestHeader("API-Version");

// Query parameter
configurer.useQueryParam("version");  // ?version=2.0

// Path segment
configurer.usePathSegment(1);  // /api/v2/products

// Media type
configurer.useMediaTypeParameter(MediaType.APPLICATION_JSON, "version");
```

## API Response Comparison

### Version 1.0 Response (Deprecated)

```json
{
  "apiVersion": "1.0",
  "timestamp": "2025-12-02T10:00:00",
  "data": [
    {
      "id": 1,
      "name": "Spring Boot in Action",
      "price": 49.99
    }
  ]
}
```

### Version 2.0 Response (Current)

```json
{
  "apiVersion": "2.0",
  "timestamp": "2025-12-02T10:00:00",
  "data": [
    {
      "id": 1,
      "name": "Spring Boot in Action",
      "price": 49.99,
      "description": "A comprehensive guide to Spring Boot 4",
      "category": "Books",
      "tags": ["spring", "java", "microservices"],
      "stock": 42,
      "createdAt": "2025-01-15T10:30:00",
      "rating": {
        "average": 4.8,
        "count": 156
      }
    }
  ]
}
```

## Project Structure

```
api-versioning-spring-boot-40/
├── src/main/java/com/example/apiversioning/
│   ├── ApiVersioningDemoApplication.java   # Main application
│   ├── config/
│   │   └── ApiVersionConfig.java           # API versioning configuration
│   ├── controller/
│   │   ├── ProductApiController.java       # Versioned REST endpoints
│   │   └── DemoController.java             # UI controller
│   └── dto/
│       ├── ProductV1.java                  # V1 response record
│       ├── ProductV2.java                  # V2 response record
│       └── ApiResponse.java                # Response wrapper
├── src/main/resources/
│   ├── application.yml
│   ├── static/css/
│   │   └── style.css                       # Dark theme styles
│   └── templates/
│       └── index.html                      # Demo UI
├── build.gradle
└── README.md
```

---

## Spring MCP Server Usage

This example was created using the **Spring Documentation MCP Server** to ensure accurate implementation of Spring Boot 4's API versioning feature.

### MCP Tools Used

#### 1. Get Architecture Flavor

```
Tool: getFlavorByName(uniqueName: "spring-boot-4-api-versioning")
Result: Retrieved comprehensive API versioning guide including:
  - Configuration options (header, query param, path, media type)
  - Controller annotation patterns
  - Client-side integration (RestClient, WebClient)
  - Deprecation handling (RFC 9745, RFC 8594)
  - Testing strategies (MockMvc, WebTestClient)
```

#### 2. Version Compatibility Check

```
Tool: listProjectsBySpringBootVersion(majorVersion: 4, minorVersion: 0)
Result: Verified compatible versions:
  - Spring Framework 7.0.x
  - Spring Security 7.0.x (if needed)
  - Spring Data 4.0.x (if needed)
```

#### 3. Language Requirements

```
Tool: getSpringBootLanguageRequirements(springBootVersion: "4.0.0")
Result: Confirmed requirements:
  - Java 25+ (required)
  - Kotlin 2.0+ (optional)
```

#### 4. Breaking Changes Check

```
Tool: getBreakingChanges(project: "spring-boot", version: "4.0.0")
Result: Reviewed breaking changes for migration awareness
```

#### 5. Documentation Search

```
Tool: searchSpringDocs(query: "API versioning", project: "spring-framework")
Result: Found official documentation references for:
  - ApiVersionConfigurer interface
  - Version resolution strategies
  - Deprecation handling
```

### MCP Tools Summary

| Tool Used | Purpose |
|-----------|---------|
| `getFlavorByName` | Get API versioning architecture pattern |
| `listProjectsBySpringBootVersion` | Check ecosystem compatibility |
| `getSpringBootLanguageRequirements` | Verify Java/Kotlin requirements |
| `getBreakingChanges` | Review breaking changes for v4.0 |
| `searchSpringDocs` | Find official documentation |
| `getFlavorsByCategory` | Browse architecture patterns |

### Flavor Categories Available

The Spring MCP Server provides flavors in these categories:

- **ARCHITECTURE** - Design patterns like API versioning, hexagonal architecture
- **COMPLIANCE** - GDPR, SOC2, PCI-DSS guidelines
- **AGENTS** - AI agent configurations for development workflows
- **INITIALIZATION** - Project scaffolding templates
- **GENERAL** - General development guidelines

---

## Testing the API

### Using cURL

```bash
# Version 1.0 (default)
curl http://localhost:8088/api/products

# Explicit Version 1.0
curl -H "API-Version: 1.0" http://localhost:8088/api/products

# Version 2.0
curl -H "API-Version: 2.0" http://localhost:8088/api/products

# Get single product
curl -H "API-Version: 2.0" http://localhost:8088/api/products/1
```

### Using HTTPie

```bash
http localhost:8088/api/products API-Version:2.0
```

### Deprecation Headers

When using version 1.0, the response includes RFC 9745/8594 compliant deprecation headers:

```http
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 01 Jun 2025 00:00:00 GMT
Link: </docs/migration/1.0>; rel="deprecation"
Content-Type: application/json
```

## Development

### Run in Development Mode

```bash
./gradlew bootRun
```

DevTools is enabled - changes auto-reload.

### Build for Production

```bash
./gradlew build
```

## License

This example is part of the Spring MCP Server project for demonstration purposes.

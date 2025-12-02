# API Versioning Example - Summary

The API Versioning Example demonstrates **Spring Framework 7's first-class API versioning support** with native `version` attribute on request mappings.

## Created: `examples/basic/api-versioning-spring-boot-40/`

### Project Structure
```
api-versioning-spring-boot-40/
├── build.gradle                    # Spring Boot 4.0.0, Java 25
├── settings.gradle
├── gradle/wrapper/gradle-wrapper.properties  # Gradle 9.2.0
├── .gitignore
├── README.md                       # Full documentation
├── src/main/java/com/example/apiversioning/
│   ├── ApiVersioningDemoApplication.java
│   ├── config/
│   │   ├── ApiVersionConfig.java   # Native ApiVersionConfigurer
│   │   └── JacksonConfig.java      # ObjectMapper setup
│   ├── controller/
│   │   ├── ProductApiController.java  # Versioned endpoints
│   │   └── DemoController.java        # Thymeleaf UI
│   └── dto/
│       ├── ProductV1.java          # Simple V1 record
│       ├── ProductV2.java          # Enhanced V2 with nested Rating
│       └── ApiResponse.java        # Response wrapper
└── src/main/resources/
    ├── application.yml             # Port 8088
    ├── static/css/style.css        # Dark Spring.io theme
    └── templates/index.html        # Interactive demo UI
```

### Quick Start
```bash
cd examples/basic/api-versioning-spring-boot-40
./gradlew bootRun                 # Start app on port 8088
```

**Demo UI**: http://localhost:8088

**API Endpoints**:
```bash
# V1 (default) - Simple response
curl http://localhost:8088/api/products

# V2 - Enhanced response with more fields
curl -H "API-Version: 2.0" http://localhost:8088/api/products
```

---

## How the Spring MCP Server Was Used

During development, these MCP tools and flavors were used:

| Tool/Flavor Used | Purpose |
|------------------|---------|
| `getFlavorByName("spring-boot-4-api-versioning")` | Retrieved API versioning architecture pattern |
| `listProjectsBySpringBootVersion(4, 0)` | Verified ecosystem compatibility |
| `getSpringBootLanguageRequirements("4.0.0")` | Confirmed Java 25 requirement |
| `searchSpringDocs("API versioning")` | Found official documentation |

### Key Discovery: Native API Versioning Configuration

The flavor and documentation revealed the **correct configuration** for Spring Framework 7:

```java
@Override
public void configureApiVersioning(ApiVersionConfigurer configurer) {
    configurer
        .useRequestHeader("API-Version")     // Choose strategy
        .addSupportedVersions("1.0", "2.0")  // REQUIRED!
        .setDefaultVersion("1.0");           // REQUIRED!
}
```

**Critical Finding**: Without `addSupportedVersions()` AND `setDefaultVersion()`, the API throws:
```
MissingApiVersionException: 400 BAD_REQUEST "API version is required."
```

This finding was added back to the flavor documentation in `flavors/architecture/API_VERSIONING.md`.

---

## Features Implemented

- **Native API Versioning** - Spring Framework 7's `version` attribute on `@GetMapping`
- **Header-Based Resolution** - Uses `API-Version` request header
- **Two API Versions** - V1 (simple) and V2 (enhanced with nested objects)
- **RFC 9745/8594 Deprecation Headers** - V1 includes Deprecation, Sunset, Link headers
- **Dark Theme UI** - Interactive Thymeleaf UI with version toggle
- **Live API Responses** - Fetch and display real JSON responses per version
- **Java Records** - Modern DTO pattern with immutable records

---

## Spring Boot 4.0.0 / Spring Framework 7 Notes

### 1. Gradle 9.2.0 Required for Java 25
- **Java 25 requires Gradle 9.x** (class file version 69)
- Error: `Unsupported class file major version 69`
- Fix: Update to Gradle 9.2.0 in `gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.0-bin.zip
```

### 2. Native API Versioning is Available!
- Spring Framework 7.0.x includes `ApiVersionConfigurer`
- The `version` attribute works on all mapping annotations:
```java
@GetMapping(version = "1.0")
@GetMapping(version = "2.0")
@GetMapping(path = "/{id}", version = "2.0")
```

### 3. Configuration is Required
- Simply implementing `WebMvcConfigurer` enables API versioning
- But you MUST configure supported versions and default version
- Without proper config, ALL requests fail with 400 error

### 4. ObjectMapper Not Auto-Configured
- When using Spring Boot without full web starter auto-config, ObjectMapper may not be available
- Fix: Create explicit `JacksonConfig` with `@Bean ObjectMapper`

---

## API Response Comparison

### Version 1.0 (Deprecated)
```json
{
  "apiVersion": "1.0",
  "timestamp": "2025-12-02T10:30:00",
  "data": [
    {"id": 1, "name": "Spring Boot in Action", "price": 49.99}
  ]
}
```

**Response Headers**:
```http
Deprecation: true
Sunset: Sat, 01 Jun 2025 00:00:00 GMT
Link: </docs/migration/1.0>; rel="deprecation"
```

### Version 2.0 (Current)
```json
{
  "apiVersion": "2.0",
  "timestamp": "2025-12-02T10:30:00",
  "data": [
    {
      "id": 1,
      "name": "Spring Boot in Action",
      "price": 49.99,
      "description": "A comprehensive guide...",
      "category": "Books",
      "tags": ["spring", "java", "microservices"],
      "stock": 42,
      "createdAt": "2025-01-15T10:30:00",
      "rating": {"average": 4.8, "count": 156}
    }
  ]
}
```

---

## Files Created

| File | Description |
|------|-------------|
| `ApiVersionConfig.java` | Native `ApiVersionConfigurer` implementation |
| `JacksonConfig.java` | ObjectMapper with JavaTimeModule |
| `ProductApiController.java` | Versioned REST endpoints with `version` attribute |
| `DemoController.java` | Thymeleaf UI controller |
| `ProductV1.java` | Simple record: id, name, price |
| `ProductV2.java` | Enhanced record with Rating nested record |
| `ApiResponse.java` | Generic response wrapper with timestamp |
| `index.html` | Interactive dark-themed demo UI |
| `style.css` | Spring.io inspired dark theme |

---

## Flavor Documentation Updated

Based on findings during this demo development, the **API Versioning flavor** (`flavors/architecture/API_VERSIONING.md`) was enhanced with:

1. **Troubleshooting section** - Common errors and fixes
2. **Correct method names** - `addSupportedVersions()` not `setSupportedVersions()`
3. **Required configuration** - Warning about MissingApiVersionException
4. **Minimal working example** - Copy-paste starting point
5. **Debug logging tips** - How to trace version resolution

---

## Verified Working Configuration

After all setup, the application successfully:
- Starts on port **8088**
- Resolves API version from `API-Version` header
- Defaults to version **1.0** when no header provided
- Routes to correct versioned endpoint
- Returns deprecation headers for V1 requests
- Displays interactive UI at root path

---

*Generated: 2025-12-02*

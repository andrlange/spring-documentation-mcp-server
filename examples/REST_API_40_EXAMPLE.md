# REST API Example - Spring Boot 4.0.0

## Created: `examples/basic/rest-api-spring-boot-40/`

### Project Structure
```
rest-api-spring-boot-40/
├── build.gradle                    # Spring Boot 4.0.0 + dependencies
├── settings.gradle
├── gradlew                         # Gradle wrapper (8.14 - REQUIRED!)
├── README.md                       # Full documentation
├── src/main/java/com/example/api/
│   ├── RestApiApplication.java     # Main application class
│   ├── model/
│   │   ├── Book.java               # Java Record entity
│   │   ├── CreateBookRequest.java  # Create DTO with validation
│   │   └── UpdateBookRequest.java  # Update DTO
│   ├── service/
│   │   └── BookService.java        # Business logic + in-memory storage
│   └── controller/
│       ├── BookController.java     # REST endpoints
│       └── GlobalExceptionHandler.java  # Error handling
└── src/main/resources/
    └── application.yml             # Port 8082
```

### Quick Start
```bash
cd examples/basic/rest-api-spring-boot-40
./gradlew bootRun
```

**Application URL**: http://localhost:8082

---

## How the Spring MCP Server Was Used

During development, these MCP tools were used to make informed decisions:

| Tool Called | Purpose | Result |
|-------------|---------|--------|
| `getLatestSpringBootVersion(4, 0)` | Get latest 4.0.x version | **4.0.0** (GA) |
| `listProjectsBySpringBootVersion(4, 0)` | Verify compatible projects | **42 projects** available |

### Version Information Retrieved:

**Spring Boot 4.0.0 Compatibility** (from `listProjectsBySpringBootVersion`):
- Spring Framework **7.0.x**
- Spring Security **7.0.x**
- Spring Data **2025.1.x** (4.0.x modules)
- Spring Batch **6.0.x**
- Spring Integration **7.0.x**

### Example MCP Tool Responses:

```json
// getLatestSpringBootVersion(4, 0)
{
  "latestVersion": {
    "version": "4.0.0",
    "state": "GA",
    "ossSupportEnd": "2026-12-01",
    "enterpriseSupportEnd": "2027-12-01"
  }
}
```

---

## Spring Boot 4.0.0 Key Differences

### Required Changes from 3.5.x:

| Change | 3.5.x | 4.0.0 |
|--------|-------|-------|
| **Gradle Version** | 8.11 | **8.14+** (REQUIRED) |
| **Spring Framework** | 6.2.x | 7.0.x |
| **Flyway Starter** | `flyway-core` | `spring-boot-starter-flyway` |
| **Health Imports** | `o.s.b.actuate.health` | `o.s.b.health.contributor` |

### Gradle Wrapper Configuration:
```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
```

---

## Features Implemented

- **REST API**: Full CRUD operations for Book entities
- **In-Memory Storage**: ConcurrentHashMap for thread-safe storage
- **Java 21 Records**: Modern DTOs with immutability
- **Bean Validation**: Jakarta validation annotations
- **Actuator Endpoints**: Health, info, and metrics
- **Global Exception Handling**: Consistent error responses

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/books` | List all books |
| GET | `/api/books/{id}` | Get book by ID |
| GET | `/api/books/search?title=...` | Search by title |
| POST | `/api/books` | Create new book |
| PUT | `/api/books/{id}` | Update book |
| DELETE | `/api/books/{id}` | Delete book |
| GET | `/api/books/stats` | Get statistics |

---

## Test Commands

```bash
# List all books
curl http://localhost:8082/api/books

# Search by title
curl "http://localhost:8082/api/books/search?title=Spring"

# Create a new book
curl -X POST http://localhost:8082/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Book","author":"Test Author","isbn":"978-1234567890","pages":200}'

# Health check
curl http://localhost:8082/actuator/health

# Stats (shows Spring Boot 4.0.0)
curl http://localhost:8082/api/books/stats
```

---

## Migration Notes

If migrating from the 3.5.x example to 4.0.0:

1. **Update Gradle**: Change `gradle-wrapper.properties` to use 8.14+
2. **Update Spring Boot**: Change plugin version from `3.5.8` to `4.0.0`
3. **Check Dependencies**: Some starters have changed (e.g., Flyway)
4. **Health Indicators**: Update imports if using custom health checks

The Spring MCP Server's `getSpringMigrationGuide` tool can provide detailed migration guidance:
```
getSpringMigrationGuide("spring-boot", "3.5", "4.0")
```

---

*Generated: 2025-11-28*
*Spring Boot Version: 4.0.0*
*Built using Spring MCP Server v1.1.0*

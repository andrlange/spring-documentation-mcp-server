# REST API Example - Spring Boot 3.5.8

## Created: `examples/basic/rest-api-spring-boot-35/`

### Project Structure
```
rest-api-spring-boot-35/
├── build.gradle                    # Spring Boot 3.5.8 + dependencies
├── settings.gradle
├── gradlew                         # Gradle wrapper (8.11)
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
    └── application.yml             # Port 8081
```

### Quick Start
```bash
cd examples/basic/rest-api-spring-boot-35
./gradlew bootRun
```

**Application URL**: http://localhost:8081

---

## How the Spring MCP Server Was Used

During development, these MCP tools were used to make informed decisions:

| Tool Called | Purpose | Result |
|-------------|---------|--------|
| `getLatestSpringBootVersion(3, 5)` | Get latest 3.5.x version | **3.5.8** (GA) |
| `listProjectsBySpringBootVersion(3, 5)` | Verify compatible projects | **48 projects** available |

### Version Information Retrieved:

**Spring Boot 3.5.8 Compatibility** (from `listProjectsBySpringBootVersion`):
- Spring Framework **6.2.14**
- Spring Security **6.5.7**
- Spring Data **2025.0.6**
- Spring AI **1.1.0**
- Spring Batch **5.2.4**

### Example MCP Tool Responses:

```json
// getLatestSpringBootVersion(3, 5)
{
  "latestVersion": {
    "version": "3.5.8",
    "state": "GA",
    "ossSupportEnd": "2026-06-01",
    "enterpriseSupportEnd": "2032-06-01"
  }
}
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
curl http://localhost:8081/api/books

# Search by title
curl "http://localhost:8081/api/books/search?title=Spring"

# Create a new book
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Book","author":"Test Author","isbn":"978-1234567890","pages":200}'

# Health check
curl http://localhost:8081/actuator/health
```

---

*Generated: 2025-11-28*
*Spring Boot Version: 3.5.8*
*Built using Spring MCP Server v1.1.0*

# REST API Example - Spring Boot 4.0.0

A simple REST API demonstrating CRUD operations with Spring Boot 4.0.0.

## Features

- RESTful Book API with CRUD operations
- In-memory storage (no database required)
- Java 21 Records for DTOs
- Bean Validation
- Actuator health endpoints
- Global exception handling

## Spring Boot 4.0.0 Requirements

**Important**: Spring Boot 4.0.0 requires:
- **Gradle 8.14 or later** (not 8.11!)
- **Java 21** (recommended, though 17+ works)

This project includes the correct Gradle wrapper version.

## Quick Start

```bash
# Run the application
./gradlew bootRun

# Or build and run JAR
./gradlew build
java -jar build/libs/rest-api-spring-boot-40-1.0.0.jar
```

**Application runs on:** http://localhost:8082

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/books` | List all books |
| GET | `/api/books/{id}` | Get book by ID |
| GET | `/api/books/search?title=...` | Search by title |
| GET | `/api/books/search?author=...` | Search by author |
| POST | `/api/books` | Create new book |
| PUT | `/api/books/{id}` | Update book |
| DELETE | `/api/books/{id}` | Delete book |
| GET | `/api/books/stats` | Get statistics |

## Example Requests

### List all books
```bash
curl http://localhost:8082/api/books
```

### Get a specific book
```bash
curl http://localhost:8082/api/books/{id}
```

### Search books by title
```bash
curl "http://localhost:8082/api/books/search?title=Spring"
```

### Create a new book
```bash
curl -X POST http://localhost:8082/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Learning Spring Boot 4",
    "author": "John Doe",
    "isbn": "978-1234567890",
    "publishedDate": "2025-01-15",
    "genre": "Technology",
    "pages": 450
  }'
```

### Update a book
```bash
curl -X PUT http://localhost:8082/api/books/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title"
  }'
```

### Delete a book
```bash
curl -X DELETE http://localhost:8082/api/books/{id}
```

## Actuator Endpoints

- Health: http://localhost:8082/actuator/health
- Info: http://localhost:8082/actuator/info
- Metrics: http://localhost:8082/actuator/metrics

## Technology Stack

- **Spring Boot**: 4.0.0
- **Spring Framework**: 7.0.x
- **Java**: 21
- **Build Tool**: Gradle 8.14

## Spring Boot 4.0.0 Breaking Changes

When migrating from Spring Boot 3.x to 4.0.0, be aware of:

1. **Gradle Version**: Requires Gradle 8.14+ (not 8.11)
2. **Flyway Starter**: Use `spring-boot-starter-flyway` instead of `flyway-core`
3. **Health Classes**: Moved from `org.springframework.boot.actuate.health` to `org.springframework.boot.health.contributor`
4. **Thymeleaf**: `#request` object no longer available by default

## Spring MCP Server Tools Used

This example was created using the Spring MCP Server tools:

| Tool | Purpose |
|------|---------|
| `getLatestSpringBootVersion(4, 0)` | Confirmed 4.0.0 as latest GA version |
| `listProjectsBySpringBootVersion(4, 0)` | Verified 42 compatible Spring projects |
| `getBreakingChanges("spring-boot", "4.0")` | Identified breaking changes |

Compatible projects include:
- Spring Framework 7.0.x
- Spring Security 7.0.x
- Spring Data 2025.1.x
- Spring Batch 6.0.x

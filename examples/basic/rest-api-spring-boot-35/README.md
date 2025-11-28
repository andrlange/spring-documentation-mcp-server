# REST API Example - Spring Boot 3.5.8

A simple REST API demonstrating CRUD operations with Spring Boot 3.5.8.

## Features

- RESTful Book API with CRUD operations
- In-memory storage (no database required)
- Java 21 Records for DTOs
- Bean Validation
- Actuator health endpoints
- Global exception handling

## Quick Start

```bash
# Run the application
./gradlew bootRun

# Or build and run JAR
./gradlew build
java -jar build/libs/rest-api-spring-boot-35-1.0.0.jar
```

**Application runs on:** http://localhost:8081

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
curl http://localhost:8081/api/books
```

### Get a specific book
```bash
curl http://localhost:8081/api/books/{id}
```

### Search books by title
```bash
curl "http://localhost:8081/api/books/search?title=Spring"
```

### Create a new book
```bash
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Learning Spring Boot",
    "author": "John Doe",
    "isbn": "978-1234567890",
    "publishedDate": "2024-01-15",
    "genre": "Technology",
    "pages": 350
  }'
```

### Update a book
```bash
curl -X PUT http://localhost:8081/api/books/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title"
  }'
```

### Delete a book
```bash
curl -X DELETE http://localhost:8081/api/books/{id}
```

## Actuator Endpoints

- Health: http://localhost:8081/actuator/health
- Info: http://localhost:8081/actuator/info
- Metrics: http://localhost:8081/actuator/metrics

## Technology Stack

- **Spring Boot**: 3.5.8
- **Java**: 21
- **Spring Framework**: 6.2.x
- **Build Tool**: Gradle 8.11

## Spring MCP Server Tools Used

This example was created using the Spring MCP Server tools:

| Tool | Purpose |
|------|---------|
| `getLatestSpringBootVersion(3, 5)` | Confirmed 3.5.8 as latest GA version |
| `listProjectsBySpringBootVersion(3, 5)` | Verified 48 compatible Spring projects |

Compatible projects include:
- Spring Framework 6.2.14
- Spring Security 6.5.7
- Spring Data 2025.0.6

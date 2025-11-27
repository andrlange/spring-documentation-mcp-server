# Spring Boot 4 Todo Application Example

A multi-user todo application built with **Spring Boot 4.0.0**, demonstrating modern Java development practices and Spring ecosystem integration.

## Features

- **Multi-user Authentication** - Spring Security with JPA-based user storage
- **Todo Management** - Create, update, delete, and organize todos
- **Priority & Status** - Support for priority levels (Low, Medium, High, Urgent) and status tracking
- **Due Dates** - Optional due dates with overdue detection
- **Dark Theme UI** - Modern Thymeleaf UI aligned with Spring.io design
- **Custom Actuator Endpoints** - Health indicators and custom statistics
- **PostgreSQL Database** - Production-ready data persistence

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 4.0.0 |
| Java | OpenJDK | 21 |
| Database | PostgreSQL | 18 |
| Security | Spring Security | 7.0.x |
| Data | Spring Data JPA | 4.0.x |
| UI | Thymeleaf + Bootstrap | 5.3 |
| Migration | Flyway | Latest |

## Quick Start

### 1. Start PostgreSQL Database

```bash
# From the todo-app-example directory
docker-compose up -d
```

This starts PostgreSQL on **port 5433** (to avoid conflicts).

### 2. Run the Application

```bash
./gradlew bootRun
```

Or build and run:

```bash
./gradlew build
java -jar build/libs/todo-app-example-1.0.0.jar
```

### 3. Access the Application

- **Application**: http://localhost:7777
- **Login**: `admin` / `admin123`
- **Actuator**: http://localhost:7777/actuator (Admin only)
- **Custom Stats**: http://localhost:7777/actuator/todostats

## Project Structure

```
todo-app-example/
├── src/main/java/com/example/todo/
│   ├── TodoApplication.java           # Main application
│   ├── config/
│   │   ├── SecurityConfig.java        # Spring Security configuration
│   │   └── DataInitializer.java       # Default admin user creation
│   ├── controller/
│   │   ├── AuthController.java        # Login/Register endpoints
│   │   ├── TodoController.java        # Todo CRUD operations
│   │   └── AdminController.java       # Admin dashboard
│   ├── dto/                           # Record-based DTOs
│   │   ├── TodoDto.java
│   │   ├── CreateTodoRequest.java
│   │   ├── UpdateTodoRequest.java
│   │   ├── UserDto.java
│   │   ├── RegisterRequest.java
│   │   └── TodoStats.java
│   ├── entity/
│   │   ├── User.java                  # User entity
│   │   └── Todo.java                  # Todo entity
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── TodoRepository.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── TodoService.java
│   │   └── CustomUserDetailsService.java
│   └── actuator/
│       ├── TodoStatsEndpoint.java     # Custom /actuator/todostats
│       ├── TodoHealthIndicator.java   # Custom health indicator
│       └── TodoInfoContributor.java   # Custom info contributor
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/
│   │   └── V1__init.sql
│   ├── static/css/
│   │   └── style.css                  # Dark Spring.io theme
│   └── templates/
│       ├── fragments/layout.html
│       ├── auth/
│       ├── todo/
│       └── admin/
├── docker-compose.yml
├── build.gradle
└── README.md
```

## Modern Java Features Used

### Java Records for DTOs

```java
public record TodoDto(
    Long id,
    @NotBlank String title,
    String description,
    Todo.Priority priority,
    Todo.Status status,
    LocalDate dueDate,
    boolean overdue
) {
    public static TodoDto fromEntity(Todo todo) {
        return new TodoDto(/* ... */);
    }
}
```

### Pattern Matching & Enhanced Switch (where applicable)

### JPA Enums

```java
public enum Priority { LOW, MEDIUM, HIGH, URGENT }
public enum Status { PENDING, IN_PROGRESS, COMPLETED, CANCELLED }
```

## Custom Actuator Endpoints

### `/actuator/todostats`

Returns comprehensive statistics:

```json
{
  "timestamp": "2025-11-27T12:00:00",
  "users": {
    "total": 5,
    "active": 4
  },
  "todos": {
    "total": 42,
    "pending": 15,
    "inProgress": 8,
    "completed": 17,
    "cancelled": 2,
    "overdue": 3,
    "dueToday": 2
  },
  "metrics": {
    "completionRate": 40.48,
    "averageTodosPerUser": 10.5
  }
}
```

### `/actuator/health`

Includes custom `todo` health indicator:

```json
{
  "status": "UP",
  "components": {
    "todo": {
      "status": "UP",
      "details": {
        "totalUsers": 5,
        "totalTodos": 42,
        "overdueTodos": 3
      }
    }
  }
}
```

---

## Spring MCP Server Usage

This example was created using the **Spring Documentation MCP Server** to assist with development decisions. Here's how the MCP server tools were used:

### 1. Version Selection

```
Tool: listSpringBootVersions(state: "GA", limit: 5)
Result: Identified Spring Boot 4.0.0 as the latest GA release
```

### 2. Compatibility Check

```
Tool: listProjectsBySpringBootVersion(majorVersion: 4, minorVersion: 0)
Result: Verified compatible versions:
- Spring Framework 7.0.x
- Spring Security 7.0.x
- Spring Data JPA 4.0.x
```

### 3. Project Discovery

```
Tool: findProjectsByUseCase(useCase: "security")
Result: Found Spring Security for authentication implementation

Tool: findProjectsByUseCase(useCase: "data access")
Result: Found Spring Data for JPA integration
```

### 4. Version Details

```
Tool: getSpringVersions(project: "spring-boot")
Result: Full version list including GA, RC, SNAPSHOT versions

Tool: getLatestSpringBootVersion(majorVersion: 4, minorVersion: 0)
Result: 4.0.0 confirmed as latest stable
```

### MCP Server Tools Summary

| Tool Used | Purpose |
|-----------|---------|
| `listSpringBootVersions` | Identify latest Spring Boot 4.x GA version |
| `listProjectsBySpringBootVersion` | Check ecosystem compatibility for Boot 4.0 |
| `findProjectsByUseCase` | Discover relevant projects (security, data) |
| `getSpringVersions` | Get detailed version info for spring-boot |
| `filterSpringBootVersionsBySupport` | Verify OSS support status |
| `listSpringProjects` | Explore available Spring projects |

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 7777 | Application port |
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5433 | PostgreSQL port |
| `DB_NAME` | todo_app | Database name |
| `DB_USER` | todouser | Database user |
| `DB_PASSWORD` | todopass | Database password |

### Security

- Passwords hashed with BCrypt (cost factor 12)
- Session timeout: 30 minutes
- Remember-me: 7 days
- CSRF protection enabled

## Development

### Run Tests

```bash
./gradlew test
```

### Hot Reload

DevTools enabled for development - changes auto-reload.

## License

This example is part of the Spring MCP Server project for demonstration purposes.

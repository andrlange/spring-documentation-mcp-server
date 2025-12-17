# Library Management System Example - Summary

The Library Management System demonstrates **Spring Boot 4.0.0**, **Spring Modulith 2.0**, **Spring Framework 7 API Versioning**, and **springdoc-openapi 3.0** with a dark-themed Thymeleaf UI.

> **Note**: This demo showcases modular monolith architecture with Named Interfaces, event-driven communication with Event Publication Registry, externalized events for Kafka/RabbitMQ, and modern API documentation using the Spring MCP Server for version info and architecture patterns.

## Created: `examples/basic/lms-modulith-openapi/`

### Project Structure

```
lms-modulith-openapi/
├── build.gradle                    # Spring Boot 4.0.0, Java 25
├── settings.gradle
├── docker-compose.yml              # PostgreSQL 18 on port 5433
├── README.md                       # Full documentation
├── assets/
│   └── dashboard.png               # Screenshot
├── src/main/java/com/example/library/
│   ├── LibraryApplication.java
│   ├── catalog/                    # Catalog Module
│   │   ├── api/                    # @NamedInterface("api")
│   │   │   ├── BookController.java
│   │   │   └── BookService.java
│   │   ├── internal/               # Book, Author, Category
│   │   └── events/                 # BookAddedEvent
│   ├── members/                    # Members Module
│   │   ├── api/                    # MemberController, MemberService
│   │   ├── internal/               # Member, MembershipCard
│   │   └── events/                 # MemberRegisteredEvent
│   ├── loans/                      # Loans Module
│   │   ├── api/                    # LoanController, LoanService
│   │   ├── internal/               # Loan, LoanPolicy
│   │   └── events/                 # BookLoanedEvent
│   ├── notifications/              # Notifications Module
│   │   ├── api/                    # NotificationController, NotificationService
│   │   └── internal/               # Event listeners
│   ├── shared/                     # Shared utilities (open)
│   ├── config/                     # Configuration (open)
│   │   ├── OpenApiConfig.java
│   │   ├── LibraryHealthIndicator.java
│   │   └── EventPublicationController.java  # Event monitoring API
│   └── web/                        # Thymeleaf controllers (open)
└── src/main/resources/
    ├── application.yml             # Port 8088, DB port 5433
    ├── db/migration/               # Flyway migrations (V1 init, V2 event_publication)
    ├── static/css/dark-theme.css   # Dark Spring.io theme
    └── templates/                  # Thymeleaf templates
        ├── layout/main.html
        ├── dashboard.html
        ├── books/list.html, detail.html, edit.html
        ├── members/list.html, detail.html, edit.html, cards.html
        ├── loans/list.html, detail.html, member.html, new.html
        ├── notifications/list.html
        ├── authors/list.html
        └── categories/list.html
```

### Quick Start

```bash
cd examples/basic/lms-modulith-openapi

# Start PostgreSQL
docker-compose up -d

# Run the application
./gradlew bootRun
```

**Web UI**: http://localhost:8088
**Swagger UI**: http://localhost:8088/swagger-ui.html
**Health**: http://localhost:8088/actuator/health

**API Endpoints**:

```bash
# List all books
curl http://localhost:8088/api/books

# Get book by ISBN
curl http://localhost:8088/api/books/978-1-61729-875-6

# Create a loan
curl -X POST http://localhost:8088/api/loans \
  -H "Content-Type: application/json" \
  -d '{"bookIsbn":"978-1-61729-875-6","memberId":1}'

# Check health
curl http://localhost:8088/actuator/health

# View event publication statistics
curl http://localhost:8088/api/admin/events/statistics

# List externalized event topics
curl http://localhost:8088/api/admin/events/topics
```

---

## How the Spring MCP Server Was Used

This example leveraged the Spring MCP Server to get accurate version information and architectural guidance.

| Tool/Flavor Used | Purpose | Outcome |
|------------------|---------|---------|
| `listSpringBootVersions` | Get available Spring Boot versions | Spring Boot 4.0.0 (GA) |
| `getSpringVersions("spring-modulith")` | Get Spring Modulith versions | Spring Modulith 2.0.0 |
| `getSpringBootLanguageRequirements("4.0.0")` | Get Java requirements | Java 25 (LTS) required |
| `searchFlavors("modulith")` | Find modulith patterns | Found spring-modulith flavor |
| `getFlavorByName("spring-modulith")` | Get implementation guide | Module structure, Named Interfaces |
| `getFlavorByName("spring-openapi-doc-sb4")` | Get OpenAPI guide | springdoc-openapi 3.0 config |

### Key Architecture Patterns Applied

From the `spring-modulith` flavor:

| Pattern | Implementation |
|---------|---------------|
| **Module Boundaries** | Each module in its own package with `package-info.java` |
| **Named Interfaces** | `@NamedInterface("api")` for public APIs |
| **Event-Driven** | Events published between modules, not direct calls |
| **Event Publication Registry** | JPA-based persistent events with at-least-once delivery |
| **Externalized Events** | `@Externalized` annotations for Kafka/RabbitMQ integration |
| **Idempotent Handlers** | Event listeners check for duplicates before processing |
| **Internal Encapsulation** | Entities and repositories in `internal/` packages |

From the `spring-openapi-doc-sb4` flavor:

| Feature | Implementation |
|---------|---------------|
| **springdoc-openapi 3.0** | Swagger UI at `/swagger-ui.html` |
| **API Groups** | Separate groups for catalog, members, loans, notifications |
| **OpenAPI 3.1** | Full schema documentation |

---

## Prompts Used

This demo required **four prompts** (iterative development):

### Prompt 1: Initial Request

```
Create a Library Management System demo in examples/basic/lms-modulith-openapi
using Spring Boot 4.0.0, Spring Modulith 2.0, and springdoc-openapi 3.0.
Use PostgreSQL on port 5433. Include a dark theme Thymeleaf UI.
Use Spring MCP Server to get version info and architecture flavors.
```

### Prompt 2: Add Web UI Pages

```
Add Thymeleaf pages for dashboard, books, members, loans with dark theme.
Create dynamic interactions. Follow the patterns from rate-limiter-demo.
```

### Prompt 3: Add Data Pages

```
For the authors, categories and membership cards endpoints the page only
shows an empty page with the hint to use the api docs. Add read only lists
to show this info extracted from their main objects.
```

### Prompt 4: Spring Modulith 2.0 Event System

```
Add Spring Modulith 2.0 Event Publication Registry support with:
- Persistent event storage for at-least-once delivery
- @Externalized annotations for Kafka/RabbitMQ integration
- Idempotent event handling for retry safety
- Event monitoring API endpoints
```

**Changes implemented:**
- Added `spring-modulith-starter-jpa` for Event Publication Registry
- Created `V2__add_event_publication_table.sql` Flyway migration
- Added `@Externalized` annotations to all domain events
- Implemented idempotency checks in event listeners
- Created `EventPublicationController` for event monitoring API

---

## Features Implemented

- **Spring Modulith 2.0** - Named Interfaces, module boundaries, event publishing
- **Event Publication Registry** - JPA-based persistent events with at-least-once delivery
- **Externalized Events** - `@Externalized` annotations for Kafka/RabbitMQ integration
- **Idempotent Event Handling** - Listeners check for duplicates before processing
- **Event Monitoring API** - REST endpoints for event publication statistics and management
- **Spring Framework 7** - Native API versioning with `@GetMapping(version = "2.0+")`
- **springdoc-openapi 3.0** - Swagger UI, API groups per module
- **Dark Theme UI** - Thymeleaf 3.4, Bootstrap 5, HTMX
- **Custom Actuators** - Library health indicator, info contributor, event publications
- **Event-Driven** - BookLoanedEvent, MemberRegisteredEvent, etc.
- **Full Web UI** - Dashboard, books, members, loans with detail/edit pages, notifications, authors, categories

---

## Module Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Library Management System                       │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────┐    ┌───────────────┐   ┌──────────────┐              │
│  │   Catalog    │    │   Members     │   │    Loans     │              │
│  │    Module    │    │    Module     │   │    Module    │              │
│  ├──────────────┤    ├───────────────┤   ├──────────────┤              │
│  │ • Books      │    │ • Member      │   │ • Loan       │              │
│  │ • Authors    │    │   Registration│   │   Processing │              │
│  │ • Categories │    │ • Profiles    │   │ • Returns    │              │
│  │ • Search     │    │ • Cards       │   │ • Overdue    │              │
│  └──────┬───────┘    └──────┬────────┘   └──────┬───────┘              │
│         │                   │                   │                      │
│         └───────────────────┼───────────────────┘                      │
│                             │ Events                                   │
│                    ┌────────▼────────┐                                 │
│                    │  Notifications  │                                 │
│                    │     Module      │                                 │
│                    ├─────────────────┤                                 │
│                    │ • Event Listener│                                 │
│                    │ • Alerts        │                                 │
│                    └─────────────────┘                                 │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│                         Shared Kernel                                  │
│              (Common types, events, utilities)                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Component | Version |
|-----------|---------|
| Spring Boot | 4.0.0 |
| Spring Framework | 7.0.1 |
| Spring Modulith | 2.0.0 |
| Java | 25 (LTS) |
| PostgreSQL | 18 |
| springdoc-openapi | 3.0.0 |
| Thymeleaf | 3.4 |
| Bootstrap | 5.3.3 |
| Gradle | 9.2.0 |

---

## Files Created

| File | Description |
|------|-------------|
| `LibraryApplication.java` | Main application with modulith bootstrap |
| `BookController.java` | REST API with versioning |
| `BookService.java` | Catalog business logic |
| `MemberController.java` | Member REST API |
| `MemberService.java` | Member business logic |
| `LoanController.java` | Loan REST API |
| `LoanService.java` | Loan processing logic |
| `NotificationEventListener.java` | Event-driven notifications with idempotency |
| `WebController.java` | Thymeleaf web controller |
| `LibraryHealthIndicator.java` | Custom health indicator |
| `OpenApiConfig.java` | Swagger UI configuration |
| `EventPublicationController.java` | Event publication monitoring API |
| `V2__add_event_publication_table.sql` | Spring Modulith event registry schema |
| `dark-theme.css` | Spring.io inspired dark theme |
| `dashboard.html` | Dashboard with statistics |
| `books/list.html, detail.html, edit.html` | Book pages |
| `members/list.html, detail.html, edit.html, cards.html` | Member pages |
| `loans/list.html, detail.html, member.html, new.html` | Loan pages |
| `authors/list.html` | Author listing |
| `categories/list.html` | Category listing |

---

## Web UI Pages

| Page | URL | Features |
|------|-----|----------|
| Dashboard | `/` | Statistics, quick actions, recent activity |
| Books | `/books` | Search, category filter, availability status |
| Book Detail | `/books/{isbn}` | Full info, loan history, active loan |
| Edit Book | `/books/{isbn}/edit` | Edit book title, author, category, copies |
| Members | `/members` | List, status/type filters, loan counts |
| Member Detail | `/members/{id}` | Full profile, loans, notifications, card |
| Edit Member | `/members/{id}/edit` | Edit name, email, type, status |
| Membership Cards | `/members/cards` | Card status, expiry warnings |
| Loans | `/loans` | Active loans, overdue alerts |
| Loan Detail | `/loans/{id}` | Full loan info, timeline, actions |
| Member Loans | `/loans/member/{id}` | All loans for specific member |
| New Loan | `/loans/new` | Book/member selection form |
| Authors | `/authors` | Author list with book counts |
| Categories | `/categories` | Category cards with descriptions |
| Notifications | `/notifications` | All notifications |

---

## API Documentation Groups

| Group | Endpoints | Description |
|-------|-----------|-------------|
| All APIs | `/swagger-ui.html?group=all-apis` | Complete API |
| Catalog | `/swagger-ui.html?group=catalog-api` | Books, Authors, Categories |
| Members | `/swagger-ui.html?group=members-api` | Member management |
| Loans | `/swagger-ui.html?group=loans-api` | Loan processing |
| Notifications | `/swagger-ui.html?group=notifications-api` | Alerts |

---

## Key Lessons Learned

1. **Spring Modulith 2.0** provides clear module boundaries without microservices complexity
2. **Named Interfaces** (`@NamedInterface`) control what's exposed between modules
3. **Event-driven communication** keeps modules loosely coupled
4. **Event Publication Registry** enables at-least-once delivery with automatic retry
5. **Externalized Events** (`@Externalized`) prepare events for Kafka/RabbitMQ without code changes
6. **Idempotent Event Handlers** are essential when events can be replayed
7. **Spring Framework 7 API versioning** is built-in - no external libraries needed
8. **springdoc-openapi 3.0** integrates seamlessly with Spring Boot 4

---

## Sample Data Included

- **Categories**: 6 (Fiction, Science, History, Technology, Philosophy, Business)
- **Authors**: 6 with biographies and birth years
- **Books**: 6 sample books
- **Members**: 3 (ACTIVE status)
- **Loan Policies**: STANDARD (5 books, 14 days) and PREMIUM (10 books, 21 days)

---

*Updated: 2025-12-17*

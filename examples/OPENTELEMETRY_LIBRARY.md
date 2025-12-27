# OpenTelemetry Library Demo - Spring Boot 4.0.1

> **Project Location**: `examples/operations/open-telemetry-library`
> **Status**: ✅ COMPLETE - All 10 Phases Implemented

## Overview

A Spring Boot 4.0.1 demo application showcasing the new OpenTelemetry features for comprehensive observability including Logging, Tracing, Metrics, and Dashboards. The application simulates a Book Library system with mocked data to demonstrate real-world observability patterns.

### Goals

1. **Demonstrate Spring Boot 4.0.1 OpenTelemetry Integration**
   - Out-of-the-box metrics (JVM, HTTP, etc.)
   - Custom business metrics with `@Observed`
   - Distributed tracing with cascaded spans
   - Structured logging with trace correlation

2. **Show Grafana LGTM Stack Integration**
   - Loki for logs
   - Grafana for visualization
   - Tempo for traces
   - Mimir for metrics

3. **Provide Interactive Demo Controls**
   - Simulate different latency patterns (10ms, 50ms, 500ms)
   - Generate batch requests for testing
   - View correlated traces through application layers

4. **Create Comprehensive Developer Documentation**
   - Step-by-step guide for OpenTelemetry setup
   - Grafana LGTM stack explanation and configuration
   - Custom metrics implementation patterns
   - Distributed tracing best practices
   - Structured logging with trace correlation
   - Testing strategies with OpenTelemetry

5. **Showcase Spring MCP Server for AI-Assisted Development**
   - Document prompts used during implementation
   - Track MCP tools used (versions, docs, dependencies, flavors)
   - Demonstrate how Spring MCP Server accelerates development
   - Provide reference for future AI-assisted Spring projects

---

## Technical Stack

| Component | Version      | Purpose |
|-----------|--------------|---------|
| Java | 25           | Runtime |
| Spring Boot | 4.0.1        | Application Framework |
| spring-boot-starter-opentelemetry | 4.0.1        | OpenTelemetry auto-configuration |
| opentelemetry-logback-appender | 2.23.0-alpha | Log export to OTLP |
| Thymeleaf | 3.4+         | Server-side templating |
| Bootstrap | 5.3.3        | UI framework (dark theme) |
| Alpine.js | 3.x          | Reactive UI components |
| HTMX | 1.9.10       | Dynamic UI updates |
| Grafana LGTM | latest       | Observability stack |

---

## Project Structure

```
examples/operations/open-telemetry-library/
├── compose.yaml                           # Grafana LGTM stack
├── build.gradle                           # Gradle build configuration
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/com/example/library/
│   │   │   ├── LibraryApplication.java    # Main application
│   │   │   ├── config/
│   │   │   │   └── OpenTelemetryConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── WebController.java     # Thymeleaf UI controller
│   │   │   │   ├── BookController.java    # REST API
│   │   │   │   ├── MemberController.java  # REST API
│   │   │   │   └── LoanController.java    # REST API
│   │   │   ├── service/
│   │   │   │   ├── BookService.java       # Book operations with @Observed
│   │   │   │   ├── MemberService.java     # Member operations
│   │   │   │   ├── LoanService.java       # Loan operations
│   │   │   │   ├── CategoryService.java   # Category operations
│   │   │   │   └── SimulationService.java # Latency simulation
│   │   │   ├── model/
│   │   │   │   ├── Book.java
│   │   │   │   ├── Category.java
│   │   │   │   ├── Member.java
│   │   │   │   └── Loan.java
│   │   │   ├── data/
│   │   │   │   └── DataInitializer.java   # Mock data generation
│   │   │   └── metrics/
│   │   │       └── LibraryMetrics.java    # Custom metrics
│   │   └── resources/
│   │       ├── application.yaml           # Configuration
│   │       ├── logback-spring.xml         # OTEL logging
│   │       ├── static/
│   │       │   └── css/
│   │       │       └── dark-theme.css     # Dark theme
│   │       └── templates/
│   │           ├── layout/
│   │           │   └── main.html          # Base layout
│   │           ├── dashboard.html         # Main dashboard
│   │           ├── simulation.html        # Trace simulation
│   │           └── fragments/             # Reusable components
│   └── test/
│       └── java/com/example/library/
│           └── LibraryApplicationTests.java
└── README.md
```

---

## Data Model

### Book (200 instances)
```java
public record Book(
    String isbn,
    String title,
    String author,
    Long categoryId,
    int publicationYear,
    int availableCopies,
    int totalCopies
) {}
```

### Category (10 instances)
```java
public record Category(
    Long id,
    String name,
    String description
) {}
```

### Member (20 instances)
```java
public record Member(
    Long id,
    String name,
    String email,
    String membershipType,  // STANDARD, PREMIUM
    LocalDate joinDate,
    boolean active
) {}
```

### Loan
```java
public record Loan(
    Long id,
    String bookIsbn,
    Long memberId,
    LocalDate loanDate,
    LocalDate dueDate,
    LocalDate returnDate,  // null if not returned
    boolean overdue
) {}
```

---

## Mock Data Generation

All data is generated at application startup relative to the current date:

### Categories (10)
```
1. Fiction
2. Science Fiction
3. Mystery/Thriller
4. Romance
5. Biography
6. History
7. Science & Technology
8. Business
9. Self-Help
10. Children's Books
```

### Books (200)
- Distributed across categories (20 books per category)
- Random publication years (2000-2024)
- Random availability (1-5 copies per title)
- Authors from predefined list

### Members (20)
- 15 STANDARD, 5 PREMIUM memberships
- Join dates: last 2 years relative to app start
- 2-3 inactive members

### Loans
- Generated based on current date
- Mix of:
  - Active loans (due in future)
  - Overdue loans (past due date)
  - Returned loans (completed)
- Loan periods: 14, 21, or 30 days based on membership

---

## OpenTelemetry Configuration

### Dependencies (build.gradle)
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

dependencies {
    // Core
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // OpenTelemetry
    implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
    implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha'

    // Docker Compose
    runtimeOnly 'org.springframework.boot:spring-boot-docker-compose'

    // AOP for @Observed
    implementation 'org.springframework.boot:spring-boot-starter-aop'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-opentelemetry-test'
}
```

### application.yaml
```yaml
spring:
  application:
    name: opentelemetry-library

  # OTLP Metrics export
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics

  # OpenTelemetry configuration
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
    resource:
      attributes:
        service.name: opentelemetry-library
        service.version: 1.0.0
        deployment.environment: development

management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for demo
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: opentelemetry-library

server:
  port: 8080
  shutdown: immediate

logging:
  level:
    com.example.library: INFO
    org.springframework.boot.actuator.autoconfigure.opentelemetry: DEBUG
    org.springframework.boot.docker.compose: DEBUG
```

### logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>

    <appender name="OTEL"
              class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="OTEL"/>
    </root>
</configuration>
```

### OpenTelemetry Appender Installer
```java
@Component
class InstallOpenTelemetryAppender implements InitializingBean {
    private final OpenTelemetry openTelemetry;

    InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }
}
```

---

## Docker Compose (compose.yaml)

```yaml
services:
  grafana-lgtm:
    image: 'grafana/otel-lgtm:latest'
    ports:
      - '3000:3000'    # Grafana UI
      - '4317:4317'    # OTLP gRPC
      - '4318:4318'    # OTLP HTTP
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
```

**Grafana Access**: http://localhost:3000
- Default credentials: admin/admin
- Pre-configured data sources for Loki, Tempo, Mimir

---

## UI Design

### Dark Theme (based on lms-modulith-openapi)

Uses CSS variables for consistent dark theming:
```css
:root {
    --bs-body-bg: #1a1d21;
    --bs-body-color: #e9ecef;
    --bs-card-bg: #212529;
    --bs-border-color: #495057;
    --bs-link-color: #6ea8fe;
    --sidebar-bg: #16181b;
    --navbar-bg: #212529;
    --table-hover-bg: #2c3034;
}
```

### Pages

#### 1. Dashboard (`/`)
- **KPI Stats Cards**: Total Books, Active Members, Active Loans, Overdue Loans
- **Category Distribution**: Books per category chart
- **Recent Activity**: Latest loans table
- **Quick Actions**: Links to simulation and Grafana

#### 2. Simulation Page (`/simulation`)
- **Latency Selector**: 10ms / 50ms / 500ms buttons
- **Request Count**: Slider or input (1-100 requests)
- **Action Buttons**:
  - Search Books (random search)
  - Create Loan (random member + book)
  - Return Book (random active loan)
  - Mixed Operations (combination)
- **Live Trace Display**: Shows trace IDs with links to Grafana/Tempo

#### 3. Status Page (`/status`)
- **System KPIs**: All counts for verification
- **Metrics Comparison**: Side-by-side with Grafana dashboard
- **Direct Links**: To relevant Grafana panels

### Navigation
```
Library Demo
├── Dashboard (Overview)
├── Catalog
│   ├── Books
│   └── Categories
├── Members
├── Loans
│   └── Active / Overdue
├── Observability
│   ├── Simulation
│   └── Status
└── External Links
    ├── Grafana Dashboard
    └── Actuator Health
```

---

## Custom Metrics

### Business Metrics (using @Observed)

```java
@Service
public class BookService {

    @Observed(name = "library.book.search",
              contextualName = "book-search",
              lowCardinalityKeyValues = {"operation", "search"})
    public List<Book> searchBooks(String query) {
        // Implementation
    }

    @Observed(name = "library.book.checkout",
              contextualName = "book-checkout")
    public void checkoutBook(String isbn, Long memberId) {
        // Implementation
    }
}
```

### Custom Counter Metrics

```java
@Component
public class LibraryMetrics {
    private final MeterRegistry registry;

    // Counters
    private final Counter bookSearchCounter;
    private final Counter loanCreatedCounter;
    private final Counter loanReturnedCounter;

    // Gauges
    private final AtomicInteger activeLoansGauge;
    private final AtomicInteger overdueLoansGauge;

    public LibraryMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.bookSearchCounter = Counter.builder("library.books.searched")
            .description("Number of book searches")
            .register(registry);

        this.loanCreatedCounter = Counter.builder("library.loans.created")
            .description("Number of loans created")
            .register(registry);

        this.loanReturnedCounter = Counter.builder("library.loans.returned")
            .description("Number of loans returned")
            .register(registry);

        this.activeLoansGauge = registry.gauge("library.loans.active",
            new AtomicInteger(0));
        this.overdueLoansGauge = registry.gauge("library.loans.overdue",
            new AtomicInteger(0));
    }

    public void recordBookSearch() { bookSearchCounter.increment(); }
    public void recordLoanCreated() { loanCreatedCounter.increment(); }
    public void recordLoanReturned() { loanReturnedCounter.increment(); }
    public void updateActiveLoans(int count) { activeLoansGauge.set(count); }
    public void updateOverdueLoans(int count) { overdueLoansGauge.set(count); }
}
```

### Metrics to Display

| Metric Name | Type | Description |
|-------------|------|-------------|
| `library.books.total` | Gauge | Total books in system |
| `library.books.available` | Gauge | Available book copies |
| `library.books.searched` | Counter | Book search operations |
| `library.members.total` | Gauge | Total members |
| `library.members.active` | Gauge | Active members |
| `library.loans.active` | Gauge | Currently active loans |
| `library.loans.overdue` | Gauge | Overdue loans |
| `library.loans.created` | Counter | Loans created |
| `library.loans.returned` | Counter | Loans returned |
| `library.categories.total` | Gauge | Number of categories |

---

## Tracing Strategy

### Trace Hierarchy
```
HTTP Request (TraceId: abc123)
└── Controller Span (SpanId: 001)
    ├── Service Span (SpanId: 002)
    │   └── Repository Span (SpanId: 003)
    │       └── Simulated DB Call (SpanId: 004)
    └── Another Service Span (SpanId: 005)
```

### Simulated Latency
```java
@Service
public class SimulationService {
    private final Tracer tracer;

    public void simulateWork(int durationMs, String operationName) {
        Span span = tracer.spanBuilder(operationName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("simulation.duration_ms", durationMs);
            Thread.sleep(durationMs);
        } finally {
            span.end();
        }
    }

    public void simulateDatabaseCall(String tableName) {
        Span span = tracer.spanBuilder("db.query")
            .setAttribute("db.system", "postgresql")
            .setAttribute("db.operation", "SELECT")
            .setAttribute("db.table", tableName)
            .startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Simulate DB latency
            Thread.sleep(5 + (int)(Math.random() * 15));
        } finally {
            span.end();
        }
    }
}
```

### Cascaded Trace Example
```
POST /api/loans (500ms total)
├── LoanController.createLoan (5ms)
│   ├── MemberService.findById (15ms)
│   │   └── db.query [members] (8ms)
│   ├── BookService.findByIsbn (12ms)
│   │   └── db.query [books] (6ms)
│   ├── BookService.checkAvailability (10ms)
│   │   └── db.query [inventory] (5ms)
│   ├── LoanService.createLoan (450ms) <- Simulated delay
│   │   ├── db.query [loans] INSERT (10ms)
│   │   └── db.query [books] UPDATE (8ms)
│   └── NotificationService.sendConfirmation (async)
```

---

## Implementation Phases

### Phase 1: Project Setup
- [x] Create project directory structure
- [x] Set up build.gradle with dependencies
- [x] Configure compose.yaml for Grafana LGTM
- [x] Create application.yaml with OTLP configuration
- [x] Set up logback-spring.xml for log export
- [x] Create InstallOpenTelemetryAppender component

### Phase 2: Data Layer
- [x] Create data models (Book, Category, Member, Loan records)
- [x] Implement DataInitializer with mock data
  - [x] 10 categories
  - [x] 200 books across categories
  - [x] 20 members (15 standard, 5 premium)
  - [x] Generate initial loans
- [x] Create service classes with in-memory storage
  - [x] BookService
  - [x] CategoryService
  - [x] MemberService
  - [x] LoanService

### Phase 3: REST API
- [x] BookController (GET /api/books, GET /api/books/{isbn}, GET /api/books/search)
- [x] MemberController (GET /api/members, GET /api/members/{id})
- [x] LoanController (GET /api/loans, POST /api/loans, PUT /api/loans/{id}/return)
- [x] Add @Observed annotations to all service methods

### Phase 4: Custom Metrics
- [x] Create LibraryMetrics component
- [x] Implement counters for book searches, loans, returns
- [x] Implement gauges for active/overdue loans
- [x] Wire metrics into service layer

### Phase 5: UI Implementation
- [x] Create dark-theme.css (based on lms-modulith-openapi)
- [x] Create main.html layout template
- [x] Implement dashboard.html with KPI cards
- [x] Implement simulation.html with controls
- [x] Implement status.html for metrics comparison
- [x] Add books.html, members.html, loans.html list pages

### Phase 6: Simulation Features
- [x] Create SimulationService for latency injection
- [x] Implement latency selector (10ms, 50ms, 500ms)
- [x] Add batch request generator (1-100 requests)
- [x] Create simulation endpoints:
  - [x] POST /api/simulate/search
  - [x] POST /api/simulate/loan
  - [x] POST /api/simulate/return
  - [x] POST /api/simulate/mixed
- [x] Display trace IDs with Grafana/Tempo links

### Phase 7: Tracing Enhancements
- [x] Add custom span attributes for business context
- [x] Implement cascaded spans through service layers
- [x] Add simulated database call spans
- [x] Ensure trace ID correlation in logs

### Phase 8: Testing & Verification
- [x] Write basic integration tests
- [x] Write OpenTelemetry-specific tests using `spring-boot-starter-opentelemetry-test`
- [x] Verify metrics appear in Grafana/Mimir
- [x] Verify traces appear in Grafana/Tempo
- [x] Verify logs appear in Grafana/Loki
- [x] Test trace correlation across service layers

### Phase 9: Developer Documentation (README.md)
Create comprehensive developer documentation covering:

- [x] **1. OpenTelemetry Introduction**
  - [x] What is OpenTelemetry and why use it
  - [x] Spring Boot 4.0.1 native OpenTelemetry support
  - [x] The three pillars: Metrics, Traces, Logs
  - [x] OTLP protocol overview

- [x] **2. Grafana LGTM Stack**
  - [x] What is LGTM (Loki, Grafana, Tempo, Mimir)
  - [x] How Grafana Labs' open-source observability works
  - [x] Docker setup and configuration
  - [x] Data source connections and queries
  - [x] Creating custom dashboards

- [x] **3. Custom Metrics Guide**
  - [x] Using `@Observed` annotation
  - [x] Creating counters, gauges, histograms with Micrometer
  - [x] Best practices for metric naming
  - [x] Viewing metrics in Grafana/Mimir
  - [x] Example queries (PromQL)

- [x] **4. Distributed Tracing Guide**
  - [x] Understanding traces, spans, and context
  - [x] Automatic instrumentation with Spring Boot
  - [x] Creating custom spans with Tracer API
  - [x] Adding span attributes for business context
  - [x] Viewing traces in Grafana/Tempo
  - [x] Trace correlation with logs

- [x] **5. Structured Logging Guide**
  - [x] OpenTelemetry Logback Appender setup
  - [x] Trace ID and Span ID injection
  - [x] Log correlation in Grafana/Loki
  - [x] LogQL query examples
  - [x] Best practices for log levels and messages

- [x] **6. Testing with OpenTelemetry**
  - [x] Using `spring-boot-starter-opentelemetry-test`
  - [x] Testing spans and traces
  - [x] Verifying custom metrics in tests
  - [x] Integration testing strategies
  - [x] Example test cases

- [x] **7. Troubleshooting Guide**
  - [x] Common issues and solutions
  - [x] Debugging OTLP export
  - [x] Verifying data flow

### Phase 10: Spring MCP Server Usage Tracking
Document the AI-assisted development process:

- [x] Record prompts used during implementation
- [x] Track MCP tools used (versions, dependencies, docs, flavors)
- [x] Document flavors applied for architecture patterns
- [x] Add "Prompts Used" section to README.md
- [x] Add "MCP Tools Used" section to README.md
- [x] Add "Flavors Used" section to README.md

---

## Grafana Dashboard Panels

### Metrics Dashboard
1. **Application Health**
   - JVM Memory Usage
   - JVM Thread Count
   - CPU Usage

2. **HTTP Metrics**
   - Request Rate (requests/sec)
   - Response Time Distribution
   - Error Rate (4xx, 5xx)

3. **Business Metrics**
   - Book Searches Over Time
   - Loans Created Over Time
   - Active Loans Gauge
   - Overdue Loans Gauge

4. **Library Stats**
   - Total Books
   - Total Members
   - Books by Category

### Trace Explorer
- Filter by service name: `opentelemetry-library`
- Search by trace ID
- View cascaded spans
- Analyze latency distribution

### Log Explorer
- Filter by trace ID for correlation
- Search by log level
- Filter by component (controller, service, etc.)

---

## Running the Demo

### Start the Application
```bash
cd examples/operations/open-telemetry-library

# Start Grafana LGTM stack (auto-started by Docker Compose support)
./gradlew bootRun
```

### Access Points
| Service | URL | Description |
|---------|-----|-------------|
| Application | http://localhost:8080 | Demo UI |
| Grafana | http://localhost:3000 | Dashboards |
| Actuator Health | http://localhost:8080/actuator/health | Health endpoint |
| Actuator Metrics | http://localhost:8080/actuator/metrics | Metrics endpoint |

### Demo Workflow
1. Open Application UI at http://localhost:8080
2. View Dashboard KPIs
3. Navigate to Simulation page
4. Select latency (500ms) and request count (10)
5. Click "Mixed Operations" button
6. Copy trace IDs and view in Grafana/Tempo
7. Compare KPIs in UI vs Grafana metrics
8. View correlated logs in Loki

---

## Key Learning Points

1. **Spring Boot 4.0.1 OpenTelemetry Starter**
   - Auto-configuration for metrics, traces, logs
   - OTLP export without manual configuration

2. **@Observed Annotation**
   - Automatic span creation for annotated methods
   - Custom metric names and attributes

3. **Trace Context Propagation**
   - Automatic trace ID injection in logs
   - Cross-service correlation (simulated in this demo)

4. **Grafana LGTM Stack**
   - Unified observability platform
   - Pre-configured data sources
   - Easy local development setup

5. **Custom Metrics with Micrometer**
   - Counters for events
   - Gauges for current state
   - Histograms for distributions

---

## References

- [Spring Boot 4.0.1 OpenTelemetry Documentation](https://docs.spring.io/spring-boot/reference/)
- [OpenTelemetry Java Instrumentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Grafana LGTM Docker Image](https://grafana.com/blog/2024/03/13/an-opentelemetry-backend-in-a-docker-image-introducing-grafana/otel-lgtm/)
- [Micrometer Documentation](https://micrometer.io/docs)
- Base Demo: `playground/ot/`
- UI Reference: `examples/basic/lms-modulith-openapi/`

---

## README.md Documentation Structure

The final README.md should include comprehensive developer documentation:

### Required Sections

```markdown
# OpenTelemetry Library Demo - Spring Boot 4.0.1

> **AI-Generated Demo**: This application was created using Claude Code with
> the Spring Documentation MCP Server. See [Prompts Used](#prompts-used) and
> [MCP Tools Used](#mcp-tools-used) sections below.

## Table of Contents
1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [OpenTelemetry Guide](#opentelemetry-guide)
   - [Introduction](#introduction)
   - [Configuration](#configuration)
4. [Grafana LGTM Stack](#grafana-lgtm-stack)
   - [What is LGTM](#what-is-lgtm)
   - [Setup & Configuration](#setup--configuration)
   - [Dashboards](#dashboards)
5. [Custom Metrics](#custom-metrics)
   - [@Observed Annotation](#observed-annotation)
   - [Micrometer Counters & Gauges](#micrometer-counters--gauges)
   - [PromQL Queries](#promql-queries)
6. [Distributed Tracing](#distributed-tracing)
   - [Automatic Instrumentation](#automatic-instrumentation)
   - [Custom Spans](#custom-spans)
   - [Viewing in Tempo](#viewing-in-tempo)
7. [Structured Logging](#structured-logging)
   - [Logback Appender](#logback-appender)
   - [Trace Correlation](#trace-correlation)
   - [LogQL Queries](#logql-queries)
8. [Testing with OpenTelemetry](#testing-with-opentelemetry)
   - [Test Dependencies](#test-dependencies)
   - [Testing Spans](#testing-spans)
   - [Testing Metrics](#testing-metrics)
9. [API Reference](#api-reference)
10. [Prompts Used](#prompts-used)
11. [MCP Tools Used](#mcp-tools-used)
```

---

## Prompts Used (Template)

> **Instructions**: Record each significant prompt used during implementation.
> Update this section as the demo is built.

### Prompt 1: Initial Planning Request

```
Create a demo using Spring Boot 4.0.1 and the new OpenTelemetry features for
Logging, Tracing, Metrics Dashboard. Use the concept from playground/ot/ as base
and its compose.yml for Grafana LGTM stack. Goals:
- Book Library context with mocked data (200 books, 10 categories, 20 members)
- Thymeleaf dark theme UI (like lms-modulith-openapi)
- Simulation buttons for trace testing (10ms, 50ms, 500ms latency)
- Custom metrics with @Observable
- KPI status page for verification
```

### Prompt 2: [Implementation Phase]
```
[To be recorded during implementation]
```

### Prompt 3: [Testing & Documentation]
```
[To be recorded during implementation]
```

---

## MCP Tools Used (Template)

> **Instructions**: Track all MCP tools used during implementation.
> Update this table as tools are invoked.

| Tool | Purpose |
|------|---------|
| `listSpringBootVersions` | Verify Spring Boot 4.0.1 availability |
| `getBreakingChanges` | Get Spring Boot 4.0 breaking changes |
| `initializrGetDependencyCategories` | Browse available dependencies |
| `initializrSearchDependencies` | Find observability dependencies |
| `searchSpringDocs` | Search OpenTelemetry documentation |
| `getSpringBootLanguageRequirements` | Verify Java 25 compatibility |
| `searchFlavors` | Find observability patterns |
| `getFlavorByName` | Get detailed implementation guidelines |
| `getClassDoc` | Look up OpenTelemetry API classes |
| `searchJavadocs` | Search for tracer, span APIs |

---

## Flavors Used (Template)

> **Instructions**: Document which Spring MCP Server flavors provided guidance.
> Update as flavors are applied.

| Flavor | Purpose |
|--------|---------|
| `observability-spring-boot` | OpenTelemetry configuration patterns |
| `metrics-micrometer` | Custom metrics implementation |
| `testing-spring-boot` | Testing patterns with RestTestClient |
| [To be added] | [During implementation] |

---

## Development Notes (Template)

> **Instructions**: Record key decisions and learnings during implementation.

### Key Decisions

1. **In-Memory Data Store**: Chose ConcurrentHashMap for thread-safe mock data
2. **Tracer API**: Using OpenTelemetry Tracer for custom spans
3. **Logback Integration**: Required `InstallOpenTelemetryAppender` component
4. [To be added during implementation]

### Learnings

1. Spring Boot 4.0.1 requires `spring-boot-starter-opentelemetry` (new starter)
2. Logback appender needs manual installation after Spring context
3. [To be added during implementation]

### Challenges Solved

1. [To be added during implementation]

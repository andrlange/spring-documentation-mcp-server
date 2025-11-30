-- Spring MCP Server - Flavors Feature
-- Version: 1.3.0
-- Description: Company guidelines, architecture patterns, compliance rules, and agent configurations
-- Date: 2025-11-30

-- ============================================================
-- FLAVORS TABLE - Single table design for all flavor categories
-- ============================================================
CREATE TABLE flavors (
    id BIGSERIAL PRIMARY KEY,

    -- Identification
    unique_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,

    -- Classification (using varchar for JPA compatibility)
    category VARCHAR(50) NOT NULL,
    pattern_name VARCHAR(255),

    -- Content
    content TEXT NOT NULL,
    description TEXT,
    content_hash VARCHAR(64),

    -- Flexible metadata (tags, slugs, rules, use_cases)
    tags TEXT[] DEFAULT '{}',

    -- Category-specific metadata as JSONB
    metadata JSONB DEFAULT '{}',

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Full-text search vector
    search_vector TSVECTOR,

    -- Constraints
    CONSTRAINT uk_flavors_unique_name UNIQUE (unique_name),
    CONSTRAINT chk_flavors_name_length CHECK (LENGTH(unique_name) >= 3),
    CONSTRAINT chk_flavors_content_not_empty CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_flavors_category CHECK (category IN ('ARCHITECTURE', 'COMPLIANCE', 'AGENTS', 'INITIALIZATION', 'GENERAL'))
);

-- Indexes for efficient querying
CREATE INDEX idx_flavors_category ON flavors(category);
CREATE INDEX idx_flavors_active ON flavors(is_active) WHERE is_active = true;
CREATE INDEX idx_flavors_pattern ON flavors(pattern_name) WHERE pattern_name IS NOT NULL;
CREATE INDEX idx_flavors_tags ON flavors USING GIN(tags);
CREATE INDEX idx_flavors_metadata ON flavors USING GIN(metadata);
CREATE INDEX idx_flavors_search ON flavors USING GIN(search_vector);
CREATE INDEX idx_flavors_created ON flavors(created_at DESC);
CREATE INDEX idx_flavors_updated ON flavors(updated_at DESC);

-- Comments
COMMENT ON TABLE flavors IS 'Company guidelines, architecture patterns, compliance rules, and agent configurations';
COMMENT ON COLUMN flavors.unique_name IS 'Unique identifier for the flavor (URL-friendly)';
COMMENT ON COLUMN flavors.category IS 'Category: ARCHITECTURE, COMPLIANCE, AGENTS, INITIALIZATION, GENERAL';
COMMENT ON COLUMN flavors.tags IS 'Flexible tags for searching (slugs, rules, use cases)';
COMMENT ON COLUMN flavors.metadata IS 'Category-specific structured metadata as JSONB';
COMMENT ON COLUMN flavors.search_vector IS 'Full-text search vector for efficient querying';

-- Trigger function for search vector and updated_at
CREATE OR REPLACE FUNCTION flavors_update_trigger() RETURNS trigger AS $$
BEGIN
    -- Update search vector
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.unique_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.display_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.pattern_name, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.tags, ' '), '')), 'B');

    -- Update timestamp
    NEW.updated_at := CURRENT_TIMESTAMP;

    -- Generate content hash for change detection
    NEW.content_hash := encode(sha256(NEW.content::bytea), 'hex');

    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_flavors_update
    BEFORE INSERT OR UPDATE ON flavors
    FOR EACH ROW EXECUTE FUNCTION flavors_update_trigger();

-- ============================================================
-- SAMPLE DATA
-- ============================================================

-- Architecture Flavor: Hexagonal Architecture
INSERT INTO flavors (unique_name, display_name, category, pattern_name, content, description, tags, metadata, created_by) VALUES
(
    'hexagonal-spring-boot',
    'Hexagonal Architecture for Spring Boot',
    'ARCHITECTURE',
    'Hexagonal Architecture',
    E'# Hexagonal Architecture Guide

## Overview
This guide defines our standard hexagonal (ports & adapters) architecture for Spring Boot services. Also known as "Ports and Adapters" pattern, it keeps the domain logic isolated from external concerns.

## Package Structure
```
com.company.service/
├── domain/
│   ├── model/          # Domain entities and value objects
│   ├── event/          # Domain events
│   ├── port/           # Port interfaces (inbound and outbound)
│   └── service/        # Domain services
├── application/
│   ├── command/        # Command handlers (write operations)
│   ├── query/          # Query handlers (read operations)
│   └── service/        # Application services (orchestration)
├── infrastructure/
│   ├── persistence/    # JPA repositories and adapters
│   ├── messaging/      # Kafka/RabbitMQ adapters
│   └── external/       # External API clients
└── interfaces/
    ├── rest/           # REST controllers
    ├── graphql/        # GraphQL resolvers
    └── events/         # Event listeners
```

## Key Principles

### 1. Domain Independence
Domain layer has **NO external dependencies**. It contains only:
- Pure Java/Kotlin code
- Domain entities with business logic
- Port interfaces defining required capabilities

### 2. Dependency Rule
Dependencies **point inward** only:
```
interfaces → application → domain ← infrastructure
```

### 3. Port/Adapter Pattern
- **Ports**: Interfaces defined in domain layer
- **Adapters**: Implementations in infrastructure layer

## Code Examples

### Port Definition (Domain Layer)
```java
// Outbound port - what the domain needs
public interface OrderRepository {
    Order findById(OrderId id);
    void save(Order order);
    List<Order> findByCustomerId(CustomerId customerId);
}

// Inbound port - what the domain offers
public interface OrderService {
    OrderId createOrder(CreateOrderCommand command);
    void cancelOrder(OrderId orderId);
}
```

### Adapter Implementation (Infrastructure Layer)
```java
@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order findById(OrderId id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public void save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        jpaRepository.save(entity);
    }
}
```

### Application Service
```java
@Service
@RequiredArgsConstructor
@Transactional
public class OrderApplicationService implements OrderService {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public OrderId createOrder(CreateOrderCommand command) {
        Order order = Order.create(command);
        orderRepository.save(order);
        eventPublisher.publish(order.getDomainEvents());
        return order.getId();
    }
}
```

## Anti-Patterns to Avoid

1. **Direct Database Access from Controllers**
   - Always go through application/domain services

2. **Business Logic in Infrastructure**
   - Keep calculations and rules in domain

3. **Anemic Domain Model**
   - Put behavior in entities, not just data

4. **Framework Annotations in Domain**
   - No @Entity, @Repository in domain layer

## Testing Strategy

| Layer | Test Type | Approach |
|-------|-----------|----------|
| Domain | Unit | Pure unit tests, no mocks needed |
| Application | Integration | Mock ports, test orchestration |
| Infrastructure | Integration | Testcontainers, real adapters |
| Interfaces | E2E | Full stack tests |

## When to Use
- Microservices with complex business logic
- Services requiring multiple external integrations
- Long-lived applications needing maintainability
- Teams practicing DDD
',
    'Standard hexagonal architecture pattern for all Spring Boot microservices',
    ARRAY['spring-boot', 'jpa', 'kafka', 'ddd', 'ports-adapters', 'clean-architecture'],
    '{"slugs": ["spring-boot", "spring-data-jpa", "spring-kafka"], "layers": ["domain", "application", "infrastructure", "interfaces"], "architectureType": "hexagonal"}',
    'system'
),

-- Compliance Flavor: GDPR User Data
(
    'gdpr-user-data-compliance',
    'GDPR User Data Handling',
    'COMPLIANCE',
    'Data Protection',
    E'# GDPR User Data Compliance Guide

## Overview
This guide defines mandatory requirements for handling personal user data under GDPR (General Data Protection Regulation).

## Required Annotations

All endpoints handling PII (Personally Identifiable Information) must use our custom annotations:

```java
@RestController
@RequestMapping("/api/v1/users")
@Audited(entity = "USER")
public class UserController {

    @PostMapping
    @Audited(action = "CREATE")
    @PIIProtection(fields = {"email", "phone", "address"})
    @RateLimited(requests = 10, window = "1m")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest dto,
            @RequestHeader("X-Correlation-ID") String correlationId) {
        return ResponseEntity.ok(userService.create(dto, correlationId));
    }

    @GetMapping("/{id}")
    @Audited(action = "READ")
    @DataMinimization(exclude = {"password", "internalNotes"})
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @DeleteMapping("/{id}")
    @Audited(action = "DELETE")
    @RightToErasure
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Data Retention Rules

### Entity Configuration
```java
@Entity
@Table(name = "users")
@Deletable(retentionDays = 30, cascadeDelete = true)
public class User {

    @PIIField(classification = "DIRECT_IDENTIFIER")
    @Column(nullable = false)
    private String email;

    @PIIField(classification = "CONTACT")
    private String phone;

    @PIIField(classification = "LOCATION")
    @Embedded
    private Address address;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime deletionRequestedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime scheduledDeletionAt;
}
```

### Soft Delete with Hard Delete Scheduler
- User requests deletion → `deletionRequestedAt` is set
- 30-day grace period for data recovery
- Scheduled job permanently deletes after grace period

## Consent Management

### Required Consent Fields
```java
@Embeddable
public class UserConsent {

    @Column(nullable = false)
    private boolean marketingConsent;

    @Column(nullable = false)
    private boolean analyticsConsent;

    @Column(nullable = false)
    private LocalDateTime consentTimestamp;

    @Column(nullable = false)
    private String consentVersion;

    @Column
    private String consentSource; // "web", "mobile", "api"
}
```

### Consent Withdrawal
```java
@PostMapping("/{id}/withdraw-consent")
@Audited(action = "CONSENT_WITHDRAWAL")
public ResponseEntity<Void> withdrawConsent(
        @PathVariable Long id,
        @RequestBody WithdrawConsentRequest request) {
    userService.withdrawConsent(id, request.getConsentTypes());
    return ResponseEntity.ok().build();
}
```

## Right to Access (Data Export)

### Implementation
```java
@GetMapping("/{id}/data-export")
@Audited(action = "DATA_EXPORT")
@RateLimited(requests = 1, window = "24h")
public ResponseEntity<Resource> exportUserData(@PathVariable Long id) {
    UserDataExport export = userService.exportAllData(id);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"user-data-export.json\"")
        .body(new ByteArrayResource(export.toJson().getBytes()));
}
```

### Export Must Include
- All personal data stored
- Processing purposes
- Data categories
- Recipients of data
- Retention periods
- Source of data

## Audit Trail Requirements

All PII access must be logged with:
- User ID performing action
- Target entity ID
- Action type (CREATE, READ, UPDATE, DELETE)
- Timestamp
- IP address
- Correlation ID

## Checklist for New Endpoints

- [ ] @Audited annotation applied
- [ ] @PIIProtection for PII fields
- [ ] @RateLimited to prevent abuse
- [ ] Input validation (@Valid)
- [ ] Correlation ID tracking
- [ ] Consent verification where required
- [ ] Data minimization applied
',
    'Mandatory GDPR compliance requirements for user data handling',
    ARRAY['gdpr', 'pii', 'data-protection', 'compliance', 'audit', 'privacy'],
    '{"ruleNames": ["GDPR-Art5", "GDPR-Art6", "GDPR-Art17", "GDPR-Art20"], "complianceFrameworks": ["GDPR"], "auditRequired": true}',
    'system'
),

-- Agent Flavor: Claude Code Backend
(
    'claude-code-backend-agent',
    'Claude Code Backend Development Setup',
    'AGENTS',
    'AI Assistant Configuration',
    E'# Claude Code Backend Development Configuration

## Overview
This configuration sets up Claude Code for optimal backend Spring Boot development in our organization.

## Claude Code Settings

Create `.claude/settings.json` in your project root:

```json
{
  "mcpServers": {
    "spring": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/spring/sse"
    }
  },
  "permissions": {
    "allowedTools": [
      "mcp__spring__*",
      "Bash",
      "Read",
      "Write",
      "Edit",
      "Glob",
      "Grep"
    ],
    "deniedTools": []
  },
  "context": {
    "includePatterns": [
      "src/**/*.java",
      "src/**/*.kt",
      "*.gradle",
      "*.yml",
      "*.yaml"
    ],
    "excludePatterns": [
      "**/build/**",
      "**/target/**",
      "**/.gradle/**"
    ]
  }
}
```

## Custom Slash Commands

### /review - Code Review
Create `.claude/commands/review.md`:
```markdown
Review this code against our standards:

1. **Architecture Compliance**
   - Check hexagonal architecture adherence
   - Verify dependency direction (inward only)
   - Ensure domain isolation

2. **GDPR Compliance**
   - Verify @Audited on data endpoints
   - Check @PIIProtection on PII fields
   - Ensure consent handling where needed

3. **Code Quality**
   - Exception handling completeness
   - Input validation presence
   - Logging appropriateness

4. **Testing**
   - Unit test coverage
   - Integration test presence
   - Edge case handling

Provide specific feedback with code examples.
```

### /test-coverage - Generate Tests
Create `.claude/commands/test-coverage.md`:
```markdown
Generate comprehensive tests for the current code:

1. **Unit Tests**
   - Domain service tests
   - Value object tests
   - Business logic validation

2. **Integration Tests**
   - Repository tests with Testcontainers
   - REST controller tests with MockMvc
   - Security tests

3. **Coverage Requirements**
   - Minimum 80% line coverage
   - All public methods tested
   - Edge cases covered

Use JUnit 5 and AssertJ assertions.
```

### /create-endpoint - New Endpoint Scaffold
Create `.claude/commands/create-endpoint.md`:
```markdown
Create a new REST endpoint following our standards:

1. Check architecture flavors for structure
2. Apply GDPR compliance requirements
3. Include proper validation
4. Add comprehensive error handling
5. Create corresponding tests

Use hexagonal architecture with proper layering.
```

## Recommended Workflow

### Before Generating Code
```
1. searchFlavors("architecture") - Get architecture guidelines
2. getComplianceRules(["GDPR"]) - Get compliance requirements
3. getArchitecturePatterns(["spring-boot"]) - Get specific patterns
```

### During Development
```
1. Follow hexagonal layer separation
2. Use port/adapter pattern
3. Apply required annotations
```

### After Coding
```
1. Run /review command
2. Run /test-coverage command
3. Fix identified issues
4. Verify build passes
```

## MCP Tools Usage

### Architecture Guidance
```javascript
// Get architecture patterns for your tech stack
await mcp.searchFlavors({
  query: "hexagonal",
  category: "ARCHITECTURE",
  tags: ["spring-boot", "jpa"]
});
```

### Compliance Verification
```javascript
// Get compliance rules before implementing user features
await mcp.getComplianceRules({
  rules: ["GDPR"]
});
```

## Integration with IDE

### IntelliJ IDEA Setup
1. Install Claude Code plugin
2. Configure MCP server connection
3. Set project-specific settings

### VS Code Setup
1. Install Claude extension
2. Configure `.claude/settings.json`
3. Add recommended extensions
',
    'Optimal Claude Code configuration for backend Spring Boot development',
    ARRAY['claude-code', 'ai-assistant', 'backend', 'spring-boot', 'development'],
    '{"useCases": ["backend-development", "api-development", "testing"], "agentType": "claude-code", "requiredTools": ["mcp__spring"]}',
    'system'
),

-- Initialization Flavor: Microservice Project
(
    'spring-boot-microservice-init',
    'Spring Boot Microservice Initialization',
    'INITIALIZATION',
    'Project Scaffolding',
    E'# Spring Boot Microservice Initialization

## Overview
Standard project initialization template for new Spring Boot microservices.

## Project Structure
```
service-name/
├── .claude/
│   ├── settings.json
│   └── commands/
│       ├── review.md
│       └── test-coverage.md
├── src/
│   ├── main/
│   │   ├── java/com/company/service/
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   ├── infrastructure/
│   │   │   └── interfaces/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── db/migration/
│   └── test/
├── build.gradle
├── docker-compose.yml
├── Dockerfile
└── README.md
```

## build.gradle Template
```groovy
plugins {
    id ''org.springframework.boot'' version ''3.5.8''
    id ''io.spring.dependency-management'' version ''1.1.6''
    id ''java''
}

group = ''com.company''
version = ''0.0.1-SNAPSHOT''

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation ''org.springframework.boot:spring-boot-starter-web''
    implementation ''org.springframework.boot:spring-boot-starter-data-jpa''
    implementation ''org.springframework.boot:spring-boot-starter-validation''
    implementation ''org.springframework.boot:spring-boot-starter-actuator''

    // Database
    runtimeOnly ''org.postgresql:postgresql''
    implementation ''org.flywaydb:flyway-core''
    implementation ''org.flywaydb:flyway-database-postgresql''

    // Utilities
    compileOnly ''org.projectlombok:lombok''
    annotationProcessor ''org.projectlombok:lombok''

    // Testing
    testImplementation ''org.springframework.boot:spring-boot-starter-test''
    testImplementation ''org.testcontainers:postgresql''
    testImplementation ''org.testcontainers:junit-jupiter''
}
```

## application.yml Template
```yaml
spring:
  application:
    name: ${SERVICE_NAME}

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## Docker Configuration

### Dockerfile
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml
```yaml
version: ''3.8''
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=postgres
      - DB_NAME=service_db
      - DB_USER=postgres
      - DB_PASSWORD=postgres
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:18-alpine
    environment:
      POSTGRES_DB: service_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5
```

## Claude Code Settings
```json
{
  "mcpServers": {
    "spring": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/spring/sse"
    }
  }
}
```

## Checklist
- [ ] Update SERVICE_NAME in application.yml
- [ ] Configure database credentials
- [ ] Set up CI/CD pipeline
- [ ] Configure monitoring/logging
- [ ] Add API documentation
- [ ] Review security settings
',
    'Standard initialization template for new Spring Boot microservices',
    ARRAY['spring-boot', 'microservice', 'initialization', 'docker', 'postgresql'],
    '{"useCases": ["microservice", "api-service", "backend"], "scaffoldingType": "spring-boot", "configFormat": "yaml"}',
    'system'
),

-- General Flavor: Java Naming Conventions
(
    'java-naming-conventions',
    'Java Naming Conventions',
    'GENERAL',
    'Coding Standards',
    E'# Java Naming Conventions

## Overview
Standard naming conventions for Java code in our organization.

## Class Naming

### General Rules
- PascalCase for all class names
- Nouns or noun phrases
- Avoid abbreviations

### Specific Patterns

| Type | Pattern | Example |
|------|---------|---------|
| Entity | `{Name}` | `Order`, `Customer` |
| Repository | `{Entity}Repository` | `OrderRepository` |
| Service Interface | `{Name}Service` | `OrderService` |
| Service Impl | `{Name}ServiceImpl` | `OrderServiceImpl` |
| Controller | `{Entity}Controller` | `OrderController` |
| DTO | `{Action}{Entity}Request/Response` | `CreateOrderRequest` |
| Exception | `{Name}Exception` | `OrderNotFoundException` |
| Mapper | `{Entity}Mapper` | `OrderMapper` |
| Config | `{Feature}Config` | `SecurityConfig` |

## Method Naming

### Conventions
- camelCase
- Start with verb
- Be descriptive

### Patterns

| Action | Pattern | Example |
|--------|---------|---------|
| Create | `create{Entity}` | `createOrder()` |
| Read | `find{Entity}By{Field}` | `findOrderById()` |
| Update | `update{Entity}` | `updateOrder()` |
| Delete | `delete{Entity}` | `deleteOrder()` |
| Boolean | `is{Condition}` | `isValid()` |
| Has | `has{Property}` | `hasPermission()` |
| Validate | `validate{What}` | `validateInput()` |
| Convert | `to{Target}` | `toDto()` |

## Variable Naming

### Local Variables
```java
// Good
Order order = orderRepository.findById(orderId);
List<OrderItem> items = order.getItems();
BigDecimal totalAmount = calculateTotal(items);

// Bad
Order o = orderRepository.findById(id);
List<OrderItem> list = o.getItems();
BigDecimal amt = calc(list);
```

### Constants
```java
// Good
public static final int MAX_RETRY_ATTEMPTS = 3;
public static final String DEFAULT_CURRENCY = "USD";

// Bad
public static final int MAX = 3;
public static final String curr = "USD";
```

## Package Naming

### Structure
```
com.company.service.{layer}.{feature}
```

### Examples
```
com.company.orderservice.domain.model
com.company.orderservice.domain.port
com.company.orderservice.application.service
com.company.orderservice.infrastructure.persistence
com.company.orderservice.interfaces.rest
```

## Test Naming

### Class Names
```java
class OrderServiceTest { }           // Unit test
class OrderServiceIntegrationTest { } // Integration test
class OrderControllerE2ETest { }      // End-to-end test
```

### Method Names
```java
@Test
void shouldCreateOrder_whenValidInput() { }

@Test
void shouldThrowException_whenOrderNotFound() { }

@Test
void shouldReturnEmpty_whenNoOrdersExist() { }
```

## Abbreviations to Avoid

| Avoid | Use Instead |
|-------|-------------|
| `msg` | `message` |
| `btn` | `button` |
| `cfg` | `config` |
| `ctx` | `context` |
| `req` | `request` |
| `res` | `response` |
| `err` | `error` |
| `num` | `number` or `count` |
',
    'Standard Java naming conventions for consistent codebase',
    ARRAY['java', 'naming', 'conventions', 'standards', 'best-practices'],
    '{"useCases": ["java", "kotlin", "coding-standards"], "applicableTo": ["java", "kotlin"]}',
    'system'
);

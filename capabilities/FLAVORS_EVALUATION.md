# Flavors Feature - Company Guidelines & AI Context Storage

> **Analysis Date**: 2025-11-30
> **Purpose**: Evaluate the feasibility and value of storing company-specific guidelines, architecture patterns, compliance rules, and agent configurations as markdown documents to improve AI-assisted development consistency

---

## Executive Summary

The "Flavors" feature could provide **50-70% improvement** in AI-assisted development consistency by giving LLMs immediate access to company-specific coding standards, architecture patterns, and project initialization rules. This addresses a fundamental limitation: LLMs operate without knowledge of organizational context, leading to code that compiles but violates internal standards.

**Key Finding**: Organizations spend significant time correcting AI-generated code that doesn't follow internal conventions. By exposing company guidelines through MCP tools, LLMs can generate code that aligns with organizational standards from the first iteration.

**Estimated Impact Areas**:

| Area | Improvement Potential | Example |
|------|----------------------|---------|
| Architecture consistency | High (50-70%) | Enforcing hexagonal architecture patterns |
| Compliance adherence | Very High (60-80%) | Following security/audit requirements |
| Agent/Subagent efficiency | High (40-60%) | Pre-configured workflows and tooling |
| Project initialization | Very High (70-85%) | Consistent project scaffolding |
| Code style consistency | High (45-65%) | Naming conventions, file organization |

---

## 1. Current Problem Analysis

### Issues Encountered During AI-Assisted Code Generation

When LLMs generate code for enterprise projects, they often:

| Issue | Type | Impact | Frequency |
|-------|------|--------|-----------|
| Ignore company architecture patterns | Structural | Major refactoring needed | Very High |
| Miss compliance requirements | Security/Audit | Critical in regulated industries | High |
| Use generic project structures | Organization | Inconsistent codebase | High |
| Apply default configurations | Setup | Wrong tooling/dependencies | Medium |
| Don't know about internal libraries | Dependencies | Reinvent existing solutions | High |
| Miss code review conventions | Quality | Extended review cycles | Medium |

### Root Cause

LLMs cannot know about:
- Company-specific architecture decisions (why microservices vs modular monolith)
- Internal security compliance rules (GDPR, SOC2, PCI-DSS implementations)
- Preferred libraries over alternatives (internal vs external)
- Naming conventions beyond language standards
- Project scaffolding requirements
- CI/CD pipeline expectations
- Internal documentation standards
- Subagent/AI assistant configurations

### Real-World Examples

#### Example 1: Architecture Violation

```java
// LLM generates (standard layered architecture)
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        // Direct entity manipulation
        return orderRepository.save(order);
    }
}

// Company standard (hexagonal architecture with domain events)
@Service
public class OrderApplicationService {
    private final OrderCommandHandler commandHandler;
    private final DomainEventPublisher eventPublisher;

    public OrderId createOrder(CreateOrderCommand command) {
        Order order = commandHandler.handle(command);
        eventPublisher.publish(order.getDomainEvents());
        return order.getId();
    }
}
```

#### Example 2: Missing Compliance Requirements

```java
// LLM generates (functional but non-compliant)
@RestController
public class UserController {
    @PostMapping("/users")
    public User createUser(@RequestBody UserDto dto) {
        return userService.create(dto);
    }
}

// Company compliance requires (audit logging, PII handling)
@RestController
@Audited(entity = "USER", action = "CREATE")
public class UserController {
    @PostMapping("/users")
    @PIIProtection(fields = {"email", "phone"})
    @RateLimited(requests = 10, window = "1m")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest dto,
            @RequestHeader("X-Correlation-ID") String correlationId) {
        return userService.create(dto, correlationId);
    }
}
```

#### Example 3: Agent Misconfiguration

```markdown
# Without company agent flavor
User: "Set up Claude Code for this project"
LLM: Creates generic .claude/settings.json without:
- Custom slash commands for code review
- Pre-commit hooks configuration
- Internal API endpoints for documentation
- Team-specific MCP server connections

# With company agent flavor
User: "Set up Claude Code for this project"
LLM: Reads agent flavor, creates comprehensive setup including:
- Custom /review, /deploy, /test-coverage commands
- Pre-configured hooks for style checking
- Connection to internal Spring MCP Server
- Team-specific prompt templates
```

---

## 2. Flavors Category Analysis

### 2.1 Category Definitions

| Category | Purpose | Key Use Cases |
|----------|---------|---------------|
| **Architecture** | Define structural patterns and design decisions | Hexagonal, DDD, CQRS, Event Sourcing |
| **Compliance** | Security, audit, and regulatory requirements | GDPR, SOC2, PCI-DSS, HIPAA |
| **Agents/Subagent** | AI assistant configurations and workflows | Claude Code setup, custom commands |
| **Initialization** | Project scaffolding and bootstrap configurations | Folder structures, dependencies, CI/CD |
| **General Flavor** | Broad guidelines not fitting other categories | Naming conventions, best practices |

### 2.2 Category Metadata Requirements

#### Architecture Flavor

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| unique_name | VARCHAR(255) | Yes | Identifier (e.g., "hexagonal-spring-boot") |
| slugs | TEXT[] | No | Related technologies (e.g., ["spring-boot", "jpa", "kafka"]) |
| pattern_name | VARCHAR(255) | No | Architecture pattern (e.g., "Hexagonal Architecture") |
| is_active | BOOLEAN | Yes | Whether searchable via MCP |
| created_at | TIMESTAMP | Auto | Creation timestamp |
| updated_at | TIMESTAMP | Auto | Last modification |

#### Compliance Flavor

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| unique_name | VARCHAR(255) | Yes | Identifier (e.g., "gdpr-user-data") |
| rule_names | TEXT[] | No | Compliance rules (e.g., ["GDPR-Art17", "SOC2-CC6.1"]) |
| pattern_name | VARCHAR(255) | No | Compliance pattern name |
| is_active | BOOLEAN | Yes | Whether searchable via MCP |
| created_at | TIMESTAMP | Auto | Creation timestamp |
| updated_at | TIMESTAMP | Auto | Last modification |

#### Agents/Subagent Flavor

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| unique_name | VARCHAR(255) | Yes | Identifier (e.g., "ui-development-agent") |
| use_cases | TEXT[] | No | Use cases (e.g., ["ui-development", "testing"]) |
| pattern_name | VARCHAR(255) | No | Agent pattern name |
| is_active | BOOLEAN | Yes | Whether searchable via MCP |
| created_at | TIMESTAMP | Auto | Creation timestamp |
| updated_at | TIMESTAMP | Auto | Last modification |

#### Initialization Flavor

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| unique_name | VARCHAR(255) | Yes | Identifier (e.g., "spring-boot-microservice-init") |
| use_cases | TEXT[] | No | Use cases (e.g., ["microservice", "api-gateway"]) |
| pattern_name | VARCHAR(255) | No | Initialization pattern name |
| is_active | BOOLEAN | Yes | Whether searchable via MCP |
| created_at | TIMESTAMP | Auto | Creation timestamp |
| updated_at | TIMESTAMP | Auto | Last modification |
| **Special**: May contain JSON blocks for tool configuration (e.g., Claude Code settings.json) |

#### General Flavor

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| unique_name | VARCHAR(255) | Yes | Identifier (e.g., "java-naming-conventions") |
| use_cases | TEXT[] | No | Use cases (e.g., ["queueing", "modularization"]) |
| pattern_name | VARCHAR(255) | No | Pattern name |
| is_active | BOOLEAN | Yes | Whether searchable via MCP |
| created_at | TIMESTAMP | Auto | Creation timestamp |
| updated_at | TIMESTAMP | Auto | Last modification |

---

## 3. Database Design Analysis

### 3.1 Single Table Approach (Recommended)

**Feasibility Assessment: RECOMMENDED**

A single table with a discriminator column is practical because:

1. **Metadata fields overlap significantly** - All categories share: name, active status, timestamps, markdown content
2. **Flexible array fields** - PostgreSQL TEXT[] handles slugs, rule_names, use_cases uniformly
3. **Simpler queries** - Cross-category search is straightforward
4. **Easier maintenance** - One repository, one service, one set of DTOs
5. **Future extensibility** - New categories require no schema migration

### 3.2 Proposed Database Schema

```sql
-- Flavors table (single table design)
CREATE TABLE flavors (
    id BIGSERIAL PRIMARY KEY,

    -- Common fields
    unique_name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL, -- ARCHITECTURE, COMPLIANCE, AGENTS, INITIALIZATION, GENERAL
    pattern_name VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,

    -- Markdown content
    content TEXT NOT NULL,
    content_hash VARCHAR(64), -- For change detection

    -- Flexible metadata (stored as array - works for slugs, rules, use_cases)
    tags TEXT[] DEFAULT '{}',

    -- JSON metadata for category-specific data
    metadata JSONB DEFAULT '{}',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Full-text search
    search_vector TSVECTOR,

    -- Constraints
    CONSTRAINT chk_category CHECK (category IN ('ARCHITECTURE', 'COMPLIANCE', 'AGENTS', 'INITIALIZATION', 'GENERAL')),
    CONSTRAINT chk_name_length CHECK (LENGTH(unique_name) >= 3)
);

-- Indexes for efficient querying
CREATE INDEX idx_flavors_category ON flavors(category);
CREATE INDEX idx_flavors_active ON flavors(is_active) WHERE is_active = true;
CREATE INDEX idx_flavors_tags ON flavors USING GIN(tags);
CREATE INDEX idx_flavors_search ON flavors USING GIN(search_vector);
CREATE INDEX idx_flavors_metadata ON flavors USING GIN(metadata);
CREATE INDEX idx_flavors_name_pattern ON flavors(unique_name, pattern_name);

-- Trigger for search vector update
CREATE OR REPLACE FUNCTION flavors_search_trigger() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.unique_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.display_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.pattern_name, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'C');
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER flavors_search_update
    BEFORE INSERT OR UPDATE ON flavors
    FOR EACH ROW EXECUTE FUNCTION flavors_search_trigger();
```

### 3.3 JSONB Metadata Examples by Category

```json
// Architecture
{
    "slugs": ["spring-boot", "jpa", "kafka"],
    "architectureType": "hexagonal",
    "layers": ["domain", "application", "infrastructure", "interfaces"]
}

// Compliance
{
    "ruleNames": ["GDPR-Art17", "SOC2-CC6.1"],
    "complianceFrameworks": ["GDPR", "SOC2"],
    "auditRequirements": true
}

// Agents/Subagent
{
    "useCases": ["ui-development", "testing", "code-review"],
    "agentType": "claude-code",
    "requiredTools": ["mcp__spring", "mcp__chrome"]
}

// Initialization
{
    "useCases": ["microservice", "api-gateway"],
    "scaffoldingType": "spring-boot",
    "configFormat": "yaml",
    "claudeSettings": { /* embedded JSON config */ }
}

// General
{
    "useCases": ["queueing", "best-practices"],
    "applicableTo": ["java", "kotlin"]
}
```

---

## 4. Markdown Editor Analysis

### 4.1 WYSIWYG Markdown Editor Options

| Option | Complexity | Features | License | Effort |
|--------|------------|----------|---------|--------|
| **SimpleMDE** | Low | Basic markdown, preview | MIT | 2-3 days |
| **EasyMDE** | Low-Medium | SimpleMDE fork, better maintained | MIT | 2-3 days |
| **Toast UI Editor** | Medium | Rich features, dual-mode | MIT | 3-5 days |
| **Editor.js** | Medium-High | Block-based, extensible | Apache 2.0 | 5-7 days |
| **Milkdown** | High | Plugin system, WYSIWYG | MIT | 7-10 days |
| **Custom (CodeMirror)** | High | Full control | MIT | 10-14 days |

### 4.2 Recommended: EasyMDE (Fork of SimpleMDE)

**Rationale**:
- Active maintenance (SimpleMDE is abandoned)
- Lightweight (~200KB minified)
- Side-by-side preview
- Toolbar customization
- No heavy framework dependencies
- Works with vanilla JS and Thymeleaf

**Implementation Complexity**: LOW

```html
<!-- Basic Integration -->
<link rel="stylesheet" href="https://unpkg.com/easymde/dist/easymde.min.css">
<script src="https://unpkg.com/easymde/dist/easymde.min.js"></script>

<textarea id="flavor-content" name="content"></textarea>

<script>
const editor = new EasyMDE({
    element: document.getElementById('flavor-content'),
    spellChecker: false,
    autosave: {
        enabled: true,
        uniqueId: 'flavor-editor',
        delay: 1000
    },
    toolbar: [
        'bold', 'italic', 'heading', '|',
        'code', 'quote', 'unordered-list', 'ordered-list', '|',
        'link', 'image', 'table', '|',
        'preview', 'side-by-side', 'fullscreen', '|',
        'guide'
    ],
    sideBySideFullscreen: false,
    previewClass: ['editor-preview', 'markdown-body']
});
</script>
```

### 4.3 Advanced Features (Phase 2)

| Feature | Complexity | Value | Priority |
|---------|------------|-------|----------|
| Syntax highlighting for code blocks | Low | High | P1 |
| JSON validation for config blocks | Medium | High | P1 |
| Markdown linting | Low | Medium | P2 |
| Image upload/paste | Medium | Medium | P2 |
| Template insertion | Low | High | P1 |
| Collaborative editing | High | Low | P3 |

---

## 5. Proposed MCP Tools

### 5.1 New Tools for Flavors

```java
@Tool(name = "searchFlavors",
      description = "Search company flavors (guidelines, patterns, compliance rules)")
public List<FlavorSummary> searchFlavors(
    @ToolParam(description = "Search query for flavor content") String query,
    @ToolParam(description = "Filter by category: ARCHITECTURE, COMPLIANCE, AGENTS, INITIALIZATION, GENERAL")
        String category,
    @ToolParam(description = "Filter by tags/slugs") List<String> tags,
    @ToolParam(description = "Maximum results (default: 10)") Integer limit
);

@Tool(name = "getFlavorByName",
      description = "Get complete flavor content by unique name")
public FlavorDetail getFlavorByName(
    @ToolParam(description = "Unique name of the flavor") String uniqueName
);

@Tool(name = "getFlavorsByCategory",
      description = "List all active flavors in a category")
public List<FlavorSummary> getFlavorsByCategory(
    @ToolParam(description = "Category: ARCHITECTURE, COMPLIANCE, AGENTS, INITIALIZATION, GENERAL")
        String category
);

@Tool(name = "getArchitecturePatterns",
      description = "Get architecture flavors relevant to specific technologies")
public List<FlavorDetail> getArchitecturePatterns(
    @ToolParam(description = "Technology slugs (e.g., spring-boot, kafka, jpa)") List<String> slugs
);

@Tool(name = "getComplianceRules",
      description = "Get compliance flavors by rule names or frameworks")
public List<FlavorDetail> getComplianceRules(
    @ToolParam(description = "Rule names or framework identifiers") List<String> rules
);

@Tool(name = "getAgentConfiguration",
      description = "Get agent/subagent configuration for a use case")
public FlavorDetail getAgentConfiguration(
    @ToolParam(description = "Use case identifier (e.g., ui-development, backend-development)")
        String useCase
);

@Tool(name = "getProjectInitialization",
      description = "Get project initialization template for a use case")
public FlavorDetail getProjectInitialization(
    @ToolParam(description = "Use case identifier (e.g., microservice, api-gateway)")
        String useCase
);

@Tool(name = "listFlavorCategories",
      description = "List all available flavor categories with counts")
public List<CategorySummary> listFlavorCategories();
```

### 5.2 Tool Usage Examples

**Example 1: Architecture Guidance**
```
User: "Create a new order service following our architecture standards"

Claude Code calls: getArchitecturePatterns(["spring-boot", "jpa", "kafka"])
→ Returns: Hexagonal Architecture flavor with:
  - Package structure requirements
  - Domain event patterns
  - Port/adapter conventions
  - Code examples

Claude generates code following company patterns
```

**Example 2: Compliance Check**
```
User: "Add user registration endpoint"

Claude Code calls: getComplianceRules(["GDPR", "user-data"])
→ Returns: GDPR User Data flavor with:
  - Required annotations (@PIIProtection, @Audited)
  - Data retention rules
  - Consent handling requirements
  - Right to deletion implementation

Claude generates compliant endpoint
```

**Example 3: Project Setup**
```
User: "Initialize Claude Code for this Spring Boot project"

Claude Code calls: getAgentConfiguration("backend-development")
→ Returns: Backend Development Agent flavor with:
  - Claude Code settings.json template
  - Custom slash commands
  - MCP server configurations
  - Hook configurations

Claude generates complete .claude/ setup
```

---

## 6. UI Design Specification

### 6.1 Page Structure (Aligned with Languages Page)

```
/flavors
├── Page Header
│   ├── Title: "Flavors" with icon (bi-palette)
│   ├── Action Buttons: [+ New Flavor] [Import]
│   └── Last modified indicator
├── Statistics Cards (6 cards)
│   ├── Total Flavors
│   ├── Architecture count
│   ├── Compliance count
│   ├── Agents count
│   ├── Initialization count
│   └── General count
├── Filter Section
│   ├── Category dropdown
│   ├── Status (Active/Inactive)
│   ├── Tags multi-select
│   └── Search input
├── Flavors Table
│   ├── Name (expandable for preview)
│   ├── Category (badge)
│   ├── Pattern Name
│   ├── Tags
│   ├── Status (active/inactive toggle)
│   ├── Last Modified
│   └── Actions (Edit, Delete)
└── Pagination (if needed)

/flavors/new | /flavors/{id}/edit
├── Basic Information Form
│   ├── Unique Name (required)
│   ├── Display Name
│   ├── Category (dropdown, required)
│   ├── Pattern Name
│   ├── Description
│   ├── Tags (multi-input)
│   └── Is Active (toggle)
├── Markdown Editor (EasyMDE)
│   ├── Full-width editor
│   ├── Preview pane
│   └── Toolbar
└── Action Buttons
    ├── Save Draft
    ├── Save & Activate
    └── Cancel
```

### 6.2 Dark Mode Styling (Spring.io Aligned)

```css
/* Flavor-specific colors */
:root {
    --flavor-architecture: #818cf8;  /* Purple */
    --flavor-compliance: #f472b6;    /* Pink */
    --flavor-agents: #22d3ee;        /* Cyan */
    --flavor-initialization: #34d399; /* Green */
    --flavor-general: #fbbf24;       /* Amber */
}

/* Category badges */
.badge-architecture { background-color: var(--flavor-architecture); }
.badge-compliance { background-color: var(--flavor-compliance); }
.badge-agents { background-color: var(--flavor-agents); }
.badge-initialization { background-color: var(--flavor-initialization); }
.badge-general { background-color: var(--flavor-general); }
```

### 6.3 Modal Dialogs

**Delete Confirmation Modal**:
- Dark theme with warning colors
- Flavor name displayed prominently
- Impact warning (e.g., "This flavor is referenced in 3 projects")
- Two-step confirmation for active flavors

**Import Modal**:
- File drop zone for markdown files
- Metadata extraction preview
- Category auto-detection
- Conflict resolution options

---

## 7. Efficiency Gains Analysis

### 7.1 Without Flavors (Current State)

| Task | Time/Iterations | Issue |
|------|-----------------|-------|
| Explain architecture requirements | 5-15 min | Verbal/written explanation each time |
| Correct compliance violations | 2-5 iterations | Reactive discovery |
| Set up AI tooling | 15-30 min | Manual configuration |
| Apply coding standards | 1-3 iterations | Inconsistent enforcement |
| **Total per developer/project** | **30-60 min setup + ongoing corrections** | Compounds across team |

### 7.2 With Flavors

| Task | Time/Iterations | Improvement |
|------|-----------------|-------------|
| Apply architecture requirements | Instant lookup | Pre-generation check |
| Follow compliance | First attempt correct | MCP-guided |
| Set up AI tooling | < 5 min | Template-based |
| Apply coding standards | First attempt | Consistent |
| **Total per developer/project** | **< 10 min** | Dramatic reduction |

### 7.3 Projected Improvement by Scenario

| Scenario | Current Issues | With Flavors | Improvement |
|----------|----------------|--------------|-------------|
| New developer onboarding | Learns standards over weeks | Instant access to standards | **60-75%** |
| New project setup | Manual template copying | Automated initialization | **70-85%** |
| Cross-team collaboration | Different standards | Unified via MCP | **50-65%** |
| Compliance audit | Manual verification | Built-in compliance | **65-80%** |
| AI code generation | Generic patterns | Company-specific | **55-70%** |

**Conservative overall estimate: 50-70% improvement** in consistency and setup time.

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Week 1)

- [ ] Create database schema (flavors table)
- [ ] Implement entity, repository, service classes
- [ ] Add Flyway migration script (V6__flavors.sql)
- [ ] Create basic CRUD controller

### Phase 2: MCP Tools (Week 1-2)

- [ ] Implement `searchFlavors` tool
- [ ] Implement `getFlavorByName` tool
- [ ] Implement `getFlavorsByCategory` tool
- [ ] Add MCP tool registration
- [ ] Write integration tests

### Phase 3: UI - List & View (Week 2)

- [ ] Create flavors list page (similar to languages)
- [ ] Add sidebar navigation entry
- [ ] Implement filtering and search
- [ ] Add statistics cards
- [ ] Create delete confirmation modal

### Phase 4: UI - Editor (Week 2-3)

- [ ] Integrate EasyMDE editor
- [ ] Create new/edit flavor form
- [ ] Implement category-specific metadata forms
- [ ] Add markdown preview
- [ ] Implement file import

### Phase 5: Advanced MCP Tools (Week 3)

- [ ] Implement `getArchitecturePatterns`
- [ ] Implement `getComplianceRules`
- [ ] Implement `getAgentConfiguration`
- [ ] Implement `getProjectInitialization`
- [ ] Add feature flag support

### Phase 6: Testing & Polish (Week 4)

- [ ] Full test coverage
- [ ] UI polish and responsive design
- [ ] Documentation
- [ ] Sample flavors creation
- [ ] Performance optimization

---

## 9. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Markdown editor complexity | Low | Medium | Use proven EasyMDE library |
| Content organization | Medium | Medium | Clear category definitions |
| Adoption by teams | Medium | High | Pre-populate with useful examples |
| Content maintenance | Medium | Medium | Track last used, suggest updates |
| Search performance | Low | Medium | Full-text search indexing |
| Over-engineering | Medium | Low | Start simple, iterate |

---

## 10. Success Metrics

### 10.1 Quantitative Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Flavor coverage | 20+ initial flavors | Count per category |
| MCP tool usage | 50+ calls/day | Tool call analytics |
| Time to first compliant code | < 5 min | User surveys |
| Code review rejections | -40% | Git metrics |

### 10.2 Qualitative Metrics

- Developer satisfaction with AI consistency
- Reduction in "that's not how we do it" feedback
- Faster onboarding of new team members
- Improved audit compliance scores

---

## 11. Conclusion & Recommendation

### Decision: **PROCEED with Implementation**

The Flavors feature offers high value with manageable complexity:

| Factor | Assessment |
|--------|------------|
| **Value** | Very High - 50-70% improvement in AI-assisted consistency |
| **Effort** | Medium - 3-4 weeks for full implementation |
| **Risk** | Low - Well-understood patterns, proven editor library |
| **Synergy** | High - Enhances all existing MCP tools |
| **Differentiation** | Strong - Unique organizational context for AI assistants |

### Recommended First Steps

1. **Start with database schema** - Single table design is efficient
2. **Implement basic MCP tools** - searchFlavors, getFlavorByName
3. **Create simple UI** - List page with basic filtering
4. **Add EasyMDE editor** - Import existing markdown files
5. **Pre-populate examples** - Architecture and compliance flavors

### Expected Outcome

With the Flavors feature, the Spring MCP Server will provide AI assistants with:
- **Organizational context** - Company-specific guidelines always available
- **Consistent code generation** - Following internal standards from first iteration
- **Faster onboarding** - New developers/projects get standards instantly
- **Compliance by default** - Regulatory requirements built into AI workflow
- **Unified AI configuration** - Consistent agent setup across teams

This positions the Spring MCP Server as a comprehensive tool for enterprise AI-assisted development with organizational awareness.

---

## Appendix A: Sample Flavor Templates

### Architecture Flavor Template

```markdown
# [Pattern Name] Architecture Guide

## Overview
Brief description of when and why to use this architecture.

## Package Structure
\`\`\`
com.company.service/
├── domain/
│   ├── model/
│   ├── event/
│   └── service/
├── application/
│   ├── command/
│   └── query/
├── infrastructure/
│   ├── persistence/
│   └── messaging/
└── interfaces/
    ├── rest/
    └── grpc/
\`\`\`

## Key Principles
1. Domain logic in domain layer only
2. Dependencies point inward
3. Ports define interfaces, adapters implement

## Code Examples
[Include actual code examples]

## Anti-patterns to Avoid
- Direct database access from controllers
- Business logic in infrastructure layer
```

### Agent Configuration Flavor Template

```markdown
# [Use Case] Agent Configuration

## Overview
Configuration for AI assistant in [use case] scenarios.

## Claude Code Settings
\`\`\`json
{
  "mcpServers": {
    "spring": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/spring/sse"
    }
  },
  "customCommands": [
    {
      "name": "/review",
      "description": "Run code review checklist",
      "prompt": "Review this code against our standards..."
    }
  ]
}
\`\`\`

## Required MCP Tools
- mcp__spring (Spring documentation)
- mcp__chrome (UI testing)

## Recommended Workflows
1. Before generating code, check architecture flavors
2. After generating, verify compliance rules
3. Use /review command before commit
```

---

*Document generated for capability planning purposes*
*Last updated: 2025-11-30*
*Author: Spring MCP Server Team*

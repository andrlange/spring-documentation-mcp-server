# Flavors Groups Feature - Team-Based Authorization & Flavor Organization

> **Analysis Date**: 2025-12-04
> **Purpose**: Evaluate the feasibility and value of implementing group-based access control and organization for Flavors, enabling team-specific guidelines and fine-grained authorization
> **Spring Boot Version**: 3.5.8
> **Spring AI Version**: 1.1.0

---

## Executive Summary

The "Flavors Groups" feature could provide **60-80% improvement** in team-specific AI-assisted development by implementing authorization boundaries for company guidelines. This addresses a critical limitation: while Flavors provide organizational context to LLMs, there's no mechanism to scope guidelines to specific teams, projects, or compliance domains.

**Key Finding**: Organizations with multiple development teams often have conflicting or specialized requirements. A payment processing team following PCI-DSS compliance shouldn't accidentally receive architecture guidelines designed for a monitoring system. By implementing group-based access control, teams receive only relevant guidance, reducing noise and improving code quality.

**Estimated Impact Areas**:

| Area | Improvement Potential | Example |
|------|----------------------|---------|
| Team-specific guidance accuracy | Very High (70-85%) | PCI team gets only PCI-relevant flavors |
| Reduced cognitive noise | High (55-70%) | Developers see only applicable guidelines |
| Governance compliance | Very High (65-80%) | Enforce separation of concerns across teams |
| Flavor organization | High (50-65%) | Logical grouping of related guidelines |
| Multi-tenant scalability | Very High (75-90%) | Support multiple teams/projects in one instance |

---

## 1. Current Problem Analysis

### Issues with Current Flavor Implementation

When all Flavors are visible to all users and API keys:

| Issue | Type | Impact | Frequency |
|-------|------|--------|-----------|
| All teams see all guidelines | Noise | Decision paralysis, wrong patterns applied | Very High |
| No team boundaries | Governance | PCI team may use non-compliant patterns | High |
| Conflicting architecture styles | Consistency | Different teams apply different patterns | High |
| Generic AI responses | Quality | LLM doesn't know team context | Very High |
| No organization hierarchy | UX | Flat list of flavors becomes unwieldy | Medium |
| Shared sensitive guidelines | Security | Compliance rules visible to all teams | High |

### Root Cause

The current Flavors implementation lacks:
- Team/project scoping mechanisms
- Authorization boundaries for flavor access
- Logical grouping beyond categories
- API key-based filtering for MCP tools
- User-based filtering for Web UI

### Real-World Examples

#### Example 1: Cross-Team Contamination

```markdown
# Scenario: Payment Team vs Monitoring Team

## Without Groups:
- Payment Team asks: "Create a user service"
- LLM retrieves ALL architecture flavors including:
  - Hexagonal Architecture (for Payment - correct)
  - Event Sourcing (for Analytics - wrong for payment)
  - Simple CRUD (for internal tools - wrong security level)

Result: LLM may generate code using Event Sourcing instead of
the PCI-compliant Hexagonal pattern required for payment services.

## With Groups:
- Payment Team API key is member of "Payment Platform" group
- LLM retrieves ONLY flavors in "Payment Platform" group:
  - Hexagonal Architecture (PCI-compliant)
  - PCI-DSS Compliance Rules
  - Payment Service Templates

Result: LLM generates PCI-compliant code from the first attempt.
```

#### Example 2: Compliance Boundary Enforcement

```markdown
# Scenario: GDPR vs HIPAA Teams

## Without Groups:
User from GDPR-focused team searches for "data retention" compliance:
- Gets GDPR Article 17 (Right to Erasure) - correct
- Gets HIPAA 6-year retention requirement - wrong jurisdiction
- Gets PCI-DSS 1-year log retention - wrong domain

Confusion leads to implementing wrong retention policies.

## With Groups:
User is member of "EU Operations" private group:
- Only sees GDPR-related compliance flavors
- No HIPAA or PCI-DSS noise
- Clear, focused compliance guidance
```

#### Example 3: Public Groups for Organization-Wide Standards

```markdown
# Scenario: Company-Wide Coding Standards

## Public Group: "Engineering Standards"
- Contains: Naming Conventions, Git Workflow, Code Review Guidelines
- No members = visible to everyone
- All teams benefit from consistent standards

## Public Group: "Spring Boot Patterns"
- Contains: REST API patterns, Exception Handling, Logging Standards
- Visible to all Spring Boot developers
- Creates consistency across microservices

Result: Common patterns are shared, team-specific patterns are scoped.
```

---

## 2. Feature Components Analysis

### 2.1 Component 0: Groups Foundation

The Groups feature provides the core infrastructure for organizing and securing Flavors.

| Component | Description | Complexity |
|-----------|-------------|------------|
| Group Entity | unique_name, display_name, description, is_active | Low |
| Group Membership | Many-to-many: Groups ↔ Users, Groups ↔ API Keys | Medium |
| Flavor Association | Many-to-many: Groups ↔ Flavors | Medium |
| Cascade Deletions | Auto-remove members/flavors on deletion | Medium |
| UI Redesign | "Users & Groups" menu, two-box selector | High |
| **Inactive Group Handling** | **When `is_active=false`: Group AND all its Flavors hidden from UI and MCP** | Medium |

### 2.2 Component 1: Public Groups

Public groups provide organizational hierarchy without access restrictions.

| Feature | Description | Value |
|---------|-------------|-------|
| No Members = Public | Automatic public status when empty | High |
| Category-Agnostic Grouping | Group flavors across categories | High |
| Expandable UI Display | Groups shown as collapsible sections | Medium |
| MCP Tool: getFlavorsGroup | Return all flavors in a named group | High |
| Topic Organization | Group by use case, not category | Very High |

### 2.3 Component 2: Private Groups

Private groups implement team-based authorization boundaries.

| Feature | Description | Value |
|---------|-------------|-------|
| Member-Based Access | Users and/or API Keys as members | Very High |
| Data Layer Filtering | Filter queries at repository level | High |
| UI Scoping | Show only accessible groups/flavors | High |
| MCP Filtering | API key determines visible flavors | Very High |
| Visibility Rules | Private groups + public groups + unassigned | High |

---

## 3. Database Design Analysis

### 3.1 New Tables Required

```sql
-- Groups table
CREATE TABLE flavor_groups (
    id BIGSERIAL PRIMARY KEY,

    -- Core fields
    unique_name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_group_name_length CHECK (LENGTH(unique_name) >= 3)
);

-- Group membership: Users
CREATE TABLE group_user_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES flavor_groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),

    CONSTRAINT uk_group_user UNIQUE (group_id, user_id)
);

-- Group membership: API Keys
CREATE TABLE group_apikey_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES flavor_groups(id) ON DELETE CASCADE,
    api_key_id BIGINT NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),

    CONSTRAINT uk_group_apikey UNIQUE (group_id, api_key_id)
);

-- Group-Flavor association
CREATE TABLE group_flavors (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES flavor_groups(id) ON DELETE CASCADE,
    flavor_id BIGINT NOT NULL REFERENCES flavors(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),

    CONSTRAINT uk_group_flavor UNIQUE (group_id, flavor_id)
);

-- Indexes for performance
CREATE INDEX idx_flavor_groups_active ON flavor_groups(is_active) WHERE is_active = true;
CREATE INDEX idx_flavor_groups_name ON flavor_groups(unique_name);
CREATE INDEX idx_group_user_members_group ON group_user_members(group_id);
CREATE INDEX idx_group_user_members_user ON group_user_members(user_id);
CREATE INDEX idx_group_apikey_members_group ON group_apikey_members(group_id);
CREATE INDEX idx_group_apikey_members_apikey ON group_apikey_members(api_key_id);
CREATE INDEX idx_group_flavors_group ON group_flavors(group_id);
CREATE INDEX idx_group_flavors_flavor ON group_flavors(flavor_id);
```

### 3.2 Visibility Logic

```sql
-- View: Determine if a group is public (no members)
CREATE OR REPLACE VIEW v_group_visibility AS
SELECT
    fg.id,
    fg.unique_name,
    fg.display_name,
    fg.is_active,
    CASE
        WHEN NOT EXISTS (
            SELECT 1 FROM group_user_members gum WHERE gum.group_id = fg.id
        ) AND NOT EXISTS (
            SELECT 1 FROM group_apikey_members gam WHERE gam.group_id = fg.id
        ) THEN true
        ELSE false
    END AS is_public,
    (SELECT COUNT(*) FROM group_user_members gum WHERE gum.group_id = fg.id) AS user_count,
    (SELECT COUNT(*) FROM group_apikey_members gam WHERE gam.group_id = fg.id) AS apikey_count,
    (SELECT COUNT(*) FROM group_flavors gf WHERE gf.group_id = fg.id) AS flavor_count
FROM flavor_groups fg;

-- Query: Get accessible flavors for an API key
-- Returns: unassigned flavors + public group flavors + private group flavors where API key is member
CREATE OR REPLACE FUNCTION get_accessible_flavors(p_api_key_id BIGINT)
RETURNS TABLE (
    flavor_id BIGINT,
    access_type VARCHAR(20),
    group_name VARCHAR(255)
) AS $$
BEGIN
    RETURN QUERY
    -- Unassigned flavors (not in any group)
    SELECT f.id, 'UNASSIGNED'::VARCHAR(20), NULL::VARCHAR(255)
    FROM flavors f
    WHERE f.is_active = true
    AND NOT EXISTS (SELECT 1 FROM group_flavors gf WHERE gf.flavor_id = f.id)

    UNION ALL

    -- Public group flavors
    SELECT f.id, 'PUBLIC'::VARCHAR(20), fg.unique_name
    FROM flavors f
    JOIN group_flavors gf ON gf.flavor_id = f.id
    JOIN flavor_groups fg ON fg.id = gf.group_id
    WHERE f.is_active = true
    AND fg.is_active = true
    AND NOT EXISTS (SELECT 1 FROM group_user_members gum WHERE gum.group_id = fg.id)
    AND NOT EXISTS (SELECT 1 FROM group_apikey_members gam WHERE gam.group_id = fg.id)

    UNION ALL

    -- Private group flavors (API key is member)
    SELECT f.id, 'PRIVATE'::VARCHAR(20), fg.unique_name
    FROM flavors f
    JOIN group_flavors gf ON gf.flavor_id = f.id
    JOIN flavor_groups fg ON fg.id = gf.group_id
    JOIN group_apikey_members gam ON gam.group_id = fg.id
    WHERE f.is_active = true
    AND fg.is_active = true
    AND gam.api_key_id = p_api_key_id;
END;
$$ LANGUAGE plpgsql;
```

### 3.3 Data Model Diagram

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│     users       │     │   group_user_members │     │  flavor_groups  │
├─────────────────┤     ├──────────────────────┤     ├─────────────────┤
│ id              │◄────│ user_id              │────►│ id              │
│ username        │     │ group_id             │     │ unique_name     │
│ role            │     │ added_at             │     │ display_name    │
│ ...             │     └──────────────────────┘     │ description     │
└─────────────────┘                                  │ is_active       │
                                                     │ ...             │
┌─────────────────┐     ┌──────────────────────┐     └────────┬────────┘
│    api_keys     │     │ group_apikey_members │              │
├─────────────────┤     ├──────────────────────┤              │
│ id              │◄────│ api_key_id           │──────────────┤
│ name            │     │ group_id             │              │
│ key_hash        │     │ added_at             │              │
│ ...             │     └──────────────────────┘              │
└─────────────────┘                                           │
                                                              │
┌─────────────────┐     ┌──────────────────────┐              │
│    flavors      │     │    group_flavors     │              │
├─────────────────┤     ├──────────────────────┤              │
│ id              │◄────│ flavor_id            │──────────────┘
│ unique_name     │     │ group_id             │
│ category        │     │ added_at             │
│ content         │     └──────────────────────┘
│ ...             │
└─────────────────┘
```

---

## 4. Proposed MCP Tools

### Spring AI 1.1.0 MCP Tool Architecture

This project uses **Spring AI 1.1.0** with the `@McpTool` annotation for automatic tool registration:

- **Auto-Registration**: Classes annotated with `@McpTool` are automatically discovered and registered with the MCP server
- **No Manual Configuration**: Unlike older approaches using `@Tool`, no manual bean registration is required
- **Type-Safe Parameters**: Use `@ToolParam` annotation for parameter descriptions and validation
- **Automatic JSON Schema**: Spring AI generates JSON schemas for tool parameters automatically

```java
// Example: Spring AI 1.1.0 MCP Tool Class
@Component
public class FlavorGroupTools {

    @McpTool(name = "getFlavorsGroup",
             description = "Get all flavors in a specific group")
    public FlavorGroupDetail getFlavorsGroup(
        @ToolParam(description = "Unique name of the group") String groupName
    ) {
        // Implementation
    }
}
```

### 4.1 New Tools for Groups

```java
import org.springframework.ai.mcp.server.McpTool;
import org.springframework.ai.tool.annotation.ToolParam;

@McpTool(name = "getFlavorsGroup",
         description = "Get all flavors in a specific group. Returns group metadata and member flavors. " +
                       "Only returns active groups - inactive groups are completely hidden.")
public FlavorGroupDetail getFlavorsGroup(
    @ToolParam(description = "Unique name of the group") String groupName
);

@McpTool(name = "listFlavorGroups",
         description = "List all accessible flavor groups. Returns active public groups and active private groups " +
                       "where caller is member. Inactive groups are completely hidden from results.")
public List<FlavorGroupSummary> listFlavorGroups(
    @ToolParam(description = "Include public groups (default: true)") Boolean includePublic,
    @ToolParam(description = "Include private groups where caller is member (default: true)") Boolean includePrivate
);
```

### 4.2 Modified Existing Tools

The following existing Flavors tools need to incorporate group-based filtering:

| Tool | Modification |
|------|--------------|
| `searchFlavors` | Filter results by API key group membership |
| `getFlavorByName` | Return only if flavor is accessible to API key |
| `getFlavorsByCategory` | Filter by accessible groups |
| `getArchitecturePatterns` | Filter by accessible groups |
| `getComplianceRules` | Filter by accessible groups |
| `getAgentConfiguration` | Filter by accessible groups |
| `getProjectInitialization` | Filter by accessible groups |
| `listFlavorCategories` | Count only accessible flavors per category |

### 4.3 Tool Response DTOs

```java
public record FlavorGroupSummary(
    String uniqueName,
    String displayName,
    String description,
    boolean isPublic,
    int memberCount,     // Users + API Keys
    int flavorCount,
    boolean isActive,
    Instant createdAt
) {}

public record FlavorGroupDetail(
    String uniqueName,
    String displayName,
    String description,
    boolean isPublic,
    List<FlavorSummary> flavors,
    int userMemberCount,
    int apiKeyMemberCount,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
```

### 4.4 Tool Usage Examples

**Example 1: Get Team-Specific Flavors**
```
User: "What architecture patterns should I use for the payment service?"

# Without API key context:
Claude calls: getArchitecturePatterns(technology: "spring")
→ Returns ALL architecture patterns (potentially conflicting)

# With API key in "Payment Platform" group:
Claude calls: getArchitecturePatterns(technology: "spring")
→ Returns ONLY: Hexagonal Architecture, PCI-DSS Patterns
→ Filtered by group membership at data layer
```

**Example 2: List Available Groups**
```
User: "What guideline groups do I have access to?"

Claude calls: listFlavorGroups(includePublic: true, includePrivate: true)
→ Returns:
  - "Engineering Standards" (public, 15 flavors)
  - "Spring Boot Patterns" (public, 8 flavors)
  - "Payment Platform" (private, 12 flavors) ← API key is member
```

**Example 3: Get Group Contents**
```
User: "Show me all Payment Platform guidelines"

Claude calls: getFlavorsGroup(groupName: "payment-platform")
→ Returns:
  {
    uniqueName: "payment-platform",
    displayName: "Payment Platform",
    isPublic: false,
    flavors: [
      { name: "hexagonal-architecture", category: "ARCHITECTURE" },
      { name: "pci-dss-compliance", category: "COMPLIANCE" },
      { name: "payment-service-template", category: "INITIALIZATION" }
    ],
    flavorCount: 3
  }
```

---

## 5. UI Design Specification

### 5.1 Page Structure Changes

**Menu Rename**: `Users` → `Users & Groups`

```
/users-groups
├── Page Header
│   ├── Title: "Users & Groups" with icon (bi-people)
│   ├── Tabs: [Users] [Groups]
│   └── Last modified indicator
│
├── [Users Tab] - Existing user management (unchanged)
│
└── [Groups Tab]
    ├── Statistics Cards (4 cards)
    │   ├── Total Groups
    │   ├── Public Groups
    │   ├── Private Groups
    │   └── Total Memberships
    ├── Action Buttons
    │   ├── [+ New Group]
    │   └── [Refresh]
    ├── Filter Section
    │   ├── Status (Active/Inactive/All)
    │   ├── Visibility (Public/Private/All)
    │   └── Search input
    ├── Groups Table
    │   ├── Name (unique_name, display_name)
    │   ├── Visibility (Public/Private badge)
    │   ├── Members (user count / api key count)
    │   ├── Flavors (count)
    │   ├── Status (active/inactive toggle)
    │   ├── Last Modified
    │   └── Actions (Edit, Delete)
    └── Pagination

/users-groups/groups/new | /users-groups/groups/{id}/edit
├── Basic Information Form
│   ├── Unique Name (required, readonly on edit)
│   ├── Display Name (required)
│   ├── Description (textarea)
│   └── Is Active (toggle)
│
├── Members Section (Two-Box Selector)
│   ├── Users
│   │   ├── [Available Users]     ──► [Added Users]
│   │   │      ↑      │                │      ↑
│   │   │      └──────┴─── [< >] ──────┴──────┘
│   │   └── Shows only active users
│   │
│   └── API Keys
│       ├── [Available API Keys]  ──► [Added API Keys]
│       │      ↑      │                │      ↑
│       │      └──────┴─── [< >] ──────┴──────┘
│       └── Shows only active API keys
│
├── Flavors Section (Two-Box Selector)
│   ├── [Available Flavors]       ──► [Added Flavors]
│   │      ↑      │                    │      ↑
│   │      └──────┴─── [< >] ──────────┴──────┘
│   └── Available: unassigned + public group flavors
│       (Cannot add flavors from other private groups)
│
└── Action Buttons
    ├── Save
    └── Cancel
```

### 5.2 Two-Box Selector Component

```html
<!-- Reusable Two-Box Selector Component -->
<div class="two-box-selector" th:fragment="twoBoxSelector(id, leftItems, rightItems, leftLabel, rightLabel)">
    <div class="row">
        <!-- Left Box: Available -->
        <div class="col-5">
            <label th:text="${leftLabel}" class="form-label">Available</label>
            <select multiple class="form-select" size="10"
                    th:id="${id + '-available'}"
                    th:each="item : ${leftItems}">
                <option th:value="${item.id}" th:text="${item.displayName}">Item</option>
            </select>
        </div>

        <!-- Center: Move Buttons -->
        <div class="col-2 d-flex flex-column justify-content-center align-items-center">
            <button type="button" class="btn btn-outline-primary btn-sm mb-2"
                    th:data-target="${id}" data-direction="right"
                    title="Add selected">
                <i class="bi bi-chevron-right"></i>
            </button>
            <button type="button" class="btn btn-outline-primary btn-sm"
                    th:data-target="${id}" data-direction="left"
                    title="Remove selected">
                <i class="bi bi-chevron-left"></i>
            </button>
        </div>

        <!-- Right Box: Selected -->
        <div class="col-5">
            <label th:text="${rightLabel}" class="form-label">Selected</label>
            <select multiple class="form-select" size="10"
                    th:id="${id + '-selected'}"
                    th:each="item : ${rightItems}">
                <option th:value="${item.id}" th:text="${item.displayName}">Item</option>
            </select>
        </div>
    </div>
</div>
```

### 5.3 Flavors Page Modifications

```
/flavors (Modified)
├── Page Header (unchanged)
├── Statistics Cards (unchanged)
├── Filter Section (modified)
│   ├── Category dropdown
│   ├── Group dropdown (NEW - filter by group)
│   ├── Status (Active/Inactive)
│   ├── Tags multi-select
│   └── Search input
│
├── Groups Section (NEW - expandable)
│   ├── [Group: Engineering Standards] ▼
│   │   ├── Flavor 1
│   │   ├── Flavor 2
│   │   └── Flavor 3
│   ├── [Group: Payment Platform] ▼
│   │   ├── Flavor 4
│   │   └── Flavor 5
│   └── [Group: Monitoring Team] ▼ (hidden if user not member)
│
├── Unassigned Flavors Section
│   └── Flavors not in any group
│
└── Pagination

/flavors/{id}/edit (Modified)
├── Basic Information Form (unchanged)
├── Markdown Editor (unchanged)
├── Groups Section (NEW)
│   ├── [Available Groups]    ──► [Assigned Groups]
│   │      ↑      │                │      ↑
│   │      └──────┴─── [< >] ──────┴──────┘
│   └── Available: public groups + private groups where admin
│
└── Action Buttons (unchanged)
```

### 5.4 Dark Mode Styling

```css
/* Group-specific colors */
:root {
    --group-public: #22d3ee;    /* Cyan */
    --group-private: #f472b6;   /* Pink */
    --group-inactive: #64748b;  /* Slate */
}

/* Visibility badges */
.badge-group-public {
    background-color: var(--group-public);
    color: #0f172a;
}
.badge-group-private {
    background-color: var(--group-private);
    color: #0f172a;
}

/* Two-box selector styling */
.two-box-selector .form-select {
    background-color: var(--bg-secondary);
    border-color: var(--border-color);
    color: var(--text-primary);
}

.two-box-selector .form-select option:checked {
    background-color: var(--spring-green);
    color: #0f172a;
}

/* Expandable group sections */
.flavor-group-header {
    cursor: pointer;
    padding: 0.75rem 1rem;
    background-color: var(--bg-tertiary);
    border-radius: 0.5rem;
    margin-bottom: 0.5rem;
}

.flavor-group-header:hover {
    background-color: var(--bg-hover);
}

.flavor-group-header .badge {
    margin-left: 0.5rem;
}
```

---

## 6. Efficiency Gains Analysis

### 6.1 Without Groups (Current State)

| Task | Time/Iterations | Issue |
|------|-----------------|-------|
| Filter relevant flavors mentally | 2-5 min per request | Manual cognitive filtering |
| Apply wrong team patterns | 1-3 corrections | AI picks any matching pattern |
| Separate team guidelines | Manual documentation | No enforcement mechanism |
| Multi-team server deployment | Not possible | All teams see everything |
| Compliance boundary enforcement | Manual review | No automated scoping |
| **Total per developer/request** | **5-15 min overhead** | Compounds with team size |

### 6.2 With Groups

| Task | Time/Iterations | Improvement |
|------|-----------------|-------------|
| Get team-specific flavors | Instant (filtered) | Automatic scoping |
| Apply correct patterns | First attempt | Group membership enforces context |
| Separate team guidelines | Automatic | Private groups |
| Multi-team server deployment | Built-in | Group isolation |
| Compliance boundary enforcement | Automatic | Data-layer filtering |
| **Total per developer/request** | **< 1 min** | Dramatic reduction |

### 6.3 Projected Improvement by Scenario

| Scenario | Current Issues | With Groups | Improvement |
|----------|----------------|-------------|-------------|
| Multi-team organization | All see all | Team-scoped access | **70-85%** |
| Compliance-sensitive teams | Manual separation | Automatic isolation | **75-90%** |
| New team onboarding | Copy/filter flavors | Add to group | **60-75%** |
| Cross-team collaboration | Manual coordination | Public groups | **50-65%** |
| AI code generation accuracy | Generic context | Team-specific context | **65-80%** |
| Governance audits | Manual verification | Group membership logs | **70-85%** |

**Conservative overall estimate: 60-80% improvement** in team-specific guidance accuracy and governance.

---

## 7. Implementation Roadmap

### Phase 1: Database Foundation (Week 1)

- [ ] Create database schema (flavor_groups, membership tables, junction tables)
- [ ] Add Flyway migration script (V8__flavor_groups.sql)
- [ ] Implement FlavorGroup entity with JPA relationships
- [ ] Implement GroupUserMember entity
- [ ] Implement GroupApiKeyMember entity
- [ ] Implement GroupFlavor entity
- [ ] Create repository classes with custom queries
- [ ] Add cascade deletion triggers/listeners

### Phase 2: Service Layer (Week 1-2)

- [ ] Implement FlavorGroupService (CRUD + membership management)
- [ ] Implement visibility calculation logic (public vs private)
- [ ] Add group-based filtering to FlavorService
- [ ] Create authorization service for access checking
- [ ] Implement API key context extraction for MCP
- [ ] Add user context extraction for Web UI
- [ ] Write unit tests for visibility logic

### Phase 3: MCP Tools (Week 2)

- [ ] Implement `getFlavorsGroup` tool
- [ ] Implement `listFlavorGroups` tool
- [ ] Modify existing 8 Flavors tools for group filtering
- [ ] Add API key context propagation to tool execution
- [ ] Update tool response DTOs
- [ ] Write integration tests for filtered results

### Phase 4: UI - Groups Management (Week 2-3)

- [ ] Rename sidebar menu: "Users" → "Users & Groups"
- [ ] Add Groups tab to users page
- [ ] Create groups list view with statistics
- [ ] Implement two-box selector component (reusable)
- [ ] Create group create/edit forms
- [ ] Add member management (Users, API Keys)
- [ ] Add flavor assignment interface
- [ ] Implement delete confirmation modal

### Phase 5: UI - Flavors Page Updates (Week 3)

- [ ] Add group filter dropdown to flavors page
- [ ] Implement expandable group sections
- [ ] Add group assignment to flavor edit form
- [ ] Implement group-based visibility filtering
- [ ] Add visual indicators for group membership
- [ ] Update statistics cards for group context

### Phase 6: Testing & Polish (Week 4)

- [ ] Full test coverage for all scenarios
- [ ] Performance testing with many groups
- [ ] UI polish and responsive design
- [ ] Documentation updates
- [ ] Sample groups creation (Engineering Standards, etc.)
- [ ] Security audit for authorization logic

---

## 8. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Complex authorization logic | High | High | Thorough unit tests, clear visibility rules |
| Performance with many groups | Medium | Medium | Database indexing, query optimization |
| UI complexity | Medium | Medium | Reusable two-box component, progressive disclosure |
| Migration of existing flavors | Low | Medium | Default to unassigned, gradual migration |
| User confusion (public vs private) | Medium | Low | Clear visual indicators, documentation |
| API key context propagation | Medium | High | Dedicated auth service, request interceptor |
| Cascade deletion edge cases | Low | High | Database constraints, soft delete consideration |

---

## 9. Success Metrics

### 9.1 Quantitative Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Groups created | 10+ groups per multi-team org | Database count |
| Flavors assigned to groups | 80% of flavors in groups | Assignment ratio |
| Private group usage | 50%+ of groups are private | Visibility ratio |
| MCP tool filtering accuracy | 100% correct access | Integration tests |
| Group management time | < 5 min per group | User timing studies |
| API key membership | 90%+ API keys in groups | Membership ratio |

### 9.2 Qualitative Metrics

- Reduced "wrong pattern" complaints from teams
- Clearer separation of team responsibilities
- Easier compliance audits with group boundaries
- Improved AI response relevance per team
- Simplified multi-tenant deployments

---

## 10. Conclusion & Recommendation

### Decision: **PROCEED with Implementation**

The Flavors Groups feature offers high value for multi-team organizations with manageable complexity:

| Factor | Assessment |
|--------|------------|
| **Value** | Very High - 60-80% improvement in team-specific guidance |
| **Effort** | Medium-High - 4 weeks for full implementation |
| **Risk** | Medium - Authorization logic requires careful testing |
| **Synergy** | Very High - Completes the Flavors feature for enterprise use |
| **Differentiation** | Strong - Team-scoped AI context is unique |

### Recommended First Steps

1. **Start with database schema** - Groups and membership tables
2. **Implement visibility logic** - Public vs private determination
3. **Add filtering to existing tools** - Modify FlavorService first
4. **Create Groups management UI** - Reusable two-box selector
5. **Update Flavors page** - Expandable groups display
6. **Pre-populate sample groups** - Engineering Standards, Team Templates

### Expected Outcome

With the Flavors Groups feature, the Spring MCP Server will provide:
- **Team-scoped context** - AI assistants receive only relevant guidelines
- **Authorization boundaries** - Compliance and governance enforcement
- **Logical organization** - Group flavors by use case, not just category
- **Multi-tenant support** - Single instance serves multiple teams securely
- **Audit capability** - Group membership logs for compliance
- **Scalable architecture** - Works for organizations of any size

This positions the Spring MCP Server as a comprehensive enterprise tool for AI-assisted development with proper team boundaries and governance controls.

---

## Appendix A: Visibility Rules Summary

### Inactive Group Rule

> **IMPORTANT**: When a Group is marked as **inactive** (`is_active = false`), the Group and ALL its associated Flavors are **completely hidden** from:
> - The Web UI (not shown in groups list, not shown in flavors page)
> - MCP Tools (not returned by any tool, including `listFlavorGroups`, `getFlavorsGroup`, and all filtered flavor queries)
>
> This allows administrators to temporarily disable entire sets of guidelines without deleting them.

### Flavor Visibility Matrix

| Group State | Flavor State | User Type | API Key Type | Visible? |
|-------------|--------------|-----------|--------------|----------|
| **Inactive** | Any | Any | Any | **No** (group inactive) |
| Active | Unassigned (no group) | Any | Any | Yes |
| Active | In public group | Any | Any | Yes |
| Active | In private group | Group member | N/A | Yes |
| Active | In private group | Non-member | N/A | No |
| Active | In private group | N/A | Group member | Yes |
| Active | In private group | N/A | Non-member | No |
| Mixed (active + inactive) | In multiple groups | Partial member | Partial member | Partial (active member groups only) |

### Group Visibility Calculation

```
is_visible = is_active AND (
    is_public OR is_member(current_user_or_apikey)
)

is_public = (user_member_count == 0) AND (apikey_member_count == 0)
```

### Access Decision Flow

```
┌───────────────────────────────────────────────────────────────┐
│                     Access Request                            │
│                   (User or API Key)                           │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────────┐
│              Is Flavor Assigned to Any Group?                 │
└───────────────────────────┬───────────────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              │ NO                        │ YES
              ▼                           ▼
┌─────────────────────────┐ ┌─────────────────────────────────────┐
│    GRANT ACCESS         │ │   For Each Group Flavor Belongs To: │
│    (Unassigned)         │ └───────────────┬─────────────────────┘
└─────────────────────────┘                 │
                                            ▼
                            ┌───────────────────────────────────┐
                            │      Is Group ACTIVE?             │
                            │   (is_active = true)              │
                            └───────────────┬───────────────────┘
                                            │
                              ┌─────────────┴─────────────┐
                              │ YES                       │ NO
                              ▼                           ▼
                ┌───────────────────────────┐ ┌─────────────────────────┐
                │   Is Group Public?        │ │    DENY ACCESS          │
                └───────────┬───────────────┘ │    (Group Inactive)     │
                            │                 │    Group + Flavors      │
                            │                 │    completely hidden    │
              ┌─────────────┴─────────────┐   └─────────────────────────┘
              │ YES                       │ NO
              ▼                           ▼
┌─────────────────────────┐ ┌─────────────────────────┐
│    GRANT ACCESS         │ │   Is Caller Member?     │
│    (Public Group)       │ └───────────┬─────────────┘
└─────────────────────────┘             │
                              ┌────────┴────────┐
                              │ YES             │ NO
                              ▼                 ▼
                ┌─────────────────────┐ ┌─────────────────────┐
                │   GRANT ACCESS      │ │    DENY ACCESS      │
                │   (Member)          │ │    (Not Member)     │
                └─────────────────────┘ └─────────────────────┘
```

---

## Appendix B: Sample Groups

### Engineering Standards (Public)

```yaml
unique_name: engineering-standards
display_name: Engineering Standards
description: Company-wide coding standards, conventions, and best practices
is_public: true
flavors:
  - java-naming-conventions
  - git-workflow
  - code-review-guidelines
  - logging-standards
  - testing-requirements
```

### Payment Platform (Private)

```yaml
unique_name: payment-platform
display_name: Payment Platform Team
description: Guidelines for PCI-compliant payment services
is_public: false
members:
  users: [alice, bob, carol]
  api_keys: [payment-ci-cd, payment-dev-env]
flavors:
  - hexagonal-architecture
  - pci-dss-compliance
  - payment-service-template
  - encryption-standards
```

### Monitoring Team (Private)

```yaml
unique_name: monitoring-team
display_name: Monitoring & Observability Team
description: Guidelines for monitoring, logging, and observability services
is_public: false
members:
  users: [dave, eve]
  api_keys: [monitoring-ci-cd]
flavors:
  - event-driven-architecture
  - prometheus-patterns
  - grafana-dashboards
```

---

*Document generated for capability planning purposes*
*Last updated: 2025-12-04*
*Author: Spring MCP Server Team*

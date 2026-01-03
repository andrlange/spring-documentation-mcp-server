# MCP Masquerading - Dynamic Tool Visibility Control

> **Analysis Date**: 2026-01-03
> **Target Version**: 1.6.2
> **Status**: Planning Phase
> **Purpose**: Evaluate and implement dynamic MCP tool visibility to prevent LLM confusion when combining multiple MCP servers or when specific tool groups are not needed

---

## Executive Summary

The "MCP Masquerading" feature enables runtime control over which MCP tools are exposed to LLMs. This addresses a critical usability issue: when MCP servers expose many tools (currently 44), LLMs can become confused about which tools to use, especially when combining this server with other MCP servers.

**Key Problem**: LLMs like Claude Code with Opus 4.5 can struggle with tool selection when presented with 50+ tools from multiple MCP servers. The cognitive load of parsing tool descriptions and choosing the right tool leads to:
- Incorrect tool selection
- Redundant tool calls
- Slower response times as the LLM evaluates all options
- Hallucinated tool parameters
- Exhausting Token consumption

**Solution**: MCP Masquerading allows administrators to:
1. Disable entire tool groups or single tools (e.g., disable Javadoc tools when not needed)
2. Customize tool descriptions for specific LLM models
3. Dynamically update tool visibility without server restart
4. Leverage the MCP protocol's `tools/list_changed` notification for real-time updates

**Estimated Impact**:

| Metric | Without Masquerading | With Masquerading | Improvement |
|--------|---------------------|-------------------|-------------|
| Tool selection accuracy | ~75% with 44+ tools | ~95% with 15-20 focused tools | **20% improvement** |
| Average response time | Higher (evaluating all tools) | Lower (focused tool set) | **15-25% faster** |
| LLM token consumption | Higher (all descriptions) | Lower (only enabled) | **30-50% reduction** |
| Multi-server compatibility | Poor (tool collision) | Good (controlled exposure) | **Significant** |

---

## 1. Problem Analysis

### 1.1 Current Tool Landscape

The Spring MCP Server currently exposes **44 tools** across 7 groups:

| Tool Group | Count | Feature Flag | Default |
|------------|-------|--------------|---------|
| Documentation | 10 | Always enabled | On |
| Migration (OpenRewrite) | 7 | `mcp.features.openrewrite.enabled` | On |
| Language Evolution | 7 | `mcp.features.language-evolution.enabled` | On |
| Flavors | 8 | `mcp.features.flavors.enabled` | On |
| Flavor Groups | 3 | `mcp.features.flavors.enabled` | On |
| Initializr | 5 | `mcp.features.initializr.enabled` | On |
| Javadoc | 4 | `mcp.features.javadocs.enabled` | On |

### 1.2 Tool Confusion Scenarios

#### Scenario 1: Combining Multiple MCP Servers

```
┌─────────────────────────────────────────────────────────────────────┐
│ Claude Code with Multiple MCP Servers                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ Spring MCP       │  │ Chrome MCP       │  │ Vaadin MCP       │   │
│  │ 44 tools         │  │ 25 tools         │  │ 15 tools         │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│                                                                     │
│                    Total: 84+ tools                                 │
│                                                                     │
│  LLM Challenge: Which "search" tool to use?                         │
│  - searchSpringDocs (Spring MCP)                                    │
│  - searchJavadocs (Spring MCP)                                      │
│  - search_vaadin_docs (Vaadin MCP)                                  │
│  - take_snapshot (Chrome MCP - for visual search)                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### Scenario 2: Task-Specific Focus

When a developer is:
- **Building a new Spring Boot project**: Only needs Initializr + Documentation tools (15 tools)
- **Migrating from Spring Boot 3 to 4**: Only needs Migration + Documentation tools (17 tools)
- **Writing Javadoc**: Only needs Javadoc + Documentation tools (14 tools)
- **Setting up company standards**: Only needs Flavors tools (11 tools)

### 1.3 Current Limitations

| Limitation | Impact | Current Workaround |
|------------|--------|-------------------|
| All-or-nothing feature flags | Can only disable entire features | Edit application.yml, restart |
| Static tool descriptions | Can't tune for specific LLMs | Hardcoded in Java annotations |
| No runtime control | Requires server restart | Manual configuration change |
| No per-tool granularity | Can't disable individual tools | None |
| Tool descriptions in code | Requires redeployment to change | None |

---

## 2. Proposed Solution: MCP Masquerading

### 2.1 Core Concept

MCP Masquerading provides a management layer between the tool implementations and the MCP protocol, allowing:

```
┌─────────────────────────────────────────────────────────────────────┐
│ Tool Registration Flow with Masquerading                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐     ┌───────────────────┐     ┌──────────────┐  │
│  │ @McpTool       │────▶│ MCP Masquerading  │────▶│ MCP Protocol │  │
│  │ Implementations│     │ Filter            │     │ tools/list   │  │
│  │ (44 tools)     │     │                   │     │              │  │
│  └────────────────┘     │ DB: mcp_tools     │     └──────────────┘  │
│                         │ - isEnabled       │            │          │
│                         │ - description     │            │          │
│                         │ - toolGroup       │            ▼          │
│                         └───────────────────┘     ┌──────────────┐  │
│                                │                  │ LLM receives │  │
│                                │                  │ only enabled │  │
│                                ▼                  │ tools (e.g., │  │
│                         ┌───────────────────┐     │ 15 of 44)    │  │
│                         │ McpSyncServer     │     └──────────────┘  │
│                         │ .notifyToolsList  │                       │
│                         │ Changed()         │                       │
│                         └───────────────────┘                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Key Features

1. **Per-Tool Enable/Disable**: Toggle individual tools on/off
2. **Custom Descriptions**: Edit tool descriptions without code changes
3. **Group Management**: Enable/disable entire tool groups
4. **Real-Time Updates**: Changes propagate instantly via MCP protocol
5. **Description Persistence**: Tool configurations stored in database
6. **Default Initialization**: Auto-populate table on first run

### 2.3 Leveraging Spring AI Dynamic Tool Updates

Based on the [Spring AI Dynamic Tool Updates blog post](https://spring.io/blog/2025/05/04/spring-ai-dynamic-tool-updates-with-mcp):

```java
// Key API from Spring AI's McpSyncServer
mcpSyncServer.addTool(SyncToolSpecification);    // Register new tool
mcpSyncServer.removeTool(String toolName);       // Deregister tool
mcpSyncServer.notifyToolsListChanged();          // Alert clients
```

This allows us to:
- Remove disabled tools from the exposed tool list
- Re-add tools when enabled
- Notify connected LLM clients instantly when tool visibility changes

---

## 3. Database Design

### 3.1 New Table: `mcp_tools`

```sql
-- V23__mcp_masquerading.sql
-- MCP Tool Masquerading Configuration

CREATE TABLE mcp_tools (
    id BIGSERIAL PRIMARY KEY,

    -- Tool identification
    tool_name VARCHAR(100) NOT NULL UNIQUE,
    tool_group VARCHAR(50) NOT NULL,  -- DOCUMENTATION, MIGRATION, LANGUAGE, FLAVORS, FLAVOR_GROUPS, INITIALIZR, JAVADOC

    -- Configuration
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    description TEXT NOT NULL,
    original_description TEXT NOT NULL,  -- Preserve original for reset

    -- Metadata
    display_order INT DEFAULT 0,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_tool_name_length CHECK (LENGTH(tool_name) >= 3),
    CONSTRAINT chk_tool_group CHECK (tool_group IN (
        'DOCUMENTATION', 'MIGRATION', 'LANGUAGE',
        'FLAVORS', 'FLAVOR_GROUPS', 'INITIALIZR', 'JAVADOC'
    ))
);

-- Indexes
CREATE INDEX idx_mcp_tools_enabled ON mcp_tools(is_enabled) WHERE is_enabled = true;
CREATE INDEX idx_mcp_tools_group ON mcp_tools(tool_group);
CREATE INDEX idx_mcp_tools_name ON mcp_tools(tool_name);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION mcp_tools_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER mcp_tools_update_trigger
    BEFORE UPDATE ON mcp_tools
    FOR EACH ROW
    EXECUTE FUNCTION mcp_tools_update_timestamp();
```

### 3.2 Initial Data Population

Tools will be auto-populated on application startup if the table is empty:

```java
@Component
public class McpToolsInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        if (mcpToolRepository.count() == 0) {
            populateAllTools();
        }
    }
}
```

---

## 4. UI Design

### 4.1 Navigation

Add new menu item in sidebar (below Embeddings):

```html
<!-- Add after Embeddings menu item -->
<li class="nav-item" sec:authorize="hasRole('ADMIN')">
    <a class="nav-link" th:href="@{/mcp-tools}"
       th:classappend="${activePage == 'mcp-tools'} ? 'active' : ''"
       data-sidebar-tooltip="MCP Tools">
        <i class="bi bi-tools" style="color: #38bdf8;"></i>
        <span class="nav-text">MCP Masquerading</span>
    </a>
</li>
```

### 4.2 Page Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│ MCP Masquerading                                        [Refresh]   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐     │
│ │ Total: 44   │ │ Enabled: 35 │ │ Disabled: 9 │ │ Groups: 7   │     │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘     │
│                                                                     │
│ ┌───────────────────────────────────────────────────────────────┐   │
│ │ ▼ Documentation (10 tools)                    [Enable All]    │   │
│ ├───────────────────────────────────────────────────────────────┤   │
│ │ ☑ searchSpringDocs          Search across all Spring...  [✏️] │   │
│ │ ☑ getSpringVersions         List available versions...   [✏️] │   │
│ │ ☑ listSpringProjects        List all available Spri...   [✏️] │   │
│ │ ...                                                           │   │
│ └───────────────────────────────────────────────────────────────┘   │
│                                                                     │
│ ┌───────────────────────────────────────────────────────────────┐   │
│ │ ▼ Migration (7 tools)                         [Enable All]    │   │
│ ├───────────────────────────────────────────────────────────────┤   │
│ │ ☐ getSpringMigrationGuide   Get comprehensive migra...   [✏️] │   │
│ │ ☐ getBreakingChanges        Get breaking changes fo...   [✏️] │   │
│ │ ...                                                           │   │
│ └───────────────────────────────────────────────────────────────┘   │
│                                                                     │
│ ... (more groups)                                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.3 Edit Description Modal

```
┌─────────────────────────────────────────────────────────────────────┐
│ Edit Tool Description                                          [X]  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ Tool: searchSpringDocs                                              │
│ Group: Documentation                                                │
│                                                                     │
│ Description:                                                        │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ Search across all Spring documentation with pagination support. │ │
│ │ Supports filtering by project, version, and documentation type. │ │
│ │ Returns relevant documentation links and snippets with          │ │
│ │ relevance ranking.                                              │ │
│ │ Use pagination (page parameter) to navigate through large       │ │
│ │ result sets.                                                    │ │
│ │ When embeddings are enabled, uses hybrid search (keyword +      │ │
│ │ semantic) for better results.                                   │ │
│ └─────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│ ⚠️ Modified from original (shows only when description changed)     │
│                                                                     │
│ [Reset to Original]                          [Cancel]  [Save]       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.4 Reset Confirmation Modal

When the user clicks "Reset to Original", a warning modal appears:

```
┌────────────────────────────────────────────────────────────────────┐
│ ⚠️ Reset Tool Description                                     [X]  │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  ⚠️ WARNING                                                 │   │
│  │                                                             │   │
│  │  You are about to reset the description for:                │   │
│  │                                                             │   │
│  │  Tool: searchSpringDocs                                     │   │
│  │                                                             │   │
│  │  This will:                                                 │   │
│  │  • Discard your custom description                          │   │
│  │  • Restore the original tool description                    │   │
│  │  • Immediately update the MCP tool list for connected LLMs  │   │
│  │                                                             │   │
│  │  This action cannot be undone.                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                    │
│                                    [Cancel]  [Reset to Original]   │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**Modal Behavior**:
- The "Reset to Original" button is styled with warning colors (amber/orange)
- The modal only appears if the current description differs from the original
- If description matches original, the reset button is disabled with tooltip "Already using original description"
- After reset, the edit modal refreshes to show the restored original description
- A toast notification confirms: "Description reset to original"

### 4.5 Styling (Dark Theme Aligned)

```css
/* MCP Tools specific styling */
.mcp-tool-group {
    background: rgba(30, 41, 59, 0.8);
    border: 1px solid rgba(148, 163, 184, 0.2);
    border-radius: 8px;
    margin-bottom: 1rem;
}

.mcp-tool-group-header {
    padding: 0.75rem 1rem;
    border-bottom: 1px solid rgba(148, 163, 184, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
}

.mcp-tool-row {
    display: flex;
    align-items: center;
    padding: 0.5rem 1rem;
    border-bottom: 1px solid rgba(148, 163, 184, 0.05);
}

.mcp-tool-row:hover {
    background: rgba(56, 189, 248, 0.05);
}

.mcp-tool-toggle {
    width: 44px;
}

.mcp-tool-name {
    width: 200px;
    font-weight: 500;
    color: #e2e8f0;
}

.mcp-tool-description {
    flex: 1;
    color: #94a3b8;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.mcp-tool-actions {
    width: 50px;
    text-align: right;
}
```

---

## 5. Technical Implementation

### 5.1 Configuration Class

Move tool descriptions from `@McpTool` annotations to a centralized configuration:

```java
@Configuration
@ConfigurationProperties(prefix = "mcp.tools")
public class McpToolsConfig {

    // Tool definitions loaded from database on startup
    private Map<String, McpToolDefinition> definitions = new HashMap<>();

    @PostConstruct
    public void loadFromDatabase() {
        // Load all tool configurations from mcp_tools table
    }

    public McpToolDefinition getDefinition(String toolName) {
        return definitions.get(toolName);
    }

    public boolean isEnabled(String toolName) {
        McpToolDefinition def = definitions.get(toolName);
        return def != null && def.isEnabled();
    }

    public String getDescription(String toolName) {
        McpToolDefinition def = definitions.get(toolName);
        return def != null ? def.getDescription() : "";
    }
}
```

### 5.2 Tool Registration Service

```java
@Service
@RequiredArgsConstructor
public class McpToolMasqueradingService {

    private final McpSyncServer mcpSyncServer;
    private final McpToolRepository toolRepository;
    private final Map<String, SyncToolSpecification> allTools = new ConcurrentHashMap<>();

    /**
     * Initialize with all available tools (called at startup)
     */
    public void registerAllTools(List<SyncToolSpecification> tools) {
        tools.forEach(tool -> allTools.put(tool.name(), tool));
        applyMasquerading();
    }

    /**
     * Apply masquerading based on database configuration
     */
    public void applyMasquerading() {
        List<McpTool> dbTools = toolRepository.findAll();

        for (McpTool dbTool : dbTools) {
            SyncToolSpecification original = allTools.get(dbTool.getToolName());
            if (original == null) continue;

            if (dbTool.isEnabled()) {
                // Create modified spec with custom description
                SyncToolSpecification modified = createModifiedSpec(
                    original, dbTool.getDescription()
                );
                mcpSyncServer.addTool(modified);
            } else {
                mcpSyncServer.removeTool(dbTool.getToolName());
            }
        }

        // Notify connected clients
        mcpSyncServer.notifyToolsListChanged();
    }

    /**
     * Toggle a single tool's visibility
     */
    @Transactional
    public void toggleTool(String toolName, boolean enabled) {
        McpTool tool = toolRepository.findByToolName(toolName)
            .orElseThrow(() -> new NotFoundException("Tool not found: " + toolName));

        tool.setEnabled(enabled);
        toolRepository.save(tool);

        // Apply change immediately
        SyncToolSpecification original = allTools.get(toolName);
        if (enabled && original != null) {
            SyncToolSpecification modified = createModifiedSpec(
                original, tool.getDescription()
            );
            mcpSyncServer.addTool(modified);
        } else {
            mcpSyncServer.removeTool(toolName);
        }

        mcpSyncServer.notifyToolsListChanged();
    }

    /**
     * Update tool description
     */
    @Transactional
    public void updateDescription(String toolName, String newDescription) {
        McpTool tool = toolRepository.findByToolName(toolName)
            .orElseThrow(() -> new NotFoundException("Tool not found: " + toolName));

        tool.setDescription(newDescription);
        toolRepository.save(tool);

        // Re-register with new description if enabled
        if (tool.isEnabled()) {
            SyncToolSpecification original = allTools.get(toolName);
            if (original != null) {
                mcpSyncServer.removeTool(toolName);
                SyncToolSpecification modified = createModifiedSpec(original, newDescription);
                mcpSyncServer.addTool(modified);
                mcpSyncServer.notifyToolsListChanged();
            }
        }
    }
}
```

### 5.3 Entity and Repository

```java
@Entity
@Table(name = "mcp_tools")
@Data
@NoArgsConstructor
public class McpToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_name", nullable = false, unique = true)
    private String toolName;

    @Column(name = "tool_group", nullable = false)
    @Enumerated(EnumType.STRING)
    private McpToolGroup toolGroup;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_description", nullable = false, columnDefinition = "TEXT")
    private String originalDescription;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}

public enum McpToolGroup {
    DOCUMENTATION("Documentation", 10),
    MIGRATION("Migration", 7),
    LANGUAGE("Language Evolution", 7),
    FLAVORS("Flavors", 8),
    FLAVOR_GROUPS("Flavor Groups", 3),
    INITIALIZR("Initializr", 5),
    JAVADOC("Javadoc", 4);

    private final String displayName;
    private final int expectedCount;

    McpToolGroup(String displayName, int expectedCount) {
        this.displayName = displayName;
        this.expectedCount = expectedCount;
    }
}

@Repository
public interface McpToolRepository extends JpaRepository<McpToolEntity, Long> {

    Optional<McpToolEntity> findByToolName(String toolName);

    List<McpToolEntity> findByToolGroupOrderByDisplayOrder(McpToolGroup group);

    List<McpToolEntity> findByEnabledTrueOrderByToolGroupAscDisplayOrderAsc();

    @Query("SELECT t.toolGroup, COUNT(t), SUM(CASE WHEN t.enabled = true THEN 1 ELSE 0 END) " +
           "FROM McpToolEntity t GROUP BY t.toolGroup")
    List<Object[]> getGroupStatistics();

    @Modifying
    @Query("UPDATE McpToolEntity t SET t.enabled = :enabled WHERE t.toolGroup = :group")
    int updateGroupEnabled(@Param("group") McpToolGroup group, @Param("enabled") boolean enabled);
}
```

### 5.4 Controller

```java
@Controller
@RequestMapping("/mcp-tools")
@RequiredArgsConstructor
public class McpToolsController {

    private final McpToolService mcpToolService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "mcp-tools");
        model.addAttribute("toolGroups", mcpToolService.getAllGroupsWithTools());
        model.addAttribute("stats", mcpToolService.getStatistics());
        return "mcp-tools/index";
    }

    @PostMapping("/{toolName}/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleTool(
            @PathVariable String toolName,
            @RequestParam boolean enabled) {
        mcpToolService.toggleTool(toolName, enabled);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{toolName}/description")
    @ResponseBody
    public ResponseEntity<?> updateDescription(
            @PathVariable String toolName,
            @RequestBody Map<String, String> request) {
        mcpToolService.updateDescription(toolName, request.get("description"));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/groups/{group}/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleGroup(
            @PathVariable McpToolGroup group,
            @RequestParam boolean enabled) {
        mcpToolService.toggleGroup(group, enabled);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{toolName}")
    @ResponseBody
    public ResponseEntity<McpToolDto> getTool(@PathVariable String toolName) {
        return ResponseEntity.ok(mcpToolService.getTool(toolName));
    }

    @PostMapping("/{toolName}/reset")
    @ResponseBody
    public ResponseEntity<?> resetDescription(@PathVariable String toolName) {
        mcpToolService.resetToOriginal(toolName);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
```

---

## 6. Implementation Plan

### Phase 1: Foundation (Tasks 1-5)

| Task | Description | Effort |
|------|-------------|--------|
| 1.1 | Create database migration V23__mcp_masquerading.sql | 1h |
| 1.2 | Create McpToolEntity and McpToolGroup enum | 1h |
| 1.3 | Create McpToolRepository with custom queries | 1h |
| 1.4 | Create McpToolDto for API responses | 30m |
| 1.5 | Implement McpToolsInitializer (auto-populate on startup) | 2h |

### Phase 2: Core Service (Tasks 6-10)

| Task | Description | Effort |
|------|-------------|--------|
| 2.1 | Create McpToolService with CRUD operations | 2h |
| 2.2 | Implement McpToolMasqueradingService (dynamic updates) | 3h |
| 2.3 | Integrate with McpSyncServer for add/remove/notify | 2h |
| 2.4 | Add caching for tool configurations | 1h |
| 2.5 | Write unit tests for services (>80% coverage) | 3h |

### Phase 3: UI Development (Tasks 11-18)

| Task | Description | Effort |
|------|-------------|--------|
| 3.1 | Add sidebar menu item for MCP Masquerading | 30m |
| 3.2 | Create mcp-tools/index.html with group accordion | 3h |
| 3.3 | Implement toggle functionality with HTMX | 2h |
| 3.4 | Create edit description modal with "Modified" indicator | 2h |
| 3.5 | Create reset confirmation warning modal with popup warning | 1.5h |
| 3.6 | Add "Modified" badge to tool rows showing custom descriptions | 1h |
| 3.7 | Implement reset button with conditional enable/disable state | 1h |
| 3.8 | Add statistics cards (total/enabled/disabled/modified/groups) | 1h |

### Phase 4: Controller & Integration (Tasks 19-24)

| Task | Description | Effort |
|------|-------------|--------|
| 4.1 | Create McpToolsController with all endpoints | 2h |
| 4.2 | Implement reset endpoint returning wasReset flag | 1h |
| 4.3 | Implement isModified check endpoint | 30m |
| 4.4 | Add security configuration for /mcp-tools/** | 30m |
| 4.5 | Add CSRF exception for API endpoints | 30m |
| 4.6 | Write integration tests for controller | 2h |

### Phase 5: Testing & Documentation (Tasks 25-31)

| Task | Description | Effort |
|------|-------------|--------|
| 5.1 | Comprehensive unit tests (>80% coverage) | 3h |
| 5.2 | Integration tests with actual MCP client | 2h |
| 5.3 | Test reset functionality end-to-end with warning modal | 1h |
| 5.4 | Test MCP protocol compliance with tool updates | 2h |
| 5.5 | Update README.md with feature documentation | 1h |
| 5.6 | Update ADDITIONAL_CONTENT.md with detailed API | 1h |
| 5.7 | Update CHANGELOG.md and VERSIONS.md | 30m |

### Phase 6: Release Preparation (Tasks 32-34)

| Task | Description | Effort |
|------|-------------|--------|
| 6.1 | Bump version to 1.6.2 in all files | 30m |
| 6.2 | Update application.yml server instructions | 30m |
| 6.3 | Final testing and verification | 2h |

---

## 7. Success Criteria

### 7.1 Functional Requirements

- [ ] All 44 tools are listed in the management UI
- [ ] Tools can be enabled/disabled individually
- [ ] Tool groups can be enabled/disabled in bulk
- [ ] Descriptions can be edited and saved
- [ ] Changes take effect immediately (no restart needed)
- [ ] Connected MCP clients receive tool list change notifications
- [ ] Original descriptions can be restored via reset button
- [ ] Reset button shows warning popup before discarding custom description
- [ ] "Modified" badge appears on tools with custom descriptions
- [ ] Reset button is disabled when description matches original
- [ ] Statistics cards show count of modified tools

### 7.2 Technical Requirements

- [ ] Test coverage > 80% for new code
- [ ] Database migration runs successfully on fresh install
- [ ] No breaking changes to existing MCP tool functionality
- [ ] UI follows existing dark theme design patterns
- [ ] Performance: Toggle operation < 500ms

### 7.3 Documentation Requirements

- [ ] README.md updated with feature description
- [ ] ADDITIONAL_CONTENT.md updated with UI documentation
- [ ] CHANGELOG.md updated with version 1.6.2 changes
- [ ] VERSIONS.md updated with version summary

---

## 8. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Spring AI 1.2 doesn't support dynamic updates | Low | High | Verify API exists before implementation |
| Tool state inconsistency on restart | Medium | Medium | Load from DB on startup, sync with Spring AI |
| Performance impact with many toggles | Low | Low | Batch notifications, use caching |
| UI complexity | Low | Low | Follow existing Flavors UI patterns |
| Breaking MCP clients | Low | High | Thorough integration testing |

---

## 9. Appendix: Tool Groups and Their Tools

### Documentation (10 tools)
1. searchSpringDocs
2. getSpringVersions
3. listSpringProjects
4. getDocumentationByVersion
5. getCodeExamples
6. listSpringBootVersions
7. getLatestSpringBootVersion
8. filterSpringBootVersionsBySupport
9. listProjectsBySpringBootVersion
10. findProjectsByUseCase

### Migration (7 tools)
1. getSpringMigrationGuide
2. getBreakingChanges
3. searchMigrationKnowledge
4. getAvailableMigrationPaths
5. getTransformationsByType
6. getDeprecationReplacement
7. checkVersionCompatibility

### Language Evolution (7 tools)
1. getLanguageVersions
2. getLanguageFeatures
3. getModernPatterns
4. getLanguageVersionDiff
5. getSpringBootLanguageRequirements
6. searchLanguageFeatures
7. getLanguageFeatureExample

### Flavors (8 tools)
1. searchFlavors
2. getFlavorByName
3. getFlavorsByCategory
4. getArchitecturePatterns
5. getComplianceRules
6. getAgentConfiguration
7. getProjectInitialization
8. listFlavorCategories

### Flavor Groups (3 tools)
1. listFlavorGroups
2. getFlavorsGroup
3. getFlavorGroupStatistics

### Initializr (5 tools)
1. initializrGetDependency
2. initializrSearchDependencies
3. initializrCheckCompatibility
4. initializrGetBootVersions
5. initializrGetDependencyCategories

### Javadoc (4 tools)
1. getClassDoc
2. getPackageDoc
3. searchJavadocs
4. listJavadocLibraries

---

## 10. Ralph Wiggum Implementation Prompt

Use the following prompt to start a Claude Code Ralph Wiggum loop for implementing this feature:

```
/ralph-wiggum:ralph-loop

Implement the MCP Masquerading feature for the Spring MCP Server as defined in capabilities/MCP_MASQUERADING.md

## Completion Promise
The implementation is COMPLETED when:
1. Database migration V23__mcp_masquerading.sql is created and applied
2. All entity, repository, service, and controller classes are implemented
3. The UI page /mcp-tools is accessible with full functionality including:
   - Tool enable/disable toggles
   - Edit description modal with "Modified" indicator
   - Reset confirmation warning modal (popup warning before discarding custom description)
   - "Modified" badge on tools with custom descriptions
   - Statistics cards including modified count
4. Reset functionality works correctly:
   - Reset button shows warning popup before discarding custom description
   - Reset restores original description from database
   - Reset button is disabled when description matches original
   - Toast notification confirms reset action
5. Unit tests achieve >80% code coverage for new classes
6. Integration tests verify MCP tool visibility changes work correctly
7. README.md is updated with MCP Masquerading feature documentation
8. ADDITIONAL_CONTENT.md is updated with detailed UI documentation
9. CHANGELOG.md is updated with version 1.6.2 changes
10. VERSIONS.md is updated with version 1.6.2 summary
11. Version bumped to 1.6.2 in: build.gradle, application.yml, docker-compose-all.yaml, build-container.sh

## Implementation Order
Follow the phases defined in the document:
1. Phase 1: Database and Entity layer
2. Phase 2: Service layer with McpSyncServer integration (including resetToOriginal and isModified)
3. Phase 3: UI with Thymeleaf templates (including reset confirmation modal)
4. Phase 4: Controller and security configuration
5. Phase 5: Tests (>80% coverage)
6. Phase 6: Documentation and version bump

## Key Technical Notes
- Use Spring AI's McpSyncServer.notifyToolsListChanged() for real-time updates
- Follow existing patterns from FlavorTools and FlavorGroupTools
- UI should follow the dark theme pattern from existing pages (see API Keys modals as reference)
- Use HTMX for toggle functionality without full page reloads
- Reset button must show warning modal before discarding custom description
- Store originalDescription separately from description to enable comparison and reset

Mark all done steps in this planning document for each step when done.
--completion-promise "COMPLETE" --max-iterations 10
```

---

*Document created: 2026-01-03*
*Author: Spring MCP Server Team*
*Status: Ready for Review*

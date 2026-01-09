# MCP Streamable-HTTP Migration Plan

> **Target:** Migrate from SSE-based MCP Server to Streamable-HTTP MCP Server
> **Spring Boot:** 3.5.9 | **Spring AI:** 1.1.2 | **Current Version:** 1.7.1
> **Protocol:** STREAMABLE (stateful HTTP with tool-change-notification support)

## IMPORTANT: Protocol Choice

| Protocol | Session State | Runtime Tool Mods | Tool Masquerading | Recommended |
|----------|---------------|-------------------|-------------------|-------------|
| `SSE` | Stateful | Yes | Yes | Legacy |
| `STREAMABLE` | Stateful | **Yes** | **Yes** | **YES** |
| `STATELESS` | None | **No** | **No** | No (breaks masquerading) |

**Decision:** Use `STREAMABLE` protocol to preserve `/mcp-tools` masquerading feature while gaining modern HTTP transport benefits.

## Executive Summary

This document outlines the migration from the current SSE (Server-Sent Events) based MCP Server implementation to the new Streamable-HTTP stateless pattern as specified in the MCP Protocol version 2025-11-25 and supported by Spring AI's `spring-ai-starter-mcp-server-webmvc`.

---

## Part 1: MCP Streamable-HTTP Transport Specification

**Source:** [MCP Specification 2025-11-25](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#streamable-http)

### Key Concepts

The **Streamable HTTP transport** replaces the deprecated HTTP+SSE transport. It provides:

- **Single unified endpoint** (vs. separate POST/SSE endpoints)
- **Cleaner session management** with explicit headers
- **Improved resumability** with event IDs
- **Better backwards compatibility**

### Endpoint Requirements

| Aspect | Requirement |
|--------|-------------|
| Endpoint | Single HTTP endpoint supporting POST and GET |
| Example | `https://example.com/mcp` |
| Security | MUST validate `Origin` header (DNS rebinding protection) |
| Error | Return HTTP 403 Forbidden if Origin invalid |

### Request Format (POST)

```http
POST /mcp HTTP/1.1
Accept: application/json, text/event-stream
Content-Type: application/json
MCP-Session-Id: [session-id]
MCP-Protocol-Version: 2025-11-25

{JSON-RPC request/notification/response}
```

### Response Types

| Request Type | Response |
|--------------|----------|
| Notification/Response | HTTP 202 Accepted (no body) |
| Request (simple) | `Content-Type: application/json` |
| Request (streaming) | `Content-Type: text/event-stream` |

### SSE Stream Format

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Transfer-Encoding: chunked

id: event-1
data:

id: event-2
data: {JSON-RPC message}

id: event-3
data: {JSON-RPC response}
```

### Session Management

| Header | Purpose |
|--------|---------|
| `MCP-Session-Id` | Session identifier (assigned during initialization) |
| `MCP-Protocol-Version` | Protocol version (required on all requests) |
| `Last-Event-ID` | Resume stream after disconnection |

**Session ID Requirements:**
- MUST be globally unique (UUID, JWT, or hash)
- MUST contain only visible ASCII characters (0x21-0x7E)
- Server MAY terminate session anytime (respond with HTTP 404)

### Listening for Server Messages (GET)

```http
GET /mcp HTTP/1.1
Accept: text/event-stream
MCP-Session-Id: [session-id]
MCP-Protocol-Version: 2025-11-25
```

### Protocol Version Header

All HTTP requests MUST include:
```
MCP-Protocol-Version: 2025-11-25
```

### Backwards Compatibility

For clients supporting legacy servers:
1. Attempt POST `InitializeRequest` with `Accept` header
2. **Success**: Server supports Streamable HTTP
3. **Failure** (400/404/405): Try GET request
4. If GET succeeds with `endpoint` event: Use old HTTP+SSE transport

---

## Part 2: Spring AI MCP Stateless Server Boot Starter

**Source:** [Spring AI MCP Stateless Server Docs](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-stateless-server-boot-starter-docs.html)

### Dependencies

**WebMVC (Servlet-based) - Recommended for this project:**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

**WebFlux (Reactive) - Alternative:**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
</dependency>
```

### Configuration Properties

#### Common Properties (`spring.ai.mcp.server.*`)

| Property | Description | Default |
|----------|-------------|---------|
| `enabled` | Enable/disable MCP server | `true` |
| `protocol` | Must be `STATELESS` | - |
| `name` | Server identification | `mcp-server` |
| `version` | Server version | `1.0.0` |
| `instructions` | Client instructions | `null` |
| `type` | Server type (SYNC/ASYNC) | `SYNC` |
| `tool-callback-converter` | Convert ToolCallbacks | `true` |
| `request-timeout` | Request timeout | `20 seconds` |
| `capabilities.resource` | Resource support | `true` |
| `capabilities.tool` | Tool support | `true` |
| `capabilities.prompt` | Prompt support | `true` |
| `capabilities.completion` | Completion support | `true` |

#### Stateless-Specific Properties (`spring.ai.mcp.server.stateless.*`)

| Property | Description | Default |
|----------|-------------|---------|
| `mcp-endpoint` | Custom endpoint path | `/mcp` |
| `disallow-delete` | Disallow delete operations | `false` |

### Example Configuration (STREAMABLE - Recommended)

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        name: spring-documentation-server
        version: 1.8.0
        type: SYNC
        instructions: "Spring Documentation MCP Server with real-time tool notifications"
        # Enable change notifications (required for masquerading)
        tool-change-notification: true
        resource-change-notification: false
        prompt-change-notification: false
        streamable-http:
          mcp-endpoint: /mcp/spring
          keep-alive-interval: 30s
```

### Example Configuration (STATELESS - NOT Recommended)

**WARNING:** STATELESS mode breaks the `/mcp-tools` masquerading feature!

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STATELESS
        name: spring-documentation-server
        version: 1.8.0
        type: SYNC
        stateless:
          mcp-endpoint: /mcp/spring
```

### Key Differences: Protocol Modes

| Feature | STREAMABLE | STATELESS | Current SSE |
|---------|------------|-----------|-------------|
| Session State | Maintained | Not maintained | Maintained |
| Transport | HTTP (single endpoint) | HTTP (single endpoint) | SSE + HTTP (two endpoints) |
| Runtime Tool Mods | **Supported** | Not supported | Supported |
| Tool Masquerading | **Works** | **BROKEN** | Works |
| Client Requests | Supported | Not supported | Supported |
| Elicitation | Supported | Not supported | Supported |
| Sampling | Supported | Not supported | Supported |
| Ping | Supported | Not supported | Supported |
| Deployment | Cloud-native, scalable | Persistent connections |
| Load Balancing | Simple | Complex (sticky sessions) |

### Limitations of Stateless Mode

1. **No MCP Client Message Requests:**
   - No elicitation support
   - No sampling support
   - No ping support

2. **No Session State:**
   - Each request is independent
   - No conversation context preservation

3. **No Tool Context:**
   - Tool context not applicable

---

## Part 3: Current Implementation Analysis

### Current Architecture

```
┌─────────────────────────────────────────┐
│         MCP Client (Claude Code)        │
└─────────────────────────────────────────┘
                    │ SSE
                    ▼
┌─────────────────────────────────────────┐
│    Spring AI MCP SSE Controller         │
│  - /mcp/spring/sse (SSE endpoint)       │  ← WILL CHANGE
│  - /mcp/spring/messages (HTTP)          │  ← WILL MERGE
│  - 46 registered tools                  │
└─────────────────────────────────────────┘
```

### Current Configuration (application.yml)

```yaml
spring:
  ai:
    mcp:
      server:
        name: "spring-documentation-server"
        type: "sync"
        version: "1.7.1"
        annotation-scanner:
          enabled: false
        sse-endpoint: /mcp/spring/sse           # REMOVE
        sse-message-endpoint: /mcp/spring/messages  # REMOVE
        capabilities:
          tool: true
          completion: false
          prompt: false
          resource: false
```

### Current Tool Classes

Location: `src/main/java/com/spring/mcp/service/tools/`

| File | Tools | Category |
|------|-------|----------|
| `SpringDocumentationTools.java` | 12 | Documentation |
| `MigrationTools.java` | 7 | Migration |
| `LanguageEvolutionTools.java` | 7 | Language Evolution |
| `FlavorTools.java` | 8 | Flavors |
| `FlavorGroupTools.java` | 3 | Flavor Groups |
| `InitializrTools.java` | 5 | Initializr |
| `JavadocTools.java` | 4 | Javadoc |
| `WikiTools.java` | 2 | Wiki |

---

## Part 4: MCP Tool Migration Checklist

### Legend
- [ ] Migration required
- [x] No changes needed
- [?] Needs verification

---

### SpringDocumentationTools.java (12 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `listSpringProjects` | [x] No changes | Stateless query |
| 2 | `getSpringVersions` | [x] No changes | Stateless query |
| 3 | `listSpringBootVersions` | [x] No changes | Stateless query |
| 4 | `filterSpringBootVersionsBySupport` | [x] No changes | Stateless query |
| 5 | `searchSpringDocs` | [x] No changes | Stateless query, pagination |
| 6 | `getCodeExamples` | [x] No changes | Stateless query |
| 7 | `findProjectsByUseCase` | [x] No changes | Stateless query |
| 8 | `getDocumentationByVersion` | [x] No changes | Stateless query |
| 9 | `getLatestSpringBootVersion` | [x] No changes | Stateless query |
| 10 | `listProjectsBySpringBootVersion` | [x] No changes | Stateless query |
| 11 | `getWikiReleaseNotes` | [x] No changes | Stateless query |
| 12 | `getWikiMigrationGuide` | [x] No changes | Stateless query |

**Status:** All tools compatible - no state dependencies

---

### MigrationTools.java (7 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `getBreakingChanges` | [x] No changes | Stateless query |
| 2 | `getSpringMigrationGuide` | [x] No changes | Stateless query |
| 3 | `searchMigrationKnowledge` | [x] No changes | Stateless query, hybrid search |
| 4 | `getTransformationsByType` | [x] No changes | Stateless query |
| 5 | `getDeprecationReplacement` | [x] No changes | Stateless query |
| 6 | `getAvailableMigrationPaths` | [x] No changes | Stateless query |
| 7 | `checkVersionCompatibility` | [x] No changes | Stateless query |

**Status:** All tools compatible - no state dependencies

---

### LanguageEvolutionTools.java (7 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `getLanguageVersions` | [x] No changes | Stateless query |
| 2 | `getLanguageFeatures` | [x] No changes | Stateless query |
| 3 | `getLanguageVersionDiff` | [x] No changes | Stateless query |
| 4 | `searchLanguageFeatures` | [x] No changes | Stateless query |
| 5 | `getSpringBootLanguageRequirements` | [x] No changes | Stateless query |
| 6 | `getModernPatterns` | [x] No changes | Stateless query |
| 7 | `getLanguageFeatureExample` | [x] No changes | Stateless query |

**Status:** All tools compatible - no state dependencies

---

### FlavorTools.java (8 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `listFlavorCategories` | [x] No changes | Stateless query |
| 2 | `getFlavorsByCategory` | [x] No changes | Stateless query |
| 3 | `getFlavorByName` | [x] No changes | Stateless query |
| 4 | `searchFlavors` | [x] No changes | Stateless query, hybrid search |
| 5 | `getArchitecturePatterns` | [x] No changes | Stateless query |
| 6 | `getComplianceRules` | [x] No changes | Stateless query |
| 7 | `getAgentConfiguration` | [x] No changes | Stateless query |
| 8 | `getProjectInitialization` | [x] No changes | Stateless query |

**Status:** All tools compatible - API key filtering works per-request

---

### FlavorGroupTools.java (3 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `listFlavorGroups` | [x] No changes | Stateless query |
| 2 | `getFlavorsGroup` | [x] No changes | Stateless query |
| 3 | `getFlavorGroupStatistics` | [x] No changes | Stateless query |

**Status:** All tools compatible - no state dependencies

---

### InitializrTools.java (5 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `initializrGetBootVersions` | [x] No changes | Stateless query, cached |
| 2 | `initializrGetDependencyCategories` | [x] No changes | Stateless query, cached |
| 3 | `initializrSearchDependencies` | [x] No changes | Stateless query |
| 4 | `initializrGetDependency` | [x] No changes | Stateless query |
| 5 | `initializrCheckCompatibility` | [x] No changes | Stateless query |

**Status:** All tools compatible - Caffeine cache works per-request

---

### JavadocTools.java (4 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `listJavadocLibraries` | [x] No changes | Stateless query |
| 2 | `searchJavadocs` | [x] No changes | Stateless query |
| 3 | `getClassDoc` | [x] No changes | Stateless query |
| 4 | `getPackageDoc` | [x] No changes | Stateless query |

**Status:** All tools compatible - no state dependencies

---

### WikiTools.java (2 tools)

| # | Tool Name | Migration Impact | Notes |
|---|-----------|------------------|-------|
| 1 | `getWikiReleaseNotes` | [x] No changes | Stateless query |
| 2 | `getWikiMigrationGuide` | [x] No changes | Stateless query |

**Status:** All tools compatible - no state dependencies

---

### Summary: All 46 Tools Compatible

| Category | Tools | Compatible | Action Required |
|----------|-------|------------|-----------------|
| Documentation | 12 | 12 | None |
| Migration | 7 | 7 | None |
| Language Evolution | 7 | 7 | None |
| Flavors | 8 | 8 | None |
| Flavor Groups | 3 | 3 | None |
| Initializr | 5 | 5 | None |
| Javadoc | 4 | 4 | None |
| Wiki | 2 | 2 | None |
| **Total** | **46** | **46** | **None** |

**Rationale:** All tools are designed as stateless queries against the PostgreSQL database. They:
- Don't maintain conversation state
- Don't require client-initiated requests (elicitation, sampling)
- Use per-request authentication (API key)
- Return complete results in single responses

---

## Part 5: Claude Code Integration Impact

### Current Claude Code Configuration

```json
{
  "mcpServers": {
    "spring": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/mcp/spring/sse"],
      "env": {
        "API_KEY": "your-api-key"
      }
    }
  }
}
```

### New Claude Code Configuration (Streamable-HTTP)

```json
{
  "mcpServers": {
    "spring": {
      "type": "streamable-http",
      "url": "http://localhost:8080/mcp/spring",
      "headers": {
        "X-API-Key": "your-api-key"
      }
    }
  }
}
```

### Configuration Changes Summary

| Aspect | Before (SSE) | After (Stateless) |
|--------|--------------|-------------------|
| Transport | `mcp-remote` wrapper | Native `streamable-http` |
| Endpoint | `/mcp/spring/sse` | `/mcp/spring` |
| Connection | Persistent SSE | Per-request HTTP |
| Authentication | Via env/query | Via headers |
| Session | Maintained | Stateless |

### Client Compatibility

| Client | SSE Support | Streamable-HTTP Support |
|--------|-------------|-------------------------|
| Claude Code | Yes (via mcp-remote) | Yes (native) |
| Claude Desktop | Yes (via mcp-remote) | Yes (native, 2025+) |
| MCP Inspector | Yes | Yes |
| Custom clients | Requires SSE | Standard HTTP |

### Benefits for Claude Code Users

1. **Simpler Configuration:** No need for `mcp-remote` wrapper
2. **Better Reliability:** Standard HTTP vs long-lived SSE connections
3. **Load Balancing:** Can distribute requests across server instances
4. **Firewall Friendly:** Standard HTTP/HTTPS ports
5. **Debugging:** Standard HTTP request/response cycle

### Migration Steps for Claude Code Users

1. Update Claude Code configuration
2. Change transport type to `streamable-http`
3. Update endpoint URL (remove `/sse` suffix)
4. Move API key to headers section
5. Restart Claude Code

---

## Part 6: Implementation Plan

### Phase 1: Dependency Changes

**GOOD NEWS:** The project already uses `spring-ai-starter-mcp-server-webmvc` (build.gradle line 65):

```gradle
// Already present - NO CHANGES NEEDED
implementation "org.springframework.ai:spring-ai-starter-mcp-server-webmvc:${springAiVersion}"
```

**No dependency changes required!**

### Phase 2: Configuration Changes

**application.yml:**
```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE  # NOT STATELESS - preserves masquerading!
        name: "spring-documentation-server"
        version: "1.8.0"
        type: SYNC
        instructions: "Spring Documentation MCP Server providing 46 tools for Spring ecosystem"
        request-timeout: 60s
        # Enable tool change notifications for masquerading
        tool-change-notification: true
        resource-change-notification: false
        prompt-change-notification: false
        capabilities:
          tool: true
          resource: false
          prompt: false
          completion: false
        streamable-http:
          mcp-endpoint: /mcp/spring
          keep-alive-interval: 30s
          disallow-delete: false
        annotation-scanner:
          enabled: false
```

### Phase 3: Remove SSE-Specific Code

**Files to review/modify:**
- `McpConfig.java` - Remove SSE-specific beans
- `SecurityConfig.java` - Update endpoint patterns
- `ApiKeyAuthenticationFilter.java` - Update for single endpoint
- Remove SSE heartbeat/timeout configuration

### Phase 4: Update Security Configuration

```java
@Bean
public SecurityFilterChain mcpFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/mcp/**")
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/mcp/spring").permitAll()  // Single endpoint
        )
        .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### Phase 5: Update UI Templates (SSE → Streamable-HTTP)

**Files with SSE endpoint references that need updating:**

| File | Line(s) | Current Content | New Content |
|------|---------|-----------------|-------------|
| `templates/dashboard.html` | 69, 73 | `/mcp/spring/sse` | `/mcp/spring` |
| `templates/dashboard/index.html` | 665, 684 | `/mcp/spring/sse` | `/mcp/spring` |
| `templates/settings/index.html` | 254 | `/mcp/spring/sse` | `/mcp/spring` |

**Search command to find all occurrences:**
```bash
grep -rn "mcp/spring/sse" src/main/resources/templates/
```

**Note:** The `templates/sync/index.html` uses SSE for real-time sync progress display. This is **internal SSE** for the UI (not MCP transport) and should remain unchanged.

### Phase 6: Version Bump to 1.8.0

**Files requiring version updates (from VERSIONS.md):**

| File | Location | Current | New |
|------|----------|---------|-----|
| `build.gradle` | Line 8: `version` property | `1.7.1` | `1.8.0` |
| `application.yml` | `info.app.version` | `1.7.1` | `1.8.0` |
| `application.yml` | `spring.ai.mcp.server.version` | `1.7.1` | `1.8.0` |
| `build-container.sh` | `APP_VERSION` | `1.7.1` | `1.8.0` |
| `docker-compose-all.yaml` | Image tag | `1.7.1` | `1.8.0` |
| `README.md` | Version header, jar filename | `1.7.1` | `1.8.0` |
| `VERSIONS.md` | Current Versions table | `1.7.1` | `1.8.0` |

### Phase 7: Update CHANGELOG.md

Add new version entry following Keep a Changelog format:

```markdown
## [1.8.0] - 2026-01-XX

### Added
- **MCP Streamable-HTTP Transport**: Migrated from SSE to Streamable-HTTP protocol (MCP spec 2025-11-25)
    - **Single Unified Endpoint**: `/mcp/spring` replaces `/mcp/spring/sse` and `/mcp/spring/messages`
    - **Protocol Version**: Updated to `2025-11-25` (from `2025-06-18`)
    - **Tool Change Notifications**: Real-time tool visibility updates via `tool-change-notification: true`
    - **Session Management**: `Mcp-Session-Id` header for stateful session handling
    - **Keep-Alive**: Configurable keep-alive interval for connection maintenance

### Changed
- MCP endpoint changed from `/mcp/spring/sse` to `/mcp/spring`
- Claude Code configuration now uses `type: "streamable-http"` instead of `type: "sse"`
- Protocol header updated from `MCP-Protocol-Version: 2025-06-18` to `2025-11-25`

### Technical Details
- Uses Spring AI `spring-ai-starter-mcp-server-webmvc` with `protocol: STREAMABLE`
- Full backwards compatibility with MCP Tool Masquerading feature (`/mcp-tools`)
- Runtime tool modifications (`addTool`/`removeTool`) continue to work
- All 46 MCP tools unchanged - no tool code modifications needed
```

**Update Version Summary table at end of CHANGELOG.md:**

```markdown
| 1.8.0 | 2026-01-XX | MCP Streamable-HTTP Transport (replaces SSE) |
```

### Phase 8: Update README.md

**Section: "Using with Claude Code" → "MCP Configuration"**

Change from:
```json
{
  "mcpServers": {
    "spring": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/spring/sse",
      "headers": {
        "X-API-Key": "YOUR_API_KEY_HERE"
      }
    }
  }
}
```

To:
```json
{
  "mcpServers": {
    "spring": {
      "type": "streamable-http",
      "url": "http://localhost:8080/mcp/spring",
      "headers": {
        "X-API-Key": "YOUR_API_KEY_HERE"
      }
    }
  }
}
```

**Section: "Quick Start" → Item 3**

Change:
- `MCP SSE Endpoint: http://localhost:8080/mcp/spring/sse`

To:
- `MCP Endpoint: http://localhost:8080/mcp/spring`

**Section: "What is this?"**

Change:
- `MCP Server`: SSE-based protocol implementation → Streamable-HTTP protocol implementation

**Section: "Application Configuration"**

Update the YAML example to show STREAMABLE configuration.

### Phase 9: Update VERSIONS.md Changelog

Add entry to changelog section at bottom:

```markdown
### v1.8.0 (2026-01-XX)
- MCP Streamable-HTTP Transport Migration (replaces SSE)
- Single unified endpoint: `/mcp/spring` (was `/mcp/spring/sse` + `/mcp/spring/messages`)
- Protocol version updated to 2025-11-25
- Tool change notifications enabled for MCP Tool Masquerading
- All 46 MCP tools unchanged
```

### Phase 10: Testing

1. **Unit Tests:** Verify tool registration with STREAMABLE protocol
2. **Integration Tests:** Test with Testcontainers
3. **Manual Testing:** Claude Code integration with new config
4. **MCP Inspector:** Protocol compliance verification
5. **Tool Masquerading Test:** Verify `/mcp-tools` UI still works

---

## Part 7: Risk Assessment

### Low Risk
- All 46 tools are stateless by design
- No breaking changes to tool interfaces
- Database interactions unchanged

### Medium Risk
- Client configuration changes required
- Existing SSE connections will break during migration
- Need to coordinate with Claude Code users

### Mitigation Strategies

1. **Dual Endpoint Support (Temporary):**
   - Keep SSE endpoint during transition
   - Add Streamable-HTTP endpoint
   - Document both options

2. **Version Bump:**
   - Release as v1.8.0
   - Clear changelog entry
   - Migration guide

3. **Backwards Compatibility:**
   - Optional: Keep SSE as fallback
   - Configurable via property

---

## Part 8: Version Compatibility Matrix

| Component | Current | After Migration |
|-----------|---------|-----------------|
| Spring Boot | 3.5.9 | 3.5.9 |
| Spring AI | 1.1.2 | 1.1.2+ |
| MCP Protocol | 2025-06-18 | 2025-11-25 |
| Java | 25 | 25 |
| PostgreSQL | 18 | 18 |

---

## Part 9: Migration Timeline

### Sprint 1: Foundation
- [ ] Update dependencies
- [ ] Update configuration
- [ ] Basic stateless endpoint working

### Sprint 2: Security & Auth
- [ ] Update security configuration
- [ ] Verify API key authentication
- [ ] Test CORS configuration

### Sprint 3: Testing & Documentation
- [ ] Integration tests
- [ ] Claude Code testing
- [ ] Documentation updates

### Sprint 4: Release
- [ ] Final testing
- [ ] Release v1.8.0
- [ ] Monitor for issues

---

## Appendix A: Tool Registration Code

Current tool registration in `McpToolsConfig.java`:

```java
@Bean("customToolSpecs")
public List<SyncToolSpecification> customToolSpecs(
    SpringDocumentationTools springDocumentationTools,
    FlavorTools flavorTools,
    FlavorGroupTools flavorGroupTools,
    JavadocTools javadocTools,
    InitializrTools initializrTools,
    MigrationTools migrationTools,
    LanguageEvolutionTools languageEvolutionTools,
    WikiTools wikiTools
) {
    // Tool specifications built using MethodToolCallbackProvider
    // No changes required for stateless migration
}
```

---

## Appendix B: References

1. [MCP Specification 2025-11-25 - Streamable HTTP](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#streamable-http)
2. [Spring AI MCP Stateless Server](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-stateless-server-boot-starter-docs.html)
3. [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
4. [Claude Code MCP Integration](https://docs.anthropic.com/claude/docs/claude-code-mcp)

---

---

## Quick Start Summary

### Prerequisites
- Spring Boot 3.5.9 (current)
- Spring AI 1.1.2 (current)
- `spring-ai-starter-mcp-server-webmvc` dependency (already present)

### Minimal Changes Required

1. **application.yml** - Add `protocol: STREAMABLE` (NOT STATELESS!) with `tool-change-notification: true`
2. **SecurityConfig.java** - Add `/mcp/spring` endpoint to permitAll and CSRF ignore
3. **McpProtocolHeaderFilter.java** - Update protocol version to `2025-11-25`
4. **Documentation** - Update Claude Code configuration instructions

### No Tool Changes Required
All 46 MCP tools are stateless by design and require no modifications.

### Tool Masquerading Preserved
Using `STREAMABLE` protocol (instead of `STATELESS`) preserves:
- Runtime tool enable/disable via `/mcp-tools` UI
- `notifyToolsListChanged()` functionality
- `McpSyncServer.addTool()` / `removeTool()` operations

---

**Document Version:** 1.1 (Updated for STREAMABLE protocol)
**Created:** 2026-01-09
**Author:** Migration Planning Agent

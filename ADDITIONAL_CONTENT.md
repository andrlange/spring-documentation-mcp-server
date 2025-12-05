# Spring Documentation MCP Server - Technical Reference

This document contains technical details, development information, and API specifications for the Spring Documentation MCP Server. For feature documentation and usage guides, see [README.md](README.md).

## Table of Contents

- [Testing the MCP Server](#testing-the-mcp-server)
  - [MCP Inspector](#mcp-inspector)
  - [Testing Individual Tools](#testing-individual-tools)
- [Technology Stack](#technology-stack)
- [Database Schema](#database-schema)
- [Development](#development)
- [API Endpoints](#api-endpoints)
- [MCP Tools Parameter Reference](#mcp-tools-parameter-reference)

---

## Testing the MCP Server

### MCP Inspector

MCP Inspector is an excellent tool for testing and debugging MCP servers. It provides a visual interface to test all MCP capabilities.

#### Install and Run MCP Inspector

```bash
npx @modelcontextprotocol/inspector
```

This will start the MCP Inspector and output something like:

```
Starting MCP inspector...
Proxy server listening on localhost:6277
Session token: 3c672c3389d66786f32ffe2f90d6d2116634bef316a09198fb6e933a5eeefe2b

MCP Inspector is up and running at:
http://localhost:6274/?MCP_PROXY_AUTH_TOKEN=3c672c3389d66786f32ffe2f90d6d2116634bef316a09198fb6e933a5eeefe2b
```

#### Configure MCP Inspector

1. Open the MCP Inspector URL in your browser
2. Select **"SSE"** as the Transport Type
3. Enter the **URL**: `http://localhost:8080/mcp/spring/sse`
4. Add **Headers** (click "Add Header"):
   - **Header Name**: `X-API-Key`
   - **Header Value**: `smcp_your_api_key_here` (your actual API key)
5. Click **"Connect"**

Once connected, you can:
- **List Tools**: View all 34 available MCP tools
- **Test Tools**: Execute tools with parameters and see responses
- **View Logs**: See real-time communication between client and server
- **Debug Issues**: Inspect request/response payloads

### Testing Individual Tools

#### Example: Testing searchSpringDocs Tool

In MCP Inspector:
1. Navigate to the **"Tools"** tab
2. Select **"searchSpringDocs"** tool
3. Fill in parameters:
   ```json
   {
     "query": "autoconfiguration",
     "project": "spring-boot",
     "version": "3.5.7"
   }
   ```
4. Click **"Execute"**
5. View the typed response with all documentation results

---

## Technology Stack

### Core Framework
- **Spring Boot**: 3.5.8
- **Java**: 21+ (25 recommended)
- **Build Tool**: Gradle 9.2.0

### MCP Protocol
- **Spring AI MCP Server**: 1.1.1
- **Protocol**: Server-Sent Events (SSE)
- **Auto-discovery**: `@Tool` annotations

### Data Layer
- **Database**: PostgreSQL 18
- **ORM**: Spring Data JPA / Hibernate 6.6
- **Migrations**: Flyway
- **Full-Text Search**: PostgreSQL tsvector + tsquery

### UI Layer
- **Template Engine**: Thymeleaf 3.4
- **Layout**: Thymeleaf Layout Dialect
- **CSS Framework**: Bootstrap 5
- **Security**: Spring Security 6 (Spring Security Extras for Thymeleaf)

### Documentation Fetching
- **HTML Parsing**: JSoup 1.21.2
- **JavaScript Support**: HtmlUnit 4.19.0
- **HTML to Markdown**: Flexmark 0.64.8
- **AsciiDoc Processing**: AsciidoctorJ 3.0.1 (for GitHub source documentation)
- **HTTP Client**: Spring WebFlux WebClient

### Security & Monitoring
- **Authentication**: Spring Security Basic Auth + API Key
- **Session Management**: HTTP Session
- **Health Checks**: Spring Boot Actuator
- **Logging**: Logback

---

## Database Schema

### Core Tables

- **spring_projects** - Spring ecosystem projects (Boot, Framework, Data, etc.)
- **project_versions** - Version tracking with state (STABLE, RC, SNAPSHOT)
- **documentation_types** - Types of documentation (Reference, API, Guide, Tutorial)
- **documentation_links** - Links to documentation resources
- **documentation_content** - Cached documentation with full-text search index
- **code_examples** - Code snippets with tags and metadata
- **users** - Application users with roles
- **api_keys** - API keys for MCP authentication
- **flavor_groups** - Team-based flavor organization
- **group_api_key_members** - API key memberships in groups
- **group_flavors** - Flavor assignments to groups
- **settings** - Application-wide settings

### Full-Text Search

PostgreSQL tsvector is used for efficient full-text search:

```sql
-- Search query example
SELECT dl.id
FROM documentation_content dc
JOIN documentation_links dl ON dc.link_id = dl.id
WHERE dc.indexed_content @@ plainto_tsquery('english', 'spring boot autoconfiguration')
ORDER BY ts_rank_cd(dc.indexed_content, plainto_tsquery('english', 'spring boot autoconfiguration')) DESC
```

---

## Development

### Running Tests
```bash
./gradlew test
```

### Running with Dev Tools
```bash
./gradlew bootRun
# Dev tools will auto-reload on file changes
```

### Database Migrations

View migration status:
```bash
./gradlew flywayInfo
```

Migrations are applied automatically on startup. Manual migration:
```bash
./gradlew flywayMigrate
```

### Cleaning Build
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

---

## API Endpoints

### Web UI
- `GET /` - Dashboard
- `GET /projects` - Projects list
- `GET /versions` - Versions list
- `GET /documentation` - Documentation list with search
- `GET /examples` - Code examples
- `GET /recipes` - Migration recipes
- `GET /languages` - Language evolution tracking
- `GET /flavors` - Flavors management
- `GET /groups` - Flavor groups management
- `GET /users` - User management (Admin only)
- `GET /settings` - Application settings

### REST API
- `GET /api/documentation/{id}/content` - Get documentation content
- `GET /api/documentation/{id}/markdown` - Get documentation as Markdown
- `POST /api/sync/comprehensive` - Trigger comprehensive sync
- `POST /api/sync/projects` - Sync projects
- `POST /api/sync/versions` - Sync versions

### MCP Protocol
- **SSE Endpoint**: `/mcp/spring/sse` (connection endpoint)
- **Message Endpoint**: `/mcp/spring/messages` (messaging endpoint)
- **Authentication**: API Key (X-API-Key header, Bearer token, or query parameter)

### Health & Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Application info
- `GET /actuator/metrics` - Metrics

---

## MCP Tools Parameter Reference

This section provides detailed parameter documentation for all 34 MCP tools.

### Documentation Tools (10 tools)

#### 1. searchSpringDocs
Search across all Spring documentation with optional filters.

**Parameters**:
- `query` (required): Search term
- `project` (optional): Project slug (e.g., `spring-boot`)
- `version` (optional): Version string (e.g., `3.5.8`)
- `docType` (optional): Documentation type (e.g., `reference`, `api`)

```json
{
  "query": "autoconfiguration",
  "project": "spring-boot",
  "version": "3.5.8"
}
```

#### 2. getSpringVersions
List all available versions for a Spring project.

**Parameters**:
- `project` (required): Project slug

```json
{
  "project": "spring-boot"
}
```

#### 3. listSpringProjects
List all available Spring projects.

**No parameters required**.

#### 4. getDocumentationByVersion
Get all documentation for a specific project version.

**Parameters**:
- `project` (required): Project slug
- `version` (required): Version string

```json
{
  "project": "spring-framework",
  "version": "6.2.1"
}
```

#### 5. getCodeExamples
Search code examples with filters.

**Parameters**:
- `query` (optional): Search in title/description
- `project` (optional): Project slug
- `version` (optional): Version string
- `language` (optional): Programming language
- `limit` (optional): Max results (default: 10, max: 50)

```json
{
  "query": "REST controller",
  "project": "spring-boot",
  "language": "java",
  "limit": 20
}
```

#### 6. listSpringBootVersions
List all Spring Boot versions with optional filtering.

**Parameters**:
- `state` (optional): Filter by state ('GA', 'RC', 'SNAPSHOT', 'MILESTONE')
- `limit` (optional): Max results (default: 20, max: 100)

```json
{
  "state": "GA",
  "limit": 10
}
```

#### 7. getLatestSpringBootVersion
Get the latest patch version for a specific Spring Boot major.minor version.

**Parameters**:
- `majorVersion` (required): Major version (e.g., 3)
- `minorVersion` (required): Minor version (e.g., 5)

```json
{
  "majorVersion": 3,
  "minorVersion": 5
}
```

#### 8. filterSpringBootVersionsBySupport
Filter Spring Boot versions by support status.

**Parameters**:
- `supportActive` (optional): true for supported, false for end-of-life
- `limit` (optional): Max results (default: 20, max: 100)

```json
{
  "supportActive": true,
  "limit": 20
}
```

#### 9. listProjectsBySpringBootVersion
List all Spring projects compatible with a specific Spring Boot version.

**Parameters**:
- `majorVersion` (required): Spring Boot major version
- `minorVersion` (required): Spring Boot minor version

```json
{
  "majorVersion": 3,
  "minorVersion": 5
}
```

#### 10. findProjectsByUseCase
Search for Spring projects by use case keywords.

**Parameters**:
- `useCase` (required): Use case keyword (e.g., 'data access', 'security', 'messaging')

```json
{
  "useCase": "security"
}
```

---

### OpenRewrite Migration Tools (7 tools)

#### 11. getSpringMigrationGuide
Get comprehensive migration guide for upgrading Spring Boot versions.

**Parameters**:
- `fromVersion` (required): Source Spring Boot version (e.g., '3.5.8')
- `toVersion` (required): Target Spring Boot version (e.g., '4.0.0')

```json
{
  "fromVersion": "3.5.8",
  "toVersion": "4.0.0"
}
```

#### 12. getBreakingChanges
Get list of breaking changes for a specific Spring project version.

**Parameters**:
- `project` (required): Project slug (e.g., 'spring-boot', 'spring-security')
- `version` (required): Target version to check (e.g., '4.0.0')

```json
{
  "project": "spring-boot",
  "version": "4.0.0"
}
```

#### 13. searchMigrationKnowledge
Search migration knowledge base for specific topics.

**Parameters**:
- `searchTerm` (required): Search term (e.g., 'flyway', 'actuator health', '@MockBean')
- `project` (optional): Project to search in (default: 'spring-boot')
- `limit` (optional): Maximum results to return (default: 10)

```json
{
  "searchTerm": "MockBean replacement",
  "project": "spring-boot",
  "limit": 5
}
```

#### 14. getAvailableMigrationPaths
Get list of available target versions for migration.

**Parameters**:
- `project` (required): Project slug (e.g., 'spring-boot')

```json
{
  "project": "spring-boot"
}
```

#### 15. getTransformationsByType
Get transformations filtered by type for a specific migration.

**Parameters**:
- `project` (required): Project slug (e.g., 'spring-boot')
- `version` (required): Target version (e.g., '4.0.0')
- `type` (required): Transformation type (IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG)

```json
{
  "project": "spring-boot",
  "version": "4.0.0",
  "type": "DEPENDENCY"
}
```

#### 16. getDeprecationReplacement
Find the replacement for a deprecated class or method.

**Parameters**:
- `className` (required): Fully qualified deprecated class name
- `methodName` (optional): Deprecated method name (null for entire class deprecation)

```json
{
  "className": "org.springframework.boot.actuate.health.Health",
  "methodName": "status"
}
```

#### 17. checkVersionCompatibility
Check if specific dependencies are compatible with a target Spring Boot version.

**Parameters**:
- `springBootVersion` (required): Target Spring Boot version (e.g., '4.0.0')
- `dependencies` (required): List of dependencies to check (e.g., ['spring-security', 'flyway'])

```json
{
  "springBootVersion": "4.0.0",
  "dependencies": ["spring-security", "flyway", "thymeleaf"]
}
```

---

### Language Evolution Tools (6 tools)

#### 18. getLanguageVersions
List all versions for Java or Kotlin with feature counts and support status.

**Parameters**:
- `language` (required): Language to query ('java' or 'kotlin')

```json
{
  "language": "java"
}
```

#### 19. getLanguageFeatures
Get features for a language version with optional filtering.

**Parameters**:
- `language` (required): Language ('java' or 'kotlin')
- `version` (optional): Specific version (e.g., '21', '1.9')
- `status` (optional): Feature status ('NEW', 'DEPRECATED', 'REMOVED', 'PREVIEW', 'INCUBATING')
- `category` (optional): Feature category (e.g., 'Language', 'API', 'Performance')

```json
{
  "language": "java",
  "version": "21",
  "status": "NEW",
  "category": "Language"
}
```

#### 20. getModernPatterns
Get old vs new code patterns for a specific feature.

**Parameters**:
- `featureId` (required): The ID of the feature to get patterns for

```json
{
  "featureId": 42
}
```

#### 21. getLanguageVersionDiff
Compare features between two versions.

**Parameters**:
- `language` (required): Language ('java' or 'kotlin')
- `fromVersion` (required): Starting version (e.g., '17')
- `toVersion` (required): Target version (e.g., '21')

```json
{
  "language": "java",
  "fromVersion": "17",
  "toVersion": "21"
}
```

#### 22. getSpringBootLanguageRequirements
Get minimum Java and Kotlin versions required for a Spring Boot version.

**Parameters**:
- `springBootVersion` (required): Spring Boot version (e.g., '3.5.8', '4.0.0')

```json
{
  "springBootVersion": "3.5.8"
}
```

#### 23. searchLanguageFeatures
Search language features by keyword across all versions.

**Parameters**:
- `searchTerm` (required): Search keyword (e.g., 'record', 'sealed', 'pattern matching')
- `language` (optional): Filter by language ('java' or 'kotlin')

```json
{
  "searchTerm": "pattern matching",
  "language": "java"
}
```

---

### Flavors Tools (8 tools)

#### 24. searchFlavors
Search company guidelines, patterns, and configurations.

**Parameters**:
- `query` (required): Search term (e.g., 'hexagonal', 'GDPR', 'microservices')
- `category` (optional): Filter by category ('ARCHITECTURE', 'COMPLIANCE', 'AGENTS', 'INITIALIZATION', 'GENERAL')
- `tags` (optional): Filter by tags
- `limit` (optional): Maximum results to return (default: 10)

```json
{
  "query": "hexagonal architecture",
  "category": "ARCHITECTURE",
  "limit": 5
}
```

#### 25. getFlavorByName
Get complete flavor content by unique name.

**Parameters**:
- `uniqueName` (required): The unique identifier of the flavor (e.g., 'hexagonal-architecture')

```json
{
  "uniqueName": "hexagonal-architecture"
}
```

#### 26. getFlavorsByCategory
List all flavors in a specific category.

**Parameters**:
- `category` (required): Category name ('ARCHITECTURE', 'COMPLIANCE', 'AGENTS', 'INITIALIZATION', 'GENERAL')

```json
{
  "category": "ARCHITECTURE"
}
```

#### 27. getArchitecturePatterns
Get architecture patterns for specific technologies.

**Parameters**:
- `slugs` (required): Technology slugs (e.g., ['spring-boot', 'kafka', 'jpa'])

```json
{
  "slugs": ["spring-boot", "microservices"]
}
```

#### 28. getComplianceRules
Get compliance rules by regulatory framework.

**Parameters**:
- `rules` (required): Rule names or framework identifiers (e.g., ['GDPR', 'SOC2', 'PCI-DSS'])

```json
{
  "rules": ["GDPR", "SOC2"]
}
```

#### 29. getAgentConfiguration
Get AI agent configuration for specific use cases.

**Parameters**:
- `useCase` (required): Use case identifier (e.g., 'backend-development', 'ui-development', 'testing')

```json
{
  "useCase": "code-review"
}
```

#### 30. getProjectInitialization
Get project initialization templates.

**Parameters**:
- `useCase` (required): Use case identifier (e.g., 'microservice', 'api-gateway', 'monolith')

```json
{
  "useCase": "microservice"
}
```

#### 31. listFlavorCategories
List all available categories with counts.

**No parameters required**.

---

### Flavor Groups Tools (3 tools)

#### 32. listFlavorGroups
List all accessible flavor groups.

**Parameters**:
- `includePublic` (optional): Include public groups (default: true)
- `includePrivate` (optional): Include private groups where caller is member (default: true)

```json
{
  "includePublic": true,
  "includePrivate": true
}
```

#### 33. getFlavorsGroup
Get all flavors in a specific group.

**Parameters**:
- `groupName` (required): Unique name of the group (e.g., 'engineering-standards', 'payment-platform')

```json
{
  "groupName": "engineering-standards"
}
```

#### 34. getFlavorGroupStatistics
Get statistics about flavor groups.

**No parameters required**.

---

## Troubleshooting

### Java Version Issues

Error: "Unsupported class file major version"

**Solution**:
```bash
java -version  # Verify Java 25
echo $JAVA_HOME  # Ensure JAVA_HOME points to Java 25
```

### Database Connection Issues

**Check PostgreSQL**:
```bash
docker-compose ps
docker-compose logs postgres
```

**Verify connection**:
```bash
psql -h localhost -U postgres -d spring_mcp
# Password: postgres
```

### Build Issues

**Clean and rebuild**:
```bash
./gradlew clean build --refresh-dependencies
```

### Port Already in Use

**Kill process on port 8080**:
```bash
lsof -ti :8080 | xargs kill -9
```

### MCP Connection Issues

1. **Verify application is running**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Check MCP endpoint with API key**:
   ```bash
   curl -H "X-API-Key: your_api_key" http://localhost:8080/mcp/spring/sse
   ```

3. **Test with MCP Inspector**:
   ```bash
   npx @modelcontextprotocol/inspector
   ```
   Then configure with URL: `http://localhost:8080/mcp/spring/sse` and your API key header.

4. **Review application logs**:
   ```bash
   tail -f logs/spring-boot-documentation-mcp-server.log
   ```

5. **Check registered tools**:
   ```bash
   grep "Registered tools" logs/spring-boot-documentation-mcp-server.log
   # Should show: Registered tools: 34
   ```

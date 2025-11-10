# Spring Documentation MCP Server

A comprehensive Spring Boot application that serves as a Model Context Protocol (MCP) Server, providing AI assistants with full-text searchable access to Spring ecosystem documentation via Server-Sent Events (SSE).

## What is this?

This MCP server enables AI assistants (like Claude) to search, browse, and retrieve Spring Framework documentation, code examples, and API references. It includes:

- **MCP Server**: SSE-based protocol implementation using Spring AI
- **Documentation Sync**: Automated synchronization from spring.io
- **Full-Text Search**: PostgreSQL-powered search across all Spring documentation
- **Web Management UI**: Thymeleaf-based interface for managing projects, versions, and documentation
- **Code Examples**: Searchable repository of Spring code snippets

## Screenshots

<table>
  <tr>
    <td width="50%">
      <img src="assets/screen-01.png" alt="Dashboard" />
      <p align="center"><b>Dashboard</b> - Overview statistics and quick actions</p>
    </td>
    <td width="50%">
      <img src="assets/screen-02.png" alt="Spring Boot" />
      <p align="center"><b>Spring Boot</b> - Spring Boot project management</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <img src="assets/screen-03.png" alt="Projects" />
      <p align="center"><b>Projects</b> - All Spring projects overview</p>
    </td>
    <td width="50%">
      <img src="assets/screen-04.png" alt="Project Details" />
      <p align="center"><b>Project Details</b> - Spring Batch project details</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <img src="assets/screen-05.png" alt="Versions" />
      <p align="center"><b>Versions</b> - Version management and tracking</p>
    </td>
    <td width="50%">
      <img src="assets/screen-06.png" alt="Documentation" />
      <p align="center"><b>Documentation</b> - Full-text search and browse</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <img src="assets/screen-07.png" alt="Settings" />
      <p align="center"><b>Settings</b> - Application configuration and sync controls</p>
    </td>
    <td width="50%">
      <img src="assets/screen-08.png" alt="Documentation Markdown Expansion" />
      <p align="center"><b>Documentation Markdown</b> - Expanded Spring Batch documentation content</p>
    </td>
  </tr>
</table>

## Current Status

### ✅ Fully Implemented Features

#### MCP Tools (5 tools available)
1. **searchSpringDocs** - Full-text search across all Spring documentation with filters
2. **getSpringVersions** - List available versions for any Spring project
3. **listSpringProjects** - Browse all available Spring projects
4. **getDocumentationByVersion** - Get all documentation for a specific version
5. **getCodeExamples** - Search code examples with language/project/version filters

#### Web Management UI
- **Dashboard** - Overview statistics and recent updates
- **Projects** - Manage Spring projects (Spring Boot, Framework, Data, Security, Cloud, etc.)
- **Versions** - Version management with latest/default marking
- **Documentation** - Browse and search documentation links with full-text search
- **Code Examples** - Code snippet library with tagging
- **Users** - User management with role-based access
- **Settings** - Application configuration and feature toggles
- **Authentication** - Spring Security with session management

#### Documentation Sync Services
- Automated sync from spring.io/projects
- Version detection and tracking
- Spring Boot version synchronization
- Project relationship mapping
- Spring Generations support
- Scheduled updates (configurable cron)
- Bootstrap data loading

#### Database Features
- PostgreSQL 18 with full-text search (tsvector)
- Flyway migrations for version control
- Optimized indexes for search performance
- Support for relationships and metadata

## Prerequisites

**IMPORTANT**: This project requires **Java 25** (LTS).

### Install Java 25

#### Option 1: SDKMAN (Recommended)
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash

# Install Java 25
sdk install java 25.0.1-tem

# Use Java 25
sdk use java 25.0.1-tem
```

#### Option 2: Download from Adoptium
- Download from: https://adoptium.net/temurin/releases/?version=25
- Install and set JAVA_HOME

#### Option 3: Homebrew (macOS)
```bash
brew install openjdk@25
```

### Verify Installation
```bash
java -version
# Should show: openjdk version "25"
```

## Quick Start

### 1. Start PostgreSQL Database
```bash
docker-compose up -d postgres
```

### 2. Verify Database is Running
```bash
docker-compose ps
# You should see spring-mcp-db with status "Up" and "healthy"
```

### 3. Build the Application
```bash
./gradlew clean build
```

### 4. Run the Application
```bash
java -jar build/libs/spring-mcp-server-1.0.0.jar
```

Or using Gradle:
```bash
./gradlew bootRun
```

### 5. Access the Application

- **Web UI**: http://localhost:8080
- **Login**: Username: `admin`, Password: `admin`
- **MCP Endpoint**: http://localhost:8080/mcp (SSE endpoint auto-configured by Spring AI)

## Using the MCP Server with Claude Code

### Configuration

Add to your Claude Desktop or Claude Code MCP configuration:

```json
{
  "mcpServers": {
    "spring-docs": {
      "command": "bash",
      "args": [
        "-c",
        "curl -N -u admin:admin http://localhost:8080/mcp"
      ]
    }
  }
}
```

**Note**: The Spring AI MCP server auto-configures the SSE endpoint at `/mcp`. Authentication is required via Basic Auth.

### Available MCP Tools

Once connected, the following tools are available to AI assistants:

#### 1. searchSpringDocs
Search across all Spring documentation with optional filters.

**Parameters**:
- `query` (required): Search term
- `project` (optional): Project slug (e.g., `spring-boot`)
- `version` (optional): Version string (e.g., `3.5.7`)
- `docType` (optional): Documentation type (e.g., `reference`, `api`)

**Example**:
```json
{
  "query": "autoconfiguration",
  "project": "spring-boot",
  "version": "3.5.7"
}
```

#### 2. getSpringVersions
List all available versions for a Spring project.

**Parameters**:
- `project` (required): Project slug

**Example**:
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

**Example**:
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

**Example**:
```json
{
  "query": "REST controller",
  "project": "spring-boot",
  "language": "java",
  "limit": 20
}
```

## Configuration

### Environment Variables

```bash
# Database Configuration
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=spring_mcp
export DB_USER=postgres
export DB_PASSWORD=postgres

# Security
export ADMIN_USER=admin
export ADMIN_PASSWORD=changeme

# Server
export SERVER_PORT=8080

# Documentation Bootstrap
export BOOTSTRAP_DOCS=false  # Set to true to load sample data on startup
```

### Application Configuration

Key configuration in `src/main/resources/application.yml`:

```yaml
mcp:
  server:
    name: "Spring Documentation MCP Server"
    version: "1.0.0"

  documentation:
    fetch:
      enabled: true
      schedule: "0 0 2 * * ?"  # Daily at 2 AM

    bootstrap:
      enabled: ${BOOTSTRAP_DOCS:false}
      on-startup: false
      projects:
        - spring-boot
        - spring-framework
        - spring-data
        - spring-security
        - spring-cloud

    search:
      max-results: 50
      default-limit: 20
```

## Technology Stack

### Core Framework
- **Spring Boot**: 3.5.7
- **Java**: 25 (LTS)
- **Build Tool**: Gradle 9.2.0

### MCP Protocol
- **Spring AI MCP Server**: 1.0.3
- **Protocol**: Server-Sent Events (SSE)
- **Auto-discovery**: `@Tool` annotations

### Data Layer
- **Database**: PostgreSQL 18
- **ORM**: Spring Data JPA / Hibernate 6.6
- **Migrations**: Flyway
- **Full-Text Search**: PostgreSQL tsvector + tsquery

### UI Layer
- **Template Engine**: Thymeleaf 3.1
- **Layout**: Thymeleaf Layout Dialect
- **CSS Framework**: Bootstrap 5
- **Security**: Spring Security 6 (Spring Security Extras for Thymeleaf)

### Documentation Fetching
- **HTML Parsing**: JSoup 1.21.2
- **JavaScript Support**: HtmlUnit 4.18.0
- **HTML to Markdown**: Flexmark 0.64.8
- **HTTP Client**: Spring WebFlux WebClient

### Security & Monitoring
- **Authentication**: Spring Security Basic Auth
- **Session Management**: HTTP Session
- **Health Checks**: Spring Boot Actuator
- **Logging**: Logback

## Project Structure

```
spring-mcp-server/
├── src/main/java/com/spring/mcp/
│   ├── config/                    # Spring configuration
│   │   ├── SecurityConfig.java    # Security & authentication
│   │   └── WebConfig.java         # Web MVC configuration
│   ├── controller/
│   │   ├── api/                   # REST API controllers
│   │   │   ├── DocumentationApiController.java
│   │   │   └── McpTestController.java
│   │   └── web/                   # Web UI controllers
│   │       ├── DashboardController.java
│   │       ├── ProjectsController.java
│   │       ├── VersionsController.java
│   │       ├── DocumentationController.java
│   │       ├── ExamplesController.java
│   │       ├── UsersController.java
│   │       └── SettingsController.java
│   ├── model/
│   │   ├── entity/                # JPA entities
│   │   │   ├── SpringProject.java
│   │   │   ├── ProjectVersion.java
│   │   │   ├── DocumentationType.java
│   │   │   ├── DocumentationLink.java
│   │   │   ├── DocumentationContent.java
│   │   │   ├── CodeExample.java
│   │   │   ├── User.java
│   │   │   └── Settings.java
│   │   └── dto/                   # Data Transfer Objects
│   ├── repository/                # Spring Data JPA repositories
│   │   ├── SpringProjectRepository.java
│   │   ├── ProjectVersionRepository.java
│   │   ├── DocumentationLinkRepository.java
│   │   ├── DocumentationContentRepository.java
│   │   ├── CodeExampleRepository.java
│   │   └── UserRepository.java
│   ├── service/
│   │   ├── tools/
│   │   │   └── SpringDocumentationTools.java  # MCP @Tool methods
│   │   ├── documentation/
│   │   │   ├── DocumentationService.java
│   │   │   └── DocumentationFetchService.java
│   │   ├── sync/                  # Documentation sync services
│   │   │   ├── ComprehensiveSyncService.java
│   │   │   ├── DocumentationSyncService.java
│   │   │   ├── ProjectSyncService.java
│   │   │   ├── SpringBootVersionSyncService.java
│   │   │   ├── SpringProjectPageCrawlerService.java
│   │   │   └── SpringGenerationsSyncService.java
│   │   ├── version/
│   │   │   └── VersionDetectionService.java
│   │   ├── bootstrap/
│   │   │   └── DocumentationBootstrapService.java
│   │   └── mcp/
│   │       └── McpRequestLoggerService.java
│   └── SpringMcpServerApplication.java
├── src/main/resources/
│   ├── db/migration/              # Flyway database migrations
│   │   ├── V1__consolidated_initial_schema.sql
│   │   ├── V2__update_user_roles.sql
│   │   └── V3__create_settings_table.sql
│   ├── templates/                 # Thymeleaf templates
│   │   ├── layouts/               # Page layouts
│   │   ├── fragments/             # Reusable fragments
│   │   ├── dashboard/
│   │   ├── projects/
│   │   ├── versions/
│   │   ├── documentation/
│   │   ├── examples/
│   │   ├── users/
│   │   └── settings/
│   ├── static/                    # CSS, JS, images
│   │   ├── css/
│   │   ├── js/
│   │   └── images/
│   └── application.yml            # Application configuration
├── docker-compose.yml             # PostgreSQL service
└── build.gradle                   # Gradle build configuration
```

## Database Schema

### Core Tables

- **spring_projects** - Spring ecosystem projects (Boot, Framework, Data, etc.)
- **project_versions** - Version tracking with state (STABLE, RC, SNAPSHOT)
- **documentation_types** - Types of documentation (Reference, API, Guide, Tutorial)
- **documentation_links** - Links to documentation resources
- **documentation_content** - Cached documentation with full-text search index
- **code_examples** - Code snippets with tags and metadata
- **users** - Application users with roles
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

## API Endpoints

### Web UI
- `GET /` - Dashboard
- `GET /projects` - Projects list
- `GET /versions` - Versions list
- `GET /documentation` - Documentation list with search
- `GET /examples` - Code examples
- `GET /users` - User management (Admin only)
- `GET /settings` - Application settings

### REST API
- `GET /api/documentation/{id}/content` - Get documentation content
- `GET /api/documentation/{id}/markdown` - Get documentation as Markdown
- `POST /api/sync/comprehensive` - Trigger comprehensive sync
- `POST /api/sync/projects` - Sync projects
- `POST /api/sync/versions` - Sync versions

### MCP Protocol
- **SSE Endpoint**: `/mcp` (auto-configured by Spring AI)
- **Authentication**: Basic Auth (username/password)

### Health & Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Application info
- `GET /actuator/metrics` - Metrics

## Features in Detail

### Documentation Synchronization

The system can automatically sync documentation from spring.io:

1. **Project Discovery**: Crawls spring.io/projects to discover projects
2. **Version Detection**: Detects available versions for each project
3. **Documentation Fetching**: Downloads and parses documentation HTML
4. **Content Conversion**: Converts HTML to searchable Markdown
5. **Indexing**: Builds PostgreSQL full-text search index
6. **Scheduling**: Runs daily updates (configurable)

Trigger manual sync via Web UI:
- Settings page → "Sync Documentation" button
- Or use REST API: `POST /api/sync/comprehensive`

### Full-Text Search

Search features:
- Natural language queries via `plainto_tsquery`
- Relevance ranking with `ts_rank_cd`
- Filter by project, version, documentation type
- Pagination support
- Highlighted snippets (planned)

### Code Examples

Manage code snippets:
- Title, description, code snippet
- Language tagging (Java, Kotlin, Groovy)
- Category organization
- Tags for discoverability
- Version association
- Source URL tracking

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

1. Verify application is running: `curl http://localhost:8080/actuator/health`
2. Check authentication: `curl -u admin:admin http://localhost:8080/mcp`
3. Review logs: `tail -f logs/spring-mcp-server.log`

## Roadmap

### Completed ✅
- [x] Spring Boot 3.5.7 project setup
- [x] PostgreSQL database with Docker Compose
- [x] Flyway migrations
- [x] Entity models and repositories
- [x] Spring Security with Basic Auth
- [x] Thymeleaf UI with Bootstrap 5
- [x] MCP Server with Spring AI
- [x] 5 MCP tools implemented
- [x] Full-text search with PostgreSQL
- [x] Documentation sync services
- [x] Version detection and tracking
- [x] Web management UI (all pages)
- [x] User management
- [x] Settings management
- [x] Code examples repository

### In Progress 🚧
- [ ] Comprehensive documentation coverage for all Spring projects
- [ ] Enhanced search with highlighting and snippets
- [ ] More code examples across Spring ecosystem
- [ ] Performance optimization for large result sets

### Planned 📋
- [ ] Semantic search using embeddings
- [ ] Version comparison and diff
- [ ] Migration guides between versions
- [ ] Export features (PDF, Markdown)
- [ ] Analytics and usage tracking
- [ ] Multi-language documentation support
- [ ] Offline mode
- [ ] Air-Gapped Replication Mode for cascaded setups
- [ ] Additional MCP resources (prompts, completions)
- [ ] GitHub integration for code samples
- [ ] Spring Initializr integration

## Contributing

This is a demonstration/reference MCP server implementation. Contributions are welcome!

Areas for contribution:
- Additional Spring project coverage
- Enhanced search algorithms
- UI/UX improvements
- Performance optimizations
- Documentation
- Test coverage

## License

This project is part of the Spring MCP Server initiative.

## Resources

- **Spring AI MCP Server Docs**: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- **MCP Protocol Specification**: https://spec.modelcontextprotocol.io/
- **Spring Documentation**: https://spring.io/projects


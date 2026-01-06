package com.spring.mcp.config;

import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.model.enums.McpToolGroup;
import com.spring.mcp.repository.McpToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Initializes MCP tool configurations in the database on application startup.
 * Only populates if the mcp_tools table is empty.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Component
@Order(10) // Run early but after database migrations
@RequiredArgsConstructor
@Slf4j
public class McpToolsInitializer implements ApplicationRunner {

    private final McpToolRepository mcpToolRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, ToolDefinition> tools = getToolDefinitions();
        long existingCount = mcpToolRepository.count();

        if (existingCount == 0) {
            // Fresh install - add all tools
            log.info("Initializing MCP tool configurations...");
            int order = 0;

            for (Map.Entry<String, ToolDefinition> entry : tools.entrySet()) {
                String toolName = entry.getKey();
                ToolDefinition def = entry.getValue();

                McpTool tool = McpTool.builder()
                        .toolName(toolName)
                        .toolGroup(def.group)
                        .enabled(true)
                        .description(def.description)
                        .originalDescription(def.description)
                        .displayOrder(order++)
                        .build();

                mcpToolRepository.save(tool);
            }

            log.info("Initialized {} MCP tool configurations", tools.size());
        } else {
            // Existing install - check for missing tools and add them
            int added = 0;
            int maxOrder = mcpToolRepository.findAll().stream()
                    .mapToInt(McpTool::getDisplayOrder)
                    .max()
                    .orElse(0);

            for (Map.Entry<String, ToolDefinition> entry : tools.entrySet()) {
                String toolName = entry.getKey();
                ToolDefinition def = entry.getValue();

                if (!mcpToolRepository.existsByToolName(toolName)) {
                    McpTool tool = McpTool.builder()
                            .toolName(toolName)
                            .toolGroup(def.group)
                            .enabled(true)
                            .description(def.description)
                            .originalDescription(def.description)
                            .displayOrder(++maxOrder)
                            .build();

                    mcpToolRepository.save(tool);
                    added++;
                    log.info("Added missing MCP tool: {} (group: {})", toolName, def.group);
                }
            }

            if (added > 0) {
                log.info("Added {} missing MCP tool configurations (total: {})", added, existingCount + added);
            } else {
                log.info("MCP tools already initialized ({} tools in database)", existingCount);
            }
        }
    }

    /**
     * Get all tool definitions organized by group.
     * This matches the @McpTool annotations in the *Tools classes.
     */
    private Map<String, ToolDefinition> getToolDefinitions() {
        Map<String, ToolDefinition> tools = new LinkedHashMap<>();

        // === DOCUMENTATION TOOLS (12) ===
        tools.put("searchSpringDocs", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Search across all Spring documentation with pagination support. " +
                "Supports filtering by project, version, and documentation type. " +
                "Returns relevant documentation links and snippets with relevance ranking. " +
                "Use pagination (page parameter) to navigate through large result sets. " +
                "When embeddings are enabled, uses hybrid search (keyword + semantic) for better results."));

        tools.put("getSpringVersions", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "List available versions for a Spring project. Shows latest stable, n-2 minor versions, and n+1 preview versions."));

        tools.put("listSpringProjects", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "List all available Spring projects in the documentation system. Returns project names, slugs, descriptions, and homepage URLs."));

        tools.put("getDocumentationByVersion", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Get all documentation for a specific Spring project version. Returns all documentation links organized by type (reference, api, guides, etc.)."));

        tools.put("getCodeExamples", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Search for code examples with optional filters. Returns code snippets, descriptions, and metadata."));

        tools.put("listSpringBootVersions", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "List all Spring Boot versions available in the system. Results include version numbers, states (GA, RC, SNAPSHOT, MILESTONE), " +
                "release dates, and support end dates (OSS and Enterprise). Results are ordered by version descending (latest first)."));

        tools.put("getLatestSpringBootVersion", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Get the latest patch version for a specific Spring Boot major.minor version. For example, for Spring Boot 3.5, returns the latest 3.5.x version."));

        tools.put("filterSpringBootVersionsBySupport", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Filter Spring Boot versions by their support status (active or ended). Uses the system's enterprise subscription setting to determine which support date to check."));

        tools.put("listProjectsBySpringBootVersion", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "List all Spring projects that are compatible with a specific Spring Boot version (major.minor). Returns projects and their compatible versions for the given Spring Boot version."));

        tools.put("findProjectsByUseCase", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Search for Spring projects by use case. Searches in project names and descriptions for keywords. Useful for finding projects that solve specific problems or use cases."));

        tools.put("getWikiReleaseNotes", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Get Spring Boot release notes for a specific version from the official GitHub wiki. " +
                "Release notes document new features, enhancements, bug fixes, and deprecations for each Spring Boot version. " +
                "Use this to understand what changed in a specific Spring Boot version. Example versions: '4.0', '3.5', '3.4', '3.3', '3.2'"));

        tools.put("getWikiMigrationGuide", new ToolDefinition(McpToolGroup.DOCUMENTATION,
                "Get Spring Boot migration guide for upgrading between specific versions from the official GitHub wiki. " +
                "Migration guides document breaking changes, required modifications, and upgrade instructions when migrating from one Spring Boot version to another. " +
                "Use this when planning an upgrade between Spring Boot versions. Example: from '3.5' to '4.0', from '2.7' to '3.0'"));

        // === MIGRATION TOOLS (7) ===
        tools.put("getSpringMigrationGuide", new ToolDefinition(McpToolGroup.MIGRATION,
                "Get comprehensive migration guide for upgrading Spring Boot versions. " +
                "Returns all breaking changes, import updates, dependency changes, property migrations, and code modifications needed. " +
                "Use this BEFORE generating code for a specific Spring Boot version."));

        tools.put("getBreakingChanges", new ToolDefinition(McpToolGroup.MIGRATION,
                "Get list of breaking changes for a specific Spring project version. Use this before generating code to avoid compilation errors. Returns severity levels: CRITICAL, ERROR, WARNING, INFO."));

        tools.put("searchMigrationKnowledge", new ToolDefinition(McpToolGroup.MIGRATION,
                "Search migration knowledge base for specific topics. Examples: 'flyway starter', 'health indicator', 'thymeleaf request', 'MockBean replacement', 'security configuration'. " +
                "Returns relevant transformations with code examples. When embeddings are enabled, uses hybrid search (keyword + semantic) for better results."));

        tools.put("getAvailableMigrationPaths", new ToolDefinition(McpToolGroup.MIGRATION,
                "Get list of available target versions for migration. Use this to discover what upgrade paths are documented."));

        tools.put("getTransformationsByType", new ToolDefinition(McpToolGroup.MIGRATION,
                "Get transformations filtered by type for a specific migration. Types: IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG."));

        tools.put("getDeprecationReplacement", new ToolDefinition(McpToolGroup.MIGRATION,
                "Find the replacement for a deprecated class or method. Use when you encounter deprecated APIs and need to find the new alternative."));

        tools.put("checkVersionCompatibility", new ToolDefinition(McpToolGroup.MIGRATION,
                "Check if specific dependencies are compatible with a target Spring Boot version. Returns compatibility information and recommended versions."));

        // === LANGUAGE EVOLUTION TOOLS (7) ===
        tools.put("getLanguageVersions", new ToolDefinition(McpToolGroup.LANGUAGE,
                "Get all available versions for Java or Kotlin language. Returns version information including LTS status, release dates, and support end dates. Use this to determine which language versions are currently supported or recommended."));

        tools.put("getLanguageFeatures", new ToolDefinition(McpToolGroup.LANGUAGE,
                "Get language features for a specific Java or Kotlin version. Returns new features, deprecations, and removals with detailed descriptions. Includes JEP/KEP numbers for Java Enhancement Proposals and Kotlin Enhancement Proposals. Use this to understand what's new or changed in a specific language version."));

        tools.put("getModernPatterns", new ToolDefinition(McpToolGroup.LANGUAGE,
                "Get modern code pattern examples showing old vs new ways to write code. Returns before/after code snippets with explanations for migrating to newer language features. Use this to help developers modernize their code when upgrading Java or Kotlin versions."));

        tools.put("getLanguageVersionDiff", new ToolDefinition(McpToolGroup.LANGUAGE,
                "Check for deprecated and removed APIs when upgrading from one language version to another. Returns list of deprecations and removals that need attention during migration. Use this before upgrading Java or Kotlin version to identify breaking changes."));

        tools.put("getSpringBootLanguageRequirements", new ToolDefinition(McpToolGroup.LANGUAGE,
                "Get Java and Kotlin version requirements for a specific Spring Boot version. Returns minimum, recommended, and maximum supported language versions. Use this to ensure correct language version when starting a new Spring Boot project."));

        tools.put("getLanguageFeatureExample", new ToolDefinition(McpToolGroup.LANGUAGE,
                "Get a code example for a specific language feature. Search by JEP number (e.g., '444'), KEP number (e.g., 'KT-11550'), or feature name (e.g., 'Virtual Threads', 'Records'). Returns practical code example with feature description and source type."));

        tools.put("searchLanguageFeatures", new ToolDefinition(McpToolGroup.LANGUAGE,
                "Search for language features by keyword across all Java and Kotlin versions. Returns features matching the search term in name or description. Use this to find specific features or capabilities across language versions."));

        // === FLAVOR TOOLS (8) ===
        tools.put("searchFlavors", new ToolDefinition(McpToolGroup.FLAVORS,
                "Search company flavors (guidelines, architecture patterns, compliance rules, agent configurations). Returns summaries matching the search criteria. When embeddings are enabled, uses hybrid search (keyword + semantic) for better results."));

        tools.put("getFlavorByName", new ToolDefinition(McpToolGroup.FLAVORS,
                "Get complete flavor content by its unique name. Returns full markdown content and metadata."));

        tools.put("getFlavorsByCategory", new ToolDefinition(McpToolGroup.FLAVORS,
                "List all active flavors in a specific category."));

        tools.put("getArchitecturePatterns", new ToolDefinition(McpToolGroup.FLAVORS,
                "Get architecture flavors relevant to specific technologies. Use this when you need architectural guidance for a particular tech stack."));

        tools.put("getComplianceRules", new ToolDefinition(McpToolGroup.FLAVORS,
                "Get compliance flavors by rule names or framework identifiers. Use this when you need to ensure code meets regulatory requirements."));

        tools.put("getAgentConfiguration", new ToolDefinition(McpToolGroup.FLAVORS,
                "Get agent/subagent configuration for a specific use case. Use this to configure AI assistants for specific development workflows."));

        tools.put("getProjectInitialization", new ToolDefinition(McpToolGroup.FLAVORS,
                "Get project initialization template for a specific use case. Use this when setting up new projects to ensure consistent scaffolding."));

        tools.put("listFlavorCategories", new ToolDefinition(McpToolGroup.FLAVORS,
                "List all available flavor categories with counts of active flavors in each."));

        // === FLAVOR GROUP TOOLS (3) ===
        tools.put("listFlavorGroups", new ToolDefinition(McpToolGroup.FLAVOR_GROUPS,
                "List all accessible flavor groups. Returns public groups and private groups where caller is member. Inactive groups are completely hidden from results. " +
                "Groups organize flavors by team or topic: PUBLIC groups (no members): visible to everyone, used for organization-wide standards. " +
                "PRIVATE groups (has members): visible only to member API keys/users, used for team-specific guidelines."));

        tools.put("getFlavorsGroup", new ToolDefinition(McpToolGroup.FLAVOR_GROUPS,
                "Get all flavors in a specific group. Returns group metadata and all member flavors. Only returns active groups - inactive groups are completely hidden. " +
                "The group must be either: PUBLIC (no members): accessible to everyone, or PRIVATE: caller must be a member (API key must belong to the group). " +
                "Use listFlavorGroups first to see available groups, then use this tool to get details."));

        tools.put("getFlavorGroupStatistics", new ToolDefinition(McpToolGroup.FLAVOR_GROUPS,
                "Get statistics about flavor groups. Returns total, active, inactive, public, and private group counts."));

        // === INITIALIZR TOOLS (5) ===
        tools.put("initializrGetDependency", new ToolDefinition(McpToolGroup.INITIALIZR,
                "Get a Spring Boot dependency with formatted snippet for Maven or Gradle. Returns the dependency XML (Maven) or DSL notation (Gradle) that can be directly added to a project's build file. " +
                "Example: Get \"web\" dependency for Gradle: dependencyId: \"web\", format: \"gradle\". Returns: implementation 'org.springframework.boot:spring-boot-starter-web'"));

        tools.put("initializrSearchDependencies", new ToolDefinition(McpToolGroup.INITIALIZR,
                "Search for Spring Boot dependencies by name, description, or ID. Useful for finding dependencies when the exact ID is unknown. " +
                "Example: Search for \"database\" to find all database-related dependencies. Can filter by category (Web, SQL, NoSQL, Security, etc.) and Spring Boot version."));

        tools.put("initializrCheckCompatibility", new ToolDefinition(McpToolGroup.INITIALIZR,
                "Check if a dependency is compatible with a specific Spring Boot version. Returns compatibility status with explanation and suggestions if incompatible. Useful before adding a dependency to ensure it works with your project's Spring Boot version."));

        tools.put("initializrGetBootVersions", new ToolDefinition(McpToolGroup.INITIALIZR,
                "Get all available Spring Boot versions from Spring Initializr. Returns version list including stable releases, release candidates, and snapshots. The default version is marked for easy identification."));

        tools.put("initializrGetDependencyCategories", new ToolDefinition(McpToolGroup.INITIALIZR,
                "Get all dependency categories with their available dependencies. Categories include: Developer Tools, Web, SQL, NoSQL, Security, Cloud, AI, etc. Useful for browsing available dependencies by category. " +
                "When bootVersion is specified, only returns dependencies compatible with that version. Categories with no compatible dependencies are excluded from the results. " +
                "Example: With bootVersion \"4.0.0\", the AI category will be excluded since Spring AI dependencies are not yet compatible with Spring Boot 4.0."));

        // === JAVADOC TOOLS (4) ===
        tools.put("getClassDoc", new ToolDefinition(McpToolGroup.JAVADOC,
                "Get full documentation for a Java class including methods, fields, and constructors. Returns the class documentation with inheritance hierarchy and all members. Use 'latest' or omit version to get the latest available version."));

        tools.put("getPackageDoc", new ToolDefinition(McpToolGroup.JAVADOC,
                "Get package documentation with list of classes/interfaces. Returns the package description and a summary of all classes in the package. Use 'latest' or omit version to get the latest available version."));

        tools.put("searchJavadocs", new ToolDefinition(McpToolGroup.JAVADOC,
                "Search across all Javadoc content using full-text search. Searches class names, descriptions, and package information. Returns ranked results with relevance scores."));

        tools.put("listJavadocLibraries", new ToolDefinition(McpToolGroup.JAVADOC,
                "List all available libraries with Javadoc documentation. Returns library names and their available versions."));

        return tools;
    }

    /**
     * Internal record for tool definition.
     */
    private record ToolDefinition(McpToolGroup group, String description) {}
}

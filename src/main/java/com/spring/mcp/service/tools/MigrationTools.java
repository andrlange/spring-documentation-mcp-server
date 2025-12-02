package com.spring.mcp.service.tools;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.model.dto.mcp.*;
import com.spring.mcp.service.migration.MigrationKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Tools for OpenRewrite migration knowledge.
 * Only registered when mcp.features.openrewrite.enabled=true
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.openrewrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MigrationTools {

    private final MigrationKnowledgeService migrationService;
    private final OpenRewriteFeatureConfig featureConfig;

    @McpTool(description = """
        Get comprehensive migration guide for upgrading Spring Boot versions.
        Returns all breaking changes, import updates, dependency changes,
        property migrations, and code modifications needed.
        Use this BEFORE generating code for a specific Spring Boot version.
        """)
    public MigrationGuideDto getSpringMigrationGuide(
            @McpToolParam(description = "Source Spring Boot version (e.g., '3.5.8')") String fromVersion,
            @McpToolParam(description = "Target Spring Boot version (e.g., '4.0.0')") String toVersion
    ) {
        log.info("Tool: getSpringMigrationGuide - from {} to {}", fromVersion, toVersion);

        if (!featureConfig.isEnabled()) {
            return MigrationGuideDto.empty("spring-boot", fromVersion, toVersion);
        }

        return migrationService.getMigrationGuide("spring-boot", fromVersion, toVersion);
    }

    @McpTool(description = """
        Get list of breaking changes for a specific Spring project version.
        Use this before generating code to avoid compilation errors.
        Returns severity levels: CRITICAL, ERROR, WARNING, INFO.
        """)
    public List<BreakingChangeDto> getBreakingChanges(
            @McpToolParam(description = "Project slug (e.g., 'spring-boot', 'spring-security', 'spring-framework')") String project,
            @McpToolParam(description = "Target version to check (e.g., '4.0.0', '7.0.0')") String version
    ) {
        log.info("Tool: getBreakingChanges - project={}, version={}", project, version);

        if (!featureConfig.isEnabled()) {
            return List.of();
        }

        return migrationService.getBreakingChanges(project, version);
    }

    @McpTool(description = """
        Search migration knowledge base for specific topics.
        Examples: 'flyway starter', 'health indicator', 'thymeleaf request',
        'MockBean replacement', 'security configuration'.
        Returns relevant transformations with code examples.
        """)
    public List<TransformationDto> searchMigrationKnowledge(
            @McpToolParam(description = "Search term (e.g., 'flyway', 'actuator health', '@MockBean')") String searchTerm,
            @McpToolParam(description = "Project to search in (default: 'spring-boot')") String project,
            @McpToolParam(description = "Maximum results to return (default: 10)") Integer limit
    ) {
        log.info("Tool: searchMigrationKnowledge - term='{}', project={}, limit={}", searchTerm, project, limit);

        if (!featureConfig.isEnabled()) {
            return List.of();
        }

        String proj = project != null && !project.isBlank() ? project : "spring-boot";
        int lim = limit != null && limit > 0 ? limit : 10;
        return migrationService.searchTransformations(proj, searchTerm, lim);
    }

    @McpTool(description = """
        Get list of available target versions for migration.
        Use this to discover what upgrade paths are documented.
        """)
    public List<String> getAvailableMigrationPaths(
            @McpToolParam(description = "Project slug (e.g., 'spring-boot')") String project
    ) {
        log.info("Tool: getAvailableMigrationPaths - project={}", project);

        if (!featureConfig.isEnabled()) {
            return List.of();
        }

        return migrationService.getAvailableMigrationTargets(project);
    }

    @McpTool(description = """
        Get transformations filtered by type for a specific migration.
        Types: IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG.
        """)
    public List<TransformationDto> getTransformationsByType(
            @McpToolParam(description = "Project slug (e.g., 'spring-boot')") String project,
            @McpToolParam(description = "Target version (e.g., '4.0.0')") String version,
            @McpToolParam(description = "Transformation type (IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG)") String type
    ) {
        log.info("Tool: getTransformationsByType - project={}, version={}, type={}", project, version, type);

        if (!featureConfig.isEnabled()) {
            return List.of();
        }

        return migrationService.getTransformationsByType(project, version, type);
    }

    @McpTool(description = """
        Find the replacement for a deprecated class or method.
        Use when you encounter deprecated APIs and need to find the new alternative.
        """)
    public DeprecationReplacementDto getDeprecationReplacement(
            @McpToolParam(description = "Fully qualified deprecated class name (e.g., 'org.springframework.boot.actuate.health.Health')") String className,
            @McpToolParam(description = "Deprecated method name (optional, null for entire class deprecation)") String methodName
    ) {
        log.info("Tool: getDeprecationReplacement - class={}, method={}", className, methodName);

        if (!featureConfig.isEnabled()) {
            return DeprecationReplacementDto.notFound(className, methodName);
        }

        return migrationService.findReplacement(className, methodName);
    }

    @McpTool(description = """
        Check if specific dependencies are compatible with a target Spring Boot version.
        Returns compatibility information and recommended versions.
        """)
    public CompatibilityReportDto checkVersionCompatibility(
            @McpToolParam(description = "Target Spring Boot version (e.g., '4.0.0')") String springBootVersion,
            @McpToolParam(description = "List of dependencies to check (e.g., 'spring-security', 'flyway', 'thymeleaf')") List<String> dependencies
    ) {
        log.info("Tool: checkVersionCompatibility - bootVersion={}, deps={}", springBootVersion, dependencies);

        if (!featureConfig.isEnabled()) {
            return CompatibilityReportDto.builder()
                    .springBootVersion(springBootVersion)
                    .allCompatible(false)
                    .warnings(List.of("OpenRewrite feature is disabled"))
                    .build();
        }

        return migrationService.checkCompatibility(springBootVersion, dependencies);
    }
}

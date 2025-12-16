package com.spring.mcp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for tool group with aggregated metrics.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolGroupDto {

    private String groupName;
    private String displayName;
    private String description;
    private String iconClass;
    private String colorClass;

    // Aggregated metrics for the group
    private long totalRequests;
    private double avgLatencyMs;
    private double errorRate;
    private long toolCount;

    // Tools in this group
    private List<ToolMetricDto> tools;

    // Expanded state for UI
    private boolean expanded;

    /**
     * Get the health status class based on error rate.
     */
    public String getHealthStatusClass() {
        if (errorRate < 1) {
            return "success";
        } else if (errorRate < 5) {
            return "warning";
        } else {
            return "danger";
        }
    }

    /**
     * Calculate aggregated metrics from tools.
     */
    public void calculateAggregates() {
        if (tools == null || tools.isEmpty()) {
            this.totalRequests = 0;
            this.avgLatencyMs = 0;
            this.errorRate = 0;
            this.toolCount = 0;
            return;
        }

        this.toolCount = tools.size();
        this.totalRequests = tools.stream().mapToLong(ToolMetricDto::getTotalRequests).sum();

        // Weighted average latency
        double totalLatencySum = tools.stream()
                .filter(t -> t.getTotalRequests() > 0)
                .mapToDouble(t -> t.getAvgLatencyMs() * t.getTotalRequests())
                .sum();
        this.avgLatencyMs = totalRequests > 0 ? totalLatencySum / totalRequests : 0;

        // Overall error rate
        long totalErrors = tools.stream().mapToLong(ToolMetricDto::getErrorCount).sum();
        this.errorRate = totalRequests > 0 ? (totalErrors * 100.0) / totalRequests : 0;
    }

    /**
     * Predefined tool groups.
     */
    public enum Group {
        DOCUMENTATION("Documentation", "Documentation access and search tools",
                "bi-book", "primary",
                List.of("searchSpringDocs", "getDocumentationByVersion", "getCodeExamples",
                        "findProjectsByUseCase", "listSpringProjects")),

        VERSIONS("Versions", "Spring Boot version information tools",
                "bi-git", "info",
                List.of("listSpringBootVersions", "getSpringVersions", "getLatestSpringBootVersion",
                        "filterSpringBootVersionsBySupport", "listProjectsBySpringBootVersion")),

        MIGRATION("Migration", "Migration guides and breaking changes",
                "bi-arrow-up-circle", "warning",
                List.of("getSpringMigrationGuide", "getBreakingChanges", "searchMigrationKnowledge",
                        "getAvailableMigrationPaths", "getTransformationsByType",
                        "getDeprecationReplacement", "checkVersionCompatibility")),

        LANGUAGE("Language", "Java and Kotlin language features",
                "bi-code-slash", "success",
                List.of("getLanguageFeatures", "getLanguageVersionDiff", "getLanguageVersions",
                        "getModernPatterns", "getSpringBootLanguageRequirements",
                        "searchLanguageFeatures")),

        FLAVORS("Flavors", "Company guidelines and architecture patterns",
                "bi-palette", "secondary",
                List.of("searchFlavors", "getFlavorByName", "getFlavorsByCategory",
                        "getArchitecturePatterns", "getComplianceRules", "getAgentConfiguration",
                        "getProjectInitialization", "listFlavorCategories", "listFlavorGroups",
                        "getFlavorsGroup", "getFlavorGroupStatistics")),

        INITIALIZR_JAVADOC("Initializr & Javadoc", "Project initialization and API documentation",
                "bi-rocket-takeoff", "dark",
                List.of("initializrCheckCompatibility", "initializrGetBootVersions",
                        "initializrGetDependency", "initializrGetDependencyCategories",
                        "initializrSearchDependencies", "getClassDoc", "getPackageDoc",
                        "listJavadocLibraries", "searchJavadocs"));

        private final String displayName;
        private final String description;
        private final String iconClass;
        private final String colorClass;
        private final List<String> toolNames;

        Group(String displayName, String description, String iconClass,
              String colorClass, List<String> toolNames) {
            this.displayName = displayName;
            this.description = description;
            this.iconClass = iconClass;
            this.colorClass = colorClass;
            this.toolNames = toolNames;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getIconClass() { return iconClass; }
        public String getColorClass() { return colorClass; }
        public List<String> getToolNames() { return toolNames; }

        /**
         * Find the group for a tool name.
         */
        public static Group findGroupForTool(String toolName) {
            for (Group group : values()) {
                if (group.toolNames.contains(toolName)) {
                    return group;
                }
            }
            return null;
        }

        /**
         * Create a DTO for this group.
         */
        public ToolGroupDto toDto() {
            return ToolGroupDto.builder()
                    .groupName(name())
                    .displayName(displayName)
                    .description(description)
                    .iconClass(iconClass)
                    .colorClass(colorClass)
                    .build();
        }
    }
}

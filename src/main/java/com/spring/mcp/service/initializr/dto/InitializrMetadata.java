package com.spring.mcp.service.initializr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Root DTO for Spring Initializr metadata from /metadata/client endpoint.
 *
 * <p>This class maps the complete metadata response from start.spring.io,
 * including all available dependencies, versions, languages, and project types.</p>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitializrMetadata {

    /**
     * API links for various project generation endpoints.
     */
    @JsonProperty("_links")
    private Map<String, Link> links;

    /**
     * Dependencies organized by category.
     * Uses the DependencyCategory DTO which contains DependencyInfo items.
     */
    private DependenciesSection dependencies;

    /**
     * Available project types (gradle-project, maven-project, etc.)
     */
    private TypeSection type;

    /**
     * Available Spring Boot versions.
     */
    private BootVersionSection bootVersion;

    /**
     * Available Java versions.
     */
    private JavaVersionSection javaVersion;

    /**
     * Available programming languages.
     */
    private LanguageSection language;

    /**
     * Available packaging types (jar, war).
     */
    private PackagingSection packaging;

    /**
     * API link definition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String href;
        private boolean templated;
    }

    /**
     * Dependencies section containing categories.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DependenciesSection {
        private String type;
        private List<DependencyCategory> values;
    }

    /**
     * Type section with available project types.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypeSection {
        private String type;
        @JsonProperty("default")
        private String defaultValue;
        private List<TypeOption> values;
    }

    /**
     * Individual project type option.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypeOption {
        private String id;
        private String name;
        private String description;
        private String action;
        private Map<String, String> tags;
    }

    /**
     * Boot version section with available Spring Boot versions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BootVersionSection {
        private String type;
        @JsonProperty("default")
        private String defaultValue;
        private List<BootVersion> values;
    }

    /**
     * Java version section with available Java versions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JavaVersionSection {
        private String type;
        @JsonProperty("default")
        private String defaultValue;
        private List<JavaVersion> values;
    }

    /**
     * Individual Java version option.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JavaVersion {
        private String id;
        private String name;
    }

    /**
     * Language section with available languages.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LanguageSection {
        private String type;
        @JsonProperty("default")
        private String defaultValue;
        private List<LanguageOption> values;
    }

    /**
     * Individual language option.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LanguageOption {
        private String id;
        private String name;
    }

    /**
     * Packaging section with available packaging types.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PackagingSection {
        private String type;
        @JsonProperty("default")
        private String defaultValue;
        private List<PackagingOption> values;
    }

    /**
     * Individual packaging option.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PackagingOption {
        private String id;
        private String name;
    }
}

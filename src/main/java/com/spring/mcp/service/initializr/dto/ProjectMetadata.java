package com.spring.mcp.service.initializr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for project generation request/response metadata.
 *
 * <p>This class represents the configuration options for generating a new
 * Spring Boot project through the Initializr API. It contains all the
 * parameters needed to customize the generated project.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ProjectMetadata project = ProjectMetadata.builder()
 *     .groupId("com.example")
 *     .artifactId("my-app")
 *     .name("My Application")
 *     .description("A Spring Boot application")
 *     .bootVersion("3.5.8")
 *     .javaVersion("21")
 *     .packaging("jar")
 *     .buildType("gradle-project")
 *     .dependencies(List.of("web", "data-jpa", "security"))
 *     .build();
 * }</pre>
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
public class ProjectMetadata {

    /**
     * Maven group ID (e.g., "com.example", "org.mycompany").
     * Default: "com.example"
     */
    @Builder.Default
    private String groupId = "com.example";

    /**
     * Maven artifact ID (e.g., "my-app", "demo").
     * This becomes the project directory name.
     * Default: "demo"
     */
    @Builder.Default
    private String artifactId = "demo";

    /**
     * Project display name.
     * Default: "demo"
     */
    @Builder.Default
    private String name = "demo";

    /**
     * Project description.
     * Default: "Demo project for Spring Boot"
     */
    @Builder.Default
    private String description = "Demo project for Spring Boot";

    /**
     * Base package name for generated code.
     * If not specified, derived from groupId + artifactId.
     * Example: "com.example.demo"
     */
    private String packageName;

    /**
     * Spring Boot version.
     * Default: Current stable version (e.g., "3.5.8")
     */
    @Builder.Default
    private String bootVersion = "3.5.8";

    /**
     * Java version.
     * Options: "17", "21", "25"
     * Default: "21"
     */
    @Builder.Default
    private String javaVersion = "21";

    /**
     * Packaging type.
     * Options: "jar", "war"
     * Default: "jar"
     */
    @Builder.Default
    private String packaging = "jar";

    /**
     * Programming language.
     * Options: "java", "kotlin", "groovy"
     * Default: "java"
     */
    @Builder.Default
    private String language = "java";

    /**
     * Build tool and project type.
     * Options: "gradle-project", "gradle-project-kotlin", "maven-project"
     * Default: "gradle-project"
     */
    @Builder.Default
    private String buildType = "gradle-project";

    /**
     * List of dependency IDs to include in the project.
     * Examples: ["web", "data-jpa", "security", "actuator"]
     */
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();

    /**
     * Project version.
     * Default: "0.0.1-SNAPSHOT"
     */
    @Builder.Default
    private String version = "0.0.1-SNAPSHOT";

    /**
     * Get the derived package name.
     *
     * @return package name, or derived from groupId if not set
     */
    public String getEffectivePackageName() {
        if (packageName != null && !packageName.isBlank()) {
            return packageName;
        }
        // Derive from groupId + artifactId
        String base = groupId != null ? groupId : "com.example";
        String artifact = artifactId != null ? artifactId.replace("-", "").toLowerCase() : "demo";
        return base + "." + artifact;
    }

    /**
     * Check if this is a Gradle project.
     *
     * @return true if using Gradle build
     */
    public boolean isGradle() {
        return buildType != null && buildType.startsWith("gradle");
    }

    /**
     * Check if this is a Maven project.
     *
     * @return true if using Maven build
     */
    public boolean isMaven() {
        return buildType != null && buildType.contains("maven");
    }

    /**
     * Check if this is a Kotlin DSL project (Gradle).
     *
     * @return true if using Kotlin DSL for Gradle
     */
    public boolean isKotlinDsl() {
        return buildType != null && buildType.contains("kotlin");
    }

    /**
     * Build the URL query parameters for Initializr API.
     *
     * @return query string for project generation endpoint
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();

        appendParam(sb, "type", buildType);
        appendParam(sb, "language", language);
        appendParam(sb, "bootVersion", bootVersion);
        appendParam(sb, "baseDir", artifactId);
        appendParam(sb, "groupId", groupId);
        appendParam(sb, "artifactId", artifactId);
        appendParam(sb, "name", name);
        appendParam(sb, "description", description);
        appendParam(sb, "packageName", getEffectivePackageName());
        appendParam(sb, "packaging", packaging);
        appendParam(sb, "javaVersion", javaVersion);

        // Add dependencies
        if (dependencies != null && !dependencies.isEmpty()) {
            appendParam(sb, "dependencies", String.join(",", dependencies));
        }

        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String name, String value) {
        if (value == null || value.isBlank()) return;

        if (!sb.isEmpty()) {
            sb.append("&");
        }
        sb.append(name).append("=").append(urlEncode(value));
    }

    private String urlEncode(String value) {
        return value.replace(" ", "%20")
                    .replace("+", "%2B")
                    .replace("&", "%26")
                    .replace("=", "%3D");
    }

    /**
     * Create a copy of this metadata with different dependencies.
     *
     * @param newDependencies the new list of dependencies
     * @return new ProjectMetadata with updated dependencies
     */
    public ProjectMetadata withDependencies(List<String> newDependencies) {
        return ProjectMetadata.builder()
            .groupId(this.groupId)
            .artifactId(this.artifactId)
            .name(this.name)
            .description(this.description)
            .packageName(this.packageName)
            .bootVersion(this.bootVersion)
            .javaVersion(this.javaVersion)
            .packaging(this.packaging)
            .language(this.language)
            .buildType(this.buildType)
            .dependencies(newDependencies)
            .version(this.version)
            .build();
    }
}

package com.spring.mcp.service.initializr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO representing detailed information about a Spring Boot dependency.
 *
 * <p>Contains all the metadata about a dependency available from the
 * Spring Initializr API, including compatibility version ranges and
 * related resource links.</p>
 *
 * <p>Example dependency:</p>
 * <pre>{@code
 * {
 *   "id": "spring-boot-starter-web",
 *   "name": "Spring Web",
 *   "description": "Build web, including RESTful, applications using Spring MVC.",
 *   "versionRange": "[3.0.0,4.0.0)",
 *   "_links": {
 *     "reference": { "href": "https://docs.spring.io/..." },
 *     "guide": { "href": "https://spring.io/guides/..." }
 *   }
 * }
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
public class DependencyInfo {

    /**
     * Unique identifier for the dependency.
     * This is used when adding dependencies to a project.
     * Examples: "web", "data-jpa", "security", "actuator"
     */
    private String id;

    /**
     * Display name of the dependency.
     * Examples: "Spring Web", "Spring Data JPA", "Spring Security"
     */
    private String name;

    /**
     * Detailed description of what the dependency provides.
     */
    private String description;

    /**
     * Maven version range indicating compatible Spring Boot versions.
     * Uses Maven version range syntax:
     * <ul>
     *   <li>[3.0.0,4.0.0) - 3.0.0 <= version < 4.0.0</li>
     *   <li>[3.2.0,) - version >= 3.2.0</li>
     *   <li>(,3.5.0] - version <= 3.5.0</li>
     * </ul>
     */
    private String versionRange;

    /**
     * Additional metadata/tags for the dependency.
     * May include keywords, categories, or other classification info.
     */
    private String starter;

    /**
     * Group ID for the dependency (e.g., "org.springframework.boot").
     */
    private String groupId;

    /**
     * Artifact ID for the dependency (e.g., "spring-boot-starter-web").
     */
    private String artifactId;

    /**
     * Specific version of the dependency (if pinned).
     */
    private String version;

    /**
     * Scope for the dependency (compile, runtime, test, provided).
     */
    private String scope;

    /**
     * Bom (Bill of Materials) this dependency belongs to.
     */
    private String bom;

    /**
     * Repository where this dependency can be found (if not Maven Central).
     */
    private String repository;

    /**
     * Related resource links (documentation, guides, etc.).
     * Keys typically include: "reference", "guide", "sample"
     * Note: API can return either a single link object or an array of links.
     * The custom deserializer normalizes both to List<DependencyLink>.
     */
    @JsonProperty("_links")
    @JsonDeserialize(using = DependencyLinksDeserializer.class)
    private Map<String, List<DependencyLink>> links;

    /**
     * Link to related resources for a dependency.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DependencyLink {
        /**
         * URL to the resource.
         */
        private String href;

        /**
         * Whether the URL is a template (contains placeholders).
         */
        private boolean templated;

        /**
         * Optional title/description for the link.
         */
        private String title;
    }

    /**
     * Check if this dependency is compatible with a given Spring Boot version.
     *
     * @param bootVersion the Spring Boot version to check (e.g., "3.5.8")
     * @return true if compatible or no version range specified, false otherwise
     */
    public boolean isCompatibleWith(String bootVersion) {
        if (versionRange == null || versionRange.isBlank()) {
            return true; // No restriction means compatible with all versions
        }
        return VersionRangeUtil.isInRange(bootVersion, versionRange);
    }

    /**
     * Generate Maven dependency XML snippet.
     *
     * @return formatted Maven dependency XML
     */
    public String toMavenDependency() {
        StringBuilder sb = new StringBuilder();
        sb.append("<dependency>\n");
        sb.append("    <groupId>").append(groupId != null ? groupId : "org.springframework.boot").append("</groupId>\n");
        sb.append("    <artifactId>").append(artifactId != null ? artifactId : "spring-boot-starter-" + id).append("</artifactId>\n");
        if (version != null && !version.isBlank()) {
            sb.append("    <version>").append(version).append("</version>\n");
        }
        if (scope != null && !scope.isBlank() && !"compile".equals(scope)) {
            sb.append("    <scope>").append(scope).append("</scope>\n");
        }
        sb.append("</dependency>");
        return sb.toString();
    }

    /**
     * Generate Gradle dependency notation.
     *
     * @return formatted Gradle dependency (Groovy DSL)
     */
    public String toGradleDependency() {
        String config = scope != null ? mapScopeToGradle(scope) : "implementation";
        String group = groupId != null ? groupId : "org.springframework.boot";
        String artifact = artifactId != null ? artifactId : "spring-boot-starter-" + id;

        if (version != null && !version.isBlank()) {
            return String.format("%s '%s:%s:%s'", config, group, artifact, version);
        }
        return String.format("%s '%s:%s'", config, group, artifact);
    }

    /**
     * Generate Gradle Kotlin DSL dependency notation.
     *
     * @return formatted Gradle dependency (Kotlin DSL)
     */
    public String toGradleKotlinDependency() {
        String config = scope != null ? mapScopeToGradle(scope) : "implementation";
        String group = groupId != null ? groupId : "org.springframework.boot";
        String artifact = artifactId != null ? artifactId : "spring-boot-starter-" + id;

        if (version != null && !version.isBlank()) {
            return String.format("%s(\"%s:%s:%s\")", config, group, artifact, version);
        }
        return String.format("%s(\"%s:%s\")", config, group, artifact);
    }

    private String mapScopeToGradle(String mavenScope) {
        return switch (mavenScope.toLowerCase()) {
            case "compile" -> "implementation";
            case "runtime" -> "runtimeOnly";
            case "test" -> "testImplementation";
            case "provided" -> "compileOnly";
            default -> "implementation";
        };
    }

    /**
     * Custom deserializer for the _links field that handles both single objects and arrays.
     * The Spring Initializr API returns links in two formats:
     * <ul>
     *   <li>Single object: {"reference": {"href": "...", "title": "..."}}</li>
     *   <li>Array: {"guide": [{"href": "...", "title": "..."}, {"href": "...", "title": "..."}]}</li>
     * </ul>
     * This deserializer normalizes both to Map<String, List<DependencyLink>>.
     */
    public static class DependencyLinksDeserializer extends JsonDeserializer<Map<String, List<DependencyLink>>> {

        @Override
        public Map<String, List<DependencyLink>> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            Map<String, List<DependencyLink>> result = new HashMap<>();

            if (p.currentToken() != JsonToken.START_OBJECT) {
                return result;
            }

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken(); // Move to value

                List<DependencyLink> links = new ArrayList<>();

                if (p.currentToken() == JsonToken.START_ARRAY) {
                    // Array of links
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        DependencyLink link = p.readValueAs(DependencyLink.class);
                        if (link != null) {
                            links.add(link);
                        }
                    }
                } else if (p.currentToken() == JsonToken.START_OBJECT) {
                    // Single link object
                    DependencyLink link = p.readValueAs(DependencyLink.class);
                    if (link != null) {
                        links.add(link);
                    }
                }

                if (!links.isEmpty()) {
                    result.put(fieldName, links);
                }
            }

            return result;
        }
    }

    /**
     * Convenience method to get the first link for a given key.
     *
     * @param linkKey the link key (e.g., "reference", "guide", "sample")
     * @return the first link if present, null otherwise
     */
    public DependencyLink getFirstLink(String linkKey) {
        if (links == null || !links.containsKey(linkKey)) {
            return null;
        }
        List<DependencyLink> linkList = links.get(linkKey);
        return linkList != null && !linkList.isEmpty() ? linkList.get(0) : null;
    }

    /**
     * Convenience method to get all links for a given key.
     *
     * @param linkKey the link key (e.g., "reference", "guide", "sample")
     * @return list of links, or empty list if none found
     */
    public List<DependencyLink> getAllLinks(String linkKey) {
        if (links == null || !links.containsKey(linkKey)) {
            return List.of();
        }
        return links.get(linkKey);
    }
}

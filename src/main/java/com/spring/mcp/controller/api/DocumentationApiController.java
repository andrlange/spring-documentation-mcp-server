package com.spring.mcp.controller.api;

import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.repository.DocumentationContentRepository;
import com.spring.mcp.repository.DocumentationLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for documentation operations
 */
@RestController
@RequestMapping("/api/documentation")
@RequiredArgsConstructor
@Slf4j
public class DocumentationApiController {

    private final DocumentationContentRepository documentationContentRepository;
    private final DocumentationLinkRepository documentationLinkRepository;

    /**
     * Mapping of project slugs to topic keywords for matching GitHub docs.
     * Spring Boot's GitHub docs cover many topics that relate to other Spring projects.
     */
    private static final Map<String, List<String>> PROJECT_TOPIC_KEYWORDS = Map.ofEntries(
        // Core projects
        Map.entry("spring-framework", List.of("Spring MVC", "WebSockets", "Aspect-Oriented", "Reactive Web", "Web Services", "Validation", "Internationalization")),
        Map.entry("spring-security", List.of("Security", "Spring Security")),
        Map.entry("spring-batch", List.of("Batch", "Spring Batch")),
        Map.entry("spring-session", List.of("Session", "Spring Session")),
        Map.entry("spring-integration", List.of("Integration", "Spring Integration")),
        Map.entry("spring-hateoas", List.of("HATEOAS", "Spring HATEOAS")),
        Map.entry("spring-graphql", List.of("GraphQL", "Spring for GraphQL")),

        // Data projects
        Map.entry("spring-data", List.of("Data", "Data Access", "SQL Databases", "NoSQL", "Database", "Caching", "Hazelcast")),
        Map.entry("spring-data-jpa", List.of("JPA", "SQL Databases", "Database Initialization")),
        Map.entry("spring-data-mongodb", List.of("MongoDB", "NoSQL")),
        Map.entry("spring-data-redis", List.of("Redis", "NoSQL", "Caching")),
        Map.entry("spring-data-jdbc", List.of("JDBC", "SQL Databases")),
        Map.entry("spring-data-r2dbc", List.of("R2DBC", "SQL Databases", "Reactive")),
        Map.entry("spring-data-rest", List.of("Data REST", "REST")),
        Map.entry("spring-data-cassandra", List.of("Cassandra", "NoSQL")),
        Map.entry("spring-data-ldap", List.of("LDAP")),

        // Messaging projects
        Map.entry("spring-amqp", List.of("AMQP", "JMS", "Messaging")),
        Map.entry("spring-kafka", List.of("Kafka", "Apache Kafka", "Messaging")),
        Map.entry("spring-pulsar", List.of("Pulsar", "Apache Pulsar", "Messaging")),

        // Cloud projects
        Map.entry("spring-cloud", List.of("Cloud", "Cloud Foundry", "Deploying to the Cloud")),
        Map.entry("spring-cloud-gateway", List.of("Gateway")),
        Map.entry("spring-cloud-config", List.of("Configuration", "Externalized Configuration")),
        Map.entry("spring-cloud-stream", List.of("Stream", "Messaging")),
        Map.entry("spring-cloud-function", List.of("Function")),
        Map.entry("spring-cloud-kubernetes", List.of("Kubernetes", "Container")),
        Map.entry("spring-cloud-vault", List.of("Vault")),
        Map.entry("spring-cloud-sleuth", List.of("Tracing", "Observability")),
        Map.entry("spring-cloud-circuitbreaker", List.of("Circuit Breaker")),

        // Other projects
        Map.entry("spring-shell", List.of("Shell", "CLI")),
        Map.entry("spring-statemachine", List.of("Statemachine", "State Machine")),
        Map.entry("spring-vault", List.of("Vault")),
        Map.entry("spring-ldap", List.of("LDAP")),
        Map.entry("spring-restdocs", List.of("REST Docs")),
        Map.entry("spring-webflow", List.of("Web Flow")),
        Map.entry("spring-ws", List.of("Web Services", "SOAP")),
        Map.entry("spring-modulith", List.of("Modulith")),
        Map.entry("spring-ai", List.of("AI"))
    );

    /**
     * Get documentation content by link ID
     *
     * @param id the documentation link ID
     * @return JSON response with markdown content
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<Map<String, Object>> getDocumentationContent(@PathVariable Long id) {
        log.debug("Fetching content for documentation link ID: {}", id);

        return documentationContentRepository.findByLinkId(id)
            .map(content -> {
                log.info("Found content for link ID {}: {} characters", id,
                    content.getContent() != null ? content.getContent().length() : 0);

                Map<String, Object> response = new HashMap<>();
                response.put("content", content.getContent() != null ? content.getContent() : "");
                response.put("contentType", content.getContentType() != null ? content.getContentType() : "markdown");
                response.put("updatedAt", content.getUpdatedAt() != null ? content.getUpdatedAt().toString() : "");

                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                log.warn("No content found for documentation link ID: {}", id);

                Map<String, Object> response = new HashMap<>();
                response.put("content", "");
                response.put("contentType", "markdown");
                response.put("message", "No content available for this documentation link");

                return ResponseEntity.ok(response);
            });
    }

    /**
     * Get GitHub documentation links for a given documentation link's project and version.
     * Uses topic-based matching: Spring Boot's GitHub docs are matched to related projects
     * based on topic keywords in the document titles.
     *
     * @param id the documentation link ID (typically an Overview doc)
     * @return JSON response with list of GitHub documentation links
     */
    @GetMapping("/{id}/github-docs")
    public ResponseEntity<Map<String, Object>> getGitHubDocs(@PathVariable Long id) {
        log.debug("Fetching GitHub docs for documentation link ID: {}", id);

        return documentationLinkRepository.findById(id)
            .map(doc -> {
                String projectSlug = doc.getVersion().getProject().getSlug();
                String projectName = doc.getVersion().getProject().getName();
                String version = doc.getVersion().getVersion();

                log.debug("Looking for GitHub docs for project: {} ({})", projectSlug, projectName);

                List<DocumentationLink> githubDocs;

                if ("spring-boot".equals(projectSlug)) {
                    // For Spring Boot, return all its GitHub docs
                    String normalizedVersion = normalizeVersion(version);
                    githubDocs = documentationLinkRepository
                        .findGitHubDocsByProjectAndVersion(projectSlug, normalizedVersion);
                    log.info("Found {} GitHub docs for Spring Boot version {}", githubDocs.size(), normalizedVersion);
                } else {
                    // For other projects, use topic-based matching from Spring Boot's GitHub docs
                    githubDocs = findGitHubDocsByTopic(projectSlug, projectName);
                    log.info("Found {} topic-matched GitHub docs for {}", githubDocs.size(), projectSlug);
                }

                List<Map<String, Object>> docsData = githubDocs.stream()
                    .map(this::mapDocToResponse)
                    .collect(Collectors.toList());

                Map<String, Object> response = new HashMap<>();
                response.put("docs", docsData);
                response.put("count", docsData.size());
                response.put("project", projectSlug);
                response.put("matchType", "spring-boot".equals(projectSlug) ? "direct" : "topic");

                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                log.warn("Documentation link ID {} not found", id);
                Map<String, Object> response = new HashMap<>();
                response.put("docs", List.of());
                response.put("count", 0);
                response.put("error", "Documentation link not found");
                return ResponseEntity.ok(response);
            });
    }

    /**
     * Find GitHub docs by topic matching.
     * Searches Spring Boot's GitHub docs for titles that match the project's topic keywords.
     */
    private List<DocumentationLink> findGitHubDocsByTopic(String projectSlug, String projectName) {
        // Get topic keywords for this project
        List<String> keywords = PROJECT_TOPIC_KEYWORDS.get(projectSlug);

        // If no explicit keywords, try to derive from project name
        if (keywords == null || keywords.isEmpty()) {
            // Extract meaningful keywords from project name
            // e.g., "Spring Cloud Gateway" -> "Gateway"
            // e.g., "Spring Authorization Server" -> "Authorization Server"
            String derivedKeyword = projectName
                .replace("Spring ", "")
                .replace("Cloud ", "")
                .replace("Data ", "")
                .trim();
            if (!derivedKeyword.isEmpty() && derivedKeyword.length() > 2) {
                keywords = List.of(derivedKeyword);
                log.debug("Derived keyword '{}' from project name '{}'", derivedKeyword, projectName);
            }
        }

        if (keywords == null || keywords.isEmpty()) {
            log.debug("No keywords found for project: {}", projectSlug);
            return List.of();
        }

        final List<String> finalKeywords = keywords;

        // Get all GitHub Reference docs (they're all from Spring Boot)
        List<DocumentationLink> allGitHubDocs = documentationLinkRepository.findAll().stream()
            .filter(doc -> doc.getDocType() != null && "GitHub Reference".equals(doc.getDocType().getName()))
            .filter(DocumentationLink::getIsActive)
            .collect(Collectors.toList());

        // Filter by topic keywords (word boundary match in title to avoid false positives like "AI" matching "container")
        return allGitHubDocs.stream()
            .filter(doc -> {
                String title = doc.getTitle().toLowerCase();
                return finalKeywords.stream()
                    .anyMatch(keyword -> matchesKeyword(title, keyword.toLowerCase()));
            })
            .sorted(Comparator.comparing(DocumentationLink::getTitle, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    /**
     * Get GitHub documentation by project slug (for projects without Overview docs).
     *
     * @param projectSlug the project slug
     * @return JSON response with list of GitHub documentation links
     */
    @GetMapping("/github-docs/project/{projectSlug}")
    public ResponseEntity<Map<String, Object>> getGitHubDocsByProject(@PathVariable String projectSlug) {
        log.debug("Fetching GitHub docs for project: {}", projectSlug);

        // Find the project and get its GitHub docs
        List<DocumentationLink> githubDocs = documentationLinkRepository.findAll().stream()
            .filter(doc -> doc.getVersion().getProject().getSlug().equals(projectSlug))
            .filter(doc -> doc.getDocType() != null && "GitHub Reference".equals(doc.getDocType().getName()))
            .filter(DocumentationLink::getIsActive)
            .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
            .collect(Collectors.toList());

        log.info("Found {} GitHub docs for project {}", githubDocs.size(), projectSlug);

        List<Map<String, Object>> docsData = githubDocs.stream()
            .map(this::mapDocToResponse)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("docs", docsData);
        response.put("count", docsData.size());
        response.put("project", projectSlug);

        return ResponseEntity.ok(response);
    }

    /**
     * Normalize version string by extracting major.minor.patch.
     * Examples: "4.0.0.RELEASE" -> "4.0.0", "3.5.7-SNAPSHOT" -> "3.5.7"
     */
    private String normalizeVersion(String version) {
        if (version == null) return "";

        // Remove common suffixes
        String normalized = version
            .replace(".RELEASE", "")
            .replace(".BUILD-SNAPSHOT", "")
            .replace("-SNAPSHOT", "")
            .replace(".GA", "");

        // Extract major.minor.patch (first 3 numeric parts)
        String[] parts = normalized.split("\\.");
        if (parts.length >= 3) {
            try {
                // Only keep numeric parts
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(3, parts.length); i++) {
                    String part = parts[i].replaceAll("[^0-9].*", "");
                    if (!part.isEmpty()) {
                        if (sb.length() > 0) sb.append(".");
                        sb.append(part);
                    }
                }
                if (sb.length() > 0) {
                    return sb.toString();
                }
            } catch (Exception e) {
                // Fall through to return original
            }
        }
        return normalized;
    }

    /**
     * Check if a keyword matches in the title using word boundary matching.
     * This prevents false positives like "AI" matching "contAIner" or "testcontAIners".
     *
     * @param title the title to search in (lowercase)
     * @param keyword the keyword to find (lowercase)
     * @return true if keyword matches as a word or phrase
     */
    private boolean matchesKeyword(String title, String keyword) {
        // Build regex pattern for word boundary match
        // Use \\b for word boundaries, but also handle phrases with spaces
        String pattern = "\\b" + java.util.regex.Pattern.quote(keyword) + "\\b";
        return java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(title)
            .find();
    }

    /**
     * Map a DocumentationLink to a response map.
     */
    private Map<String, Object> mapDocToResponse(DocumentationLink doc) {
        Map<String, Object> docData = new HashMap<>();
        docData.put("id", doc.getId());
        docData.put("title", doc.getTitle());
        docData.put("url", doc.getUrl());
        docData.put("description", doc.getDescription());
        docData.put("version", doc.getVersion() != null ? doc.getVersion().getVersion() : "");
        docData.put("lastFetched", doc.getLastFetched() != null ? doc.getLastFetched().toString() : "");
        return docData;
    }
}

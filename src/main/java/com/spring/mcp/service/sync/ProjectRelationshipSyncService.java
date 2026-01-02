package com.spring.mcp.service.sync;

import com.spring.mcp.model.entity.ProjectRelationship;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.ProjectRelationshipRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

/**
 * Service for detecting and synchronizing parent/child relationships between Spring projects.
 * Parses the Spring.io projects navigation menu to identify project hierarchies.
 *
 * Examples:
 * - Spring Data (parent) → Spring Data JPA, Spring Data MongoDB, etc. (children)
 * - Spring Cloud (parent) → Spring Cloud Azure, Spring Cloud Alibaba, etc. (children)
 *
 * Data source: https://spring.io HTML navigation menu
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectRelationshipSyncService {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectRelationshipRepository projectRelationshipRepository;
    private final WebClient.Builder webClientBuilder;

    private static final String SPRING_IO_URL = "https://spring.io";
    private static final String SPRING_PROJECTS_URL = "https://spring.io/projects";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Synchronize project relationships by parsing spring.io HTML
     *
     * @return SyncResult with statistics
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncResult syncProjectRelationships() {
        log.info("Starting project relationships sync");
        SyncResult result = new SyncResult();

        try {
            // Fetch HTML from spring.io
            String html = fetchSpringIoHtml();

            // Parse HTML to find parent/child relationships
            Map<String, List<String>> relationships = parseProjectRelationships(html);
            log.info("Found {} parent projects with child relationships", relationships.size());

            // Process each relationship
            for (Map.Entry<String, List<String>> entry : relationships.entrySet()) {
                String parentSlug = entry.getKey();
                List<String> childSlugs = entry.getValue();

                try {
                    processRelationship(parentSlug, childSlugs, result);
                } catch (Exception e) {
                    log.error("Error processing relationship for parent: {}", parentSlug, e);
                    result.incrementErrors();
                }
            }

            result.setSuccess(true);
            log.info("Project relationships sync completed. Created: {}, Skipped: {}, Errors: {}",
                result.getRelationshipsCreated(), result.getRelationshipsSkipped(), result.getErrorsEncountered());

        } catch (Exception e) {
            log.error("Error during project relationships sync", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Fetch HTML from spring.io
     */
    private String fetchSpringIoHtml() {
        log.debug("Fetching HTML from: {}", SPRING_PROJECTS_URL);

        WebClient webClient = webClientBuilder.build();

        return webClient.get()
            .uri(SPRING_PROJECTS_URL)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(REQUEST_TIMEOUT)
            .doOnError(error -> log.error("Error fetching spring.io HTML: {}", error.getMessage()))
            .block();
    }

    /**
     * Parse HTML to extract parent/child project relationships.
     * Looks for navigation menus with class="is-parent" to identify parent projects.
     *
     * @param html The HTML content from spring.io
     * @return Map of parent slug to list of child slugs
     */
    private Map<String, List<String>> parseProjectRelationships(String html) {
        Map<String, List<String>> relationships = new HashMap<>();

        if (html == null || html.isEmpty()) {
            log.warn("Empty HTML provided to parseProjectRelationships");
            return relationships;
        }

        try {
            Document doc = Jsoup.parse(html);

            // Find all project links with parent indicator
            // Spring.io uses various navigation structures, we'll try multiple selectors
            Elements parentElements = doc.select("a[href*='/projects/'].is-parent, " +
                                                 "li.is-parent > a[href*='/projects/'], " +
                                                 "div.project-parent a[href*='/projects/']");

            log.debug("Found {} potential parent project elements", parentElements.size());

            for (Element parentElement : parentElements) {
                String href = parentElement.attr("href");
                String parentSlug = extractProjectSlug(href);

                if (parentSlug != null && !parentSlug.isEmpty()) {
                    // Find child projects (siblings or nested elements)
                    List<String> childSlugs = findChildProjects(parentElement);

                    if (!childSlugs.isEmpty()) {
                        relationships.put(parentSlug, childSlugs);
                        log.debug("Found parent '{}' with {} children: {}",
                            parentSlug, childSlugs.size(), childSlugs);
                    }
                }
            }

            // Also parse known parent/child relationships from project structure
            // This is a fallback for when HTML structure doesn't have clear markers
            addKnownRelationships(relationships);

        } catch (Exception e) {
            log.error("Error parsing project relationships from HTML", e);
        }

        return relationships;
    }

    /**
     * Extract project slug from URL
     * Example: "/projects/spring-boot" → "spring-boot"
     */
    private String extractProjectSlug(String href) {
        if (href == null || href.isEmpty()) {
            return null;
        }

        // Remove /projects/ prefix and any query parameters or anchors
        String slug = href.replaceAll(".*/projects/", "")
                         .replaceAll("[?#].*", "")
                         .trim();

        return slug.isEmpty() ? null : slug;
    }

    /**
     * Find child projects related to a parent element
     */
    private List<String> findChildProjects(Element parentElement) {
        List<String> children = new ArrayList<>();

        // Strategy 1: Look for sibling or nested lists
        Element parentLi = parentElement.parent();
        if (parentLi != null) {
            Elements childLinks = parentLi.select("ul a[href*='/projects/'], " +
                                                  "li a[href*='/projects/']");

            for (Element childLink : childLinks) {
                String href = childLink.attr("href");
                String childSlug = extractProjectSlug(href);

                if (childSlug != null && !childSlug.isEmpty()) {
                    children.add(childSlug);
                }
            }
        }

        // Strategy 2: Look for data attributes or specific classes
        Elements dataChildren = parentElement.parent()
            .select("[data-parent], .child-project a[href*='/projects/']");

        for (Element childElem : dataChildren) {
            String href = childElem.attr("href");
            if (href.isEmpty()) {
                href = childElem.select("a[href*='/projects/']").attr("href");
            }

            String childSlug = extractProjectSlug(href);
            if (childSlug != null && !childSlug.isEmpty() && !children.contains(childSlug)) {
                children.add(childSlug);
            }
        }

        return children;
    }

    /**
     * Add known parent/child relationships as a fallback.
     * These are well-known Spring project hierarchies.
     */
    private void addKnownRelationships(Map<String, List<String>> relationships) {
        // Spring Data family
        addIfNotExists(relationships, "spring-data", Arrays.asList(
            "spring-data-jpa", "spring-data-mongodb", "spring-data-redis",
            "spring-data-elasticsearch", "spring-data-cassandra", "spring-data-neo4j",
            "spring-data-r2dbc", "spring-data-jdbc", "spring-data-rest",
            "spring-data-couchbase", "spring-data-ldap"
        ));

        // Spring Cloud family
        addIfNotExists(relationships, "spring-cloud", Arrays.asList(
            "spring-cloud-azure", "spring-cloud-alibaba", "spring-cloud-aws",
            "spring-cloud-config", "spring-cloud-gateway", "spring-cloud-netflix",
            "spring-cloud-stream", "spring-cloud-sleuth", "spring-cloud-vault"
        ));

        // Spring Security family
        addIfNotExists(relationships, "spring-security", Arrays.asList(
            "spring-security-kerberos", "spring-security-oauth"
        ));

        // Spring Session family
        addIfNotExists(relationships, "spring-session", Arrays.asList(
            "spring-session-data-geode"
        ));

        log.debug("Added known relationships as fallback");
    }

    /**
     * Add relationship if parent doesn't already have children defined
     */
    private void addIfNotExists(Map<String, List<String>> relationships,
                               String parentSlug,
                               List<String> childSlugs) {
        if (!relationships.containsKey(parentSlug)) {
            relationships.put(parentSlug, new ArrayList<>(childSlugs));
        } else {
            // Merge children
            List<String> existing = relationships.get(parentSlug);
            for (String child : childSlugs) {
                if (!existing.contains(child)) {
                    existing.add(child);
                }
            }
        }
    }

    /**
     * Process a parent/child relationship and create database entries
     */
    private void processRelationship(String parentSlug, List<String> childSlugs, SyncResult result) {
        // Find or create parent project
        Optional<SpringProject> parentOpt = springProjectRepository.findBySlug(parentSlug);

        if (parentOpt.isEmpty()) {
            log.debug("Parent project not found: {}, skipping relationships", parentSlug);
            result.incrementSkipped(childSlugs.size());
            return;
        }

        SpringProject parentProject = parentOpt.get();

        // Process each child
        for (String childSlug : childSlugs) {
            Optional<SpringProject> childOpt = springProjectRepository.findBySlug(childSlug);

            if (childOpt.isEmpty()) {
                log.debug("Child project not found: {}, skipping relationship", childSlug);
                result.incrementSkipped();
                continue;
            }

            SpringProject childProject = childOpt.get();

            // Check if relationship already exists
            boolean exists = projectRelationshipRepository
                .existsByParentProjectAndChildProject(parentProject, childProject);

            if (exists) {
                log.trace("Relationship already exists: {} → {}", parentSlug, childSlug);
                result.incrementSkipped();
                continue;
            }

            // Create new relationship
            ProjectRelationship relationship = ProjectRelationship.builder()
                .parentProject(parentProject)
                .childProject(childProject)
                .build();

            projectRelationshipRepository.save(relationship);
            result.incrementCreated();

            log.info("Created relationship: {} → {}", parentSlug, childSlug);
        }
    }

    /**
     * Sync result holder
     */
    @Data
    public static class SyncResult {
        private boolean success;
        private String errorMessage;
        private int relationshipsCreated;
        private int relationshipsSkipped;
        private int errorsEncountered;

        public void incrementCreated() {
            this.relationshipsCreated++;
        }

        public void incrementSkipped() {
            this.relationshipsSkipped++;
        }

        public void incrementSkipped(int count) {
            this.relationshipsSkipped += count;
        }

        public void incrementErrors() {
            this.errorsEncountered++;
        }
    }
}

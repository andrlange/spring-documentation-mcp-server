package com.spring.mcp.service.sync;

import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.CodeExampleRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for syncing code examples from Spring project pages.
 * Crawls https://spring.io/projects/{slug} to extract samples from the "Samples" section.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 * @since Code Examples Feature
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExamplesSyncService {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final CodeExampleRepository codeExampleRepository;
    private final WebClient.Builder webClientBuilder;
    private final SpringGuideFetchService springGuideFetchService;
    private final GitHubSamplesFetchService gitHubSamplesFetchService;

    private static final String SPRING_IO_BASE_URL = "https://spring.io";
    private static final String PROJECT_URL_PATTERN = "%s/projects/%s";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String USER_AGENT = "Spring-MCP-Server/1.0.0 (Code Examples Fetcher)";

    /**
     * Syncs code examples from both Spring Guides and GitHub sample repositories.
     * - Spring Guides: Tutorial-style code examples with full source code
     * - GitHub Samples: Official sample repositories from Spring organizations
     *
     * @return SyncResult with statistics
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncResult syncCodeExamples() {
        log.info("Starting comprehensive code examples sync...");
        SyncResult result = new SyncResult();
        result.setStartTime(LocalDateTime.now());

        int totalExamples = 0;
        int errors = 0;

        try {
            // Sync from Spring Guides
            log.info("Phase 1/2: Syncing code examples from Spring Guides...");
            int guidesExamples = springGuideFetchService.syncSpringGuides();
            totalExamples += guidesExamples;
            log.info("Synced {} code examples from Spring Guides", guidesExamples);

            // Sync from GitHub sample repositories
            log.info("Phase 2/2: Syncing sample repositories from GitHub...");
            int githubSamples = gitHubSamplesFetchService.syncGitHubSamples();
            totalExamples += githubSamples;
            log.info("Synced {} sample repositories from GitHub", githubSamples);

            result.setExamplesCreated(totalExamples);
            result.setProjectsProcessed(2); // Guides + GitHub
            result.setSuccess(true);
            result.setSummaryMessage(String.format(
                "Synced %d total examples (%d from guides, %d from GitHub)",
                totalExamples, guidesExamples, githubSamples
            ));

            log.info("Successfully synced {} total code examples", totalExamples);

        } catch (Exception e) {
            log.error("Error during code examples sync", e);
            result.setSuccess(false);
            result.setSummaryMessage("Code examples sync failed: " + e.getMessage());
            result.addError();
            errors++;
        } finally {
            result.setEndTime(LocalDateTime.now());
            result.errorsEncountered = errors;
        }

        log.info("Code examples sync completed: {}", result.getSummaryMessage());
        return result;
    }

    /**
     * Syncs code examples for a single Spring project.
     *
     * @param projectSlug the project slug (e.g., "spring-boot", "spring-security")
     * @return SyncResult with statistics for this project
     */
    @Transactional
    public SyncResult syncProjectExamples(String projectSlug) {
        log.info("Syncing code examples for project: {}", projectSlug);
        SyncResult result = new SyncResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // Verify project exists
            SpringProject project = springProjectRepository.findBySlug(projectSlug)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectSlug));

            // Get the latest version to associate examples with
            ProjectVersion latestVersion = projectVersionRepository
                .findFirstByProjectOrderByCreatedAtDesc(project)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No versions found for project: " + projectSlug));

            // Fetch project page HTML
            String htmlContent = fetchProjectPage(projectSlug);

            // Parse HTML to extract sample links
            List<SampleData> samples = parseSamplesSection(htmlContent);
            log.debug("Parsed {} samples from page for {}", samples.size(), projectSlug);

            // Save examples to database
            int created = 0;
            int updated = 0;

            for (SampleData sample : samples) {
                try {
                    if (codeExampleRepository.existsByVersionAndSourceUrl(latestVersion, sample.getUrl())) {
                        // Example already exists, optionally update it
                        log.debug("Sample already exists: {}", sample.getTitle());
                        updated++;
                    } else {
                        // Create new example
                        CodeExample example = CodeExample.builder()
                            .version(latestVersion)
                            .title(sample.getTitle())
                            .description(sample.getDescription())
                            .sourceUrl(sample.getUrl())
                            .codeSnippet("See source repository")
                            .language("java")
                            .category(sample.getCategory())
                            .build();

                        codeExampleRepository.save(example);
                        created++;
                        log.debug("Created code example: {}", sample.getTitle());
                    }
                } catch (Exception e) {
                    log.error("Error saving code example {}: {}", sample.getTitle(), e.getMessage());
                    result.addError();
                }
            }

            result.setExamplesCreated(created);
            result.setExamplesUpdated(updated);
            result.setProjectsProcessed(1);
            result.setSuccess(true);

            log.info("Synced examples for {}: {} created, {} updated",
                projectSlug, created, updated);

        } catch (Exception e) {
            log.error("Error syncing examples for project: {}", projectSlug, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.addError();
        } finally {
            result.setEndTime(LocalDateTime.now());
        }

        return result;
    }

    /**
     * Fetches the HTML content of a Spring project page.
     */
    private String fetchProjectPage(String projectSlug) {
        String url = String.format(PROJECT_URL_PATTERN, SPRING_IO_BASE_URL, projectSlug);
        log.debug("Fetching project page: {}", url);

        WebClient webClient = webClientBuilder
            .defaultHeader("User-Agent", USER_AGENT)
            .defaultHeader("Accept", "text/html,application/xhtml+xml")
            .build();

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(REQUEST_TIMEOUT)
            .onErrorResume(error -> {
                log.error("Error fetching {}: {}", url, error.getMessage());
                return Mono.just("");
            })
            .block();
    }

    /**
     * Parses the samples section from the project page HTML.
     * Looks for sections with id or heading containing "sample" or "example".
     */
    private List<SampleData> parseSamplesSection(String htmlContent) {
        List<SampleData> samples = new ArrayList<>();

        if (htmlContent == null || htmlContent.isEmpty()) {
            log.warn("Empty HTML content provided");
            return samples;
        }

        try {
            Document doc = Jsoup.parse(htmlContent);

            // Strategy 1: Look for sections with id="samples" or similar
            Elements sampleSections = doc.select(
                "section#samples, section#examples, " +
                "div#samples, div#examples, " +
                "[id*=sample], [id*=example]"
            );

            // Strategy 2: Look for headings containing "Sample" or "Example"
            Elements sampleHeadings = doc.select(
                "h2:containsOwn(Sample), h2:containsOwn(Example), " +
                "h3:containsOwn(Sample), h3:containsOwn(Example)"
            );

            // Process sections found by ID
            for (Element section : sampleSections) {
                extractSamplesFromElement(section, samples, "General");
            }

            // Process sections found by heading
            for (Element heading : sampleHeadings) {
                String category = heading.text();
                Element nextSibling = heading.nextElementSibling();
                if (nextSibling != null) {
                    extractSamplesFromElement(nextSibling, samples, category);
                }
            }

            // Strategy 3: Look for links in specific patterns (e.g., github.com/spring-projects)
            if (samples.isEmpty()) {
                Elements githubLinks = doc.select("a[href*=github.com/spring-projects]," +
                    "a[href*=github.com/spring-guides]");
                for (Element link : githubLinks) {
                    String href = link.absUrl("href");
                    String title = link.text();
                    String description = extractDescriptionNearLink(link);

                    if (!href.isEmpty() && !title.isEmpty()) {
                        samples.add(new SampleData(title, description, href, "Sample"));
                    }
                }
            }

            log.debug("Extracted {} samples from HTML", samples.size());

        } catch (Exception e) {
            log.error("Error parsing samples from HTML: {}", e.getMessage(), e);
        }

        return samples;
    }

    /**
     * Extracts sample links from a given HTML element.
     */
    private void extractSamplesFromElement(Element element, List<SampleData> samples, String category) {
        // Look for all links in the element
        Elements links = element.select("a[href]");

        for (Element link : links) {
            String href = link.absUrl("href");
            String title = link.text();

            // Skip empty or invalid links
            if (href.isEmpty() || title.isEmpty()) {
                continue;
            }

            // Extract description from nearby text
            String description = extractDescriptionNearLink(link);

            samples.add(new SampleData(title, description, href, category));
        }
    }

    /**
     * Attempts to extract description text near a link element.
     */
    private String extractDescriptionNearLink(Element link) {
        // Try to find description in parent or sibling elements
        Element parent = link.parent();
        if (parent != null) {
            // Get all text from parent, excluding the link text
            String parentText = parent.text();
            String linkText = link.text();
            String description = parentText.replace(linkText, "").trim();

            if (!description.isEmpty() && description.length() > 10) {
                return description.length() > 500 ? description.substring(0, 500) : description;
            }
        }

        // Try next sibling
        Element nextSibling = link.nextElementSibling();
        if (nextSibling != null && nextSibling.tagName().equals("p")) {
            String text = nextSibling.text();
            return text.length() > 500 ? text.substring(0, 500) : text;
        }

        return "";
    }

    /**
     * Data structure for parsed sample information.
     */
    @Data
    @AllArgsConstructor
    private static class SampleData {
        private String title;
        private String description;
        private String url;
        private String category;
    }

    /**
     * Result object for code examples sync operation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncResult {
        private boolean success;
        private String summaryMessage;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        private int examplesCreated;
        private int examplesUpdated;
        private int projectsProcessed;
        private int errorsEncountered;

        public void addExamplesCreated(int count) {
            this.examplesCreated += count;
        }

        public void addExamplesUpdated(int count) {
            this.examplesUpdated += count;
        }

        public void addProjectsProcessed(int count) {
            this.projectsProcessed += count;
        }

        public void addError() {
            this.errorsEncountered++;
        }
    }
}

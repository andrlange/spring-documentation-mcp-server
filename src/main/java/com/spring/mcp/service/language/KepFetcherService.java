package com.spring.mcp.service.language;

import com.spring.mcp.model.entity.KepSpecification;
import com.spring.mcp.model.entity.LanguageFeature;
import com.spring.mcp.repository.KepSpecificationRepository;
import com.spring.mcp.repository.LanguageFeatureRepository;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for fetching KEP (Kotlin Enhancement Proposal) content.
 * Sources:
 * - Primary: GitHub KEEP repository (https://github.com/Kotlin/KEEP)
 * - Fallback: JetBrains YouTrack (https://youtrack.jetbrains.com)
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-12-17
 */
@Service
@Slf4j
public class KepFetcherService {

    private final KepSpecificationRepository kepRepository;
    private final LanguageFeatureRepository featureRepository;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    @Value("${mcp.features.language-evolution.sync.timeout:30000}")
    private int fetchTimeout;

    private static final String KEEP_RAW_BASE_URL = "https://raw.githubusercontent.com/Kotlin/KEEP/master/proposals/";
    private static final String KEEP_BROWSE_BASE_URL = "https://github.com/Kotlin/KEEP/blob/master/proposals/";
    private static final String YOUTRACK_BASE_URL = "https://youtrack.jetbrains.com/issue/";
    private static final String USER_AGENT = "Spring-MCP-Server/1.5.2";

    // Mapping from KEP numbers (KT-XXXXX) to KEEP proposal file names
    private static final Map<String, String> KEP_TO_KEEP_MAPPING = Map.ofEntries(
            Map.entry("KT-11550", "context-parameters"),
            Map.entry("KT-13626", "guards"),
            Map.entry("KT-1436", "break-continue-in-inline-lambdas"),
            Map.entry("KT-2462", "sealed-classes"),
            Map.entry("KT-6494", "type-classes"),
            Map.entry("KT-4107", "coroutines"),
            Map.entry("KT-14663", "contracts"),
            Map.entry("KT-10468", "value-classes")
    );

    public KepFetcherService(KepSpecificationRepository kepRepository,
                             LanguageFeatureRepository featureRepository) {
        this.kepRepository = kepRepository;
        this.featureRepository = featureRepository;

        // Initialize Flexmark parser and renderer
        MutableDataSet options = new MutableDataSet();
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Fetch and save a KEP specification by number.
     * Tries KEEP first, then falls back to YouTrack.
     *
     * @param kepNumber KEP identifier (e.g., "KT-11550" or "context-parameters")
     * @return the KEP specification, or empty if fetch failed
     */
    @Transactional
    public Optional<KepSpecification> fetchAndSave(String kepNumber) {
        // Check if already fetched
        Optional<KepSpecification> existing = kepRepository.findByKepNumber(kepNumber);
        if (existing.isPresent() && existing.get().isFetched()) {
            log.debug("KEP {} already fetched, skipping", kepNumber);
            return existing;
        }

        KepSpecification kep = null;

        // Try KEEP repository first
        String keepFileName = resolveKeepFileName(kepNumber);
        if (keepFileName != null) {
            try {
                kep = fetchFromKeep(kepNumber, keepFileName);
            } catch (Exception e) {
                log.debug("Failed to fetch KEP {} from KEEP: {}", kepNumber, e.getMessage());
            }
        }

        // Fall back to YouTrack for KT-numbers
        if (kep == null && kepNumber.startsWith("KT-")) {
            try {
                kep = fetchFromYouTrack(kepNumber);
            } catch (Exception e) {
                log.debug("Failed to fetch KEP {} from YouTrack: {}", kepNumber, e.getMessage());
            }
        }

        if (kep != null) {
            // Update existing or save new
            if (existing.isPresent()) {
                KepSpecification existingKep = existing.get();
                updateKepFields(existingKep, kep);
                return Optional.of(kepRepository.save(existingKep));
            } else {
                return Optional.of(kepRepository.save(kep));
            }
        }

        return Optional.empty();
    }

    /**
     * Resolve KEP number to KEEP proposal file name.
     */
    private String resolveKeepFileName(String kepNumber) {
        // Check mapping for KT-numbers
        if (KEP_TO_KEEP_MAPPING.containsKey(kepNumber)) {
            return KEP_TO_KEEP_MAPPING.get(kepNumber);
        }

        // If it's not a KT-number, assume it's already a file name
        if (!kepNumber.startsWith("KT-")) {
            return kepNumber;
        }

        return null;
    }

    /**
     * Fetch KEP content from GitHub KEEP repository.
     */
    private KepSpecification fetchFromKeep(String kepNumber, String fileName) throws IOException, InterruptedException {
        String rawUrl = KEEP_RAW_BASE_URL + fileName + ".md";
        String browseUrl = KEEP_BROWSE_BASE_URL + fileName + ".md";

        log.info("Fetching KEP {} from KEEP: {}", kepNumber, rawUrl);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(fetchTimeout))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rawUrl))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofMillis(fetchTimeout))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + rawUrl);
        }

        String markdown = response.body();
        return parseKeepMarkdown(kepNumber, markdown, browseUrl);
    }

    /**
     * Parse KEEP markdown content and create a KEP specification.
     */
    private KepSpecification parseKeepMarkdown(String kepNumber, String markdown, String sourceUrl) {
        KepSpecification kep = new KepSpecification();
        kep.setKepNumber(kepNumber);
        kep.setSourceType(KepSpecification.SOURCE_KEEP);
        kep.setSourceUrl(sourceUrl);
        kep.setMarkdownContent(markdown);
        kep.setFetchedAt(LocalDateTime.now());

        // Parse markdown to extract sections
        String[] lines = markdown.split("\n");
        StringBuilder currentSection = new StringBuilder();
        String currentSectionName = null;

        for (String line : lines) {
            if (line.startsWith("# ")) {
                // Main title
                kep.setTitle(line.substring(2).trim());
            } else if (line.startsWith("## ")) {
                // Save previous section
                if (currentSectionName != null) {
                    saveSectionContent(kep, currentSectionName, currentSection.toString().trim());
                }
                currentSectionName = line.substring(3).trim().toLowerCase();
                currentSection = new StringBuilder();
            } else if (currentSectionName != null) {
                currentSection.append(line).append("\n");
            }
        }

        // Save last section
        if (currentSectionName != null) {
            saveSectionContent(kep, currentSectionName, currentSection.toString().trim());
        }

        // Convert markdown to HTML
        Node document = markdownParser.parse(markdown);
        kep.setHtmlContent(htmlRenderer.render(document));

        log.debug("Parsed KEP {} from KEEP: title='{}'", kepNumber, kep.getTitle());
        return kep;
    }

    /**
     * Save section content to the appropriate field.
     */
    private void saveSectionContent(KepSpecification kep, String sectionName, String content) {
        if (content.isEmpty()) return;

        switch (sectionName) {
            case "summary", "abstract" -> kep.setSummary(content);
            case "motivation", "rationale" -> kep.setMotivation(content);
            case "description", "detailed design", "design" -> kep.setDescription(content);
            case "status" -> kep.setStatus(content);
        }
    }

    /**
     * Fetch KEP content from JetBrains YouTrack.
     */
    private KepSpecification fetchFromYouTrack(String kepNumber) throws IOException {
        String url = YOUTRACK_BASE_URL + kepNumber;
        log.info("Fetching KEP {} from YouTrack: {}", kepNumber, url);

        Document doc = Jsoup.connect(url)
                .timeout(fetchTimeout)
                .userAgent(USER_AGENT)
                .get();

        return parseYouTrackPage(kepNumber, doc, url);
    }

    /**
     * Parse YouTrack issue page and create a KEP specification.
     */
    private KepSpecification parseYouTrackPage(String kepNumber, Document doc, String sourceUrl) {
        KepSpecification kep = new KepSpecification();
        kep.setKepNumber(kepNumber);
        kep.setSourceType(KepSpecification.SOURCE_YOUTRACK);
        kep.setSourceUrl(sourceUrl);
        kep.setFetchedAt(LocalDateTime.now());

        // Extract title from page
        Element titleElement = doc.selectFirst("h1, .issue-summary, [data-test='issue-title']");
        if (titleElement != null) {
            String title = titleElement.text().trim();
            // Remove the issue number prefix if present
            if (title.startsWith(kepNumber)) {
                title = title.substring(kepNumber.length()).trim();
                if (title.startsWith(":") || title.startsWith("-")) {
                    title = title.substring(1).trim();
                }
            }
            kep.setTitle(title);
        }

        // Extract description
        Element descElement = doc.selectFirst(".issue-description, [data-test='issue-description'], .wiki-text");
        if (descElement != null) {
            kep.setDescription(descElement.text());
            kep.setHtmlContent(descElement.html());
        }

        // Extract status
        Element statusElement = doc.selectFirst("[data-test='issue-state'], .issue-state");
        if (statusElement != null) {
            kep.setStatus(statusElement.text().trim());
        }

        log.debug("Parsed KEP {} from YouTrack: title='{}'", kepNumber, kep.getTitle());
        return kep;
    }

    /**
     * Update existing KEP fields from newly fetched data.
     */
    private void updateKepFields(KepSpecification existing, KepSpecification fetched) {
        existing.setTitle(fetched.getTitle());
        existing.setSummary(fetched.getSummary());
        existing.setDescription(fetched.getDescription());
        existing.setMotivation(fetched.getMotivation());
        existing.setMarkdownContent(fetched.getMarkdownContent());
        existing.setHtmlContent(fetched.getHtmlContent());
        existing.setStatus(fetched.getStatus());
        existing.setSourceType(fetched.getSourceType());
        existing.setSourceUrl(fetched.getSourceUrl());
        existing.setFetchedAt(fetched.getFetchedAt());
    }

    /**
     * Sync all KEP specifications for features that have KEP numbers.
     *
     * @return number of KEPs synced
     */
    @Transactional
    public int syncAllKeps() {
        log.info("Starting KEP specification sync...");
        int synced = 0;

        // Get all features with KEP numbers
        List<LanguageFeature> featuresWithKeps = featureRepository.findAll().stream()
                .filter(f -> f.getKepNumber() != null && !f.getKepNumber().isBlank())
                .toList();

        log.info("Found {} features with KEP numbers", featuresWithKeps.size());

        for (LanguageFeature feature : featuresWithKeps) {
            String kepNumber = feature.getKepNumber();

            // Check if already fetched
            Optional<KepSpecification> existing = kepRepository.findByKepNumber(kepNumber);
            if (existing.isPresent() && existing.get().isFetched()) {
                continue; // Skip already fetched
            }

            // Fetch and save
            try {
                Optional<KepSpecification> fetched = fetchAndSave(kepNumber);
                if (fetched.isPresent()) {
                    synced++;
                    log.debug("Synced KEP {}: {}", kepNumber, fetched.get().getTitle());
                }

                // Rate limiting
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to sync KEP {}: {}", kepNumber, e.getMessage());
            }
        }

        log.info("KEP sync completed: {} specifications fetched", synced);
        return synced;
    }

    /**
     * Get a KEP specification, fetching on-demand if not present.
     *
     * @param kepNumber KEP identifier
     * @return the KEP specification, or empty if not found and fetch failed
     */
    @Transactional
    public Optional<KepSpecification> getKep(String kepNumber) {
        Optional<KepSpecification> existing = kepRepository.findByKepNumber(kepNumber);

        if (existing.isPresent() && existing.get().isFetched()) {
            return existing;
        }

        // Try to fetch on-demand
        return fetchAndSave(kepNumber);
    }
}

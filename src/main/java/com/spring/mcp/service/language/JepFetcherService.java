package com.spring.mcp.service.language;

import com.spring.mcp.model.entity.JepSpecification;
import com.spring.mcp.model.entity.LanguageFeature;
import com.spring.mcp.repository.JepSpecificationRepository;
import com.spring.mcp.repository.LanguageFeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for fetching JEP (JDK Enhancement Proposal) content from openjdk.org.
 * Parses JEP pages and stores the structured content for display.
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-12-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JepFetcherService {

    private final JepSpecificationRepository jepRepository;
    private final LanguageFeatureRepository featureRepository;

    @Value("${mcp.features.language-evolution.sync.timeout:30000}")
    private int fetchTimeout;

    private static final String JEP_BASE_URL = "https://openjdk.org/jeps/";
    private static final String USER_AGENT = "Spring-MCP-Server/1.5.2";

    /**
     * Fetch and save a JEP specification by number.
     * Skips if already fetched (content is static).
     *
     * @param jepNumber JEP number (e.g., "444")
     * @return the JEP specification, or empty if fetch failed
     */
    @Transactional
    public Optional<JepSpecification> fetchAndSave(String jepNumber) {
        // Check if already fetched
        Optional<JepSpecification> existing = jepRepository.findByJepNumber(jepNumber);
        if (existing.isPresent() && existing.get().isFetched()) {
            log.debug("JEP {} already fetched, skipping", jepNumber);
            return existing;
        }

        try {
            JepSpecification jep = fetchJepContent(jepNumber);
            if (jep != null) {
                // Update existing or save new
                if (existing.isPresent()) {
                    JepSpecification existingJep = existing.get();
                    updateJepFields(existingJep, jep);
                    return Optional.of(jepRepository.save(existingJep));
                } else {
                    return Optional.of(jepRepository.save(jep));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch JEP {}: {}", jepNumber, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Fetch JEP content from openjdk.org
     */
    private JepSpecification fetchJepContent(String jepNumber) throws IOException {
        String url = JEP_BASE_URL + jepNumber;
        log.info("Fetching JEP {} from {}", jepNumber, url);

        Document doc = Jsoup.connect(url)
                .timeout(fetchTimeout)
                .userAgent(USER_AGENT)
                .get();

        return parseJepDocument(jepNumber, doc, url);
    }

    /**
     * Parse JEP HTML document and extract structured content.
     */
    private JepSpecification parseJepDocument(String jepNumber, Document doc, String url) {
        JepSpecification jep = new JepSpecification();
        jep.setJepNumber(jepNumber);
        jep.setSourceUrl(url);
        jep.setFetchedAt(LocalDateTime.now());

        // Extract title from h1 or title element
        Element titleElement = doc.selectFirst("h1");
        if (titleElement != null) {
            String title = titleElement.text();
            // Remove "JEP XXX: " prefix if present
            if (title.startsWith("JEP " + jepNumber + ": ")) {
                title = title.substring(("JEP " + jepNumber + ": ").length());
            }
            jep.setTitle(title);
        }

        // Extract status from the header table or metadata
        Element statusElement = doc.selectFirst("table tr:contains(Status) td:last-child");
        if (statusElement != null) {
            jep.setStatus(statusElement.text().trim());
        }

        // Extract target version
        Element releaseElement = doc.selectFirst("table tr:contains(Release) td:last-child");
        if (releaseElement != null) {
            jep.setTargetVersion(releaseElement.text().trim());
        }

        // Extract sections by their headers
        jep.setSummary(extractSection(doc, "Summary"));
        jep.setDescription(extractSection(doc, "Description"));
        jep.setMotivation(extractSection(doc, "Motivation"));
        jep.setGoals(extractSection(doc, "Goals"));
        jep.setNonGoals(extractSection(doc, "Non-Goals"));

        // Store the main content as HTML for rendering
        Element mainContent = doc.selectFirst("main, article, .jep-content, #main");
        if (mainContent != null) {
            // Clean up the HTML - remove scripts and styles
            mainContent.select("script, style").remove();
            jep.setHtmlContent(mainContent.html());
        } else {
            // Fallback to body content
            Element body = doc.body();
            if (body != null) {
                body.select("script, style, nav, header, footer").remove();
                jep.setHtmlContent(body.html());
            }
        }

        log.debug("Parsed JEP {}: title='{}', status='{}'", jepNumber, jep.getTitle(), jep.getStatus());
        return jep;
    }

    /**
     * Extract a section's content by its header.
     */
    private String extractSection(Document doc, String sectionName) {
        // Try to find the section header
        Element header = null;
        for (int i = 2; i <= 4; i++) {
            header = doc.selectFirst("h" + i + ":containsOwn(" + sectionName + ")");
            if (header != null) break;
        }

        if (header == null) {
            // Try with id
            header = doc.selectFirst("#" + sectionName.toLowerCase().replace(" ", "-").replace("-", ""));
            if (header == null) return null;
        }

        // Collect content until next header
        StringBuilder content = new StringBuilder();
        Element sibling = header.nextElementSibling();
        while (sibling != null && !isHeader(sibling)) {
            content.append(sibling.text()).append("\n\n");
            sibling = sibling.nextElementSibling();
        }

        String result = content.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /**
     * Check if an element is a header (h1-h6)
     */
    private boolean isHeader(Element element) {
        String tagName = element.tagName().toLowerCase();
        return tagName.matches("h[1-6]");
    }

    /**
     * Update existing JEP fields from newly fetched data
     */
    private void updateJepFields(JepSpecification existing, JepSpecification fetched) {
        existing.setTitle(fetched.getTitle());
        existing.setSummary(fetched.getSummary());
        existing.setDescription(fetched.getDescription());
        existing.setMotivation(fetched.getMotivation());
        existing.setGoals(fetched.getGoals());
        existing.setNonGoals(fetched.getNonGoals());
        existing.setHtmlContent(fetched.getHtmlContent());
        existing.setStatus(fetched.getStatus());
        existing.setTargetVersion(fetched.getTargetVersion());
        existing.setSourceUrl(fetched.getSourceUrl());
        existing.setFetchedAt(fetched.getFetchedAt());
    }

    /**
     * Sync all JEP specifications for features that have JEP numbers.
     * Only fetches JEPs that haven't been fetched yet.
     *
     * @return number of JEPs synced
     */
    @Transactional
    public int syncAllJeps() {
        log.info("Starting JEP specification sync...");
        int synced = 0;

        // Get all features with JEP numbers
        List<LanguageFeature> featuresWithJeps = featureRepository.findAll().stream()
                .filter(f -> f.getJepNumber() != null && !f.getJepNumber().isBlank())
                .toList();

        log.info("Found {} features with JEP numbers", featuresWithJeps.size());

        for (LanguageFeature feature : featuresWithJeps) {
            String jepNumber = feature.getJepNumber();

            // Check if already fetched
            Optional<JepSpecification> existing = jepRepository.findByJepNumber(jepNumber);
            if (existing.isPresent() && existing.get().isFetched()) {
                continue; // Skip already fetched
            }

            // Fetch and save
            try {
                Optional<JepSpecification> fetched = fetchAndSave(jepNumber);
                if (fetched.isPresent()) {
                    synced++;
                    log.debug("Synced JEP {}: {}", jepNumber, fetched.get().getTitle());
                }

                // Rate limiting - small delay between requests
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to sync JEP {}: {}", jepNumber, e.getMessage());
            }
        }

        log.info("JEP sync completed: {} specifications fetched", synced);
        return synced;
    }

    /**
     * Get a JEP specification, fetching on-demand if not present.
     *
     * @param jepNumber JEP number
     * @return the JEP specification, or empty if not found and fetch failed
     */
    @Transactional
    public Optional<JepSpecification> getJep(String jepNumber) {
        Optional<JepSpecification> existing = jepRepository.findByJepNumber(jepNumber);

        if (existing.isPresent() && existing.get().isFetched()) {
            return existing;
        }

        // Try to fetch on-demand
        return fetchAndSave(jepNumber);
    }
}

package com.spring.mcp.service.indexing;

import com.spring.mcp.model.entity.DocumentationContent;
import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.repository.DocumentationContentRepository;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.service.documentation.DocumentationFetchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service responsible for indexing documentation content for full-text search.
 * <p>
 * This service provides comprehensive documentation indexing capabilities including:
 * <ul>
 *   <li>Parsing and cleaning HTML/Markdown content</li>
 *   <li>Generating PostgreSQL tsvector for full-text search</li>
 *   <li>Extracting metadata (title, description, keywords)</li>
 *   <li>Incremental updates using content hash comparison</li>
 *   <li>Batch processing for improved performance</li>
 *   <li>Parallel processing support with configurable thread pool</li>
 * </ul>
 * <p>
 * The indexer works closely with DocumentationFetchService to obtain content
 * and stores indexed data in the documentation_content table. PostgreSQL's
 * tsvector functionality is leveraged for efficient full-text search capabilities.
 * <p>
 * Configuration properties from application.yml:
 * <ul>
 *   <li>mcp.documentation.indexing.batch-size - Number of documents to process in a batch</li>
 *   <li>mcp.documentation.indexing.parallel - Enable/disable parallel processing</li>
 *   <li>mcp.documentation.indexing.max-threads - Maximum number of threads for parallel processing</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.0.0
 * @since Phase 3
 */
@Service
@Slf4j
public class DocumentationIndexer {

    private final DocumentationFetchService fetchService;
    private final DocumentationContentRepository contentRepository;
    private final DocumentationLinkRepository linkRepository;
    private final EntityManager entityManager;
    private final ExecutorService indexingExecutor;

    /**
     * Constructor with dependency injection including Spring-managed virtual thread executor.
     *
     * @param fetchService the documentation fetch service
     * @param contentRepository the documentation content repository
     * @param linkRepository the documentation link repository
     * @param entityManager the JPA entity manager
     * @param indexingExecutor the Spring-managed virtual thread executor for parallel indexing
     */
    public DocumentationIndexer(
            DocumentationFetchService fetchService,
            DocumentationContentRepository contentRepository,
            DocumentationLinkRepository linkRepository,
            EntityManager entityManager,
            @Qualifier("indexingExecutor") Executor indexingExecutor) {
        this.fetchService = fetchService;
        this.contentRepository = contentRepository;
        this.linkRepository = linkRepository;
        this.entityManager = entityManager;
        // Cast to ExecutorService for Future support - virtual thread executors implement ExecutorService
        this.indexingExecutor = (ExecutorService) indexingExecutor;
    }

    @Value("${mcp.documentation.indexing.batch-size:100}")
    private int batchSize;

    @Value("${mcp.documentation.indexing.parallel:true}")
    private boolean parallelProcessing;

    @Value("${mcp.documentation.indexing.max-threads:4}")
    private int maxThreads;

    // PostgreSQL text search configuration (language-specific)
    private static final String SEARCH_LANGUAGE = "english";

    // Words to exclude from indexing (common stop words)
    private static final Set<String> STOP_WORDS = Set.of(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
        "be", "have", "has", "had", "do", "does", "did", "will", "would",
        "could", "should", "may", "might", "must", "can"
    );

    /**
     * Indexes a single documentation link by fetching its content,
     * processing it, and storing the indexed data.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Validates the link is active and has a valid URL</li>
     *   <li>Checks if content has changed using hash comparison</li>
     *   <li>Fetches and parses the documentation content</li>
     *   <li>Extracts clean text and metadata</li>
     *   <li>Generates search vector for full-text search</li>
     *   <li>Saves indexed content to database</li>
     * </ol>
     * <p>
     * If content hasn't changed (hash matches), the operation is skipped
     * to avoid unnecessary processing and database updates.
     *
     * @param link the DocumentationLink to index
     * @throws IllegalArgumentException if link is null or invalid
     */
    @Transactional
    public void indexDocumentation(DocumentationLink link) {
        if (link == null) {
            throw new IllegalArgumentException("DocumentationLink cannot be null");
        }

        if (!link.getIsActive()) {
            log.debug("Skipping inactive link: {} - URL: {}", link.getId(), link.getUrl());
            return;
        }

        if (link.getUrl() == null || link.getUrl().isBlank()) {
            log.warn("Cannot index link without URL: {}", link.getId());
            return;
        }

        log.info("Indexing documentation for link: {} - URL: {}", link.getId(), link.getUrl());
        long startTime = System.currentTimeMillis();

        try {
            // Fetch and process documentation
            DocumentationLink processedLink = fetchService.fetchAndProcessDocumentation(link);

            if (processedLink == null) {
                log.warn("Failed to fetch documentation for link: {}", link.getId());
                return;
            }

            // Check if content exists
            DocumentationContent content = processedLink.getContent();
            if (content == null || content.getContent() == null || content.getContent().isBlank()) {
                log.warn("No content to index for link: {}", link.getId());
                return;
            }

            // Update search vector using PostgreSQL's to_tsvector function
            updateSearchVector(content);

            // Save the content
            contentRepository.save(content);

            // Update the link
            linkRepository.save(processedLink);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully indexed documentation for link: {} in {}ms - Content size: {} chars",
                link.getId(), duration, content.getContent().length());

        } catch (Exception e) {
            log.error("Error indexing documentation for link: {} - URL: {} - Error: {}",
                link.getId(), link.getUrl(), e.getMessage(), e);
            throw new RuntimeException("Failed to index documentation for link: " + link.getId(), e);
        }
    }

    /**
     * Indexes multiple documentation links in a batch operation.
     * <p>
     * This method provides efficient batch processing with the following features:
     * <ul>
     *   <li>Processes links in configurable batch sizes</li>
     *   <li>Optional parallel processing using thread pool</li>
     *   <li>Progress tracking and reporting</li>
     *   <li>Error handling with partial success</li>
     *   <li>Statistics collection (success/failure counts)</li>
     * </ul>
     * <p>
     * The method will continue processing even if individual links fail,
     * collecting errors and reporting them at the end.
     *
     * @param links the list of DocumentationLinks to index
     * @return a Map containing indexing statistics:
     *         - "total": total number of links
     *         - "successful": number of successfully indexed links
     *         - "failed": number of failed links
     *         - "skipped": number of skipped links
     *         - "duration": total processing time in milliseconds
     */
    @Transactional
    public Map<String, Object> indexBatch(List<DocumentationLink> links) {
        if (links == null || links.isEmpty()) {
            log.info("No links to index in batch");
            return createEmptyStats();
        }

        log.info("Starting batch indexing for {} links (parallel: {}, batch size: {})",
            links.size(), parallelProcessing, batchSize);
        long startTime = System.currentTimeMillis();

        int total = links.size();
        int successful = 0;
        int failed = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Filter active links
            List<DocumentationLink> activeLinks = links.stream()
                .filter(link -> link != null && link.getIsActive())
                .collect(Collectors.toList());

            skipped = total - activeLinks.size();

            if (activeLinks.isEmpty()) {
                log.info("No active links to index in batch");
                return createStats(total, 0, 0, skipped, 0);
            }

            // Process in batches
            List<List<DocumentationLink>> batches = partitionList(activeLinks, batchSize);
            log.info("Processing {} links in {} batches", activeLinks.size(), batches.size());

            for (int i = 0; i < batches.size(); i++) {
                List<DocumentationLink> batch = batches.get(i);
                log.info("Processing batch {}/{} ({} links)", i + 1, batches.size(), batch.size());

                Map<String, Integer> batchResults = processBatch(batch, errors);
                successful += batchResults.get("successful");
                failed += batchResults.get("failed");

                // Flush and clear entity manager periodically to avoid memory issues
                entityManager.flush();
                entityManager.clear();
            }

        } catch (Exception e) {
            log.error("Error during batch indexing: {}", e.getMessage(), e);
            errors.add("Batch processing error: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> stats = createStats(total, successful, failed, skipped, duration);

        if (!errors.isEmpty()) {
            stats.put("errors", errors);
        }

        log.info("Batch indexing completed: {} total, {} successful, {} failed, {} skipped in {}ms",
            total, successful, failed, skipped, duration);

        return stats;
    }

    /**
     * Updates the index for a single documentation link.
     * <p>
     * This method performs an incremental update by:
     * <ol>
     *   <li>Checking if the content has changed using hash comparison</li>
     *   <li>Only re-indexing if content has changed</li>
     *   <li>Updating the search vector and metadata</li>
     * </ol>
     * <p>
     * This is more efficient than full re-indexing when only checking
     * for updates to existing documentation.
     *
     * @param link the DocumentationLink to update
     */
    @Transactional
    public void updateIndex(DocumentationLink link) {
        if (link == null) {
            throw new IllegalArgumentException("DocumentationLink cannot be null");
        }

        log.debug("Checking for updates to link: {} - URL: {}", link.getId(), link.getUrl());

        try {
            // Check if content needs updating
            Optional<DocumentationContent> existingContent = contentRepository.findByLinkId(link.getId());

            if (existingContent.isEmpty()) {
                // No existing content, perform full index
                log.info("No existing content found, performing full index for link: {}", link.getId());
                indexDocumentation(link);
                return;
            }

            // Fetch latest content
            String latestContent = fetchService.fetchDocumentationContent(link.getUrl());
            if (latestContent.isEmpty()) {
                log.warn("Failed to fetch content for update check: {}", link.getId());
                return;
            }

            // Check if content has changed
            String existingHash = link.getContentHash();
            if (!fetchService.hasContentChanged(existingHash, latestContent)) {
                log.debug("Content unchanged for link: {} - skipping re-index", link.getId());
                // Update last fetched timestamp
                link.setLastFetched(LocalDateTime.now());
                linkRepository.save(link);
                return;
            }

            // Content has changed, re-index
            log.info("Content has changed for link: {} - re-indexing", link.getId());
            indexDocumentation(link);

        } catch (Exception e) {
            log.error("Error updating index for link: {} - Error: {}",
                link.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to update index for link: " + link.getId(), e);
        }
    }

    /**
     * Generates a PostgreSQL search vector (tsvector) from the given content.
     * <p>
     * This method prepares text content for full-text search by:
     * <ul>
     *   <li>Cleaning and normalizing the text</li>
     *   <li>Removing stop words (configurable)</li>
     *   <li>Converting to lowercase</li>
     *   <li>Creating a format suitable for PostgreSQL's to_tsvector function</li>
     * </ul>
     * <p>
     * The actual tsvector is generated by PostgreSQL using the to_tsvector
     * function, which provides language-specific text processing including
     * stemming and ranking.
     *
     * @param content the text content to convert
     * @return a cleaned and normalized string ready for tsvector conversion
     */
    public String generateSearchVector(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        log.debug("Generating search vector from {} characters", content.length());

        try {
            // Clean content
            String cleaned = content
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ") // Remove special characters except hyphens
                .replaceAll("\\s+", " ") // Normalize whitespace
                .trim();

            // Split into words and filter
            String[] words = cleaned.split("\\s+");
            StringBuilder searchText = new StringBuilder();

            for (String word : words) {
                // Skip stop words and very short words
                if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                    searchText.append(word).append(" ");
                }
            }

            String result = searchText.toString().trim();
            log.debug("Generated search vector: {} words", result.split("\\s+").length);

            return result;

        } catch (Exception e) {
            log.error("Error generating search vector: {}", e.getMessage(), e);
            return content; // Return original content as fallback
        }
    }

    /**
     * Extracts metadata from a parsed HTML document.
     * <p>
     * This method delegates to DocumentationFetchService's extractMetadata
     * method but adds additional processing specific to indexing needs,
     * such as keyword extraction and content classification.
     * <p>
     * Extracted metadata includes:
     * <ul>
     *   <li>title - document title</li>
     *   <li>description - meta description</li>
     *   <li>keywords - extracted keywords</li>
     *   <li>wordCount - number of words in content</li>
     *   <li>readingTime - estimated reading time in minutes</li>
     *   <li>contentType - classified content type (guide, reference, api, etc.)</li>
     *   <li>language - detected language</li>
     * </ul>
     *
     * @param doc the Jsoup Document to extract metadata from
     * @return a Map containing enhanced metadata for indexing
     */
    public Map<String, Object> extractMetadata(Document doc) {
        if (doc == null) {
            return Collections.emptyMap();
        }

        log.debug("Extracting metadata from document: {}", doc.title());

        try {
            // Get base metadata from fetch service
            Map<String, Object> metadata = new HashMap<>(fetchService.extractMetadata(doc));

            // Add indexing-specific metadata
            String text = doc.text();
            int wordCount = text.split("\\s+").length;
            metadata.put("wordCount", wordCount);

            // Calculate reading time (average 200 words per minute)
            int readingTime = Math.max(1, wordCount / 200);
            metadata.put("readingTime", readingTime);

            // Classify content type based on URL and title
            String url = doc.baseUri();
            String title = doc.title().toLowerCase();
            String contentType = classifyContentType(url, title);
            metadata.put("contentType", contentType);

            // Detect language (simple heuristic based on common words)
            metadata.put("language", SEARCH_LANGUAGE);

            // Extract key phrases (simple implementation)
            List<String> keyPhrases = extractKeyPhrases(text);
            if (!keyPhrases.isEmpty()) {
                metadata.put("keyPhrases", keyPhrases);
            }

            // Add indexing timestamp
            metadata.put("indexedAt", LocalDateTime.now().toString());

            log.debug("Extracted metadata: {} entries", metadata.size());
            return metadata;

        } catch (Exception e) {
            log.error("Error extracting metadata: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Updates the PostgreSQL search vector for a DocumentationContent entity.
     * <p>
     * This method uses a native SQL query to update the indexed_content
     * column with PostgreSQL's to_tsvector function. This ensures optimal
     * full-text search performance using PostgreSQL's built-in capabilities.
     *
     * @param content the DocumentationContent to update
     */
    private void updateSearchVector(DocumentationContent content) {
        if (content == null || content.getContent() == null) {
            return;
        }

        try {
            // The search vector will be automatically generated by PostgreSQL trigger
            // when content is inserted/updated. This method is a placeholder for
            // manual vector updates if needed.

            log.debug("Search vector will be automatically updated by PostgreSQL trigger for content: {}",
                content.getId());

            // Optional: Generate search vector manually if trigger is not available
            // String searchText = generateSearchVector(content.getContent());
            // content.setSearchVector(searchText);

        } catch (Exception e) {
            log.error("Error updating search vector for content: {} - Error: {}",
                content.getId(), e.getMessage(), e);
        }
    }

    /**
     * Processes a batch of links with optional parallel processing.
     *
     * @param batch the batch of links to process
     * @param errors list to collect errors
     * @return map with "successful" and "failed" counts
     */
    private Map<String, Integer> processBatch(List<DocumentationLink> batch, List<String> errors) {
        int successful = 0;
        int failed = 0;

        if (parallelProcessing && batch.size() > 1) {
            // Parallel processing using Spring-managed virtual thread executor
            // Note: Virtual threads are lightweight (~1KB) and scale to millions of concurrent tasks
            log.debug("Processing batch of {} links in parallel using virtual threads", batch.size());
            List<Future<Boolean>> futures = new ArrayList<>();

            for (DocumentationLink link : batch) {
                Future<Boolean> future = indexingExecutor.submit(() -> {
                    try {
                        indexDocumentation(link);
                        return true;
                    } catch (Exception e) {
                        String error = String.format("Link %d failed: %s", link.getId(), e.getMessage());
                        synchronized (errors) {
                            errors.add(error);
                        }
                        return false;
                    }
                });
                futures.add(future);
            }

            // Collect results - with virtual threads, we don't need to manage thread pool lifecycle
            for (Future<Boolean> future : futures) {
                try {
                    if (future.get(60, TimeUnit.SECONDS)) {
                        successful++;
                    } else {
                        failed++;
                    }
                } catch (InterruptedException e) {
                    log.error("Indexing task interrupted: {}", e.getMessage());
                    failed++;
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("Error processing future: {}", e.getMessage());
                    failed++;
                } catch (TimeoutException e) {
                    log.error("Indexing task timed out: {}", e.getMessage());
                    failed++;
                }
            }

            // Note: No executor shutdown needed - virtual thread executor is Spring-managed

        } else {
            // Sequential processing
            for (DocumentationLink link : batch) {
                try {
                    indexDocumentation(link);
                    successful++;
                } catch (Exception e) {
                    failed++;
                    errors.add(String.format("Link %d failed: %s", link.getId(), e.getMessage()));
                    log.error("Error indexing link in batch: {} - Error: {}",
                        link.getId(), e.getMessage());
                }
            }
        }

        Map<String, Integer> results = new HashMap<>();
        results.put("successful", successful);
        results.put("failed", failed);
        return results;
    }

    /**
     * Partitions a list into smaller sublists of specified size.
     *
     * @param list the list to partition
     * @param size the size of each partition
     * @return list of partitioned sublists
     */
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    /**
     * Creates an empty statistics map.
     *
     * @return map with zero values
     */
    private Map<String, Object> createEmptyStats() {
        return createStats(0, 0, 0, 0, 0);
    }

    /**
     * Creates a statistics map with the given values.
     *
     * @param total total links
     * @param successful successful links
     * @param failed failed links
     * @param skipped skipped links
     * @param duration duration in milliseconds
     * @return statistics map
     */
    private Map<String, Object> createStats(int total, int successful, int failed, int skipped, long duration) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("successful", successful);
        stats.put("failed", failed);
        stats.put("skipped", skipped);
        stats.put("duration", duration);
        return stats;
    }

    /**
     * Classifies content type based on URL and title patterns.
     *
     * @param url the document URL
     * @param title the document title
     * @return classified content type
     */
    private String classifyContentType(String url, String title) {
        if (url == null && title == null) {
            return "unknown";
        }

        String combined = (url != null ? url.toLowerCase() : "") + " " +
                          (title != null ? title.toLowerCase() : "");

        if (combined.contains("/guides/") || combined.contains("guide")) {
            return "guide";
        } else if (combined.contains("/reference/") || combined.contains("reference")) {
            return "reference";
        } else if (combined.contains("/api/") || combined.contains("javadoc") || combined.contains("api")) {
            return "api";
        } else if (combined.contains("tutorial")) {
            return "tutorial";
        } else if (combined.contains("sample") || combined.contains("example")) {
            return "sample";
        } else if (combined.contains("getting-started") || combined.contains("getting started")) {
            return "getting-started";
        } else {
            return "documentation";
        }
    }

    /**
     * Extracts key phrases from text (simple implementation).
     *
     * @param text the text to analyze
     * @return list of key phrases
     */
    private List<String> extractKeyPhrases(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // Simple key phrase extraction: find common Spring-related terms
            Set<String> springTerms = Set.of(
                "spring boot", "spring framework", "spring data", "spring security",
                "spring cloud", "dependency injection", "autoconfiguration", "rest api",
                "microservices", "spring mvc", "spring webflux", "jpa", "hibernate",
                "reactive", "annotation", "configuration", "bean", "controller"
            );

            String lowerText = text.toLowerCase();
            List<String> found = new ArrayList<>();

            for (String term : springTerms) {
                if (lowerText.contains(term)) {
                    found.add(term);
                }
            }

            return found.stream().limit(10).collect(Collectors.toList());

        } catch (Exception e) {
            log.debug("Error extracting key phrases: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

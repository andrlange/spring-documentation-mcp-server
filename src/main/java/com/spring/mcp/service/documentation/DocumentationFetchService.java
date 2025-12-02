package com.spring.mcp.service.documentation;

import com.spring.mcp.model.entity.DocumentationContent;
import com.spring.mcp.model.entity.DocumentationLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service responsible for fetching documentation content from external URLs.
 * Provides HTTP client functionality with retry logic, timeout handling,
 * and HTML parsing capabilities using Jsoup.
 * <p>
 * This service supports:
 * <ul>
 *   <li>Fetching documentation content from Spring documentation URLs</li>
 *   <li>Parsing HTML documents and extracting text content</li>
 *   <li>Retry logic with exponential backoff for failed requests</li>
 *   <li>Content hash calculation for change detection</li>
 *   <li>Metadata extraction from HTML documents</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.0.0
 * @since Phase 3
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentationFetchService {

    private final WebClient.Builder webClientBuilder;
    private final HtmlToMarkdownConverter htmlToMarkdownConverter;

    @Value("${mcp.documentation.fetch.timeout:30000}")
    private int fetchTimeout;

    @Value("${mcp.documentation.fetch.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${mcp.documentation.fetch.retry.delay:5000}")
    private long retryDelay;

    @Value("${mcp.documentation.fetch.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${mcp.documentation.base-urls.spring-docs:https://docs.spring.io}")
    private String springDocsBaseUrl;

    @Value("${mcp.documentation.base-urls.spring-guides:https://spring.io/guides}")
    private String springGuidesBaseUrl;

    // Pattern for validating Spring documentation URLs
    private static final Pattern SPRING_DOCS_PATTERN = Pattern.compile(
        "^https?://(docs\\.spring\\.io|spring\\.io|springdoc\\.org|www\\.baeldung\\.com).*$",
        Pattern.CASE_INSENSITIVE
    );

    // spring.io base URL
    private static final String SPRING_IO_BASE = "https://spring.io";

    // User agent to identify our service
    private static final String USER_AGENT = "Spring-MCP-Server/1.0.0 (Documentation Fetcher)";

    /**
     * Fetches documentation content from the specified URL with retry logic and timeout handling.
     * <p>
     * This method uses Spring WebFlux's WebClient to perform non-blocking HTTP requests
     * with configurable retry behavior. Failed requests are retried with exponential backoff.
     *
     * @param url the URL to fetch content from, must be a valid Spring documentation URL
     * @return the HTML content as a String, or empty string if fetching fails
     * @throws IllegalArgumentException if the URL is null, empty, or invalid
     */
    public String fetchDocumentationContent(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        if (!isValidDocumentationUrl(url)) {
            log.warn("Invalid documentation URL: {}", url);
            throw new IllegalArgumentException("URL is not a valid Spring documentation URL: " + url);
        }

        log.debug("Fetching documentation from URL: {}", url);
        long startTime = System.currentTimeMillis();

        try {
            WebClient webClient = webClientBuilder
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader("Accept-Language", "en-US,en;q=0.9")
                .defaultHeader("Accept-Encoding", "gzip, deflate")
                .build();

            String content = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(fetchTimeout))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(retryDelay))
                    .maxBackoff(Duration.ofMillis(retryDelay * (long) Math.pow(retryMultiplier, maxRetryAttempts)))
                    .filter(this::isRetryableException)
                    .doBeforeRetry(retrySignal -> {
                        log.warn("Retry attempt {} for URL: {} - Error: {}",
                            retrySignal.totalRetries() + 1, url, retrySignal.failure().getMessage());
                    }))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("HTTP error fetching URL: {} - Status: {} - Message: {}",
                        url, e.getStatusCode(), e.getMessage());
                    return Mono.just("");
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching documentation from URL: {} - Error: {}",
                        url, e.getMessage(), e);
                    return Mono.just("");
                })
                .block();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully fetched documentation from URL: {} in {}ms (size: {} bytes)",
                url, duration, content != null ? content.length() : 0);

            return content != null ? content : "";

        } catch (Exception e) {
            log.error("Unexpected error fetching documentation from URL: {} - Error: {}",
                url, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Parses HTML content into a Jsoup Document for further processing.
     * <p>
     * The method sets a base URI for resolving relative URLs within the document.
     *
     * @param html the HTML content to parse
     * @param baseUri the base URI for resolving relative URLs
     * @return a Jsoup Document object
     * @throws IllegalArgumentException if html is null or empty
     */
    public Document parseHtmlDocument(String html, String baseUri) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("HTML content cannot be null or empty");
        }

        log.debug("Parsing HTML document with base URI: {}", baseUri);
        Document doc = Jsoup.parse(html, baseUri != null ? baseUri : "");

        // Clean up the document
        doc.outputSettings()
            .prettyPrint(false)
            .charset(StandardCharsets.UTF_8);

        log.debug("Parsed HTML document: title='{}', size={}",
            doc.title(), html.length());

        return doc;
    }

    /**
     * Parses HTML content into a Jsoup Document using the default base URI.
     *
     * @param html the HTML content to parse
     * @return a Jsoup Document object
     */
    public Document parseHtmlDocument(String html) {
        return parseHtmlDocument(html, springDocsBaseUrl);
    }

    /**
     * Validates whether a URL is a valid Spring documentation URL.
     * <p>
     * Valid URLs must:
     * <ul>
     *   <li>Use HTTP or HTTPS protocol</li>
     *   <li>Point to recognized Spring documentation domains</li>
     *   <li>Be properly formatted as a URI</li>
     * </ul>
     *
     * @param url the URL to validate
     * @return true if the URL is valid, false otherwise
     */
    public boolean isValidDocumentationUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();

            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                log.debug("Invalid URL scheme: {}", scheme);
                return false;
            }

            boolean matches = SPRING_DOCS_PATTERN.matcher(url).matches();
            if (!matches) {
                log.debug("URL does not match Spring documentation pattern: {}", url);
            }

            return matches;

        } catch (URISyntaxException e) {
            log.debug("Invalid URL syntax: {} - Error: {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * Extracts clean text content from a Jsoup Document.
     * <p>
     * This method removes unwanted elements (scripts, styles, navigation, etc.)
     * and extracts the main content area. It attempts to identify the main
     * documentation content by looking for common container elements.
     *
     * @param doc the Jsoup Document to extract text from
     * @return cleaned text content, or empty string if extraction fails
     */
    public String extractTextContent(Document doc) {
        if (doc == null) {
            log.warn("Cannot extract text from null document");
            return "";
        }

        log.debug("Extracting text content from document: {}", doc.title());

        try {
            // Remove unwanted elements
            doc.select("script, style, nav, header, footer, .sidebar, .navigation, .menu, .ad, .advertisement")
                .remove();

            // Try to find main content area
            Element mainContent = findMainContent(doc);

            if (mainContent != null) {
                String text = mainContent.text();
                log.debug("Extracted {} characters from main content", text.length());
                return text.trim();
            }

            // Fallback to body text if main content not found
            String bodyText = doc.body().text();
            log.debug("Extracted {} characters from body (fallback)", bodyText.length());
            return bodyText.trim();

        } catch (Exception e) {
            log.error("Error extracting text content: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Extracts metadata from an HTML document.
     * <p>
     * Extracted metadata includes:
     * <ul>
     *   <li>title - document title</li>
     *   <li>description - meta description</li>
     *   <li>keywords - meta keywords</li>
     *   <li>author - document author</li>
     *   <li>lastModified - last modification date</li>
     *   <li>canonical - canonical URL</li>
     * </ul>
     *
     * @param doc the Jsoup Document to extract metadata from
     * @return a Map containing metadata key-value pairs
     */
    public Map<String, Object> extractMetadata(Document doc) {
        if (doc == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> metadata = new HashMap<>();

        try {
            // Title
            metadata.put("title", doc.title());

            // Meta tags
            extractMetaTag(doc, "description").ifPresent(v -> metadata.put("description", v));
            extractMetaTag(doc, "keywords").ifPresent(v -> metadata.put("keywords", v));
            extractMetaTag(doc, "author").ifPresent(v -> metadata.put("author", v));

            // Canonical URL
            Element canonical = doc.select("link[rel=canonical]").first();
            if (canonical != null) {
                metadata.put("canonical", canonical.attr("href"));
            }

            // Last modified date
            extractMetaTag(doc, "last-modified").ifPresent(v -> metadata.put("lastModified", v));

            // OG tags for social media
            extractMetaTag(doc, "og:title", "property").ifPresent(v -> metadata.put("ogTitle", v));
            extractMetaTag(doc, "og:description", "property").ifPresent(v -> metadata.put("ogDescription", v));

            // Word count
            metadata.put("wordCount", doc.text().split("\\s+").length);

            // Links count
            metadata.put("linksCount", doc.select("a[href]").size());

            log.debug("Extracted metadata: {} entries", metadata.size());

        } catch (Exception e) {
            log.error("Error extracting metadata: {}", e.getMessage(), e);
        }

        return metadata;
    }

    /**
     * Calculates a SHA-256 hash of the given content for change detection.
     *
     * @param content the content to hash
     * @return the hash as a hexadecimal string, or null if hashing fails
     */
    public String calculateContentHash(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if content has changed by comparing hashes.
     *
     * @param existingHash the existing content hash
     * @param newContent the new content to compare
     * @return true if content has changed, false otherwise
     */
    public boolean hasContentChanged(String existingHash, String newContent) {
        String newHash = calculateContentHash(newContent);
        return newHash != null && !newHash.equals(existingHash);
    }

    /**
     * Fetches and processes documentation for a DocumentationLink entity.
     * <p>
     * This method performs the complete fetch workflow:
     * <ol>
     *   <li>Fetches HTML content from the URL</li>
     *   <li>Parses the HTML document</li>
     *   <li>Extracts text content and metadata</li>
     *   <li>Calculates content hash</li>
     *   <li>Updates the DocumentationLink entity</li>
     * </ol>
     *
     * @param link the DocumentationLink to fetch content for
     * @return the updated DocumentationLink, or null if fetching fails
     */
    public DocumentationLink fetchAndProcessDocumentation(DocumentationLink link) {
        if (link == null || link.getUrl() == null) {
            log.warn("Cannot fetch documentation for null link or URL");
            return null;
        }

        try {
            log.info("Fetching documentation for link: {} - URL: {}", link.getId(), link.getUrl());

            // Fetch HTML content
            String html = fetchDocumentationContent(link.getUrl());
            if (html.isEmpty()) {
                log.warn("Failed to fetch content for link: {}", link.getId());
                return null;
            }

            // Parse document
            Document doc = parseHtmlDocument(html, link.getUrl());

            // Extract text content
            String textContent = extractTextContent(doc);

            // Extract metadata
            Map<String, Object> metadata = extractMetadata(doc);

            // Calculate content hash
            String contentHash = calculateContentHash(textContent);

            // Check if content has changed
            if (link.getContentHash() != null && link.getContentHash().equals(contentHash)) {
                log.info("Content unchanged for link: {} - URL: {}", link.getId(), link.getUrl());
                link.setLastFetched(LocalDateTime.now());
                return link;
            }

            // Update link
            link.setContentHash(contentHash);
            link.setLastFetched(LocalDateTime.now());

            // Create or update content entity
            DocumentationContent content = link.getContent();
            if (content == null) {
                content = DocumentationContent.builder()
                    .link(link)
                    .build();
                link.setContent(content);
            }

            content.setContentType("text/html");
            content.setContent(textContent);
            content.setMetadata(metadata);

            log.info("Successfully processed documentation for link: {} - Hash: {} - Content size: {} bytes",
                link.getId(), contentHash, textContent.length());

            return link;

        } catch (Exception e) {
            log.error("Error fetching and processing documentation for link: {} - URL: {} - Error: {}",
                link.getId(), link.getUrl(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts the base URL from a full URL.
     *
     * @param url the full URL
     * @return the base URL (protocol + host)
     */
    private String extractBaseUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (URISyntaxException e) {
            log.debug("Failed to extract base URL from: {}", url);
            return springDocsBaseUrl;
        }
    }

    /**
     * Determines if an exception is retryable.
     *
     * @param throwable the exception to check
     * @return true if the exception should trigger a retry
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webClientException) {
            int statusCode = webClientException.getStatusCode().value();
            // Retry on 5xx server errors and 429 (Too Many Requests)
            return statusCode >= 500 || statusCode == 429;
        }
        // Retry on timeout and connection errors
        return throwable instanceof java.util.concurrent.TimeoutException
            || throwable instanceof java.net.ConnectException
            || throwable instanceof java.io.IOException;
    }

    /**
     * Attempts to find the main content area in an HTML document.
     * <p>
     * This method looks for common content container elements used in
     * Spring documentation sites.
     *
     * @param doc the document to search
     * @return the main content Element, or null if not found
     */
    private Element findMainContent(Document doc) {
        // Try common content container selectors
        String[] contentSelectors = {
            "main",
            "article",
            "[role=main]",
            "#content",
            ".content",
            "#main-content",
            ".main-content",
            ".documentation",
            ".doc-content",
            "#body-inner",
            ".markdown-body"
        };

        for (String selector : contentSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null && !element.text().isBlank()) {
                log.debug("Found main content using selector: {}", selector);
                return element;
            }
        }

        log.debug("Main content area not found, falling back to body");
        return null;
    }

    /**
     * Extracts a meta tag value from the document.
     *
     * @param doc the document
     * @param name the meta tag name
     * @return an Optional containing the content value
     */
    private Optional<String> extractMetaTag(Document doc, String name) {
        return extractMetaTag(doc, name, "name");
    }

    /**
     * Extracts a meta tag value from the document using a specific attribute.
     *
     * @param doc the document
     * @param value the attribute value to search for
     * @param attribute the attribute name (e.g., "name" or "property")
     * @return an Optional containing the content value
     */
    private Optional<String> extractMetaTag(Document doc, String value, String attribute) {
        Element element = doc.selectFirst("meta[" + attribute + "=" + value + "]");
        if (element != null) {
            String content = element.attr("content");
            if (!content.isBlank()) {
                return Optional.of(content);
            }
        }
        return Optional.empty();
    }

    /**
     * Fetches the documentation content from a Spring project page and converts it to Markdown.
     * <p>
     * This method fetches from https://docs.spring.io/{slug}/index.html, extracts the content from
     * the article.doc element, and converts it to Markdown format preserving
     * headings, lists, code blocks, tables, and images.
     *
     * @param projectSlug the project slug (e.g., "spring-boot", "spring-cloud")
     * @return the Markdown content, or empty string if fetching fails
     */
    public String fetchProjectOverviewAsMarkdown(String projectSlug) {
        if (projectSlug == null || projectSlug.isBlank()) {
            throw new IllegalArgumentException("Project slug cannot be null or empty");
        }

        String url = "https://spring.io/projects/" + projectSlug;
        log.info("Fetching documentation content for project: {} from URL: {} (JavaScript-rendered)", projectSlug, url);

        try {
            // Fetch JavaScript-rendered HTML using HtmlUnit
            String html = fetchJavaScriptRenderedContent(url);
            if (html.isEmpty()) {
                log.warn("Failed to fetch content for project: {}", projectSlug);
                return "";
            }

            // Parse the document
            Document doc = Jsoup.parse(html, url);

            // Extract the documentation content - it's in .markdown.content element
            Element overviewContent = doc.selectFirst(".markdown.content");
            if (overviewContent == null) {
                log.warn("Documentation content not found for project: {} at selector .markdown.content", projectSlug);
                return "";
            }

            // Convert HTML to Markdown
            String markdown = htmlToMarkdownConverter.convert(overviewContent.html());

            log.info("Successfully fetched and converted OVERVIEW for project: {} - Markdown size: {} chars",
                projectSlug, markdown.length());

            return markdown;

        } catch (Exception e) {
            log.error("Error fetching project OVERVIEW for {}: {}", projectSlug, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Fetches HTML content using HtmlUnit.
     * JavaScript is disabled for performance and to avoid parsing errors with modern JS.
     * Spring.io uses Gatsby SSG, so HTML content is pre-rendered and doesn't require JS execution.
     *
     * @param url the URL to fetch
     * @return the HTML content, or empty string if fetching fails
     */
    private String fetchJavaScriptRenderedContent(String url) {
        if (url == null || url.isBlank()) {
            log.error("URL cannot be null or empty");
            return "";
        }

        log.debug("Fetching page from URL: {}", url);
        long startTime = System.currentTimeMillis();

        try (org.htmlunit.WebClient webClient = new org.htmlunit.WebClient(org.htmlunit.BrowserVersion.CHROME)) {
            // Configure HtmlUnit - JavaScript DISABLED for performance and to avoid JS parsing errors
            // Spring.io uses Gatsby with Static Site Generation, so HTML is pre-rendered
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(fetchTimeout);
            webClient.getOptions().setPrintContentOnFailingStatusCode(false);

            // Fetch the page
            org.htmlunit.html.HtmlPage page = webClient.getPage(url);

            // Get the HTML (no need to wait for JS since it's disabled)
            String html = page.asXml();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully fetched page from URL: {} in {}ms (size: {} bytes)",
                url, duration, html.length());

            return html;

        } catch (Exception e) {
            log.error("Error fetching content from URL: {} - Error: {}",
                url, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Fetches project OVERVIEW content and returns it as DocumentationContent entity.
     * This is a convenience method for creating documentation content ready to be saved.
     *
     * @param projectSlug the project slug
     * @param link the DocumentationLink to associate with the content
     * @return DocumentationContent with markdown content and metadata, or null if fetching fails
     */
    public DocumentationContent fetchProjectOverviewAsContent(String projectSlug, DocumentationLink link) {
        if (projectSlug == null || link == null) {
            log.warn("Cannot fetch content with null project slug or link");
            return null;
        }

        try {
            // Fetch and convert to markdown
            String markdown = fetchProjectOverviewAsMarkdown(projectSlug);
            if (markdown.isEmpty()) {
                return null;
            }

            // Calculate content hash
            String contentHash = calculateContentHash(markdown);

            // Update link
            link.setContentHash(contentHash);
            link.setLastFetched(LocalDateTime.now());

            // Create content entity
            DocumentationContent content = DocumentationContent.builder()
                .link(link)
                .contentType("text/markdown")
                .content(markdown)
                .metadata(Map.of(
                    "source", "spring.io",
                    "projectSlug", projectSlug,
                    "section", "overview",
                    "fetchedAt", LocalDateTime.now().toString(),
                    "contentLength", markdown.length()
                ))
                .build();

            log.info("Created DocumentationContent for project: {} - Hash: {}", projectSlug, contentHash);
            return content;

        } catch (Exception e) {
            log.error("Error creating DocumentationContent for project {}: {}", projectSlug, e.getMessage(), e);
            return null;
        }
    }
}

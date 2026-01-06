package com.spring.mcp.service.github;

import com.spring.mcp.service.documentation.HtmlToMarkdownConverter;
import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for converting AsciiDoc content to Markdown format.
 *
 * This service uses AsciidoctorJ to first convert AsciiDoc to HTML,
 * then leverages the existing HtmlToMarkdownConverter for the final conversion.
 *
 * The two-step conversion ensures we can leverage the mature HTML-to-Markdown
 * conversion logic already in place while gaining AsciiDoc source support.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@Slf4j
public class AsciiDocToMarkdownConverter {

    private final HtmlToMarkdownConverter htmlToMarkdownConverter;
    private Asciidoctor asciidoctor;

    /**
     * Patterns for AsciiDoc cross-reference links.
     */
    private static final Pattern XREF_PATTERN = Pattern.compile("xref:([^\\[]+)\\[([^\\]]+)\\]");
    private static final Pattern INCLUDE_CODE_PATTERN = Pattern.compile("include-code::([\\w.]+)\\[\\]");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("include::([^\\[]+)\\[([^\\]]*)\\]");
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

    /**
     * Patterns for post-processing markdown artifacts.
     */
    // Matches anchor artifacts like {#_test_code} or {#anchor-name}
    private static final Pattern ANCHOR_ARTIFACT_PATTERN = Pattern.compile("\\s*\\{#[^}]+\\}\\s*");
    // Matches broken table rows (pipe-separated with only dashes between)
    private static final Pattern BROKEN_TABLE_SEPARATOR = Pattern.compile("\\|[-\\s|]+\\|");
    // Matches table rows with content
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\s*\\|(.+)\\|\\s*$", Pattern.MULTILINE);

    public AsciiDocToMarkdownConverter(HtmlToMarkdownConverter htmlToMarkdownConverter) {
        this.htmlToMarkdownConverter = htmlToMarkdownConverter;
    }

    private boolean asciidoctorAvailable = false;

    /**
     * Initialize the Asciidoctor instance.
     * This is done lazily/on-demand since Asciidoctor initialization can be slow.
     * If initialization fails, we gracefully degrade (skip AsciiDoc conversion).
     */
    @PostConstruct
    public void init() {
        log.info("Initializing AsciiDocToMarkdownConverter...");
        try {
            // Create Asciidoctor instance
            this.asciidoctor = Asciidoctor.Factory.create();
            this.asciidoctorAvailable = true;
            log.info("AsciiDocToMarkdownConverter initialized successfully");
        } catch (Throwable e) {
            // Catch all throwables including JRuby errors
            log.warn("Failed to initialize Asciidoctor (AsciiDoc conversion will be skipped): {}", e.getMessage());
            this.asciidoctorAvailable = false;
        }
    }

    /**
     * Check if Asciidoctor is available for use.
     */
    public boolean isAvailable() {
        return asciidoctorAvailable;
    }

    /**
     * Clean up the Asciidoctor instance on shutdown.
     */
    @PreDestroy
    public void destroy() {
        if (asciidoctor != null) {
            try {
                asciidoctor.close();
                log.info("Asciidoctor instance closed");
            } catch (Exception e) {
                log.warn("Error closing Asciidoctor instance: {}", e.getMessage());
            }
        }
    }

    /**
     * Convert AsciiDoc content to Markdown.
     *
     * @param asciidocContent the AsciiDoc content
     * @return the Markdown content
     */
    public String convert(String asciidocContent) {
        if (asciidocContent == null || asciidocContent.isBlank()) {
            log.warn("Cannot convert null or empty AsciiDoc content");
            return "";
        }

        if (!asciidoctorAvailable) {
            log.debug("Asciidoctor not available, returning raw content");
            return asciidocContent;
        }

        try {
            // Pre-process AsciiDoc to handle special directives
            String processedAdoc = preProcess(asciidocContent);

            // Convert AsciiDoc to HTML
            String html = convertToHtml(processedAdoc);

            // Convert HTML to Markdown
            String markdown = htmlToMarkdownConverter.convert(html);

            // Post-process markdown
            markdown = postProcess(markdown);

            log.debug("Converted AsciiDoc to Markdown: {} chars -> {} chars",
                     asciidocContent.length(), markdown.length());

            return markdown;

        } catch (Exception e) {
            log.error("Error converting AsciiDoc to Markdown: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Convert AsciiDoc content with cross-reference resolution.
     *
     * @param content the AsciiDoc content
     * @param baseUrl the base URL for resolving xref links
     * @return the Markdown content with resolved links
     */
    public String convertWithXrefResolution(String content, String baseUrl) {
        // First resolve xrefs
        String resolved = resolveXrefs(content, baseUrl);

        // Then convert
        return convert(resolved);
    }

    /**
     * Pre-process AsciiDoc content before conversion.
     * Handles special directives that Asciidoctor might not process correctly.
     */
    private String preProcess(String content) {
        // Remove document title if it's just metadata (single = title)
        // We'll preserve == and below headings

        // Handle include-code:: directives - replace with placeholder
        content = INCLUDE_CODE_PATTERN.matcher(content).replaceAll(
            "**Code Example:** `$1`\n\n*(See source repository for complete implementation)*");

        // Handle include:: directives - replace with note
        content = INCLUDE_PATTERN.matcher(content).replaceAll(
            "*[Included content: $1]*");

        // Remove anchor markers but keep content
        content = ANCHOR_PATTERN.matcher(content).replaceAll("");

        return content;
    }

    /**
     * Convert AsciiDoc to HTML using AsciidoctorJ.
     */
    private String convertToHtml(String asciidocContent) {
        // Build attributes
        Attributes attributes = Attributes.builder()
            .backend("html5")
            .docType("article")
            .imagesDir("images")
            .attribute("showtitle", true)
            .attribute("last-update-label!", null)  // Disable last update label
            .attribute("source-highlighter", "")    // Disable syntax highlighting (we'll do it in markdown)
            .build();

        // Build options
        Options options = Options.builder()
            .safe(SafeMode.SAFE)
            .standalone(false)  // Don't include header/footer (renamed from headerFooter in 3.0.0)
            .attributes(attributes)
            .build();

        return asciidoctor.convert(asciidocContent, options);
    }

    /**
     * Post-process the converted Markdown.
     * Fixes common issues from AsciiDoc to HTML to Markdown conversion.
     */
    private String postProcess(String markdown) {
        // Clean up excessive whitespace
        markdown = markdown.replaceAll("\n{3,}", "\n\n");

        // Fix any escaped characters that shouldn't be escaped
        markdown = markdown.replace("\\*", "*");
        markdown = markdown.replace("\\_", "_");

        // Ensure proper code block formatting
        markdown = markdown.replaceAll("```([^\n])", "```\n$1");
        markdown = markdown.replaceAll("([^\n])```", "$1\n```");

        // Fix anchor artifacts like {#_test_code} - remove them completely
        markdown = ANCHOR_ARTIFACT_PATTERN.matcher(markdown).replaceAll(" ");

        // Fix table formatting issues
        markdown = fixTableFormatting(markdown);

        // Fix headings that appear on the same line as previous content
        // Ensure ### headings are on their own line with proper spacing
        markdown = markdown.replaceAll("([^\n])\\s*(#{1,6}\\s+)", "$1\n\n$2");

        // Clean up any double spaces created by replacements
        markdown = markdown.replaceAll("  +", " ");

        // Fix heading with anchor artifacts (e.g., "## Test Code {#_test_code}" -> "## Test Code")
        markdown = markdown.replaceAll("(#{1,6}\\s+[^\\n]+?)\\s*\\{#[^}]+\\}", "$1");

        // Trim whitespace
        markdown = markdown.trim();

        return markdown;
    }

    /**
     * Fix table formatting issues from AsciiDoc conversion.
     * Converts broken table formats to proper Markdown tables.
     */
    private String fixTableFormatting(String markdown) {
        // Split into lines for processing
        String[] lines = markdown.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inTable = false;
        boolean headerRowProcessed = false;
        int columnCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Check if this looks like a table row (starts and ends with |)
            if (line.startsWith("|") && line.endsWith("|")) {
                // Check if it's a separator row (only dashes and pipes)
                if (BROKEN_TABLE_SEPARATOR.matcher(line).matches() || line.matches("^\\|[-:\\s|]+\\|$")) {
                    // This is a separator row
                    if (inTable && !headerRowProcessed) {
                        // Generate proper separator based on column count
                        StringBuilder separator = new StringBuilder("|");
                        for (int c = 0; c < columnCount; c++) {
                            separator.append(" --- |");
                        }
                        result.append(separator).append("\n");
                        headerRowProcessed = true;
                    }
                    // Skip the original broken separator
                    continue;
                }

                // Count columns from content row
                String[] cells = line.split("\\|");
                int currentColumns = 0;
                for (String cell : cells) {
                    if (!cell.trim().isEmpty()) {
                        currentColumns++;
                    }
                }

                if (!inTable) {
                    // Starting a new table
                    inTable = true;
                    headerRowProcessed = false;
                    columnCount = currentColumns > 0 ? currentColumns : 2;
                }

                // Clean up the row - ensure proper cell formatting
                String cleanedRow = cleanTableRow(line, columnCount);
                result.append(cleanedRow).append("\n");

            } else {
                // Not a table row
                if (inTable) {
                    // Table ended
                    inTable = false;
                    headerRowProcessed = false;
                    result.append("\n");
                }
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Clean up a table row to ensure proper Markdown formatting.
     */
    private String cleanTableRow(String row, int expectedColumns) {
        // Remove leading/trailing pipes and split
        String content = row.trim();
        if (content.startsWith("|")) content = content.substring(1);
        if (content.endsWith("|")) content = content.substring(0, content.length() - 1);

        String[] cells = content.split("\\|");

        // Rebuild with proper formatting
        StringBuilder cleanRow = new StringBuilder("|");
        for (int i = 0; i < expectedColumns; i++) {
            String cell = i < cells.length ? cells[i].trim() : "";
            cleanRow.append(" ").append(cell).append(" |");
        }

        return cleanRow.toString();
    }

    /**
     * Fix common markdown conversion artifacts in existing content.
     * This can be used to re-process already converted markdown.
     *
     * @param markdown the markdown content to fix
     * @return the fixed markdown content
     */
    public String fixMarkdownArtifacts(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
        return postProcess(markdown);
    }

    /**
     * Resolve AsciiDoc cross-reference (xref) links to Markdown links.
     *
     * @param content the AsciiDoc content
     * @param baseUrl the base URL for link resolution
     * @return content with resolved xref links
     */
    public String resolveXrefs(String content, String baseUrl) {
        Matcher matcher = XREF_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String target = matcher.group(1);
            String linkText = matcher.group(2);

            // Convert .adoc extension to .md (or remove entirely for web URLs)
            String resolvedTarget = target.replace(".adoc", ".md");

            // Build the full URL if baseUrl is provided
            String url = baseUrl != null && !baseUrl.isBlank()
                ? baseUrl + "/" + resolvedTarget
                : resolvedTarget;

            // Replace with Markdown link format
            String replacement = "[" + linkText + "](" + url + ")";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Extract title from AsciiDoc content.
     *
     * @param content the AsciiDoc content
     * @return the document title, or null if not found
     */
    public String extractTitle(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        // Look for document title (= Title)
        Pattern titlePattern = Pattern.compile("^= (.+)$", Pattern.MULTILINE);
        Matcher matcher = titlePattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Look for navtitle attribute
        Pattern navTitlePattern = Pattern.compile(":navtitle: (.+)$", Pattern.MULTILINE);
        matcher = navTitlePattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * Extract description from AsciiDoc content.
     *
     * @param content the AsciiDoc content
     * @return the document description (first paragraph), or null if not found
     */
    public String extractDescription(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        // Skip metadata and title lines, find first paragraph
        String[] lines = content.split("\n");
        StringBuilder paragraph = new StringBuilder();
        boolean foundContent = false;
        boolean inPreamble = false;

        for (String line : lines) {
            // Skip attributes and empty lines at the start
            if (line.startsWith(":") || line.startsWith("=") || line.startsWith("[")) {
                if (foundContent) break; // End if we've already found content
                continue;
            }

            if (line.isBlank()) {
                if (foundContent) break; // End of first paragraph
                continue;
            }

            // Found content
            foundContent = true;
            if (paragraph.length() > 0) {
                paragraph.append(" ");
            }
            paragraph.append(line.trim());
        }

        String description = paragraph.toString().trim();
        return description.isEmpty() ? null : description;
    }

    /**
     * Extract all code blocks from AsciiDoc content.
     *
     * @param content the AsciiDoc content
     * @return list of code blocks with their language
     */
    public java.util.List<CodeBlock> extractCodeBlocks(String content) {
        java.util.List<CodeBlock> codeBlocks = new java.util.ArrayList<>();

        if (content == null || content.isBlank()) {
            return codeBlocks;
        }

        // Pattern for source blocks: [source,language] followed by ---- block
        Pattern sourceBlockPattern = Pattern.compile(
            "\\[source,([^\\]]+)\\][^\\n]*\\n----\\n([\\s\\S]*?)\\n----",
            Pattern.MULTILINE);

        Matcher matcher = sourceBlockPattern.matcher(content);

        while (matcher.find()) {
            String language = matcher.group(1).trim();
            String code = matcher.group(2).trim();
            codeBlocks.add(new CodeBlock(language, code));
        }

        return codeBlocks;
    }

    /**
     * Check if the AsciiDoc content appears to be a valid document.
     *
     * @param content the content to check
     * @return true if it appears to be valid AsciiDoc
     */
    public boolean isValidAsciiDoc(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        // Check for common AsciiDoc patterns
        boolean hasTitle = content.contains("= ");
        boolean hasAttribute = content.contains(":") && content.contains(":");
        boolean hasSection = content.contains("== ");
        boolean hasSourceBlock = content.contains("[source,");

        // At least one of these should be present
        return hasTitle || hasAttribute || hasSection || hasSourceBlock;
    }

    /**
     * Record representing a code block.
     */
    public record CodeBlock(String language, String code) {}
}

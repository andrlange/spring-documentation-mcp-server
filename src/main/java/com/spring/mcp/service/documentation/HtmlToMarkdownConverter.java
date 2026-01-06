package com.spring.mcp.service.documentation;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for converting HTML content to Markdown format.
 * Uses Flexmark HTML-to-Markdown converter with custom configuration
 * to preserve formatting like headings, lists, code blocks, tables, and images.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@Slf4j
public class HtmlToMarkdownConverter {

    private final FlexmarkHtmlConverter converter;

    // Patterns for cleaning up markdown artifacts
    private static final Pattern ANCHOR_ARTIFACT_PATTERN = Pattern.compile("\\s*\\{#[^}]+\\}\\s*");
    private static final Pattern BROKEN_TABLE_SEPARATOR = Pattern.compile("\\|[-\\s|]+\\|");

    public HtmlToMarkdownConverter() {
        // Configure the converter options
        MutableDataSet options = new MutableDataSet();

        // Configure conversion options
        options.set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false); // Use ATX headings (###)
        options.set(FlexmarkHtmlConverter.OUTPUT_UNKNOWN_TAGS, false);
        options.set(FlexmarkHtmlConverter.TYPOGRAPHIC_QUOTES, false);
        options.set(FlexmarkHtmlConverter.TYPOGRAPHIC_SMARTS, false);
        options.set(FlexmarkHtmlConverter.WRAP_AUTO_LINKS, true);
        options.set(FlexmarkHtmlConverter.EXTRACT_AUTO_LINKS, true);
        options.set(FlexmarkHtmlConverter.RENDER_COMMENTS, false);
        options.set(FlexmarkHtmlConverter.DOT_ONLY_NUMERIC_LISTS, false);
        options.set(FlexmarkHtmlConverter.PRE_CODE_PRESERVE_EMPHASIS, false);

        this.converter = FlexmarkHtmlConverter.builder(options).build();

        log.info("HtmlToMarkdownConverter initialized with custom options");
    }

    /**
     * Converts HTML content to Markdown format.
     * Cleans the HTML before conversion by removing anchor permalink elements
     * and other non-content elements.
     *
     * @param html the HTML content to convert
     * @return the Markdown representation, or empty string if conversion fails
     */
    public String convert(String html) {
        if (html == null || html.isBlank()) {
            log.warn("Cannot convert null or empty HTML");
            return "";
        }

        try {
            // Clean HTML first
            String cleanedHtml = cleanHtml(html);

            // Convert to markdown
            String markdown = converter.convert(cleanedHtml);

            // Post-process markdown
            markdown = postProcessMarkdown(markdown);

            log.debug("Converted HTML to Markdown: {} chars -> {} chars",
                html.length(), markdown.length());

            return markdown;

        } catch (Exception e) {
            log.error("Error converting HTML to Markdown: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Cleans HTML content before conversion.
     * Removes anchor permalinks, empty anchor tags, and other clutter.
     *
     * @param html the HTML to clean
     * @return cleaned HTML
     */
    private String cleanHtml(String html) {
        Document doc = Jsoup.parse(html);

        // Remove anchor permalink elements (common in documentation)
        doc.select("a.anchor, a.headerlink, a[aria-label*='permalink']").remove();

        // Remove empty anchor tags that just have the heading link
        doc.select("a[href^='#']").forEach(anchor -> {
            if (anchor.text().isBlank() || anchor.select("svg, img").size() > 0) {
                anchor.remove();
            }
        });

        // Remove SVG icons in headings
        doc.select("h1 svg, h2 svg, h3 svg, h4 svg, h5 svg, h6 svg").remove();

        // Remove any script or style tags that might have been missed
        doc.select("script, style").remove();

        // Get the body content (or the whole doc if no body)
        Element body = doc.body();
        return body != null ? body.html() : doc.html();
    }

    /**
     * Post-processes the converted markdown to clean up any issues.
     *
     * @param markdown the markdown to post-process
     * @return cleaned markdown
     */
    private String postProcessMarkdown(String markdown) {
        // Remove excessive blank lines (more than 2 consecutive)
        markdown = markdown.replaceAll("\n{3,}", "\n\n");

        // Ensure proper spacing around code blocks
        markdown = markdown.replaceAll("```([^\n])", "```\n$1");
        markdown = markdown.replaceAll("([^\n])```", "$1\n```");

        // Fix anchor artifacts like {#_test_code} - remove them completely
        markdown = ANCHOR_ARTIFACT_PATTERN.matcher(markdown).replaceAll(" ");

        // Fix heading with anchor artifacts (e.g., "## Test Code {#_test_code}" -> "## Test Code")
        markdown = markdown.replaceAll("(#{1,6}\\s+[^\\n]+?)\\s*\\{#[^}]+\\}", "$1");

        // Fix table formatting
        markdown = fixTableFormatting(markdown);

        // Fix headings that appear on the same line as previous content
        // Ensure ### headings are on their own line with proper spacing
        markdown = markdown.replaceAll("([^\n])\\s*(#{1,6}\\s+)", "$1\n\n$2");

        // Clean up any double spaces created by replacements
        markdown = markdown.replaceAll("  +", " ");

        // Trim whitespace
        markdown = markdown.trim();

        return markdown;
    }

    /**
     * Fix table formatting issues from HTML conversion.
     * Converts broken table formats to proper Markdown tables.
     */
    private String fixTableFormatting(String markdown) {
        String[] lines = markdown.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inTable = false;
        boolean headerRowProcessed = false;
        int columnCount = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|")) {
                // Check if it's a separator row (only dashes and pipes)
                if (BROKEN_TABLE_SEPARATOR.matcher(trimmedLine).matches() || trimmedLine.matches("^\\|[-:\\s|]+\\|$")) {
                    if (inTable && !headerRowProcessed) {
                        StringBuilder separator = new StringBuilder("|");
                        for (int c = 0; c < columnCount; c++) {
                            separator.append(" --- |");
                        }
                        result.append(separator).append("\n");
                        headerRowProcessed = true;
                    }
                    continue;
                }

                // Count columns from content row
                String[] cells = trimmedLine.split("\\|");
                int currentColumns = 0;
                for (String cell : cells) {
                    if (!cell.trim().isEmpty()) {
                        currentColumns++;
                    }
                }

                if (!inTable) {
                    inTable = true;
                    headerRowProcessed = false;
                    columnCount = currentColumns > 0 ? currentColumns : 2;
                }

                String cleanedRow = cleanTableRow(trimmedLine, columnCount);
                result.append(cleanedRow).append("\n");

            } else {
                if (inTable) {
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
        String content = row.trim();
        if (content.startsWith("|")) content = content.substring(1);
        if (content.endsWith("|")) content = content.substring(0, content.length() - 1);

        String[] cells = content.split("\\|");

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
        return postProcessMarkdown(markdown);
    }

    /**
     * Converts HTML content to Markdown and extracts a specific element by CSS selector first.
     *
     * @param html the full HTML page
     * @param selector the CSS selector to extract content from
     * @return the Markdown representation of the selected content
     */
    public String convertWithSelector(String html, String selector) {
        if (html == null || html.isBlank()) {
            log.warn("Cannot convert null or empty HTML");
            return "";
        }

        if (selector == null || selector.isBlank()) {
            return convert(html);
        }

        try {
            Document doc = Jsoup.parse(html);
            Element selected = doc.selectFirst(selector);

            if (selected == null) {
                log.warn("Selector '{}' not found in HTML, converting entire document", selector);
                return convert(html);
            }

            log.debug("Found element with selector '{}', converting to markdown", selector);
            return convert(selected.html());

        } catch (Exception e) {
            log.error("Error extracting element with selector '{}': {}", selector, e.getMessage());
            return convert(html);
        }
    }
}

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
     * Pattern to detect inline table separator (| --- | --- | ... |)
     */
    private static final Pattern INLINE_TABLE_SEPARATOR_PATTERN =
            Pattern.compile("\\|\\s*---\\s*\\|(?:\\s*---\\s*\\|)+");

    /**
     * Fix table formatting issues from HTML conversion.
     * Converts broken table formats to proper Markdown tables.
     * Handles both multi-line tables and inline tables (entire table on one line).
     */
    private String fixTableFormatting(String markdown) {
        // First, check for inline tables (entire table on one line) and split them
        markdown = splitInlineTables(markdown);

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
     * Split inline tables (entire table on one line) into proper multi-line format.
     * Detects tables where the separator row (| --- | --- |) appears inline with content.
     */
    private String splitInlineTables(String markdown) {
        String[] lines = markdown.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            // Check if this line contains an inline table (has separator pattern inline)
            java.util.regex.Matcher sepMatcher = INLINE_TABLE_SEPARATOR_PATTERN.matcher(line);
            // An inline table has the separator in the middle of the line (not at start)
            // and has content after the separator
            if (sepMatcher.find() && sepMatcher.start() > 0 && sepMatcher.end() < line.length()) {
                // This is likely an inline table - split it
                String splitTable = splitSingleInlineTable(line);
                result.append(splitTable);
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Split a single inline table into proper rows.
     *
     * Input format: | Header1 | Header2 | Header3 | | --- | --- | --- | | Data1 | Data2 | Data3 | | Data4 | Data5 | Data6 |
     * Output format:
     * | Header1 | Header2 | Header3 |
     * | --- | --- | --- |
     * | Data1 | Data2 | Data3 |
     * | Data4 | Data5 | Data6 |
     */
    private String splitSingleInlineTable(String line) {
        // Find the separator pattern to determine column count
        java.util.regex.Matcher sepMatcher = INLINE_TABLE_SEPARATOR_PATTERN.matcher(line);
        if (!sepMatcher.find()) {
            return line + "\n";
        }

        String separator = sepMatcher.group();
        int separatorStart = sepMatcher.start();
        int separatorEnd = sepMatcher.end();

        // Count columns from separator (count the dashes)
        int columnCount = separator.split("---").length - 1;
        if (columnCount < 1) columnCount = 1;

        // Extract header (before separator)
        String headerPart = line.substring(0, separatorStart).trim();

        // Extract data rows (after separator)
        String dataPart = line.substring(separatorEnd).trim();

        StringBuilder result = new StringBuilder();

        // Add header row
        if (!headerPart.isEmpty()) {
            if (!headerPart.startsWith("|")) headerPart = "|" + headerPart;
            if (!headerPart.endsWith("|")) headerPart = headerPart + "|";
            result.append(headerPart).append("\n");
        }

        // Add separator row
        result.append(separator).append("\n");

        // Split data part into rows using column counting
        // Each row has exactly columnCount cells, with a boundary marker between rows
        if (!dataPart.isEmpty()) {
            // Remove outer pipes and split by pipe
            if (dataPart.startsWith("|")) dataPart = dataPart.substring(1);
            if (dataPart.endsWith("|")) dataPart = dataPart.substring(0, dataPart.length() - 1);

            String[] allItems = dataPart.split("\\|", -1);

            // Group into rows: every (columnCount) items form a row,
            // followed by 1 boundary item (empty) which we skip
            java.util.List<String> currentRow = new java.util.ArrayList<>();
            for (String item : allItems) {
                if (currentRow.size() < columnCount) {
                    // Still building current row
                    currentRow.add(item.trim());
                } else {
                    // Row is complete, output it
                    StringBuilder row = new StringBuilder("|");
                    for (String cell : currentRow) {
                        row.append(" ").append(cell).append(" |");
                    }
                    result.append(row).append("\n");
                    currentRow.clear();
                    // Skip this item - it's the boundary between rows
                }
            }

            // Output any remaining row
            if (currentRow.size() == columnCount) {
                StringBuilder row = new StringBuilder("|");
                for (String cell : currentRow) {
                    row.append(" ").append(cell).append(" |");
                }
                result.append(row).append("\n");
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

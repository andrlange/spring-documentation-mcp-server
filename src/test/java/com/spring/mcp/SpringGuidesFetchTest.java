package com.spring.mcp;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Test fetching Spring Guides from spring.io/guides.
 *
 * This test explores:
 * 1. Guides index page structure
 * 2. Individual guide page structure
 * 3. Code example extraction
 * 4. Metadata (tags, categories, difficulty)
 *
 * DISABLED: This test makes external HTTP calls to spring.io.
 * Run manually when needed for integration testing.
 */
@Disabled("Makes external HTTP calls to spring.io - run manually when needed")
public class SpringGuidesFetchTest {

    private static final String GUIDES_INDEX_URL = "https://spring.io/guides";
    private static final String SAMPLE_GUIDE_URL = "https://spring.io/guides/gs/rest-service";

    @Test
    public void testAllApproaches() throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("SPRING GUIDES FETCH TEST - Exploring Structure");
        System.out.println("=".repeat(80));
        System.out.println();

        // Test 1: Fetch guides index page
        testGuidesIndexPage();

        // Test 2: Fetch individual guide
        testIndividualGuide();

        // Test 3: Extract code examples from guide
        testCodeExtraction();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST COMPLETE - Check /tmp/spring-guides-*.html for details");
        System.out.println("=".repeat(80));
    }

    /**
     * Test 1: Fetch and analyze the guides index page
     */
    private void testGuidesIndexPage() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 1: Guides Index Page");
        System.out.println("-".repeat(80));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            configureWebClient(webClient);

            System.out.println("Fetching: " + GUIDES_INDEX_URL);
            long startTime = System.currentTimeMillis();

            HtmlPage page = webClient.getPage(GUIDES_INDEX_URL);
            webClient.waitForBackgroundJavaScript(15000);

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Page fetched in " + fetchTime + " ms");

            String html = page.asXml();
            System.out.println("✓ HTML size: " + html.length() + " characters");

            // Look for guide cards/links
            System.out.println("\nSearching for guide elements:");

            String[] guideSelectors = {
                "a[href*='/guides/gs/']",
                "a[href*='/guides/topical/']",
                "a[href*='/guides/tutorials/']",
                ".guide",
                ".guide-card",
                "[data-guide]",
                ".card"
            };

            int totalGuides = 0;
            for (String selector : guideSelectors) {
                DomNodeList<DomNode> elements = page.querySelectorAll(selector);
                if (!elements.isEmpty()) {
                    System.out.println("  ✓ Found " + elements.size() + " elements with: " + selector);
                    totalGuides = Math.max(totalGuides, elements.size());

                    // Show first few guide titles/links
                    if (elements.size() > 0 && elements.size() < 20) {
                        for (int i = 0; i < Math.min(5, elements.size()); i++) {
                            DomNode element = elements.get(i);
                            String text = element.asNormalizedText();
                            if (!text.isEmpty() && text.length() < 100) {
                                System.out.println("    - " + text.trim());
                            }
                            // Try to get href
                            if (element instanceof DomElement) {
                                String href = ((DomElement) element).getAttribute("href");
                                if (href != null && !href.isEmpty()) {
                                    System.out.println("      URL: " + href);
                                }
                            }
                        }
                    }
                }
            }

            if (totalGuides == 0) {
                System.out.println("  ✗ No guide elements found with any selector");
            }

            // Save HTML for inspection
            Files.writeString(Path.of("/tmp/spring-guides-index.html"), html);
            System.out.println("\n✓ Full HTML saved to: /tmp/spring-guides-index.html");

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2: Fetch and analyze an individual guide page
     */
    private void testIndividualGuide() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 2: Individual Guide Page (RESTful Web Service)");
        System.out.println("-".repeat(80));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            configureWebClient(webClient);

            System.out.println("Fetching: " + SAMPLE_GUIDE_URL);
            long startTime = System.currentTimeMillis();

            HtmlPage page = webClient.getPage(SAMPLE_GUIDE_URL);
            webClient.waitForBackgroundJavaScript(15000);

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Page fetched in " + fetchTime + " ms");

            String html = page.asXml();
            System.out.println("✓ HTML size: " + html.length() + " characters");

            // Look for content structure
            System.out.println("\nAnalyzing guide structure:");

            // Main content
            DomElement mainContent = page.querySelector("main, .main-content, article");
            if (mainContent != null) {
                System.out.println("  ✓ Found main content container");
            }

            // Headers/sections
            DomNodeList<DomNode> headers = page.querySelectorAll("h2, h3");
            System.out.println("  ✓ Found " + headers.size() + " section headers");
            if (headers.size() > 0) {
                System.out.println("    Sections:");
                for (int i = 0; i < Math.min(10, headers.size()); i++) {
                    String headerText = headers.get(i).asNormalizedText();
                    if (!headerText.isEmpty()) {
                        System.out.println("    - " + headerText);
                    }
                }
            }

            // Code blocks
            System.out.println("\n  Looking for code blocks:");
            String[] codeSelectors = {
                "pre code",
                "pre",
                ".highlight",
                "code[class*='language-']",
                ".code-block"
            };

            int totalCodeBlocks = 0;
            for (String selector : codeSelectors) {
                DomNodeList<DomNode> codeElements = page.querySelectorAll(selector);
                if (!codeElements.isEmpty()) {
                    System.out.println("    ✓ Found " + codeElements.size() + " blocks with: " + selector);
                    totalCodeBlocks = Math.max(totalCodeBlocks, codeElements.size());
                }
            }

            if (totalCodeBlocks > 0) {
                System.out.println("    ✓ Total code blocks: " + totalCodeBlocks);
            } else {
                System.out.println("    ✗ No code blocks found");
            }

            // Save HTML
            Files.writeString(Path.of("/tmp/spring-guides-individual.html"), html);
            System.out.println("\n✓ Full HTML saved to: /tmp/spring-guides-individual.html");

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 3: Extract code examples with metadata
     */
    private void testCodeExtraction() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 3: Code Extraction with Metadata");
        System.out.println("-".repeat(80));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            configureWebClient(webClient);

            System.out.println("Fetching: " + SAMPLE_GUIDE_URL);
            HtmlPage page = webClient.getPage(SAMPLE_GUIDE_URL);
            webClient.waitForBackgroundJavaScript(15000);

            // Try to extract code blocks
            DomNodeList<DomNode> codeBlocks = page.querySelectorAll("pre code");

            if (codeBlocks.isEmpty()) {
                // Fallback to just pre
                codeBlocks = page.querySelectorAll("pre");
            }

            System.out.println("\nExtracted " + codeBlocks.size() + " code blocks:");

            List<CodeExample> examples = new ArrayList<>();

            for (int i = 0; i < Math.min(10, codeBlocks.size()); i++) {
                DomNode codeBlock = codeBlocks.get(i);
                String code = codeBlock.asNormalizedText();

                if (code.length() > 50) { // Skip very short snippets
                    CodeExample example = new CodeExample();
                    example.code = code;
                    example.index = i + 1;

                    // Try to detect language from class attribute
                    if (codeBlock instanceof DomElement) {
                        DomElement elem = (DomElement) codeBlock;
                        String className = elem.getAttribute("class");
                        if (className != null && !className.isEmpty()) {
                            if (className.contains("java")) example.language = "java";
                            else if (className.contains("xml")) example.language = "xml";
                            else if (className.contains("json")) example.language = "json";
                            else if (className.contains("yaml")) example.language = "yaml";
                            else if (className.contains("kotlin")) example.language = "kotlin";
                            else if (className.contains("groovy")) example.language = "groovy";
                        }
                    }

                    // Try to find preceding header for context
                    DomNode parent = codeBlock.getParentNode();
                    if (parent != null) {
                        // Look for h2/h3 before this code block
                        DomNodeList<DomNode> precedingHeaders = page.querySelectorAll("h2, h3");
                        for (DomNode header : precedingHeaders) {
                            // Simple heuristic: if header comes before code in DOM
                            String headerText = header.asNormalizedText();
                            if (!headerText.isEmpty()) {
                                example.context = headerText;
                                break; // Use first one found
                            }
                        }
                    }

                    examples.add(example);

                    System.out.println("\n  Example #" + example.index + ":");
                    System.out.println("    Language: " + (example.language != null ? example.language : "unknown"));
                    System.out.println("    Context: " + (example.context != null ? example.context : "none"));
                    System.out.println("    Code length: " + example.code.length() + " chars");
                    System.out.println("    Preview: " +
                        example.code.substring(0, Math.min(100, example.code.length()))
                            .replace("\n", " "));
                }
            }

            System.out.println("\n✓ Successfully extracted " + examples.size() + " code examples");

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configure WebClient with standard settings
     */
    private void configureWebClient(WebClient webClient) {
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(30000);
    }

    /**
     * Simple DTO for code examples
     */
    private static class CodeExample {
        int index;
        String code;
        String language;
        String context; // Section header or description
    }
}

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
import java.util.List;

/**
 * Test different approaches for fetching code examples from spring.io.
 *
 * This test explores:
 * 1. HtmlUnit with different JavaScript wait strategies
 * 2. JSoup for static HTML parsing
 * 3. Different selectors and page structures
 * 4. Finding the best approach before implementing in services
 *
 * DISABLED: This test makes external HTTP calls to spring.io.
 * Run manually when needed for integration testing.
 */
@Disabled("Makes external HTTP calls to spring.io - run manually when needed")
public class CodeExamplesFetchTest {

    private static final String TEST_PROJECT_URL = "https://spring.io/projects/spring-boot";
    private static final String ALT_PROJECT_URL = "https://spring.io/projects/spring-amqp";

    @Test
    public void testAllApproaches() throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("CODE EXAMPLES FETCH TEST - Exploring Different Approaches");
        System.out.println("=".repeat(80));
        System.out.println();

        // Test 1: HtmlUnit with standard settings
        testHtmlUnitStandardApproach();

        // Test 2: HtmlUnit with extended wait time
        testHtmlUnitExtendedWait();

        // Test 3: JSoup approach (for comparison)
        testJSoupApproach();

        // Test 4: Analyze page structure
        analyzePageStructure();

        // Test 5: Try alternative project
        testAlternativeProject();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST COMPLETE - Check /tmp/code-examples-*.html for details");
        System.out.println("=".repeat(80));
    }

    /**
     * Test 1: HtmlUnit with standard configuration
     * Based on the approach used for documentation fetching
     */
    private void testHtmlUnitStandardApproach() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 1: HtmlUnit Standard Approach (10s wait)");
        System.out.println("-".repeat(80));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            // Configure HtmlUnit
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(30000);

            System.out.println("Fetching: " + TEST_PROJECT_URL);
            long startTime = System.currentTimeMillis();

            HtmlPage page = webClient.getPage(TEST_PROJECT_URL);

            // Wait for JavaScript
            System.out.println("Waiting for JavaScript (10 seconds)...");
            webClient.waitForBackgroundJavaScript(10000);

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Page fetched in " + fetchTime + " ms");

            String html = page.asXml();
            System.out.println("✓ HTML size: " + html.length() + " characters");

            // Try different selectors for code examples
            String[] codeSelectors = {
                "pre code",
                ".highlight",
                ".code-example",
                "pre.language-java",
                "code.language-java",
                ".example",
                ".sample-code",
                "pre",
                "[data-language='java']",
                ".markdown.content pre code"
            };

            System.out.println("\nSearching for code examples with different selectors:");
            int totalCodeBlocks = 0;

            for (String selector : codeSelectors) {
                DomNodeList<DomNode> elements = page.querySelectorAll(selector);
                if (!elements.isEmpty()) {
                    totalCodeBlocks += elements.size();
                    System.out.println("  ✓ Found " + elements.size() + " elements with: " + selector);

                    // Show first example
                    if (elements.size() > 0) {
                        DomNode firstElement = elements.get(0);
                        String codeText = firstElement.asNormalizedText();
                        if (codeText.length() > 50) {
                            System.out.println("    First example preview: " +
                                codeText.substring(0, Math.min(100, codeText.length())).replace("\n", " "));
                        }
                    }
                }
            }

            if (totalCodeBlocks == 0) {
                System.out.println("  ✗ No code blocks found with any selector");
            }

            // Save HTML for inspection
            Files.writeString(Path.of("/tmp/code-examples-htmlunit-standard.html"), html);
            System.out.println("\n✓ Full HTML saved to: /tmp/code-examples-htmlunit-standard.html");

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2: HtmlUnit with extended wait time (30 seconds)
     * Some JavaScript-heavy pages need more time to render
     */
    private void testHtmlUnitExtendedWait() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 2: HtmlUnit Extended Wait (30s wait)");
        System.out.println("-".repeat(80));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            // Same configuration but longer wait
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(60000); // Increased to 60s

            System.out.println("Fetching: " + TEST_PROJECT_URL);
            long startTime = System.currentTimeMillis();

            HtmlPage page = webClient.getPage(TEST_PROJECT_URL);

            // Extended wait for JavaScript
            System.out.println("Waiting for JavaScript (30 seconds)...");
            int jobsRemaining = webClient.waitForBackgroundJavaScript(30000);
            System.out.println("JavaScript jobs remaining: " + jobsRemaining);

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Page fetched in " + fetchTime + " ms");

            String html = page.asXml();
            System.out.println("✓ HTML size: " + html.length() + " characters");

            // Check for specific content sections
            System.out.println("\nLooking for content sections:");
            String[] contentSelectors = {
                ".markdown.content",
                "#overview",
                "#learn",
                "#samples",
                ".tab-content",
                "main",
                "article"
            };

            for (String selector : contentSelectors) {
                DomElement element = page.querySelector(selector);
                if (element != null) {
                    String text = element.asNormalizedText();
                    System.out.println("  ✓ Found section: " + selector + " (" + text.length() + " chars)");
                } else {
                    System.out.println("  ✗ Not found: " + selector);
                }
            }

            // Save HTML
            Files.writeString(Path.of("/tmp/code-examples-htmlunit-extended.html"), html);
            System.out.println("\n✓ Full HTML saved to: /tmp/code-examples-htmlunit-extended.html");

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 3: JSoup approach (for static content - baseline comparison)
     */
    private void testJSoupApproach() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 3: JSoup Approach (No JavaScript)");
        System.out.println("-".repeat(80));

        try {
            System.out.println("Fetching: " + TEST_PROJECT_URL);
            long startTime = System.currentTimeMillis();

            Document doc = Jsoup.connect(TEST_PROJECT_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Page fetched in " + fetchTime + " ms");
            System.out.println("✓ HTML size: " + doc.html().length() + " characters");

            // Try to find code examples
            System.out.println("\nSearching for code examples:");

            Elements preElements = doc.select("pre");
            System.out.println("  Found " + preElements.size() + " <pre> elements");

            Elements codeElements = doc.select("code");
            System.out.println("  Found " + codeElements.size() + " <code> elements");

            Elements highlightElements = doc.select(".highlight, .highlighter-rouge");
            System.out.println("  Found " + highlightElements.size() + " highlight elements");

            // Check body content
            String bodyText = doc.body().text();
            System.out.println("  Body text length: " + bodyText.length() + " characters");

            if (bodyText.length() < 1000) {
                System.out.println("  ⚠ Warning: Body content seems minimal (likely JavaScript-rendered)");
            }

            // Save HTML
            Files.writeString(Path.of("/tmp/code-examples-jsoup.html"), doc.html());
            System.out.println("\n✓ Full HTML saved to: /tmp/code-examples-jsoup.html");

        } catch (IOException e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 4: Analyze the page structure to understand how code examples are organized
     */
    private void analyzePageStructure() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 4: Page Structure Analysis");
        System.out.println("-".repeat(80));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(30000);

            System.out.println("Fetching and analyzing: " + TEST_PROJECT_URL);
            HtmlPage page = webClient.getPage(TEST_PROJECT_URL);
            webClient.waitForBackgroundJavaScript(15000);

            System.out.println("\nAnalyzing DOM structure:");

            // Look for main content container
            System.out.println("\n1. Main content containers:");
            DomNodeList<DomNode> mains = page.querySelectorAll("main, .main-content, #main");
            System.out.println("   Found " + mains.size() + " main containers");

            // Look for tabs
            System.out.println("\n2. Tab structure:");
            DomNodeList<DomNode> tabs = page.querySelectorAll("[role='tab'], .tab, .nav-tabs");
            System.out.println("   Found " + tabs.size() + " tab elements");
            for (DomNode tab : tabs) {
                String tabText = tab.asNormalizedText();
                if (tabText.length() < 50) {
                    System.out.println("   - Tab: " + tabText);
                }
            }

            // Look for documentation sections
            System.out.println("\n3. Documentation sections:");
            String[] sectionIds = {"overview", "learn", "support", "samples"};
            for (String sectionId : sectionIds) {
                DomElement section = page.getElementById(sectionId);
                if (section != null) {
                    System.out.println("   ✓ Found section: " + sectionId);
                } else {
                    System.out.println("   ✗ Missing section: " + sectionId);
                }
            }

            // Look for markdown content
            System.out.println("\n4. Markdown content areas:");
            DomNodeList<DomNode> markdownAreas = page.querySelectorAll(".markdown, [class*='markdown'], .content");
            System.out.println("   Found " + markdownAreas.size() + " markdown-related elements");

            // Look for code blocks by language
            System.out.println("\n5. Language-specific code:");
            String[] languages = {"java", "kotlin", "groovy", "xml", "yaml", "json", "bash"};
            for (String lang : languages) {
                DomNodeList<DomNode> langBlocks = page.querySelectorAll(
                    "code.language-" + lang + ", pre.language-" + lang + ", [data-lang='" + lang + "']"
                );
                if (!langBlocks.isEmpty()) {
                    System.out.println("   ✓ Found " + langBlocks.size() + " " + lang.toUpperCase() + " code blocks");
                }
            }

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 5: Try a different project to see if structure is consistent
     */
    private void testAlternativeProject() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TEST 5: Alternative Project (Spring AMQP)");
        System.out.println("-".repeat(80));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(30000);

            System.out.println("Fetching: " + ALT_PROJECT_URL);
            HtmlPage page = webClient.getPage(ALT_PROJECT_URL);
            webClient.waitForBackgroundJavaScript(15000);

            String html = page.asXml();
            System.out.println("✓ HTML size: " + html.length() + " characters");

            // Quick check for code examples
            DomNodeList<DomNode> codeBlocks = page.querySelectorAll("pre code");
            System.out.println("✓ Found " + codeBlocks.size() + " <pre><code> blocks");

            // Check for specific selectors
            String[] testSelectors = {
                ".markdown.content",
                "#samples",
                "pre.language-java"
            };

            for (String selector : testSelectors) {
                DomElement element = page.querySelector(selector);
                if (element != null) {
                    System.out.println("  ✓ Found: " + selector);
                } else {
                    System.out.println("  ✗ Not found: " + selector);
                }
            }

            // Save HTML
            Files.writeString(Path.of("/tmp/code-examples-spring-amqp.html"), html);
            System.out.println("\n✓ Full HTML saved to: /tmp/code-examples-spring-amqp.html");

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

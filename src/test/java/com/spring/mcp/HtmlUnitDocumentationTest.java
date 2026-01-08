package com.spring.mcp;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.DomElement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test HtmlUnit's ability to fetch JavaScript-rendered Spring documentation.
 *
 * DISABLED: This test makes external HTTP calls to spring.io and docs.spring.io.
 * Run manually when needed for integration testing.
 */
@Disabled("Makes external HTTP calls to spring.io - run manually when needed")
public class HtmlUnitDocumentationTest {

    @Test
    public void testFetchSpringIoWithJavaScript() throws Exception {
        System.out.println("=== HtmlUnit Test: Fetching Spring Boot Documentation ===\n");

        // Test 1: Fetch from spring.io/projects/spring-boot (JavaScript-rendered)
        testSpringIoWithHtmlUnit();

        // Test 2: Fetch from docs.spring.io/spring-boot/index.html (static HTML)
        testDocsSpringIo();
    }

    /**
     * Test fetching JavaScript-rendered content from spring.io
     */
    private void testSpringIoWithHtmlUnit() {
        System.out.println("TEST 1: Fetching https://spring.io/projects/spring-boot");
        System.out.println("-------------------------------------------------------");

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            // Configure HtmlUnit
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(30000); // 30 seconds

            System.out.println("Fetching page...");
            long startTime = System.currentTimeMillis();

            HtmlPage page = webClient.getPage("https://spring.io/projects/spring-boot");

            // Wait for JavaScript to execute
            System.out.println("Waiting for JavaScript execution...");
            webClient.waitForBackgroundJavaScript(10000); // Wait up to 10 seconds

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("Page fetched in " + fetchTime + " ms");

            // Get the full HTML
            String html = page.asXml();
            int htmlSize = html.length();
            System.out.println("HTML size: " + htmlSize + " characters");

            // Try to find the OVERVIEW section
            DomElement overviewContent = page.querySelector(".markdown.content");
            if (overviewContent != null) {
                String overviewText = overviewContent.asNormalizedText();
                System.out.println("\n✓ OVERVIEW section found!");
                System.out.println("Content length: " + overviewText.length() + " characters");
                System.out.println("\nFirst 500 characters:");
                System.out.println(overviewText.substring(0, Math.min(500, overviewText.length())));

                // Save to file for inspection
                Files.writeString(Path.of("/tmp/htmlunit-spring-io.html"), html);
                System.out.println("\nFull HTML saved to: /tmp/htmlunit-spring-io.html");
            } else {
                System.out.println("\n✗ OVERVIEW section NOT found using selector: .markdown.content");
                System.out.println("Searching for alternative selectors...");

                // Try alternative selectors
                String[] selectors = {
                    "#overview",
                    ".tab-content",
                    "[data-tab='overview']",
                    "main .content",
                    "article"
                };

                for (String selector : selectors) {
                    DomElement element = page.querySelector(selector);
                    if (element != null) {
                        System.out.println("  ✓ Found element with selector: " + selector);
                        System.out.println("    Text length: " + element.asNormalizedText().length());
                    }
                }

                // Save HTML for manual inspection
                Files.writeString(Path.of("/tmp/htmlunit-spring-io.html"), html);
                System.out.println("\nFull HTML saved for inspection: /tmp/htmlunit-spring-io.html");
            }

        } catch (IOException e) {
            System.err.println("Error fetching page: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n");
    }

    /**
     * Test fetching static HTML from docs.spring.io (for comparison)
     */
    private void testDocsSpringIo() {
        System.out.println("TEST 2: Fetching https://docs.spring.io/spring-boot/index.html");
        System.out.println("----------------------------------------------------------------");

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            // Minimal configuration since no JavaScript needed
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(10000);

            System.out.println("Fetching page...");
            long startTime = System.currentTimeMillis();

            HtmlPage page = webClient.getPage("https://docs.spring.io/spring-boot/index.html");

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("Page fetched in " + fetchTime + " ms");

            String html = page.asXml();
            System.out.println("HTML size: " + html.length() + " characters");

            // Find article content
            DomElement article = page.querySelector("article.doc");
            if (article != null) {
                String content = article.asNormalizedText();
                System.out.println("\n✓ Article content found!");
                System.out.println("Content length: " + content.length() + " characters");
                System.out.println("\nFirst 300 characters:");
                System.out.println(content.substring(0, Math.min(300, content.length())));

                Files.writeString(Path.of("/tmp/htmlunit-docs-spring-io.html"), html);
                System.out.println("\nFull HTML saved to: /tmp/htmlunit-docs-spring-io.html");
            } else {
                System.out.println("\n✗ Article content NOT found");
            }

        } catch (IOException e) {
            System.err.println("Error fetching page: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== Test Complete ===");
    }
}

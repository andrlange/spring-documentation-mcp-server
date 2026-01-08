package com.spring.mcp;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Test to explore fetching samples from Spring project pages with JavaScript tabs.
 * Example URL: https://spring.io/projects/spring-cloud#samples
 *
 * The #samples fragment indicates a client-side tab that needs JavaScript rendering.
 *
 * DISABLED: This test makes external HTTP calls to spring.io.
 * Run manually when needed for integration testing.
 */
@Disabled("Makes external HTTP calls to spring.io - run manually when needed")
public class ProjectPageSamplesTest {

    private static final String SPRING_CLOUD_URL = "https://spring.io/projects/spring-cloud";
    private static final String SPRING_BOOT_URL = "https://spring.io/projects/spring-boot";
    private static final String SPRING_SECURITY_URL = "https://spring.io/projects/spring-security";

    @Test
    public void testExtractSamplesFromProjectPages() throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("PROJECT PAGE SAMPLES EXTRACTION TEST");
        System.out.println("=".repeat(80));
        System.out.println();

        // Test multiple projects
        testProjectSamples("spring-cloud", SPRING_CLOUD_URL);
        testProjectSamples("spring-boot", SPRING_BOOT_URL);
        testProjectSamples("spring-security", SPRING_SECURITY_URL);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST COMPLETE - Check /tmp/project-samples-*.html for details");
        System.out.println("=".repeat(80));
    }

    private void testProjectSamples(String projectSlug, String projectUrl) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("Testing: " + projectSlug);
        System.out.println("-".repeat(80));

        try (WebClient webClient = createWebClient()) {
            System.out.println("Fetching: " + projectUrl);
            long startTime = System.currentTimeMillis();

            HtmlPage page = webClient.getPage(projectUrl);

            // Wait for JavaScript to render the page
            System.out.println("Waiting for JavaScript to execute (30 seconds)...");
            webClient.waitForBackgroundJavaScript(30000);
            Thread.sleep(5000); // Additional wait to ensure all AJAX calls complete

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Page loaded in " + fetchTime + " ms");

            String html = page.asXml();
            System.out.println("✓ HTML size: " + html.length() + " characters");

            // Strategy 1: Look for samples section by ID
            System.out.println("\nStrategy 1: Looking for #samples section...");
            DomElement samplesSection = page.getElementById("samples");
            if (samplesSection != null) {
                System.out.println("  ✓ Found #samples section!");
                analyzeSamplesSection(samplesSection, "ID selector");
            } else {
                System.out.println("  ✗ No #samples section found");
            }

            // Strategy 2: Look for tab with samples content
            System.out.println("\nStrategy 2: Looking for samples tab content...");
            String[] samplesSelectors = {
                "[id='samples']",
                "[data-tab='samples']",
                ".tab-pane#samples",
                ".samples-section",
                "[role='tabpanel'][aria-labelledby*='samples']"
            };

            for (String selector : samplesSelectors) {
                DomNodeList<DomNode> elements = page.querySelectorAll(selector);
                if (!elements.isEmpty()) {
                    System.out.println("  ✓ Found " + elements.size() + " element(s) with: " + selector);
                    for (DomNode element : elements) {
                        if (element instanceof DomElement) {
                            analyzeSamplesSection((DomElement) element, selector);
                        }
                    }
                }
            }

            // Strategy 3: Look for any links containing "sample", "example", "github"
            System.out.println("\nStrategy 3: Looking for sample-related links...");
            DomNodeList<DomNode> allLinks = page.querySelectorAll("a[href]");
            List<SampleLink> sampleLinks = new ArrayList<>();

            for (DomNode linkNode : allLinks) {
                if (linkNode instanceof DomElement) {
                    DomElement link = (DomElement) linkNode;
                    String href = link.getAttribute("href");
                    String text = link.asNormalizedText();

                    // Filter for sample-related links
                    if (href.contains("github.com") &&
                        (href.contains("sample") || href.contains("example") ||
                         text.toLowerCase().contains("sample") || text.toLowerCase().contains("example"))) {

                        sampleLinks.add(new SampleLink(text, href));
                    }
                }
            }

            System.out.println("  Found " + sampleLinks.size() + " potential sample links:");
            for (int i = 0; i < Math.min(10, sampleLinks.size()); i++) {
                SampleLink link = sampleLinks.get(i);
                System.out.println("    - " + link.text);
                System.out.println("      URL: " + link.url);
            }

            // Strategy 4: Search for specific patterns in the HTML
            System.out.println("\nStrategy 4: Analyzing HTML structure...");
            if (html.contains("id=\"samples\"") || html.contains("id='samples'")) {
                System.out.println("  ✓ HTML contains samples ID");
            }
            if (html.contains("data-tab=\"samples\"") || html.contains("data-tab='samples'")) {
                System.out.println("  ✓ HTML contains samples tab attribute");
            }
            if (html.contains("Samples") || html.contains("SAMPLES")) {
                System.out.println("  ✓ HTML contains 'Samples' text");

                // Try to find the context around "Samples"
                int samplesIndex = html.toLowerCase().indexOf("samples");
                if (samplesIndex > 0) {
                    int start = Math.max(0, samplesIndex - 200);
                    int end = Math.min(html.length(), samplesIndex + 200);
                    String context = html.substring(start, end);
                    System.out.println("  Context around 'Samples':");
                    System.out.println("  " + context.replace("\n", " ").replaceAll("\\s+", " "));
                }
            }

            // Save HTML for manual inspection
            String filename = "/tmp/project-samples-" + projectSlug + ".html";
            Files.writeString(Path.of(filename), html);
            System.out.println("\n✓ Full HTML saved to: " + filename);

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void analyzeSamplesSection(DomElement samplesSection, String foundBy) {
        System.out.println("  Analyzing samples section (found by: " + foundBy + "):");

        // Get all links in the samples section
        DomNodeList<DomNode> links = samplesSection.querySelectorAll("a[href]");
        System.out.println("    Links found: " + links.size());

        for (int i = 0; i < Math.min(5, links.size()); i++) {
            if (links.get(i) instanceof DomElement) {
                DomElement link = (DomElement) links.get(i);
                String href = link.getAttribute("href");
                String text = link.asNormalizedText();
                System.out.println("      - " + text);
                System.out.println("        URL: " + href);
            }
        }

        // Get text content
        String content = samplesSection.asNormalizedText();
        if (content.length() > 0) {
            System.out.println("    Content preview: " +
                content.substring(0, Math.min(200, content.length())));
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(60000); // 60 seconds
        return webClient;
    }

    private static class SampleLink {
        String text;
        String url;

        SampleLink(String text, String url) {
            this.text = text;
            this.url = url;
        }
    }
}

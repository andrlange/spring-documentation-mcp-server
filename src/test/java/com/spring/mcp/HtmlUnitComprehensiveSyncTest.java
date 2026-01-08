package com.spring.mcp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.htmlunit.WebClient;
import org.htmlunit.BrowserVersion;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Comprehensive test of HtmlUnit-based documentation fetching for multiple Spring projects.
 *
 * DISABLED: This test makes external HTTP calls to spring.io.
 * Run manually when needed for integration testing.
 */
@Disabled("Makes external HTTP calls to spring.io - run manually when needed")
public class HtmlUnitComprehensiveSyncTest {

    @Test
    public void testFetchMultipleSpringProjects() throws Exception {
        System.out.println("=== Testing HtmlUnit Documentation Sync for Multiple Projects ===\n");

        String[] projects = {"spring-boot", "spring-framework", "spring-data", "spring-security", "spring-cloud"};

        for (String project : projects) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Project: " + project);
            System.out.println("=".repeat(60));

            String url = "https://spring.io/projects/" + project;
            System.out.println("URL: " + url);

            long startTime = System.currentTimeMillis();

            try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
                // Configure HtmlUnit
                webClient.getOptions().setJavaScriptEnabled(true);
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setThrowExceptionOnScriptError(false);
                webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                webClient.getOptions().setTimeout(30000);

                // Fetch the page
                HtmlPage page = webClient.getPage(url);

                // Wait for JavaScript to execute
                webClient.waitForBackgroundJavaScript(10000);

                // Get the rendered HTML
                String html = page.asXml();
                long fetchDuration = System.currentTimeMillis() - startTime;

                System.out.println("✓ Fetched in " + fetchDuration + "ms (size: " + html.length() + " bytes)");

                // Parse with Jsoup
                Document doc = Jsoup.parse(html, url);

                // Extract the documentation content
                Element overviewContent = doc.selectFirst(".markdown.content");

                if (overviewContent != null) {
                    String overviewHtml = overviewContent.html();
                    System.out.println("✓ OVERVIEW content found: " + overviewHtml.length() + " characters");

                    // Convert to markdown
                    FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
                    String markdown = converter.convert(overviewHtml);

                    System.out.println("✓ Converted to Markdown: " + markdown.length() + " characters");
                    System.out.println("\nMarkdown Preview (first 300 chars):");
                    System.out.println("-".repeat(60));
                    System.out.println(markdown.substring(0, Math.min(300, markdown.length())));
                    System.out.println("-".repeat(60));

                    // Save to file
                    String filename = "/tmp/htmlunit-" + project + ".md";
                    Files.writeString(Path.of(filename), markdown);
                    System.out.println("\n✓ Saved to: " + filename);

                } else {
                    System.out.println("✗ ERROR: .markdown.content not found!");
                    // Save full HTML for debugging
                    Files.writeString(Path.of("/tmp/htmlunit-" + project + "-debug.html"), html);
                    System.out.println("✗ Saved HTML for debugging to: /tmp/htmlunit-" + project + "-debug.html");
                }

            } catch (Exception e) {
                System.out.println("✗ ERROR: " + e.getMessage());
                e.printStackTrace();
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            System.out.println("\nTotal processing time: " + totalDuration + "ms");
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("SYNC TEST COMPLETED");
        System.out.println("=".repeat(60));
    }
}

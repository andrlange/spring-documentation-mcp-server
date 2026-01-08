package com.spring.mcp;

import com.spring.mcp.repository.DocumentationContentRepository;
import com.spring.mcp.service.sync.DocumentationSyncService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full integration test to sync all Spring project documentation using HtmlUnit.
 *
 * DISABLED: This test makes external HTTP calls to spring.io and GitHub.
 * Run manually when needed for integration testing.
 */
@Disabled("Makes external HTTP calls to spring.io and GitHub - run manually when needed")
@SpringBootTest
@Transactional
public class FullDocumentationSyncTest {

    @Autowired
    private DocumentationSyncService documentationSyncService;

    @Autowired
    private DocumentationContentRepository documentationContentRepository;

    @Test
    public void testFullDocumentationSync() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FULL DOCUMENTATION SYNC TEST - Using HtmlUnit");
        System.out.println("=".repeat(80) + "\n");

        long startTime = System.currentTimeMillis();

        try {
            // Trigger comprehensive sync
            System.out.println("Triggering comprehensive documentation sync...\n");
            var result = documentationSyncService.syncAllDocumentation();

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("\n" + "=".repeat(80));
            System.out.println("SYNC COMPLETED SUCCESSFULLY!");
            System.out.println("Total time: " + (duration / 1000) + " seconds");
            System.out.println("Sync Result: " + result);
            System.out.println("=".repeat(80) + "\n");

            // Verify documentation was saved correctly
            long totalDocs = documentationContentRepository.count();
            System.out.println("Total documentation records in database: " + totalDocs);

            // Check a few sample records
            var sampleDocs = documentationContentRepository.findAll().stream()
                .limit(5)
                .toList();

            System.out.println("\nSample Documentation Content:");
            System.out.println("-".repeat(80));
            sampleDocs.forEach(doc -> {
                System.out.println("Link: " + doc.getLink().getTitle());
                System.out.println("Content Length: " + (doc.getContent() != null ? doc.getContent().length() : 0) + " characters");
                if (doc.getContent() != null && doc.getContent().length() > 0) {
                    System.out.println("Preview: " + doc.getContent().substring(0, Math.min(100, doc.getContent().length())) + "...");
                }
                System.out.println("-".repeat(80));
            });

            // Assert that documentation was actually saved
            assertTrue(totalDocs > 0, "No documentation was saved to database!");
            sampleDocs.forEach(doc -> {
                assertTrue(doc.getContent() != null && doc.getContent().length() > 100,
                    "Content too short or empty for: " + doc.getLink().getTitle());
            });

            System.out.println("\n✓ All assertions passed! Documentation saved correctly.\n");

        } catch (Exception e) {
            System.err.println("\n✗ ERROR during sync: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

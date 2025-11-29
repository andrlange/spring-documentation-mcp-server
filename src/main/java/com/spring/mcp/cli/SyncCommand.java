package com.spring.mcp.cli;

import com.spring.mcp.service.sync.DocumentationSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Command-line runner to trigger documentation sync
 * Activate with: --sync.on-startup=true
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sync.on-startup", havingValue = "true")
public class SyncCommand implements CommandLineRunner {

    private final DocumentationSyncService documentationSyncService;

    @Override
    public void run(String... args) throws Exception {
        log.info("={}", "=".repeat(79));
        log.info("DOCUMENTATION SYNC - Starting comprehensive sync");
        log.info("={}", "=".repeat(79));

        long startTime = System.currentTimeMillis();

        var result = documentationSyncService.syncAllDocumentation();

        long duration = System.currentTimeMillis() - startTime;

        log.info("");
        log.info("={}", "=".repeat(79));
        log.info("SYNC COMPLETED SUCCESSFULLY!");
        log.info("Duration: {} seconds", duration / 1000);
        log.info("Result: {}", result);
        log.info("={}", "=".repeat(79));
    }
}

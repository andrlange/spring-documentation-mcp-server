package com.spring.mcp.service.wiki;

import com.spring.mcp.model.entity.WikiMigrationGuide;
import com.spring.mcp.model.entity.WikiReleaseNotes;
import com.spring.mcp.model.event.SyncProgressEvent;
import com.spring.mcp.repository.WikiMigrationGuideRepository;
import com.spring.mcp.repository.WikiReleaseNotesRepository;
import com.spring.mcp.service.github.AsciiDocToMarkdownConverter;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for syncing Spring Boot Wiki content (Release Notes and Migration Guides)
 * from the GitHub wiki repository.
 *
 * The wiki content is in AsciiDoc format and is converted to Markdown for storage.
 * Files are cloned from https://github.com/spring-projects/spring-boot.wiki.git
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WikiSyncService {

    private static final String WIKI_REPO_URL = "https://github.com/spring-projects/spring-boot.wiki.git";
    private static final String WIKI_BASE_URL = "https://github.com/spring-projects/spring-boot/wiki";

    // Patterns for wiki file names (files use .asciidoc extension)
    private static final Pattern RELEASE_NOTES_PATTERN = Pattern.compile(
            "Spring-Boot-([0-9]+)\\.([0-9]+)-Release-Notes\\.asciidoc", Pattern.CASE_INSENSITIVE);
    // Migration guides are named by target version only (e.g., Spring-Boot-3.0-Migration-Guide.asciidoc)
    private static final Pattern MIGRATION_GUIDE_PATTERN = Pattern.compile(
            "Spring-Boot-([0-9]+)\\.([0-9]+)-Migration-Guide\\.asciidoc", Pattern.CASE_INSENSITIVE);

    private final WikiReleaseNotesRepository releaseNotesRepository;
    private final WikiMigrationGuideRepository migrationGuideRepository;
    private final AsciiDocToMarkdownConverter asciiDocConverter;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;

    /**
     * Sync all wiki content from the GitHub repository.
     *
     * @return SyncResult with statistics about the sync operation
     */
    public SyncResult syncWikiContent() {
        log.info("=".repeat(60));
        log.info("WIKI SYNC STARTED - Fetching from Spring Boot Wiki");
        log.info("=".repeat(60));

        SyncResult result = new SyncResult();
        result.setStartTime(LocalDateTime.now());

        Path wikiDir = null;
        try {
            // Clone or update the wiki repository
            publishProgress("Cloning wiki repository...", 5);
            wikiDir = cloneWikiRepository();

            if (wikiDir == null) {
                result.setSuccess(false);
                result.setErrorMessage("Failed to clone wiki repository");
                return result;
            }

            // Find and process all wiki files
            publishProgress("Scanning for wiki files...", 15);
            List<Path> asciidocFiles = findAsciidocFiles(wikiDir);
            log.info("Found {} AsciiDoc files in wiki", asciidocFiles.size());

            // Separate release notes and migration guides
            List<Path> releaseNotesFiles = new ArrayList<>();
            List<Path> migrationGuideFiles = new ArrayList<>();

            for (Path file : asciidocFiles) {
                String fileName = file.getFileName().toString();
                if (RELEASE_NOTES_PATTERN.matcher(fileName).matches()) {
                    releaseNotesFiles.add(file);
                } else if (MIGRATION_GUIDE_PATTERN.matcher(fileName).matches()) {
                    migrationGuideFiles.add(file);
                }
            }

            log.info("Found {} release notes files, {} migration guide files",
                    releaseNotesFiles.size(), migrationGuideFiles.size());

            // Process release notes
            publishProgress("Processing release notes...", 30);
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            int releaseNotesProcessed = 0;
            for (Path file : releaseNotesFiles) {
                try {
                    Boolean processed = txTemplate.execute(status -> {
                        try {
                            return processReleaseNotesFile(file);
                        } catch (Exception e) {
                            status.setRollbackOnly();
                            log.error("Error processing release notes {}: {}", file.getFileName(), e.getMessage());
                            return false;
                        }
                    });
                    if (Boolean.TRUE.equals(processed)) {
                        releaseNotesProcessed++;
                        result.incrementReleaseNotesCreated();
                    }
                } catch (Exception e) {
                    result.incrementErrorsEncountered();
                    log.error("Failed to process release notes file: {}", file.getFileName(), e);
                }

                int progress = 30 + (int) ((releaseNotesProcessed * 30.0) / Math.max(1, releaseNotesFiles.size()));
                publishProgress("Processing release notes: " + releaseNotesProcessed + "/" + releaseNotesFiles.size(), progress);
            }

            // Process migration guides
            publishProgress("Processing migration guides...", 60);
            int migrationGuidesProcessed = 0;
            for (Path file : migrationGuideFiles) {
                try {
                    Boolean processed = txTemplate.execute(status -> {
                        try {
                            return processMigrationGuideFile(file);
                        } catch (Exception e) {
                            status.setRollbackOnly();
                            log.error("Error processing migration guide {}: {}", file.getFileName(), e.getMessage());
                            return false;
                        }
                    });
                    if (Boolean.TRUE.equals(processed)) {
                        migrationGuidesProcessed++;
                        result.incrementMigrationGuidesCreated();
                    }
                } catch (Exception e) {
                    result.incrementErrorsEncountered();
                    log.error("Failed to process migration guide file: {}", file.getFileName(), e);
                }

                int progress = 60 + (int) ((migrationGuidesProcessed * 35.0) / Math.max(1, migrationGuideFiles.size()));
                publishProgress("Processing migration guides: " + migrationGuidesProcessed + "/" + migrationGuideFiles.size(), progress);
            }

            result.setSuccess(true);
            result.setEndTime(LocalDateTime.now());

            log.info("=".repeat(60));
            log.info("WIKI SYNC COMPLETED - {} release notes, {} migration guides, {} errors",
                    result.getReleaseNotesCreated(),
                    result.getMigrationGuidesCreated(),
                    result.getErrorsEncountered());
            log.info("=".repeat(60));

            publishProgress("Wiki sync complete!", 100);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Wiki sync failed: {}", e.getMessage(), e);
        } finally {
            // Cleanup cloned repository
            if (wikiDir != null) {
                cleanupDirectory(wikiDir);
            }
        }

        return result;
    }

    /**
     * Clone the wiki repository to a temporary directory.
     */
    private Path cloneWikiRepository() {
        try {
            Path tempDir = Files.createTempDirectory("spring-boot-wiki-");
            log.info("Cloning wiki repository to: {}", tempDir);

            ProcessBuilder pb = new ProcessBuilder(
                    "git", "clone", "--depth", "1", WIKI_REPO_URL, tempDir.toString());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("git: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Git clone failed with exit code {}: {}", exitCode, output);
                cleanupDirectory(tempDir);
                return null;
            }

            log.info("Successfully cloned wiki repository");
            return tempDir;

        } catch (Exception e) {
            log.error("Failed to clone wiki repository: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Find all .asciidoc files in the wiki directory and its releasenotes subdirectory.
     */
    private List<Path> findAsciidocFiles(Path wikiDir) throws IOException {
        List<Path> asciidocFiles = new ArrayList<>();

        // Check root directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(wikiDir, "*.asciidoc")) {
            for (Path file : stream) {
                asciidocFiles.add(file);
            }
        }

        // Check releasenotes subdirectory (where release notes and migration guides are stored)
        Path releaseNotesDir = wikiDir.resolve("releasenotes");
        if (Files.exists(releaseNotesDir) && Files.isDirectory(releaseNotesDir)) {
            int beforeCount = asciidocFiles.size();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(releaseNotesDir, "*.asciidoc")) {
                for (Path file : stream) {
                    asciidocFiles.add(file);
                }
            }
            log.info("Found {} files in releasenotes/ subdirectory", asciidocFiles.size() - beforeCount);
        } else {
            log.warn("No releasenotes/ subdirectory found in wiki repository");
        }

        return asciidocFiles;
    }

    /**
     * Process a release notes file and store in database.
     */
    private boolean processReleaseNotesFile(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        Matcher matcher = RELEASE_NOTES_PATTERN.matcher(fileName);

        if (!matcher.matches()) {
            return false;
        }

        int majorVersion = Integer.parseInt(matcher.group(1));
        int minorVersion = Integer.parseInt(matcher.group(2));
        String versionString = majorVersion + "." + minorVersion;

        log.debug("Processing release notes for version {}", versionString);

        // Read AsciiDoc content
        String asciidocContent = Files.readString(file, StandardCharsets.UTF_8);
        String contentHash = computeHash(asciidocContent);

        // Check if already exists with same content
        Optional<WikiReleaseNotes> existing = releaseNotesRepository.findByVersionString(versionString);
        if (existing.isPresent() && contentHash.equals(existing.get().getContentHash())) {
            log.debug("Release notes {} unchanged, skipping", versionString);
            return false;
        }

        // Convert to Markdown
        String markdownContent = asciiDocConverter.convert(asciidocContent);
        String title = asciiDocConverter.extractTitle(asciidocContent);
        if (title == null || title.isBlank()) {
            title = "Spring Boot " + versionString + " Release Notes";
        }

        // Build URL
        String sourceUrl = WIKI_BASE_URL + "/" + fileName.replace(".asciidoc", "").replace(" ", "-");

        // Create or update entity
        WikiReleaseNotes releaseNotes = existing.orElseGet(WikiReleaseNotes::new);
        releaseNotes.setVersionString(versionString);
        releaseNotes.setMajorVersion(majorVersion);
        releaseNotes.setMinorVersion(minorVersion);
        releaseNotes.setTitle(title);
        releaseNotes.setContent(asciidocContent);
        releaseNotes.setContentMarkdown(markdownContent);
        releaseNotes.setContentHash(contentHash);
        releaseNotes.setSourceUrl(sourceUrl);
        releaseNotes.setSourceFile(fileName);
        releaseNotes.setWikiLastModified(LocalDateTime.now());

        releaseNotesRepository.save(releaseNotes);
        log.info("Saved release notes for Spring Boot {}", versionString);
        return true;
    }

    /**
     * Process a migration guide file and store in database.
     * Migration guides are named by target version only (e.g., Spring-Boot-3.0-Migration-Guide.asciidoc).
     * Source version is computed as the previous major version (X-1.0 for X.0 migrations).
     */
    private boolean processMigrationGuideFile(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        Matcher matcher = MIGRATION_GUIDE_PATTERN.matcher(fileName);

        if (!matcher.matches()) {
            return false;
        }

        // Extract target version from filename
        int targetMajor = Integer.parseInt(matcher.group(1));
        int targetMinor = Integer.parseInt(matcher.group(2));

        // Compute source version (previous major version for X.0 migrations)
        // e.g., 3.0 migration guide is for upgrading from 2.x to 3.0
        int sourceMajor;
        int sourceMinor;
        if (targetMinor == 0) {
            // Major version upgrade (e.g., 3.0 -> source is 2.7)
            sourceMajor = targetMajor - 1;
            sourceMinor = 0; // Will be shown as X.x
        } else {
            // Minor version upgrade (e.g., 3.1 -> source is 3.0)
            sourceMajor = targetMajor;
            sourceMinor = targetMinor - 1;
        }

        String sourceVersionString = sourceMajor + "." + sourceMinor;
        String targetVersionString = targetMajor + "." + targetMinor;

        log.debug("Processing migration guide {} -> {}", sourceVersionString, targetVersionString);

        // Read AsciiDoc content
        String asciidocContent = Files.readString(file, StandardCharsets.UTF_8);
        String contentHash = computeHash(asciidocContent);

        // Check if already exists with same content
        Optional<WikiMigrationGuide> existing = migrationGuideRepository
                .findBySourceVersionStringAndTargetVersionString(sourceVersionString, targetVersionString);
        if (existing.isPresent() && contentHash.equals(existing.get().getContentHash())) {
            log.debug("Migration guide {} -> {} unchanged, skipping", sourceVersionString, targetVersionString);
            return false;
        }

        // Convert to Markdown
        String markdownContent = asciiDocConverter.convert(asciidocContent);
        String title = asciiDocConverter.extractTitle(asciidocContent);
        if (title == null || title.isBlank()) {
            title = "Spring Boot " + sourceVersionString + " to " + targetVersionString + " Migration Guide";
        }

        // Build URL
        String sourceUrl = WIKI_BASE_URL + "/" + fileName.replace(".asciidoc", "").replace(" ", "-");

        // Create or update entity
        WikiMigrationGuide guide = existing.orElseGet(WikiMigrationGuide::new);
        guide.setSourceVersionString(sourceVersionString);
        guide.setTargetVersionString(targetVersionString);
        guide.setSourceMajor(sourceMajor);
        guide.setSourceMinor(sourceMinor);
        guide.setTargetMajor(targetMajor);
        guide.setTargetMinor(targetMinor);
        guide.setTitle(title);
        guide.setContent(asciidocContent);
        guide.setContentMarkdown(markdownContent);
        guide.setContentHash(contentHash);
        guide.setSourceUrl(sourceUrl);
        guide.setSourceFile(fileName);
        guide.setWikiLastModified(LocalDateTime.now());

        migrationGuideRepository.save(guide);
        log.info("Saved migration guide {} -> {}", sourceVersionString, targetVersionString);
        return true;
    }

    /**
     * Compute SHA-256 hash of content for change detection.
     */
    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.warn("Failed to compute hash: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Cleanup temporary directory.
     */
    private void cleanupDirectory(Path dir) {
        try {
            if (dir != null && Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete: {}", path);
                            }
                        });
                log.debug("Cleaned up temporary wiki directory");
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup directory {}: {}", dir, e.getMessage());
        }
    }

    /**
     * Publish sync progress event.
     */
    private void publishProgress(String message, int percentComplete) {
        SyncProgressEvent event = SyncProgressEvent.builder()
                .currentPhase(10)  // Wiki is Phase 10
                .totalPhases(11)
                .phaseDescription("Wiki Sync")
                .status(percentComplete == 100 ? "completed" : "running")
                .progressPercent(percentComplete)
                .message(message)
                .completed(percentComplete == 100)
                .build();
        eventPublisher.publishEvent(event);
    }

    /**
     * Result of wiki sync operation.
     */
    @Data
    @NoArgsConstructor
    public static class SyncResult {
        private boolean success;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int releaseNotesCreated;
        private int migrationGuidesCreated;
        private int errorsEncountered;

        public void incrementReleaseNotesCreated() {
            releaseNotesCreated++;
        }

        public void incrementMigrationGuidesCreated() {
            migrationGuidesCreated++;
        }

        public void incrementErrorsEncountered() {
            errorsEncountered++;
        }
    }

    /**
     * Reprocess all existing wiki markdown to fix table formatting issues.
     * This applies the latest table formatting fixes to already-stored content
     * without needing to re-fetch from GitHub.
     *
     * @return number of documents reprocessed
     */
    public int reprocessExistingMarkdown() {
        log.info("Reprocessing existing wiki markdown to fix table formatting...");
        int count = 0;

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // Reprocess release notes
        var releaseNotes = releaseNotesRepository.findAll();
        for (var notes : releaseNotes) {
            if (notes.getContent() != null && !notes.getContent().isBlank()) {
                try {
                    txTemplate.execute(status -> {
                        // Re-convert from stored AsciiDoc content
                        String fixedMarkdown = asciiDocConverter.convert(notes.getContent());
                        notes.setContentMarkdown(fixedMarkdown);
                        releaseNotesRepository.save(notes);
                        return null;
                    });
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to reprocess release notes {}: {}", notes.getVersionString(), e.getMessage());
                }
            }
        }

        // Reprocess migration guides
        var guides = migrationGuideRepository.findAll();
        for (var guide : guides) {
            if (guide.getContent() != null && !guide.getContent().isBlank()) {
                try {
                    txTemplate.execute(status -> {
                        // Re-convert from stored AsciiDoc content
                        String fixedMarkdown = asciiDocConverter.convert(guide.getContent());
                        guide.setContentMarkdown(fixedMarkdown);
                        migrationGuideRepository.save(guide);
                        return null;
                    });
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to reprocess migration guide {} -> {}: {}",
                            guide.getSourceVersionString(), guide.getTargetVersionString(), e.getMessage());
                }
            }
        }

        log.info("Reprocessed {} wiki documents", count);
        return count;
    }
}

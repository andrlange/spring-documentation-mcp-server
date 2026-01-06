package com.spring.mcp.service.wiki;

import com.spring.mcp.model.entity.WikiMigrationGuide;
import com.spring.mcp.model.entity.WikiReleaseNotes;
import com.spring.mcp.repository.WikiMigrationGuideRepository;
import com.spring.mcp.repository.WikiReleaseNotesRepository;
import com.spring.mcp.service.github.AsciiDocToMarkdownConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of WikiService for Release Notes and Migration Guide operations.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WikiServiceImpl implements WikiService {

    private final WikiReleaseNotesRepository releaseNotesRepository;
    private final WikiMigrationGuideRepository migrationGuideRepository;
    private final AsciiDocToMarkdownConverter asciiDocConverter;

    // === Release Notes Operations ===

    @Override
    public Optional<WikiReleaseNotes> findReleaseNotesByVersion(String versionString) {
        return releaseNotesRepository.findByVersionString(versionString);
    }

    @Override
    public Optional<WikiReleaseNotes> findReleaseNotesByVersion(int majorVersion, int minorVersion) {
        return releaseNotesRepository.findByMajorVersionAndMinorVersion(majorVersion, minorVersion);
    }

    @Override
    public List<WikiReleaseNotes> findAllReleaseNotes() {
        return releaseNotesRepository.findAllOrderByVersionDesc();
    }

    @Override
    public List<WikiReleaseNotes> findReleaseNotesByMajorVersion(int majorVersion) {
        return releaseNotesRepository.findByMajorVersionOrderByMinorVersionDesc(majorVersion);
    }

    @Override
    public List<WikiReleaseNotes> searchReleaseNotes(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return releaseNotesRepository.searchByQuery(query.trim(), limit);
    }

    @Override
    public long countReleaseNotes() {
        return releaseNotesRepository.count();
    }

    @Override
    public long countReleaseNotesWithEmbedding() {
        return releaseNotesRepository.countWithEmbedding();
    }

    // === Migration Guide Operations ===

    @Override
    public Optional<WikiMigrationGuide> findMigrationGuide(String sourceVersion, String targetVersion) {
        return migrationGuideRepository.findBySourceVersionStringAndTargetVersionString(
                sourceVersion, targetVersion);
    }

    @Override
    public List<WikiMigrationGuide> findMigrationGuidesTo(String targetVersion) {
        return migrationGuideRepository.findByTargetVersionStringOrderBySourceMajorDescSourceMinorDesc(
                targetVersion);
    }

    @Override
    public List<WikiMigrationGuide> findMigrationGuidesFrom(String sourceVersion) {
        return migrationGuideRepository.findBySourceVersionStringOrderByTargetMajorDescTargetMinorDesc(
                sourceVersion);
    }

    @Override
    public List<WikiMigrationGuide> findAllMigrationGuides() {
        return migrationGuideRepository.findAllOrderByVersionDesc();
    }

    @Override
    public List<WikiMigrationGuide> findMigrationGuidesByTargetMajorVersion(int targetMajor) {
        return migrationGuideRepository.findByTargetMajorOrderByTargetMinorDescSourceMajorDescSourceMinorDesc(
                targetMajor);
    }

    @Override
    public List<WikiMigrationGuide> findDirectUpgradePaths() {
        return migrationGuideRepository.findDirectUpgradePaths();
    }

    @Override
    public List<WikiMigrationGuide> searchMigrationGuides(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return migrationGuideRepository.searchByQuery(query.trim(), limit);
    }

    @Override
    public long countMigrationGuides() {
        return migrationGuideRepository.count();
    }

    @Override
    public long countMigrationGuidesWithEmbedding() {
        return migrationGuideRepository.countWithEmbedding();
    }

    // === Maintenance Operations ===

    @Override
    @Transactional
    public int fixExistingMarkdownArtifacts() {
        log.info("Starting fix of existing markdown artifacts in wiki content...");
        int fixed = 0;

        // Fix release notes
        List<WikiReleaseNotes> releaseNotes = releaseNotesRepository.findAll();
        log.info("Processing {} release notes", releaseNotes.size());
        for (WikiReleaseNotes notes : releaseNotes) {
            String original = notes.getContentMarkdown();
            if (original != null && !original.isBlank()) {
                // Check if content has inline tables
                boolean hasInlineTable = original.contains("| --- |") && original.contains("| |");
                if (hasInlineTable) {
                    log.info("Release notes {} has potential inline table (length: {})",
                            notes.getVersionString(), original.length());
                }

                String fixedContent = asciiDocConverter.fixMarkdownArtifacts(original);
                if (!fixedContent.equals(original)) {
                    notes.setContentMarkdown(fixedContent);
                    releaseNotesRepository.save(notes);
                    fixed++;
                    log.info("Fixed markdown artifacts in release notes: {}", notes.getVersionString());
                }
            }
        }
        log.info("Fixed {} release notes", fixed);

        // Fix migration guides
        int migrationFixed = 0;
        List<WikiMigrationGuide> guides = migrationGuideRepository.findAll();
        log.info("Processing {} migration guides", guides.size());
        for (WikiMigrationGuide guide : guides) {
            String original = guide.getContentMarkdown();
            if (original != null && !original.isBlank()) {
                // Check if content has inline tables
                boolean hasInlineTable = original.contains("| --- |") && original.contains("| |");
                if (hasInlineTable) {
                    log.info("Migration guide {} has potential inline table (length: {})",
                            guide.getMigrationPath(), original.length());
                }

                String fixedContent = asciiDocConverter.fixMarkdownArtifacts(original);
                if (!fixedContent.equals(original)) {
                    guide.setContentMarkdown(fixedContent);
                    migrationGuideRepository.save(guide);
                    migrationFixed++;
                    log.info("Fixed markdown artifacts in migration guide: {}", guide.getMigrationPath());
                }
            }
        }
        log.info("Fixed {} migration guides", migrationFixed);

        int total = fixed + migrationFixed;
        log.info("Completed fixing markdown artifacts. Total documents fixed: {}", total);
        return total;
    }

    // === Statistics ===

    @Override
    public WikiStats getStatistics() {
        long releaseNotesCount = countReleaseNotes();
        long releaseNotesWithEmbedding = countReleaseNotesWithEmbedding();
        long migrationGuidesCount = countMigrationGuides();
        long migrationGuidesWithEmbedding = countMigrationGuidesWithEmbedding();

        // Get latest release notes version
        String latestReleaseNotesVersion = findAllReleaseNotes().stream()
                .findFirst()
                .map(WikiReleaseNotes::getVersionString)
                .orElse(null);

        // Get latest migration path
        String latestMigrationPath = findAllMigrationGuides().stream()
                .findFirst()
                .map(WikiMigrationGuide::getMigrationPath)
                .orElse(null);

        return new WikiStats(
                releaseNotesCount,
                releaseNotesWithEmbedding,
                migrationGuidesCount,
                migrationGuidesWithEmbedding,
                latestReleaseNotesVersion,
                latestMigrationPath
        );
    }
}

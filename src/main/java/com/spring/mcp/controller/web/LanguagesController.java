package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.LanguageType;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.language.LanguageEvolutionService;
import com.spring.mcp.service.language.LanguageSyncService;
import com.spring.mcp.service.scheduler.LanguageSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Languages page.
 * Displays Java and Kotlin version information, features, and code patterns.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Controller
@RequestMapping("/languages")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mcp.features.language-evolution.enabled", havingValue = "true", matchIfMissing = true)
public class LanguagesController {

    private final LanguageEvolutionService languageEvolutionService;
    private final LanguageSyncService languageSyncService;
    private final LanguageSchedulerService languageSchedulerService;
    private final LanguageVersionRepository versionRepository;
    private final LanguageFeatureRepository featureRepository;
    private final LanguageCodePatternRepository codePatternRepository;

    /**
     * Display languages list page with filters.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listLanguages(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Model model) {

        log.debug("Languages page: language={}, version={}, status={}, category={}, search={}",
                language, version, status, category, search);

        model.addAttribute("activePage", "languages");
        model.addAttribute("pageTitle", "Language Evolution");

        // Normalize empty strings to null for proper query handling
        String normalizedVersion = normalizeEmptyToNull(version);
        String normalizedCategory = normalizeEmptyToNull(category);
        String normalizedSearch = normalizeEmptyToNull(search);

        // Parse filters
        LanguageType langType = parseLanguageType(language);
        FeatureStatus featureStatus = parseFeatureStatus(status);

        // Get all versions for filters
        List<LanguageVersion> javaVersions = versionRepository
                .findByLanguageOrderByMajorVersionDescMinorVersionDesc(LanguageType.JAVA);
        List<LanguageVersion> kotlinVersions = versionRepository
                .findByLanguageOrderByMajorVersionDescMinorVersionDesc(LanguageType.KOTLIN);

        model.addAttribute("javaVersions", javaVersions);
        model.addAttribute("kotlinVersions", kotlinVersions);

        // Get categories for filter
        List<String> categories = languageEvolutionService.getCategories();
        model.addAttribute("categories", categories);

        // Get statuses for filter
        model.addAttribute("statuses", FeatureStatus.values());

        // Get features based on filters
        List<LanguageFeature> features;
        if (normalizedSearch != null) {
            features = languageEvolutionService.searchFeatures(langType, normalizedVersion, featureStatus, normalizedCategory, normalizedSearch);
        } else if (langType != null || normalizedVersion != null || featureStatus != null || normalizedCategory != null) {
            features = languageEvolutionService.searchFeatures(langType, normalizedVersion, featureStatus, normalizedCategory, null);
        } else {
            // Default: show all features
            features = featureRepository.findAll();
            features.sort((a, b) -> {
                int langCompare = a.getLanguageVersion().getLanguage().compareTo(b.getLanguageVersion().getLanguage());
                if (langCompare != 0) return langCompare;
                int versionCompare = Integer.compare(b.getLanguageVersion().getMajorVersion(), a.getLanguageVersion().getMajorVersion());
                if (versionCompare != 0) return versionCompare;
                return a.getFeatureName().compareTo(b.getFeatureName());
            });
        }

        model.addAttribute("features", features);

        // Calculate statistics
        Map<String, Long> stats = calculateStats(features);
        model.addAttribute("stats", stats);

        // Current filter values
        model.addAttribute("selectedLanguage", language);
        model.addAttribute("selectedVersion", version);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchTerm", search);

        // Version counts
        model.addAttribute("javaVersionCount", javaVersions.size());
        model.addAttribute("kotlinVersionCount", kotlinVersions.size());

        // LTS versions
        List<LanguageVersion> ltsVersions = versionRepository
                .findByLanguageAndIsLtsTrueOrderByMajorVersionDesc(LanguageType.JAVA);
        model.addAttribute("ltsVersions", ltsVersions);

        // Last sync info
        LanguageSchedulerSettings schedulerSettings = languageSchedulerService.getSettings();
        model.addAttribute("lastSyncRun", schedulerSettings.getLastSyncRun());
        model.addAttribute("nextSyncRun", schedulerSettings.getNextSyncRun());

        return "languages/list";
    }

    /**
     * Get version details with features.
     */
    @GetMapping("/version/{language}/{version}")
    @PreAuthorize("isAuthenticated()")
    public String showVersionDetails(
            @PathVariable String language,
            @PathVariable String version,
            Model model) {

        log.debug("Version details: language={}, version={}", language, version);

        LanguageType langType = parseLanguageType(language);
        if (langType == null) {
            return "redirect:/languages";
        }

        Optional<LanguageVersion> versionOpt = versionRepository.findByLanguageAndVersion(langType, version);
        if (versionOpt.isEmpty()) {
            return "redirect:/languages";
        }

        LanguageVersion langVersion = versionOpt.get();
        List<LanguageFeature> features = languageEvolutionService.getFeaturesForVersion(langVersion.getId());

        model.addAttribute("activePage", "languages");
        model.addAttribute("pageTitle", langVersion.getDisplayName());
        model.addAttribute("version", langVersion);
        model.addAttribute("features", features);
        model.addAttribute("stats", calculateStats(features));

        return "languages/version-details";
    }

    /**
     * Get feature details with code patterns (AJAX endpoint).
     */
    @GetMapping("/feature/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFeatureDetails(@PathVariable Long id) {
        log.debug("Feature details: id={}", id);

        Optional<LanguageFeature> featureOpt = featureRepository.findById(id);
        if (featureOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LanguageFeature feature = featureOpt.get();
        List<LanguageCodePattern> patterns = codePatternRepository.findByFeatureId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("id", feature.getId());
        response.put("featureName", feature.getFeatureName());
        response.put("description", feature.getDescription());
        response.put("status", feature.getStatus().name());
        response.put("statusBadgeClass", feature.getStatus().getBadgeClass());
        response.put("category", feature.getCategory());
        response.put("jepNumber", feature.getJepNumber());
        response.put("kepNumber", feature.getKepNumber());
        response.put("proposalUrl", feature.getEnhancementProposalUrl());
        response.put("impactLevel", feature.getImpactLevel() != null ? feature.getImpactLevel().name() : null);
        response.put("version", feature.getLanguageVersion().getDisplayName());
        response.put("codeExample", feature.getCodeExample());

        List<Map<String, Object>> patternsList = patterns.stream()
                .map(p -> {
                    Map<String, Object> patternMap = new HashMap<>();
                    patternMap.put("id", p.getId());
                    patternMap.put("patternLanguage", p.getPatternLanguage());
                    patternMap.put("oldPattern", p.getOldPattern());
                    patternMap.put("newPattern", p.getNewPattern());
                    patternMap.put("explanation", p.getExplanation());
                    patternMap.put("minVersion", p.getMinVersion());
                    return patternMap;
                })
                .collect(Collectors.toList());
        response.put("patterns", patternsList);

        return ResponseEntity.ok(response);
    }

    /**
     * Get code patterns for a feature (AJAX endpoint).
     */
    @GetMapping("/feature/{id}/patterns")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFeaturePatterns(@PathVariable Long id) {
        log.debug("Feature patterns: id={}", id);

        List<LanguageCodePattern> patterns = codePatternRepository.findByFeatureId(id);

        List<Map<String, Object>> patternsList = patterns.stream()
                .map(p -> {
                    Map<String, Object> patternMap = new HashMap<>();
                    patternMap.put("id", p.getId());
                    patternMap.put("patternLanguage", p.getPatternLanguage());
                    patternMap.put("oldPattern", p.getOldPattern());
                    patternMap.put("newPattern", p.getNewPattern());
                    patternMap.put("explanation", p.getExplanation());
                    patternMap.put("minVersion", p.getMinVersion());
                    return patternMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(patternsList);
    }

    /**
     * Trigger manual language sync.
     */
    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerSync() {
        log.info("Manual language sync triggered");

        try {
            LanguageSyncService.SyncResult result = languageSchedulerService.triggerManualSync();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("versionsUpdated", result.getVersionsUpdated());
            response.put("featuresUpdated", result.getFeaturesUpdated());
            response.put("compatibilityUpdated", result.getCompatibilityUpdated());

            if (!result.isSuccess()) {
                response.put("error", result.getErrorMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during manual language sync", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Compare two versions (AJAX endpoint).
     */
    @GetMapping("/compare")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> compareVersions(
            @RequestParam String language,
            @RequestParam String fromVersion,
            @RequestParam String toVersion) {

        log.debug("Compare versions: language={}, from={}, to={}", language, fromVersion, toVersion);

        LanguageType langType = parseLanguageType(language);
        if (langType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid language"));
        }

        LanguageEvolutionService.VersionDiff diff = languageEvolutionService.getVersionDiff(
                langType, fromVersion, toVersion);

        Map<String, Object> response = new HashMap<>();
        response.put("language", langType.getDisplayName());
        response.put("fromVersion", fromVersion);
        response.put("toVersion", toVersion);
        response.put("totalChanges", diff.getTotalChanges());

        List<Map<String, String>> newFeatures = diff.getNewFeatures().stream()
                .map(f -> Map.of(
                        "name", f.getFeatureName(),
                        "description", f.getDescription() != null ? f.getDescription() : "",
                        "category", f.getCategory() != null ? f.getCategory() : ""
                ))
                .collect(Collectors.toList());
        response.put("newFeatures", newFeatures);

        List<Map<String, String>> deprecatedFeatures = diff.getDeprecatedFeatures().stream()
                .map(f -> Map.of(
                        "name", f.getFeatureName(),
                        "description", f.getDescription() != null ? f.getDescription() : "",
                        "category", f.getCategory() != null ? f.getCategory() : ""
                ))
                .collect(Collectors.toList());
        response.put("deprecatedFeatures", deprecatedFeatures);

        List<Map<String, String>> removedFeatures = diff.getRemovedFeatures().stream()
                .map(f -> Map.of(
                        "name", f.getFeatureName(),
                        "description", f.getDescription() != null ? f.getDescription() : "",
                        "category", f.getCategory() != null ? f.getCategory() : ""
                ))
                .collect(Collectors.toList());
        response.put("removedFeatures", removedFeatures);

        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    /**
     * Normalize empty strings to null for proper query handling.
     * Native SQL queries need actual nulls, not empty strings.
     */
    private String normalizeEmptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private LanguageType parseLanguageType(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }
        return LanguageType.fromString(language);
    }

    private FeatureStatus parseFeatureStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return FeatureStatus.fromString(status);
    }

    private Map<String, Long> calculateStats(List<LanguageFeature> features) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) features.size());
        stats.put("new", features.stream().filter(f -> f.getStatus() == FeatureStatus.NEW).count());
        stats.put("deprecated", features.stream().filter(f -> f.getStatus() == FeatureStatus.DEPRECATED).count());
        stats.put("removed", features.stream().filter(f -> f.getStatus() == FeatureStatus.REMOVED).count());
        stats.put("preview", features.stream().filter(f -> f.getStatus() == FeatureStatus.PREVIEW).count());
        stats.put("incubating", features.stream().filter(f -> f.getStatus() == FeatureStatus.INCUBATING).count());
        return stats;
    }
}

package com.spring.mcp.controller.web;

import com.spring.mcp.config.LanguageEvolutionFeatureConfig;
import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.service.language.LanguageSyncService;
import com.spring.mcp.service.scheduler.LanguageSchedulerService;
import com.spring.mcp.service.sync.CodeExamplesSyncService;
import com.spring.mcp.service.sync.ComprehensiveSyncService;
import com.spring.mcp.service.sync.ProjectSyncService;
import com.spring.mcp.service.sync.RecipeSyncService;
import com.spring.mcp.service.sync.SpringGenerationsSyncService;
import com.spring.mcp.service.sync.SyncProgressTracker;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for synchronizing Spring projects and versions from external sources.
 * Admin-only access required.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/sync")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SyncController {

    private final ProjectSyncService projectSyncService;
    private final SpringGenerationsSyncService generationsSyncService;
    private final ComprehensiveSyncService comprehensiveSyncService;
    private final CodeExamplesSyncService codeExamplesSyncService;
    private final SyncProgressTracker progressTracker;

    // Additional services for individual phase syncs
    private final com.spring.mcp.service.sync.SpringProjectPageCrawlerService crawlerService;
    private final com.spring.mcp.service.sync.ProjectRelationshipSyncService relationshipSyncService;
    private final com.spring.mcp.service.sync.DocumentationSyncService documentationSyncService;
    private final com.spring.mcp.repository.SpringProjectRepository springProjectRepository;

    // OpenRewrite recipe sync (optional - depends on feature being enabled)
    private final OpenRewriteFeatureConfig openRewriteFeatureConfig;
    private final Optional<RecipeSyncService> recipeSyncService;

    // Language Evolution sync (optional - depends on feature being enabled)
    private final LanguageEvolutionFeatureConfig languageEvolutionFeatureConfig;
    private final Optional<LanguageSchedulerService> languageSchedulerService;

    /**
     * Show sync page with options.
     *
     * @param model Spring MVC model
     * @return view name "sync/index"
     */
    @GetMapping
    public String showSyncPage(Model model) {
        log.debug("Showing sync page");
        model.addAttribute("activePage", "sync");
        model.addAttribute("pageTitle", "Synchronize Projects & Versions");
        return "sync/index";
    }

    /**
     * Trigger manual sync of Spring Boot project and versions.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/spring-boot")
    public String syncSpringBoot(RedirectAttributes redirectAttributes) {
        log.info("Manual Spring Boot sync triggered");

        try {
            ProjectSyncService.SyncResult result = projectSyncService.syncSpringBoot();

            if (result.isSuccess()) {
                String message = String.format(
                    "Spring Boot sync completed successfully! Created %d versions (errors: %d)",
                    result.getVersionsCreated(),
                    result.getErrorsEncountered()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Spring Boot sync failed: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during Spring Boot sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual sync of all Spring projects and versions from Spring Generations API.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/generations")
    public String syncGenerations(RedirectAttributes redirectAttributes) {
        log.info("Manual Spring Generations sync triggered");

        try {
            SpringGenerationsSyncService.SyncResult result = generationsSyncService.syncAllGenerations();

            if (result.isSuccess()) {
                String message = String.format(
                    "Spring Generations sync completed successfully! Created %d projects and %d versions (errors: %d)",
                    result.getProjectsCreated(),
                    result.getVersionsCreated(),
                    result.getErrorsEncountered()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Spring Generations sync failed: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during Spring Generations sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger comprehensive sync of ALL Spring projects and versions from ALL sources.
     * This is the master sync that ensures complete data coverage.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/all")
    public String syncAll(RedirectAttributes redirectAttributes) {
        log.info("Manual COMPREHENSIVE sync triggered - syncing ALL projects and versions");

        try {
            ComprehensiveSyncService.ComprehensiveSyncResult result = comprehensiveSyncService.syncAll();

            if (result.isSuccess()) {
                String message = String.format(
                    "Comprehensive sync completed successfully! " +
                    "Total: %d projects, %d versions created, %d versions updated (errors: %d)",
                    result.getTotalProjectsCreated(),
                    result.getTotalVersionsCreated(),
                    result.getTotalVersionsUpdated(),
                    result.getTotalErrors()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Comprehensive sync completed with errors: " + result.getSummaryMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during comprehensive sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual sync of code examples from Spring project pages.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/examples")
    public String syncCodeExamples(RedirectAttributes redirectAttributes) {
        log.info("Manual code examples sync triggered");

        try {
            CodeExamplesSyncService.SyncResult result = codeExamplesSyncService.syncCodeExamples();

            if (result.isSuccess()) {
                String message = String.format(
                    "Code examples sync completed successfully! Created %d examples, updated %d (errors: %d)",
                    result.getExamplesCreated(),
                    result.getExamplesUpdated(),
                    result.getErrorsEncountered()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Code examples sync completed with errors: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during code examples sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual crawl of all Spring project pages.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/crawl-pages")
    public String syncCrawlPages(RedirectAttributes redirectAttributes) {
        log.info("Manual project pages crawl triggered");

        try {
            int projectsCrawled = 0;
            int versionsUpdated = 0;
            int errors = 0;

            var projects = springProjectRepository.findAll();
            for (var project : projects) {
                try {
                    var result = crawlerService.crawlProject(project.getSlug());
                    projectsCrawled++;
                    versionsUpdated += result.getVersionsUpdated();
                    if (!result.isSuccess()) {
                        errors++;
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("Error crawling project: {}", project.getSlug(), e);
                }
            }

            String message = String.format(
                "Project pages crawl completed! Crawled %d projects, updated %d versions (errors: %d)",
                projectsCrawled,
                versionsUpdated,
                errors
            );
            redirectAttributes.addFlashAttribute("success", message);
            log.info(message);

        } catch (Exception e) {
            String errorMsg = "Error during project pages crawl: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual sync of project relationships.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/relationships")
    public String syncRelationships(RedirectAttributes redirectAttributes) {
        log.info("Manual project relationships sync triggered");

        try {
            com.spring.mcp.service.sync.ProjectRelationshipSyncService.SyncResult result =
                relationshipSyncService.syncProjectRelationships();

            if (result.isSuccess()) {
                String message = String.format(
                    "Project relationships sync completed successfully! Created %d relationships, skipped %d (errors: %d)",
                    result.getRelationshipsCreated(),
                    result.getRelationshipsSkipped(),
                    result.getErrorsEncountered()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Project relationships sync failed: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during project relationships sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual sync of documentation content.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/documentation")
    public String syncDocumentation(RedirectAttributes redirectAttributes) {
        log.info("Manual documentation sync triggered");

        try {
            com.spring.mcp.service.sync.DocumentationSyncService.SyncResult result =
                documentationSyncService.syncAllDocumentation();

            if (result.isSuccess()) {
                String message = String.format(
                    "Documentation sync completed successfully! Created %d links, updated %d (errors: %d)",
                    result.getLinksCreated(),
                    result.getLinksUpdated(),
                    result.getErrorsEncountered()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Documentation sync completed with errors: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during documentation sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual sync of migration recipes (OpenRewrite knowledge).
     * Only available when OpenRewrite feature is enabled.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/recipes")
    public String syncRecipes(RedirectAttributes redirectAttributes) {
        log.info("Manual recipe sync triggered");

        if (!openRewriteFeatureConfig.isEnabled()) {
            redirectAttributes.addFlashAttribute("error", "OpenRewrite feature is disabled");
            return "redirect:/sync";
        }

        if (recipeSyncService.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Recipe sync service is not available");
            return "redirect:/sync";
        }

        try {
            recipeSyncService.get().syncRecipes();
            RecipeSyncService.RecipeSyncStatus status = recipeSyncService.get().getSyncStatus();

            String message = String.format(
                "Recipe sync completed successfully! %d recipes, %d transformations available",
                status.recipeCount(),
                status.transformationCount()
            );
            redirectAttributes.addFlashAttribute("success", message);
            log.info(message);

        } catch (Exception e) {
            String errorMsg = "Error during recipe sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual sync of language evolution data (Java/Kotlin versions, features, patterns).
     * Only available when Language Evolution feature is enabled.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/languages")
    public String syncLanguages(RedirectAttributes redirectAttributes) {
        log.info("Manual languages sync triggered");

        if (!languageEvolutionFeatureConfig.isEnabled()) {
            redirectAttributes.addFlashAttribute("error", "Language Evolution feature is disabled");
            return "redirect:/sync";
        }

        if (languageSchedulerService.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Language scheduler service is not available");
            return "redirect:/sync";
        }

        try {
            LanguageSyncService.SyncResult result = languageSchedulerService.get().triggerManualSync();

            if (result.isSuccess()) {
                String message = String.format(
                    "Languages sync completed successfully! Updated %d versions, %d features, %d compatibility mappings, %d code examples",
                    result.getVersionsUpdated(),
                    result.getFeaturesUpdated(),
                    result.getCompatibilityUpdated(),
                    result.getCodeExamplesUpdated()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Languages sync completed with errors: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }

        } catch (Exception e) {
            String errorMsg = "Error during languages sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * SSE endpoint for streaming sync progress to the UI.
     *
     * @return SSE emitter for progress updates
     */
    @GetMapping(value = "/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSyncProgress() {
        log.debug("New SSE connection for sync progress");

        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout
        progressTracker.registerEmitter(emitter);

        return emitter;
    }
}

package com.spring.mcp.controller.web;

import com.spring.mcp.config.FlavorsFeatureConfig;
import com.spring.mcp.config.LanguageEvolutionFeatureConfig;
import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.model.dto.flavor.CategoryStatsDto;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.LanguageType;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.FlavorGroupService;
import com.spring.mcp.service.FlavorService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the dashboard page.
 * Provides overview statistics and quick access to main features.
 *
 * <p>This controller displays:
 * <ul>
 *   <li>Total count of Spring projects</li>
 *   <li>Total count of project versions</li>
 *   <li>Total count of documentation links</li>
 *   <li>Recent activity summary</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final DocumentationLinkRepository documentationLinkRepository;
    private final CodeExampleRepository codeExampleRepository;
    private final UserRepository userRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final OpenRewriteFeatureConfig openRewriteFeatureConfig;
    private final MigrationRecipeRepository migrationRecipeRepository;
    private final MigrationTransformationRepository migrationTransformationRepository;
    private final LanguageEvolutionFeatureConfig languageEvolutionFeatureConfig;
    private final LanguageVersionRepository languageVersionRepository;
    private final LanguageFeatureRepository languageFeatureRepository;
    private final FlavorsFeatureConfig flavorsFeatureConfig;
    private final FlavorService flavorService;
    private final FlavorGroupService flavorGroupService;

    /**
     * Display the dashboard page with statistics.
     *
     * <p>This endpoint aggregates key metrics from the database:
     * <ul>
     *   <li>Total number of Spring projects in the system</li>
     *   <li>Total number of versions across all projects</li>
     *   <li>Total number of documentation links</li>
     * </ul>
     *
     * <p>Security: Requires authentication. All authenticated users can access
     * the dashboard regardless of their role (ADMIN, USER, or READONLY).
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "dashboard/index" which renders the dashboard template
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showDashboard(Model model, HttpServletRequest request) {
        log.debug("Loading dashboard statistics");

        try {
            // Build MCP endpoint URL
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();

            String mcpEndpoint = scheme + "://" + serverName +
                (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort) +
                contextPath + "/mcp/spring";

            model.addAttribute("mcpEndpoint", mcpEndpoint);

            // Gather statistics from repositories
            long projectCount = springProjectRepository.count();
            long versionCount = projectVersionRepository.count();
            long documentationLinkCount = documentationLinkRepository.count();
            long codeExampleCount = codeExampleRepository.count();
            long userCount = userRepository.count();

            // Count Spring Boot versions with pre-release filtering
            // (hide SNAPSHOT/MILESTONE/RC when GA exists for same version)
            List<SpringBootVersion> allBootVersions = springBootVersionRepository.findAllOrderByVersionDesc()
                    .stream()
                    .filter(v -> v.getState() != null)
                    .toList();
            long springBootCount = filterSupersededPreReleaseVersions(allBootVersions).size();

            // Add basic statistics to model
            model.addAttribute("projectCount", projectCount);
            model.addAttribute("versionCount", versionCount);
            model.addAttribute("documentationLinkCount", documentationLinkCount);
            model.addAttribute("codeExampleCount", codeExampleCount);
            model.addAttribute("userCount", userCount);
            model.addAttribute("springBootCount", springBootCount);

            // Calculate derived statistics
            long activeProjectCount = springProjectRepository.findByActiveTrue().size();
            model.addAttribute("activeProjectCount", activeProjectCount);

            // Add percentage of active projects
            double activePercentage = projectCount > 0
                ? (double) activeProjectCount / projectCount * 100
                : 0.0;
            model.addAttribute("activePercentage", String.format("%.1f", activePercentage));

            // Recent activity statistics (last 30 days)
            // Recent Projects: Count of distinct projects with releases in last 30 days
            long recentProjectsCount = projectVersionRepository.countDistinctProjectsWithRecentReleases(30);
            model.addAttribute("recentProjectsCount", recentProjectsCount);

            // Recent Docs: Count of documentation links where project versions were updated in last 30 days
            long recentDocsCount = documentationLinkRepository.countWithRecentlyUpdatedVersions(30);
            model.addAttribute("recentDocsCount", recentDocsCount);

            // Recent Activity: Top 5 most recently added/synced versions
            var recentActivityProjects = projectVersionRepository.findTopByCreatedAtDesc(PageRequest.of(0, 5));
            model.addAttribute("recentActivityProjects", recentActivityProjects);

            // OpenRewrite Recipe Statistics (conditional on feature flag)
            model.addAttribute("openRewriteEnabled", openRewriteFeatureConfig.isEnabled());
            if (openRewriteFeatureConfig.isEnabled()) {
                long recipeCount = migrationRecipeRepository.countByIsActiveTrue();
                long transformationCount = migrationTransformationRepository.count();
                long breakingChangeCount = migrationTransformationRepository.countByBreakingChangeTrue();
                var recipeProjects = migrationRecipeRepository.findDistinctProjects();

                model.addAttribute("recipeCount", recipeCount);
                model.addAttribute("transformationCount", transformationCount);
                model.addAttribute("breakingChangeCount", breakingChangeCount);
                model.addAttribute("recipeProjects", recipeProjects);

                log.debug("Recipe stats - Recipes: {}, Transformations: {}, Breaking: {}",
                    recipeCount, transformationCount, breakingChangeCount);
            }

            // Language Evolution Statistics (conditional on feature flag)
            model.addAttribute("languageEvolutionEnabled", languageEvolutionFeatureConfig.isEnabled());
            if (languageEvolutionFeatureConfig.isEnabled()) {
                long javaVersionCount = languageVersionRepository.countByLanguage(LanguageType.JAVA);
                long kotlinVersionCount = languageVersionRepository.countByLanguage(LanguageType.KOTLIN);
                long newFeaturesCount = languageFeatureRepository.countByStatus(FeatureStatus.NEW);
                long deprecatedFeaturesCount = languageFeatureRepository.countByStatus(FeatureStatus.DEPRECATED);
                long removedFeaturesCount = languageFeatureRepository.countByStatus(FeatureStatus.REMOVED);
                long previewFeaturesCount = languageFeatureRepository.countByStatus(FeatureStatus.PREVIEW);
                long totalFeaturesCount = languageFeatureRepository.count();

                model.addAttribute("javaVersionCount", javaVersionCount);
                model.addAttribute("kotlinVersionCount", kotlinVersionCount);
                model.addAttribute("newFeaturesCount", newFeaturesCount);
                model.addAttribute("deprecatedFeaturesCount", deprecatedFeaturesCount);
                model.addAttribute("removedFeaturesCount", removedFeaturesCount);
                model.addAttribute("previewFeaturesCount", previewFeaturesCount);
                model.addAttribute("totalFeaturesCount", totalFeaturesCount);

                log.debug("Language stats - Java: {}, Kotlin: {}, New: {}, Deprecated: {}, Removed: {}, Total: {}",
                    javaVersionCount, kotlinVersionCount, newFeaturesCount, deprecatedFeaturesCount,
                    removedFeaturesCount, totalFeaturesCount);
            }

            // Flavors Statistics (conditional on feature flag)
            model.addAttribute("flavorsEnabled", flavorsFeatureConfig.isEnabled());
            if (flavorsFeatureConfig.isEnabled()) {
                CategoryStatsDto flavorStats = flavorService.getStatistics();
                model.addAttribute("flavorStats", flavorStats);

                // Flavor Groups Statistics
                FlavorGroupService.GroupStatistics groupStats = flavorGroupService.getGroupStatistics();
                model.addAttribute("groupStats", groupStats);

                log.debug("Flavor stats - Active: {}, Architecture: {}, Compliance: {}, Agents: {}, Init: {}, General: {}",
                    flavorStats.getTotalActive(),
                    flavorStats.getArchitectureCount(),
                    flavorStats.getComplianceCount(),
                    flavorStats.getAgentsCount(),
                    flavorStats.getInitializationCount(),
                    flavorStats.getGeneralCount());

                log.debug("Group stats - Total: {}, Active: {}, Public: {}, Private: {}",
                    groupStats.totalGroups(),
                    groupStats.activeGroups(),
                    groupStats.publicGroups(),
                    groupStats.privateGroups());
            }

            // Set active page for sidebar navigation
            model.addAttribute("activePage", "dashboard");

            log.info("Dashboard loaded successfully - Projects: {}, Versions: {}, Docs: {}, Examples: {}, Users: {}",
                projectCount, versionCount, documentationLinkCount, codeExampleCount, userCount);

            return "dashboard/index";
        } catch (Exception e) {
            log.error("Error loading dashboard statistics", e);
            model.addAttribute("error", "Failed to load dashboard statistics");
            return "error/general";
        }
    }

    /**
     * Alternative endpoint for root path.
     * Redirects to the main dashboard.
     *
     * @return redirect to /dashboard
     */
    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public String redirectToDashboard() {
        return "redirect:/dashboard";
    }

    /**
     * Filters out pre-release versions (SNAPSHOT, MILESTONE, RC) when a GA version exists
     * for the same major.minor.patch base version.
     *
     * @param versions list of all versions to filter
     * @return filtered list with superseded pre-release versions removed
     */
    private List<SpringBootVersion> filterSupersededPreReleaseVersions(List<SpringBootVersion> versions) {
        Set<String> gaBaseVersions = versions.stream()
                .filter(v -> v.getState() == VersionState.GA)
                .map(this::getBaseVersionKey)
                .collect(Collectors.toSet());

        return versions.stream()
                .filter(v -> v.getState() == VersionState.GA || !gaBaseVersions.contains(getBaseVersionKey(v)))
                .toList();
    }

    /**
     * Gets the base version key (major.minor.patch) for a SpringBootVersion.
     *
     * @param version the Spring Boot version
     * @return base version key like "3.5.9"
     */
    private String getBaseVersionKey(SpringBootVersion version) {
        return String.format("%d.%d.%s",
                version.getMajorVersion(),
                version.getMinorVersion(),
                version.getPatchVersion() != null ? version.getPatchVersion() : "x");
    }
}

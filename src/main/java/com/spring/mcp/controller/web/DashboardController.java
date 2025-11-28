package com.spring.mcp.controller.web;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.repository.*;
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
                contextPath + "/mcp/spring/sse";

            model.addAttribute("mcpEndpoint", mcpEndpoint);

            // Gather statistics from repositories
            long projectCount = springProjectRepository.count();
            long versionCount = projectVersionRepository.count();
            long documentationLinkCount = documentationLinkRepository.count();
            long codeExampleCount = codeExampleRepository.count();
            long userCount = userRepository.count();
            long springBootCount = springBootVersionRepository.count();

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

            // Recent Activity: Top 5 projects with newest release dates
            var recentActivityProjects = projectVersionRepository.findTopByReleaseDateDesc(PageRequest.of(0, 5));
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
}

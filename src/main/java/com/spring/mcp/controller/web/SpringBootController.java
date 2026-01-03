package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for managing and displaying Spring Boot versions.
 * Provides a dedicated view for Spring Boot version information.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Listing all Spring Boot versions with comprehensive details</li>
 *   <li>Displaying version-specific information including support dates and documentation links</li>
 * </ul>
 *
 * <p>Spring Boot versions are the central filtering mechanism across the entire system,
 * making this a critical view for navigating documentation and examples.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-09
 */
@Controller
@RequestMapping("/spring-boot")
@RequiredArgsConstructor
@Slf4j
public class SpringBootController {

    private final SpringBootVersionRepository springBootVersionRepository;
    private final SettingsService settingsService;

    /**
     * List all Spring Boot versions.
     *
     * <p>Displays a comprehensive list of all Spring Boot versions in the system,
     * ordered by version number (ascending). The view includes:
     * <ul>
     *   <li>Spring Boot name and version number</li>
     *   <li>Version state (GA, RC, SNAPSHOT, MILESTONE)</li>
     *   <li>Current version flag (isCurrent)</li>
     *   <li>OSS Support end date</li>
     *   <li>Enterprise Support end date</li>
     *   <li>Reference Documentation link</li>
     *   <li>API Documentation link</li>
     * </ul>
     *
     * <p>Spring Boot versions serve as the central filtering table for the entire
     * documentation system, allowing users to navigate all Spring project versions
     * based on their Spring Boot version compatibility.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "spring-boot/list" which renders the Spring Boot versions template
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listSpringBootVersions(Model model) {
        log.debug("Listing all Spring Boot versions");

        // Fetch all Spring Boot versions ordered by version ascending
        final List<SpringBootVersion> allVersions = springBootVersionRepository.findAllOrderByVersionAsc();

        // Filter out null states and apply pre-release filtering
        final List<SpringBootVersion> versions = filterSupersededPreReleaseVersions(
                allVersions.stream().filter(v -> v.getState() != null).toList()
        );

        // Calculate statistics
        long totalVersions = versions.size();
        long currentVersionCount = versions.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsCurrent()))
                .count();
        long gaVersionsCount = versions.stream()
                .filter(v -> v.getState() == VersionState.GA)
                .count();
        long activeSupportCount = versions.stream()
                .filter(SpringBootVersion::isOssSupportActive)
                .count();
        long endOfLifeCount = versions.stream()
                .filter(SpringBootVersion::isEndOfLife)
                .count();

        // Get enterprise subscription setting
        boolean enterpriseSubscriptionEnabled = settingsService.isEnterpriseSubscriptionEnabled();

        // Add attributes to the model
        model.addAttribute("versions", versions);
        model.addAttribute("totalVersions", totalVersions);
        model.addAttribute("currentVersionCount", currentVersionCount);
        model.addAttribute("gaVersionsCount", gaVersionsCount);
        model.addAttribute("activeSupportCount", activeSupportCount);
        model.addAttribute("endOfLifeCount", endOfLifeCount);
        model.addAttribute("enterpriseSubscriptionEnabled", enterpriseSubscriptionEnabled);
        model.addAttribute("pageTitle", "Spring Boot Versions");
        model.addAttribute("activePage", "spring-boot");

        log.info("Retrieved {} Spring Boot versions ({} current, {} GA, {} active support, {} end-of-life)",
                totalVersions, currentVersionCount, gaVersionsCount, activeSupportCount, endOfLifeCount);

        return "spring-boot/list";
    }

    /**
     * Filters out pre-release versions (SNAPSHOT, MILESTONE, RC) when a GA version exists
     * for the same major.minor.patch base version.
     *
     * <p>Version evolution follows: SNAPSHOT → MILESTONE → RC → GA
     * Once a version reaches GA status, the pre-release versions are superseded and hidden.
     *
     * <p>Examples:
     * <ul>
     *   <li>If 3.5.9 (GA) exists → hide 3.5.9-SNAPSHOT, 3.5.9-M1, 3.5.9-RC1</li>
     *   <li>If only 3.5.10-SNAPSHOT exists (no GA) → show 3.5.10-SNAPSHOT</li>
     *   <li>If 3.6.0-RC1 exists but no 3.6.0 GA → show 3.6.0-RC1</li>
     * </ul>
     *
     * @param versions list of all versions to filter
     * @return filtered list with superseded pre-release versions removed
     */
    private List<SpringBootVersion> filterSupersededPreReleaseVersions(List<SpringBootVersion> versions) {
        // Build a set of base versions (major.minor.patch) that have a GA release
        Set<String> gaBaseVersions = versions.stream()
                .filter(v -> v.getState() == VersionState.GA)
                .map(this::getBaseVersionKey)
                .collect(Collectors.toSet());

        // Filter: keep version if it's GA, or if no GA exists for its base version
        return versions.stream()
                .filter(v -> v.getState() == VersionState.GA || !gaBaseVersions.contains(getBaseVersionKey(v)))
                .toList();
    }

    /**
     * Gets the base version key (major.minor.patch) for a SpringBootVersion.
     * This is used to group versions that differ only by their state (GA, RC, SNAPSHOT, etc.)
     *
     * @param version the Spring Boot version
     * @return base version key like "3.5.9" or "3.5.null" if patch is null
     */
    private String getBaseVersionKey(SpringBootVersion version) {
        return String.format("%d.%d.%s",
                version.getMajorVersion(),
                version.getMinorVersion(),
                version.getPatchVersion() != null ? version.getPatchVersion() : "x");
    }
}

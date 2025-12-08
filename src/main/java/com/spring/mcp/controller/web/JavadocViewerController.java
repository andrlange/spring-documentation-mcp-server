package com.spring.mcp.controller.web;

import com.spring.mcp.config.JavadocsFeatureConfig;
import com.spring.mcp.model.entity.JavadocClass;
import com.spring.mcp.model.entity.JavadocPackage;
import com.spring.mcp.repository.JavadocClassRepository;
import com.spring.mcp.repository.JavadocPackageRepository;
import com.spring.mcp.service.javadoc.JavadocVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for viewing Javadoc documentation.
 * Provides endpoints to browse packages, classes, and search documentation.
 *
 * @author Spring MCP Server
 * @version 1.4.2
 * @since 2025-12-08
 */
@Controller
@RequestMapping("/javadoc")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocViewerController {

    private final JavadocPackageRepository packageRepository;
    private final JavadocClassRepository classRepository;
    private final JavadocVersionService versionService;
    private final JavadocsFeatureConfig config;

    /**
     * Show Javadoc index page for a library version.
     * Lists all packages available for the library.
     *
     * @param library the library name (e.g., "spring-ai")
     * @param version the version (e.g., "1.1.1" or "latest")
     * @param model Spring MVC model
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name or redirect
     */
    @GetMapping("/view/{library}/{version}")
    @PreAuthorize("isAuthenticated()")
    public String viewLibrary(
            @PathVariable String library,
            @PathVariable String version,
            Model model,
            RedirectAttributes redirectAttributes) {
        log.debug("Viewing Javadoc index for {}/{}", library, version);

        // Resolve version
        Optional<String> resolvedVersion = versionService.resolveVersion(library, version);
        if (resolvedVersion.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "No Javadoc documentation found for " + library + " version " + version);
            return "redirect:/projects";
        }

        String ver = resolvedVersion.get();

        // Get all packages for this library/version
        List<JavadocPackage> packages = packageRepository.findByLibraryNameAndVersion(library, ver);
        if (packages.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning",
                    "No packages found for " + library + " " + ver + ". Documentation may not be synced yet.");
            return "redirect:/projects";
        }

        // Sort packages by name
        packages.sort(Comparator.comparing(JavadocPackage::getPackageName));

        // Get available versions for version switcher
        List<String> availableVersions = versionService.getAvailableVersions(library);

        model.addAttribute("library", library);
        model.addAttribute("version", ver);
        model.addAttribute("packages", packages);
        model.addAttribute("packageCount", packages.size());
        model.addAttribute("availableVersions", availableVersions);
        model.addAttribute("pageTitle", library + " " + ver + " API");
        model.addAttribute("activePage", "projects");

        return "javadoc/index";
    }

    /**
     * Show package documentation with list of classes.
     *
     * @param library the library name
     * @param version the version
     * @param packageName the package name (URL encoded)
     * @param model Spring MVC model
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name or redirect
     */
    @GetMapping("/view/{library}/{version}/package/**")
    @PreAuthorize("isAuthenticated()")
    public String viewPackage(
            @PathVariable String library,
            @PathVariable String version,
            jakarta.servlet.http.HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Extract package name from the path (everything after /package/)
        String requestUri = request.getRequestURI();
        String packageName = extractPackageName(requestUri);

        log.debug("Viewing package {}/{}/{}", library, version, packageName);

        // Resolve version
        Optional<String> resolvedVersion = versionService.resolveVersion(library, version);
        if (resolvedVersion.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Version not found");
            return "redirect:/javadoc/view/" + library + "/latest";
        }

        String ver = resolvedVersion.get();

        // Get the package
        Optional<JavadocPackage> packageOpt = packageRepository
                .findByLibraryNameAndVersionAndPackageName(library, ver, packageName);
        if (packageOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Package not found: " + packageName);
            return "redirect:/javadoc/view/" + library + "/" + ver;
        }

        JavadocPackage pkg = packageOpt.get();

        // Get classes in the package
        List<JavadocClass> classes = classRepository.findByPackageId(pkg.getId());

        // Group classes by kind
        Map<String, List<JavadocClass>> classesByKind = classes.stream()
                .sorted(Comparator.comparing(JavadocClass::getSimpleName))
                .collect(Collectors.groupingBy(c ->
                        c.getKind() != null ? c.getKind().name() : "CLASS"));

        model.addAttribute("library", library);
        model.addAttribute("version", ver);
        model.addAttribute("package", pkg);
        model.addAttribute("classesByKind", classesByKind);
        model.addAttribute("totalClasses", classes.size());
        model.addAttribute("pageTitle", packageName + " - " + library + " " + ver);
        model.addAttribute("activePage", "projects");

        return "javadoc/package";
    }

    /**
     * Show class documentation with methods, fields, constructors.
     *
     * @param library the library name
     * @param version the version
     * @param fqcn the fully qualified class name (URL encoded)
     * @param model Spring MVC model
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name or redirect
     */
    @GetMapping("/view/{library}/{version}/class/**")
    @PreAuthorize("isAuthenticated()")
    public String viewClass(
            @PathVariable String library,
            @PathVariable String version,
            jakarta.servlet.http.HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Extract FQCN from the path (everything after /class/)
        String requestUri = request.getRequestURI();
        String fqcn = extractClassName(requestUri);

        log.debug("Viewing class {}/{}/{}", library, version, fqcn);

        // Resolve version
        Optional<String> resolvedVersion = versionService.resolveVersion(library, version);
        if (resolvedVersion.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Version not found");
            return "redirect:/javadoc/view/" + library + "/latest";
        }

        String ver = resolvedVersion.get();

        // Get the class with all members
        Optional<JavadocClass> classOpt = classRepository.findByLibraryVersionAndFqcn(library, ver, fqcn);
        if (classOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Class not found: " + fqcn);
            return "redirect:/javadoc/view/" + library + "/" + ver;
        }

        JavadocClass cls = classOpt.get();

        // Get the full class with members (eager load)
        Optional<JavadocClass> fullClassOpt = classRepository.findByIdWithMembers(cls.getId());
        if (fullClassOpt.isPresent()) {
            cls = fullClassOpt.get();
        }

        model.addAttribute("library", library);
        model.addAttribute("version", ver);
        model.addAttribute("cls", cls);
        model.addAttribute("package", cls.getJavadocPackage());
        model.addAttribute("pageTitle", cls.getSimpleName() + " - " + library + " " + ver);
        model.addAttribute("activePage", "projects");

        return "javadoc/class";
    }

    /**
     * Search Javadocs across a library version.
     *
     * @param library the library name (optional)
     * @param version the version (optional)
     * @param q the search query
     * @param limit max results (default 20)
     * @param model Spring MVC model
     * @return view name
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public String search(
            @RequestParam(required = false) String library,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            Model model) {
        log.debug("Searching Javadocs: library={}, version={}, q={}, limit={}", library, version, q, limit);

        model.addAttribute("library", library);
        model.addAttribute("version", version);
        model.addAttribute("query", q);
        model.addAttribute("pageTitle", "Search Javadocs");
        model.addAttribute("activePage", "projects");

        if (q == null || q.isBlank()) {
            model.addAttribute("results", List.of());
            model.addAttribute("totalResults", 0);
            return "javadoc/search";
        }

        int maxResults = Math.min(limit != null ? limit : 20, config.getSearch().getMaxLimit());

        List<JavadocClass> results;
        if (library != null && !library.isBlank()) {
            // Resolve version
            Optional<String> resolvedVersion = versionService.resolveVersion(library, version);
            if (resolvedVersion.isPresent()) {
                results = classRepository.searchByKeyword(library, resolvedVersion.get(), q, maxResults);
                model.addAttribute("version", resolvedVersion.get());
            } else {
                results = List.of();
            }
        } else {
            // Global search
            results = classRepository.searchByKeywordGlobal(q, maxResults);
        }

        model.addAttribute("results", results);
        model.addAttribute("totalResults", results.size());

        // Get available libraries for filter dropdown
        List<String> libraries = packageRepository.findDistinctLibraryNames();
        model.addAttribute("availableLibraries", libraries);

        return "javadoc/search";
    }

    /**
     * Extract package name from request URI.
     */
    private String extractPackageName(String uri) {
        String marker = "/package/";
        int idx = uri.indexOf(marker);
        if (idx >= 0) {
            return uri.substring(idx + marker.length());
        }
        return "";
    }

    /**
     * Extract class name from request URI.
     */
    private String extractClassName(String uri) {
        String marker = "/class/";
        int idx = uri.indexOf(marker);
        if (idx >= 0) {
            return uri.substring(idx + marker.length());
        }
        return "";
    }
}

package com.spring.mcp.service.javadoc;

import com.spring.mcp.config.JavadocsFeatureConfig;
import com.spring.mcp.model.entity.JavadocClass;
import com.spring.mcp.model.entity.JavadocPackage;
import com.spring.mcp.service.javadoc.dto.ParsedClassDto;
import com.spring.mcp.service.javadoc.dto.ParsedPackageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for orchestrating Javadoc crawling operations.
 * Coordinates fetching, parsing, and storing of Javadoc documentation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocCrawlService {

    private final JavadocFetcherService fetcherService;
    private final JavadocParserService parserService;
    private final JavadocStorageService storageService;
    private final JavadocsFeatureConfig config;

    /**
     * Crawl a Javadoc site starting from the base URL.
     *
     * @param baseUrl     Base URL of the Javadoc site (e.g., https://docs.spring.io/spring-ai/docs/1.1.1/api/)
     * @param libraryName Library name for storage (e.g., "spring-ai")
     * @param version     Version for storage (e.g., "1.1.1")
     * @return CrawlResult with statistics
     */
    public CrawlResult crawlJavadoc(String baseUrl, String libraryName, String version) {
        Instant startTime = Instant.now();
        log.info("Starting Javadoc crawl for {}/{} from {}", libraryName, version, baseUrl);

        CrawlResult result = new CrawlResult(libraryName, version);
        String normalizedUrl = normalizeUrl(baseUrl);

        try {
            // Step 1: Fetch and parse package list
            List<String> packageNames = fetchPackageList(normalizedUrl);
            if (packageNames.isEmpty()) {
                log.warn("No packages found for {}/{}", libraryName, version);
                result.addError("No packages found in package-list/element-list");
                return result;
            }

            result.totalPackages = packageNames.size();
            log.info("Found {} packages for {}/{}", packageNames.size(), libraryName, version);

            // Step 2: Process each package
            for (String packageName : packageNames) {
                try {
                    processPackage(normalizedUrl, packageName, libraryName, version, result);
                } catch (Exception e) {
                    log.error("Failed to process package {}: {}", packageName, e.getMessage());
                    result.addError("Package " + packageName + ": " + e.getMessage());
                }

                // Rate limiting is handled in fetcher, but add progress logging
                if (result.packagesProcessed % 10 == 0) {
                    log.info("Progress: {}/{} packages processed for {}/{}",
                            result.packagesProcessed, result.totalPackages, libraryName, version);
                }
            }

            result.duration = Duration.between(startTime, Instant.now());
            log.info("Completed Javadoc crawl for {}/{}: {} packages, {} classes ({} skipped), {} methods in {}s",
                    libraryName, version, result.packagesProcessed, result.classesProcessed,
                    result.classesSkipped, result.methodsStored, result.duration.getSeconds());

        } catch (Exception e) {
            log.error("Crawl failed for {}/{}: {}", libraryName, version, e.getMessage(), e);
            result.addError("Crawl failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Fetch and parse the package list.
     */
    private List<String> fetchPackageList(String baseUrl) {
        List<String> packages = new ArrayList<>();

        try {
            String content = fetcherService.fetchPackageList(baseUrl).block();
            if (content != null) {
                // Parse package list (one package per line)
                String[] lines = content.split("\\r?\\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    // Skip module declarations (element-list format)
                    if (!trimmed.isEmpty() && !trimmed.startsWith("module:")) {
                        packages.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch package list: {}", e.getMessage());
        }

        return packages;
    }

    /**
     * Process a single package.
     * Skips processing if the package already exists in the database (resume support).
     */
    private void processPackage(String baseUrl, String packageName, String libraryName,
                                String version, CrawlResult result) {
        // Check if package already exists - if so, skip entirely (no HTTP fetch needed)
        if (storageService.packageExists(libraryName, version, packageName)) {
            log.trace("Package already exists, skipping: {}/{}/{}", libraryName, version, packageName);
            result.packagesSkipped++;
            return;
        }

        String packagePath = packageName.replace('.', '/');
        String packageUrl = baseUrl + packagePath + "/package-summary.html";

        try {
            // Fetch and parse package summary
            Optional<String> htmlOpt = fetcherService.fetchPageBlocking(packageUrl);
            if (htmlOpt.isEmpty()) {
                log.debug("Package summary not found: {}", packageName);
                return;
            }

            ParsedPackageDto pkgDto = parserService.parsePackageSummary(htmlOpt.get(), packageUrl);

            // Override package name from list (more reliable)
            pkgDto = ParsedPackageDto.builder()
                    .packageName(packageName)
                    .summary(pkgDto.summary())
                    .description(pkgDto.description())
                    .classes(pkgDto.classes())
                    .sourceUrl(packageUrl)
                    .build();

            // Save package
            JavadocPackage savedPackage = storageService.savePackage(pkgDto, libraryName, version);
            result.packagesProcessed++;

            // Process classes in the package
            int classesInPackage = 0;
            int maxClasses = config.getParser().getMaxClassesPerPackage();

            // Get class links from package summary
            List<String> classNames = extractClassNamesFromPackage(htmlOpt.get(), packageName);

            for (String className : classNames) {
                if (classesInPackage >= maxClasses) {
                    log.debug("Reached max classes ({}) for package {}", maxClasses, packageName);
                    break;
                }

                try {
                    processClass(baseUrl, packagePath, className, savedPackage, libraryName, version, result);
                    classesInPackage++;
                } catch (Exception e) {
                    log.debug("Failed to process class {}.{}: {}", packageName, className, e.getMessage());
                    result.addError("Class " + className + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            log.debug("Failed to process package {}: {}", packageName, e.getMessage());
            result.addError("Package " + packageName + ": " + e.getMessage());
        }
    }

    // Javadoc pages that are NOT classes and should be excluded
    private static final Set<String> EXCLUDED_PAGES = Set.of(
            "package-summary.html",
            "package-tree.html",
            "package-use.html",
            "package-frame.html",
            "allclasses.html",
            "allclasses-index.html",
            "allclasses-noframe.html",
            "allpackages-index.html",
            "constant-values.html",
            "deprecated-list.html",
            "help-doc.html",
            "index.html",
            "index-all.html",
            "overview-summary.html",
            "overview-tree.html",
            "overview-frame.html",
            "serialized-form.html"
    );

    /**
     * Extract class names from package summary HTML.
     */
    private List<String> extractClassNamesFromPackage(String html, String packageName) {
        List<String> classNames = new ArrayList<>();

        // Use Jsoup to extract class links
        var doc = org.jsoup.Jsoup.parse(html);
        var links = doc.select("a[href$=.html]");

        for (var link : links) {
            String href = link.attr("href");
            // Skip if contains path separator, is a known metadata page, or starts with "class-use/"
            if (!href.contains("/") && !EXCLUDED_PAGES.contains(href) && !href.startsWith("class-use")) {
                String className = href.replace(".html", "");
                // Also skip if the class name looks like a metadata page
                if (!className.contains("-") || isValidClassName(className)) {
                    if (!classNames.contains(className)) {
                        classNames.add(className);
                    }
                }
            }
        }

        return classNames;
    }

    /**
     * Check if the name looks like a valid Java class name.
     * Valid class names can contain hyphens in inner class names (e.g., "Outer.Inner-Class")
     * but not in regular patterns like "package-summary", "class-use", etc.
     */
    private boolean isValidClassName(String name) {
        // If it doesn't contain hyphen at all, it's valid
        if (!name.contains("-")) {
            return true;
        }
        // Allow inner class names with dots (e.g., "SomeClass.Builder")
        // But reject patterns like "xxx-use", "xxx-summary", "xxx-frame", "xxx-index"
        String lower = name.toLowerCase();
        return !lower.endsWith("-use") &&
               !lower.endsWith("-summary") &&
               !lower.endsWith("-frame") &&
               !lower.endsWith("-index") &&
               !lower.endsWith("-list") &&
               !lower.endsWith("-tree") &&
               !lower.endsWith("-form") &&
               !lower.endsWith("-values");
    }

    /**
     * Process a single class.
     * Skips processing if the class already exists in the database (resume support).
     */
    private void processClass(String baseUrl, String packagePath, String className,
                              JavadocPackage pkg, String libraryName, String version, CrawlResult result) {
        // Build FQCN to check if class already exists
        String fqcn = pkg.getPackageName() + "." + className;

        // Check if class already exists (resume support - skip already synced classes)
        if (storageService.classExists(libraryName, version, fqcn)) {
            result.classesSkipped++;
            return;
        }

        String classUrl = baseUrl + packagePath + "/" + className + ".html";

        try {
            Optional<String> htmlOpt = fetcherService.fetchPageBlocking(classUrl);
            if (htmlOpt.isEmpty()) {
                return;
            }

            ParsedClassDto classDto = parserService.parseClassPage(htmlOpt.get(), classUrl);

            // Ensure we have the correct simple name and FQCN
            classDto = ParsedClassDto.builder()
                    .fqcn(fqcn)
                    .simpleName(className)
                    .kind(classDto.kind())
                    .modifiers(classDto.modifiers())
                    .summary(classDto.summary())
                    .description(classDto.description())
                    .superClass(classDto.superClass())
                    .interfaces(classDto.interfaces())
                    .sourceUrl(classUrl)
                    .deprecated(classDto.deprecated())
                    .deprecatedMessage(classDto.deprecatedMessage())
                    .methods(classDto.methods())
                    .fields(classDto.fields())
                    .constructors(classDto.constructors())
                    .annotations(classDto.annotations())
                    .build();

            // Save class with all members
            JavadocClass savedClass = storageService.saveClass(classDto, pkg);
            if (savedClass != null) {
                result.classesProcessed++;
                result.methodsStored += savedClass.getMethodCount();
                result.fieldsStored += savedClass.getFieldCount();
                result.constructorsStored += savedClass.getConstructorCount();
            }

        } catch (Exception e) {
            log.debug("Failed to process class {}: {}", className, e.getMessage());
        }
    }

    /**
     * Normalize URL to ensure it ends with a slash.
     * Also strips index.html suffix if present (common in API doc URLs).
     */
    private String normalizeUrl(String url) {
        if (url == null) return "";
        // Strip index.html suffix (URLs like .../api/index.html should become .../api/)
        if (url.endsWith("/index.html")) {
            url = url.substring(0, url.length() - 10); // Remove "index.html", keep "/"
        } else if (url.endsWith("index.html")) {
            url = url.substring(0, url.length() - 10); // Remove "index.html"
        }
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * Result of a crawl operation.
     */
    public static class CrawlResult {
        public final String libraryName;
        public final String version;
        public int totalPackages;
        public int packagesProcessed;
        public int packagesSkipped;  // Packages already synced and skipped
        public int classesProcessed;
        public int classesSkipped;  // Classes already synced and skipped
        public int methodsStored;
        public int fieldsStored;
        public int constructorsStored;
        public Duration duration;
        public final List<String> errors = new ArrayList<>();

        public CrawlResult(String libraryName, String version) {
            this.libraryName = libraryName;
            this.version = version;
        }

        public void addError(String error) {
            if (errors.size() < 100) { // Limit error list size
                errors.add(error);
            }
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean isSuccessful() {
            // Success if we processed anything OR everything was already synced
            return packagesProcessed > 0 || packagesSkipped > 0;
        }

        @Override
        public String toString() {
            return String.format("CrawlResult[%s/%s: %d packages (%d skipped), %d classes (%d skipped), %d methods, %d errors, %ds]",
                    libraryName, version, packagesProcessed, packagesSkipped, classesProcessed, classesSkipped, methodsStored,
                    errors.size(), duration != null ? duration.getSeconds() : 0);
        }
    }
}

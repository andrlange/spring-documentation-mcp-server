package com.spring.mcp.service.tools;

import com.spring.mcp.config.JavadocsFeatureConfig;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.javadoc.JavadocVersionService;
import com.spring.mcp.service.tools.dto.javadoc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Tools for Javadoc documentation.
 * Provides tools to query stored Javadoc documentation for Spring projects.
 * Only registered when mcp.features.javadocs.enabled=true
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocTools {

    private final JavadocClassRepository classRepository;
    private final JavadocPackageRepository packageRepository;
    private final JavadocVersionService versionService;
    private final JavadocsFeatureConfig config;

    @McpTool(description = """
        Get full documentation for a Java class including methods, fields, and constructors.
        Returns the class documentation with inheritance hierarchy and all members.
        Use 'latest' or omit version to get the latest available version.
        """)
    @Transactional(readOnly = true)
    public ClassDocResult getClassDoc(
            @McpToolParam(description = "Library name (e.g., 'spring-ai', 'spring-boot')") String library,
            @McpToolParam(description = "Version (e.g., '1.1.1', 'latest', or omit for latest)") String version,
            @McpToolParam(description = "Fully qualified class name (e.g., 'org.springframework.ai.chat.ChatClient')") String fqcn
    ) {
        log.info("Tool: getClassDoc - library={}, version={}, fqcn={}", library, version, fqcn);

        if (!config.isEnabled()) {
            return ClassDocResult.notFound(library, version, fqcn);
        }

        // Resolve version
        Optional<String> resolvedVersion = versionService.resolveVersion(library, version);
        if (resolvedVersion.isEmpty()) {
            log.debug("Version not found: {}/{}", library, version);
            return ClassDocResult.notFound(library, version, fqcn);
        }

        String ver = resolvedVersion.get();

        // Find the class
        Optional<JavadocClass> classOpt = classRepository.findByLibraryVersionAndFqcn(library, ver, fqcn);
        if (classOpt.isEmpty()) {
            log.debug("Class not found: {}/{}/{}", library, ver, fqcn);
            return ClassDocResult.notFound(library, ver, fqcn);
        }

        JavadocClass cls = classOpt.get();

        // Build method summaries
        List<ClassDocResult.MethodSummary> methods = cls.getMethods().stream()
                .sorted(Comparator.comparing(JavadocMethod::getName))
                .map(m -> ClassDocResult.MethodSummary.builder()
                        .name(m.getName())
                        .signature(m.getSignature())
                        .returnType(m.getReturnType())
                        .summary(m.getSummary())
                        .deprecated(m.getDeprecated() != null && m.getDeprecated())
                        .parameters(m.getParameters())
                        .build())
                .collect(Collectors.toList());

        // Build field summaries
        List<ClassDocResult.FieldSummary> fields = cls.getFields().stream()
                .sorted(Comparator.comparing(JavadocField::getName))
                .map(f -> ClassDocResult.FieldSummary.builder()
                        .name(f.getName())
                        .type(f.getType())
                        .modifiers(f.getModifiers())
                        .summary(f.getSummary())
                        .deprecated(f.getDeprecated() != null && f.getDeprecated())
                        .constantValue(f.getConstantValue())
                        .build())
                .collect(Collectors.toList());

        // Build constructor summaries
        List<ClassDocResult.ConstructorSummary> constructors = cls.getConstructors().stream()
                .map(c -> ClassDocResult.ConstructorSummary.builder()
                        .signature(c.getSignature())
                        .summary(c.getSummary())
                        .deprecated(c.getDeprecated() != null && c.getDeprecated())
                        .parameters(c.getParameters())
                        .build())
                .collect(Collectors.toList());

        return ClassDocResult.builder()
                .library(library)
                .version(ver)
                .fqcn(cls.getFqcn())
                .simpleName(cls.getSimpleName())
                .kind(cls.getKind() != null ? cls.getKind().name() : null)
                .modifiers(cls.getModifiers())
                .summary(cls.getSummary())
                .description(cls.getDescription())
                .superClass(cls.getSuperClass())
                .interfaces(cls.getInterfaces() != null ? cls.getInterfaces() : List.of())
                .deprecated(cls.getDeprecated() != null && cls.getDeprecated())
                .deprecatedMessage(cls.getDeprecatedMessage())
                .sourceUrl(cls.getSourceUrl())
                .methods(methods)
                .fields(fields)
                .constructors(constructors)
                .annotations(cls.getAnnotations() != null ? cls.getAnnotations() : List.of())
                .build();
    }

    @McpTool(description = """
        Get package documentation with list of classes/interfaces.
        Returns the package description and a summary of all classes in the package.
        Use 'latest' or omit version to get the latest available version.
        """)
    @Transactional(readOnly = true)
    public PackageDocResult getPackageDoc(
            @McpToolParam(description = "Library name (e.g., 'spring-ai', 'spring-boot')") String library,
            @McpToolParam(description = "Version (e.g., '1.1.1', 'latest', or omit for latest)") String version,
            @McpToolParam(description = "Package name (e.g., 'org.springframework.ai.chat')") String packageName
    ) {
        log.info("Tool: getPackageDoc - library={}, version={}, package={}", library, version, packageName);

        if (!config.isEnabled()) {
            return PackageDocResult.notFound(library, version, packageName);
        }

        // Resolve version
        Optional<String> resolvedVersion = versionService.resolveVersion(library, version);
        if (resolvedVersion.isEmpty()) {
            log.debug("Version not found: {}/{}", library, version);
            return PackageDocResult.notFound(library, version, packageName);
        }

        String ver = resolvedVersion.get();

        // Find the package
        Optional<JavadocPackage> pkgOpt = packageRepository
                .findByLibraryNameAndVersionAndPackageName(library, ver, packageName);
        if (pkgOpt.isEmpty()) {
            log.debug("Package not found: {}/{}/{}", library, ver, packageName);
            return PackageDocResult.notFound(library, ver, packageName);
        }

        JavadocPackage pkg = pkgOpt.get();

        // Get classes in the package
        List<JavadocClass> classes = classRepository.findByPackageId(pkg.getId());

        List<PackageDocResult.ClassEntry> classEntries = classes.stream()
                .sorted(Comparator.comparing(JavadocClass::getSimpleName))
                .map(c -> PackageDocResult.ClassEntry.builder()
                        .simpleName(c.getSimpleName())
                        .fqcn(c.getFqcn())
                        .kind(c.getKind() != null ? c.getKind().name() : null)
                        .summary(c.getSummary())
                        .deprecated(c.getDeprecated() != null && c.getDeprecated())
                        .build())
                .collect(Collectors.toList());

        return PackageDocResult.builder()
                .library(library)
                .version(ver)
                .packageName(pkg.getPackageName())
                .summary(pkg.getSummary())
                .description(pkg.getDescription())
                .sourceUrl(pkg.getSourceUrl())
                .classes(classEntries)
                .totalClasses(classEntries.size())
                .build();
    }

    @McpTool(description = """
        Search across all Javadoc content using full-text search.
        Searches class names, descriptions, and package information.
        Returns ranked results with relevance scores.
        """)
    @Transactional(readOnly = true)
    public List<JavadocSearchResult> searchJavadocs(
            @McpToolParam(description = "Library name to search in (optional, omit to search all)") String library,
            @McpToolParam(description = "Version to search in (optional, omit for latest)") String version,
            @McpToolParam(description = "Search query (e.g., 'ChatClient', 'embedding model', 'tool call')") String query,
            @McpToolParam(description = "Maximum results to return (default: 10, max: 50)") Integer limit
    ) {
        log.info("Tool: searchJavadocs - library={}, version={}, query={}, limit={}",
                library, version, query, limit);

        if (!config.isEnabled() || query == null || query.isBlank()) {
            return List.of();
        }

        int maxResults = Math.min(limit != null && limit > 0 ? limit : 10, 50);

        List<JavadocSearchResult> results = new ArrayList<>();

        // Resolve version if library is specified
        String resolvedVersion = null;
        if (library != null && !library.isBlank()) {
            Optional<String> versionOpt = versionService.resolveVersion(library, version);
            if (versionOpt.isPresent()) {
                resolvedVersion = versionOpt.get();
            }
        }

        // Search classes using existing methods
        List<JavadocClass> classResults;
        if (library != null && !library.isBlank() && resolvedVersion != null) {
            // Search within specific library/version
            classResults = classRepository.searchByKeyword(library, resolvedVersion, query, maxResults);
        } else {
            // Search globally
            classResults = classRepository.searchByKeywordGlobal(query, maxResults);
        }

        for (JavadocClass cls : classResults) {
            JavadocPackage pkg = cls.getJavadocPackage();
            results.add(JavadocSearchResult.fromClass(
                    pkg.getLibraryName(),
                    pkg.getVersion(),
                    cls.getFqcn(),
                    cls.getSimpleName(),
                    cls.getKind() != null ? cls.getKind().name() : null,
                    cls.getSummary(),
                    cls.getSourceUrl(),
                    cls.getDeprecated() != null && cls.getDeprecated(),
                    1.0 // Default score, ranking is done by the database ORDER BY
            ));
        }

        // Also search packages if we have room
        if (results.size() < maxResults && library != null && resolvedVersion != null) {
            int remaining = maxResults - results.size();
            List<JavadocPackage> packageResults = packageRepository.searchByKeyword(
                    library, resolvedVersion, query, remaining);

            for (JavadocPackage pkg : packageResults) {
                results.add(JavadocSearchResult.fromPackage(
                        pkg.getLibraryName(),
                        pkg.getVersion(),
                        pkg.getPackageName(),
                        pkg.getSummary(),
                        pkg.getSourceUrl(),
                        0.5 // Lower score for packages
                ));
            }
        }

        return results.subList(0, Math.min(results.size(), maxResults));
    }

    @McpTool(description = """
        List all available libraries with Javadoc documentation.
        Returns library names and their available versions.
        """)
    @Transactional(readOnly = true)
    public List<LibraryInfo> listJavadocLibraries() {
        log.info("Tool: listJavadocLibraries");

        if (!config.isEnabled()) {
            return List.of();
        }

        List<String> libraries = packageRepository.findDistinctLibraryNames();

        return libraries.stream()
                .map(lib -> {
                    List<String> versions = versionService.getAvailableVersions(lib);
                    String latest = versionService.getLatestVersion(lib).orElse(null);
                    return LibraryInfo.builder()
                            .library(lib)
                            .versions(versions)
                            .latestVersion(latest)
                            .versionCount(versions.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Library information result.
     */
    @lombok.Builder
    public record LibraryInfo(
            String library,
            List<String> versions,
            String latestVersion,
            int versionCount
    ) {}
}

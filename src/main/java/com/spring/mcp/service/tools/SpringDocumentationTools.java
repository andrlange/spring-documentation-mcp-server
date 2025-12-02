package com.spring.mcp.service.tools;

import com.spring.mcp.model.dto.DocumentationDto;
import com.spring.mcp.model.dto.mcp.*;
import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootCompatibility;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.CodeExampleRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringBootCompatibilityRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.SettingsService;
import com.spring.mcp.service.documentation.DocumentationService;
import com.spring.mcp.service.documentation.DocumentationServiceImpl;
import com.spring.mcp.service.mcp.McpVersionResolverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Tools for Spring Documentation
 * Uses @Tool annotation for Spring AI MCP Server auto-discovery
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpringDocumentationTools {

    private final DocumentationService documentationService;
    private final DocumentationServiceImpl documentationServiceImpl;
    private final SpringProjectRepository projectRepository;
    private final ProjectVersionRepository versionRepository;
    private final CodeExampleRepository codeExampleRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final SpringBootCompatibilityRepository compatibilityRepository;
    private final SettingsService settingsService;
    private final McpVersionResolverService versionResolver;

    /**
     * Search Spring documentation with full-text search
     */
    @McpTool(description = """
        Search across all Spring documentation. Supports filtering by project, version, and documentation type.
        Returns relevant documentation links and snippets with relevance ranking.
        """)
    public SearchDocsResponse searchSpringDocs(
            @McpToolParam(description = "Search query string (required)") String query,
            @McpToolParam(description = "Project slug filter (optional, e.g., 'spring-boot', 'spring-framework')") String project,
            @McpToolParam(description = "Version filter (optional, e.g., '3.5.7')") String version,
            @McpToolParam(description = "Documentation type filter (optional, e.g., 'reference', 'api')") String docType) {

        log.info("Tool: searchSpringDocs - query={}, project={}, version={}, docType={}", query, project, version, docType);
        Instant startTime = Instant.now();

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query parameter is required");
        }

        List<DocumentationDto> results = documentationService.search(query, project, version, docType);
        long totalResults = documentationServiceImpl.countSearchResults(query, project, version, docType);
        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        SearchDocsResponse.SearchFilters filters = new SearchDocsResponse.SearchFilters(
            project != null ? project : "all",
            version != null ? version : "all",
            docType != null ? docType : "all"
        );

        List<SearchDocsResponse.DocumentationResult> docResults = results.stream()
            .map(this::mapToDocumentationResult)
            .collect(Collectors.toList());

        return new SearchDocsResponse(
            query,
            filters,
            totalResults,
            results.size(),
            executionTimeMs,
            docResults
        );
    }

    /**
     * Get available versions for a Spring project
     */
    @McpTool(description = """
        List available versions for a Spring project. Shows latest stable, n-2 minor versions, and n+1 preview versions.
        """)
    public VersionsResponse getSpringVersions(
            @McpToolParam(description = "Project slug (required, e.g., 'spring-boot', 'spring-framework', 'spring-data')") String project) {

        log.info("Tool: getSpringVersions - project={}", project);

        if (project == null || project.isBlank()) {
            throw new IllegalArgumentException("Project parameter is required");
        }

        SpringProject springProject = projectRepository.findBySlug(project)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + project));

        List<ProjectVersion> versions = versionRepository.findByProjectOrderByVersionDesc(springProject);

        List<VersionsResponse.VersionInfo> versionInfos = versions.stream()
            .map(v -> new VersionsResponse.VersionInfo(
                v.getVersion(),
                v.getState().name(),
                v.getIsLatest(),
                v.getIsDefault(),
                v.getReleaseDate() != null ? v.getReleaseDate().toString() : "unknown"
            ))
            .collect(Collectors.toList());

        return new VersionsResponse(
            springProject.getName(),
            springProject.getSlug(),
            springProject.getDescription() != null ? springProject.getDescription() : "",
            versionInfos,
            versions.size()
        );
    }

    /**
     * List all available Spring projects
     */
    @McpTool(description = """
        List all available Spring projects in the documentation system.
        Returns project names, slugs, descriptions, and homepage URLs.
        """)
    public ProjectsListResponse listSpringProjects() {
        log.info("Tool: listSpringProjects");
        Instant startTime = Instant.now();

        List<SpringProject> projects = projectRepository.findByActiveTrue();
        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        List<ProjectsListResponse.ProjectInfo> projectInfos = projects.stream()
            .map(p -> new ProjectsListResponse.ProjectInfo(
                p.getName(),
                p.getSlug(),
                p.getDescription() != null ? p.getDescription() : "",
                p.getHomepageUrl() != null ? p.getHomepageUrl() : "",
                p.getGithubUrl() != null ? p.getGithubUrl() : ""
            ))
            .collect(Collectors.toList());

        return new ProjectsListResponse(
            projects.size(),
            executionTimeMs,
            projectInfos
        );
    }

    /**
     * Get all documentation for a specific Spring project version
     */
    @McpTool(description = """
        Get all documentation for a specific Spring project version.
        Returns all documentation links organized by type (reference, api, guides, etc.).
        """)
    public DocumentationByVersionResponse getDocumentationByVersion(
            @McpToolParam(description = "Project slug (required, e.g., 'spring-boot', 'spring-framework')") String project,
            @McpToolParam(description = "Version string (required, e.g., '3.5.7', '6.2.1')") String version) {

        log.info("Tool: getDocumentationByVersion - project={}, version={}", project, version);
        Instant startTime = Instant.now();

        if (project == null || project.isBlank()) {
            throw new IllegalArgumentException("Project parameter is required");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version parameter is required");
        }

        SpringProject springProject = projectRepository.findBySlug(project)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + project));

        // Use version resolver to handle naming inconsistencies (e.g., 4.0.0 vs 4.0.0.RELEASE)
        ProjectVersion projectVersion = versionResolver.resolveVersion(springProject, version)
            .orElseThrow(() -> new IllegalArgumentException(
                "Version " + version + " not found for project " + project));

        // Aggregate docs from all related version records
        List<ProjectVersion> relatedVersions = versionResolver.findRelatedVersions(springProject, version);
        List<DocumentationDto> docs = relatedVersions.stream()
            .flatMap(pv -> documentationService.getByVersion(pv.getId()).stream())
            .distinct()
            .collect(Collectors.toList());
        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        Map<String, List<DocumentationByVersionResponse.DocumentationItem>> groupedDocs = docs.stream()
            .collect(Collectors.groupingBy(
                DocumentationDto::getDocType,
                Collectors.mapping(this::mapToDocumentationItem, Collectors.toList())
            ));

        List<DocumentationByVersionResponse.DocumentationItem> allDocuments = docs.stream()
            .map(this::mapToDocumentationItem)
            .collect(Collectors.toList());

        return new DocumentationByVersionResponse(
            springProject.getName(),
            project,
            version,
            projectVersion.getState().name(),
            projectVersion.getIsLatest(),
            docs.size(),
            executionTimeMs,
            groupedDocs,
            allDocuments
        );
    }

    /**
     * Search for code examples with optional filters
     */
    @McpTool(description = """
        Search for code examples with optional filters.
        Returns code snippets, descriptions, and metadata.
        """)
    public CodeExamplesResponse getCodeExamples(
            @McpToolParam(description = "Search query for title/description (optional)") String query,
            @McpToolParam(description = "Project slug filter (optional, e.g., 'spring-boot')") String project,
            @McpToolParam(description = "Version filter (optional, e.g., '3.5.7')") String version,
            @McpToolParam(description = "Programming language filter (optional, e.g., 'java', 'kotlin', 'groovy')") String language,
            @McpToolParam(description = "Maximum number of results (optional, default: 10, max: 50)") Integer limit) {

        log.info("Tool: getCodeExamples - query={}, project={}, version={}, language={}, limit={}",
            query, project, version, language, limit);
        Instant startTime = Instant.now();

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;

        List<CodeExample> examples;

        if (project != null && !project.isBlank() && version != null && !version.isBlank()) {
            SpringProject springProject = projectRepository.findBySlug(project)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + project));

            // Use version resolver to aggregate examples from related versions
            // This handles cases where docs are under "4.0.0" but examples under "4.0.0.RELEASE"
            if (language != null && !language.isBlank()) {
                examples = versionResolver.getCodeExamplesAggregated(springProject, version, language);
            } else {
                examples = versionResolver.getCodeExamplesAggregated(springProject, version);
            }
        } else if (query != null && !query.isBlank()) {
            examples = codeExampleRepository.searchByTitle(query);
        } else {
            examples = codeExampleRepository.findAll();
        }

        if (query != null && !query.isBlank() && (project != null || version != null)) {
            String lowerQuery = query.toLowerCase();
            examples = examples.stream()
                .filter(e -> e.getTitle().toLowerCase().contains(lowerQuery) ||
                    (e.getDescription() != null && e.getDescription().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
        }

        int totalFound = examples.size();
        List<CodeExample> limitedExamples = examples.stream()
            .limit(effectiveLimit)
            .collect(Collectors.toList());

        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        CodeExamplesResponse.CodeExampleFilters filters = new CodeExamplesResponse.CodeExampleFilters(
            query != null ? query : "none",
            project != null ? project : "all",
            version != null ? version : "all",
            language != null ? language : "all",
            effectiveLimit
        );

        List<CodeExamplesResponse.CodeExampleItem> exampleItems = limitedExamples.stream()
            .map(this::mapToCodeExampleItem)
            .collect(Collectors.toList());

        return new CodeExamplesResponse(
            filters,
            totalFound,
            limitedExamples.size(),
            executionTimeMs,
            exampleItems
        );
    }

    // ==================== Spring Boot Version Tools ====================

    /**
     * List all Spring Boot versions with optional filtering
     */
    @McpTool(description = """
        List all Spring Boot versions available in the system. Results include version numbers, states (GA, RC, SNAPSHOT, MILESTONE),
        release dates, and support end dates (OSS and Enterprise). Results are ordered by version descending (latest first).
        """)
    public SpringBootVersionsResponse listSpringBootVersions(
            @McpToolParam(description = "Filter by version state (optional): 'GA', 'RC', 'SNAPSHOT', 'MILESTONE'") String state,
            @McpToolParam(description = "Maximum number of results (optional, default: 20, max: 100)") Integer limit) {

        log.info("Tool: listSpringBootVersions - state={}, limit={}", state, limit);
        Instant startTime = Instant.now();

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
        boolean enterpriseEnabled = settingsService.isEnterpriseSubscriptionEnabled();

        List<SpringBootVersion> versions;
        if (state != null && !state.isBlank()) {
            try {
                VersionState versionState = VersionState.valueOf(state.toUpperCase());
                versions = springBootVersionRepository.findByState(versionState);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid state: " + state + ". Valid values: GA, RC, SNAPSHOT, MILESTONE");
            }
        } else {
            versions = springBootVersionRepository.findAllOrderByVersionDesc();
        }

        int totalFound = versions.size();
        List<SpringBootVersion> limitedVersions = versions.stream()
            .limit(effectiveLimit)
            .collect(Collectors.toList());

        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        SpringBootVersionsResponse.SpringBootVersionFilters filters =
            new SpringBootVersionsResponse.SpringBootVersionFilters(
                state != null ? state : "all",
                effectiveLimit
            );

        List<SpringBootVersionsResponse.SpringBootVersionInfo> versionInfos = limitedVersions.stream()
            .map(v -> mapToSpringBootVersionInfo(v, enterpriseEnabled))
            .collect(Collectors.toList());

        return new SpringBootVersionsResponse(
            filters,
            enterpriseEnabled,
            totalFound,
            limitedVersions.size(),
            executionTimeMs,
            versionInfos
        );
    }

    /**
     * Get the latest Spring Boot version for a given major.minor combination
     */
    @McpTool(description = """
        Get the latest patch version for a specific Spring Boot major.minor version.
        For example, for Spring Boot 3.5, returns the latest 3.5.x version.
        """)
    public LatestSpringBootVersionResponse getLatestSpringBootVersion(
            @McpToolParam(description = "Major version number (required, e.g., 3)") Integer majorVersion,
            @McpToolParam(description = "Minor version number (required, e.g., 5)") Integer minorVersion) {

        log.info("Tool: getLatestSpringBootVersion - major={}, minor={}", majorVersion, minorVersion);
        Instant startTime = Instant.now();

        if (majorVersion == null || minorVersion == null) {
            throw new IllegalArgumentException("Both majorVersion and minorVersion parameters are required");
        }

        boolean enterpriseEnabled = settingsService.isEnterpriseSubscriptionEnabled();

        List<SpringBootVersion> versions = springBootVersionRepository
            .findByMajorVersionAndMinorVersionOrderByPatchVersionDesc(majorVersion, minorVersion);

        if (versions.isEmpty()) {
            throw new IllegalArgumentException(
                "No Spring Boot version found for " + majorVersion + "." + minorVersion);
        }

        SpringBootVersion latestVersion = versions.get(0);
        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        List<SpringBootVersionsResponse.SpringBootVersionInfo> allVersionInfos = versions.stream()
            .map(v -> mapToSpringBootVersionInfo(v, enterpriseEnabled))
            .collect(Collectors.toList());

        return new LatestSpringBootVersionResponse(
            majorVersion,
            minorVersion,
            versions.size(),
            executionTimeMs,
            mapToSpringBootVersionInfo(latestVersion, enterpriseEnabled),
            allVersionInfos
        );
    }

    /**
     * Filter Spring Boot versions by support status
     */
    @McpTool(description = """
        Filter Spring Boot versions by their support status (active or ended).
        Uses the system's enterprise subscription setting to determine which support date to check.
        """)
    public FilteredSpringBootVersionsResponse filterSpringBootVersionsBySupport(
            @McpToolParam(description = "Filter by support status (optional) - 'true' for supported versions, 'false' for end-of-life") Boolean supportActive,
            @McpToolParam(description = "Maximum number of results (optional, default: 20, max: 100)") Integer limit) {

        log.info("Tool: filterSpringBootVersionsBySupport - supportActive={}, limit={}", supportActive, limit);
        Instant startTime = Instant.now();

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
        boolean enterpriseEnabled = settingsService.isEnterpriseSubscriptionEnabled();
        LocalDate today = LocalDate.now();

        List<SpringBootVersion> allVersions = springBootVersionRepository.findAllOrderByVersionDesc();

        List<SpringBootVersion> filteredVersions = allVersions.stream()
            .filter(v -> {
                if (supportActive == null) {
                    return true;
                }

                LocalDate supportEndDate = enterpriseEnabled ? v.getEnterpriseSupportEnd() : v.getOssSupportEnd();

                if (supportEndDate == null) {
                    return supportActive;
                }

                boolean isSupported = today.isBefore(supportEndDate);
                return supportActive == isSupported;
            })
            .limit(effectiveLimit)
            .collect(Collectors.toList());

        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        FilteredSpringBootVersionsResponse.SupportFilters filters =
            new FilteredSpringBootVersionsResponse.SupportFilters(
                supportActive != null ? supportActive.toString() : "all",
                effectiveLimit
            );

        List<SpringBootVersionsResponse.SpringBootVersionInfo> versionInfos = filteredVersions.stream()
            .map(v -> mapToSpringBootVersionInfo(v, enterpriseEnabled))
            .collect(Collectors.toList());

        return new FilteredSpringBootVersionsResponse(
            filters,
            enterpriseEnabled,
            enterpriseEnabled ? "enterpriseSupportEnd" : "ossSupportEnd",
            filteredVersions.size(),
            executionTimeMs,
            versionInfos
        );
    }

    /**
     * List all Spring projects compatible with a specific Spring Boot version
     */
    @McpTool(description = """
        List all Spring projects that are compatible with a specific Spring Boot version (major.minor).
        Returns projects and their compatible versions for the given Spring Boot version.
        """)
    public ProjectsBySpringBootVersionResponse listProjectsBySpringBootVersion(
            @McpToolParam(description = "Spring Boot major version number (required, e.g., 3)") Integer majorVersion,
            @McpToolParam(description = "Spring Boot minor version number (required, e.g., 5)") Integer minorVersion) {

        log.info("Tool: listProjectsBySpringBootVersion - major={}, minor={}", majorVersion, minorVersion);
        Instant startTime = Instant.now();

        if (majorVersion == null || minorVersion == null) {
            throw new IllegalArgumentException("Both majorVersion and minorVersion parameters are required");
        }

        List<SpringBootVersion> bootVersions = springBootVersionRepository
            .findByMajorVersionAndMinorVersionOrderByPatchVersionDesc(majorVersion, minorVersion);

        if (bootVersions.isEmpty()) {
            throw new IllegalArgumentException(
                "No Spring Boot version found for " + majorVersion + "." + minorVersion);
        }

        SpringBootVersion springBootVersion = bootVersions.get(0);

        List<SpringBootCompatibility> compatibilities =
            compatibilityRepository.findAllBySpringBootVersionIdWithProjectDetails(springBootVersion.getId());

        Map<String, List<ProjectVersion>> projectVersionMap = compatibilities.stream()
            .map(SpringBootCompatibility::getCompatibleProjectVersion)
            .collect(Collectors.groupingBy(
                pv -> pv.getProject().getSlug(),
                Collectors.toList()
            ));

        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        ProjectsBySpringBootVersionResponse.SpringBootVersionInfo versionInfo =
            new ProjectsBySpringBootVersionResponse.SpringBootVersionInfo(
                springBootVersion.getVersion(),
                springBootVersion.getMajorVersion(),
                springBootVersion.getMinorVersion(),
                springBootVersion.getState().name()
            );

        List<ProjectsBySpringBootVersionResponse.CompatibleProject> compatibleProjects =
            projectVersionMap.entrySet().stream()
                .map(entry -> {
                    ProjectVersion firstVersion = entry.getValue().get(0);
                    SpringProject project = firstVersion.getProject();

                    List<ProjectsBySpringBootVersionResponse.CompatibleVersion> compatibleVersions =
                        entry.getValue().stream()
                            .map(pv -> new ProjectsBySpringBootVersionResponse.CompatibleVersion(
                                pv.getVersion(),
                                pv.getState().name(),
                                pv.getIsLatest(),
                                pv.getReferenceDocUrl() != null ? pv.getReferenceDocUrl() : "",
                                pv.getApiDocUrl() != null ? pv.getApiDocUrl() : ""
                            ))
                            .collect(Collectors.toList());

                    return new ProjectsBySpringBootVersionResponse.CompatibleProject(
                        project.getSlug(),
                        project.getName(),
                        project.getDescription() != null ? project.getDescription() : "",
                        project.getHomepageUrl() != null ? project.getHomepageUrl() : "",
                        compatibleVersions
                    );
                })
                .collect(Collectors.toList());

        return new ProjectsBySpringBootVersionResponse(
            versionInfo,
            projectVersionMap.size(),
            compatibilities.size(),
            executionTimeMs,
            compatibleProjects
        );
    }

    /**
     * Find Spring projects by use case keyword search
     */
    @McpTool(description = """
        Search for Spring projects by use case. Searches in project names and descriptions for keywords.
        Useful for finding projects that solve specific problems or use cases.
        """)
    public ProjectsByUseCaseResponse findProjectsByUseCase(
            @McpToolParam(description = "Use case keyword or phrase (required, e.g., 'data access', 'security', 'messaging', 'web')") String useCase) {

        log.info("Tool: findProjectsByUseCase - useCase={}", useCase);
        Instant startTime = Instant.now();

        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase parameter is required");
        }

        List<SpringProject> allProjects = projectRepository.findByActiveTrue();
        String lowerCaseQuery = useCase.toLowerCase();

        List<SpringProject> matchingProjects = allProjects.stream()
            .filter(p -> {
                String name = p.getName().toLowerCase();
                String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                return name.contains(lowerCaseQuery) || desc.contains(lowerCaseQuery);
            })
            .collect(Collectors.toList());

        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

        List<ProjectsByUseCaseResponse.ProjectInfo> projectInfos = matchingProjects.stream()
            .map(p -> new ProjectsByUseCaseResponse.ProjectInfo(
                p.getName(),
                p.getSlug(),
                p.getDescription() != null ? p.getDescription() : "",
                p.getHomepageUrl() != null ? p.getHomepageUrl() : "",
                p.getGithubUrl() != null ? p.getGithubUrl() : ""
            ))
            .collect(Collectors.toList());

        return new ProjectsByUseCaseResponse(
            useCase,
            matchingProjects.size(),
            executionTimeMs,
            projectInfos
        );
    }

    // ==================== Helper Methods ====================

    private SearchDocsResponse.DocumentationResult mapToDocumentationResult(DocumentationDto dto) {
        return new SearchDocsResponse.DocumentationResult(
            dto.getId(),
            dto.getTitle(),
            dto.getUrl(),
            dto.getDescription() != null ? dto.getDescription() : "",
            dto.getProjectName(),
            dto.getProjectSlug(),
            dto.getVersion(),
            dto.getDocType(),
            dto.getContentType() != null ? dto.getContentType() : "",
            dto.getSnippet() != null && !dto.getSnippet().isEmpty() ? dto.getSnippet() : null,
            dto.getRank() != null && dto.getRank() > 0 ? dto.getRank() : null,
            dto.getRank() != null && dto.getRank() > 0 ? calculateRelevanceLabel(dto.getRank()) : null
        );
    }

    private DocumentationByVersionResponse.DocumentationItem mapToDocumentationItem(DocumentationDto dto) {
        return new DocumentationByVersionResponse.DocumentationItem(
            dto.getId(),
            dto.getTitle(),
            dto.getUrl(),
            dto.getDescription() != null ? dto.getDescription() : "",
            dto.getProjectName(),
            dto.getProjectSlug(),
            dto.getVersion(),
            dto.getDocType(),
            dto.getContentType() != null ? dto.getContentType() : "",
            dto.getSnippet() != null && !dto.getSnippet().isEmpty() ? dto.getSnippet() : null,
            dto.getRank() != null && dto.getRank() > 0 ? dto.getRank() : null,
            dto.getRank() != null && dto.getRank() > 0 ? calculateRelevanceLabel(dto.getRank()) : null
        );
    }

    private CodeExamplesResponse.CodeExampleItem mapToCodeExampleItem(CodeExample example) {
        ProjectVersion version = example.getVersion();

        return new CodeExamplesResponse.CodeExampleItem(
            example.getId(),
            example.getTitle(),
            example.getDescription() != null ? example.getDescription() : "",
            example.getCodeSnippet(),
            example.getLanguage(),
            example.getCategory() != null ? example.getCategory() : "",
            example.getTags() != null && example.getTags().length > 0 ?
                Arrays.asList(example.getTags()) : List.of(),
            example.getSourceUrl() != null ? example.getSourceUrl() : "",
            version != null ? version.getProject().getName() : null,
            version != null ? version.getProject().getSlug() : null,
            version != null ? version.getVersion() : null
        );
    }

    private SpringBootVersionsResponse.SpringBootVersionInfo mapToSpringBootVersionInfo(
            SpringBootVersion version, boolean enterpriseEnabled) {

        return new SpringBootVersionsResponse.SpringBootVersionInfo(
            version.getId(),
            version.getVersion(),
            version.getMajorVersion(),
            version.getMinorVersion(),
            version.getPatchVersion() != null ? version.getPatchVersion() : 0,
            version.getState().name(),
            version.getIsCurrent(),
            version.getReleasedAt() != null ? version.getReleasedAt().toString() : null,
            version.getOssSupportEnd() != null ? version.getOssSupportEnd().toString() : null,
            version.getEnterpriseSupportEnd() != null ? version.getEnterpriseSupportEnd().toString() : null,
            version.referenceDocUrlFormatted(),
            version.apiDocUrlFormatted(),
            version.isOssSupportActive(),
            version.isEnterpriseSupportActive(),
            version.isEndOfLife(),
            enterpriseEnabled ? version.isEnterpriseSupportActive() : version.isOssSupportActive()
        );
    }

    private String calculateRelevanceLabel(double rank) {
        if (rank > 0.5) {
            return "high";
        } else if (rank > 0.2) {
            return "medium";
        } else {
            return "low";
        }
    }
}

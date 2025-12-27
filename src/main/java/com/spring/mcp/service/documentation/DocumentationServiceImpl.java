package com.spring.mcp.service.documentation;

import com.spring.mcp.model.dto.DocumentationDto;
import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.DocumentationContentRepository;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentationService with full-text search support
 * Uses PostgreSQL full-text search with ts_rank_cd ranking
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentationServiceImpl implements DocumentationService {

    private final DocumentationContentRepository contentRepository;
    private final DocumentationLinkRepository linkRepository;
    private final SpringProjectRepository projectRepository;
    private final ProjectVersionRepository versionRepository;

    @Value("${mcp.documentation.search.default-limit:5}")
    private int defaultLimit;

    @Value("${mcp.documentation.search.max-results:50}")
    private int maxResults;

    @Value("${mcp.documentation.search.highlight:true}")
    private boolean enableHighlight;

    /**
     * Search documentation with full-text search and comprehensive filters
     * Uses PostgreSQL's ts_rank_cd for ranking and supports pagination
     *
     * @param query Search query (required)
     * @param project Project slug (optional)
     * @param version Version string (optional)
     * @param docType Documentation type slug (optional)
     * @return List of matching documentation sorted by rank
     */
    @Override
    @Cacheable(value = "documentationSearch", key = "#query + '-' + #project + '-' + #version + '-' + #docType", unless = "#result.isEmpty()")
    public List<DocumentationDto> search(String query, String project, String version, String docType) {
        return searchDocumentation(query, project, version, docType, defaultLimit, 0);
    }

    /**
     * Search documentation with pagination support
     *
     * @param query Search query (required)
     * @param project Project slug (optional)
     * @param version Version string (optional)
     * @param docType Documentation type slug (optional)
     * @param limit Maximum number of results
     * @param offset Number of results to skip
     * @return List of matching documentation sorted by rank
     */
    @Cacheable(value = "documentationSearchPaged",
            key = "#query + '-' + #project + '-' + #version + '-' + #docType + '-' + #limit + '-' + #offset",
            unless = "#result.isEmpty()")
    public List<DocumentationDto> searchDocumentation(String query, String project, String version,
                                                       String docType, int limit, int offset) {
        log.debug("Searching documentation: query='{}', project='{}', version='{}', docType='{}', limit={}, offset={}",
                query, project, version, docType, limit, offset);

        // Validate query
        if (query == null || query.trim().isEmpty()) {
            log.warn("Search query is empty");
            return Collections.emptyList();
        }

        // Sanitize and validate limit and offset
        int effectiveLimit = Math.min(limit, maxResults);
        int effectiveOffset = Math.max(offset, 0);

        try {
            // Execute full-text search - returns link IDs only
            List<Long> linkIds = contentRepository.advancedFullTextSearch(
                    query.trim(),
                    project,
                    version,
                    docType,
                    effectiveLimit,
                    effectiveOffset
            );

            log.info("Found {} search results for query '{}' (limit={}, offset={})",
                    linkIds.size(), query, effectiveLimit, effectiveOffset);

            // Fetch DocumentationLink entities by IDs and convert to DTOs
            return linkIds.stream()
                    .map(linkId -> linkRepository.findById(linkId).orElse(null))
                    .filter(link -> link != null)
                    .map(this::mapLinkToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during documentation search: query='{}', project='{}', version='{}', docType='{}'",
                    query, project, version, docType, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all documentation for a specific project version
     *
     * @param versionId Version ID
     * @return List of all documentation for the version
     */
    @Override
    @Cacheable(value = "documentationByVersion", key = "#versionId")
    public List<DocumentationDto> getByVersion(Long versionId) {
        log.debug("Getting documentation for version ID: {}", versionId);

        try {
            // Find the version
            ProjectVersion version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

            // Get all active documentation links for this version
            List<DocumentationLink> links = linkRepository.findByVersionAndIsActiveTrue(version);

            log.info("Found {} documentation links for version ID {}", links.size(), versionId);

            // Convert to DTOs
            return links.stream()
                    .map(this::mapLinkToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting documentation for version ID {}", versionId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get versions for a project
     *
     * @param projectSlug Project slug
     * @return List of versions
     */
    public List<ProjectVersion> getVersions(String projectSlug) {
        log.debug("Getting versions for project: {}", projectSlug);

        try {
            SpringProject project = projectRepository.findBySlug(projectSlug)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectSlug));

            List<ProjectVersion> versions = versionRepository.findByProjectOrderByVersionDesc(project);

            log.info("Found {} versions for project '{}'", versions.size(), projectSlug);

            return versions;

        } catch (Exception e) {
            log.error("Error getting versions for project '{}'", projectSlug, e);
            return Collections.emptyList();
        }
    }

    /**
     * List all active Spring projects
     *
     * @return List of active projects
     */
    public List<SpringProject> listProjects() {
        log.debug("Listing all active Spring projects");

        try {
            List<SpringProject> projects = projectRepository.findAllActiveOrderByName();

            log.info("Found {} active Spring projects", projects.size());

            return projects;

        } catch (Exception e) {
            log.error("Error listing Spring projects", e);
            return Collections.emptyList();
        }
    }

    /**
     * Count total search results (for pagination)
     *
     * @param query Search query
     * @param project Project slug (optional)
     * @param version Version string (optional)
     * @param docType Documentation type slug (optional)
     * @return Total count of matching documents
     */
    public long countSearchResults(String query, String project, String version, String docType) {
        log.debug("Counting search results: query='{}', project='{}', version='{}', docType='{}'",
                query, project, version, docType);

        if (query == null || query.trim().isEmpty()) {
            return 0L;
        }

        try {
            Long count = contentRepository.countAdvancedSearch(
                    query.trim(),
                    project,
                    version,
                    docType
            );

            log.debug("Found {} total results for query '{}'", count, query);

            return count != null ? count : 0L;

        } catch (Exception e) {
            log.error("Error counting search results: query='{}'", query, e);
            return 0L;
        }
    }

    /**
     * Map search result Object[] to DocumentationDto
     * Object[] structure matches the SELECT in advancedFullTextSearch query
     */
    private DocumentationDto mapToDocumentationDto(Object[] result) {
        try {
            // Extract fields from result array
            // Index matches SELECT order in advancedFullTextSearch query
            BigInteger contentId = (BigInteger) result[0];
            String content = (String) result[1];
            String contentType = (String) result[2];
            BigInteger linkId = (BigInteger) result[3];
            String title = (String) result[4];
            String url = (String) result[5];
            String description = (String) result[6];
            BigInteger projectId = (BigInteger) result[7];
            String projectName = (String) result[8];
            String projectSlug = (String) result[9];
            BigInteger versionId = (BigInteger) result[10];
            String version = (String) result[11];
            BigInteger docTypeId = (BigInteger) result[12];
            String docTypeName = (String) result[13];
            String docTypeSlug = (String) result[14];
            Double rank = result[15] != null ? ((Number) result[15]).doubleValue() : 0.0;

            // Generate snippet from content
            String snippet = generateSnippet(content);

            return DocumentationDto.builder()
                    .id(linkId.longValue())
                    .title(title)
                    .url(url)
                    .description(description)
                    .projectName(projectName)
                    .projectSlug(projectSlug)
                    .version(version)
                    .docType(docTypeName)
                    .snippet(snippet)
                    .rank(rank)
                    .contentType(contentType)
                    .build();

        } catch (Exception e) {
            log.error("Error mapping search result to DTO", e);
            return null;
        }
    }

    /**
     * Map DocumentationLink entity to DocumentationDto
     */
    private DocumentationDto mapLinkToDto(DocumentationLink link) {
        try {
            ProjectVersion version = link.getVersion();
            SpringProject project = version.getProject();

            return DocumentationDto.builder()
                    .id(link.getId())
                    .title(link.getTitle())
                    .url(link.getUrl())
                    .description(link.getDescription())
                    .projectName(project.getName())
                    .projectSlug(project.getSlug())
                    .version(version.getVersion())
                    .docType(link.getDocType().getName())
                    .contentType(link.getContent() != null ? link.getContent().getContentType() : null)
                    .build();

        } catch (Exception e) {
            log.error("Error mapping DocumentationLink to DTO: linkId={}", link.getId(), e);
            return null;
        }
    }

    /**
     * Generate a snippet from content for preview
     * Takes first 300 characters and adds ellipsis
     */
    private String generateSnippet(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Remove excessive whitespace
        String cleaned = content.trim().replaceAll("\\s+", " ");

        // Return first 300 characters
        if (cleaned.length() <= 300) {
            return cleaned;
        }

        // Find a good breaking point (end of sentence or word)
        int breakPoint = 300;
        int lastPeriod = cleaned.lastIndexOf('.', breakPoint);
        int lastSpace = cleaned.lastIndexOf(' ', breakPoint);

        if (lastPeriod > 200) {
            breakPoint = lastPeriod + 1;
        } else if (lastSpace > 250) {
            breakPoint = lastSpace;
        }

        return cleaned.substring(0, breakPoint).trim() + "...";
    }
}

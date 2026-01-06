package com.spring.mcp.service.documentation;

import com.spring.mcp.model.dto.DocumentationDto;
import java.util.List;

/**
 * Service interface for documentation operations
 */
public interface DocumentationService {

    /**
     * Search documentation
     * @param query Search query
     * @param project Project slug (optional)
     * @param version Version string (optional)
     * @param docType Documentation type (optional)
     * @return List of matching documentation
     */
    List<DocumentationDto> search(String query, String project, String version, String docType);

    /**
     * Get all documentation for a project version
     */
    List<DocumentationDto> getByVersion(Long versionId);

    /**
     * Fix markdown artifacts in existing documentation content.
     * Processes all documentation content to fix:
     * - Table formatting issues from HTML conversion
     * - Anchor artifacts like {#_test_code}
     *
     * @return the number of documents fixed
     */
    int fixExistingMarkdownArtifacts();
}

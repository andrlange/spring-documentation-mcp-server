package com.spring.mcp.repository;

import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.model.entity.DocumentationType;
import com.spring.mcp.model.entity.ProjectVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DocumentationLink entities
 */
@Repository
public interface DocumentationLinkRepository extends JpaRepository<DocumentationLink, Long> {

    /**
     * Find all links for a specific version
     */
    List<DocumentationLink> findByVersion(ProjectVersion version);

    /**
     * Find all active links for a specific version
     */
    List<DocumentationLink> findByVersionAndIsActiveTrue(ProjectVersion version);

    /**
     * Find links by version and documentation type
     */
    List<DocumentationLink> findByVersionAndDocType(ProjectVersion version, DocumentationType docType);

    /**
     * Find active links by version and type
     */
    List<DocumentationLink> findByVersionAndDocTypeAndIsActiveTrue(
        ProjectVersion version,
        DocumentationType docType
    );

    /**
     * Find link by URL
     */
    Optional<DocumentationLink> findByUrl(String url);

    /**
     * Search documentation links by title
     */
    @Query("SELECT d FROM DocumentationLink d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) AND d.isActive = true")
    List<DocumentationLink> searchByTitle(@Param("query") String query);

    /**
     * Count active links for a version
     */
    long countByVersionAndIsActiveTrue(ProjectVersion version);

    /**
     * Count active links for a version and doc type
     */
    long countByVersionAndDocTypeAndIsActiveTrue(ProjectVersion version, DocumentationType docType);

    /**
     * Find all links for a specific version ID
     */
    List<DocumentationLink> findByVersionId(Long versionId);

    /**
     * Count documentation links that have been updated in the last N days
     *
     * @param days number of days to look back
     * @return count of documentation links recently updated
     */
    @Query(value = """
        SELECT COUNT(d.id)
        FROM documentation_links d
        WHERE d.updated_at >= CURRENT_TIMESTAMP - INTERVAL '1 day' * :days
        AND d.is_active = true
        """, nativeQuery = true)
    long countWithRecentlyUpdatedVersions(@Param("days") int days);

    /**
     * Find GitHub Reference docs by project slug and normalized version (major.minor.patch).
     * This matches GitHub docs to Overview docs by normalizing version strings.
     *
     * @param projectSlug the project slug (e.g., "spring-boot")
     * @param versionPrefix the version prefix to match (e.g., "4.0.0" matches "4.0.0", "4.0.0.RELEASE", etc.)
     * @return list of GitHub Reference documentation links
     */
    @Query(value = """
        SELECT dl.*
        FROM documentation_links dl
        JOIN documentation_types dt ON dl.doc_type_id = dt.id
        JOIN project_versions pv ON dl.version_id = pv.id
        JOIN spring_projects sp ON pv.project_id = sp.id
        WHERE sp.slug = :projectSlug
        AND dt.name = 'GitHub Reference'
        AND dl.is_active = true
        AND (
            pv.version = :versionPrefix
            OR pv.version LIKE :versionPrefix || '.%'
            OR pv.version LIKE :versionPrefix || '-SNAPSHOT'
            OR REPLACE(REPLACE(pv.version, '.RELEASE', ''), '.BUILD-SNAPSHOT', '') = :versionPrefix
        )
        ORDER BY dl.title
        """, nativeQuery = true)
    List<DocumentationLink> findGitHubDocsByProjectAndVersion(
        @Param("projectSlug") String projectSlug,
        @Param("versionPrefix") String versionPrefix
    );

    /**
     * Find GitHub Reference docs by project ID.
     * Returns all GitHub docs for the given project.
     *
     * @param projectId the project ID
     * @return list of GitHub Reference documentation links
     */
    @Query(value = """
        SELECT dl.*
        FROM documentation_links dl
        JOIN documentation_types dt ON dl.doc_type_id = dt.id
        JOIN project_versions pv ON dl.version_id = pv.id
        WHERE pv.project_id = :projectId
        AND dt.name = 'GitHub Reference'
        AND dl.is_active = true
        ORDER BY pv.version DESC, dl.title
        """, nativeQuery = true)
    List<DocumentationLink> findGitHubDocsByProject(@Param("projectId") Long projectId);
}

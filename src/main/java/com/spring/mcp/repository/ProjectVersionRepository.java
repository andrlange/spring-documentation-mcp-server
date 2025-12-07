package com.spring.mcp.repository;

import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProjectVersion entities
 */
@Repository
public interface ProjectVersionRepository extends JpaRepository<ProjectVersion, Long> {

    /**
     * Find all versions for a specific project
     */
    List<ProjectVersion> findByProject(SpringProject project);

    /**
     * Find all versions for a project, ordered by version
     */
    @Query("SELECT v FROM ProjectVersion v WHERE v.project = :project ORDER BY v.majorVersion DESC, v.minorVersion DESC, v.patchVersion DESC")
    List<ProjectVersion> findByProjectOrderByVersionDesc(@Param("project") SpringProject project);

    /**
     * Find versions by project ID
     */
    List<ProjectVersion> findByProjectId(Long projectId);

    /**
     * Find version by project and version string
     */
    Optional<ProjectVersion> findByProjectAndVersion(SpringProject project, String version);

    /**
     * Find the latest version for a project
     */
    Optional<ProjectVersion> findByProjectAndIsLatestTrue(SpringProject project);

    /**
     * Find the default version for a project
     */
    Optional<ProjectVersion> findByProjectAndIsDefaultTrue(SpringProject project);

    /**
     * Find all versions of a specific state
     */
    List<ProjectVersion> findByState(VersionState state);

    /**
     * Find all versions by project and state
     */
    List<ProjectVersion> findByProjectAndState(SpringProject project, VersionState state);

    /**
     * Find versions by project and status (CURRENT, GA, etc.) ordered by version descending
     */
    @Query("SELECT v FROM ProjectVersion v WHERE v.project = :project AND v.status = :status ORDER BY v.majorVersion DESC, v.minorVersion DESC, v.patchVersion DESC")
    List<ProjectVersion> findByProjectAndStatusOrderByVersionDesc(@Param("project") SpringProject project, @Param("status") String status);

    /**
     * Check if a version exists for a project
     */
    boolean existsByProjectAndVersion(SpringProject project, String version);

    /**
     * Find all versions compatible with a specific Spring Boot version
     *
     * @param springBootVersion the Spring Boot version (e.g., "3.5.x", "3.4.x")
     * @param pageable pagination information
     * @return page of versions compatible with the specified Spring Boot version
     */
    @Query(value = """
        SELECT DISTINCT v FROM ProjectVersion v
        JOIN FETCH v.project
        JOIN SpringBootCompatibility sbc ON sbc.compatibleProjectVersion = v
        JOIN SpringBootVersion sbv ON sbc.springBootVersion = sbv
        WHERE sbv.version = :springBootVersion
        """,
        countQuery = """
        SELECT COUNT(DISTINCT v) FROM ProjectVersion v
        JOIN SpringBootCompatibility sbc ON sbc.compatibleProjectVersion = v
        JOIN SpringBootVersion sbv ON sbc.springBootVersion = sbv
        WHERE sbv.version = :springBootVersion
        """)
    Page<ProjectVersion> findBySpringBootVersion(@Param("springBootVersion") String springBootVersion, Pageable pageable);

    /**
     * Find all distinct Spring Boot versions that have compatibility mappings
     *
     * @return list of Spring Boot versions ordered by major, minor, patch version descending
     */
    @Query(value = """
        SELECT version FROM (
            SELECT DISTINCT sbv.version, sbv.major_version, sbv.minor_version, sbv.patch_version
            FROM spring_boot_versions sbv
            JOIN spring_boot_compatibility sbc ON sbc.spring_boot_version_id = sbv.id
        ) AS subquery
        ORDER BY major_version DESC, minor_version DESC, patch_version DESC
        """, nativeQuery = true)
    List<String> findDistinctSpringBootVersions();

    /**
     * Find all versions with project eagerly fetched
     *
     * @param pageable pagination information
     * @return page of versions with projects
     */
    @Query(value = "SELECT v FROM ProjectVersion v JOIN FETCH v.project",
           countQuery = "SELECT COUNT(v) FROM ProjectVersion v")
    Page<ProjectVersion> findAllWithProject(Pageable pageable);

    /**
     * Find the first version for a project ordered by creation date descending (latest first)
     *
     * @param project the Spring project
     * @return the most recently created version for the project
     */
    Optional<ProjectVersion> findFirstByProjectOrderByCreatedAtDesc(SpringProject project);

    /**
     * Find all versions by project slug
     *
     * @param projectSlug the project slug
     * @return list of versions for the project
     */
    @Query("SELECT v FROM ProjectVersion v JOIN v.project p WHERE p.slug = :projectSlug")
    List<ProjectVersion> findByProjectSlug(@Param("projectSlug") String projectSlug);

    /**
     * Find all versions of a specific project that are compatible with a specific Spring Boot version.
     * Uses the spring_boot_compatibility table to determine compatibility.
     *
     * @param projectId the project ID
     * @param springBootVersionId the Spring Boot version ID
     * @return list of compatible versions for the project
     */
    @Query("""
        SELECT DISTINCT v
        FROM ProjectVersion v
        JOIN SpringBootCompatibility sbc ON sbc.compatibleProjectVersion = v
        WHERE v.project.id = :projectId
        AND sbc.springBootVersion.id = :springBootVersionId
        ORDER BY v.majorVersion DESC, v.minorVersion DESC, v.patchVersion DESC
        """)
    List<ProjectVersion> findByProjectIdAndSpringBootVersionId(
        @Param("projectId") Long projectId,
        @Param("springBootVersionId") Long springBootVersionId
    );

    /**
     * Find all versions of a project with specific major and minor version numbers.
     * Used for compatibility mapping to match major.minor versions, ignoring patch.
     *
     * @param project the Spring project
     * @param majorVersion the major version number
     * @param minorVersion the minor version number
     * @return list of matching versions
     */
    List<ProjectVersion> findByProjectAndMajorVersionAndMinorVersion(
        SpringProject project,
        Integer majorVersion,
        Integer minorVersion
    );

    /**
     * Find all versions across all projects that are compatible with a specific Spring Boot version.
     * Uses the spring_boot_compatibility table to determine compatibility.
     *
     * @param springBootVersionId the Spring Boot version ID
     * @return list of all compatible versions across all projects
     */
    @Query("""
        SELECT DISTINCT v
        FROM ProjectVersion v
        JOIN FETCH v.project
        JOIN SpringBootCompatibility sbc ON sbc.compatibleProjectVersion = v
        WHERE sbc.springBootVersion.id = :springBootVersionId
        ORDER BY v.project.name ASC, v.majorVersion DESC, v.minorVersion DESC, v.patchVersion DESC
        """)
    List<ProjectVersion> findAllBySpringBootVersionId(@Param("springBootVersionId") Long springBootVersionId);

    /**
     * Count distinct projects that have versions released in the last N days
     *
     * @param days number of days to look back
     * @return count of distinct projects with recent releases
     */
    @Query(value = """
        SELECT COUNT(DISTINCT project_id)
        FROM project_versions
        WHERE release_date >= CURRENT_DATE - :days
        """, nativeQuery = true)
    long countDistinctProjectsWithRecentReleases(@Param("days") int days);

    /**
     * Find top N projects with most recent release dates
     *
     * @param pageable pagination information (use PageRequest.of(0, limit))
     * @return list of project versions with most recent releases
     */
    @Query("""
        SELECT v
        FROM ProjectVersion v
        JOIN FETCH v.project
        WHERE v.releaseDate IS NOT NULL
        ORDER BY v.releaseDate DESC
        """)
    List<ProjectVersion> findTopByReleaseDateDesc(Pageable pageable);
}

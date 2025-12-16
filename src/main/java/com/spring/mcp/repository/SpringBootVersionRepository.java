package com.spring.mcp.repository;

import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.enums.VersionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SpringBootVersion entities
 */
@Repository
public interface SpringBootVersionRepository extends JpaRepository<SpringBootVersion, Long> {

    /**
     * Find a Spring Boot version by its version string
     */
    Optional<SpringBootVersion> findByVersion(String version);

    /**
     * Find the current Spring Boot version
     */
    Optional<SpringBootVersion> findByIsCurrentTrue();

    /**
     * Find all versions with a specific state
     */
    List<SpringBootVersion> findByState(VersionState state);

    /**
     * Find all GA (General Availability) versions
     */
    List<SpringBootVersion> findByStateOrderByMajorVersionDescMinorVersionDescPatchVersionDesc(VersionState state);

    /**
     * Find all Spring Boot versions ordered by version (descending)
     */
    @Query("SELECT v FROM SpringBootVersion v ORDER BY v.majorVersion DESC, v.minorVersion DESC, v.patchVersion DESC")
    List<SpringBootVersion> findAllOrderByVersionDesc();

    /**
     * Find all Spring Boot versions ordered by version (ascending)
     */
    @Query("SELECT v FROM SpringBootVersion v ORDER BY v.majorVersion ASC, v.minorVersion ASC, v.patchVersion ASC")
    List<SpringBootVersion> findAllOrderByVersionAsc();

    /**
     * Check if a version exists
     */
    boolean existsByVersion(String version);

    /**
     * Find versions by major and minor version
     */
    List<SpringBootVersion> findByMajorVersionAndMinorVersionOrderByPatchVersionDesc(Integer majorVersion, Integer minorVersion);

    /**
     * Find versions starting with a version prefix (e.g., "3.5" matches "3.5.0", "3.5.1", etc.)
     * Useful for finding compatible versions when only major.minor is specified.
     */
    List<SpringBootVersion> findByVersionStartingWith(String versionPrefix);
}

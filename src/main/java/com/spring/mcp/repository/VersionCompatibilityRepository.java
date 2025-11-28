package com.spring.mcp.repository;

import com.spring.mcp.model.entity.VersionCompatibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for VersionCompatibility entities.
 */
@Repository
public interface VersionCompatibilityRepository extends JpaRepository<VersionCompatibility, Long> {

    /**
     * Find all compatibility entries for a Spring Boot version
     */
    List<VersionCompatibility> findBySpringBootVersion(String springBootVersion);

    /**
     * Find verified compatibility entries for a Spring Boot version
     */
    List<VersionCompatibility> findBySpringBootVersionAndVerifiedTrue(String springBootVersion);

    /**
     * Find specific dependency compatibility
     */
    Optional<VersionCompatibility> findBySpringBootVersionAndDependencyGroupAndDependencyArtifact(
        String springBootVersion,
        String dependencyGroup,
        String dependencyArtifact
    );

    /**
     * Find all versions of a specific dependency across Spring Boot versions
     */
    @Query("""
        SELECT vc FROM VersionCompatibility vc
        WHERE vc.dependencyGroup = :group
        AND vc.dependencyArtifact = :artifact
        ORDER BY vc.springBootVersion DESC
        """)
    List<VersionCompatibility> findDependencyVersionHistory(
        @Param("group") String group,
        @Param("artifact") String artifact
    );

    /**
     * Check if a dependency exists for a Spring Boot version
     */
    boolean existsBySpringBootVersionAndDependencyGroupAndDependencyArtifact(
        String springBootVersion,
        String dependencyGroup,
        String dependencyArtifact
    );
}

package com.spring.mcp.repository;

import com.spring.mcp.model.entity.DeprecationReplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DeprecationReplacement entities.
 */
@Repository
public interface DeprecationReplacementRepository extends JpaRepository<DeprecationReplacement, Long> {

    /**
     * Find replacement by deprecated class (entire class deprecated)
     */
    Optional<DeprecationReplacement> findByDeprecatedClassAndDeprecatedMethodIsNull(String deprecatedClass);

    /**
     * Find replacement by deprecated class and method
     */
    Optional<DeprecationReplacement> findByDeprecatedClassAndDeprecatedMethod(
        String deprecatedClass,
        String deprecatedMethod
    );

    /**
     * Find all deprecations for a project
     */
    List<DeprecationReplacement> findByProjectSlug(String projectSlug);

    /**
     * Find all deprecations removed in a specific version
     */
    @Query("SELECT dr FROM DeprecationReplacement dr WHERE dr.removedIn = :removedIn")
    List<DeprecationReplacement> findByRemovedInVersion(@Param("removedIn") String removedIn);

    /**
     * Find deprecations by partial class name match
     */
    @Query("""
        SELECT dr FROM DeprecationReplacement dr
        WHERE dr.deprecatedClass LIKE %:className%
        ORDER BY dr.deprecatedClass
        """)
    List<DeprecationReplacement> searchByDeprecatedClass(@Param("className") String className);

    /**
     * Find deprecations between versions
     */
    @Query("""
        SELECT dr FROM DeprecationReplacement dr
        WHERE dr.projectSlug = :project
        AND dr.removedIn = :version
        ORDER BY dr.deprecatedClass
        """)
    List<DeprecationReplacement> findRemovedInVersion(
        @Param("project") String project,
        @Param("version") String version
    );
}

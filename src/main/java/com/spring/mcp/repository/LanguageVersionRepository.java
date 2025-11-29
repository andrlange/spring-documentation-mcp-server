package com.spring.mcp.repository;

import com.spring.mcp.model.entity.LanguageVersion;
import com.spring.mcp.model.enums.LanguageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LanguageVersion entity.
 * Provides access to Java and Kotlin version information.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Repository
public interface LanguageVersionRepository extends JpaRepository<LanguageVersion, Long> {

    /**
     * Find all versions for a specific language, ordered by version (newest first)
     */
    List<LanguageVersion> findByLanguageOrderByMajorVersionDescMinorVersionDesc(LanguageType language);

    /**
     * Find a specific version by language and version string
     */
    Optional<LanguageVersion> findByLanguageAndVersion(LanguageType language, String version);

    /**
     * Find all LTS versions for a language
     */
    List<LanguageVersion> findByLanguageAndIsLtsTrueOrderByMajorVersionDesc(LanguageType language);

    /**
     * Find the current (latest) version for a language
     */
    Optional<LanguageVersion> findByLanguageAndIsCurrentTrue(LanguageType language);

    /**
     * Find versions by language, with support info eager loaded
     */
    @Query("SELECT v FROM LanguageVersion v WHERE v.language = :language ORDER BY v.majorVersion DESC, v.minorVersion DESC")
    List<LanguageVersion> findAllByLanguage(@Param("language") LanguageType language);

    /**
     * Count versions by language
     */
    long countByLanguage(LanguageType language);

    /**
     * Find versions greater than or equal to a minimum version
     */
    @Query("SELECT v FROM LanguageVersion v WHERE v.language = :language AND " +
           "(v.majorVersion > :majorVersion OR (v.majorVersion = :majorVersion AND v.minorVersion >= :minorVersion)) " +
           "ORDER BY v.majorVersion DESC, v.minorVersion DESC")
    List<LanguageVersion> findByLanguageAndVersionGreaterThanEqual(
            @Param("language") LanguageType language,
            @Param("majorVersion") int majorVersion,
            @Param("minorVersion") int minorVersion);

    /**
     * Check if a version exists
     */
    boolean existsByLanguageAndVersion(LanguageType language, String version);

    /**
     * Find all versions with eager fetch of features
     */
    @Query("SELECT DISTINCT v FROM LanguageVersion v LEFT JOIN FETCH v.features WHERE v.language = :language ORDER BY v.majorVersion DESC, v.minorVersion DESC")
    List<LanguageVersion> findByLanguageWithFeatures(@Param("language") LanguageType language);
}

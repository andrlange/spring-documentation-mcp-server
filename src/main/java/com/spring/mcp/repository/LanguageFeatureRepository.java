package com.spring.mcp.repository;

import com.spring.mcp.model.entity.LanguageFeature;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.LanguageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for LanguageFeature entity.
 * Provides access to language features, deprecations, and removals.
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-11-29
 */
@Repository
public interface LanguageFeatureRepository extends JpaRepository<LanguageFeature, Long> {

    /**
     * Find all features for a language version
     */
    List<LanguageFeature> findByLanguageVersionIdOrderByStatusAscFeatureNameAsc(Long languageVersionId);

    /**
     * Find features by status
     */
    List<LanguageFeature> findByStatus(FeatureStatus status);

    /**
     * Find features by category
     */
    List<LanguageFeature> findByCategory(String category);

    /**
     * Find features by language version ID and status
     */
    List<LanguageFeature> findByLanguageVersionIdAndStatus(Long languageVersionId, FeatureStatus status);

    /**
     * Search features by name (case-insensitive)
     */
    @Query("SELECT f FROM LanguageFeature f WHERE LOWER(f.featureName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<LanguageFeature> searchByFeatureName(@Param("searchTerm") String searchTerm);

    /**
     * Search features by name or description
     */
    @Query("SELECT f FROM LanguageFeature f WHERE " +
           "LOWER(f.featureName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(f.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<LanguageFeature> searchByFeatureNameOrDescription(@Param("searchTerm") String searchTerm);

    /**
     * Find features for a language (across all versions)
     */
    @Query("SELECT f FROM LanguageFeature f JOIN f.languageVersion v WHERE v.language = :language ORDER BY v.majorVersion DESC, v.minorVersion DESC, f.featureName")
    List<LanguageFeature> findByLanguage(@Param("language") LanguageType language);

    /**
     * Find features for a language with pagination
     */
    @Query("SELECT f FROM LanguageFeature f JOIN f.languageVersion v WHERE v.language = :language")
    Page<LanguageFeature> findByLanguage(@Param("language") LanguageType language, Pageable pageable);

    /**
     * Find features for a language and version string
     */
    @Query("SELECT f FROM LanguageFeature f JOIN f.languageVersion v WHERE v.language = :language AND v.version = :version ORDER BY f.status, f.featureName")
    List<LanguageFeature> findByLanguageAndVersion(@Param("language") LanguageType language, @Param("version") String version);

    /**
     * Find features by JEP number
     */
    List<LanguageFeature> findByJepNumber(String jepNumber);

    /**
     * Find features by KEP number
     */
    List<LanguageFeature> findByKepNumber(String kepNumber);

    /**
     * Count features by status
     */
    long countByStatus(FeatureStatus status);

    /**
     * Count features by language version
     */
    long countByLanguageVersionId(Long languageVersionId);

    /**
     * Get distinct categories
     */
    @Query("SELECT DISTINCT f.category FROM LanguageFeature f WHERE f.category IS NOT NULL ORDER BY f.category")
    List<String> findDistinctCategories();

    /**
     * Complex search with multiple filters.
     * Uses native query to avoid PostgreSQL type inference issues with null parameters.
     * Searches in feature_name, description, jep_number, and kep_number fields.
     */
    @Query(value = "SELECT lf.* FROM language_features lf " +
           "JOIN language_versions lv ON lv.id = lf.language_version_id WHERE " +
           "(CAST(:language AS VARCHAR) IS NULL OR lv.language = CAST(:language AS VARCHAR)) AND " +
           "(CAST(:version AS VARCHAR) IS NULL OR lv.version = CAST(:version AS VARCHAR)) AND " +
           "(CAST(:status AS VARCHAR) IS NULL OR lf.status = CAST(:status AS VARCHAR)) AND " +
           "(CAST(:category AS VARCHAR) IS NULL OR lf.category = CAST(:category AS VARCHAR)) AND " +
           "(CAST(:searchTerm AS VARCHAR) IS NULL OR " +
           "LOWER(lf.feature_name) LIKE LOWER('%' || CAST(:searchTerm AS VARCHAR) || '%') OR " +
           "LOWER(lf.description) LIKE LOWER('%' || CAST(:searchTerm AS VARCHAR) || '%') OR " +
           "LOWER(COALESCE(lf.jep_number, '')) LIKE LOWER('%' || CAST(:searchTerm AS VARCHAR) || '%') OR " +
           "LOWER(COALESCE(lf.kep_number, '')) LIKE LOWER('%' || CAST(:searchTerm AS VARCHAR) || '%')) " +
           "ORDER BY lv.major_version DESC, lv.minor_version DESC, lf.feature_name",
           nativeQuery = true)
    List<LanguageFeature> searchFeatures(
            @Param("language") String language,
            @Param("version") String version,
            @Param("status") String status,
            @Param("category") String category,
            @Param("searchTerm") String searchTerm);

    /**
     * Find deprecated or removed features for a language version range
     */
    @Query("SELECT f FROM LanguageFeature f JOIN f.languageVersion v WHERE " +
           "v.language = :language AND " +
           "(v.majorVersion > :fromMajor OR (v.majorVersion = :fromMajor AND v.minorVersion >= :fromMinor)) AND " +
           "(v.majorVersion < :toMajor OR (v.majorVersion = :toMajor AND v.minorVersion <= :toMinor)) AND " +
           "f.status IN ('DEPRECATED', 'REMOVED') " +
           "ORDER BY v.majorVersion, v.minorVersion, f.featureName")
    List<LanguageFeature> findDeprecationsAndRemovalsBetweenVersions(
            @Param("language") LanguageType language,
            @Param("fromMajor") int fromMajor,
            @Param("fromMinor") int fromMinor,
            @Param("toMajor") int toMajor,
            @Param("toMinor") int toMinor);
}

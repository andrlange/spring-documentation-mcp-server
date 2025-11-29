package com.spring.mcp.repository;

import com.spring.mcp.model.entity.LanguageCodePattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for LanguageCodePattern entity.
 * Provides access to code examples showing old vs new patterns.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Repository
public interface LanguageCodePatternRepository extends JpaRepository<LanguageCodePattern, Long> {

    /**
     * Find all patterns for a feature
     */
    List<LanguageCodePattern> findByFeatureId(Long featureId);

    /**
     * Find patterns by code language
     */
    List<LanguageCodePattern> findByPatternLanguage(String patternLanguage);

    /**
     * Search patterns by explanation
     */
    @Query("SELECT p FROM LanguageCodePattern p WHERE LOWER(p.explanation) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<LanguageCodePattern> searchByExplanation(@Param("searchTerm") String searchTerm);

    /**
     * Find patterns available for a minimum version
     */
    @Query("SELECT p FROM LanguageCodePattern p WHERE p.minVersion <= :version")
    List<LanguageCodePattern> findByMinVersionLessThanEqual(@Param("version") String version);

    /**
     * Find patterns for a language (via feature -> version relationship)
     */
    @Query("SELECT p FROM LanguageCodePattern p " +
           "JOIN p.feature f " +
           "JOIN f.languageVersion v " +
           "WHERE LOWER(p.patternLanguage) = LOWER(:language)")
    List<LanguageCodePattern> findByLanguage(@Param("language") String language);

    /**
     * Count patterns for a feature
     */
    long countByFeatureId(Long featureId);

    /**
     * Check if a feature has patterns
     */
    boolean existsByFeatureId(Long featureId);
}

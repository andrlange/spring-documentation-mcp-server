package com.spring.mcp.repository;

import com.spring.mcp.model.entity.SpringBootLanguageRequirement;
import com.spring.mcp.model.enums.LanguageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SpringBootLanguageRequirement entity.
 * Provides access to Spring Boot language version requirements.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Repository
public interface SpringBootLanguageRequirementRepository extends JpaRepository<SpringBootLanguageRequirement, Long> {

    /**
     * Find requirements for a Spring Boot version
     */
    List<SpringBootLanguageRequirement> findBySpringBootVersionId(Long springBootVersionId);

    /**
     * Find requirement for a specific Spring Boot version and language
     */
    Optional<SpringBootLanguageRequirement> findBySpringBootVersionIdAndLanguage(Long springBootVersionId, LanguageType language);

    /**
     * Find all requirements for a language
     */
    List<SpringBootLanguageRequirement> findByLanguage(LanguageType language);

    /**
     * Find requirements by Spring Boot version string
     */
    @Query("SELECT r FROM SpringBootLanguageRequirement r JOIN r.springBootVersion v WHERE v.version = :version")
    List<SpringBootLanguageRequirement> findBySpringBootVersion(@Param("version") String version);

    /**
     * Find Java requirement for a Spring Boot version
     */
    @Query("SELECT r FROM SpringBootLanguageRequirement r JOIN r.springBootVersion v WHERE v.version = :version AND r.language = 'JAVA'")
    Optional<SpringBootLanguageRequirement> findJavaRequirementBySpringBootVersion(@Param("version") String version);

    /**
     * Find Kotlin requirement for a Spring Boot version
     */
    @Query("SELECT r FROM SpringBootLanguageRequirement r JOIN r.springBootVersion v WHERE v.version = :version AND r.language = 'KOTLIN'")
    Optional<SpringBootLanguageRequirement> findKotlinRequirementBySpringBootVersion(@Param("version") String version);

    /**
     * Check if requirements exist for a Spring Boot version
     */
    boolean existsBySpringBootVersionId(Long springBootVersionId);
}

package com.spring.mcp.repository;

import com.spring.mcp.model.entity.KepSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for KepSpecification entity.
 * Provides access to cached KEP/KEEP specification content.
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-12-17
 */
@Repository
public interface KepSpecificationRepository extends JpaRepository<KepSpecification, Long> {

    /**
     * Find a KEP specification by its number
     */
    Optional<KepSpecification> findByKepNumber(String kepNumber);

    /**
     * Check if a KEP specification exists
     */
    boolean existsByKepNumber(String kepNumber);

    /**
     * Find KEP specifications by source type (KEEP or YOUTRACK)
     */
    List<KepSpecification> findBySourceType(String sourceType);

    /**
     * Find KEP specifications by status
     */
    List<KepSpecification> findByStatus(String status);

    /**
     * Find KEP specifications that have not been fetched yet
     */
    @Query("SELECT k FROM KepSpecification k WHERE k.fetchedAt IS NULL")
    List<KepSpecification> findNotFetched();

    /**
     * Find KEP specifications that have been fetched
     */
    @Query("SELECT k FROM KepSpecification k WHERE k.fetchedAt IS NOT NULL ORDER BY k.kepNumber")
    List<KepSpecification> findAllFetched();

    /**
     * Search KEP specifications by title or summary
     */
    @Query("SELECT k FROM KepSpecification k WHERE " +
           "LOWER(k.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(k.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<KepSpecification> searchByTitleOrSummary(@Param("searchTerm") String searchTerm);

    /**
     * Count fetched specifications
     */
    @Query("SELECT COUNT(k) FROM KepSpecification k WHERE k.fetchedAt IS NOT NULL")
    long countFetched();

    /**
     * Find all KEP numbers
     */
    @Query("SELECT k.kepNumber FROM KepSpecification k ORDER BY k.kepNumber")
    List<String> findAllKepNumbers();

    /**
     * Find specifications from KEEP repository
     */
    @Query("SELECT k FROM KepSpecification k WHERE k.sourceType = 'KEEP' ORDER BY k.kepNumber")
    List<KepSpecification> findFromKeep();

    /**
     * Find specifications from YouTrack
     */
    @Query("SELECT k FROM KepSpecification k WHERE k.sourceType = 'YOUTRACK' ORDER BY k.kepNumber")
    List<KepSpecification> findFromYouTrack();
}

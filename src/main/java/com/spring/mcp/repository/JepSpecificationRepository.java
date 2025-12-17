package com.spring.mcp.repository;

import com.spring.mcp.model.entity.JepSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for JepSpecification entity.
 * Provides access to cached JEP specification content.
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-12-17
 */
@Repository
public interface JepSpecificationRepository extends JpaRepository<JepSpecification, Long> {

    /**
     * Find a JEP specification by its number
     */
    Optional<JepSpecification> findByJepNumber(String jepNumber);

    /**
     * Check if a JEP specification exists
     */
    boolean existsByJepNumber(String jepNumber);

    /**
     * Find JEP specifications by status
     */
    List<JepSpecification> findByStatus(String status);

    /**
     * Find JEP specifications by target version
     */
    List<JepSpecification> findByTargetVersion(String targetVersion);

    /**
     * Find JEP specifications that have not been fetched yet
     */
    @Query("SELECT j FROM JepSpecification j WHERE j.fetchedAt IS NULL")
    List<JepSpecification> findNotFetched();

    /**
     * Find JEP specifications that have been fetched
     */
    @Query("SELECT j FROM JepSpecification j WHERE j.fetchedAt IS NOT NULL ORDER BY j.jepNumber")
    List<JepSpecification> findAllFetched();

    /**
     * Search JEP specifications by title or summary
     */
    @Query("SELECT j FROM JepSpecification j WHERE " +
           "LOWER(j.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(j.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<JepSpecification> searchByTitleOrSummary(@Param("searchTerm") String searchTerm);

    /**
     * Count fetched specifications
     */
    @Query("SELECT COUNT(j) FROM JepSpecification j WHERE j.fetchedAt IS NOT NULL")
    long countFetched();

    /**
     * Find all JEP numbers
     */
    @Query("SELECT j.jepNumber FROM JepSpecification j ORDER BY j.jepNumber")
    List<String> findAllJepNumbers();
}

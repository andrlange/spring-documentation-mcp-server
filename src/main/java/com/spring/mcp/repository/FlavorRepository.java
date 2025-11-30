package com.spring.mcp.repository;

import com.spring.mcp.model.entity.Flavor;
import com.spring.mcp.model.enums.FlavorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Flavor entity operations.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
@Repository
public interface FlavorRepository extends JpaRepository<Flavor, Long> {

    Optional<Flavor> findByUniqueName(String uniqueName);

    Optional<Flavor> findByUniqueNameAndIsActiveTrue(String uniqueName);

    List<Flavor> findByCategory(FlavorCategory category);

    List<Flavor> findByCategoryAndIsActiveTrue(FlavorCategory category);

    List<Flavor> findByIsActiveTrue();

    @Query("SELECT f FROM Flavor f WHERE f.isActive = true ORDER BY f.category, f.displayName")
    List<Flavor> findAllActiveOrdered();

    @Query("SELECT f FROM Flavor f ORDER BY f.category, f.displayName")
    List<Flavor> findAllOrdered();

    /**
     * Full-text search using PostgreSQL tsvector.
     */
    @Query(value = """
        SELECT * FROM flavors f
        WHERE f.is_active = true
        AND f.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(f.search_vector, plainto_tsquery('english', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Flavor> searchByQuery(@Param("query") String query, @Param("limit") int limit);

    /**
     * Full-text search filtered by category.
     */
    @Query(value = """
        SELECT * FROM flavors f
        WHERE f.is_active = true
        AND f.category = :category
        AND f.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(f.search_vector, plainto_tsquery('english', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Flavor> searchByQueryAndCategory(
        @Param("query") String query,
        @Param("category") String category,
        @Param("limit") int limit
    );

    /**
     * Find flavors containing any of the specified tags.
     */
    @Query(value = """
        SELECT * FROM flavors f
        WHERE f.is_active = true
        AND f.tags && CAST(:tags AS TEXT[])
        ORDER BY f.updated_at DESC
        """, nativeQuery = true)
    List<Flavor> findByTagsContaining(@Param("tags") String[] tags);

    /**
     * Find architecture patterns by technology slugs with relevance ranking.
     */
    @Query(value = """
        SELECT * FROM flavors f
        WHERE f.is_active = true
        AND f.category = 'ARCHITECTURE'
        AND f.tags && CAST(:slugs AS TEXT[])
        ORDER BY array_length(
            array(SELECT unnest(f.tags) INTERSECT SELECT unnest(CAST(:slugs AS TEXT[]))), 1
        ) DESC NULLS LAST
        """, nativeQuery = true)
    List<Flavor> findArchitectureByTechnologySlugs(@Param("slugs") String[] slugs);

    /**
     * Find compliance rules by rule names in metadata.
     */
    @Query(value = """
        SELECT * FROM flavors f
        WHERE f.is_active = true
        AND f.category = 'COMPLIANCE'
        AND jsonb_exists_any(f.metadata -> 'ruleNames', CAST(:rules AS TEXT[]))
        ORDER BY f.display_name
        """, nativeQuery = true)
    List<Flavor> findComplianceByRules(@Param("rules") String[] rules);

    /**
     * Find agent configurations by use case.
     */
    @Query(value = """
        SELECT * FROM flavors f
        WHERE f.is_active = true
        AND f.category = 'AGENTS'
        AND jsonb_exists(f.metadata -> 'useCases', :useCase)
        LIMIT 1
        """, nativeQuery = true)
    Optional<Flavor> findAgentByUseCase(@Param("useCase") String useCase);

    /**
     * Find initialization templates by use case.
     */
    @Query(value = """
        SELECT * FROM flavors f
        WHERE f.is_active = true
        AND f.category = 'INITIALIZATION'
        AND jsonb_exists(f.metadata -> 'useCases', :useCase)
        LIMIT 1
        """, nativeQuery = true)
    Optional<Flavor> findInitializationByUseCase(@Param("useCase") String useCase);

    /**
     * Count flavors by category (for statistics).
     */
    @Query(value = """
        SELECT f.category, COUNT(*) as count
        FROM flavors f
        WHERE f.is_active = true
        GROUP BY f.category
        """, nativeQuery = true)
    List<Object[]> countByCategory();

    boolean existsByUniqueName(String uniqueName);

    long countByIsActiveTrue();

    long countByIsActiveFalse();

    long countByCategoryAndIsActiveTrue(FlavorCategory category);
}

package com.spring.mcp.repository;

import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.model.entity.MigrationTransformation.TransformationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for MigrationTransformation entities.
 */
@Repository
public interface MigrationTransformationRepository extends JpaRepository<MigrationTransformation, Long> {

    List<MigrationTransformation> findByRecipeId(Long recipeId);

    List<MigrationTransformation> findByRecipeIdAndBreakingChangeTrue(Long recipeId);

    @Query("""
        SELECT t FROM MigrationTransformation t
        WHERE t.recipe.id = :recipeId
        AND t.transformationType = :type
        ORDER BY t.priority DESC
        """)
    List<MigrationTransformation> findByRecipeAndType(
        @Param("recipeId") Long recipeId,
        @Param("type") TransformationType type
    );

    @Query("""
        SELECT t FROM MigrationTransformation t
        WHERE t.recipe.id = :recipeId
        AND t.category = :category
        ORDER BY t.priority DESC
        """)
    List<MigrationTransformation> findByRecipeAndCategory(
        @Param("recipeId") Long recipeId,
        @Param("category") String category
    );

    /**
     * Search within a specific recipe using PostgreSQL full-text search
     */
    @Query(value = """
        SELECT * FROM migration_transformations
        WHERE recipe_id = :recipeId
        AND search_vector @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(search_vector, plainto_tsquery('english', :searchTerm)) DESC
        """, nativeQuery = true)
    List<MigrationTransformation> searchInRecipe(
        @Param("recipeId") Long recipeId,
        @Param("searchTerm") String searchTerm
    );

    /**
     * Search across all transformations for a project
     */
    @Query(value = """
        SELECT t.* FROM migration_transformations t
        JOIN migration_recipes r ON t.recipe_id = r.id
        WHERE r.from_project = :project
        AND r.is_active = true
        AND t.search_vector @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(t.search_vector, plainto_tsquery('english', :searchTerm)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<MigrationTransformation> searchAcrossProject(
        @Param("project") String project,
        @Param("searchTerm") String searchTerm,
        @Param("limit") int limit
    );

    /**
     * Get transformations by type across all recipes for a project and version
     */
    @Query("""
        SELECT t FROM MigrationTransformation t
        WHERE t.recipe.fromProject = :project
        AND t.recipe.toVersion = :version
        AND t.recipe.isActive = true
        AND t.transformationType = :type
        ORDER BY t.priority DESC
        """)
    List<MigrationTransformation> findByProjectVersionAndType(
        @Param("project") String project,
        @Param("version") String version,
        @Param("type") TransformationType type
    );

    /**
     * Count breaking changes
     */
    long countByBreakingChangeTrue();

    /**
     * Count transformations for a specific recipe
     */
    long countByRecipeId(Long recipeId);
}

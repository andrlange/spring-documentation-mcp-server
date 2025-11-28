package com.spring.mcp.repository;

import com.spring.mcp.model.entity.MigrationRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MigrationRecipe entities.
 */
@Repository
public interface MigrationRecipeRepository extends JpaRepository<MigrationRecipe, Long> {

    Optional<MigrationRecipe> findByName(String name);

    @Query("""
        SELECT r FROM MigrationRecipe r
        WHERE r.fromProject = :project
        AND r.toVersion = :toVersion
        AND r.isActive = true
        """)
    List<MigrationRecipe> findByProjectAndTargetVersion(
        @Param("project") String project,
        @Param("toVersion") String toVersion
    );

    @Query("""
        SELECT r FROM MigrationRecipe r
        WHERE r.fromProject = :project
        AND r.isActive = true
        ORDER BY r.toVersion DESC
        """)
    List<MigrationRecipe> findAllByProject(@Param("project") String project);

    @Query("""
        SELECT DISTINCT r.toVersion FROM MigrationRecipe r
        WHERE r.fromProject = :project
        AND r.isActive = true
        ORDER BY r.toVersion DESC
        """)
    List<String> findAvailableTargetVersions(@Param("project") String project);

    /**
     * Find recipes by linked Spring project
     */
    @Query("""
        SELECT r FROM MigrationRecipe r
        JOIN RecipeProjectMapping rpm ON rpm.recipe.id = r.id
        WHERE rpm.project.id = :projectId
        AND r.isActive = true
        ORDER BY r.toVersion DESC
        """)
    List<MigrationRecipe> findByProjectId(@Param("projectId") Long projectId);

    /**
     * Find recipes that migrate FROM a specific version
     */
    @Query("""
        SELECT r FROM MigrationRecipe r
        JOIN RecipeVersionMapping rvm ON rvm.recipe.id = r.id
        WHERE rvm.version.id = :versionId
        AND rvm.mappingType = com.spring.mcp.model.entity.RecipeVersionMapping.MappingType.SOURCE
        AND r.isActive = true
        """)
    List<MigrationRecipe> findBySourceVersion(@Param("versionId") Long versionId);

    /**
     * Find recipes that migrate TO a specific version
     */
    @Query("""
        SELECT r FROM MigrationRecipe r
        JOIN RecipeVersionMapping rvm ON rvm.recipe.id = r.id
        WHERE rvm.version.id = :versionId
        AND rvm.mappingType = com.spring.mcp.model.entity.RecipeVersionMapping.MappingType.TARGET
        AND r.isActive = true
        """)
    List<MigrationRecipe> findByTargetVersion(@Param("versionId") Long versionId);

    /**
     * Get distinct projects that have recipes
     */
    @Query("SELECT DISTINCT r.fromProject FROM MigrationRecipe r WHERE r.isActive = true ORDER BY r.fromProject")
    List<String> findDistinctProjects();

    /**
     * Get distinct target versions
     */
    @Query("SELECT DISTINCT r.toVersion FROM MigrationRecipe r WHERE r.isActive = true ORDER BY r.toVersion DESC")
    List<String> findDistinctTargetVersions();

    /**
     * Find by from project with pagination
     */
    Page<MigrationRecipe> findByFromProjectAndIsActiveTrue(String fromProject, Pageable pageable);

    /**
     * Find by to version with pagination
     */
    Page<MigrationRecipe> findByToVersionAndIsActiveTrue(String toVersion, Pageable pageable);

    /**
     * Full-text search across recipes using JPQL
     */
    @Query("""
        SELECT r FROM MigrationRecipe r
        WHERE r.isActive = true
        AND (
            LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(r.displayName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        ORDER BY r.displayName, r.toVersion DESC
        """)
    Page<MigrationRecipe> searchByText(@Param("search") String search, Pageable pageable);

    /**
     * Count active recipes
     */
    long countByIsActiveTrue();
}

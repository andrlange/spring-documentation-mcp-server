package com.spring.mcp.repository;

import com.spring.mcp.model.entity.RecipeProjectMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RecipeProjectMapping entities.
 */
@Repository
public interface RecipeProjectMappingRepository extends JpaRepository<RecipeProjectMapping, Long> {

    List<RecipeProjectMapping> findByRecipeId(Long recipeId);

    List<RecipeProjectMapping> findByProjectId(Long projectId);

    Optional<RecipeProjectMapping> findByRecipeIdAndProjectId(Long recipeId, Long projectId);

    boolean existsByRecipeIdAndProjectId(Long recipeId, Long projectId);

    void deleteByRecipeId(Long recipeId);
}

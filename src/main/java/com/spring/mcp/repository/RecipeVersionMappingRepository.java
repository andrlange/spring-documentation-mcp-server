package com.spring.mcp.repository;

import com.spring.mcp.model.entity.RecipeVersionMapping;
import com.spring.mcp.model.entity.RecipeVersionMapping.MappingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RecipeVersionMapping entities.
 */
@Repository
public interface RecipeVersionMappingRepository extends JpaRepository<RecipeVersionMapping, Long> {

    List<RecipeVersionMapping> findByRecipeId(Long recipeId);

    List<RecipeVersionMapping> findByVersionId(Long versionId);

    List<RecipeVersionMapping> findByRecipeIdAndMappingType(Long recipeId, MappingType mappingType);

    Optional<RecipeVersionMapping> findByRecipeIdAndVersionIdAndMappingType(
        Long recipeId,
        Long versionId,
        MappingType mappingType
    );

    boolean existsByRecipeIdAndVersionIdAndMappingType(
        Long recipeId,
        Long versionId,
        MappingType mappingType
    );

    void deleteByRecipeId(Long recipeId);
}

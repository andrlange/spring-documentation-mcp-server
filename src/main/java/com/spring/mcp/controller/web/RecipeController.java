package com.spring.mcp.controller.web;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.model.entity.MigrationRecipe;
import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.repository.MigrationRecipeRepository;
import com.spring.mcp.repository.MigrationTransformationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for managing and viewing OpenRewrite migration recipes.
 * Only active when mcp.features.openrewrite.enabled=true
 */
@Controller
@RequestMapping("/recipes")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "mcp.features.openrewrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecipeController {

    private final MigrationRecipeRepository recipeRepository;
    private final MigrationTransformationRepository transformationRepository;
    private final OpenRewriteFeatureConfig featureConfig;

    /**
     * List all recipes with pagination and filtering
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listRecipes(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        log.debug("Listing recipes - project={}, search={}, page={}, size={}", project, search, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "toVersion"));

        Page<MigrationRecipe> recipes;
        if (search != null && !search.isBlank()) {
            recipes = recipeRepository.searchByText(search, pageable);
        } else if (project != null && !project.isBlank()) {
            recipes = recipeRepository.findByFromProjectAndIsActiveTrue(project, pageable);
        } else {
            recipes = recipeRepository.findAll(pageable);
        }

        // Get distinct projects for filter dropdown
        List<String> projects = recipeRepository.findDistinctProjects();

        // Statistics
        long totalRecipes = recipeRepository.countByIsActiveTrue();
        long totalTransformations = transformationRepository.count();
        long breakingChanges = transformationRepository.countByBreakingChangeTrue();

        model.addAttribute("recipes", recipes);
        model.addAttribute("projects", projects);
        model.addAttribute("selectedProject", project);
        model.addAttribute("searchQuery", search);
        model.addAttribute("totalRecipes", totalRecipes);
        model.addAttribute("totalTransformations", totalTransformations);
        model.addAttribute("breakingChanges", breakingChanges);
        model.addAttribute("activePage", "recipes");

        return "recipes/list";
    }

    /**
     * View a single recipe with all its transformations
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewRecipe(@PathVariable Long id, Model model) {
        log.debug("Viewing recipe with id={}", id);

        MigrationRecipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + id));

        List<MigrationTransformation> transformations = transformationRepository.findByRecipeId(id);

        // Group transformations by type
        Map<String, List<MigrationTransformation>> byType = transformations.stream()
                .collect(Collectors.groupingBy(t -> t.getTransformationType().name()));

        // Count breaking changes
        long breakingCount = transformations.stream()
                .filter(t -> Boolean.TRUE.equals(t.getBreakingChange()))
                .count();

        model.addAttribute("recipe", recipe);
        model.addAttribute("transformations", transformations);
        model.addAttribute("transformationsByType", byType);
        model.addAttribute("breakingCount", breakingCount);
        model.addAttribute("activePage", "recipes");

        return "recipes/view";
    }

    /**
     * Search transformations within a recipe
     */
    @GetMapping("/{id}/search")
    @PreAuthorize("isAuthenticated()")
    public String searchInRecipe(
            @PathVariable Long id,
            @RequestParam String query,
            Model model
    ) {
        log.debug("Searching in recipe {} for: {}", id, query);

        MigrationRecipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + id));

        List<MigrationTransformation> results = transformationRepository.searchInRecipe(id, query);

        model.addAttribute("recipe", recipe);
        model.addAttribute("searchResults", results);
        model.addAttribute("searchQuery", query);
        model.addAttribute("activePage", "recipes");

        return "recipes/search-results";
    }
}

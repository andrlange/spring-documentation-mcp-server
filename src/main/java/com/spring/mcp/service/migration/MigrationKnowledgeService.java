package com.spring.mcp.service.migration;

import com.spring.mcp.model.dto.mcp.*;
import com.spring.mcp.model.entity.DeprecationReplacement;
import com.spring.mcp.model.entity.MigrationRecipe;
import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.model.entity.MigrationTransformation.TransformationType;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootCompatibility;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.VersionCompatibility;
import com.spring.mcp.repository.DeprecationReplacementRepository;
import com.spring.mcp.repository.MigrationRecipeRepository;
import com.spring.mcp.repository.MigrationTransformationRepository;
import com.spring.mcp.repository.SpringBootCompatibilityRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.VersionCompatibilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying migration knowledge from OpenRewrite recipes.
 * Provides methods for MCP tools to access migration information.
 */
@Service
@Transactional(readOnly = true)
public class MigrationKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(MigrationKnowledgeService.class);

    private final MigrationRecipeRepository recipeRepository;
    private final MigrationTransformationRepository transformationRepository;
    private final DeprecationReplacementRepository deprecationRepository;
    private final VersionCompatibilityRepository compatibilityRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final SpringBootCompatibilityRepository springBootCompatibilityRepository;

    public MigrationKnowledgeService(
            MigrationRecipeRepository recipeRepository,
            MigrationTransformationRepository transformationRepository,
            DeprecationReplacementRepository deprecationRepository,
            VersionCompatibilityRepository compatibilityRepository,
            SpringBootVersionRepository springBootVersionRepository,
            SpringBootCompatibilityRepository springBootCompatibilityRepository) {
        this.recipeRepository = recipeRepository;
        this.transformationRepository = transformationRepository;
        this.deprecationRepository = deprecationRepository;
        this.compatibilityRepository = compatibilityRepository;
        this.springBootVersionRepository = springBootVersionRepository;
        this.springBootCompatibilityRepository = springBootCompatibilityRepository;
    }

    /**
     * Get complete migration guide from one version to another
     */
    public MigrationGuideDto getMigrationGuide(String project, String fromVersion, String toVersion) {
        log.info("Getting migration guide: {} {} -> {}", project, fromVersion, toVersion);

        List<MigrationRecipe> recipes = recipeRepository.findByProjectAndTargetVersion(project, toVersion);

        if (recipes.isEmpty()) {
            log.debug("No recipes found for {} -> {}", project, toVersion);
            return MigrationGuideDto.empty(project, fromVersion, toVersion);
        }

        // Aggregate all transformations
        List<MigrationTransformation> allTransformations = recipes.stream()
                .flatMap(r -> transformationRepository.findByRecipeId(r.getId()).stream())
                .sorted(Comparator.comparingInt(t -> -t.getPriority()))
                .toList();

        // Group by type
        Map<TransformationType, List<MigrationTransformation>> byType = allTransformations.stream()
                .collect(Collectors.groupingBy(MigrationTransformation::getTransformationType));

        // Get source URL and license from first recipe
        MigrationRecipe primaryRecipe = recipes.get(0);

        return MigrationGuideDto.builder()
                .project(project)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .totalChanges(allTransformations.size())
                .breakingChanges(countBreakingChanges(allTransformations))
                .importChanges(toTransformationDtos(byType.get(TransformationType.IMPORT)))
                .dependencyChanges(toTransformationDtos(byType.get(TransformationType.DEPENDENCY)))
                .propertyChanges(toTransformationDtos(byType.get(TransformationType.PROPERTY)))
                .codeChanges(toTransformationDtos(byType.get(TransformationType.CODE)))
                .buildChanges(toTransformationDtos(byType.get(TransformationType.BUILD)))
                .templateChanges(toTransformationDtos(byType.get(TransformationType.TEMPLATE)))
                .annotationChanges(toTransformationDtos(byType.get(TransformationType.ANNOTATION)))
                .configChanges(toTransformationDtos(byType.get(TransformationType.CONFIG)))
                .sourceUrl(primaryRecipe.getSourceUrl())
                .license(primaryRecipe.getLicense())
                .build();
    }

    /**
     * Get only breaking changes for a version
     */
    public List<BreakingChangeDto> getBreakingChanges(String project, String version) {
        log.info("Getting breaking changes: {} {}", project, version);

        List<MigrationRecipe> recipes = recipeRepository.findByProjectAndTargetVersion(project, version);

        return recipes.stream()
                .flatMap(r -> transformationRepository.findByRecipeIdAndBreakingChangeTrue(r.getId()).stream())
                .map(BreakingChangeDto::from)
                .sorted(Comparator.comparing(BreakingChangeDto::severity).reversed())
                .toList();
    }

    /**
     * Search for specific transformation
     */
    public List<TransformationDto> searchTransformations(String project, String searchTerm, int limit) {
        log.info("Searching transformations: {} query='{}' limit={}", project, searchTerm, limit);

        List<MigrationTransformation> results = transformationRepository.searchAcrossProject(project, searchTerm, limit);
        return results.stream().map(TransformationDto::from).toList();
    }

    /**
     * Get available migration paths for a project
     */
    public List<String> getAvailableMigrationTargets(String project) {
        return recipeRepository.findAvailableTargetVersions(project);
    }

    /**
     * Get transformations by type for a specific version
     */
    public List<TransformationDto> getTransformationsByType(String project, String version, String type) {
        log.info("Getting transformations by type: {} {} {}", project, version, type);

        try {
            TransformationType transformationType = TransformationType.valueOf(type.toUpperCase());
            List<MigrationTransformation> transformations = transformationRepository
                    .findByProjectVersionAndType(project, version, transformationType);
            return transformations.stream().map(TransformationDto::from).toList();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transformation type: {}", type);
            return List.of();
        }
    }

    /**
     * Find replacement for deprecated class/method
     */
    public DeprecationReplacementDto findReplacement(String className, String methodName) {
        log.info("Finding replacement for: {}#{}", className, methodName);

        Optional<DeprecationReplacement> replacement;
        if (methodName == null || methodName.isBlank()) {
            replacement = deprecationRepository.findByDeprecatedClassAndDeprecatedMethodIsNull(className);
        } else {
            replacement = deprecationRepository.findByDeprecatedClassAndDeprecatedMethod(className, methodName);
        }

        return replacement.map(DeprecationReplacementDto::from)
                .orElse(DeprecationReplacementDto.notFound(className, methodName));
    }

    /**
     * Check compatibility of dependencies with a Spring Boot version.
     * First checks the migration-specific version_compatibility table,
     * then falls back to the spring_boot_compatibility table which contains
     * data synced from spring.io.
     */
    public CompatibilityReportDto checkCompatibility(String springBootVersion, List<String> dependencies) {
        log.info("Checking compatibility for Spring Boot {} with {} dependencies", springBootVersion, dependencies.size());

        List<CompatibilityReportDto.DependencyCompatibility> compatibilities = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean allCompatible = true;

        // Find the Spring Boot version entity for spring_boot_compatibility lookup
        List<SpringBootVersion> bootVersions = springBootVersionRepository
                .findByVersionStartingWith(springBootVersion);

        // Get Spring Boot compatibility data if available
        List<SpringBootCompatibility> springBootCompatibilities = new ArrayList<>();
        if (!bootVersions.isEmpty()) {
            SpringBootVersion bootVersion = bootVersions.get(0);
            springBootCompatibilities = springBootCompatibilityRepository
                    .findAllBySpringBootVersionIdWithProjectDetails(bootVersion.getId());
            log.debug("Found {} spring.io compatibility mappings for Spring Boot {}",
                    springBootCompatibilities.size(), springBootVersion);
        }

        for (String dependency : dependencies) {
            // First, try to find in migration-specific version_compatibility table
            List<VersionCompatibility> migrationMatches = compatibilityRepository
                    .findBySpringBootVersion(springBootVersion)
                    .stream()
                    .filter(vc -> vc.getDependencyArtifact().contains(dependency) ||
                                  vc.getDependencyGroup().contains(dependency))
                    .toList();

            if (!migrationMatches.isEmpty()) {
                // Found in migration knowledge base
                VersionCompatibility vc = migrationMatches.get(0);
                compatibilities.add(new CompatibilityReportDto.DependencyCompatibility(
                        dependency,
                        vc.getCompatibleVersion(),
                        Boolean.TRUE.equals(vc.getVerified()),
                        vc.getNotes()
                ));
                log.debug("Found {} in migration knowledge: version {}", dependency, vc.getCompatibleVersion());
            } else {
                // Fall back to spring_boot_compatibility table (spring.io data)
                String lowerDep = dependency.toLowerCase();
                Optional<SpringBootCompatibility> springIoMatch = springBootCompatibilities.stream()
                        .filter(sbc -> {
                            ProjectVersion pv = sbc.getCompatibleProjectVersion();
                            String projectSlug = pv.getProject().getSlug().toLowerCase();
                            String projectName = pv.getProject().getName().toLowerCase();
                            // Match by project slug or name containing the dependency search term
                            return projectSlug.contains(lowerDep) ||
                                   projectName.contains(lowerDep) ||
                                   lowerDep.contains(projectSlug.replace("spring-", ""));
                        })
                        .findFirst();

                if (springIoMatch.isPresent()) {
                    ProjectVersion pv = springIoMatch.get().getCompatibleProjectVersion();
                    String compatibleVersion = pv.getVersion();
                    String projectName = pv.getProject().getName();

                    compatibilities.add(new CompatibilityReportDto.DependencyCompatibility(
                            projectName,
                            compatibleVersion,
                            true, // spring.io data is considered verified
                            "Compatible with Spring Boot " + springBootVersion +
                            " (source: spring.io)"
                    ));
                    log.debug("Found {} in spring.io compatibility: {} version {}",
                            dependency, projectName, compatibleVersion);
                } else {
                    warnings.add("No compatibility information found for: " + dependency);
                    compatibilities.add(new CompatibilityReportDto.DependencyCompatibility(
                            dependency, "unknown", false, "No compatibility data available"
                    ));
                    allCompatible = false;
                    log.debug("No compatibility data found for {}", dependency);
                }
            }
        }

        return CompatibilityReportDto.builder()
                .springBootVersion(springBootVersion)
                .dependencies(compatibilities)
                .allCompatible(allCompatible)
                .warnings(warnings)
                .build();
    }

    private long countBreakingChanges(List<MigrationTransformation> transformations) {
        return transformations.stream()
                .filter(t -> Boolean.TRUE.equals(t.getBreakingChange()))
                .count();
    }

    private List<TransformationDto> toTransformationDtos(List<MigrationTransformation> transformations) {
        if (transformations == null) {
            return List.of();
        }
        return transformations.stream().map(TransformationDto::from).toList();
    }
}

package com.spring.mcp.service.sync;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.model.entity.MigrationRecipe;
import com.spring.mcp.model.entity.MigrationTransformation;
import com.spring.mcp.model.entity.MigrationTransformation.Severity;
import com.spring.mcp.model.entity.MigrationTransformation.TransformationType;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.event.SyncProgressEvent;
import com.spring.mcp.repository.MigrationRecipeRepository;
import com.spring.mcp.repository.MigrationTransformationRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for syncing OpenRewrite migration recipes.
 * Handles fetching and updating migration knowledge from various sources.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.openrewrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecipeSyncService {

    private final MigrationRecipeRepository recipeRepository;
    private final MigrationTransformationRepository transformationRepository;
    private final SpringProjectRepository springProjectRepository;
    private final OpenRewriteFeatureConfig featureConfig;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Perform a full sync of recipe data.
     * Dynamically generates migration recipes based on Spring projects in the database.
     */
    @Transactional
    public void syncRecipes() {
        if (!featureConfig.isEnabled()) {
            log.info("OpenRewrite feature is disabled, skipping recipe sync");
            return;
        }

        log.info("Starting OpenRewrite recipe sync - generating recipes from Spring projects...");
        publishProgress("Recipe Sync", "Starting dynamic recipe generation", 0);

        try {
            // Get all active Spring projects from the database
            List<SpringProject> projects = springProjectRepository.findByActiveTrue();

            if (projects.isEmpty()) {
                log.warn("No Spring projects found in database. Run project sync first.");
                publishProgress("Recipe Sync", "No projects found - run project sync first", 100);
                return;
            }

            log.info("Found {} active Spring projects to process", projects.size());
            publishProgress("Recipe Sync", String.format("Processing %d Spring projects", projects.size()), 10);

            int recipesCreated = 0;
            int recipesUpdated = 0;
            int transformationsCreated = 0;
            int projectsProcessed = 0;

            for (SpringProject project : projects) {
                try {
                    RecipeGenerationResult result = generateRecipesForProject(project);
                    recipesCreated += result.recipesCreated;
                    recipesUpdated += result.recipesUpdated;
                    transformationsCreated += result.transformationsCreated;
                    projectsProcessed++;

                    int progressPercent = 10 + (int) ((projectsProcessed * 80.0) / projects.size());
                    publishProgress("Recipe Sync",
                        String.format("Processed %s (%d/%d)", project.getSlug(), projectsProcessed, projects.size()),
                        progressPercent);
                } catch (Exception e) {
                    log.error("Error generating recipes for project: {}", project.getSlug(), e);
                }
            }

            // Final count
            long totalRecipes = recipeRepository.countByIsActiveTrue();
            long totalTransformations = transformationRepository.count();

            log.info("Recipe sync complete: {} recipes created, {} updated, {} transformations created. Total: {} recipes, {} transformations",
                recipesCreated, recipesUpdated, transformationsCreated, totalRecipes, totalTransformations);

            publishProgress("Recipe Sync",
                String.format("Sync complete: %d recipes, %d transformations", totalRecipes, totalTransformations),
                100);
        } catch (Exception e) {
            log.error("Error during recipe sync", e);
            publishProgress("Recipe Sync", "Error: " + e.getMessage(), -1);
            throw new RuntimeException("Recipe sync failed", e);
        }
    }

    /**
     * Generate migration recipes for a specific Spring project based on its versions.
     */
    private RecipeGenerationResult generateRecipesForProject(SpringProject project) {
        int recipesCreated = 0;
        int recipesUpdated = 0;
        int transformationsCreated = 0;

        // Get sorted versions for this project
        List<ProjectVersion> versions = project.getVersions().stream()
            .filter(v -> v.getMajorVersion() != null && v.getMinorVersion() != null)
            .sorted(Comparator.comparing(ProjectVersion::getMajorVersion)
                .thenComparing(ProjectVersion::getMinorVersion)
                .thenComparing(v -> v.getPatchVersion() != null ? v.getPatchVersion() : 0))
            .toList();

        if (versions.isEmpty()) {
            log.debug("No versions found for project: {}", project.getSlug());
            return new RecipeGenerationResult(0, 0, 0);
        }

        // Group versions by major.minor
        Map<String, List<ProjectVersion>> versionGroups = versions.stream()
            .collect(Collectors.groupingBy(v -> v.getMajorVersion() + "." + v.getMinorVersion()));

        // Get sorted major.minor keys
        List<String> sortedMinorVersions = versionGroups.keySet().stream()
            .sorted(Comparator.comparing(this::parseVersion))
            .toList();

        // Create upgrade recipes between consecutive major.minor versions
        for (int i = 0; i < sortedMinorVersions.size() - 1; i++) {
            String fromMinor = sortedMinorVersions.get(i);
            String toMinor = sortedMinorVersions.get(i + 1);

            String recipeName = String.format("org.openrewrite.%s.Upgrade%sTo%s",
                toPackageName(project.getSlug()),
                fromMinor.replace(".", "_"),
                toMinor.replace(".", "_"));

            // Check if recipe already exists
            Optional<MigrationRecipe> existingRecipe = recipeRepository.findByName(recipeName);

            if (existingRecipe.isEmpty()) {
                MigrationRecipe recipe = createRecipeForVersionUpgrade(project, fromMinor, toMinor, recipeName);
                recipeRepository.save(recipe);

                // Generate transformations for this recipe
                List<MigrationTransformation> transformations = generateTransformationsForRecipe(recipe, project, fromMinor, toMinor);
                transformationRepository.saveAll(transformations);

                recipesCreated++;
                transformationsCreated += transformations.size();

                log.debug("Created recipe: {} with {} transformations", recipeName, transformations.size());
            } else {
                recipesUpdated++;
                log.debug("Recipe already exists: {}", recipeName);
            }
        }

        // Create a "latest upgrade" recipe if we have multiple versions
        if (sortedMinorVersions.size() > 1) {
            String fromMinor = sortedMinorVersions.get(0);
            String toMinor = sortedMinorVersions.get(sortedMinorVersions.size() - 1);

            String recipeName = String.format("org.openrewrite.%s.UpgradeToLatest_%s",
                toPackageName(project.getSlug()),
                toMinor.replace(".", "_"));

            if (recipeRepository.findByName(recipeName).isEmpty()) {
                MigrationRecipe recipe = createRecipeForVersionUpgrade(project, fromMinor, toMinor, recipeName);
                recipe.setDisplayName(String.format("Upgrade %s to Latest (%s)", project.getName(), toMinor));
                recipe.setDescription(String.format("Comprehensive upgrade recipe for %s to the latest version %s. " +
                    "Includes all necessary transformations for imports, dependencies, properties, and annotations.",
                    project.getName(), toMinor));
                recipeRepository.save(recipe);

                List<MigrationTransformation> transformations = generateTransformationsForRecipe(recipe, project, fromMinor, toMinor);
                transformationRepository.saveAll(transformations);

                recipesCreated++;
                transformationsCreated += transformations.size();
            }
        }

        return new RecipeGenerationResult(recipesCreated, recipesUpdated, transformationsCreated);
    }

    /**
     * Create a migration recipe for upgrading between versions.
     */
    private MigrationRecipe createRecipeForVersionUpgrade(SpringProject project, String fromMinor, String toMinor, String recipeName) {
        return MigrationRecipe.builder()
            .name(recipeName)
            .displayName(String.format("Upgrade %s %s → %s", project.getName(), fromMinor, toMinor))
            .description(String.format("Migration recipe for upgrading %s from version %s.x to %s.x. " +
                "This recipe handles common transformations including dependency updates, " +
                "import changes, property migrations, and annotation updates.",
                project.getName(), fromMinor, toMinor))
            .fromProject(project.getSlug())
            .fromVersionMin(fromMinor + ".0")
            .fromVersionMax(fromMinor + ".99")
            .toVersion(toMinor + ".0")
            .sourceType("GENERATED")
            .sourceUrl(project.getGithubUrl())
            .license("Apache-2.0")
            .isActive(true)
            .build();
    }

    /**
     * Generate transformations for a recipe based on project type.
     */
    private List<MigrationTransformation> generateTransformationsForRecipe(
            MigrationRecipe recipe, SpringProject project, String fromMinor, String toMinor) {

        List<MigrationTransformation> transformations = new ArrayList<>();
        String projectSlug = project.getSlug().toLowerCase();
        String packageBase = getPackageBase(projectSlug);

        // 1. Dependency transformation (always needed)
        transformations.add(createDependencyTransformation(recipe, project, fromMinor, toMinor));

        // 2. Import transformation (based on project type)
        if (packageBase != null) {
            transformations.add(createImportTransformation(recipe, packageBase, fromMinor, toMinor));
        }

        // 3. Property transformations (for projects with properties)
        if (hasPropertySupport(projectSlug)) {
            transformations.addAll(createPropertyTransformations(recipe, projectSlug, fromMinor, toMinor));
        }

        // 4. Annotation transformations (if applicable)
        transformations.addAll(createAnnotationTransformations(recipe, projectSlug, fromMinor, toMinor));

        return transformations;
    }

    private MigrationTransformation createDependencyTransformation(
            MigrationRecipe recipe, SpringProject project, String fromMinor, String toMinor) {

        String groupId = getGroupId(project.getSlug());
        String artifactId = getArtifactId(project.getSlug());

        return MigrationTransformation.builder()
            .recipe(recipe)
            .transformationType(TransformationType.DEPENDENCY)
            .category("Dependencies")
            .subcategory("Version Update")
            .oldPattern(String.format("%s:%s:%s.x", groupId, artifactId, fromMinor))
            .newPattern(String.format("%s:%s:%s.x", groupId, artifactId, toMinor))
            .explanation(String.format("Update %s dependency from version %s.x to %s.x in your build file.",
                project.getName(), fromMinor, toMinor))
            .codeExample(String.format("""
                // Gradle
                implementation '%s:%s:%s.0'

                // Maven
                <dependency>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s.0</version>
                </dependency>""",
                groupId, artifactId, toMinor, groupId, artifactId, toMinor))
            .breakingChange(false)
            .severity(Severity.INFO)
            .priority(100)
            .tags(List.of("dependency", "version-upgrade", project.getSlug()))
            .build();
    }

    private MigrationTransformation createImportTransformation(
            MigrationRecipe recipe, String packageBase, String fromMinor, String toMinor) {

        return MigrationTransformation.builder()
            .recipe(recipe)
            .transformationType(TransformationType.IMPORT)
            .category("Imports")
            .subcategory("Package Changes")
            .oldPattern(String.format("import %s.**", packageBase))
            .newPattern(String.format("import %s.**", packageBase))
            .explanation(String.format("Review import statements for any deprecated or renamed classes in version %s.", toMinor))
            .breakingChange(false)
            .severity(Severity.INFO)
            .priority(90)
            .tags(List.of("import", "package", recipe.getFromProject()))
            .build();
    }

    private List<MigrationTransformation> createPropertyTransformations(
            MigrationRecipe recipe, String projectSlug, String fromMinor, String toMinor) {

        List<MigrationTransformation> props = new ArrayList<>();
        String prefix = getPropertyPrefix(projectSlug);

        if (prefix != null) {
            props.add(MigrationTransformation.builder()
                .recipe(recipe)
                .transformationType(TransformationType.PROPERTY)
                .category("Properties")
                .subcategory("Configuration")
                .oldPattern(String.format("%s.*", prefix))
                .newPattern(String.format("%s.*", prefix))
                .explanation(String.format("Review %s.* properties for any deprecated or renamed configuration keys in version %s.",
                    prefix, toMinor))
                .filePattern("application*.properties,application*.yml,application*.yaml")
                .breakingChange(false)
                .severity(Severity.INFO)
                .priority(80)
                .tags(List.of("property", "configuration", projectSlug))
                .build());
        }

        return props;
    }

    private List<MigrationTransformation> createAnnotationTransformations(
            MigrationRecipe recipe, String projectSlug, String fromMinor, String toMinor) {

        List<MigrationTransformation> annotations = new ArrayList<>();
        List<String> projectAnnotations = getProjectAnnotations(projectSlug);

        for (String annotation : projectAnnotations) {
            annotations.add(MigrationTransformation.builder()
                .recipe(recipe)
                .transformationType(TransformationType.ANNOTATION)
                .category("Annotations")
                .subcategory("Framework Annotations")
                .oldPattern("@" + annotation)
                .newPattern("@" + annotation)
                .explanation(String.format("Review @%s annotation usage for any changes in version %s.", annotation, toMinor))
                .filePattern("*.java")
                .breakingChange(false)
                .severity(Severity.INFO)
                .priority(70)
                .tags(List.of("annotation", projectSlug))
                .build());
        }

        return annotations;
    }

    // Helper methods for project-specific configuration

    private String toPackageName(String slug) {
        return slug.replace("-", ".");
    }

    private double parseVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            return Double.parseDouble(parts[0]) * 100 + Double.parseDouble(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getPackageBase(String projectSlug) {
        return switch (projectSlug) {
            case "spring-boot" -> "org.springframework.boot";
            case "spring-framework" -> "org.springframework";
            case "spring-security" -> "org.springframework.security";
            case "spring-data-jpa" -> "org.springframework.data.jpa";
            case "spring-data-mongodb" -> "org.springframework.data.mongodb";
            case "spring-data-redis" -> "org.springframework.data.redis";
            case "spring-data-elasticsearch" -> "org.springframework.data.elasticsearch";
            case "spring-data-cassandra" -> "org.springframework.data.cassandra";
            case "spring-data-neo4j" -> "org.springframework.data.neo4j";
            case "spring-data-rest" -> "org.springframework.data.rest";
            case "spring-data-commons" -> "org.springframework.data";
            case "spring-cloud-gateway" -> "org.springframework.cloud.gateway";
            case "spring-cloud-config" -> "org.springframework.cloud.config";
            case "spring-cloud-netflix" -> "org.springframework.cloud.netflix";
            case "spring-cloud-openfeign" -> "org.springframework.cloud.openfeign";
            case "spring-cloud-stream" -> "org.springframework.cloud.stream";
            case "spring-batch" -> "org.springframework.batch";
            case "spring-integration" -> "org.springframework.integration";
            case "spring-kafka" -> "org.springframework.kafka";
            case "spring-amqp" -> "org.springframework.amqp";
            case "spring-graphql" -> "org.springframework.graphql";
            case "spring-session" -> "org.springframework.session";
            case "spring-hateoas" -> "org.springframework.hateoas";
            case "spring-webflow" -> "org.springframework.webflow";
            case "spring-ws" -> "org.springframework.ws";
            case "spring-ldap" -> "org.springframework.ldap";
            case "spring-vault" -> "org.springframework.vault";
            case "spring-credhub" -> "org.springframework.credhub";
            case "spring-ai" -> "org.springframework.ai";
            case "spring-modulith" -> "org.springframework.modulith";
            default -> "org.springframework." + projectSlug.replace("spring-", "").replace("-", ".");
        };
    }

    private String getGroupId(String projectSlug) {
        if (projectSlug.startsWith("spring-cloud-")) {
            return "org.springframework.cloud";
        } else if (projectSlug.startsWith("spring-data-")) {
            return "org.springframework.data";
        }
        return "org.springframework." + projectSlug.replace("spring-", "").split("-")[0];
    }

    private String getArtifactId(String projectSlug) {
        return projectSlug;
    }

    private boolean hasPropertySupport(String projectSlug) {
        return Set.of(
            "spring-boot", "spring-security", "spring-data-jpa", "spring-data-mongodb",
            "spring-data-redis", "spring-cloud-gateway", "spring-cloud-config",
            "spring-batch", "spring-kafka", "spring-session", "spring-ai"
        ).contains(projectSlug);
    }

    private String getPropertyPrefix(String projectSlug) {
        return switch (projectSlug) {
            case "spring-boot" -> "spring";
            case "spring-security" -> "spring.security";
            case "spring-data-jpa" -> "spring.jpa";
            case "spring-data-mongodb" -> "spring.data.mongodb";
            case "spring-data-redis" -> "spring.data.redis";
            case "spring-cloud-gateway" -> "spring.cloud.gateway";
            case "spring-cloud-config" -> "spring.cloud.config";
            case "spring-batch" -> "spring.batch";
            case "spring-kafka" -> "spring.kafka";
            case "spring-session" -> "spring.session";
            case "spring-ai" -> "spring.ai";
            default -> null;
        };
    }

    private List<String> getProjectAnnotations(String projectSlug) {
        return switch (projectSlug) {
            case "spring-boot" -> List.of("SpringBootApplication", "EnableAutoConfiguration", "ConfigurationProperties");
            case "spring-framework" -> List.of("Component", "Service", "Repository", "Controller", "RestController", "Configuration");
            case "spring-security" -> List.of("EnableWebSecurity", "PreAuthorize", "Secured", "EnableGlobalMethodSecurity");
            case "spring-data-jpa" -> List.of("Entity", "Repository", "Query", "EnableJpaRepositories");
            case "spring-data-mongodb" -> List.of("Document", "EnableMongoRepositories");
            case "spring-data-redis" -> List.of("RedisHash", "EnableRedisRepositories");
            case "spring-batch" -> List.of("EnableBatchProcessing", "StepScope", "JobScope");
            case "spring-cloud-gateway" -> List.of("EnableGateway");
            case "spring-cloud-config" -> List.of("EnableConfigServer", "RefreshScope");
            case "spring-cloud-openfeign" -> List.of("EnableFeignClients", "FeignClient");
            case "spring-kafka" -> List.of("KafkaListener", "EnableKafka");
            case "spring-amqp" -> List.of("RabbitListener", "EnableRabbit");
            case "spring-integration" -> List.of("IntegrationComponentScan", "EnableIntegration");
            case "spring-session" -> List.of("EnableSpringHttpSession", "EnableRedisHttpSession");
            default -> List.of();
        };
    }

    private record RecipeGenerationResult(int recipesCreated, int recipesUpdated, int transformationsCreated) {}

    /**
     * Add a custom transformation to an existing recipe.
     * Useful for manually adding migration knowledge not in OpenRewrite.
     */
    @Transactional
    public MigrationTransformation addCustomTransformation(
            Long recipeId,
            TransformationType type,
            String category,
            String oldPattern,
            String newPattern,
            String explanation,
            boolean breakingChange
    ) {
        MigrationRecipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + recipeId));

        MigrationTransformation transformation = MigrationTransformation.builder()
                .recipe(recipe)
                .transformationType(type)
                .category(category)
                .oldPattern(oldPattern)
                .newPattern(newPattern)
                .explanation(explanation)
                .breakingChange(breakingChange)
                .severity(breakingChange ? Severity.ERROR : Severity.INFO)
                .build();

        return transformationRepository.save(transformation);
    }

    /**
     * Create a new custom recipe.
     */
    @Transactional
    public MigrationRecipe createCustomRecipe(
            String name,
            String displayName,
            String description,
            String fromProject,
            String fromVersionMin,
            String toVersion
    ) {
        MigrationRecipe recipe = MigrationRecipe.builder()
                .name(name)
                .displayName(displayName)
                .description(description)
                .fromProject(fromProject)
                .fromVersionMin(fromVersionMin)
                .toVersion(toVersion)
                .sourceType("MANUAL")
                .license("Custom")
                .isActive(true)
                .build();

        return recipeRepository.save(recipe);
    }

    /**
     * Get sync status summary.
     */
    public RecipeSyncStatus getSyncStatus() {
        return new RecipeSyncStatus(
                featureConfig.isEnabled(),
                recipeRepository.countByIsActiveTrue(),
                transformationRepository.count(),
                transformationRepository.countByBreakingChangeTrue(),
                recipeRepository.findDistinctProjects(),
                recipeRepository.findDistinctTargetVersions()
        );
    }

    private void publishProgress(String phase, String message, int progress) {
        try {
            SyncProgressEvent event = SyncProgressEvent.builder()
                .currentPhase(7) // Recipe sync is Phase 7
                .totalPhases(8)
                .phaseDescription(phase)
                .status(progress < 0 ? "error" : (progress >= 100 ? "completed" : "running"))
                .progressPercent(Math.max(0, progress))
                .message(message)
                .completed(progress >= 100)
                .errorMessage(progress < 0 ? message : null)
                .build();
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.debug("Could not publish progress event: {}", e.getMessage());
        }
    }

    /**
     * Record class for sync status
     */
    public record RecipeSyncStatus(
            boolean enabled,
            long recipeCount,
            long transformationCount,
            long breakingChangeCount,
            List<String> projects,
            List<String> targetVersions
    ) {}

    /**
     * Record class for sync result
     */
    public record RecipeSyncResult(
            boolean success,
            int recipesProcessed,
            int transformationsProcessed,
            int errorsEncountered,
            String errorMessage
    ) {}
}

package com.spring.mcp.config;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.*;
import com.spring.mcp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Test data bootstrap configuration.
 * Populates the in-memory H2 database with test data for MCP integration tests.
 */
@TestConfiguration
@Profile("test")
public class TestDataBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(TestDataBootstrapConfig.class);

    // Known test API key - plain text for testing
    public static final String TEST_API_KEY = "test_api_key_for_mcp_testing_12345";
    public static final String TEST_API_KEY_NAME = "test-api-key";

    @Bean
    @Transactional
    public ApplicationRunner testDataBootstrap(
            SpringProjectRepository projectRepository,
            ProjectVersionRepository versionRepository,
            SpringBootVersionRepository springBootVersionRepository,
            CodeExampleRepository codeExampleRepository,
            ApiKeyRepository apiKeyRepository,
            LanguageVersionRepository languageVersionRepository,
            LanguageFeatureRepository languageFeatureRepository,
            FlavorRepository flavorRepository) {

        return args -> {
            log.info("Bootstrapping test data for MCP integration tests...");

            // Create test API key
            if (apiKeyRepository.findByName(TEST_API_KEY_NAME).isEmpty()) {
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
                ApiKey apiKey = new ApiKey();
                apiKey.setName(TEST_API_KEY_NAME);
                apiKey.setKeyHash(encoder.encode(TEST_API_KEY));
                apiKey.setCreatedBy("test-system");
                apiKey.setDescription("Test API key for MCP integration tests");
                apiKey.setIsActive(true);
                apiKey.setCreatedAt(LocalDateTime.now());
                apiKeyRepository.save(apiKey);
                log.info("Created test API key: {}", TEST_API_KEY_NAME);
            }

            // Create test projects
            createTestProjects(projectRepository, versionRepository, codeExampleRepository);

            // Create test Spring Boot versions
            createTestSpringBootVersions(springBootVersionRepository);

            // Create test language versions
            createTestLanguageVersions(languageVersionRepository, languageFeatureRepository);

            // Create test flavors
            createTestFlavors(flavorRepository);

            log.info("Test data bootstrap completed.");
        };
    }

    private void createTestProjects(SpringProjectRepository projectRepository,
                                     ProjectVersionRepository versionRepository,
                                     CodeExampleRepository codeExampleRepository) {
        // Spring Boot
        SpringProject springBoot = createProject(projectRepository, "Spring Boot", "spring-boot",
                "Spring Boot makes it easy to create stand-alone, production-grade Spring applications",
                "https://spring.io/projects/spring-boot",
                "https://github.com/spring-projects/spring-boot");

        ProjectVersion sbVersion = createVersion(versionRepository, springBoot, "3.5.8", 3, 5, 8,
                VersionState.GA, true, true);

        createCodeExample(codeExampleRepository, sbVersion, "Hello World", "Basic Spring Boot application",
                "@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}",
                "java", "Getting Started", new String[]{"beginner", "spring-boot"});

        // Spring Framework
        SpringProject springFramework = createProject(projectRepository, "Spring Framework", "spring-framework",
                "Provides core support for dependency injection, transaction management, web applications",
                "https://spring.io/projects/spring-framework",
                "https://github.com/spring-projects/spring-framework");

        createVersion(versionRepository, springFramework, "6.2.1", 6, 2, 1, VersionState.GA, true, true);

        // Spring Security
        SpringProject springSecurity = createProject(projectRepository, "Spring Security", "spring-security",
                "Highly customizable authentication and access-control framework",
                "https://spring.io/projects/spring-security",
                "https://github.com/spring-projects/spring-security");

        createVersion(versionRepository, springSecurity, "6.4.2", 6, 4, 2, VersionState.GA, true, true);

        // Spring Data JPA
        SpringProject springDataJpa = createProject(projectRepository, "Spring Data JPA", "spring-data-jpa",
                "Simplifies data access with JPA",
                "https://spring.io/projects/spring-data-jpa",
                "https://github.com/spring-projects/spring-data-jpa");

        createVersion(versionRepository, springDataJpa, "3.4.1", 3, 4, 1, VersionState.GA, true, true);

        // Spring Cloud
        SpringProject springCloud = createProject(projectRepository, "Spring Cloud", "spring-cloud",
                "Tools for building common patterns in distributed systems",
                "https://spring.io/projects/spring-cloud",
                "https://github.com/spring-cloud");

        createVersion(versionRepository, springCloud, "2024.0.0", 2024, 0, 0, VersionState.GA, true, true);

        log.info("Created {} test projects", 5);
    }

    private void createTestSpringBootVersions(SpringBootVersionRepository springBootVersionRepository) {
        createSpringBootVersion(springBootVersionRepository, "4.0.0", 4, 0, 0, VersionState.GA, true,
                LocalDate.of(2025, 11, 20), LocalDate.of(2027, 11, 20), LocalDate.of(2029, 11, 20));

        createSpringBootVersion(springBootVersionRepository, "3.5.8", 3, 5, 8, VersionState.GA, false,
                LocalDate.of(2025, 5, 15), LocalDate.of(2026, 5, 15), LocalDate.of(2028, 5, 15));

        createSpringBootVersion(springBootVersionRepository, "3.4.12", 3, 4, 12, VersionState.GA, false,
                LocalDate.of(2024, 11, 21), LocalDate.of(2025, 11, 21), LocalDate.of(2027, 11, 21));

        createSpringBootVersion(springBootVersionRepository, "4.0.1-SNAPSHOT", 4, 0, 1, VersionState.SNAPSHOT, false,
                null, null, null);

        log.info("Created test Spring Boot versions");
    }

    private void createTestLanguageVersions(LanguageVersionRepository languageVersionRepository,
                                             LanguageFeatureRepository languageFeatureRepository) {
        // Java versions
        LanguageVersion java21 = createLanguageVersion(languageVersionRepository, LanguageType.JAVA, "21", 21, 0,
                true, true, LocalDate.of(2023, 9, 19), LocalDate.of(2028, 9, 1));

        createLanguageFeature(languageFeatureRepository, java21, "Virtual Threads", "Lightweight threads for high-throughput concurrent applications",
                FeatureStatus.NEW, "Concurrency", "444", null, ImpactLevel.HIGH);

        createLanguageFeature(languageFeatureRepository, java21, "Record Patterns", "Deconstruct record values directly in pattern matching",
                FeatureStatus.NEW, "Pattern Matching", "440", null, ImpactLevel.MEDIUM);

        LanguageVersion java17 = createLanguageVersion(languageVersionRepository, LanguageType.JAVA, "17", 17, 0,
                true, false, LocalDate.of(2021, 9, 14), LocalDate.of(2026, 9, 1));

        createLanguageFeature(languageFeatureRepository, java17, "Sealed Classes", "Restrict which classes may extend or implement them",
                FeatureStatus.NEW, "Language", "409", null, ImpactLevel.MEDIUM);

        // Kotlin version
        LanguageVersion kotlin20 = createLanguageVersion(languageVersionRepository, LanguageType.KOTLIN, "2.0", 2, 0,
                false, true, LocalDate.of(2024, 5, 21), null);

        createLanguageFeature(languageFeatureRepository, kotlin20, "K2 Compiler", "New compiler with improved performance",
                FeatureStatus.NEW, "Compiler", null, "KEEP-1", ImpactLevel.HIGH);

        log.info("Created test language versions and features");
    }

    private void createTestFlavors(FlavorRepository flavorRepository) {
        createFlavor(flavorRepository, "hexagonal-spring-boot", "Hexagonal Architecture for Spring Boot",
                FlavorCategory.ARCHITECTURE, "hexagonal-architecture",
                "# Hexagonal Architecture\n\n## Overview\nHexagonal architecture separates business logic from external concerns.\n\n## Ports\n- Input ports: Use cases\n- Output ports: Repositories\n\n## Adapters\n- Primary: Controllers, CLI\n- Secondary: Database, External APIs",
                List.of("spring-boot", "architecture", "hexagonal"));

        createFlavor(flavorRepository, "gdpr-compliance", "GDPR Compliance Guidelines",
                FlavorCategory.COMPLIANCE, "gdpr",
                "# GDPR Compliance\n\n## Requirements\n- Data minimization\n- Right to erasure\n- Consent management\n\n## Implementation\n- Use @PersonalData annotation\n- Implement data export endpoints\n- Audit data access",
                List.of("gdpr", "compliance", "privacy"));

        createFlavor(flavorRepository, "api-development-agent", "API Development Agent Configuration",
                FlavorCategory.AGENTS, "api-development",
                "# API Development Agent\n\n## Capabilities\n- REST API design\n- OpenAPI spec generation\n- Endpoint testing\n\n## Tools\n- Spring Web\n- SpringDoc OpenAPI\n- RestAssured",
                List.of("api", "rest", "spring-web"));

        createFlavor(flavorRepository, "microservice-template", "Microservice Project Template",
                FlavorCategory.INITIALIZATION, "microservice",
                "# Microservice Template\n\n## Structure\n```\nsrc/\n  main/\n    java/\n      config/\n      controller/\n      service/\n      repository/\n```\n\n## Dependencies\n- Spring Boot Starter Web\n- Spring Boot Starter Data JPA\n- Spring Cloud Config Client",
                List.of("microservice", "spring-cloud", "template"));

        log.info("Created test flavors");
    }

    // Helper methods

    private SpringProject createProject(SpringProjectRepository projectRepository,
                                         String name, String slug, String description,
                                         String homepageUrl, String githubUrl) {
        return projectRepository.findBySlug(slug)
                .orElseGet(() -> {
                    SpringProject project = new SpringProject();
                    project.setName(name);
                    project.setSlug(slug);
                    project.setDescription(description);
                    project.setHomepageUrl(homepageUrl);
                    project.setGithubUrl(githubUrl);
                    project.setActive(true);
                    return projectRepository.save(project);
                });
    }

    private ProjectVersion createVersion(ProjectVersionRepository versionRepository,
                                          SpringProject project, String version,
                                          int major, int minor, int patch,
                                          VersionState state, boolean isLatest, boolean isDefault) {
        return versionRepository.findByProjectAndVersion(project, version)
                .orElseGet(() -> {
                    ProjectVersion pv = new ProjectVersion();
                    pv.setProject(project);
                    pv.setVersion(version);
                    pv.setMajorVersion(major);
                    pv.setMinorVersion(minor);
                    pv.setPatchVersion(patch);
                    pv.setState(state);
                    pv.setIsLatest(isLatest);
                    pv.setIsDefault(isDefault);
                    pv.setReleaseDate(LocalDate.now().minusDays(30));
                    pv.setReferenceDocUrl("https://docs.spring.io/" + project.getSlug() + "/reference/" + version);
                    pv.setApiDocUrl("https://docs.spring.io/" + project.getSlug() + "/" + version + "/api/java/");
                    return versionRepository.save(pv);
                });
    }

    private void createCodeExample(CodeExampleRepository codeExampleRepository,
                                   ProjectVersion version, String title, String description,
                                   String code, String language, String category, String[] tags) {
        CodeExample example = new CodeExample();
        example.setVersion(version);
        example.setTitle(title);
        example.setDescription(description);
        example.setCodeSnippet(code);
        example.setLanguage(language);
        example.setCategory(category);
        example.setTags(tags);
        example.setSourceUrl("https://github.com/spring-projects/spring-boot/blob/main/examples/" + title.toLowerCase().replace(" ", "-"));
        codeExampleRepository.save(example);
    }

    private void createSpringBootVersion(SpringBootVersionRepository springBootVersionRepository,
                                          String version, int major, int minor, int patch,
                                          VersionState state, boolean isCurrent,
                                          LocalDate releasedAt, LocalDate ossSupportEnd,
                                          LocalDate enterpriseSupportEnd) {
        if (springBootVersionRepository.findByVersion(version).isEmpty()) {
            SpringBootVersion sbv = new SpringBootVersion();
            sbv.setVersion(version);
            sbv.setMajorVersion(major);
            sbv.setMinorVersion(minor);
            sbv.setPatchVersion(patch);
            sbv.setState(state);
            sbv.setIsCurrent(isCurrent);
            sbv.setReleasedAt(releasedAt);
            sbv.setOssSupportEnd(ossSupportEnd);
            sbv.setEnterpriseSupportEnd(enterpriseSupportEnd);
            sbv.setReferenceDocUrl("https://docs.spring.io/spring-boot/" + version + "/index.html");
            sbv.setApiDocUrl("https://docs.spring.io/spring-boot/" + version + "/api/java/index.html");
            springBootVersionRepository.save(sbv);
        }
    }

    private LanguageVersion createLanguageVersion(LanguageVersionRepository languageVersionRepository,
                                                   LanguageType language, String version,
                                                   int major, int minor, boolean isLts, boolean isCurrent,
                                                   LocalDate releaseDate, LocalDate ossSupportEnd) {
        LanguageVersion lv = new LanguageVersion();
        lv.setLanguage(language);
        lv.setVersion(version);
        lv.setMajorVersion(major);
        lv.setMinorVersion(minor);
        lv.setIsLts(isLts);
        lv.setIsCurrent(isCurrent);
        lv.setReleaseDate(releaseDate);
        lv.setOssSupportEnd(ossSupportEnd);
        return languageVersionRepository.save(lv);
    }

    private void createLanguageFeature(LanguageFeatureRepository languageFeatureRepository,
                                        LanguageVersion version, String name, String description,
                                        FeatureStatus status, String category,
                                        String jepNumber, String kepNumber, ImpactLevel impactLevel) {
        LanguageFeature feature = new LanguageFeature();
        feature.setLanguageVersion(version);
        feature.setFeatureName(name);
        feature.setDescription(description);
        feature.setStatus(status);
        feature.setCategory(category);
        feature.setJepNumber(jepNumber);
        feature.setKepNumber(kepNumber);
        feature.setImpactLevel(impactLevel);
        languageFeatureRepository.save(feature);
    }

    private void createFlavor(FlavorRepository flavorRepository,
                               String uniqueName, String displayName, FlavorCategory category,
                               String patternName, String content, List<String> tags) {
        if (flavorRepository.findByUniqueName(uniqueName).isEmpty()) {
            Flavor flavor = new Flavor();
            flavor.setUniqueName(uniqueName);
            flavor.setDisplayName(displayName);
            flavor.setCategory(category);
            flavor.setPatternName(patternName);
            flavor.setContent(content);
            flavor.setDescription("Test flavor: " + displayName);
            flavor.setTags(tags);
            flavor.setIsActive(true);
            flavorRepository.save(flavor);
        }
    }
}
